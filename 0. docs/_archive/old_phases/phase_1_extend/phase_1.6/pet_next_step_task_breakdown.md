# AI Pet — Next Step Task Breakdown (Production-Ready)

Version: v4  
Purpose: Detailed implementation backlog for the next-step pet system.  
Audience: Codex / Claude / senior engineers / product execution  
Execution rule: **one task at a time**, build-safe, verifiable, no placeholder production logic.

---

# 1. Global rules for every task

Every task must:

- keep the app build green
- preserve offline-first behavior
- preserve event-driven architecture
- avoid unrelated refactor drift
- avoid fake production logic
- expose visible or inspectable verification
- update only the minimum required surface area

Required output after each task:

- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks

Default build command:

- `./gradlew assembleDebug`

If touched logic has stable tests:

- `./gradlew test`

---

# 2. Recommended execution philosophy

Do not try to “finish the whole emotional system” in one pass.

Build in this order:

1. state foundation
2. greeting/return loop
3. care actions
4. neglect and repair
5. behavior scoring
6. personality influence
7. invitations and session variation
8. progression and rare affection
9. tuning and debug hardening

This order matters.
If behavior output is built before state and repair logic exist, the pet will feel fake.

---

# 3. Batch map

- **NX1** — Internal state foundation
- **NX2** — Return-after-absence and greeting loop
- **NX3** — Care actions and immediate reaction effects
- **NX4** — Neglect, overstimulation, and recovery
- **NX5** — Behavior scoring and explanation surfaces
- **NX6** — Personality influence and trait evolution hooks
- **NX7** — Invitation system and session variation
- **NX8** — Progression, rare affection, and emotional payoffs
- **NX9** — Debug hardening, balancing tools, and analytics-ready events

---

# Batch NX1 — Internal State Foundation

## NX1-01 Define PetState v2 domain model

Goal:
Create the upgraded pet state domain model with needs, mood, and relationship layers separated clearly.

Scope:
- Domain models only.
- No UI binding yet.

Must include at minimum:
- energy
- hunger
- sleepiness
- socialNeed
- comfort
- stimulation
- moodValence
- moodArousal
- bondScore
- trustScore
- attachmentScore
- neglectStreak
- careStreak
- lastUpdatedAt
- lastOpenAt
- lastMeaningfulInteractionAt

Must also include:
- safe clamping helpers
- immutable update ergonomics suitable for production use

Definition of Done:
- model compiles
- values are range-safe
- model is usable from brain, memory, and app layers

Verification:
- reference the model in app/brain code and run a build

## NX1-02 Add Room entity and DAO for PetState v2

Goal:
Persist the upgraded pet state as the source of truth.

Scope:
- Add/upgrade Room entity and DAO only.
- No greeting logic yet.

Must include:
- getCurrentState
- upsertState
- clear/reset only if safe and already architecturally acceptable

Definition of Done:
- state persists without crash
- migration path is handled safely
- app startup remains stable

Verification:
- initialize DB, write state, read state, restart app, re-read state

## NX1-03 Implement PetStateRepository with default initializer

Goal:
Ensure a valid pet state always exists, including on fresh install.

Scope:
- repository initialization only
- no behavioral side effects yet

Definition of Done:
- fresh install creates one valid default state
- there is no null-state path in production flow
- repository API is usable by later screens and startup flows

Verification:
- clear app data and launch app
- inspect debug/log output or DB query path

## NX1-04 Implement elapsed-time decay engine

Goal:
Make the pet change meaningfully while the user is away.

Scope:
- deterministic time-based updates only
- no greeting or visible reaction yet

Must affect at minimum:
- energy
- hunger
- sleepiness
- socialNeed
- stimulation
- comfort (more slowly)

Rules:
- values must remain in valid range
- elapsed time must come from lastUpdatedAt
- very long gaps must not produce nonsense values

Definition of Done:
- multi-hour gaps change state measurably
- output remains stable and bounded

Verification:
- simulate time gap with controllable time source or manual debug injection

## NX1-05 Add PetCondition resolver

Goal:
Resolve meaningful high-level conditions from raw state.

