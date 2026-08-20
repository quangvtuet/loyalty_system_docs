# Loyalty Banking — Kiến trúc Phân tầng ArchiMate 3.2 (ArchiMate Layer Diagrams)

**Domain**: Loyalty Banking Platform (Hệ thống Khách hàng Thân thiết Ngân hàng)  
**Tiêu chuẩn Kiến trúc**: The Open Group ArchiMate® 3.2 Specification  
**Phiên bản**: 3.0 (Chuẩn hóa Chính tả & Tối giản Hạ tầng)  
**Tài liệu tham chiếu**:
- [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) — Tổng quan nghiệp vụ Loyalty
- [Architecture-Overview.md](file:///d:/learn/loyalty/architecture/Architecture-Overview.md) — Kiến trúc tổng thể hệ thống
- [Data-Architecture-and-Schema.md](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) — Kiến trúc dữ liệu & Star Schema
- [Security-and-Integration-Architecture.md](file:///d:/learn/loyalty/architecture/Security-and-Integration-Architecture.md) — Bảo mật & Tích hợp Đối tác
- [domain-event-catalog.md](file:///d:/learn/loyalty/architecture/domain-event-catalog.md) — Danh mục sự kiện Kafka

---

## 1. Tổng quan Khung Kiến trúc ArchiMate trong Ngữ cảnh Loyalty Banking

Khung kiến trúc hệ thống **Loyalty Banking** được thiết kế nhằm giải quyết bài toán cốt lõi: **Xử lý tích điểm giao dịch ngân hàng thời gian thực (Real-time Accrual), duy trì sổ cái điểm bất biến (Immutable Ledger), tự động hóa vòng đời hạng thẻ (Tier Lifecycle), đổi thưởng tức thì (FIFO Redemption), và kiểm soát nợ điểm tài chính (Point Liability Reporting).**

```mermaid
flowchart TD
    M["1. Motivation & Strategy Layer
    (Động lực & Chiến lược: Giữ chân khách hàng, Mở rộng liên minh, SLA ≤ 60s, Sổ cái bất biến)"] --> B["2. Business Layer
    (Nghiệp vụ: Tích điểm, Nâng hạng Silver/Gold/Platinum, Đổi quà FIFO, Bút toán kép, Báo cáo nợ)"]
    
    B --> A["3. Application Layer
    (5 Microservices: Earning Engine, Tiering, Redemption, Program Mgmt, Analytics DW + Kafka)"]
    
    A --> T["4. Technology & Infrastructure Layer
    (Hạ tầng Tối giản: K8s Cluster, Kafka Event Bus, Redis Cache/Lock, PostgreSQL DB-per-Service, Analytics DW)"]
    
    I["5. Implementation & Migration Layer
    (Lộ trình Triển khai: Plateau 0 -> Plateau 3 với 6 Work Packages)"] -.-> M
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

## 2. Motivation & Strategy Layer (Tầng Động lực & Chiến lược Loyalty)

### 2.1. Phân tích Chi tiết Ngữ cảnh Loyalty
- **Động lực Kinh doanh (Drivers)**: Ngân hàng cần gia tăng tỷ lệ sử dụng thẻ (Card Spend), thúc đẩy giao dịch thanh toán số, mở rộng mạng lưới liên minh đối tác (Merchant Coalition), đồng thời bảo đảm an toàn tài chính (ngăn ngừa thất thoát và gian lận điểm thưởng).
- **Mục tiêu Đo lường (Goals)**:
  - Thông lượng xử lý $\ge 500$ sự kiện tích điểm/giây với độ trễ $p95 \le 2.000$ ms.
  - Hấp thụ giao dịch quyết toán từ Core Banking trong vòng $\le 60$ giây.
  - Sổ cái điểm bất biến (100% Immutable Append-Only Ledger), 0% trùng lặp giao dịch (Idempotency).
  - Tự động hóa đánh giá và thăng hạng tức thì khi đủ Điểm xét hạng (Qualifying Points - QP).
- **Nguyên tắc Kiến trúc (Principles / ADRs)**:
  - **ADR-001**: Phân lập dữ liệu hoàn toàn giữa các Module (Database-per-Service), nghiêm cấm gọi chéo CSDL.
  - **ADR-002**: Kiến trúc hướng sự kiện (EDA) với Kafka và khóa phân tán Redis Idempotency.
  - **ADR-003**: Tách biệt 100% CSDL giao dịch (OLTP) và CSDL phân tích (OLAP Star Schema) qua Debezium CDC.
  - **Bảo mật**: Cơ chế phê duyệt kép (Dual-Control) đối với các bút toán thủ công $> 500$ điểm.

### 2.2. Sơ đồ ArchiMate Motivation & Strategy

```mermaid
flowchart TB
    subgraph Motivation["Motivation Layer (Động lực Kiến trúc Loyalty)"]
        direction TB
        
        subgraph Stakeholders["Stakeholders (Bên liên quan)"]
            SH_EXEC["Stakeholder: Ban Lãnh đạo Ngân hàng"]
            SH_MKT["Stakeholder: Quản lý Tiếp thị & Loyalty"]
            SH_FIN["Stakeholder: Kế toán & Quản trị Rủi ro"]
            SH_CUST["Stakeholder: Khách hàng / Chủ thẻ Ngân hàng"]
            SH_PART["Stakeholder: Đối tác Liên minh Đổi thưởng"]
        end

        subgraph Drivers["Drivers (Động cơ Thúc đẩy)"]
            DR_RET["Driver: Giữ chân Khách hàng & Tăng chi tiêu Thẻ"]
            DR_ECO["Driver: Mở rộng Hệ sinh thái Liên minh Đối tác"]
            DR_AUD["Driver: Minh bạch Tài chính & Tránh thất thoát Điểm"]
            DR_SLA["Driver: Trải nghiệm Thưởng Tức thì Thời gian thực"]
        end

        subgraph Goals["Goals (Mục tiêu Hệ thống Loyalty)"]
            G_TXN["Goal: Xử lý ≥ 500 sự kiện/giây (p95 < 2s, SLA ≤ 60s)"]
            G_LEDGER["Goal: 100% Sổ cái Bất biến & Không trùng lặp Bút toán"]
            G_TIER["Goal: Nâng hạng Tức thì & Tự động hóa Ân hạn 30 ngày"]
            G_EXP["Goal: Giám sát Điểm hết hạn & Nợ tài chính (< 10 phút)"]
        end

        subgraph Principles["Principles (Nguyên tắc Thiết kế Cốt lõi)"]
            PR_ISO["Principle: ADR-001 Phân lập Module & Dữ liệu CSDL"]
            PR_EVT["Principle: ADR-002 Hướng Sự kiện & Khóa Idempotency"]
            PR_OLAP["Principle: ADR-003 Tách biệt Giao dịch và Phân tích (CDC)"]
            PR_SEC["Principle: Bảo mật Zero-Trust & Kiểm soát Kép (Dual-Control)"]
        end
    end

    subgraph Strategy["Strategy Layer (Chiến lược & Năng lực Loyalty)"]
        direction TB
        
        subgraph Capabilities["Core Loyalty Capabilities (Năng lực Cốt lõi)"]
            CAP_EARN["Capability: Tích điểm Thời gian thực Quy mô lớn"]
            CAP_TIER["Capability: Quản lý Vòng đời Hạng thẻ Tự động"]
            CAP_RED["Capability: Đổi thưởng Đa kênh Danh mục Đối tác"]
            CAP_PROG["Capability: Quản trị Chương trình & Chiến dịch Khuyến mãi"]
            CAP_ANL["Capability: Phân tích Nợ điểm & Dự báo Điểm hết hạn"]
            CAP_GOV["Capability: Tích hợp Đối tác OAuth & Kiểm soát Bút toán"]
        end

        subgraph ValueStream["Value Stream: Customer Loyalty Journey (Chuỗi Giá trị Khách hàng)"]
            VS_ENGAGE["1. Chi tiêu & Giao dịch Thẻ\n(Giao dịch qua POS / E-commerce)"] --> VS_ACCRUE["2. Tích điểm & Thăng hạng\n(Cộng điểm & Nâng hạng tức thì)"]
            VS_ACCRUE --> VS_REWARD["3. Đổi quà & Voucher\n(Đổi quà tặng, dặm bay, mã ưu đãi)"]
            VS_REWARD --> VS_RETAIN["4. Tái gắn kết & Duy trì\n(Tăng cường gắn bó với ngân hàng)"]
        end
    end

    %% Mapping
    SH_EXEC --> DR_RET
    SH_MKT --> DR_ECO
    SH_FIN --> DR_AUD
    SH_CUST --> DR_SLA
    SH_PART --> DR_ECO

    DR_RET --> G_TXN
    DR_AUD --> G_LEDGER
    DR_SLA --> G_TIER
    DR_ECO --> G_EXP

    G_TXN -.-> CAP_EARN
    G_LEDGER -.-> CAP_EARN
    G_TIER -.-> CAP_TIER
    G_EXP -.-> CAP_ANL

    PR_ISO -.-> CAP_EARN
    PR_ISO -.-> CAP_RED
    PR_EVT -.-> CAP_EARN
    PR_OLAP -.-> CAP_ANL
    PR_SEC -.-> CAP_GOV

    CAP_EARN --> VS_ACCRUE
    CAP_TIER --> VS_ACCRUE
    CAP_RED --> VS_REWARD
```

---

## 3. Business Layer (Tầng Nghiệp vụ Loyalty Banking)

### 3.1. Các Khái niệm & Quy trình Nghiệp vụ Cốt lõi
1. **Quy trình Tích điểm (Earning Process)**:
   - Tỷ lệ cơ bản (Base Rate): **1 USD = 1 điểm** (hoặc giá trị quy đổi VNĐ tương đương).
   - Hệ số nhân Hạng thẻ (Tier Multiplier): Silver (1.0×), Gold (1.5×), Platinum (2.0×).
   - Chiến dịch khuyến mãi (Campaign Multipliers): Ví dụ "Double Points August" (2.0×).
   - Ghi nhận bút toán vào **Sổ cái (Earning Ledger)** dưới dạng bất biến (append-only) và cập nhật số dư tức thời (`point_balance`). Thiết lập hạn sử dụng điểm (Rolling 12 tháng hoặc Fixed 31/12).
2. **Quy trình Quản lý Hạng thẻ (Tiering Lifecycle)**:
   - Tích lũy **Điểm xét hạng (Qualifying Points - QP)** tách biệt hoàn toàn với điểm thưởng đổi quà.
   - Ngưỡng thăng hạng chuẩn: Silver (0 QP), Gold (1.000 QP), Platinum (3.000 QP).
   - Thăng hạng (Upgrade): Có hiệu lực **ngay lập tức** khi điểm QP tích lũy vượt ngưỡng.
   - Hạ hạng (Downgrade): Đánh giá vào cuối chu kỳ năm (31/12), áp dụng **30 ngày ân hạn (Grace Period)**, mỗi chu kỳ chỉ hạ tối đa 1 bậc.
3. **Quy trình Đổi thưởng (Redemption Process)**:
   - Thành viên duyệt danh mục quà tặng (E-voucher, Dặm bay, Tiền mặt/Cashback).
   - Khóa điểm và trừ điểm theo nguyên tắc **FIFO (First-In, First-Out)** đối với các lô điểm cũ nhất.
   - Gửi yêu cầu phát hành thưởng tới API đối tác. Nếu đối tác xử lý thất bại $\rightarrow$ tự động kích hoạt quy trình bù trừ (Reversal/Compensation) để hoàn điểm cho khách hàng.
4. **Quy trình Điều chỉnh Bút toán Kép (Dual-Control Adjustment)**:
   - Chuyên viên CSKH/Support tạo phiếu yêu cầu điều chỉnh.
   - Bút toán $\le 500$ điểm: Hệ thống tự động phê duyệt.
   - Bút toán $> 500$ điểm: Chuyển Supervisor/Admin phê duyệt trước khi ghi vào Sổ cái.
5. **Quy trình Đối soát & Báo cáo Nợ điểm (Liability & Breakage)**:
   - CDC trích xuất biến động sổ cái sang Data Warehouse.
   - Tính toán nợ điểm chưa sử dụng (Outstanding Liability), dự báo điểm hết hạn (Breakage Forecast), và doanh thu đối tác.

### 3.2. Sơ đồ ArchiMate Business Layer

```mermaid
flowchart TB
    subgraph BusinessLayer["Business Layer (Tầng Nghiệp vụ Loyalty)"]
        
        subgraph BusinessActors["Business Actors & Roles (Tác nhân & Vai trò Nghiệp vụ)"]
            BA_MEMBER["Actor: Khách hàng / Chủ thẻ (Member)"]
            BA_ADMIN["Actor: Quản trị viên Chương trình (Admin)"]
            BA_FINANCE["Actor: Chuyên viên Kế toán & Tài chính (Finance)"]
            BA_SUPPORT["Actor: Chuyên viên Hỗ trợ Khách hàng (Support)"]
            BA_PARTNER["Actor: Đối tác Liên minh Đổi thưởng (Partner)"]
            BA_CORE["Actor: Hệ thống Quyết toán Ngân hàng Lõi (Core Banking)"]
        end

        subgraph BusinessServices["Business Services (Dịch vụ Nghiệp vụ Khách hàng & Nội bộ)"]
            BS_ENROLL["BS: Dịch vụ Đăng ký & Quản lý Tài khoản Hội viên"]
            BS_EARN["BS: Dịch vụ Tích điểm Giao dịch & Chiến dịch"]
            BS_TIERING["BS: Dịch vụ Xét hạng & Đặc quyền Thành viên"]
            BS_REDEMPTION["BS: Dịch vụ Duyệt & Đổi thưởng Danh mục"]
            BS_CAMPAIGN["BS: Dịch vụ Cấu hình Chương trình & Khuyến mãi"]
            BS_ADJUSTMENT["BS: Dịch vụ Điều chỉnh Điểm Kiểm soát Kép"]
            BS_LIABILITY["BS: Dịch vụ Báo cáo Nợ điểm & Đối soát Tài chính"]
        end

        subgraph BusinessProcesses["Business Processes (Quy trình Nghiệp vụ Chi tiết)"]
            direction TB
            BP_EARN["BP 01: Quy trình Tích điểm Giao dịch Core Banking
            (Nhận GD quyết toán -> Tính điểm Base+Multiplier -> Ghi Sổ cái -> Cập nhật Số dư -> Đặt Hạn dùng)"]
            
            BP_TIER["BP 02: Quy trình Xét hạng Động & Quản lý Ân hạn
            (Tích lũy QP -> Nâng hạng Tức thì -> Quét Định kỳ Cuối năm -> Kích hoạt Ân hạn 30 ngày -> Hạ bậc)"]
            
            BP_RED["BP 03: Quy trình Đổi thưởng Danh mục & Đối tác
            (Kiểm tra Hạng & Điểm -> Trừ điểm FIFO -> Gọi API Đối tác -> Xử lý Bù trừ / Hoàn điểm nếu lỗi)"]
            
            BP_ADJ["BP 04: Quy trình Điều chỉnh Điểm Thủ công (Dual-Control)
            (Support tạo phiếu -> Kiểm tra Ngưỡng 500 điểm -> Quản lý Duyệt -> Ghi Sổ cái Bất biến)"]
            
            BP_REP["BP 05: Quy trình Đối soát Nợ điểm & Dự báo Điểm Hết hạn
            (CDC đồng bộ Sổ cái -> Tổng hợp Star Schema -> Xuất 7 Báo cáo Chuẩn Nợ & Hết hạn)"]
        end

        subgraph BusinessObjects["Business Objects (Thực thể Nghiệp vụ Cốt lõi)"]
            BO_PROGRAM["BO: Chương trình Loyalty & Chiến dịch Khuyến mãi"]
            BO_RULE["BO: Quy tắc Tích điểm (Earn Rule) & Quy tắc Hạng (Tier Rule)"]
            BO_LEDGER["BO: Bút toán Sổ cái Bất biến (Point Transaction Ledger)"]
            BO_BALANCE["BO: Snapshot Số dư Điểm Khả dụng (Point Balance)"]
            BO_TIER["BO: Thông tin Hạng thẻ (Silver/Gold/Platinum) & Sổ cái QP"]
            BO_ORDER["BO: Đơn Đổi thưởng, Voucher & Bản ghi Giao hàng Đối tác"]
            BO_REPORT["BO: Báo cáo Nợ điểm, Tỷ lệ Hết hạn & Đối soát Đối tác"]
        end

        subgraph BusinessProducts["Business Products (Sản phẩm Khách hàng Thân thiết)"]
            PROD_BANK["Product: Gói Khách hàng Thân thiết Ngân hàng (Thẻ & Ngân hàng Số)"]
            PROD_COALITION["Product: Mạng lưới Liên minh Thưởng Đối tác (Coalition Rewards)"]
        end
    end

    %% Actor to Service
    BA_MEMBER --> BS_ENROLL
    BA_MEMBER --> BS_REDEMPTION
    BA_CORE --> BS_EARN
    BA_PARTNER --> BS_EARN
    BA_ADMIN --> BS_CAMPAIGN
    BA_SUPPORT --> BS_ADJUSTMENT
    BA_FINANCE --> BS_LIABILITY

    %% Service to Process
    BS_EARN --> BP_EARN
    BS_TIERING --> BP_TIER
    BS_REDEMPTION --> BP_RED
    BS_ADJUSTMENT --> BP_ADJ
    BS_LIABILITY --> BP_REP

    %% Process to Object
    BP_EARN --> BO_LEDGER
    BP_EARN --> BO_BALANCE
    BP_TIER --> BO_TIER
    BP_RED --> BO_ORDER
    BP_ADJ --> BO_LEDGER
    BP_REP --> BO_REPORT

    %% Product realization
    PROD_BANK -.-> BS_EARN
    PROD_BANK -.-> BS_TIERING
    PROD_COALITION -.-> BS_REDEMPTION
```

---

## 4. Application Layer (Tầng Ứng dụng & Microservices Loyalty)

### 4.1. Phân rã 5 Microservices Độc lập (Domain-Driven Design)
1. **Earning Engine Service**:
   - Ingestion Worker tiêu thụ sự kiện từ topic `settled_transactions`.
   - Rule Evaluator tính điểm Base + Multiplier theo Hạng và Chiến dịch.
   - Ghi bút toán `point_transaction` (Append-only) và cập nhật `point_balance`.
   - Redis Lock ngăn chặn xử lý trùng lặp giao dịch (Idempotency Key = `tx_settlement_id`).
   - Lên lịch gửi cảnh báo hết hạn điểm (30 ngày và 7 ngày trước khi hết hạn).
2. **Tiering System Service**:
   - Quản lý `qp_ledger` và thực thể `member_tier`.
   - Lắng nghe `tier_event` từ Earning Engine $\rightarrow$ Tự động thăng hạng Silver $\rightarrow$ Gold $\rightarrow$ Platinum.
   - Cron Job chạy đánh giá cuối năm (31/12) $\rightarrow$ Kích hoạt trạng thái `IN_GRACE_PERIOD` (30 ngày).
   - Phát sự kiện `tier_change` sang Kafka để Earning Engine và Redemption Engine cập nhật quyền lợi.
3. **Redemption Engine Service**:
   - Quản lý kho quà `reward_item` (Voucher điện tử, Dặm bay, Sản phẩm vật lý).
   - Thực thi trừ điểm nguyên tử theo thuật toán **FIFO** dựa trên các lô điểm còn hiệu lực.
   - Dispatcher gọi REST API sang Đối tác Fulfillment; quản lý Saga Compensation (Hoàn điểm tự động nếu giao voucher thất bại).
4. **Program Management Service**:
   - Quản trị cấu hình `loyalty_program`, `campaign`, và phiên bản quy tắc (`config_version_log`).
   - Quản lý danh mục đối tác, cấp phát OAuth 2.0 Client Credentials, thực thi Rate Limit 1.000 req/min.
   - Quản lý quy trình kiểm soát kép (Dual-Control) cho lệnh điều chỉnh điểm thủ công.
5. **Analytics & Reporting Service**:
   - CDC Consumer nhận luồng biến động từ `cdc_stream` (Debezium).
   - Nạp dữ liệu vào Star Schema CSDL Phân tích (Fact & Dimension Tables).
   - Tính toán nợ điểm, tỷ lệ hết hạn (Breakage Rate), và cung cấp 7 báo cáo tài chính tiêu chuẩn.

### 4.2. Sơ đồ ArchiMate Application Layer

```mermaid
flowchart TB
    subgraph ApplicationLayer["Application Layer (Kiến trúc Ứng dụng & Dịch vụ Loyalty)"]
        
        subgraph AppInterfaces["User & Integration Interfaces (Giao diện Ứng dụng)"]
            IF_MOBILE["UI: Ứng dụng Mobile Banking (Tra cứu điểm & Đổi quà)"]
            IF_WEB["UI: Cổng Internet Banking Web"]
            IF_ADMIN["UI: Cổng Quản trị Chương trình & Chiến dịch"]
            IF_FINANCE["UI: Cổng Kế toán & Báo cáo Nợ điểm"]
            IF_SUPPORT["UI: Cổng Hỗ trợ CSKH & Điều chỉnh Điểm"]
            IF_PARTNER["API: Cổng Tích hợp Đối tác OAuth 2.0 (Earn & Catalog API)"]
        end

        subgraph EdgeGateway["Edge Routing & Security"]
            COMP_GW["App Component: API Gateway & OAuth 2.0 Auth Server
            (Xác thực Token JWT, Giới hạn Rate Limit Đối tác, Phân quyền RBAC)"]
        end

        subgraph DomainServices["Core Domain Microservices (5 Dịch vụ Độc lập)"]
            COMP_EE["App Component: Earning Engine Service
            • Worker tiêu thụ GD quyết toán
            • Bộ tính điểm Base + Multiplier
            • Quản lý Sổ cái Bất biến
            • Snapshot Số dư & Đặt Hạn dùng"]

            COMP_TS["App Component: Tiering System Service
            • Quản lý Sổ cái Điểm xét hạng (QP)
            • Bộ máy Nâng hạng Tức thì
            • Job Đánh giá Batch Cuối năm
            • Quản lý Trạng thái Ân hạn 30 ngày"]

            COMP_RE["App Component: Redemption Engine Service
            • Quản lý Danh mục Quà & Kho
            • Trừ điểm Nguyên tử theo FIFO
            • Điều phối Giao hàng Đối tác
            • Quy trình Bù trừ & Hoàn điểm Lỗi"]

            COMP_PM["App Component: Program Management Service
            • Vòng đời Chương trình & Chiến dịch
            • Quản lý Phiên bản Quy tắc
            • Quản lý Đối tác & Cấp OAuth
            • Quy trình Kiểm soát Kép (Dual-Control)"]

            COMP_AR["App Component: Analytics & Reporting Service
            • Tiêu thụ Luồng CDC Debezium
            • Tổng hợp Star Schema Kho Dữ liệu
            • Tính toán Nợ điểm & Tỷ lệ Hết hạn
            • API Xuất 7 Báo cáo Chuẩn"]
        end

        subgraph EventTopics["Event-Driven Message Broker (Apache Kafka Topics)"]
            TOPIC_SETTLED["Topic: settled_transactions\n(Core Banking -> Earning)"]
            TOPIC_TIER_EVT["Topic: tier_event\n(Earning -> Tiering)"]
            TOPIC_TIER_CHG["Topic: tier_change\n(Tiering -> Earning, Redemption)"]
            TOPIC_RED_EVT["Topic: redemption_event\n(Redemption -> Earning)"]
            TOPIC_CDC["Topic: cdc_stream\n(Tất cả Services -> Analytics DW)"]
            TOPIC_NOTIF["Topic: notifications_outbound\n(Tất cả Services -> CRM Gateway)"]
        end

        subgraph DataObjects["Application Data Objects (Mô hình Dữ liệu Đóng gói)"]
            DO_LEDGER["Data Object: point_transaction (Sổ cái) & point_balance (Snapshot)"]
            DO_TIER_DATA["Data Object: qp_ledger, member_tier & tier_rule"]
            DO_RED_DATA["Data Object: reward_item, redemption_order & fulfillment_record"]
            DO_PROG_DATA["Data Object: loyalty_program, campaign, partner & manual_adjustment_log"]
            DO_DW_DATA["Data Object: fact_point_transaction, fact_redemption_order, dim_*"]
        end

        subgraph ExternalSystems["External Integrated Platforms"]
            EXT_CORE["Ext App: Hệ thống Quyết toán Ngân hàng Lõi (Core Banking)"]
            EXT_CRM["Ext App: Cổng Thông báo CRM (SMS, Push Notification, Email)"]
            EXT_PARTNERS["Ext App: API Hệ thống Đối tác (Grab, Shopee, Vietnam Airlines)"]
        end
    end

    %% UI to Gateway
    IF_MOBILE --> COMP_GW
    IF_WEB --> COMP_GW
    IF_ADMIN --> COMP_GW
    IF_FINANCE --> COMP_GW
    IF_SUPPORT --> COMP_GW
    IF_PARTNER --> COMP_GW

    %% Gateway to Services
    COMP_GW -->|REST / gRPC| COMP_PM
    COMP_GW -->|REST / gRPC| COMP_RE
    COMP_GW -->|REST / gRPC| COMP_TS
    COMP_GW -->|REST / gRPC| COMP_AR
    COMP_GW -->|REST / gRPC| COMP_EE

    %% Upstream Core Banking Stream
    EXT_CORE -->|Đẩy sự kiện quyết toán| TOPIC_SETTLED
    TOPIC_SETTLED -->|Tiêu thụ sự kiện| COMP_EE

    %% Inter-service Event Choreography
    COMP_EE -->|Bắn sự kiện tích lũy QP| TOPIC_TIER_EVT
    TOPIC_TIER_EVT -->|Tiêu thụ sự kiện| COMP_TS

    COMP_TS -->|Thông báo Nâng/Hạ hạng| TOPIC_TIER_CHG
    TOPIC_TIER_CHG -->|Cập nhật Multiplier| COMP_EE
    TOPIC_TIER_CHG -->|Cập nhật Quyền lợi quà| COMP_RE

    COMP_RE -->|Thông báo điểm đã đổi| TOPIC_RED_EVT
    TOPIC_RED_EVT -->|Cập nhật trừ điểm FIFO| COMP_EE

    %% CDC Stream to Analytics
    COMP_EE -->|CDC| TOPIC_CDC
    COMP_TS -->|CDC| TOPIC_CDC
    COMP_RE -->|CDC| TOPIC_CDC
    COMP_PM -->|CDC| TOPIC_CDC
    TOPIC_CDC -->|Tiêu thụ & Tổng hợp| COMP_AR

    %% Notifications & Partner Fulfillment
    COMP_EE -->|Báo sắp hết hạn điểm| TOPIC_NOTIF
    COMP_TS -->|Báo nâng hạng/ân hạn| TOPIC_NOTIF
    COMP_RE -->|Báo đổi thưởng thành công| TOPIC_NOTIF
    TOPIC_NOTIF -->|Gửi tới khách hàng| EXT_CRM

    COMP_RE -->|Gọi API phát hành mã quà| EXT_PARTNERS

    %% Service to Data
    COMP_EE --- DO_LEDGER
    COMP_TS --- DO_TIER_DATA
    COMP_RE --- DO_RED_DATA
    COMP_PM --- DO_PROG_DATA
    COMP_AR --- DO_DW_DATA
```

---

## 5. Technology & Infrastructure Layer (Tầng Công nghệ & Hạ tầng Loyalty — Tối giản)

### 5.1. Khái quát Hạ tầng Kỹ thuật
Kiến trúc hạ tầng được thiết kế tinh gọn thành **4 khối chính**, dễ hiểu cho báo cáo Capstone nhưng vẫn đảm bảo toàn bộ yêu cầu phi chức năng ($\ge 500$ TPS, SLA $\le 60$s, 0% trùng lặp, CSDL cô lập):

1. **Khối Cổng & Mạng (Edge & Gateway)**: 
   - **API Gateway (Kong / Envoy)**: Tiếp nhận toàn bộ truy cập từ Mobile App, Web Portal và Partner API, giải mã Token JWT và giới hạn tần suất gọi (Rate Limiting).
2. **Khối Xử lý & Dịch vụ (Kubernetes Container Platform)**:
   - **Kubernetes Cluster**: Vận hành 5 Microservices đóng gói dưới dạng Docker Containers, hỗ trợ tự động co giãn Pods (HPA) khi lưu lượng giao dịch tăng cao.
3. **Khối Truyền thông & Đệm (Middleware & Event Streaming)**:
   - **Apache Kafka**: Hàng đợi tin nhắn phân tán lưu trữ các topic giao dịch (`settled_transactions`, `tier_event`, `cdc_stream`).
   - **Redis Cluster**: Bộ nhớ đệm tốc độ cao lưu trữ khóa Idempotency (ngăn xử lý trùng) và khóa phân tán `Redlock` (cho giao dịch trừ điểm FIFO).
4. **Khối Lưu trữ Dữ liệu (Polyglot Persistence)**:
   - **PostgreSQL Databases (Database-per-Service)**: Mỗi microservice sở hữu một CSDL riêng biệt (Earning DB, Tiering DB, Redemption DB, Program DB).
   - **Debezium CDC**: Tự động bắt biến động dữ liệu từ PostgreSQL đẩy sang Kafka.
   - **Data Warehouse (ClickHouse / PostgreSQL Star Schema)**: CSDL phân tích chuyên dụng lưu trữ Fact/Dim để xuất 7 báo cáo tài chính mà không ảnh hưởng CSDL giao dịch.

### 5.2. Sơ đồ ArchiMate Technology Layer (Tối giản & Trực quan)

```mermaid
flowchart TD
    subgraph TechLayer["Technology & Infrastructure Layer (Hạ tầng Kỹ thuật Tinh gọn)"]
        direction TB

        %% 1. EDGE & GATEWAY
        subgraph Layer_Edge["1. Khối Cổng Tiếp nhận & Bảo mật (Edge & Gateway)"]
            INGRESS["API Gateway / Ingress Controller\n(Xác thực JWT, Định tuyến REST/gRPC, Rate Limit 1.000 req/phút)"]
        end

        %% 2. COMPUTE
        subgraph Layer_Compute["2. Khối Tính toán & Vận hành (Kubernetes Container Platform)"]
            direction LR
            SVC_EE["Earning Engine\n(Container Pod)"]
            SVC_TS["Tiering System\n(Container Pod)"]
            SVC_RE["Redemption Engine\n(Container Pod)"]
            SVC_PM["Program Mgmt\n(Container Pod)"]
            SVC_AR["Analytics Service\n(Container Pod)"]
        end

        %% 3. MIDDLEWARE
        subgraph Layer_Middleware["3. Khối Truyền thông & Bộ nhớ đệm (Middleware Tier)"]
            direction LR
            KAFKA[("Apache Kafka Cluster\n(Message Broker: settled_transactions, tier_event, cdc_stream)")]
            REDIS[("Redis Cluster\n(In-Memory Cache, Khóa Phân tán Redlock & Idempotency Key)")]
        end

        %% 4. PERSISTENCE
        subgraph Layer_Storage["4. Khối Lưu trữ Dữ liệu Phân lập (Data Storage Tier)"]
            direction LR
            DB_EE[("Earning DB\n(PostgreSQL: Sổ cái Bất biến & Snapshot)")]
            DB_TS[("Tiering DB\n(PostgreSQL: Sổ cái QP & Hạng thẻ)")]
            DB_RED[("Redemption DB\n(PostgreSQL: Danh mục Quà & Đơn hàng)")]
            DB_PM[("Program DB\n(PostgreSQL: Cấu hình & WORM Audit)")]
            DB_DW[("Analytics DW\n(ClickHouse / Star Schema Fact & Dim)")]
        end
    end

    %% Kết nối Gateway xuống Services
    INGRESS -->|Định tuyến API| SVC_EE
    INGRESS -->|Định tuyến API| SVC_TS
    INGRESS -->|Định tuyến API| SVC_RE
    INGRESS -->|Định tuyến API| SVC_PM
    INGRESS -->|Định tuyến API| SVC_AR

    %% Kết nối Services với Middleware
    SVC_EE <-->|Publish / Consume| KAFKA
    SVC_TS <-->|Publish / Consume| KAFKA
    SVC_RE <-->|Publish / Consume| KAFKA
    SVC_PM <-->|Publish CDC| KAFKA
    KAFKA -->|Consume CDC| SVC_AR

    SVC_EE <-->|Idempotency Lock| REDIS
    SVC_RE <-->|FIFO Debit Lock| REDIS

    %% Kết nối Services với CSDL riêng (Database-per-Service)
    SVC_EE --- DB_EE
    SVC_TS --- DB_TS
    SVC_RE --- DB_RED
    SVC_PM --- DB_PM
    SVC_AR --- DB_DW

    %% Đồng bộ CDC từ OLTP sang OLAP
    DB_EE -.->|CDC Stream| KAFKA
    DB_TS -.->|CDC Stream| KAFKA
    DB_RED -.->|CDC Stream| KAFKA
    DB_PM -.->|CDC Stream| KAFKA

    style Layer_Edge fill:#e8f4f8,stroke:#007acc,stroke-width:2px
    style Layer_Compute fill:#e8f8f5,stroke:#2ecc71,stroke-width:2px
    style Layer_Middleware fill:#fef9e7,stroke:#f1c40f,stroke-width:2px
    style Layer_Storage fill:#fdedec,stroke:#e74c3c,stroke-width:2px
```

---

## 6. Implementation & Migration Layer (Lộ trình Triển khai Capstone)

### 6.1. Phân kỳ Phát triển theo 4 Plateaus & 6 Work Packages
- **Plateau 0 (Khởi tạo)**: Thiết lập đặc tả yêu cầu, kiến trúc phân tầng ArchiMate, OpenAPI 3.0 Specs, và mô hình dữ liệu.
- **Plateau 1 (MVP Tích điểm & Hạng thẻ)**: Xây dựng Earning Engine với Sổ cái bất biến, cơ chế Idempotency qua Redis, Tiering System với QP Ledger và thăng hạng tự động.
- **Plateau 2 (Đổi quà & Tích hợp Đối tác)**: Phát triển Redemption Engine với thuật toán FIFO, Partner OAuth Gateway với Rate Limiter, và cơ chế Reversal hoàn điểm.
- **Plateau 3 (Hệ thống Hoàn chỉnh & Sẵn sàng Vận hành)**: Triển khai Debezium CDC + Star Schema Data Warehouse, cổng Dual-Control duyệt lệnh $> 500$ điểm, và vượt qua Quality Gates (Kiểm thử tải 500 TPS trên JMeter, 0 lỗi trùng lặp).

### 6.2. Sơ đồ ArchiMate Implementation Layer

```mermaid
flowchart LR
    subgraph ImplementationLayer["Implementation & Migration Layer (Lộ trình Thực thi Capstone Loyalty)"]
        
        subgraph Plateaus["Architecture Plateaus (Các Mốc Chuyển giao Kiến trúc)"]
            PL_0["Plateau 0: Thiết kế Cơ sở & Đặc tả Yêu cầu\n(Kiến trúc, CSDL & OpenAPI 3.0 Specs)"]
            PL_1["Plateau 1: MVP Tích điểm & Hạng thẻ Cốt lõi\n(Sổ cái Bất biến, Khóa Idempotency & Nâng hạng)"]
            PL_2["Plateau 2: Nền tảng Đổi quà & Liên minh Đối tác\n(Đổi quà FIFO, Partner OAuth 2.0 & Reversal)"]
            PL_3["Plateau 3: Hệ thống Doanh nghiệp Hoàn chỉnh\n(Kho Dữ liệu Analytics, Dual-Control & Production Gate)"]
        end

        subgraph WorkPackages["Work Packages (Các Gói Công việc Thực thi)"]
            WP_1["WP 1: Thiết lập Hạ tầng & Hàng đợi Sự kiện\n(Cấu hình Kafka, PostgreSQL Schemas, Spring Boot Skeletons)"]
            WP_2["WP 2: Sổ cái Tích điểm Bất biến & Khóa Idempotency\n(Xử lý GD quyết toán, Tính điểm, Redis Lock, Hạn dùng)"]
            WP_3["WP 3: Hệ thống Xét hạng Động & Quản lý Ân hạn 30 ngày\n(Tích lũy QP, Thăng hạng Tức thì, Job Batch cuối năm)"]
            WP_4["WP 4: Đổi quà FIFO & Cổng Tích hợp Đối tác\n(Trừ điểm FIFO, OAuth 2.0 Gateway, Saga Hoàn điểm)"]
            WP_5["WP 5: Kho Dữ liệu Star Schema & Đường ống CDC\n(Tiêu thụ CDC Stream, Xuất 7 Báo cáo Tài chính Chuẩn)"]
            WP_6["WP 6: Kiểm soát Kép & Kiểm thử Tải Quality Gates\n(Ngưỡng duyệt 500 điểm, Kiểm thử 500 TPS JMeter, SonarQube)"]
        end

        subgraph Deliverables["Deliverables (Sản phẩm Đồ án Bàn giao)"]
            DEL_DOCS["Deliverable: Bộ Tài liệu Kiến trúc Hệ thống & OpenAPI 3.0 Specs"]
            DEL_CONTAINERS["Deliverable: 5 Bộ Docker Images Microservices & Cấu hình K8s"]
            DEL_DBS["Deliverable: 4 CSDL Microservices + Star Schema Data Warehouse"]
            DEL_PORTALS["Deliverable: Ứng dụng Mobile Hội viên + 3 Web Portals Quản trị"]
            DEL_AUDIT["Deliverable: Báo cáo Kiểm thử Quality Gates (≥ 500 TPS, 0 Lỗi Trùng)"]
        end
    end

    PL_0 --> WP_1
    WP_1 --> PL_1
    WP_2 --> PL_1
    WP_3 --> PL_1

    PL_1 --> WP_4
    WP_4 --> PL_2

    PL_2 --> WP_5
    PL_2 --> WP_6
    WP_5 --> PL_3
    WP_6 --> PL_3

    PL_3 -.-> DEL_DOCS
    PL_3 -.-> DEL_CONTAINERS
    PL_3 -.-> DEL_DBS
    PL_3 -.-> DEL_PORTALS
    PL_3 -.-> DEL_AUDIT
```

---

## 7. Cross-Layer Traceability View (Sơ đồ Tích hợp Đa tầng Loyalty)

Sơ đồ thể hiện chuỗi liên kết xuyên suốt (**End-to-End Alignment**) từ Động lực Chiến lược $\rightarrow$ Quy trình Nghiệp vụ $\rightarrow$ Dịch vụ Phần mềm $\rightarrow$ Bảng CSDL và Hạ tầng Công nghệ:

```mermaid
flowchart TB
    %% 1. MOTIVATION & STRATEGY
    subgraph Layer_Motivation["1. Motivation & Strategy Layer"]
        G_AUDIT["Goal: 100% Sổ cái Bất biến & Không Trùng lặp Giao dịch"]
        G_SLA["Goal: Thông lượng ≥ 500 sự kiện/giây (SLA ≤ 60s, p95 < 2s)"]
        CAP_EARNING["Capability: Năng lực Tích điểm Tốc độ cao & Quản lý Hạng thẻ"]
        
        G_AUDIT -.-> CAP_EARNING
        G_SLA -.-> CAP_EARNING
    end

    %% 2. BUSINESS LAYER
    subgraph Layer_Business["2. Business Layer"]
        BS_EARN_SERVICE["Business Service: Dịch vụ Tích điểm Giao dịch & Xếp hạng"]
        BP_EARN_PROCESS["Business Process: Quy trình Tiếp nhận GD & Ghi Sổ cái"]
        BO_TXN_LEDGER["Business Object: Bút toán Sổ cái Bất biến (Point Transaction)"]

        CAP_EARNING ==> BS_EARN_SERVICE
        BS_EARN_SERVICE --> BP_EARN_PROCESS
        BP_EARN_PROCESS --> BO_TXN_LEDGER
    end

    %% 3. APPLICATION LAYER
    subgraph Layer_Application["3. Application Layer"]
        APP_SVC_EARN["Application Service: API Tiếp nhận & Đánh giá Quy tắc Điểm"]
        COMP_EE_SRV["Application Component: Earning Engine Service (Microservice)"]
        DO_POINT_TXN["Data Object: point_transaction (Ledger) & point_balance (Snapshot)"]

        BP_EARN_PROCESS -.->|Hiện thực hóa bởi| APP_SVC_EARN
        APP_SVC_EARN --> COMP_EE_SRV
        COMP_EE_SRV --> DO_POINT_TXN
    end

    %% 4. TECHNOLOGY LAYER
    subgraph Layer_Technology["4. Technology & Infrastructure Layer"]
        TECH_SVC_KAFKA["Technology Service: Hàng đợi Sự kiện Kafka (settled_transactions)"]
        TECH_SVC_REDIS["Technology Service: Bộ nhớ đệm & Khóa Idempotency Redis"]
        TECH_SVC_PG["Technology Service: CSDL PostgreSQL (Phân vùng Bảng Sổ cái)"]
        TECH_NODE_K8S["Technology Node: Kubernetes Node (Tự động Co giãn Pods HPA)"]

        COMP_EE_SRV -.->|Vận hành trên| TECH_NODE_K8S
        COMP_EE_SRV -.->|Tiêu thụ từ| TECH_SVC_KAFKA
        COMP_EE_SRV -.->|Khóa Idempotency với| TECH_SVC_REDIS
        DO_POINT_TXN -.->|Lưu trữ vật lý tại| TECH_SVC_PG
    end

    %% Cross-layer links
    Layer_Motivation ==> Layer_Business
    Layer_Business ==> Layer_Application
    Layer_Application ==> Layer_Technology
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
