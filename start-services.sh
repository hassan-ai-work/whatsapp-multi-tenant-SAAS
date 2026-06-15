#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SERVICES=("api-gateway" "kafka-service" "whatsapp-brain-service")
PORTS=("9000" "8080" "8080")
DEBUG_PORTS=("5005" "5006" "5007")
SELECTED_SERVICES=()
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$PROJECT_ROOT/logs"
PID_FILE="$PROJECT_ROOT/.microservices.pids"
DEBUG_MODE=false
DEBUG_SUSPEND="n"
START_INFRA=false
START_SERVICES=false

# Set JAVA_HOME if not already set
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME="/usr/lib/jvm/temurin-21-jdk"
fi

print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_usage() {
    cat <<EOF
Usage: bash start-services.sh [options] [service-flags]

Options:
  --start-infra             Start docker infrastructure services only.
  --start-services          Build and start selected services locally.
  --all                     Start both infrastructure and selected services.
  --debug                   Start selected services with remote debugging enabled.
  --debug-suspend           Start selected services in debug mode and wait for the debugger to attach.
  --api-gateway             Select api-gateway.
  --kafka-service           Select kafka-service.
  --whatsapp-brain-service  Select whatsapp-brain-service.
  --help                    Show this help message.

Examples:
  bash start-services.sh --start-infra
  bash start-services.sh --start-services --api-gateway
  bash start-services.sh --all --debug --api-gateway --kafka-service

Notes:
  - If no service flags are provided, all services are selected.
  - Use --start-infra when you only want Docker dependencies running.
  - Run services from IntelliJ if you want IDE debugging instead of script-based startup.

Remote debug ports:
  api-gateway              5005
  kafka-service            5006
  whatsapp-brain-service   5007
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

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Microservices Startup Script${NC}"
echo -e "${BLUE}========================================${NC}"

while [ $# -gt 0 ]; do
    case "$1" in
        --start-infra)
            START_INFRA=true
            ;;
        --start-services)
            START_SERVICES=true
            ;;
        --all)
            START_INFRA=true
            START_SERVICES=true
            ;;
        --debug)
            DEBUG_MODE=true
            ;;
        --debug-suspend)
            DEBUG_MODE=true
            DEBUG_SUSPEND="y"
            ;;
        --api-gateway)
            add_selected_service "api-gateway"
            ;;
        --kafka-service)
            add_selected_service "kafka-service"
            ;;
        --whatsapp-brain-service)
            add_selected_service "whatsapp-brain-service"
            ;;
        --help)
            print_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            echo
            print_usage
            exit 1
            ;;
    esac
    shift
done

if [ "$START_INFRA" = false ] && [ "$START_SERVICES" = false ]; then
    print_warning "No start mode selected."
    echo
    print_usage
    exit 1
fi

ACTIVE_SERVICES=()
ACTIVE_PORTS=()
ACTIVE_DEBUG_PORTS=()