Must include at least:
- HUNGRY
- SLEEPY
- BORED
- NEEDY
- CALM
- PLAYFUL
- DISTANT
- OVERSTIMULATED

Scope:
- pure rule resolution only
- no UI output beyond debug later

Definition of Done:
- given sample states, conditions resolve predictably
- rules are explainable and debuggable

Verification:
- run sample states through resolver and inspect outputs

## NX1-06 Add PetState v2 debug panel

Goal:
Expose live state visibly in the app.

Must show:
- all needs
- mood values
- bond/trust/attachment
- neglect/care streaks
- derived conditions
- lastUpdatedAt

Definition of Done:
- debug UI shows real persisted state
- state updates are visible after decay or actions

Verification:
- launch app and confirm values reflect persisted state changes

---

# Batch NX2 — Return-After-Absence and Greeting Loop

## NX2-01 Implement absence classifier

Goal:
Classify the user’s return context from elapsed time and recent relationship state.

Suggested buckets:
- SHORT_RETURN
- MEDIUM_RETURN
- LONG_RETURN
- NEGLECT_RETURN

Inputs should consider:
- elapsed time
- neglectStreak
- recent care rhythm if available

Definition of Done:
- classification is deterministic and testable
- long absence does not always equal neglect automatically

Verification:
- feed multiple return scenarios and inspect bucket outputs

## NX2-02 Define PetGreetingContext model

Goal:
Create an explicit domain model for app-open reunion logic.

Must include:
- absence bucket
- relevant PetState snapshot
- derived conditions
- relationship summary
- optional future personality summary hook

Definition of Done:
- greeting pipeline has a clean typed input model
- no ad-hoc string-based context plumbing is required

Verification:
- use the model from resolver code and run a build

## NX2-03 Implement greeting emotion resolver

Goal:
Resolve visible greeting emotion from return context.

Potential outputs:
- HAPPY
- CURIOUS
- SLEEPY
- NEEDY
- RELIEVED
- HESITANT
- DISTANT

Definition of Done:
- greeting emotion changes meaningfully across scenarios
- resolution is explainable, not random-only

Verification:
- run scenario table and inspect selected emotions

## NX2-04 Implement greeting style resolver

Goal:
Resolve how the pet greets, not just what emotion label it has.

Suggested styles:
- WARM
- GENTLE
- SLEEPY
- PLAYFUL
- NEEDY
- HESITANT
- DISTANT
- RELIEVED

Inputs:
- absence bucket
- current state
- trustScore
- bondScore
- attachmentScore
- neglectStreak
- careStreak

Definition of Done:
- style varies meaningfully
- long absence with strong bond can differ from long absence with weak trust

Verification:
- scenario test matrix produces believable different styles

## NX2-05 Apply state decay and greeting on app open

Goal:
Make app open feel like a reunion instead of a static launch.

Scope:
- run decay refresh
- build greeting context
- resolve emotion/style
- surface a visible greeting reaction

Definition of Done:
- app open triggers a fast, visible greeting
- state refresh happens before greeting resolution
- flow is stable across cold start and warm start

Verification:
- open app after different time gaps and confirm greeting differs

## NX2-06 Emit app-open lifecycle events

Goal:
Make the reunion loop observable.

Must emit at minimum:
- PET_STATE_DECAY_APPLIED
- PET_GREETED

Payload should include:
- absence bucket
- selected greeting style
- selected emotion
- top-level reason summary where appropriate

Definition of Done:
- debug/event viewer shows reunion lifecycle events

Verification:
- open app and inspect event logs

## NX2-07 Add greeting explanation debug panel

Goal:
Show why the greeting was chosen.

Must show:
- absence bucket
- state snapshot summary
- selected style
- selected emotion
- top scoring reasons / suppression notes

Definition of Done:
- developer can explain greeting decisions without reading code

Verification:
- trigger different openings and compare explanation panel

---

# Batch NX3 — Care Actions and Immediate Emotional Feedback

## NX3-01 Define InteractionAction model

Goal:
Normalize user care actions into a reusable domain model.

