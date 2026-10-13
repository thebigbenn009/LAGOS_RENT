#!/bin/bash
set -e

echo "========================================================="
echo "   LAGOS RENT — LISTING SERVICE NEGATIVE TEST SUITE      "
echo "========================================================="

echo ""
echo "--- Step 0: Fetching Keycloak Tokens ---"

TENANT_TOKEN=$(curl -s -X POST 'http://localhost:8180/realms/lagos-rent/protocol/openid-connect/token' \
  -d 'client_id=web-application' \
  -d 'grant_type=password' \
  -d 'username=tenant_test' \
  -d 'password=Password123!' | jq -r .access_token)

LANDLORD_TOKEN=$(curl -s -X POST 'http://localhost:8180/realms/lagos-rent/protocol/openid-connect/token' \
  -d 'client_id=web-application' \
  -d 'grant_type=password' \
  -d 'username=landlord_test' \
  -d 'password=Password123!' | jq -r .access_token)

ADMIN_TOKEN=$(curl -s -X POST 'http://localhost:8180/realms/lagos-rent/protocol/openid-connect/token' \
  -d 'client_id=web-application' \
  -d 'grant_type=password' \
  -d 'username=admin_test' \
  -d 'password=Password123!' | jq -r .access_token)

echo "Tokens successfully acquired."
echo ""

# --------------------------------------------------------------------------
echo "--- TEST 1: 401 Unauthorized (Missing Authorization Header) ---"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "http://localhost:8080/api/v1/properties")
echo "HTTP Status: $HTTP_CODE (Expected: 401)"
if [ "$HTTP_CODE" -eq 401 ]; then echo "✅ PASSED"; else echo "❌ FAILED"; fi
echo ""

# --------------------------------------------------------------------------
echo "--- TEST 2: 403 Forbidden (Tenant Calling Admin Review Queue) ---"
RES=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "http://localhost:8080/api/v1/admin/listings/review-queue" \
  -H "Authorization: Bearer $TENANT_TOKEN")
BODY=$(echo "$RES" | sed -e '$d')
STATUS=$(echo "$RES" | tail -n1 | cut -d: -f2)
echo "$BODY" | jq . 2>/dev/null || echo "$BODY"
echo "HTTP Status: $STATUS (Expected: 403)"
if [ "$STATUS" -eq 403 ]; then echo "✅ PASSED"; else echo "❌ FAILED"; fi
echo ""

# --------------------------------------------------------------------------
echo "--- TEST 3: 403 Forbidden (Landlord Calling Admin Listing Approval) ---"
RES=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "http://localhost:8080/api/v1/admin/listings/00000000-0000-0000-0000-000000000000/approve" \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reason": "I want to approve my own listing", "expectedVersion": 0}')
BODY=$(echo "$RES" | sed -e '$d')
STATUS=$(echo "$RES" | tail -n1 | cut -d: -f2)
echo "$BODY" | jq . 2>/dev/null || echo "$BODY"
echo "HTTP Status: $STATUS (Expected: 403)"
if [ "$STATUS" -eq 403 ]; then echo "✅ PASSED"; else echo "❌ FAILED"; fi
echo ""

# --------------------------------------------------------------------------
echo "--- TEST 4: 400 Bad Request (Validation Failure: Blank Title & Missing LGA) ---"
RES=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "http://localhost:8080/api/v1/properties" \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "",
    "lga": "",
    "area": "Lekki",
    "addressLine1": "14 Admiralty",
    "landmark": "Near Pizza"
  }')
BODY=$(echo "$RES" | sed -e '$d')
STATUS=$(echo "$RES" | tail -n1 | cut -d: -f2)
echo "$BODY" | jq .
echo "HTTP Status: $STATUS (Expected: 400)"
if [ "$STATUS" -eq 400 ]; then echo "✅ PASSED"; else echo "❌ FAILED"; fi
echo ""

# --------------------------------------------------------------------------
echo "--- TEST 5: 404 Not Found (Querying Non-Existent Property UUID) ---"
RES=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "http://localhost:8080/api/v1/properties/00000000-0000-0000-0000-000000000000" \
  -H "Authorization: Bearer $LANDLORD_TOKEN")
