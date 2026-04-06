# AI Pet Robot — Task Breakdown After `mvp_stability_pass_plan`

Version: v1  
Date: 2026-04-04  
Purpose: Execution-grade task breakdown for the **next Phase 1 push** after finishing `mvp_stability_pass_plan`.

This breakdown is intentionally focused on the documented remaining blockers to Phase 1 completion.

---

## 0. Standard execution wrapper

Use this wrapper for every task:

```md
Read and follow `AGENTS.md` first.
Then read only the documents listed in "Read first" below.
Implement only this task.
Do not add mock production logic.
Do not leave TODOs, empty methods, or placeholder business logic.
Do not refactor unrelated systems.
Do not mix Cloud AI, robot body work, or new perception engines unless the task explicitly requires it.
Run the required build command before finishing.
Report exactly:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

---

## 1. Source of truth for this batch

Use this priority:

1. `docs/project_sync_status.md`
2. `docs/phase1_completion_guide.md`
3. `docs/09_pet_app_definition_full.md`
4. `docs/project_manifest.md`
5. `docs/02_android_pet_brain_architecture.md`
6. `docs/06_personality_engine.md`
7. `docs/07_robot_memory_system.md`
8. Existing verified stability-pass work

If there is any conflict:
- prioritize **pet feels alive**
- prioritize **visible behavior integration** over new subsystems
- do not turn the app into a chatbot, dashboard, or refactor project

---

## 2. Global rules for this breakdown

A task is DONE only when:

- the app builds successfully
- the change is visible, audible, persisted, or otherwise directly verifiable
- the task improves the real pet loop, not only debug internals
- no production path is left half-connected
- existing verified flows are not regressed

Default build command:

```bash
./gradlew assembleDebug
```

Run tests too if stable and directly relevant:

```bash
./gradlew test
```

---

# Batch A — BehaviorEngine v2 → Avatar Visibility

## N1

```md
Task ID: N1
Title: Audit and document the active home-avatar signal chain

Goal:
Identify the exact currently active path from Home screen state sources into the production avatar runtime, so BehaviorEngine v2 can be wired into the correct signal path without touching the wrong avatar module.

Read first:
- docs/project_sync_status.md
- docs/phase1_completion_guide.md

Scope:
- Only map the active signal chain.
- Do not refactor the avatar modules.
- Do not change behavior logic yet.

Files likely touched:
- only files needed for a small repo-adjacent architecture note and minimal code comments if truly necessary

Implementation steps:
1. Inspect the active Home screen avatar path.
2. Confirm which module is production-active (`ui-avatar` vs `pixel-avatar`).
3. Document the exact mapping chain into the avatar runtime.
4. Add only minimal clarifying comments or a short repo note if needed.

Definition of Done:
- The correct production avatar signal path is clearly identified.
- Future tasks can safely wire BehaviorEngine v2 into the right path.
- Build succeeds if code files were touched.

How to verify:
- Review the documented chain and confirm it matches the current running Home experience.

Build command:
- ./gradlew assembleDebug
```

## N2

```md
Task ID: N2
Title: Add BehaviorPlan-to-avatar bridge model

Goal:
Create the smallest production-safe bridge model that converts BehaviorEngine v2 output into a home-avatar-consumable signal without changing the avatar runtime itself.

Read first:
- docs/project_sync_status.md
- docs/phase1_completion_guide.md
- docs/02_android_pet_brain_architecture.md

Scope:
- Only add the bridge model/mapping layer.
- Do not redesign BehaviorEngine.
- Do not redesign avatar runtime.

