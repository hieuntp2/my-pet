
# Codex Tasks — Pet App Full Backlog (Android, Offline-First)

Version: v1  
Purpose: Bộ backlog đầy đủ, copy-paste được cho AI coding agents (Codex/Claude) để triển khai **Pet App MVP** theo từng task nhỏ, build-safe, event-driven, offline-first.  
Scope baseline: Pet App chạy trên Android, dùng state + memory + avatar + audio reaction để tạo cảm giác một **digital pet sống động**, không phải chatbot hay assistant.

---

## 0. Standard Prompt Wrapper

Dùng wrapper này cho **mọi task** dưới đây:

```md
Read and follow `AGENTS.md` first.

Then read only the documents listed in "Read first".

Implement only this task.

Do not:
- add mock production logic
- leave TODOs, empty methods, or placeholder business logic
- break existing verified flows
- silently expand into adjacent tasks

Must:
- keep build green
- preserve offline-first behavior
- preserve event-driven architecture
- integrate with Room-backed persistence where the task requires persistence
- keep debug visibility for new behavior

Run the required build command before finishing.

Report exactly:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

---

## 1. Source of Truth for This Backlog

Use these documents as the decision hierarchy for the Pet App tasks:

1. `docs/project_manifest.md`
2. `docs/development_roadmap.md`
3. `docs/09_pet_app_definition_full.md`
4. `docs/07_robot_memory_system.md`
5. `docs/06_personality_engine.md`
6. `docs/08_audio_interaction_architecture.md`
7. existing Phase 1 and Phase 1.5 task docs

If there is any conflict:
- keep the Pet App as a **digital creature**
- prioritize **emotion + behavior first**
- do not turn the product into a chatbot or assistant
- do not bypass the event/memory architecture

---

## 2. Global Rules for Every Pet Task

A task is DONE only when:
- app builds successfully
- the new behavior is visible, audible, persisted, or otherwise verifiable
- no production path is left half-implemented
- no fake logic is introduced in place of required real behavior
- the implementation stays within the requested task scope

Required build command unless the task explicitly adds tests:
- `./gradlew assembleDebug`

If tests exist for changed logic and are stable:
- `./gradlew test`

---

# Batch 17 — Pet Core State Foundation

## C104

```md
Task ID: C104
Title: Define PetMood and PetState core models

Goal:
Create the core pet state models that represent the pet's internal condition and can be used consistently across brain, UI, memory, and behavior layers.

Read first:
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../pet/model/PetMood.kt
- android-brain/brain/src/main/java/.../pet/model/PetState.kt

Implementation steps:
1. Add a `PetMood` enum covering at least HAPPY, NEUTRAL, SAD, EXCITED, CURIOUS, SLEEPY, HUNGRY.
2. Add a `PetState` data model containing:
   - mood
   - energy
   - hunger
   - sleepiness
   - social
   - bond
   - lastUpdatedAt
3. Add safe value constraints or helper functions if needed to keep numeric state ranges valid.

Definition of Done:
- Core state models compile.
- Other modules can reference them.
- Build succeeds.

How to verify:
- Reference the models from app or brain code and run a build.

Build command:
- ./gradlew assembleDebug
```

## C105

```md
Task ID: C105
Title: Add Room entity and DAO for current PetState persistence

Goal:
Persist the pet's current state in Room so it survives app restarts and becomes the source of truth for lifecycle updates.

Read first:
- docs/09_pet_app_definition_full.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/memory/src/main/java/.../pet/PetStateEntity.kt
- android-brain/memory/src/main/java/.../pet/PetStateDao.kt
- android-brain/memory/src/main/java/.../db/AppDatabase.kt

Implementation steps:
1. Create a `PetStateEntity` that maps the current state into Room.
2. Add a DAO with:
   - getCurrentState()
   - upsertState()
3. Register the entity and DAO in the Room database.

Definition of Done:
- Room compiles with the new pet state schema.
- Pet state can be read and written through the DAO.
- Build succeeds.

How to verify:
- Initialize the database and confirm app startup does not crash.

Build command:
- ./gradlew assembleDebug
```

## C106

```md
Task ID: C106
Title: Implement PetStateRepository with default state initialization

