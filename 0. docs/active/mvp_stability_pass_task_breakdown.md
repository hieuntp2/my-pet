# MVP Stability Pass — Detailed Task Breakdown

Version: v1  
Owner: Hieu Le  
Phase: Post Behavior-Authoritative Experience Integration  
Execution style: Production-safe, build-safe, one task at a time

---

# 0. Purpose

This document breaks the MVP Stability Pass into small, verifiable, implementation-ready tasks for Codex / Claude.

This phase exists to finish the current Pet App MVP properly.

It assumes the project already has:
- advanced Phase 1 systems
- behavior-authoritative pet experience integration underway or completed
- real persistence, memory, perception, audio, and diary infrastructure

This phase does not add new major features.
It closes, stabilizes, verifies, and productizes what already exists.

---

# 1. Global Rules

For every task:
- Read `AGENTS.md` first.
- Read only the documents listed in Read first.
- Implement only the requested task.
- Do not add mock production logic.
- Do not leave TODO, placeholder business logic, or empty methods.
- Keep the build green.
- Preserve offline-first behavior.
- Preserve event-driven architecture.
- Prefer the smallest complete vertical slice.

Required output after each task:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks

Required build command:
- `./gradlew assembleDebug`

If stable tests exist for touched areas:
- `./gradlew test`

---

# 2. Source of Truth

Use this decision hierarchy:

1. `docs/core/project_sync_status.md`
2. `docs/core/project_manifest.md`
3. `docs/core/development_roadmap.md`
4. `docs/core/pet_app_definition.md`
5. `docs/core/personality_engine.md`
6. `docs/core/memory_system.md`
7. `docs/core/audio_architecture.md`
8. this task breakdown

If there is conflict:
- keep the pet as a digital creature
- prioritize the core pet loop
- prefer product experience over architectural theory
- do not expand scope into cloud AI or robot body

---

# 3. Phase Success Criteria

This phase is only complete when:
- app-open reaction is reliable
- interaction loop is stateful and persistent
- time-away continuity is visible
- diary is emotionally useful
- settings/reset flows are safe
- a formal MVP verification checklist exists

---

# Batch S1 — Core Loop Audit Closure

## S1.1

```md
Task ID: S1.1
Title: Audit and close any missing app-open core loop gaps

Goal:
Ensure app open consistently performs the full alive-loop: state refresh, condition resolution, greeting, and visible pet presentation without dead or duplicate startup behavior.

Read first:
- docs/core/project_sync_status.md
- docs/core/pet_app_definition.md
- docs/active/next_phase_behavior_experience_design.md

Scope:
- Only inspect and fix the app-open experience path.
- Do not redesign unrelated systems.

Files likely touched:
- android-brain/app/src/main/java/.../PetBrainApp.kt
- android-brain/app/src/main/java/.../startup/*
- android-brain/brain/src/main/java/.../pet/*
- android-brain/app/src/main/java/.../home/*

Implementation steps:
1. Trace the current app-open lifecycle from startup to visible Home state.
2. Identify duplicate, delayed, missing, or conflicting greeting/reaction paths.
3. Fix the path so decay refresh, visible greeting, and first live state happen coherently.
4. Preserve existing persistence/event flows.

Definition of Done:
- App open no longer feels dead or double-triggered.
- Greeting and first visible state are coherent.
- Build succeeds.

How to verify:
- Cold launch the app several times.
- Confirm the first 3 seconds are stable and alive.
- Confirm there are no duplicate greetings or broken startup visuals.

Build command:
- ./gradlew assembleDebug
```

## S1.2

```md
Task ID: S1.2
Title: Ensure tap interaction completes the full pet feedback loop

Goal:
Make sure tapping the pet always triggers a complete response chain: visible reaction, optional audio path, state change, event log, and persistence.

Read first:
- docs/core/project_sync_status.md
- docs/core/pet_app_definition.md
- docs/core/memory_system.md

Scope:
- Only fix the tap interaction loop.
- Do not expand into unrelated interactions yet.

Files likely touched:
- android-brain/app/src/main/java/.../home/*
- android-brain/brain/src/main/java/.../interaction/*
- android-brain/brain/src/main/java/.../pet/*
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Trace the current tap flow end to end.
2. Confirm visible reaction, state mutation, event emission, and persistence all happen.
3. Fix any missing persistence, skipped state updates, or cosmetic-only reaction paths.
4. Preserve cooldown and anti-repeat logic where appropriate.

Definition of Done:
- Tap is not just visual.
- Tap changes real pet state and that state survives.
- Build succeeds.

How to verify:
- Tap the pet multiple times with reasonable spacing.
- Confirm visible reaction, state change, and persisted outcome via debug or restart.

Build command:
- ./gradlew assembleDebug
```