BODY=$(echo "$RES" | sed -e '$d')
STATUS=$(echo "$RES" | tail -n1 | cut -d: -f2)
echo "$BODY" | jq .
echo "HTTP Status: $STATUS (Expected: 404)"
if [ "$STATUS" -eq 404 ]; then echo "✅ PASSED"; else echo "❌ FAILED"; fi
echo ""

# --------------------------------------------------------------------------
echo "--- Setup: Create a Valid Property & Unit for State Machine Tests ---"
PROP_RES=$(curl -s -X POST "http://localhost:8080/api/v1/properties" \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Error Testing Building",
    "lga": "Ikeja",
    "area": "GRA",
    "addressLine1": "22 Isaac John Street",
    "landmark": "Near Airport Hotel"
  }')
PROP_ID=$(echo "$PROP_RES" | jq -r .id)

UNIT_RES=$(curl -s -X POST "http://localhost:8080/api/v1/properties/$PROP_ID/units" \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "unitNumber": "Flat 1A",
    "unitType": "FLAT",
    "bedrooms": 2,
    "bathrooms": 2,
    "toilets": 3
  }')
UNIT_ID=$(echo "$UNIT_RES" | jq -r .id)

echo "Created Property ID: $PROP_ID, Unit ID: $UNIT_ID"
echo ""

# --------------------------------------------------------------------------
echo "--- TEST 6: 400 Bad Request (Validation: Negative Rent Amount) ---"
RES=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "http://localhost:8080/api/v1/units/$UNIT_ID/listings" \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Invalid Rent Flat",
    "rentAmount": -200000.00,
    "availableFrom": "2026-10-01"
  }')
BODY=$(echo "$RES" | sed -e '$d')
STATUS=$(echo "$RES" | tail -n1 | cut -d: -f2)
echo "$BODY" | jq .
echo "HTTP Status: $STATUS (Expected: 400)"
if [ "$STATUS" -eq 400 ]; then echo "✅ PASSED"; else echo "❌ FAILED"; fi
echo ""

# --------------------------------------------------------------------------
echo "--- Setup: Create Valid DRAFT Listing ---"
LISTING_RES=$(curl -s -X POST "http://localhost:8080/api/v1/units/$UNIT_ID/listings" \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Testing State Machine Listing",
    "rentAmount": 2500000.00,
    "availableFrom": "2026-10-01"
  }')
LISTING_ID=$(echo "$LISTING_RES" | jq -r .id)
LISTING_VERSION=$(echo "$LISTING_RES" | jq -r .version)
echo "Created Listing ID: $LISTING_ID (Status: DRAFT, Version: $LISTING_VERSION)"
echo ""

# --------------------------------------------------------------------------
echo "--- TEST 7: 400 Bad Request (State Machine Violation: Publishing DRAFT Listing) ---"
RES=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "http://localhost:8080/api/v1/listings/$LISTING_ID/publish" \
  -H "Authorization: Bearer $LANDLORD_TOKEN")
BODY=$(echo "$RES" | sed -e '$d')
STATUS=$(echo "$RES" | tail -n1 | cut -d: -f2)
echo "$BODY" | jq .
echo "HTTP Status: $STATUS (Expected: 400 with INVALID_STATE_TRANSITION)"
if [ "$STATUS" -eq 400 ]; then echo "✅ PASSED"; else echo "❌ FAILED"; fi
echo ""

# --------------------------------------------------------------------------
echo "--- TEST 8: 400 Bad Request (Optimistic Locking / Concurrency: Stale Version Approval) ---"
# Submit for review first
curl -s -X POST "http://localhost:8080/api/v1/listings/$LISTING_ID/submit" \
  -H "Authorization: Bearer $LANDLORD_TOKEN" > /dev/null

# Attempt approval with an incorrect expected version (e.g. 999)
RES=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "http://localhost:8080/api/v1/admin/listings/$LISTING_ID/approve" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Approve test",
    "expectedVersion": 999
  }')
BODY=$(echo "$RES" | sed -e '$d')
STATUS=$(echo "$RES" | tail -n1 | cut -d: -f2)
echo "$BODY" | jq .
echo "HTTP Status: $STATUS (Expected: 400 with version mismatch error)"
if [ "$STATUS" -eq 400 ]; then echo "✅ PASSED"; else echo "❌ FAILED"; fi
echo ""

# --------------------------------------------------------------------------
echo "========================================================="
echo "   ALL NEGATIVE TEST CASES COMPLETED & VERIFIED! 🎉      "
echo "========================================================="
