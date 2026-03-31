# AI Pet — Next Step Product Design (Optimized, Production-Ready)

Version: v4  
Status: Next-step product and systems design  
Owner perspective: Product Owner + Senior Game Designer + System Design  
Purpose: Define the **real next production step** for the pet so the app evolves from “interactive demo” into a **living digital creature** with continuity, attachment, and replayable daily value.

---

# 1. Executive summary

The previous “next step” direction was correct, but too shallow.
It described themes such as personality, bonding, and retention, but it did **not** define the system deeply enough to guide implementation, balancing, tuning, and AI-agent execution safely.

The real next step is **not** to add more AI features.
The real next step is:

> turn the pet into a stateful creature with a believable inner life, emotional continuity, and return-driven interaction loops.

This stage should produce a pet that:

- feels different when the user comes back after time away
- reacts with emotional logic instead of random animation
- becomes warmer, clingier, calmer, or more distant based on how it is treated
- creates tiny emotional rewards without spam or fake game mechanics
- becomes easier to tune because behavior reasons are visible in debug surfaces

This document defines the next-step system in a way that is:

- compatible with the current offline-first Android architecture
- compatible with event-driven persistence and Room
- safe for phased implementation
- specific enough to hand to Codex/Claude task by task

---

# 2. What this phase is trying to achieve

## 2.1 Product objective

The next version of the pet must answer one question convincingly:

> “Why would someone open this app again tomorrow?”

The answer should not be:

- because there is a daily reward
- because the app nags the user
- because it behaves like a chatbot

The answer should be:

- because the pet feels alive
- because the pet reacts differently depending on what happened before
- because the user wants to see how the pet is doing
- because the relationship feels slightly deeper over time

## 2.2 Experience target

When the user opens the app, the first 5–15 seconds should feel like a tiny reunion.

That reunion should communicate:

- where the pet is emotionally right now
- whether it missed the user or not
- whether it feels playful, tired, hungry, comforted, needy, or distant
- whether the relationship is warm, fragile, recovering, or strong

This is the emotional heartbeat of the product.
If this does not work, the rest of the system will feel like decoration.

## 2.3 What success looks like

This phase is successful when the user begins to think in sentences like:

- “It looks sleepy today.”
- “It missed me.”
- “It got upset when I kept poking it.”
- “It seems more attached lately.”
- “It acts a bit differently than before.”

That is the product bar.
Not “the avatar animates nicely.”
Not “the AI can say more things.”

---

# 3. Strategic design stance

## 3.1 Core philosophy

The pet should behave like a **creature**, not a feature menu.

That means the design must prioritize:

1. **continuity over novelty**
2. **state-driven behavior over random output**
3. **emotional logic over content volume**
4. **subtle attachment over explicit gamification**
5. **clear internal rules over black-box magic**

## 3.2 What this phase is not

This phase is **not** about:

- full conversation
- cloud-first intelligence
- productivity features
- mini-game expansion
- adding many buttons for the user to press
- fake “engagement” tricks like streak popups and forced rewards

## 3.3 Design danger to avoid

The biggest failure mode is making the pet feel like one of these:

- an animation toy
- a notification trap
- a shallow tamagotchi clone
- a chatbot with a face
- a random reaction machine

The pet must instead feel like:

- a small creature with a body clock
- a small creature with moods
- a small creature with memory traces
- a small creature whose trust and warmth can shift

---

# 4. Why the previous next-step design was not enough

The earlier files had four structural problems.

## 4.1 They described outcomes, not systems

Statements like “make the pet emotionally sticky” and “add return after absence logic” are directionally right, but too broad.
An AI coding agent cannot safely implement them without making assumptions.

## 4.2 They merged too many concepts together

The old design blurred:

- needs
- mood
- visible emotion
- bond
- attachment
- personality
- progression

If those are not separated, behavior becomes impossible to tune.

## 4.3 They did not define enough anti-spam logic

Affection without limits becomes clingy spam.
Invitation without suppression becomes annoying repetition.
Return logic without cooldown becomes fake drama.

## 4.4 They did not define the runtime loop tightly enough

A believable pet requires a stable loop:

`time passes -> state drifts -> app opens -> context is classified -> greeting is resolved -> user acts -> pet reacts -> state changes -> memory recorded -> behavior baseline updates`

Without this loop, “personality” stays cosmetic.

---

# 5. The core experience loop to build now

The next-step loop should be:

