# Next Phase Design — Behavior-to-Experience Binding System

Version: v1  
Status: Active implementation design  
Target quality: Production-ready, no POC shortcuts  
Project: AI Pet Robot / Android Pet App

---

# 1. Purpose of This Phase

This phase exists to solve the most important gap in the current product:

> the app already has strong internal systems, but the pet's visible experience is still not driven primarily by those systems.

The project already contains real perception, persistence, audio capture, Vosk-based keyword/ASR, a real pet state model, personality traits, a diary pipeline, and an advanced `BehaviorEngine v2`. However, the visible pet experience is still dominated by direct state mapping and event-local reactions rather than by a single authoritative behavioral layer.

That creates a product problem:

- the pet is technically capable,
- but it does not yet feel as alive as it should,
- because internal intention is not consistently expressed through avatar, audio, talk bubble, pacing, and reaction sequencing.

This phase fixes that.

The goal is not to add more features. The goal is to make the existing pet **feel alive, coherent, and intentional**.

---

# 2. Strategic Decision

## 2.1 Why this phase comes now

Based on the current project sync status, the app is already an advanced Phase 1 Android pet system with:

- full-screen pet home experience,
- persisted pet state with decay and lifecycle,
- face detection / recognition / teach-person flow,
- object detection,
- audio perception and pre-recorded audio playback,
- evolution system persistence,
- `BehaviorEngine v2`,
- strong debug tooling,
- diary and trait systems.

But the sync status also identifies the most important gap:

- `BehaviorEngine v2` generates plans and intentions,
- yet it does not directly drive the avatar/audio experience,
- and the home avatar is still primarily driven by `PetEmotionResolver` and `PetConditionResolver` instead of by the behavioral layer.

That means the project risks becoming a technically strong but emotionally weak pet product. The correct next move is therefore to bind behavior to visible and audible experience before adding more major features. This aligns with the manifest and product definition: emotion, behavior, and believability come before higher intelligence or body hardware. fileciteturn2file5 fileciteturn2file0 fileciteturn2file14

## 2.2 Product principle

The product definition is explicit:

- the pet is a **digital creature**, not a chatbot or assistant,
- the core experience loop is: open app → pet reacts immediately → user interacts → pet reacts visibly/audibly → state changes → memory is recorded → time passes → pet is meaningfully different on return,
- the app succeeds when the pet feels alive, not when it exposes the most technical systems.

Therefore this phase must strengthen the product loop, not expand scope sideways. fileciteturn2file14

---

# 3. Phase Goal

## 3.1 Primary goal

Create a production-ready **Behavior-to-Experience Binding System** that makes `BehaviorEngine v2` the authoritative driver of:

- visible pet reactions,
- animation intent,
- audio response selection,
- talk bubble timing and messaging priority,
- micro-pacing of interaction,
- interruption and cooldown rules,
- layered reaction sequencing.

## 3.2 What success looks like

When the user opens the app or interacts with the pet:

- the pet does not just change expression,
- the pet appears to **decide** how to react,
- the reaction has a beginning, peak, and settle,
- visual state, audio, and talk bubble feel like one event,
- repeated interactions do not feel robotic or spammy,
- the same pet state can still produce slightly different but believable reactions,
- debug tooling can explain why the pet reacted that way.

## 3.3 Non-goal

This phase is **not** for:

- full conversation,
- LLM integration,
- object teaching UI,
- body hardware,
- broad DI refactor,
- replacing Room or navigation,
- redesigning the whole home screen,
- inventing a second behavior engine.

---

# 4. Problem Statement

## 4.1 Current state problem

The current implementation already has these strengths:

- strong pet state,
- real lifecycle decay,
- advanced pixel animation runtime,
- reaction orchestration,
- audio playback categories,
- transient reaction controller,
- event-driven architecture.

But the product still has a structural gap:

```text
Perception / Memory / State
        ↓
BehaviorEngine v2
        ↓
   BehaviorPlan
        ↓
  (not the main driver)
        ↓
PetEmotionResolver / local UI reaction logic
        ↓
Avatar + talk bubble + audio
```

This creates five concrete issues:

1. **Behavior is not authoritative**  
   The most advanced internal decision layer is not the main source of visible pet behavior.

2. **Experience channels are only partially coordinated**  
   Avatar, audio, and talk bubble are not consistently produced from one intention object.

3. **Reaction sequencing is fragmented**  
   Some reactions are state-driven, some event-driven, some transient, some hardcoded in screen logic.

4. **Believability is capped**  
   The pet can look expressive, but not yet deeply intentional.

5. **Debug explainability is weaker than it should be**  
   Internal behavior reasoning exists, but the player-visible result is not cleanly mapped from it.

## 4.2 Product risk if not solved now

If this gap is not solved before Phase 2 or Phase 3:

