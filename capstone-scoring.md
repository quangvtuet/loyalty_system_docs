# Capstone scoring — independent team project

Instructor only. Standard: [capstone.md](../capstone.md). Not Day-3 SME Lab 11. Do not invent names; copy I-11 strings from **this team’s** Lab 1.

**Group:** _______________  
**Scorer:** _______________  
**Date:** _______________  
**Lab 10 Done?** Y / N  
**Runtime location** (sibling of modeling packs?): _______________  
**RACI:** Dev **R** _______________ · SA **A** _______________ · Test **C** _______________

## How to score

1. Confirm Lab 10 Done. If N, stop: sitting not Done.
2. Tick **Automatic fail**. Any tick = sitting not Done. Still fill comments. Total = 0 / not scored.
3. If no automatic fail: mark each line **Pass** / **Partial** / **Fail** + comment. Roll each lens to **0–10**.
4. Weighted total uses the four lenses only. Principles and demo are evidence, not extra percent.
5. Same artifact may appear under two lenses; it is **not** scored twice in the total.

| Mark | Means |
|------|--------|
| **Pass** | Meets the named fail clause in `capstone.md` |
| **Partial** | Artifact exists; named fail still possible |
| **Fail** | Missing or contradicts the brief |

Lens 0–10 = completeness of that lens’s lines, not a second rubric.

---

## Automatic fail

Any tick → sitting not Done. Total 0 / not scored.

| Tick | Fail if |
|------|---------|
| [ ] | Lab 10 not Done |
| [ ] | Forked or invented names |
| [ ] | Missing I-11 use case or named `alt` |
| [ ] | Real I-3 or production credentials |
| [ ] | OpenAPI missing or drifted from runtime |
| [ ] | Extra I-4 / I-6 / product as a new identity |
| [ ] | Extra deployable unit that is not a documented collapse |
| [ ] | I-5 / I-9 violation possible (no test that attempts it) |
| [ ] | I-7 ownership shared or moved |
| [ ] | Domain rules outside I-7 owner |
| [ ] | Code not on the spec-trace |
| [ ] | Implementation inside modeling packs (before pack, after pack, or Lab 7 file) |

**Comments:**

---

## Bound form / pack contents

| Tick | File that counts |
|------|------------------|
| [ ] | Runnable I-11 (sibling folder/repo; not inside packs) |
| [ ] | OpenAPI (served or committed file) |
| [ ] | Automated tests |
| [ ] | Name-identity map (module / package / process → I-4; collapse rows if used) |
| [ ] | Spec-trace (in-scope path → OpenAPI operation → test id) |
| [ ] | I-3 mock list |
| [ ] | Collapse mapped if one process / in-memory store / in-process bus |

---

## Architecture — 25%

Does the runtime still realize the after pack (C4 / I-4 / I-8 / I-9), not a new landscape?

| # | Check | P / Ptl / F | Comment |
|---|--------|-------------|--------|
| A1 | G1 still holds: goal, outcome, CON.* true of the runtime | | |
| A2 | G2 still holds: process and I-6 states match the after pack | | |
| A3 | G3 still holds: C4 names, I-3 externals, sync/async as labeled | | |
| A4 | I-4 independently deployable **or** collapse documented (module → I-4) | | |
| A5 | Coupling = Lab 9 and I-8 only; no extra I-4 / product as a new identity | | |
| A6 | No extra deployable without a collapse row | | |
| A7 | Processes follow I-9 (map says which location a collapsed process stands for) | | |
| A8 | Internals only in the one I-11 container; neighbours black boxes | | |
| A9 | **Microservices:** Lab 9 / I-8 coupling; collapse mapped, not a second landscape | | |
| A10 | **Cloud native:** I-3 mocked backing services; cluster not output; product names are labels | | |

**Architecture score (0–10):** _____  
**Comments:**

---

## Design — 25%

Does the sitting specify and trace the I-11 slice (Lab 3 / Lab 10 / OpenAPI / spec-trace)?

Copy use-case names from this team’s Lab 1 I-11. Add rows if needed.

| Use case (Lab 1 string) | Named `alt` (Lab 1 string) | Happy in runtime P/Ptl/F | `alt` in runtime P/Ptl/F | G5 specified (trigger + compensate + who) P/Ptl/F | Comment |
|-------------------------|----------------------------|--------------------------|--------------------------|---------------------------------------------------|--------|
| | | | | | |
| | | | | | |
| | | | | | |
| | | | | | |

I-1 “in scope” that is not I-11 listed N/A (not built): _______________

