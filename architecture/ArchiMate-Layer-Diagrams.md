# Loyalty Banking — Kiến trúc Phân tầng ArchiMate 3.2 (ArchiMate Layer Diagrams)

**Domain**: Loyalty Banking Platform (Hệ thống Khách hàng Thân thiết Ngân hàng)  
**Tiêu chuẩn Kiến trúc**: The Open Group ArchiMate® 3.2 Specification & C4 Model Container Layout  
**Phiên bản**: 3.2  
**Tài liệu tham chiếu**:
- [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) — Tổng quan nghiệp vụ Loyalty
- [Architecture-Overview.md](file:///d:/learn/loyalty/architecture/Architecture-Overview.md) — Kiến trúc tổng thể hệ thống
- [Data-Architecture-and-Schema.md](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) — Kiến trúc dữ liệu & Star Schema
- [Security-and-Integration-Architecture.md](file:///d:/learn/loyalty/architecture/Security-and-Integration-Architecture.md) — Bảo mật & Tích hợp Đối tác
- [domain-event-catalog.md](file:///d:/learn/loyalty/architecture/domain-event-catalog.md) — Danh mục sự kiện Kafka

---

## 🎨 Quy chuẩn Bảng màu & Icon Chuẩn ArchiMate 3.2

### 1. Bảng màu Chuẩn (Archi Tool Standard)
| Tầng / Khía cạnh (Layer / Aspect) | Màu Nền (Fill) | Màu Viền (Stroke) | Hex Code | Ý nghĩa & Phân loại |
|---|:---:|:---:|:---:|---|
| **Motivation Aspect** | Lavender Purple | `#8888CC` | `#CCCCFF` | Stakeholders, Drivers, Goals, Principles, Requirements |
| **Strategy Layer** | Cream Tan | `#C4A055` | `#F5DEAA` | Capabilities, Resources, Courses of Action, Value Streams |
| **Business Layer** | Pale Yellow | `#CCCC66` | `#FFFFB5` | Business Actors, Roles, Services, Processes, Business Objects |
| **Application Layer** | Light Cyan | `#66CCCC` | `#B5FFFF` | Application Components, Services, Interfaces, Data Objects |
| **Technology Layer** | Mint Green | `#7CB342` | `#C9E7B7` | Nodes, System Software, Technology Services, Artifacts |
| **Implementation Layer** | Salmon Pink | `#CC6666` | `#FFB5B5` | Work Packages, Deliverables, Plateaus, Gaps |

### 2. Danh mục Icon & Stereotype Chuẩn ArchiMate 3.2
- **Motivation**: `👤 «Stakeholder»`, `🧭 «Driver»`, `🎯 «Goal»`, `📜 «Principle»`
- **Strategy**: `▦ «Capability»`, `⏩ «Value Stream»`
- **Business**: `👤 «Business Actor»`, `◖ «Business Service»`, `⚙ «Business Process»`, `📄 «Business Object»`
- **Application**: `▣ «Application Component»`, `⊸ «Application Interface»`, `⊞ «Application Service»`, `⇄ «Application Event Bus»`, `💾 «Data Object»`
- **Technology**: `🧊 «Node»`, `⚙ «System Software»`, `🔄 «Technology Service»`, `🔒 «Technology Service»`, `💾 «Artifact»`
- **Implementation & Migration**: `🏁 «Plateau»`, `📋 «Work Package»`, `📦 «Deliverable»`

---

## 1. Tổng quan Khung Phân tầng Kiến trúc (ArchiMate Core Stack)

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

## 2. Motivation & Strategy Layer (Tầng Động lực & Chiến lược)

```mermaid
flowchart TB
    subgraph MOT["MOTIVATION ASPECT"]
        direction TB
        SH["👤 «Stakeholder»\nLãnh đạo, Marketing,\nKế toán, Chủ thẻ, Đối tác"]
        DR["🧭 «Driver»\nGiữ chân KH, Mở rộng\nliên minh, Tránh thất thoát"]
        G1["🎯 «Goal»\nThông lượng ≥ 500 TPS\np95 < 2s, SLA ≤ 60s"]
        G2["🎯 «Goal»\n100% Sổ cái Bất biến\nKhông trùng lặp bút toán"]
        PR1["📜 «Principle»\nADR-001 Cô lập DB per Service"]
        PR2["📜 «Principle»\nADR-002 Event-Driven & Idempotency"]
        PR3["📜 «Principle»\nADR-003 Tách biệt OLTP / OLAP CDC"]
    end

    subgraph STR["STRATEGY LAYER"]
        direction TB
        C1("▦ «Capability»\nTích điểm Tốc độ cao")
        C2("▦ «Capability»\nXét hạng Tự động")
        C3("▦ «Capability»\nĐổi quà FIFO Đa kênh")
        C4("▦ «Capability»\nPhân tích Nợ điểm")
        VS1("⏩ «Value Stream»\n1. Chi tiêu Thẻ") --> VS2("⏩ «Value Stream»\n2. Tích điểm & Nâng hạng")
        VS2 --> VS3("⏩ «Value Stream»\n3. Đổi Quà & Voucher")
        VS3 --> VS4("⏩ «Value Stream»\n4. Gắn kết & Duy trì")
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

## 3. Business Layer (Tầng Nghiệp vụ Loyalty)

```mermaid
flowchart TD
    subgraph ACTORS["ACTIVE STRUCTURE — Business Actors"]
        direction LR
        A1["👤 «Business Actor»\nKhách hàng (Member)"]
        A2["🏦 «Business Actor»\nCore Banking System"]
        A3["👤 «Business Actor»\nQuản trị viên (Admin)"]
        A4["👤 «Business Actor»\nKế toán (Finance)"]
        A5["🤝 «Business Actor»\nĐối tác Liên minh (Partner)"]
    end

    subgraph SERVICES["BEHAVIOR — Business Services"]
        direction LR
        S1("◖ «Business Service»\nDịch vụ Tích điểm")
        S2("◖ «Business Service»\nDịch vụ Xét hạng")
        S3("◖ «Business Service»\nDịch vụ Đổi quà")
        S4("◖ «Business Service»\nDịch vụ Duyệt kép")
        S5("◖ «Business Service»\nDịch vụ Báo cáo Nợ")
    end

    subgraph PROCESSES["BEHAVIOR — Business Processes"]
        direction TB
        P1("⚙ «Business Process» BP-01\nTích điểm GD & Ghi Sổ cái")
        P2("⚙ «Business Process» BP-02\nXét hạng & Ân hạn 30 ngày")
        P3("⚙ «Business Process» BP-03\nĐổi quà FIFO & Saga Reversal")
        P4("⚙ «Business Process» BP-04\nĐiều chỉnh Điểm Dual-Control")
        P5("⚙ «Business Process» BP-05\nBáo cáo Nợ điểm & Breakage")
    end

    subgraph OBJECTS["PASSIVE STRUCTURE — Business Objects"]
        direction LR
        O1[("📄 «Business Object»\nSổ cái Điểm (Ledger)")]
        O2[("📄 «Business Object»\nSố dư Khả dụng")]
        O3[("📄 «Business Object»\nHạng thẻ & QP")]
        O4[("📄 «Business Object»\nĐơn Đổi quà")]
        O5[("📄 «Business Object»\nBáo cáo Nợ")]
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

## 4. Application Layer — C4 Container Architecture Layout

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

## 5. Technology & Infrastructure Layer (Tầng Công nghệ & Hạ tầng)

```mermaid
flowchart TB
    subgraph INFRA_STACK["TECHNOLOGY & INFRASTRUCTURE PLATFORM"]
        direction TB

        subgraph T_GATEWAY["1. EDGE INGRESS & SECURITY TIER"]
            INGRESS["⊸ «Technology Interface»\nIngress Controller / API Gateway\nJWT Verification · SSL Termination · Rate Limiter (1.000 req/min)"]
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

## 6. Implementation & Migration Layer (Lộ trình Triển khai Capstone)

```mermaid
flowchart LR
    subgraph PL["PLATEAUS — Architecture Transition States"]
        P0["🏁 «Plateau» P0\nThiết kế Cơ sở & Specs"]
        P1["🏁 «Plateau» P1\nMVP Tích điểm & Hạng thẻ"]
        P2["🏁 «Plateau» P2\nĐổi quà & Đối tác"]
        P3["🏁 «Plateau» P3\nHệ thống Hoàn chỉnh"]
    end

    subgraph WP["WORK PACKAGES — Implementation Tasks"]
        direction TB
        W1["📋 «Work Package» WP-1\nHạ tầng K8s & Kafka"]
        W2["📋 «Work Package» WP-2\nSổ cái & Idempotency"]
        W3["📋 «Work Package» WP-3\nXét hạng & Ân hạn 30d"]
        W4["📋 «Work Package» WP-4\nĐổi quà FIFO & OAuth"]
        W5["📋 «Work Package» WP-5\nStar Schema & CDC"]
        W6["📋 «Work Package» WP-6\nDual-Control & 500 TPS"]
    end

    subgraph DEL["DELIVERABLES"]
        direction TB
        D1[("📦 «Deliverable»\nKiến trúc & API Specs")]
        D2[("📦 «Deliverable»\n5 Docker Containers")]
        D3[("📦 «Deliverable»\nCSDL & Data Warehouse")]
        D4[("📦 «Deliverable»\nMobile App & Portals")]
        D5[("📦 «Deliverable»\nQuality Gate Report")]
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

## 7. Cross-Layer Traceability View (Sơ đồ Tích hợp Đa tầng)

```mermaid
flowchart TB
    subgraph L1["MOTIVATION & STRATEGY"]
        G1["🎯 «Goal»\nSổ cái Bất biến 100%\n0 Trùng lặp Bút toán"]
        G2["🎯 «Goal»\nThông lượng ≥ 500 TPS\np95 < 2s, SLA ≤ 60s"]
        CAP("▦ «Capability»\nTích điểm Tốc độ cao")
        G1 -.->|realizes| CAP
        G2 -.->|realizes| CAP
    end

    subgraph L2["BUSINESS LAYER"]
        BS("◖ «Business Service»\nDịch vụ Tích điểm GD")
        BP("⚙ «Business Process»\nTiếp nhận GD & Ghi Sổ cái")
        BO[("📄 «Business Object»\nBút toán point_transaction")]
        CAP ==>|serves| BS
        BS -->|triggers| BP
        BP -->|accesses| BO
    end

    subgraph L3["APPLICATION LAYER"]
        AS("⊞ «Application Service»\nEarn Ingestion API")
        AC["▣ «Application Component»\nEarning Engine Service"]
        DO[("💾 «Data Object»\npoint_transaction\npoint_balance")]
        BP -.->|realized by| AS
        AS -->|serves| AC
        AC -->|accesses| DO
    end

    subgraph L4["TECHNOLOGY LAYER"]
        TK["🔄 «Technology Service»\nApache Kafka (settled_transactions)"]
        TR["🔒 «Technology Service»\nRedis Cluster (Idempotency Lock)"]
        TP[("💾 «Artifact»\nPostgreSQL (Partitioned Ledger)")]
        TN["🧊 «Node»\nKubernetes Pods (HPA)"]

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

## 8. Bảng Ma trận Ánh xạ Truy vết Phần tử ArchiMate (Traceability Matrix)

| STT | ArchiMate Layer | Loại Phần tử (Element Type) | Tên Phần tử trong Dự án Loyalty | Trách nhiệm & Ý nghĩa Nghiệp vụ / Kỹ thuật |
|:---:|---|---|---|---|
| 1 | **Motivation** | `👤 «Stakeholder»` | Ban Lãnh đạo, Marketing, Kế toán, Chủ thẻ, Đối tác | Các bên tham gia trực tiếp vào chuỗi giá trị Loyalty |
| 2 | **Motivation** | `🎯 «Goal»` | Thông lượng $\ge 500$ TPS, SLA $\le 60$s, Sổ cái Bất biến | Chỉ số đo lường hiệu năng và cam kết dịch vụ tài chính |
| 3 | **Motivation** | `📜 «Principle»` | ADR-001 (Cô lập DB), ADR-002 (Sự kiện/Idempotency), ADR-003 (CDC) | 3 quyết định kiến trúc cốt lõi định hình nền tảng |
| 4 | **Strategy** | `▦ «Capability»` | Tích điểm tốc độ cao, Quản lý Hạng thẻ động, Đổi quà FIFO | Năng lực cốt lõi phục vụ hệ sinh thái Loyalty Ngân hàng |
| 5 | **Business** | `👤 «Business Actor»` | Khách hàng (Member), Core Banking, Support Agent, Kế toán | Con người và hệ thống nguồn tham gia luồng nghiệp vụ |
| 6 | **Business** | `◖ «Business Service»` | Tích điểm, Nâng hạng Silver/Gold/Platinum, Đổi thưởng, Báo cáo Nợ | Gói dịch vụ cung cấp ra người dùng và nội bộ ngân hàng |
| 7 | **Business** | `⚙ «Business Process»` | Tích điểm từ Core Banking, Chu kỳ Hạng 30d Grace, Lệnh Duyệt kép | Các quy trình kinh doanh và luồng nghiệp vụ chuẩn |
| 8 | **Business** | `📄 «Business Object»` | Sổ cái Điểm (`point_transaction`), Số dư Điểm, QP, Đơn Đổi quà | Thực thể thông tin và dữ liệu kinh doanh tài chính |
| 9 | **Application** | `▣ «Application Component»` | Earning Engine, Tiering, Redemption, Program Mgmt, Analytics DW | 5 Microservices độc lập theo nguyên tắc Domain-Driven Design |
| 10 | **Application** | `⊸ «Application Interface»` | `settled_transactions`, `tier_event`, `cdc_stream`, REST Gateway | Giao diện API và hàng đợi điều phối sự kiện phân tán Kafka |
| 11 | **Application** | `💾 «Data Object»` | `point_transaction`, `point_balance`, `qp_ledger`, `fact_*` | Mô hình dữ liệu quan hệ và Star Schema Data Warehouse |
| 12 | **Technology** | `🧊 «Node»` / `⚙ «System SW»` | Kubernetes Cluster, Kafka, Redis Cluster, PostgreSQL, ClickHouse | Nền tảng hạ tầng tính toán, bộ nhớ đệm và CSDL phân tán |
| 13 | **Technology** | `💾 «Artifact»` | `earning-engine.jar`, Docker Containers, `*.sql` Migration Scripts | Các tệp nhị phân đóng gói triển khai thực tế trên K8s |
| 14 | **Implementation** | `📋 «Work Package»` | WP1 (Hạ tầng) $\rightarrow$ WP6 (Dual-Control & Quality Gates 500 TPS) | Các gói công việc triển khai thực tế cho đồ án Capstone |
| 15 | **Implementation** | `🏁 «Plateau»` | Plateau 0 (Baseline) $\rightarrow$ Plateau 3 (Target State Hoàn chỉnh) | Các giai đoạn bàn giao kiến trúc qua từng chặng đồ án |