- cloud AI will make the system more complex without making the pet feel more alive,
- physical hardware will amplify awkward or incoherent reactions,
- more event handlers will accumulate in the app layer,
- `PetBrainApp.kt` will continue to grow as the accidental orchestration layer,
- the product will feel like a bundle of systems instead of a creature.

---

# 5. Design Principle for This Phase

## 5.1 Single behavioral authority

There must be one primary place where the pet decides:

- what it is trying to do,
- how strongly,
- for how long,
- whether it can interrupt current behavior,
- what expression/audio/message bundle should accompany that intent.

That place is `BehaviorEngine v2` plus a new execution/binding layer, not `HomeScreen`, not the avatar composable, and not ad hoc event branches.

## 5.2 Experience is a bundle, not isolated outputs

Every meaningful pet reaction should be represented as a coordinated bundle:

- **visual directive**
- **audio directive**
- **speech/talk directive**
- **timing / duration / settle rules**
- **priority / interruption policy**
- **cooldown / anti-repeat metadata**

## 5.3 Behavior should be legible

The user should feel:

- “the pet noticed me,”
- “the pet considered how to react,”
- “the pet responded in a way that matches its mood and history.”

Not:

- “the screen swapped states,”
- “a sound happened,”
- “the app randomly played an animation.”

## 5.4 Strong fallback path

The system must remain stable if behavior execution cannot produce a rich plan.

Fallback order:

1. behavior-driven experience bundle,
2. simplified behavior-derived emotion mapping,
3. existing `PetEmotionResolver` / `PetConditionResolver` fallback.

That preserves stability while migrating authority.

---

# 6. Phase Scope

## 6.1 In scope

This phase includes:

- new intention-to-experience domain model,
- behavior output mapping into executable experience bundles,
- production `PetIntentionExecutor` / `BehaviorExperienceBinder`,
- authoritative sequencing for avatar + audio + talk bubble,
- interruption / priority / cooldown rules,
- anti-repeat at experience level,
- debug visibility for “why this reaction happened,”
- wiring behavior-driven reactions into Home experience,
- preserving current state-based fallback behavior.

## 6.2 Out of scope

This phase does **not** include:

- redesigning event persistence,
- new Room schemas unless absolutely needed for diagnostics,
- refactoring all DI to Hilt/Koin,
- teaching objects by name,
- changing camera UX,
- new mini-games,
- new cloud services,
- robot body integration,
- replacing manual navigation.

---

# 7. Target Architecture After This Phase

## 7.1 Desired high-level flow

```text
Perception / Interaction / Lifecycle Events
                    ↓
             BehaviorEngine v2
                    ↓
         BehaviorPlan / PetIntention
                    ↓
     Behavior-to-Experience Binding Layer
                    ↓
      Experience Bundle / Execution Policy
                    ↓
 Avatar Runtime + Audio Response + Talk Bubble
                    ↓
 Visible pet reaction with settle / cooldown
```

## 7.2 New architectural layer

Introduce a new layer conceptually located between `BehaviorEngine v2` and the home presentation runtime:

- **Behavior-to-Experience Binding Layer**

Suggested implementation names:

- `PetIntentionExecutor`
- `BehaviorExperienceBinder`
- `ExperienceDirectiveResolver`
- `PetReactionExecutionCoordinator`

Naming can be finalized during implementation, but responsibilities must remain clear.

## 7.3 Why a new layer is needed

`BehaviorEngine v2` should not know the details of:

- specific animation clips,
- talk bubble dedupe policy,
- audio category arbitration,
- UI timing primitives.

At the same time, `HomeScreen` must not decide behavior semantics.

So we need a dedicated mapping/execution layer that:

- consumes behavior output,
- resolves it into user-visible directives,
- enforces product rules,
- exposes debug traces.

---

# 8. Proposed Domain Model

## 8.1 Core concept: Pet intention

The binding layer should work from a clean intention object rather than raw event fragments.

Suggested conceptual model:

```text
PetIntention
- intentionType
- sourceReason
- target
- emotionalTone
- urgency
- confidence
- noveltyLevel
- persistence
- interruptibility
- createdAt
- expiresAt?
```

Example intention types:

- `GREET`
- `SEEK_ATTENTION`
- `ACKNOWLEDGE_TOUCH`
- `PLAYFUL_RESPONSE`
- `CURIOUS_LOOK`
- `STARTLE`
- `RESTING_SETTLE`
- `HUNGRY_REQUEST`
- `LISTENING_ATTEND`
- `THINKING_PAUSE`
- `BONDING_WARMTH`

This model may already partially exist in the behavior engine. The task is not to duplicate it, but to standardize the execution-facing shape.

## 8.2 Experience bundle

Suggested conceptual model:

```text
PetExperienceBundle
- visualDirective
- audioDirective?
- talkDirective?
- durationMs
- settleDirective?
- priority
- canInterruptCurrent
- cooldownKey?
- antiRepeatKey?
- debugReason
```

