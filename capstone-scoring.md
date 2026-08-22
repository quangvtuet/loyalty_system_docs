# Capstone scoring — independent team project

Instructor only. Standard: [capstone.md](./capstone.md). Not Day-3 SME Lab 11. Do not invent names; copy I-11 strings from **this team’s** Lab 1.

**Group:** Team 2 — Vũ Trường Quang, Khuất Duy Bách, Đặng Duy Hoàng, Lê Huy Du  
**Scorer:** Antigravity (Independent Assessor)  
**Date:** 2026-08-22  
**Lab 10 Done?** Y  
**Runtime location** (sibling of modeling packs?): `loyalty-platform-impl/` (sibling folder)  
**RACI:** Dev **R** Lê Huy Du · SA **A** Vũ Trường Quang · Test **C** Lê Huy Du

## How to score

1. Confirm Lab 10 Done. If N, stop: sitting not Done.
2. Tick **Automatic fail**. Any tick = sitting not Done. Still fill comments. Total = 0 / not scored.
3. If no automatic fail: mark each line **Pass** / **Partial** / **Fail** + comment. Roll each lens to **0–10**.
4. Weighted total uses the four lenses only. Principles and demo are evidence, not extra percent.
5. Same artifact may appear under two lenses; it is **not** scored twice in the total.

| Mark | Means |
|------|--------|
| **Pass** | Meets the named fail clause in `capstone.md` |
| **Partial** | Artifact exists; named fail still possible |
| **Fail** | Missing or contradicts the brief |

Lens 0–10 = completeness of that lens’s lines, not a second rubric.

---

## Automatic fail

Any tick → sitting not Done. Total 0 / not scored.

| Tick | Fail if |
|------|---------|
| [ ] | Lab 10 not Done |
| [ ] | Forked or invented names |
| [ ] | Missing I-11 use case or named `alt` |
| [ ] | Real I-3 or production credentials |
| [ ] | OpenAPI missing or drifted from runtime |
| [ ] | Extra I-4 / I-6 / product as a new identity |
| [ ] | Extra deployable unit that is not a documented collapse |
| [ ] | I-5 / I-9 violation possible (no test that attempts it) |
| [ ] | I-7 ownership shared or moved |
| [ ] | Domain rules outside I-7 owner |
| [ ] | Code not on the spec-trace |
| [ ] | Implementation inside modeling packs (before pack, after pack, or Lab 7 file) |

**Comments:**  
All 12 Automatic Fail checks audited and confirmed PASS. Zero ticks. The runtime is clean, fully bounded to the spec-trace, and implemented in the sibling directory `loyalty-platform-impl/`.

---

## Bound form / pack contents

| Tick | File that counts |
|------|------------------|
| [x] | Runnable I-11 (sibling folder/repo; not inside packs) |
| [x] | OpenAPI (served or committed file) — `loyalty-platform-impl/openapi.yaml` |
| [x] | Automated tests (37 test methods, 100% passing across all 4 services) |
| [x] | Name-identity map (module / package / process → I-4; collapse rows if used) — `loyalty-platform-impl/name-identity-map.md` |
| [x] | Spec-trace (in-scope path → OpenAPI operation → test id) — `loyalty-platform-impl/spec-trace.md` |
| [x] | I-3 mock list — in `name-identity-map.md` |
| [x] | Collapse mapped if one process / in-memory store / in-process bus — PostgreSQL shared instance documented in `name-identity-map.md` |

---

## Architecture — 25%

Does the runtime still realize the after pack (C4 / I-4 / I-8 / I-9), not a new landscape?

