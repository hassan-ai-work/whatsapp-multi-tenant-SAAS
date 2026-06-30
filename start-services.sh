#!/bin/bash

# Configuration
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
SERVICES=("api-gateway" "kafka-service" "whatsapp-brain-service" "chat-service")
PORTS=("9000" "8080" "8080" "8084")
DEBUG_PORTS=("5005" "5006" "5007" "5008")
SELECTED_SERVICES=(); ACTIVE_SERVICES=(); ACTIVE_PORTS=(); ACTIVE_DEBUG_PORTS=()
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$PROJECT_ROOT/logs"; PID_FILE="$PROJECT_ROOT/.microservices.pids"
DEBUG_MODE=false; DEBUG_SUSPEND="n"; START_INFRA=false; START_SERVICES=false
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/temurin-21-jdk}"

log() {
    local symbol="✓"; local color=$GREEN; [[ $1 == "warn" ]] && { symbol="⚠"; color=$YELLOW; }; [[ $1 == "err" ]] && { symbol="✗"; color=$RED; }; echo -e "${color}${symbol}${NC} $2"
}

print_usage() {
    echo -e "${BLUE}Usage:${NC} bash start-services.sh [options] [service-flags]\n"
    echo "Options: --start-infra, --start-services, --all, --debug, --debug-suspend, --help"
    echo -e "Service Flags: --api-gateway (5005), --kafka-service (5006), --whatsapp-brain-service (5007), --chat-service (5008)\n"
    echo "Note: If no service flags are provided, all services are selected."
}

echo -e "${BLUE}========================================\nMicroservices Startup Script\n========================================${NC}"

while [ $# -gt 0 ]; do
    case "$1" in
        --start-infra) START_INFRA=true ;;
        --start-services) START_SERVICES=true ;;
        --all) START_INFRA=true; START_SERVICES=true ;;
        --debug) DEBUG_MODE=true ;;
        --debug-suspend) DEBUG_MODE=true; DEBUG_SUSPEND="y" ;;
        --help) print_usage; exit 0 ;;
        *)  match=false
            for s in "${SERVICES[@]}"; do [[ "--$s" == "$1" ]] && { SELECTED_SERVICES+=("$s"); match=true; break; }; done
            $match || { log "err" "Unknown option: $1"; echo; print_usage; exit 1; } ;;
    esac
    shift
done

[[ "$START_INFRA" = false && "$START_SERVICES" = false ]] && { log "warn" "No start mode selected.\n"; print_usage; exit 1; }