if [ ${#SELECTED_SERVICES[@]} -eq 0 ]; then
    ACTIVE_SERVICES=("${SERVICES[@]}")
    ACTIVE_PORTS=("${PORTS[@]}")
    ACTIVE_DEBUG_PORTS=("${DEBUG_PORTS[@]}")
else
    for selected in "${SELECTED_SERVICES[@]}"; do
        for i in "${!SERVICES[@]}"; do
            if [ "${SERVICES[$i]}" = "$selected" ]; then
                ACTIVE_SERVICES+=("${SERVICES[$i]}")
                ACTIVE_PORTS+=("${PORTS[$i]}")
                ACTIVE_DEBUG_PORTS+=("${DEBUG_PORTS[$i]}")
                break
            fi
        done
    done
fi

if [ "$DEBUG_MODE" = true ] && [ "$START_SERVICES" = false ]; then
    print_warning "--debug was provided, but services will not be started by this script."
fi

if [ "$DEBUG_MODE" = true ] && [ "$START_SERVICES" = true ]; then
    print_warning "Debug mode enabled (suspend=${DEBUG_SUSPEND})"
fi

# Prepare runtime directories
mkdir -p "$LOG_DIR"
print_status "Logs directory ready: $LOG_DIR"

if [ "$START_SERVICES" = true ]; then
    rm -f "$PID_FILE"
fi

# Clean up legacy root-level service logs
for SERVICE in "${SERVICES[@]}"; do
    rm -f "$PROJECT_ROOT/${SERVICE}.log"
done

# Start infrastructure only if requested
if [ "$START_INFRA" = true ]; then
    echo -e "\n${BLUE}Checking Docker...${NC}"
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker first."
        exit 1
    fi
    print_status "Docker is running"

    echo -e "\n${BLUE}Starting infrastructure services...${NC}"
    cd "$PROJECT_ROOT" || exit 1
    docker compose up -d
    if [ $? -eq 0 ]; then
        print_status "Infrastructure services started successfully"
        print_warning "The root docker-compose.yaml currently includes configurations from:"
        echo -e "  • api-gateway/docker-compose.yaml (Keycloak + PostgreSQL)"
        echo -e "  • kafka-service/docker-compose.yaml (Kafka + Kafka UI + MinIO)"
        echo -e "  • whatsapp-brain-service/docker-compose.yaml"
    else
        print_error "Failed to start infrastructure services"
        exit 1
    fi

    echo -e "\n${BLUE}Waiting for infrastructure services to initialize...${NC}"
    sleep 10
else
    print_warning "Skipping infrastructure startup. Start it manually with: docker compose up -d"
fi

# Build and start services only if requested
if [ "$START_SERVICES" = true ]; then
    echo -e "\n${BLUE}Building selected modules...${NC}"
    for SERVICE in "${ACTIVE_SERVICES[@]}"; do
        SERVICE_BUILD_LOG="$LOG_DIR/build-${SERVICE}.log"
        echo -e "\n${BLUE}Building ${SERVICE}...${NC}"

        cd "$PROJECT_ROOT/$SERVICE" || {
            print_error "Service directory not found: $PROJECT_ROOT/$SERVICE"
            exit 1
        }

        export JAVA_HOME="/usr/lib/jvm/temurin-21-jdk"
        ./mvnw clean package -DskipTests -q > "$SERVICE_BUILD_LOG" 2>&1

        if [ $? -eq 0 ]; then
            print_status "${SERVICE} built successfully"
            print_warning "Build logs: $SERVICE_BUILD_LOG"
        else
            print_error "${SERVICE} build failed. Check: $SERVICE_BUILD_LOG"
            exit 1
        fi
    done

    echo -e "\n${BLUE}Starting microservices...${NC}"
    for i in "${!ACTIVE_SERVICES[@]}"; do
        SERVICE="${ACTIVE_SERVICES[$i]}"
        PORT="${ACTIVE_PORTS[$i]}"

        echo -e "\n${BLUE}Starting ${SERVICE} on port ${PORT}...${NC}"

        cd "$PROJECT_ROOT/$SERVICE" || {
            print_error "Service directory not found: $PROJECT_ROOT/$SERVICE"
            exit 1
        }

        export JAVA_HOME="/usr/lib/jvm/temurin-21-jdk"
        STARTUP_LOG="$LOG_DIR/startup-${SERVICE}.log"

        if [ "$DEBUG_MODE" = true ]; then
            DEBUG_PORT="${ACTIVE_DEBUG_PORTS[$i]}"
            nohup ./mvnw spring-boot:run "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=${DEBUG_SUSPEND},address=*:${DEBUG_PORT}" > "$STARTUP_LOG" 2>&1 &
        else
            nohup ./mvnw spring-boot:run > "$STARTUP_LOG" 2>&1 &
        fi

        PID=$!
        echo "$PID" >> "$PID_FILE"

        print_status "${SERVICE} started (PID: $PID)"
        print_warning "Startup logs: tail -f $STARTUP_LOG"
        print_warning "Application logs may be available under: $PROJECT_ROOT/$SERVICE/logs/"
        if [ "$DEBUG_MODE" = true ]; then
            print_warning "Remote debug: attach IntelliJ to localhost:${DEBUG_PORT}"
        fi

        sleep 5
    done
else
    print_warning "Skipping service startup. Run services from IntelliJ for local debugging."
fi

echo -e "\n${BLUE}========================================${NC}"
echo -e "${GREEN}Requested startup actions completed!${NC}"
echo -e "${BLUE}========================================${NC}"

if [ "$START_SERVICES" = true ]; then
    echo -e "\nServices running on:"
    for i in "${!ACTIVE_SERVICES[@]}"; do
        echo -e "  ${GREEN}${ACTIVE_SERVICES[$i]}${NC}: http://localhost:${ACTIVE_PORTS[$i]}"
    done
fi

if [ "$START_INFRA" = true ]; then
    echo -e "\nInfrastructure endpoints:"
    echo -e "  ${GREEN}Keycloak${NC}: http://localhost:8180"
    echo -e "  ${GREEN}Kafka UI${NC}: http://localhost:8989/kafka-ui"
    echo -e "  ${GREEN}MinIO API${NC}: http://localhost:9005"
    echo -e "  ${GREEN}MinIO Console${NC}: http://localhost:9006"
fi

if [ "$START_SERVICES" = true ]; then
    echo -e "\n${YELLOW}To view logs:${NC}"
    for SERVICE in "${ACTIVE_SERVICES[@]}"; do
        echo -e "  tail -f $LOG_DIR/build-${SERVICE}.log"
        echo -e "  tail -f $LOG_DIR/startup-${SERVICE}.log"
    done
fi

if [ "$DEBUG_MODE" = true ] && [ "$START_SERVICES" = true ]; then
    echo -e "\n${YELLOW}Remote debug ports:${NC}"
    for i in "${!ACTIVE_SERVICES[@]}"; do
        echo -e "  ${GREEN}${ACTIVE_SERVICES[$i]}${NC}: localhost:${ACTIVE_DEBUG_PORTS[$i]}"
    done
fi

echo -e "\n${YELLOW}To stop script-started services:${NC}"
echo -e "  bash $PROJECT_ROOT/stop-services.sh"

echo -e "\n${YELLOW}To check service status:${NC}"
echo -e "  bash $PROJECT_ROOT/check-services.sh"

echo -e "\n${YELLOW}Examples:${NC}"
echo -e "  Start only infra:     bash $PROJECT_ROOT/start-services.sh --start-infra"
echo -e "  Start only services:  bash $PROJECT_ROOT/start-services.sh --start-services --api-gateway"
echo -e "  Start both:           bash $PROJECT_ROOT/start-services.sh --all"
echo -e "  IntelliJ workflow:    docker compose up -d   # then run services from IntelliJ"
