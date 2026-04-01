# Attention System — Production Design for Focus, Notice, and Social Presence

Version: 1.0  
Target: AI Pet Android production runtime

---

## 1. Why attention is the real “life” system

Animation and behavior feel fake when the pet seems to notice everything equally, instantly, or not at all.

A believable creature needs limited focus.

Attention is the system that answers:
- what is the pet currently paying attention to?
- how strongly?
- for how long?
- what can interrupt it?
- what does it ignore?
- what does it return to after interruption?

Without attention:
- reactions feel random
- gaze feels decorative
- invitations feel like app prompts
- user presence has no emotional weight

Attention is the missing bridge between perception and believable behavior.

---

## 2. Core design principle

The pet should not respond to all stimuli equally.

It should have:
- focus
- inertia
- switching cost
- interruption rules
- social bias
- selective responsiveness

That is what makes it feel alive.

---

## 3. Scope of the attention system

The system governs:
- gaze target
- notice / orient / inspect / disengage
- how long focus is sustained
- how internal needs compete with external stimuli
- how pet presence feels socially directed
- how quickly the pet changes its mind

It biases:
- animation
- behavior selection
- bubble timing
- invitation timing
- voice responsiveness
- idle family selection

---

## 4. Core concepts

## 4.1 FocusTarget

The pet’s current target of attention.

```kotlin
enum class FocusTargetType {
    USER_FACE,
    USER_VOICE,
    TOUCH_SOURCE,
    SOUND_SOURCE,
    INTERNAL_NEED,
    GAME_TARGET,
    AMBIENT_SPACE,
    NONE
}
```

```kotlin
data class FocusTarget(
    val type: FocusTargetType,
    val id: String?,
    val confidence: Float,
    val salience: Float,
    val acquiredAt: Long,
    val lastReinforcedAt: Long
)
```

## 4.2 AttentionMode

How the pet is attending, not just what it attends to.

```kotlin
enum class AttentionMode {
    IDLE_SCANNING,
    PASSIVE_COMPANION,
    CURIOUS_INSPECTION,
    SOCIAL_LOCK,
    LISTENING,
    ALERT,
    DOZING,
    PLAY_FOCUS,
    WITHDRAWN
}
```

## 4.3 AttentionState

```kotlin
data class AttentionState(
    val activeTarget: FocusTarget?,
    val mode: AttentionMode,
    val intensity: Float,
    val stickiness: Float,
    val fatigue: Float,
    val availableForInterrupt: Boolean,
    val lastShiftAt: Long
)
```

---

## 5. Salience model

Every potential target should compete for attention based on salience.

### Example salience contributors
- recency
- novelty
- user-related priority
- urgency
- relationship relevance
- internal need pressure
- current mode compatibility
- confidence from perception
- anti-repeat suppression

### Example formula
```text
effective_salience =
    raw_salience
  + relationship_bonus
  + urgency_bonus
  + novelty_bonus
  + current_need_bonus
  - switch_cost
  - fatigue_penalty
  - anti_repeat_penalty
```

---

## 6. Switching cost

The pet should not whip between targets every moment.

Switching cost is essential.

### Must support
- weak stimuli should not instantly steal focus
- high urgency stimuli can interrupt
- social lock should resist low-value distractions
- sleepy mode should resist unnecessary switches
- play focus should remain stable while game is active

This is one of the biggest realism multipliers in the whole product.

---

## 7. Attention modes in detail

## 7.1 IDLE_SCANNING
Used when:
- no strong target
- user absent or passive
- pet is calm / lightly curious

Traits:
- slow gaze drift
- low stickiness
- broad soft attention
- easy redirection

## 7.2 PASSIVE_COMPANION
Used when:
- user is present nearby
- no direct interaction yet
- pet is socially available

Traits:
- center-biased attention
- warm re-checks toward user
- medium stickiness
- gentle invitation bias

## 7.3 CURIOUS_INSPECTION
Used when:
- something novel happened
- sound or motion draws interest

Traits:
- quick orient
- short focused hold
- follow-up glance behavior
- easy recovery

## 7.4 SOCIAL_LOCK
Used when:
- user is touching / speaking / strongly engaging
- pet is greeting / bonding / cuddling

Traits:
- strong center focus
- high stickiness
- reduced distraction susceptibility
- emotionally warm

## 7.5 LISTENING
Used when:
- voice activity or ambiguous sound occurs

Traits:
- directional orient
- low visual movement except subtle listening beats
- temporary pause in playful wandering

## 7.6 ALERT
Used when:
- loud sound
- unexpected event
- uncertain environment

Traits:
- fast acquisition
- short intense hold
- high interruption authority
- quick recovery or escalation

## 7.7 DOZING
Used when:
- sleepiness high
- low stimulation
- low urgency

Traits:
- drifting focus
- low switch willingness
- strong interruption thresholds
- delayed reacquisition

## 7.8 PLAY_FOCUS
Used when:
- mini-game active
- invitation accepted
- high playful engagement

Traits:
- precise target lock
- fast but purposeful shifts
- reduced unrelated idle behavior

## 7.9 WITHDRAWN
Used when:
- overstimulated
- annoyed
- comfort depleted

