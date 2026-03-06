# AI Pet Robot — Agent Instructions

This repository is built for **small, verifiable, build-safe tasks**.

The main goal is to help coding agents such as Codex or Claude implement the AI Pet Robot project incrementally without breaking the build, without introducing placeholder logic, and without drifting away from the project architecture.

---

## 1. Source of Truth

Always read these files before making architectural or feature decisions.

### Core project documents

* `docs/project_manifest.md`
* `docs/development_roadmap.md`
* `docs/backlog_master_tasks.md`
* `docs/codex_tasks_phase1.md`

### Phase 1 documents

* `docs/02_android_pet_brain_architecture.md`
* `docs/06_personality_engine.md`
* `docs/07_robot_memory_system.md`

### Phase 2 document

* `docs/03_ai_cloud_integration.md`

### Phase 3 documents

* `docs/04_robot_body_hardware.md`
* `docs/05_full_system_architecture.md`

### Market and feasibility research

* `docs/01_robot_pet_market_research.md`

If there is any conflict:

1. `project_manifest.md`
2. `development_roadmap.md`
3. phase-specific architecture docs
4. backlog files

Do not invent a different architecture if the docs already define one.

---

## 2. Project Intent

This project builds an **AI Pet Robot** in three phases:

1. **Phase 1 — Android Pet Brain**

   * offline-first
   * Android is the brain and face
   * camera perception
   * event-based memory
   * personality and behavior loop

2. **Phase 2 — Cloud / AI Intelligence**

   * LLM integration
   * memory-aware chat
   * structured intents
   * Android brain still makes final decisions

3. **Phase 3 — Physical Body**

   * ESP32 / Arduino body controller
   * BLE or similar communication
   * motors, servos, sensors
   * Android sends high-level commands only

The architecture must remain:

* **offline-first for Phase 1**
* **event-driven**
* **modular**
* **incremental**
* **safe to build after every task**

---

## 3. Non-Negotiable Rules

### 3.1 No fake production logic

Do not:

* add mock implementations for production flows
* leave `TODO` in core logic
* leave empty methods in production code
* use `throw NotImplementedException()` in production code
* add placeholder business logic just to make the build pass

### 3.2 Every task must end in a runnable state

A task is only complete if:

* the code compiles
* the app or firmware builds successfully
* the feature is visible, testable, or verifiable
* there is no broken navigation or unfinished production path introduced by the task

### 3.3 Do not overreach

Only do the requested task.
Do not silently:

* refactor unrelated modules
* rename files broadly without need
* change architecture patterns
* change build tooling unless necessary for the task
* introduce large dependency changes unless required

### 3.4 Prefer the smallest complete vertical slice

A good task should go all the way through the stack for one tiny capability.
Examples:

* button → event publish → DB save → screen display
* camera frame → analyzer → event → overlay metric

Avoid partial layers that cannot be exercised.

---

## 4. Execution Model for Agents

For every task:

1. Read only the minimum relevant docs.
2. Identify the smallest working implementation.
3. Implement only that slice.
4. Build the project.
5. Report what changed and how to verify it.

If the task is ambiguous, choose the **smaller, safer implementation** that still satisfies the task.

If the requested change would break architecture, do not improvise. Follow the docs.

---

## 5. Required Output After Each Task

After finishing a task, always report in this format:

### Summary of changes

* concise summary of what was implemented

### Files changed

* list of modified files

### How to verify

* manual verification steps

### Build result

* exact command run
* whether it succeeded

### Remaining risks

* concise list of unresolved but relevant risks

Do not claim success unless the build actually succeeded.

---

## 6. Build and Verification Rules

### Android

Run after every Android task:

```bash
./gradlew assembleDebug
```

If tests exist for touched code, also run:

```bash
./gradlew test
```

### Firmware

If the task touches `robot-body/`, run the documented firmware build command for that project.
Do not skip firmware build verification if firmware code was changed.

### Verification expectation

Every task must include at least one of:

* visible UI result
* log output
* DB record
* file output
* hardware command response
* sensor telemetry

---

## 7. Android Project Rules