```text
User is away
   ↓
Time decay and absence state accumulate
   ↓
User opens app
   ↓
Pet refreshes internal state from elapsed time
   ↓
Pet classifies return context
   ↓
Pet expresses a greeting based on state + relationship + personality
   ↓
User interacts (tap / soothe / play / feed / linger / ignore)
   ↓
Pet reacts visually and emotionally
   ↓
Internal state, bond, trust, and mood update
   ↓
Memory/event log records the moment
   ↓
Pet baseline for the rest of the session changes
   ↓
User leaves
   ↓
Future reunion is now different
```

This loop is the minimal engine of attachment.

---

# 6. System pillars

The next-step pet should be built from seven interconnected pillars.

## 6.1 Internal life

The pet needs private, persistent internal state.
Not just emotion labels.

## 6.2 Emotional readability

The user must be able to roughly understand what the pet feels.
Not through text explanation, but through consistent reactions.

## 6.3 Consequence

The user’s actions must matter.
Not massively, but measurably.

## 6.4 Relationship drift

The pet should not reset emotionally every session.

## 6.5 Daily variation

Pet today should feel a little different than pet yesterday.

## 6.6 Invitation without spam

The pet should sometimes seek interaction, but with suppression rules.

## 6.7 Debuggability

Developers must be able to inspect:

- why a greeting happened
- why a reaction was chosen
- why the pet feels distant / playful / sleepy
- how bond and trust changed

Without this, balancing will stall.

---

# 7. Layered creature model

A believable digital creature needs multiple time scales.
The model should have these layers.

## 7.1 Layer A — Needs (slow pressures)

These are the core internal pressures.
They should change gradually over time and through actions.

Recommended fields:

- `energy` (0–100)
- `hunger` (0–100)
- `sleepiness` (0–100)
- `socialNeed` (0–100)
- `comfort` (0–100)
- `stimulation` (0–100)

### Meaning of each field

**energy**  
How able the pet is to react actively.
Low energy reduces playfulness and animation intensity.

**hunger**  
How strongly the pet needs feeding/care signals.
High hunger increases needy or food-seeking expression.

**sleepiness**  
How strongly the pet wants rest.
High sleepiness produces slow greetings and drowsy reactions.

**socialNeed**  
How much the pet craves affectionate interaction.
Low social fulfillment creates loneliness, clinginess, or apathy depending on personality.

**comfort**  
How emotionally safe / settled the pet feels.
Too much rough or spammy interaction reduces comfort.
Comfort supports calm trust-based behavior.

**stimulation**  
How engaged or bored the pet feels.
Very low stimulation increases boredom/invitation behaviors.
Very high stimulation may create over-excitement or irritability.

### Design rule

Do not make all six change at the same speed.
That creates mush.
Suggested relative speeds:

- sleepiness: medium-fast
- hunger: medium
- energy: medium
- socialNeed: medium
- stimulation: fast-medium
- comfort: slower, more resistant

## 7.2 Layer B — Mood (slow emotional climate)

Mood is not what happened in the last second.
Mood is the emotional weather left behind by recent history.

Recommended representation:

- `moodValence` (-100 to +100)
- `moodArousal` (0–100)
- optional derived label:
  - calm
  - playful
  - sleepy
  - needy
  - distant
  - tense
  - content

### Mood rules

- mood should move slowly
- repeated care should gently improve valence
- repeated neglect should lower valence
- spammy interruptions should raise arousal and may lower comfort
- mood should decay toward center over long periods, but not too aggressively

## 7.3 Layer C — Visible emotion (short-lived output)

This is the user-facing expression in the moment.

Examples:

- happy
- curious
- sleepy
- startled
- shy
- hungry
- excited
- sad
- relieved
- distant

### Emotion rules

- lasts seconds, not hours
- explains the current visible reaction
- may temporarily override baseline mood for display
- must be attributable to a trigger plus current state

## 7.4 Layer D — Relationship

Relationship should be more than one number.

Recommended fields:

- `bondScore`
- `trustScore`
- `attachmentScore`
- `neglectStreak`
- `careStreak`
- `lastMeaningfulInteractionAt`
- `lastComfortAt`

### Difference between the fields

**bondScore**  
Long-term emotional closeness.
Harder to build, slower to lose.

**trustScore**  
How safe the pet feels with the user right now.
More fragile than bond.
Affected by roughness, spam, inconsistency.

**attachmentScore**  
How strongly the pet seeks reunion, closeness, and check-ins.
This drives clinginess or affectionate return behavior.

