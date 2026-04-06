# Next Phase Task Breakdown — Behavior-to-Experience Binding System

Version: v1  
Status: Active execution backlog  
Target quality: Production-ready only  
Project: AI Pet Robot / Android Pet App

---

# 1. Objective of This Task Set

This task set exists to implement the next critical product phase:

> make the pet feel alive by binding `BehaviorEngine v2` directly to avatar, audio, and talk-bubble experience.

The goal is not to add more subsystems. The project already has strong internal capability. The missing step is coordinated execution of visible behavior.

This task set therefore focuses on:

- behavior-authoritative experience output,
- unified reaction execution,
- production-safe sequencing,
- debug visibility,
- preserving fallback stability.

This task set must stay aligned with:

- `project_manifest.md`
- `development_roadmap.md`
- `pet_app_definition.md`
- `project_sync_status.md`
- the current advanced Phase 1 code reality

The current sync status explicitly identifies this as the highest-priority next step before cloud AI or robot body work. fileciteturn2file17 fileciteturn2file5 fileciteturn2file14

---

# 2. Global Execution Rules

## 2.1 Non-negotiable rules

Every task in this file must follow these rules:

- Build must remain green.
- No placeholder production logic.
- No TODOs in core paths.
- No giant refactor outside the current task scope.
- No second behavior engine.
- No new feature track unrelated to behavior-to-experience binding.
- Existing working home flow must not be broken.
- Existing state-based emotion path must remain available as fallback until the new path is proven stable.

## 2.2 Required output after each task

Every task must report exactly:

- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks

## 2.3 Build commands

Primary:

```bash
./gradlew assembleDebug
```

If touched logic already has stable tests:

```bash
./gradlew test
```

## 2.4 Scope protection

Unless the task explicitly says so, do **not**:

- refactor `PetBrainApp.kt` broadly,
- remove legacy `pixel-avatar`,
- rework Room schema,
- migrate navigation,
- add new major perception features,
- add cloud or body features.

---

# 3. Delivery Strategy

The work should be implemented in six controlled batches:

1. **Foundation contracts**  
   Create canonical intention/bundle/execution contracts.

2. **Binder path**  
   Map behavior output into an experience bundle.

3. **Visual execution**  
   Make avatar reactions behavior-driven first.

4. **Audio execution**  
   Add behavior-driven audio to the same bundle path.

5. **Talk and sequencing**  
   Bind talk bubbles and interruption policies.

6. **Debug + default adoption**  
   Expose trace/debug info and make the new path the home default.

This order is deliberate. Visible behavior comes before cleanup and before expansion.

---

# 4. Task Batches

## Batch BE-1 — Foundation Contracts

### Task BE-1.1

**Task ID:** BE-1.1  
**Title:** Define canonical behavior-to-experience domain contracts

**Goal**  
Create the production-facing domain models that will carry behavior output into coordinated experience execution.

**Read first**
- `docs/core/project_sync_status.md`
- `docs/core/pet_app_definition.md`
- `docs/active/next_phase_behavior_experience_design.md`

**Scope**
- Create only the core contracts.
- Do not wire them into UI yet.
- Do not redesign existing behavior models unless necessary for compatibility.

**What the task changes**
- Adds canonical types such as:
  - `PetExperienceBundle`
  - `VisualDirective`
  - `AudioDirective`
  - `TalkDirective`
  - `ExecutionPriority`
  - `InterruptPolicy`
  - `ExperienceCooldownKey`
  - execution result / rejection reason models

**What it must not do**
- No UI wiring
- No playback changes
- No refactor of the behavior engine itself

**Current context / risks**
- Existing behavior output likely already contains intention-like structure.
- Avoid duplicate semantics or a parallel behavior language.

**Expected outcome**
- A clear execution-facing contract that other tasks can consume.

**Implementation requirements by parts**
1. Add production domain models in the correct module.
2. Keep them explicit and debuggable.
3. Include typed rejection/suppression reasons for future traceability.

**Definition of done**
- Contracts compile.
- They can be referenced from app/brain/avatar/audio layers.
- No business logic paths are broken.

