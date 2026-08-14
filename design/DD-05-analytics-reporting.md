# Detailed Design: Analytics & Reporting (DD-05)

**Module**: Analytics & Reporting  
**Version**: 1.0  
**Date**: 2026-08-14  
**Source**: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) | [FR-05](file:///d:/learn/loyalty/requirements/FR-05-analytics-reporting.md) | [AS-05](file:///d:/learn/loyalty/analytics/AS-05-analytics-reporting.md) | [Quality-Gates-Design.md](file:///d:/learn/loyalty/quality-gates/Quality-Gates-Design.md)

---

## 1. Module Overview & Responsibilities

The **Analytics & Reporting** module delivers cross-platform business intelligence, operational alerting, and regulatory reporting for the Loyalty Banking platform. It computes financial point liability, breakage forecasts, campaign ROI lift, and member engagement metrics while maintaining strict physical workload isolation from the transactional databases.

---

## 2. Component Architecture

```mermaid
flowchart TB
    subgraph Ingestion["1. Streaming CDC Ingestion"]
        CDC_DEBEZIUM[Debezium CDC Connector<br/>PostgreSQL WAL Reader]
        KAFKA_STREAM[Kafka Topic: loyalty.cdc.platform_events]
        ETL_WORKER[Stream Processing Workers<br/>Transforms & Star Schema Loader]
    end

    subgraph Storage["2. Analytics Data Warehouse"]
        DW_FACT[(Fact Tables: point_transaction, redemption_order)]
        DW_DIM[(Dimension Tables: member, program, campaign, date, tier)]
        MAT_VIEWS[(Pre-Aggregated Materialized Views<br/>5-Min Refresh)]
    end

    subgraph ServiceLayer["3. Reporting & Alert Services"]
        REPORT_GEN[Report Generation Engine<br/>CSV / XLSX / PDF Exporter]
        ALERT_MONITOR[Real-Time Anomaly & Threshold Monitor<br/>5-Min Poller]
        RBAC_GUARD[RBAC Program-Level Scoping Guard]
    end

    subgraph Egress["4. Delivery Channels"]
        DASHBOARD[Operational & Executive Dashboards]
        EMAIL_SFTP[Email & SFTP Scheduled Delivery]
        ALERT_PAGER[PagerDuty / Ops Alerts]
    end

    CDC_DEBEZIUM --> KAFKA_STREAM
    KAFKA_STREAM --> ETL_WORKER
    ETL_WORKER --> DW_FACT
    ETL_WORKER --> DW_DIM
    DW_FACT --> MAT_VIEWS
    DW_DIM --> MAT_VIEWS

    MAT_VIEWS --> REPORT_GEN
    MAT_VIEWS --> ALERT_MONITOR
    REPORT_GEN --> RBAC_GUARD
    RBAC_GUARD --> DASHBOARD
    RBAC_GUARD --> EMAIL_SFTP
    ALERT_MONITOR --> ALERT_PAGER
```

---

## 3. Core KPI Mathematical Formulations

### 3.1 Financial Point Liability & Aging Buckets
$$\text{PointLiability} = \sum (\text{confirmed\_unspent\_points}) \times \text{cost\_per\_point}$$
Where default $\text{cost\_per\_point} = \$0.010000$ for BankRewards 2025.

#### Aging Breakdown
- **0–3 Months**: $\sum \text{balance}(\text{earn\_date} \ge \text{TODAY} - 90)$
- **3–6 Months**: $\sum \text{balance}(\text{earn\_date} \in [\text{TODAY}-180, \text{TODAY}-91])$
- **6–12 Months**: $\sum \text{balance}(\text{earn\_date} \in [\text{TODAY}-365, \text{TODAY}-181])$
- **> 12 Months**: $\sum \text{balance}(\text{earn\_date} < \text{TODAY}-365)$

### 3.2 Program Health KPIs
- **Redemption Rate**: $\frac{\text{Total Points Redeemed}}{\text{Total Points Issued}} \times 100$
- **Breakage Rate**: $\frac{\text{Total Points Expired}}{\text{Total Points Issued}} \times 100$
- **Active Member Rate**: $\frac{\text{Count}(\text{Members with } \ge 1 \text{ activity})}{\text{Total Enrolled Members}} \times 100$ (Dormant = $> 90$ days no activity)
- **Campaign Earn Lift**: $\left( \frac{\text{Points Earned During Campaign}}{\text{30-Day Pre-Campaign Baseline}} - 1 \right) \times 100$

---

## 4. Sequence Diagrams

### 4.1 Change Data Capture (CDC) to Data Warehouse (FLOW-10)

```mermaid
sequenceDiagram
    autonumber
    participant EarningDB as Earning Engine PostgreSQL
    participant Debezium as Debezium CDC Connector
    participant Kafka as Kafka Event Broker
    participant StreamWorker as DW Stream Loader
    participant DW as Data Warehouse (Star Schema)

    EarningDB->>EarningDB: Commit INSERT point_transaction (EARN, 100 pts)
    Debezium->>EarningDB: Read WAL change stream (near-zero overhead)
    Debezium->>Kafka: Publish loyalty.cdc.platform_events (record payload, ts)
    StreamWorker->>Kafka: Consume change event batch
    StreamWorker->>StreamWorker: Enrich with dim_date, dim_member, dim_program keys
    StreamWorker->>DW: Micro-batch INSERT INTO fact_point_transaction
    Note over EarningDB,DW: End-to-end data latency commit -> DW is < 30 seconds (SLA ≤ 10 min)
```

### 4.2 Scheduled Report Generation & Delivery (UC-05-09)

```mermaid
sequenceDiagram
    autonumber
    participant Scheduler as Quartz Report Scheduler (Mon 08:00)
    participant Engine as Report Generation Engine
    participant RBAC as Program RBAC Guard
    participant DW as Data Warehouse
    participant Mailer as Email / SFTP Gateway

    Scheduler->>Engine: Trigger Scheduled Job: Weekly Redemption Report
    Engine->>RBAC: Validate user authorizations for program scope
    RBAC-->>Engine: Authorized Programs: [BankRewards 2025]
    Engine->>DW: Execute aggregated query on fact_redemption_order
    DW-->>Engine: Result dataset (order totals, reversals, SLA metrics)
    Engine->>Engine: Format and render CSV / PDF document
    Engine->>Mailer: Deliver report to configured email distribution list
```

### 4.3 Real-Time Operational Dashboard Liability Alert (UC-05-08)

```mermaid
sequenceDiagram
    autonumber
    participant Poller as Operational Alert Monitor (Every 5 min)
    participant DW as DW Materialized Views
    participant Dashboard as Admin Real-Time Dashboard
    participant Pager as Alert Notification Gateway

    Poller->>DW: SELECT current_liability FROM mv_realtime_kpis WHERE program_id = :id
    DW-->>Poller: current_liability = $1,250,000 (Configured Threshold: $1,000,000)
    Poller->>Dashboard: Push WebSocket Event: "CRITICAL ALERT: Point liability exceeded ceiling"
    Poller->>Pager: Trigger High-Priority Alert to Finance Team & CFO
```
