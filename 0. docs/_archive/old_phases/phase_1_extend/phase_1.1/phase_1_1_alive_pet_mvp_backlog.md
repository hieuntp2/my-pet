# PHASE 1.1 — ALIVE PET MVP BACKLOG

## Purpose

This backlog converts the **Phase 1.1 — Alive Pet MVP Plan** into **execution-ready batches and tasks** for AI coding agents such as Codex.

This phase is not about adding more systems.
It is about turning the current foundation into a **real pet product experience**.

Core principle:

- Build the **heart first** (`PetState`)
- Then build the **user-facing shell** (`Home`)
- Then build **playable interaction**
- Then make **personality visible**
- Then make **memory emotional**
- Then upgrade **animation infrastructure**

---

## Global Rules For Every Task

- Follow `AGENTS.md` first.
- Keep the project offline-first.
- Keep the project event-driven.
- Keep Room persistence real.
- Do not add mock production logic.
- Do not leave TODOs, empty methods, or placeholder business logic.
- Keep `PetProfile`, `PetState`, and `PetTrait` separated.
- Do not couple identity storage to runtime state.
- Do not snapshot mutable profile fields into secondary persistence paths unless explicitly required.
- Keep build green whenever the environment allows it.
- If build/test is blocked by environment issues, report that honestly and distinguish:
  - source-level completion
  - build validation status
  - emulator/runtime validation status

---

# Batch A — Pet State Foundation

## Batch Goal

Introduce a **real, persistent, time-evolving pet state** that can drive UX, gameplay, greeting, and future animation.

## Batch Success Criteria

- Pet has a real saved state
- State changes after time passes
- App open applies decay
- Greeting depends on state
- State is ready to drive UI and animation abstraction

---

## Task A1
**Task ID:** P11-A1  
**Title:** Define PetState domain model and persistence schema

### Goal
Create the real state model that represents the internal condition of the pet.

### Scope
- Add `PetState` model
- Add Room entity + DAO
- Register schema in database
- No lifecycle logic yet

### Required fields
- mood
- energy
- hunger
- social
- bond
- sleepiness
- lastUpdatedAt

### Definition of Done
- `PetState` compiles
- Room can persist and read the state
- Database schema is updated properly

### Risks to watch
- Do not merge `PetState` into `PetProfile`
- Do not treat raw derived conditions as persisted source-of-truth

---

## Task A2
**Task ID:** P11-A2  
**Title:** Implement PetStateRepository with default initialization

### Goal
Ensure the app always has one valid current state.

### Scope
- Add repository for load/update/create
- Create default state on first load if missing
- Expose current state in a clean API

### Default baseline
- mood: NEUTRAL
- energy: 70
- hunger: 30
- social: 50
- bond: 0
- sleepiness: 20

### Definition of Done
- Fresh install gets a valid state
- Existing state is reused
- No null pet state path remains

### Risks to watch
- Keep initialization deterministic
- Do not hide state creation in random UI code

---

## Task A3
**Task ID:** P11-A3  
**Title:** Implement PetState decay engine for time-based evolution

### Goal
Make the pet feel different when the user comes back later.

### Scope
- Add decay rules based on elapsed time
- Clamp values to safe ranges
- Update `lastUpdatedAt`

### Required effects
- energy decreases over time
- hunger increases over time
- sleepiness increases over time
- social gradually decreases over time

### Definition of Done
- Time delta changes state meaningfully
- State remains bounded and safe
- Logic is centralized and testable

### Risks to watch
- Avoid overly aggressive decay
- Avoid scattered decay math across multiple files

---

## Task A4
**Task ID:** P11-A4  
**Title:** Add derived condition resolver from raw PetState

### Goal
Translate raw values into meaningful pet conditions.

### Scope
- Add resolver for conditions such as:
  - HUNGRY
  - SLEEPY
  - LONELY
  - PLAYFUL
  - CALM
- Keep conditions derived, not primary persisted state

### Definition of Done
- Same input state always yields explainable conditions
- Resolver is reusable from greeting, UI, gameplay, and animation layers

### Risks to watch
- Do not persist every derived condition as a separate primary field
- Keep logic deterministic

---

## Task A5
**Task ID:** P11-A5  
**Title:** Apply state decay on app open and save resulting state

### Goal
Ensure the pet evolves when the app is reopened.