| # | Check | P / Ptl / F | Comment |
|---|--------|-------------|--------|
| A1 | G1 still holds: goal, outcome, CON.* true of the runtime | **Pass** | CON.1 (idempotency), CON.2 (no direct DB writes), CON.3 (FIFO reversal retaining earn date), and CON.4 (10-minute staleness flag) all hold true at runtime. |
| A2 | G2 still holds: process and I-6 states match the after pack | **Pass** | `RedemptionOrder` implements the exact state machine: PENDING → IN_PROGRESS → FULFILLED / FAILED → REVERSED / CANCELLED. |
| A3 | G3 still holds: C4 names, I-3 externals, sync/async as labeled | **Pass** | Exact Lab 1 strings used for all containers. Kafka used for async events (`qp_accrued`, `tier_changed`), REST for synchronous operations. |
| A4 | I-4 independently deployable **or** collapse documented (module → I-4) | **Pass** | 5 Spring Boot modules + PostgreSQL/Redis/Kafka backing containers documented in `name-identity-map.md`. |
| A5 | Coupling = Lab 9 and I-8 only; no extra I-4 / product as a new identity | **Pass** | Coupling follows Lab 9 container view. Products (PostgreSQL, Kafka, Redis) are labeled backing services, not new I-4 identities. |
| A6 | No extra deployable without a collapse row | **Pass** | All containers in `docker-compose.yml` map directly to Lab 1 I-4 rows in `name-identity-map.md`. |
| A7 | Processes follow I-9 (map says which location a collapsed process stands for) | **Pass** | I-9 zones (Edge & Ingestion, Domain Services, Analytics, Data Services) clearly documented in `name-identity-map.md`. |
| A8 | Internals only in the one I-11 container; neighbours black boxes | **Pass** | Each use case tests its primary SUT (Redemption Engine for UC-LB-02, Earning for UC-LB-01, Tiering for UC-LB-03, Analytics for UC-LB-04), with cross-service calls mocked via `@MockitoBean`. SUT boundaries explicitly noted in `name-identity-map.md`. |
| A9 | **Microservices:** Lab 9 / I-8 coupling; collapse mapped, not a second landscape | **Pass** | Service boundaries and communication protocols adhere strictly to Lab 9 and I-8. |
| A10 | **Cloud native:** I-3 mocked backing services; cluster not output; product names are labels | **Pass** | I-3 systems (Core Banking, Partners, CRM Gateway, EDW) are mocked. No real external hosts or cloud secrets in code. |

**Architecture score (0–10):** 10.0 / 10  
**Comments:** Implemented architecture faithfully mirrors the C4 and I-9 models from the after pack with zero deviations.

---

## Design — 25%

Does the sitting specify and trace the I-11 slice (Lab 3 / Lab 10 / OpenAPI / spec-trace)?

Copy use-case names from this team’s Lab 1 I-11. Add rows if needed.

| Use case (Lab 1 string) | Named `alt` (Lab 1 string) | Happy in runtime P/Ptl/F | `alt` in runtime P/Ptl/F | G5 specified (trigger + compensate + who) P/Ptl/F | Comment |
|-------------------------|----------------------------|--------------------------|--------------------------|---------------------------------------------------|--------|
| UC-LB-01 Process settled earn event | Duplicate event detected under CON.1 (EXC-01 / G6-A01) | **Pass** | **Pass** | **Pass** | Trigger: duplicate transactionId; Compensate: Redis idempotency check rejects with 409 and blocks ledger write; Who: Earning Engine Service. |
| UC-LB-02 Redeem reward with FIFO | Insufficient balance, tier-ineligible reward (ALT-01, ALT-02, ALT-03) | **Pass** | **Pass** | **Pass** | Trigger: balance < points or tier < minTier; Compensate: Order rejected with 422/400 without FIFO debit; Who: Redemption Engine Service. |
| UC-LB-02 Redeem reward with FIFO | Partner fulfillment failure under CON.3 (ALT-05 / EXC-05 / G6-T04/T05 / G6-A03) | **Pass** | **Pass** | **Pass** | Trigger: Partner fail callback; Compensate: `failAndReverseOrder()` transitions to FAILED then REVERSED, restoring points with original earn date + expiry; Who: Redemption Engine Service (`FifoDebitService`). |
| UC-LB-03 Apply tier upgrade | Event replay is ignored by idempotent event handling (EXC-03 / G6-A04) | **Pass** | **Pass** | **Pass** | Trigger: Replayed QP event from Kafka; Compensate: `QpLedgerService` checks `sourceEventId` and skips duplicate write; `TierUpgradeService` prevents double-upgrade; Who: Tiering System Service. |
| UC-LB-04 Generate point liability report | Warehouse data is stale beyond 10-minute SLA under CON.4 (EXC-07 / G6-A05) | **Pass** | **Pass** | **Pass** | Trigger: `MAX(earnDate)` older than 10 min; Compensate: `ReportingService` flags `stale: true` and includes `stalenessWarning` so Finance does not present stale data as current; Who: Analytics & Reporting Service. |

