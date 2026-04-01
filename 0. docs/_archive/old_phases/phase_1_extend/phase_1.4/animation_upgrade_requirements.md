# Animation Upgrade Requirements — AI Pet Home (Looi-like)

## Purpose
Upgrade the current pet animation system from simple blinking to a fully state-driven, layered animation system that makes the pet feel alive, responsive, and emotionally expressive.

---

## 1. Problem Statement

Current animation is too limited:
- Mostly blinking
- Minimal variation
- No anticipation or follow-through
- Feels like animated UI, not a living creature

Goal:
Create a **living digital pet animation system**.

---

## 2. Design Principles

The pet must:
- Show attention
- Anticipate actions
- React with emotional continuity
- Settle naturally
- Idle with variation
- Avoid repetition

---

## 3. Animation Structure (Mandatory)

Every reaction must follow:

1. Trigger
2. Attention
3. Anticipation
4. Main Reaction
5. After-effect
6. Settle
7. Return to Idle

---

## 4. Motion Layers

Implement layered animation:

- Blink Layer
  - normal
  - double
  - sleepy
  - startled

- Eyelid Layer
  - squint
  - wide
  - droopy

- Gaze Layer
  - center
  - drift
  - snap
  - jitter

- Eye Shape
  - squish
  - stretch
  - tension

- Breathing / Life Motion
  - subtle idle rhythm

---

## 5. Idle System

Minimum 5 idle families:

- Calm
- Curious
- Sleepy
- Expecting
- Playful

Each must:
- vary timing
- vary gaze
- avoid loops

---

## 6. Emotional States

Each emotion must have variants:

- HAPPY
- CURIOUS
- SLEEPY
- HUNGRY
- SAD
- EXCITED
- STARTLED

Each includes:
- visual style
- motion pattern
- intensity variation

---

## 7. Interaction Behaviors

### Tap
- happy
- playful
- annoyed (if spam)

### Long Press
- cuddle
- overstimulated
- sleepy

### Sound
- detect
- look direction
- react

---

## 8. Mini Game Integration

Pet must:
- react during game
- celebrate win
- show fail emotion
- influence state

---

## 9. State Leakage

Pet internal state must affect animation:

- low energy → slower motion
- hungry → attention seeking
- high bond → affectionate
- sleepy → droopy eyes

---

## 10. Anti-Repetition

Implement:
- weighted randomness
- cooldown
- history suppression
- rare behaviors

---

## 11. Rare Micro Behaviors

Examples:
- one-eye blink
- side glance
- fake ignore
- micro nod

---

## 12. Transition System

Types:
- smooth
- snap
- sleepy drift
- startled cut

---

## 13. Priority Rules

Priority:
1. Startled
2. Strong reactions
3. Medium reactions
4. Idle

---

## 14. Talking Integration

Talking must:
- sync with animation
- include emphasis
- include settle

---

## 15. Architecture Requirement

Must implement:

- FaceResolver
- IdleDirector
- ReactionOrchestrator
- AntiRepeatGuard

---

## 16. Definition of Done

System is complete when:

- Pet feels alive when idle
- No obvious repetition
- Reactions feel natural
- Emotions are readable
- Interaction feels responsive

---

## 17. Execution Instruction for AI

- Do NOT implement as simple animation swaps
- Build a layered animation runtime
- Use state + event driven system
- Keep build passing after each step
- Iterate until animation feels alive, not mechanical
