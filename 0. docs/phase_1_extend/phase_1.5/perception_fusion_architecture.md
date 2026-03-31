# Perception Fusion Architecture — Production Design for Camera, Audio, Voice, and Context

Version: 1.0  
Target: Android AI Pet production system  
Role framing: Senior Solution Architect + Senior Game Systems Designer

---

## 1. Why fusion is necessary

Most pet-app architectures fail because they process perception as isolated triggers:

- camera event
- audio event
- voice command event
- touch event

That leads to brittle behavior:
- pet reacts to sound without considering user presence
- pet greets without knowing if the user is actually there
- voice commands are treated as raw triggers rather than social interaction
- lonely state ignores whether the user has actually been seen recently

A believable creature needs a **unified understanding of the current world**.

Perception Fusion is the layer that merges:
- camera understanding
- user presence
- face recognition
- gaze and attention cues
- environmental sound
- voice activity
- recognized commands
- touch interactions
- recent perception history

into one coherent snapshot that the Brain can reason over.

---

## 2. Main objective

Create a stable, semantic, production-safe context model:

`raw sensors -> normalized perception signals -> fused context -> behavior decision`

This layer must answer questions like:

- Is the user present right now?
- Is this the owner / familiar user / unknown person?
- Is the user looking at the pet?
- Did the pet hear something important?
- Is the user trying to speak to the pet?
- Is the pet alone?
- Is a sound likely external or caused by the pet itself?
- Is interaction passive, active, or affectionate?
- Should the pet remain calm, become curious, or prioritize response?

---

## 3. Fusion principles

### Principle 1 — Semantic before behavioral
Perception Fusion should not directly choose animation.
It builds a world model that the Behavior Engine uses.

### Principle 2 — Confidence-aware
Not all detections are equally reliable.
Fusion must track confidence and temporal stability.

### Principle 3 — Temporal smoothing
Single-frame spikes should not dominate behavior unless they are urgent.

### Principle 4 — Multi-sensor corroboration
When multiple signals align, confidence goes up.

### Principle 5 — Degrade gracefully
If voice recognition is unavailable, audio + presence should still work.
If camera is unavailable, audio and touch should still support behavior.

---

## 4. Perception domains

## 4.1 Camera domain

### Raw capabilities
- camera available / unavailable
- face detected
- face count
- recognized person id
- face confidence
- approximate face position
- dwell duration
- user proximity approximation
- head orientation / attention approximation where feasible
- object of interest (optional, lower priority)

### Semantic outputs
- user present
- familiar person present
- user likely watching pet
- user entered scene
- user left scene
- user lingering nearby
- visually idle scene / no person

---

## 4.2 Audio domain

### Raw capabilities
- sound energy
- sound spike
- VAD-light
- loud sound
- continuous ambient sound
- playback active flag
- audio self-trigger suppression state

### Semantic outputs
- ambient sound present
- sudden sound event
- likely external sound
- likely self-generated sound
- sustained noisy environment
- quiet environment
- sound worth orienting toward

---

## 4.3 Voice / ASR domain

### Raw capabilities
- voice activity detected
- ASR partial / final recognition
- recognized keyword
- parsed command
- command confidence
- command timestamp

### Semantic outputs
- user attempting to talk to pet
- direct command to pet
- praise / affection phrase
- play intent
- stop intent
- comfort intent
- unclear / ignored speech

---

## 4.4 Touch domain

### Raw capabilities
- tap
- long press
- touch duration
- recent touch cadence

### Semantic outputs
- affectionate touch
- playful interaction
- spammy touch
- overstimulating contact
- comfort-seeking contact

---

## 5. Fusion output contract

The fusion layer should expose a single snapshot:

```kotlin
data class PerceptionFusionSnapshot(
    val presence: PresenceState,
    val socialContext: SocialContext,
    val attentionContext: AttentionContext,
    val audioContext: AudioContext,
    val voiceContext: VoiceContext,
    val touchContext: TouchContext,
    val environmentContext: EnvironmentContext,
    val recentPerceptionSummary: RecentPerceptionSummary,
    val updatedAt: Long
)
```

### PresenceState
```kotlin
data class PresenceState(
    val userPresent: Boolean,
    val familiarUserPresent: Boolean,
    val recognizedPersonId: String?,
    val faceCount: Int,
    val stablePresenceMs: Long,
    val absenceMs: Long,
    val entryEventRecently: Boolean,
    val exitEventRecently: Boolean
)
```

### SocialContext
```kotlin
data class SocialContext(
    val userWatchingPet: Boolean,
    val eyeContactLikelihood: Float,
    val interactionAvailability: Float,
    val socialWarmthSignal: Float
)
```