**Verification steps**
- Reference the new contracts from one existing integration point.
- Run `./gradlew assembleDebug`.

**Required output**
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks

**Short review checklist**
- Does the model unify visual/audio/talk?
- Is it explicit enough for debug?
- Did the task avoid inventing a second engine?

---

### Task BE-1.2

**Task ID:** BE-1.2  
**Title:** Add active experience runtime state model

**Goal**  
Create a runtime state model that represents the currently executing pet experience and can be observed from the UI layer.

**Read first**
- `docs/core/project_sync_status.md`
- `docs/active/next_phase_behavior_experience_design.md`

**Scope**
- Introduce runtime execution state only.
- Do not execute reactions yet.

**What the task changes**
- Adds `ActiveExperienceState` and supporting models.

**What it must not do**
- No home screen binding yet
- No direct avatar/audio changes yet

**Current context / risks**
- Current home screen already has transient reaction state and talk orchestration.
- New runtime state must complement, not fight, existing state until migrated.

**Expected outcome**
- A stable observable execution state object for later tasks.

**Implementation requirements by parts**
1. Represent active bundle, phase, start time, settle time, cooldown keys, and suppression notes.
2. Expose an observable state holder.
3. Keep it easy to inspect from debug UIs.

**Definition of done**
- Runtime state compiles and can be observed.
- No UI regression.

**Verification steps**
- Add a minimal debug reference to the runtime state.
- Run `./gradlew assembleDebug`.

**Short review checklist**
- Is the state model execution-focused rather than behavior-planning-focused?
- Can it support interrupt/settle later without redesign?

---

## Batch BE-2 — Binder Path

### Task BE-2.1

**Task ID:** BE-2.1  
**Title:** Implement initial BehaviorExperienceBinder from existing behavior output

**Goal**  
Create the first production binder that maps current `BehaviorEngine v2` output into a `PetExperienceBundle`.

**Read first**
- `docs/core/project_sync_status.md`
- `docs/core/pet_app_definition.md`
- `docs/active/next_phase_behavior_experience_design.md`

**Scope**
- Behavior output → bundle mapping only.
- Start with a safe subset of intention types.

**What the task changes**
- Adds `BehaviorExperienceBinder`
- Maps at least:
  - greeting
  - touch acknowledgment
  - playful response
  - attentive/listening
  - startled reaction
  - sleepy settle

**What it must not do**
- No direct UI calls
- No audio playback logic execution
- No large behavior-engine refactor

**Current context / risks**
- Behavior plans may be richer than the initial binder can fully express.
- Use safe fallback mapping for unsupported cases.

**Expected outcome**
- A real binder returning executable bundles for a meaningful subset of pet behavior.

**Implementation requirements by parts**
1. Consume current behavior outputs without duplicating planning.
2. Produce visual/audio/talk directives where supported.
3. Attach a structured debug reason.
4. Provide explicit fallback behavior for unsupported plans.

**Definition of done**
- Binder produces real bundles for supported intentions.
- Unsupported behavior outputs degrade safely.
- Build succeeds.

**Verification steps**
- Add unit-safe mapping verification if feasible.
- Log or expose resolved bundles from a controlled debug trigger.
- Run `./gradlew assembleDebug`.

**Short review checklist**
- Is the binder mapping behavior, not replacing it?
- Is there a safe fallback path?
- Are bundle reasons inspectable?

---

### Task BE-2.2

**Task ID:** BE-2.2  
**Title:** Publish binder debug trace events

**Goal**  
Make bundle resolution observable by publishing trace/debug events or equivalent structured logs when behavior is converted into an experience bundle.

**Read first**
- `docs/core/project_sync_status.md`
- `docs/07_robot_memory_system.md`
- `docs/active/next_phase_behavior_experience_design.md`

**Scope**
- Trace publication only.
- No execution yet.

**What the task changes**
- Adds behavior-to-experience trace events or structured debug log model.

**What it must not do**
- No new DB table unless strictly necessary
- No UI overhaul

**Current context / risks**
- Event system is rich already; avoid noisy spam.

