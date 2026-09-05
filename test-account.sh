#!/bin/bash
set -e

echo "1. Fetching fresh JWT token from Keycloak..."
TOKEN=$(curl -s -X POST "http://localhost:8180/realms/lagos-rent/protocol/openid-connect/token" \
  -d "client_id=web-application" \
  -d "grant_type=password" \
  -d "username=tenant_test" \
  -d "password=Password123!" | jq -r .access_token)

echo "2. Calling API Gateway (/api/v1/accounts/me)..."
curl -i -X GET "http://localhost:8080/api/v1/accounts/me" \
  -H "Authorization: Bearer $TOKEN"

echo ""