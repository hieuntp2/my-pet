
# Codex Task Breakdown — Phase 1.9 v3 (Production-Ready)

Version: v1  
Purpose: Production-ready task breakdown for implementing the **Phase 1.9 v3 Advanced Creature Engine** in safe batches using Codex local.  
Scope: Android Pet App only. Offline-first. No Cloud AI. No robot body integration. No architecture rewrite outside the minimum required glue and hardening.

---

## 1. Source of Truth

Read in this order before implementing any batch:

1. `docs/project_sync_status.md`
2. `docs/phase1_completion_guide.md`
3. `docs/project_manifest.md`
4. `docs/development_roadmap.md`
5. `docs/09_pet_app_definition_full.md`
6. `docs/06_personality_engine.md`
7. `docs/07_robot_memory_system.md`
8. `docs/08_audio_interaction_architecture.md`
9. `docs/02_android_pet_brain_architecture.md`
10. `docs/AGENTS.md`
11. `docs/active/phase1_9_pet_sensing_runtime_v3_fixed.md`

If real code and old backlog docs conflict, trust:
1. actual codebase
2. `project_sync_status.md`
3. `phase1_completion_guide.md`
4. the rest of the docs

---

## 2. Global Rules

### 2.1 Non-negotiable implementation rules

Do not:
- add mock production logic
- leave TODOs or empty methods
- add placeholder business logic
- silently redesign unrelated systems
- start Phase 2 conversation work
- start robot body work
- convert the pet into a chatbot or assistant
- move everything into one giant orchestrator file

Must:
- keep build green after each task
- preserve offline-first behavior
- preserve event-driven architecture
- preserve or improve debug visibility
- prefer smallest complete vertical slice
- degrade gracefully when subsystems fail
- keep the home experience product-first, not debug-first

### 2.2 Required output after each task

Codex must report exactly:

- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks

### 2.3 Required build command

After every task:

```bash
./gradlew assembleDebug
```

If touched logic has stable tests:

```bash
./gradlew test
```

---

## 3. Strategic Implementation Order

Implement in this order:

1. Runtime sensing ownership
2. Attention ownership
3. Behavior arbitration
4. Emotional inertia
5. Memory-driven behavior
6. Learning trigger refinement
7. Resilience / global exception safety
8. Performance tuning / degradation
9. Final Phase 1.9 validation

This order is important.

Do **not** start with learning first.  
Without attention ownership and arbitration, learning will feel noisy and incorrect.

---

## 4. Execution Batches

---

# Batch A — Runtime Sensing Ownership

## Goal

Move camera/audio sensing from debug-owned flows into real pet runtime ownership in a safe, resource-aware way.

## Outcome

The pet should be able to sense the environment in normal home/runtime mode without brute-force always-on heavy inference.

---

## A1 — Audit and define runtime sensing ownership boundaries

### Goal
Identify exactly which current camera/audio flows are debug-owned, which are reusable, and where runtime ownership should live.

### What this task changes
- Adds or updates a concise internal design note / code comments / active doc if needed
- Identifies the real composition root and ownership boundaries
- Does not yet change behavior deeply

### Must not do
- no broad refactor
- no new heavy logic
- no UI redesign

### Expected outputs
- clear runtime owner for camera sensing
- clear runtime owner for audio sensing
- clear statement of what remains debug-only
- explicit boundary between passive sensing and debug screens

### Definition of done
- there is a concrete implementation target for runtime sensing ownership
- no ambiguity remains around where sensing should be started/stopped from

### Verification
- inspect current root wiring and note exact runtime integration points
- build passes

---

## A2 — Introduce passive sensing runtime state model

### Goal
Create real domain models for sensing mode ownership.

### Required model concepts
- `SensingMode`:
  - `OFF`
  - `PASSIVE_AWARENESS`
  - `ATTENTION`
  - `FOCUS`
- runtime sensing status:
  - current mode
  - camera cadence / enabled state
  - audio capture mode
  - throttle / degraded flags
  - failure / recovery metadata if useful

### What this task changes
- adds typed models in brain/domain or appropriate runtime layer
- adds safe defaults
- makes models available to app/home/runtime glue

### Must not do
- no full behavior logic yet
- no learning prompts yet

