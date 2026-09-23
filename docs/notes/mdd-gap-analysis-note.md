# Note — MDD gap analysis (not a lab artifact)

> **Outside the modeling pack.** This is an internal working note written before the pack was re-sequenced. It is **not** submitted for any lab, and the file paths and document names in it are stale.
> The Lab 4 cleanup is in [`../lab4-cleanup.md`](../lab4-cleanup.md).

## Goal

Standardize all design and architecture documentation in the Loyalty Banking project to follow a **Model-Driven Design (MDD)** methodology. This means every document must be anchored to formal models (domain model, data model, behavioral models, structural models) — not free-form prose — ensuring full traceability from domain concepts through requirements → analytics → architecture → design → quality gates.

## Background

Currently on `feature/lab3`, the repository has comprehensive documentation:

- **Domain Model**: [loyalty_domain.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/loyalty_domain.md)
- **Requirements** (5 FR docs): [requirements/](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/requirements)
- **Analytics Specs** (5 AS docs): [analytics/](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/analytics)
- **Architecture** (3 docs + 3 ADRs): [architecture/](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture)
- **Detailed Design** (5 DD docs): [design/](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design)
- **Quality Gates** (2 docs): [quality-gates/](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/quality-gates)
- **Traceability Matrix**: [traceability.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/traceability.md)

However, there are **gaps** when evaluated against a proper MDD framework:

## Gap Analysis

| MDD Artifact Category                     | Current State                                                       | Gap                                                                                                                               |
| ----------------------------------------- | ------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| **Domain Model (Conceptual)**             | ✅`loyalty_domain.md` exists with entities, glossary, flows         | ⚠️ Missing formal UML class diagram for domain model; entities defined only in tables                                             |
| **Use Case Model**                        | ✅ Use cases listed in FR docs (UC-01-01..06 etc.)                  | ⚠️ Missing UML use case diagrams per module                                                                                       |
| **Data Model (Logical + Physical)**       | ✅ ER diagram + full DDL in Data-Architecture-and-Schema.md         | ✅ Well covered                                                                                                                   |
| **Behavioral Models**                     | ✅ Sequence diagrams in DD-01                                       | ⚠️ DD-02..05 have NO sequence diagrams — only text flows                                                                          |
| **State Machine Models**                  | ✅ One state diagram in Security-Architecture (adjustment workflow) | ⚠️ Missing state machines for: Point lifecycle, Redemption Order lifecycle, Tier lifecycle, Program lifecycle, Campaign lifecycle |
| **Component Architecture**                | ✅ DD-01 has component flowchart                                    | ⚠️ DD-02..05 have NO component architecture diagrams                                                                              |
| **C4 Model (System/Container/Component)** | ✅ L1 + L2 in Architecture-Overview                                 | ⚠️ Missing C4 Level 3 (Component) diagrams per module                                                                             |
| **Domain Event Catalog**                  | ⚠️ Partially covered in Architecture-Overview §5.2 topic table      | ⚠️ No formal event catalog with full schema definitions                                                                           |
| **API Contract Model**                    | ❌ No OpenAPI/AsyncAPI specs exist                                  | ❌ Missing entirely — only mentioned in quality gates as TODO                                                                     |
| **Cross-Cutting Concerns Model**          | ⚠️ Security doc exists but no unified cross-cutting model           | ⚠️ Missing: observability model, error handling model, resilience model                                                           |

## User Review Required

> [!IMPORTANT]
> **Scope Decision**: This is a large undertaking. I recommend doing this in **phases**, prioritizing the highest-impact MDD gaps. Below are two scope options:

### Option A: Full MDD Standardization (Comprehensive)

All gaps above are addressed — every DD document gets component diagrams, sequence diagrams, state machines; formal API contracts are created; C4 L3 per module; domain event catalog; cross-cutting concerns model.

**Estimated effort**: ~20 files modified/created

### Option B: Core MDD Standardization (Recommended)

Focus on the most impactful gaps that bring the docs to MDD standard:

1. **Add formal UML Domain Model diagram** to `loyalty_domain.md`
2. **Add missing behavioral models** (sequence diagrams) to DD-02..DD-05
3. **Add missing state machine diagrams** for all entity lifecycles (Point, RedemptionOrder, Tier, Program, Campaign)
4. **Add component architecture diagrams** to DD-02..DD-05 (matching DD-01 standard)
5. **Create Domain Event Catalog** document
6. **Create a Model-Driven Design Index** that maps every model to its artifact

**Estimated effort**: ~12 files modified/created

## Open Questions

> [!IMPORTANT]
>
> 1. **Which scope** do you prefer — Option A (full) or Option B (core)?
> 2. Should this work go on a new branch `feature/lab4` or continue on `feature/lab3`?
> 3. Are there any specific MDD framework standards you need to follow (e.g., UML 2.5, ArchiMate, C4 model strictly)?
> 4. Should I also add **Use Case Diagrams** (UML) per module, or are the use case tables in the FR docs sufficient?

