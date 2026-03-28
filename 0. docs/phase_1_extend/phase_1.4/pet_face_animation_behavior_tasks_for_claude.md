# Claude Task Breakdown — Pet Face, Animation, Behavior, and Randomization

Version: v1  
Purpose: Task breakdown ready to hand to Claude for implementation planning/execution.  
Execution style: small, verifiable, build-safe vertical slices.

---

## 0. Global execution wrapper for Claude

Use this wrapper for every task below:

```md
Read and follow `AGENTS.md` first.
Then read only the documents listed in "Read first".
Implement only this task.
Do not add mock production logic.
Do not leave TODOs, empty methods, or placeholder business logic.
Do not silently expand into adjacent tasks.
Keep the build green.
Preserve offline-first, event-driven, modular architecture.
Run the required build command before finishing.
Report exactly:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

---

## 1. Task strategy

### Why this breakdown exists
Phần face + animation + behavior rất dễ bị Claude làm quá tay: đụng Home, avatar, emotion mapping, state, events, audio, bubble, debug. Nếu không chia nhỏ, rủi ro lớn nhất là:
- scope creep
- UI đẹp nhưng không nối state thật
- animation nhiều nhưng random vô nghĩa
- phá Home hiện tại hoặc phá build

### Mandatory implementation rules
- Mỗi task phải tạo kết quả nhìn thấy được hoặc verify được.
- Nếu task nói có continuity/state/event thì phải đi qua flow thật.
- Không được dùng fake sample content thay cho behavior production.
- Không chuyển pet thành chatbot UI.
- Manual debug controls chỉ được tồn tại nếu không phá pet flow chính.

---

## 2. Recommended execution order

Execute in this order:
1. FA-01 Face and animation contracts
2. FA-02 Expand PetEmotion / avatar mapping readiness
3. FA-03 Home pet stage productization shell
4. FA-04 Talking bubble foundation
5. FA-05 Greeting reaction visual pack
6. FA-06 Tap reaction visual pack
7. FA-07 Activity reaction pack
8. FA-08 Idle animation runtime and bounded variation
9. FA-09 Sound-reaction visual integration
10. FA-10 Trait-biased weighting for animation selection
11. FA-11 Debug visibility for animation reasoning
12. FA-12 Final stability and quality pass

---

# Batch FA-A — Contracts and Face System Foundation

## FA-01

```md
Task ID: FA-01
Title: Define canonical face/emotion presentation contracts for the avatar layer

Goal:
Create the face presentation contracts that separate dominant pet emotion, animation intent, animation variant, and optional message-bubble content so later animation work does not become ad-hoc UI state.

Read first:
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md
- docs/development_roadmap.md

Scope:
- Contract/model layer only.
- Do not yet redesign Home or implement many animations.
- Keep the design compatible with the existing avatar system.