### AttentionContext
```kotlin
data class AttentionContext(
    val likelyFocusTarget: FocusTarget?,
    val focusDirection: FocusDirection,
    val noveltySignal: Float,
    val orientingUrgency: Float
)
```

### AudioContext
```kotlin
data class AudioContext(
    val ambientLevel: Float,
    val loudEventActive: Boolean,
    val externalSoundConfidence: Float,
    val selfPlaybackActive: Boolean,
    val shouldOrientToSound: Boolean
)
```

### VoiceContext
```kotlin
data class VoiceContext(
    val voiceActivity: Boolean,
    val command: VoiceCommand?,
    val commandConfidence: Float,
    val commandAddressedToPet: Boolean,
    val recentSpeechMs: Long
)
```

### TouchContext
```kotlin
data class TouchContext(
    val recentTap: Boolean,
    val recentLongPress: Boolean,
    val affectionLikelihood: Float,
    val spamLikelihood: Float,
    val lastTouchMs: Long
)
```

### EnvironmentContext
```kotlin
data class EnvironmentContext(
    val visualQuiet: Boolean,
    val audioQuiet: Boolean,
    val interactionPressure: Float
)
```

---

## 6. Fusion state machine thinking

Fusion should not simply mirror latest raw readings.
It should infer durable interaction states:

### Suggested high-level fused states
- ALONE_QUIET
- USER_PRESENT_PASSIVE
- USER_PRESENT_ENGAGED
- USER_AFFECTION_ACTIVE
- USER_SPEAKING
- UNEXPECTED_SOUND
- NOISY_ENVIRONMENT
- PET_SELF_PLAYBACK
- ATTENTION_SEEKING_WINDOW

These are not final behavior states.
They are semantic modes that help the brain interpret context cleanly.

---

## 7. Temporal fusion rules

## 7.1 Stability windows
Use windows for confidence:
- presence stable after N consistent frames
- user gone only after absence threshold
- voice context active for short hangover after final speech
- loud sound recovery window to avoid repeated startle loops

## 7.2 Recency weighting
Recent relevant events should influence context:
- tap from 1 second ago still matters
- greeting from 20 minutes ago probably should not

## 7.3 Decay
All temporary perception signals should decay:
- novelty fades
- orienting urgency fades
- speech targeting fades
- affection window fades

---

## 8. Camera fusion logic

## 8.1 User presence inference
Do not switch `userPresent` on and off based on a single missed frame.

Use:
- face detection confidence
- tracking continuity
- stability duration
- absence timeout

### Result
The pet should not appear confused because the camera flickered for one frame.

## 8.2 Recognized user handling
Recognized identity should be:
- stable enough for relationship-aware behavior
- decoupled from greeting spam
- invalidated carefully, not instantly

## 8.3 Looking-at-pet inference
If precise gaze is unavailable, approximate with:
- face centeredness
- head orientation proxy
- stable front-facing duration

Use a confidence score, not a boolean.

---

## 9. Audio fusion logic

## 9.1 Self-trigger safety
When the pet is emitting its own audio:
- suppress low-value sound reactions
- only allow reactions to clearly external, strong events

### Why
Otherwise the pet becomes self-reactive and breaks believability.

## 9.2 Loud sound policy
A loud sound should generate:
- urgent orienting context
- temporary alert mode
- possible startle candidate for behavior engine

But:
- do not repeatedly re-trigger startle during sustained noise

Use a hold + cooldown window.

## 9.3 Quiet/noisy environment
Ambient sound should inform:
- whether the pet feels safe to speak / chirp
- whether subtle voice input is likely
- whether invitation behavior should be restrained

---

## 10. Voice fusion logic

## 10.1 Command confidence
Commands should be attached with confidence and freshness.
Behavior engine can ignore or soften low-confidence speech.

## 10.2 Social interpretation
Not all recognized speech should produce a hard action.
Voice context should differentiate:
- direct instruction
- praise
- emotional speech nearby
- unclear speech

## 10.3 Voice + presence cross-check
If voice activity is present but no user presence is visible:
- still allow response
- but maybe reduce certainty / intimacy

If both voice + familiar presence are present:
- boost response confidence

---

## 11. Touch fusion logic

Touch is socially rich and should be interpreted, not just passed through.

### Touch patterns
- one soft tap
- repeated playful taps
- long press cuddle
- repeated annoying poke
- delayed reassurance touch after lonely phase

Fusion should convert these into social meaning:
- affection
- invitation to play
- overstimulation
- repair / reconnection

---

