# Deep Systems Task Breakdown — Behavior Engine v2, Perception Fusion, Attention System

Version: 1.0  
Execution model: Production-first, build-safe, event-driven, Android brain remains final decision maker.

---

## Global rules

- No mock production logic
- No TODOs, empty methods, or placeholder business rules
- Every task must end in a runnable / verifiable state
- Build after every meaningful batch
- Keep architecture modular
- Preserve offline-first behavior

Build command:
- `./gradlew assembleDebug`

If touched tests exist and are stable:
- `./gradlew test`

---

# Phase B1 — Behavior Engine v2 Foundation

## B1-01
Create `WorkingContext` domain model and supporting snapshot builders.

## B1-02
Define `RelationshipState` and persistence/read surface for behavior usage.

## B1-03
Define `EmotionMomentum` model and lifecycle update rules.

## B1-04
Create `PetIntention` enum and candidate scoring contract.

## B1-05
Create `IntentionCandidate` model with reason tracing for debug.

## B1-06
Create `BehaviorPlan` model with animation/bubble/audio/game intents.

## B1-07
Create `CooldownState` / fatigue tracking models.

## B1-08
Create `BehaviorEngineDebugState` model exposing top candidates, winner, blockers.

---

# Phase B2 — Intention Scoring and Arbitration

## B2-01
Implement base intention candidate generation from need pressures.

## B2-02
Add relationship modifiers into intention scoring.

## B2-03
Add perception modifiers into intention scoring.

## B2-04
Add cooldown / fatigue / anti-repeat penalties into intention scoring.

## B2-05
Implement intention winner arbitration with continuity bias.

## B2-06
Implement debug surface for top scored intentions.

## B2-07
Verify intention outcomes differ under meaningful state changes.

---

# Phase B3 — Behavior Planning and Execution Contract

## B3-01
Map selected intentions into first-pass `BehaviorPlan` templates.

## B3-02
Add interruption policy and expected duration to plans.

## B3-03
Add after-effect rules (state mutation + memory write intents).

## B3-04
Add plan execution surface for animation/bubble/audio/game orchestrators.

## B3-05
Persist and expose current active plan for debug and continuity.

---

# Phase B4 — Emotion Momentum and Daily Continuity

## B4-01
Implement reaction affect updates from touch / sound / game / greeting.

## B4-02
Implement mood drift update rules.

## B4-03
Add carry-over between sessions based on last state and recent interactions.

## B4-04
Add time-of-day modifiers to behavior bias.

## B4-05
Verify emotional continuity across app reopen / delayed sessions.

---

# Phase P1 — Perception Fusion Foundation

## P1-01
Define normalized semantic context models:
- PresenceState
- SocialContext
- AttentionContext
- AudioContext
- VoiceContext
- TouchContext
- EnvironmentContext
- PerceptionFusionSnapshot

## P1-02
Create `PerceptionFusionRepository` contract and runtime holder.

## P1-03
Create `RecentPerceptionSummary` model.

## P1-04
Create raw input adapters / mappers from existing camera/audio/touch/voice outputs.

---

# Phase P2 — Presence and Camera Fusion

## P2-01
Implement stable user presence inference with smoothing / absence timeout.

## P2-02
Implement familiar user recognition surface into fusion.

## P2-03
Implement user-watching likelihood approximation where available.

## P2-04
Emit semantic events:
- USER_PRESENT_ENTERED
- USER_PRESENT_LEFT
- USER_LIKELY_WATCHING

## P2-05
Expose fused presence state in debug UI.

---

# Phase P3 — Audio and Voice Fusion

## P3-01
Implement external vs self-generated sound confidence model.

## P3-02
Implement loud sound semantic context with recovery window.

## P3-03
Implement voice activity semantic state.

## P3-04
Integrate parsed command confidence into voice context.

## P3-05
Emit semantic events:
- EXTERNAL_SOUND_WORTH_ATTENTION
- USER_SPOKE_TO_PET

