#!/bin/bash
set -e

LANDLORD_TOKEN=$(curl -s -X POST 'http://localhost:8180/realms/lagos-rent/protocol/openid-connect/token' \
  -d 'client_id=web-application' \
  -d 'grant_type=password' \
  -d 'username=landlord_test' \
  -d 'password=Password123!' | jq -r .access_token)

echo "Token received: $LANDLORD_TOKEN"

echo "=== 2. Creating Property (Lekki Phase 1 Building) ==="
PROP_RES=$(curl -s -X POST "http://localhost:8080/api/v1/properties" \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Admiralty Heights",
    "lga": "Eti-Osa",
    "area": "Lekki Phase 1",
    "addressLine1": "14 Admiralty Way",
    "landmark": "Opposite Domino'\''s Pizza",
    "latitude": 6.4474,
    "longitude": 3.4723
  }')
echo $PROP_RES | jq .
PROP_ID=$(echo $PROP_RES | jq -r .id)

echo "=== 3. Adding Rental Unit (Flat 2B) to Property ==="
UNIT_RES=$(curl -s -X POST "http://localhost:8080/api/v1/properties/$PROP_ID/units" \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "unitNumber": "Flat 2B",
    "unitType": "FLAT",
    "bedrooms": 3,
    "bathrooms": 3,
    "toilets": 4,
    "sizeSqMeters": 145.5,
    "floorNumber": 2
  }')
echo $UNIT_RES | jq .
UNIT_ID=$(echo $UNIT_RES | jq -r .id)

echo "=== 4. Creating Listing with Transparent Fee Breakdown ==="
LISTING_RES=$(curl -s -X POST "http://localhost:8080/api/v1/units/$UNIT_ID/listings" \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Luxury 3 Bedroom Apartment in Lekki Phase 1",
    "description": "Spacious flat with 24/7 power, swimming pool, and fitted kitchen.",
    "rentAmount": 4500000.00,
    "rentPeriod": "ANNUALLY",
    "availableFrom": "2026-10-01",
    "fees": [
      {
        "feeType": "CAUTION_DEPOSIT",
        "amount": 500000.00,
        "recipient": "LANDLORD",
        "frequency": "ONE_OFF",
        "isRefundable": true,
        "explanation": "Refundable deposit held against property damage."
      },
      {
        "feeType": "SERVICE_CHARGE",
        "amount": 750000.00,
        "recipient": "PROPERTY_MANAGER",
        "frequency": "ANNUALLY",
        "isRefundable": false,
        "explanation": "Generator diesel, security, waste disposal, and facility cleaning."
      },
      {
        "feeType": "LEGAL_AND_COUNSEL",
        "amount": 225000.00,
        "recipient": "AGENT",
        "frequency": "ONE_OFF",
        "isRefundable": false,
        "explanation": "Tenancy contract drafting and legal documentation (5% of rent)."
      }
    ]
  }')
echo $LISTING_RES | jq .
LISTING_ID=$(echo $LISTING_RES | jq -r .id)
LISTING_VERSION=$(echo $LISTING_RES | jq -r .version)

echo "=== 5. Submitting Listing for Admin Review ==="
SUBMIT_RES=$(curl -s -X POST "http://localhost:8080/api/v1/listings/$LISTING_ID/submit" \
  -H "Authorization: Bearer $LANDLORD_TOKEN")
echo $SUBMIT_RES | jq .
LISTING_VERSION=$(echo $SUBMIT_RES | jq -r .version)

echo "=== 6. Getting Admin Token ==="
ADMIN_TOKEN=$(curl -s -X POST 'http://localhost:8180/realms/lagos-rent/protocol/openid-connect/token' \
  -d 'client_id=web-application' \
  -d 'grant_type=password' \
  -d 'username=admin_test' \
  -d 'password=Password123!' | jq -r .access_token)

echo "Token received: $ADMIN_TOKEN"

echo "=== 7. Admin Fetches Listing Details from Review Queue ==="
LISTING_DETAIL=$(curl -s -X GET "http://localhost:8080/api/v1/listings/$LISTING_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
echo $LISTING_DETAIL | jq .
CURRENT_VERSION=$(echo $LISTING_DETAIL | jq -r .version)
echo "Current Version to verify: $CURRENT_VERSION"
Rabb
echo "=== 8. Admin Approves Listing ==="
APPROVE_RES=$(curl -s -X POST "http://localhost:8080/api/v1/admin/listings/$LISTING_ID/approve" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Listing verified: all fee items are standard and move-in total is accurate.",
    "expectedVersion": '$CURRENT_VERSION'
  }')
echo $APPROVE_RES | jq .

echo "=== 9. Landlord Publishes Approved Listing ==="
PUBLISH_RES=$(curl -s -X POST "http://localhost:8080/api/v1/listings/$LISTING_ID/publish" \
  -H "Authorization: Bearer $LANDLORD_TOKEN")
echo $PUBLISH_RES | jq .

echo "=== Success! Complete Phase 10 Listing Flow Verified! ==="