# Detailed Design: Redemption Engine (DD-03)

**Module**: Redemption Engine  
**Version**: 1.0  
**Date**: 2026-08-14  
**Source**: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) | [FR-03](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md) | [AS-03](file:///d:/learn/loyalty/analytics/AS-03-redemption-engine.md) | [Quality-Gates-Design.md](file:///d:/learn/loyalty/quality-gates/Quality-Gates-Design.md)

---

## 1. Module Overview & Responsibilities

The **Redemption Engine** manages the reward catalog, validates member balance and tier eligibility, executes atomic FIFO point debits, dispatches orders to fulfillment partners, and manages automated reversals that restore original point earn dates and expiry positions.

### Key Operational Constants
- **Redemption Rate**: **100 points = $1 redemption value**
- **Minimum Redemption**: **100 points per transaction**
- **Cash-Back SLA**: Account credit initiated **≤ 1 business day** of order fulfillment

---

## 2. Component Architecture

```mermaid
flowchart TB
    subgraph API["1. API Layer"]
        CATALOG_CTRL[Catalog API Controller]
        ORDER_CTRL[Redemption Order Controller]
    end

    subgraph Validation["2. Validation & Concurrency"]
        TIER_VAL[Tier Access Validator]
        BAL_LOCK[Redis Distributed Lock<br/>key: lock:member:bal:UUID]
    end

    subgraph FIFODebit["3. FIFO Debit Engine"]
        LEDGER_CLIENT[Earning Ledger Client]
        FIFO_BATCH_RESOLVER[FIFO Batch Allocator<br/>Consumes Oldest Earn Date First]
    end

    subgraph Fulfillment["4. Fulfillment & Reversal"]
        ORDER_MGR[Redemption Order State Machine]
        DISPATCHER[Partner Fulfillment Dispatcher]
        REVERSAL_SVC[Auto-Reversal Handler]
        DB_RED[(PostgreSQL: redemption_db)]
    end

    ORDER_CTRL --> BAL_LOCK
    BAL_LOCK --> TIER_VAL
    TIER_VAL --> FIFO_BATCH_RESOLVER
    FIFO_BATCH_RESOLVER --> LEDGER_CLIENT
    LEDGER_CLIENT --> ORDER_MGR
    ORDER_MGR --> DB_RED
    ORDER_MGR --> DISPATCHER
    DISPATCHER -->|On Failure Callback| REVERSAL_SVC
    REVERSAL_SVC --> LEDGER_CLIENT
```

---

## 3. FIFO Batch Consumption & Restoration Algorithm

### 3.1 FIFO Debit Allocation
When debiting $P$ points for a member:
1. Lock member balance: `SET lock:member:bal:{member_id} {order_id} NX EX 10`.
2. Query confirmed unexpired batches:
   ```sql
   SELECT transaction_id, remaining_balance, earn_date, expiry_date 
   FROM point_transaction 
   WHERE member_id = :member_id AND status = 'CONFIRMED' AND remaining_balance > 0 
   ORDER BY earn_date ASC 
   FOR UPDATE;
   ```
3. Loop through batches in ascending `earn_date` order until $\sum \text{debit} = P$.
4. Mark batch amounts as `PENDING_DEBIT`.
5. Release distributed lock.

### 3.2 Reversal & FIFO Restoration (FR-03-041)
If fulfillment returns `FAILED`:
- Do **not** create a new earn date.
- Update the original batch entries: `remaining_balance = remaining_balance + debit_amount`.
- Append an audit reversal transaction to the ledger with `type='REVERSAL'` linked to `order_id`.
- The member's original FIFO queue position and expiry schedule are preserved exactly.

---

## 4. Sequence Diagrams

### 4.1 Successful Redemption with FIFO Point Consumption (FLOW-04 / UC-03-02)

```mermaid
sequenceDiagram
    autonumber
    participant Member as Member Mobile App
    participant Controller as Redemption Order Controller
    participant Lock as Redis Distributed Lock
    participant Ledger as Earning Ledger Service
    participant DB as Redemption DB
    participant Partner as Partner Fulfillment System

    Member->>Controller: POST /api/v1/redemptions/orders (item_id=voucher-300, qty=1, 300 pts)
    Controller->>Lock: Acquire lock:member:bal:{member_id}
    Lock-->>Controller: Lock Acquired (TTL: 10s)

    Controller->>Ledger: Query & Allocate FIFO Batches (300 pts required)
    Note over Ledger: Batch 1 (Jan 10): 200 pts available (consumed completely)<br/>Batch 2 (Mar 15): 300 pts available (50 pts consumed)
    Ledger->>Ledger: Set 250 pts to PENDING_DEBIT in ledger
    Ledger-->>Controller: Batches Reserved Successfully

    Controller->>DB: INSERT redemption_order (order_id, status='PENDING', 300 pts)
    Controller->>Lock: Release lock:member:bal:{member_id}
    Controller-->>Member: 201 Created (order_id, status='PENDING')

    Controller->>Partner: Dispatch Fulfillment Request (Digital Voucher Delivery)
    Partner-->>Controller: Webhook: Fulfillment SUCCESS (voucher_code_hash)
    Controller->>DB: UPDATE redemption_order SET status='FULFILLED'
    Controller->>Ledger: Confirm Debit: Transition PENDING_DEBIT -> CONFIRMED_DEBIT
```

### 4.2 Fulfillment Failure & Automatic Reversal (FLOW-05 / UC-03-06)

```mermaid
sequenceDiagram
    autonumber
    participant Partner as Partner Fulfillment Gateway
    participant OrderService as Redemption Service
    participant Ledger as Earning Ledger Service
    participant DB as Redemption DB
    participant Notif as Notification Service

    Partner->>OrderService: Callback: Fulfillment FAILED (Reason: OUT_OF_STOCK)
    OrderService->>DB: UPDATE redemption_order SET status='FAILED', failure_reason='OUT_OF_STOCK'
    
    OrderService->>Ledger: Reversal Request (order_id, 300 pts)
    Ledger->>Ledger: Restore remaining_balance on Jan 10 (+200 pts) and Mar 15 (+50 pts) batches
    Ledger->>Ledger: INSERT point_transaction (type='REVERSAL', amount=300, status='CONFIRMED')
    
    OrderService->>DB: UPDATE redemption_order SET status='REVERSED'
    OrderService->>Notif: Send Reversal Alert: "Reward delivery failed. 300 points restored with original expiry."
```

### 4.3 Tier-Restricted Catalog Check (FLOW-09 / UC-03-02)

```mermaid
sequenceDiagram
    autonumber
    participant Member as Member (Silver Tier)
    participant Controller as Redemption Controller
    participant TierSvc as Tiering System API

    Member->>Controller: POST /api/v1/redemptions/orders (item_id=platinum-lounge-pass)
    Controller->>TierSvc: GET /api/v1/members/{member_id}/tier
    TierSvc-->>Controller: 200 OK (current_tier='SILVER')
    Controller->>Controller: Validate: Item requires 'PLATINUM' > Member 'SILVER'
    Controller-->>Member: 403 Forbidden (code: ERR_RED_TIER_ELIGIBILITY_FAILED)
```