### 7.1 Module boundaries

Expected Android modules:

* `app`
* `core-common`
* `ui-avatar`
* `brain`
* `memory`
* `perception`

Keep responsibilities separated.
Do not move all logic into `app`.

### 7.2 Architecture direction

For Phase 1, the important data flow is:

`Camera/Input -> Perception -> Events -> Memory -> Brain -> Avatar/UI`

Use that direction when adding new functionality.

### 7.3 Event-first architecture

Important changes in perception, memory, behavior, and interaction should become events.
Events are not optional architecture decoration.
They are core infrastructure.

### 7.4 Persistence

If a task claims data should survive app restarts, use real persistence.
Do not simulate persistence with in-memory lists.

### 7.5 Debuggability

Prefer implementations that expose:

* event logs
* debug overlays
* visible state
* inspectable screens

For this project, debuggability is a feature.

---

## 8. Phase-Specific Rules

### 8.1 Phase 1

Focus on:

* avatar UI
* event log
* Room persistence
* camera pipeline
* face detection
* face embedding
* person memory
* object detection
* simple behavior loop
* basic personality traits

Avoid early overengineering:

* no vector DB unless the docs require it
* no cloud AI in Phase 1 tasks
* no heavy refactor of architecture just to be “cleaner”
* no attempt to make personality overly complex

### 8.2 Phase 2

LLM output must not directly control the robot body.
LLM may propose:

* intent
* speech
* emotion
* suggested action

The Android brain must validate and decide what to execute.

### 8.3 Phase 3

Android sends high-level commands.
Microcontroller handles:

* realtime control
* safety stop
* sensor readout
* actuator timing

Do not move high-level cognition into the MCU.

---

## 9. Coding Style Rules

### General

* Keep code simple and explicit.
* Prefer readable code over clever abstractions.
* Keep changes localized.
* Add comments only where they clarify non-obvious intent.

### Kotlin / Android

* Prefer small composables and small classes.
* Avoid giant files.
* Use real state flow, not ad-hoc global state.
* Keep business logic out of UI code when possible.

### Persistence

* Use Room for real persisted structured data.
* Keep entity-to-domain mapping explicit if needed.

### Error handling

* Fail gracefully in UI.
* Do not swallow exceptions silently.
* Surface meaningful state when model loading, camera, or DB fails.

---

## 10. What Not to Do

Do not:

* replace a working implementation with a mock
* add dead code “for future use”
* create unused helpers or abstractions
* batch several backlog items into one large speculative refactor
* mark a task complete before running the build
* change file layout just because it “feels nicer”
* introduce placeholder screens with no real flow behind them

---

## 11. Preferred Task Granularity

The ideal task:

* touches a small number of files
* has one clear goal
* can be completed in one build cycle
* produces an observable result

Examples of good task granularity:

* add Room `events` table and DAO
* wire test event button to EventBus
* render face bounding box overlay
* save one taught person to DB
* show recognized person label on camera screen

Examples of bad task granularity:

* implement all perception
* refactor the whole architecture
* create complete AI memory system in one task

---

## 12. If You Need to Make a Choice

When multiple implementation options exist, choose in this order:

1. the option explicitly aligned with the docs
2. the smallest working solution
3. the easiest to verify manually
4. the safest for future extension

If still uncertain, favor:

* fewer dependencies
* simpler flow
* better debug visibility

---

## 13. Recommended First Execution Order

If starting from scratch, begin with:

1. Android multi-module project skeleton
2. MainActivity + navigation shell
3. avatar models and static avatar
4. debug screen
5. event models and in-memory EventBus
6. debug overlay with latest event
7. Room event persistence
8. event viewer screen
9. CameraX preview
10. frame analyzer and camera frame events

This order is intentionally chosen to create a stable vertical slice early.

---

## 14. Final Instruction

Your job is not to produce the largest amount of code.
Your job is to produce the **smallest correct, buildable, verifiable increment** that matches the project documents.

If a task is done but not buildable, then it is not done.
If a task builds but has fake production logic, then it is not done.
If a task works but ignores the architecture docs, then it is not done.
