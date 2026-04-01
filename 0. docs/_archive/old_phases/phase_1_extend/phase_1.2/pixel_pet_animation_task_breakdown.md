# Pixel Pet Eye Animation — Task Breakdown for Claude

Version: v1  
Owner: Hieu Le  
Date: 2026-03-20  
Status: Ready for implementation planning and execution

---

## 1. Purpose

This document converts the **64x64 eye-only pixel pet animation direction** into a Claude-executable implementation plan.

It is designed so an AI coding agent can work batch by batch without drifting into unrelated systems.

This plan assumes:
- Android pet app already exists
- app architecture is modular and event-driven
- animation work must remain **safe, incremental, and buildable after every task**
- the pet remains a **digital creature**, not a generic assistant UI

---

## 2. Product Direction Locked by This Plan

The animation system in this plan is based on these decisions:
- logical sprite size is **64x64**
- style is **pixel art**, but only for the pet avatar area, not the full app UI
- the pet is **eye-focused only**
- no mouth, no nose, no full-body dependency in this phase
- emotions are expressed through:
  - eye shape
  - eyelid openness
  - pupil position
  - blink rhythm
  - gaze direction
  - widening / squinting
  - tiny motion accents
- each visual state should own **3 to 4 animation variants** to avoid robotic repetition

---

## 3. Execution Rules for Claude

Claude must follow these rules for every task in this document.

### 3.1 Do not change project architecture

Do not:
- rewrite module boundaries
- move animation logic into unrelated screens
- bypass existing state/event systems if a proper adapter can be added
- add speculative systems outside task scope

### 3.2 Keep animation logic isolated

The UI layer should not directly own animation timing randomness.

The implementation should preserve a clear separation between:
- visual state
- animation set
- animation clip
- animation frame
- renderer
- controller/runtime selection logic

### 3.3 Prefer vertical slices

Each task must end with something testable, such as:
- a visible preview screen
- a visible animation on the pet
- a deterministic state-mapping result
- a build-safe data model

### 3.4 Avoid fake completion

Do not mark a task done unless:
- the project builds
- the changed slice is verifiable
- the task output is visible or inspectable

### 3.5 Do not over-scope art generation

For early tasks, prioritize:
- engine
- renderer
- state pipeline
- preview tooling
- a small production clip pack

Do not try to generate a huge library of expressive art in one pass.

---

## 4. Target Deliverables

By the end of this plan, the project should have:
- a reusable 64x64 pixel frame representation
- a pixel renderer suitable for Compose-based display
- an animation clip and variant model
- a controller that selects among 3–4 variants per state
- a preview/debug screen for rapid visual validation
- a first usable production pack for these states:
  - Neutral
  - Happy
  - Curious
  - Sleepy
  - Thinking
- integration from existing pet state/emotion output into the pixel avatar runtime

---

## 5. Suggested Batch Order

This plan is organized into 8 batches:

1. **Batch A — Foundations and data model**
2. **Batch B — Pixel renderer and display surface**
3. **Batch C — Animation clip system and runtime controller**
4. **Batch D — Preview/debug tooling**
5. **Batch E — Base eye template and Neutral state pack**
6. **Batch F — Happy and Curious state packs**
7. **Batch G — Sleepy and Thinking state packs**
8. **Batch H — App integration, polish, and verification**

Each batch is intentionally small enough to be implemented safely.

---

# Batch A — Foundations and Data Model

## Goal

Create the minimum reusable animation data model for a 64x64 eye-only pixel pet.

## Scope

This batch only defines core types and module-safe structure. It must not implement rendering yet.

## Must change

- add animation domain types
- define 64x64 frame representation
- define clip / variant / set abstractions
- define pet visual state enum or adapter-facing mapping model

## Must not do

- do not build renderer yet
- do not add UI preview yet
- do not author large frame data yet
- do not integrate into Home screen yet

---

## Task A1 — Define pixel animation domain package

### Goal
Create a clean package/module location for the pixel avatar system.

### Requirements
- introduce a dedicated namespace for pixel avatar animation
- keep naming consistent with current project style
- place types where both renderer and UI integration can use them later without circular dependencies

### Expected output
- a clear domain package/folder for pixel animation
- no runtime behavior yet

### Definition of done
- package exists
- project builds
- no unused placeholder files are left behind

### Verification
- inspect package structure
- build project

---

## Task A2 — Define `PixelFrame64`

### Goal
Represent one 64x64 frame in a way that is safe, explicit, and easy to render.

### Requirements
- lock logical dimensions to 64x64
- representation may be one of these:
  - dense 64x64 color matrix
  - palette-index matrix
  - sparse diff against base frame