Goal:
Ensure the pet always has a valid state, including on first launch where no saved state exists yet.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/brain/src/main/java/.../pet/PetStateRepository.kt
- android-brain/memory/src/main/java/.../pet/*

Implementation steps:
1. Create a repository that loads the current PetState from Room.
2. If no state exists yet, create and persist a default initial state.
3. Expose repository methods for:
   - getOrCreateState()
   - saveState()
   - observeState() if useful for UI integration

Definition of Done:
- Fresh install creates a valid default pet state.
- No app path depends on null state.
- Build succeeds.

How to verify:
- Launch the app on a fresh install and confirm state initialization completes without crashes.

Build command:
- ./gradlew assembleDebug
```

## C107

```md
Task ID: C107
Title: Implement time-based PetState decay engine

Goal:
Make the pet's internal state evolve meaningfully as time passes while the user is away.

Read first:
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md

Files likely touched:
- android-brain/brain/src/main/java/.../pet/PetStateDecayEngine.kt

Implementation steps:
1. Calculate elapsed time from `lastUpdatedAt`.
2. Apply gradual decay rules at minimum for:
   - energy decreases
   - hunger increases
   - sleepiness increases
   - social decreases or drifts based on design
3. Clamp values into valid ranges.
4. Return the updated state with a new `lastUpdatedAt`.

Definition of Done:
- The decay engine produces real state changes for non-zero time gaps.
- State never goes out of range.
- Build succeeds.

How to verify:
- Simulate a multi-hour gap in code or debug flow and inspect the updated state values.

Build command:
- ./gradlew assembleDebug
```

## C108

```md
Task ID: C108
Title: Apply PetState decay on app open lifecycle

Goal:
Update the pet state when the app is opened so the user immediately sees that time has passed.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/app/src/main/java/.../MainActivity.kt
- android-brain/app/src/main/java/.../startup/*
- android-brain/brain/src/main/java/.../pet/*

Implementation steps:
1. Add an app-open state refresh flow.
2. Load current saved state.
3. Apply the time-based decay engine.
4. Save the updated state back to persistence.
5. Keep startup stable and idempotent.

Definition of Done:
- Reopening the app after a time gap changes the pet state.
- Startup remains stable.
- Build succeeds.

How to verify:
- Leave the app closed for a while or use a controllable time provider, then reopen and inspect state changes.

Build command:
- ./gradlew assembleDebug
```

## C109

```md
Task ID: C109
Title: Derive high-level PetConditions from raw numeric state

Goal:
Translate raw numeric state into meaningful high-level conditions that later behavior and avatar logic can use.

Read first:
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md

Files likely touched:
- android-brain/brain/src/main/java/.../pet/model/PetCondition.kt
- android-brain/brain/src/main/java/.../pet/PetConditionResolver.kt

Implementation steps:
1. Define a model for derived conditions such as:
   - HUNGRY
   - SLEEPY
   - BORED
   - PLAYFUL
   - ATTACHED
2. Implement rule-based resolution from numeric state.
3. Keep rules deterministic and easy to debug.

Definition of Done:
- Raw PetState can be converted into derived pet conditions.
- Build succeeds.

How to verify:
- Feed several sample PetState inputs and confirm the expected derived conditions.

Build command:
- ./gradlew assembleDebug
```

## C110

```md
Task ID: C110
Title: Add PetState debug panel on Home or Debug screen

Goal:
Expose the current pet state visibly in the app so state-driven behavior can be verified during development.

Read first:
- docs/development_roadmap.md
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/app/src/main/java/.../debug/PetStateDebugPanel.kt
- android-brain/app/src/main/java/.../home/HomeScreen.kt
- android-brain/app/src/main/java/.../debug/DebugScreen.kt

Implementation steps:
1. Create a compact UI panel showing at least mood, energy, hunger, sleepiness, social, and bond.
2. Bind it to the real current state from the repository.
3. Show sensible loading and empty states if needed.

Definition of Done:
- Developers can see the live current pet state in the app.
- Values come from real persisted state.
- Build succeeds.

How to verify:
- Launch the app and confirm the panel shows real values from state persistence.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 18 — Emotion Mapping and Greeting

## C111

```md
Task ID: C111
Title: Define PetEmotion model for avatar-facing expression mapping

Goal:
Add a dedicated emotion model that sits between internal state and avatar rendering.

Read first:
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md

Files likely touched:
- android-brain/brain/src/main/java/.../pet/model/PetEmotion.kt

Implementation steps:
1. Define PetEmotion values aligned with the MVP experience, including:
   - IDLE
   - HAPPY
   - CURIOUS
   - SLEEPY
   - SAD
   - EXCITED
   - HUNGRY
2. Keep the model reusable across avatar, audio, and greeting logic.

Definition of Done:
- Emotion model compiles.
- Other modules can reference it.
- Build succeeds.

How to verify:
- Reference the emotion model from avatar mapping code and run a build.

Build command:
- ./gradlew assembleDebug
```

## C112

```md
Task ID: C112
Title: Implement PetState to PetEmotion resolver

Goal:
Resolve the visible emotion of the pet from internal state and derived conditions.

Read first:
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md

Files likely touched:
- android-brain/brain/src/main/java/.../pet/PetEmotionResolver.kt

Implementation steps:
1. Map current numeric state and derived conditions into a visible PetEmotion.
2. Prefer clear rules over opaque randomness.
3. Allow small weighted variation only where it improves believability without breaking determinism.

Definition of Done:
- A real emotion is derived from internal state.
- The mapping is stable and explainable.
- Build succeeds.

How to verify:
- Feed sample states and verify the expected resolved emotion.

Build command:
- ./gradlew assembleDebug
```

## C113

```md
Task ID: C113
Title: Bind PetEmotion to the existing Avatar system

Goal:
Make the avatar expression reflect the resolved pet emotion instead of only manual controls.

Read first:
- docs/09_pet_app_definition_full.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../home/HomeScreen.kt
- android-brain/ui-avatar/src/main/java/.../*
- android-brain/brain/src/main/java/.../pet/*

Implementation steps:
1. Add a mapper from PetEmotion into the current avatar state/rendering model.
2. Feed the resolved PetEmotion into the Home screen avatar.
3. Keep manual debug controls available only if they do not break the real pet flow.

Definition of Done:
- Avatar visual state updates from real pet emotion.
- Existing app flow remains stable.
- Build succeeds.

How to verify:
- Change or simulate pet state and confirm the avatar expression changes accordingly.

Build command:
- ./gradlew assembleDebug
```

## C114

```md
Task ID: C114
Title: Add state-aware greeting reaction on app open

Goal:
Ensure the pet reacts immediately when the app opens, with a greeting that reflects its current state.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/brain/src/main/java/.../pet/PetGreetingResolver.kt
- android-brain/app/src/main/java/.../startup/*
- android-brain/ui-avatar/src/main/java/.../*

Implementation steps:
1. Resolve a greeting reaction from the current pet state and emotion.
2. Trigger a visible greeting reaction on app open.
3. Keep the greeting brief and consistent with the pet's current mood.

Definition of Done:
- App open does not feel like a dead static screen.
- Greeting varies meaningfully with current state.
- Build succeeds.

How to verify:
- Open the app under different state conditions and confirm greeting differences are visible.

Build command:
- ./gradlew assembleDebug
```

## C115

```md
Task ID: C115
Title: Emit PET_GREETED event for app-open greeting flow

Goal:
Record pet greetings in the event system so the opening loop becomes observable and persistent.

Read first:
- docs/09_pet_app_definition_full.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/brain/src/main/java/.../events/EventType.kt
- android-brain/app/src/main/java/.../startup/*
- android-brain/brain/src/main/java/.../pet/*

Implementation steps:
1. Add a new event type for pet greeting if it does not exist yet.
2. Publish a real greeting event when the app-open greeting is triggered.
3. Include useful payload metadata such as emotion or reason.

Definition of Done:
- App-open greeting emits a real event.
- Event is visible in the existing event flow.
- Build succeeds.

How to verify:
- Launch the app and confirm the event appears in overlay, logs, or Event Viewer.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 19 — Pet Profile and Core Identity

## C116

```md
Task ID: C116
Title: Define PetProfile model and Room schema

Goal:
Introduce the pet's durable identity separate from temporary runtime state.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/brain/src/main/java/.../pet/model/PetProfile.kt
- android-brain/memory/src/main/java/.../pet/PetProfileEntity.kt
- android-brain/memory/src/main/java/.../pet/PetProfileDao.kt
- android-brain/memory/src/main/java/.../db/AppDatabase.kt

Implementation steps:
1. Define a PetProfile with at least:
   - id
   - name
   - createdAt
2. Add a Room entity and DAO for loading and saving the single active profile.
3. Register the schema with the database.

Definition of Done:
- Pet profile compiles and persists in Room.
- Build succeeds.

How to verify:
- Save and read a profile through the DAO or repository path.

Build command:
- ./gradlew assembleDebug
```

## C117

```md
Task ID: C117
Title: Implement PetProfileRepository with default pet creation

Goal:
Ensure the app always has a valid active pet profile, including first launch.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/brain/src/main/java/.../pet/PetProfileRepository.kt
- android-brain/memory/src/main/java/.../pet/*

Implementation steps:
1. Create a repository for active profile access.
2. If no profile exists yet, create a default one.
3. Keep the repository compatible with future onboarding-based naming.

Definition of Done:
- Fresh app startup creates a valid pet profile.
- Existing launches reuse the same persisted profile.
- Build succeeds.

How to verify:
- Fresh install creates a pet profile and relaunch preserves it.

Build command:
- ./gradlew assembleDebug
```

## C118

```md
Task ID: C118
Title: Show active pet name and identity on Home screen

Goal:
Make the pet feel like a real individual by displaying its identity on the Home screen.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/app/src/main/java/.../home/HomeScreen.kt

Implementation steps:
1. Load the active PetProfile on Home.
2. Show the current pet name in the main Home UI.
3. Keep the layout stable with or without later onboarding.

Definition of Done:
- Home screen shows the active pet identity from real persistence.
- Build succeeds.

How to verify:
- Launch app and confirm the current pet name is visible.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 20 — Tap Interaction Core Loop

## C119

```md
Task ID: C119
Title: Add tap detection on the avatar surface

Goal:
Allow the user to directly tap the pet avatar as the main MVP interaction.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/app/src/main/java/.../home/HomeScreen.kt
- android-brain/ui-avatar/src/main/java/.../*

Implementation steps:
1. Add a tap target on the avatar area.
2. Ensure repeated taps are detected reliably.
3. Keep the interaction path simple and production-usable.

Definition of Done:
- Tapping the avatar triggers a real interaction callback.
- Build succeeds.

How to verify:
- Launch the app and tap the avatar repeatedly to confirm callbacks occur.

Build command:
- ./gradlew assembleDebug
```

## C120

```md
Task ID: C120
Title: Emit PET_TAPPED event with real interaction metadata

Goal:
Record avatar tap interactions in the event system.

Read first:
- docs/09_pet_app_definition_full.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/brain/src/main/java/.../events/EventType.kt
- android-brain/app/src/main/java/.../home/HomeScreen.kt
- android-brain/brain/src/main/java/.../interaction/*

Implementation steps:
1. Add PET_TAPPED event type if needed.
2. Publish the event from the real tap interaction path.
3. Include useful payload metadata such as timestamp and interaction source.

Definition of Done:
- Each avatar tap emits a real event.
- Event persists through the existing event pipeline.
- Build succeeds.

How to verify:
- Tap the avatar and confirm PET_TAPPED appears in logs or Event Viewer.

Build command:
- ./gradlew assembleDebug
```

## C121

```md
Task ID: C121
Title: Add tap interaction cooldown to prevent reaction spam

Goal:
Keep tap interactions believable by preventing uncontrolled repeated triggering.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/brain/src/main/java/.../interaction/TapInteractionLimiter.kt
- android-brain/app/src/main/java/.../home/HomeScreen.kt

Implementation steps:
1. Add a simple cooldown or debounce policy for avatar taps.
2. Keep the cooldown short enough to preserve responsiveness.
3. Ensure blocked taps do not break the UI flow.

Definition of Done:
- Rapid repeated taps are rate-limited sensibly.
- Normal tapping still works.
- Build succeeds.

How to verify:
- Tap rapidly and confirm reaction frequency is limited without freezing the UI.

Build command:
- ./gradlew assembleDebug
```

## C122

```md
Task ID: C122
Title: Implement tap reaction mapping to visible pet response

Goal:
Make the pet visibly react when tapped.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/brain/src/main/java/.../interaction/TapReactionResolver.kt
- android-brain/ui-avatar/src/main/java/.../*
- android-brain/app/src/main/java/.../home/HomeScreen.kt

Implementation steps:
1. Map a successful tap into a visible response such as happy or curious reaction.
2. Keep the reaction short and clearly noticeable.
3. Integrate with the existing avatar rendering flow.

Definition of Done:
- Tapping the pet causes a visible pet reaction.
- The reaction comes from the real interaction path.
- Build succeeds.

How to verify:
- Tap the avatar and observe the visible change immediately.

Build command:
- ./gradlew assembleDebug
```

## C123

```md
Task ID: C123
Title: Update PetState from successful tap interactions

Goal:
Ensure taps change the pet internally, not just cosmetically.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/brain/src/main/java/.../interaction/TapStateEffectResolver.kt
- android-brain/brain/src/main/java/.../pet/*

Implementation steps:
1. Apply a real state effect for successful taps, such as:
   - mood bias toward positive
   - social increase
   - bond increase
2. Save the updated state through the repository.
3. Keep value changes bounded and small.

Definition of Done:
- Tapping the pet changes its saved state.
- State changes are visible in debug state UI.
- Build succeeds.

How to verify:
- Tap the pet several times and confirm state values update in the debug panel.

Build command:
- ./gradlew assembleDebug
```

## C124

```md
Task ID: C124
Title: Emit PET_INTERACTION_APPLIED event after successful tap state change

Goal:
Capture the completed interaction effect as a distinct event for memory and debugging.

Read first:
- docs/09_pet_app_definition_full.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/brain/src/main/java/.../events/EventType.kt
- android-brain/brain/src/main/java/.../interaction/*

Implementation steps:
1. Add an event type for completed interaction effects if needed.
2. Emit the event only after the state update succeeds.
3. Include the key deltas in the event payload.

Definition of Done:
- A successful tap produces both the interaction event and the applied-effect event.
- Build succeeds.

How to verify:
- Tap the avatar and inspect the resulting event sequence in the Event Viewer.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 21 — Long Press and Interaction Variation

## C125

```md
Task ID: C125
Title: Add long-press interaction detection on the avatar

Goal:
Introduce a second intentional interaction type beyond simple taps.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/app/src/main/java/.../home/HomeScreen.kt
- android-brain/ui-avatar/src/main/java/.../*

Implementation steps:
1. Add long-press detection on the avatar surface.
2. Keep it separate from short tap handling.
3. Preserve responsiveness and avoid accidental conflicts.

Definition of Done:
- Long pressing the avatar triggers a distinct interaction path.
- Build succeeds.

How to verify:
- Short tap and long press the avatar and confirm both paths can be triggered intentionally.

Build command:
- ./gradlew assembleDebug
```

## C126

```md
Task ID: C126
Title: Emit PET_LONG_PRESSED event and reaction path

Goal:
Make long press observable and distinct in the event and reaction system.

Read first:
- docs/09_pet_app_definition_full.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/brain/src/main/java/.../events/EventType.kt
- android-brain/brain/src/main/java/.../interaction/*
- android-brain/app/src/main/java/.../home/HomeScreen.kt

Implementation steps:
1. Add a long-press event type if needed.
2. Emit PET_LONG_PRESSED from the real long-press path.
3. Route it into a distinct reaction compared with a normal tap.

Definition of Done:
- Long press creates a distinct event and visible pet response.
- Build succeeds.

How to verify:
- Long press the avatar and confirm the event and visible reaction differ from a short tap.

Build command:
- ./gradlew assembleDebug
```

## C127

```md
Task ID: C127
Title: Apply state effects for long press interactions

Goal:
Let long press affect the pet differently than a simple tap so the interaction system has variation.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/brain/src/main/java/.../interaction/*

Implementation steps:
1. Define a distinct state effect for long press, such as stronger social or bond change.
2. Keep the effect bounded and consistent with current mood or condition if appropriate.
3. Persist the updated state.

Definition of Done:
- Long press changes the saved state in a way distinct from short tap.
- Build succeeds.

How to verify:
- Compare state changes after short tap vs long press in the debug state UI.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 22 — Activities: Feed, Play, Rest

## C128

```md
Task ID: C128
Title: Add Activities section UI on Home screen

Goal:
Provide visible user controls for basic care activities: feed, play, and rest.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/app/src/main/java/.../home/HomeScreen.kt
- android-brain/app/src/main/java/.../home/ActivitiesSection.kt

Implementation steps:
1. Add buttons or controls for Feed, Play, and Rest.
2. Place them in a stable Home screen section.
3. Keep the UI simple and clearly labeled.

Definition of Done:
- Home screen shows real activity controls.
- Build succeeds.

How to verify:
- Launch the app and confirm the three activities are visible and tappable.

Build command:
- ./gradlew assembleDebug
```

## C129

```md
Task ID: C129
Title: Implement Feed activity state effects

Goal:
Let the user feed the pet and change its internal state meaningfully.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/brain/src/main/java/.../activity/FeedPetUseCase.kt
- android-brain/brain/src/main/java/.../pet/*

Implementation steps:
1. Implement the Feed activity flow.
2. Apply state updates such as:
   - hunger decreases
   - mood shifts positive
   - bond increases slightly
3. Persist the updated state.

Definition of Done:
- Feed activity updates and saves the pet state.
- Build succeeds.

How to verify:
- Trigger Feed and confirm hunger and related values update in the debug state UI.

Build command:
- ./gradlew assembleDebug
```

## C130

```md
Task ID: C130
Title: Implement Play activity state effects

Goal:
Let the user play with the pet and update internal state accordingly.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/brain/src/main/java/.../activity/PlayWithPetUseCase.kt
- android-brain/brain/src/main/java/.../pet/*

Implementation steps:
1. Implement the Play activity flow.
2. Apply state updates such as:
   - mood increases
   - social increases
   - bond increases
   - energy decreases slightly
3. Persist the updated state.

Definition of Done:
- Play activity updates the real saved pet state.
- Build succeeds.

How to verify:
- Trigger Play and inspect the state changes in the debug panel.

Build command:
- ./gradlew assembleDebug
```

## C131

```md
Task ID: C131
Title: Implement Rest activity state effects

Goal:
Let the user intentionally rest the pet and reduce fatigue-related needs.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/brain/src/main/java/.../activity/LetPetRestUseCase.kt
- android-brain/brain/src/main/java/.../pet/*

Implementation steps:
1. Implement the Rest activity flow.
2. Apply state updates such as:
   - energy increases
   - sleepiness decreases
   - activity mood bias shifts calmer
3. Persist the updated state.

Definition of Done:
- Rest activity updates the real saved pet state.
- Build succeeds.

How to verify:
- Trigger Rest and inspect the debug panel for state changes.

Build command:
- ./gradlew assembleDebug
```

## C132

```md
Task ID: C132
Title: Emit activity events for Feed, Play, and Rest

Goal:
Record user care actions in the event system for later memory and diary views.

Read first:
- docs/09_pet_app_definition_full.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/brain/src/main/java/.../events/EventType.kt
- android-brain/brain/src/main/java/.../activity/*

Implementation steps:
1. Add activity event types if needed.
2. Emit events for each successful activity execution.
3. Include useful metadata such as state deltas or activity reason.

Definition of Done:
- Feed, Play, and Rest actions emit real persistent events.
- Build succeeds.

How to verify:
- Trigger each activity and confirm the Event Viewer shows the corresponding events.

Build command:
- ./gradlew assembleDebug
```

## C133

```md
Task ID: C133
Title: Add visible activity reactions to avatar after Feed, Play, and Rest

Goal:
Ensure activities have immediate visible feedback instead of only hidden state changes.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/brain/src/main/java/.../activity/*
- android-brain/ui-avatar/src/main/java/.../*
- android-brain/app/src/main/java/.../home/HomeScreen.kt

Implementation steps:
1. Add a visible post-activity reaction mapping for each activity.
2. Keep reactions short and emotionally aligned:
   - Feed -> satisfied / happy
   - Play -> excited / happy
   - Rest -> sleepy / calm
3. Integrate with the current avatar rendering flow.

Definition of Done:
- Each activity causes a visible pet reaction.
- Build succeeds.

How to verify:
- Trigger each activity and confirm the avatar visibly responds differently.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 23 — Memory Cards and Diary Foundation

## C134

```md
Task ID: C134
Title: Define MemoryCard domain model for human-readable pet memories

Goal:
Add a human-readable memory layer on top of raw events for diary and memory UI features.

Read first:
- docs/09_pet_app_definition_full.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/memory/src/main/java/.../memorycard/MemoryCard.kt

Implementation steps:
1. Define a MemoryCard model that can represent:
   - title
   - subtitle or summary
   - timestamp
   - source event type
   - optional importance or category
2. Keep it simple and compatible with event-derived generation.

Definition of Done:
- MemoryCard domain model compiles.
- Build succeeds.

How to verify:
- Reference the model from diary mapping code and run a build.

Build command:
- ./gradlew assembleDebug
```

## C135

```md
Task ID: C135
Title: Implement event-to-memory-card mapper for core pet interactions

Goal:
Convert core events into readable memory entries that feel like pet moments instead of raw logs.

Read first:
- docs/09_pet_app_definition_full.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/memory/src/main/java/.../memorycard/EventToMemoryCardMapper.kt

Implementation steps:
1. Map important event types such as:
   - PET_GREETED
   - PET_TAPPED
   - PET_LONG_PRESSED
   - FEED activity
   - PLAY activity
   - REST activity
2. Generate short human-readable summaries.
3. Ignore events that are too low-level for diary display.

Definition of Done:
- Core pet interaction events can be transformed into readable memory cards.
- Build succeeds.

How to verify:
- Feed sample events through the mapper and inspect generated memory cards.

Build command:
- ./gradlew assembleDebug
```

## C136

```md
Task ID: C136
Title: Create Diary screen shell for pet memories

Goal:
Create the diary UI entry point where pet memories will be shown.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/app/src/main/java/.../diary/DiaryScreen.kt
- android-brain/app/src/main/java/.../navigation/*

Implementation steps:
1. Add a Diary screen and route.
2. Show a real empty state when no memory cards exist.
3. Link to the screen from Home or Debug navigation.

Definition of Done:
- Diary screen opens successfully.
- Empty state is clear and not backed by fake data.
- Build succeeds.

How to verify:
- Navigate to Diary screen and confirm the shell works.

Build command:
- ./gradlew assembleDebug
```

## C137

```md
Task ID: C137
Title: Back Diary screen with real memory cards from saved events

Goal:
Populate the diary from actual saved events instead of placeholder data.

Read first:
- docs/09_pet_app_definition_full.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/app/src/main/java/.../diary/DiaryScreen.kt
- android-brain/memory/src/main/java/.../memorycard/*
- android-brain/memory/src/main/java/.../events/*

Implementation steps:
1. Query recent saved events from the existing event store.
2. Map them into MemoryCards.
3. Render the resulting list in the Diary screen.
4. Keep unsupported events filtered out cleanly.

Definition of Done:
- Diary shows real memories derived from actual saved events.
- Build succeeds.

How to verify:
- Trigger several pet interactions and confirm diary entries appear.

Build command:
- ./gradlew assembleDebug
```

## C138

```md
Task ID: C138
Title: Highlight notable pet moments in the diary view

Goal:
Make the diary emotionally useful by surfacing important moments more clearly than routine logs.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/memory/src/main/java/.../memorycard/*
- android-brain/app/src/main/java/.../diary/DiaryScreen.kt

Implementation steps:
1. Add simple notable-moment rules such as:
   - first feed of the day
   - long absence return
   - unusual mood state
2. Mark or style these diary entries differently.
3. Keep logic explainable and deterministic.

Definition of Done:
- Notable moments are visibly distinguished in diary output.
- Build succeeds.

How to verify:
- Trigger qualifying events and confirm the diary highlights them.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 24 — Daily Summary and Lifecycle Continuity

## C139

```md
Task ID: C139
Title: Define DailyPetSummary model and generation contract

Goal:
Introduce a day-level summary model so the pet can feel continuous across repeated sessions.

Read first:
- docs/09_pet_app_definition_full.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/memory/src/main/java/.../summary/DailyPetSummary.kt
- android-brain/memory/src/main/java/.../summary/DailySummaryGenerator.kt

Implementation steps:
1. Define a daily summary model including:
   - date
   - interaction count
   - activities count
   - dominant mood or condition
   - notable moments
2. Define a generation contract from saved events and/or state.

Definition of Done:
- Daily summary types compile.
- Build succeeds.

How to verify:
- Reference the model and generator from app code and build successfully.

Build command:
- ./gradlew assembleDebug
```

## C140

```md
Task ID: C140
Title: Generate daily summary from saved events

Goal:
Create a human-readable daily summary for the pet based on actual saved activity and interaction history.

Read first:
- docs/09_pet_app_definition_full.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/memory/src/main/java/.../summary/DailySummaryGenerator.kt
- android-brain/memory/src/main/java/.../events/*

Implementation steps:
1. Aggregate the current day's relevant saved events.
2. Compute simple counts and dominant themes.
3. Generate a readable daily summary object.
4. Keep logic robust when there are few or no events.

Definition of Done:
- Real daily summaries can be generated from saved events.
- Build succeeds.

How to verify:
- Trigger interactions and activities, then inspect the generated summary through logs or debug UI.

Build command:
- ./gradlew assembleDebug
```

## C141

```md
Task ID: C141
Title: Show today's pet summary in the Diary or Home screen

Goal:
Expose a meaningful daily summary so the user can immediately sense what kind of day the pet has had.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/app/src/main/java/.../home/HomeScreen.kt
- android-brain/app/src/main/java/.../diary/DiaryScreen.kt

Implementation steps:
1. Choose a stable location for today's summary.
2. Bind the UI to the real generated daily summary.
3. Handle empty or low-activity days cleanly.

Definition of Done:
- The app shows a real daily pet summary based on saved data.
- Build succeeds.

How to verify:
- Interact with the pet and confirm the day's summary updates accordingly.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 25 — Personality Traits Foundation

## C142

```md
Task ID: C142
Title: Define PetTrait model and persistence schema

Goal:
Create a simple trait system that allows the pet personality to evolve gradually over repeated interactions.

Read first:
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md

Files likely touched:
- android-brain/brain/src/main/java/.../personality/PetTrait.kt
- android-brain/memory/src/main/java/.../personality/PetTraitEntity.kt
- android-brain/memory/src/main/java/.../personality/PetTraitDao.kt
- android-brain/memory/src/main/java/.../db/AppDatabase.kt

Implementation steps:
1. Define at least these traits:
   - playful
   - lazy
   - curious
   - social
2. Create Room schema for storing trait values.
3. Register the DAO and database schema.

Definition of Done:
- Trait models and persistence compile successfully.
- Build succeeds.

How to verify:
- Save and read trait values through DAO or repository code.

Build command:
- ./gradlew assembleDebug
```

## C143

```md
Task ID: C143
Title: Implement PetTraitRepository with default values

Goal:
Ensure every pet has valid persisted personality trait values.

Read first:
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md

Files likely touched:
- android-brain/brain/src/main/java/.../personality/PetTraitRepository.kt
- android-brain/memory/src/main/java/.../personality/*

Implementation steps:
1. Load trait values from persistence.
2. Create default values if none exist.
3. Expose read and write methods for later update rules.

Definition of Done:
- Fresh app startup has valid personality traits.
- Trait persistence survives restarts.
- Build succeeds.

How to verify:
- Fresh install creates default trait values and relaunch preserves them.

Build command:
- ./gradlew assembleDebug
```

## C144

```md
Task ID: C144
Title: Update traits from repeated interactions and activities

Goal:
Make the pet gradually change personality based on how the user treats it over time.

Read first:
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md

Files likely touched:
- android-brain/brain/src/main/java/.../personality/PetTraitUpdateEngine.kt
- android-brain/brain/src/main/java/.../interaction/*
- android-brain/brain/src/main/java/.../activity/*

Implementation steps:
1. Add simple trait update rules, for example:
   - repeated play increases playful
   - repeated rest increases lazy
   - repeated interactions increase social
2. Keep updates small and cumulative.
3. Persist updated trait values.

Definition of Done:
- Repeated user behavior changes saved trait values over time.
- Build succeeds.

How to verify:
- Repeat several qualifying interactions and inspect trait values in logs or debug UI.

Build command:
- ./gradlew assembleDebug
```

## C145

```md
Task ID: C145
Title: Add personality debug panel for current trait values

Goal:
Expose personality traits visibly so their evolution can be inspected during development.

Read first:
- docs/development_roadmap.md
- docs/06_personality_engine.md

Files likely touched:
- android-brain/app/src/main/java/.../debug/PetTraitDebugPanel.kt
- android-brain/app/src/main/java/.../debug/DebugScreen.kt

Implementation steps:
1. Add a trait debug panel showing current trait values.
2. Bind it to the real trait repository.
3. Keep it readable and stable under updates.

Definition of Done:
- Developers can inspect live persisted trait values in the app.
- Build succeeds.

How to verify:
- Trigger trait-changing behavior and confirm the panel updates accordingly.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 26 — Personality-Aware Behavior Weighting

## C146

```md
Task ID: C146
Title: Implement personality-aware reaction weighting

Goal:
Use persisted traits to influence which reactions the pet prefers, without replacing core state logic.

Read first:
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md

Files likely touched:
- android-brain/brain/src/main/java/.../personality/BehaviorWeightResolver.kt
- android-brain/brain/src/main/java/.../interaction/*
- android-brain/brain/src/main/java/.../activity/*

Implementation steps:
1. Add a resolver that adjusts reaction preference weights using traits.
2. Keep state and condition logic primary; use traits as a secondary modifier.
3. Integrate the weighting into at least one existing reaction path.

Definition of Done:
- Trait values influence at least one real pet reaction path.
- Build succeeds.

How to verify:
- Compare reaction selection under different trait profiles and confirm differences are observable.

Build command:
- ./gradlew assembleDebug
```

## C147

```md
Task ID: C147
Title: Show personality-influenced reaction differences in visible pet behavior

Goal:
Make the personality system user-visible by ensuring different trait profiles can produce different pet behavior.

Read first:
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md

Files likely touched:
- android-brain/brain/src/main/java/.../personality/*
- android-brain/ui-avatar/src/main/java/.../*
- android-brain/app/src/main/java/.../home/HomeScreen.kt

Implementation steps:
1. Choose one or more interaction/reaction flows already implemented.
2. Make the selected visible response vary according to personality weighting.
3. Keep all behavior believable and within the same emotional family.

Definition of Done:
- Different trait profiles can lead to visibly different pet responses.
- Build succeeds.

How to verify:
- Use debug trait values or repeated interactions to create contrasting trait profiles and compare behavior.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 27 — Audio Integration into Pet Core Loop

## C148

```md
Task ID: C148
Title: Integrate sound detection events into pet reaction mapping

Goal:
Make the pet react to real sound-related events from the existing audio pipeline.

Read first:
- docs/09_pet_app_definition_full.md
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../audio/*
- android-brain/brain/src/main/java/.../pet/*
- android-brain/app/src/main/java/.../home/HomeScreen.kt

Implementation steps:
1. Subscribe pet behavior logic to the existing sound-related events from the audio layer.
2. Map sound events into pet-visible reactions such as curious or startled responses.
3. Keep the flow event-driven and independent from cloud or conversation logic.

Definition of Done:
- Real sound input can produce a visible pet reaction.
- Build succeeds.

How to verify:
- Start the audio pipeline, produce sound near the mic, and confirm the avatar reacts.

Build command:
- ./gradlew assembleDebug
```

## C149

```md
Task ID: C149
Title: Apply PetState effects for real sound-reactive behavior

Goal:
Ensure sound reactions change the pet internally instead of remaining a cosmetic-only effect.

Read first:
- docs/09_pet_app_definition_full.md
- docs/08_audio_interaction_architecture.md

Files likely touched:
- android-brain/brain/src/main/java/.../audio/*
- android-brain/brain/src/main/java/.../pet/*

Implementation steps:
1. Define bounded state effects for sound-reactive behavior, such as curiosity, mild social increase, or overstimulation if events are too frequent.
2. Persist the updated state after successful sound reactions.
3. Keep repeated triggers controlled by the existing audio event architecture.

Definition of Done:
- Real sound reactions update and save pet state.
- Build succeeds.

How to verify:
- Generate sound events and inspect state changes in the debug panel.

Build command:
- ./gradlew assembleDebug
```

## C150

```md
Task ID: C150
Title: Emit and persist PET_REACTED_TO_SOUND memory-worthy event

Goal:
Capture sound-based pet reactions as meaningful memory events for diary continuity.

Read first:
- docs/09_pet_app_definition_full.md
- docs/07_robot_memory_system.md
- docs/08_audio_interaction_architecture.md

Files likely touched:
- android-brain/brain/src/main/java/.../events/EventType.kt
- android-brain/brain/src/main/java/.../audio/*
- android-brain/memory/src/main/java/.../memorycard/*

Implementation steps:
1. Add a dedicated event type for pet sound reaction if needed.
2. Emit the event only when the pet actually reacts, not on every low-level sound metric.
3. Ensure the event goes through the existing persistence and memory-card mapping path.

Definition of Done:
- Real sound-reactive pet behavior produces a saved memory-worthy event.
- Diary-compatible mapping path exists.
- Build succeeds.

How to verify:
- Produce a sound trigger, confirm the pet reacts, then inspect Event Viewer or Diary for the resulting event.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 28 — Productization and MVP Readiness

## C151

```md
Task ID: C151
Title: Add simple pet naming onboarding for first launch

Goal:
Allow the user to name the pet on first launch without introducing heavy onboarding complexity.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/app/src/main/java/.../onboarding/*
- android-brain/brain/src/main/java/.../pet/*
- android-brain/app/src/main/java/.../navigation/*

Implementation steps:
1. Detect first launch or missing pet name.
2. Show a simple naming flow.
3. Save the chosen name into the active PetProfile.
4. Return cleanly to the Home screen.

Definition of Done:
- First launch can collect a pet name and persist it.
- Returning launches skip onboarding once completed.
- Build succeeds.

How to verify:
- Fresh install shows naming flow, saves the chosen name, and later launches reuse it.

Build command:
- ./gradlew assembleDebug
```

## C152

```md
Task ID: C152
Title: Add simple pet settings for sound toggle and reset flow

Goal:
Provide minimal product controls needed for a usable MVP.

Read first:
- docs/09_pet_app_definition_full.md

Files likely touched:
- android-brain/app/src/main/java/.../settings/*
- android-brain/core-common/src/main/java/.../settings/*
- android-brain/app/src/main/java/.../navigation/*

Implementation steps:
1. Add a settings screen or section.
2. Include at minimum:
   - sound enabled/disabled
   - reset pet data flow
3. Wire settings to real stored configuration.

Definition of Done:
- User can toggle sound behavior and access a real reset flow.
- Build succeeds.

How to verify:
- Change sound setting and confirm the app honors it.
- Inspect reset entry point presence and basic flow behavior.

Build command:
- ./gradlew assembleDebug
```

## C153

```md
Task ID: C153
Title: Implement real reset flow for pet profile, state, traits, and derived memories

Goal:
Provide a safe way to reset the pet without leaving partial stale data behind.

Read first:
- docs/09_pet_app_definition_full.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/app/src/main/java/.../settings/*
- android-brain/brain/src/main/java/.../pet/*
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Define which tables and data must be reset.
2. Clear pet profile, state, traits, and related pet-specific memories/events as intended by product design.
3. Recreate default pet state/profile cleanly after reset if needed.
4. Add confirmation UX to avoid accidental destructive action.

Definition of Done:
- Reset flow clears the intended pet data without leaving broken app state.
- App remains usable immediately after reset.
- Build succeeds.

How to verify:
- Use the reset flow, confirm pet data is cleared/reinitialized, and confirm app still opens cleanly.

Build command:
- ./gradlew assembleDebug
```

## C154

```md
Task ID: C154
Title: Add MVP stability pass for core pet open-interact-return loop

Goal:
Stabilize the complete MVP loop so the app consistently feels alive and does not regress across the implemented pet features.

Read first:
- docs/09_pet_app_definition_full.md
- docs/development_roadmap.md

Files likely touched:
- only files required to fix issues in the verified pet loop

Implementation steps:
1. Review the full loop:
   - app open greeting
   - tap interaction
   - activities
   - state persistence
   - diary visibility
   - sound reaction if audio tasks are present
2. Fix only stability issues directly blocking that loop.
3. Do not expand scope into new features.

Definition of Done:
- Core MVP loop works reliably end to end.
- Build succeeds.

How to verify:
- Run the full manual loop and confirm all expected visible, persisted, and state-changing steps work.

Build command:
- ./gradlew assembleDebug
```

## C155

```md
Task ID: C155
Title: Add MVP verification checklist screen or document inside the app debug flow

Goal:
Make MVP readiness easy to verify repeatedly without depending on tribal knowledge.

Read first:
- docs/09_pet_app_definition_full.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../debug/MvpChecklistScreen.kt
- android-brain/app/src/main/java/.../navigation/*

Implementation steps:
1. Create a simple verification checklist surface in the app debug flow or generate a maintained static checklist source used by the app.
2. Cover at minimum:
   - open app reaction
   - tap reaction
   - activity actions
   - state persistence
   - diary visibility
   - return-after-time change
3. Keep it lightweight and clearly aligned with the current MVP criteria.

Definition of Done:
- There is a repeatable in-app or repo-adjacent verification checklist for the MVP loop.
- Build succeeds.

How to verify:
- Open the checklist and walk through each step against the running app.

Build command:
- ./gradlew assembleDebug
```

---

# 3. Recommended Execution Order

Execute these batches in order:

1. Batch 17 — Pet Core State Foundation
2. Batch 18 — Emotion Mapping and Greeting
3. Batch 19 — Pet Profile and Core Identity
4. Batch 20 — Tap Interaction Core Loop
5. Batch 21 — Long Press and Interaction Variation
6. Batch 22 — Activities
7. Batch 23 — Memory Cards and Diary Foundation
8. Batch 24 — Daily Summary and Lifecycle Continuity
9. Batch 25 — Personality Traits Foundation
10. Batch 26 — Personality-Aware Behavior Weighting
11. Batch 27 — Audio Integration
12. Batch 28 — Productization and MVP Readiness

---

# 4. Important Notes for Coding Agents

- The pet must remain a **digital creature**, not drift into assistant behavior.
- State changes must be real and persisted when the task requires it.
- The diary must be built from actual events, not fake sample content.
- Personality should remain simple, slow-changing, and explainable.
- Audio integration should remain at the pet-reaction level, not voice-assistant level.
- Every completed task should leave the app in a runnable and verifiable state.

# End of file
