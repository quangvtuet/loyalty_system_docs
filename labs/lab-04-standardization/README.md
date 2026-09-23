# Lab 4 — First cleanup of Labs 1–3

**System-in-focus:** Loyalty Banking Platform
**R:** SA · **A:** EA
**Input:** Labs 1–3 as first written (frozen in `before-pack/`) and the current Labs 1–3 at the repository root
**Language:** current team language. English only.

This sitting is a **cleanup of our own three labs using our own method**. No modelling standard is adopted here and no diagram is drawn here — those are later sittings. What we did was read Labs 1, 2, and 3 side by side, find every place where the same thing was called two different things, and make one name win.

The messy copies are frozen in `before-pack/` and were not edited in place.

---

## 1. Cleaned 1–3 pack

| Lab | Cleaned artifact | Was |
|---|---|---|
| Lab 1 | `loyalty.md` | `before-pack/lab1/loyalty.md` |
| Lab 2 | `lab2-requirements.md` | `before-pack/lab2/` — twelve files (five FR, five AS, domain model, traceability matrix) |
| Lab 3 | `lab3-spec.md` | `before-pack/lab3/lab3-section-from-impl-readme.md` — a section inside the implementation folder's README |

### 1.1 One list of containers

These thirteen strings are the only container names any of our three labs may use.

| # | Container | Owns / responsible for |
|---:|---|---|
| 1 | API Gateway | Edge routing, authentication, and rate limiting for every request from outside |
| 2 | Message Broker | Carries every asynchronous event between services |
| 3 | Earning Engine Service | Calculates points and owns the point ledger |
| 4 | Tiering System Service | Accrues qualifying points and owns tier state |
| 5 | Redemption Engine Service | Runs the reward catalogue, the debit, and the reversal |
| 6 | Program Management Service | Owns programme, campaign, rule, partner, and adjustment configuration |
| 7 | Analytics & Reporting Service | Builds and serves reporting figures |
| 8 | Idempotency Store | Holds the duplicate-check keys and the per-Member debit lock |
| 9 | Earning DB | Source of truth for `PointTransaction` and `PointBalance` |
| 10 | Tiering DB | Source of truth for `MemberTier` |
| 11 | Redemption DB | Source of truth for `RedemptionOrder` |
| 12 | Program Mgmt DB | Source of truth for `LoyaltyProgram` and `Campaign` |
| 13 | Data Warehouse | Source of truth for `FactPointTransaction` |

### 1.2 One list of actors and external systems

| Kind | Name | Role |
|---|---|---|
| Actor | Member | Earns points, checks balance, redeems rewards |
| Actor | Program Admin | Configures programmes, campaigns, and rules |
| Actor | Finance | Reviews liability, breakage, and reconciliation |
| Actor | Support Agent | Raises manual adjustments and reversals |
| External | Core Banking System | Publishes settled transactions, authorisations, and reversals |
| External | Partner Systems | Submit partner earn events and fulfil rewards |
| External | CRM & Notification Gateway | Customer profile reference and outbound notifications |
| External | Enterprise Data Warehouse | Group-wide archival and business intelligence destination |

### 1.3 One list of data objects and states

| Data object | Source of truth |
|---|---|
| `PointTransaction` | Earning DB |
| `PointBalance` | Earning DB |
| `MemberTier` | Tiering DB |
| `RedemptionOrder` | Redemption DB |
| `LoyaltyProgram` | Program Mgmt DB |
| `Campaign` | Program Mgmt DB |
| `FactPointTransaction` | Data Warehouse |

`RedemptionOrder` is the one object with a named state list: `PENDING`, `IN_PROGRESS`, `FULFILLED`, `FAILED`, `CANCELLED`, `REVERSED`. Terminal: `FULFILLED`, `CANCELLED`, `REVERSED`.

---

## 2. Name-identity check

Every string used in the cleaned Lab 2 and Lab 3 resolves to a Lab 1 entry. Checked by reading each name in Labs 2 and 3 against the three lists above.

