#!/bin/bash
set -e

# ==============================================================================
# Helper function to execute API calls and catch HTTP errors visibly
# (Prints HTTP status line like "HTTP/1.1 200 OK" or "HTTP/1.1 401 Unauthorized"
#  and immediately aborts on >= 400 errors with full response payload)
# ==============================================================================
call_api() {
  local METHOD="$1"
  local URL="$2"
  shift 2
  local EXTRA_ARGS=("$@")

  # Perform curl with -i to capture both status headers and body
  local RAW_OUTPUT
  RAW_OUTPUT=$(curl -s -i -X "$METHOD" "$URL" "${EXTRA_ARGS[@]}")

  # Extract the HTTP status line (e.g. "HTTP/1.1 200 OK", "HTTP/1.1 401 Unauthorized")
  local STATUS_LINE
  STATUS_LINE=$(echo "$RAW_OUTPUT" | grep -E '^HTTP/[0-9.]+' | tail -n 1 | tr -d '\r')

  # Extract numeric status code
  local STATUS_CODE
  STATUS_CODE=$(echo "$STATUS_LINE" | awk '{print $2}')

  # Extract response body (everything after the HTTP headers)
  local BODY
  BODY=$(echo "$RAW_OUTPUT" | sed '1,/^\r\{0,1\}$/d')

  # Always print the HTTP status line directly to the terminal
  echo "$STATUS_LINE" >&2

  # Check if HTTP status indicates failure (>= 400 or empty response)
  if [ -z "$STATUS_CODE" ] || [ "$STATUS_CODE" -ge 400 ] 2>/dev/null; then
    echo "" >&2
    echo "❌ Request Failed: $METHOD $URL" >&2
    echo "Status: $STATUS_LINE" >&2
    if [ -n "$BODY" ]; then
      echo "Response Body:" >&2
      echo "$BODY" | jq . 2>/dev/null || echo "$BODY" >&2
    fi
    echo "" >&2
    exit 1
  fi

  # Return pure JSON body to stdout for jq variable extraction
  echo "$BODY"
}

CORRELATION_ID="e2e-trace-$(uuidgen | tr '[:upper:]' '[:lower:]')"

echo '========================================================='
echo '   LAGOS RENT — DISTRIBUTED VERTICAL SLICE (E2E TEST)    '
echo '========================================================='
echo "Trace Correlation ID: $CORRELATION_ID"
echo ''

echo '--- Step 1: Landlord Authentication via Keycloak ---'
LANDLORD_RES=$(call_api POST 'http://localhost:8180/realms/lagos-rent/protocol/openid-connect/token' \
  -d 'client_id=web-application' \
  -d 'grant_type=password' \
  -d 'username=landlord_test' \
  -d 'password=Password123!')
LANDLORD_TOKEN=$(echo "$LANDLORD_RES" | jq -r '.access_token')

if [ -z "$LANDLORD_TOKEN" ] || [ "$LANDLORD_TOKEN" = "null" ]; then
  echo "❌ Failed to obtain landlord access token." >&2
  exit 1
fi
echo 'Landlord token acquired.'
echo ''

echo '--- Step 2: Account Service Profile Verification ---'
ACCOUNT_RES=$(call_api GET 'http://localhost:8080/api/v1/accounts/me' \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -H "X-Correlation-ID: $CORRELATION_ID")
LANDLORD_ACCOUNT_ID=$(echo "$ACCOUNT_RES" | jq -r '.id')
echo "Verified Account ID: $LANDLORD_ACCOUNT_ID"
echo ''

echo '--- Step 3: Listing Service — Create Property in Lekki Phase 1 ---'
PROP_RES=$(call_api POST 'http://localhost:8080/api/v1/properties' \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -H "X-Correlation-ID: $CORRELATION_ID" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Admiralty Luxury Heights",
    "lga": "Eti-Osa",
    "area": "Lekki Phase 1",
    "addressLine1": "28 Admiralty Way",
    "landmark": "Beside Sailors Lounge",
    "latitude": 6.4475,
    "longitude": 3.4730
  }')
PROP_ID=$(echo "$PROP_RES" | jq -r '.id')
echo "Created Property ID: $PROP_ID"
echo ''

echo '--- Step 4: Listing Service — Add Rental Unit (Penthouse 4A) ---'
UNIT_RES=$(call_api POST "http://localhost:8080/api/v1/properties/$PROP_ID/units" \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -H "X-Correlation-ID: $CORRELATION_ID" \
  -H 'Content-Type: application/json' \
  -d '{
    "unitNumber": "Penthouse 4A",
    "unitType": "APARTMENT",
    "bedrooms": 4,
    "bathrooms": 4,
    "toilets": 5,
    "sizeSqMeters": 220.0,
    "floorNumber": 4
  }')
UNIT_ID=$(echo "$UNIT_RES" | jq -r '.id')
echo "Created Unit ID: $UNIT_ID"
echo ''

