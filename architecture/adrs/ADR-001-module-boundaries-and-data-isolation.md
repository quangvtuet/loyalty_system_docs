# ADR-001: Module Boundaries & Database-per-Service Isolation

**Status**: Accepted  
**Date**: 2026-08-14  
**Context**: Architecture Gate G01-01-002, Quality Gates Architecture  
**Deciders**: Solution Architect, Tech Lead, Data Architect

---

## 1. Context & Problem Statement

The Loyalty Banking system encompasses 5 distinct business domains: Earning Engine, Tiering System, Redemption Engine, Program Management, and Analytics & Reporting. In legacy loyalty systems, a monolithic shared database often leads to tight coupling, race conditions across balance updates, and schema locks during batch evaluations or reporting queries.

We must decide on the architectural boundary pattern and database ownership model for the Loyalty Banking services.

---

## 2. Decision Drivers

- **Domain Autonomy**: Each module must be able to evolve its internal entities without breaking other modules.
- **Audit & Financial Integrity**: Point ledger entries and tier history require strict append-only and WORM audit guarantees.
- **Performance & Scaling**: Earning ingestion (≥ 500 events/sec) must not suffer lock contention from heavy redemption debits or batch tier evaluation jobs.
- **Zero Cross-DB Pollution**: Direct cross-schema joins must be prevented to avoid hidden architectural dependencies.

---

## 3. Considered Options

1. **Shared Monolithic Database (Single Schema)**: All modules share one relational database instance and schema.
2. **Shared Database Instance with Logical Schema Separation**: Single PostgreSQL instance with separate database schemas per module.
3. **Database-per-Service (Isolated Physical / Logical Instances)**: Complete encapsulation where each service owns its private database and exposes domain operations exclusively via APIs and event streams.

---

## 4. Decision

We choose **Option 3: Database-per-Service Isolation**.

Each of the 5 modules owns its dedicated PostgreSQL database schema:
- `earning_db`: Owns `point_transaction`, `earn_rule`, `earn_event_log`.
- `tiering_db`: Owns `member_tier`, `qp_ledger`, `tier_rule`, `tier_evaluation_log`.
- `redemption_db`: Owns `reward_item`, `redemption_order`, `fulfillment_record`.
- `program_mgmt_db`: Owns `loyalty_program`, `campaign`, `partner`, `enrollment`, `config_version_log`, `manual_adjustment_log`.
- `analytics_dw`: Dedicated star-schema data warehouse.

### Rules of Engagement
- No direct SQL queries, foreign keys, or cross-database transactions between module databases.
- Cross-module data access is conducted via synchronous REST/gRPC APIs or asynchronous event streams over Apache Kafka.

---

## 5. Consequences

### Positive
- Independent schema migrations without coordinated downtime across teams.
- Granular connection pooling and database resource tuning (e.g., write-heavy optimizations for Earning vs. read-heavy for Catalog).
- Enforces loose coupling and bounded contexts aligned with Domain-Driven Design (DDD).

### Negative / Mitigations
- Lack of distributed foreign keys is mitigated by domain event eventual consistency and Saga patterns.
- Cross-module data aggregation for reports is resolved by CDC streaming into the dedicated Data Warehouse (see ADR-003).