Traits:
- reduced social lock
- shorter tolerance
- inward focus bias
- slower re-engagement

---

## 8. Internal vs external attention

Not all attention targets are external.

The pet must sometimes attend to internal needs:
- hunger
- comfort need
- desire for play
- tiredness

This creates initiative and self-driven behavior.

### Example
If no external stimuli are strong, but social need is high:
- internal focus target becomes `INTERNAL_NEED`
- behavior engine may choose `SEEK_ATTENTION`

---

## 9. Social attention

This is the most valuable attention behavior.

The pet should distinguish:
- user present and watching
- user present but not engaging
- user absent
- user interacting warmly
- user interacting mechanically / spammy

### Resulting focus behavior
- present + watching → more center lock, companion mode
- present + speaking → listening / social lock
- absent → drift / internal focus / loneliness eligible
- affectionate touch → strong social lock
- repeated spam taps → irritation + reduced warmth

---

## 10. Attention events

The attention system should emit important semantic events:
- ATTENTION_TARGET_ACQUIRED
- ATTENTION_TARGET_SHIFTED
- ATTENTION_TARGET_LOST
- SOCIAL_LOCK_ENTERED
- SOCIAL_LOCK_EXITED
- ATTENTION_FATIGUE_HIGH
- ATTENTION_RETURNED_TO_USER

Use these sparingly for debug / memory / behavior shaping.

---

## 11. Attention lifecycle

Every focus target should move through a lifecycle:

1. **Candidate appears**
2. **Notice threshold crossed**
3. **Orient**
4. **Acquire**
5. **Hold / inspect**
6. **Reinforce or decay**
7. **Release**
8. **Return or retarget**

This maps perfectly to lifelike gaze and anticipation.

---

## 12. Gaze contract with animation system

The attention system does not draw eyes directly.
It provides:
- target
- urgency
- mode
- intensity
- hold duration
- release timing

Animation system uses this to create:
- snap
- drift
- micro-corrections
- listening pause
- sleepy delayed orient
- companion return-to-center

---

## 13. Attention fatigue

A pet should not remain hyper-focused forever.

Track:
- current focus duration
- recent number of shifts
- stimulation intensity
- social fatigue
- play fatigue
- sound interruption load

### Effects
- reduces responsiveness to weak repeated stimuli
- encourages settle / rest / withdraw
- helps anti-spam behavior
- prevents frantic target jumping

---

## 14. Relationship-aware attention

Bond should affect attention.

Examples:
- high bond → user face gets higher baseline salience
- high neglect but strong bond → user return gets emotional weight
- low trust → slower social lock acquisition
- high familiarity → easier voice-to-focus acquisition

This is one of the easiest ways to create “it knows me” feeling.

---

## 15. Voice-driven attention

When the user speaks:
- attention should often shift before full command parse completes
- LISTENING mode can begin from VAD / keyword / confidence threshold
- final command can then reinforce social lock or convert to action plan

This creates a much more lifelike sequence:
hear → orient → listen → understand → react

instead of:
command recognized → instant animation

---

## 16. Touch-driven attention

Touch should almost always produce focus reinforcement, but with nuance.

### Examples
- soft single tap → brief social lock
- affectionate long press → sustained lock and warmth
- repeated rapid taps → irritation and reduced stickiness
- sleepy pet + long press → slow comforting lock

---

## 17. Invitation behavior and attention

To invite play believably, the pet must first allocate attention to the user.

Invitation should usually come from:
- passive companion → orient to user → playful build-up → invitation

Not from:
- arbitrary timer showing bubble

Attention is what makes invitation feel intentional.

---

## 18. Time-of-day and attention profile

Attention should vary by time and state:
- morning: more curious / faster acquisition
- late night: more dozing / slower shift
- post-game: lower switch urgency
- lonely phase: more user-biased attention
- overstimulated phase: more withdrawn attention

---

## 19. Debug visibility requirements

Expose:
- current attention mode
- current target
- top candidate targets with salience
- switch cost
- hold duration
- fatigue
- interruption eligibility
- last shift reason

Without this, tuning gaze and social presence will be blind.

---

## 20. Common failure modes

### A. Decorative gaze
Eyes move, but not because of meaningful focus.

### B. Hyper-reactive pet
Any small signal steals focus immediately.

### C. Dead pet
User is present but pet never meaningfully locks on.

### D. No social difference
Touch, voice, and camera presence all feel the same.

### E. Invitation spam
Play prompt appears without prior social attention build-up.

Attention system must solve all five.

---

## 21. Module design

Recommended components:
- AttentionTargetEvaluator
- AttentionModeResolver
- AttentionArbitrator
- FocusStickinessPolicy
- AttentionFatigueTracker
- AttentionStateRepository
- AttentionDebugFormatter

---

## 22. Definition of done

Attention System is complete only when:
- the pet visibly notices and follows meaningful things
- focus shifts are understandable
- user presence changes social behavior
- gaze is not decorative
- voice and touch affect focus before full reaction
- invitation behavior is attention-led
- sleepy / withdrawn / playful states show different focus behavior
- debug tools explain current focus logic

---

## 23. Final truth

Perception tells the pet what exists.  
Attention tells the pet **what matters right now**.