Must include:
- PET_TAP
- LONG_PRESS
- PLAY_ACTION
- FEED_ACTION
- SOOTHE_ACTION
- SESSION_LINGER
- OPTIONAL_IGNORE_SIGNAL if architecture supports it cleanly

Definition of Done:
- model is shared cleanly across app, brain, and memory

Verification:
- reference the action model from UI and reaction logic

## NX3-02 Implement petting/tap effect pipeline

Goal:
Turn tapping into a real emotional and stateful interaction.

Must update:
- socialNeed or social relief
- comfort
- moodValence
- bondScore (small)
- possible overstimulation if bursty

Must produce:
- visible reaction
- persisted state change
- emitted event

Definition of Done:
- tap causes real state deltas and visible output

Verification:
- tap pet several times and inspect debug state/event changes

## NX3-03 Implement long-press / hold effect pipeline

Goal:
Use hold as a closeness / calming action.

Rules:
- should help comfort/trust when pet is receptive
- should not always be positive when pet is overstimulated or sleepy

Definition of Done:
- long press creates distinct effects from tap
- recovery-friendly use case exists

Verification:
- test long press under calm vs overstimulated conditions

## NX3-04 Implement play action effect pipeline

Goal:
Make play a meaningful state-changing action.

Must affect:
- stimulation up
- mood up
- socialNeed relief
- energy down somewhat
- bond slightly up

Definition of Done:
- play visibly changes internal state and output mood

Verification:
- trigger play and inspect state deltas

## NX3-05 Implement feed action effect pipeline

Goal:
Make feeding reduce hunger and improve care perception.

Must affect:
- hunger down
- comfort up
- trust slightly up when hunger was relevant
- mood improvement if hunger pressure was meaningful

Definition of Done:
- feed is not cosmetic
- hunger visibly decreases in debug panel

Verification:
- raise hunger, feed, inspect state and output

## NX3-06 Implement soothe action effect pipeline

Goal:
Create a direct emotional repair action.

Must affect:
- comfort up
- arousal down
- trust up
- recovery-friendly emotion transitions

Definition of Done:
- soothe is especially helpful after neglect, tension, or overstimulation

Verification:
- create tense or distant state, use soothe, inspect changes

## NX3-07 Emit care action events with deltas

Goal:
Make all care actions observable and tunable.

Must emit:
- action type
- pre/post state summary or delta payload
- selected reaction outcome
- whether diminishing returns applied

Definition of Done:
- event logs make care tuning practical

Verification:
- trigger each action once and inspect emitted event payloads

---

# Batch NX4 — Neglect, Overstimulation, and Recovery

## NX4-01 Implement neglect tracker

Goal:
Track repeated absence / weak care patterns that cool the relationship.

Inputs may include:
- repeated long absence
- repeated open-and-leave behavior if safely observable
- insufficient meaningful care over time

Definition of Done:
- neglectStreak changes in a bounded, explainable way
- not every long gap counts as neglect automatically

Verification:
- simulate multiple return patterns and inspect streak behavior

## NX4-02 Implement overstimulation tracker

Goal:
Detect when repeated rapid interactions should stop being purely positive.

Inputs:
- repeated taps/holds/actions in burst windows
- current stimulation level
- patience trait if available later

Definition of Done:
- burst repetition produces diminishing returns and mild cost

Verification:
- spam tap and confirm positive returns flatten or reverse slightly

## NX4-03 Add diminishing returns logic for burst interactions

Goal:
Prevent the optimal play pattern from becoming spam.

Must affect at least:
- tap
- long press
- optionally play if triggered repeatedly too fast

Definition of Done:
- repeated fast interactions stop generating full rewards
- output remains readable and not punitive

Verification:
- compare first few taps vs later taps in same burst

## NX4-04 Implement trust cooling from neglect and overstimulation

Goal:
Make trust more fragile than bond.

Rules:
- neglect can lower trust gradually
- spammy rough interaction can lower comfort and trust slightly
- bond should remain more resilient

Definition of Done:
- trust and bond can diverge in believable ways

Verification:
- simulate neglect and spam scenarios, inspect scores

## NX4-05 Implement repair effect resolver

Goal:
Make recovery behavior intentional and tunable.

