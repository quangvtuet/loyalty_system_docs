# Detailed Design: Tiering System (DD-02)

**Module**: Tiering System  
**Version**: 1.0  
**Date**: 2026-08-14  
**Source**: [loyalty_domain.md](../lab2-requirements.md) | [FR-02](../lab2-requirements.md) | [AS-02](../lab2-requirements.md) | [Quality-Gates-Design.md](../quality-gates/Quality-Gates-Design.md)

---

## 1. Module Overview & Responsibilities

The **Tiering System** segments members into loyalty tiers (**Silver, Gold, Platinum**) based on Qualifying Points (QP) accumulated over a tier period (default: calendar year 1 Jan – 31 Dec). It evaluates real-time upgrades on every transaction, executes end-of-period batch evaluation jobs, manages 30-day grace periods, and coordinates benefit activations across downstream modules.

---

## 2. Component Architecture

```mermaid
flowchart TB
    subgraph Ingestion["1. QP Accrual Ingestion"]
        QP_CONSUMER[Kafka Consumer<br/>topic: loyalty.earning.qp_accrued]
        QP_LEDGER_SVC[QP Ledger Writer Service]
        DB_QP[(PostgreSQL: qp_ledger)]
    end

    subgraph RealTime["2. Real-Time Upgrade Engine"]
        UPGRADE_EVAL[Real-Time Threshold Checker<br/>SLA: ≤ 500ms]
        TIER_RULES_CACHE[Tier Rules In-Memory Cache<br/>Silver: 0, Gold: 1000, Plat: 3000]
    end

    subgraph BatchEngine["3. Periodic Evaluation & Grace Engine"]
        BATCH_JOB[End-of-Period Batch Evaluator<br/>SLA: ≤ 4 hours for 1M members]
        GRACE_MGR[30-Day Grace Period State Machine]
        DB_TIER[(PostgreSQL: member_tier)]
    end

    subgraph Dispatch["4. Notification & Downstream Sync"]
        EVENT_PUB[Kafka Producer<br/>loyalty.tiering.tier_changed]
        NOTIF_SVC[Member Notification Dispatcher]
    end

    QP_CONSUMER --> QP_LEDGER_SVC
    QP_LEDGER_SVC --> DB_QP
    QP_LEDGER_SVC --> UPGRADE_EVAL
    TIER_RULES_CACHE --> UPGRADE_EVAL
    UPGRADE_EVAL -->|Threshold Crossed| DB_TIER
    UPGRADE_EVAL -->|Upgrade Event| EVENT_PUB
    UPGRADE_EVAL -->|Upgrade Event| NOTIF_SVC

    BATCH_JOB --> DB_QP
    BATCH_JOB --> GRACE_MGR
    GRACE_MGR --> DB_TIER
    GRACE_MGR -->|Pending Downgrade Alert| NOTIF_SVC
    GRACE_MGR -->|Confirmed Downgrade| EVENT_PUB
```

---

## 3. Tier Thresholds & Benefit Matrix

| Tier Name | QP Threshold | Earn Multiplier | Catalog Access Tier | Grace Period | Downgrade Policy |
|---|---|---|---|---|---|
| **Silver** | **0 QP** (Base tier) | **1.0×** | Standard Catalog | N/A | Base tier (cannot downgrade) |
| **Gold** | **1,000 QP** | **1.5×** | Gold + Standard Catalog | 30 Days | 1 level per cycle (Gold → Silver) |
| **Platinum** | **3,000 QP** | **2.0×** | Platinum Exclusive Catalog | 30 Days | 1 level per cycle (Platinum → Gold) |

---

## 4. Grace Period State Machine

```mermaid
stateDiagram-v2
    [*] --> EVALUATING: End-of-Period Job (31 Dec 23:59)
    
    state ThresholdCheck <<choice>>
    EVALUATING --> ThresholdCheck

    ThresholdCheck --> TIER_MAINTAINED: QP ≥ Maintenance Threshold
    ThresholdCheck --> GRACE_PERIOD_ACTIVE: QP < Maintenance Threshold

    TIER_MAINTAINED --> RESET_QP: Reset QP=0 for New Year
    
    GRACE_PERIOD_ACTIVE --> RESCUED: Member earns QP Shortfall within 30 Days
    GRACE_PERIOD_ACTIVE --> DOWNGRADED: 30 Days Expire without QP Shortfall Met

    RESCUED --> RESET_QP: Downgrade Cancelled, Tier Retained
    DOWNGRADED --> RESET_QP: Step-down 1 Tier Level, Tier Retained
    
    RESET_QP --> [*]
```

---

## 5. Sequence Diagrams

### 5.1 Real-Time Tier Upgrade (FLOW-02 / UC-02-02)

```mermaid
sequenceDiagram
    autonumber
    participant EE as Earning Engine
    participant Consumer as Tiering System Consumer
    participant QPLedger as QP Ledger DB
    participant UpgradeEval as Real-Time Upgrade Evaluator
    participant TierDB as Member Tier DB
    participant Broker as Kafka Broker
    participant Notif as Notification Service

    EE->>Consumer: Event: loyalty.earning.qp_accrued (Member Silver with 900 QP earns +200 QP)
    Consumer->>QPLedger: INSERT qp_ledger (amount=200, cumulative=1100 QP)
    Consumer->>UpgradeEval: EvaluateTier(member_id, cumulative_qp=1100)
    UpgradeEval->>UpgradeEval: Check Thresholds (Gold=1000 QP) -> Upgrade Warranted!
    
    UpgradeEval->>TierDB: UPDATE member_tier SET current_tier='GOLD', previous_tier='SILVER', updated_at=NOW()
    UpgradeEval->>Broker: Publish loyalty.tiering.tier_changed (member_id, old='SILVER', new='GOLD', multiplier=1.5)
    UpgradeEval->>Notif: Dispatch Tier Upgrade Notification (Email/SMS/Push)
    
    Note over EE,UpgradeEval: Total real-time upgrade execution completed in < 120ms (SLA ≤ 500ms)
```

### 5.2 End-of-Period Tier Downgrade & 30-Day Grace Rescue (FLOW-03 / UC-02-04)

```mermaid
sequenceDiagram
    autonumber
    participant Batch as Tier Batch Evaluator (31 Dec)
    participant TierDB as Member Tier DB
    participant Notif as Notification Service
    participant EE as Earning Engine
    participant Broker as Kafka Broker

    Batch->>TierDB: Query members below threshold (Platinum member with 1,500 QP, threshold 3,000)
    Batch->>TierDB: UPDATE member_tier SET status='IN_GRACE_PERIOD', grace_period_end=NOW() + 30 DAYS
    Batch->>Notif: Send Grace Alert: "Pending Downgrade to Gold in 30 Days. Shortfall: 1,500 QP"

    Note over TierDB,Notif: Day 15 of Grace Period: Member performs high-value transaction earning 1,600 QP

    EE->>Batch: Event: loyalty.earning.qp_accrued (member_id, +1600 QP, total=3100 QP)
    Batch->>TierDB: Check active grace period: Cumulative QP 3,100 ≥ 3,000 threshold
    Batch->>TierDB: UPDATE member_tier SET status='ACTIVE', grace_period_end=NULL (Grace Rescued)
    Batch->>Notif: Send Confirmation: "Grace period rescue successful! Platinum tier maintained."
```
