#!/usr/bin/env bash
# ==============================================================================
# LAGOS RENT PLATFORM — MICROSERVICES ORCHESTRATION SCRIPT
#
# Launches all 9 microservices in strict dependency order:
#   1. Config Server (Port 8888) -> Wait until Actuator reports UP
#   2. Discovery Service (Eureka) (Port 8761) -> Wait until Actuator reports UP
#   3. API Gateway (Port 8080) -> Wait until Actuator reports UP
#   4. Domain Microservices:
#      - Account Service (Port 8081)
#      - Listing Service (Port 8082)
#      - Rental Service (Port 8083)
#      - Payment Service (Port 8084)
#      - Search Service (Port 8085)
#      - Notification Service (Port 8086)
# ==============================================================================

set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

LOG_DIR="$SCRIPT_DIR/logs"
PID_DIR="$LOG_DIR/pids"
mkdir -p "$LOG_DIR" "$PID_DIR"

# Color constants
C_RESET='\033[0m'
C_BOLD='\033[1m'
C_GREEN='\033[32m'
C_YELLOW='\033[33m'
C_CYAN='\033[36m'
C_RED='\033[31m'
C_DIM='\033[2m'

# Service definitions: "name:port:relative_module_path"
ORDERED_SERVICES=(
  "config-server:8888:services/config-server"
  "discovery-service:8761:services/discovery-service"
  "api-gateway:8080:services/api-gateway"
  "account-service:8081:services/account-service"
  "listing-service:8082:services/listing-service"
  "rental-service:8083:services/rental-service"
  "payment-service:8084:services/payment-service"
  "search-service:8085:services/search-service"
  "notification-service:8086:services/notification-service"
)

log_info() {
  echo -e "${C_CYAN}[INFO]${C_RESET} $1"
}

log_success() {
  echo -e "${C_GREEN}[SUCCESS]${C_RESET} $1"
}

log_warn() {
  echo -e "${C_YELLOW}[WARN]${C_RESET} $1"
}

log_error() {
  echo -e "${C_RED}[ERROR]${C_RESET} $1"
}

# ------------------------------------------------------------------------------
# Load and Export .env File (Inherited by all background microservices)
# ------------------------------------------------------------------------------
load_dotenv() {
  local env_file="$SCRIPT_DIR/.env"
  if [ -f "$env_file" ]; then
    log_info "Loading environment variables from .env..."
    set -a
    # shellcheck disable=SC1090
    source "$env_file"
    set +a
    log_success "Environment variables exported (inherited by all child microservices)."
  else
    log_warn "No .env file found at ${env_file}. Proceeding with system environment."
  fi
}

# ------------------------------------------------------------------------------
# Check if a service's /actuator/health is UP
# ------------------------------------------------------------------------------
is_service_up() {
  local port="$1"
  local resp
  resp=$(curl -s -m 2 "http://localhost:${port}/actuator/health" 2>/dev/null || echo "")
  if [ -n "$resp" ]; then
    if command -v jq >/dev/null 2>&1; then
      local status
      status=$(echo "$resp" | jq -r '.status // empty' 2>/dev/null)
      if [ "$status" = "UP" ]; then
        return 0
      fi
    fi
    if echo "$resp" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
      return 0
    fi
  fi
  return 1
}

# ------------------------------------------------------------------------------
# Get PID listening on a specific TCP port
# ------------------------------------------------------------------------------
get_pid_on_port() {
  local port="$1"
  lsof -tiTCP:"${port}" -sTCP:LISTEN 2>/dev/null | head -n 1
}

