# Team modeling pack

**This is the only trainee file.** Groups of 3–4. Each team has **its own topic**. Do not use other briefs.

**Rules:** English. Simulated scenario only — no real customer data, no internal production system names, no production credentials.

Do labs **one at a time, from Lab 1 to Lab 10**. Finish Lab *n* (Done when) before opening Lab *n+1*. Do not skip. Do not parallelize. Do not apply the Guide until Lab 7.

This is how you live the **messy then standardized** journey: Labs **1–6** in your current style, archive that pack, Lab **7** adopts the Guide, Labs **8–10** draw to the standard.

```
1 → 2 → 3 → 4 → 5 → 6  →  ARCHIVE  →  7 → 8 → 9 → 10
     messy (current style)              Guide, then after views
```

| Pack | When | What |
|------|------|------|
| **Before modeling** | Labs 1–6 | Scope, requirements, spec, first cleanup, UML, ecosystem — current style allowed. **Archive unchanged.** |
| **Modeling** | Lab 7 | Adopt the Guide. Do **not** start this until Labs 1–6 are archived. |
| **After modeling** | Labs 8–10 | ArchiMate, C4, UML to the Guide: one language, Input names, header + RACI. |
| **Compare** | Lab 10 (and Lab 4 note) | Lab 5 messy UML vs Lab 10; Lab 4 note on what you cleaned in 1–3. |

**Fail the pack if:** a lab is skipped or started before the previous is Done; Lab 7 starts before Labs 1–6 are archived; the Guide / header / RACI is applied on Labs 1–6; the before pack is deleted or overwritten.

