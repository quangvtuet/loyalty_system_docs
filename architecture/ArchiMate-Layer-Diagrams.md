# Loyalty Banking — Kiến trúc Phân tầng ArchiMate 3.2 (ArchiMate Layer Diagrams)

**Domain**: Loyalty Banking Platform (Hệ thống Khách hàng Thân thiết Ngân hàng)  
**Tiêu chuẩn Kiến trúc**: The Open Group ArchiMate® 3.2 Specification  
**Phiên bản**: 3.2 (Sơ đồ Tối giản & Trực quan cho Báo cáo Capstone)  
**Tài liệu tham chiếu**:
- [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) — Tổng quan nghiệp vụ Loyalty
- [Architecture-Overview.md](file:///d:/learn/loyalty/architecture/Architecture-Overview.md) — Kiến trúc tổng thể hệ thống
- [Data-Architecture-and-Schema.md](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) — Kiến trúc dữ liệu & Star Schema
- [Security-and-Integration-Architecture.md](file:///d:/learn/loyalty/architecture/Security-and-Integration-Architecture.md) — Bảo mật & Tích hợp Đối tác
- [domain-event-catalog.md](file:///d:/learn/loyalty/architecture/domain-event-catalog.md) — Danh mục sự kiện Kafka

---

## 1. Tổng quan Khung Phân tầng Kiến trúc (ArchiMate Core Stack)

```mermaid
flowchart TD
    M["1. Motivation & Strategy Layer\n(Mục tiêu ≥ 500 TPS, Sổ cái Bất biến, 3 Nguyên tắc ADRs)"] --> B["2. Business Layer\n(5 Quy trình: Tích điểm, Xét hạng, Đổi quà FIFO, Duyệt kép, Báo cáo nợ)"]
    B --> A["3. Application Layer\n(5 Microservices: Earning, Tiering, Redemption, Program, Analytics)"]
    A --> T["4. Technology Layer\n(Hạ tầng: K8s Container, Kafka, Redis, PostgreSQL per service, ClickHouse)"]
    I["5. Implementation Layer\n(Lộ trình: Plateau 0 -> Plateau 3 với 6 Work Packages)"] -.-> M
    I -.-> B
    I -.-> A
    I -.-> T

    style M fill:#f8d7da,stroke:#721c24,stroke-width:2px,color:#000
    style B fill:#fff3cd,stroke:#856404,stroke-width:2px,color:#000
    style A fill:#d1ecf1,stroke:#0c5460,stroke-width:2px,color:#000
    style T fill:#d4edda,stroke:#155724,stroke-width:2px,color:#000
    style I fill:#e2e3e5,stroke:#383d41,stroke-width:2px,color:#000
```

---

## 2. Motivation & Strategy Layer (Tầng Động lực & Chiến lược)

```mermaid
flowchart TB
    subgraph Motivation["Motivation Layer (Động lực & Mục tiêu)"]
        direction TB
        SH["Stakeholders\n(Lãnh đạo, Marketing, Kế toán, Chủ thẻ, Đối tác)"]
        DR["Drivers\n(Giữ chân khách hàng, Mở rộng liên minh, Tránh thất thoát điểm)"]
        GOAL["Goals\n(≥ 500 TPS, SLA ≤ 60s, Sổ cái Bất biến 100%)"]
        PRIN["Principles\n(ADR-001 Cô lập DB, ADR-002 Hướng sự kiện, ADR-003 Tách biệt OLAP)"]

        SH --> DR
        DR --> GOAL
        PRIN -.-> GOAL
    end

    subgraph Strategy["Strategy Layer (Năng lực & Chuỗi Giá trị)"]
        direction TB
        CAP["Core Capabilities\n(Tích điểm tốc độ cao, Xét hạng tự động, Đổi quà đa kênh, Phân tích nợ điểm)"]
        
        subgraph ValueStream["Value Stream: Hành trình Khách hàng Loyalty"]
            V1["1. Chi tiêu Thẻ"] --> V2["2. Tích điểm & Nâng hạng"]
            V2 --> V3["3. Đổi Quà & Voucher"]
            V3 --> V4["4. Gắn kết & Duy trì"]
        end
    end

    GOAL ==> CAP
    CAP --> V2
    CAP --> V3

    style Motivation fill:#fdf2f8,stroke:#db2777,stroke-width:2px,color:#000
    style Strategy fill:#faf5ff,stroke:#9333ea,stroke-width:2px,color:#000
```

---

## 3. Business Layer (Tầng Nghiệp vụ Loyalty)

```mermaid
flowchart TD
    subgraph Actors["Tác nhân Nghiệp vụ (Business Actors)"]
        direction LR
        A1["Chủ thẻ (Member)"]
        A2["Core Banking"]
        A3["Quản trị viên (Admin)"]
        A4["Kế toán (Finance)"]
        A5["Đối tác (Partner)"]
    end

    subgraph Services["Dịch vụ Nghiệp vụ (Business Services)"]
        direction LR
        S1["Dịch vụ Tích điểm"]
        S2["Dịch vụ Xét hạng"]
        S3["Dịch vụ Đổi quà"]
        S4["Dịch vụ Duyệt kép"]
        S5["Dịch vụ Báo cáo Nợ"]
    end

    subgraph Processes["5 Quy trình Nghiệp vụ Cốt lõi (Business Processes)"]
        direction TB
        P1["BP 01: Tích điểm Giao dịch & Ghi Sổ cái Bất biến"]
        P2["BP 02: Tích lũy QP, Nâng hạng Tức thì & Ân hạn 30 ngày"]
        P3["BP 03: Đổi quà Danh mục, Trừ điểm FIFO & Đối tác"]
        P4["BP 04: Điều chỉnh Điểm Kiểm soát Kép (Ngưỡng 500 điểm)"]
        P5["BP 05: Tổng hợp CDC & Báo cáo Nợ điểm Tài chính"]
    end

    subgraph Objects["Thực thể Dữ liệu Nghiệp vụ (Business Objects)"]
        direction LR
        O1[("Sổ cái Điểm (Ledger)")]
        O2[("Số dư Khả dụng")]
        O3[("Hạng thẻ & QP")]
        O4[("Đơn Đổi quà")]
        O5[("Báo cáo Nợ & Breakage")]
    end

    Actors --> Services
    Services --> Processes
    P1 --> O1
    P1 --> O2
    P2 --> O3
    P3 --> O4
    P4 --> O1
    P5 --> O5

    style Actors fill:#fffbeb,stroke:#f59e0b,stroke-width:1px,color:#000
    style Services fill:#fef3c7,stroke:#d97706,stroke-width:2px,color:#000
    style Processes fill:#fef9c3,stroke:#ca8a04,stroke-width:2px,color:#000
    style Objects fill:#ecfdf5,stroke:#10b981,stroke-width:1px,color:#000
```

---

## 4. Application Layer (Tầng Ứng dụng & Microservices)

```mermaid
flowchart TD
    subgraph Clients["Clients & External Systems"]
        direction LR
        CLI_MOB["Mobile App"]
        CLI_WEB["Admin / Web Portal"]
        CLI_PART["Partner API"]
        CLI_CORE["Core Banking Stream"]
    end

    subgraph Gateway["Edge Gateway"]
        GW["API Gateway & OAuth 2.0 Auth Server\n(JWT Validation, RBAC, Rate Limit)"]
    end

    subgraph Microservices["5 Core Domain Microservices"]
        direction LR
        EE["Earning Engine Service\n(Tích điểm, Sổ cái, Idempotency)"]
        TS["Tiering System Service\n(QP Ledger, Nâng hạng, 30d Grace)"]
        RE["Redemption Engine Service\n(Kho quà, Trừ điểm FIFO, Đối tác)"]
        PM["Program Management Service\n(Cấu hình, Chiến dịch, Duyệt kép)"]
        AR["Analytics & Reporting Service\n(Kho phân tích, 7 Báo cáo chuẩn)"]
    end

    subgraph EventBus["Message Broker (Apache Kafka)"]
        direction LR
        K_TXN["topic: settled_transactions"]
        K_TIER["topic: tier_event / tier_change"]
        K_RED["topic: redemption_event"]
        K_CDC["topic: cdc_stream (Debezium)"]
    end

    CLI_MOB --> GW
    CLI_WEB --> GW
    CLI_PART --> GW
    CLI_CORE --> K_TXN

    GW --> EE
    GW --> TS
    GW --> RE
    GW --> PM
    GW --> AR

    K_TXN --> EE
    EE <--> K_TIER
    K_TIER <--> TS
    RE <--> K_RED
    K_RED --> EE

    EE -.-> K_CDC
    TS -.-> K_CDC
    RE -.-> K_CDC
    PM -.-> K_CDC
    K_CDC --> AR

    style Gateway fill:#e0f2fe,stroke:#0284c7,stroke-width:2px,color:#000
    style Microservices fill:#dbeafe,stroke:#2563eb,stroke-width:2px,color:#000
    style EventBus fill:#f1f5f9,stroke:#475569,stroke-width:1px,color:#000
```

---

## 5. Technology & Infrastructure Layer (Tầng Công nghệ & Hạ tầng Tinh gọn)

```mermaid
flowchart TD
    subgraph Edge["1. Khối Cổng Tiếp nhận (Edge & Security)"]
        INGRESS["API Gateway / Ingress Controller\n(Xác thực Token JWT, Định tuyến REST/gRPC, Rate Limit 1.000 req/min)"]
    end

    subgraph Compute["2. Khối Tính toán & Dịch vụ (Kubernetes Cluster)"]
        direction LR
        P_EE["Earning Engine Pod"]
        P_TS["Tiering System Pod"]
        P_RE["Redemption Engine Pod"]
        P_PM["Program Mgmt Pod"]
        P_AR["Analytics Pod"]
    end

    subgraph Middleware["3. Khối Truyền thông & Bộ nhớ đệm (Middleware Tier)"]
        direction LR
        KAFKA[("Apache Kafka\n(Event Streaming Broker)")]
        REDIS[("Redis Cluster\n(Khóa Redlock & Idempotency)")]
    end

    subgraph Storage["4. Khối CSDL Phân lập (Database-per-Service)"]
        direction LR
        DB_EE[("Earning DB\n(PostgreSQL)")]
        DB_TS[("Tiering DB\n(PostgreSQL)")]
        DB_RED[("Redemption DB\n(PostgreSQL)")]
        DB_PM[("Program DB\n(PostgreSQL)")]
        DB_DW[("Analytics DW\n(ClickHouse Star Schema)")]
    end

    INGRESS --> Compute

    Compute <-->|Pub / Sub Events| KAFKA
    P_EE <-->|Idempotency Lock| REDIS
    P_RE <-->|FIFO Lock| REDIS

    P_EE --- DB_EE
    P_TS --- DB_TS
    P_RE --- DB_RED
    P_PM --- DB_PM
    P_AR --- DB_DW

    DB_EE -.->|CDC| KAFKA
    DB_TS -.->|CDC| KAFKA
    DB_RED -.->|CDC| KAFKA
    DB_PM -.->|CDC| KAFKA
    KAFKA -->|CDC Stream| P_AR

    style Edge fill:#e8f4f8,stroke:#007acc,stroke-width:2px,color:#000
    style Compute fill:#e8f8f5,stroke:#2ecc71,stroke-width:2px,color:#000
    style Middleware fill:#fef9e7,stroke:#f1c40f,stroke-width:2px,color:#000
    style Storage fill:#fdedec,stroke:#e74c3c,stroke-width:2px,color:#000
```

---

## 6. Implementation & Migration Layer (Lộ trình Triển khai Capstone)

```mermaid
flowchart LR
    subgraph Plateaus["4 Chặng Chuyển giao Kiến trúc (Architecture Plateaus)"]
        PL0["Plateau 0: Thiết kế Cơ sở & Specs"]
        PL1["Plateau 1: MVP Tích điểm & Hạng thẻ"]
        PL2["Plateau 2: Đổi quà & Tích hợp Đối tác"]
        PL3["Plateau 3: Hệ thống Doanh nghiệp Hoàn chỉnh"]
    end

    subgraph Packages["6 Gói Công việc (Work Packages)"]
        direction TB
        WP1["WP 1: Thiết lập Hạ tầng K8s & Kafka Bus"]
        WP2["WP 2: Sổ cái Bất biến & Khóa Idempotency"]
        WP3["WP 3: Xét hạng Động & Ân hạn 30 ngày"]
        WP4["WP 4: Đổi quà FIFO & Cổng OAuth Đối tác"]
        WP5["WP 5: Kho Dữ liệu Star Schema & CDC Stream"]
        WP6["WP 6: Kiểm soát Kép & Kiểm thử 500 TPS"]
    end

    subgraph Deliverables["Sản phẩm Đồ án (Deliverables)"]
        direction TB
        D1["Tài liệu Kiến trúc & OpenAPI Specs"]
        D2["5 Microservices Docker Containers"]
        D3["CSDL PostgreSQL & ClickHouse DW"]
        D4["Mobile App & Web Portals"]
        D5["Báo cáo Quality Gates (≥ 500 TPS, 0 Duplication)"]
    end

    PL0 --> WP1
    WP1 --> PL1
    WP2 --> PL1
    WP3 --> PL1
    PL1 --> WP4
    WP4 --> PL2
    PL2 --> WP5
    PL2 --> WP6
    WP5 --> PL3
    WP6 --> PL3

    PL3 -.-> Deliverables

    style Plateaus fill:#fff7ed,stroke:#ea580c,stroke-width:2px,color:#000
    style Packages fill:#f8fafc,stroke:#64748b,stroke-width:1px,color:#000
    style Deliverables fill:#ecfdf5,stroke:#059669,stroke-width:2px,color:#000
```

---

## 7. Cross-Layer Traceability View (Sơ đồ Tích hợp Đa tầng)

```mermaid
flowchart TB
    subgraph L1["1. Motivation & Strategy Layer"]
        G1["Mục tiêu: Sổ cái Bất biến & Không trùng lặp Bút toán"]
        G2["Mục tiêu: Thông lượng ≥ 500 sự kiện/giây (p95 < 2s)"]
        CAP["Năng lực: Tích điểm Thời gian thực Quy mô lớn"]
        G1 -.-> CAP
        G2 -.-> CAP
    end

    subgraph L2["2. Business Layer"]
        BS["Dịch vụ Nghiệp vụ: Dịch vụ Tích điểm Giao dịch"]
        BP["Quy trình Nghiệp vụ: Tiếp nhận GD & Ghi Sổ cái Bất biến"]
        BO[("Thực thể: Bút toán Sổ cái (point_transaction)")]
        CAP ==> BS
        BS --> BP
        BP --> BO
    end

    subgraph L3["3. Application Layer"]
        AS["Dịch vụ Ứng dụng: Earn Ingestion & Rule Evaluator API"]
        AC["Thành phần Ứng dụng: Earning Engine Service (Microservice)"]
        DO["Thực thể Dữ liệu: point_transaction & point_balance"]
        BP -.->|Hiện thực hóa| AS
        AS --> AC
        AC --> DO
    end

    subgraph L4["4. Technology Layer"]
        T_KAFKA["Dịch vụ Công nghệ: Apache Kafka (settled_transactions)"]
        T_REDIS["Dịch vụ Công nghệ: Redis Cluster (Khóa Idempotency)"]
        T_PG["CSDL Vật lý: PostgreSQL (Partitioned Ledger)"]
        T_K8S["Hạ tầng Tính toán: Kubernetes Worker Pods (HPA)"]

        AC -.->|Vận hành trên| T_K8S
        AC -.->|Tiêu thụ từ| T_KAFKA
        AC -.->|Khóa giao dịch với| T_REDIS
        DO -.->|Lưu trữ tại| T_PG
    end

    L1 ==> L2
    L2 ==> L3
    L3 ==> L4

    style L1 fill:#fdf2f8,stroke:#db2777,stroke-width:2px,color:#000
    style L2 fill:#fffbeb,stroke:#d97706,stroke-width:2px,color:#000
    style L3 fill:#eff6ff,stroke:#2563eb,stroke-width:2px,color:#000
    style L4 fill:#f0fdf4,stroke:#16a34a,stroke-width:2px,color:#000
```

---

## 8. Bảng Ma trận Ánh xạ Truy vết Phần tử ArchiMate (Traceability Matrix)

| STT | ArchiMate Layer | Loại Phần tử (Element Type) | Tên Phần tử trong Dự án Loyalty | Trách nhiệm & Ý nghĩa Nghiệp vụ / Kỹ thuật |
|:---:|---|---|---|---|
| 1 | **Motivation** | Stakeholder | Ban Lãnh đạo, Marketing, Kế toán, Chủ thẻ, Đối tác | Các bên tham gia trực tiếp vào chuỗi giá trị Loyalty |
| 2 | **Motivation** | Driver / Goal | Thông lượng $\ge 500$ TPS, SLA $\le 60$s, Sổ cái Bất biến | Chỉ số đo lường hiệu năng và cam kết dịch vụ tài chính |
| 3 | **Motivation** | Principle | ADR-001 (Cô lập DB), ADR-002 (Sự kiện/Idempotency), ADR-003 (CDC) | 3 quyết định kiến trúc cốt lõi định hình nền tảng |
| 4 | **Strategy** | Capability | Tích điểm tốc độ cao, Quản lý Hạng thẻ động, Đổi quà FIFO | Năng lực cốt lõi phục vụ hệ sinh thái Loyalty Ngân hàng |
| 5 | **Business** | Business Actor | Khách hàng (Member), Core Banking, Support Agent, Kế toán | Con người và hệ thống nguồn tham gia luồng nghiệp vụ |
| 6 | **Business** | Business Service | Tích điểm, Nâng hạng Silver/Gold/Platinum, Đổi thưởng, Báo cáo Nợ | Gói dịch vụ cung cấp ra người dùng và nội bộ ngân hàng |
| 7 | **Business** | Business Process | Tích điểm từ Core Banking, Chu kỳ Hạng 30d Grace, Lệnh Duyệt kép | Các quy trình kinh doanh và luồng nghiệp vụ chuẩn |
| 8 | **Business** | Business Object | Sổ cái Điểm (`point_transaction`), Số dư Điểm, QP, Đơn Đổi quà | Thực thể thông tin và dữ liệu kinh doanh tài chính |
| 9 | **Application** | App Component | Earning Engine, Tiering, Redemption, Program Mgmt, Analytics DW | 5 Microservices độc lập theo nguyên tắc Domain-Driven Design |
| 10 | **Application** | App Interface / Topic | `settled_transactions`, `tier_event`, `cdc_stream`, REST Gateway | Giao diện API và hàng đợi điều phối sự kiện phân tán Kafka |
| 11 | **Application** | Data Object | `point_transaction`, `point_balance`, `qp_ledger`, `fact_*` | Mô hình dữ liệu quan hệ và Star Schema Data Warehouse |
| 12 | **Technology** | Node / System Software | Kubernetes Cluster, Kafka, Redis Cluster, PostgreSQL, ClickHouse | Nền tảng hạ tầng tính toán, bộ nhớ đệm và CSDL phân tán |
| 13 | **Technology** | Artifact | `earning-engine.jar`, Docker Containers, `*.sql` Migration Scripts | Các tệp nhị phân đóng gói triển khai thực tế trên K8s |
| 14 | **Implementation** | Work Package | WP1 (Hạ tầng) $\rightarrow$ WP6 (Dual-Control & Quality Gates 500 TPS) | Các gói công việc triển khai thực tế cho đồ án Capstone |
| 15 | **Implementation** | Plateau | Plateau 0 (Baseline) $\rightarrow$ Plateau 3 (Target State Hoàn chỉnh) | Các giai đoạn bàn giao kiến trúc qua từng chặng đồ án |