### Definition of done
- runtime sensing state is represented by real models
- models are consumable by runtime glue and debug UI
- build passes

### Verification
- reference the models from app/runtime code
- build passes

---

## A3 — Add runtime sensing coordinator skeleton

### Goal
Create a real runtime coordinator that owns sensing mode transitions.

### Responsibilities
- expose current sensing mode
- allow transitions between passive / attention / focus
- be lifecycle-aware
- remain independent from debug screens

### Must not do
- no brute-force full sensing loop
- no learning logic
- no deep performance tuning yet

### Definition of done
- there is one real coordinator for sensing mode ownership
- app runtime can depend on it
- build passes

### Verification
- coordinator can be instantiated and observed from runtime code
- build passes

---

## A4 — Wire audio passive awareness into runtime

### Goal
Move lightweight audio awareness into the real home/runtime experience.

### Required behavior
- audio energy / VAD-light or equivalent passive signal can run in runtime mode
- debug screen may still inspect it, but no longer owns it
- failures do not crash the app
- runtime can turn it on/off through the sensing coordinator

### Must not do
- no full ASR loop
- no conversation flow
- no permanent high-cost audio stack

### Definition of done
- audio sensing works in runtime mode outside debug ownership
- event flow remains visible
- build passes

### Verification
- open app in normal runtime
- produce sound near the mic
- confirm runtime path receives audio stimulus without entering debug screen
- build passes

---

## A5 — Wire low-cadence camera awareness into runtime

### Goal
Move lightweight camera presence awareness into runtime mode.

### Required behavior
- low-cadence camera sampling or existing lightweight perception can run in runtime mode
- runtime mode should detect high-level presence cues only
- heavy work stays throttled until attention/focus

### Must not do
- no permanent full-rate inference
- no force-opening debug camera screen
- no UI takeover

### Definition of done
- camera awareness exists in runtime mode
- passive runtime camera path is separate from debug ownership
- build passes

### Verification
- open app in normal runtime
- present face/motion in front of device
- confirm runtime path produces presence / face-candidate signals
- build passes

---

## A6 — Add runtime sensing debug visibility

### Goal
Expose runtime sensing mode and ownership cleanly for developers.

### Required visibility
- current sensing mode
- whether audio passive awareness is active
- whether camera passive awareness is active
- recent transition reason

### Definition of done
- developers can inspect runtime sensing state without depending on logs alone
- home experience remains uncluttered for normal users
- build passes

### Verification
- inspect debug panel or developer surface while runtime is active
- build passes

---

# Batch B — Attention Ownership System

## Goal

Make the pet maintain a single dominant focus so it behaves like a creature rather than reacting randomly to all stimuli.

## Outcome

The pet has a real attention target that can strengthen, decay, and be interrupted.

---

## B1 — Define attention ownership domain model

### Goal
Create the models for attention target ownership.

### Required concepts
- `AttentionTargetType`
  - person
  - object
  - sound
  - internal_need
  - none
- `AttentionTarget`
  - target id / label if known
  - source event / source type
  - strength
  - acquiredAt
  - lastUpdatedAt
  - ttl or decay parameters
- `AttentionState`
  - current dominant target
  - previous target if useful
  - interruption info if useful

### Definition of done
- typed models exist
- build passes

### Verification
- models compile and are referenceable from brain/runtime
- build passes

---

## B2 — Implement attention acquisition rules

### Goal
Create the real logic for acquiring focus.

### Required rules
- user interaction strongly overrides existing focus
- person presence should outrank weak sound
- urgent internal needs can acquire attention when appropriate
- very weak / stale signals should not steal focus aggressively

### Must not do
- no random focus stealing
- no giant planner rewrite

### Definition of done
- attention acquisition is deterministic and explainable
- build passes

### Verification
- unit or debug-driven scenarios show expected winner target
- build passes

---

## B3 — Implement attention decay and interruption

### Goal
Make focus persist briefly, then decay rather than disappearing instantly.

### Required behavior
- focus strength decays over time
- higher-priority stimuli can interrupt
- weak duplicate stimuli refresh instead of creating chaos
- stale targets expire cleanly

### Definition of done
- attention feels stable, not jittery
- build passes

### Verification
- observe focus switch scenarios through debug state and runtime behavior
- build passes