### Scope
- Hook state load + decay into app-open flow
- Persist resulting state after decay
- Keep integration minimal and production-safe

### Definition of Done
- Reopening after time gap updates state
- State is saved after lifecycle application
- Existing app-open flow remains stable

### Risks to watch
- Do not run decay repeatedly in the same session by mistake
- Keep app-open lifecycle entry point small and clear

---

## Task A6
**Task ID:** P11-A6  
**Title:** Add state-aware greeting selection on app open

### Goal
Make app open feel alive instead of static.

### Scope
- Resolve greeting category from current state + derived conditions
- Support at least:
  - calm greeting
  - happy greeting
  - sleepy greeting
  - needy/hungry greeting
- Emit greeting through existing event/reaction flow

### Definition of Done
- App open greeting changes based on state
- Greeting is brief and visible
- Greeting path is event-observable

### Risks to watch
- Do not let greeting become random noise
- Keep greeting grounded in actual state

---

## Task A7
**Task ID:** P11-A7  
**Title:** Introduce PetAnimator abstraction contract for future Rive integration

### Goal
Prepare the animation layer now so the app can ship behavior before real Rive assets arrive.

### Scope
- Define `PetAnimator` interface or equivalent contract
- Include inputs/actions needed for:
  - mood / emotion
  - greeting
  - reaction
  - tap
  - long press
  - activity result
  - sound reaction
- No real Rive implementation yet

### Definition of Done
- Brain and UI can depend on the abstraction instead of direct animation details
- Contract is future-compatible with Rive state-machine inputs and triggers

### Risks to watch
- Do not couple animation contract to Compose-specific implementation details
- Do not put business logic inside the animator contract

---

## Task A8
**Task ID:** P11-A8  
**Title:** Implement temporary FakePetAnimator for pre-Rive development

### Goal
Allow gameplay and state work to proceed before real animation assets exist.

### Scope
- Add simple implementation backed by existing UI/Compose state
- Support visible but lightweight reactions
- Keep implementation swappable

### Definition of Done
- App can render visible reactions through the abstraction
- No behavior logic depends on direct Compose animation calls

### Risks to watch
- Keep it temporary and minimal
- Do not let the fake implementation become the architecture

---

# Batch B — Home UX

## Batch Goal

Replace the current debug-heavy main screen with a **real pet home screen** that users can understand instantly.

## Batch Success Criteria

- Home centers on pet presence
- Main screen shows condition, not debug logs
- Quick actions are visible
- Debug remains accessible but separate

---

## Task B1
**Task ID:** P11-B1  
**Title:** Redesign Home screen information architecture for pet-first UX

### Goal
Define the new main screen layout and remove the developer-console feel.

### Scope
- Center avatar/pet presence
- Show pet name
- Show top-level status
- Reserve area for quick actions
- Reserve separate access path for diary/debug

### Definition of Done
- Main screen layout is pet-first
- Raw engineering logs are no longer the primary surface

### Risks to watch
- Do not turn Home into a dashboard wall
- Keep information density low

---

## Task B2
**Task ID:** P11-B2  
**Title:** Show current pet name and identity prominently on Home

### Goal
Make the pet feel like an individual rather than a generic widget.

### Scope
- Load active profile
- Show current pet name
- Keep identity rendering stable even before onboarding improvements

### Definition of Done
- Home clearly identifies the current pet
- Name comes from real profile persistence

### Risks to watch
- Do not hardcode pet name into UI
- Keep identity display separate from runtime state

---

## Task B3
**Task ID:** P11-B3  
**Title:** Add state indicators for mood, energy, hunger, and social need

### Goal
Make the pet condition visible at a glance.

### Scope
- Add simple UI indicators or chips/bars
- Show only the most useful current state
- Keep labels understandable for normal users

### Definition of Done
- User can understand how the pet is doing in seconds
- UI is driven from real `PetState`

### Risks to watch
- Avoid over-exposing raw internal numbers if the UI becomes noisy
- Keep mapping clear and user-facing

---

## Task B4
**Task ID:** P11-B4  
**Title:** Add one-line pet status summary text on Home

### Goal
Humanize the current condition into readable language.

### Scope
- Generate short status line such as:
  - “Cún looks playful today”
  - “Cún seems sleepy”
  - “Cún is getting hungry”
- Drive from current state and conditions

### Definition of Done
- Status text is readable and meaningful
- Text updates when state changes

