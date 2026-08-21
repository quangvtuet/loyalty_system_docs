# Detailed Design: Program Management (DD-04)

**Module**: Program Management  
**Version**: 1.0  
**Date**: 2026-08-14  
**Source**: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) | [FR-04](file:///d:/learn/loyalty/requirements/FR-04-program-management.md) | [AS-04](file:///d:/learn/loyalty/analytics/AS-04-program-management.md) | [Quality-Gates-Design.md](file:///d:/learn/loyalty/quality-gates/Quality-Gates-Design.md)

---

## 1. Module Overview & Responsibilities

The **Program Management** module is the central configuration and governance foundation for the Loyalty Banking platform. It administers program lifecycle (**"BankRewards 2025"**), promotional campaigns (**"Double Points August"** — 2× multiplier), rule versioning, partner onboarding & OAuth 2.0 API gateway (1,000 req/min rate limit), member enrollments, and dual-control manual balance adjustments.

---

## 2. Component Architecture

```mermaid
flowchart TB
    subgraph AdminLayer["1. Admin & Partner Ingress"]
        ADMIN_API[Program Admin REST API]
        PARTNER_GATEWAY[Partner OAuth 2.0 Gateway<br/>1,000 req/min Rate Limiter]
    end

    subgraph CoreEngines["2. Core Program Services"]
        PROG_SVC[Program Lifecycle Service]
        RULE_REG[Versioned Rule Registry Engine<br/>Prospective Versioning]
        CAMP_MGR[Campaign & Priority Engine]
        ENROLL_SVC[Member Enrollment Service]
        ADJUST_SVC[Manual Adjustment Workflow Engine<br/>Dual-Control State Machine]
    end

    subgraph DataStore["3. Persistence & Audit"]
        DB_PROG[(PostgreSQL: program_mgmt_db)]
        WORM_AUDIT[(WORM Audit Logs<br/>config_version_log & manual_adjustment_log)]
    end

    subgraph EventPub["4. Config Event Egress"]
        KAFKA_PUB[Kafka Producer<br/>loyalty.config.rule_updated]
    end

    ADMIN_API --> PROG_SVC
    ADMIN_API --> RULE_REG
    ADMIN_API --> CAMP_MGR
    ADMIN_API --> ADJUST_SVC
    PARTNER_GATEWAY --> ENROLL_SVC

    PROG_SVC --> DB_PROG
    RULE_REG --> DB_PROG
    CAMP_MGR --> DB_PROG
    ENROLL_SVC --> DB_PROG
    ADJUST_SVC --> WORM_AUDIT

    RULE_REG --> KAFKA_PUB
    CAMP_MGR --> KAFKA_PUB
```

---

## 3. Rule Versioning & Conflict Resolution

### 3.1 Prospective Rule Versioning (FR-04-004, FR-04-005)
When an admin updates an active `EarnRule` or `TierRule`:
1. The existing active rule version has its `valid_to` set to `NOW()`.
2. A new rule row is inserted with `version = previous_version + 1`, `valid_from = NOW()`, and `valid_to = NULL`.
3. A record is appended to `config_version_log` capturing `operator_id`, `previous_value`, `new_value`, and `changed_at`.
4. Historical transactions evaluated under prior timestamps remain 100% unaffected.

### 3.2 Campaign Priority Conflict Resolution (FR-04-012)
When multiple active campaigns match an earn event:
$$\text{WinningCampaign} = \arg\min_{c \in \text{MatchedCampaigns}} (c.\text{priority})$$
- Priority 1 overrides Priority 2, 3, etc.
- If stacking is enabled in program configuration: $\text{TotalBonus} = \sum \text{Bonus}(c_i)$.

---

## 4. Sequence Diagrams

### 4.1 Prospective Rule Change without Retroactive Impact (FLOW-07 / UC-04-04)

```mermaid
sequenceDiagram
    autonumber
    participant Admin as Program Admin
    participant Controller as Rule Registry Controller
    participant DB as Program DB
    participant Audit as WORM Config Version Log
    participant Broker as Kafka Broker

    Admin->>Controller: PUT /api/v1/rules/earn/{rule_id} (New Rate: 1.25 pt/$1, effective NOW)
    Controller->>DB: UPDATE earn_rule SET valid_to=NOW(), status='INACTIVE' WHERE rule_id=v1
    Controller->>DB: INSERT earn_rule (version=2, earn_rate=1.25, valid_from=NOW(), status='ACTIVE')
    Controller->>Audit: INSERT config_version_log (entity='earn_rule', old_rate=1.0, new_rate=1.25, operator_id)
    Controller->>Broker: Publish loyalty.config.rule_updated (rule_id, version=2, rate=1.25)
    Controller-->>Admin: 200 OK (Rule updated to v2. Past transactions remain at 1.0 rate)
```

### 4.2 Partner Earn Event via OAuth 2.0 Gateway (FLOW-08 / UC-04-06)

```mermaid
sequenceDiagram
    autonumber
    participant Partner as Merchant Partner System
    participant Gateway as Partner API Gateway
    participant RateLimit as Redis Rate Limiter (1,000 req/min)
    participant Earning as Earning Engine Service
    participant Ledger as Point Ledger DB

    Partner->>Gateway: POST /api/v1/partners/earn (Bearer JWT, payload: $100 spend)
    Gateway->>RateLimit: Check partner quota (< 1,000 req/min)
    RateLimit-->>Gateway: OK (Current: 42/1000)
    Gateway->>Earning: Dispatch Partner Earn Event (source_type='PARTNER', partner_id)
    Earning->>Ledger: Append point_transaction (100 pts, source_type='PARTNER')
    Earning-->>Gateway: Transaction confirmed
    Gateway-->>Partner: 201 Created (transaction_id, points_credited=100)
```

### 4.3 High-Value Manual Adjustment with Dual Approval (FLOW-11 / UC-04-07)

```mermaid
sequenceDiagram
    autonumber
    participant Operator as Support Agent (Operator)
    participant Supervisor as Admin / Supervisor
    participant AdjustSvc as Manual Adjustment Service
    participant Audit as WORM Adjustment Log
    participant Ledger as Earning Ledger Service

    Operator->>AdjustSvc: POST /api/v1/adjustments (member_id, amount=+10,000 pts, reason="Service Recovery")
    AdjustSvc->>AdjustSvc: Check threshold (10,000 pts > 5,000 dual-control threshold)
    AdjustSvc->>Audit: INSERT manual_adjustment_log (status='PENDING_APPROVAL', operator_id)
    AdjustSvc-->>Operator: 202 Accepted (adjustment_id, status='PENDING_APPROVAL')

    Supervisor->>AdjustSvc: POST /api/v1/adjustments/{id}/approve (supervisor_id != operator_id)
    AdjustSvc->>Ledger: Execute Adjustment Debit/Credit (+10,000 pts to member balance)
    AdjustSvc->>Audit: UPDATE manual_adjustment_log SET status='APPROVED', approver_id=supervisor_id, approved_at=NOW()
    AdjustSvc-->>Supervisor: 200 OK (Adjustment committed to point ledger)
```