## S1.3

```md
Task ID: S1.3
Title: Ensure feed, play, and rest actions are all real stateful loops

Goal:
Verify and complete the full action loop for the core pet menu actions so each one produces the intended state, reaction, persistence, and memory/event outcome.

Read first:
- docs/core/project_sync_status.md
- docs/core/pet_app_definition.md
- docs/core/personality_engine.md

Scope:
- Only fix the existing Feed / Play / Rest actions.
- Do not add new actions.

Files likely touched:
- android-brain/app/src/main/java/.../home/*
- android-brain/brain/src/main/java/.../care/*
- android-brain/brain/src/main/java/.../pet/*
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Trace feed, play, and rest flows one by one.
2. Confirm each changes the correct parts of pet state.
3. Confirm each has meaningful visible feedback and any appropriate audio/reaction.
4. Confirm events and persistence happen.
5. Fix any action that is partially cosmetic or inconsistently persisted.

Definition of Done:
- Feed, Play, and Rest each function as complete loops.
- Their effects survive restart.
- Build succeeds.

How to verify:
- Trigger each action.
- Inspect visible result, state change, event visibility, and post-restart continuity.

Build command:
- ./gradlew assembleDebug
```

---

# Batch S2 — Stability Hardening

## S2.1

```md
Task ID: S2.1
Title: Remove duplicate or conflicting reaction execution paths on Home

Goal:
Reduce instability by identifying and resolving duplicate or competing reaction paths that can cause conflicting visual or audible pet responses.

Read first:
- docs/core/project_sync_status.md
- docs/active/next_phase_behavior_experience_design.md

Scope:
- Only address duplicate/conflicting Home reaction ownership.
- Do not refactor the whole app.

Files likely touched:
- android-brain/app/src/main/java/.../home/*
- android-brain/brain/src/main/java/.../behavior/*
- android-brain/ui-avatar/src/main/java/.../*
- android-brain/app/src/main/java/.../PetBrainApp.kt

Implementation steps:
1. Map which systems currently trigger Home reactions.
2. Identify duplicate or overlapping execution paths.
3. Consolidate ownership so the intended primary path wins.
4. Preserve necessary fallback behavior only where clearly justified.

Definition of Done:
- Home reactions feel calmer and more deterministic.
- Conflicting duplicate triggers are reduced or removed.
- Build succeeds.

How to verify:
- Repeat common interactions and sound/activity triggers.
- Confirm the pet no longer fires obviously duplicated reactions.

Build command:
- ./gradlew assembleDebug
```

## S2.2

```md
Task ID: S2.2
Title: Harden lifecycle and restart continuity for Home pet runtime state

Goal:
Ensure the home pet runtime does not lose coherence or enter stale/duplicated states after pause/resume, process death, or restart.

Read first:
- docs/core/project_sync_status.md
- docs/core/development_roadmap.md

Scope:
- Focus on lifecycle continuity for the home/runtime pet experience.
- Do not redesign persistence broadly.

Files likely touched:
- android-brain/app/src/main/java/.../PetBrainApp.kt
- android-brain/app/src/main/java/.../home/*
- android-brain/brain/src/main/java/.../pet/*
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Inspect pause/resume and cold-start behavior.
2. Fix stale UI state, duplicate launch reactions, or runtime-only state that breaks continuity.
3. Ensure persisted state remains the source of truth after restart.
4. Keep the home experience stable across normal Android lifecycle events.

Definition of Done:
- Restart and resume no longer cause broken or obviously duplicated pet behavior.
- Build succeeds.

How to verify:
- Open app, interact, background it, resume it, force-close it, and reopen it.
- Confirm behavior continuity remains coherent.

Build command:
- ./gradlew assembleDebug
```

## S2.3

```md
Task ID: S2.3
Title: Stabilize audio reaction execution and suppression behavior

Goal:
Ensure the current audio reaction path behaves consistently, respects cooldowns/suppression, and does not produce incoherent or spammy results.

Read first:
- docs/core/project_sync_status.md
- docs/core/audio_architecture.md
- docs/active/next_phase_behavior_experience_design.md

Scope:
- Only stabilize the existing audio reaction path.
- Do not add new voice/ASR features.

Files likely touched:
- android-brain/app/src/main/java/.../home/*
- android-brain/brain/src/main/java/.../audio/*
- android-brain/brain/src/main/java/.../behavior/*
- android-brain/app/src/main/java/.../debug/*

Implementation steps:
1. Trace the current audio response execution path.
2. Confirm cooldown, anti-repeat, and skip reasons work correctly.
3. Fix spam, overlap, or incoherent mismatch where found.
4. Expose enough debug visibility to confirm executed vs skipped behavior.

Definition of Done:
- Audio strengthens the pet experience instead of destabilizing it.
- Executed/skipped behavior can be debugged.
- Build succeeds.

How to verify:
- Trigger several pet reactions over a short session.
- Confirm audio feels intentional, not noisy or broken.

Build command:
- ./gradlew assembleDebug
```

