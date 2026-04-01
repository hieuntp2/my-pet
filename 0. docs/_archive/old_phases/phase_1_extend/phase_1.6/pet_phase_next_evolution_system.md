# AI PET — PHASE NEXT: EVOLUTION SYSTEM (POST CORE LIFE)

Version: v2  
Status: Product + System Design  
Scope: The next production phase after the current core life system batches  
Audience: Product owner, game/system designers, Android engineers, AI coding agents  

---

# 1. Purpose of this phase

The previous phase turns the pet from a static animated screen into a reactive digital creature with:

- internal state
- core needs and decay
- greeting logic
- care / neglect loop
- invitation basics
- personality influence
- memory-aware behavior at a foundational level

That is necessary, but it is still not enough for long-term attachment.

This phase exists to solve the next-level product problem:

> How do we make the pet feel like it has continuity, personal history, growing attachment, routines, expectations, and a relationship arc that deepens over weeks instead of minutes?

This phase is where the pet stops being only a responsive system and starts becoming an evolving character.

---

# 2. Product outcome we want

After this phase, the user should feel all of the following:

- the pet remembers what kind of owner I am
- the pet reacts differently depending on how I treated it recently
- the pet has a daily rhythm and expectations
- the pet becomes more itself over time instead of staying flat
- the pet feels closer, more attached, or more distant for understandable reasons
- opening the app after a gap feels emotionally meaningful, not just mechanically stateful

This phase is successful only if the pet becomes harder to forget and easier to bond with.

---

# 3. Strategic shift

## Before this phase

Pet = reactive life simulation

- responds to stimuli
- has visible state
- can be cared for or neglected
- can vary greetings and invitations

## After this phase

Pet = evolving companion system

- maintains autobiographical continuity
- forms relationship dynamics
- detects and adapts to user habits
- evolves trait expression over time
- carries emotional consequences across sessions
- builds a recognizable identity arc

---

# 4. Design principles for this phase

## 4.1 Continuity over spectacle

Do not solve retention with flashy animation spam or noisy systems.

The pet should become sticky because it has continuity and emotional logic, not because it shouts louder.

## 4.2 History must matter

Memory is not a diary feature. It must influence:

- greetings
- mood bias
- trust / attachment
- willingness to initiate
- reaction softness or hesitation

## 4.3 Relationship must be multidimensional

Bond cannot remain a single scalar forever.

A believable pet relationship needs at least separate dimensions for:

- affection
- trust
- dependency / expectation
- stability / consistency

## 4.4 Routine creates anticipation

If the pet learns when the user usually appears, it can feel like it was waiting.

That feeling is stronger than many “clever” AI features.

## 4.5 Personality must evolve slowly

Traits should not swing wildly from one session.

This phase should make the pet feel shaped over days and weeks.

## 4.6 No fake depth

Avoid systems that only store labels but never affect behavior.

If a model is persisted, it must have real downstream consequences.

---

# 5. Scope of this phase

This phase contains five major systems plus two cross-cutting upgrades.

## Major systems

1. Episodic and semantic long-term memory
2. Relationship and bond system v2
3. User habit and routine learning
4. Personality evolution engine v2
5. Daily life simulation engine

## Cross-cutting upgrades

6. Behavior scoring v2
7. Invitation / expectation / reunion upgrade

---

# 6. System 1 — Long-term memory system

## 6.1 Goal

Move from raw event history to meaningful memory.

The pet should not only know that events happened. It should be able to infer patterns from them and use those patterns in future behavior.

## 6.2 Memory layers

### A. Raw event history

Already exists or is expected from prior phases.

Used for:
- audit
- debug
- episode generation
- reprocessing logic

### B. Episodic memory

A grouped interaction story with beginning, end, mood, and relationship meaning.

Examples:
- “You opened the app late at night and played a lot.”
- “You returned after a long absence, but only tapped once and left.”
- “You spent several short caring sessions throughout the day.”

### C. Semantic memory

Stable or semi-stable facts inferred from repeated episodes.