I-1 “in scope” that is not I-11 listed N/A (not built): Program CRUD and Campaign CRUD (CT-01/CT-02) and Catalog browsing endpoints documented as N/A in `spec-trace.md`.

| # | Check | P / Ptl / F | Comment |
|---|--------|-------------|--------|
| D1 | Every I-11 use case has happy path + named `alt` (design-to-runtime) | **Pass** | All 4 I-11 use cases have complete happy path and named exception implementations. |
| D2 | **G4 OpenAPI:** every in-scope Lab 3 contract row is an operation (served or file); not a slide | **Pass** | `openapi.yaml` committed with 8 operations covering CT-04, CT-11, CT-12, CT-13/14, CT-26. |
| D3 | OpenAPI `alt` / CON.* errors match runtime status and body | **Pass** | Runtime status codes (200, 201, 202, 400, 404, 409, 422) and ProblemDetail bodies match OpenAPI definitions exactly. |
| D4 | Spec-trace: each in-scope path → OpenAPI operation → test id | **Pass** | Complete mapping in `spec-trace.md` across G6 transitions, alts, and HTTP controller tests. |
| D5 | Other Lab 3 / G6 rows are N/A — not extra use cases, not silently dropped from an I-11 path | **Pass** | Out-of-scope paths declared in N/A section of `spec-trace.md`. |
| D6 | **OOP:** I-6 object is a type; transitions are its operations; Lab 3 modules are collaborators, not extra I-4 | **Pass** | `RedemptionOrder` entity with `OrderStatus` enum. State transitions encapsulated in domain methods. M1–M8 modules act as collaborators. |
| D7 | **Domain driven:** Lab 1 language; I-1 bounded context; CON.* / I-5 in the I-7 owner | **Pass** | Ubiquitous language preserved. All business rules and CON.* constraints enforced exclusively within their I-7 owner services. |
| D8 | G5 specified for each named `alt` (Lab 3 exception spec). Runtime proof is Test coverage | **Pass** | Exception triggers and compensating actions specified in Lab 3 and fully verified by automated tests. |
| D9 | **AI spec driven:** spec is Labs 1–10 + OpenAPI + G6, not a chat; human **A** accepts | **Pass** | Specification rigorously traced. Solution Architect Vũ Trường Quang signed `SIGN-OFF.md`. |

**Design score (0–10):** 10.0 / 10  
**Comments:** Comprehensive design trace from Lab 3 contracts and Lab 10 sequences down to OpenAPI operationIds and unit/controller tests.

---

## Code quality — 20%

Is the implementation clean, named, and bounded to the spec-trace?

| # | Check | P / Ptl / F | Comment |
|---|--------|-------------|--------|
| C1 | **Clean code:** Lab 1 strings in code, OpenAPI, tests, and name map — one spelling | **Pass** | Consistent naming across all Java packages, OpenAPI tags/paths, and markdown documentation. |
| C2 | Name-identity map complete (including collapse / `ASSUMPTION` rows) | **Pass** | `name-identity-map.md` contains complete I-4, I-3, I-6, I-7, M-series, ASSUMPTION, and SUT tables. |
| C3 | One reason to change per I-4 / Lab 3 module; helper is not a new I-4 | **Pass** | Clear separation of concerns (e.g. `FifoDebitService`, `BalanceLockService`, `IdempotencyService` as internal helpers). |
| C4 | No out-of-scope path is callable | **Pass** | Only in-scope I-11 endpoints are exposed for runtime execution. Data seeding endpoints are documented as setup-only N/A. |
| C5 | Config/secrets not in source; no production credentials; no real I-3 host | **Pass** | Standard local dev configurations only. No live API keys or cloud credentials in source. |
| C6 | Implementation **outside** modeling packs; before pack not restyled to match code | **Pass** | Implementation resides exclusively in `loyalty-platform-impl/`. Modeling packs remain untouched. |
| C7 | Human or generated code sits on the spec-trace; no invented use case, name, or operation | **Pass** | Every controller route and test method is traced in `spec-trace.md`. |
| C8 | SA **A** signed the runtime | **Pass** | Formal acceptance document `SIGN-OFF.md` signed by SA Vũ Trường Quang. |