Must consider:
- soothe
- feed when hungry
- gentle petting with good timing
- calm session linger

Definition of Done:
- repair actions accelerate trust/comfort recovery under the right conditions

Verification:
- compare same action in healthy vs strained state

## NX4-06 Add recovery reaction outputs

Goal:
Visually show the user that repair is working.

Suggested reaction family:
- RELIEF_AFTER_SOOTHE
- SOFTENING_AFTER_DISTANCE
- CONTENT_SETTLE

Definition of Done:
- repair is emotionally visible, not just numeric

Verification:
- move pet into hesitant/distant state, repair it, observe output

## NX4-07 Emit neglect and recovery events

Goal:
Make relationship cooling and repair observable.

Must emit:
- PET_NEGLECT_IMPACT_APPLIED
- PET_RECOVERY_APPLIED
- optional OVERSTIMULATION_APPLIED if event model allows cleanly

Definition of Done:
- logs reflect when strain and repair happen

Verification:
- simulate neglect and repair, inspect event viewer/debug logs

---

# Batch NX5 — Behavior Scoring and Explanation Surfaces

## NX5-01 Define behavior candidate model

Goal:
Represent behavior families as scored candidates instead of hard-coded one-off branches.

Suggested candidates:
- greet warm
- greet sleepy
- greet distant
- seek comfort
- invite play
- ask for food
- settle calmly
- withdraw slightly
- relief after soothe

Definition of Done:
- typed candidate model exists and compiles

Verification:
- reference candidate model from resolver code

## NX5-02 Implement lightweight behavior scorer

Goal:
Score candidate reactions using state, relationship, and context.

Inputs should include:
- needs
- mood
- relationship
- absence context
- current conditions
- suppression penalties

Definition of Done:
- scorer selects a winner deterministically from weighted inputs
- behavior choice is no longer brittle if-else only

Verification:
- run scorer against scenario table and inspect ranked outputs

## NX5-03 Add suppression and cooldown policy

Goal:
Prevent repetitive or inappropriate output.

Must suppress based on:
- recent same reaction
- ignored invitation history
- low energy/high sleepiness
- overstimulation
- recent rare moment cooldown

Definition of Done:
- scorer can explain why an obvious reaction lost due to policy constraints

Verification:
- run repeated same scenario and inspect changed winner / penalties

## NX5-04 Bind scorer output to avatar/audio-facing reaction mapping

Goal:
Use scored reaction outcomes to drive visible pet output.

Definition of Done:
- visible reaction comes from scored behavior family, not ad-hoc direct UI branching

Verification:
- inspect scenario changes and confirm different candidates map to different visible behavior

## NX5-05 Add behavior scoring explanation debug surface

Goal:
Make behavior resolution inspectable.

Must show:
- ranked candidates
- top positive contributors
- suppression penalties
- winning candidate

Definition of Done:
- dev can explain “why this happened” without tracing code manually

Verification:
- run several scenarios and inspect ranked explanation panel

## NX5-06 Emit behavior selection events

Goal:
Persist scored choice outcomes for balancing.

Must emit:
- selected candidate
- top competing candidates
- selected emotion/style where relevant
- suppression reason summary

Definition of Done:
- event logs capture enough information for tuning sessions later

Verification:
- trigger behavior decisions and inspect event payloads

---

# Batch NX6 — Personality Influence and Trait Evolution Hooks

## NX6-01 Define PersonalityProfile v2

Goal:
Expand personality into a scoring-usable trait profile.

Must include:
- curiosity
- sociability
- playfulness
- patience
- attachment
- energyProfile

Definition of Done:
- trait model exists and is loadable by behavior logic

Verification:
- reference the trait model from debug/scoring code and build

## NX6-02 Persist PersonalityProfile v2

Goal:
Store traits as durable pet identity, separate from transient current state.

Definition of Done:
- traits survive restart
- clean repository path exists

Verification:
- modify and reload traits through persistence path

## NX6-03 Apply personality biases to behavior scoring

Goal:
Make different pets behave differently under the same state.

Examples:
- sociability boosts warm greeting and check-in tendency
- playfulness boosts play invitation and excited reactions
- patience reduces overstimulation penalties
- attachment increases reunion warmth and social seeking

