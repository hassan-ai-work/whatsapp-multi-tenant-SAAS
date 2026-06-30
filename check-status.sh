#!/bin/bash

set -u

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JSON_MODE=false
NO_COLOR=false

for arg in "$@"; do
  case "$arg" in
    --json) JSON_MODE=true ;;
    --no-color) NO_COLOR=true ;;
    --help)
      cat <<'EOF'
Usage: bash check-status.sh [--json] [--no-color] [--help]

Options:
  --json      Print machine-readable JSON output.
  --no-color  Disable ANSI colors.
  --help      Show this help message.
EOF
      exit 0
      ;;
    *)
      echo "Unknown option: $arg" >&2
      exit 1
      ;;
  esac
done

if [ "$NO_COLOR" = true ]; then
  RED=''; GREEN=''; YELLOW=''; BLUE=''; NC=''
else
  RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
fi

container_specs=(
  "keycloak-postgres|5433|service"
  "keycloak|8180|service"
  "kafka-broker|9092|service"
  "kafka-ui|8989|service"
  "local-s3-server|9005,9006|service"
  "local-s3-initializer|-|oneshot"
  "replato-postgres|5432|service"
  "chat-service|8084|service"
  "chat-service-redis|6379|service"
)

service_specs=(
  "api-gateway|9000"
  "whatsapp-brain-service|8080"
  "chat-service|8084"
  "kafka-service|9092"
)

declare -A container_status_map
declare -A container_ports_map

while IFS='|' read -r name status ports; do
  [ -z "$name" ] && continue
  container_status_map["$name"]="$status"
  container_ports_map["$name"]="$ports"
done < <(docker ps -a --format '{{.Names}}|{{.Status}}|{{.Ports}}' 2>/dev/null)

is_port_open() {
  local port="$1"
  ss -ltn "( sport = :$port )" | grep -q ":$port"
}

status_color() {
  local state="$1"
  if [ "$state" = "running" ]; then
    printf "%bRUNNING%b" "$GREEN" "$NC"
  elif [ "$state" = "completed" ]; then
    printf "%bCOMPLETED%b" "$YELLOW" "$NC"
  else
    printf "%bSTOPPED%b" "$RED" "$NC"
  fi
}

container_json_rows=()
service_json_rows=()
container_running=0
container_stopped=0
container_completed=0
service_running=0
service_stopped=0

if [ "$JSON_MODE" = false ]; then
  echo -e "${BLUE}=== Docker Containers ===${NC}"
  printf "%-26s %-10s %-28s %s\n" "NAME" "STATUS" "EXPECTED PORTS" "PUBLISHED PORTS"
fi

for spec in "${container_specs[@]}"; do
  IFS='|' read -r name expected_ports container_kind <<<"$spec"
  raw_status="${container_status_map[$name]:-missing}"
  published_ports="${container_ports_map[$name]:-}"

  if [[ "$raw_status" == Up* ]]; then
    state="running"
    running=true
    container_running=$((container_running + 1))
  elif [[ "$raw_status" == Exited* ]] && [ "$container_kind" = "oneshot" ]; then
    state="completed"
    running=false
    container_completed=$((container_completed + 1))
  else
    state="stopped"
    running=false
    container_stopped=$((container_stopped + 1))
  fi

  container_json_rows+=("{\"name\":\"$name\",\"expected_ports\":\"$expected_ports\",\"published_ports\":\"$published_ports\",\"kind\":\"$container_kind\",\"state\":\"$state\",\"running\":$running,\"raw_status\":\"$raw_status\"}")

  if [ "$JSON_MODE" = false ]; then
    printf "%-26s %-10s %-28s %s\n" "$name" "$(status_color "$state")" "$expected_ports" "${published_ports:--}"
  fi
done

if [ "$JSON_MODE" = false ]; then
  echo
  echo -e "${BLUE}=== Local Services (Ports) ===${NC}"
  printf "%-26s %-10s %s\n" "SERVICE" "STATUS" "PORT"
fi

for spec in "${service_specs[@]}"; do
  name="${spec%%|*}"
  port="${spec##*|}"
  if is_port_open "$port"; then
    running=true
    state="running"
    service_running=$((service_running + 1))
  else
    running=false
    state="stopped"
    service_stopped=$((service_stopped + 1))
  fi

  service_json_rows+=("{\"name\":\"$name\",\"port\":$port,\"running\":$running}")

  if [ "$JSON_MODE" = false ]; then
    printf "%-26s %-10s %s\n" "$name" "$(status_color "$state")" "$port"
  fi
done

if [ "$JSON_MODE" = false ]; then
  echo
  echo -e "${BLUE}=== Summary ===${NC}"
  echo "Containers running: $container_running"
  echo "Containers completed: $container_completed"
  echo "Containers stopped: $container_stopped"
  echo "Services running:   $service_running"
  echo "Services stopped:   $service_stopped"
else
  printf '{"summary":{"containers_running":%d,"containers_completed":%d,"containers_stopped":%d,"services_running":%d,"services_stopped":%d},"containers":[%s],"services":[%s]}\n' \
    "$container_running" "$container_completed" "$container_stopped" "$service_running" "$service_stopped" \
    "$(IFS=,; echo "${container_json_rows[*]}")" \
    "$(IFS=,; echo "${service_json_rows[*]}")"
fi

