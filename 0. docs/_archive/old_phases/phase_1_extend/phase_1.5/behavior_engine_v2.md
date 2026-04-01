# Behavior Engine v2 — Production Architecture for AI Pet

Version: 1.0  
Role framing: Senior Game Designer + Senior Solution Architect  
Target: Android-first digital pet that behaves like a living creature, not a reactive mascot.

---

## 1. Why this exists

The current product can become visually impressive without becoming truly alive.

That failure mode happens when the system is still fundamentally:

`stimulus -> direct reaction`

A believable pet needs:

`state + memory + perception + time + relationship + context -> intention -> behavior plan -> expression/action`

This document defines the **real brain layer** that sits above animation and below any future LLM / cloud intelligence.

This is the layer that determines:

- what the pet wants right now
- what it should do next
- why it behaves differently today versus yesterday
- why the same tap can produce different reactions under different internal states
- how continuity is preserved across sessions

---

## 2. Product philosophy

The pet is not a chatbot.  
The pet is not an assistant.  
The pet is not a menu with a mascot.

The pet is a **stateful creature simulation** with:

- persistent needs
- emotional inertia
- attention limits
- relationship memory
- competing intentions
- initiative
- short-term and long-term consequences

The engine must optimize for:

1. **Believability**
2. **Continuity**
3. **Readability**
4. **Variety without chaos**
5. **Production stability**

---

## 3. Core outcome

Behavior Engine v2 must convert all available information into a single coherent decision loop:

### Inputs
- persistent PetState
- recent events
- memory summaries
- current perception context
- current focus / attention
- time context
- cooldowns / fatigue
- session history
- game state
- audio state
- relationship indicators

### Outputs
- active intention
- selected behavior plan
- emotion bias
- animation intent
- talk bubble intent
- audio response intent
- mini-game invitation / interaction intent
- event emissions
- state changes
- memory writes

---

## 4. High-level architecture

```text
Perception Layer
(camera + presence + audio + touch + voice)
        ↓
Perception Fusion
        ↓
Working Context Builder
        ↓
Need System + Emotion Momentum + Relationship Context
        ↓
Intention Scoring / Arbitration
        ↓
Behavior Planner
        ↓
Execution Layer
(animation / bubble / audio / game / event / state update)
        ↓
Memory Writeback + Cooldowns + After-effects
```

---

## 5. Main subsystems

## 5.1 Need System

The pet must have persistent, decaying, non-binary needs.

### Required need dimensions
- energy
- sleepiness
- hunger
- social need
- play need
- comfort need
- curiosity need
- stimulation load / overstimulation
- affection need
- trust / security level (optional but highly valuable)

### Rules
- needs drift over time
- needs affect intention weighting continuously
- needs affect response style, not just top-level emotion
- needs can reinforce or suppress each other

### Examples
- high sleepiness reduces motion amplitude and suppresses play invitations
- high social need increases attention-seeking and responsiveness to user presence
- high overstimulation reduces tolerance for repeated touch
- low comfort increases hesitant / cautious behaviors

---

## 5.2 Emotion Momentum System

Static emotion mapping is not enough.

The pet needs **emotional carry-over**:
- reactions should leave traces
- mood should not flip instantly
- repeated events should shape longer-lived tendencies

### Required layers
- immediate reaction affect
- short-lived emotional momentum
- medium mood drift
- long-term trait bias

### Suggested model
Use 3 layers:

#### A. Reaction affect (seconds)
Fast-changing signal driven by current event.
Examples:
- startled spike
- joy burst
- annoyance flicker

#### B. Mood field (minutes / session)
Slow-moving background that biases behavior selection.
Examples:
- playful
- withdrawn
- warm
- drowsy
- needy

#### C. Trait bias (days / long-term)
Very slow-moving personality tendencies.
Examples:
- clingy
- curious
- mellow
- excitable
- stubborn

### Design requirement
New behavior selection should never read only the instantaneous emotion.
It must combine:
- current affect
- mood field
- current needs
- relationship context

---

## 5.3 Relationship System

Bonding is the main retention mechanic.

The pet must treat the user differently depending on:
- total interaction history
- recency of care
- frequency of neglect
- game participation
- affectionate interactions
- voice usage
- consistency across days