---

## B4 — Publish attention-related events and debug surface

### Goal
Make attention ownership observable.

### Required outputs
- attention acquired
- attention refreshed
- attention interrupted
- attention expired

### Definition of done
- event flow and debug surfaces reflect attention changes
- build passes

### Verification
- trigger sound / face / touch
- inspect attention transitions
- build passes

---

## B5 — Connect attention to visible pet behavior

### Goal
The home pet should visibly reflect what it is currently focused on.

### Required behavior
- sound focus → orientation / attentive reaction
- person focus → socially interested reaction
- object focus → curious / looking reaction
- internal need focus → state-consistent self-directed behavior

### Definition of done
- attention is not just stored; it is visible
- build passes

### Verification
- trigger different focus types and observe home/avatar differences
- build passes

---

# Batch C — Behavior Arbitration Engine

## Goal

Resolve competing triggers into one coherent behavior plan.

## Outcome

The pet stops behaving like multiple subsystems fighting each other.

---

## C1 — Audit current behavior signal conflicts

### Goal
Inspect existing signal ownership among:
- greeting flow
- interaction reactions
- idle
- audio stimulus reactions
- emotion/state-driven avatar
- any existing behavior plans

### Definition of done
- the real conflict map is understood
- target arbitration points are identified
- build passes

### Verification
- document or code comments show actual arbitration insertion point
- build passes

---

## C2 — Define behavior arbitration input/output contracts

### Goal
Create explicit models for arbitration.

### Required inputs
- current attention state
- current pet state / conditions
- recent stimuli
- greeting requests
- direct user interactions
- cooldown status
- current sensing mode if relevant

### Required output
- single `BehaviorPlan` or equivalent containing:
  - selected behavior type
  - priority
  - duration / timeout if useful
  - source reason
  - optional avatar/audio hints

### Definition of done
- clear contract exists between state/stimulus and visible behavior
- build passes

### Verification
- models compile and integrate
- build passes

---

## C3 — Implement arbitration priority rules

### Goal
Make one system choose the winning behavior.

### Suggested priority
1. direct user interaction
2. urgent internal need
3. greeting
4. strong attention target
5. sound reaction
6. idle

Adjust only if real code proves a better order is needed.

### Definition of done
- one winning behavior is selected consistently
- build passes

### Verification
- simulate conflicting triggers and inspect chosen behavior
- build passes

---

## C4 — Integrate arbitration into home/avatar signal path

### Goal
Make the chosen behavior plan drive visible runtime output.

### Required behavior
- arbitration output becomes the dominant visible behavior source
- lower-priority signals do not leak through incorrectly
- existing home pet remains expressive, not flattened

### Definition of done
- visible behavior is cleaner and more intentional
- build passes

### Verification
- test tap during idle, greeting during sound, sound during existing attention, etc.
- build passes

---

## C5 — Add arbitration debug visibility

### Goal
Expose which behavior won and why.

### Required visibility
- current behavior plan
- winning priority
- source reason
- suppressed competing inputs if practical

### Definition of done
- developers can explain current behavior without digging only through logs
- build passes

### Verification
- inspect current winning behavior through debug surface
- build passes

---

# Batch D — Emotional Inertia Engine

## Goal

Make emotion transitions accumulate and decay instead of flipping instantly.

## Outcome

The pet feels emotionally continuous rather than mechanical.

---

## D1 — Define emotional inertia state model

### Goal
Create a model for emotional momentum / carry-over.

### Required concepts
- base resolved emotion
- emotional momentum / inertia
- recent reinforcement from repeated stimuli
- decay timing

### Definition of done
- emotional inertia has explicit domain representation
- build passes

### Verification
- models compile and integrate
- build passes

---

## D2 — Implement inertia update rules

### Goal
Emotion should build up gradually with repeated reinforcing stimuli.

### Required behavior
- repeated positive interaction builds happiness/excitement gradually
- repeated neglect or low-energy conditions drift toward neutral/sleepy/sad states appropriately
- inertia decays over time rather than disappearing immediately

### Must not do
- no opaque randomness
- no giant emotion rewrite disconnected from current pet state

### Definition of done
- emotional momentum updates based on real stimuli and time
- build passes