Files likely touched:
- android-brain/ui-avatar/src/main/java/.../model/*
- android-brain/brain/src/main/java/.../pet/*

Implementation steps:
1. Add a canonical model for visible pet presentation state.
2. Include at minimum:
   - dominant visible emotion
   - optional micro-state/modifier
   - animation intent
   - animation variant id
   - optional talking-message payload/model
3. Keep the model reusable across greeting, tap, activities, idle, and sound reactions.
4. Ensure the app compiles with the new contracts.

Definition of Done:
- Presentation models compile.
- The models are usable from app, brain, and avatar layers.
- Build succeeds.

How to verify:
- Reference the contracts from Home or preview/debug code and run a build.

Build command:
- ./gradlew assembleDebug
```

## FA-02

```md
Task ID: FA-02
Title: Expand avatar-facing emotion model and map it cleanly from real pet state

Goal:
Ensure the pet can express the full MVP-visible emotional range through the existing state/emotion pipeline, not only through manual debug states.

Read first:
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md
- docs/01_codex_tasks_pet_app_full.md

Scope:
- Real mapping only.
- Do not add uncontrolled randomness.
- Keep the mapping stable and explainable.

Files likely touched:
- android-brain/brain/src/main/java/.../pet/model/*
- android-brain/brain/src/main/java/.../pet/PetEmotionResolver.kt
- android-brain/ui-avatar/src/main/java/.../*

Implementation steps:
1. Review the current PetEmotion and avatar mapping path.
2. Expand or refine the model so the visible set can cover at least:
   - IDLE
   - HAPPY
   - CURIOUS
   - SLEEPY
   - SAD
   - EXCITED
   - HUNGRY
3. Ensure the resolver uses current PetState and derived conditions.
4. Keep manual controls secondary and avoid overriding the real flow.

Definition of Done:
- Avatar-facing emotion is derived from real pet state.
- The visible emotion range is sufficient for later animation work.
- Build succeeds.

How to verify:
- Feed sample or real states and confirm the resolved visible emotion changes correctly.

Build command:
- ./gradlew assembleDebug
```

---

# Batch FA-B — Home Productization Shell

## FA-03

```md
Task ID: FA-03
Title: Productize the Home pet stage shell while preserving the real pet loop

Goal:
Refactor the Home screen layout into a pet-first product shell where the avatar stage is dominant, state indicators stay lightweight, quick actions remain simple, and debug clutter is removed from the main surface.

Read first:
- docs/09_pet_app_definition_full.md
- docs/development_roadmap.md

Scope:
- Layout/productization only.
- Do not yet add the full bubble or animation runtime.
- Preserve current real state/event flow.

Files likely touched:
- android-brain/app/src/main/java/.../home/HomeScreen.kt
- android-brain/app/src/main/java/.../home/*
- android-brain/app/src/main/java/.../debug/*

Implementation steps:
1. Make the avatar/pet stage the visual focal point.
2. Keep only lightweight state indicators and simple quick actions.
3. Reduce Home debug controls down to one subtle debug/demo entry.
4. Ensure the current real pet data still feeds the Home UI.

Definition of Done:
- Home no longer looks like a demo/control screen.
- Pet stage is visually dominant.
- Only one subtle debug entry remains on Home.
- Build succeeds.

How to verify:
- Launch the app and inspect Home visually.
- Confirm the current pet flow still loads.

Build command:
- ./gradlew assembleDebug
```

---

# Batch FA-C — Talking Bubble System

## FA-04

```md
Task ID: FA-04
Title: Create reusable talking-message bubble UI and runtime policy

Goal:
Add a reusable bubble component and runtime policy so the pet can show temporary on-screen talking messages without turning the app into a chat UI.

Read first:
- docs/09_pet_app_definition_full.md
- docs/development_roadmap.md

Scope:
- Bubble component + runtime policy only.
- Do not invent fake production messages.
- Keep it ready for real greeting/tap/activity/sound flows.

Files likely touched:
- android-brain/app/src/main/java/.../home/*
- android-brain/ui-avatar/src/main/java/.../*
- android-brain/brain/src/main/java/.../pet/*

Implementation steps:
1. Add a reusable talking bubble UI component anchored to the pet stage.
2. Add show/hide timing and smooth enter/exit animation.
3. Add a simple bounded policy:
   - cooldown
   - replace or queue rule
   - no overlap chaos
4. Wire the component to accept a real message payload from later resolvers.

Definition of Done:
- Talking bubble UI exists and can be driven from real flows.
- Bubble timing and dismissal are stable.
- Build succeeds.

How to verify:
- Trigger the bubble from a controlled real path or temporary integration path and confirm stable appearance/disappearance.

Build command:
- ./gradlew assembleDebug
```

## FA-05

```md
Task ID: FA-05
Title: Wire app-open greeting flow to visible talking bubbles and greeting presentation

Goal:
Make app-open greeting visibly feel alive by combining the existing greeting logic with a real greeting bubble and greeting-oriented avatar presentation.

Read first:
- docs/09_pet_app_definition_full.md
- docs/07_robot_memory_system.md
- docs/01_codex_tasks_pet_app_full.md

Scope:
- Use the real greeting path.
- Do not fake greeting text unrelated to state.
- Keep the greeting brief and state-aware.

Files likely touched:
- android-brain/brain/src/main/java/.../pet/PetGreetingResolver.kt
- android-brain/app/src/main/java/.../startup/*
- android-brain/app/src/main/java/.../home/*
- android-brain/ui-avatar/src/main/java/.../*

Implementation steps:
1. Review the existing greeting path.
2. Add a real presentation mapping from greeting result to:
   - visible greeting animation intent
   - optional greeting bubble text
3. Show the bubble on Home when the app-open greeting fires.
4. Keep event emission/persistence behavior intact.

Definition of Done:
- App open feels like an immediate pet reaction.
- Greeting bubble text comes from a real resolver/path.
- Existing greeting event flow remains intact.
- Build succeeds.

How to verify:
- Open the app under different state conditions and confirm greeting reaction/bubble differences.

Build command:
- ./gradlew assembleDebug
```

---

# Batch FA-D — Authored Animation Packs

## FA-06

```md
Task ID: FA-06
Title: Implement the first authored visual reaction pack for tap interactions

Goal:
Create a small but real tap reaction pack so successful taps produce visibly different pet responses based on current state instead of one generic reaction.

Read first:
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md
- docs/01_codex_tasks_pet_app_full.md

Scope:
- Tap reactions only.
- Use the real tap path.
- Keep cooldown and state awareness intact.

Files likely touched:
- android-brain/brain/src/main/java/.../interaction/*
- android-brain/ui-avatar/src/main/java/.../animation/*
- android-brain/app/src/main/java/.../home/*

Implementation steps:
1. Review the current tap interaction path.
2. Add at least 3 visibly distinct tap reaction variants for different state bands, for example:
   - happy/normal tap acknowledgement
   - sleepy low-energy acknowledgement
   - attached/warm acknowledgement
3. Map real tap outcomes into the appropriate presentation intent.
4. Keep repeated tap spam bounded.

Definition of Done:
- Tapping the pet produces a visible real reaction from the production path.
- The reaction is not identical in every relevant state.
- Build succeeds.

How to verify:
- Tap the avatar in multiple state conditions and compare the visible results.

Build command:
- ./gradlew assembleDebug
```

## FA-07

```md
Task ID: FA-07
Title: Implement authored reaction packs for Feed, Play, and Rest activities

Goal:
Make the pet react visibly and distinctly to core activities using real state-changing activity flows.

Read first:
- docs/09_pet_app_definition_full.md
- docs/development_roadmap.md
- docs/01_codex_tasks_pet_app_full.md

Scope:
- Feed/Play/Rest only.
- Use real activity flows and persistence.
- Do not invent activity UI unrelated to the current Home product direction.

Files likely touched:
- android-brain/brain/src/main/java/.../activity/*
- android-brain/ui-avatar/src/main/java/.../animation/*
- android-brain/app/src/main/java/.../home/*

Implementation steps:
1. Review the real Feed/Play/Rest path.
2. Add distinct presentation mappings for each activity.
3. Optionally attach a real short bubble if the activity resolver already supports it cleanly.
4. Ensure resulting state/UI refresh remains correct.

Definition of Done:
- Each activity has a distinct visible pet reaction.
- Activity effects remain real and persisted where expected.
- Build succeeds.

How to verify:
- Trigger Feed/Play/Rest and confirm the pet response is visually distinct and state-aware.

Build command:
- ./gradlew assembleDebug
```

---

# Batch FA-E — Idle Runtime and Bounded Randomization

## FA-08

```md
Task ID: FA-08
Title: Add multi-layer idle runtime with bounded variation and anti-repeat rules

Goal:
Make the pet feel alive while idle by adding a controlled idle runtime that chooses among compatible idle variants without causing chaotic randomness.

Read first:
- docs/09_pet_app_definition_full.md
- docs/06_personality_engine.md

Scope:
- Idle runtime only.
- Do not yet bias by long-term traits unless necessary.
- Do not allow idle logic to override active greeting/reaction flows.

Files likely touched:
- android-brain/ui-avatar/src/main/java/.../animation/*
- android-brain/brain/src/main/java/.../pet/*
- android-brain/app/src/main/java/.../home/*

Implementation steps:
1. Add a small pool of authored idle variants compatible with the current visible emotion families.
2. Add a lightweight selector that uses:
   - current visible emotion
   - state compatibility
   - recent animation history
   - cooldown and anti-repeat rules
3. Keep the result debuggable and bounded.
4. Do not interrupt active reactions in a jarring way.

Definition of Done:
- Idle behavior shows subtle variation over time.
- Repetition is reduced without creating chaos.
- Incompatible variants do not appear in wrong state bands.
- Build succeeds.

How to verify:
- Leave Home open for 30–60 seconds and observe varied but bounded idle behavior.

Build command:
- ./gradlew assembleDebug
```

## FA-09

```md
Task ID: FA-09
Title: Integrate sound-reaction presentation with attentive/startle visual variants

Goal:
Make real sound events produce meaningful pet visuals on Home without turning the app into a voice assistant.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/09_pet_app_definition_full.md
- docs/development_roadmap.md

Scope:
- Visual reaction only for real sound events.
- Do not expand into ASR/TTS/conversation.
- Respect audio cooldown/boundedness.

Files likely touched:
- android-brain/perception/src/main/java/.../audio/*
- android-brain/brain/src/main/java/.../pet/*
- android-brain/ui-avatar/src/main/java/.../animation/*
- android-brain/app/src/main/java/.../home/*

Implementation steps:
1. Review the current sound event path.
2. Add presentation mapping for at least:
   - attentive/listening reaction
   - small startle reaction
3. Keep the visual response short and non-spammy.
4. Optionally attach a small bubble only if the event path already supports it cleanly.

Definition of Done:
- Real sound events can cause visible pet reactions.
- The reaction stays emotional, not conversational.
- Build succeeds.

How to verify:
- Trigger real sound input and confirm the pet responds visually without spamming.

Build command:
- ./gradlew assembleDebug
```

---

# Batch FA-F — Personality Bias and Debuggability

## FA-10

```md
Task ID: FA-10
Title: Add trait-biased weighting for compatible animation and reaction selection

Goal:
Allow personality traits to bias reaction and idle variant selection without overriding the core state-driven logic.

Read first:
- docs/06_personality_engine.md
- docs/09_pet_app_definition_full.md

Scope:
- Bias only.
- No hard override of base logic.
- Keep the implementation explainable and small.

Files likely touched:
- android-brain/brain/src/main/java/.../pet/*
- android-brain/ui-avatar/src/main/java/.../animation/*

Implementation steps:
1. Review the current trait or personality data available.
2. Add small weighting adjustments for compatible variants, for example:
   - playful -> more playful/excited variants
   - lazy -> calmer/rest-like variants
   - affectionate -> warmer greeting/tap variants
   - curious -> more attentive/peek variants
3. Keep trait effects slow and bounded.
4. Ensure incompatible variants remain blocked by state rules.

Definition of Done:
- Traits bias selection where relevant.
- Base state logic still dominates.
- Build succeeds.

How to verify:
- Compare variant tendencies under different trait conditions and confirm only weighting, not hard replacement.

Build command:
- ./gradlew assembleDebug
```

## FA-11

```md
Task ID: FA-11
Title: Add debug visibility for animation intent, chosen variant, and bounded-random reasoning

Goal:
Make the new animation system debuggable so future tuning does not devolve into guesswork.

Read first:
- docs/09_pet_app_definition_full.md
- docs/development_roadmap.md

Scope:
- Debug visibility only.
- Do not clutter the main Home surface.
- Reuse the existing debug path where possible.

Files likely touched:
- android-brain/app/src/main/java/.../debug/*
- android-brain/brain/src/main/java/.../pet/*
- android-brain/ui-avatar/src/main/java/.../animation/*

Implementation steps:
1. Surface at minimum:
   - dominant visible emotion
   - current animation intent
   - chosen animation variant
   - recent variant history
   - optional selection reason tags
2. Keep this visible in debug flow, not as a main Home control panel.
3. Ensure data comes from the real runtime.

Definition of Done:
- Developers can inspect runtime animation decisions.
- Debug info reflects the real running state.
- Build succeeds.

How to verify:
- Open the debug flow while the pet is running and confirm the reported values change meaningfully.

Build command:
- ./gradlew assembleDebug
```

---

# Batch FA-G — Final Stability Pass

## FA-12

```md
Task ID: FA-12
Title: Stability and polish pass for the full face-animation-behavior presentation loop

Goal:
Stabilize the complete Home presentation loop so the pet consistently feels alive without regressions across greeting, tap, activities, sound reaction, idle variation, and talking bubbles.

Read first:
- docs/09_pet_app_definition_full.md
- docs/development_roadmap.md
- docs/06_personality_engine.md

Scope:
- Fix only issues blocking quality and stability in the verified loop.
- Do not expand scope into new product features.

Files likely touched:
- only files required to fix issues in the verified presentation loop

Implementation steps:
1. Review the complete visible loop:
   - app open greeting
   - Home pet stage
   - talking bubbles
   - tap reactions
   - activity reactions
   - idle bounded variation
   - sound visual reactions if present
   - debug visibility
2. Fix issues such as:
   - jarring transitions
   - spammy bubbles
   - wrong-state animation selection
   - repetition too frequent
   - reaction interruption bugs
3. Preserve the architecture and verified flows.

Definition of Done:
- The visible pet loop feels stable and product-like.
- Animation behavior remains state-aware and bounded.
- Build succeeds.

How to verify:
- Run a full manual pass through open/tap/activity/idle/sound/return flows and confirm the visible experience remains coherent and alive.

Build command:
- ./gradlew assembleDebug
```

---

## 3. Short review checklist for Claude after each task

Before reporting done, Claude should confirm:
- task changed a real slice of the pet loop
- visible result exists
- state/event integration is still correct where required
- randomness is bounded, not freeform
- Home remains pet-first
- no fake production logic was introduced
- build was actually run

---

## 4. Final note

If Claude sees that the current codebase lacks prerequisite infrastructure for a later task, it should not silently implement the whole future system. It should complete only the smallest safe vertical slice needed for the current task and report the remaining risk clearly.