# ------------------------------------------------------------------------------
# Pre-flight infrastructure check (PostgreSQL, RabbitMQ, Keycloak)
# ------------------------------------------------------------------------------
check_infrastructure() {
  echo -e "\n${C_BOLD}Checking Core Infrastructure Containers...${C_RESET}"
  local infra_ok=true

  # PostgreSQL :5432
  if nc -z localhost 5432 2>/dev/null || lsof -i :5432 >/dev/null 2>&1; then
    echo -e "  ✓ PostgreSQL (5432) ${C_GREEN}Available${C_RESET}"
  else
    echo -e "  ✗ PostgreSQL (5432) ${C_RED}Not reachable${C_RESET}"
    infra_ok=false
  fi

  # RabbitMQ :5672
  if nc -z localhost 5672 2>/dev/null || lsof -i :5672 >/dev/null 2>&1; then
    echo -e "  ✓ RabbitMQ (5672)   ${C_GREEN}Available${C_RESET}"
  else
    echo -e "  ✗ RabbitMQ (5672)   ${C_RED}Not reachable${C_RESET}"
    infra_ok=false
  fi

  # Keycloak :8180
  if nc -z localhost 8180 2>/dev/null || lsof -i :8180 >/dev/null 2>&1; then
    echo -e "  ✓ Keycloak (8180)   ${C_GREEN}Available${C_RESET}"
  else
    echo -e "  ✗ Keycloak (8180)   ${C_RED}Not reachable${C_RESET}"
    infra_ok=false
  fi

  if [ "$infra_ok" = false ]; then
    log_warn "One or more infrastructure services are not running."
    log_warn "If using docker-compose, start them with: docker compose up -d"
    echo -n "Do you want to proceed anyway? (y/N): "
    read -r answer
    if [[ ! "$answer" =~ ^[Yy]$ ]]; then
      log_info "Startup aborted. Please start infrastructure first."
      exit 1
    fi
  fi
}

# ------------------------------------------------------------------------------
# Launch and poll a single service
# ------------------------------------------------------------------------------
start_service() {
  local service_name="$1"
  local port="$2"
  local module_path="$3"
  local timeout="${4:-90}"

  local log_file="$LOG_DIR/${service_name}.log"
  local pid_file="$PID_DIR/${service_name}.pid"

  echo -e "\n------------------------------------------------------------------------"
  echo -e "${C_BOLD}Starting [${service_name}] on Port ${port}...${C_RESET}"
  echo -e "  Log output: ${C_DIM}${log_file}${C_RESET}"

  # 1. Check if already UP
  if is_service_up "$port"; then
    local existing_pid
    existing_pid=$(get_pid_on_port "$port")
    echo "$existing_pid" > "$pid_file"
    log_success "${service_name} is ALREADY running and healthy (PID: ${existing_pid}, Port: ${port})"
    return 0
  fi

  # 2. Check if port is occupied by an unresponsive process
  local conflict_pid
  conflict_pid=$(get_pid_on_port "$port")
  if [ -n "$conflict_pid" ]; then
    log_warn "Port ${port} is occupied by PID ${conflict_pid} (not responding to /actuator/health)."
    echo -n "Kill unresponsive process ${conflict_pid} and restart? (Y/n): "
    read -r answer
    if [[ ! "$answer" =~ ^[Nn]$ ]]; then
      kill -9 "$conflict_pid" 2>/dev/null || true
      sleep 2
    else
      log_error "Cannot start ${service_name} due to port conflict on ${port}."
      return 1
    fi
  fi

  # 3. Launch with Maven
  nohup mvn -pl "$module_path" spring-boot:run > "$log_file" 2>&1 &
  local mvn_pid=$!
  echo "$mvn_pid" > "$pid_file"

  # 4. Wait loop for health probe
  local elapsed=0
  local spinner=( '⠋' '⠙' '⠹' '⠸' '⠼' '⠴' '⠦' '⠧' '⠇' '⠏' )
  local spin_idx=0

  while [ $elapsed -lt "$timeout" ]; do
    if is_service_up "$port"; then
      local actual_pid
      actual_pid=$(get_pid_on_port "$port")
      if [ -n "$actual_pid" ]; then
        echo "$actual_pid" > "$pid_file"
      fi
      echo -e "\r  ${C_GREEN}✓${C_RESET} ${service_name} is UP & Healthy in ${elapsed}s! (Port: ${port}, PID: ${actual_pid:-$mvn_pid})    "
      return 0
    fi

    # Check if Maven died prematurely
    if ! kill -0 "$mvn_pid" 2>/dev/null && [ -z "$(get_pid_on_port "$port")" ]; then
      echo -e "\r  ${C_RED}✗${C_RESET} ${service_name} process terminated unexpectedly!                "
      log_error "Recent log output from ${log_file}:"
      echo -e "${C_DIM}"
      tail -n 25 "$log_file"
      echo -e "${C_RESET}"
      return 1
    fi

    local spin_char="${spinner[$spin_idx]}"
    echo -ne "\r  ${C_YELLOW}${spin_char}${C_RESET} Waiting for http://localhost:${port}/actuator/health (${elapsed}s / ${timeout}s)..."
    spin_idx=$(( (spin_idx + 1) % 10 ))
    sleep 2
    elapsed=$((elapsed + 2))
  done

  echo -e "\r  ${C_RED}✗${C_RESET} Timeout waiting for ${service_name} to become healthy (${timeout}s).    "
  log_error "Check log file for root cause: ${log_file}"
  echo -e "${C_DIM}"
  tail -n 25 "$log_file"
  echo -e "${C_RESET}"
  return 1
}