| Go to | What it is |
|-------|------------|
| **[Order](#order-lab-1-to-lab-10)** | Mandatory: Lab 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 |
| **[Labs](#labs)** | Ten labs — Input, Output, Done when, Fail if |
| [Input](#input) | Blank tables — fill for *this* topic (Lab 1) |
| [Guide](#guide) | Method. Open in Lab 7. Enforce on the **after** pack |
| [Legend](#legend) | Short names (`I-*`, `CON.*`, `G1–G6`, `L1–L3`, …) |
| [Submit](#submit-checklist) | Checklist Lab 1 → 10 |

## Order: Lab 1 to Lab 10

One lab at a time. **Start only after** the previous lab is Done. Worksheets: [Input](#input). Method: [Guide](#guide) (open in Lab 7, not before). Short names: [Legend](#legend).

| Lab | Phase | This sitting | Start only after |
|-----|-------|--------------|------------------|
| [1](#lab-1) | Lock | Fill [Input](#input) I-1–I-11 | Topic assigned |
| [2](#lab-2) | Messy | Requirements in current language. **No** G1–G6 | Lab 1 Done |
| [3](#lab-3) | Messy | Build list, Component, sequence, contracts, exception spec, test spec — Lab 1 names | Lab 2 Done |
| [4](#lab-4) | Messy | First cleanup of Labs 1–3 with **your** method. Keep copies. Not the Guide | Lab 3 Done |
| [5](#lab-5) | Messy | UML sequence, activity, state for named use cases | Lab 4 Done |
| [6](#lab-6) | Messy | Gateway / bus / adapter as containers. **Then archive Labs 1–6** | Lab 5 Done |
| [7](#lab-7) | Modeling | Adopt the Guide as written. Map Lab 2 requirements to G1–G6 | Labs 1–6 archived |
| [8](#lab-8) | After | Four ArchiMate views — Guide, header, RACI | Lab 7 Done |
| [9](#lab-9) | After | One Context + one Container — Guide | Lab 8 Done |
| [10](#lab-10) | After | UML vs C4 names; restyle Lab 5; comparison note | Lab 9 Done |

### If you get stuck

Do not jump ahead.

1. **Missing an Input value?** Invent a plausible simulated one, mark it `ASSUMPTION`, and carry on.
2. **Disagree on a name?** The Lab 1 index wins. Change it in [Input](#input) once, then use it everywhere.
3. **Tempted to open the Guide during Labs 1–6?** That is the trap. Messy is the point — defects become the Lab 4 note and the Lab 10 comparison.
4. **Still blocked after 10 minutes?** Ask the facilitator. Do not skip. Do not start the next lab.

---

# Labs

Follow **[Lab 1 to Lab 10](#order-lab-1-to-lab-10)**. One lab at a time. Labs **1–6** are the before pack (messy). Lab **7** is the Guide. Labs **8–10** are the after pack.

---

<a id="lab-1"></a>

## Lab 1 — Scopes with concrete values

**Run:** Lab 1 of 10. **Start only after:** topic assigned. Next: Lab 2.  
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
**Fail if:** vague system (“the new app”); real vendor/host IDs; two masters for one data object; process steps that name containers not listed; any later lab started before this index is complete.

---

<a id="lab-2"></a>

## Lab 2 — Requirements, analysis, quality gates

**Run:** Lab 2 of 10. **Start only after:** Lab 1 Done. Next: Lab 3.  
**Bound form:** one file, current language. **No** G1–G6 (those are filled in Lab 7). Do not invent a second gate set later.

**R:** BA (requirements) · **A:** Owner

### Input

- Lab 1 scope index (I-1, I-5, I-6, I-10).
- Current language. Do not open the Guide to “fix” this file.

### Output

| Artifact | Content |
|----------|---------|
| Requirements list | Each requirement traces to Goal, a CON.*, a process step, or a state |
| Analysis | As-is vs to-be; capabilities implied by the goal; exception paths named |
| Trace table | Requirement ID → process step → CON.* → named object/state |

**Done when:** requirements + analysis + trace exist in current language; every Lab 1 goal / outcome / CON.* appears; **no** gate register yet.  
**Fail if:** the Guide was used to rewrite this file; requirements that add systems not in Lab 1; Lab 3 started before this is Done.

---

<a id="lab-3"></a>

## Lab 3 — Implement architecture, design, and test

**Run:** Lab 3 of 10. **Start only after:** Lab 2 Done. Next: Lab 4. Use Lab 1 names only (no C4 diagram yet).  
**Bound form:** specification of what will be built and tested. Same names as Lab 1. One selected container from I-11.

The six artifacts below are **design evidence** for later gates G4–G6 — models and tables, not runtime proof. You still do not code, run tests, or deploy anything.

**R:** Dev (build list, Component, contracts) · Test (test spec) · **A:** SA

### Input

- Lab 1: I-4 containers, I-8 integration, I-9 deployment, I-11 selected container, I-6 states, I-10 CON.*.
- Lab 2: requirements and CON.* for exception paths.

### Output

| Artifact | Content |
|----------|---------|
| Build list | Every I-4 container: owner (Dev name), build order (1…n), environment from I-9 |
| To-be Component | Modules inside the **one** I-11 container; neighbours as black boxes |
| To-be sequence | Named use case for that container; each message owned by a module or a neighbour container |
| Contract register (G4) | One row per I-8 relationship: producer, consumer, sync or async, operation or event name |
| Exception spec (G5) | Critical failure path from CON.*: trigger, compensating action, who performs it |
| Test spec (G6) | One row per I-6 transition and per sequence `alt`: ID, SUT (I-4 name), expected result |

**Done when:** all six artifacts are filled; every contract row matches an I-8 edge; every test row maps to I-6 or a sequence `alt`; SUT names = I-4.  
**Fail if:** this lab started before Lab 2; a contract invents an external not in I-3; a test SUT is not an I-4 name; Component internals for a container that is not I-11; the Guide was applied.

---

<a id="lab-4"></a>

## Lab 4 — Standardize following modeling-driven design

**Run:** Lab 4 of 10. **Start only after:** Lab 3 Done. Next: Lab 5.  
**Bound form:** first cleanup of Labs **1–3** with **your current method**. Keep copies of 1–3 as they were. Do **not** open the Guide (that is Lab 7). Do not draw ArchiMate / C4 / UML here (those are Labs 8–10).

**R:** SA · **A:** EA

### Input

- Lab 1 name-identity index (as filled).
- Lab 2 requirements file.
- Lab 3 spec (build list, contracts, test spec).
- Copies of Labs 1–3 **before** this cleanup (do not edit those copies in place).

### Output

| Artifact | Content |
|----------|---------|
| Cleaned 1–3 pack | Same artifacts, names made consistent; one list of containers/actors |
| Name-identity check | Every string in Labs 2–3 = Lab 1; no forks |
| Defect list (before) | Failures found on Labs 1–3 as first written, each with owner |
| Comparison note | What you cleaned — and what you still do not know how to standardize |

**Done when:** copies of messy 1–3 exist; cleaned pack exists; comparison note is written.  
**Fail if:** this lab started before Lab 3; the Guide was used; messy copies were overwritten; ArchiMate / C4 / UML diagrams were drawn here.

---

<a id="lab-5"></a>

## Lab 5 — Low-level design (UML)

**Run:** Lab 5 of 10. **Start only after:** Lab 4 Done. Next: Lab 6. **Before pack** — current style. Do not apply the Guide, header, or RACI.  
**Bound form:** UML for **named use cases only**. MVP is not trainee output.

**Lab 5 vs Lab 10:** Lab 5 **creates** the behaviour (sequence, activity, state) from Lab 1 names. Lab 10 **audits and restyles** those diagrams against Lab 9 C4 names. Do not skip Lab 5 because Lab 10 exists.

**R:** Dev (sequence) · Test (activity/state) · **A:** SA (sequence) · BA (activity/state)

### Input

- Lab 1: I-11 use cases; I-6 object + states + terminals; I-4 container names; I-2 actors.
- Lab 2: CON.* for `alt` / decision branches.
- Lab 3 to-be sequence (may be messy — you may redraw).
- Happy path = I-5 (no Lab 8 yet).

### Output

| Artifact | Rules |
|----------|--------|
| UML Sequence (one per named use case) | One use case per canvas; one `alt` minimum; participants ⊆ I-4 + I-2 |
| UML Activity | Same happy path as I-5; decisions show CON.* |
| UML State | **One** object per machine; states = I-6 |
| G6 checklist | Each transition and each `alt` has a planned test (not executed) |

**Done when:** every I-11 use case has a sequence; the named object has a state machine; G6 checklist filled. Archive in the before pack.  
**Fail if:** Guide / header / RACI applied; Lab 7 started already; several objects on one state machine; happy path only; MVP or source code.

---

<a id="lab-6"></a>

## Lab 6 — Integration ecosystem (model, do not build)

**Run:** Lab 6 of 10. **Start only after:** Lab 5 Done. Next: archive Labs 1–6, then Lab 7. **Before pack.** Product names are labels. Do not install. Do not apply the Guide.  
**Bound form:** draw gateway / event bus / adapter **as containers**. Product names (Kong, Apigee, Kafka, Keycloak, …) are **labels only**. Do not install.

**R:** SA · **A:** EA · **C:** Sec, Ops — the drawer never approves their own view

### Input

- Lab 1: I-4 containers, I-8 integration, I-9 deployment.
- Lab 3 contract register (I-8 edges).
- AuthN rule: if AuthN already sits on the API gateway, do **not** add a separate IAM product as a system.

### Output

| Artifact | Rules |
|----------|--------|
| Ecosystem sketch | Gateway, event bus, adapter **only if** they are in I-4 |
| Edge labels | Protocol + **sync vs async**; event names if an event bus exists |
| Label note | Optional product label on the container; not a second box |
| Negative evidence | No Docker, no cluster, no IAM realm, no broker admin |

**Done when:** every I-8 pattern is visible; no extra product-system; nothing installed; **Labs 1–6 archived** as the before pack.  
**Fail if:** Guide applied; a running Kong / Keycloak / Kafka stack; IAM added as a new system while AuthN is on the gateway; archive skipped.

---

<a id="lab-7"></a>

## Lab 7 — Hierarchy, focus matrix, quality gates, RACI

**Run:** Lab 7 of 10. **Start only after:** Labs 1–6 archived. Next: Lab 8. Do **not** start this lab first.  
**Bound form:** **adopt** the [Guide](#guide) in this file as written. Do not rewrite G1–G6 or invent a parallel RACI.

**R:** EA · **A:** Owner

### Input

- The **Guide** section of this file (open it **now**, not during Labs 1–6).
- Group roster: who plays EA, SA, Dev, Test (one person may hold two roles).
- Archived before pack (Labs 1–6) — required.
- Lab 2 requirements (to map onto G1–G6).

### Output

| Artifact | Content |
|----------|---------|
| Adoption record | Names mapped to EA / SA / Dev / Test; statement that the Guide (G1–G6 and RACI) is used as written |
| RACI line template | Copied onto every **after** diagram header (Labs 8–10) |
| Gate register | G1–G6 rows: pass rule (product wording), evidence artifact (will be Labs 8–10), Pass? |

**Done when:** roster + adoption record + gate register exist; no competing gate list; before pack still archived unchanged.  
**Fail if:** this lab started before Labs 1–6; a custom quality-gate table; a new G7+; the before pack was edited to look like the Guide.

---

<a id="lab-8"></a>

## Lab 8 — ArchiMate views (named set)

**Run:** Lab 8 of 10. **Start only after:** Lab 7 Done. Next: Lab 9. **After pack** — Guide, header, RACI required.  
**Bound form:** **four named views**, not every ArchiMate layer.

**R:** EA (Motivation/Strategy) · BA (Process) · SA (Application Cooperation) · Ops/SA (Technology) · **A:** Owner (Motivation, Process)

### Input

- Lab 1: I-1 goal/outcome, I-5 process, I-4 containers, I-9 deployment, I-10 CON.*.
- Lab 2 requirements; Lab 7 gate register (G1 on view 1, G2 on view 2).
- Lab 7 header + RACI template.

### Output

| # | View | Must show | Must not show |
|---|------|-----------|----------------|
| 1 | Motivation **or** Strategy | Goal, outcome, CON.* (**G1**) | Protocol, pods, JDBC, container internals |
| 2 | Business Process | Happy path I-5; CON.* on branches (**G2**) | C4 containers as process boxes; sync/async labels |
| 3 | Application Cooperation | Containers = I-4; same strings as later C4 Container | UML messages; mixed C4 notation |
| 4 | Technology / hybrid | Locations from I-9; no forbidden path | Channel (or equivalent) writing the core ledger DB |

**Done when:** four views exist; headers + RACI; names = Lab 1; G1 on view 1, G2 on view 2.  
**Fail if:** “all layers”; missing header/RACI; mixed languages; Lab 7 skipped.

---

<a id="lab-9"></a>

## Lab 9 — C4 Context and Container

**Run:** Lab 9 of 10. **Start only after:** Lab 8 Done. Next: Lab 10. **After pack** — Guide required.  
**Bound form:** **one** Context (L1) + **one** Container (L2). Optional: **one** Component inside **one** container.

**RACI — one R and one A per artifact** (this lab has three artifacts, so three separate rows; that is not "two A's on one view"):

| Artifact | R (draws) | A (approves) |
|----------|-----------|--------------|
| C4 Context (L1) | SA | Owner |
| C4 Container (L2) | SA | EA |
| C4 Component (optional, L3) | Dev | SA |

R and A must be **different people** on every row. If your group is too small, split the artifact rather than letting one person approve their own drawing.

### Input

- Lab 1: I-1 system-in-focus, I-2 actors, I-3 externals, I-4 containers, I-8 sync/async, I-11 optional Component container.
- Lab 8 Application Cooperation (name identity).
- Lab 6 ecosystem sketch (gateway / bus / adapter must appear **only if** they are in I-4).
- Lab 7 header + RACI template.

### Output

| Artifact | Include | Forbid |
|----------|---------|--------|
| **C4 Context (L1)** | People + system-in-focus + externals. Relationships = *what happens*, not protocol | Containers, databases, pods, event buses, class names |
| **C4 Container (L2)** | I-4 containers; externals as needed; protocol + **sync vs async** | Exploding every container; unnamed externals |
| **C4 Component (optional)** | Internals of **one** I-11 container; neighbours as black boxes | Those components on Context; a second container exploded |

**Done when:** one Context + one Container exist; no internals on Context; sync/async labeled; names = Lab 1 (**G3**); header + RACI.  
**Fail if:** several Context diagrams; mixed L1+L2+L3; new externals not in I-3; Lab 7 skipped; missing header/RACI.

---

<a id="lab-10"></a>

## Lab 10 — UML low-level design for named C4 use cases

**Run:** Lab 10 of 10. **Start only after:** Lab 9 Done. **After pack.** Restyle Lab 5 against Lab 9 names. Write the comparison note.  
**Bound form:** LLD for **named use cases**, not every C4 component.

**Relation to Lab 5:** take the Lab 5 sequences and **audit** them — every lifeline must resolve to an I-4 / Lab 9 container string. Add module lifelines only inside the **one** I-11 container. Correct Lab 5; do not invent a second set of use cases.

**R:** Dev · **A:** SA · **C:** Test, BA

### Input

- Lab 1: I-11 use cases; I-6 states.
- Lab 9 Container names. Optional Component internals for **one** container only.
- Lab 6 gateway / event bus names if they are participants.
- Lab 5 sequences / activity / state (before pack).
- Lab 2 CON.* for exception branches.
- Lab 7 header + RACI template.

### Output

| Artifact | Rules |
|----------|--------|
| Audited sequence per named use case | The Lab 5 sequence, corrected: participants ⊆ I-4 / Lab 9 (+ actors). Component modules only inside the one selected container |
| State (only if Lab 5 did not produce it) | One object; I-6 states |
| Participant = SUT map | Each lifeline → I-4 string |
| Coverage note | G6: every `alt` and every state transition listed |
| Comparison note | Lab 5 (messy) vs this sitting (Guide): names, mixed language, missing header/RACI |

**Done when:** every named use case has a sequence; participants match Lab 9; G6 note complete; header + RACI; comparison note written.  
**Fail if:** “all components”; Lab 5 skipped; Lab 7 skipped; Component details of a container that was not selected; before pack deleted.

---

# Guide

**Open this section in Lab 7, not during Labs 1–6.** Using it early skips the messy-then-standardized journey.

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

**R ≠ A.** Every artifact needs a second pair of eyes, so the person who draws never signs their own view. One person may hold two roles, but not both seats on the same artifact — split the artifact or pass **A** to EA.

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
| C4 Container | A | R | I | C | C | C | I | C | I |
| C4 Component | I | A | I | C | C | R | C | I | I |
| Application Cooperation | A | R | I | C | C | C | I | I | I |
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
As-Is | To-Be | Transition:  ______ (circle one; this pack = To-Be
            unless the view is the Lab 2 as-is analysis)
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

### Doubling up in a group of three

Four seats, three people. Pair them like this:

| Group size | Works | Avoid | Why |
|------------|-------|-------|-----|
| 4 | one seat each | — | cleanest |
| 3 | **EA + BA** (both upstream, same readers) or **Dev + Test** (both delivery grain) | **SA + Dev** | SA approves the C4 Component that Dev draws — one person would sign their own work |
| 3 | Owner played by the facilitator | Owner played by a team member who also draws | Owner is **A** on Motivation, Process, and Context |

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

**Object:** _______________ (one business / data object — not a container)

| State | Trigger / event | Next state | Terminal? |
|-------|-----------------|------------|-----------|
| | | | |
| | | | |
| | | | |

**Terminal states** (list them; every machine needs at least one):

-

Use these exact state strings on the UML State machine (Lab 5 / Lab 10) and in the Lab 3 test spec.

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

### Lab wording

| Short | Means |
|-------|--------|
| **Bound form** | The deliverable shape is fixed. Produce exactly the listed artifacts — no substitutes, no extras, no free-form slide deck instead |
| **Before pack** | Labs 1, 2 (before), 8, 9, 6, 5, 10 as first drawn, in your current style. Archived unchanged |
| **After pack** | The same views from Lab 4, restyled to the Guide |
| **Sitting** | One lab worked start-to-finish. Finish Done-when before opening the next |

### Other

| Short | Means |
|-------|--------|
| **AuthN** | Authentication |
| **IAM** | Identity and access management product (do not add as a new system if AuthN is already on the gateway) |
| **UAT** | User acceptance test |
| **MVP** | Minimum viable product — not trainee output in these labs |
| **JDBC** | Database protocol — fail if drawn on Motivation / Process |


# Submit checklist

Complete **Lab 1 then 2 then 3 … then 10**. Tick a row only when that lab is Done.

- [ ] **Lab 1:** Input I-1–I-11 complete
- [ ] **Lab 2:** requirements in current language; **no** G1–G6
- [ ] **Lab 3:** build list, to-be Component, to-be sequence, contract register, exception spec, test spec
- [ ] **Lab 4:** messy 1–3 copies kept; cleaned pack + comparison note (Guide not used)
- [ ] **Lab 5:** UML for named use cases; one object per state machine; archived
- [ ] **Lab 6:** ecosystem modeled, not built; **Labs 1–6 archived**
- [ ] **Lab 7:** adoption record + G1–G6 register. Not started before archive
- [ ] **Lab 8:** four named ArchiMate views; header + RACI; G1 / G2
- [ ] **Lab 9:** one Context (no internals) + one Container (sync/async); header + RACI
- [ ] **Lab 10:** Lab 5 UML audited vs C4 names; G6 note; comparison note
- [ ] After views: header + RACI + legend; English; simulated names only
- [ ] No MVP; no Kong / Keycloak / Kafka stand-up