### Risks to watch
- Keep wording deterministic and simple
- Do not drift into chatbot-style prose

---

## Task B5
**Task ID:** P11-B5  
**Title:** Move raw debug/event logs out of the main Home surface

### Goal
Stop the app from feeling like an internal tool.

### Scope
- Remove raw event list from main user surface
- Keep access through Debug screen or debug section only
- Preserve observability for developers

### Definition of Done
- Home is no longer dominated by technical logs
- Debug functionality still exists

### Risks to watch
- Do not delete useful debug tooling
- Just move it out of the main product flow

---

## Task B6
**Task ID:** P11-B6  
**Title:** Add minimal navigation shell for Home, Diary, and Debug

### Goal
Give the app a simple usable structure.

### Scope
- Ensure user can reach:
  - Home
  - Diary
  - Debug
- Keep navigation lightweight

### Definition of Done
- Core routes are accessible
- Home is the clear default landing surface

### Risks to watch
- Avoid large navigation redesign
- Keep structure stable and minimal

---

## Task B7
**Task ID:** P11-B7  
**Title:** Bind Home avatar surface through PetAnimator abstraction

### Goal
Make the Home avatar ready for future Rive replacement without reworking the behavior layer.

### Scope
- Render current avatar through abstraction-driven state
- Ensure Home does not depend on direct animation internals
- Preserve fake animator path for now

### Definition of Done
- Home reacts through animation abstraction
- Avatar container is ready for later Rive adapter

### Risks to watch
- Do not hardwire Home to temporary animation implementation
- Keep contract stable

---

# Batch C — Core Gameplay

## Batch Goal

Turn the pet from a passive display into something the user can **interact with and care for**.

## Batch Success Criteria

- User has meaningful actions
- Pet responds visibly
- State changes are saved
- Events are emitted for memory and debugging
- Main loop becomes playable

---

## Task C1
**Task ID:** P11-C1  
**Title:** Add quick action controls for Feed, Play, and Rest on Home

### Goal
Expose real care actions in the main product loop.

### Scope
- Add visible quick actions
- Place them near the pet interaction area
- Keep one-tap simple UX

### Definition of Done
- Feed, Play, and Rest are tappable from Home
- Layout remains pet-centric

### Risks to watch
- Do not bury actions in menus
- Do not overload the first version with too many controls

---

## Task C2
**Task ID:** P11-C2  
**Title:** Implement Feed action state updates and event emission

### Goal
Let the user feed the pet and see meaningful results.

### Scope
- Hunger decreases
- Mood may improve
- Bond may increase slightly
- Emit event for successful feed

### Definition of Done
- Feed changes saved state
- Feed emits a real event
- Feed produces a visible reaction through animator abstraction

### Risks to watch
- Avoid making feed a full reset
- Keep delta bounded

---

## Task C3
**Task ID:** P11-C3  
**Title:** Implement Play action state updates and event emission

### Goal
Let the user play with the pet.

### Scope
- Mood improves
- Social improves
- Bond increases
- Energy decreases slightly
- Emit event for successful play

### Definition of Done
- Play changes saved state
- Play emits a real event
- Play produces visible reaction

### Risks to watch
- Avoid making play always optimal regardless of current state
- Keep action believable

---

## Task C4
**Task ID:** P11-C4  
**Title:** Implement Rest action state updates and event emission

### Goal
Let the user help the pet recover.

### Scope
- Energy increases
- Sleepiness decreases
- Mood may shift calmer
- Emit event for successful rest

### Definition of Done
- Rest changes saved state
- Rest emits a real event
- Rest produces visible reaction

### Risks to watch
- Avoid instant full restore behavior
- Keep recovery bounded

---

## Task C5
**Task ID:** P11-C5  
**Title:** Stabilize tap interaction as a meaningful pet action

### Goal
Make tapping feel good and useful instead of cosmetic or unclear.

### Scope
- Ensure tap path is visible
- Update state with small bounded effect
- Emit event
- Respect cooldown

### Definition of Done
- Tap gives clear visible feedback
- Tap changes state slightly
- Tap is preserved as lightweight interaction

### Risks to watch
- Do not let tap spam break the loop
- Keep tap distinct from care actions

---

## Task C6
**Task ID:** P11-C6  
**Title:** Stabilize long-press interaction as a distinct intentional action

### Goal
Give long press a clearly different meaning from tap.

