# Rive Pet Animation Adoption Spec

## Purpose

This document defines the first-pass Rive integration contract for the Android pet brain.
It is intentionally constrained to the current Phase 1.1 alive-pet behavior model so future
asset work can plug into code that already exists without rethinking product semantics.

## Current code contract

Animation runtimes should treat `PetAnimationFrame` and `PetAnimationTrigger` as the source of
truth for animation-facing inputs.

### Continuous/stateful inputs

`PetAnimationFrame` carries:

- `emotion`
  - `CALM`
  - `HAPPY`
  - `CURIOUS`
  - `SLEEPY`
  - `SAD`
  - `EXCITED`
  - `HUNGRY`
- `energyBand`
  - `LOW`
  - `MID`
  - `HIGH`
- `hungerBand`
  - `LOW`
  - `MID`
  - `HIGH`
- `socialBand`
  - `LOW`
  - `MID`
  - `HIGH`
- `flavor`
  - `BALANCED`
  - `PLAYFUL`
  - `CALM`
  - `CURIOUS`
  - `AFFECTIONATE`

These inputs are stable, bounded, and deliberately avoid exposing raw 0..100 product values to
Rive state machines.

### Trigger/event inputs

`PetAnimationTrigger` carries:

- `reactionType`
  - `NONE`
  - `GREETING`
  - `TAP`
  - `LONG_PRESS`
  - `FEED`
  - `PLAY`
  - `REST`
  - `SOUND`
  - `REACTION`
- `emotion` override (optional)
- `greetingType` (for greeting triggers)
  - `CALM`
  - `WARM`
  - `HUNGRY`
  - `SLEEPY`
  - `LONELY`
  - `CURIOUS`
  - `PLAYFUL`
- `activityResult` (for care/action triggers)
  - `FEED`
  - `PLAY`
  - `REST`

## Mapping source of truth

The app currently maps product state into animation inputs through `PetAnimationInputMapper`.
That mapping must remain the single place that translates:

- `PetState`
- derived `PetCondition`s
- selected `PetEmotion`
- trait-driven flavor (`PetTrait` -> `PetAnimationFlavor`)
- behavior decisions for greetings/interactions/activities
- sound categories

Future Rive work must not duplicate product rules in composables or asset glue code.

## Required motion set for first Rive pass

The first asset drop should support these motion buckets only:

1. **Idle presence**
   - calm idle
   - playful/alert idle
   - sleepy idle
   - affectionate/soft idle
   - blink support

2. **Greeting**
   - calm greeting
   - warm greeting
   - hungry greeting
   - sleepy greeting
   - lonely greeting
   - curious greeting
   - playful greeting

3. **Touch reactions**
   - tap reaction
   - long-press reaction

4. **Care/activity reactions**
   - feed reaction
   - play reaction
   - rest reaction

5. **Sound reaction**
   - surprised / listening-style reaction for current sound-driven behavior

6. **Generic reaction slot**
   - small positive/neutral transient used by current non-gameplay success feedback

## Suggested first Rive state-machine shape

Use one main state machine with:

### State inputs

- enum/int input for `emotion`
- enum/int input for `energyBand`
- enum/int input for `hungerBand`
- enum/int input for `socialBand`
- enum/int input for `flavor`

### Trigger inputs

- trigger `greeting`
- trigger `tap`
- trigger `long_press`
- trigger `feed`
- trigger `play`
- trigger `rest`
- trigger `sound`
- trigger `reaction`

### Optional helper inputs

- enum/int input for `greetingType`
- enum/int input for `activityResult`
- enum/int input for trigger `emotion` override

## Behavioral expectations to preserve

Future Rive integration must preserve these product semantics:

- Home screen remains animation-runtime agnostic and consumes only `PetAnimationState`.
- Animation mapping continues to reflect the real pet state and derived conditions.
- Trait influence stays bounded before animation and is represented as animation flavor, not as
  arbitrary asset-side logic.
- Fake and Rive runtimes stay swappable through `PetAnimator` and the animator factory.
- Greeting, tap, long press, feed, play, rest, and sound reactions remain distinct.
- Diary/memory logic stays independent from animation runtime changes.
- No profile identity is persisted into animation state/history.

## What this spec explicitly does not require yet

- shipped `.riv` assets
- final art direction
- production tuning for every timing curve
- replacing the fake animator immediately
- redesigning the Home screen UI shell

## Handoff checklist for future Rive work

Before swapping the placeholder adapter with a real Rive implementation, verify:

1. every `PetAnimationFrame` field has a matching Rive input
2. every `PetAnimationTrigger.reactionType` has a matching Rive trigger path
3. greeting subtype handling exists for all current `PetAnimationGreetingType` values
4. feed/play/rest remain visually distinct
5. sound reaction remains supported
6. idle + blink still work without user interaction
7. the runtime can coexist with the current `PetAnimatorFactory` seam
