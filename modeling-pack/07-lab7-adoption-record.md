# Lab 7 Adoption Record: Hierarchy, Focus Matrix, Quality Gates, RACI

**R**: EA  
**A**: Owner  
**Adoption statement**: This trainee modeling pack adopts the Guide in `template/list.md` as written. The authoritative modeling gates for this pack are `G1` to `G6` only. Existing extended engineering gates are retained as supplementary governance and do not replace `G1` to `G6`.

## Role Roster

| Guide role | Assigned name |
|---|---|
| Owner | Business Owner (simulated) |
| EA | Enterprise Architect (team role) |
| SA | Solution Architect (team role) |
| BA / PO | Business Analyst / Product Owner (team role) |
| DA | Domain / Data Architect (team role) |
| Sec | Security / Compliance / Risk (team role) |
| Dev | Software Engineer (team role) |
| Test | Quality Engineer (team role) |
| Ops | DevOps Engineer (team role) |

## Adopted Modeling Hierarchy

| Level | Language | Loyalty Banking evidence |
|---|---|---|
| Top - enterprise | ArchiMate | Motivation/Strategy, Business Process, Application Cooperation, Technology views under `architecture/archimate/` |
| Middle - solution | C4 plus ArchiMate Application / Technology | `architecture/Architecture-Overview.md` C4 Context and Container views |
| Base - delivery | UML plus one C4 Component | `design/DD-01..05`, `design/entity-lifecycle-models.md`, selected C4 Component for `Redemption Engine Service` |

## Adopted Quality Gates

| Gate | Blocks | Pass rule for Loyalty Banking |
|---|---|---|
| G1 | Solution design | Goal, measurable outcome, and `CON.1` to `CON.3` are listed in the Lab 1 index and Motivation/Strategy evidence |
| G2 | Dev + Test design | `RedemptionOrder` states match the state view and process branch evidence |
| G3 | Implementation | C4 Context and Container have no unnamed externals; sync/async edges and names match the Lab 1 index |
| G4 | Coding of integrations | Every C4 Container relationship has a contract row in the Lab 3 contract register |
| G5 | Production release | Critical fulfillment failure compensation is modeled from `CON.3` |
| G6 | UAT sign-off | State transitions and sequence alternatives are mapped to planned tests with C4 SUT names |

## RACI Line Template for After Views

```text
Title:      <view title>
Viewpoint:  <ArchiMate / C4 / UML>
Layer(s):   <Strategy / Business / App / Tech / Delivery>
As-Is | To-Be | Transition:  To-Be
Owner:      Role <Owner role>  Name <simulated role name>
RACI:       R <role>  A <role>  C <role list>  I <role list>
Version:    v1.0  Date 2026-08-21  Status Review
Legend:     relationships listed in the diagram or view-specific legend
RACI legend: R = draws; A = approves; C = consulted; I = informed
Scope:      in-scope and out-of-scope as defined in Lab 1 I-1
```

## RACI by Artifact

| Artifact | R | A | C | I |
|---|---|---|---|---|
| Motivation / Strategy | EA | Owner | SA, BA / PO, Sec | DA, Dev, Test, Ops |
| Business Process | BA / PO | Owner | EA, SA, Sec, Test | DA, Dev, Ops |
| Application Cooperation | SA | SA | EA, DA, Sec, Dev | BA / PO, Test, Ops, Owner |
| Technology / Deployment | Ops | SA | Sec, Dev | EA, BA / PO, DA, Test, Owner |
| C4 Context | SA | Owner | EA, BA / PO, Sec | DA, Dev, Test, Ops |
| C4 Container | SA | SA | DA, Sec, Dev, Ops | EA, BA / PO, Test, Owner |
| C4 Component | Dev | SA | DA, Sec, Test | EA, BA / PO, Ops, Owner |
| UML Sequence | Dev | SA | BA / PO, Sec, Test | EA, DA, Ops, Owner |
| UML Activity / State | Test | BA / PO | SA, Sec, Dev | EA, DA, Ops, Owner |

## Non-Competing Gate Policy

The repository keeps existing files such as `quality-gates/Quality-Gates-Architecture.md`, `quality-gates/Quality-Gates-Design.md`, and `architecture/Architectural-Governance-Framework.md` when present. Those files are engineering governance extensions. They are not used as a replacement for the trainee pack gates `G1` to `G6`.
