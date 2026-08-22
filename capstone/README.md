# Capstone — Loyalty Banking Platform runtime

Implementation of the I-11 slice of the after pack. Not a modeling lab, and not part of the before or after pack.

**Spec:** `../loyalty.md` (Lab 1), `../lab3-spec.md` (contract, exception, test spec), `../lab7-adoption.md` (G1–G6), `../lab8-archimate-views.md`, `../lab-09-c4-after.md`, `../lab-10-uml-after.md`.
**R** Dev Lê Huy Du · **A** SA Vũ Trường Quang · **C** Test Lê Huy Du. Labs 1–10 are unchanged by this sitting, apart from decision D-4 in `SIGN-OFF.md`.

> This runtime is **SA-accepted** for the current `feature/capstone_temp` worktree. See `SIGN-OFF.md`.

| Document | What it is |
|---|---|
| `openapi.yaml` | G4 — the public contract, three operations, all from the Lab 3 register |
| `name-identity-map.md` | Code identity to Lab 1 string, collapse rows, assumptions |
| `spec-trace.md` | Each in-scope path → OpenAPI operation → test id, plus the non-public and N/A rows |
| `SIGN-OFF.md` | SA acceptance record — **ACCEPTED** for the current worktree |

---

## Build, test, run

Requires a **JDK 11 or later**. Nothing else — no build tool, no network, no container runtime.

```bash
cd capstone

# compile
mkdir -p out
javac -d out $(find src -name "*.java")

# run the G6 suite  (exit code 0 = all green)
javac -d out -cp out $(find test -name "*.java")
java -cp out com.loyalty.capstone.CapstoneTests

# start the runtime
java -cp out com.loyalty.capstone.Main 8080
```

On Windows PowerShell, replace the `$(find …)` parts:

```powershell
javac -d out (Get-ChildItem -Recurse src -Filter *.java | % FullName)
javac -d out -cp out (Get-ChildItem -Recurse test -Filter *.java | % FullName)
java -cp out com.loyalty.capstone.CapstoneTests
```

---

## Demo — ten minutes

1. **I-1 goal.** Read the goal and outcome from `../loyalty.md`, and CON.1–CON.4.
2. **One I-11 sequence on screen.** Open the UC-LB-02 sequence in `../lab-10-uml-after.md`.
3. **Live happy path.**

```bash
java -cp out com.loyalty.capstone.Main 8080 &

# earn: 200 spend, 1 point per unit, doubled by the active campaign = 400 points
curl -s -X POST localhost:8080/partner-earn \
  -H 'Content-Type: application/json' \
  -d '{"sourceTransactionId":"TXN-1001","memberId":"M-1001","amount":200}'

# redeem 300 points, oldest batch first, partner delivers
curl -s -X POST localhost:8080/redemptions \
  -H 'Content-Type: application/json' \
  -d '{"memberId":"M-1001","rewardItemId":"RI-VOUCHER-300"}'
```

Expect `"state":"FULFILLED"`.

4. **Live named `alt` and CON.\*.**

```bash
# CON.1 — the same source transaction again: 409, no second posting
curl -s -i -X POST localhost:8080/partner-earn \
  -H 'Content-Type: application/json' \
  -d '{"sourceTransactionId":"TXN-1001","memberId":"M-1001","amount":200}' | head -1

# tier-ineligible reward: 422, cancelled before any debit
curl -s -X POST localhost:8080/redemptions \
  -H 'Content-Type: application/json' \
  -d '{"memberId":"M-1001","rewardItemId":"RI-LOUNGE-500"}'

# CON.3 — partner fails: order ends REVERSED and the original batch is restored
curl -s -X POST localhost:8080/partner-earn \
  -H 'Content-Type: application/json' \
  -d '{"sourceTransactionId":"TXN-1002","memberId":"M-1001","amount":200}'
curl -s -X POST localhost:8080/redemptions \
  -H 'Content-Type: application/json' \
  -d '{"memberId":"M-1001","rewardItemId":"RI-OUTOFSTOCK-300"}'

# UC-LB-04
curl -s localhost:8080/reports/point-liability
```

5. **Test report.** `java -cp out com.loyalty.capstone.CapstoneTests` — 27 tests: 10 G6 rows, 3 negative tests, 4 use-case paths, 6 HTTP contract tests, 4 OpenAPI drift tests.

Run the suite from the `capstone` directory so the drift tests can find `openapi.yaml`.

---

## How the hard rules are made impossible

Not comments, not README warnings — the tests attempt each violation and assert the rejection.

| Rule | Mechanism | Test |
|---|---|---|
| CON.1 no duplicate posting | The duplicate check runs before any write; a repeat returns the original result | `G6-A01` |
| CON.2 no write outside the owner | Every store carries the I-4 name of its only writer; anything else throws `OwnershipViolation` | `NEG-I5-01` |
| CON.3 restore on fulfillment failure | Points go back onto the **same** batches, so earn date and expiry survive and no new batch appears | `G6-A03` |
| CON.4 ten-minute freshness | The report carries `stale` and alerts Finance instead of presenting an old figure as current | `G6-A05` |
| I-9 forbidden path | No gateway route writes a store; a direct attempt from an external name is refused | `NEG-I9-01` |
| I-6 six states only | `RedemptionOrder` is a type whose transitions are operations; anything outside I-6 throws | `NEG-I6-01` |
| G4 no drift | The suite parses `openapi.yaml` and compares paths and statuses with the runtime | `G4-D01`…`G4-D04` |

---

## Scope

I-11 only: UC-LB-01, UC-LB-02, UC-LB-03, UC-LB-04, each with the `alt` named in I-11. Everything else from Lab 1 "in scope" is listed N/A in `name-identity-map.md` §8 and `spec-trace.md` §5, and is deliberately not built.

The runtime is a documented collapse — one process, in-memory stores, in-process bus — permitted by the capstone brief. No product is stood up and no cluster is output; `Kafka`, `Redis`, and `PostgreSQL` stay labels in Lab 1 and appear nowhere in this code.

All four I-3 externals are stubs or in-process fakes. No real host, no credential, no configuration secret.