# ------------------------------------------------------------------------------
# Start Core Services Only (Config Server -> Discovery -> API Gateway)
# ------------------------------------------------------------------------------
start_core() {
  echo -e "========================================================================"
  echo -e "${C_BOLD}${C_CYAN}         LAGOS RENT — CORE PLATFORM SERVICES (3 SERVICES)       ${C_RESET}"
  echo -e "========================================================================"

  check_infrastructure
  load_dotenv

  log_info "Pre-compiling modules once with Maven..."
  mvn test-compile -DskipTests -q || {
    log_error "Maven compilation failed. Fix compilation errors before launching."
    exit 1
  }

  # Step 1: Config Server
  start_service "config-server" 8888 "services/config-server" 90 || exit 1

  # Step 2: Discovery Service (Eureka)
  start_service "discovery-service" 8761 "services/discovery-service" 90 || exit 1

  # Step 3: API Gateway
  start_service "api-gateway" 8080 "services/api-gateway" 90 || exit 1

  echo -e "\n========================================================================"
  echo -e "${C_BOLD}${C_GREEN}   CORE SERVICES (CONFIG, DISCOVERY, GATEWAY) ARE UP AND HEALTHY!   ${C_RESET}"
  echo -e "========================================================================"
  show_status
}

# ------------------------------------------------------------------------------
# Stop Core Services Only
# ------------------------------------------------------------------------------
stop_core() {
  echo -e "\n${C_BOLD}Stopping Core Platform Services...${C_RESET}"
  stop_single_service "api-gateway"
  stop_single_service "discovery-service"
  stop_single_service "config-server"
  log_success "Core platform services stopped."
}