**Expected outcome**
- Developers can see why a bundle was chosen or rejected.

**Definition of done**
- Trace output exists for supported binder paths.
- Build succeeds.

**Verification steps**
- Trigger a supported behavior path and inspect event/log output.
- Run `./gradlew assembleDebug`.

**Short review checklist**
- Is the trace useful and concise?
- Did the task avoid event spam?

---

## Batch BE-3 — Visual Execution First

### Task BE-3.1

**Task ID:** BE-3.1  
**Title:** Implement PetIntentionExecutor visual channel

**Goal**  
Create the executor that can accept a `PetExperienceBundle` and drive the avatar runtime through the visual directive channel only.

**Read first**
- `docs/core/project_sync_status.md`
- `docs/active/next_phase_behavior_experience_design.md`

**Scope**
- Visual execution only.
- No audio or talk execution yet.

**What the task changes**
- Adds `PetIntentionExecutor` or equivalent execution coordinator.
- Wires `VisualDirective` into the active avatar runtime.

**What it must not do**
- No new custom animation engine
- No removal of current avatar runtime
- No audio or talk changes yet

**Current context / risks**
- Current avatar runtime already has strong transient reaction support.
- The executor must integrate with existing runtime, not bypass it.

**Expected outcome**
- Behavior-driven visual reactions become possible end-to-end.

**Implementation requirements by parts**
1. Accept bundle input.
2. Apply visual directive into current reaction/orchestrator pipeline.
3. Track active execution phase.
4. Support settle back to baseline state.

**Definition of done**
- A supported intention can visibly change the pet through the new executor path.
- Existing fallback emotion path still works.
- Build succeeds.

**Verification steps**
- Trigger greeting/tap/playful reaction and confirm the avatar is now driven by executor output.
- Run `./gradlew assembleDebug`.

**Short review checklist**
- Is the executor driving the existing runtime instead of duplicating it?
- Does the visual path settle cleanly?
- Is fallback intact?

---

### Task BE-3.2

**Task ID:** BE-3.2  
**Title:** Route app-open greeting through behavior-driven visual execution

**Goal**  
Make app-open greeting use the new binder + executor visual path instead of relying only on state/emotion resolution.

**Read first**
- `docs/core/pet_app_definition.md`
- `docs/core/project_sync_status.md`
- `docs/active/next_phase_behavior_experience_design.md`

**Scope**
- Only app-open greeting path.
- No full replacement of all reactions yet.

**What the task changes**
- Migrates greeting flow to the new path.

**What it must not do**
- No new greeting feature expansion
- No audio/talk migration yet unless required minimally

**Current context / risks**
- App-open greeting is the most important first 3-second experience.
- It is the safest place to prove the new behavior authority.

**Expected outcome**
- The first visible pet moment feels more intentional and less like a static state reveal.

**Definition of done**
- App-open greeting is behavior-driven through the new path.
- Fallback exists if bundle resolution fails.
- Build succeeds.

**Verification steps**
- Open the app under multiple pet-state conditions.
- Confirm greeting visibly differs and flows through the executor.
- Run `./gradlew assembleDebug`.

**Short review checklist**
- Does open-app feel more alive?
- Is the change localized and safe?

---

## Batch BE-4 — Audio Execution

### Task BE-4.1

**Task ID:** BE-4.1  
**Title:** Add behavior-driven audio execution channel

**Goal**  
Extend the executor so a `PetExperienceBundle` can drive audio playback through the same coordinated path as visual execution.

**Read first**
- `docs/core/project_sync_status.md`
- `docs/core/audio_architecture.md`
- `docs/active/next_phase_behavior_experience_design.md`

**Scope**
- Add audio directive execution only.
- Use current production audio playback system.

**What the task changes**
- Wires `AudioDirective` into audio playback execution.

**What it must not do**
- No new audio engine
- No conversation/TTS work
- No broad rework of sound categories

**Current context / risks**
- Audio system already has category routing and arbitration.
- New path must respect existing cooldown and suppression logic.

**Expected outcome**
- A behavior bundle can now produce a coordinated sound response.

