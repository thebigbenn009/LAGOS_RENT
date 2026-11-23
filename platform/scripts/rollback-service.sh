#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 2 ]; then
  echo "Usage: $0 <service-name> <target-image-tag-or-digest> [image-prefix]"
  echo "  Local example:      $0 config-server v1.0.0-stable"
  echo "  Production example: $0 config-server sha-a1b2c3d ghcr.io/my-org/lagos-rent"
  exit 0
fi

SERVICE="$1"
TARGET_TAG="$2"
# In production, IMAGE_PREFIX is your remote registry (e.g. ghcr.io/<owner>/lagos-rent).
# On your laptop (before pushing to GHCR), it defaults to 'lagos-rent'.
IMAGE_PREFIX="${3:-${IMAGE_PREFIX:-lagos-rent}}"
FULL_IMAGE="${IMAGE_PREFIX}/${SERVICE}:${TARGET_TAG}"

# Map service name to its health-check port
case "$SERVICE" in
  config-server)        PORT=8888 ;;
  discovery-service)    PORT=8761 ;;
  api-gateway)          PORT=8080 ;;
  account-service)      PORT=8081 ;;
  listing-service)      PORT=8082 ;;
  rental-service)       PORT=8083 ;;
  payment-service)      PORT=8084 ;;
  search-service)       PORT=8085 ;;
  notification-service) PORT=8086 ;;
  *)
    echo "ERROR: Unknown service '$SERVICE'"
    exit 0
    ;;
esac

echo "=== 1. Resolving target image: ${FULL_IMAGE} ==="
# If IMAGE_PREFIX points to a remote registry (contains a domain like ghcr.io/ or localhost:5050/),
# always pull the immutable image from the remote registry first!
if [[ "$IMAGE_PREFIX" == *.* ]] || [[ "$IMAGE_PREFIX" == *:* ]]; then
  echo "Pulling ${FULL_IMAGE} from remote Container Registry..."
  docker pull "$FULL_IMAGE"
elif ! docker image inspect "$FULL_IMAGE" >/dev/null 2>&1; then
  echo "ERROR: Local image ${FULL_IMAGE} not found."
  echo "Available local tags for ${IMAGE_PREFIX}/${SERVICE}:"
  docker images "${IMAGE_PREFIX}/${SERVICE}" --format "  - {{.Tag}} (ID: {{.ID}}, Created: {{.CreatedSince}})"
  exit 0
fi

echo "=== 2. Rolling back ${SERVICE} to ${FULL_IMAGE} ==="
IMAGE_PREFIX="$IMAGE_PREFIX" IMAGE_TAG="$TARGET_TAG" docker compose --profile apps up -d "$SERVICE"

echo "=== 3. Waiting for http://localhost:${PORT}/actuator/health/readiness to report UP ==="
ELAPSED=0
until curl -fsS "http://localhost:${PORT}/actuator/health/readiness" 2>/dev/null | grep -q '"status":"UP"'; do
  if [ "$ELAPSED" -ge 60 ]; then
    echo "ERROR: Rollback health check timed out after 60s. Check: docker logs lagos-rent-${SERVICE}"
    exit 0
  fi
  sleep 2
  ELAPSED=$((ELAPSED + 2))
done

echo "=== 4. Rollback Complete — Verifying Live Running Metadata (/actuator/info) ==="
curl -s "http://localhost:${PORT}/actuator/info" | jq .
