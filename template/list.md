# Team modeling pack

**This is the only trainee file.** Groups of 3–4. Each team has **its own topic**. Do not use other briefs.

**Rules:** English. Simulated scenario only — no real customer data, no internal production system names, no production credentials.

Keep **two packs** of the same architecture and design:

| Pack | What |
|------|------|
| **Before modeling** | Labs 1, 2, 5, 6, 8, 9, 10 as first drawn. Current style allowed. Archive unchanged. |
| **Modeling** | Lab 7 — adopt the Guide in this file (do not rewrite it). |
| **After modeling** | Lab 4 restyles the same views. One language, Input names, header + RACI. |
| **Compare** | Lab 4 comparison note — what modeling changed. |

Do not delete the before pack.

| Go to | What it is |
|-------|------------|
| **[Labs](#labs)** | Ten labs — Input, Output, Done when, Fail if |
| [Input](#input) | Blank tables — fill for *this* topic (Lab 1) |
| [Guide](#guide) | Method. Adopt in Lab 7. Enforce on the **after** pack |
| [Legend](#legend) | Short names (`I-*`, `CON.*`, `G1–G6`, `L1–L3`, …) |
| [Submit](#submit-checklist) | End-of-pack checklist |

## Labs at a glance

Full Input / Output / Done / Fail for each lab is **immediately below**. Worksheets: [Input](#input). Method: [Guide](#guide). Short names: [Legend](#legend).

| Lab | Title | You deliver |
|-----|--------|-------------|
| [1](#lab-1) | Scopes with concrete values | Fill [Input](#input) I-1–I-11 |
| [2](#lab-2) | Requirements, analysis, quality gates | Requirements, analysis, trace, G1–G6 register (after pass) |
| [3](#lab-3) | Implement architecture, design, and test | Build list, Component, sequence, contracts, exception spec, test spec |
| [4](#lab-4) | Standardize following modeling-driven design | After pack + comparison note (same views restyled) |
| [5](#lab-5) | Low-level design (UML) | Sequence, activity, state for named use cases only |
| [6](#lab-6) | Integration ecosystem (model, do not build) | Gateway / bus / adapter as containers; labels only |
| [7](#lab-7) | Hierarchy, focus matrix, quality gates, RACI | Adoption record — Guide as written |
| [8](#lab-8) | ArchiMate views (named set) | Four views: Motivation or Strategy, Process, Application, Technology |
| [9](#lab-9) | C4 Context and Container | One Context (L1) + one Container (L2); optional one Component |
| [10](#lab-10) | UML low-level design for named C4 use cases | Sequence (+ state if not Lab 5) for I-11 use cases |

---

# Labs

Fill [Input](#input) for your topic. Deliver each lab’s Output. Architecture and design may be drawn **before** Lab 7; Lab 4 produces the **after** pack for comparison.

---

<a id="lab-1"></a>

## Lab 1 — Scopes with concrete values

**Bound form:** lock the initiative. Empty cells are not allowed.

**R:** BA / SA · **A:** Owner

### Input

- Team topic / initiative name (assigned to the group).
- Owner appetite: why this change exists (one sentence is enough to start).
- As-is pain in business language (not a system list).
- Hard limits: no real customer data, no production system names, no production credentials.

### Output

Completed **[Input I-1–I-11](#input)** (name-identity index). Every downstream view may use **only** these strings.

**Done when:** every Input field is filled; names do not collide; in/out do not overlap.  
**Fail if:** vague system (“the new app”); real vendor/host IDs; two masters for one data object; process steps that name containers not listed.

---

<a id="lab-2"></a>

## Lab 2 — Requirements, analysis, quality gates

**Bound form:** write requirements and analysis for the topic. **After** Lab 7, regenerate against G1–G6 in the [Guide](#guide). Keep both files. Do not invent a second gate set.

**R:** BA (requirements) · EA (trace to Motivation) · **A:** Owner

### Input

- Lab 1 scope index (I-1, I-5, I-6, I-10).
- **Before modeling:** no gate table required. Write requirements and analysis in the team’s current language.
- **After modeling:** G1–G6 from the Guide. Pass-rule *wording* may be adjusted to the product; gates may not be skipped or replaced.
- Stakeholders who will sign G1 (Owner) and G2 (BA + Test) on the after pass.

### Output

| Artifact | Content |
|----------|---------|
| Requirements list | Each requirement traces to Goal, a CON.*, a process step, or a state |
| Analysis | As-is vs to-be; capabilities implied by the goal; exception paths named |
| Gate register (after pass) | G1–G6 rows: pass rule (product wording), evidence artifact, Pass? |
| Trace table | Requirement ID → process step → CON.* → named object/state |

**Done when:** every Lab 1 goal / outcome / CON.* appears in the requirements list. After pass: G1–G2 evidence is named; before and after files both kept; no extra gate table.  
**Fail if:** the before file is deleted; a new G7+ on the after pass; requirements that add systems not in Lab 1.

---

<a id="lab-3"></a>

## Lab 3 — Implement architecture, design, and test

**Bound form:** specification of what will be built and tested. Same names as Lab 1. One selected container from I-11.

**R:** Dev (build list, Component, contracts) · Test (test spec) · **A:** SA

### Input

- Lab 1: I-4 containers, I-8 integration, I-9 deployment, I-11 selected container, I-6 states, I-10 CON.*.
- Lab 9: C4 Container (and Component if drawn).
- Lab 5 / Lab 10: sequence and state for named use cases.
- Lab 2: requirements and CON.* for exception paths.

### Output

| Artifact | Content |
|----------|---------|
| Build list | Every I-4 container: owner (Dev name), build order (1…n), environment from I-9 |
| To-be Component | C4 Component inside the **one** I-11 container; neighbours as black boxes |
| To-be sequence | Named use case for that container; each message owned by a Component module or a neighbour container |
| Contract register (G4) | One row per Container relationship: producer, consumer, sync or async, operation or event name |
| Exception spec (G5) | Critical failure path from CON.*: trigger, compensating action, who performs it |
| Test spec (G6) | One row per state transition and per sequence `alt`: ID, SUT (C4 name), expected result |

**Done when:** all six artifacts are filled; every contract row matches a Lab 9 / I-8 edge; every test row maps to I-6 or a sequence `alt`; SUT names = I-4.  
**Fail if:** a contract invents an external not in I-3; a test SUT is not a C4 container name; Component internals for a container that is not I-11.

---

<a id="lab-4"></a>

## Lab 4 — Standardize following modeling-driven design

**Bound form:** restyle the **before** architecture and design with the Guide. Keep both packs and compare.

**R:** SA · **A:** EA

### Input

- Lab 1 name-identity index.
- Lab 7 adoption (Guide in this file).
- **Before pack** — copies of Labs 8, 9, 10, 5, 6 as first drawn (do not edit in place).

### Output

| Artifact | Content |
|----------|---------|
| After pack | Same views as before, restyled: one language, Lab 1 names, header + RACI |
| Name-identity check | Every box / lifeline string = Lab 1; no forks |
| Language check | One viewpoint per canvas; no mixed relationships |
| Defect list (before) | Failures found on the before pack, each with owner |
| Comparison note | What changed: names, layers, language mix, missing legend/RACI, internals on Context |

**Automatic fail (after pack only):** mixed languages; forked names; missing legend; missing RACI letters; two **A**s; internals on Context; sequence participants that are not C4 Container names.

**Done when:** before pack is archived unchanged; after pack exists; comparison note lists before vs after.  
**Fail if:** the before pack is overwritten; after pack is a new landscape instead of the same views restyled.

---

<a id="lab-5"></a>

## Lab 5 — Low-level design (UML)

**Bound form:** UML for **named use cases only**. MVP is not trainee output.

**R:** Dev (sequence) · Test (activity/state) · **A:** SA (sequence) · BA (activity/state)

### Input

- Lab 1: I-11 use cases; I-6 object + states + terminals.
- Container names: Lab 9 if drawn, else I-4.
- Lab 2 CON.* for `alt` / decision branches (if written).
- Lab 8 process if drawn.

### Output

| Artifact | Rules |
|----------|--------|
| UML Sequence (one per named use case) | One use case per canvas; one `alt` minimum; participants ⊆ I-4 / Lab 9 containers + I-2 actors |
| UML Activity | Same happy path as I-5 / Lab 8; decisions show CON.* |
| UML State | **One** object per machine; states = I-6 |
| G6 checklist | Each transition and each `alt` has a planned test (not executed) |

**Done when:** every I-11 use case has a sequence; the named object has a state machine; G6 checklist filled.  
**Fail if:** after pack uses lifelines not in I-4 / Lab 9; several objects on one state machine; happy path only; MVP or source code; before pack deleted.

---

<a id="lab-6"></a>

## Lab 6 — Integration ecosystem (model, do not build)

**Bound form:** draw gateway / event bus / adapter **as containers**. Product names (Kong, Apigee, Kafka, Keycloak, …) are **labels only**. Do not install.

**R:** SA · **A:** SA · **C:** Sec, Ops

### Input

- Lab 1: I-4 containers, I-8 integration, I-9 deployment.
- Lab 9 Container or Lab 8 Application Cooperation if already drawn.
- AuthN rule: if AuthN already sits on the API gateway, do **not** add a separate IAM product as a system.

### Output

| Artifact | Rules |
|----------|--------|
| Ecosystem on Container / Application Cooperation | Gateway, event bus, adapter **only if** they are in I-4 |
| Edge labels | Protocol + **sync vs async**; event names if an event bus exists |
| Label note | Optional product label on the container; not a second box |
| Negative evidence | No Docker, no cluster, no IAM realm, no broker admin |

**Done when:** every I-8 pattern is visible; no extra product-system; nothing installed.  
**Fail if:** a running Kong / Keycloak / Kafka stack; IAM added as a new system while AuthN is on the gateway; internals of the broker on Context.

---

<a id="lab-7"></a>

## Lab 7 — Hierarchy, focus matrix, quality gates, RACI

**Bound form:** **adopt** the [Guide](#guide) in this file as written. Do not rewrite G1–G6 or invent a parallel RACI. Does not block Labs 1, 2, 5, 6, 8, 9, 10.

**R:** EA · **A:** Owner

### Input

- The **Guide** section of this file.
- Group roster: who plays EA, SA, Dev, Test (one person may hold two roles).
- Before pack if already drawn (useful for Lab 4).

### Output

| Artifact | Content |
|----------|---------|
| Adoption record | Names mapped to EA / SA / Dev / Test; statement that the Guide (G1–G6 and RACI) is used as written |
| RACI line template | Copied onto every **after** diagram header |

**Done when:** roster + adoption record exist; no competing gate list.  
**Fail if:** a custom quality-gate table; two A’s on an after view; treating C4 as an EA language.

---

<a id="lab-8"></a>

## Lab 8 — ArchiMate views (named set)

**Bound form:** **four named views**, not every ArchiMate layer.

**R:** EA (Motivation/Strategy) · BA (Process) · SA (Application Cooperation) · Ops/SA (Technology) · **A:** Owner (Motivation, Process)

### Input

- Lab 1: I-1 goal/outcome, I-5 process, I-4 containers, I-9 deployment, I-10 CON.*.
- Lab 2 requirements if already written.
- **Before:** draw in the team’s current style; keep copies.
- **After:** ArchiMate only; header + RACI from the Guide.

### Output

| # | View | Must show | Must not show |
|---|------|-----------|----------------|
| 1 | Motivation **or** Strategy | Goal, outcome, CON.* (G1) | Protocol, pods, JDBC, container internals |
| 2 | Business Process | Happy path I-5; CON.* on branches (G2) | C4 containers as process boxes; sync/async labels |
| 3 | Application Cooperation | Containers = I-4; same strings as C4 Container | UML messages; mixed C4 notation |
| 4 | Technology / hybrid | Locations from I-9; no forbidden path | Channel (or equivalent) writing the core ledger DB |

**Done when:** four views exist. After pack: headers, names = Lab 1, G1 on view 1, G2 on view 2.  
**Fail if:** “all layers”; after pack mixes languages or forks Lab 1 names; before pack deleted.

---

<a id="lab-9"></a>

## Lab 9 — C4 Context and Container

**Bound form:** **one** Context (L1) + **one** Container (L2). Optional: **one** Component inside **one** container.

**R:** SA · **A:** Owner (Context) · SA (Container) · Dev **R** / SA **A** (optional Component)

### Input

- Lab 1: I-1 system-in-focus, I-2 actors, I-3 externals, I-4 containers, I-8 sync/async, I-11 optional Component container.
- Lab 8 Application Cooperation if it exists.
- **Before:** draw in the team’s current style; keep copies.
- **After:** no internals on Context; do not mix L1+L2+L3 on one canvas.

### Output

| Artifact | Include | Forbid |
|----------|---------|--------|
| **C4 Context (L1)** | People + system-in-focus + externals. Relationships = *what happens*, not protocol | Containers, databases, pods, event buses, class names |
| **C4 Container (L2)** | I-4 containers; externals as needed; protocol + **sync vs async** | Exploding every container; unnamed externals |
| **C4 Component (optional)** | Internals of **one** I-11 container; neighbours as black boxes | Those components on Context; a second container exploded |

**Done when:** one Context + one Container exist. After pack: G3 (no internals; sync/async; names = Lab 1).  
**Fail if:** several Context diagrams; after pack internals on Context or mixed L1+L2+L3; new externals not in I-3; before pack deleted.

---

<a id="lab-10"></a>

## Lab 10 — UML low-level design for named C4 use cases

**Bound form:** LLD for **named use cases**, not every C4 component.

**R:** Dev · **A:** SA · **C:** Test, BA

### Input

- Lab 1: I-11 use cases; I-6 states.
- Lab 9 Container names if drawn; else I-4. Optional Component internals for **one** container only.
- Lab 6 gateway / event bus names if they are participants.
- Lab 2 CON.* for exception branches.

### Output

| Artifact | Rules |
|----------|--------|
| Sequence for each named use case | Participants ⊆ I-4 / Lab 9 (+ actors). Component modules only inside the one selected container |
| State (if not already done in Lab 5) | One object; I-6 states |
| Participant = SUT map | Each lifeline → I-4 string |
| Coverage note | G6: every `alt` and every state transition listed |

**Done when:** every named use case has a sequence; participants match I-4 / Lab 9; G6 note complete.  
**Fail if:** “all components”; after pack lifeline not in I-4 / Lab 9; Component details of a container that was not selected; before pack deleted.

---

# Guide

Use this standard from Lab 7 onward. The **after** pack must follow it. The **before** pack may not.

Golden rule: **ArchiMate aligns the enterprise, C4 aligns the build, UML aligns the behavior and tests.**

Pick the language by the **question**, then by audience. Do not pick a language because a tool can draw it.

## Languages

| Language | What it is | What it is not | Question it answers |
| --- | --- | --- | --- |
| **ArchiMate 3.2** | Enterprise notation: why, who, capability, layer | A software zoom. No container, protocol, or sequence grain | Why / who / what capability / which layer |
| **C4** | Nested software architecture: Context, Containers, Components. A **container** is a runnable unit — not Docker | An EA language. No motivation, strategy, or process catalog | Which system, container, component |
| **UML** | Structure and behavior: sequence, activity, state, class | A bank-wide landscape. Not a nested C4 zoom | How it behaves; exact types |

| Question | Language | Typical diagram |
| --- | --- | --- |
| Why are we changing? Which capabilities? | ArchiMate | Motivation, Strategy |
| Who does the work? What product / contract? | ArchiMate | Organization, Product, Business Process |
| What systems exist and who uses them? | C4 | Context (L1) |
| How is the platform decomposed for build? | C4 | Container (L2) |
| What runs inside one container? | C4 | Component (L3) — **one** container only |
| What is the happy / exception path of one use case? | UML | Sequence |
| What are the business steps and decisions? | UML or ArchiMate Process | Activity / Business Process |
| What states can one named object be in? | UML | State machine |
| Where does it run? | ArchiMate Technology and C4 / UML Deployment | Deployment |

**After pack:** one language per diagram. Cross-reference with a mapping table and a name-identity list — not by mixing boxes.

**Name identity:** ArchiMate Application Component **is** the C4 Container **is** the UML sequence participant **is** the test SUT. List those strings once in Input. Do not fork names.

| Grain | Language | Meaning |
| --- | --- | --- |
| Landscape application | ArchiMate Application Component = C4 Container | Runnable / deployable building block |
| Inside one container | C4 Component | Module the team builds |
| Time-ordered messages | UML Sequence | Participants ⊆ C4 Container names |
| Lifecycle of one object | UML State | One business / data object, not a container |

## Hierarchy

Three **levels of design**, not three competing notations. Upper levels constrain lower ones; lower levels realize upper ones.

| Level | Language | Focus | Readers | Typical views |
|-------|----------|-------|---------|---------------|
| **Top** — enterprise | ArchiMate | Governance, strategy, process | EA, Owner, Risk, BA | Motivation, Strategy, Business Process |
| **Middle** — solution | C4 (+ ArchiMate Application / Technology) | System boundary, containers | SA, DA, Security | C4 Context, C4 Container, Application Cooperation, Technology |
| **Base** — delivery | UML (+ one C4 Component) | Sequence, states, types | Dev, Test, Ops | C4 Component (one container), Sequence, Activity, State |

One nesting thread: capability → system-in-focus → runnable container → (optional) module inside **one** container → messages / states. Do not start a second enterprise story mid-stack.

The stack is a **bridge**: Enterprise (ArchiMate) → Solution (C4) → Delivery (UML) → as-built feedback back to SA → landscape update.

## Focus matrix

| | ArchiMate — landscape | C4 — bridge | UML — deployable behavior |
| --- | --- | --- | --- |
| **Focus** | Why / who / capability / layer | Which system, container, component | How it behaves; exact types |
| **Zoom** | Enterprise | Solution (L1–L2) and one-container design (L3) | Delivery |
| **Audience** | EA, Owner, Risk, BA | SA, DA, Security; Dev on L3 | Dev, Test, Ops |
| **Pack** | Architecture | Architecture owns L1–L2; Design owns L3 | Design |
| **Fail if** | JDBC or pods on Motivation / Process | Internals on Context; mixed L1+L2+L3 on one canvas | Lifelines that are not C4 names; several objects on one state machine |

## Quality gates (G1–G6 only)

**Block coding / UAT if red.** Informal slides do not pass a gate. Do not invent a second gate set. Adjust pass-rule *wording* to your product; do not skip a gate.

| Gate | Blocks | Pass rule |
| --- | --- | --- |
| **G1** Strategy signed | Solution design | Goal, outcome, constraints listed |
| **G2** Process + states | Dev + Test design | Named states match the information / state view |
| **G3** C4 Context + Container | Implementation | No unnamed externals; sync / async labeled; names match the Input index |
| **G4** Contracts | Coding of integrations | Contract (OpenAPI or equivalent) for every relationship on the Container diagram |
| **G5** Critical exception path | Production release | Compensating actions on the critical failure path are modeled |
| **G6** Test coverage | UAT sign-off | All state transitions + sequence alts mapped; participants = C4 names |

This pack **draws through G3**. G4–G6 are checklists on the models. You do **not** implement, code, or stand up runtime.

## RACI

RACI is per **artifact**, not a job title. One person may hold two roles; the artifact still has one **R** and one **A**.

| Letter | Meaning | Typical action |
| --- | --- | --- |
| **R** | Responsible — draws | Produce the view; keep names identical to the Input index |
| **A** | Accountable — approves (exactly one, except dossier index) | Accept / reject |
| **C** | Consulted — two-way before freeze | Review, constrain |
| **I** | Informed — one-way after accept | Read; do not redraw |

Typically one **R**. If two roles share the pen, split the artifact.

| Abbr. | Role |
| --- | --- |
| **Owner** | Business Owner |
| **PO** | Product Owner |
| **BA** | Business Analyst |
| **EA** | Enterprise Architect |
| **DA** | Domain / Data Architect |
| **SA** | Solution Architect |
| **Sec** | Security / Compliance / Risk |
| **Dev** | Software engineer |
| **Test** | Quality engineer |
| **Ops** | DevOps engineer |

| Artifact | EA | SA | BA / PO | DA | Sec | Dev | Test | Ops | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Motivation / Strategy | R | C | C | I | C | I | I | I | A |
| Business Process | C | C | R | I | C | I | C | I | A |
| C4 Context | C | R | C | I | C | I | I | I | A |
| C4 Container | I | R | I | C | C | C | I | C | I |
| C4 Component | I | A | I | C | C | R | C | I | I |
| Application Cooperation | C | R | I | C | C | C | I | I | I |
| UML Sequence | I | A | C | I | C | R | C | I | I |
| UML Activity / State | I | C | A | I | C | C | R | I | I |
| Technology / Deployment | I | A | I | I | C | C | I | R | I |

How to read a row: find **R** (who draws) and **A** (who signs). Example — C4 Context: SA **R**, Owner **A**. Example — C4 Component: Dev **R**, SA **A**. Example — Activity / State: Test **R**, BA **A**.

### Role input → output (handoff)

| Role | Minimum input to start | Minimum output to hand off | Next consumer |
| --- | --- | --- | --- |
| EA | Strategy, as-is landscape | Motivation, capabilities, principles | SA, BA, Owner |
| BA / PO | Goals, journeys | Process, product, use cases, activity | SA, Test, Owner |
| SA | EA + BA packs | C4 L1–L2, app cooperation, interfaces, NFRs | Dev, DA, Sec, Ops, Test |
| DA | Business objects + containers | Information structure, source of truth | Dev, Test, Sec |
| Sec | Constraints + C4 + data | Risk view, trust boundaries | Dev, Test, SA |
| Dev | C4 L2, contracts, sequence, states | C4 L3, as-built sequence | Test, Ops, SA |
| Test | Process, state, sequence, C4, constraints | Scenario catalog, coverage | Dev, SA, Owner |
| Ops | Tech view, containers, NFRs | Deployment, paths | Dev, SA, Test |

## Packs

**Architecture pack** (enough to start a container drill): Motivation, Strategy, Process, Application Cooperation, Technology, C4 Context, C4 Container, NFRs, interface list, which **one** container Dev drills to L3.

Out of Architecture pack: C4 Component internals; UML as the primary design; protocol / JDBC / pods / class names on Context.

**Design pack** starts at C4 Component (L3): one-container internals, UML Sequence (participants ⊆ C4 Container names), Activity / State (one object), contracts derived from those models.

Out of Design pack: a second landscape; exploding every L2 container at once; new externals not already on Context.

## Diagram header (every **after** view)

```
Title:      ________________________________
Viewpoint:  ArchiMate / C4 / UML ___________
Layer(s):   Strategy / Business / App / Tech
As-Is | To-Be | Transition:  _______________
Owner:      Role ________  Name ____________
RACI:       R ____  A ____  C ____  I ____
Version:    v____  Date ________  Status Draft|Review|Approved
Legend:     relationships listed
RACI legend: R = draws · A = approves · C = consulted · I = informed
Scope:      in-scope / out-of-scope
```

**Automatic fail (after pack only):** mixed languages; forked names; missing legend; missing RACI letters; two **A**s; internals on Context; sequence participants that are not C4 Container names.

## Model, do not build

If the topic has a gateway or event bus, draw them as **C4 Containers / ArchiMate Application Components**. Product names (Kong, Apigee, Kafka, Keycloak, …) may appear as **labels** only.

Do **not** install Docker or stand up those products. Do **not** add a security product as a new system if AuthN already belongs on the API gateway. Do **not** implement code, automated tests, or an MVP in this pack.

## Suggested group roles

| Role | Owns (output) |
|------|----------------|
| EA | Motivation / Strategy |
| SA | C4 Context + Container, Application Cooperation |
| Dev | One C4 Component **or** UML sequence for a named use case |
| Test | State of the named business object + G6 coverage checklist |

---

# Input

Fill for **your team’s topic** (Lab 1). Complete every cell. These strings become the name-identity index. Downstream views may use **only** these names.

## I-1. Team and topic

| Field | Your value |
|-------|------------|
| Group | |
| Topic / initiative name | |
| System-in-focus | |
| Goal | |
| Outcome (measurable) | |
| Product | |
| Contract | |
| Baseline → target | |
| In scope | |
| Out of scope | |

## I-2. Actors

| Name | ArchiMate | C4 (Person or —) | Role in the process |
|------|-----------|------------------|---------------------|
| | | | |
| | | | |

## I-3. External systems

| Name (simulated) | Responsibility |
|------------------|----------------|
| | |
| | |

Do not use real vendor contract IDs or production host names.

## I-4. Internal containers

Same strings on ArchiMate Application Cooperation and C4 Container.

| Name | Responsibility |
|------|----------------|
| | |
| | |

## I-5. Business process (happy path)

Numbered steps. Name the business object that moves.

1.
2.
3.

**Principle / hard rules** (what must never happen):

-

## I-6. Named object states (use exactly on UML State)

**Object:** _______________

States and transitions:

Terminal states:

## I-7. Source of truth

| Data object | Meaning | Source of truth (one container or external) |
|-------------|---------|---------------------------------------------|
| | | |

## I-8. Integration (label sync vs async on Container)

| Pattern | Mechanism | Example on your landscape |
|---------|-----------|---------------------------|
| Sync | | |
| Async | | |
| Legacy / adapter (if any) | | |

## I-9. Deployment

| Location | What runs there |
|----------|-----------------|
| | |

Forbidden path (example: channel must not write the core ledger DB):

## I-10. Constraints (must appear on Motivation and on decision branches)

| ID | Constraint | Effect on the process |
|----|------------|------------------------|
| CON.1 | | |
| CON.2 | | |
| CON.3 | | |

## I-11. Named use cases for UML (not every component)

| Use case | Happy path | At least one exception (`alt`) |
|----------|------------|--------------------------------|
| | | |

**One container** for optional C4 Component (circle one): _______________

---


# Legend

Short names used in the labs. `*` is a wildcard: `CON.*` means every constraint ID, not an element named `CON.*`.  
RACI letters and role abbreviations are in the [Guide](#guide).

### Input index

| Short | Means |
|-------|--------|
| **I-*** | Any Input section (`I-1`…`I-11`) |
| **I-1** | Team, topic, system-in-focus, goal, outcome, product, in/out |
| **I-2** | Actors |
| **I-3** | External systems |
| **I-4** | Internal containers (same strings on Application Cooperation and C4 Container) |
| **I-5** | Happy-path process + hard rules |
| **I-6** | Named object states (UML State) |
| **I-7** | Source of truth |
| **I-8** | Integration (sync / async / adapter) |
| **I-9** | Deployment locations + one forbidden path |
| **I-10** | Constraints table (`CON.1`…) |
| **I-11** | Named use cases + the **one** container for optional C4 Component |

### Constraints and optional ArchiMate IDs

| Short | Means |
|-------|--------|
| **CON.*** | All constraints from I-10 |
| **CON.n** | One constraint (`CON.1`, `CON.2`, or a named ID such as `CON.KYC`) |
| **MOT.CON.n** | Same rule drawn as an ArchiMate Motivation **Constraint** (optional prefix) |
| **MOT.GOAL.n** / **MOT.OUT.n** / **MOT.REQ.n** | Optional Motivation IDs for Goal / Outcome / Requirement |
| **STR.CAP.n** | Optional Strategy **Capability** ID |

### Quality gates

Full pass rules stay in the Guide. Do not add **G7**.

| Short | Means | Blocks |
|-------|--------|--------|
| **G1–G6** | The six quality gates (adopt as written) | — |
| **G1** | Strategy signed — goal, outcome, `CON.*` listed | Solution design |
| **G2** | Process + states — named states match the state view | Dev + Test design |
| **G3** | C4 Context + Container — names, externals, sync/async | Implementation |
| **G4** | Contracts — one contract per Container relationship | Coding of integrations |
| **G5** | Critical exception path — compensating action from `CON.*` | Production release |
| **G6** | Test coverage — every state transition and sequence `alt` | UAT sign-off |

### C4 zoom and UML / test

| Short | Means |
|-------|--------|
| **L1** | C4 Context |
| **L2** | C4 Container |
| **L3** | C4 Component — internals of **one** container |
| **L4** | C4 Code — out of pack unless I-1 scoped it |
| **`alt`** | UML sequence fragment: exception / decision branch (show `CON.*`) |
| **SUT** | System under test — must be a C4 / I-4 container name |
| **⊆** | Participants are a **subset** of named containers (and actors) |
| **LLD** | Low-level design (UML), not an MVP |

### Other

| Short | Means |
|-------|--------|
| **AuthN** | Authentication |
| **IAM** | Identity and access management product (do not add as a new system if AuthN is already on the gateway) |
| **UAT** | User acceptance test |
| **MVP** | Minimum viable product — not trainee output in these labs |
| **JDBC** | Database protocol — fail if drawn on Motivation / Process |


# Submit checklist

- [ ] **Before pack** archived (Labs 1, 2, 5, 6, 8, 9, 10 as first drawn) — not deleted
- [ ] Input I-1–I-11 complete (Lab 1)
- [ ] Lab 2 requirements: before file kept; after file against G1–G6 (no new gates)
- [ ] Lab 3: build list, to-be Component, to-be sequence, contract register, exception spec, test spec
- [ ] Lab 7 adoption record (Guide as written)
- [ ] Lab 4 after pack + comparison note (same views restyled)
- [ ] Lab 8: four named ArchiMate views only
- [ ] Lab 9: one Context (no internals after) + one Container (sync/async)
- [ ] Lab 5 / Lab 10: UML for named use cases only; one object per state machine
- [ ] Lab 6: ecosystem modeled, not built
- [ ] After views: header + RACI + legend; English; simulated names only
- [ ] No MVP; no Kong / Keycloak / Kafka stand-up