**neglectStreak**  
A contextual memory of repeated long absences without repair.
Must not erase bond immediately, but should cool warmth.

**careStreak**  
A lightweight reflection of recent healthy interaction rhythm.
Used to support recovery and warm greetings.

## 7.5 Layer E — Personality

Personality should be long-term and low-volatility.
It should modify scoring, not just titles.

Recommended core traits:

- `curiosity`
- `sociability`
- `playfulness`
- `patience`
- `attachment`
- `energyProfile`
- optional later:
  - `sensitivity`
  - `boldness`

### Personality rules

- do not let one event swing a trait
- trait change should accumulate across days
- traits should influence *how* state is expressed
- same state + different traits = different behavior

Example:

Two pets are both bored and mildly attached.

- high-playfulness pet invites play quickly
- low-playfulness, high-attachment pet gives a quiet look and lingers

## 7.6 Layer F — Progression / relationship stage

This is not RPG leveling.
It is emotional stage progression.

Suggested stages:

- `STRANGER`
- `FAMILIAR`
- `ATTACHED`
- `BONDED`

### Stage purpose

- unlock warmth, not power
- adjust expression richness and invitation style
- support emotional continuity

## 7.7 Layer G — Session memory

Even before advanced memory systems, the pet needs current-session context.

Suggested transient fields:

- `sessionStartAt`
- `sessionInteractionCount`
- `lastActionType`
- `lastReactionType`
- `interactionBurstCount`
- `wasComfortedThisSession`
- `wasIgnoredThisSession`

This helps the pet avoid repetitive output and creates in-session continuity.

---

# 8. Emotional design map

The pet must be legible without words.
That means each major internal state combination should map to a readable emotional surface.

## 8.1 Core readable surfaces

| Internal pattern | Outward feel |
|---|---|
| low energy + high sleepiness | drowsy / slow |
| high hunger + moderate bond | needy / asking |
| low stimulation + high attachment | soft invitation |
| low comfort + high interruption | slightly withdrawn |
| high bond + medium absence | warm reunion |
| high neglect streak + low trust | hesitant / guarded |
| high stimulation + high playfulness | excited / playful |
| strong recovery after care | relieved / affectionate |

## 8.2 Reaction vocabulary

The pet should use a small but strong vocabulary of reactions rather than too many weak ones.

Suggested core reaction types:

- `WARM_GREET`
- `SLEEPY_GREET`
- `HESITANT_GREET`
- `DISTANT_GREET`
- `SOFT_AFFECTION`
- `PLAY_INVITE`
- `NEEDY_CHECKIN`
- `STARTLED_LOOK`
- `CONTENT_SETTLE`
- `OVERSTIMULATED_PULLBACK`
- `RELIEF_AFTER_SOOTHE`

This should later map into avatar, audio, and behavior scoring.

---

# 9. Return-after-absence design

This is the single most important new feature cluster.

## 9.1 Why it matters

Reunion is where attachment becomes visible.
Without this, every app open feels identical.

## 9.2 Absence buckets

The system should classify absence into buckets.
Suggested starting buckets:

- `SHORT_RETURN`
- `MEDIUM_RETURN`
- `LONG_RETURN`
- `NEGLECT_RETURN`

### Suggested interpretation

**SHORT_RETURN**  
User came back fairly soon.
Pet should react lightly, not dramatically.

**MEDIUM_RETURN**  
Normal useful gap.
Pet can show more warmth or context.

**LONG_RETURN**  
Meaningful absence.
Pet can show stronger state consequences.

**NEGLECT_RETURN**  
Repeated or harsh gap pattern.
Pet should not melodramatically punish the user, but should show cooled warmth or hesitation.

## 9.3 Return context inputs

Greeting should not be based on elapsed time alone.
It should consider:

- absence bucket
- current needs
- mood valence/arousal
- bondScore
- trustScore
- attachmentScore
- neglectStreak
- recent careStreak
- personality traits
- prior session ending tone if available

## 9.4 Greeting output dimensions

A greeting should resolve across several axes:

- visible emotion
- greeting style
- intensity
- duration
- whether audio plays
- whether follow-up invitation appears later in session

## 9.5 Greeting styles

Suggested styles:

- `WARM`
- `GENTLE`
- `SLEEPY`
- `PLAYFUL`
- `NEEDY`
- `HESITANT`
- `DISTANT`
- `RELIEVED`

## 9.6 Important product rule

