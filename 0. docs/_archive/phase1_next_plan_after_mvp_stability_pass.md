# AI Pet Robot — Next Phase 1 Plan After `mvp_stability_pass_plan`

Version: v1  
Date: 2026-04-04  
Purpose: Define the **next implementation plan** now that `mvp_stability_pass_plan` is complete, keeping the project focused on **finishing Phase 1 as a believable digital pet**, not drifting into cloud AI or robot-body work.

---

## 1. Executive conclusion

The next step is **not** Cloud AI, LLM conversation, or robot body integration.

The next step is the **final visible Phase 1 push**:

> **Connect the existing BehaviorEngine v2 and current pet-state/evolution systems into the actual home-screen pet experience so the pet visibly feels alive.**

This is the highest-leverage move because the project already has substantial real systems implemented — behavior planning, attention, emotion momentum, audio perception, greeting flow, state decay, persistence, diary, traits, and evolution — but the documents say the core remaining gap is still **connection + expression + behavior loop**.

---

## 2. Why this is the next step

The current authoritative project sync says the app is already an advanced Phase 1 Android Pet Brain with real perception, real persistence, and a strong product UI, but the biggest missing piece is that **BehaviorEngine v2 does not yet drive the avatar/home signal chain**. The document explicitly marks **“Connect BehaviorEngine v2 to avatar signal chain”** as the highest-priority next implementation task, because it makes existing invisible behavior work visible without adding new major logic. It also identifies state visibility, missing teach-object UX, evolution feedback verification, and avatar-module confusion as the key remaining gaps. The same document says not to mix this task with broad refactors or new unrelated systems.

The final Phase 1 completion guide says Phase 1 is only done when the pet feels alive through four conditions: **instant life on app open, full interaction loop, alive idle behavior, and meaningful time passage**. It identifies the blockers as: **Behavior → Avatar binding**, **Idle Life System**, **State Visibility**, **Greeting System v2**, and **Variation Engine**.

The Pet App product definition reinforces the same direction: the core loop is open app → pet reacts immediately → user interacts → pet reacts with animation/audio/state change → memory recorded → time passes → user returns and the pet is meaningfully different. It also requires each interaction to produce visible reaction, state change, and memory event, while audio remains emotional rather than conversational.

---

## 3. Strategic goal

## Goal
Complete the **final experiential integration layer** for Phase 1 so the home pet behaves like a living creature rather than a set of isolated subsystems.

## Success statement
When the user opens the app, taps the pet, waits idly, leaves for hours, returns, or exposes the pet to sound/camera stimuli, the pet should show:

- a visible intention
- clear attention and urgency shifts
- meaningful state-driven expression
- bounded variation
- continuity over time
- observable but non-debuggy emotional feedback

---

## 4. Strict scope boundary

## In scope

1. **BehaviorEngine v2 → Avatar/Home binding**
2. **Idle life system** on the home screen
3. **State → expression mapping improvements**
4. **Greeting system v2**
5. **Variation and anti-repeat hardening**
6. **Teach-object UX flow**
7. **Evolution feedback loop verification and surfacing**
8. **Phase 1 cleanup gates** directly required for the above

## Out of scope

- Cloud AI / LLM chat
- Full voice conversation
- Robot body / BLE / ESP32 work
- New perception engines unless strictly required by a gap above
- Big architecture rewrite
- Broad `PetBrainApp.kt` refactor during the main integration tasks
- New mini-games or side features
- Advanced object embeddings or scene understanding

---

## 5. Phase 1 final push structure

## Stage A — Behavior Visibility Integration (highest priority)

### Objective
Make the current behavior planner visibly control the pet on the home screen.

### What must happen
- Wire `BehaviorPlan` / intention / attention / urgency into `HomePixelPetAvatarSignal` or the equivalent active avatar signal pipeline.
- Ensure the avatar reflects:
  - intention
  - attention target
  - stimulus type
  - urgency / arousal level
- Respect current priority rules so greeting, transient reactions, and higher-priority states still work.
- Do **not** add new major behavior logic; expose the logic that already exists.

### Expected user result
The pet should feel more attentive, more purposeful, and less like it is only reacting from a simple emotion resolver.

---

## Stage B — Idle Life System

### Objective
Ensure the pet never feels dead when the user is not interacting.

### What must happen
- Add an idle scheduler with randomized but bounded intervals.
- Trigger low-intensity idle actions such as:
  - look around
  - micro attention shifts
  - curious glance
  - subtle reaction changes
  - idle event emission
- Prevent idle spam with cooldowns and anti-repeat guards.
- Keep idle behavior subordinate to higher-priority interaction/greeting/reaction states.

### Expected user result
After 10–30 seconds, the pet still feels alive, observant, and present.

---

## Stage C — State Visibility on Main Experience