### Verification
- repeated interaction scenarios produce gradual emotional build-up
- build passes

---

## D3 — Integrate inertia into visible emotion resolution

### Goal
Visible emotion must incorporate inertia instead of only base state.

### Required behavior
- resolved visible emotion reflects both current state and emotional carry-over
- abrupt oscillation is reduced
- emotional continuity improves

### Definition of done
- emotion transitions feel smoother
- build passes

### Verification
- observe repeated taps, repeated play, long idle, return after absence
- build passes

---

## D4 — Add inertia debug visibility

### Goal
Developers can inspect emotional momentum.

### Required visibility
- base emotion
- momentum / inertia contribution
- final visible emotion
- recent drivers if practical

### Definition of done
- emotional continuity is debuggable
- build passes

### Verification
- inspect emotional values under repeated interaction
- build passes

---

# Batch E — Memory-Driven Behavior

## Goal

Make memory change what the pet does, not just what it stores.

## Outcome

Known people, known objects, and prior interaction history influence current behavior.

---

## E1 — Audit usable memory signals already in code

### Goal
Find the strongest existing memory signals that can affect behavior now.

### Candidate signals
- recognized person familiarity
- bond state
- repeated object exposure
- visit frequency / habit profile
- recent positive/negative care actions
- last meaningful interaction

### Definition of done
- a minimal, high-value set of memory signals is selected for behavior use
- build passes

### Verification
- exact chosen signals are documented in code or active doc
- build passes

---

## E2 — Map memory signals to behavior modifiers

### Goal
Create a real mapping from memory to current behavior.

### Required examples
- known person → warmer greeting / lower social hesitation
- unfamiliar repeated person → curious, not instantly familiar
- repeated known object → reduced curiosity
- long absence + high bond → stronger reunion reaction

### Definition of done
- behavior uses memory in an explainable way
- build passes

### Verification
- simulate memory conditions and inspect selected behavior differences
- build passes

---

## E3 — Integrate memory signals into greeting and social reactions

### Goal
Make memory matter immediately in the most visible flows.

### Required behavior
- greeting varies by prior relationship and absence
- social response intensity changes for known vs unknown people
- known objects feel less novel than new ones

### Definition of done
- users can feel continuity over time
- build passes

### Verification
- compare known-person and unknown-person reactions
- compare known-object and unknown-object reactions
- build passes

---

## E4 — Add memory influence debug visibility

### Goal
Show which memories affected the current behavior.

### Required visibility
- current memory modifiers in effect
- simple explanation of why greeting / reaction was stronger or weaker

### Definition of done
- memory-driven behavior is inspectable
- build passes

### Verification
- debug view shows memory modifiers during greeting / recognition
- build passes

---

# Batch F — Learning Trigger Refinement

## Goal

Make person/object learning feel contextual, not intrusive.

## Outcome

The pet learns only when repeated exposure and engagement justify it.

---

## F1 — Define person familiarity accumulation rules

### Goal
Use repeated unknown-person exposure to build familiarity before asking for name.

### Required inputs
- repeated unknown observations
- seen count
- timing / recency
- session spacing if available
- current engagement / interaction context

### Definition of done
- familiarity score for unknown repeated person is calculated and persisted or derived safely
- build passes

### Verification
- repeated unknown face exposure increases familiarity
- build passes

---

## F2 — Implement contextual trigger for asking a person's name

### Goal
Pet should ask only when the context makes sense.

### Required trigger conditions
- familiarity above threshold
- user currently engaged or attention locked strongly on person
- no recent repeated ask spam
- pet not in urgent conflicting state

### Must not do
- do not ask immediately on first unknown face
- do not ask repeatedly every session

### Definition of done
- name prompt is contextual and rate-limited
- build passes

### Verification
- simulate repeated unknown person presence
- confirm the ask appears only after threshold and context conditions
- build passes

---

## F3 — Define object curiosity accumulation rules

### Goal
Repeated unknown objects should build curiosity before prompting.

### Required inputs
- repeated unknown object exposure
- contextual engagement
- whether the object is already known
- cooldowns / anti-spam

### Definition of done
- object curiosity score or equivalent exists
- build passes

### Verification
- repeated object exposure increases curiosity appropriately
- build passes

---

