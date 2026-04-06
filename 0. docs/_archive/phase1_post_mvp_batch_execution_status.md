# Phase 1 Post-MVP Batch Execution Status

Date: 2026-04-04
Owner: Codex lead implementation agent

## Scope of this run

Executed the post-MVP Phase 1 batches in order with code-first validation and build safety.

## Batch Status

- Batch A (Behavior -> Avatar/Home): Completed
  - Behavior-driven intent now includes attention-aware tuning in the active home avatar bridge path.
  - Priority ownership remains: behavior > greeting > transient interaction > sound > baseline.
- Batch B (Idle Life System): Completed
  - Added bounded idle scheduler in the behavior loop.
  - Emits persisted `PET_IDLE_ACTIVITY` events with intent/attention/reason payload.
  - Scheduler suppresses during higher-priority moments (greeting, transient reactions, recent audio output, high alert/intensity, pending invite, recent direct interaction).
- Batch C (State -> Expression/Reaction): Completed (targeted reinforcement)
  - Avatar runtime state leakage now includes bond/social warmth floor through `FaceResolver` + `FaceAnimationRuntime`.
  - Existing hunger/sleepiness/energy mappings remain active and were preserved.
- Batch D (Greeting System v2): Completed (already strong + variation pass)
  - Existing contextual greeting stack (absence + bond/trust + state + evolution context) preserved.
  - Added controlled phrase variation in `PetGreetingResolver` while keeping calm baseline stable.
- Batch E (Variation/Anti-repeat): Completed (targeted)
  - Ambient talk lines now avoid immediate repetition.
  - Greeting phrasing now varies deterministically by real context.
  - Existing anti-repeat/cooldown in behavior execution and idle runtime remained intact.
- Batch F (Teach-object UX): Completed (verified + event closure)
  - Existing unknown-object teach dialog flow persists objects in Room.
  - Added `USER_TAUGHT_OBJECT` event publication with payload on successful object teach.
- Batch G (Evolution/Diary/Memory feedback): Completed (visible integration pass)
  - Home talk bubble orchestrator now surfaces one continuity bubble from today summary during idle windows.
  - Diary and daily summary pipeline remains unchanged and active.
- Batch H (Close-out hardening): In progress
  - Build and targeted tests pass.
  - Remaining close-out items are manual product-feel validation runs across real app scenarios.

## Commands run

- `./gradlew assembleDebug` (multiple runs) -> SUCCESS
- `./gradlew :brain:testDebugUnitTest --tests com.aipet.brain.brain.pet.PetGreetingResolverTest` -> SUCCESS
- `./gradlew :app:testDebugUnitTest --tests com.aipet.brain.app.avatar.RealPixelPetBridgeStateAdapterTest` -> SUCCESS
- `./gradlew :ui-avatar:testDebugUnitTest` -> SUCCESS

## Remaining risks

- Kotlin daemon access-denied warnings appeared in test runs and fell back to non-daemon compilation, but builds/tests still succeeded.
- Final Phase 1 declaration still depends on manual behavioral verification for:
  - 30s idle observation across multiple state bands
  - repeated app-open greeting feel under different absence contexts
  - repeated interaction anti-repeat feel on device
