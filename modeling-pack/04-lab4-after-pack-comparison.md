# Lab 4: After Pack and Comparison Note

**R**: SA  
**A**: EA  
**Rule**: This file adds the Lab 4 submission form without overwriting the existing architecture and design files.

## 1. Pack Index

| Pack | Evidence | Preservation rule |
|---|---|---|
| Before modeling | Branches `feature/lab2`, `feature/lab3`, `feature/lab8`, `feature/lab8-archimate`; existing docs before `feature/du-lab4` | Do not rewrite. Treat branch history as the unchanged archive. |
| Modeling | `07-lab7-adoption-record.md` and `template/list.md` Guide | Use Guide as written for trainee pack review. |
| After modeling | Current files plus `feature/du-lab4` additions: `mdd-index.md`, `architecture/domain-event-catalog.md`, `design/entity-lifecycle-models.md`, C4 L3 additions, DD additions | Restyled and supplemented by this file. |
| Compare | This file | Lists defects and before/after changes. |

## 2. After Pack View Register

| View family | Before evidence | After evidence | Language |
|---|---|---|---|
| Lab 8 Motivation / Strategy | Earlier ArchiMate drafts in branch history | `architecture/archimate/motivation-layer.md`, `architecture/archimate/strategy-layer.md`, `architecture/ArchiMate-Layer-Diagrams.md` | ArchiMate |
| Lab 8 Business Process | Earlier process/model drafts in branch history | `architecture/archimate/business-layer.md` | ArchiMate |
| Lab 8 Application Cooperation | Earlier application layer drafts | `architecture/archimate/application-layer.md` | ArchiMate |
| Lab 8 Technology / hybrid | Earlier technology/deployment drafts | `architecture/archimate/technology-layer.md`, `Architecture-Overview.md` deployment section | ArchiMate |
| Lab 9 C4 Context | `Architecture-Overview.md` section 2 before MDD additions | `Architecture-Overview.md` section 2 | C4 |
| Lab 9 C4 Container | `Architecture-Overview.md` section 3 before MDD additions | `Architecture-Overview.md` section 3 | C4 |
| Lab 9 C4 Component | Earlier selected/incomplete component docs | `Architecture-Overview.md` C4 Level 3 additions; `DD-03` selected component evidence | C4 |
| Lab 5 / 10 UML Sequence | DD sequence diagrams before standardization | `design/DD-01..05` sequence diagrams | UML |
| Lab 5 / 10 UML State | Partial states before standardization | `design/entity-lifecycle-models.md`, `design/DD-03`, `design/DD-04` | UML |
| Lab 6 Integration ecosystem | C4 Container / Application layer integration model | `Architecture-Overview.md`, `architecture/archimate/application-layer.md`, `architecture/domain-event-catalog.md` | C4 / ArchiMate, one viewpoint per canvas |

## 3. After View Header Register

The following header metadata applies to after diagrams referenced above.

| View | Viewpoint | Layer(s) | Owner | RACI |
|---|---|---|---|---|
| Motivation / Strategy | ArchiMate | Strategy / Motivation | Owner | R EA, A Owner, C SA BA/PO Sec, I DA Dev Test Ops |
| Business Process | ArchiMate | Business | Owner | R BA/PO, A Owner, C EA SA Sec Test, I DA Dev Ops |
| Application Cooperation | ArchiMate | Application | SA | R SA, A SA, C EA DA Sec Dev, I BA/PO Test Ops Owner |
| Technology / Deployment | ArchiMate | Technology | SA | R Ops, A SA, C Sec Dev, I EA BA/PO DA Test Owner |
| C4 Context | C4 | Solution Context | Owner | R SA, A Owner, C EA BA/PO Sec, I DA Dev Test Ops |
| C4 Container | C4 | Solution Container | SA | R SA, A SA, C DA Sec Dev Ops, I EA BA/PO Test Owner |
| C4 Component: Redemption Engine Service | C4 | Delivery Component | SA | R Dev, A SA, C DA Sec Test, I EA BA/PO Ops Owner |
| UML Sequence: Redeem reward with FIFO | UML | Delivery Behavior | SA | R Dev, A SA, C BA/PO Sec Test, I EA DA Ops Owner |
| UML State: RedemptionOrder | UML | Delivery State | BA/PO | R Test, A BA/PO, C SA Sec Dev, I EA DA Ops Owner |

Legend for after views:

- ArchiMate views use ArchiMate relationship names such as Serving, Flow, Access, Realization, Triggering, and Assignment.
- C4 views use actor/system/container/component relationships with relationship labels.
- UML views use message, `alt`, and state transition labels only.
- RACI legend: R = draws; A = approves; C = consulted; I = informed.