### Required relationship metrics
- bond
- familiarity
- trust
- recent warmth
- recent neglect
- responsiveness expectation

### Behavioral use
- greeting intensity
- willingness to initiate play
- tolerance for repeated tapping
- choice of clingy vs independent idle
- bubble tone
- post-ignore recovery

### Example
A high-bond pet that has been ignored recently might:
- still seek attention gently
- look slightly hesitant
- recover faster after user re-engagement

A low-bond pet with high neglect might:
- respond less warmly
- initiate less often
- show more watchful than affectionate behavior

---

## 5.4 Working Context Builder

The behavior engine must not consume raw events one by one.
It needs a **working context snapshot**.

### WorkingContext contents
- current PetState
- derived conditions
- current perception fusion snapshot
- active attention target
- recent event summaries
- current relationship state
- last behavior plan
- current fatigue / cooldown flags
- time-of-day context
- session age
- recent interaction cadence
- current game eligibility / invitation state

### Why this matters
Without a working context, the engine becomes brittle and scattered.
All decision functions must read from a stable snapshot.

---

## 5.5 Intention System

Intention is the bridge between internal life and visible behavior.

The pet should not directly pick animation from state.
It should first decide **what it wants**.

### Required PetIntention set

```kotlin
enum class PetIntention {
    REST,
    DOZE,
    SEEK_ATTENTION,
    SEEK_COMFORT,
    INVITE_PLAY,
    PLAY,
    RESPOND_TO_USER,
    OBSERVE,
    INVESTIGATE,
    REQUEST_FOOD,
    SELF_SOOTHE,
    STAY_NEAR,
    WITHDRAW,
    LISTEN,
    STARTLE_RECOVER,
    CELEBRATE,
    RECOVER
}
```

### Important note
Intention is not the same as animation or emotion.

Examples:
- same intention `SEEK_ATTENTION` can appear as lonely, playful, clingy, shy
- same emotion `sleepy` can produce different intentions: `DOZE`, `SEEK_COMFORT`, `WITHDRAW`

---

## 5.6 Intention Scoring / Arbitration

This is the most important technical system in the brain.

Each decision cycle should:
1. generate candidate intentions
2. score them
3. apply suppression / gating
4. select winner
5. plan behavior

### Candidate scoring inputs
- need pressures
- emotion momentum
- relationship modifiers
- perception relevance
- active interrupts
- recency suppression
- cooldowns
- fatigue
- current engagement state
- session novelty
- anti-repeat guard

### Recommended scoring structure

```text
intention_score =
    base_weight
  + need_pressure
  + emotion_bias
  + relationship_modifier
  + perception_trigger_bonus
  + time_context_bonus
  - cooldown_penalty
  - fatigue_penalty
  - anti_repeat_penalty
  - interruption_block_penalty
```

### Gating examples
- `INVITE_PLAY` blocked if sleepy too high
- `PLAY` blocked if no eligible interaction target
- `SEEK_ATTENTION` reduced if user absent and no recent presence
- `RESPOND_TO_USER` boosted immediately after touch or voice
- `WITHDRAW` boosted when overstimulated

### Tie-break rules
- prefer continuity over random jumps
- prefer interrupt safety
- prefer readability
- apply bounded variety only after coherence

---

## 5.7 Behavior Planner

Once an intention is selected, the engine still must not directly trigger one animation.
It should create a **behavior plan**.

### BehaviorPlan structure
- intention
- plan id
- start conditions
- sequence type
- animation family
- bubble policy
- audio policy
- game / interaction hooks
- expected duration
- interruption priority
- exit conditions
- after-effect state updates
- memory write intent

### Example
Intention: `INVITE_PLAY`

BehaviorPlan:
- center focus
- anticipation beat
- playful animation
- invitation bubble
- wait window
- if accepted → launch game
- if ignored → soften, settle, apply ignore count
- if interrupted by startling sound → cancel gracefully

---

## 5.8 Execution Layer

Execution should be split across orchestrators:

- AnimationController
- ReactionOrchestrator
- TalkBubbleOrchestrator
- AudioResponseController
- GameInvitationController
- EventEmitter
- StateMutationApplier
- MemoryRecorder

