#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$PROJECT_ROOT/logs"

SERVICES=("api-gateway" "product-service" "order-service" "inventory-service" "kafka-service" "notification-service" "chat-service")
PORTS=("9000" "8080" "8081" "8082" "8989" "8083" "8084")
SELECTED_SERVICES=()
START_INFRA=false

print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

usage() {
    cat << 'EOF'
Usage:
  bash restart-service.sh <number|service-name>
  bash restart-service.sh [options] [service-flags]

Options:
  --start-infra            Start docker infrastructure before restart.
  --help                   Show this help message.

Examples:
  bash restart-service.sh 6
  bash restart-service.sh notification-service
  bash restart-service.sh --notification-service
  bash restart-service.sh --kafka-service
  bash restart-service.sh --start-infra --api-gateway

Service flags:
  --api-gateway
  --product-service
  --order-service
  --inventory-service
  --kafka-service
  --notification-service
  --chat-service
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

start_infra_if_requested() {
    if [ "$START_INFRA" = true ]; then
        echo -e "${BLUE}Starting infrastructure...${NC}"
        cd "$PROJECT_ROOT" || return 1
        docker compose up -d
        if [ $? -eq 0 ]; then
            print_status "Infrastructure started successfully"
            sleep 5
        else
            print_error "Failed to start infrastructure"
            return 1
        fi
    fi
}

restart_single_service() {
    local target_service=$1
    local target_port=$2

    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Restarting ${target_service}${NC}"
    echo -e "${BLUE}========================================${NC}"

    if [ "$target_service" = "kafka-service" ]; then
        echo -e "${BLUE}Running Docker restart sequence for Kafka ecosystem...${NC}"
        cd "$PROJECT_ROOT/kafka-service" || return 1
        docker compose restart kafka kafka-ui
        if [ $? -eq 0 ]; then
            print_status "Kafka containers restarted successfully via Docker"
            echo
            return 0
        else
            print_error "Failed to restart Kafka containers via Docker"
            echo
            return 1
        fi
    fi

    mkdir -p "$LOG_DIR"

    pids_on_port=$(lsof -ti :"$target_port" 2>/dev/null | tr '\n' ' ')
    if [ -n "$pids_on_port" ]; then
        kill $pids_on_port 2>/dev/null
        sleep 2
        print_status "Stopped process(es) on port ${target_port}: $pids_on_port"
    else
        print_warning "No process found on port ${target_port}; starting fresh"
    fi

    pkill -f "/${target_service}/.*spring-boot:run" 2>/dev/null || true

    cd "$PROJECT_ROOT/$target_service" || return 1
    export JAVA_HOME="/usr/lib/jvm/temurin-21-jdk"
    STARTUP_LOG="$LOG_DIR/startup-${target_service}.log"

    nohup ./mvnw spring-boot:run > "$STARTUP_LOG" 2>&1 &
    NEW_PID=$!

    sleep 4
    if ps -p "$NEW_PID" > /dev/null 2>&1; then
        print_status "${target_service} restarted successfully (PID: $NEW_PID)"
        print_warning "Startup logs: tail -f $STARTUP_LOG"
        print_warning "If using IntelliJ, you may prefer running this service directly from the IDE instead"
        echo
        return 0
    fi

    print_error "Failed to start ${target_service}. Check startup logs: $STARTUP_LOG"
    echo
    return 1
}

if [ $# -eq 0 ]; then
    usage
    exit 1
fi

for arg in "$@"; do
    case "$arg" in
        --api-gateway)
            add_selected_service "api-gateway"
            ;;
        --product-service)
            add_selected_service "product-service"
            ;;
        --order-service)
            add_selected_service "order-service"
            ;;
        --inventory-service)
            add_selected_service "inventory-service"
            ;;
        --kafka-service)
            add_selected_service "kafka-service"
            ;;
        --notification-service)
            add_selected_service "notification-service"
            ;;
        --chat-service)
            add_selected_service "chat-service"
            ;;
        --start-infra)
            START_INFRA=true
            ;;
        --help)
            usage
            exit 0
            ;;
        *)
            resolved=false
            for i in "${!SERVICES[@]}"; do
                if [ "$arg" = "$((i + 1))" ] || [ "$arg" = "${SERVICES[$i]}" ]; then
                    add_selected_service "${SERVICES[$i]}"
                    resolved=true
                    break
                fi
            done

            if [ "$resolved" = false ]; then
                print_error "Invalid service identifier: $arg"
                usage
                exit 1
            fi
            ;;
    esac
done

if [ ${#SELECTED_SERVICES[@]} -eq 0 ]; then
    print_error "No service selected"
    usage
    exit 1
fi

start_infra_if_requested || exit 1

overall_exit=0
for service in "${SELECTED_SERVICES[@]}"; do
    for i in "${!SERVICES[@]}"; do
        if [ "${SERVICES[$i]}" = "$service" ]; then
            restart_single_service "$service" "${PORTS[$i]}" || overall_exit=1
            break
        fi
    done
done

exit $overall_exit
