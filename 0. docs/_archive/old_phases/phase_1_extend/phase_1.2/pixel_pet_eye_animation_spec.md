# Pixel Pet Eye Animation Specification

## 1. Goal

Define a practical visual specification for the first production-ready pixel animation style of the mobile pet app.

This version intentionally narrows scope to an **eye-focused character**, inspired by the feel of pets like Cosmos or Emo:
- no mouth
- no nose
- no full character body animation requirement
- emotion is expressed mainly through **eyes, eyelids, pupils, eyebrow-like eye shapes, blinking rhythm, gaze direction, squinting, widening, and small motion accents**

The objective is to create a system that is:
- simple enough to implement quickly
- expressive enough to feel alive
- structured enough for AI-assisted generation
- stable enough to scale later without rewriting the app architecture

---

## 2. Scope

This document defines:
- the visual direction for the pixel pet
- the fixed drawing space of **64x64**
- the component breakdown for eye-only animation
- the state model for emotions and behavior display
- the frame/clip model for avoiding repetitive motion
- implementation boundaries so animation logic stays maintainable

This document does **not** define:
- backend AI logic
- memory/event architecture
- voice/audio systems
- pet progression systems
- monetization/cosmetics
- full-screen game-style pixel UI

The output here is only the **pixel avatar animation specification** for the app.

---

## 3. Design Intent

The pet should not look like a game sprite with many body parts. It should feel like a **small digital being living inside the phone**, where the eyes carry most of the emotional weight.

That means the visual language must prioritize:
- immediate readability at small size
- strong emotional clarity from only the eyes
- low frame count but believable life
- subtle variation so repeated loops do not feel robotic

Core principle:

> The pet feels alive not because it has many pixels, but because its eyes react with rhythm, timing, asymmetry, and small variations.

---

## 4. Canvas and Rendering Model

### 4.1 Logical sprite size

The pet uses a fixed logical canvas of:
- **64 x 64 pixels**

This is the source drawing grid for every animation frame.

### 4.2 Why 64x64

64x64 is the best tradeoff for this project because it gives enough room for:
- two expressive eyes
- eyelid changes
- pupil movement
- squint/widen states
- small decorative accents if needed later

while still staying:
- lightweight
- easy to author by code
- visibly pixel-art
- easy to preview and debug

### 4.3 Display scaling

The 64x64 logical sprite should be scaled up cleanly with nearest-neighbor rendering.

Recommended initial display sizes:
- compact: 256dp
- standard: 320dp
- large hero mode: 384dp

Important rule:
- never blur the sprite
- never use anti-aliased scaling for the main pixel output

### 4.4 Screen usage

Only the **pet avatar area** is pixel-rendered.
The rest of the app UI remains modern, readable, and standard mobile UI.

This prevents the app from feeling like a retro game and keeps the product accessible.

---

## 5. Character Composition

The character is built from a small number of visual layers.

### 5.1 Required layers

At minimum:
1. background/void face area
2. left eye white or eye shape area
3. right eye white or eye shape area
4. left pupil/inner eye detail
5. right pupil/inner eye detail
6. eyelid overlays / blink shapes
7. optional accent pixels for emotion emphasis

### 5.2 Face philosophy

There is no need to render a full face.
The user should emotionally read the pet from:
- eye spacing
- eye size
- openness
- pupil position
- blink speed
- asymmetry
- slight bounce or drift

The “face” can be mostly implied.

### 5.3 Recommended visual center

Suggested center alignment for the eyes:
- place the two eyes slightly above the vertical center of the 64x64 canvas
- leave enough margin around them so motion does not clip the frame
- keep a stable anchor so all states feel like the same character

---

## 6. Visual Style Rules

To keep the pet consistent, every animation should follow these rules.

### 6.1 Shape language

Preferred style:
- rounded-rectangle eyes or soft oval pixel eyes
- simple pupil shapes
- low-detail outlines
- no excessive dithering

Avoid:
- overly detailed anime eyes
- too many highlight pixels
- hard-to-read thin details
- shape changes so extreme that the pet looks like a different character

### 6.2 Palette rules

Start with a very small palette.

Recommended initial palette size:
- 1 background tone or transparency
- 1 primary eye color
- 1 pupil/dark detail color
- 1 highlight color
- 1 accent/emotion color if needed

A small palette keeps the identity clean and helps AI-generated frames stay consistent.

