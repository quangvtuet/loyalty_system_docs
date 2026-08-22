# Loyalty Platform — Legacy Implementation Spike (Archived)

> [!WARNING]
> **SUPERSEDED & QUARANTINED — NOT A LAB DELIVERABLE AND NOT THE CAPSTONE.**
> This directory contains an early exploratory microservice prototype created prior to Model-Driven Design (MDD) standardization.
> - **Official Runnable Capstone**: The sole authoritative, tested, and SA-accepted runtime for the I-11 slice is in [`../capstone/`](../capstone/).
> - **Official Modeling Pack**: Labs 1 through 10 reside at the repository root and in `before-pack/`.
> - This codebase is archived for historical reference only. It is **not** submitted for evaluation and does not need to be built, run, or deployed.

---
- [Docker](https://docs.docker.com/get-docker/) và Docker Compose (để chạy Kafka, Redis, PostgreSQL).
- [Java 17](https://adoptium.net/temurin/releases/) (nếu chạy ứng dụng trực tiếp bằng Maven, hoặc không cần nếu dùng container hóa toàn bộ - hiện tại đang chạy native Java + Maven Wrapper).
- Môi trường: Unix/Linux, macOS hoặc Windows (WSL2).

## Cấu trúc thư mục

```
loyalty-platform-impl/
├── docker-compose.yml       # Cấu hình hạ tầng (PostgreSQL 16, Redis 7, Kafka 7.4)
├── earning-engine/          # DD-01: Earning Engine (Port 8081)
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── src/main/java/com/loyalty/earning_engine/
│       ├── api/          # REST Controller (PartnerEarnController)
│       ├── domain/       # JPA Entities (PointTransaction, PointBalance)
│       ├── kafka/        # Kafka Consumer (TransactionSettledConsumer)
│       ├── repository/   # PointTransactionRepo, PointBalanceRepo
│       └── service/      # EarnCalculator, EarningLedgerService
├── tiering-system/          # DD-02: Tiering System (Port 8082)
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── src/main/java/com/loyalty/tiering_system/
│       ├── domain/       # Entities (QpLedger, MemberTier) & Enums (TierName, TierStatus)
│       ├── event/        # DTOs (QpAccruedEvent, TierChangedEvent)
│       ├── kafka/        # Kafka Consumer (QpAccruedConsumer)
│       ├── repository/   # QpLedgerRepository, MemberTierRepository
│       └── service/      # QpLedgerService, TierUpgradeService, GracePeriodService
└── redemption-engine/       # DD-03: Redemption Engine (Port 8083)
    ├── pom.xml
    ├── mvnw / mvnw.cmd
    └── src/main/java/com/loyalty/redemption_engine/
        ├── domain/       # Entities (RewardItem, RedemptionOrder) & Enums (OrderStatus, FulfillmentType)
        ├── repository/   # RewardItemRepository, RedemptionOrderRepository
        └── service/      # CatalogService, BalanceLockService, FifoDebitService, RedemptionService
├── program-management/      # DD-04: Program Management (Port 8084)
    ├── pom.xml
    ├── mvnw / mvnw.cmd
    └── src/main/java/com/loyalty/program_management/
        ├── api/          # REST Controller (ProgramController)
        ├── domain/       # Entities (LoyaltyProgram, Campaign, ConfigVersionLog) & Enums
        ├── repository/   # Repositories for configuration entities
        └── service/      # ProgramService, CampaignService, AuditLogService
├── analytics-reporting/     # DD-05: Analytics & Reporting (Port 8085)
    ├── pom.xml
    ├── mvnw / mvnw.cmd
    └── src/main/java/com/loyalty/analytics_reporting/
        ├── api/          # REST Controller (ReportingController)
        ├── domain/       # Fact & Dimension Tables (Star Schema)
        ├── repository/   # FactPointTransactionRepository for computing Liability
        └── service/      # ReportingService
```

## Hướng dẫn cài đặt và chạy (Step-by-Step)

### Bước 1: Khởi động cơ sở hạ tầng (Infrastructure)

Mở terminal tại thư mục gốc `loyalty-platform-impl/` và chạy:

```bash
docker-compose up -d
```
Lệnh này sẽ tải và khởi động các container:
- `loyalty-postgres` (Port 5432)
- `loyalty-redis` (Port 6379)
- `loyalty-kafka` (Port 9092)
- `loyalty-zookeeper` (Port 2181)

*Kiểm tra trạng thái:* `docker-compose ps` để đảm bảo tất cả đều `Up`.

### Bước 2: Chạy từng service

**Earning Engine** (Port 8081):
```bash
cd earning-engine/
./mvnw spring-boot:run
```

**Tiering System** (Port 8082) — mở terminal mới:
```bash
cd tiering-system/
./mvnw spring-boot:run
```

**Redemption Engine** (Port 8083) — mở terminal mới:
```bash
cd redemption-engine/
./mvnw spring-boot:run
```

**Program Management** (Port 8084) — mở terminal mới:
```bash
cd program-management/
./mvnw spring-boot:run
```

**Analytics & Reporting** (Port 8085) — mở terminal mới:
```bash
cd analytics-reporting/
./mvnw spring-boot:run
```
*(Trên Windows dùng: `mvnw.cmd spring-boot:run`)*

### Bước 3: Chạy Unit Tests (từng service)

```bash
cd earning-engine/  && ./mvnw test
cd tiering-system/  && ./mvnw test
cd redemption-engine/ && ./mvnw test
cd program-management/ && ./mvnw test
cd analytics-reporting/ && ./mvnw test
```

## Kiểm thử chức năng (Manual Verification)

Bạn có thể dùng `curl` hoặc Postman để kiểm tra API.

**Gửi yêu cầu tích điểm từ Partner:**
```bash
curl -X POST http://localhost:8081/api/v1/partners/earn \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "txn-abc-123",
    "memberId": "member-001",
    "spendAmount": 1000,
    "campaignId": "DOUBLE_POINTS_AUG"
  }'
```

*Kết quả mong đợi:* Trả về `202 Accepted` và sinh ra log báo tính điểm thành công.
*Kiểm tra Idempotency:* Nếu bạn chạy lại nguyên lệnh curl trên, hệ thống sẽ báo `409 Conflict - Duplicate transaction detected` nhờ vào Redis Idempotency.

---

**Khởi tạo dữ liệu mẫu catalog cho Redemption Engine:**
```bash
# Thêm phần thưởng Silver (300 điểm = $3)
curl -X POST http://localhost:8083/api/v1/catalog/items \
  -H "Content-Type: application/json" \
  -d '{
    "programId": "DEFAULT_PROG",
    "name": "Coffee Voucher",
    "category": "VOUCHER",
    "pointsCost": 300,
    "currencyValue": 3.00,
    "fulfillmentType": "DIGITAL",
    "minTierRequired": "SILVER"
  }'
```

**Đổi điểm (Redemption Order):**
```bash
curl -X POST http://localhost:8083/api/v1/redemptions/orders \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": "member-001",
    "programId": "DEFAULT_PROG",
    "rewardItemId": "<item-id-from-catalog>",
    "quantity": 1,
    "memberTier": "SILVER",
    "availableBalance": 500
  }'
```

*Kết quả mong đợi:* Trả về `201 Created` kèm `orderId` và `status: PENDING`.

## Kiểm tra dữ liệu trong Database

Tất cả các service đều dùng chung PostgreSQL `loyalty_db`. Dưới đây là các lệnh kiểm tra trực tiếp qua Docker:

**1. Kiểm tra lịch sử giao dịch (Sổ cái Earning Engine):**
```bash
docker exec -it loyalty-postgres psql -U loyalty_user -d loyalty_db -c "SELECT * FROM point_transaction;"
```

**2. Kiểm tra tổng số dư hiện tại (Snapshot):**
```bash
docker exec -it loyalty-postgres psql -U loyalty_user -d loyalty_db -c "SELECT * FROM point_balance;"
```

**3. Kiểm tra lịch sử tích lũy QP (Tiering System):**
```bash
docker exec -it loyalty-postgres psql -U loyalty_user -d loyalty_db -c "SELECT * FROM qp_ledger ORDER BY accrual_date DESC;"
```

**4. Kiểm tra trạng thái tier hiện tại của member:**
```bash
docker exec -it loyalty-postgres psql -U loyalty_user -d loyalty_db -c "SELECT member_id, current_tier, previous_tier, cumulative_qp, status, grace_period_end FROM member_tier;"
```

**5. Kiểm tra catalog phần thưởng (Redemption Engine):**
```bash
docker exec -it loyalty-postgres psql -U loyalty_user -d loyalty_db -c "SELECT item_id, name, points_cost, min_tier_required, status FROM reward_item;"
```

**6. Kiểm tra đơn đổi điểm:**
```bash
docker exec -it loyalty-postgres psql -U loyalty_user -d loyalty_db -c "SELECT order_id, member_id, total_points_debited, member_tier_at_order, status, failure_reason FROM redemption_order ORDER BY created_at DESC;"
```

Hoặc bạn có thể dùng một công cụ quản lý CSDL (như DBeaver, DataGrip, pgAdmin) để kết nối vào Database với thông số:
- **Host**: `localhost`
- **Port**: `5432`
- **Database**: `loyalty_db`
- **User**: `loyalty_user`
- **Password**: `loyalty_password`


## Thiết kế nổi bật
- **Idempotency**: Earning Engine dùng Redis NX + SHA-256 ngăn truy vấn trùng lặp.
- **Precision**: Dùng `Math.floor()` đảm bảo làm tròn xuống theo yêu cầu DD-01.
- **Event-Driven**: Earning → Tiering qua Kafka topic `loyalty.earning.qp_accrued`.
- **FIFO Debit**: Redemption Engine tiêu điểm theo thứ tự `earn_date ASC` để giữ nguyên lịch sử expiry.
- **Distributed Lock**: Redemption dùng Redis `SET NX EX 10` để chống concurrent redemption cùng member.
- **Dual Table**: `point_transaction` (append-only ledger) + `point_balance` (snapshot số dư) tách biệt hiệu năng đọc/ghi.

## Ports Summary

| Service | Port | Design Doc |
|---|---|---|
| Earning Engine | 8081 | DD-01 |
| Tiering System | 8082 | DD-02 |
| Redemption Engine | 8083 | DD-03 |
| Program Management| 8084 | DD-04 |
| Analytics & Reporting| 8085 | DD-05 |