## F4 — Implement contextual trigger for asking object label

### Goal
Object learning prompt should feel pet-like and non-annoying.

### Required conditions
- curiosity high enough
- object still unknown
- user engaged / focus context valid
- no recent prompt spam
- no stronger current behavior

### Definition of done
- object prompt is contextual and rate-limited
- build passes

### Verification
- repeated unknown object + engagement leads to prompt
- build passes

---

## F5 — Integrate learning prompts with arbitration and attention

### Goal
Learning prompts must behave as first-class behaviors, not popup noise.

### Required behavior
- learning prompts respect current priority model
- prompts should acquire or use attention ownership
- prompts should be suppressed when stronger behavior is active

### Definition of done
- learning feels intentional and correctly timed
- build passes

### Verification
- test learning under idle, under sound reaction, under direct user interaction
- build passes

---

# Batch G — Resilience, Exception Safety, and No-Crash Runtime

## Goal

Prevent app crashes while keeping failures visible and recoverable.

## Outcome

Subsystem failures degrade gracefully instead of killing the app.

---

## G1 — Audit crash-prone runtime boundaries

### Goal
Identify the highest-risk runtime boundaries.

### Likely candidates
- camera pipeline
- audio capture pipeline
- ML inference calls
- coroutine scopes in runtime orchestration
- avatar signal mapping
- memory writes on hot paths
- prompt dialogs / overlays triggered from runtime

### Definition of done
- crash-prone boundaries are identified and prioritized
- build passes

### Verification
- audit recorded in code/doc comments or active doc
- build passes

---

## G2 — Add local subsystem failure isolation

### Goal
Each critical subsystem should fail locally and degrade gracefully.

### Required behavior
- camera failures do not kill the app
- audio failures do not kill the app
- inference failures do not crash the runtime loop
- memory write failures do not take down the home experience

### Definition of done
- critical subsystem boundaries are wrapped safely
- build passes

### Verification
- inspect error handling in critical runtime paths
- build passes

---

## G3 — Add or harden coroutine exception policy

### Goal
Unhandled coroutine failures should not tear down the user experience.

### Required behavior
- use `SupervisorJob` where appropriate
- apply a real `CoroutineExceptionHandler`
- surface failure state or recovery when useful
- avoid silent black holes

### Definition of done
- coroutine runtime is failure-tolerant
- build passes

### Verification
- simulated coroutine failure does not crash app
- build passes

---

## G4 — Add or harden global uncaught exception handling

### Goal
Unhandled exceptions must not hard-crash the app if recoverable handling is possible.

### Required behavior
- log exception
- surface developer-visible error state / overlay / crash screen for debug
- keep app in safe degraded mode when possible
- do not silently swallow without trace

### Important note
Do this honestly. Some truly fatal platform-level crashes may still terminate process boundaries. The goal is maximum graceful recovery, not dishonest silent failure.

### Definition of done
- uncaught runtime exceptions are captured and surfaced responsibly
- build passes

### Verification
- simulate throw path where practical
- confirm app does not simply vanish without signal
- build passes

---

## G5 — Add developer-visible runtime error surface

### Goal
Make runtime failures diagnosable without reading logs only.

### Required visibility
- last subsystem error
- degraded subsystem flags
- recovery state if any

### Definition of done
- developers can inspect runtime failure state in-app
- build passes

### Verification
- trigger handled runtime failure and inspect debug surface
- build passes

---

# Batch H — Performance, Cadence, and Degradation

## Goal

Keep sensing and behavior believable without melting the device.

## Outcome

Adaptive cadence and degradation rules protect performance and battery.

---

## H1 — Define sensing cadence policy

### Goal
Formalize what runs in passive vs attention vs focus modes.

### Required policy
- passive: minimal camera cadence + lightweight audio awareness
- attention: increased camera cadence + face detection + selective object checks
- focus: short-lived higher-cost analysis only when justified

### Definition of done
- cadence policy is explicit in code and runtime behavior
- build passes

### Verification
- inspect runtime mode transitions and active subsystems
- build passes

---

## H2 — Implement throttling and time budgets

### Goal
Prevent continuous heavy inference.

### Required behavior
- throttle expensive face embedding
- throttle object detection appropriately
- rate-limit repetitive event emission
- avoid duplicate work under stable conditions

