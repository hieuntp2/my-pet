# AI PET — PHASE NEXT: EVOLUTION SYSTEM TASK BREAKDOWN

Version: v1  
Status: Execution backlog for AI coding agents  
Scope: Production-safe implementation tasks for the post-core-life evolution phase  
Execution rule: one task at a time, build-safe, verifiable, no production mocks

---

# 1. How to use this backlog

This file is designed to be used directly with Claude, Codex, or another coding agent.

The agent must:
- read only the required docs for the current task
- implement one task at a time
- keep the build green
- avoid scope creep
- report verification and remaining risks after every task

This backlog assumes the foundational pet systems from the prior phase already exist or are being completed.

---

# 2. Decision hierarchy

Follow this priority order if there is any conflict:

1. `docs/project_manifest.md`
2. `docs/development_roadmap.md`
3. `docs/09_pet_app_definition_full.md`
4. `pet_phase_next_evolution_system.md`
5. `docs/07_robot_memory_system.md`
6. `docs/06_personality_engine.md`
7. `docs/08_audio_interaction_architecture.md`

---

# 3. Global rules for every task

A task is DONE only when:
- the app builds successfully
- the new behavior is visible, persisted, or otherwise verifiable
- there is no mock production logic replacing required behavior
- no unrelated refactor is introduced
- debug visibility exists where the task changes runtime behavior

Required output after each task:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks

Required build command unless the task states otherwise:

```bash
./gradlew assembleDebug
```

---

# 4. Batch NE1 — Episodic memory foundation

## NE1-01 — Define MemoryEpisode model and Room entity

### Goal
Introduce a persistent episode model that can represent grouped interaction sessions rather than raw individual events only.

### Read first
- `pet_phase_next_evolution_system.md`
- `docs/07_robot_memory_system.md`
- `docs/09_pet_app_definition_full.md`

### Scope
- Define the domain model for MemoryEpisode
- Add Room entity + DAO
- Do not implement episode generation logic yet

### Files likely touched
- memory episode model files
- Room database files
- DAO registration files

### Definition of Done
- episode model compiles
- Room schema compiles and initializes
- DAO can insert and query episodes

### How to verify
- create a temporary debug insertion path or repository call
- confirm the app builds and the table is reachable

---

## NE1-02 — Implement EpisodeRepository and mapping layer

### Goal
Create a repository abstraction for loading and saving episodes without coupling the app to raw DAO usage.

### Read first
- `pet_phase_next_evolution_system.md`
- `docs/07_robot_memory_system.md`

### Scope
- repository only
- no session grouping logic yet

### Definition of Done
- repository can save and query episodes
- domain/entity mapping is complete
- build succeeds

---

## NE1-03 — Define session boundary rules and episode candidate model

### Goal
Create the logic contract for grouping runtime events into interaction sessions.

### Read first
- `pet_phase_next_evolution_system.md`
- `docs/09_pet_app_definition_full.md`

### Scope
- define rules and candidate state model
- do not yet finalize summary text generation

### Definition of Done
- session boundary logic is represented in code in a debuggable way
- build succeeds

---

## NE1-04 — Implement runtime episode grouping from event stream

### Goal
Group raw pet interaction events into episode candidates during or after sessions.

### Read first
- `pet_phase_next_evolution_system.md`
- `docs/07_robot_memory_system.md`

### Scope
- consume real event flow
- generate real grouped episode candidates
- no semantic fact generation yet

### Definition of Done
- episodes are created from real interaction windows
- boundaries are stable and not overly noisy
- build succeeds

### How to verify
- perform several different short sessions
- confirm episode creation in debug or DB

---

## NE1-05 — Persist finalized episodes on session end / inactivity timeout

### Goal
Ensure episode candidates are finalized and written to Room after meaningful interaction windows end.

### Definition of Done
- finished episodes persist
- multiple sessions produce multiple episodes
- build succeeds

---

