# Lab 7 — Guide adoption, roster, RACI template, gate register

**System-in-focus:** Loyalty Banking Platform
**R:** EA · **A:** Owner
**Input:** the Guide section of `template/list.md`; the group roster; the archived before pack; `lab2-requirements.md`

---

## 0. Adoption statement

The team adopts the **Guide in `template/list.md` as written**. We do not rewrite it, we do not reword the gates beyond the product wording the Guide permits, and we do not run a second gate set alongside it.

Specifically:

- The six gates **G1–G6** in the Guide are the only quality gates for this pack. There is no G7 and no parallel gate list.
- The **RACI table** in the Guide is used exactly as printed. Section 3 below fills our names into it; it changes no letter.
- The **diagram header** in the Guide is used verbatim on every after view (Labs 8–10).
- The Guide was not opened during Labs 1–6, and the before pack in `before-pack/` has not been edited to look like the Guide.

**Any other gate or governance material in this repository is not a gate for this pack.** That includes everything under `quality-gates/` and the five-stage lifecycle inside `architecture/Architectural-Governance-Framework.md`. Those were written before the pack was re-sequenced, are marked as outside the pack, and are formally renounced here: they do not gate, block, or sign off anything.

### 0.1 Open item — before pack is incomplete

The Guide requires Labs 1–6 to be archived before this sitting. Our archive currently holds **Labs 1–3 only** (`before-pack/`). Lab 5 (UML for named use cases) and Lab 6 (integration ecosystem) have not been run.

This is recorded rather than hidden. The consequence is carried into the gate register in section 4: G2 and G6 cannot be closed until Lab 5 exists, because there is no messy state machine or sequence to audit against in Lab 10.

---

## 1. Group roster

Four members, four seats. The Owner seat is played by the facilitator, because the Guide warns against an Owner who also draws — Owner is the approver on Motivation, Business Process, and C4 Context.

| Guide role | Name | Also holds |
|---|---|---|
| **SA** — Solution Architect | Vũ Trường Quang | — |
| **EA** — Enterprise Architect | Khuất Duy Bách | DA, Sec |
| **BA / PO** — Business Analyst / Product Owner | Đặng Duy Hoàng | Ops |
| **Dev** — Software engineer | Lê Huy Du | Test |
| **Owner** — Business Owner | Facilitator (simulated) | — |

Why the doubling is arranged this way:

- **Dev + Test on one person** is a pairing the Guide allows: both sit at delivery grain.
- **SA + Dev on one person is avoided.** SA approves the C4 Component that Dev draws, so one person would sign their own work.
- **DA and Sec are never R or A** anywhere in the Guide's table, so holding them alongside EA creates no conflict.
- **Ops is R on Technology / Deployment only**, where SA approves — so Ops must not be the SA. It sits with BA / PO instead.

---

## 2. R ≠ A check

The Guide requires a second pair of eyes: the person who draws never signs the same artifact.

| Artifact | R draws | A approves | Same person? |
|---|---|---|---|
| Motivation / Strategy | EA — Khuất Duy Bách | Owner — Facilitator | No |
| Business Process | BA / PO — Đặng Duy Hoàng | Owner — Facilitator | No |
| C4 Context | SA — Vũ Trường Quang | Owner — Facilitator | No |
| C4 Container | SA — Vũ Trường Quang | EA — Khuất Duy Bách | No |
| C4 Component | Dev — Lê Huy Du | SA — Vũ Trường Quang | No |
| Application Cooperation | SA — Vũ Trường Quang | EA — Khuất Duy Bách | No |
| UML Sequence | Dev — Lê Huy Du | SA — Vũ Trường Quang | No |
| UML Activity / State | Test — Lê Huy Du | BA / PO — Đặng Duy Hoàng | No |
| Technology / Deployment | Ops — Đặng Duy Hoàng | SA — Vũ Trường Quang | No |

Nine artifacts, no artifact with two A's, no artifact where R and A are the same person.

---

## 3. RACI as adopted

The letters below are the Guide's table, unchanged. Only the name row underneath is ours.

| Artifact | EA | SA | BA / PO | DA | Sec | Dev | Test | Ops | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Motivation / Strategy | R | C | C | I | C | I | I | I | A |
| Business Process | C | C | R | I | C | I | C | I | A |
| C4 Context | C | R | C | I | C | I | I | I | A |
| C4 Container | A | R | I | C | C | C | I | C | I |
| C4 Component | I | A | I | C | C | R | C | I | I |
| Application Cooperation | A | R | I | C | C | C | I | I | I |
| UML Sequence | I | A | C | I | C | R | C | I | I |
| UML Activity / State | I | C | A | I | C | C | R | I | I |
| Technology / Deployment | I | A | I | I | C | C | I | R | I |