## P3-06
Expose voice/audio fusion state in debug UI.

---

# Phase P4 — Touch Fusion and Social Meaning

## P4-01
Convert tap cadence into affection vs spam likelihood.

## P4-02
Convert long press into comfort / intimacy signal.

## P4-03
Add touch meaning into fusion snapshot.

## P4-04
Emit semantic events:
- AFFECTION_INTERACTION
- TOUCH_SPAM_DETECTED

---

# Phase A1 — Attention System Foundation

## A1-01
Define `FocusTargetType`, `FocusTarget`, `AttentionMode`, and `AttentionState`.

## A1-02
Create `AttentionStateRepository`.

## A1-03
Create `AttentionDebugState` with candidate target breakdown.

## A1-04
Create contracts for attention target evaluation and arbitration.

---

# Phase A2 — Attention Evaluation and Focus Logic

## A2-01
Generate candidate focus targets from fusion snapshot + internal needs.

## A2-02
Implement salience scoring.

## A2-03
Implement switch cost policy.

## A2-04
Implement focus acquisition / hold / release lifecycle.

## A2-05
Implement attention modes:
- IDLE_SCANNING
- PASSIVE_COMPANION
- CURIOUS_INSPECTION
- SOCIAL_LOCK
- LISTENING
- ALERT
- DOZING
- PLAY_FOCUS
- WITHDRAWN

## A2-06
Expose current mode and active target in debug UI.

---

# Phase A3 — Animation and Behavior Binding

## A3-01
Bind attention target + mode into animation-facing gaze contract.

## A3-02
Bias behavior engine using active attention target and mode.

## A3-03
Make invitation behavior attention-led rather than timer-led.

## A3-04
Make voice response orient before full action when possible.

## A3-05
Verify touch/voice/camera presence create distinct focus behavior.

---

# Phase I1 — Integration with Existing Home

## I1-01
Replace direct raw-event-driven behavior shortcuts where they conflict with Brain v2.

## I1-02
Integrate WorkingContext with fusion + attention + relationship + state.

## I1-03
Wire Behavior Engine v2 winner / active plan into home runtime.

## I1-04
Preserve current animation system compatibility while upgrading decision sources.

## I1-05
Expose consolidated debug panel for:
- fusion
- attention
- intention
- active plan

---

# Phase Q1 — Continuity, Tuning, and Anti-Spam

## Q1-01
Tune invitation suppression / fatigue with attention and relationship inputs.

## Q1-02
Tune user return behavior after long absence.

## Q1-03
Tune lonely vs companion vs withdrawn behavior selection.

## Q1-04
Tune sound interruption severity vs recovery.

## Q1-05
Tune cuddle / spam touch divergence.

---

# Phase Q2 — Performance and Stability

## Q2-01
Audit recomposition hotspots from high-frequency perception streams.

## Q2-02
Throttle / summarize high-frequency updates before UI.

## Q2-03
Audit coroutine / effect lifecycle in behavior + fusion + attention systems.

## Q2-04
Remove duplicate observers and stale timers.

## Q2-05
Verify build and stable runtime after integration.

---

# Phase Q3 — Final Production Pass

## Q3-01
Consistency pass on decision readability:
- does the pet behavior feel intentional?

## Q3-02
Continuity pass:
- does the pet carry state across sessions meaningfully?

## Q3-03
Debug completeness pass:
- can developers inspect why the pet chose current behavior?

## Q3-04
Polish pass:
- remove brittle logic
- reduce scattered ad hoc behavior rules
- centralize domain decisions

---

## Definition of done

Deep systems implementation is complete only when:
- the pet has a real intention system
- perception is fused rather than consumed raw
- attention selects what matters
- behavior is meaningfully initiative-driven
- user presence and relationship matter
- voice, audio, touch, and camera produce coherent downstream decisions
- debug tooling explains the decision chain
- builds remain green