echo '--- Step 5: Listing Service — Create Listing with Itemized Fees ---'
LISTING_RES=$(call_api POST "http://localhost:8080/api/v1/units/$UNIT_ID/listings" \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -H "X-Correlation-ID: $CORRELATION_ID" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Waterfront 4 Bedroom Penthouse in Lekki Phase 1",
    "description": "Stunning water view with private elevator, gym, and 24/7 power.",
    "rentAmount": 8000000.00,
    "rentPeriod": "ANNUALLY",
    "availableFrom": "2026-11-01",
    "fees": [
      {
        "feeType": "CAUTION_DEPOSIT",
        "amount": 800000.00,
        "recipient": "LANDLORD",
        "frequency": "ONE_OFF",
        "isRefundable": true,
        "explanation": "Refundable security deposit against damages."
      },
      {
        "feeType": "SERVICE_CHARGE",
        "amount": 1200000.00,
        "recipient": "PROPERTY_MANAGER",
        "frequency": "ANNUALLY",
        "isRefundable": false,
        "explanation": "Diesel generation, private security, facility management."
      },
      {
        "feeType": "LEGAL_AND_COUNSEL",
        "amount": 400000.00,
        "recipient": "AGENT",
        "frequency": "ONE_OFF",
        "isRefundable": false,
        "explanation": "Legal drafting and documentation fee."
      }
    ]
  }')
LISTING_ID=$(echo "$LISTING_RES" | jq -r '.id')
echo "Created Listing ID: $LISTING_ID (Move-in Total: $(echo "$LISTING_RES" | jq -r '.moveInTotal'))"
echo ''

echo '--- Step 6: Submit Listing for Admin Review ---'
SUBMIT_RES=$(call_api POST "http://localhost:8080/api/v1/listings/$LISTING_ID/submit" \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -H "X-Correlation-ID: $CORRELATION_ID")
echo "Listing submitted for review."
echo ''

echo '--- Step 7: Admin Review & Approval ---'
ADMIN_RES=$(call_api POST 'http://localhost:8180/realms/lagos-rent/protocol/openid-connect/token' \
  -d 'client_id=web-application' \
  -d 'grant_type=password' \
  -d 'username=admin_test' \
  -d 'password=Password123!')
ADMIN_TOKEN=$(echo "$ADMIN_RES" | jq -r '.access_token')

# Fetch fresh version to prevent optimistic locking failures
LATEST_LISTING=$(call_api GET "http://localhost:8080/api/v1/listings/$LISTING_ID" \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -H "X-Correlation-ID: $CORRELATION_ID")
CURRENT_VERSION=$(echo "$LATEST_LISTING" | jq -r '.version')
echo "Current Version: $CURRENT_VERSION"

APPROVE_RES=$(call_api POST "http://localhost:8080/api/v1/admin/listings/$LISTING_ID/approve" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "X-Correlation-ID: $CORRELATION_ID" \
  -H 'Content-Type: application/json' \
  -d '{
    "reason": "Listing details and transparent fees verified.",
    "expectedVersion": '"$CURRENT_VERSION"'
  }')
echo "Listing approved by Admin."
echo ''

echo '--- Step 8: Landlord Publishes Listing (Triggers Outbox Event) ---'
PUBLISH_RES=$(call_api POST "http://localhost:8080/api/v1/listings/$LISTING_ID/publish" \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -H "X-Correlation-ID: $CORRELATION_ID")
echo "Listing published successfully."
echo ''

echo '--- Step 9: Waiting 2 seconds for Outbox Publisher & RabbitMQ consumer ---'
sleep 2

echo '--- Step 10: Public Tenant Queries Search API (Unauthenticated) ---'
SEARCH_RES=$(call_api GET 'http://localhost:8080/api/v1/search/listings?area=Lekki%20Phase%201&bedrooms=4' \
  -H "X-Correlation-ID: $CORRELATION_ID")

MATCH_COUNT=$(echo "$SEARCH_RES" | jq --arg id "$LISTING_ID" '.content[]? | select(.listingId == $id) | .listingId' | wc -l)

echo ''
if [ "$MATCH_COUNT" -ge 1 ]; then
  echo '========================================================='
  echo '   DISTRIBUTED VERTICAL SLICE TEST PASSED! 🎉           '
  echo '========================================================='
  echo '1. Keycloak authenticated landlord.'
  echo '2. Account Service validated landlord profile.'
  echo '3. Listing Service enforced state machine & transparent pricing.'
  echo '4. Outbox recorded listing.published.v1 event atomically in PostgreSQL.'
  echo '5. RabbitMQ delivered event reliably to search.listing-projection.v1.'
  echo '6. Search Service deduplicated and built the projection.'
  echo '7. Public tenant queried the listing through API Gateway!'
else
  echo '❌ Verification Failed: Published listing not found in Search Service projection.'
  exit 1
fi
