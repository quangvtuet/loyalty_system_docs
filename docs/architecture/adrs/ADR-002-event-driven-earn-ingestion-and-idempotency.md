# ADR-002: Event-Driven Earn Ingestion & Distributed Idempotency

**Status**: Accepted  
**Date**: 2026-08-14  
**Context**: FR-01-001, FR-01-040..042, NFR-01-001, NFR-01-002, NFR-01-004  
**Deciders**: Solution Architect, Tech Lead, Performance Engineer

---

## 1. Context & Problem Statement

The Earning Engine must consume settled financial transaction events from the Core Banking System and third-party partners. It must handle peak ingestion volumes of **≥ 500 events per second**, process events end-to-end within **≤ 2,000 ms (p95)**, and guarantee **exactly-once point crediting** (zero duplicate credits even under network retries or process crashes).

---

## 2. Decision Drivers

- **High Ingestion Throughput**: Must absorb burst traffic without blocking core banking settlement feeds.
- **Strict Idempotency**: Core banking systems frequently re-transmit messages during connection resets.
- **Durability & Ordering**: Events for the same member must be processed in causal order.
- **Latency SLA**: Real-time event consumption within ≤ 60 seconds of transaction settlement.

---

## 3. Considered Options

1. **Synchronous HTTP Ingestion Endpoint**: Core banking invokes a synchronous REST endpoint per settled transaction.
2. **Batch File Upload (SFTP / S3 Polling)**: Scheduled batch ingestion of settlement CSV files.
3. **Partitioned Event Streaming with Redis Idempotency Cache & Database Unique Constraints**: Core banking publishes to Apache Kafka partitioned by `member_id`; consumer uses a two-tier idempotency check (Redis fast filter + PostgreSQL unique constraint).

---

## 4. Decision

We choose **Option 3: Partitioned Event Streaming with Two-Tier Idempotency**.

### Technical Architecture
1. **Message Broker**: Apache Kafka topic `corebanking.transactions.settled` partitioned by `member_id` (guaranteeing in-order processing per member).
2. **Idempotency Key Formulation**:
   $$\text{idempotency\_key} = \text{SHA-256}(\text{source\_transaction\_id} \mathbin{\Vert} \text{program\_id})$$
3. **Two-Tier Deduplication Strategy**:
   - **Tier 1 (In-Memory Fast Filter)**: Redis `SET idempotency_key "PROCESSING" NX EX 86400`. If key exists, return original cached response immediately.
   - **Tier 2 (Storage Hard Constraint)**: `point_transaction.idempotency_key` with a `UNIQUE` index. On DB conflict, roll back and return the existing ledger entry.
4. **Settlement Filter**: Non-settled (authorization-only) events are routed to a pending queue; only confirmed settlement events commit points to the available balance.

---

## 5. Consequences

### Positive
- Elastic scalability up to thousands of events/sec by adding Kafka partition consumer pods.
- Sub-millisecond duplicate rejection at Tier 1 without touching the database.
- Guarantees exactly-once crediting semantics under all failure modes.

### Negative / Mitigations
- Requires running and maintaining a Kafka cluster and Redis cache layer. Mitigated by managed cloud services (AWS MSK / Amazon ElastiCache) with multi-AZ replication.
