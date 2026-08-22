#!/usr/bin/env bash
set -euo pipefail

# I-11 runtime smoke flow. Catalog data is a test fixture, supplied by REWARD_ITEM_ID.
: "${REWARD_ITEM_ID:?Set REWARD_ITEM_ID to an existing test fixture}"
MEMBER_ID="member-e2e-$(date +%s)"
TXN_ID="txn-e2e-$(date +%s)"
PROGRAM_ID="${PROGRAM_ID:-DEFAULT_PROG}"

curl --fail -sS -X POST http://localhost:8081/api/v1/partners/earn \
  -H 'Content-Type: application/json' \
  -d "{\"transactionId\":\"$TXN_ID\",\"memberId\":\"$MEMBER_ID\",\"programId\":\"$PROGRAM_ID\",\"spendAmount\":1000}" >/dev/null

curl --fail -sS "http://localhost:8082/api/v1/tiering/members/$MEMBER_ID/tier?programId=$PROGRAM_ID" >/dev/null

ORDER_JSON=$(curl --fail -sS -X POST http://localhost:8083/api/v1/redemptions/orders \
  -H 'Content-Type: application/json' \
  -d "{\"memberId\":\"$MEMBER_ID\",\"programId\":\"$PROGRAM_ID\",\"rewardItemId\":\"$REWARD_ITEM_ID\",\"quantity\":1}")
ORDER_ID=$(printf '%s' "$ORDER_JSON" | jq -r '.orderId')

curl --fail -sS -X PATCH "http://localhost:8083/api/v1/redemptions/orders/$ORDER_ID/fulfill" >/dev/null
printf 'I-11 smoke flow completed for order %s\n' "$ORDER_ID"