### Scope
- Different event
- Different reaction
- Different state effect
- Keep gesture handling clean

### Definition of Done
- Long press feels intentionally different
- No accidental double-trigger with short tap path

### Risks to watch
- Gesture conflict
- Too much overlap with tap behavior

---

## Task C7
**Task ID:** P11-C7  
**Title:** Add user-facing cooldown and feedback rules for repeated interactions

### Goal
Prevent spammy or confusing repeated actions.

### Scope
- Apply cooldown policy to tap and actions where necessary
- Preserve responsiveness
- Optional light user feedback when action is temporarily blocked

### Definition of Done
- Interaction spam is controlled
- UI does not feel frozen or broken

### Risks to watch
- Do not over-throttle normal usage
- Keep policy simple and explainable

---

## Task C8
**Task ID:** P11-C8  
**Title:** Add human-readable feedback text for successful actions

### Goal
Translate pet actions into understandable emotional feedback.

### Scope
- Add short feedback lines after actions, such as:
  - “Cún enjoyed that”
  - “Cún looks more energetic”
  - “Cún seems satisfied”
- Keep brief and deterministic

### Definition of Done
- Users see immediate readable outcome after actions
- Text aligns with actual state effects

### Risks to watch
- Do not add verbose narration
- Do not show text that contradicts state change

---

## Task C9
**Task ID:** P11-C9  
**Title:** Validate and harden the core gameplay loop end-to-end

### Goal
Ensure the main loop works reliably:
open → greet → interact → react → state change → event saved

### Scope
- Review broken links between UI, behavior, state, and memory events
- Fix silent failure paths
- Keep integration minimal

### Definition of Done
- No dead action path remains
- Successful actions consistently update UI, state, and events

### Risks to watch
- Silent event loss
- State update without UI refresh
- UI feedback without actual saved state

---

# Batch D — Personality Visibility

## Batch Goal

Make the pet’s personality feel **real and visible**, not just stored in the database.

## Batch Success Criteria

- Traits bias reactions
- Greeting/idle/interaction visibly differ based on traits
- Debug surface explains why
- Personality remains bounded and believable

---

## Task D1
**Task ID:** P11-D1  
**Title:** Review and normalize trait update rules for gameplay-driven personality shaping

### Goal
Ensure trait changes are slow, cumulative, and aligned with real user behavior.

### Scope
- Review trait update rules from interactions and activities
- Normalize scaling and bounds
- Ensure repeated actions affect traits gradually

### Definition of Done
- Trait updates are stable and believable
- No single action causes dramatic personality shifts

### Risks to watch
- Overly aggressive trait growth
- Hidden inconsistent update logic

---

## Task D2
**Task ID:** P11-D2  
**Title:** Apply trait-aware weighting to greeting reactions

### Goal
Make the first moment of app open feel more personal.

### Scope
- Let traits bias greeting style/intensity
- Keep actual state and conditions as primary guardrails
- Preserve greeting brevity

### Definition of Done
- Two pets with different traits can greet differently in the same general condition
- Greeting remains explainable

### Risks to watch
- Do not let traits override tired/hungry state reality
- Keep trait influence bounded

---

## Task D3
**Task ID:** P11-D3  
**Title:** Apply trait-aware weighting to tap and long-press reactions

### Goal
Make basic interactions feel different across personalities.

### Scope
- Use traits to bias selected reaction among acceptable candidates
- Preserve event and state flow
- Keep randomness bounded if used

### Definition of Done
- Same input can produce slightly different believable responses across trait profiles
- Differences remain debug-explainable

### Risks to watch
- Do not produce chaotic reaction variance
- Keep reaction categories readable

---

## Task D4
**Task ID:** P11-D4  
**Title:** Apply trait-aware weighting to Feed, Play, and Rest reaction outcomes

### Goal
Make activities feel more expressive depending on personality.

### Scope
- Playful pet responds more enthusiastically to Play
- Lazy pet responds more calmly to Rest
- Social pet responds more warmly to care actions
- Keep state deltas grounded in action rules

### Definition of Done
- Activities feel more personal
- Trait influence changes reaction flavor, not core action correctness

### Risks to watch
- Do not make actions ineffective because of personality
- Preserve consistency and trust

---

## Task D5
**Task ID:** P11-D5  
**Title:** Add lightweight personality summary surfaced to the user