# 5. Batch NE2 — Episode summary and importance

## NE2-01 — Add episode summary fields and importance scoring model

### Goal
Extend the episode model to support summary text, importance, and emotionally meaningful metadata.

### Read first
- `pet_phase_next_evolution_system.md`
- `docs/07_robot_memory_system.md`

### Definition of Done
- summary fields exist in schema and domain model
- importance model is defined and explainable
- build succeeds

---

## NE2-02 — Implement deterministic episode summarizer v1

### Goal
Generate short structured summary text from episode data without using cloud AI.

### Scope
- rule-based only
- no external AI
- keep summary readable and compact

### Definition of Done
- persisted episodes receive real summary text
- summaries reflect session type and emotional meaning
- build succeeds

---

## NE2-03 — Implement importance scoring from episode characteristics

### Goal
Calculate an importance score based on reunion type, emotional shift, neglect, recovery, and interaction intensity.

### Definition of Done
- importance scores are real and debuggable
- edge cases do not overinflate trivial sessions
- build succeeds

---

## NE2-04 — Add episode debug viewer screen

### Goal
Create a debug screen showing saved episodes, summaries, importance, and key deltas.

### Definition of Done
- developers can inspect episode history in app
- screen is stable and readable
- build succeeds

---

# 6. Batch NE3 — Semantic memory foundation

## NE3-01 — Define SemanticMemoryFact model and Room schema

### Goal
Introduce a persistent fact layer for learned stable patterns.

### Read first
- `pet_phase_next_evolution_system.md`
- `docs/07_robot_memory_system.md`

### Definition of Done
- semantic fact model and DAO compile
- Room schema updates successfully
- build succeeds

---

## NE3-02 — Implement SemanticMemoryRepository

### Goal
Provide a repository for loading, upserting, and querying semantic facts.

### Definition of Done
- repository is production-usable
- build succeeds

---

## NE3-03 — Implement semantic fact inference from episodes

### Goal
Infer stable facts from repeated episodes, such as preferred time slots and primary interaction style.

### Scope
- start with 2–3 fact families only
- keep confidence-based updating

### Definition of Done
- repeated episodes generate or update semantic facts
- confidence updates are stable
- build succeeds

---

## NE3-04 — Add semantic memory debug panel

### Goal
Expose learned semantic facts and confidence values in a debug surface.

### Definition of Done
- developers can inspect learned facts and confidence
- build succeeds

---

# 7. Batch NE4 — Relationship system v2 foundation

## NE4-01 — Define BondState v2 model

### Goal
Add affection, trust, dependency, and stability as the new relationship model.

### Read first
- `pet_phase_next_evolution_system.md`
- `docs/09_pet_app_definition_full.md`

### Definition of Done
- model compiles and is reusable across layers
- build succeeds

---

## NE4-02 — Add Room persistence for BondState v2

### Goal
Persist the active bond state so it survives app restarts.

### Definition of Done
- Room schema compiles
- bond state can be loaded and saved
- build succeeds

---

## NE4-03 — Implement BondRepository with default initialization and migration path

### Goal
Ensure the app always has a valid bond state and can bridge from older single-score bond systems if needed.

### Definition of Done
- repository initializes correctly on fresh and existing app state
- build succeeds

---

## NE4-04 — Implement relationship update engine from interaction outcomes and episodes

### Goal
Update affection, trust, dependency, and stability based on episodes, care quality, neglect, and routine consistency.

### Definition of Done
- relationship dimensions update for real sessions
- updates are bounded and explainable
- build succeeds

---

## NE4-05 — Add relationship debug panel

### Goal
Display bond dimensions, recent deltas, and derived relationship state labels in-app.

### Definition of Done
- live relationship state is visible to developers
- build succeeds

---

# 8. Batch NE5 — Reunion and recovery upgrade

## NE5-01 — Define ReunionType model and resolver inputs

### Goal
Create a structured reunion model for app-open behavior selection.