### Definition of done
- heavy inference is bounded
- build passes

### Verification
- inspect logs/debug state under continuous presence
- build passes

---

## H3 — Implement degradation rules under stress

### Goal
Reduce runtime load when necessary.

### Suggested conditions
- repeated inference failures
- thermal / CPU pressure if accessible
- battery constraints if already available in app context
- camera or audio subsystem instability

### Required behavior
- drop from focus → attention → passive when needed
- emit or expose degraded state
- recover when safe

### Definition of done
- runtime can degrade and recover intentionally
- build passes

### Verification
- simulate degraded mode and inspect recovery path
- build passes

---

## H4 — Add performance/debug telemetry

### Goal
Developers can see cadence and degraded state clearly.

### Required visibility
- current sensing mode
- active inference cadence
- degraded flags
- recent downgrade reason

### Definition of done
- performance behavior is inspectable in-app
- build passes

### Verification
- inspect telemetry while switching modes
- build passes

---

# Batch I — Final Validation and Readiness

## Goal

Validate that Phase 1.9 meaningfully improves creature-like behavior without destabilizing the product.

## Outcome

A truthful readiness report and next-step recommendation exist.

---

## I1 — Run experiential validation against core criteria

### Goal
Evaluate the new runtime against creature-quality criteria.

### Required checks
1. pet senses environment outside debug mode
2. pet maintains a dominant focus
3. visible behavior is cleaner and less conflicting
4. emotional transitions are smoother
5. memory changes behavior
6. person/object learning triggers are contextual
7. failures degrade safely
8. runtime is explainable through debug visibility

### Definition of done
- validation is recorded honestly
- build passes

### Verification
- run through real app-path scenarios
- build passes

---

## I2 — Update active docs with post-implementation reality

### Goal
Record what is truly finished and what remains.

### Required outputs
- concise active sync note or completion note
- list of non-blocking remaining risks
- clear recommendation:
  - continue to Phase 2
  - or one more small stabilization pass

### Definition of done
- docs reflect reality, not optimism
- build passes

### Verification
- doc exists and matches implemented state
- build passes

---

## I3 — Final recommendation gate

### Goal
Decide whether the pet now feels alive enough without conversation.

### Gate question
Can the pet feel intentional, emotionally continuous, and socially reactive **without** cloud conversation?

If no:
- do not move to Phase 2 yet
- identify the smallest remaining pass

If yes:
- recommend moving to Phase 2 conversation planning

### Definition of done
- final recommendation is honest and specific

---

## 5. Suggested Prompt Wrapper for Each Task

Use this wrapper when sending a single task to Codex local:

```md
Read and follow `docs/AGENTS.md` first.

Then read only:
- docs/project_sync_status.md
- docs/phase1_completion_guide.md
- docs/09_pet_app_definition_full.md
- docs/active/phase1_9_pet_sensing_runtime_v3_fixed.md
- this task breakdown file
- any code directly related to this task

Implement only this task.

Do not:
- add mock production logic
- leave TODOs or empty methods
- expand into adjacent tasks
- break existing verified flows

Must:
- keep build green
- preserve offline-first behavior
- preserve event-driven architecture
- keep or improve debug visibility
- report exactly:
  - Summary of changes
  - Files changed
  - How to verify
  - Build result
  - Remaining risks

Run:
- ./gradlew assembleDebug

If touched tests are stable, also run:
- ./gradlew test
```

---

## 6. Highest-Leverage Starting Point

Start here:

1. A2 — passive sensing runtime state model
2. A3 — runtime sensing coordinator skeleton
3. B1 — attention ownership domain model
4. C2 — behavior arbitration contracts

Reason:
These tasks create the backbone needed for the rest of the system without prematurely locking the wrong implementation.

---

## 7. Definition of Success for the Entire v3 Breakdown

This breakdown succeeds when:

- sensing is runtime-owned, not debug-owned
- the pet has one dominant focus at a time
- behavior conflicts are centrally resolved
- emotional continuity exists
- memory changes current behavior
- person/object learning is contextual
- failures degrade instead of crashing
- performance is intentionally managed
- developers can explain what the pet is doing and why

If these are not true, Phase 1.9 is not complete.