Examples:
- user usually opens the app in the evening
- user tends to interact through play more than feeding
- recent care quality has been inconsistent
- long absences are becoming more common

### D. Relationship memory

A special slice of memory dedicated to the emotional relationship.

Examples:
- recent emotional tone of care
- strongest recent reunion
- repeated neglect pattern
- recovery after neglect

## 6.3 Why event log is not enough

An event log only says what happened.

A pet companion system needs to derive what it means.

Without episode-building and summary layers:
- greetings stay shallow
- attachment stays numeric-only
- memory never changes emotional output
- the pet never feels like it has perspective

## 6.4 Core episodic data model

```text
MemoryEpisode
- id
- startTime
- endTime
- durationMs
- eventCount
- interactionCount
- interactionTypes
- dominantPetEmotion
- dominantPetMood
- careScoreDelta
- bondDelta
- neglectSignal
- reunionType
- userBehaviorTag
- importanceScore
- summaryText
- createdAt
```

## 6.5 Semantic memory model

```text
SemanticMemoryFact
- id
- key
- valueJson
- confidence
- sourceEpisodeCount
- firstLearnedAt
- lastConfirmedAt
- lastUpdatedAt
```

Examples of keys:
- `habit.preferred_time_slots`
- `interaction.primary_style`
- `relationship.recent_consistency`
- `care.recent_quality_band`

## 6.6 Episode generation rules

Episodes should not be created per event.

Episodes should group meaningful interaction windows, for example:
- app open to app background / inactivity timeout
- short burst windows merged if close together
- special reunion episodes split out when long absence preceded the session

Rules should support:
- session summarization
- replayability in debug
- downstream relationship scoring

## 6.7 Importance scoring

Every episode should have an importance score used to bias retrieval and behavior.

Importance may increase when:
- long absence occurred
- large recovery happened after neglect
- unusually high care intensity happened
- new behavior milestone was unlocked
- strong emotional state change occurred

Importance should remain small for routine low-signal sessions.

## 6.8 Memory effects on behavior

Memory must influence:

- greeting tone
- first visible emotion on app open
- invitation intensity after a quiet period
- pet warmth or hesitation
- likelihood of affectionate rare moments
- recovery behavior after improved treatment

## 6.9 Debug requirements

Expose:
- last 20 episodes
- importance score
- summary text
- care / bond deltas
- which episodes affected current greeting / behavior

---

# 7. System 2 — Relationship and bond system v2

## 7.1 Goal

Replace or extend the simplistic bond model with a richer relationship model that can support nuanced pet behavior.

## 7.2 Why one number is not enough

A single “bond” score cannot explain the difference between:
- loving but insecure
- trusting but calm and independent
- attached but unstable after repeated neglect
- affectionate yet low expectation

These feel different and should behave differently.

## 7.3 Relationship dimensions

```text
BondState
- affection
- trust
- dependency
- stability
- lastUpdatedAt
```

### Affection
How warm and emotionally positive the pet feels toward the user.

### Trust
How safe and secure the pet feels based on consistency and care reliability.

### Dependency
How strongly the pet expects and seeks the user.

### Stability
How resilient the relationship feels against short-term fluctuations.

## 7.4 Design behavior of each dimension

### High affection
- softer greetings
- more positive default interpretations
- more affectionate rare moments

### High trust
- less hesitation after short absence
- less defensive reaction after neutral interactions
- more confident reunion behavior

### High dependency
- stronger expectation
- more noticeable loneliness when routine breaks
- higher chance of invitations when unmet

### High stability
- less overreaction to one bad session
- less volatility in behavior
- more mature consistent pet feel

## 7.5 Evolution rules

### Affection increases from:
- repeated caring interactions
- emotionally positive sessions
- play and comfort moments

### Trust increases from:
- consistency
- good reunions after absence
- low spam / respectful interaction patterns

### Dependency increases from:
- regular routine-based return
- frequent strong emotional sessions
- repeated rewarding interactions

### Stability increases from:
- steady patterns over time
- reduced oscillation between care and neglect