### 6.3 Consistency rules

All frames must preserve:
- same eye baseline position
- same inter-eye spacing
- same character silhouette logic
- same anchor point in the 64x64 canvas

Changes should come from expression, not random redesign.

---

## 7. Animation Model

## 7.1 Core strategy

Each high-level pet state should not map to a single loop.
Instead, each state should own:
- **3 to 4 animation variants**

This is important because a single repeated loop becomes obviously robotic very quickly.

Example:
- idle state has 4 variants
- happy state has 3 variants
- sleepy state has 4 variants

The runtime can rotate, weight, or randomly choose among them.

### 7.2 Clip philosophy

Each clip should be:
- short
- readable
- loopable or one-shot depending on purpose
- built from few frames

Recommended initial frame count per clip:
- 2 to 6 frames for loops
- 3 to 8 frames for one-shot reactions

### 7.3 Motion sources

Since there is no mouth or body, life comes from:
- blinking
- eyelid compression
- eye widening
- eye narrowing
- gaze shifts
- micro bobs
- subtle horizontal drift
- short squeeze/release motions
- synchronized vs slightly unsynchronized eye movement

### 7.4 Avoiding robotic repetition

The system should allow variation through:
- different clip selection within a state
- random delay between loops
- occasional extra blink
- occasional gaze drift
- asymmetrical eyelid timing
- different hold durations on nearly identical frames

Even tiny timing differences matter.

---

## 8. Proposed State Set

This section defines the first recommended visual states for the eye-only pet.

The exact names can later map to existing emotion/state enums in the app.

### 8.1 Neutral / Idle

Purpose:
- default resting state
- most common visible state

Feeling:
- calm, aware, present

Animation ideas:
1. slow blink idle
2. micro side glance
3. tiny widen then settle
4. double-blink rare variant

### 8.2 Happy

Purpose:
- user seen
- positive interaction
- greeting

Feeling:
- warm, excited, friendly

Animation ideas:
1. curved squint-happy blink
2. bright open eyes with tiny bounce
3. short sparkle-like widen
4. wink-like asymmetry variant

### 8.3 Curious

Purpose:
- new object detected
- listening moment
- uncertain observation

Feeling:
- attentive, interested, thinking

Animation ideas:
1. look left then center
2. look right then center
3. one-eye slight squint + focused gaze
4. widen then narrow softly

### 8.4 Sleepy

Purpose:
- low energy
- late-night mood
- inactivity period

Feeling:
- drowsy, soft, low-tempo

Animation ideas:
1. half-lid droop loop
2. long blink with delayed reopen
3. micro drift downward
4. staggered eyelid closing

### 8.5 Sad / Lonely

Purpose:
- neglected state
- no interaction for a while
- low social energy

Feeling:
- muted, vulnerable, quiet

Animation ideas:
1. slightly lowered eye line with slow blink
2. center hold with reduced pupil energy
3. brief downward glance
4. soft narrowing and hold

### 8.6 Excited

Purpose:
- strong positive trigger
- play moment
- favorite interaction

Feeling:
- energetic, alert, eager

Animation ideas:
1. wide-open eyes with quick bounce
2. rapid blink then open
3. outward glance and snap center
4. tiny jitter energy loop

### 8.7 Alert / Surprised

Purpose:
- sudden event
- unexpected input

Feeling:
- sharp awareness

Animation ideas:
1. instant widen hold
2. widen then small recoil
3. blink interruption open
4. asymmetric surprise variant

### 8.8 Thinking / Processing

Purpose:
- response generation
- listening or internal reasoning state

Feeling:
- focused, analytical, internally active

Animation ideas:
1. slow side glance and hold
2. slight squint alternating sides
3. small repeated focus pulse
4. minimal blink with centered stare

---

## 9. Animation Variant System Per State

Each state should define 3 to 4 animation variants.

Recommended structure:

```text
State
 ├─ Variant A: primary/common
 ├─ Variant B: secondary/common
 ├─ Variant C: rare flavor variation
 └─ Variant D: optional special variation
```

### Example for Neutral

- Neutral_A: standard slow blink
- Neutral_B: micro glance left
- Neutral_C: micro glance right
- Neutral_D: double blink

### Runtime behavior recommendation

At runtime:
- choose one primary variant most often
- mix in secondary variants less often
- use rare variants sparingly

Suggested weighting example:
- A = 50%
- B = 20%
- C = 20%
- D = 10%