### Goal
Start making personality user-visible without overwhelming the Home screen.

### Scope
- Add subtle visible signal such as:
  - “Playful today”
  - “Curious mood”
  - “Calm personality”
- Prefer lightweight surface over raw trait numbers

### Definition of Done
- User can start noticing personality differences
- UI remains simple

### Risks to watch
- Do not expose developer-style trait matrix on Home
- Keep wording stable

---

## Task D6
**Task ID:** P11-D6  
**Title:** Expand debug explainability for trait-influenced reaction selection

### Goal
Ensure developers can understand why a reaction was selected.

### Scope
- Show current state
- Show relevant traits
- Show selected reaction
- Show key weights or reasoning inputs

### Definition of Done
- Behavior choices are inspectable
- Tuning can happen without guesswork

### Risks to watch
- Avoid overly verbose debug noise
- Keep it focused on decision clarity

---

# Batch E — Memory UX

## Batch Goal

Turn event history into **emotionally readable memory** and make the diary worth opening.

## Batch Success Criteria

- Diary shows human-readable entries
- Important moments are highlighted
- Daily continuity exists
- Home gets a lightweight “today with pet” feeling

---

## Task E1
**Task ID:** P11-E1  
**Title:** Expand event-to-memory mapping for gameplay and greeting events

### Goal
Ensure the new real pet loop generates meaningful diary-ready memories.

### Scope
- Map greeting
- Map tap
- Map long press
- Map feed/play/rest
- Map sound reaction where appropriate
- Filter low-level noise

### Definition of Done
- Supported product-level events generate readable memory cards
- Diary remains concise and emotional

### Risks to watch
- Do not promote every technical event into diary content
- Keep summaries deterministic

---

## Task E2
**Task ID:** P11-E2  
**Title:** Improve memory-card wording for emotional readability

### Goal
Make memory entries feel like pet moments, not engineering translations.

### Scope
- Refine titles and summaries
- Keep short and repeatable
- Ensure wording matches actual underlying event meaning

### Definition of Done
- Memory cards are readable and product-appropriate
- No log-style wording remains in main diary content

### Risks to watch
- Avoid fake narrative flourish
- Avoid overfitting wording to one pet name or identity snapshot

---

## Task E3
**Task ID:** P11-E3  
**Title:** Add notable-moment rules for stronger diary highlights

### Goal
Help the diary surface moments that feel special.

### Scope
- Examples:
  - first feed of the day
  - first interaction after long absence
  - unusually high-energy session
  - notably sleepy/hungry return
- Keep deterministic rules

### Definition of Done
- Special moments are visibly distinguished
- Highlights are explainable from real persisted data

### Risks to watch
- Do not over-highlight routine actions
- Keep rules simple

---

## Task E4
**Task ID:** P11-E4  
**Title:** Add lightweight daily summary generation for recent pet activity

### Goal
Give users a quick sense of the pet’s day.

### Scope
- Generate short day-level summary from persisted events/state
- Handle empty days honestly
- Keep summary concise

### Definition of Done
- Daily summaries are based on real data
- Diary can show day-level continuity

### Risks to watch
- Do not invent content for empty days
- Do not create second source-of-truth persistence

---

## Task E5
**Task ID:** P11-E5  
**Title:** Surface a simple “today with your pet” summary on Home

### Goal
Bring memory and continuity back into the main surface.

### Scope
- Add one lightweight home summary card/line
- Show recent notable memory or daily summary snippet
- Keep non-intrusive

### Definition of Done
- Home feels more alive and continuous
- Summary is backed by real memory/daily-summary logic

### Risks to watch
- Keep Home uncluttered
- Avoid duplicating the full diary on Home

---

## Task E6
**Task ID:** P11-E6  
**Title:** Improve diary empty and low-activity states

### Goal
Make the diary honest and still product-appropriate when little data exists.

### Scope
- Empty state for no memories
- Low-activity state for sparse data
- Avoid fake filler content

### Definition of Done
- Diary handles early usage cleanly
- Empty states feel intentional, not broken

### Risks to watch
- Do not insert fake memories
- Keep tone aligned with product

---

# Batch F — Animation Upgrade Track

## Batch Goal

Prepare and begin migration toward **Rive-based animation** without blocking MVP execution.

## Batch Success Criteria

- Animation architecture is Rive-ready
- State/emotion/reaction inputs are clearly defined
- Temporary fake animator can be swapped later
- First Rive integration path is planned and optionally prototyped