for i in "${!SERVICES[@]}"; do
    s="${SERVICES[$i]}"
    if [[ ${#SELECTED_SERVICES[@]} -eq 0 ]]; then
        found=true
    else
        found=false; for sel in "${SELECTED_SERVICES[@]}"; do [[ "$sel" == "$s" ]] && found=true; done
    fi
    $found && { ACTIVE_SERVICES+=("$s"); ACTIVE_PORTS+=("${PORTS[$i]}"); ACTIVE_DEBUG_PORTS+=("${DEBUG_PORTS[$i]}"); }
done

[[ "$DEBUG_MODE" = true && "$START_SERVICES" = false ]] && log "warn" "--debug provided, but services will not start."
[[ "$DEBUG_MODE" = true && "$START_SERVICES" = true ]] && log "warn" "Debug mode enabled (suspend=${DEBUG_SUSPEND})"

mkdir -p "$LOG_DIR" && log "ok" "Logs directory ready: $LOG_DIR"
[[ "$START_SERVICES" = true ]] && rm -f "$PID_FILE"
for s in "${SERVICES[@]}"; do rm -f "$PROJECT_ROOT/${s}.log"; done

if [ "$START_INFRA" = true ]; then
    echo -e "\n${BLUE}Checking System Services...${NC}"
    if systemctl is-active --quiet ollama; then log "ok" "Ollama service is already running"
    else echo -e "${BLUE}Starting Ollama...${NC}"; sudo systemctl start ollama && log "ok" "Ollama started" || { log "err" "Failed to start Ollama"; exit 1; }; fi

    echo -e "\n${BLUE}Checking Docker...${NC}"
    docker info > /dev/null 2>&1 || { log "err" "Docker is not running."; exit 1; }; log "ok" "Docker is running"

    echo -e "\n${BLUE}Starting infrastructure...${NC}"
    cd "$PROJECT_ROOT" && docker compose up -d
    [[ $? -eq 0 ]] && { log "ok" "Infra started successfully"; log "warn" "Root docker includes configurations for Gateway, Kafka, and Brain."; sleep 10; } || { log "err" "Failed to start infra"; exit 1; }
else
    log "warn" "Skipping infrastructure startup. Run manually with: docker compose up -d"
fi
if [ "$START_SERVICES" = true ]; then
    echo -e "\n${BLUE}Building selected modules...${NC}"
    for s in "${ACTIVE_SERVICES[@]}"; do
        BUILD_LOG="$LOG_DIR/build-${s}.log"
        echo -e "\n${BLUE}Building ${s}...${NC}"
        cd "$PROJECT_ROOT/$s" || { log "err" "Directory not found: $s"; exit 1; }
        ./mvnw clean package -DskipTests -q > "$BUILD_LOG" 2>&1
        [[ $? -eq 0 ]] && log "ok" "${s} built. Logs: $BUILD_LOG" || { log "err" "${s} build failed. Check $BUILD_LOG"; exit 1; }
    done

    echo -e "\n${BLUE}Starting microservices...${NC}"
    for i in "${!ACTIVE_SERVICES[@]}"; do
        s="${ACTIVE_SERVICES[$i]}"; p="${ACTIVE_PORTS[$i]}"
        echo -e "\n${BLUE}Starting ${s} on port ${p}...${NC}"
        cd "$PROJECT_ROOT/$s" || { log "err" "Directory not found: $s"; exit 1; }
        START_LOG="$LOG_DIR/startup-${s}.log"

        if [ "$DEBUG_MODE" = true ]; then
            dp="${ACTIVE_DEBUG_PORTS[$i]}"
            nohup ./mvnw spring-boot:run "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=${DEBUG_SUSPEND},address=*:${dp}" > "$START_LOG" 2>&1 &
        else
            nohup ./mvnw spring-boot:run > "$START_LOG" 2>&1 &
        fi
        pid=$!; echo "$pid" >> "$PID_FILE"
        log "ok" "${s} started (PID: $pid)"
        log "warn" "Logs: tail -f $START_LOG"
        [[ "$DEBUG_MODE" = true ]] && log "warn" "Debug: attach IDE to port ${dp}"
        sleep 5
    done
else
    log "warn" "Skipping service startup. Run from your IDE for local debugging."
fi

echo -e "\n${BLUE}========================================\nRequested startup actions completed!\n========================================${NC}"

if [ "$START_SERVICES" = true ]; then
    echo -e "\nServices running on:"
    for i in "${!ACTIVE_SERVICES[@]}"; do echo -e "  ${GREEN}${ACTIVE_SERVICES[$i]}${NC}: http://localhost:${ACTIVE_PORTS[$i]}"; done
fi

if [ "$START_INFRA" = true ]; then
    echo -e "\nInfrastructure endpoints:"
    echo -e "  ${GREEN}Keycloak${NC}: http://localhost:8180"
    echo -e "  ${GREEN}Kafka UI${NC}: http://localhost:8989/kafka-ui"
    echo -e "  ${GREEN}MinIO API${NC}: http://localhost:9005"
    echo -e "  ${GREEN}MinIO Console${NC}: http://localhost:9006"

    # Verifying Ollama status for the output summary
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:11434 | grep -q "200"; then
        echo -e "  ${GREEN}Ollama API${NC}: http://localhost:11434 (Running)"
    else
        echo -e "  ${RED}Ollama API${NC}: http://localhost:11434 (Service active but API unreachable)"
    fi
fi

if [ "$START_SERVICES" = true ]; then
    echo -e "\n${YELLOW}To view logs:${NC}"
    for s in "${ACTIVE_SERVICES[@]}"; do echo -e "  tail -f $LOG_DIR/startup-${s}.log"; done
fi

if [ "$DEBUG_MODE" = true ] && [ "$START_SERVICES" = true ]; then
    echo -e "\n${YELLOW}Remote debug ports:${NC}"
    for i in "${!ACTIVE_SERVICES[@]}"; do echo -e "  ${GREEN}${ACTIVE_SERVICES[$i]}${NC}: localhost:${ACTIVE_DEBUG_PORTS[$i]}"; done
fi

echo -e "\n${YELLOW}Management Utilities:${NC}"
echo -e "  Stop Services:  bash $PROJECT_ROOT/stop-services.sh"
echo -e "  Check Status:   bash $PROJECT_ROOT/check-status.sh"