### Objective
Let the user feel internal state from the main screen without opening debug panels.

### What must happen
- Surface state through expressive behavior, not stat bars.
- Map at least:
  - hunger → animation/audio cue
  - energy → reaction speed / softness / activity level
  - sleepiness → eyes / blink / idle posture
  - bond → greeting warmth / enthusiasm / closeness
  - loneliness/social need → subtle mood/ambience/behavior bias
- Add one lightweight main-screen signal if needed (for example mood-driven ambiance or state-health styling), but do not turn Home into a dashboard.

### Expected user result
The user should be able to infer “hungry”, “sleepy”, “playful”, “attached”, or “needs attention” from the pet itself.

---

## Stage D — Greeting System v2

### Objective
Turn app-open into an emotional moment rather than a generic greeting.

### What must happen
- Resolve greeting from:
  - time gap since last open
  - current state
  - mood/emotion
  - bond/relationship
  - last meaningful interaction if already available
- Produce a coordinated bundle:
  - visible animation/reaction
  - optional audio clip
  - talk bubble/message
  - event log
- Preserve perceived startup responsiveness.

### Expected user result
Opening the app should immediately communicate that the pet remembers time has passed and has a current emotional condition.

---

## Stage E — Variation Engine / Anti-Repetition

### Objective
Stop same-state outputs from feeling repetitive.

### What must happen
- Add bounded weighted variation for repeated greetings, idle actions, and common reactions.
- Strengthen anti-repeat guards.
- Keep selection explainable and stable.
- Ensure variation biases outcomes without breaking state meaning.

### Expected user result
The pet should feel naturally variable, not random and not repetitive.

---

## Stage F — Teach-Object UX Completion

### Objective
Close the missing object-learning loop that is still absent in user-facing flow.

### What must happen
- Implement a real teach-object-by-name UX analogous in spirit to teach-person.
- Use the current object detection + DB flow; do not add heavy object embeddings.
- Support naming and persisting known objects.
- Allow later behavioral hooks such as toy/food semantics.

### Expected user result
The pet can learn and later meaningfully react to named objects in a user-visible way.

---

## Stage G — Evolution Feedback Verification

### Objective
Verify that the evolution system creates visible effects across sessions.

### What must happen
- Verify end-to-end loop from episodes / trait updates / bond changes / inferred facts → visible behavior or expression differences.
- Add minimal visible surfacing only where needed to confirm the loop is real.
- Document verification steps and evidence.

### Expected user result
Over repeated use, the pet should actually feel different, not merely store hidden data.

---

## Stage H — Phase 1 Close-out Hardening

### Objective
Remove just enough ambiguity/risk to safely declare Phase 1 done.

### What must happen
- Formally mark `pixel-avatar` as legacy/deprecated if the active runtime is `ui-avatar`.
- Avoid touching both avatar systems by accident in follow-up work.
- Add or update a final Phase 1 verification checklist.
- Only perform cleanup directly required for Phase 1 completion confidence.

### Must not happen here
- No giant refactor of `PetBrainApp.kt` as part of feature delivery.
- No architectural cleanup project disguised as Phase 1 completion.

---

## 6. Recommended implementation order

1. **BehaviorEngine v2 → avatar signal chain**
2. **Idle life system**
3. **State visibility on Home**
4. **Greeting system v2**
5. **Variation / anti-repeat hardening**
6. **Teach-object UX**
7. **Evolution feedback verification**
8. **Avatar-system deprecation marker + Phase 1 final checklist**

Reason:
- Steps 1–5 directly solve the documented blockers to “pet feels alive”.
- Step 6 closes the biggest missing functional Phase 1 loop.
- Steps 7–8 make Phase 1 believable, verifiable, and closeable.

---

## 7. What Phase 1 should look like when this plan is done

Phase 1 should be considered done when all of these are true:

- Open app → pet reacts immediately with state-aware greeting
- Tap / activity / sound / relevant perception events → pet visibly reacts, state changes, and event is logged
- Idle pet remains alive without manual input
- Return after hours → pet is meaningfully different
- Main screen communicates state through pet behavior and ambiance
- BehaviorEngine v2 visibly matters
- Teach-object flow is user-facing and real
- Evolution/traits/bond changes produce observable differences over time
- The product still feels like a **digital creature**, not a chatbot or dashboard

---

## 8. Delivery rule for agents

For the next execution cycle, every task should preserve these rules:

- build-safe after each task
- no TODO / no fake production logic
- no unrelated refactor
- no Cloud AI
- no robot body work
- always prefer a **small complete vertical slice** that changes the real pet experience

---

## 9. Final recommendation

If you want one sentence to guide the next batch, use this:

> **Finish Phase 1 by making the existing brain visibly drive the pet.**

That is the shortest path from “technically rich app” to “pet that feels alive”.
