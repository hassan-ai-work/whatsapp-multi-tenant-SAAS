#!/bin/bash

set -u

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JSON_MODE=false
RESULTS_FILE=$(mktemp)

cleanup() {
    rm -f "$RESULTS_FILE"
}
trap cleanup EXIT

while [ $# -gt 0 ]; do
    case "$1" in
        --json)
            JSON_MODE=true
            ;;
        --help)
            cat <<EOF
Usage: bash healthcheck-project.sh [--json] [--help]

Options:
  --json   Print machine-readable JSON output.
  --help   Show this help message.
EOF
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            exit 1
            ;;
    esac
    shift
done

PASS_COUNT=0
FAIL_COUNT=0

print_pass() {
    if [ "$JSON_MODE" = false ]; then
        echo "[PASS] $1"
    fi
    printf 'PASS\t%s\n' "$1" >> "$RESULTS_FILE"
    PASS_COUNT=$((PASS_COUNT + 1))
}

print_fail() {
    if [ "$JSON_MODE" = false ]; then
        echo "[FAIL] $1"
    fi
    printf 'FAIL\t%s\n' "$1" >> "$RESULTS_FILE"
    FAIL_COUNT=$((FAIL_COUNT + 1))
}

run_http_check() {
    local name="$1"
    local url="$2"
    local expected_codes_regex="$3"
    local required_substring=""
    if [ "$#" -ge 4 ]; then
        required_substring="$4"
        shift 4
    else
        shift 3
    fi

    local body_file
    body_file=$(mktemp)
    local header_file
    header_file=$(mktemp)

    local code
    code=$(curl -sS --max-time 8 "$@" -D "$header_file" -o "$body_file" -w "%{http_code}" "$url" 2>/dev/null || true)
    if [ -z "$code" ]; then
        code="000"
    fi

    local body_preview
    body_preview=$(head -c 140 "$body_file" | tr '\n' ' ')
    local full_response
    full_response="$(cat "$header_file")$(cat "$body_file")"

    rm -f "$body_file"
    rm -f "$header_file"

    if [[ "$code" =~ ^($expected_codes_regex)$ ]]; then
        if [ -n "$required_substring" ] && [[ "$full_response" != *"$required_substring"* ]]; then
            print_fail "$name -> HTTP $code but missing expected text '$required_substring' (body: $body_preview)"
            return
        fi
        print_pass "$name -> HTTP $code"
    else
        print_fail "$name -> HTTP $code (body: $body_preview)"
    fi
}

run_service_status_check() {
    local status_json
    if ! status_json=$(bash "$PROJECT_ROOT/check-services.sh" --json 2>/dev/null); then
        print_fail "Service status check command failed"
        return
    fi

    if printf '%s' "$status_json" | python -c '
import json, sys
obj = json.load(sys.stdin)
summary = obj.get("summary", {})
required = {"api-gateway", "whatsapp-brain-service"}
seen_running = {s.get("name") for s in obj.get("services", []) if s.get("running") is True}
if not required.issubset(seen_running):
    raise SystemExit(1)
' >/dev/null 2>&1
    then
        print_pass "Core services are running (api-gateway, whatsapp-brain-service)"
    else
        print_fail "Service status check failed: not all core services are running"
        if [ "$JSON_MODE" = false ]; then
            echo "$status_json"
        fi
    fi
}

if [ "$JSON_MODE" = false ]; then
    echo "=== Project Healthcheck ==="
fi

run_service_status_check

run_http_check "Keycloak base endpoint reachable" "http://localhost:8180" "200|301|302"
run_http_check "Gateway Swagger UI protected or reachable" "http://localhost:9000/swagger-ui/index.html" "200|302"
run_http_check "WhatsApp Brain OpenAPI docs reachable" "http://localhost:8080/v3/api-docs" "200" "\"openapi\""
run_http_check "WhatsApp Brain Swagger UI reachable" "http://localhost:8080/swagger-ui/index.html" "200|302" "OpenAPI definition"
run_http_check "Kafka UI reachable" "http://localhost:8989/kafka-ui" "200|301|302"
run_http_check "MinIO API health reachable" "http://localhost:9005/minio/health/live" "200"
run_http_check "MinIO Console reachable" "http://localhost:9006" "200|301|302"

if [ "$JSON_MODE" = false ]; then
    echo
    echo "=== Healthcheck Summary ==="
    echo "Passed: $PASS_COUNT"
    echo "Failed: $FAIL_COUNT"
else
    python - <<'PY' "$RESULTS_FILE" "$PASS_COUNT" "$FAIL_COUNT"
import json
import sys

results_file = sys.argv[1]
pass_count = int(sys.argv[2])
fail_count = int(sys.argv[3])

checks = []
with open(results_file, 'r', encoding='utf-8') as handle:
    for line in handle:
        line = line.rstrip('\n')
        if not line:
            continue
        status, message = line.split('\t', 1)
        checks.append({"status": status, "message": message})

print(json.dumps({
    "summary": {
        "passed": pass_count,
        "failed": fail_count,
        "total": pass_count + fail_count,
        "ok": fail_count == 0
    },
    "checks": checks
}, indent=2))
PY
fi

if [ "$FAIL_COUNT" -gt 0 ]; then
    exit 1
fi

exit 0
