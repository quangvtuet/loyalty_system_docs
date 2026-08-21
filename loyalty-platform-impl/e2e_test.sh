#!/bin/bash

# E2E Test Script for Loyalty Platform
echo "=========================================="
echo " Starting Loyalty Platform E2E Test"
echo "=========================================="

MEMBER_ID="member-e2e-$(date +%s)"
TXN_ID="txn-$(date +%s)"

# 1. Program Management (8084)
echo -e "\n[1] Creating Program in Program Management (8084)..."
PROGRAM_JSON=$(curl -s -X POST http://localhost:8084/api/v1/programs \
  -H "Content-Type: application/json" \
  -H "X-Operator-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "name": "BankRewards E2E",
    "currencyName": "Points",
    "costPerPoint": 0.01
  }')
PROGRAM_ID=$(echo $PROGRAM_JSON | jq -r '.programId')
echo "Created Program ID: $PROGRAM_ID"

echo "Activating Program..."
curl -s -X POST "http://localhost:8084/api/v1/programs/$PROGRAM_ID/activate" \
  -H "X-Operator-Id: 00000000-0000-0000-0000-000000000000" > /dev/null

echo "Creating Campaign..."
CAMPAIGN_JSON=$(curl -s -X POST http://localhost:8084/api/v1/campaigns \
  -H "Content-Type: application/json" \
  -H "X-Operator-Id: 00000000-0000-0000-0000-000000000000" \
  -d "{
    \"programId\": \"$PROGRAM_ID\",
    \"name\": \"E2E Double Points\",
    \"multiplier\": 2.0,
    \"flatBonus\": 0,
    \"priority\": 1,
    \"startDate\": \"2025-01-01T00:00:00\",
    \"endDate\": \"2026-12-31T23:59:59\"
  }")
CAMPAIGN_ID=$(echo $CAMPAIGN_JSON | jq -r '.campaignId')
echo "Created Campaign ID: $CAMPAIGN_ID"

# 2. Redemption Engine (8083) - Seed Catalog
echo -e "\n[2] Seeding Reward Item in Redemption Engine (8083)..."
REWARD_JSON=$(curl -s -X POST http://localhost:8083/api/v1/catalog/items \
  -H "Content-Type: application/json" \
  -d "{
    \"programId\": \"$PROGRAM_ID\",
    \"name\": \"E2E Gold Reward\",
    \"category\": \"VOUCHER\",
    \"pointsCost\": 500,
    \"currencyValue\": 5.00,
    \"fulfillmentType\": \"DIGITAL\",
    \"minTierRequired\": \"GOLD\"
  }")
REWARD_ID=$(echo $REWARD_JSON | jq -r '.itemId')
echo "Created Reward Item ID: $REWARD_ID"

# 3. Earning Engine (8081) - Earn Points
echo -e "\n[3] Earning Points in Earning Engine (8081)..."
curl -s -X POST http://localhost:8081/api/v1/partners/earn \
  -H "Content-Type: application/json" \
  -d "{
    \"transactionId\": \"$TXN_ID\",
    \"memberId\": \"$MEMBER_ID\",
    \"programId\": \"$PROGRAM_ID\",
    \"spendAmount\": 6000,
    \"campaignId\": \"DOUBLE_E2E\"
  }" > /dev/null
echo "Earned 6000 points (Base) + 6000 points (Bonus) for Member: $MEMBER_ID"

# Wait for Kafka to process QP and Tier upgrade
echo "Waiting 3 seconds for Kafka events (QP Accrual -> Tier Upgrade)..."
sleep 3

# Check Tier
TIER=$(docker exec loyalty-postgres psql -U loyalty_user -d loyalty_db -t -c "SELECT current_tier FROM member_tier WHERE member_id = '$MEMBER_ID';" | xargs)
echo "Current Member Tier in DB: $TIER"

# 4. Redemption Engine (8083) - Place Order
echo -e "\n[4] Redeeming Points in Redemption Engine (8083)..."
ORDER_JSON=$(curl -s -X POST http://localhost:8083/api/v1/redemptions/orders \
  -H "Content-Type: application/json" \
  -d "{
    \"memberId\": \"$MEMBER_ID\",
    \"programId\": \"$PROGRAM_ID\",
    \"rewardItemId\": \"$REWARD_ID\",
    \"quantity\": 1,
    \"memberTier\": \"$TIER\",
    \"availableBalance\": 12000
  }")
ORDER_ID=$(echo $ORDER_JSON | jq -r '.orderId')
echo "Created Redemption Order ID: $ORDER_ID (Status: PENDING)"

echo -e "\n[4.5] Simulating Partner Fulfillment Webhook..."
curl -s -X POST "http://localhost:8083/api/v1/redemptions/orders/$ORDER_ID/fulfill" > /dev/null
echo "Fulfillment Webhook called for Order ID: $ORDER_ID"

# Check Order Status
ORDER_STATUS=$(docker exec loyalty-postgres psql -U loyalty_user -d loyalty_db -t -c "SELECT status FROM redemption_order WHERE order_id = '$ORDER_ID';" | xargs)
echo "Current Order Status in DB: $ORDER_STATUS"

# 5. Analytics Reporting (8085) - Simulate ETL and check Liability
echo -e "\n[5] Simulating ETL and Checking Liability in Analytics Reporting (8085)..."
docker exec loyalty-postgres psql -U loyalty_user -d loyalty_db -c "
INSERT INTO dim_program (program_id, name, cost_per_point, status) 
VALUES ('$PROGRAM_ID', 'BankRewards E2E', 0.01, 'ACTIVE');

INSERT INTO fact_point_transaction (date_key, member_key, program_key, tier_name, channel, transaction_type, event_type, status, points_amount, points_remaining, financial_liability_usd, earn_date, expiry_date)
VALUES (20260815, 1, (SELECT program_key FROM dim_program WHERE program_id = '$PROGRAM_ID'), 'GOLD', 'API', 'EARN', 'EARN', 'CONFIRMED', 12000, 11500, 115.00, NOW(), NOW() + INTERVAL '1 year');
" > /dev/null

LIABILITY_JSON=$(curl -s -X GET http://localhost:8085/api/v1/analytics/liability/$PROGRAM_ID)
echo "Liability Report: $LIABILITY_JSON"

echo "=========================================="
echo " E2E Test Completed!"
echo "=========================================="
