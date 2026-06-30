#!/bin/bash

# Configuration
RED='\033;0/31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICES=("api-gateway" "kafka-service" "whatsapp-brain-service" "chat-service")
PORTS=("9000" "8080" "8080" "8084")
SELECTED_SERVICES=(); STOP_INFRA=false; STOP_ALL=false

log() {
    local symbol="✓"; local color=$GREEN; [[ $1 == "warn" ]] && { symbol="⚠"; color=$YELLOW; }; [[ $1 == "err" ]] && { symbol="✗"; color=$RED; }; echo -e "${color}${symbol}${NC} $2"
}

print_usage() {
    echo -e "${BLUE}Usage:${NC} bash stop-services.sh [options] [service-flags]\n"
    echo "Options: --stop-infra, --all, --help"
    echo "Service Flags: --api-gateway, --kafka-service, --whatsapp-brain-service, --chat-service"
}

echo -e "${BLUE}========================================\nStopping Microservices\n========================================${NC}"

for arg in "$@"; do
    case "$arg" in
        --stop-infra) STOP_INFRA=true ;;
        --all) STOP_ALL=true; STOP_INFRA=true ;;
        --help) print_usage; exit 0 ;;
        *)  match=false
            for s in "${SERVICES[@]}"; do [[ "--$s" == "$arg" ]] && { SELECTED_SERVICES+=("$s"); match=true; break; }; done
            $match || { log "err" "Unknown option: $arg"; print_usage; exit 1; } ;;
    esac
done

[[ "$STOP_ALL" = true || ${#SELECTED_SERVICES[@]} -eq 0 ]] && SELECTED_SERVICES=("${SERVICES[@]}")

echo -e "\n${BLUE}Stopping selected services...${NC}"
for s in "${SELECTED_SERVICES[@]}"; do
    for i in "${!SERVICES[@]}"; do
        if [[ "${SERVICES[$i]}" == "$s" ]]; then
            pkill -f "/${s}/.*spring-boot:run" 2>/dev/null
            pids=$(lsof -ti :"${PORTS[$i]}" 2>/dev/null | tr '\n' ' ')
            [[ -n "$pids" ]] && { kill $pids 2>/dev/null; log "ok" "Stopped ${s} on port ${PORTS[$i]} (PID(s): ${pids})"; } || log "warn" "No process found on port ${PORTS[$i]} for ${s}"
        fi
    done
done

# Clean up dead PIDs from local tracker file
PID_FILE="$PROJECT_ROOT/.microservices.pids"
[[ -f "$PID_FILE" ]] && awk '{print $1}' "$PID_FILE" | xargs ps -p >/dev/null 2>&1 | awk '{print $1}' > "${PID_FILE}.tmp" && mv "${PID_FILE}.tmp" "$PID_FILE"

if [ "$STOP_INFRA" = true ]; then
    echo -e "\n${BLUE}Stopping system infrastructure...${NC}"
    if systemctl is-active --quiet ollama; then
        echo -e "${BLUE}Stopping Ollama service...${NC}"
        sudo systemctl stop ollama && log "ok" "Ollama stopped successfully" || log "warn" "Failed to stop Ollama container service"
    fi

    echo -e "\n${BLUE}Stopping Docker containers...${NC}"
    cd "$PROJECT_ROOT" && docker compose down
    [[ $? -eq 0 ]] && log "ok" "Containers stopped successfully" || log "err" "Error stopping containers"
else
    log "warn" "Infrastructure containers and host services left running"
fi

echo -e "\n${GREEN}Done.${NC}"