# ------------------------------------------------------------------------------
# Ensure Core Dependencies are UP before launching a domain service
# ------------------------------------------------------------------------------
ensure_core_running() {
  local missing=()
  is_service_up 8888 || missing+=("config-server:8888:services/config-server")
  is_service_up 8761 || missing+=("discovery-service:8761:services/discovery-service")
  is_service_up 8080 || missing+=("api-gateway:8080:services/api-gateway")

  if [ ${#missing[@]} -gt 0 ]; then
    log_warn "This service depends on the core platform (Config, Discovery, Gateway)."
    log_info "The following core dependencies are not running yet: ${missing[*]}"
    echo -n "Automatically start required core dependencies first? (Y/n): "
    read -r answer
    if [[ ! "$answer" =~ ^[Nn]$ ]]; then
      for item in "${missing[@]}"; do
        IFS=":" read -r c_name c_port c_mod <<< "$item"
        start_service "$c_name" "$c_port" "$c_mod" 90 || exit 1
      done
    else
      log_warn "Proceeding without verifying core dependencies..."
    fi
  fi
}

# ------------------------------------------------------------------------------
# Start All Services
# ------------------------------------------------------------------------------
start_all() {
  echo -e "========================================================================"
  echo -e "${C_BOLD}${C_CYAN}         LAGOS RENT — 9-SERVICE PRODUCTION ORCHESTRATOR         ${C_RESET}"
  echo -e "========================================================================"

  check_infrastructure
  load_dotenv

  log_info "Pre-compiling modules once with Maven to ensure rapid boot..."
  mvn test-compile -DskipTests -q || {
    log_error "Maven compilation failed. Fix compilation errors before launching."
    exit 1
  }
  log_success "Build clean. Launching services in strict dependency order."

  # Step 1: Config Server
  start_service "config-server" 8888 "services/config-server" 90 || exit 1

  # Step 2: Discovery Service (Eureka)
  start_service "discovery-service" 8761 "services/discovery-service" 90 || exit 1

  # Step 3: API Gateway
  start_service "api-gateway" 8080 "services/api-gateway" 90 || exit 1

  # Step 4: Domain Services (in order)
  echo -e "\n${C_BOLD}Foundational infrastructure is UP. Starting Domain Services...${C_RESET}"
  start_service "account-service" 8081 "services/account-service" 90 || exit 1
  start_service "listing-service" 8082 "services/listing-service" 90 || exit 1
  start_service "rental-service" 8083 "services/rental-service" 90 || exit 1
  start_service "payment-service" 8084 "services/payment-service" 90 || exit 1
  start_service "search-service" 8085 "services/search-service" 90 || exit 1
  start_service "notification-service" 8086 "services/notification-service" 90 || exit 1

  echo -e "\n========================================================================"
  echo -e "${C_BOLD}${C_GREEN}         ALL 9 LAGOS RENT SERVICES ARE UP AND HEALTHY!          ${C_RESET}"
  echo -e "========================================================================"
  show_status
}

# ------------------------------------------------------------------------------
# Stop All Services (in reverse order)
# ------------------------------------------------------------------------------
stop_all() {
  echo -e "\n${C_BOLD}Stopping Lagos Rent Services...${C_RESET}"

  # Iterate backwards
  for ((i=${#ORDERED_SERVICES[@]}-1; i>=0; i--)); do
    IFS=":" read -r name port _ <<< "${ORDERED_SERVICES[i]}"
    local pid
    pid=$(get_pid_on_port "$port")

    if [ -n "$pid" ]; then
      echo -n "Stopping ${name} on port ${port} (PID: ${pid})... "
      kill "$pid" 2>/dev/null || true

      # Grace period
      local waited=0
      while kill -0 "$pid" 2>/dev/null && [ $waited -lt 10 ]; do
        sleep 1
        waited=$((waited + 1))
      done

      # Force kill if still lingering
      if kill -0 "$pid" 2>/dev/null; then
        kill -9 "$pid" 2>/dev/null || true
      fi
      rm -f "$PID_DIR/${name}.pid"
      echo -e "${C_GREEN}Stopped${C_RESET}"
    else
      echo -e "${C_DIM}${name} on port ${port} is not running.${C_RESET}"
      rm -f "$PID_DIR/${name}.pid"
    fi
  done
  log_success "All services stopped."
}

# ------------------------------------------------------------------------------
# Status display
# ------------------------------------------------------------------------------
show_status() {
  echo ""
  printf "${C_BOLD}%-22s %-6s %-16s %-8s %s${C_RESET}\n" "SERVICE NAME" "PORT" "STATUS" "PID" "LOG FILE"
  echo "----------------------------------------------------------------------------------------"

  for entry in "${ORDERED_SERVICES[@]}"; do
    IFS=":" read -r name port _ <<< "$entry"
    local pid
    pid=$(get_pid_on_port "$port")
    local status_str
    if is_service_up "$port"; then
      status_str="${C_GREEN}UP (Healthy)${C_RESET}"
    elif [ -n "$pid" ]; then
      status_str="${C_YELLOW}STARTING${C_RESET}"
    else
      status_str="${C_RED}DOWN${C_RESET}"
    fi

    printf "%-22s %-6s %-22b %-8s %s\n" \
      "$name" "$port" "$status_str" "${pid:--}" "logs/${name}.log"
  done
  echo ""
}

# ------------------------------------------------------------------------------
# Stop a Single Service
# ------------------------------------------------------------------------------
stop_single_service() {
  local target="$1"
  local found=false

  for entry in "${ORDERED_SERVICES[@]}"; do
    IFS=":" read -r name port _ <<< "$entry"
    if [ "$name" = "$target" ] || [ "$name" = "${target}-service" ]; then
      found=true
      local pid
      pid=$(get_pid_on_port "$port")
      if [ -n "$pid" ]; then
        echo -n "Stopping ${name} on port ${port} (PID: ${pid})... "
        kill "$pid" 2>/dev/null || true
        local waited=0
        while kill -0 "$pid" 2>/dev/null && [ $waited -lt 10 ]; do
          sleep 1
          waited=$((waited + 1))
        done
        if kill -0 "$pid" 2>/dev/null; then
          kill -9 "$pid" 2>/dev/null || true
        fi
        rm -f "$PID_DIR/${name}.pid"
        echo -e "${C_GREEN}Stopped${C_RESET}"
      else
        echo -e "${C_DIM}${name} on port ${port} is not running.${C_RESET}"
        rm -f "$PID_DIR/${name}.pid"
      fi
      break
    fi
  done

  if [ "$found" = false ]; then
    log_error "Unknown service name: ${target}"
    echo "Available services: config-server, discovery-service, api-gateway, account-service, listing-service, rental-service, payment-service, search-service, notification-service"
    return 1
  fi
}

# ------------------------------------------------------------------------------
# Start a Single Service
# ------------------------------------------------------------------------------
start_single_service() {
  local target="$1"

  if [ "$target" = "core" ] || [ "$target" = "base" ]; then
    start_core
    return 0
  fi

  if [ "$target" = "all" ]; then
    start_all
    return 0
  fi

  load_dotenv

  local found=false

  for entry in "${ORDERED_SERVICES[@]}"; do
    IFS=":" read -r name port module <<< "$entry"
    if [ "$name" = "$target" ] || [ "$name" = "${target}-service" ]; then
      found=true
      # If starting a domain service, ensure core dependencies are up
      if [ "$name" != "config-server" ] && [ "$name" != "discovery-service" ] && [ "$name" != "api-gateway" ]; then
        ensure_core_running
      fi
      start_service "$name" "$port" "$module" 90
      break
    fi
  done

  if [ "$found" = false ]; then
    log_error "Unknown service name: ${target}"
    echo "Available services:"
    echo "  Core:   core, config-server, discovery-service, api-gateway"
    echo "  Domain: account-service, listing-service, rental-service, payment-service, search-service, notification-service"
    return 1
  fi
}

# ------------------------------------------------------------------------------
# CLI Dispatcher
# ------------------------------------------------------------------------------
case "${1:-start}" in
  core|base)
    start_core
    ;;
  all)
    start_all
    ;;
  start)
    if [ "$2" = "core" ] || [ "$2" = "base" ]; then
      start_core
    elif [ "$2" = "all" ] || [ -z "$2" ]; then
      start_all
    else
      start_single_service "$2"
    fi
    ;;
  stop)
    if [ "$2" = "core" ] || [ "$2" = "base" ]; then
      stop_core
    elif [ "$2" = "all" ] || [ -z "$2" ]; then
      stop_all
    else
      stop_single_service "$2"
    fi
    ;;
  restart)
    if [ "$2" = "core" ] || [ "$2" = "base" ]; then
      stop_core
      sleep 2
      start_core
    elif [ "$2" = "all" ] || [ -z "$2" ]; then
      stop_all
      sleep 2
      start_all
    else
      stop_single_service "$2"
      sleep 2
      start_single_service "$2"
    fi
    ;;
  status)
    show_status
    ;;
  logs)
    if [ -n "$2" ]; then
      tail -f "$LOG_DIR/${2}.log"
    else
      log_info "Available logs in $LOG_DIR/:"
      ls -lh "$LOG_DIR"/*.log 2>/dev/null || echo "No logs found."
    fi
    ;;
  *)
    # Check if first argument is a service name directly (e.g. ./run-all-services.sh rental-service)
    for entry in "${ORDERED_SERVICES[@]}"; do
      IFS=":" read -r name _ _ <<< "$entry"
      if [ "$name" = "$1" ] || [ "$name" = "${1}-service" ]; then
        start_single_service "$1"
        exit 0
      fi
    done

    echo "Usage: $0 [command] [service-name]"
    echo ""
    echo "Commands:"
    echo "  $0 core                         # Start ONLY the 3 core services (Config, Discovery, Gateway)"
    echo "  $0 start <service-name>         # Start a single service (e.g., rental-service)"
    echo "  $0 <service-name>               # Shorthand to start a service (e.g., $0 rental-service)"
    echo "  $0 all (or $0 start)            # Start all 9 services in order"
    echo "  $0 stop core                    # Stop ONLY the 3 core services"
    echo "  $0 stop <service-name>          # Stop a single service"
    echo "  $0 stop                         # Stop all services"
    echo "  $0 restart <service-name>       # Restart a single service"
    echo "  $0 restart core                 # Restart the 3 core services"
    echo "  $0 status                       # View status of all services"
    echo "  $0 logs <service-name>          # Tail logs for a specific service"
    exit 1
    ;;
esac