- implementation must prefer maintainability over clever compression
- include lightweight validation so malformed frames are caught early

### Must not do
- do not optimize prematurely for storage size
- do not add serialization unless naturally trivial

### Expected output
- a frame type with clear dimensions and pixel access contract

### Definition of done
- invalid dimensions cannot silently pass
- type can be instantiated in test/demo code
- build passes

### Verification
- add or run a small unit test if project conventions allow
- otherwise verify with deterministic local construction

---

## Task A3 — Define palette and color model

### Goal
Provide a stable small-palette model for pixel art consistency.

### Requirements
- define a palette representation usable by frame and renderer
- initial palette can be intentionally small
- support transparent/background handling if needed by renderer approach
- keep API simple and explicit

### Expected output
- reusable palette type and default palette source

### Definition of done
- frame model can reference palette cleanly
- build passes

---

## Task A4 — Define animation clip abstractions

### Goal
Model frame sequences without leaking runtime policy into the UI.

### Requirements
- define types for:
  - animation frame entry
  - animation clip
  - animation variant
  - animation set per state
- frame entries should support per-frame duration or hold timing
- clip should indicate whether it is looping or one-shot

### Expected output
- a structured clip model that can support both preview and runtime selection later

### Definition of done
- types compile
- a small in-code sample clip can be created without hacks

---

## Task A5 — Define pet visual state contract

### Goal
Create the animation-facing visual state vocabulary for eye-only animation.

### Requirements
- define the first production state set:
  - Neutral
  - Happy
  - Curious
  - Sleepy
  - Thinking
- keep room for later expansion without breaking current API
- if existing app already has emotion/state types, do not replace them; define an adapter-safe layer instead

### Must not do
- do not refactor unrelated behavior/state systems

### Definition of done
- runtime code can target these states without ambiguity
- build passes

---

## Task A6 — Define state-to-variant container structure

### Goal
Prepare a structure that can hold 3–4 variants per state.

### Requirements
- support weighted or categorized variant selection later
- support primary/common vs rare variants
- avoid hardcoding random logic here

### Expected output
- a container type for all variants belonging to a state

### Definition of done
- state packs can be registered later without redesign
- build passes

---

## Batch A verification

### Build commands
```bash
./gradlew assembleDebug
./gradlew test
```

### Expected result
- compile/build succeeds
- foundational types are available for later batches

### Batch A remaining risks
- exact frame storage strategy may need minor tuning once renderer is implemented
- palette details may evolve after first real state pack is drawn

---

# Batch B — Pixel Renderer and Display Surface

## Goal

Render a 64x64 pixel frame crisply on screen inside a dedicated avatar surface.

## Scope

This batch only renders static frames. It must not yet implement stateful runtime animation selection.

## Must change

- add renderer for 64x64 pixel frames
- add a reusable Compose display component
- support nearest-neighbor style crisp scaling

## Must not do

- do not wire to production pet state yet
- do not add random clip controller yet

---

## Task B1 — Implement `PixelFrameRenderer`

### Goal
Render one `PixelFrame64` into a visible output.

### Requirements
- output must preserve hard pixel edges
- scaling must not blur the sprite
- renderer should be deterministic and easy to preview
- prefer a simple, readable implementation over advanced rendering tricks

### Expected output
- one reusable renderer component/function

### Definition of done
- a known test frame can be rendered clearly on screen
- scaling looks crisp

---

## Task B2 — Implement `PixelPetAvatar` composable

### Goal
Create a reusable UI surface that shows a pixel pet frame at app-friendly sizes.

### Requirements
- accept a `PixelFrame64`
- accept display size configuration
- support at least compact/standard rendering sizes or a general size param
- keep composable free from animation policy

### Must not do
- do not place state randomness here
- do not let UI decide frame stepping

### Definition of done
- composable can render any supplied frame cleanly
- build passes

---

## Task B3 — Add static sample frame preview

### Goal
Prove renderer correctness using one or more sample eye frames.

### Requirements
- create a minimal sample frame or two
- preview should visually show eye-only design direction
- this is not the final art pack; it is a renderer validation slice

### Definition of done
- sample frame is visible in app or preview tooling
- project builds

---

## Batch B verification

### Manual checks
- edges look crisp, not blurred
- aspect ratio is correct
- frame sits centered and stable

### Build commands
```bash
./gradlew assembleDebug
./gradlew test
```

### Batch B remaining risks
- exact rendering performance on lower-end devices still unverified
- base sample art is temporary and should not be mistaken for final state packs

---

# Batch C — Animation Clip System and Runtime Controller

## Goal

Enable playback of clips and controlled variation selection per state.