## 12. Presence awareness system

Presence is one of the highest-value systems for a living pet.

### Must support
- user enters view
- user remains nearby
- user disappears
- user returns after absence
- user watches pet passively
- user interacts actively

### Behavioral significance
- greeting
- lonely decay
- attention-seeking eligibility
- initiative timing
- bond warmth
- eye contact / expectant idle

---

## 13. Attention target synthesis

Perception Fusion must synthesize what the pet should likely attend to.

### Possible focus targets
- USER_FACE
- USER_VOICE
- TOUCH_SOURCE
- SOUND_SOURCE
- GAME_PROMPT
- INTERNAL_NEED
- NONE

### Selection factors
- urgency
- recency
- confidence
- existing active plan
- interruption level

This is not final behavior selection, but it strongly biases it.

---

## 14. RecentPerceptionSummary

Fusion should keep a short semantic memory of recent perception:
- user entered recently
- sound spike 3 seconds ago
- voice command 1 second ago
- long press just ended
- user absent for 2 minutes

This allows the brain to behave with continuity instead of reacting only to current frame values.

---

## 15. Recommended module design

## 15.1 Input adapters
- CameraPerceptionAdapter
- AudioPerceptionAdapter
- VoicePerceptionAdapter
- TouchPerceptionAdapter

## 15.2 Domain fusion layer
- PresenceInterpreter
- AudioInterpreter
- VoiceInterpreter
- TouchInterpreter
- AttentionSynthesisEngine
- PerceptionFusionCoordinator

## 15.3 Output contract
- `PerceptionFusionRepository`
- `observeFusionSnapshot(): Flow<PerceptionFusionSnapshot>`
- `getCurrentFusionSnapshot(): PerceptionFusionSnapshot`

---

## 16. Event model

Fusion should consume raw perception events and may emit semantic perception events:

### Raw events
- FACE_DETECTED
- PERSON_RECOGNIZED
- SOUND_DETECTED
- LOUD_SOUND
- VOICE_ACTIVITY_STARTED
- COMMAND_PARSED
- PET_TAPPED
- PET_LONG_PRESSED

### Semantic events
- USER_PRESENT_ENTERED
- USER_PRESENT_LEFT
- USER_LIKELY_WATCHING
- EXTERNAL_SOUND_WORTH_ATTENTION
- USER_SPOKE_TO_PET
- AFFECTION_INTERACTION
- TOUCH_SPAM_DETECTED

These semantic events should be used sparingly to avoid flooding.
The fusion snapshot remains the main surface.

---

## 17. Production safety rules

### Rule 1 — Fusion is not behavior
Do not let fusion directly choose pet actions.

### Rule 2 — Suppress noise
Do not emit semantic state changes for tiny unstable fluctuations.

### Rule 3 — Make uncertainty visible
Confidence matters. Use scores.

### Rule 4 — Respect privacy
Keep perception processing on-device where feasible.
Persist summaries, not raw audio/video unless explicitly required.

### Rule 5 — Be debuggable
You must be able to inspect why the system currently thinks the user is present, speaking, or absent.

---

## 18. Debug visibility

Debug must expose:
- raw perception values
- fused presence state
- current attention target
- voice confidence
- external sound confidence
- self-playback suppression state
- user-watching estimate
- stable presence duration
- absence duration
- semantic recent events

Without this, tuning fusion will be guesswork.

---

## 19. Performance requirements

- camera inference should be throttled and lifecycle-safe
- fusion should operate on summarized signals, not full-resolution image logic
- audio energy / VAD updates can be faster, but fusion summarization should be bounded
- command parsing should not trigger full behavioral recompute on every partial token
- avoid recomposing UI directly from high-frequency raw perception streams

---

## 20. Failure modes to avoid

### A. Face flicker presence
User presence toggles rapidly due to weak smoothing.

### B. Sound confusion
Pet reacts to its own chirps and speech.

### C. Voice over-trust
Low-confidence ASR becomes hard command.

### D. Lonely blindness
Engine says lonely even though user has been visually present for a while.

### E. Passive-user blindness
Pet cannot distinguish “user present but inactive” from “user absent”.

Fusion must solve all five.

---

## 21. Definition of done

Perception Fusion is complete only when:
- user presence is stable and readable
- familiar presence affects downstream behavior
- sound reaction is self-safe
- voice commands are integrated semantically
- touch carries social meaning
- the brain consumes a single coherent snapshot
- debug tools expose the fused world model
- performance remains production-safe

---

## 22. Final truth

Raw perception tells the pet what happened.  
Perception Fusion tells the pet **what the world currently means**.
