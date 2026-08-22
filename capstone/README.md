# Loyalty Platform Implementation Runtime (Capstone)

This directory is the Team 2 capstone runtime implemented using **Spring Boot**, **Kafka**, **Redis**, and **JPA** for the I-11 slice:

- `UC-LB-01` Process settled earn event (`earning-engine`)
- `UC-LB-02` Redeem reward with FIFO (`redemption-engine`)
- `UC-LB-03` Apply tier upgrade (`tiering-system`)
- `UC-LB-04` Generate point liability report (`analytics-reporting`)

The source of truth remains the Lab 1, Lab 3, Lab 7, Lab 8, Lab 9, and Lab 10 artifacts. This runtime is outside the modeling packs.

---

## Runtime Architecture & Microservices

The capstone path uses these named services, technologies, and contracts:

| Service | Port | Technology Stack | In-scope Responsibility |
|---|---|---|---|
| **Earning Engine Service** | 8081 | Spring Boot, Spring Kafka, Spring Data Redis, Spring Data JPA | UC-LB-01, CT-04 partner earn, CT-01/02 settled consumer, CT-05 QP producer, CT-13 point ownership/FIFO debit/restore |
| **Tiering System Service** | 8082 | Spring Boot, Spring Kafka, Spring Data JPA | UC-LB-03, CT-06 QP accrual consumer, CT-07 tier changed producer, CT-12 authoritative member tier read |
| **Redemption Engine Service** | 8083 | Spring Boot, Spring Data Redis (BalanceLock), Spring Data JPA | UC-LB-02, CT-10/11 order state machine (6 states), CT-12/13/14 orchestration |
| **Analytics & Reporting Service** | 8085 | Spring Boot, Spring Data JPA | UC-LB-04, CT-26 liability report, CON.4 data freshness check (10-minute staleness flag) |

---

## Build and Test

### Prerequisites
- JDK 17 or JDK 21
- Apache Maven 3.9+ (or use `./mvnw`)

### Running All Test Suites
Run the entire platform test suite from the `capstone` root:

```bash
cd capstone
mvn clean test
```

Or run each in-scope module independently:

```bash
cd earning-engine && mvn test
cd tiering-system && mvn test
cd redemption-engine && mvn test
cd analytics-reporting && mvn test
```

---

## Verification & Artifacts

- [openapi.yaml](openapi.yaml) is the G4 contract.
- [spec-trace.md](spec-trace.md) maps in-scope paths to operations and executable tests.
- [name-identity-map.md](name-identity-map.md) maps I-4 names, I-7 owners, I-9 zones, and infrastructure collapses.
- [SIGN-OFF.md](SIGN-OFF.md) is the SA architectural sign-off.