| Name used in Labs 2–3 | Lab 1 source | Fork found before cleanup? |
|---|---|---|
| API Gateway | I-4 | Yes — was also written `API Gateway / OAuth 2.0 Auth Server`, and `OAuth 2.0` appeared alone as if it were a system |
| Message Broker | I-4 | Yes — was also written `Message Broker - Apache Kafka` and plain `Kafka` |
| Earning Engine Service | I-4 | No |
| Tiering System Service | I-4 | No |
| Redemption Engine Service | I-4 | No |
| Program Management Service | I-4 | No |
| Analytics & Reporting Service | I-4 | No |
| Idempotency Store | I-4 | Yes — was `Redis Cache & Distributed Lock` and plain `Redis` |
| Earning DB | I-4 | Yes — was `Earning DB - PostgreSQL`, and also appeared as `Sổ cái`, `Earning Ledger`, and `EarningLedger` |
| Tiering DB | I-4 | Yes — was `Tiering DB - PostgreSQL` |
| Redemption DB | I-4 | Yes — was `Redemption DB - PostgreSQL` |
| Program Mgmt DB | I-4 | Yes — was `Program Mgmt DB - PostgreSQL` |
| Data Warehouse | I-4 | Yes — was `Data Warehouse - Star Schema` |
| Member | I-2 | Yes — was `Member / Customer` and `Cardholder` |
| Program Admin | I-2 | Yes — was `Program Admin / Marketing`, and `Marketing Team` and `Partner Manager` appeared as separate actors |
| Finance | I-2 | Yes — was `Finance & Compliance`, and `Auditor` and `Executive / BI Team` appeared as separate actors |
| Support Agent | I-2 | No |
| Core Banking System | I-3 | No |
| Partner Systems | I-3 | Yes — was also `Partner`, `Fulfillment System`, and `Fulfillment Partner` |
| CRM & Notification Gateway | I-3 | Yes — was also `Notification Service` |
| Enterprise Data Warehouse | I-3 | Yes — was `Enterprise Data Warehouse / Lake` |

Names removed entirely because they were never in Lab 1: `SFTP`, `Auditor`, `Partner Manager`, `Marketing Team`, `Executive / BI Team`, `Fulfillment System`, `Notification Service`, `OAuth 2.0` as a system.

Product names now appear only as labels inside Lab 1 I-4, never as a system, an actor, or the subject of a requirement.

---

## 3. Defect list — Labs 1–3 as first written

Each defect is a failure found on the frozen copies in `before-pack/`, with the person who owns the fix.

### Lab 1

| ID | Defect | Owner |
|---|---|---|
| D-01 | Container names carried a product inside the name (`Earning DB - PostgreSQL`, `Message Broker - Apache Kafka`, `API Gateway / OAuth 2.0 Auth Server`, `Data Warehouse - Star Schema`, `Redis Cache & Distributed Lock`). Downstream documents shortened them inconsistently, so one container ended up with two or three names. | Khuất Duy Bách (BA/SA) |
| D-02 | Three actor names packed two roles into one string (`Member / Customer`, `Program Admin / Marketing`, `Finance & Compliance`). Downstream documents picked whichever half they preferred, and then invented extra actors to cover the other half. | Khuất Duy Bách (BA/SA) |
| D-03 | I-5 listed four hard rules but I-10 listed only three constraints. The rule that reporting must not degrade transactional workload had no `CON` identifier, so no requirement could cite it and it fell out of the pack. | Vũ Trường Quang (SA) |
| D-04 | I-6 was written as a loose From/To/Trigger list without a terminal flag per row, so the test spec had to guess which states end the lifecycle. | Lê Huy Du (Test) |

### Lab 2

| ID | Defect | Owner |
|---|---|---|
| D-05 | Lab 2 was spread across twelve files (five FR, five AS, a domain model, a traceability matrix) when the deliverable is one file. | Đặng Duy Hoàng (BA) |
| D-06 | Requirements named systems that do not exist in Lab 1: `Sổ cái` / `Earning Ledger`, `Fulfillment System`, `Partner Manager`, `Auditor`, `Marketing Team`, `Executive / BI Team`, and `SFTP` as a report destination. | Đặng Duy Hoàng (BA) |
| D-07 | Vietnamese terms were mixed into an English pack, mainly `Sổ cái` used as the name of the ledger. | Đặng Duy Hoàng (BA) |
| D-08 | `OAuth 2.0` was written as a required product in its own right, when authentication belongs to API Gateway and the product is only a label. | Vũ Trường Quang (SA) |
| D-09 | Requirement rows traced to sections of the domain document rather than to a Goal, a `CON.*`, an I-5 step, or a state, so the trace could not be checked against Lab 1. | Đặng Duy Hoàng (BA) |
| D-10 | The analysis section described our own documents and review process instead of the business as-is and to-be from I-1. | Đặng Duy Hoàng (BA) |
| D-11 | A quality-gate register was written into Lab 2. Gates are a later sitting and this pulled a standard into the pack before we had agreed one. | Vũ Trường Quang (SA) |
| D-12 | The 60-second outcome was traced to CON.1, which is the no-duplicate-posting rule. Wrong constraint — 60 seconds comes from the I-1 outcome. | Đặng Duy Hoàng (BA) |
| D-13 | CON.2 was cited by a single requirement and CON.4 by none, so two of our four constraints were effectively unimplemented. | Vũ Trường Quang (SA) |