---

# Batch S3 — Diary and Memory Usefulness

## S3.1

```md
Task ID: S3.1
Title: Validate diary event-to-memory visibility for recent pet interactions

Goal:
Ensure recent interactions that matter to the pet loop appear meaningfully in the Diary, rather than disappearing or feeling like raw debug output.

Read first:
- docs/core/project_sync_status.md
- docs/core/memory_system.md
- docs/core/pet_app_definition.md

Scope:
- Focus on diary visibility from existing event/memory derivation.
- Do not redesign memory architecture or add new storage models unless directly needed.

Files likely touched:
- android-brain/app/src/main/java/.../diary/*
- android-brain/brain/src/main/java/.../memory/*
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Trace how recent pet interactions become diary-visible content.
2. Identify missing, low-value, or overly debug-like memory cards.
3. Improve the mapping or presentation so diary entries reflect meaningful pet experiences.
4. Preserve current event-driven derivation unless a direct bug forces adjustment.

Definition of Done:
- Recent important pet interactions are visible in the Diary.
- Diary feels more like memory visibility than raw debugging.
- Build succeeds.

How to verify:
- Perform several interactions.
- Open Diary and confirm those experiences are represented meaningfully.

Build command:
- ./gradlew assembleDebug
```

## S3.2

```md
Task ID: S3.2
Title: Improve diary empty state and continuity messaging

Goal:
Make the diary usable and emotionally coherent even when memories are sparse, especially on new installs or low-activity sessions.

Read first:
- docs/core/pet_app_definition.md
- docs/core/project_sync_status.md

Scope:
- Only improve diary empty/light-content experience.
- Do not add unrelated diary features.

Files likely touched:
- android-brain/app/src/main/java/.../diary/*

Implementation steps:
1. Inspect current empty or low-memory diary experience.
2. Improve empty state messaging so it still supports the pet fantasy.
3. Ensure the diary communicates continuity, not there is no data.
4. Keep the implementation lightweight and product-aligned.

Definition of Done:
- Diary empty/light states feel intentional and product-ready.
- Build succeeds.

How to verify:
- Test on a fresh or reset profile.
- Open the Diary before and after a few interactions.

Build command:
- ./gradlew assembleDebug
```

---

# Batch S4 — Settings and Reset Readiness

## S4.1

```md
Task ID: S4.1
Title: Add a minimal sound setting for pet audio reactions

Goal:
Provide a real user-facing way to control pet audio behavior, at minimum with an on/off or mute setting that persists.

Read first:
- docs/core/pet_app_definition.md
- docs/core/audio_architecture.md
- docs/core/project_sync_status.md

Scope:
- Add only the minimal sound control needed for MVP readiness.
- Do not build a full settings product.

Files likely touched:
- android-brain/app/src/main/java/.../settings/*
- android-brain/brain/src/main/java/.../audio/*
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Add a persisted sound-enabled setting.
2. Surface it in an appropriate minimal settings/debug-accessible UI.
3. Respect the setting in the real audio response path.
4. Keep existing behavior stable when sound is enabled.

Definition of Done:
- Users can disable pet reaction sound.
- The preference persists across restart.
- Build succeeds.

How to verify:
- Disable sound, trigger reactions, confirm no audio plays.
- Restart app and confirm the setting persists.

Build command:
- ./gradlew assembleDebug
```

## S4.2

```md
Task ID: S4.2
Title: Implement safe reset pet data flow

Goal:
Provide a safe reset path that clears pet-specific user data while restoring the app to a valid first-run or fresh-pet state.

Read first:
- docs/core/project_sync_status.md
- docs/core/memory_system.md
- docs/core/pet_app_definition.md

Scope:
- Only implement reset relevant to MVP product readiness.
- Do not build export/import or account sync systems.

Files likely touched:
- android-brain/app/src/main/java/.../settings/*
- android-brain/memory/src/main/java/.../*
- android-brain/brain/src/main/java/.../pet/*
- android-brain/app/src/main/java/.../startup/*

Implementation steps:
1. Define which persisted pet/user-facing data should be cleared.
2. Implement the clear/reset path safely.
3. Ensure a valid new default pet state/profile is created after reset.
4. Ensure onboarding or naming flow behaves correctly after reset.

Definition of Done:
- Reset produces a valid fresh pet app state.
- The app remains usable immediately after reset.
- Build succeeds.

How to verify:
- Interact with the pet to create state/history.
- Reset the app.
- Confirm a clean fresh-pet flow appears and the app remains stable.

Build command:
- ./gradlew assembleDebug
```

