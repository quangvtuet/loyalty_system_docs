# Capstone — Implement what you designed

Independent project after Lab 10 Done. **Not** a modeling lab. **Not** Day-3 SME Lab 11. I-11 only. **R** Dev · **A** SA · **C** Test. Labs 1–10 unchanged. Implementation lives **outside** the before and after packs.

**Where:** a sibling folder or repo next to the modeling packs. Never inside the before pack, after pack, or Lab 7 file.

**Files that count:** runnable I-11; OpenAPI; automated tests; name-identity map; spec-trace; mock list for I-3.

**Collapse allowed:** one process / in-memory store / in-process bus **if** modules keep Lab 1 / I-4 strings and the name map says so. Product names stay labels. A cluster is not output.

**Demo (10 min):** I-1 goal → one I-11 sequence on screen → live happy path → live named `alt` / CON.* → test report.

| | |
|---|---|
| **Input** | Lab 1 I-1…I-11; Lab 3 register / exception / test spec; Lab 7 G1–G6; Lab 8–10 names, sequences, SUT map, G6 |
| **Output** | Runtime of every I-11 use case (happy path + the `alt` named in I-11); name-identity map (code module / package / process → I-4; collapse rows if used); OpenAPI of in-scope Lab 3 operations (served or file; status codes match runtime); automated tests (those I-11 paths; I-5 / I-9 as negative tests on this slice; in-scope G6); spec-trace (each in-scope path → OpenAPI operation → test id); I-3 mocked (stub / fake); G5 (each I-11 `alt` runs the Lab 3 exception spec: trigger + compensating action) |
| **Done when** | All Output rows present; G1–G3 still hold on the after pack; G4–G6 pass on the runtime |
| **Fail if** | Lab 10 not Done; forked or invented names; missing I-11 use case or named `alt`; real I-3 or production credentials; OpenAPI missing or drifted from runtime; extra I-4 / I-6 / product as a new identity; extra deployable unit that is not a documented collapse; I-5 / I-9 violation possible (no test that attempts it); I-7 ownership shared or moved; domain rules outside I-7 owner; code not on the spec-trace; implementation inside modeling packs |

## Requirements

**Source of truth.** This team’s Lab 1 Input and after pack (Labs 8–10), plus Lab 3 register / exception / test spec and Lab 7 G1–G6. Not another team’s topic, not the Day-3 SME brief, not a chat. If Lab 1 and the after pack disagree, stop; SA fixes the pack, then Dev codes.

**I-11 only.** Runtime of **every** I-11 use case: the happy path and the `alt` named in I-11. Drill internals only in the one I-11 container. I-1 “in scope” items that are not I-11 use cases are out — list them N/A, do not build them.

**In-scope.** Operations, I-6 transitions, and sequence `alt`s that those I-11 use cases need. Other Lab 3 / G6 rows are N/A on the spec-trace: not extra code, and not silently omitted from an I-11 path. Fail if an I-11 `alt` has no operation or no test, or if an N/A row is implemented as a new use case.

**Simulated names.** Use Lab 1 strings only. No real customer data, no production system names, no production credentials. I-3 is mocked (stub or in-process fake). Fail if the runtime calls a real host or embeds a live secret.

**G4 form is OpenAPI.** Every in-scope Lab 3 contract row is an OpenAPI operation. That document is the public contract of this sitting (served or committed file). Fail if G4 is a slide, a wiki table, or a code comment instead of OpenAPI.

**G5 on named alts.** Each I-11 `alt` runs the Lab 3 exception spec: trigger, compensating action, who performs it. A named error status without the compensating action does not pass G5. Fail if the `alt` only returns 4xx and the I-6 state / neighbour mock is unchanged when the spec required compensate.

**G6 executes.** In-scope G6 rows are automated tests that **run**, not a checklist. SUT names = I-4 / Lab 9; use the Lab 10 participant = SUT map. Fail if coverage is “planned” only, or a test SUT is not an I-4 name.

**I-5 and I-9 on this slice.** The I-11 happy path cannot skip a named I-5 hard rule. A test **attempts** the I-9 forbidden call; the runtime rejects it (assert on mock or rejection). Do not build the rest of the landscape to prove I-5 or I-9.