## 7.6 Degradation rules

Affection, trust, dependency, and stability should degrade differently.

Examples:
- neglect hurts affection and trust
- inconsistent behavior hurts stability
- long repeated gaps may reduce dependency or distort it into anxious seeking depending on design tuning

## 7.7 Relationship states / labels

Useful derived relationship labels:
- distant
- warming_up
- attached
- clingy
- secure
- unstable
- recovering

These labels are for debug, analytics, behavior rules, and future progression unlocks.

## 7.8 Relationship effects on behavior

Relationship state must affect:
- reunion style
- speed of warmth on app open
- willingness to initiate contact
- tolerance to brief neglect
- softness vs caution in reactions

## 7.9 Recovery arc

A key feature of this phase is that the pet should support emotional recovery.

If recent treatment improved after a neglect period:
- pet should not instantly reset to full warmth
- pet should show cautious recovery
- warmth should rebuild over multiple good sessions

This is essential for perceived emotional realism.

---

# 8. System 3 — User habit and routine learning

## 8.1 Goal

Teach the pet to recognize the user’s behavioral rhythm so it can form expectations.

This is one of the strongest long-term attachment systems because it enables the feeling:

> “It knows when I usually come back.”

## 8.2 What habit learning should observe

- app open time distribution
- session count by daypart
- average session duration
- interaction density
- preferred actions (tap, feed, play, comfort, idle presence)
- consistency vs irregularity

## 8.3 Habit profile model

```text
UserHabitProfile
- preferredTimeSlots
- avgSessionLengthMs
- avgSessionsPerDay
- primaryInteractionStyle
- recentConsistencyScore
- strongestDaypart
- lastUpdatedAt
```

## 8.4 Daypart model

Use at least:
- morning
- day
- evening
- night

Optionally add finer time bands later, but keep the first implementation debuggable.

## 8.5 What habit learning enables

- expectation before usual return window
- softer disappointment when the user misses a usual time
- stronger reunion when the user returns within expected range
- changed invitation style based on when the user usually engages

## 8.6 Behavior effects

### When user returns at a familiar time
- pet can feel ready / expectant
- greeting can be more immediate and warm

### When user misses a common time slot
- pet can show subtle longing or mild disappointment
- later reunion can carry more weight

### When routine becomes unstable
- relationship stability can be affected
- invitations can become less confident or more tentative

## 8.7 Anti-abuse / anti-overfit rules

Do not overfit too fast.

Habit inference should:
- require repeated confirmation
- decay gradually when behavior changes
- stay probabilistic, not brittle

## 8.8 Debug surfaces

Expose:
- strongest daypart
- recent consistency score
- preferred time slots
- how today compares to expected behavior

---

# 9. System 4 — Personality evolution engine v2

## 9.1 Goal

Upgrade the trait system from “stored long-term modifiers” into a slow but meaningful evolutionary layer.

## 9.2 Trait set

Recommended core traits for this phase:

```text
- sociability
- curiosity
- playfulness
- clinginess
- sensitivity
```

Optional to preserve previous naming if the repo already uses a different trait model, but semantics must remain clear.

## 9.3 Trait semantics

### Sociability
How strongly the pet likes contact and interaction.

### Curiosity
How likely the pet is to notice and investigate.

### Playfulness
How often the pet prefers playful reactions and invitations.

### Clinginess
How strongly the pet seeks closeness and reacts to separation.

### Sensitivity
How strongly the pet reacts to neglect, overstimulation, inconsistency, and emotional signals.

## 9.4 Why this evolution matters

Without evolution, traits become decorative metadata.

With evolution:
- two pets under different care patterns diverge
- user style visibly shapes the pet
- long-term identity forms

## 9.5 Trait evolution sources

Traits should evolve from repeated patterns, not isolated moments.

Examples:
- frequent play reinforces playfulness
- repeated warm returns reinforces sociability / clinginess depending on tuning
- repeated neglect may increase sensitivity or reduce sociability depending on model
- comfort-focused care may increase attachment patterns