## S4.3

```md
Task ID: S4.3
Title: Verify reset consistency across state, diary, and settings-sensitive flows

Goal:
Ensure the reset flow does not leave inconsistent state behind across Home, Diary, events, and sound/settings-sensitive behavior.

Read first:
- docs/core/project_sync_status.md
- docs/core/pet_app_definition.md

Scope:
- Validate reset coherence only.
- Do not add new settings beyond what already exists.

Files likely touched:
- android-brain/app/src/main/java/.../*
- android-brain/memory/src/main/java/.../*
- android-brain/brain/src/main/java/.../*

Implementation steps:
1. Exercise the real reset flow against live app features.
2. Fix any leftover stale state, broken diary output, or invalid startup behavior after reset.
3. Ensure reset does not create hidden continuity corruption.

Definition of Done:
- Reset leaves the app clean, coherent, and re-usable.
- Build succeeds.

How to verify:
- Create pet history, disable/enable settings as applicable, reset, and then walk through the app again.
- Confirm no stale content remains unexpectedly.

Build command:
- ./gradlew assembleDebug
```

---

# Batch S5 — MVP Verification System

## S5.1

```md
Task ID: S5.1
Title: Create an in-repo Pet App MVP verification checklist document

Goal:
Create a formal markdown checklist that defines how to verify whether the Pet App MVP is actually complete.

Read first:
- docs/core/project_sync_status.md
- docs/core/pet_app_definition.md
- docs/active/mvp_stability_pass_plan.md

Scope:
- Create a verification document only.
- Do not change app behavior in this task.

Files likely touched:
- docs/active/pet_app_mvp_verification_checklist.md

Implementation steps:
1. Write a checklist covering the true MVP completion criteria.
2. Include manual verification steps for:
   - app open reaction
   - tap reaction
   - feed/play/rest actions
   - diary visibility
   - time-away continuity
   - restart continuity
   - sound enabled/disabled behavior
   - reset flow
   - offline-first flow
3. Include pass/fail criteria and expected observations.

Definition of Done:
- A contributor can use the checklist without guessing.
- The checklist is aligned with the product definition and current app reality.

How to verify:
- Review the checklist and confirm it covers the full core loop.

Build command:
- None required for doc-only task
```

## S5.2

```md
Task ID: S5.2
Title: Execute and close the MVP verification checklist against the live app

Goal:
Run through the verification checklist, fix any discovered MVP-blocking issues, and leave an accurate final status.

Read first:
- docs/active/pet_app_mvp_verification_checklist.md
- docs/core/project_sync_status.md
- docs/core/pet_app_definition.md

Scope:
- Only fix issues discovered during checklist execution that directly block MVP completion.
- Do not expand scope into future-phase improvements.

Files likely touched:
- any directly relevant app/brain/memory files
- docs/active/pet_app_mvp_verification_results.md

Implementation steps:
1. Run the verification checklist end to end.
2. Record pass/fail findings.
3. Fix MVP-blocking issues only.
4. Create a final verification results document showing:
   - passed items
   - failed items
   - what was fixed
   - remaining non-MVP risks

Definition of Done:
- The app has a real MVP pass/fail record.
- Any remaining failures are explicitly documented.
- Build succeeds for code changes.

How to verify:
- Re-run the checklist after fixes and confirm the final result matches reality.

Build command:
- ./gradlew assembleDebug
```

---

# 4. Recommended Execution Order

Run tasks in this order:
1. S1.1
2. S1.2
3. S1.3
4. S2.1
5. S2.2
6. S2.3
7. S3.1
8. S3.2
9. S4.1
10. S4.2
11. S4.3
12. S5.1
13. S5.2

This order is intentional:
- first close the loop
- then harden it
- then ensure memory visibility
- then add product readiness controls
- then verify the MVP formally

---

# 5. Definition of Phase Done

This stability phase is done only when:
- S1 through S5 are complete
- no core loop blocker remains hidden
- the verification results document says the app qualifies as MVP complete
- remaining risks are post-MVP, not MVP blockers
