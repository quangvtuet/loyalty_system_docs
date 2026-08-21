# Lab 8: ArchiMate Views Check

**R**: EA for Motivation/Strategy; BA for Process; SA for Application Cooperation; Ops/SA for Technology  
**A**: Owner for Motivation and Process  
**Rule**: Four named views only for trainee review.

## Input Coverage

| Lab 1 input | Evidence in this supplement |
|---|---|
| I-1 goal/outcome | `01-lab1-input-index.md` I-1; Motivation / Strategy view row below |
| I-4 containers | `01-lab1-input-index.md` I-4; Application Cooperation view row below |
| I-5 process | `01-lab1-input-index.md` I-5; Business Process view row below |
| I-9 deployment | `01-lab1-input-index.md` I-9; Technology view row below |
| I-10 constraints | `01-lab1-input-index.md` I-10; Motivation and Process view rows below |

## Four Named Views

| # | View | After evidence | Must show | Must not show | Status |
|---:|---|---|---|---|---|
| 1 | Motivation or Strategy | `architecture/archimate/motivation-layer.md`; `architecture/archimate/strategy-layer.md` | Goal, outcome, `CON.1` to `CON.3` as constraints or principles | Protocol, pods, JDBC, container internals | Pass with constraint mapping below |
| 2 | Business Process | `architecture/archimate/business-layer.md` | I-5 happy path and `CON.*` on branches | C4 containers as process boxes; sync/async labels | Pass with process-to-constraint mapping below |
| 3 | Application Cooperation | `architecture/archimate/application-layer.md` | Containers from I-4 using same strings as C4 Container | UML messages; mixed C4 notation | Pass with name-identity mapping below |
| 4 | Technology / hybrid | `architecture/archimate/technology-layer.md`; deployment section in `Architecture-Overview.md` | Locations from I-9 and forbidden path | Channel writing core ledger DB | Pass with forbidden-path check below |

## After Header Register

Use this register as the required after-view header source. It preserves old diagrams and adds the required review metadata.

| View | Header |
|---|---|
| Motivation / Strategy | Title: Loyalty Banking Motivation and Strategy; Viewpoint: ArchiMate; Layer(s): Strategy / Motivation; To-Be; Owner: Owner; RACI: R EA, A Owner, C SA BA/PO Sec, I DA Dev Test Ops; Version: v1.0, Date 2026-08-21, Status Review; Legend: Influence, Realization, Association, Triggering, Serving; Scope: Lab 1 I-1 |
| Business Process | Title: Loyalty Banking Business Process; Viewpoint: ArchiMate; Layer(s): Business; To-Be; Owner: Owner; RACI: R BA/PO, A Owner, C EA SA Sec Test, I DA Dev Ops; Version: v1.0, Date 2026-08-21, Status Review; Legend: Serving, Triggering, Assignment, Access, Realization; Scope: Lab 1 I-5 |
| Application Cooperation | Title: Loyalty Banking Application Cooperation; Viewpoint: ArchiMate; Layer(s): Application; To-Be; Owner: SA; RACI: R SA, A SA, C EA DA Sec Dev, I BA/PO Test Ops Owner; Version: v1.0, Date 2026-08-21, Status Review; Legend: Serving, Flow, Association, Access; Scope: Lab 1 I-4 and I-8 |
| Technology / hybrid | Title: Loyalty Banking Technology Deployment; Viewpoint: ArchiMate; Layer(s): Technology; To-Be; Owner: SA; RACI: R Ops, A SA, C Sec Dev, I EA BA/PO DA Test Owner; Version: v1.0, Date 2026-08-21, Status Review; Legend: Assignment, Serving, Access, Flow; Scope: Lab 1 I-9 |

## G1: Motivation / Strategy Constraint Mapping

| G1 item | Lab 1 source | View representation |
|---|---|---|
| Goal | I-1 Goal | Real-time earning, ledger integrity, reporting isolation, and governed adjustments in Motivation/Strategy evidence |
| Outcome | I-1 Outcome | SLA and data-staleness goals in Motivation/Strategy evidence |
| CON.1 | No duplicate point posting | Principle / requirement for event-driven idempotency and ledger integrity |
| CON.2 | No direct database writes from channels, partners, or other services | Principle / requirement for database-per-service and source-of-truth ownership |
| CON.3 | Fulfillment failure must compensate by restoring points | Requirement/constraint attached to redemption and ledger integrity capability |

## G2: Business Process and State Mapping

| I-5 step | ArchiMate Business Process representation | Constraint branch | Related state |
|---:|---|---|---|
| 1 | Settlement ingestion event enters loyalty process | CON.1 if duplicate source transaction | `PointTransaction` duplicate avoided |
| 2 | Point earning and ledger accrual | CON.1 if idempotency check fails | `PointTransaction` created/confirmed |
| 3 | QP accrual event published | CON.2 prevents direct tier DB write by Earning Engine | `MemberTier` updated by owner |
| 4 | Tier progression process evaluates threshold | CON.2 controls owner writes | `MemberTier` active/upgraded |
| 5 | Member submits redemption request | CON.2 enforces API route through gateway/service | `RedemptionOrder` PENDING |
| 6 | FIFO redemption and validation process | CON.2 blocks direct ledger DB mutation by Redemption Engine | `RedemptionOrder` IN_PROGRESS |
| 7 | Fulfillment process completes or fails | CON.3 on failure branch | `RedemptionOrder` FULFILLED or FAILED |
| 8 | Analytics reporting process consumes CDC | CON.2 blocks reporting reads from OLTP | Warehouse facts updated |

## Application Cooperation Name Identity

| I-4 canonical name | Application view usage |
|---|---|
| API Gateway / OAuth 2.0 Auth Server | Edge application interface/component |
| Message Broker - Apache Kafka | Application event bus |
| Earning Engine Service | Application component |
| Tiering System Service | Application component |
| Redemption Engine Service | Application component |
| Program Management Service | Application component |
| Analytics & Reporting Service | Application component |
| Redis Cache & Distributed Lock | Application/technology support component label |
| Data Warehouse - Star Schema | Analytics data object/store |

## Technology / Forbidden Path Check

| Location from I-9 | Technology view evidence | Forbidden-path status |
|---|---|---|
| Edge & Ingestion Zone | API gateway and event bus technology services | Does not write directly to Earning DB - PostgreSQL |
| Domain Services Zone | Service runtimes for earning, tiering, redemption, program management | Each service writes only its owned database |
| Analytics Zone | Analytics service and Data Warehouse - Star Schema | Analytics reads warehouse/CDC, not OLTP directly |
| Data Services Zone | PostgreSQL service databases and Redis | Owned by service boundaries and not exposed to external actors |

## Negative Evidence

| Forbidden item | Status |
|---|---|
| Protocol, pods, JDBC, or container internals on Motivation / Strategy review view | Excluded from trainee Motivation / Strategy mapping |
| C4 containers as Business Process boxes | Business process mapping uses process names and business objects |
| UML messages inside Application Cooperation | Application Cooperation mapping uses ArchiMate application flows/interfaces |
| Channel writing core ledger DB | Explicitly forbidden by I-9 and CON.2 |
| More than four required Lab 8 views | Extra ArchiMate files/presentations are retained as supporting material, but the trainee review set is the four named views above |