**Implementation requirements by parts**
1. Use bundle audio directives.
2. Reuse current playback arbitration where possible.
3. Surface suppression reason if audio is not played.

**Definition of done**
- Supported behaviors can trigger audio through the bundle path.
- Audio suppression/cooldown remains stable.
- Build succeeds.

**Verification steps**
- Trigger greeting and playful/tap cases.
- Confirm audio plays through the new path or shows a clear suppression trace.
- Run `./gradlew assembleDebug`.

**Short review checklist**
- Is audio now part of one coordinated bundle?
- Are suppression reasons inspectable?

---

### Task BE-4.2

**Task ID:** BE-4.2  
**Title:** Add behavior-level audio anti-repeat and category cooldown enforcement

**Goal**  
Move final audio anti-repeat responsibility into the behavior execution path so repeated interactions do not sound robotic.

**Read first**
- `docs/core/audio_architecture.md`
- `docs/active/next_phase_behavior_experience_design.md`

**Scope**
- Audio cooldown/anti-repeat only.

**What the task changes**
- Adds final behavior-level gating for audio playback.

**What it must not do**
- No new assets
- No new categories

**Expected outcome**
- Repeated taps or repeated low-value events stop sounding spammy.

**Definition of done**
- Cooldown rules are enforced through the execution path.
- Build succeeds.

**Verification steps**
- Repeatedly trigger the same interaction.
- Confirm playback becomes intentionally suppressed instead of repetitive.
- Run `./gradlew assembleDebug`.

**Short review checklist**
- Does this improve believability rather than merely suppressing sound?
- Is the rule observable in debug?

---

## Batch BE-5 — Talk and Sequencing

### Task BE-5.1

**Task ID:** BE-5.1  
**Title:** Bind TalkDirective into HomeTalkBubbleOrchestrator

**Goal**  
Make talk-bubble messaging a coordinated output of the same experience bundle used for avatar and audio.

**Read first**
- `docs/core/project_sync_status.md`
- `docs/core/pet_app_definition.md`
- `docs/active/next_phase_behavior_experience_design.md`

**Scope**
- Talk bubble binding only.

**What the task changes**
- Routes `TalkDirective` into the home talk bubble pipeline.

**What it must not do**
- No rewrite of all home text content
- No generative text system

**Current context / risks**
- Existing talk bubble system already performs dedupe and priority handling.
- Use it as execution infrastructure, not as the semantic source.

**Expected outcome**
- Messaging becomes part of the same behavioral reaction bundle.

**Definition of done**
- Greeting/tap/playful bundles can show coordinated talk output.
- Existing talk bubble stability remains intact.
- Build succeeds.

**Verification steps**
- Trigger multiple supported reactions and confirm message selection follows bundle output.
- Run `./gradlew assembleDebug`.

**Short review checklist**
- Is meaning now decided before UI delivery?
- Is dedupe preserved?

---

### Task BE-5.2

**Task ID:** BE-5.2  
**Title:** Implement execution interruption and settle policy

**Goal**  
Add production interruption/settle rules so reactions have coherent timing and priority.

**Read first**
- `docs/active/next_phase_behavior_experience_design.md`
- `docs/core/project_sync_status.md`

**Scope**
- Execution-phase timing and interruption only.

**What the task changes**
- Introduces:
  - execution phase tracking,
  - interruptibility rules,
  - settle handling,
  - low-priority suppression during high-salience reactions.

**What it must not do**
- No redesign of the behavior engine
- No global refactor of all event flows

**Current context / risks**
- This is where accidental complexity can explode.
- Keep rules explicit and minimal.

**Expected outcome**
- Reactions feel deliberate rather than overlapping or jittery.

**Definition of done**
- High-priority reactions can interrupt low-priority ones.
- Low-value reactions do not break major greeting/startle flows.
- Settle transitions work.
- Build succeeds.

**Verification steps**
- Trigger overlapping tap/sound/greeting scenarios.
- Confirm priority handling is coherent.
- Run `./gradlew assembleDebug`.

**Short review checklist**
- Are interruption rules understandable?
- Did the implementation avoid a giant second state machine?