## 9.6 Trait evolution constraints

- changes should be slow
- each update should be small
- major swings should require repeated evidence
- all trait movement must remain bounded and explainable

## 9.7 Trait effects on behavior

Traits should affect:
- invitation frequency
- greeting intensity
- recovery speed
- rare behavior selection
- visible emotion mapping bias
- initiative confidence

## 9.8 Trait expression versus trait storage

Important distinction:
- a trait value in storage is not enough
- there must be runtime expression logic that changes behavior probabilities and style

## 9.9 Derived personality profiles

Useful profile labels for debug and analytics:
- secure_playful
- needy_attached
- curious_independent
- calm_gentle
- sensitive_unstable

These should not replace traits. They summarize them.

---

# 10. System 5 — Daily life simulation engine

## 10.1 Goal

Give the pet a believable rhythm across the day.

The user should feel that the pet exists in time, not only at the moment the app opens.

## 10.2 Why this matters

Without a day rhythm:
- all greetings feel same-y
- state drift feels purely mechanical
- the pet never feels like it has a “day”

## 10.3 Day lifecycle phases

Recommended minimum phases:
- morning
- day
- evening
- night

## 10.4 Daily life baseline effects

Each phase can bias:
- energy baseline
- mood baseline
- invitation confidence
- greeting softness / activity level
- likely animation and sound intensity

Examples:
- morning: curious / waking energy
- day: stable / neutral / playful potential
- evening: attached / warm / reunion-friendly
- night: sleepy / softer / lower initiative

## 10.5 Interaction with needs

Day lifecycle does not replace needs. It biases them.

For example:
- high sleepiness at night should feel stronger than the same numeric sleepiness at noon
- playful invitation at night should be rarer and more subdued

## 10.6 Interaction with habit learning

If the user usually opens in the evening:
- evening becomes the emotional center of many reunion patterns
- missing that window can matter more

## 10.7 Debug requirements

Expose:
- current day phase
- baseline modifiers
- resolved day bias on greeting / behavior

---

# 11. Cross-cutting system A — Behavior scoring v2

## 11.1 Goal

Upgrade behavior selection from simpler weighted logic into a system that can combine long-term and short-term signals coherently.

## 11.2 Scoring inputs

A behavior score should consider at least:

```text
score =
  stateWeight
+ relationshipWeight
+ memoryWeight
+ habitWeight
+ personalityWeight
+ lifecycleWeight
+ controlledVariation
- suppressionPenalty
- cooldownPenalty
```

## 11.3 Behavior categories

Suggested categories:
- greeting
- idle
- reaction
- invitation
- comfort-seeking
- recovery
- affectionate rare moment

## 11.4 Controlled variation

Variation is required, but it must be small and contextual.

Never allow randomness to override emotional logic.

Good variation:
- two warm greetings with slightly different animation or timing
- multiple playful invitations chosen from a set that matches current mood

Bad variation:
- hungry pet acting ecstatic for no reason
- distant pet suddenly acting clingy without recovery arc

## 11.5 Suppression system

The system must include:
- cooldowns
- anti-repetition suppression
- diminishing returns on repeated triggers
- optional context lockout for annoying loops

## 11.6 Explainability requirement

Every scored decision should be debuggable.

Developers should be able to inspect:
- top candidate behaviors
- component scores
- suppression reasons
- final selected behavior

---

# 12. Cross-cutting system B — Reunion, expectation, and invitation upgrade

## 12.1 Goal

Make app-open and between-session behavior emotionally richer.

## 12.2 Reunion states

Suggested reunion categories:
- quick_return
- routine_return
- late_return
- long_absence_return
- recovery_return
- missed_expected_return

## 12.3 Inputs for reunion resolution

- time since last session
- recent care quality
- current relationship state
- user habit expectation
- current pet needs
- recent important episode

## 12.4 Expected return feeling

This is one of the most powerful emotional mechanics.

When the system knows the user often appears at a certain time, the pet can:
- become subtly expectant near that window
- feel fulfilled if the user returns
- feel slightly off if the window is missed