| # | Check | P / Ptl / F | Comment |
|---|--------|-------------|--------|
| D1 | Every I-11 use case has happy path + named `alt` (design-to-runtime) | | |
| D2 | **G4 OpenAPI:** every in-scope Lab 3 contract row is an operation (served or file); not a slide | | |
| D3 | OpenAPI `alt` / CON.* errors match runtime status and body | | |
| D4 | Spec-trace: each in-scope path → OpenAPI operation → test id | | |
| D5 | Other Lab 3 / G6 rows are N/A — not extra use cases, not silently dropped from an I-11 path | | |
| D6 | **OOP:** I-6 object is a type; transitions are its operations; Lab 3 modules are collaborators, not extra I-4 | | |
| D7 | **Domain driven:** Lab 1 language; I-1 bounded context; CON.* / I-5 in the I-7 owner | | |
| D8 | G5 specified for each named `alt` (Lab 3 exception spec). Runtime proof is Test coverage | | |
| D9 | **AI spec driven:** spec is Labs 1–10 + OpenAPI + G6, not a chat; human **A** accepts | | |

**Design score (0–10):** _____  
**Comments:**

---

## Code quality — 20%

Is the implementation clean, named, and bounded to the spec-trace?

| # | Check | P / Ptl / F | Comment |
|---|--------|-------------|--------|
| C1 | **Clean code:** Lab 1 strings in code, OpenAPI, tests, and name map — one spelling | | |
| C2 | Name-identity map complete (including collapse / `ASSUMPTION` rows) | | |
| C3 | One reason to change per I-4 / Lab 3 module; helper is not a new I-4 | | |
| C4 | No out-of-scope path is callable | | |
| C5 | Config/secrets not in source; no production credentials; no real I-3 host | | |
| C6 | Implementation **outside** modeling packs; before pack not restyled to match code | | |
| C7 | Human or generated code sits on the spec-trace; no invented use case, name, or operation | | |
| C8 | SA **A** signed the runtime | | |

**Code quality score (0–10):** _____  
**Comments:**

---

## Test coverage — 30%

Do automated tests **execute** in-scope G6, I-11 paths, and hard rules? Copy the same I-11 names as Design.

| Use case (Lab 1 string) | Happy-path test id | `alt` test id | G5 compensate actually happens (not 4xx alone) P/Ptl/F | SUT = I-4 / Lab 9 P/Ptl/F | Comment |
|-------------------------|--------------------|---------------|------------------------------------------------------|---------------------------|--------|
| | | | | | |
| | | | | | |
| | | | | | |
| | | | | | |

| # | Check | P / Ptl / F | Comment |
|---|--------|-------------|--------|
| T1 | In-scope G6 rows **run** (not a checklist) | | |
| T2 | SUT names = I-4 / Lab 9; Lab 10 participant = SUT map used | | |
| T3 | I-5 hard rule cannot be skipped on the I-11 happy path (test attempts skip) | | |
| T4 | I-9 forbidden call attempted and rejected (assert on mock or rejection) | | |
| T5 | CON.* / named `alt` errors asserted against OpenAPI status/body | | |
| T6 | I-3 mocked in tests (no live host) | | |

**Test coverage score (0–10):** _____  
**Comments:**

---

## Principles — evidence only

Not a fifth weight. Use to justify automatic fail or a lens score.

| Principle | P / Ptl / F | Feeds lens | Comment |
|-----------|-------------|------------|--------|
| Models win (code vs after pack; generated code has no authority) | | Design / Code | |
| Do not invent (no extra I-4 / I-6 / I-11 / product; `ASSUMPTION` one string; live I-3 invents an external) | | Architecture / Code | |
| Hard rules impossible (test attempts I-5 / I-9 / CON.*) | | Test coverage | |
| Before pack is archive (Labs 1–6 unchanged; no code inside) | | Code quality | |
| One sitting, one slice (I-11 only; N/A not a backlog) | | Design | |
| Human A accepts (SA signs; demo does not rewrite Lab 1) | | Design / Code | |

---

## Demo (10 min) — comment only

Order: I-1 goal → one I-11 sequence on screen → live happy path → live named `alt` / CON.* → test report.

Feeds Design and Test coverage comments. Not a fifth weight.

| Tick | Observed |
|------|----------|
| [ ] | I-1 goal stated |
| [ ] | One I-11 sequence on screen (Lab 10 names) |
| [ ] | Live happy path |
| [ ] | Live named `alt` / CON.* |
| [ ] | Test report shown |

**Notes:**

---

## Done when

| Tick | Rule |
|------|------|
| [ ] | All Output rows present |
| [ ] | G1–G3 still hold on the after pack |
| [ ] | G4–G6 pass **on the runtime** |

---

## Weighted total

If any automatic fail is ticked: **Total = 0 / not scored**. Do not add a parallel I-11 / name-map / standards total.

| Lens | Weight | Score (0–10) | Weighted (weight × score) | Comments |
|------|--------|--------------|---------------------------|----------|
| Architecture | 25% | | | |
| Design | 25% | | | |
| Code quality | 20% | | | |
| Test coverage | 30% | | | |
| **Total** | **100%** | | **/ 10** | |

Weighted = weight × score (e.g. Architecture 8 → 0.25 × 8 = 2.00). Sum the four weighted values for **Total / 10**.

Test coverage is heaviest because this sitting is where G4–G6 must pass on the runtime. Architecture and design still gate via automatic fail before this total applies.