### Visual directive

```text
VisualDirective
- reactionType or animationIntent
- targetVisualState
- intensity
- holdMs
- transitionProfile
- allowBlinkOverride
- allowIdleSuppression
```

### Audio directive

```text
AudioDirective
- category
- clipStrategy (specific / weighted-random / none)
- priority
- cooldownKey
- selfSuppressWindowMs
```

### Talk directive

```text
TalkDirective
- text or messageToken
- priority
- dedupeKey
- maxDisplayMs
- interruptPolicy
```

## 8.3 Execution result model

Need a structured execution result for debug and eventing:

```text
PetExperienceExecutionResult
- bundleAccepted: Boolean
- rejectedReason?
- visualStarted: Boolean
- audioStarted: Boolean
- talkShown: Boolean
- interruptionOccurred: Boolean
- debugSummary
```

This is important for traceability and debug UI.

---

# 9. Binding Rules

## 9.1 Behavior must drive all three channels together

A valid high-priority pet reaction should try to coordinate:

- avatar reaction,
- audio reaction,
- talk bubble / messaging.

Not every bundle needs all three, but the binder must consider all three together.

Examples:

### App open greeting

Input:
- intention: `GREET`
- tone: warm / sleepy / excited depending on pet state and absence

Output:
- visual: greeting reaction + emotion-biased idle state
- audio: optional greeting clip category if allowed by cooldown
- talk: context greeting bubble
- settle: return to emotion-driven idle after short duration

### Tap while playful

Input:
- intention: `PLAYFUL_RESPONSE`
- tone: excited

Output:
- visual: excited bounce / blink / micro-hop equivalent
- audio: happy chirp clip
- talk: short playful line or no text if audio already sufficient
- settle: quick return to lively idle

### Loud sound while sleepy

Input:
- intention: `STARTLE`
- tone: startled, low energy

Output:
- visual: startled attentive burst, then sleepy recovery
- audio: short surprised sound if cooldown allows
- talk: optional only if not too spammy
- settle: return to sleepy/attentive hybrid

## 9.2 Fallback hierarchy

The binder must gracefully degrade.

### Level A — full bundle

Behavior plan resolves cleanly to visual + audio + talk.

### Level B — partial bundle

If audio is suppressed or unavailable, visual + talk still execute.

### Level C — visual only

If the interaction is too small or the pet is under cooldown, the system can produce only a subtle visual response.

### Level D — resolver fallback

If the behavior binder cannot produce an executable bundle, existing state-driven emotion fallback remains active.

## 9.3 Anti-spam rules

The binder must enforce product-safe reaction behavior.

At minimum:

- same audio category cannot replay too quickly,
- same talk bubble dedupe key cannot immediately repeat,
- same exact reaction clip cannot loop from repeated taps,
- very low-value interactions can be coalesced,
- low-priority behaviors cannot interrupt high-priority visible reactions.

## 9.4 Interruption rules

We need explicit policies for interruption.

Suggested priority ordering:

1. safety / critical interrupt (future-ready)
2. startle / strong environmental attention
3. greeting / high-salience social event
4. direct touch response
5. behavior-driven ambient desire
6. idle / ambient motion

Rules:

- a new high-priority event can interrupt idle or low-value loops,
- greeting should not be immediately canceled by low-value ambient reactions,
- touch response can interrupt idle but not major startle recovery,
- settle state should absorb minor noise unless sufficiently novel.

---

# 10. Experience State Machine

## 10.1 Why it matters

The pet should not snap between unrelated reactions. It needs temporal structure.

Every meaningful reaction should have three phases:

1. **orient / start**
2. **peak expression**
3. **settle / return**

## 10.2 Minimal state machine

Suggested execution phases:

- `IDLE_BASE`
- `PREPARING_REACTION`
- `PRIMARY_REACTION`
- `SETTLING`
- `COOLDOWN`

This does not replace the behavior engine. It is the execution state for visible experience.

## 10.3 Why not overbuild this

This must stay intentionally lightweight. The product does not need a second giant engine. It needs predictable execution and timing around already-generated intention.

---

# 11. Integration with Existing Systems

## 11.1 Avatar runtime integration

The current `ui-avatar` production runtime already has strong building blocks:

- animation runtime,
- independent blink,
- idle director,
- reaction orchestrator,
- authored animation pack,
- anti-repeat guard.

This phase should **use** those systems, not replace them.

The missing step is to feed them through a behavior-authoritative directive pipeline.

### Required change

The binder should become the main producer of:

- transient reaction directives,
- target visual state bias,
- settle behavior,
- optional idle suppression or intensity boosts.

## 11.2 Audio response integration

The current audio system already supports:

- pre-recorded categories,
- playback arbitration,
- low-latency response.