### Read first
- `pet_phase_next_evolution_system.md`
- `docs/09_pet_app_definition_full.md`

### Definition of Done
- reunion types compile
- resolver inputs are explicit and debuggable
- build succeeds

---

## NE5-02 — Implement reunion resolver using absence, bond, recent care, and memory importance

### Goal
Determine reunion type from current context rather than simple elapsed time only.

### Definition of Done
- reunion resolution behaves differently across scenarios
- build succeeds

---

## NE5-03 — Implement recovery-aware greeting behavior

### Goal
Support cautious warming-up behavior after neglect instead of instant reset.

### Definition of Done
- pet can show real recovery arc in greetings
- build succeeds

---

## NE5-04 — Emit reunion / recovery events with payload details

### Goal
Make reunion reasoning visible in the event system and debug views.

### Definition of Done
- reunion and recovery events appear in logs or viewer
- payload includes useful context
- build succeeds

---

# 9. Batch NE6 — Habit and routine learning

## NE6-01 — Define UserHabitProfile model and persistence

### Goal
Introduce a persistent habit profile with preferred time slots, session patterns, and consistency score.

### Read first
- `pet_phase_next_evolution_system.md`

### Definition of Done
- model and persistence compile
- build succeeds

---

## NE6-02 — Implement habit aggregation from episodes

### Goal
Update the habit profile from repeated episodes and session metadata.

### Definition of Done
- preferred daypart and consistency can be derived from real use
- build succeeds

---

## NE6-03 — Implement expected return window logic

### Goal
Determine whether the current time is early, on-time, or missed relative to learned routine.

### Definition of Done
- expectation states are derived and debuggable
- build succeeds

---

## NE6-04 — Add habit debug panel

### Goal
Show preferred time slots, consistency score, and current expectation state.

### Definition of Done
- habit debug data is visible in app
- build succeeds

---

# 10. Batch NE7 — Personality evolution v2

## NE7-01 — Extend trait model for evolution-ready personality state

### Goal
Ensure the trait model supports slow drift, source attribution, and debug visibility.

### Read first
- `pet_phase_next_evolution_system.md`
- `docs/06_personality_engine.md`

### Definition of Done
- trait model supports evolution metadata or compatible tracking
- build succeeds

---

## NE7-02 — Implement trait drift engine from repeated care patterns and episodes

### Goal
Update personality traits slowly from repeated behavioral evidence.

### Definition of Done
- traits drift over time in bounded ways
- drift is real, explainable, and not noisy
- build succeeds

---

## NE7-03 — Implement derived personality profile labels

### Goal
Generate debuggable profile labels from current trait combinations.

### Definition of Done
- profile labels are computed and visible
- build succeeds

---

## NE7-04 — Add personality evolution debug panel

### Goal
Show current traits, recent drift reasons, and derived profile label.

### Definition of Done
- personality debug visibility is available in-app
- build succeeds

---

# 11. Batch NE8 — Daily life simulation

## NE8-01 — Define day phase model and resolver

### Goal
Introduce a day lifecycle model with morning, day, evening, and night phases.

### Read first
- `pet_phase_next_evolution_system.md`
- `docs/09_pet_app_definition_full.md`

### Definition of Done
- day phase resolver compiles and is testable
- build succeeds

---

## NE8-02 — Implement lifecycle baseline modifiers for mood / energy / initiative

### Goal
Add day-phase baseline influence to core pet state interpretation and behavior.

### Definition of Done
- lifecycle modifiers are real and debuggable
- build succeeds

---

## NE8-03 — Integrate day phase into greeting and idle behavior selection

### Goal
Make time-of-day actually affect visible pet behavior.

### Definition of Done
- same state can resolve to different expression style by day phase
- build succeeds

---

## NE8-04 — Add daily lifecycle debug panel

### Goal
Show current day phase and active baseline modifiers.

### Definition of Done
- lifecycle debug data is visible in app
- build succeeds

---