Behavior Engine v2 should not directly mutate UI state in-line.
It should produce commands / intents for these orchestrators.

---

## 5.9 Cooldown / Fatigue Layer

To feel alive, the pet must avoid spam.

### Required tracked cooldown domains
- touch reaction cooldown
- long press intimacy cooldown
- invitation cooldown
- game fatigue cooldown
- audio reaction cooldown
- loud sound recovery cooldown
- attention-seeking cooldown
- bubble repetition cooldown

### Ignore / fatigue counters
- invitation ignored streak
- recent touch spam count
- recent repeated voice command count
- repeated no-response windows

### Result
The pet should not ask too often, react identically every time, or feel needy in a UI-prompt way.

---

## 6. Decision loop cadence

Use multiple cadences, not one monolithic update tick.

### Suggested cadences

#### Fast loop (50–200ms)
For:
- interruption checks
- urgent attention shifts
- reaction continuation
- sound-trigger gating

#### Medium loop (500ms–2s)
For:
- intention reevaluation
- ambient idle family changes
- invitation eligibility
- perception-driven behavior updates

#### Slow loop (5s–60s)
For:
- mood drift
- need adjustments during active session
- anti-fatigue recovery
- session-level variation

#### Long loop (app open / close / background / foreground / hourly)
For:
- time-based decay
- relationship adjustment
- daily evolution
- memory summarization triggers

---

## 7. Perception integration contract

Behavior Engine must not depend directly on raw camera/audio systems.
It consumes a fused snapshot:

```kotlin
data class PerceptionSnapshot(
    val userPresence: PresenceState,
    val recognizedPersonId: String?,
    val faceAttentionConfidence: Float,
    val userWatchingPet: Boolean,
    val objectOfInterest: String?,
    val soundLevel: Float,
    val soundType: SoundType?,
    val voiceActivity: Boolean,
    val parsedCommand: VoiceCommand?,
    val recentPerceptionEvents: List<PerceptionEventSummary>
)
```

This gives Behavior Engine a stable semantic surface.

---

## 8. Session and daily variation

The pet must not feel identical each session.

### Required variation systems
- session mood seed
- daily tone shift
- novelty decay
- last-session carry-over
- time-of-day modifier
- streak-based bond uplift
- neglect-based hesitation

### Examples
- morning sessions: more alert, curious
- late-night sessions: slower, sleepier, affectionate if bond high
- after missed day: cautious / needy / excited depending on relationship profile
- after many games yesterday: more tired but friendlier

---

## 9. Behavior categories to support

Behavior Engine v2 must support at least these production-level categories:

### Ambient
- calm idle
- curious idle
- sleepy drift
- lonely waiting
- expectant companion
- post-interaction glow
- overstimulated cool-down

### User-responsive
- tap response
- long press cuddle / tolerance / overload
- voice response
- greeting
- welcome back
- apology / recovery after ignore

### Need-driven
- hungry ask
- sleepy settle
- social-seeking
- comfort-seeking
- self-soothing

### Initiative behaviors
- invite play
- seek attention
- approach emotionally
- choose to observe silently
- request rest

### Recovery
- startled recover
- post-failure recovery
- overstimulation recovery
- post-game settle

---

## 10. Planning rules

### Rule 1 — Coherence before variety
A believable but slightly repetitive pet is better than a random pet.

### Rule 2 — Intention continuity
Do not flip intention every second without reason.

### Rule 3 — After-effects matter
Most behaviors must modify:
- emotion momentum
- memory
- cooldowns
- future scoring

### Rule 4 — Higher-priority interruptions are rare but respected
Example:
- loud sound can interrupt invitation
- direct user touch can interrupt light idle
- cuddle can suppress weak attention-seeking for a while

### Rule 5 — User-facing readability
Behavior must be interpretable:
- user should feel “it wants attention”
- not “some random animation happened”

---

## 11. Data model proposal

## 11.1 Core domain models

