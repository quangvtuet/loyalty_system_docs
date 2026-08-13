# Loyalty Banking — Domain Summary

## Domain Overview

**Loyalty Banking** is a financial-services loyalty platform that manages the full lifecycle of customer loyalty points — from earning and tier progression to redemption and reporting. It sits at the intersection of **retail banking**, **rewards programs**, and **customer engagement**, enabling banks and financial institutions to incentivize customer behavior, deepen relationships, and drive product usage.

---

## Scope Summary

| # | Module | Purpose |
|---|--------|---------|
| 1 | Earning Engine | Calculates and awards points based on customer transactions and activities |
| 2 | Tiering System | Classifies customers into loyalty tiers based on accumulated value or activity |
| 3 | Redemption Engine | Processes point redemption requests against available reward catalogs |
| 4 | Program Management | Configures and administers loyalty programs, campaigns, and rules |
| 5 | Analytics & Reporting | Provides insights on program performance, customer behavior, and point liability |

---

## 1. Earning Engine

### Purpose
Automatically calculates and credits loyalty points to a customer's account based on qualifying transactions or activities.

### Key Concepts
- **Earn Rate**: Points awarded per unit of spend (e.g., 1 point per $1 spent).
- **Earn Event**: Any qualifying activity that triggers point accrual (purchase, transfer, bill payment, login milestone, etc.).
- **Bonus Campaign**: A time-limited multiplier or flat bonus applied on top of the base earn rate (e.g., 3x points on weekends).
- **Expiry**: Points may carry an expiry date, either rolling (e.g., 12 months from earn date) or fixed (e.g., end of calendar year).
- **Pending vs. Confirmed Points**: Points may be held in a pending state until a transaction is fully settled, then confirmed/credited.

### Key Entities
| Entity | Description |
|--------|-------------|
| `EarnRule` | Defines earn rate, eligible transaction types, channels, and validity period |
| `PointTransaction` | Ledger record of each point credit/debit event |
| `Member` | The loyalty account holder |
| `TransactionEvent` | Source event from core banking (e.g., purchase, payment) |

### Business Rules
- Points are only awarded for **settled** transactions.
- Bonus rules are evaluated **after** the base earn rule.
- If multiple bonus rules apply, the **highest multiplier** wins (or rules are stacked, depending on program config).
- Points are rounded **down** to the nearest integer.
- Earn events must be **idempotent** — the same source transaction must not credit points more than once.

### Flow
```
Transaction Event -> Earn Rule Evaluation -> Bonus Rule Evaluation
  -> Point Calculation -> Point Ledger Credit -> Expiry Scheduling
```

---

## 2. Tiering System

### Purpose
Segments customers into loyalty tiers (e.g., Silver, Gold, Platinum) based on their cumulative qualifying activity over a defined period. Tiers unlock benefits and influence earn rates.

### Key Concepts
- **Qualifying Points (QP)**: A separate metric used solely for tier calculation — not the same as redeemable points.
- **Tier Period**: The window (e.g., calendar year or rolling 12 months) over which QP are accumulated.
- **Tier Threshold**: The minimum QP required to reach or maintain a tier.
- **Tier Upgrade**: When a member crosses the threshold for a higher tier.
- **Tier Downgrade**: When a member fails to maintain the QP required for their current tier at evaluation time.
- **Grace Period**: A buffer period after the tier period ends during which a member retains their current tier before downgrade takes effect.

### Key Entities
| Entity | Description |
|--------|-------------|
| `Tier` | Definition of a tier (name, threshold, benefits) |
| `TierRule` | Configuration of evaluation frequency, QP source, grace periods |
| `MemberTier` | Current and previous tier for a member, with effective dates |
| `TierEvaluationLog` | Audit record of each tier recalculation |

### Business Rules
- Tier upgrades take effect **immediately** when the threshold is crossed.
- Tier downgrades take effect only **after** the evaluation cycle ends (+ grace period).
- QP do **not** expire mid-tier-period; they reset at the start of each new period.
- A member cannot skip tiers downward — they downgrade one level at a time per cycle (program-configurable).

### Tier Benefit Examples
| Tier | Example Benefits |
|------|-----------------|
| Silver | 1x base earn rate, standard support |
| Gold | 1.5x earn rate, priority support, lounge access |
| Platinum | 2x earn rate, concierge, exclusive offers |

### Flow
```
Qualifying Transaction -> QP Accrual -> Periodic Tier Evaluation
  -> Threshold Check -> Tier Upgrade / Maintain / Downgrade
  -> Benefit Activation -> Member Notification
```

---

## 3. Redemption Engine

### Purpose
Enables members to exchange their confirmed loyalty points for rewards — cash back, vouchers, merchandise, travel, charity donations, or statement credits.

### Key Concepts
- **Redemption Rate**: Points required per reward unit (e.g., 100 points = $1 value).
- **Redemption Catalog**: The collection of available reward options (items, partners, categories).
- **Redemption Request**: A member-initiated request to exchange points for a specific reward.
- **Fulfillment**: The actual delivery of the reward to the member (digital code, physical shipment, account credit).
- **Minimum Redemption**: The lowest number of points that can be redeemed in a single transaction.
- **Reversal**: Cancellation of a redemption and re-crediting of points to the member's account.

### Key Entities
| Entity | Description |
|--------|-------------|
| `RewardItem` | A redeemable reward in the catalog |
| `RedemptionOrder` | A member's redemption request, including status lifecycle |
| `PointLedger` | Master ledger of all point movements (earn + redeem + adjust + expire) |
| `FulfillmentRecord` | Tracks delivery status of the redeemed reward |