Definition of Done:
- changing traits changes scored outcomes in believable ways

Verification:
- run same scenario under different trait sets and compare outputs

## NX6-04 Add trait-aware absence and repair modifiers

Goal:
Make personality influence reunion and repair behavior.

Definition of Done:
- attachment and patience meaningfully affect reunion and recovery

Verification:
- compare same neglect scenario across different trait profiles

## NX6-05 Add slow trait evolution hooks

Goal:
Create the infrastructure for long-term personality drift without fully tuning it yet.

Inputs may include:
- play frequency
- soothe frequency
- neglect rhythm
- rough/spam interaction rhythm

Definition of Done:
- traits can be updated through bounded small deltas
- no single event causes large swings

Verification:
- simulate repeated patterns and inspect small trait drift

## NX6-06 Add personality debug panel

Goal:
Make traits visible and understandable in the app.

Must show:
- current trait values
- recent trait changes if available
- which behaviors they currently bias most strongly

Definition of Done:
- developer can inspect how traits affect behavior

Verification:
- change trait values and compare debug panel + scored outputs

---

# Batch NX7 — Invitation System and Session Variation

## NX7-01 Define invitation context and types

Goal:
Create a domain model for subtle pet-initiated bids for attention.

Suggested invitation types:
- SOFT_CHECKIN
- PLAY_INVITE
- NEEDY_LOOK
- CURIOUS_GLANCE

Definition of Done:
- invitation types are typed and usable from behavior logic

Verification:
- reference types from scorer/UI code and build

## NX7-02 Implement invitation eligibility resolver

Goal:
Decide when the pet is allowed to invite interaction.

Must consider:
- stimulation
- socialNeed
- energy/sleepiness
- attachment
- recent invitation history
- ignored invitation count

Definition of Done:
- invitation is possible but not frequent or spammy

Verification:
- simulate idle sessions with different states and inspect eligibility

## NX7-03 Implement invitation suppression logic

Goal:
Prevent repeated bids for attention from becoming annoying.

Must suppress on:
- recent ignored invitation
- too many invitations this session
- low energy / sleepy state
- distant or distrustful state

Definition of Done:
- invitation system backs off when the user does not engage

Verification:
- repeatedly ignore invitations and inspect suppression behavior

## NX7-04 Add invitation outputs to Home session flow

Goal:
Let the pet sometimes bid for interaction during a session.

Definition of Done:
- invitation appears subtly and visibly under correct conditions
- invitation does not hijack the app constantly

Verification:
- keep app open in eligible state and observe invitation behavior

## NX7-05 Track invitation acceptance vs ignore outcomes

Goal:
Use invitation outcomes to shape later session behavior.

Definition of Done:
- accepted invitations improve warmth/engagement
- ignored invitations contribute to suppression and minor relational context

Verification:
- compare accepted vs ignored invitation flows in debug state

## NX7-06 Add session variation resolver

Goal:
Create lightweight variation based on current session context rather than randomness alone.

Definition of Done:
- same pet under same broad state can still feel slightly different between sessions for explainable reasons

Verification:
- run similar sessions with different prior contexts and inspect output differences

---

# Batch NX8 — Progression, Rare Affection, and Emotional Payoff

## NX8-01 Define relationship stage model

Goal:
Represent emotional progression explicitly.

Suggested stages:
- STRANGER
- FAMILIAR
- ATTACHED
- BONDED

Definition of Done:
- stage model compiles and is usable from state and behavior logic

Verification:
- reference stage model from scorer/debug code and build

## NX8-02 Implement relationship stage resolver

Goal:
Resolve current stage from bond/trust/attachment and behavior history.

Definition of Done:
- stage changes only when conditions truly justify it
- stage is stable, not flickery

Verification:
- feed sample score combinations and inspect stage outputs

## NX8-03 Apply stage effects to greeting and invitation warmth

Goal:
Make progression felt through behavior, not just a label.

Definition of Done:
- higher stages subtly expand warmth ceiling, softness, or invitation style

Verification:
- compare same scenario across different stages

## NX8-04 Define rare-affection eligibility rules