Do not turn long absence into guilt manipulation.
That will feel fake and unpleasant.

Correct behavior:

- a bit cooler
- a bit unsure
- slower to re-open emotionally
- recoverable through soothing and consistency

Incorrect behavior:

- punishing the user
- excessive sadness every time
- dramatic attachment spam

---

# 10. Care loop design

The pet needs a small but real care loop.
Not a chore loop.

## 10.1 Core care actions for this phase

Suggested user actions:

- tap / pet
- long press / hold
- play
- feed
- soothe
- linger without acting
- ignore while present

## 10.2 Each action must produce 4 things

Every action should produce:

1. visible reaction
2. state update
3. relationship update
4. logged event / memory trace

If any action only triggers animation and nothing else, it is too shallow.

## 10.3 Example action effects

### Tap / pet

Primary purpose:

- low-friction affection

Likely effects:

- small mood improvement
- small bond increase
- slight socialNeed relief
- slight comfort increase if not spammed
- possible irritation if repeated too rapidly

### Play

Primary purpose:

- engagement and stimulation

Likely effects:

- stimulation up
- mood valence up
- energy down a bit
- socialNeed relief
- bond up

### Feed

Primary purpose:

- direct care

Likely effects:

- hunger down
- comfort up
- trust slightly up
- mood improved if hunger was meaningful

### Soothe

Primary purpose:

- repair / emotional safety

Likely effects:

- comfort up
- arousal down
- trust up
- best response after neglect or overstimulation

### Long press / hold

Primary purpose:

- closeness / calming

Likely effects:

- comfort up if pet is receptive
- could irritate if pet is overstimulated or sleepy

## 10.4 Anti-spam rule

The same action repeated too quickly must go through diminishing returns.

Suggested effects of burst repetition:

- reduced positive bond gain
- increased overstimulation
- reduced expressiveness
- chance of mild withdrawal reaction

This is critical.
Otherwise the best strategy becomes “tap 100 times,” which breaks the fantasy.

---

# 11. Neglect and recovery design

A living creature must be affected by inconsistency.
But the product must stay emotionally safe and pleasant.

## 11.1 Neglect in this product

Neglect should mean:

- long absences relative to recent pattern
- repeated lack of meaningful interaction
- opening app and leaving immediately many times
- interacting only in spammy ways without comfort/care

## 11.2 Effects of neglect

Neglect should influence:

- greeting warmth
- trust recovery speed
- invitation frequency style
- visible hesitation or distance
- emotional responsiveness

It should **not** instantly nuke:

- all bond
- all friendliness
- all progression

## 11.3 Recovery design

Recovery is as important as neglect.
The user must be able to repair the relationship.

Recovery should happen through:

- soothing
- feeding when hungry
- gentle petting with healthy timing
- lingering in a calm session
- repeated consistent check-ins over time

## 11.4 Recovery emotional texture

The most emotionally satisfying reactions in this phase will often come from repair.

Examples:

- hesitant -> softening after soothe
- distant -> gentle warmth after a few caring actions
- tired and needy -> visibly relieved after feeding and comfort

This gives the user a sense that the pet is emotionally responsive, not static.

---

# 12. Invitation system design

The pet should sometimes seek attention, but never like a pop-up system.

## 12.1 What invitations are

Invitations are small bids for interaction.
Examples:

- curious look
- soft audio chirp
- play-ready expression
- needy check-in after low social fulfillment

## 12.2 Invitation triggers

The system may consider:

- low stimulation
- high socialNeed
- medium/high attachment
- healthy enough comfort/trust
- current session inactivity
- time since last invitation

## 12.3 Suppression rules

Invitation must be suppressed when:

- recent invitation was ignored
- multiple invitations already happened this session
- pet is sleepy / low energy
- pet is distant from neglect
- user is rapidly spamming actions

## 12.4 Product rule

Invitation should be a whisper, not a notification.
It must feel organic and optional.

---

# 13. Rare affection and variable reward

This is where the pet becomes emotionally sticky.
But it must be done with restraint.

## 13.1 Goal

Occasionally, the pet should do something slightly more special than usual.
This creates anticipation and warmth.

## 13.2 Good rare moments

Examples:

- unusually warm reunion after a healthy pattern of care
- soft affection reaction after soothe
- special playful spark when mood, bond, and stimulation align
- quiet “settled with you” behavior after a calm session

## 13.3 Bad rare moments

Avoid:

- explicit loot-style randomness
- manipulative FOMO
- content spam disguised as surprise

