# Loyalty Banking Platform — Model-Driven Design Course & Reference

Welcome to the **Loyalty Banking Platform** repository. This project is a comprehensive case study and executable reference architecture demonstrating **Model-Driven Design (MDD)** for an enterprise banking loyalty system.

---

## 🎯 Quick Navigation & Learning Path

Follow the structured learning path through the 10 sequential lab exercises, or explore the architectural blueprints and code directly:

```
loyalty_system_docs/
├── labs/              # 📚 10 Course Lab Deliverables (Step-by-Step Learning)
├── docs/              # 🏛️ Formal Architecture, Detailed Designs, and Governance
├── capstone/          # 💻 Runnable Java Reference Implementation & Tests
└── mdd-index.md       # 📑 Complete Master Artifact Index & Quality Gates
```

---

## 📚 Course Lab Syllabus

| Lab | Name & Topic | Key Focus & Models | Documentation Link |
|:---:|---|---|:---:|
| **01** | **Scope & Business Invariants** | System boundaries, core entities, and non-negotiable invariants (CON.1–CON.4). | [Lab 01 Overview](labs/lab-01-scope/README.md) |
| **02** | **Requirements & Traceability** | 63 functional & non-functional requirements (REQ-LB-01..63) and trace matrix. | [Lab 02 Overview](labs/lab-02-requirements/README.md) |
| **03** | **Specifications & Contracts** | 13 Containers, 5 Canonical modules, and 27 inter-service contracts (CT-01..27). | [Lab 03 Overview](labs/lab-03-specifications/README.md) |
| **04** | **MDD Standardization** | Gap analysis, eliminating anti-patterns, defect catalog, before/after comparison. | [Lab 04 Overview](labs/lab-04-standardization/README.md) |
| **05** | **Legacy UML (Before Pack)** | Initial UML diagrams prior to formal MDD standardization. | [Lab 05 Overview](labs/lab-05-uml-legacy/README.md) |
| **06** | **Legacy Ecosystem (Before Pack)** | Initial integration ecosystem before formal boundary design. | [Lab 06 Overview](labs/lab-06-ecosystem-legacy/README.md) |
| **07** | **Governance & Quality Gates** | RACI matrix and G1–G6 Quality Gate acceptance criteria. | [Lab 07 Overview](labs/lab-07-governance/README.md) |
| **08** | **ArchiMate Architecture Views** | Motivation, Business Process, Application Cooperation, and Technology views. | [Lab 08 Overview](labs/lab-08-archimate/README.md) |
| **09** | **C4 Architecture Models** | C4 Context (L1), Container (L2), and Component (L3 for Redemption Engine). | [Lab 09 Overview](labs/lab-09-c4-models/README.md) |
| **10** | **Behavioral Models & UML** | Sequence diagrams (UC-LB-01..04), Activity diagram, and Order State Machine. | [Lab 10 Overview](labs/lab-10-behavioral-models/README.md) |

---

## 🏛️ System Architecture & Design

* **Architecture Overview**: [`docs/architecture/Architecture-Overview.md`](docs/architecture/Architecture-Overview.md)
* **C4 Diagrams & Visuals**: [`docs/architecture/C4/`](docs/architecture/C4/)
* **Data Architecture & Schemas**: [`docs/architecture/Data-Architecture-and-Schema.md`](docs/architecture/Data-Architecture-and-Schema.md)
* **Domain Event Catalog**: [`docs/architecture/domain-event-catalog.md`](docs/architecture/domain-event-catalog.md)
* **Architecture Decision Records**:
  * [`ADR-001: Module Boundaries & Database Isolation`](docs/architecture/adrs/ADR-001-module-boundaries-and-data-isolation.md)
  * [`ADR-002: Event-Driven Earn Ingestion & Idempotency`](docs/architecture/adrs/ADR-002-event-driven-earn-ingestion-and-idempotency.md)
  * [`ADR-003: Data Warehouse & Analytics Isolation`](docs/architecture/adrs/ADR-003-data-warehouse-and-analytics-isolation.md)
* **Detailed Engine Designs**:
  * [`DD-01: Earning Engine Service`](docs/detailed-design/DD-01-earning-engine.md)
  * [`DD-02: Tiering System Service`](docs/detailed-design/DD-02-tiering-system.md)
  * [`DD-03: Redemption Engine Service`](docs/detailed-design/DD-03-redemption-engine.md)
  * [`DD-04: Program Management Service`](docs/detailed-design/DD-04-program-management.md)
  * [`DD-05: Analytics & Reporting Service`](docs/detailed-design/DD-05-analytics-reporting.md)
  * [`Entity Lifecycle Models`](docs/detailed-design/entity-lifecycle-models.md)

---

## 💻 Runnable Java Capstone (Quick Start)

The reference implementation is self-contained in Java (JDK 11+) with no external build tools or docker required.

### 1. Run Automated Quality Gate & Contract Tests (27/27 Passing)

**PowerShell (Windows):**
```powershell
cd capstone
if (!(Test-Path out)) { New-Item -ItemType Directory -Path out }
javac -d out (Get-ChildItem -Recurse src -Filter *.java | % FullName)
javac -d out -cp out (Get-ChildItem -Recurse test -Filter *.java | % FullName)
java -cp out com.loyalty.capstone.CapstoneTests
```

**Bash / Linux / macOS:**
```bash
cd capstone
mkdir -p out
javac -d out $(find src -name "*.java")
javac -d out -cp out $(find test -name "*.java")
java -cp out com.loyalty.capstone.CapstoneTests
```

### 2. Start the Live HTTP Gateway & Demo

```bash
java -cp out com.loyalty.capstone.Main 8080
```

* **Earn points (UC-LB-01)**:
  ```bash
  curl -X POST http://localhost:8080/partner-earn \
    -H "Content-Type: application/json" \
    -d '{"sourceTransactionId":"TXN-1001","memberId":"M-1001","amount":200}'
  ```

* **Redeem reward (UC-LB-02)**:
  ```bash
  curl -X POST http://localhost:8080/redemptions \
    -H "Content-Type: application/json" \
    -d '{"memberId":"M-1001","rewardItemId":"RW-COFFEE-01"}'
  ```

* **Get Liability Report (UC-LB-04)**:
  ```bash
  curl -X GET http://localhost:8080/reports/point-liability
  ```

---

## 🔒 Architectural Invariants (CON.1–CON.4)

1. **CON.1 (Zero Point Inflation)**: Strict duplicate detection on source transactions using SHA-256 idempotency cache.
2. **CON.2 (FIFO Expiry-First Consumption)**: Oldest unexpired points are debited first.
3. **CON.3 (Redemption Reversal Guarantee)**: If downstream partner delivery fails, reserved points are auto-refunded.
4. **CON.4 (Liability Staleness Guard)**: Financial analytics requests report replication lag to ensure accuracy.
