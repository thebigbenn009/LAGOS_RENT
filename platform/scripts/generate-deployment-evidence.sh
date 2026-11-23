#!/usr/bin/env bash
set -euo pipefail

ENVIRONMENT="${1:-dev}"
IMAGE_PREFIX="${2:-lagos-rent}"
CONFIG_REPO_DIR="${CONFIG_REPO_DIR:-../lagos_rent_config}"
OUTPUT_DIR="${OUTPUT_DIR:-target/deployment-evidence}"

mkdir -p "$OUTPUT_DIR"

APP_COMMIT_FULL=$(git rev-parse HEAD)
APP_COMMIT_SHORT=$(git rev-parse --short HEAD)
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

# Resolve lagos_rent_config Git revision if present alongside the monorepo
if [ -d "$CONFIG_REPO_DIR/.git" ]; then
  CONFIG_COMMIT_SHORT=$(git -C "$CONFIG_REPO_DIR" rev-parse --short HEAD)
  CONFIG_COMMIT_FULL=$(git -C "$CONFIG_REPO_DIR" rev-parse HEAD)
else
  CONFIG_COMMIT_SHORT="${CONFIG_REPO_SHA:-external}"
  CONFIG_COMMIT_FULL="${CONFIG_REPO_SHA:-external}"
fi

SERVICES=(
  "config-server"
  "discovery-service"
  "api-gateway"
  "account-service"
  "listing-service"
  "rental-service"
  "payment-service"
  "search-service"
  "notification-service"
)

MD_FILE="$OUTPUT_DIR/deployment-evidence.md"
JSON_FILE="$OUTPUT_DIR/deployment-evidence.json"

cat <<HEADER > "$MD_FILE"
# Deployment Evidence Manifest — Environment: \`${ENVIRONMENT}\`

- **Generated At (UTC):** \`${TIMESTAMP}\`
- **Application Commit:** \`${APP_COMMIT_SHORT}\` (\`${APP_COMMIT_FULL}\`)
- **Config Repo Revision (\`lagos_rent_config\`):** \`${CONFIG_COMMIT_SHORT}\` (\`${CONFIG_COMMIT_FULL}\`)

| Service | Image Tag | Immutable Image Digest / ID | Latest Flyway Migration |
|---|---|---|---|
HEADER

echo "{" > "$JSON_FILE"
echo "  \"environment\": \"${ENVIRONMENT}\"," >> "$JSON_FILE"
echo "  \"generatedAt\": \"${TIMESTAMP}\"," >> "$JSON_FILE"
echo "  \"appCommitSha\": \"${APP_COMMIT_FULL}\"," >> "$JSON_FILE"
echo "  \"configCommitSha\": \"${CONFIG_COMMIT_FULL}\"," >> "$JSON_FILE"
echo "  \"services\": [" >> "$JSON_FILE"

for i in "${!SERVICES[@]}"; do
  svc="${SERVICES[$i]}"
  TAG="${IMAGE_PREFIX}/${svc}:sha-${APP_COMMIT_SHORT}"
  FALLBACK_TAG="${IMAGE_PREFIX}/${svc}:latest"

  # 1. Resolve image @sha256 digest (or local image ID if not pushed to remote registry yet)
  DIGEST=$(docker inspect --format='{{if .RepoDigests}}{{index .RepoDigests 0}}{{else}}{{.Id}}{{end}}' "$TAG" 2>/dev/null \
        || docker inspect --format='{{if .RepoDigests}}{{index .RepoDigests 0}}{{else}}{{.Id}}{{end}}' "$FALLBACK_TAG" 2>/dev/null \
        || echo "not-built-locally")

  # 2. Resolve latest Flyway SQL migration version for this service
  MIGRATION_DIR="services/${svc}/src/main/resources/db/migration"
  if [ -d "$MIGRATION_DIR" ]; then
    LATEST_SQL=$(ls -1 "$MIGRATION_DIR"/V*.sql 2>/dev/null | sort -V | tail -n 1 || true)
    if [ -n "$LATEST_SQL" ]; then
      FLYWAY_VER=$(basename "$LATEST_SQL")
    else
      FLYWAY_VER="none"
    fi
  else
    FLYWAY_VER="N/A (stateless)"
  fi

  echo "| \`${svc}\` | \`sha-${APP_COMMIT_SHORT}\` | \`${DIGEST}\` | \`${FLYWAY_VER}\` |" >> "$MD_FILE"

  COMMA=","
  if [ "$i" -eq $(( ${#SERVICES[@]} - 1 )) ]; then
    COMMA=""
  fi
  cat <<ITEM >> "$JSON_FILE"
    {
      "service": "${svc}",
      "imageTag": "${TAG}",
      "imageDigest": "${DIGEST}",
      "configRevision": "${CONFIG_COMMIT_SHORT}",
      "flywayMigration": "${FLYWAY_VER}"
    }${COMMA}
ITEM
done

echo "  ]" >> "$JSON_FILE"
echo "}" >> "$JSON_FILE"

cat "$MD_FILE"