Who each column is: **EA** Khuất Duy Bách · **SA** Vũ Trường Quang · **BA / PO** Đặng Duy Hoàng · **DA** Khuất Duy Bách · **Sec** Khuất Duy Bách · **Dev** Lê Huy Du · **Test** Lê Huy Du · **Ops** Đặng Duy Hoàng · **Owner** Facilitator.

RACI legend: R = draws · A = approves · C = consulted · I = informed.

---

## 4. Diagram header template

Copied from the Guide. This block goes on **every** after view in Labs 8, 9, and 10.

```
Title:      ________________________________
Viewpoint:  ArchiMate / C4 / UML ___________
Layer(s):   Strategy / Business / App / Tech
As-Is | To-Be | Transition:  ______ (circle one; this pack = To-Be
            unless the view is the Lab 2 as-is analysis)
Owner:      Role ________  Name ____________
RACI:       R ____  A ____  C ____  I ____
Version:    v____  Date ________  Status Draft|Review|Approved
Legend:     relationships listed
RACI legend: R = draws · A = approves · C = consulted · I = informed
Scope:      in-scope / out-of-scope
```

Worked example, so every drawer fills it the same way:

```
Title:      Loyalty Banking — Application Cooperation
Viewpoint:  ArchiMate
Layer(s):   Application
As-Is | To-Be | Transition:  To-Be
Owner:      Role SA          Name Vũ Trường Quang
RACI:       R SA   A EA   C DA Sec Dev   I BA/PO Test Ops Owner
Version:    v1.0  Date __________  Status Draft
Legend:     Serving, Flow, Access, Realization
RACI legend: R = draws · A = approves · C = consulted · I = informed
Scope:      in-scope Lab 1 I-4 containers and I-8 edges / out-of-scope container internals
```

---

## 5. Gate register — G1 to G6

Pass rules are the Guide's rules in our product wording. No gate is skipped, no gate is added.

Evidence for every gate is an after-pack artifact from Labs 8–10, which have not been drawn yet. Nothing below is marked Pass at this sitting.

| Gate | Blocks | Pass rule for Loyalty Banking Platform | Evidence artifact | Pass? |
|---|---|---|---|---|
| **G1** Strategy signed | Solution design | The Motivation or Strategy view carries the I-1 Goal, the I-1 measurable Outcome, and all four constraints CON.1–CON.4. No protocol, node, or container internal appears on it. | Lab 8 view 1 | Not yet — Lab 8 not drawn |
| **G2** Process + states | Dev + Test design | The Business Process view shows the I-5 happy path with the relevant `CON.*` on its decision branches, and the `RedemptionOrder` states it names are exactly the six in I-6. | Lab 8 view 2, checked against the Lab 5 state machine | Not yet — Lab 8 not drawn, and Lab 5 not run |
| **G3** C4 Context + Container | Implementation | Exactly one Context and one Container exist. Context carries no container, database, or event bus. Every external is named and is in I-3. Every Container edge is labelled sync or async. Every box name is a Lab 1 string. | Lab 9 Context and Container | Not yet — Lab 9 not drawn |
| **G4** Contracts | Coding of integrations | Every relationship drawn on the Lab 9 Container view has a matching row in the `lab3-spec.md` contract register giving producer, consumer, sync or async, and the operation or event name. | `lab3-spec.md` §4 reconciled against Lab 9 | Not yet — Lab 9 not drawn |
| **G5** Critical exception path | Production release | The fulfilment-failure path from CON.3 is modelled with its compensating action: `RedemptionOrder` moves `IN_PROGRESS` → `FAILED` → `REVERSED`, and the restored points keep their original earn date and expiry. | `lab3-spec.md` §5 EXC-05, shown on the Lab 10 sequence | Not yet — Lab 10 not drawn |
| **G6** Test coverage | UAT sign-off | Every I-6 transition of `RedemptionOrder` and every `alt` on the Lab 10 sequences maps to a planned test, and every SUT name is a Lab 9 container name. | `lab3-spec.md` §6 reconciled against Lab 10 | Not yet — Lab 10 not drawn, and Lab 5 not run |

This pack **draws through G3**. G4, G5, and G6 are checklists over the models — nothing is implemented, coded, or stood up to satisfy them.

---

## 6. Done-when check for this sitting

| Requirement | Status |
|---|---|
| Roster exists | Yes — section 1 |
| Adoption record exists, Guide used as written | Yes — section 0 |
| Gate register G1–G6 exists | Yes — section 5 |
| No competing gate list | Renounced in section 0; `quality-gates/` and the five-stage lifecycle are marked outside the pack |
| No G7 or later | None |
| Before pack still archived unchanged | `before-pack/` unchanged, but holds Labs 1–3 only — see section 0.1 |