### Lab 3

| ID | Defect | Owner |
|---|---|---|
| D-14 | The Lab 3 specification lived inside the implementation folder's README, mixed in with install and run instructions, so the specification and the side spike could not be told apart. | Vũ Trường Quang (SA) |
| D-15 | The contract register was built one row per container-diagram edge and cited diagrams as evidence, when the source for Lab 3 is the I-8 integration table. | Khuất Duy Bách (Dev) |
| D-16 | Test rows cited real test classes and a running system as evidence. Lab 3 tests are planned, not executed. | Lê Huy Du (Test) |
| D-17 | The test spec contained a `Start -> PENDING` row that is not a transition in I-6, and did not have one row per I-6 transition. | Lê Huy Du (Test) |
| D-18 | The exception spec covered three constraints and missed the reporting-isolation constraint — a direct consequence of D-03. | Vũ Trường Quang (SA) |
| D-19 | Build-list owners were written as generic functions (`Dev / Ops`) instead of a named person, so no row had an accountable individual. | Khuất Duy Bách (Dev) |
| D-20 | The build list and contract register still used the long container names from D-01 after Lab 1 had been shortened. | Khuất Duy Bách (Dev) |

---

## 4. Comparison note

### 4.1 What we cleaned

**Names.** This was the bulk of the work. One container had up to four names across the pack — `Earning DB`, `Earning DB - PostgreSQL`, `Earning Ledger`, and `Sổ cái` were all the same thing. We took the shortest name that still says what it is, moved every product name to a label, and deleted the eight actors and systems that had been invented along the way. Fourteen of twenty-one names in the index had forked before this cleanup.

**Constraints.** We had four hard rules but only three numbered constraints, and the missing one was the reason the reporting isolation rule kept dropping out of downstream documents. Giving it an identifier immediately gave it requirements and an exception row.

**Shape.** Lab 2 went from twelve files to one, and Lab 3 moved out of the implementation README into its own file. Neither change altered the content much — the concrete rules we had agreed (1 point per $1, 60 seconds, 2× campaigns, Silver/Gold/Platinum, 100-point minimum, oldest-points-first, restore on failure) all survived intact. What changed is that they are now in one place, in one language, and each one points back at a line in Lab 1.

**Tracing.** Before, a requirement pointed at a section of our own domain document, which pointed at another document. Now every requirement points at a Goal, an Outcome, a constraint, a process step, or a state — all of which live in Lab 1. That means the trace can actually be checked instead of merely looking thorough.

**Language.** The pack is English throughout. The one Vietnamese term we had used as a real name is gone.

### 4.2 What we still do not know how to standardise

Being honest about the gaps, because these are the things a method is supposed to solve for us.

- **Sync versus async is stuck in a table.** We can say in a contract row that something is asynchronous, but we have no agreed way to *show* it, and no way to prove a picture and the table still agree with each other.
- **We wrote a lot of material out of order.** `architecture/`, `design/`, and `quality-gates/` were produced before we had a locked name index, and they still carry the old long names and the old constraint set. We do not yet know whether to rewrite them, fold them into later sittings, or drop them. For now they are stale and we have left them alone rather than half-fixing them.
- **Name identity is enforced by hand.** Our check is a person reading and searching for strings. It caught the forks this time, but nothing stops the next document from inventing a fifteenth container.
- **Two names are uncomfortably close.** `Data Warehouse` is ours and `Enterprise Data Warehouse` is somebody else's. They read almost the same and we expect them to be confused again.
- **One container may be doing two jobs.** `Idempotency Store` holds both the duplicate-check keys and the per-Member debit lock. Those are arguably two responsibilities sharing a box, and we have no rule that tells us when to split a container.
- **We have no rule for where a product name may appear.** We agreed it goes on the label and not in the name, but that was our judgement in this sitting, not a standard we can point at.
- **We do not know how to record who approves what.** Each lab tells us who is responsible and who is accountable, but we have not written that down anywhere durable, and nothing on our artifacts shows it.