# 12. Batch NE9 — Behavior scoring v2

## NE9-01 — Define behavior score component model

### Goal
Represent behavior scoring as explainable components rather than opaque totals.

### Read first
- `pet_phase_next_evolution_system.md`
- existing behavior engine docs / code

### Definition of Done
- score components are structured and reusable
- build succeeds

---

## NE9-02 — Integrate memory, relationship, habit, personality, and lifecycle into candidate scoring

### Goal
Upgrade the runtime behavior selection engine to use the new long-term systems.

### Definition of Done
- behavior selection visibly changes with new context inputs
- build succeeds

---

## NE9-03 — Add suppression / cooldown / anti-repetition penalties to scoring v2

### Goal
Prevent spam and repetitive loops while preserving emotional logic.

### Definition of Done
- repeated triggers get penalized properly
- behavior remains lively without spam
- build succeeds

---

## NE9-04 — Add behavior decision debug viewer with score breakdowns

### Goal
Make runtime decisions inspectable for tuning and QA.

### Definition of Done
- developers can inspect selected behavior and top rejected candidates
- build succeeds

---

# 13. Batch NE10 — Invitation and expectation upgrade

## NE10-01 — Define invitation intent types and confidence model

### Goal
Structure invitation behavior into clear categories such as playful, comforting, expectant, and low-energy.

### Read first
- `pet_phase_next_evolution_system.md`
- `docs/09_pet_app_definition_full.md`

### Definition of Done
- invitation intent types compile
- build succeeds

---

## NE10-02 — Implement expectation-aware invitation resolver

### Goal
Select invitation styles using habit expectation, relationship state, and current needs.

### Definition of Done
- invitation style varies meaningfully with context
- build succeeds

---

## NE10-03 — Implement ignore-response suppression and re-engagement recovery

### Goal
Reduce spam when invitations are ignored and let confidence recover over time.

### Definition of Done
- ignored invites reduce subsequent pressure
- recovery is gradual and visible
- build succeeds

---

## NE10-04 — Emit invitation decision and outcome events

### Goal
Make invitation reasoning and outcomes visible in the event system.

### Definition of Done
- invitation events appear with useful payloads
- build succeeds

---

# 14. Batch NE11 — QA, tuning, and balancing

## NE11-01 — Create deterministic scenario test harness for major relationship / reunion paths

### Goal
Provide a structured way to simulate scenarios such as long absence, recovery, routine return, and inconsistent care.

### Read first
- `pet_phase_next_evolution_system.md`
- relevant runtime code

### Definition of Done
- scenario harness or debug controls allow repeatable testing
- build succeeds

---

## NE11-02 — Tune thresholds for episode creation, trust change, and habit detection

### Goal
Refine the first-pass thresholds so the system feels believable and not noisy.

### Definition of Done
- thresholds are explicit and documented in code
- build succeeds

---

## NE11-03 — Tune invitation suppression and recovery windows

### Goal
Ensure the pet stays emotionally expressive without becoming annoying.

### Definition of Done
- invitation cadence feels controlled in repeated tests
- build succeeds

---

## NE11-04 — Perform cross-system refactor pass limited to maintainability and duplication removal

### Goal
Clean up duplicated logic introduced across the new systems without changing product behavior.

### Definition of Done
- duplication reduced
- architecture remains aligned with source docs
- build succeeds

---

# 15. Recommended execution order

Recommended order for implementation:

1. NE1
2. NE2
3. NE3
4. NE4
5. NE5
6. NE6
7. NE7
8. NE8
9. NE9
10. NE10
11. NE11

Do not start advanced scoring before the new inputs exist.

---

# 16. Final execution note

This backlog is intentionally designed to avoid a common failure mode:

building a lot of “smart” infrastructure that never reaches visible pet behavior.

Every batch here must end in something that can be observed, tuned, and felt in the product.

That is the only way this phase turns into an actual companion upgrade instead of architecture theater.