This gives life without chaos.

---

## 10. Recommended Technical Representation

The implementation should separate art data from behavior logic.

### 10.1 Needed concepts

Recommended conceptual model:
- `PetVisualState`
- `PixelFrame64`
- `AnimationClip`
- `AnimationVariant`
- `AnimationSet`
- `AnimationController`
- `PixelRenderer`

### 10.2 Frame model

A frame should be represented as one of these:
- full 64x64 pixel matrix
- layered component definition
- sparse changed-pixel format relative to a base frame

For maintainability, the best starting point is usually:
- **base eye template + per-frame diffs**

This avoids duplicating the entire 64x64 grid for every small blink change.

### 10.3 Strong recommendation

Do not put raw animation timing logic inside screen composables.

The UI should only ask something like:
- current state
- current clip
- current frame

The animation system itself should decide:
- which variant to play
- when to blink
- when to transition
- how to randomize idle variation

This boundary matters a lot for long-term maintainability.

---

## 11. Suggested First Production State Pack

For the first usable version, do not attempt every possible emotion.

Start with these 5 states:
- Neutral
- Happy
- Curious
- Sleepy
- Thinking

For each state, create:
- 3 variants minimum
- 4 variants ideal

That gives:
- minimum 15 clips
- ideal 20 clips

This is already enough to make the pet feel substantially less repetitive.

---

## 12. Suggested Clip Inventory for Phase 1

### Neutral
- Neutral_A_SlowBlink
- Neutral_B_GlanceLeft
- Neutral_C_GlanceRight
- Neutral_D_DoubleBlink

### Happy
- Happy_A_SoftSquint
- Happy_B_OpenBounce
- Happy_C_WinkAsymmetry
- Happy_D_SparkleOpen

### Curious
- Curious_A_LeftInspect
- Curious_B_RightInspect
- Curious_C_FocusSquint
- Curious_D_WidenSettle

### Sleepy
- Sleepy_A_HalfLidLoop
- Sleepy_B_LongBlink
- Sleepy_C_DroopDrift
- Sleepy_D_StaggerClose

### Thinking
- Thinking_A_SideHold
- Thinking_B_AlternatingSquint
- Thinking_C_FocusPulse
- Thinking_D_CenteredStillness

---

## 13. AI-Assisted Creation Strategy

Yes, AI can help generate this system, but it should be directed carefully.

### 13.1 What AI should generate

AI is well suited to generate:
- rendering engine structure
- frame data classes
- animation clip containers
- state-to-variant mapping
- initial frame drafts for each variant
- test preview screens/tools

### 13.2 What humans should still review

A human should still approve:
- eye shape identity
- emotional readability
- consistency across variants
- timing feel
- whether the character still feels like one pet

### 13.3 Best workflow

Recommended workflow:
1. define style rules first
2. generate one base eye template
3. generate one state at a time
4. review and lock the state style
5. expand to more variants
6. only then scale to full pack

Do not ask AI to generate every state at once without a locked reference style.
That usually produces inconsistency.

---

## 14. Boundaries and Non-Goals

To avoid scope creep, the first version must not try to do these things:
- full body pet animation
- complex particle systems
- full retro pixel UI conversion
- 10+ emotions immediately
- procedural generation of all art without review
- highly elastic tween-based deformation that breaks pixel style

This version is specifically about a **clean, expressive, eye-only pixel pet**.

---

## 15. Definition of Done for This Spec

This spec is considered ready for implementation when the team agrees on:
- fixed 64x64 logical sprite size
- eye-only character direction
- initial state list
- 3 to 4 variants per state
- stable technical separation between state, clip, frame, and renderer
- Phase 1 clip inventory

---

## 16. Final Recommendation

The correct next step is not to code everything immediately.
The correct next step is:

1. lock this visual direction
2. create the base eye template
3. define the first 5 states
4. design 3 to 4 variants per state
5. only then generate the implementation tasks for Codex

This reduces waste and prevents the animation system from becoming a pile of random generated frames.

---

## 17. Implementation Next Step

After this document, the next document should be:

**Pixel Pet Animation Task Breakdown**

That follow-up document should contain:
- data model tasks
- renderer tasks
- animation controller tasks
- preview/debug screen tasks
- per-state frame authoring tasks
- acceptance criteria for each batch

This current document should be treated as the visual and structural source of truth for that task breakdown.

