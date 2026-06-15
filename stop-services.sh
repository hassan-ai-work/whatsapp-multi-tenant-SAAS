#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICES=("api-gateway" "kafka-service" "whatsapp-brain-service")
PORTS=("9000" "8080" "8080")
SELECTED_SERVICES=()
STOP_INFRA=false

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Stopping Microservices${NC}"
echo -e "${BLUE}========================================${NC}"

print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_usage() {
    cat <<EOF
Usage: bash stop-services.sh [options] [service-flags]

Options:
  --stop-infra              Stop docker infrastructure containers.
  --all                     Stop selected services and infrastructure.
  --help                    Show this help message.

Service flags:
  --api-gateway
  --kafka-service
  --whatsapp-brain-service

Examples:
  bash stop-services.sh
  bash stop-services.sh --api-gateway
  bash stop-services.sh --stop-infra
  bash stop-services.sh --all
  bash stop-services.sh --api-gateway --kafka-service
EOF
}

add_selected_service() {
    local service=$1
    for existing in "${SELECTED_SERVICES[@]}"; do
        if [ "$existing" = "$service" ]; then
            return
        fi
    done
    SELECTED_SERVICES+=("$service")
}

stop_single_service() {
    local service=$1
    local port=$2

    pkill -f "/${service}/.*spring-boot:run" 2>/dev/null || true

    local pids_on_port
    pids_on_port=$(lsof -ti :"$port" 2>/dev/null | tr '\n' ' ')
    if [ -n "$pids_on_port" ]; then
        kill $pids_on_port 2>/dev/null || true
        print_status "Stopped ${service} on port ${port} (PID(s): ${pids_on_port})"
    else
        print_warning "No process found on port ${port} for ${service}"
    fi
}

STOP_ALL=false

for arg in "$@"; do
    case "$arg" in
        --api-gateway)
            add_selected_service "api-gateway"
            ;;
        --kafka-service)
            add_selected_service "kafka-service"
            ;;
        --whatsapp-brain-service)
            add_selected_service "whatsapp-brain-service"
            ;;
        --stop-infra)
            STOP_INFRA=true
            ;;
        --all)
            STOP_ALL=true
            STOP_INFRA=true
            ;;
        --help)
            print_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $arg"
            print_usage
            exit 1
            ;;
    esac
done

if [ "$STOP_ALL" = true ] || [ ${#SELECTED_SERVICES[@]} -eq 0 ]; then
    SELECTED_SERVICES=("${SERVICES[@]}")
fi

echo -e "\n${BLUE}Stopping selected services...${NC}"
for service in "${SELECTED_SERVICES[@]}"; do
    for i in "${!SERVICES[@]}"; do
        if [ "${SERVICES[$i]}" = "$service" ]; then
            stop_single_service "$service" "${PORTS[$i]}"
            break
        fi
    done
done

if [ -f "$PROJECT_ROOT/.microservices.pids" ]; then
    tmp_file=$(mktemp)
    while read -r PID; do
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "$PID" >> "$tmp_file"
        fi
    done < "$PROJECT_ROOT/.microservices.pids"
    mv "$tmp_file" "$PROJECT_ROOT/.microservices.pids"
fi

echo -e "\n${BLUE}Stopping any remaining matching Spring Boot processes...${NC}"
for service in "${SELECTED_SERVICES[@]}"; do
    pkill -f "/${service}/.*spring-boot:run" 2>/dev/null || true
done
print_status "Selected Spring Boot processes processed"

if [ "$STOP_INFRA" = true ]; then
    echo -e "\n${BLUE}Stopping containers...${NC}"
    cd "$PROJECT_ROOT" || exit 1
    docker compose down
    if [ $? -eq 0 ]; then
        print_status "Containers stopped successfully"
    else
        print_error "Error stopping containers"
    fi
else
    print_warning "Infrastructure containers left running"
fi

echo -e "\n${GREEN}Done.${NC}"