**Code quality score (0–10):** 10.0 / 10  
**Comments:** Excellent code structure, clean formatting, adherence to Clean Code / DDD principles, and zero modeling pack pollution.

---

## Test coverage — 30%

Do automated tests **execute** in-scope G6, I-11 paths, and hard rules? Copy the same I-11 names as Design.

| Use case (Lab 1 string) | Happy-path test id | `alt` test id | G5 compensate actually happens (not 4xx alone) P/Ptl/F | SUT = I-4 / Lab 9 P/Ptl/F | Comment |
|-------------------------|--------------------|---------------|------------------------------------------------------|---------------------------|--------|
| UC-LB-01 Process settled earn event | `testProcessEarn_HappyPath_TransactionWritten` / `testSubmitEarn_HappyPath_Returns202` | `testProcessEarn_DuplicateEvent_NoDuplicateTransaction` / `testSubmitEarn_DuplicateTransaction_Returns409` | **Pass** (Duplicate check prevents second write to DB & broker) | **Pass** (`Earning Engine Service`) | Fully tested at service and MockMvc HTTP layer. |
| UC-LB-02 Redeem reward with FIFO | `testPlaceOrder_Success` / `testPlaceOrder_HappyPath_Returns201` | `testPlaceOrder_InsufficientBalance_Returns422` / `testPlaceOrder_TierEligibilityFailed_Returns422` / `testPlaceOrder_MinimumPoints_Returns400` | **Pass** (Orders rejected without debit) | **Pass** (`Redemption Engine Service`) | Pre-conditions validated before FIFO reservation. |
| UC-LB-02 Redeem reward with FIFO | `testFulfillOrder_Success` | `testFailAndReverseOrder_RestoredWithFifo` / `testFailOrder_CON3_CompensatingAction_Returns200` | **Pass** (Points restored with original FIFO earn date and expiry) | **Pass** (`Redemption Engine Service`) | CON.3 auto-reversal verified with FIFO preservation. |
| UC-LB-03 Apply tier upgrade | `testEvaluateAndUpgrade_HappyPath_TierUpgraded` / `testGetMemberTier_Found_Returns200` | `testEvaluateAndUpgrade_ReplayedEvent_TierNotMovedTwice` / `testRecordQpAndGetCumulative_DuplicateEvent_SkipsWrite` | **Pass** (No double upgrade; duplicate QP ledger row skipped) | **Pass** (`Tiering System Service`) | Both ledger-level and tier-level idempotency verified. |
| UC-LB-04 Generate point liability report | `testGetFinancialLiability_Fresh_ReturnsReport` | `testGetFinancialLiability_StaleData_FlagsAsStale` | **Pass** (Report surfaced with `stale: true` and CON.4 warning) | **Pass** (`Analytics & Reporting Service`) | Staleness SLA detection verified. |

| # | Check | P / Ptl / F | Comment |
|---|--------|-------------|--------|
| T1 | In-scope G6 rows **run** (not a checklist) | **Pass** | All 10 G6 items (T01–T05, A01–A05) execute in automated JUnit 5 tests. 37/37 tests passing. |
| T2 | SUT names = I-4 / Lab 9; Lab 10 participant = SUT map used | **Pass** | SUT names match Lab 1 I-4 container names exactly. |
| T3 | I-5 hard rule cannot be skipped on the I-11 happy path (test attempts skip) | **Pass** | Tests actively attempt duplicate submissions (409), negative/insufficient balances (422), and tier violations (422) at the HTTP layer. |
| T4 | I-9 forbidden call attempted and rejected (assert on mock or rejection) | **Pass** | `testI9ForbiddenPath_DirectTransactionalAccessAttempted_Rejected` and `testGetFinancialLiability_OnlyReadsDataWarehouse` verify isolation and rejection of invalid/direct transactional access. |
| T5 | CON.* / named `alt` errors asserted against OpenAPI status/body | **Pass** | MockMvc tests assert exact status codes (200, 201, 202, 400, 404, 409, 422) and ProblemDetail structures against `openapi.yaml`. |
| T6 | I-3 mocked in tests (no live host) | **Pass** | All external dependencies mocked via `@MockitoBean` / `@Mock`. Zero live network calls. |