### Business Rules
- A member cannot redeem more points than their **confirmed available balance**.
- Points are **debited immediately** on redemption request approval.
- If fulfillment fails, points must be **reversed** back to the member's account.
- Expired points cannot be redeemed (FIFO expiry applied during debit).
- Some rewards may require a **minimum tier** (e.g., Platinum-only catalog items).

### Redemption Lifecycle
```
Redemption Request -> Balance Validation -> Point Debit (Pending)
  -> Catalog Fulfillment -> Delivery Confirmation -> Point Debit (Confirmed)
  [On Failure] -> Reversal -> Points Re-credited
```

---

## 4. Program Management

### Purpose
Provides the administrative layer for configuring, launching, and maintaining loyalty programs, including campaign setup, rule management, partner integration, and member enrollment.

### Key Concepts
- **Loyalty Program**: The top-level container defining the overall structure, currency, and rules (e.g., "BankRewards 2025").
- **Campaign**: A time-bound promotional configuration that modifies earn/redeem behavior (e.g., "Double Points in August").
- **Enrollment**: The process by which a customer opts into a loyalty program.
- **Partner Integration**: Third-party merchants or service providers that participate in the ecosystem (earn at partner, redeem with partner).
- **Rule Engine**: The configurable logic layer that evaluates earn/tier/redeem rules without code changes.

### Key Entities
| Entity | Description |
|--------|-------------|
| `LoyaltyProgram` | Top-level program definition (name, currency, dates, terms) |
| `Campaign` | Time-limited promotional rule set |
| `EarnRule` / `TierRule` / `RedemptionRule` | Configurable rule records per program |
| `Partner` | External participant in the program ecosystem |
| `Enrollment` | Member-to-program join record with effective date and status |

### Business Rules
- A member can be enrolled in **multiple programs** simultaneously, subject to program eligibility rules.
- Campaigns must define a **priority** to resolve conflicts when multiple campaigns apply.
- Program configuration changes are **versioned** — existing member balances and tier status are unaffected by retroactive rule changes.
- Partner integrations use **standardized APIs** for earn/redeem event exchange.

### Admin Capabilities
- Create / update / deactivate programs and campaigns
- Define and publish reward catalog items
- Manage partner onboarding and API credentials
- Adjust member point balances (manual adjustment with audit trail)
- Configure notifications and member communications

---

## 5. Analytics and Reporting

### Purpose
Delivers data-driven insights into loyalty program performance, member behavior, point liability, and business impact. Supports both operational monitoring and strategic decision-making.

### Key Concepts
- **Point Liability**: The financial obligation represented by unspent points on the balance sheet (outstanding points x redemption value).
- **Breakage Rate**: The percentage of earned points that expire without being redeemed (a source of program revenue).
- **Redemption Rate**: The percentage of earned points that are redeemed by members.
- **Active Members**: Members who have performed at least one qualifying activity in the reporting period.
- **Tier Distribution**: Breakdown of the member base across each tier level.

### Key Reports
| Report | Description |
|--------|-------------|
| Point Issuance Report | Total points earned by members over a period, by program/campaign/channel |
| Redemption Report | Total points redeemed, reward categories, and fulfillment status |
| Point Liability Report | Current outstanding balance x redemption rate = liability in currency |
| Tier Movement Report | Upgrades, downgrades, and member distribution per tier |
| Expiry Report | Points expiring in upcoming periods; breakage forecast |
| Member Engagement Report | Activity rates, earn frequency, and dormancy indicators |
| Campaign Performance Report | Incremental activity and cost-per-point for each campaign |

### Key Metrics (KPIs)
| KPI | Formula / Definition |
|-----|---------------------|
| **Earn Rate** (actual) | Total points issued / Total qualifying spend |
| **Redemption Rate** | Points redeemed / Points issued |
| **Breakage Rate** | Points expired / Points issued |
| **Active Member Rate** | Active members / Total enrolled members |
| **Point Liability** | Outstanding points x cost-per-point |
| **Program ROI** | Incremental revenue attributable to loyalty / Program cost |

### Data Sources
- Core banking transaction feed (for earn events)
- CRM / member profile data
- Redemption fulfillment system
- Campaign management system

---

## Domain Glossary

| Term | Definition |
|------|-----------|
| **Points** | The loyalty currency issued to members for qualifying activity |
| **Qualifying Points (QP)** | Points used exclusively for tier calculation |
| **Earn Event** | A transaction or activity that triggers point accrual |
| **Tier** | A membership level that grants specific benefits |
| **Breakage** | Points that expire without being redeemed |
| **Point Liability** | Financial value of unspent points on the books |
| **Redemption** | Exchange of points for a reward |
| **Fulfillment** | Delivery of a reward after redemption |
| **Campaign** | A time-limited promotional rule overlay |
| **Enrollment** | A member's opt-in to a loyalty program |
| **Partner** | Third-party participant in the earn/redeem ecosystem |
| **FIFO Expiry** | First-in, first-out expiry — oldest points are consumed first |
| **Pending Points** | Points awaiting transaction settlement before confirmation |
| **Grace Period** | Buffer window before a tier downgrade takes effect |

---

*Source: [loyalty.md](file:///d:/learn/loyalty/loyalty.md) — Domain: Loyalty Banking, Scope: Earning Engine, Tiering System, Redemption Engine, Program Management, Analytics & Reporting*
