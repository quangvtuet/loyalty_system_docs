# Motivation Layer - Banking Loyalty Platform

> **Not a Lab 8 view — outside the modeling pack.**
> The four Lab 8 ArchiMate views are in [`../../lab8-archimate-views.md`](../../lab8-archimate-views.md).
> This file was drawn before the Guide was adopted. It has no header and no RACI, it uses the old container names, and it is kept only as before-pack material. It is not submitted and not reviewed.


<!--
Title:      Motivation View — Loyalty Banking Platform
Viewpoint:  ArchiMate — Motivation Aspect
Layer(s):   Motivation / Strategy
As-Is | To-Be | Transition:  To-Be
Owner:      Role EA  Name Loyalty Banking Modeling Team
RACI:       R EA  A Owner  C SA BA/PO Sec  I DA Dev Test Ops
Version:    v1.0  Date 2026-08-21  Status Review
Legend:     Influence (––>), Realization (..>), Association (—)
RACI legend: R = draws · A = approves · C = consulted · I = informed
Scope:      in-scope: Stakeholders, Drivers, Goals, Principles (ADR-001–003 = CON.* equivalents); out-of-scope: protocol labels, pods, JDBC
-->

@startuml
!pragma layout smetana
!include <archimate/Archimate>

title Motivation Layer - Banking Loyalty Platform
left to right direction

rectangle "Motivation Aspect" as MotivationBoundary {
    Motivation_Stakeholder(ExecutiveBoard, "Executive Board\nBusiness sponsorship and risk appetite")
    Motivation_Stakeholder(MarketingTeam, "Marketing Team\nRetention and campaign growth")
    Motivation_Stakeholder(FinanceAccounting, "Finance & Accounting\nLiability and reconciliation")
    Motivation_Stakeholder(Cardholders, "Cardholders / Members\nEarn and redeem loyalty value")
    Motivation_Stakeholder(AlliancePartners, "Alliance Partners\nMerchant and fulfillment ecosystem")

    Motivation_Driver(CustomerRetention, "Customer Retention\nIncrease active card usage")
    Motivation_Driver(CoalitionEcosystem, "Coalition Ecosystem\nPartner earn/redeem network")
    Motivation_Driver(ZeroFinancialLeakage, "Zero Financial Leakage\nPrevent duplicate or invalid postings")

    Motivation_Goal(RealTimeIngestion, "Real-Time Ingestion Performance\n>=500 TPS, p95 <2s, SLA <=60s")
    Motivation_Goal(LedgerIntegrity, "Ledger Integrity\n100% immutable ledger, zero duplicate postings")
    Motivation_Goal(AnalyticsIsolation, "Reporting Isolation\nCDC to analytics with <10 min lag")
    Motivation_Goal(GovernedAdjustment, "Governed Manual Adjustments\nDual-control for high-value changes")

    Motivation_Principle(DatabasePerService, "ADR-001 Database-per-Service\nNo direct cross-module DB access")
    Motivation_Principle(EventDrivenIdempotency, "ADR-002 Event-Driven and Idempotency\nKafka ingestion, Redis dedupe locks")
    Motivation_Principle(OltpOlapSeparation, "ADR-003 OLTP / OLAP Separation\nCDC into dedicated analytics store")
    Motivation_Principle(ZeroTrustIntegration, "Zero Trust Integration\nOAuth 2.0, JWT, TLS/mTLS")
    Motivation_Principle(AuditNonRepudiation, "Audit and Non-Repudiation\nWORM audit logs and immutable ledger")
}

Rel_Influence(ExecutiveBoard, CustomerRetention, "sets priority")
Rel_Influence(MarketingTeam, CustomerRetention, "drives")
Rel_Influence(MarketingTeam, CoalitionEcosystem, "drives")
Rel_Influence(FinanceAccounting, ZeroFinancialLeakage, "requires")
Rel_Influence(Cardholders, CustomerRetention, "expects value")
Rel_Influence(AlliancePartners, CoalitionEcosystem, "enables")

Rel_Influence(CustomerRetention, RealTimeIngestion, "motivates")
Rel_Influence(CustomerRetention, GovernedAdjustment, "requires trust")
Rel_Influence(CoalitionEcosystem, RealTimeIngestion, "motivates")
Rel_Influence(ZeroFinancialLeakage, LedgerIntegrity, "motivates")
Rel_Influence(ZeroFinancialLeakage, GovernedAdjustment, "motivates")
Rel_Influence(ZeroFinancialLeakage, AnalyticsIsolation, "requires")

Rel_Realization(DatabasePerService, LedgerIntegrity, "supports")
Rel_Realization(EventDrivenIdempotency, RealTimeIngestion, "realizes")
Rel_Realization(EventDrivenIdempotency, LedgerIntegrity, "prevents duplicates")
Rel_Realization(OltpOlapSeparation, AnalyticsIsolation, "realizes")
Rel_Realization(ZeroTrustIntegration, GovernedAdjustment, "protects access")
Rel_Realization(AuditNonRepudiation, LedgerIntegrity, "proves history")
Rel_Realization(AuditNonRepudiation, GovernedAdjustment, "records approval")

@enduml
```
