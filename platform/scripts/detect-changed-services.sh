#!/usr/bin/env bash
set -euo pipefail

# Base git ref to compare against (e.g. "origin/main" in CI, or "HEAD" locally)
BASE_REF="${1:-HEAD}"

ALL_SERVICES=(
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

# Gather changed files (committed changes since BASE_REF + any uncommitted local edits/untracked files)
CHANGED_FILES=$(
  {
    git diff --name-only "$BASE_REF" 2>/dev/null || true
    git status --porcelain | awk '{print $NF}'
  } | sort -u
)

BuildAll=false

# 1. If root pom.xml, contracts/, or CI workflows changed, test all 9 services
if echo "$CHANGED_FILES" | grep -qE '^(pom\.xml|contracts/|\.github/workflows/)'; then
  BuildAll=true
fi

SELECTED_SERVICES=()

if [ "$BuildAll" = true ]; then
  SELECTED_SERVICES=("${ALL_SERVICES[@]}")
else
  for svc in "${ALL_SERVICES[@]}"; do
    if echo "$CHANGED_FILES" | grep -q "^services/${svc}/"; then
      SELECTED_SERVICES+=("$svc")
    fi
  done
fi

# Format 1: Comma-separated Maven -pl list (e.g. services/listing-service,services/rental-service)
MAVEN_PL=""
# Format 2: JSON array for GitHub Actions matrix (e.g. ["listing-service","rental-service"])
MATRIX_JSON="["

for i in "${!SELECTED_SERVICES[@]}"; do
  svc="${SELECTED_SERVICES[$i]}"
  if [ "$i" -gt 0 ]; then
    MAVEN_PL="${MAVEN_PL},"
    MATRIX_JSON="${MATRIX_JSON},"
  fi
  MAVEN_PL="${MAVEN_PL}services/${svc}"
  MATRIX_JSON="${MATRIX_JSON}\"${svc}\""
done
MATRIX_JSON="${MATRIX_JSON}]"

echo "build_all=${BuildAll}"
echo "service_count=${#SELECTED_SERVICES[@]}"
echo "maven_modules=${MAVEN_PL}"
echo "matrix_json=${MATRIX_JSON}"