Files likely touched:
- android-brain/brain/src/main/java/.../behavior/*
- android-brain/app/src/main/java/.../home/*
- android-brain/ui-avatar/src/main/java/.../* (only if a tiny compatible interface extension is required)

Implementation steps:
1. Review current BehaviorPlan / intention / attention outputs.
2. Define a compact bridge model for avatar-facing intent, attention, and urgency.
3. Map existing BehaviorEngine v2 outputs into that bridge model.
4. Keep it deterministic and debuggable.

Definition of Done:
- There is a real bridge model from behavior output to avatar-facing signal.
- No unrelated systems are changed.
- Build succeeds.

How to verify:
- Trigger sample behavior-producing flows and inspect logs/debug state to confirm the bridge outputs meaningful values.

Build command:
- ./gradlew assembleDebug
```

## N3

```md
Task ID: N3
Title: Wire BehaviorEngine v2 into HomePixelPetAvatarSignal

Goal:
Connect the existing behavior bridge into the active Home avatar signal chain so visible pet expression reflects real behavior output.

Read first:
- docs/project_sync_status.md
- docs/phase1_completion_guide.md
- docs/09_pet_app_definition_full.md

Scope:
- Only wire the existing behavior output into the existing home-avatar signal path.
- Do not add new behavior types.
- Do not refactor `PetBrainApp.kt` broadly.

Files likely touched:
- android-brain/app/src/main/java/.../home/*
- android-brain/brain/src/main/java/.../behavior/*
- android-brain/ui-avatar/src/main/java/.../* (only if required for signal compatibility)

Implementation steps:
1. Feed behavior bridge output into the active Home avatar signal source.
2. Respect priority ordering with existing greeting and transient reactions.
3. Ensure behavior can influence visible attention/intention without flicker.
4. Preserve current stable flows.

Definition of Done:
- BehaviorEngine v2 visibly affects the pet on the Home screen.
- Existing greeting/reaction flows still work.
- Build succeeds.

How to verify:
- Open the app and observe that behavior-driven visual changes occur without needing debug screens.
- Confirm reactions differ under different behavior contexts.

Build command:
- ./gradlew assembleDebug
```

## N4

```md
Task ID: N4
Title: Add behavior-priority arbitration for greeting, reaction, and idle signals

Goal:
Prevent conflicts between greeting, transient reactions, and new behavior-driven avatar states.

Read first:
- docs/phase1_completion_guide.md
- docs/09_pet_app_definition_full.md

Scope:
- Only add signal priority/arbitration.
- Do not add new pet features.

Files likely touched:
- android-brain/app/src/main/java/.../home/*
- android-brain/ui-avatar/src/main/java/.../*
- android-brain/brain/src/main/java/.../behavior/*

Implementation steps:
1. Define priority order between greeting, transient reactions, behavior-driven state, and baseline state.
2. Prevent lower-priority signals from overriding active high-priority moments.
3. Keep transitions smooth and bounded.

Definition of Done:
- Signal conflicts are controlled.
- Greeting/reaction/idle behavior no longer fight visibly.
- Build succeeds.

How to verify:
- Trigger app-open greeting, tap reaction, and idle behavior close together.
- Confirm the pet behaves coherently and transitions cleanly.

Build command:
- ./gradlew assembleDebug
```

---

# Batch B — Idle Life System

## N5

```md
Task ID: N5
Title: Implement idle activity scheduler for Home pet

Goal:
Create a bounded idle scheduler that triggers low-intensity life signals while the user is not interacting.

Read first:
- docs/phase1_completion_guide.md
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md

Scope:
- Only add scheduler logic.
- Do not yet add many new animations beyond existing supported reactions/states.

Files likely touched:
- android-brain/brain/src/main/java/.../behavior/*
- android-brain/app/src/main/java/.../home/*

Implementation steps:
1. Add randomized-but-bounded idle timing.
2. Pause or suppress idle during higher-priority states.
3. Expose scheduled idle actions to the active Home behavior path.

Definition of Done:
- Idle actions are scheduled while the pet is otherwise inactive.
- Idle does not spam or interfere with interaction flows.
- Build succeeds.

How to verify:
- Open the app and leave it untouched for 30 seconds.
- Confirm the pet performs subtle idle actions instead of remaining dead still.

Build command:
- ./gradlew assembleDebug
```

## N6

```md
Task ID: N6
Title: Emit and persist IDLE_ACTIVITY events

Goal:
Make idle life observable and persistent through the event system.

Read first:
- docs/phase1_completion_guide.md
- docs/07_robot_memory_system.md

Scope:
- Only add idle event publication and persistence through existing event architecture.

Files likely touched:
- android-brain/brain/src/main/java/.../events/*
- android-brain/app/src/main/java/.../home/*
- android-brain/memory/src/main/java/.../* if event wiring needs a tiny update

Implementation steps:
1. Add an idle activity event type if not present.
2. Emit real idle activity events from the scheduler.
3. Ensure events flow through existing viewer/export/persistence systems.

Definition of Done:
- Idle actions create real persisted events.
- Event viewer can show idle behavior history.
- Build succeeds.

How to verify:
- Leave the pet idle long enough to trigger activity.
- Open Event Viewer and confirm idle events are present.

Build command:
- ./gradlew assembleDebug
```

## N7

```md
Task ID: N7
Title: Add bounded idle micro-variation set

Goal:
Introduce a small set of low-intensity idle variations so idle behavior does not feel repetitive.

Read first:
- docs/phase1_completion_guide.md
- docs/09_pet_app_definition_full.md

Scope:
- Keep the set intentionally small.
- Reuse current runtime capabilities as much as possible.

Files likely touched:
- android-brain/ui-avatar/src/main/java/.../*
- android-brain/app/src/main/java/.../home/*
- android-brain/brain/src/main/java/.../behavior/*

Implementation steps:
1. Define a few safe idle variants such as look shift, curious pause, softer blink bias, or brief attentive state.
2. Route them through the active signal path.
3. Add anti-repeat rules.

Definition of Done:
- Idle moments show visible variation.
- Repetition is reduced without feeling random.
- Build succeeds.

How to verify:
- Observe the pet during idle for 1–2 minutes.
- Confirm idle moments vary but remain subtle.

Build command:
- ./gradlew assembleDebug
```

---

# Batch C — State Visibility on Main Experience

## N8

```md
Task ID: N8
Title: Add state-to-visible-expression mapping for hunger, sleepiness, energy, and bond

Goal:
Make the pet's important internal state fields visibly affect the main pet experience.

Read first:
- docs/phase1_completion_guide.md
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md

Scope:
- Only map already-existing state into visible behavior/expression.
- Do not add stat bars to Home.

Files likely touched:
- android-brain/brain/src/main/java/.../pet/*
- android-brain/app/src/main/java/.../home/*
- android-brain/ui-avatar/src/main/java/.../*

Implementation steps:
1. Define visible mappings for hunger, sleepiness, energy, and bond.
2. Feed those mappings into home expression/animation behavior.
3. Keep signals readable and non-debuggy.

Definition of Done:
- Users can infer meaningful state from the pet itself.
- Main screen remains product-like, not dashboard-like.
- Build succeeds.

How to verify:
- Simulate different state conditions and confirm visibly different outcomes on Home.

Build command:
- ./gradlew assembleDebug
```

## N9

```md
Task ID: N9
Title: Add lightweight mood or state-health ambiance signal on Home

Goal:
Surface inner pet condition on the Home screen through ambiance rather than numeric UI.

Read first:
- docs/project_sync_status.md
- docs/09_pet_app_definition_full.md

Scope:
- Add only one lightweight main-screen signal.
- Do not turn Home into a stat dashboard.

Files likely touched:
- android-brain/app/src/main/java/.../home/*

Implementation steps:
1. Choose one subtle ambiance mechanism such as glow, softness, energy, or tone bias.
2. Drive it from persisted state or resolved condition.
3. Keep it consistent with existing visual design.

Definition of Done:
- State is more perceptible from the main screen.
- The Home screen remains minimal and expressive.
- Build succeeds.

How to verify:
- Compare Home under multiple pet-state conditions and confirm ambiance changes are readable.

Build command:
- ./gradlew assembleDebug
```

---

# Batch D — Greeting System v2

## N10

```md
Task ID: N10
Title: Expand greeting resolver inputs with time gap, bond, and last interaction context

Goal:
Upgrade greeting selection so app-open greeting reflects continuity, not just current emotion.

Read first:
- docs/phase1_completion_guide.md
- docs/09_pet_app_definition_full.md
- docs/07_robot_memory_system.md

Scope:
- Only enhance greeting resolution inputs and selection.
- Do not add unrelated UI work.

Files likely touched:
- android-brain/brain/src/main/java/.../pet/*
- android-brain/app/src/main/java/.../startup/*
- android-brain/memory/src/main/java/.../* if a tiny query is needed

Implementation steps:
1. Add time-gap awareness.
2. Add bond/relationship influence.
3. Add last interaction context if already available cheaply.
4. Keep greeting deterministic and bounded.

Definition of Done:
- Greeting selection is clearly more contextual than before.
- Build succeeds.

How to verify:
- Reopen the app under different time gaps and state conditions and compare greeting outcomes.

Build command:
- ./gradlew assembleDebug
```

## N11

```md
Task ID: N11
Title: Coordinate greeting animation, audio, bubble, and event logging as one bundle

Goal:
Make app-open greeting feel like one coherent pet reaction instead of several loosely related outputs.

Read first:
- docs/phase1_completion_guide.md
- docs/09_pet_app_definition_full.md

Scope:
- Only coordinate the existing greeting outputs.
- Do not add full conversation or new audio systems.

Files likely touched:
- android-brain/app/src/main/java/.../startup/*
- android-brain/app/src/main/java/.../home/*
- android-brain/brain/src/main/java/.../events/*

Implementation steps:
1. Build a greeting bundle with animation/reaction, optional audio clip, text bubble, and event.
2. Trigger it on app open.
3. Ensure it respects signal priority and startup responsiveness.

Definition of Done:
- App-open greeting feels like one coordinated emotional moment.
- Greeting remains fast and stable.
- Build succeeds.

How to verify:
- Cold open the app several times under different pet states and inspect the visible coordinated greeting output.

Build command:
- ./gradlew assembleDebug
```

---

# Batch E — Variation and Anti-Repetition

## N12

```md
Task ID: N12
Title: Add weighted variation for common greeting and idle outputs

Goal:
Reduce repetitive feel while preserving clear state meaning.

Read first:
- docs/phase1_completion_guide.md
- docs/09_pet_app_definition_full.md

Scope:
- Only apply weighted variation where repetition is most noticeable.
- Keep rules explainable.

Files likely touched:
- android-brain/brain/src/main/java/.../pet/*
- android-brain/app/src/main/java/.../home/*

Implementation steps:
1. Add small weighted choice sets for greetings and idle reactions.
2. Bias choices by state/conditions rather than randomizing blindly.
3. Preserve coherence with anti-repeat protection.

Definition of Done:
- Repetition is lower.
- Outputs still clearly match the pet state.
- Build succeeds.

How to verify:
- Trigger repeated greeting and idle scenarios.
- Confirm outputs vary within a controlled range.

Build command:
- ./gradlew assembleDebug
```

## N13

```md
Task ID: N13
Title: Strengthen anti-repeat guard across repeated pet reactions

Goal:
Prevent the pet from replaying the same visible or audible reaction too many times in a short window.

Read first:
- docs/phase1_completion_guide.md
- docs/09_pet_app_definition_full.md

Scope:
- Only harden anti-repeat behavior.
- Do not add large new reaction systems.

Files likely touched:
- android-brain/ui-avatar/src/main/java/.../*
- android-brain/app/src/main/java/.../home/*
- android-brain/brain/src/main/java/.../* if selection memory lives there

Implementation steps:
1. Review current anti-repeat logic.
2. Extend it to the most repetitive greeting/idle/transient reactions.
3. Keep the behavior bounded and easy to reason about.

Definition of Done:
- Repeated taps/openings/idle loops feel less spammy.
- Build succeeds.

How to verify:
- Repeat the same trigger several times and confirm the pet avoids obvious repetition.

Build command:
- ./gradlew assembleDebug
```

---

# Batch F — Teach-Object UX Completion

## N14

```md
Task ID: N14
Title: Implement Teach Object screen shell and navigation flow

Goal:
Create the user-facing entry point for naming and saving recognized objects.

Read first:
- docs/project_sync_status.md
- docs/09_pet_app_definition_full.md
- docs/development_roadmap.md

Scope:
- Only add the teach-object UX shell and route.
- Do not redesign object detection engine.

Files likely touched:
- android-brain/app/src/main/java/.../object/*
- android-brain/app/src/main/java/.../navigation/*

Implementation steps:
1. Add a Teach Object screen and route.
2. Connect it to the current object-detection context.
3. Show only real detection data or clear empty states.

Definition of Done:
- There is a real user-facing object teach entry point.
- The screen is stable and reachable.
- Build succeeds.

How to verify:
- Navigate into the flow from the appropriate object-detection context and confirm it opens without crashing.

Build command:
- ./gradlew assembleDebug
```

## N15

```md
Task ID: N15
Title: Save named objects through the teach-object flow

Goal:
Allow the user to name and persist a detected object using the existing object storage path.

Read first:
- docs/project_sync_status.md
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Scope:
- Use the current object DB/repository flow.
- Do not add object embeddings.

Files likely touched:
- android-brain/app/src/main/java/.../object/*
- android-brain/memory/src/main/java/.../object/*
- android-brain/brain/src/main/java/.../events/*

Implementation steps:
1. Capture the current object candidate.
2. Save the user-provided object name and underlying known-object record.
3. Emit and persist a real event for object teaching.
4. Refresh the relevant UI/debug surfaces.

Definition of Done:
- A named object can be taught and persisted.
- The flow is real, not cosmetic.
- Build succeeds.

How to verify:
- Detect an object, teach it by name, then confirm it appears in the underlying known-object path or related UI/debug surface.

Build command:
- ./gradlew assembleDebug
```

## N16

```md
Task ID: N16
Title: Add known-object reaction hook on Home or relevant flow

Goal:
Make the newly taught object produce at least one real behavior or expression difference.

Read first:
- docs/project_sync_status.md
- docs/09_pet_app_definition_full.md

Scope:
- Only add one minimal meaningful known-object reaction path.
- Do not add complex object semantics.

Files likely touched:
- android-brain/brain/src/main/java/.../*
- android-brain/app/src/main/java/.../home/*
- android-brain/app/src/main/java/.../object/*

Implementation steps:
1. Detect when a known object is present.
2. Trigger one bounded reaction path such as curious recognition, toy interest, or named acknowledgment.
3. Persist the event and keep the flow stable.

Definition of Done:
- Taught objects have a visible product-level effect.
- Build succeeds.

How to verify:
- Teach an object, present it again, and confirm the pet behaves differently from unknown-object behavior.

Build command:
- ./gradlew assembleDebug
```

---

# Batch G — Evolution Feedback Verification

## N17

```md
Task ID: N17
Title: Add end-to-end verification path for trait/bond/evolution visibility

Goal:
Verify that persisted episodes, trait drift, and bond updates create observable downstream changes in the pet experience.

Read first:
- docs/project_sync_status.md
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md

Scope:
- Focus on verification and minimal visibility support.
- Do not redesign the evolution system.

Files likely touched:
- android-brain/app/src/main/java/.../debug/*
- android-brain/brain/src/main/java/.../*
- android-brain/memory/src/main/java/.../* only if a tiny query/helper is needed

Implementation steps:
1. Identify at least one reproducible path where evolution data should influence visible behavior.
2. Add minimal observability to prove the path is real.
3. Verify across multiple sessions or simulated time gaps.

Definition of Done:
- There is a repeatable way to demonstrate that evolution/bond/traits affect visible pet behavior.
- Build succeeds.

How to verify:
- Follow the documented verification flow and confirm different outputs after meaningful interaction history.

Build command:
- ./gradlew assembleDebug
```

---

# Batch H — Phase 1 Close-out Safety

## N18

```md
Task ID: N18
Title: Mark legacy avatar module as deprecated for future work safety

Goal:
Reduce implementation risk by making it clear which avatar system is active and which one is legacy.

Read first:
- docs/project_sync_status.md

Scope:
- Only add safe deprecation markers, comments, or documentation.
- Do not remove the old module unless explicitly requested.

Files likely touched:
- docs/*
- android-brain/pixel-avatar/* (comments/docs only if appropriate)
- repo-adjacent notes if helpful

Implementation steps:
1. Mark the older avatar path as legacy/deprecated in the clearest low-risk way.
2. Point future work toward the production avatar runtime.
3. Avoid code churn.

Definition of Done:
- Contributors are less likely to touch the wrong avatar system.
- Build remains unaffected.

How to verify:
- Inspect the note/comments and confirm the active-vs-legacy distinction is explicit.

Build command:
- ./gradlew assembleDebug
```

## N19

```md
Task ID: N19
Title: Create final Phase 1 verification checklist aligned with current real product

Goal:
Make it easy to repeatedly verify whether Phase 1 is truly done using the current actual app experience.

Read first:
- docs/phase1_completion_guide.md
- docs/project_sync_status.md
- docs/09_pet_app_definition_full.md

Scope:
- Only create/update the checklist artifact or screen.
- Do not add new features.

Files likely touched:
- docs/* or android-brain/app/src/main/java/.../debug/*

Implementation steps:
1. Capture the real verification gates for:
   - app open reaction
   - tap/activity reaction
   - idle life
   - return-after-time meaning
   - behavior visibility
   - object-teach loop
   - diary/memory continuity
2. Make the checklist easy to repeat.
3. Keep it aligned with the actual current app.

Definition of Done:
- There is a final Phase 1 verification checklist grounded in the actual product.
- Build succeeds if code was touched.

How to verify:
- Walk through the checklist against a running build.

Build command:
- ./gradlew assembleDebug
```

---

## 3. Recommended execution order

Execute in this order:

1. N1 → N4
2. N5 → N7
3. N8 → N9
4. N10 → N11
5. N12 → N13
6. N14 → N16
7. N17
8. N18 → N19

---

## 4. One-line handoff summary

> After `mvp_stability_pass_plan`, the correct next move is to finish Phase 1 by wiring the existing brain into the visible pet experience, then closing the remaining missing loops that still block “pet feels alive”.