## 13.4 Rules for rare behavior

Rare affection should depend on:

- bond/trust/attachment
- session tone
- recent care quality
- cooldown since last rare moment
- personality compatibility

Rare moments should be:

- infrequent
- emotionally earned
- subtle enough to feel personal

---

# 14. Progression design

Progression here means emotional deepening.
Not XP.

## 14.1 Relationship stages

Suggested stages:

- `STRANGER`
- `FAMILIAR`
- `ATTACHED`
- `BONDED`

## 14.2 What stage affects

Stage may affect:

- greeting warmth ceiling
- likelihood of soft affection reactions
- recovery speed after minor neglect
- invitation style
- expressiveness richness
- tolerance to bursty interaction

## 14.3 What stage must not do

Do not make stage:

- a visible grinding bar
- a mechanical unlock screen
- the main user motivation

It should mostly be felt in behavior.

---

# 15. Personality influence design

Personality should influence scoring, not just descriptors.

## 15.1 Core use of traits

Traits should bias:

- which reaction family wins
- how strong invitation drive is
- how quickly irritation rises
- how fast comfort returns
- how warm greetings become at high bond

## 15.2 Examples

### High curiosity

- more likely to investigate
- more likely to shift toward curious emotion during idle/session variation
- more tolerant of novelty

### High sociability

- stronger check-ins
- warmer reunions
- greater socialNeed sensitivity

### High playfulness

- more likely to invite play when stimulation is low
- faster shift to excited reactions

### High patience

- less overstimulation from bursty taps
- slower withdrawal from spam

### High attachment

- stronger absence effect
- stronger comfort from soothe/hold
- warmer reunion when trust is good

### High energyProfile

- slower drift into sleepy, faster playful recovery

---

# 16. Session variation design

Every session should not feel the same.
But variation must remain explainable.

## 16.1 Variation sources

Allowed sources:

- current internal state
- return context
- mood climate
- personality bias
- recent interaction pattern
- rare-affection eligibility

## 16.2 Variation types

Examples:

- different greeting style for same absence bucket but different mood
- different invitation timing for two pets with different traits
- same play action causing different emotional tone depending on current comfort

## 16.3 What not to do

Do not randomize core feelings arbitrarily.
Randomness should flavor expression, not replace state logic.

---

# 17. Behavior selection model

This phase needs a lightweight but real scoring model.

## 17.1 Why scoring is needed

If behavior is chosen by rigid if-else only, the pet will feel brittle and repetitive.
A weighted resolver gives more organic variation while remaining debuggable.

## 17.2 Candidate behavior families

Suggested candidates:

- greet warm
- greet distant
- greet sleepy
- seek comfort
- invite play
- stay calm / settle
- ask for food
- curious idle
- withdraw slightly
- relief after soothe

## 17.3 Inputs to scoring

Each candidate can score from:

- needs
- mood
- relationship
- personality
- absence context
- session context
- cooldown/suppression
- recent repetition penalties

## 17.4 Important rule

Scoring reasons must be inspectable in debug UI.
The team must be able to answer:

- why did this candidate win?
- what penalties were applied?
- what conditions suppressed the obvious choice?

---

# 18. Baseline tuning targets

These are qualitative tuning goals for early balancing.

## 18.1 The pet should feel…

- present, not passive
- affectionate, not clingy
- responsive, not frantic
- moody, not erratic
- variable, not random
- forgiving, not consequence-free

## 18.2 Failure patterns to watch

### Too flat

Symptoms:

- every session feels identical
- greetings feel cosmetic
- user actions do not matter

### Too chaotic

Symptoms:

- mood swings too much
- pet becomes unreadable
- same action yields wildly inconsistent output

### Too needy

Symptoms:

- constant invitations
- repeated sound cues
- overreactive absence logic

### Too cold

Symptoms:

- positive care has too little visible effect
- recovery feels impossible
- bond progression is not emotionally noticeable

---

# 19. Debug and balancing surfaces

This phase absolutely requires deeper debug tooling.

## 19.1 Minimum debug panels

### Current creature state

Show:

- needs
- mood values
- bond/trust/attachment
- neglect/care streak
- current relationship stage
- personality trait values

### Greeting resolver

Show:

- absence bucket
- selected greeting style
- selected visible emotion
- top candidate scores
- suppression penalties

### Interaction effect panel

Show:

- action type
- raw state delta
- relationship delta
- resulting emotion
- whether diminishing returns applied

### Session summary panel

