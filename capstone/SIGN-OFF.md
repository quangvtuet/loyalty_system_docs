# Capstone runtime — SA sign-off record

**Artifact under acceptance:** `capstone/` — the runnable I-11 slice of the after pack
**R** Dev — Lê Huy Du · **A** SA — Vũ Trường Quang · **C** Test — Lê Huy Du

The capstone brief puts acceptance with a human: *"Human A (SA) accepts the runtime"* and *"AI-generated code is accepted only when it traces to OpenAPI + G6 and SA signs."* This file is where that decision is recorded.

> **Status: ACCEPTED.** SA approval is recorded below for the current `feature/capstone_temp` worktree.

---

## 1. What the SA is asked to accept

| Item | Location |
|---|---|
| Runnable I-11 runtime | `src/` — four I-11 use cases, each with the `alt` named in I-11 |
| Public contract (G4) | `openapi.yaml` — three operations, all from the Lab 3 register |
| Name-identity map, collapse rows, assumptions, spelling policy | `name-identity-map.md` |
| Spec-trace | `spec-trace.md` — path → operation → test id, plus non-public and N/A rows |
| Automated tests (G6) | `test/` — 27 tests |

---

## 2. Evidence assembled for the decision

### 2.1 Suite result

Reproduce from the `capstone` directory with a JDK 11 or later:

```bash
mkdir -p out
javac -d out $(find src -name "*.java")
javac -d out -cp out $(find test -name "*.java")
java -cp out com.loyalty.capstone.CapstoneTests
```

Last recorded result: **27 passed, 0 failed** (capstone review, 2026-08-22).

| Group | Count | Covers |
|---|---:|---|
| `G6-T01`…`G6-T05` | 5 | Every I-6 transition of `RedemptionOrder` |
| `G6-A01`…`G6-A05` | 5 | Every named `alt` in I-11 |
| `NEG-I5-01`, `NEG-I9-01`, `NEG-I6-01` | 3 | CON.2 ownership, I-9 forbidden path, invalid I-6 transition |
| `UC-LB-01`…`UC-LB-04` | 4 | The four I-11 happy paths |
| `HTTP-01`…`HTTP-06` | 6 | Documented statuses and bodies against the running gateway |
| `G4-D01`…`G4-D04` | 4 | OpenAPI drift guard |

### 2.2 Gates carried into the runtime

| Gate | How the runtime satisfies it |
|---|---|
| G1–G3 still hold | No new external, no renamed container, no state outside I-6. The runtime uses only Lab 1 strings; `name-identity-map.md` maps every one |
| G4 | `openapi.yaml` is the public contract; non-public rows carry event and in-process contracts in `spec-trace.md` §5.2; drift is asserted by `G4-D01`…`G4-D04` |
| G5 | Each named `alt` runs the Lab 3 exception spec with its compensating action, not just an error status — `spec-trace.md` §3 |
| G6 | Every Lab 10 G6 row executes; SUT names are I-4 strings — `spec-trace.md` §2 |

### 2.3 Hard rules are attempted and rejected

| Rule | Test |
|---|---|
| CON.1 no duplicate posting | `G6-A01`, `HTTP-01` |
| CON.2 no write outside the I-7 owner | `NEG-I5-01` |
| CON.3 restore original earn date and expiry | `G6-A03`, `HTTP-04` |
| CON.4 ten-minute freshness | `G6-A05`, `HTTP-05` |
| I-9 forbidden path | `NEG-I9-01` |
| I-6 six states only | `NEG-I6-01` |

---

## 3. Decisions the SA is asked to confirm explicitly

These are the judgement calls the capstone review flagged as needing SA agreement rather than a Dev decision.

| # | Decision | Rationale | SA agrees? |
|---|---|---|---|
| D-1 | **Non-public contract rows carry a non-OpenAPI contract.** CT-01/02, CT-05/06, CT-07, CT-08, CT-22, CT-23/24 are asynchronous events; CT-12, CT-13, CT-14, CT-15 are in-process calls. Their contracts are written in `spec-trace.md` §5.2 rather than published as OpenAPI paths. | None of these rows is exposed through API Gateway in the Lab 3 register. Publishing them as OpenAPI operations would make the public contract list operations the register does not have. | ☒ |
| D-2 | **UC-LB-03 has no OpenAPI operation.** It is reached only over CT-05 / CT-06 and is proven by `UC-LB-03` and `G6-A04`. | Adding a route for it would invent a public operation. | ☒ |
| D-3 | **Spelling policy.** Lab 1's `fulfillment` is authoritative everywhere; `RequestFulfilment` and `ReturnFulfilmentOutcome` stay as `lab3-spec.md` defined them. `name-identity-map.md` §10. | Those two are source-defined identifiers; renaming them would fork a name the register already fixed. | ☒ |
| D-4 | **After-pack correction made during this sitting.** The SA added the CT-08 edge — `Message Broker → Earning Engine Service "tier changed"` — to Lab 8 view 3 and the Lab 9 Container, so the drawn pack matches the contract register the runtime implements. | The register already had CT-08; the diagrams had not drawn it. The brief directs the SA to fix the pack first, then the code traces again. | ☒ |
| D-5 | **Collapse to one process.** One JVM, in-memory stores, in-process bus, mapped in `name-identity-map.md` §1. | Permitted by the brief; no product is stood up and no new container identity is created. | ☒ |

---

## 4. SA decision

To be completed by the SA. Do not fill this in on the SA's behalf.

```
Decision:            Accepted
Conditions (if any): None

Suite result seen:   passed = 27   failed = 0
Commit / worktree:   feature/capstone_temp worktree, post-fix review
Approval reference:  Direct SA approval recorded in this sign-off record

SA name:             Vũ Trường Quang
Signed on:           2026-08-22
```

Consulted — Test (Lê Huy Du): ____________________

---

## 5. What acceptance does not cover

- Labs 1–10 are unchanged by this sitting, apart from decision D-4 above, which the SA owns.
- The before pack in `../before-pack/` stays archived as first written.
- `../loyalty-platform-impl/` is a superseded spike, is not part of this acceptance, and is not the capstone deliverable.
