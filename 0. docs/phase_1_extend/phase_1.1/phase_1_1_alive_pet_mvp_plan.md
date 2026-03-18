# 📄 PHASE 1.1 — ALIVE PET MVP PLAN

## 1. Purpose

This phase transforms the current system from a **technical foundation** into a **playable, emotionally responsive pet app (true MVP)**.

The goal is NOT to add more features.
The goal is to make the existing system **feel alive, interactive, and meaningful to users**.

---

## 2. Do We Need Research?

❌ No additional research phase is required.

Reason:
- Product vision is already clear
- Architecture foundation is already built
- Current gap is **execution + experience**, not unknowns

👉 This phase is **execution-heavy**, not research-heavy.

---

## 3. MVP Definition (Target State)

The app is considered MVP-complete when:

- Open app → pet reacts immediately
- User interacts → pet reacts (animation + sound + state change)
- Pet state changes over time (decay)
- User sees pet condition clearly (mood, energy, hunger)
- User has meaningful actions (feed, play, rest)
- Personality begins to show in behavior
- Memory/diary feels human-readable
- App no longer feels like a debug tool

---

## 4. Core Experience Loop (Must Work End-to-End)

```
Open App
→ Pet greets based on state
→ User interacts (tap / feed / play / rest)
→ Pet reacts (animation + sound)
→ State changes
→ Memory recorded
→ Time passes
→ User returns
→ Pet feels different
```

---

## 5. Phase Structure

## Epic 1 — Real Pet State System

### Goal
Introduce persistent, time-evolving pet state.

### Scope
- PetState model:
  - mood
  - energy
  - hunger
  - social
  - bond
  - lastUpdatedAt
- Time-based decay
- Derived states (sleepy, playful, hungry)
- Greeting based on time delta

### Definition of Done
- Pet changes after hours of absence
- State is persisted and restored
- Greeting reflects state

---

## Epic 2 — Home Screen (User-Facing)

### Goal
Replace debug screen with real user experience.

### Scope
- Pet avatar (center)
- Pet name
- Mood indicator
- Energy / hunger indicators
- One-line status text
- Quick actions (feed / play / rest)
- Remove raw debug logs from main UI

### Definition of Done
- User understands pet condition instantly
- No technical logs shown on main screen

---

## Epic 3 — Gameplay (Core Actions)

### Goal
Enable meaningful interaction.

### Scope
- Tap
- Long press
- Feed
- Play
- Rest

Each action must:
- Trigger animation
- Trigger sound
- Update state
- Emit memory event

### Definition of Done
- At least 4 meaningful actions
- Visible and different reactions

---

## Epic 4 — Personality Visibility

### Goal
Make traits affect behavior visibly.

### Scope
- Trait-based reaction weighting
- Personality affects:
  - greetings
  - idle behavior
  - reactions
- Small but noticeable variation

### Definition of Done
- Same action can produce different responses
- Differences are explainable and consistent

---

## Epic 5 — Memory & Diary

### Goal
Make memory emotional, not technical.

### Scope
- Human-readable memory entries
- Daily summary
- Notable moments
- Simple diary screen

### Definition of Done
- Diary is readable and meaningful
- Not just raw event logs

---

## Epic 6 — Animation Upgrade (Initial)

### Goal
Improve visual life of pet.

### Tech Recommendation
- Rive (primary)
- Compose (UI shell)
- Lottie (optional micro animations)

### Scope
- Idle animation
- Blink
- Greeting
- Tap reaction
- Feed/play/rest reactions

### Definition of Done
- Pet feels visually alive
- Animation maps to state/emotion

---

## 6. Batch Execution Plan

### Batch A — Pet State Foundation
- Implement PetState
- Persistence + decay
- Greeting logic

### Batch B — Home UX
- Redesign Home screen
- Remove debug logs
- Add state indicators

### Batch C — Core Gameplay
- Feed / Play / Rest
- Tap / Long press improvements

### Batch D — Personality
- Trait-based behavior weighting

### Batch E — Memory UX
- Diary UI
- Human-readable events

### Batch F — Animation
- Introduce Rive
- Replace placeholder avatar

---

## 7. Priorities

### P0 (Must Have)
- PetState
- Home redesign
- Core actions
- Greeting

### P1 (Should Have)
- Personality effects
- Diary UX
- Basic onboarding

### P2 (Later)
- Refactor architecture
- Advanced animation
- Optimization

---

## 8. Risks

### Product Risks
- Pet still feels repetitive
- Interactions lack depth

### Technical Risks
- Over-coupling state and UI
- Animation integration complexity

### UX Risks
- Too many states without clarity
- Slow feedback

---

## 9. Success Criteria

Phase is successful when:

- Pet feels alive within 5 seconds
- User interacts at least 3 times per session
- Pet visibly changes over time
- Diary contains readable memories
- No debug-only experience remains

---

## 10. Timeline Estimate

- 2 weeks → Playable MVP (internal)
- 3–5 weeks → Demo-ready MVP

---

## 11. Final Principle

This phase is about:

👉 Turning a system into a creature  
👉 Turning events into emotions  
👉 Turning interaction into attachment