---

## Task F1
**Task ID:** P11-F1  
**Title:** Define animation input grammar for future Rive state machine

### Goal
Create the stable animation contract that Rive will consume later.

### Scope
- Define animation-facing enums/bands/inputs such as:
  - emotion
  - reaction type
  - greeting type
  - energy band
  - hunger band
  - social band
  - activity result
- Keep inputs minimal and reusable

### Definition of Done
- Animation input grammar is explicit and stable
- Business logic can map to it cleanly

### Risks to watch
- Do not pass raw 0–100 values everywhere by default
- Prefer clear bands where useful

---

## Task F2
**Task ID:** P11-F2  
**Title:** Map PetState, derived conditions, and trait-weighted behaviors into animation inputs

### Goal
Connect the product brain to animation in a clean, swappable way.

### Scope
- Build mapper from current pet decision/state to animator inputs
- Keep mappers testable and centralized
- Preserve separation from concrete animation runtime

### Definition of Done
- Behavior layer can drive animation abstraction consistently
- Mapping rules are understandable and inspectable

### Risks to watch
- Do not spread mapping logic across UI composables
- Keep trait influence bounded before animation

---

## Task F3
**Task ID:** P11-F3  
**Title:** Upgrade FakePetAnimator reactions to better simulate final motion semantics

### Goal
Make the temporary animation layer good enough to support meaningful product testing before Rive assets arrive.

### Scope
- Improve fake idle
- Improve greeting reaction
- Improve tap/activity reaction visibility
- Keep implementation lightweight and replaceable

### Definition of Done
- Product testing benefits from clearer animation semantics
- Fake animator remains swappable

### Risks to watch
- Do not turn fake animator into a long-term engine
- Avoid overengineering temporary visuals

---

## Task F4
**Task ID:** P11-F4  
**Title:** Add Rive integration seam and runtime adapter placeholder

### Goal
Prepare code structure for later asset drop-in.

### Scope
- Add `RivePetAnimator` stub or adapter boundary
- Wire dependency injection / factory path if needed
- Keep runtime path optional until assets exist

### Definition of Done
- Project has a clear seam for future Rive implementation
- Existing fake animator path still works

### Risks to watch
- Do not break current runtime while preparing Rive seam
- Do not fake completed Rive behavior

---

## Task F5
**Task ID:** P11-F5  
**Title:** Create initial Rive adoption specification inside the code/docs boundary

### Goal
Ensure future animation work is implementation-ready, not vague.

### Scope
- Document expected state machine inputs
- Document required triggers
- Document core motion set:
  - idle
  - blink
  - greeting
  - tap
  - long press
  - feed
  - play
  - rest
  - sound reaction
- Keep aligned with actual app behavior model

### Definition of Done
- Rive work can start later without rethinking the whole behavior contract
- Spec aligns with current code abstractions

### Risks to watch
- Do not invent animations unsupported by behavior model
- Keep spec practical for first asset pass

---

# Recommended Execution Order Across Batches

1. **Batch A — Pet State Foundation**
2. **Batch B — Home UX**
3. **Batch C — Core Gameplay**
4. **Batch D — Personality Visibility**
5. **Batch E — Memory UX**
6. **Batch F — Animation Upgrade Track**

---

# Priority Labels

## P0 — Must complete for internal MVP
- A1–A8
- B1–B7
- C1–C9

## P1 — Strongly recommended for credible MVP
- D1–D6
- E1–E6

## P2 — Start during or after MVP stabilization
- F1–F5

---

# Phase 1.1 Final Definition of Done

Phase 1.1 is complete when:

- Opening the app shows a pet that reacts immediately
- The pet has a real saved state that changes over time
- The Home screen is user-facing, not debug-facing
- Users can tap, long press, feed, play, and rest the pet
- Those actions visibly react, change state, and emit events
- Personality begins to affect reaction style
- Diary entries are readable and feel like memories
- Animation architecture is ready for Rive without requiring a future rewrite

---

# Notes For Codex Conversion

This file is a **batch/task backlog**, not a direct one-shot coding prompt.

Best workflow:
1. Pick one batch
2. Split each task into a Codex-ready prompt
3. Execute task-by-task
4. Review output and remaining risks after each task
5. Carry relevant remaining risks into the next task prompt without changing the task itself