This should be expressed gently, never melodramatically.

## 12.5 Invitation evolution

Invitation behavior should advance from generic “ask for attention” to context-aware bids.

Examples:
- playful invite when energy is high and care has been good
- soft comfort-seeking when sensitivity is high after inconsistency
- reduced initiative when trust is damaged

## 12.6 Anti-spam requirements

Invitation system must not become retention spam.

Rules:
- hard cooldowns
- confidence threshold
- suppression after ignored invites
- recover over time when relationship improves

---

# 13. Core loops strengthened by this phase

## 13.1 Memory loop

```text
Events
→ episode grouping
→ summary
→ semantic learning
→ behavior bias
→ new experience
→ updated memory
```

## 13.2 Relationship loop

```text
User pattern
→ relationship state update
→ changed reunion / invitation / warmth
→ user responds
→ relationship deepens or destabilizes
```

## 13.3 Routine loop

```text
Repeated schedule
→ expectation
→ anticipated reunion
→ fulfilled or missed expectation
→ emotional effect
→ stronger continuity
```

## 13.4 Personality loop

```text
Repeated interaction style
→ trait drift
→ changed behavioral expression
→ more recognizable pet identity
```

---

# 14. UX and debug surfaces required

This phase adds complexity. Without strong debug visibility, tuning will become guesswork.

## 14.1 Required developer views

### Memory debug
- recent episodes
- semantic facts
- importance scores
- retrieval source for current behavior

### Relationship debug
- affection / trust / dependency / stability
- recent deltas
- recovery or decline trend

### Habit debug
- preferred time slots
- consistency score
- current expectation state

### Personality debug
- trait values
- recent trait drift causes
- active derived personality profile

### Behavior debug
- top candidate behaviors
- score breakdown
- cooldown/suppression reasons
- final selected behavior

## 14.2 Optional user-facing surfaces later

Not required now, but future product-ready surfaces may include:
- pet diary
- relationship summary
- memory highlights
- named moods or milestones

For this phase, prioritize internal debug first.

---

# 15. Anti-patterns to avoid

## 15.1 Memory that never matters

If memory is stored but not used in live behavior, the system becomes expensive decoration.

## 15.2 Bond inflation

If all positive interactions always push bond upward with little resistance, the relationship becomes shallow and trivial.

## 15.3 Hyper-reactive volatility

If one bad session causes the pet to behave dramatically differently, the system feels fake.

## 15.4 Habit overfitting

Do not assume one or two sessions define a real user habit.

## 15.5 Trait chaos

Do not update personality traits too aggressively or too often.

## 15.6 Invitation spam

This destroys trust in the product and weakens emotional authenticity.

## 15.7 Daily lifecycle as mere theme switch

Day phases must affect real behavior selection and emotional framing, not just labels.

---

# 16. Success criteria

This phase is successful when all of the following become true:

1. The user can return after different gaps and clearly feel different reunion styles.
2. The pet’s recent treatment history changes how warm or cautious it behaves.
3. The pet appears to recognize routine and subtle expectation.
4. The pet’s trait expression visibly diverges over time.
5. Developers can explain why a given behavior happened.
6. The system avoids spam and still feels emotionally alive.
7. The pet starts to feel like a companion with memory, not just a responsive toy.

---

# 17. Recommended implementation order

Recommended order for the engineering phase:

1. Episode system
2. Episode summarization and semantic memory basics
3. Relationship model v2
4. Relationship-integrated reunion logic
5. Habit detection and expectation model
6. Personality evolution v2
7. Daily lifecycle engine
8. Behavior scoring v2
9. Invitation / expectation / recovery upgrade
10. Debug surfaces and tuning pass

This order keeps the architecture stable and avoids building advanced scoring before the inputs exist.

---

# 18. Final note

This phase determines whether the pet becomes:

- a polished toy with better state logic

or

- a believable companion system with continuity and attachment

If executed well, this phase creates the first truly defensible emotional moat of the product.
