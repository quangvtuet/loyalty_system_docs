# Banking Loyalty System — ArchiMate 3.2 Enterprise Architecture Specification

> **Not a Lab 8 view — outside the modeling pack.**
> The four Lab 8 ArchiMate views are in [`../lab8-archimate-views.md`](../lab8-archimate-views.md).
> This file was drawn before the Guide was adopted. It has no header and no RACI, it uses the old container names, and it is kept only as before-pack material. It is not submitted and not reviewed.


**Domain**: Banking Loyalty Platform (Enterprise Financial Customer Engagement System)  
**Standard**: The Open Group ArchiMate® 3.2 Specification & C4 Model Container Layout  
**Version**: 3.2 (Production Release)  

---

## 📚 1. Architecture References & Documentation Catalog

| Document / Asset | Category | Key Contents & Architecture Scope | Repository Link |
|---|---|---|---|
| **C4 Level 1: System Context Diagram** | *C4 Model* | High-level system context diagram showing customer channels, core banking, partners, and external services. | [context.png](C4/context.png) |
| **C4 Level 2: Container Diagram** | *C4 Model* | Container architecture decomposition into API Gateway, 5 microservices, Kafka event bus, and polyglot DBs. | [container.png](C4/container.png) |
| **C4 Level 3: Component Diagram** | *C4 Model* | Internal component diagram showing Spring Boot controllers, domain rules, Kafka listeners, and Redlock managers. | [component.png](C4/component.png) |
| **Loyalty Domain Specification** | *Domain Rules* | Base earn rates, tier multipliers (Silver 1.0x, Gold 1.5x, Platinum 2.0x), 30-day grace period, FIFO debiting, and breakage liability rules. | [loyalty_domain.md](../lab2-requirements.md) |
| **Architecture Overview** | *C4 Model* | C4 System Context & Container diagrams, 5 domain microservices decomposition, synchronous vs asynchronous communication patterns. | [Architecture-Overview.md](Architecture-Overview.md) |
| **Data Architecture & Schema** | *Database & DW* | PostgreSQL Database-per-Service schemas, immutable point ledger, mutable snapshot tables, Redis Redlock configurations, and ClickHouse Star Schema. | [Data-Architecture-and-Schema.md](Data-Architecture-and-Schema.md) |
| **Security & Integration** | *Security & Ingress* | OAuth 2.0 Client Credentials flow for partners, API rate limiting policies (1,000 req/min/partner), and supervisor Dual-Control threshold approval (> 500 pts). | [Security-and-Integration-Architecture.md](Security-and-Integration-Architecture.md) |
| **Domain Event Catalog** | *Kafka Messaging* | Apache Kafka topic taxonomy, Avro/JSON event schemas, partition key strategy (`member_id`), consumer groups, and dead-letter queues (DLQ). | [domain-event-catalog.md](domain-event-catalog.md) |
| **The Open Group ArchiMate 3.2** | *Standard Spec* | Official Enterprise Architecture Modeling Language standard defining Motivation, Strategy, Business, Application, and Technology metamodels. | [ArchiMate Specification](https://www.opengroup.org/archimate-forum/archimate-overview) |

---

## 🎨 2. ArchiMate 3.2 Official Color Palette & Canonical Notation

### Color Palette (The Open Group & Archi Tool Standard)
| Layer / Aspect | Fill Color | Stroke Color | Hex Code | Visual Classification |
|---|:---:|:---:|:---:|---|
| **Motivation Aspect** | Lavender Purple | `#8888CC` | `#CCCCFF` | Stakeholders, Drivers, Goals, Principles, Requirements |
| **Strategy Layer** | Cream Tan | `#C4A055` | `#F5DEAA` | Capabilities, Resources, Courses of Action, Value Streams |
| **Business Layer** | Pale Yellow | `#CCCC66` | `#FFFFB5` | Business Actors, Roles, Services, Processes, Business Objects |
| **Application Layer** | Light Cyan | `#66CCCC` | `#B5FFFF` | Application Components, Services, Interfaces, Data Objects |
| **Technology Layer** | Mint Green | `#7CB342` | `#C9E7B7` | Nodes, System Software, Technology Services, Artifacts |
| **Implementation Layer** | Salmon Pink | `#CC6666` | `#FFB5B5` | Work Packages, Deliverables, Plateaus, Gaps |

### Canonical Stereotypes & Visual Cues
- **Motivation Elements**: `👤 «Stakeholder»`, `🧭 «Driver»`, `🎯 «Goal»`, `📜 «Principle»`
- **Strategy Elements**: `▦ «Capability»`, `⏩ «Value Stream»`
- **Business Elements**: `👤 «Business Actor»`, `◖ «Business Service»`, `⚙ «Business Process»`, `📄 «Business Object»`
- **Application Elements**: `▣ «Application Component»`, `⊸ «Application Interface»`, `⊞ «Application Service»`, `⇄ «Application Event Bus»`, `💾 «Data Object»`
- **Technology Elements**: `🧊 «Node»`, `⚙ «System Software»`, `🔄 «Technology Service»`, `🔒 «Technology Service»`, `💾 «Artifact»`
- **Implementation & Migration**: `🏁 «Plateau»`, `📋 «Work Package»`, `📦 «Deliverable»`

---

## 3. Architecture Overview (ArchiMate Core Stack)

```mermaid
flowchart TD
    M["🎯 1. MOTIVATION & STRATEGY\n─────────────────────\n👤 Stakeholders · 🧭 Drivers · 🎯 Goals · 📜 Principles\n▦ Capabilities · ⏩ Value Stream"] --> B["👤 2. BUSINESS LAYER\n─────────────────────\n👤 Actors · ◖ Services · ⚙ 5 Processes\n📄 Business Objects"]
    B --> A["▣ 3. APPLICATION LAYER\n─────────────────────\n▣ 5 Microservices · ⊸ API Gateway\n⇄ Kafka Topics · 💾 Data Objects"]
    A --> T["🧊 4. TECHNOLOGY LAYER\n─────────────────────\n🧊 K8s · 🔄 Kafka · 🔒 Redis · 💾 PostgreSQL\n💾 ClickHouse · 🔄 Debezium CDC"]
    I["🏁 5. IMPLEMENTATION & MIGRATION\n─────────────────────\n🏁 4 Plateaus · 📋 6 Work Packages\n📦 Deliverables"] -.-> M
    I -.-> B
    I -.-> A
    I -.-> T

    style M fill:#CCCCFF,stroke:#8888CC,stroke-width:2px,color:#1a1a2e
    style B fill:#FFFFB5,stroke:#CCCC66,stroke-width:2px,color:#1a1a2e
    style A fill:#B5FFFF,stroke:#66CCCC,stroke-width:2px,color:#1a1a2e
    style T fill:#C9E7B7,stroke:#7CB342,stroke-width:2px,color:#1a1a2e
    style I fill:#FFB5B5,stroke:#CC6666,stroke-width:2px,color:#1a1a2e
```

---

## 4. Motivation & Strategy Layer

```mermaid
flowchart TB
    subgraph MOT["MOTIVATION ASPECT"]
        direction TB
        SH["👤 «Stakeholder»\nExecutive Board, Marketing,\nFinance, Cardholders, Partners"]
        DR["🧭 «Driver»\nCustomer Retention, Coalition\nEcosystem, Zero Financial Leakage"]
        G1["🎯 «Goal»\nIngestion Throughput ≥ 500 TPS\np95 < 2s, SLA ≤ 60s"]
        G2["🎯 «Goal»\n100% Immutable Ledger\nZero Duplicate Postings"]
        PR1["📜 «Principle»\nADR-001 Database-per-Service"]
        PR2["📜 «Principle»\nADR-002 Event-Driven & Idempotency"]
        PR3["📜 «Principle»\nADR-003 OLTP / OLAP CDC Separation"]
    end

    subgraph STR["STRATEGY LAYER"]
        direction TB
        C1("▦ «Capability»\nHigh-Speed Real-Time Earning")
        C2("▦ «Capability»\nAutomated Tier Lifecycle")
        C3("▦ «Capability»\nMulti-Channel FIFO Redemption")
        C4("▦ «Capability»\nFinancial Liability Analytics")
        VS1("⏩ «Value Stream»\n1. Card Spending") --> VS2("⏩ «Value Stream»\n2. Point Accrual & Tier Upgrade")
        VS2 --> VS3("⏩ «Value Stream»\n3. Reward & Voucher Redemption")
        VS3 --> VS4("⏩ «Value Stream»\n4. Retention & Re-Engagement")
    end

    SH -->|influences| DR
    DR -->|motivates| G1
    DR -->|motivates| G2
    PR1 -.->|realizes| G2
    PR2 -.->|realizes| G1
    PR3 -.->|realizes| G1
    G1 ==>|realizes| C1
    G2 ==>|realizes| C1
    G1 -.->|realizes| C2
    G2 -.->|realizes| C3
    G1 -.->|realizes| C4
    C1 --> VS2
    C3 --> VS3

    style MOT fill:#EEDDFF,stroke:#8888CC,stroke-width:2px,color:#1a1a2e
    style STR fill:#FBF0D4,stroke:#C4A055,stroke-width:2px,color:#1a1a2e
    style SH fill:#CCCCFF,stroke:#8888CC,stroke-width:1.5px,color:#1a1a2e
    style DR fill:#CCCCFF,stroke:#8888CC,stroke-width:1.5px,color:#1a1a2e
    style G1 fill:#CCCCFF,stroke:#8888CC,stroke-width:1.5px,color:#1a1a2e
    style G2 fill:#CCCCFF,stroke:#8888CC,stroke-width:1.5px,color:#1a1a2e
    style PR1 fill:#CCCCFF,stroke:#8888CC,stroke-width:1.5px,color:#1a1a2e
    style PR2 fill:#CCCCFF,stroke:#8888CC,stroke-width:1.5px,color:#1a1a2e
    style PR3 fill:#CCCCFF,stroke:#8888CC,stroke-width:1.5px,color:#1a1a2e
    style C1 fill:#F5DEAA,stroke:#C4A055,stroke-width:1.5px,color:#1a1a2e
    style C2 fill:#F5DEAA,stroke:#C4A055,stroke-width:1.5px,color:#1a1a2e
    style C3 fill:#F5DEAA,stroke:#C4A055,stroke-width:1.5px,color:#1a1a2e
    style C4 fill:#F5DEAA,stroke:#C4A055,stroke-width:1.5px,color:#1a1a2e
    style VS1 fill:#F5DEAA,stroke:#C4A055,stroke-width:1px,color:#1a1a2e
    style VS2 fill:#F5DEAA,stroke:#C4A055,stroke-width:1px,color:#1a1a2e
    style VS3 fill:#F5DEAA,stroke:#C4A055,stroke-width:1px,color:#1a1a2e
    style VS4 fill:#F5DEAA,stroke:#C4A055,stroke-width:1px,color:#1a1a2e
```

---

## 5. Business Layer

```mermaid
flowchart TD
    subgraph ACTORS["ACTIVE STRUCTURE — Business Actors"]
        direction LR
        A1["👤 «Business Actor»\nBank Customer (Member)"]
        A2["🏦 «Business Actor»\nCore Banking System"]
        A3["👤 «Business Actor»\nProgram Administrator"]
        A4["👤 «Business Actor»\nFinance & Accounting"]
        A5["🤝 «Business Actor»\nAlliance Merchant Partner"]
    end

    subgraph SERVICES["BEHAVIOR — Business Services"]
        direction LR
        S1("◖ «Business Service»\nPoint Earning Service")
        S2("◖ «Business Service»\nTier Management Service")
        S3("◖ «Business Service»\nReward Redemption Service")
        S4("◖ «Business Service»\nDual-Control Adjustment Service")
        S5("◖ «Business Service»\nLiability Reporting Service")
    end

    subgraph PROCESSES["BEHAVIOR — Business Processes"]
        direction TB
        P1("⚙ «Business Process» BP-01\nSettlement Ingestion & Ledger Accrual")
        P2("⚙ «Business Process» BP-02\nTier Progression & 30-Day Grace Period")
        P3("⚙ «Business Process» BP-03\nFIFO Redemption & Saga Reversal")
        P4("⚙ «Business Process» BP-04\nDual-Control Approval (> 500 pts)")
        P5("⚙ «Business Process» BP-05\nFinancial Liability & Breakage Audit")
    end

    subgraph OBJECTS["PASSIVE STRUCTURE — Business Objects"]
        direction LR
        O1[("📄 «Business Object»\nPoint Transaction Ledger")]
        O2[("📄 «Business Object»\nAvailable Balance Snapshot")]
        O3[("📄 «Business Object»\nTier Info & QP Balance")]
        O4[("📄 «Business Object»\nRedemption Order")]
        O5[("📄 «Business Object»\nLiability Report")]
    end

    A1 -->|accesses| S1
    A1 -->|accesses| S3
    A2 -->|serves| S1
    A3 -->|accesses| S4
    A4 -->|accesses| S5
    A5 -->|serves| S1
    A5 -->|serves| S3

    S1 -->|triggers| P1
    S2 -->|triggers| P2
    S3 -->|triggers| P3
    S4 -->|triggers| P4
    S5 -->|triggers| P5

    P1 -->|accesses| O1
    P1 -->|accesses| O2
    P2 -->|accesses| O3
    P3 -->|accesses| O4
    P4 -->|accesses| O1
    P5 -->|accesses| O5

    style ACTORS fill:#FFFFF0,stroke:#CCCC66,stroke-width:2px,color:#1a1a2e
    style SERVICES fill:#FFFFF0,stroke:#CCCC66,stroke-width:2px,color:#1a1a2e
    style PROCESSES fill:#FFFFF0,stroke:#CCCC66,stroke-width:2px,color:#1a1a2e
    style OBJECTS fill:#FFFFF0,stroke:#CCCC66,stroke-width:2px,color:#1a1a2e
    style A1 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style A2 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style A3 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style A4 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style A5 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style S1 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style S2 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style S3 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style S4 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style S5 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style P1 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style P2 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style P3 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style P4 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style P5 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style O1 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style O2 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style O3 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style O4 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style O5 fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
```

---

## 6. Application Layer — C4 Container Architecture Layout

```mermaid
flowchart TB
    subgraph EXT_LEFT["EXTERNALS (LEFT)"]
        direction TB
        CUST["👤 «Business Actor»\nBank Customer\n(Mobile / Online Banking)"]
        CORE["🏦 «External System»\nCore Banking System\n(Settlement Transactions)"]
        PART["✈ «Partner System»\nPartner Systems\n(Merchant POS / Airlines)"]
    end

    subgraph LOYALTY_SYSTEM["LOYALTY SYSTEM (CONTAINER BOUNDARY)"]
        direction TB
        
        GW["⊸ «Application Interface»\nAPI Gateway & OAuth 2.0 Server\n(Authentication, Rate Limiting, Routing)"]

        subgraph SERVICES_ROW["5 DOMAIN APPLICATION COMPONENTS"]
            direction LR
            EE["▣ «Application Component»\nEarning Service\nManages earning rules &\nprocesses transactions"]
            TS["▣ «Application Component»\nTiering Service\nCalculates tiers &\ntriggers benefits"]
            RE["▣ «Application Component»\nRedemption Service\nReward redemption\nworkflows & Saga"]
            PM["▣ «Application Component»\nProgram Management\nAdmin interface for\nrules & catalog"]
            AR["▣ «Application Component»\nAnalytics Service\nProcesses data for\nreports & liability"]
        end

        BUS[("⇄ «Application Event Bus»\nCommunication Bus (Apache Kafka)\nTopics: TransactionEvent · TierEvent · RedemptionEvent · cdc_stream")]
        
        DB[("💾 «Data Object»\nCentral Loyalty Database (Polyglot)\nMember Profiles · Point Ledger · Rule Configurations · Tiers & Audits")]
    end

    subgraph EXT_RIGHT["EXTERNALS (RIGHT)"]
        direction TB
        ADMIN["👤 «Business Actor»\nBank Administrator\n(Admin Web Portal)"]
        IDP["🔑 «External Service»\nIdentity Provider\n(OAuth 2.0 / JWT Auth)"]
        NOTIF["✉ «External Service»\nNotification System\n(SMS, Push, Email)"]
    end

    %% External Connections
    CUST -->|calls| GW
    ADMIN -->|calls| GW
    IDP <-->|authenticates| GW
    CORE -->|Webhooks| EE
    PART -->|Webhooks| EE

    %% Gateway Routing
    GW -->|calls| EE
    GW -->|calls| TS
    GW -->|calls| RE
    GW -->|calls| PM
    GW -->|calls| AR

    %% Service to Database
    EE <-->|reads/writes| DB
    TS <-->|reads/writes| DB
    RE <-->|reads/writes| DB
    PM <-->|reads/writes| DB
    AR <-->|reads/writes| DB

    %% Service to Event Bus
    EE <-->|publishes/subscribes| BUS
    TS <-->|publishes/subscribes| BUS
    RE <-->|publishes/subscribes| BUS
    PM -->|publishes CDC| BUS
    BUS -->|reads/writes| AR
    BUS -->|subscribes| NOTIF

    %% Styling with ArchiMate Application Cyan Palette
    style LOYALTY_SYSTEM fill:#EAF7F9,stroke:#2980B9,stroke-width:2px,color:#1a1a2e
    style SERVICES_ROW fill:#D8F2F7,stroke:#2980B9,stroke-width:1.5px,color:#1a1a2e
    style EXT_LEFT fill:#F4F6F7,stroke:#BDC3C7,stroke-width:1.5px,color:#1a1a2e
    style EXT_RIGHT fill:#F4F6F7,stroke:#BDC3C7,stroke-width:1.5px,color:#1a1a2e

    style GW fill:#B5FFFF,stroke:#2980B9,stroke-width:1.5px,color:#1a1a2e
    style EE fill:#B5FFFF,stroke:#2980B9,stroke-width:1.5px,color:#1a1a2e
    style TS fill:#B5FFFF,stroke:#2980B9,stroke-width:1.5px,color:#1a1a2e
    style RE fill:#B5FFFF,stroke:#2980B9,stroke-width:1.5px,color:#1a1a2e
    style PM fill:#B5FFFF,stroke:#2980B9,stroke-width:1.5px,color:#1a1a2e
    style AR fill:#B5FFFF,stroke:#2980B9,stroke-width:1.5px,color:#1a1a2e
    style BUS fill:#B5FFFF,stroke:#2980B9,stroke-width:1.5px,color:#1a1a2e
    style DB fill:#B5FFFF,stroke:#2980B9,stroke-width:1.5px,color:#1a1a2e

    style CUST fill:#FFFFFF,stroke:#7F8C8D,stroke-width:1px,color:#1a1a2e
    style CORE fill:#FFFFFF,stroke:#7F8C8D,stroke-width:1px,color:#1a1a2e
    style PART fill:#FFFFFF,stroke:#7F8C8D,stroke-width:1px,color:#1a1a2e
    style ADMIN fill:#FFFFFF,stroke:#7F8C8D,stroke-width:1px,color:#1a1a2e
    style IDP fill:#FFFFFF,stroke:#7F8C8D,stroke-width:1px,color:#1a1a2e
    style NOTIF fill:#FFFFFF,stroke:#7F8C8D,stroke-width:1px,color:#1a1a2e
```

---

## 7. Technology & Infrastructure Layer

```mermaid
flowchart TB
    subgraph INFRA_STACK["TECHNOLOGY & INFRASTRUCTURE PLATFORM"]
        direction TB

        subgraph T_GATEWAY["1. EDGE INGRESS & SECURITY TIER"]
            INGRESS["⊸ «Technology Interface»\nIngress Controller / API Gateway\nJWT Verification · SSL Termination · Rate Limiter (1,000 req/min)"]
        end

        subgraph T_COMPUTE["2. KUBERNETES CONTAINER COMPUTE TIER"]
            direction LR
            K_EE["⚙ «System Software»\nEarning Pod"]
            K_TS["⚙ «System Software»\nTiering Pod"]
            K_RE["⚙ «System Software»\nRedemption Pod"]
            K_PM["⚙ «System Software»\nProgram Pod"]
            K_AR["⚙ «System Software»\nAnalytics Pod"]
        end

        subgraph T_MIDDLEWARE["3. MIDDLEWARE & COMMUNICATION BUS"]
            direction LR
            KF[("🔄 «Technology Service»\nApache Kafka Cluster\nPartitioned Message Broker")]
            RD[("🔒 «Technology Service»\nRedis Sentinel Cluster\nDistributed Redlock & Idempotency Key")]
        end

        subgraph T_STORAGE["4. POLYGLOT DATA STORAGE TIER"]
            direction LR
            P_EE[("💾 «Artifact»\nEarning DB\nPostgreSQL (Ledger)")]
            P_TS[("💾 «Artifact»\nTiering DB\nPostgreSQL (QP)")]
            P_RED[("💾 «Artifact»\nRedemption DB\nPostgreSQL (Orders)")]
            P_PM[("💾 «Artifact»\nProgram DB\nPostgreSQL (Rules)")]
            P_DW[("💾 «Artifact»\nAnalytics DW\nClickHouse (Star Schema)")]
        end
    end

    %% Routing
    INGRESS -->|routes to| T_COMPUTE

    %% Compute with Middleware
    K_EE <-->|pub/sub| KF
    K_TS <-->|pub/sub| KF
    K_RE <-->|pub/sub| KF
    K_PM -->|publishes CDC| KF
    KF -->|consumes CDC| K_AR

    K_EE <-->|Idempotency Lock| RD
    K_RE <-->|FIFO Debit Lock| RD

    %% Compute with Storage
    K_EE --- P_EE
    K_TS --- P_TS
    K_RE --- P_RED
    K_PM --- P_PM
    K_AR --- P_DW

    %% CDC Stream from Databases
    P_EE -.->|CDC Stream| KF
    P_TS -.->|CDC Stream| KF
    P_RED -.->|CDC Stream| KF
    P_PM -.->|CDC Stream| KF

    %% Styling with ArchiMate Technology Mint Green Palette
    style INFRA_STACK fill:#EAF7EC,stroke:#27AE60,stroke-width:2px,color:#1a1a2e
    style T_GATEWAY fill:#DCF4E0,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e
    style T_COMPUTE fill:#DCF4E0,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e
    style T_MIDDLEWARE fill:#DCF4E0,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e
    style T_STORAGE fill:#DCF4E0,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e

    style INGRESS fill:#C9E7B7,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e
    style K_EE fill:#C9E7B7,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e
    style K_TS fill:#C9E7B7,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e
    style K_RE fill:#C9E7B7,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e
    style K_PM fill:#C9E7B7,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e
    style K_AR fill:#C9E7B7,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e

    style KF fill:#C9E7B7,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e
    style RD fill:#C9E7B7,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e

    style P_EE fill:#C9E7B7,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e
    style P_TS fill:#C9E7B7,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e
    style P_RED fill:#C9E7B7,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e
    style P_PM fill:#C9E7B7,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e
    style P_DW fill:#C9E7B7,stroke:#27AE60,stroke-width:1.5px,color:#1a1a2e
```

---

## 8. Implementation & Migration Layer

```mermaid
flowchart LR
    subgraph PL["PLATEAUS — Architecture Transition States"]
        P0["🏁 «Plateau» P0\nBaseline Architecture & Specs"]
        P1["🏁 «Plateau» P1\nMVP Core Earning & Tiering"]
        P2["🏁 «Plateau» P2\nRedemption & Partner Gateway"]
        P3["🏁 «Plateau» P3\nEnterprise Target State"]
    end

    subgraph WP["WORK PACKAGES — Implementation Tasks"]
        direction TB
        W1["📋 «Work Package» WP-1\nK8s Infra & Kafka Bus Setup"]
        W2["📋 «Work Package» WP-2\nImmutable Ledger & Idempotency"]
        W3["📋 «Work Package» WP-3\nTiering Engine & 30d Grace"]
        W4["📋 «Work Package» WP-4\nFIFO Redemption & OAuth API"]
        W5["📋 «Work Package» WP-5\nStar Schema DW & CDC Stream"]
        W6["📋 «Work Package» WP-6\nDual-Control & 500 TPS Gate"]
    end

    subgraph DEL["DELIVERABLES"]
        direction TB
        D1[("📦 «Deliverable»\nArchitecture & OpenAPI Specs")]
        D2[("📦 «Deliverable»\n5 Microservice Docker Images")]
        D3[("📦 «Deliverable»\nPostgreSQL & ClickHouse DW")]
        D4[("📦 «Deliverable»\nMobile App & Web Portals")]
        D5[("📦 «Deliverable»\nQuality Gate Test Audit Report")]
    end

    P0 -->|triggers| W1
    W1 -->|realizes| P1
    W2 -->|realizes| P1
    W3 -->|realizes| P1
    P1 -->|triggers| W4
    W4 -->|realizes| P2
    P2 -->|triggers| W5
    P2 -->|triggers| W6
    W5 -->|realizes| P3
    W6 -->|realizes| P3
    P3 -.->|delivers| D1
    P3 -.->|delivers| D2
    P3 -.->|delivers| D3
    P3 -.->|delivers| D4
    P3 -.->|delivers| D5

    style PL fill:#FFE0E0,stroke:#CC6666,stroke-width:2px,color:#1a1a2e
    style WP fill:#FFE0E0,stroke:#CC6666,stroke-width:2px,color:#1a1a2e
    style DEL fill:#FFE0E0,stroke:#CC6666,stroke-width:2px,color:#1a1a2e
    style P0 fill:#FFB5B5,stroke:#CC6666,stroke-width:1.5px,color:#1a1a2e
    style P1 fill:#FFB5B5,stroke:#CC6666,stroke-width:1.5px,color:#1a1a2e
    style P2 fill:#FFB5B5,stroke:#CC6666,stroke-width:1.5px,color:#1a1a2e
    style P3 fill:#FFB5B5,stroke:#CC6666,stroke-width:1.5px,color:#1a1a2e
    style W1 fill:#FFB5B5,stroke:#CC6666,stroke-width:1.5px,color:#1a1a2e
    style W2 fill:#FFB5B5,stroke:#CC6666,stroke-width:1.5px,color:#1a1a2e
    style W3 fill:#FFB5B5,stroke:#CC6666,stroke-width:1.5px,color:#1a1a2e
    style W4 fill:#FFB5B5,stroke:#CC6666,stroke-width:1.5px,color:#1a1a2e
    style W5 fill:#FFB5B5,stroke:#CC6666,stroke-width:1.5px,color:#1a1a2e
    style W6 fill:#FFB5B5,stroke:#CC6666,stroke-width:1.5px,color:#1a1a2e
    style D1 fill:#FFB5B5,stroke:#CC6666,stroke-width:1.5px,color:#1a1a2e
    style D2 fill:#FFB5B5,stroke:#CC6666,stroke-width:1.5px,color:#1a1a2e
    style D3 fill:#FFB5B5,stroke:#CC6666,stroke-width:1.5px,color:#1a1a2e
    style D4 fill:#FFB5B5,stroke:#CC6666,stroke-width:1.5px,color:#1a1a2e
    style D5 fill:#FFB5B5,stroke:#CC6666,stroke-width:1.5px,color:#1a1a2e
```

---

## 9. Cross-Layer Traceability View

```mermaid
flowchart TB
    subgraph L1["MOTIVATION & STRATEGY"]
        G1["🎯 «Goal»\n100% Immutable Ledger\nZero Duplicate Postings"]
        G2["🎯 «Goal»\nThroughput ≥ 500 TPS\np95 < 2s, SLA ≤ 60s"]
        CAP("▦ «Capability»\nHigh-Speed Real-Time Earning")
        G1 -.->|realizes| CAP
        G2 -.->|realizes| CAP
    end

    subgraph L2["BUSINESS LAYER"]
        BS("◖ «Business Service»\nPoint Earning Business Service")
        BP("⚙ «Business Process»\nSettlement Ingestion & Ledger Accrual")
        BO[("📄 «Business Object»\nPoint Transaction Ledger Entry")]
        CAP ==>|serves| BS
        BS -->|triggers| BP
        BP -->|accesses| BO
    end

    subgraph L3["APPLICATION LAYER"]
        AS("⊞ «Application Service»\nEarn Ingestion & Evaluation API")
        AC["▣ «Application Component»\nEarning Engine Service"]
        DO[("💾 «Data Object»\npoint_transaction & point_balance")]
        BP -.->|realized by| AS
        AS -->|serves| AC
        AC -->|accesses| DO
    end

    subgraph L4["TECHNOLOGY LAYER"]
        TK["🔄 «Technology Service»\nApache Kafka (settled_transactions)"]
        TR["🔒 «Technology Service»\nRedis Cluster (Idempotency Lock)"]
        TP[("💾 «Artifact»\nPostgreSQL (Partitioned Ledger DB)")]
        TN["🧊 «Node»\nKubernetes Worker Pods (HPA)"]

        AC -.->|deployed on| TN
        AC -.->|consumes from| TK
        AC -.->|accesses| TR
        DO -.->|stored in| TP
    end

    L1 ==> L2
    L2 ==> L3
    L3 ==> L4

    style L1 fill:#EEDDFF,stroke:#8888CC,stroke-width:2px,color:#1a1a2e
    style L2 fill:#FFFFF0,stroke:#CCCC66,stroke-width:2px,color:#1a1a2e
    style L3 fill:#E5FFFF,stroke:#66CCCC,stroke-width:2px,color:#1a1a2e
    style L4 fill:#E0F2E0,stroke:#7CB342,stroke-width:2px,color:#1a1a2e
    style G1 fill:#CCCCFF,stroke:#8888CC,stroke-width:1.5px,color:#1a1a2e
    style G2 fill:#CCCCFF,stroke:#8888CC,stroke-width:1.5px,color:#1a1a2e
    style CAP fill:#F5DEAA,stroke:#C4A055,stroke-width:1.5px,color:#1a1a2e
    style BS fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style BP fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style BO fill:#FFFFB5,stroke:#CCCC66,stroke-width:1.5px,color:#1a1a2e
    style AS fill:#B5FFFF,stroke:#66CCCC,stroke-width:1.5px,color:#1a1a2e
    style AC fill:#B5FFFF,stroke:#66CCCC,stroke-width:1.5px,color:#1a1a2e
    style DO fill:#B5FFFF,stroke:#66CCCC,stroke-width:1.5px,color:#1a1a2e
    style TK fill:#C9E7B7,stroke:#7CB342,stroke-width:1.5px,color:#1a1a2e
    style TR fill:#C9E7B7,stroke:#7CB342,stroke-width:1.5px,color:#1a1a2e
    style TP fill:#C9E7B7,stroke:#7CB342,stroke-width:1.5px,color:#1a1a2e
    style TN fill:#C9E7B7,stroke:#7CB342,stroke-width:1.5px,color:#1a1a2e
```

---

## 10. Traceability Matrix

| # | Layer | Element Type | Loyalty System Element Name | Business / Technical Responsibility |
|:---:|---|---|---|---|
| 1 | **Motivation** | `👤 «Stakeholder»` | Executive Board, Marketing, Finance/Accounting, Cardholder, Partners | Direct participants and beneficiaries in the Loyalty value chain |
| 2 | **Motivation** | `🎯 «Goal»` | Ingestion $\ge 500$ TPS, SLA $\le 60$s, 100% Immutable Ledger | Core performance SLA and financial non-repudiation targets |
| 3 | **Motivation** | `📜 «Principle»` | ADR-001 (DB Isolation), ADR-002 (Event/Idempotency), ADR-003 (CDC) | Foundational architectural invariant decisions |
| 4 | **Strategy** | `▦ «Capability»` | High-Speed Earning, Automated Tier Lifecycle, Multi-Channel FIFO Redemption | Core strategic enterprise capabilities |
| 5 | **Business** | `👤 «Business Actor»` | Cardholder (Member), Core Banking System, Program Admin, Finance, Partner | Human actors and external source banking systems |
| 6 | **Business** | `◖ «Business Service»` | Earning Service, Tier Evaluation Service, Reward Redemption, Liability Audit | Business services delivered to members and internal stakeholders |
| 7 | **Business** | `⚙ «Business Process»` | Core Banking Earning (BP01), Tiering Grace Period (BP02), FIFO Redemption (BP03), Dual-Control (BP04) | Standard enterprise business workflows and rules |
| 8 | **Business** | `📄 «Business Object»` | Point Transaction Ledger, Point Balance Snapshot, QP Ledger, Redemption Order | Passive informational and financial state business entities |
| 9 | **Application** | `▣ «Application Component»` | Earning Engine, Tiering System, Redemption Engine, Program Mgmt, Analytics Service | 5 autonomous Domain-Driven microservices |
| 10 | **Application** | `⊸ «Application Interface»` | `settled_transactions`, `tier_event`, `cdc_stream`, REST API Gateway | APIs and Kafka choreography event streams |
| 11 | **Application** | `💾 «Data Object»` | `point_transaction`, `point_balance`, `qp_ledger`, `fact_*` | Relational transactional data models and OLAP Star Schema |
| 12 | **Technology** | `🧊 «Node»` / `⚙ «System SW»` | Kubernetes Cluster, Kafka 3-Brokers, Redis Sentinel, PostgreSQL, ClickHouse | Distributed compute, caching, messaging, and storage nodes |
| 13 | **Technology** | `💾 «Artifact»` | `earning-engine.jar`, Docker Containers, `*.sql` Migration Scripts | Physical deployed binaries and database schema definitions |
| 14 | **Implementation** | `📋 «Work Package»` | WP1 (Infra Setup) $\rightarrow$ WP6 (Dual-Control & Quality Gates 500 TPS) | Capstone engineering implementation work packages |
| 15 | **Implementation** | `🏁 «Plateau»` | Plateau 0 (Baseline Specs) $\rightarrow$ Plateau 3 (Enterprise Target State) | Target architecture deliverables and handover states |

---

## 11. Lab 8 Four-View Review Register

This section binds the broader ArchiMate material above to the four named Lab 8 views required by `template/list.md`. Extra views and presentation material remain supporting evidence, but the trainee review set is the four rows below.

### 11.1 Input Coverage

| Lab 1 input | Evidence |
|---|---|
| I-1 goal/outcome | `loyalty.md` I-1; Motivation / Strategy view row below |
| I-4 containers | `loyalty.md` I-4; Application Cooperation view row below |
| I-5 process | `loyalty.md` I-5; Business Process view row below |
| I-9 deployment | `loyalty.md` I-9; Technology view row below |
| I-10 constraints | `loyalty.md` I-10; Motivation and Process view rows below |

### 11.2 Four Named Views

| # | View | After evidence | Must show | Must not show | Status |
|---:|---|---|---|---|---|
| 1 | Motivation or Strategy | `architecture/archimate/motivation-layer.md`; `architecture/archimate/strategy-layer.md`; sections 4 and 9 above | Goal, outcome, `CON.1` to `CON.3` as constraints or principles | Protocol, pods, JDBC, container internals | Pass with constraint mapping |
| 2 | Business Process | `architecture/archimate/business-layer.md`; section 5 above | I-5 happy path and `CON.*` on branches | C4 containers as process boxes; sync/async labels | Pass with process-to-constraint mapping |
| 3 | Application Cooperation | `architecture/archimate/application-layer.md`; section 6 above | Containers from I-4 using same strings as C4 Container | UML messages; mixed C4 notation | Pass with name-identity mapping |
| 4 | Technology / hybrid | `architecture/archimate/technology-layer.md`; section 7 above; deployment section in `Architecture-Overview.md` | Locations from I-9 and forbidden path | Channel writing core ledger DB | Pass with forbidden-path check |

### 11.3 After Header Register

| View | Header |
|---|---|
| Motivation / Strategy | Title: Loyalty Banking Motivation and Strategy; Viewpoint: ArchiMate; Layer(s): Strategy / Motivation; To-Be; Owner: Owner; RACI: R EA, A Owner, C SA BA/PO Sec, I DA Dev Test Ops; Version: v1.0, Date 2026-08-21, Status Review; Legend: Influence, Realization, Association, Triggering, Serving; Scope: Lab 1 I-1 |
| Business Process | Title: Loyalty Banking Business Process; Viewpoint: ArchiMate; Layer(s): Business; To-Be; Owner: Owner; RACI: R BA/PO, A Owner, C EA SA Sec Test, I DA Dev Ops; Version: v1.0, Date 2026-08-21, Status Review; Legend: Serving, Triggering, Assignment, Access, Realization; Scope: Lab 1 I-5 |
| Application Cooperation | Title: Loyalty Banking Application Cooperation; Viewpoint: ArchiMate; Layer(s): Application; To-Be; Owner: SA; RACI: R SA, A SA, C EA DA Sec Dev, I BA/PO Test Ops Owner; Version: v1.0, Date 2026-08-21, Status Review; Legend: Serving, Flow, Association, Access; Scope: Lab 1 I-4 and I-8 |
| Technology / hybrid | Title: Loyalty Banking Technology Deployment; Viewpoint: ArchiMate; Layer(s): Technology; To-Be; Owner: SA; RACI: R Ops, A SA, C Sec Dev, I EA BA/PO DA Test Owner; Version: v1.0, Date 2026-08-21, Status Review; Legend: Assignment, Serving, Access, Flow; Scope: Lab 1 I-9 |

### 11.4 G1 Motivation / Strategy Constraint Mapping

| G1 item | Lab 1 source | View representation |
|---|---|---|
| Goal | I-1 Goal | Real-time earning, ledger integrity, reporting isolation, and governed adjustments in Motivation/Strategy evidence |
| Outcome | I-1 Outcome | SLA and data-staleness goals in Motivation/Strategy evidence |
| CON.1 | No duplicate point posting | Principle / requirement for event-driven idempotency and ledger integrity |
| CON.2 | No direct database writes from channels, partners, or other services | Principle / requirement for database-per-service and source-of-truth ownership |
| CON.3 | Fulfillment failure must compensate by restoring points | Requirement/constraint attached to redemption and ledger integrity capability |

### 11.5 G2 Business Process and State Mapping

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

### 11.6 Application Cooperation Name Identity

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

### 11.7 Technology / Forbidden Path Check

| Location from I-9 | Technology view evidence | Forbidden-path status |
|---|---|---|
| Edge & Ingestion Zone | API gateway and event bus technology services | Does not write directly to Earning DB - PostgreSQL |
| Domain Services Zone | Service runtimes for earning, tiering, redemption, program management | Each service writes only its owned database |
| Analytics Zone | Analytics service and Data Warehouse - Star Schema | Analytics reads warehouse/CDC, not OLTP directly |
| Data Services Zone | PostgreSQL service databases and Redis | Owned by service boundaries and not exposed to external actors |

### 11.8 Negative Evidence

| Forbidden item | Status |
|---|---|
| Protocol, pods, JDBC, or container internals on Motivation / Strategy review view | Excluded from trainee Motivation / Strategy mapping |
| C4 containers as Business Process boxes | Business process mapping uses process names and business objects |
| UML messages inside Application Cooperation | Application Cooperation mapping uses ArchiMate application flows/interfaces |
| Channel writing core ledger DB | Explicitly forbidden by I-9 and CON.2 |
| More than four required Lab 8 views | Extra ArchiMate files/presentations are retained as supporting material, but the trainee review set is the four named views above |
