# Loyalty Platform - Earning Engine

Dự án này là module **Earning Engine (DD-01)** của hệ thống Loyalty Banking Platform, được phát triển bằng **Java Spring Boot 3**.

## Yêu cầu hệ thống (Prerequisites)
- [Docker](https://docs.docker.com/get-docker/) và Docker Compose (để chạy Kafka, Redis, PostgreSQL).
- [Java 17](https://adoptium.net/temurin/releases/) (nếu chạy ứng dụng trực tiếp bằng Maven, hoặc không cần nếu dùng container hóa toàn bộ - hiện tại đang chạy native Java + Maven Wrapper).
- Môi trường: Unix/Linux, macOS hoặc Windows (WSL2).

## Cấu trúc thư mục

```
loyalty-platform-impl/
├── docker-compose.yml       # Cấu hình hạ tầng (PostgreSQL 16, Redis 7, Kafka 7.4)
├── earning-engine/          # Mã nguồn Spring Boot cho Earning Engine (Port 8081)
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── src/
│       ├── main/java/com/loyalty/earning_engine/
│       │   ├── api/          # REST Controller (PartnerEarnController)
│       │   ├── domain/       # JPA Entities (PointTransaction, PointBalance)
│       │   ├── kafka/        # Kafka Consumer (TransactionSettledConsumer)
│       │   ├── repository/   # Repositories (PointTransactionRepo, PointBalanceRepo)
│       │   └── service/      # EarnCalculator, EarningLedgerService
│       └── resources/application.yml
└── tiering-system/          # Mã nguồn Spring Boot cho Tiering System (Port 8082)
    ├── pom.xml
    ├── mvnw / mvnw.cmd
    └── src/
        ├── main/java/com/loyalty/tiering_system/
        │   ├── domain/       # Entities (QpLedger, MemberTier) & Enums (TierName, TierStatus)
        │   ├── event/        # DTOs (QpAccruedEvent, TierChangedEvent)
        │   ├── kafka/        # Kafka Consumer (QpAccruedConsumer)
        │   ├── repository/   # QpLedgerRepository, MemberTierRepository
        │   └── service/      # QpLedgerService, TierUpgradeService, GracePeriodService
        └── resources/application.yml
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

### Bước 2: Chạy ứng dụng Spring Boot

Mở một terminal khác, chuyển vào thư mục `earning-engine`:

```bash
cd earning-engine/
./mvnw spring-boot:run
```
*(Trên Windows dùng lệnh: `mvnw.cmd spring-boot:run`)*

Ứng dụng sẽ tự động khởi tạo kết nối Database (Hibernate auto update schema), Kafka và Redis.
Server sẽ chạy trên port `8081`.

### Bước 3: Chạy Unit Tests

Để đảm bảo logic tính toán điểm chính xác:

```bash
cd earning-engine/
./mvnw test
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

## Cách kiểm tra dữ liệu Sổ cái (Earning Ledger) trong Database

Dữ liệu lịch sử cộng điểm được ghi nhận không thể sửa xóa (append-only) vào **Sổ cái (Earning Ledger)** thông qua bảng `point_transaction`. Đồng thời, tổng số dư hiện tại của khách hàng được cập nhật đồng bộ vào bảng **Snapshot Số dư (`point_balance`)**. Bạn có thể kiểm tra trực tiếp qua Docker container bằng lệnh sau:

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

Hoặc bạn có thể dùng một công cụ quản lý CSDL (như DBeaver, DataGrip, pgAdmin) để kết nối vào Database với thông số:
- **Host**: `localhost`
- **Port**: `5432`
- **Database**: `loyalty_db`
- **User**: `loyalty_user`
- **Password**: `loyalty_password`

## Hướng dẫn chạy Tiering System

### Bước 1: Đảm bảo Infrastructure đang chạy
```bash
docker-compose up -d
```

### Bước 2: Chạy Tiering System (Port 8082)
```bash
cd tiering-system/
./mvnw spring-boot:run
```
*(Trên Windows: `mvnw.cmd spring-boot:run`)*

### Bước 3: Chạy Unit Tests
```bash
cd tiering-system/
./mvnw test
```

> **Lưu ý**: Để end-to-end flow hoạt động đầy đủ (Earning → Tiering), cần chạy đồng thời cả `earning-engine` (port 8081) và `tiering-system` (port 8082). Khi Earning Engine phát event `loyalty.earning.qp_accrued`, Tiering System sẽ tự động nhận và đánh giá tier.

## Thiết kế nổi bật
- **Idempotency**: Dùng Redis setIfAbsent (NX) với thời gian tồn tại 24h kết hợp hàm hash SHA-256 của chuỗi giao dịch.
- **Precision**: Dùng `Math.floor()` đảm bảo việc làm tròn xuống theo yêu cầu kiến trúc (DD-01).
- **Event-Driven**: Đã thiết lập sẵn Kafka Consumer lắng nghe event `corebanking.transactions.settled`.