## Scope

This batch implements animation timing and clip selection, but still uses simple sample data.

## Must change

- add clip playback engine/controller
- add state pack runtime selection
- support 3–4 variants per state structure

## Must not do

- do not fully integrate with behavior engine yet
- do not author all production clips yet

---

## Task C1 — Implement clip playback timeline

### Goal
Advance through clip frames according to configured durations.

### Requirements
- support looping clips
- support one-shot clips if already modeled
- keep timeline logic separate from UI rendering
- avoid embedding timing rules in composables

### Definition of done
- a sample multi-frame clip visibly advances correctly
- loop behavior is stable

---

## Task C2 — Implement variant selection model

### Goal
Allow a state to choose among 3–4 clip variants instead of replaying one loop forever.

### Requirements
- support weighted selection or an equivalent deterministic-friendly strategy
- design so future randomness can be seeded or debugged
- keep policy in controller, not in frame data

### Definition of done
- runtime can choose different variants for the same state
- selection does not require UI-layer logic

---

## Task C3 — Implement `PixelAnimationController`

### Goal
Own playback state, current clip, current frame, and state transitions.

### Requirements
- accept a visual state input
- select an appropriate variant set
- expose current frame for rendering
- handle transition from one state to another without UI hacks
- preserve stable anchor behavior across states

### Must not do
- do not tie controller directly to HomeScreen timing loops

### Definition of done
- a simple demo can switch states and see clips update correctly
- build passes

---

## Task C4 — Add deterministic debug mode

### Goal
Make animation behavior inspectable during development.

### Requirements
- support forcing a state and forcing a specific variant in debug/preview contexts
- support pausing or stepping if easy within project style
- keep debug hooks isolated from production logic

### Definition of done
- developers can inspect one exact clip repeatedly
- build passes

---

## Batch C verification

### Manual checks
- frame timing is stable
- state change swaps clip families correctly
- debug forcing works

### Build commands
```bash
./gradlew assembleDebug
./gradlew test
```

### Batch C remaining risks
- natural-feeling randomness still depends on real state packs being authored
- transition behavior may need tuning once production states are integrated

---

# Batch D — Preview and Debug Tooling

## Goal

Create a dedicated tooling surface so Claude and humans can validate pixel animation quickly without relying on full product flows.

## Scope

This batch builds preview/debug UI only.

## Must change

- add preview screen or debug route
- expose state and variant inspection controls
- support visual review of frame/clip output

## Must not do

- do not couple preview tool to production navigation flow unnecessarily
- do not add unrelated debug systems

---

## Task D1 — Add Pixel Animation Preview screen

### Goal
Provide one place to inspect the eye-only pet animation system.

### Requirements
- show current frame/animation visibly at useful scale
- allow selecting visual state
- allow selecting specific variant or auto mode
- display simple metadata such as current state, clip name, variant name, or frame index if available

### Definition of done
- screen is reachable in a safe debug/dev path
- animation is inspectable without triggering unrelated app flows

---

## Task D2 — Add playback controls

### Goal
Support review without rebuilding for every change.

### Requirements
- play / pause if feasible
- restart current clip
- force auto rotation or single-variant mode
- keep controls lightweight and dev-focused

### Definition of done
- reviewer can inspect loops and compare variants interactively

---

## Task D3 — Add reference overlay or alignment aid

### Goal
Reduce art drift and anchor inconsistency across frames.

### Requirements
- optionally show grid, center lines, or bounding guide in debug mode only
- must not appear in production avatar output

### Definition of done
- frame alignment can be checked visually
- debug-only behavior is clear

---

## Batch D verification

### Manual checks
- preview screen is reachable
- switching states is fast
- alignment guide helps compare frames

### Build commands
```bash
./gradlew assembleDebug
./gradlew test
```

### Batch D remaining risks
- art quality is still limited by current sample assets until state packs are authored

---

# Batch E — Base Eye Template and Neutral State Pack

## Goal

Establish the visual identity of the pet and ship the first real production state pack.

## Scope

This batch defines the base eye template and all Neutral variants.

## Must change

- create canonical eye style
- create production-ready Neutral clips
- wire them into preview/runtime system

## Must not do

- do not try to finalize every emotion yet
- do not redesign the renderer/controller in this batch unless strictly necessary

---

## Task E1 — Create base eye template

### Goal
Lock the character identity before scaling variant production.

### Requirements
- define the canonical eye spacing, eye size, pupil style, and baseline openness
- keep design readable at 64x64
- keep silhouette simple and memorable
- base template must support later widening, squinting, drooping, and glance motion