This phase should route audio from behavior intent, not from ad hoc scattered event branches.

### Required change

Audio selection must be driven by `AudioDirective` from the same experience bundle that drives avatar and talk.

## 11.3 Talk bubble integration

`HomeTalkBubbleOrchestrator` already exists and deduplicates messages.

This phase should not remove it. It should make it the **execution target** of `TalkDirective` instead of allowing business meaning to leak into UI event handling.

## 11.4 State-based emotion resolver integration
n
Existing emotion mapping remains useful as:

- baseline idle emotion,
- fallback when no explicit bundle is active,
- settle target after transient reactions.

It should no longer be the highest source of visible authority.

---

# 12. Suggested Production Components

## 12.1 `BehaviorExperienceBinder`

Responsibility:

- map behavior output into an executable `PetExperienceBundle`
- attach debug reason strings / structured metadata
- apply product rules for fallback and coalescing

Must not:

- directly call UI composables,
- own playback engine internals,
- become a hidden second behavior engine.

## 12.2 `PetIntentionExecutor`

Responsibility:

- accept `PetExperienceBundle`
- enforce interruption / cooldown / anti-repeat / settle timing
- dispatch directives to avatar runtime, audio playback, and talk bubble orchestrator
- publish execution events for debug visibility

Must not:

- decide *why* the pet reacted; that is the binder/behavior layer’s job
- own Room persistence business logic

## 12.3 `ActiveExperienceState`

Responsibility:

- represent currently executing experience bundle
- expose the active visual/audio/talk state to UI layer
- allow UI to remain declarative and inspectable

## 12.4 `BehaviorExperienceDebugTrace`

Responsibility:

- capture the chain:
  - source stimulus,
  - behavior intention,
  - chosen bundle,
  - suppressed channels,
  - final execution result

This is critical for developer trust.

---

# 13. Required Debuggability

This phase must not reduce visibility. It must increase it.

At minimum we need:

- current active intention label,
- current active bundle summary,
- current execution phase,
- last rejected bundle reason,
- last suppressed audio reason,
- last talk dedupe reason,
- current cooldown keys,
- interruption history (short tail),
- per-channel execution result.

Suggested screens/panels:

- compact home debug overlay extension,
- dedicated behavior-experience debug screen,
- optional execution trace list.

---

# 14. Metrics for Success

## 14.1 Product metrics

A good implementation should improve:

- app-open first 3-second liveliness,
- perceived coherence of reactions,
- reduction in repetitive reactions,
- clearer differentiation between happy / sleepy / curious / startled moments,
- stronger feeling that the pet noticed and responded to the user.

## 14.2 Engineering metrics

We should be able to verify:

- the binder produces bundles from real behavior outputs,
- the executor is deterministic under repeated input,
- no runaway loop causes repeated audio/talk spam,
- fallback path is stable,
- no visible regression to current home experience,
- build remains green,
- debug screens explain active reactions.

---

# 15. Delivery Strategy

## 15.1 Implementation approach

This phase should be delivered in vertical slices, not as one giant refactor.

Recommended order:

1. establish canonical experience bundle types,
2. create binder from behavior output to bundle,
3. create executor with visual channel only,
4. add audio channel integration,
5. add talk bubble channel integration,
6. add interruption/cooldown/anti-repeat policies,
7. add debug and trace visibility,
8. make binder the default path for home reactions,
9. keep state-based resolver as fallback.

## 15.2 Why vertical slices are required

This project already has substantial complexity. Big-bang replacement would create brittle regressions. Small production slices reduce risk and keep the pet usable after every task.

---

# 16. Definition of Done for This Phase

This phase is done only when all of the following are true:

1. `BehaviorEngine v2` is the primary source for visible reaction execution on the home experience.
2. There is a production binding layer that maps behavior output into executable experience bundles.
3. Avatar, audio, and talk bubble can be driven from one intention bundle.
4. Interruption, cooldown, and anti-repeat behavior are real and inspectable.
5. Existing state-based emotion mapping remains available as a fallback, not as the primary visible driver.
6. A developer can inspect why a reaction happened from debug UI/logs without reading raw code.
7. The app still builds and core home flow still feels stable and responsive.
8. No POC-only scaffolding, fake logic, or disconnected architecture is introduced.

---

# 17. Explicit Boundaries to Avoid Scope Creep

Do not mix this phase with:

- object teach naming flow,
- full memory card persistence redesign,
- DI framework migration,
- removal of the legacy `pixel-avatar` module,
- conversation stack,
- body controller work,
- reworking all navigation.

Those may be valid later. They are not the critical path now.

---

# 18. Recommended Next Artifact

This design document should be paired with:

- `next_phase_behavior_experience_task_breakdown.md`

That task document must break this phase into small, build-safe, production tasks with strict boundaries and verification steps.