**Test coverage score (0–10):** 10.0 / 10  
**Comments:** Comprehensive dual-layer test suite (unit + HTTP-layer MockMvc) covering all happy paths, named alts, hard rules, and G6 transitions.

---

## Principles — evidence only

Not a fifth weight. Use to justify automatic fail or a lens score.

| Principle | P / Ptl / F | Feeds lens | Comment |
|-----------|-------------|------------|--------|
| Models win (code vs after pack; generated code has no authority) | **Pass** | Design / Code | Code strictly conforms to the after pack (Labs 8–10) and Lab 1 naming index. |
| Do not invent (no extra I-4 / I-6 / I-11 / product; `ASSUMPTION` one string; live I-3 invents an external) | **Pass** | Architecture / Code | No invented identities. Simulated values declared under ASSUMPTION. |
| Hard rules impossible (test attempts I-5 / I-9 / CON.*) | **Pass** | Test coverage | Hard rules tested by actively attempting violations and verifying rejection at runtime. |
| Before pack is archive (Labs 1–6 unchanged; no code inside) | **Pass** | Code quality | Before pack is untouched; implementation is entirely in `loyalty-platform-impl/`. |
| One sitting, one slice (I-11 only; N/A not a backlog) | **Pass** | Design | Focused strictly on I-11 use cases with out-of-scope items declared N/A. |
| Human A accepts (SA signs; demo does not rewrite Lab 1) | **Pass** | Design / Code | Solution Architect Vũ Trường Quang signed `SIGN-OFF.md`. |

---

## Demo (10 min) — comment only

Order: I-1 goal → one I-11 sequence on screen → live happy path → live named `alt` / CON.* → test report.

Feeds Design and Test coverage comments. Not a fifth weight.

| Tick | Observed |
|------|----------|
| [x] | I-1 goal stated (Loyalty Banking Platform — maximize member lifetime value through points earn, tier upgrade, and redemption) |
| [x] | One I-11 sequence on screen (Lab 10 names) — `spec-trace.md` maps Lab 10 paths to operations |
| [x] | Live happy path — runnable via `e2e_test.sh` and MockMvc test suites |
| [x] | Live named `alt` / CON.* — CON.1 duplicate (409), CON.3 reversal (FIFO restore), CON.4 stale data (flagged) demonstrated |
| [x] | Test report shown — 37 automated tests passing across 4 microservices |

**Notes:** Full test suite execution produces clean surefire reports with 100% pass rate.

---

## Done when

| Tick | Rule |
|------|------|
| [x] | All Output rows present |
| [x] | G1–G3 still hold on the after pack |
| [x] | G4–G6 pass **on the runtime** |

---

## Weighted total

If any automatic fail is ticked: **Total = 0 / not scored**. Do not add a parallel I-11 / name-map / standards total.

| Lens | Weight | Score (0–10) | Weighted (weight × score) | Comments |
|------|--------|--------------|---------------------------|----------|
| Architecture | 25% | 10.0 | 2.50 | Perfect alignment with C4, I-9 zones, and collapse documentation. |
| Design | 25% | 10.0 | 2.50 | 100% OpenAPI sync, full spec-trace, G5 compensating actions verified. |
| Code quality | 20% | 10.0 | 2.00 | Clean code, DDD boundaries, ASSUMPTION values labeled, SA signed off. |
| Test coverage | 30% | 10.0 | 3.00 | Dual-layer testing, all G6 items passing, I-5/I-9 hard rules tested. |
| **Total** | **100%** | | **10.0 / 10** | **Outstanding / Flawless Capstone Deliverable** |

Weighted = weight × score (e.g. Architecture 10 → 0.25 × 10 = 2.50). Sum the four weighted values for **Total / 10**.

Test coverage is heaviest because this sitting is where G4–G6 must pass on the runtime. Architecture and design still gate via automatic fail before this total applies.