### Definition of done
- one approved base eye template exists in code/data form
- all later states can derive from this template

---

## Task E2 — Author `Neutral_A_SlowBlink`

### Goal
Create the main idle loop.

### Requirements
- calm and alive, not sleepy
- blink timing should feel natural
- frame count should stay small and readable

### Definition of done
- clip is integrated and visible in preview

---

## Task E3 — Author `Neutral_B_GlanceLeft`

### Goal
Add subtle curiosity without changing state family.

### Requirements
- glance should be small and believable
- return to center cleanly
- preserve character identity

---

## Task E4 — Author `Neutral_C_GlanceRight`

### Goal
Mirror or complement left glance without feeling copy-pasted.

### Requirements
- can be symmetric if style allows, but timing/hold can differ slightly if beneficial

---

## Task E5 — Author `Neutral_D_DoubleBlink`

### Goal
Add a rare idle flavor variant to reduce robotic repetition.

### Requirements
- use sparingly in runtime weighting
- must remain unmistakably Neutral, not Sleepy

---

## Task E6 — Register Neutral state pack

### Goal
Ship the first full 4-variant state pack.

### Requirements
- register all Neutral variants under one state set
- assign sensible default weighting
- verify auto-selection behavior

### Suggested weighting
- A = 50%
- B = 20%
- C = 20%
- D = 10%

### Definition of done
- auto mode rotates Neutral variants in a believable way
- preview can force each one individually

---

## Batch E verification

### Manual checks
- Neutral feels alive without being busy
- variants are recognizable as the same pet
- no clipping or anchor drift appears

### Build commands
```bash
./gradlew assembleDebug
./gradlew test
```

### Batch E remaining risks
- final eye identity may still need one round of refinement before all later emotions are authored

---

# Batch F — Happy and Curious State Packs

## Goal

Expand the pet into positive and attentive expression families while preserving the same character identity.

## Scope

This batch authors 2 production state packs.

---

## Task F1 — Author Happy state pack

### Goal
Create 3–4 Happy variants.

### Required variants
At minimum implement 3, ideally 4:
- Happy_A_SoftSquint
- Happy_B_OpenBounce
- Happy_C_WinkAsymmetry
- Happy_D_SparkleOpen

### Requirements
- Happy must read immediately from the eyes alone
- avoid turning Happy into Alert/Surprised by over-widening too much
- asymmetry should feel charming, not broken

### Definition of done
- Happy state has at least 3 working production clips
- preview and controller can play them

---

## Task F2 — Author Curious state pack

### Goal
Create 3–4 Curious variants.

### Required variants
At minimum implement 3, ideally 4:
- Curious_A_LeftInspect
- Curious_B_RightInspect
- Curious_C_FocusSquint
- Curious_D_WidenSettle

### Requirements
- Curious should feel attentive and observant
- eye motion should be controlled, not hyperactive
- maintain same base character proportions

### Definition of done
- Curious state has at least 3 working production clips
- preview and controller can play them

---

## Task F3 — Tune weighting and state readability

### Goal
Prevent different packs from collapsing into the same emotional read.

### Requirements
- compare Neutral vs Happy vs Curious in preview
- tune clips if two states feel visually too similar
- keep changes localized to frame data or state registration, not architecture

### Definition of done
- the three states are visually distinguishable at a glance

---

## Batch F verification

### Manual checks
- Happy reads as friendly/positive
- Curious reads as attentive/inquisitive
- no state looks like a different pet

### Build commands
```bash
./gradlew assembleDebug
./gradlew test
```

### Batch F remaining risks
- stronger emotion differentiation may still need refinement once Sleepy and Thinking are added

---

# Batch G — Sleepy and Thinking State Packs

## Goal

Complete the first production emotional range with low-energy and internal-processing expressions.

## Scope

This batch authors 2 production state packs.

---

## Task G1 — Author Sleepy state pack

### Goal
Create 3–4 Sleepy variants.

### Required variants
At minimum implement 3, ideally 4:
- Sleepy_A_HalfLidLoop
- Sleepy_B_LongBlink
- Sleepy_C_DroopDrift
- Sleepy_D_StaggerClose

### Requirements
- Sleepy must read as low energy, not sad or broken
- long holds are acceptable if they feel intentional
- preserve eye identity even when lids are more closed

### Definition of done
- Sleepy state has at least 3 working clips

---

## Task G2 — Author Thinking state pack

### Goal
Create 3–4 Thinking variants.

### Required variants
At minimum implement 3, ideally 4:
- Thinking_A_SideHold
- Thinking_B_AlternatingSquint
- Thinking_C_FocusPulse
- Thinking_D_CenteredStillness