## 4. Name-Identity Check

| Lab 1 string | ArchiMate / C4 / UML usage |
|---|---|
| Loyalty Banking Platform | System-in-focus in C4 Context and top-level platform in enterprise views |
| API Gateway / OAuth 2.0 Auth Server | Application component and C4 container |
| Message Broker - Apache Kafka | Application event bus and C4 container |
| Earning Engine Service | Application component, C4 container, UML participant, test SUT |
| Tiering System Service | Application component, C4 container, UML participant, test SUT |
| Redemption Engine Service | Application component, C4 container, C4 Component boundary, UML participant, test SUT |
| Program Management Service | Application component, C4 container, UML participant, test SUT |
| Analytics & Reporting Service | Application component, C4 container, UML participant, test SUT |
| Redis Cache & Distributed Lock | C4 container and technology/data service |
| Data Warehouse - Star Schema | C4 container and analytics data store |
| RedemptionOrder | UML state object and domain entity |

No new external system should be introduced outside I-3. Product names such as Kafka, PostgreSQL, Redis, OAuth, and Spring Boot are labels or implementation technologies, not separate business systems for the trainee pack.

## 5. Language Check

| Canvas | One viewpoint? | Notes |
|---|---|---|
| Motivation / Strategy | Yes | ArchiMate only; no protocol, pod, or JDBC grain in the trainee review view |
| Business Process | Yes | ArchiMate process view; process steps refer to business objects and constraints |
| Application Cooperation | Yes | ArchiMate application components and event/interface relationships |
| Technology / hybrid | Yes | ArchiMate technology/deployment view; implementation labels are acceptable only as labels |
| C4 Context | Yes | Context only; no component internals |
| C4 Container | Yes | Containers and external systems with sync/async labels |
| C4 Component | Yes | Only `Redemption Engine Service` is exploded for the trainee selected container |
| UML Sequence | Yes | Lifelines are actors, C4 containers, or selected container components only |
| UML State | Yes | One object: `RedemptionOrder` |

## 6. Defect List Found in Before Pack

| Defect ID | Before issue | Owner | After correction |
|---|---|---|---|
| D-L4-01 | No completed Lab 1 name-identity index was available to constrain labels | BA / SA | `01-lab1-input-index.md` defines I-1 to I-11 |
| D-L4-02 | Quality gates existed as extended custom gate sets rather than the required `G1` to `G6` trainee gates | EA | `07-lab7-adoption-record.md` adopts `G1` to `G6` as authoritative for the pack |
| D-L4-03 | Before/after pack comparison was not explicit | SA | This file adds Pack Index and comparison |
| D-L4-04 | RACI/header data was not available as a per-after-view register | SA | Header register added in section 3 |
| D-L4-05 | Contract and G6 coverage existed across docs/tests but not in the required Lab 3 tables | Dev / Test | `03-lab3-build-design-test-spec.md` adds contract, exception, and test tables |
| D-L4-06 | Lab 4 standardization added strong MDD artifacts but did not map them to the exact template outputs | SA / EA | This comparison file maps each artifact to template outputs |

## 7. Comparison Note

| Area | Before modeling | After modeling |
|---|---|---|
| Names | Names existed across docs but no single enforced name-identity index | I-1 to I-11 define canonical names; after checks map boxes, lifelines, and SUTs back to those names |
| Layers | Architecture, design, and implementation content were detailed but not packaged by lab viewpoint | Views are mapped to ArchiMate, C4, and UML responsibilities |
| Language mix | Some diagrams used mixed implementation and architecture vocabulary | After register separates ArchiMate, C4, UML, and implementation labels |
| Legend/RACI | Existing docs did not carry the required after-view header/RACI form | Header/RACI register is added here and can be copied into rendered diagrams if required |
| Internals on Context | Context risk existed where protocol or runtime detail appeared in context-level views | Context review rule now forbids internals; C4 Component is limited to `Redemption Engine Service` |
| Contracts | Domain event catalog and APIs existed across docs/code but were not in G4 register form | G4 contract register added in Lab 3 supplement |
| Test coverage | Tests existed but were not mapped to state transitions and sequence alternatives | G6 test spec maps transitions/alts to SUTs and tests |

## 8. Done Check

| Requirement | Status |
|---|---|
| Before pack archived unchanged | Satisfied by branch history references; no old files overwritten |
| After pack exists | Satisfied by current model-driven docs plus this supplement |
| Same views restyled, not a new landscape | Satisfied by mapping old view families to after evidence |
| Comparison note lists before vs after | Satisfied in section 7 |
| Name-identity check exists | Satisfied in section 4 |
| Language check exists | Satisfied in section 5 |
| Defect list has owner | Satisfied in section 6 |