## Proposed Changes (Option B — Core MDD)

### Domain Model Enhancement

#### [MODIFY] [loyalty_domain.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/loyalty_domain.md)

- Add formal **UML Class Diagram** (Mermaid) showing all domain entities, their attributes, relationships, and multiplicities
- Add **Aggregate Boundaries** marking which entities belong to which bounded context

---

### Detailed Design — Missing Behavioral & Structural Models

#### [MODIFY] [DD-02-tiering-system.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-02-tiering-system.md)

- Add **Component Architecture Diagram** (matching DD-01 format)
- Add **Sequence Diagrams**: Tier Upgrade (FLOW-02), Tier Downgrade with Grace (FLOW-03), Batch Evaluation
- Add **State Machine Diagram**: MemberTier lifecycle (ACTIVE → IN_GRACE_PERIOD → DOWNGRADED → ACTIVE)

#### [MODIFY] [DD-03-redemption-engine.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-03-redemption-engine.md)

- Add **Component Architecture Diagram**
- Add **Sequence Diagrams**: Redemption with FIFO (FLOW-04), Fulfillment Failure Reversal (FLOW-05), Tier-Restricted Redemption (FLOW-09)
- Add **State Machine Diagram**: RedemptionOrder lifecycle (PENDING → IN_PROGRESS → FULFILLED/FAILED → REVERSED)

#### [MODIFY] [DD-04-program-management.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-04-program-management.md)

- Add **Component Architecture Diagram**
- Add **Sequence Diagrams**: Campaign Activation (FLOW-06), Rule Version Change (FLOW-07), Partner Earn (FLOW-08), Manual Adjustment (FLOW-11)
- Add **State Machine Diagrams**: Program lifecycle (DRAFT → ACTIVE → SUSPENDED → DEACTIVATED), Campaign lifecycle (DRAFT → ACTIVE → PAUSED → COMPLETED)

#### [MODIFY] [DD-05-analytics-reporting.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-05-analytics-reporting.md)

- Add **Component Architecture Diagram**
- Add **Sequence Diagrams**: Analytics Pipeline (FLOW-10), Expiry + Breakage Capture (FLOW-12)

---

### New MDD Artifacts

#### [NEW] [domain-event-catalog.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/domain-event-catalog.md)

- Formal catalog of all domain events with: event name, producer, consumer(s), payload schema, partition key, ordering guarantees, idempotency contract
- Covers all events from Architecture-Overview §5.2 Kafka topics table, expanded with full schemas

#### [NEW] [entity-lifecycle-models.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/entity-lifecycle-models.md)

- Consolidated state machine diagrams for all key entities:
  - `PointTransaction` lifecycle: PENDING → CONFIRMED → EXPIRED / REDEEMED / CANCELLED
  - `RedemptionOrder` lifecycle: PENDING → IN_PROGRESS → FULFILLED / FAILED → REVERSED / CANCELLED
  - `MemberTier` lifecycle: ACTIVE → IN_GRACE_PERIOD → DOWNGRADED
  - `LoyaltyProgram` lifecycle: DRAFT → ACTIVE → SUSPENDED → DEACTIVATED
  - `Campaign` lifecycle: DRAFT → ACTIVE → PAUSED → COMPLETED → DEACTIVATED
  - `Enrollment` lifecycle: PENDING → ACTIVE → SUSPENDED → CANCELLED
  - `FulfillmentRecord` lifecycle: PENDING → IN_PROGRESS → FULFILLED / FAILED

#### [NEW] [mdd-index.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/mdd-index.md)

- **Model-Driven Design Index**: master catalog mapping every model type to its location
- Columns: Model Type | Model Name | Document | Section | Entities Covered | Related FR/AS/QG

---

### Existing Architecture Enhancement

#### [MODIFY] [Architecture-Overview.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Architecture-Overview.md)

- Add **C4 Level 3 (Component)** diagrams for each of the 5 domain services, showing internal components and their interactions

---

## Verification Plan

### Manual Verification

- Every Mermaid diagram renders correctly
- All model references in the MDD Index resolve to actual document sections
- Traceability matrix in `traceability.md` remains consistent after changes
- All state transitions in state machine diagrams match the `status` enum values in the DDL schemas
- Sequence diagram participants align with the component architecture diagrams
- New branch `feature/lab4` passes review by stakeholder

### Consistency Checks

- State machine status values match database schema `status` column CHECK constraints
- Sequence diagram participants match C4 component names
- Domain event catalog entries match Kafka topic table in Architecture-Overview §5.2
- Entity lifecycle models cover every `status` enum value defined in the DDL