```kotlin
data class PetState(
    val energy: Int,
    val hunger: Int,
    val sleepiness: Int,
    val social: Int,
    val playNeed: Int,
    val comfortNeed: Int,
    val overstimulation: Int,
    val bond: Int,
    val lastUpdatedAt: Long
)

data class RelationshipState(
    val familiarity: Float,
    val trust: Float,
    val recentWarmth: Float,
    val recentNeglect: Float,
    val responsivenessExpectation: Float
)

data class EmotionMomentum(
    val joy: Float,
    val comfort: Float,
    val curiosity: Float,
    val drowsiness: Float,
    val neediness: Float,
    val irritation: Float,
    val caution: Float,
    val startled: Float
)

data class WorkingContext(
    val petState: PetState,
    val relationship: RelationshipState,
    val emotionMomentum: EmotionMomentum,
    val perception: PerceptionSnapshot,
    val recentMemorySummary: RecentMemorySummary,
    val activeFocus: AttentionTarget?,
    val currentBehavior: ActiveBehaviorState?,
    val sessionContext: SessionContext,
    val cooldowns: CooldownState
)
```

## 11.2 Intention candidate

```kotlin
data class IntentionCandidate(
    val intention: PetIntention,
    val score: Float,
    val reasons: List<String>,
    val blocked: Boolean = false
)
```

## 11.3 Behavior plan

```kotlin
data class BehaviorPlan(
    val id: String,
    val intention: PetIntention,
    val sequenceType: BehaviorSequenceType,
    val animationIntent: AnimationIntent,
    val bubbleIntent: BubbleIntent?,
    val audioIntent: AudioIntent?,
    val gameIntent: GameIntent?,
    val priority: Int,
    val expectedDurationMs: Long,
    val interruptibility: Interruptibility,
    val exitPolicy: ExitPolicy,
    val afterEffects: List<BehaviorAfterEffect>
)
```

---

## 12. Persistence requirements

Some of this lives in memory, some must persist.

### Persisted
- PetState
- relationship state
- recent interaction counters
- last invitation timestamps
- ignore streak summaries
- daily session summaries
- trait drift inputs
- important behavior history markers

### In-memory
- current working context
- active plan
- current attention target
- short-term emotion bursts
- anti-repeat recent queue
- immediate cooldown timers

---

## 13. Debug visibility requirements

For production development, debug visibility is mandatory.

### Debug panel must expose
- current intention
- top 5 scored candidates
- current behavior plan
- active attention target
- active ambient family
- current cooldowns
- ignore streak values
- invitation eligibility state
- emotion momentum values
- relationship snapshot

Without this, tuning will be guesswork.

---

## 14. Performance constraints

Behavior must feel alive without burning battery or thrashing recomposition.

### Requirements
- no brute-force heavy recompute every frame
- derived values should be memoized / cached where reasonable
- intention scoring should run on medium cadence, not animation cadence
- fast-loop urgent checks must be lightweight
- avoid unbounded coroutine spawning
- avoid duplicate observers per screen lifecycle

---

## 15. Failure modes to avoid

### Bad mode A — Random pet
Symptoms:
- animation varies but behavior feels meaningless

### Bad mode B — Deterministic puppet
Symptoms:
- user learns exact same sequence every time

### Bad mode C — Need spam
Symptoms:
- pet asks too often / feels like notifications

### Bad mode D — Mood flip-flop
Symptoms:
- sleepy to excited to sad instantly with no continuity

### Bad mode E — Reactive only
Symptoms:
- pet never initiates anything

Behavior Engine v2 must avoid all five.

---

## 16. Production rollout strategy

Implement in layers, not a huge rewrite.

### Stage 1
- WorkingContext
- intention scoring
- simple behavior plans
- debug panel

### Stage 2
- emotion momentum
- relationship weighting
- initiative behaviors
- invitation policy

### Stage 3
- daily/session variation
- nuanced recovery behaviors
- long-term drift / trait shaping

### Stage 4
- optional future cloud / LLM augmentation hooks
- but Android brain remains final decision maker

---

## 17. Definition of done

Behavior Engine v2 is complete only when:

- the pet visibly chooses different behaviors under different states
- the same user action can produce different believable results
- the pet initiates interaction on its own
- continuity is preserved across sessions
- user neglect / care changes behavior over time
- invitation behavior is not spammy
- debug tools make decisions inspectable
- performance remains stable
- animation feels driven by intent, not direct event mapping

---

## 18. Final design truth

Animation makes the pet look alive.  
Behavior Engine v2 makes the pet **actually behave alive**.