---

## Batch BE-6 — Debug Visibility and Default Adoption

### Task BE-6.1

**Task ID:** BE-6.1  
**Title:** Add behavior-to-experience debug panel and trace screen

**Goal**  
Expose the new behavior execution path clearly so developers can inspect why the pet reacted in a certain way.

**Read first**
- `docs/core/project_sync_status.md`
- `docs/active/next_phase_behavior_experience_design.md`

**Scope**
- Debug UI only.

**What the task changes**
- Adds a debug panel/screen showing:
  - active intention
  - current bundle summary
  - active execution phase
  - last suppression reason
  - cooldown keys
  - last interruption record

**What it must not do**
- No product-facing redesign
- No raw internal dump that is unreadable

**Expected outcome**
- Developers can verify the new path without guessing.

**Definition of done**
- Debug UI shows meaningful execution state.
- Build succeeds.

**Verification steps**
- Trigger several reactions and inspect the debug screen.
- Run `./gradlew assembleDebug`.

**Short review checklist**
- Can a developer explain the pet’s reaction from this screen?
- Is the information useful rather than noisy?

---

### Task BE-6.2

**Task ID:** BE-6.2  
**Title:** Make behavior-to-experience path the default home reaction path with fallback retained

**Goal**  
Adopt the new binder + executor path as the default way the home experience reacts, while keeping existing state-based emotion mapping as fallback for stability.

**Read first**
- `docs/core/project_sync_status.md`
- `docs/core/pet_app_definition.md`
- `docs/active/next_phase_behavior_experience_design.md`

**Scope**
- Default adoption only.
- No large cleanup beyond what is required to make the new path authoritative.

**What the task changes**
- Switches home reaction authority to the behavior-driven path.
- Keeps resolver fallback when no active bundle exists or when bundle resolution fails.

**What it must not do**
- No deletion of legacy paths unless they are conclusively unused in this scope
- No broad DI cleanup

**Current context / risks**
- This is the most sensitive task in the batch set.
- Stability matters more than elegance.

**Expected outcome**
- The pet now visibly behaves like a creature with intention, not only a state-rendered avatar.

**Definition of done**
- Home experience defaults to behavior-authoritative execution.
- Existing fallback path preserves stability.
- Build succeeds.
- Open-app, tap, sound-reactive, and playful response paths all use the new system for supported cases.

**Verification steps**
- Cold-open the app under different pet states.
- Tap pet repeatedly.
- Trigger sound-reactive behavior.
- Confirm the pet reacts through the coordinated behavior path.
- Run `./gradlew assembleDebug`.

**Short review checklist**
- Is the pet now behavior-led instead of only state-rendered?
- Is fallback still stable?
- Did the task stay within phase scope?

---

# 5. Recommended Order of Execution

Run the tasks in this order:

1. BE-1.1
2. BE-1.2
3. BE-2.1
4. BE-2.2
5. BE-3.1
6. BE-3.2
7. BE-4.1
8. BE-4.2
9. BE-5.1
10. BE-5.2
11. BE-6.1
12. BE-6.2

Do not skip directly to default adoption.

---

# 6. Definition of Done for the Full Batch Set

This whole task set is complete only when:

- `BehaviorEngine v2` behavior output becomes the primary source for visible home reactions,
- avatar, audio, and talk can all be driven from one coordinated experience bundle,
- interruption/cooldown/anti-repeat are real and inspectable,
- app-open greeting feels more alive and intentional,
- repeated interactions no longer feel mechanically repetitive,
- debug tooling clearly explains bundle resolution and execution,
- existing fallback state-driven path remains available for unsupported or failed bundle cases,
- no unrelated feature track was mixed into this phase.

---

# 7. What Should Come After This Phase

Only after this phase is stable should the project choose among these next priorities:

1. targeted cleanup of `PetBrainApp.kt`
2. legacy `pixel-avatar` deprecation plan
3. object teach UX flow
4. memory card persistence/indexing improvements
5. Phase 2 conversation planning

Cloud AI and robot body should remain deferred until this phase proves the pet can already feel alive on-device.