### Requirements
- Thinking should feel focused and internal
- avoid making it look like Sad or Curious by mistake
- motion should be restrained and deliberate

### Definition of done
- Thinking state has at least 3 working clips

---

## Task G3 — Cross-state review and cleanup

### Goal
Validate all 5 production states together.

### Requirements
- compare Neutral, Happy, Curious, Sleepy, Thinking side by side
- fix anchor drift, palette inconsistencies, or state ambiguity
- avoid large code changes; prefer asset/frame cleanup

### Definition of done
- all 5 state packs feel cohesive as one pet

---

## Batch G verification

### Manual checks
- Sleepy is clearly distinct from Neutral
- Thinking is clearly distinct from Curious
- all five states preserve the same pet identity

### Build commands
```bash
./gradlew assembleDebug
./gradlew test
```

### Batch G remaining risks
- some states may still need tuning after real app integration and live behavior triggers

---

# Batch H — App Integration, Mapping, and Final Verification

## Goal

Connect the pixel eye animation system to the real app flow without polluting architecture or UI screens.

## Scope

This batch is integration and stabilization only.

## Must change

- map existing app state/emotion outputs into pixel visual states
- replace or add avatar rendering path in target UI surface
- preserve preview/debug tooling

## Must not do

- do not rewrite pet behavior logic
- do not refactor unrelated app screens broadly
- do not hardcode animation timing in HomeScreen

---

## Task H1 — Add adapter from app state/emotion to pixel visual state

### Goal
Bridge existing runtime outputs to the new pixel avatar system.

### Requirements
- create an adapter layer rather than rewriting current state model
- map existing pet emotion/state into:
  - Neutral
  - Happy
  - Curious
  - Sleepy
  - Thinking
- handle unmapped inputs safely with a sensible fallback

### Definition of done
- integration does not force broad upstream changes
- fallback behavior is explicit

---

## Task H2 — Integrate `PixelPetAvatar` into target app surface

### Goal
Show the pixel pet in the real product flow.

### Requirements
- integrate into the intended pet/avatar area only
- preserve rest of app UI as normal modern UI
- ensure rendering updates when visual state changes

### Definition of done
- app shows production pixel pet in the target surface
- state changes visibly affect the avatar

---

## Task H3 — Preserve debug route and inspection hooks

### Goal
Make future iteration safe after production integration.

### Requirements
- preview/debug path must remain available for artists and agents
- integration must not remove direct clip inspection ability

### Definition of done
- developers can still inspect state packs without navigating full behavior flows

---

## Task H4 — Final stabilization pass

### Goal
Ship a coherent first production version.

### Requirements
- remove temporary sample frames no longer needed
- clean obvious naming issues
- confirm no dead code from early scaffolding remains
- verify no direct UI timing hacks slipped in during integration

### Definition of done
- build passes
- first production-ready pixel eye animation system is present

---

## Batch H verification

### Manual checks
- app opens and shows pixel pet correctly
- state changes are visible in the real UI
- preview/debug tooling still works
- avatar area remains crisp and stable

### Build commands
```bash
./gradlew assembleDebug
./gradlew test
```

### Batch H remaining risks
- long-term art authoring workflow may later need dedicated tooling if state count expands far beyond five core states
- future non-eye features must preserve the same renderer/controller boundaries

---

# 6. Task Authoring Template for Claude

Claude should use this exact structure when executing any single task from the plan.

## Task Header
- Task ID
- Title

## Goal
- what the task is trying to achieve

## Scope
- what this task changes
- what this task must not change

## Current context / risks
- relevant constraints from previous batch
- specific boundaries to avoid drift

## Implementation requirements
- required types, UI, behavior, or mappings
- constraints on architecture and module boundaries

## Definition of done
- visible or testable result
- build expectations

## Verification
- manual verification steps
- exact build/test commands

## Required output
- summary of changes
- files changed
- how to verify
- build result
- remaining risks

---

# 7. Recommended Execution Order for Claude

Claude should not attempt the whole document in one pass.

Recommended order:
1. finish Batch A completely
2. finish Batch B completely
3. finish Batch C completely
4. build preview tool in Batch D
5. lock base eye identity in Batch E before scaling art generation
6. expand packs through F and G
7. only then perform Batch H integration

This order reduces wasted art and prevents architectural churn.

---

# 8. Final Recommendation

The most important discipline in this work is this:

> lock the engine and the base eye identity first, then expand state packs, then integrate.

If Claude skips that order and tries to generate all emotions immediately, the likely result is:
- inconsistent art
- duplicated logic
- weak state readability
- rework across renderer and controller

This plan is intentionally structured to avoid that failure mode.
