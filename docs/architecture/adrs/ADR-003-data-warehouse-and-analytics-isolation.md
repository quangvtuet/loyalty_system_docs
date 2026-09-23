# ADR-003: Data Warehouse & Analytics Workload Isolation

**Status**: Accepted  
**Date**: 2026-08-14  
**Context**: FR-05-001..082, NFR-05-003, NFR-05-005, Quality-Gates-Architecture G01-01-004, G02-05-001  
**Deciders**: Solution Architect, Tech Lead, Data Architect

---

## 1. Context & Problem Statement

The Analytics & Reporting module must generate 7 comprehensive standard reports (including Point Liability, Breakage Forecast, and Member Engagement), power real-time operational dashboards (5-minute refresh), and support ad-hoc querying across multi-million row historical datasets.

Executing complex aggregations, table scans, and historical window functions directly on the operational transactional databases (`earning_db`, `redemption_db`) would severely degrade transactional throughput (NFR-05-005: report generation must not degrade OLTP latency by > 1%).

---

## 2. Decision Drivers

- **OLTP Protection**: Transactional processing (earning, redemption, tiering) must experience zero lock contention from reporting queries.
- **Near Real-Time Freshness**: Operational dashboard data staleness must be ≤ 10 minutes (NFR-05-003).
- **Fast Columnar Aggregations**: Large multi-dimensional reports (> 1M rows) must generate efficiently.
- **Historical Audit Retainability**: Analytical reporting data must be retained for 7 years without bloating operational OLTP tables.

---

## 3. Considered Options

1. **Read-Only Database Replicas (PostgreSQL Streaming Replication)**: Run reporting queries on asynchronous PostgreSQL read replicas of each module's database.
2. **Dedicated Star-Schema Data Warehouse via Change Data Capture (CDC)**: Stream transactional changes from all module DBs via Debezium/Kafka into a centralized columnar / analytical data warehouse.
3. **On-Demand ETL Batch Nightly Dump**: Dump OLTP databases nightly into a reporting database.

---

## 4. Decision

We choose **Option 2: Dedicated Star-Schema Data Warehouse via Change Data Capture (CDC)**.

### Architectural Pipeline
1. **CDC Engine**: Debezium PostgreSQL connector reads the write-ahead logs (WAL) of `earning_db`, `tiering_db`, `redemption_db`, and `program_mgmt_db` with near-zero overhead.
2. **Kafka Transport**: CDC events are published to `loyalty.cdc.platform_events` in streaming real-time.
3. **DW Stream Loader**: Streaming ingestion workers consume change events, denormalize relations into the Star Schema (`fact_point_transaction`, `fact_redemption_order`, `fact_enrollment`, `dim_*`), and commit batches every 60 seconds.
4. **Data Warehouse Store**: ClickHouse / PostgreSQL Analytical Replica with columnar indexing and pre-computed materialized views for 5-minute dashboard refreshes.
5. **Freshness SLA**: End-to-end data latency from OLTP commit to Data Warehouse availability is guaranteed **≤ 10 minutes** (typically < 30 seconds).

---

## 5. Consequences

### Positive
- Strict physical and performance isolation: long-running analytical queries cannot impact transactional SLAs.
- Star schema optimizes multi-dimensional slicing (by program, tier, channel, campaign, age bucket).
- Supports independent archival and WORM compliance for 7-year regulatory audits.

### Negative / Mitigations
- Introduces eventual consistency for analytics (up to 10 minutes lag). Mitigated by clearly documenting dashboard staleness expectations in UI and architecture specs.