Goal:
Allow occasional special emotional moments without feeling gamified.

Eligibility should consider:
- bond/trust quality
- healthy care rhythm
- cooldown since last rare moment
- current mood/context suitability

Definition of Done:
- rare moments are possible but meaningfully constrained

Verification:
- scenario simulation shows rarity and appropriateness

## NX8-05 Implement rare-affection reaction outputs

Goal:
Surface occasional earned emotional payoffs.

Examples:
- extra-soft reunion
- warm settle-after-soothe
- unusually affectionate play-ready moment

Definition of Done:
- rare output is visible and distinguishable from normal output
- happens infrequently

Verification:
- trigger eligible scenarios and inspect cooldown behavior

## NX8-06 Emit progression and rare-affection events

Goal:
Make stage changes and rare moments observable.

Must emit:
- PET_RELATIONSHIP_STAGE_CHANGED
- PET_RARE_AFFECTION_TRIGGERED

Definition of Done:
- event logs show emotional milestones clearly

Verification:
- simulate stage progression and rare moment trigger paths

---

# Batch NX9 — Debug Hardening, Balancing Tools, and Analytics-Ready Events

## NX9-01 Create unified emotional systems debug screen

Goal:
Consolidate the key next-step debug panels into one usable developer surface.

Must include at minimum:
- current needs
- mood and relationship
- trait values
- active conditions
- recent care/neglect impact
- latest greeting details
- latest scored behavior result

Definition of Done:
- one screen gives a coherent picture of the pet’s inner life

Verification:
- perform several interactions and inspect the unified debug screen

## NX9-02 Add state delta history panel

Goal:
Show recent state changes in a compact timeline.

Definition of Done:
- developers can inspect what changed after each action or decay cycle

Verification:
- trigger actions and confirm delta entries appear

## NX9-03 Add scenario injection tools for balancing

Goal:
Speed up tuning by allowing safe local simulation of common conditions.

Suggested scenario presets:
- tired pet
- hungry pet
- neglected pet
- bonded pet
- overstimulated pet

Definition of Done:
- developers can simulate states without corrupting architecture
- feature is clearly debug-only

Verification:
- apply preset and inspect resulting UI/debug outputs

## NX9-04 Standardize event payloads for emotional systems

Goal:
Ensure all next-step systems emit payloads consistent enough for later analytics or replay.

Definition of Done:
- emotional/reunion/care/repair/progression events follow a predictable structure

Verification:
- inspect sample event JSONs and confirm field consistency

## NX9-05 Add lightweight balancing notes / constants organization

Goal:
Separate core emotional constants and thresholds into a maintainable configuration structure.

Must include organization for:
- decay rates
- neglect thresholds
- invitation cooldowns
- overstimulation limits
- rare-affection cooldowns
- stage thresholds

Definition of Done:
- tuning values are centralized enough to iterate safely
- code does not hide magic numbers everywhere

Verification:
- inspect code paths and confirm thresholds come from organized constants/configs

## NX9-06 Run differentiation validation pass

Goal:
Confirm the new system actually creates meaningful variation.

Must validate at minimum:
- different absence scenarios create different greetings
- different trait profiles create different outputs under same state
- neglect and repair are both visible and recoverable
- spam interactions do not remain optimal

Definition of Done:
- validation findings are documented in task output
- remaining balance risks are explicitly listed

Verification:
- run scenario matrix and include results in agent report

---

# 4. Recommended first task to execute

The correct first task is:

**NX1-01 Define PetState v2 domain model**

Reason:
Everything else depends on it.
Without a correct state model, the team will fake greeting logic and relationship behavior prematurely.

---

# 5. Definition of done for the whole next-step backlog

This backlog is successful only when all of the following are true:

- app open feels like a reunion
- user care actions have real persistent effects
- neglect changes emotional warmth without turning punitive
- recovery is emotionally visible and satisfying
- two pets with different traits do not behave the same
- invitation behavior is subtle and not spammy
- special affectionate moments feel earned, not random loot
- developers can explain major behavior choices from debug surfaces

If these conditions are not met, the implementation may be large but the product result is still not good enough.
