#!/usr/bin/env bash
# ------------------------------------------------------------------
# CreatorConnect — Backend Startup Script
#
# Starts every microservice in dependency order and waits for each
# to become healthy before launching the next.
#
# Prerequisites:
#   - Java 17+ on PATH
#   - MySQL running on localhost:3307 (or set MYSQL_URL / MYSQL_PASSWORD)
#   - APP_JWT_SECRET set in the environment (all services share the
#     same HMAC key so tokens issued by auth-service are accepted
#     by every other service).
#
# Usage:
#   bash start-backend.sh          # start all services
#   bash start-backend.sh --prod   # build JARs first, then start
# ------------------------------------------------------------------

set -euo pipefail

# ---------- tunables ----------
EUREKA_PORT=8761
GATEWAY_PORT=8080
HEALTH_TIMEOUT=60          # seconds to wait for each service
STARTUP_DELAY=5            # seconds to pause between launches

# ---------- colours ----------
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()  { echo -e "${GREEN}[OK]${NC}   $1"; }
warn()  { echo -e "${YELLOW}[WAIT]${NC} $1"; }
fail()  { echo -e "${RED}[FAIL]${NC} $1"; }

# ---------- health check ----------
wait_for_health() {
  local name="$1" port="$2" elapsed=0
  warn "Waiting for $name on port $port ..."
  while ! curl -sf "http://localhost:$port/actuator/health" >/dev/null 2>&1; do
    sleep 1
    elapsed=$((elapsed + 1))
    if [ "$elapsed" -ge "$HEALTH_TIMEOUT" ]; then
      fail "$name did not become healthy within ${HEALTH_TIMEOUT}s"
      return 1
    fi
  done
  info "$name is healthy (port $port)"
}

# ---------- optional: build first ----------
if [[ "${1:-}" == "--prod" ]]; then
  echo "Building all backend modules ..."
  cd backend
  mvn clean package -DskipTests -q
  cd ..
  info "Build complete"
fi

# ---------- launch a Spring Boot app ----------
launch() {
  local name="$1" module="$2" port="$3"
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  info "Starting $name (port $port) ..."
  cd "backend/$module"
  # Run in background; stdout/stderr go to a log file
  LOG="../${module}.log"
  java -jar target/*.jar > "$LOG" 2>&1 &
  echo $! > "../${module}.pid"
  cd ../..
  wait_for_health "$name" "$port"
}

echo ""
echo "╔═══════════════════════════════════════════════════════╗"
echo "║        CreatorConnect — Backend Startup              ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo ""

# 1) Service Registry (Eureka)
launch "Service Registry" "service-registry" "$EUREKA_PORT"
sleep "$STARTUP_DELAY"

# 2) API Gateway
launch "API Gateway" "api-gateway" "$GATEWAY_PORT"
sleep "$STARTUP_DELAY"

# 3) Auth Service
launch "Auth Service" "auth-service" 8081
sleep "$STARTUP_DELAY"

# 4) Profile Service
launch "Profile Service" "profile-service" 8082
sleep "$STARTUP_DELAY"

# 5) Project Service
launch "Project Service" "project-service" 8083
sleep "$STARTUP_DELAY"

# 6) Hiring Service
launch "Hiring Service" "hiring-service" 8084
sleep "$STARTUP_DELAY"

# 7) AI Service
launch "AI Service" "ai-service" 8085

echo ""
echo "╔═══════════════════════════════════════════════════════╗"
echo "║  All services are running!                           ║"
echo "║                                                     ║"
echo "║  Service Registry :8761                             ║"
echo "║  API Gateway      :8080                             ║"
echo "║  Auth Service     :8081                             ║"
echo "║  Profile Service  :8082                             ║"
echo "║  Project Service  :8083                             ║"
echo "║  Hiring Service   :8084                             ║"
echo "║  AI Service       :8085                             ║"
echo "║                                                     ║"
echo "║  Now start the frontend:                            ║"
echo "║    cd frontend && npm run dev                       ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo ""

# Quick smoke test
echo "Smoke-testing endpoints through the gateway ..."
for path in "/auth" "/profile/me" "/projects" "/hiring/notifications/unread-count" "/ai/status"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$GATEWAY_PORT$path" 2>/dev/null || echo "000")
  if [[ "$code" == "401" || "$code" == "200" ]]; then
    info "GET $path → $code"
  else
    warn "GET $path → $code (may need auth token)"
  fi
done
echo ""