**G1–G3 still hold.** Goal, outcome, and CON.* (G1), process and I-6 states (G2), and C4 names / externals / sync-async (G3) remain true of the runtime. This sitting does not reopen or restyle Labs 1–10. Fail if the running system needs a new external, a renamed container, or a state that is not in the after pack.

## Standards

**Clean code.** Use Lab 1 strings in code, OpenAPI, tests, and the name map — one spelling per thing. One reason to change per I-4 container or Lab 3 module; a helper is not a new I-4. Every route, handler, and package appears on the spec-trace. Fail if a name is forked, a utility is promoted to a container, or an out-of-scope path is callable.

**OpenAPI.** This sitting’s G4. Public contract = in-scope Lab 3 register operations only. Each named `alt` / CON.* has an error in the document and the same status and body at runtime. A served spec or a committed file is enough if it matches the running API. Fail if OpenAPI is missing, lists extra operations, or has drifted (documented 400, runtime 500 with a new error name).

**OOP.** The I-6 named object is a type (class, record, or aggregate). State transitions are operations on that type, not a bag of scripts. Lab 3 Component modules are collaborators of that type, not extra I-4 boxes. Fail if states live only as unconstrained strings, or a module is given a new container identity.

**Microservices.** Each I-4 C4 Container is independently deployable unless the name map documents collapse (one process, in-memory store, in-process bus). Coupling is only Lab 9 relationships and I-8 patterns, with sync vs async as labeled. Fail if a new deployable appears without a collapse row, or containers call each other on a path not on Lab 9 / I-8.

**Cloud native.** Runtime processes follow I-9 (if collapsed, the map says which I-9 location that process stands for). I-3 are mocked backing services. Config and secrets are not in source. A cluster is not output — product names (Kong, Kafka, Keycloak, …) stay labels. Fail if production credentials, a real I-3 host, or a product stand-up appear.

**Domain driven.** Ubiquitous language = Lab 1 index. Bounded context = I-1 system-in-focus. CON.* and I-5 invariants are enforced in the I-7 source-of-truth owner, not copied into every adapter. Fail if two modules both write the same I-7 object, ownership is moved, or a rule exists only in the gateway or UI.

**AI spec driven.** The spec is Labs 1–10 + OpenAPI + G6, not a chat transcript. Human or generated code must sit on the spec-trace (in-scope path → OpenAPI operation → test id). Human **A** (SA) accepts the runtime. Fail if generated code adds a use case, name, or operation that is not in the after pack, or if there is no spec-trace row.

## Principles

**Models win.** The after pack and Lab 1 index are the spec. If code and a diagram disagree, change the code, or ask SA to change the after pack first, then retie the spec-trace. Do not “fix” a name only in the runtime. Generated code has no authority over a named view.

**Do not invent.** No extra I-4, I-6 state, I-11 use case, actor, external, or product. If an Input cell is missing, invent a plausible simulated value, mark `ASSUMPTION` on the name map, and use that one string everywhere. Collapse maps an existing I-4 onto a module or process; it does not create a new container identity. Connecting a live I-3 invents an external the spec did not give you.

**Hard rules impossible.** I-5 hard rules, the I-9 forbidden path, and CON.* are not comments or README warnings. A test must **attempt** the violation; the runtime must reject it (assert on mock, status, or unchanged I-6 state). If a client can skip a named hard rule, write the forbidden path, or bypass the I-7 owner, the sitting fails even if the happy path demos.

**Before pack is archive.** Labs 1–6 stay as first drawn. Do not restyle them to match the code. Do not put implementation, OpenAPI, or tests inside them. The after pack (Labs 8–10) plus this runtime are the to-be; the before pack is evidence of the messy journey only.

**One sitting, one slice.** Scope is I-11 only. Lab 3 / G6 rows that those use cases do not need are N/A on the spec-trace — not a backlog to code. Do not build the rest of the landscape to “complete” I-5 or I-9; prove them with negative tests on this slice.

**Human A accepts.** SA is **A**. Dev is **R**. Test is **C**. If the model is wrong, SA updates the after pack, then Dev traces again. A passing demo does not rewrite Lab 1 names. AI-generated code is accepted only when it traces to OpenAPI + G6 and SA signs.