Show:

- interaction count
- invitation count
- ignored invitation count
- rare-affection triggered yes/no
- repair event count

## 19.2 Why this matters

Without these surfaces, the team will keep guessing and balancing will become subjective and slow.

---

# 20. Data model recommendation (next-step scope)

This does not need final database code yet, but the product model should be explicit.

## 20.1 Persistent current state

Single active pet state should include:

- needs
- mood
- relationship scores
- current stage
- lastUpdatedAt
- lastOpenAt
- lastMeaningfulInteractionAt

## 20.2 Personality profile

Persist:

- curiosity
- sociability
- playfulness
- patience
- attachment
- energyProfile
- lastTraitUpdatedAt

## 20.3 Session state

Ephemeral or semi-persistent:

- sessionStartAt
- sessionInteractionCount
- lastInvitationAt
- ignoredInvitationCount
- lastActionType

## 20.4 Event records

Persist events for:

- decay applied
- greeting resolved
- care action applied
- neglect detected
- recovery action applied
- invitation emitted / ignored / accepted
- rare affection triggered

## 20.5 Future-safe note

Do not over-normalize too early.
A single active-pet architecture is fine for now.
The priority is believable behavior, not enterprise data modeling.

---

# 21. Suggested emotional state machine thinking

The pet does not need a rigid emotional FSM for everything, but some bounded transitions are helpful.

## 21.1 Example transition logic

- repeated soothing after tension -> `RELIEF`
- long absence + high bond + okay trust -> `WARM` or `RELIEVED`
- long absence + neglect streak + low comfort -> `HESITANT`
- low energy + high sleepiness -> `SLEEPY`
- high hunger + decent trust -> `NEEDY`
- high stimulation + high playfulness -> `EXCITED`
- high interruption burst + low patience -> `WITHDRAWN`

These are not full states of the whole creature.
They are short reaction outcomes emerging from the layered model.

---

# 22. Example scenarios

## 22.1 Healthy reunion

Context:

- medium absence
- bond high
- trust good
- sleepiness low
- hunger moderate

Output:

- warm greeting
- soft happy/curious emotion
- optional light audio
- later mild play invitation

## 22.2 Neglect repair

Context:

- long absence repeated twice recently
- trust slightly low
- attachment still high
- user returns and uses soothe + feed

Output:

- initial hesitant greeting
- pet softens after soothe
- relief-after-care reaction appears
- trust recovers modestly
- session ends more warmly than it started

## 22.3 Overstimulation

Context:

- user taps rapidly many times
- pet patience average
- stimulation already high

Output:

- first few taps positive
- later taps diminished
- expression becomes mildly overwhelmed
- comfort drops slightly
- further invitation suppressed

## 22.4 Sleepy pet

Context:

- late in daily rhythm or long elapsed time
- sleepiness high, energy low

Output:

- slow greeting
- smaller movement intensity
- soothe is more effective than play
- play may be accepted weakly or deferred

---

# 23. Implementation priority recommendation

Do not build everything at once.
The correct order is:

1. state foundation
2. decay engine
3. derived conditions
4. greeting resolver
5. care action effects
6. neglect + recovery
7. scoring resolver
8. personality influence
9. invitation logic
10. rare affection + tuning

If the order is violated, the team will add surface reactions without enough internal structure.
That will create rewrite risk.

---

# 24. Definition of done for this phase

This next-step phase is truly done when all of the following are true:

## 24.1 Product reality checks

- app open feels like a reunion, not a static page
- user actions have visible and persistent emotional consequences
- the pet can become slightly warmer or slightly more hesitant depending on history
- recovery after neglect is possible and emotionally visible
- the pet occasionally feels surprising in a subtle, earned way

## 24.2 System reality checks

- state survives restart
- elapsed time changes the pet meaningfully
- greeting selection is explainable
- care actions update state and relationship for real
- behavior selection is inspectable in debug surfaces

## 24.3 Differentiation checks

- two pets with different personality values do not react identically
- two sessions with different absence and care history do not open the same way

If these are not true, the phase is not complete, even if many UI changes exist.

---

# 25. Final product direction

The job of this next-step phase is to create the pet’s **inner life**.

Not more screens.
Not more buttons.
Not more AI.

The pet becomes compelling when the user senses three things:

- it has a state
- it remembers how it has been treated
- it responds in a way that feels emotionally coherent

That is the foundation on which later audio, memory richness, perception, and physical embodiment can become genuinely powerful.
