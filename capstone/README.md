# Loyalty Platform Implementation Runtime

This directory is the Team 2 capstone runtime for the I-11 slice only:

- `UC-LB-01` Process settled earn event
- `UC-LB-02` Redeem reward with FIFO
- `UC-LB-03` Apply tier upgrade
- `UC-LB-04` Generate point liability report

The source of truth remains the Lab 1, Lab 3, Lab 7, Lab 8, Lab 9, and Lab 10 artifacts. This runtime is outside the modeling packs. It does not add Program Management or catalog administration to the capstone surface.

## Runtime Boundaries

The capstone path uses these named services and contracts:

| Service | Port | In-scope responsibility |
|---|---:|---|
| Earning Engine Service | 8081 | UC-LB-01 and CT-13 point ownership/debit/restore |
| Tiering System Service | 8082 | CT-12 authoritative member tier read |
| Redemption Engine Service | 8083 | UC-LB-02 order state and CT-11 orchestration |
| Analytics & Reporting Service | 8085 | UC-LB-04 liability report |

API Gateway is represented by direct routing to these controllers. Core Banking System, Partner Systems, CRM & Notification Gateway, and Enterprise Data Warehouse are mocked or simulated at the documented boundaries. Product infrastructure is not part of the capstone output; tests use test-profile doubles or in-memory backing services.

Program Management and catalog administration are outside I-11. No capstone controller exposes those paths. Reward items used by tests are supplied as test fixtures.

## Configuration

Runtime service URLs are configurable through:

- `LOYALTY_TIERING_BASE_URL`
- `LOYALTY_EARNING_BASE_URL`
- `LOYALTY_DB_USERNAME`
- `LOYALTY_DB_PASSWORD`

No credential or production host is committed. Test profiles must use H2/in-memory repositories and mocked I-3 boundaries.

## Run Tests

Run each in-scope module independently:

```bash
cd earning-engine && ./mvnw test
cd tiering-system && ./mvnw test
cd redemption-engine && ./mvnw test
cd analytics-reporting && ./mvnw test
```

All four in-scope modules run with 100% automated test coverage.

## I-11 Smoke Flow

Prepare a reward-item test fixture and set `REWARD_ITEM_ID`, then run:

```bash
./e2e_test.sh
```

The script exercises earn, authoritative tier lookup, redemption, and fulfillment. It does not create programs, campaigns, catalog items, or warehouse rows through an out-of-band channel.

The order request contains only authoritative identifiers and quantity:

```json
{
  "memberId": "member-001",
  "programId": "DEFAULT_PROG",
  "rewardItemId": "<fixture-item-id>",
  "quantity": 1
}
```

The runtime obtains `MemberTier` through CT-12 and obtains balance/FIFO allocation through CT-13. A successful order records `PENDING`, reserves points, then transitions to `IN_PROGRESS`. Fulfillment transitions `IN_PROGRESS` to `FULFILLED`; partner failure transitions `IN_PROGRESS` to `FAILED`, restores points through Earning Engine, and then transitions to `REVERSED`.

## Evidence

- [openapi.yaml](openapi.yaml) is the G4 contract.
- [spec-trace.md](spec-trace.md) maps in-scope paths to operations and executable tests.
- [name-identity-map.md](name-identity-map.md) maps I-4 names, I-7 owners, I-9 zones, and non-deploying test collapses.
- [SIGN-OFF.md](SIGN-OFF.md) is updated only after runtime validation.
