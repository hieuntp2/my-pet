
# Phase 1.9 — Pet Sensing & Behavior System (v3 — Advanced Creature Engine)

Version: v3 (Pro Level)
Status: Production Architecture + Implementation Spec

---

## 1. Objective

Transform the pet from:
- Reactive system → Autonomous, Emotional, Intentional Creature

Core upgrades:
- Attention Ownership System
- Emotional Inertia Engine
- Memory-Driven Behavior
- Behavior Arbitration Layer

---

## 2. Core Behavioral Evolution

Old:
Event → Reaction

New:
Perception → Attention → Intention → Behavior → Expression → Memory

---

## 3. Attention Ownership System

### Purpose
Pet must "focus" on something like a real creature.

### Model
- target: (person / object / sound / internal need)
- strength: 0–1
- duration
- decay_rate

### Rules
- only ONE dominant focus at a time
- stronger stimulus overrides weaker
- focus decays gradually

### Example
- sound → focus sound
- face appears → override
- user tap → full override

---

## 4. Emotional Inertia Engine

### Purpose
Avoid instant emotional switching

### Model
emotion = base_state + momentum

- momentum accumulates with repeated stimuli
- momentum decays slowly over time

### Example
- repeated play → gradually happier
- no interaction → drift to neutral
- tiredness accumulates over time

---

## 5. Memory-Driven Behavior

### Upgrade
Memory influences behavior, not just stored.

### Effects
- greeting tone
- reaction intensity
- curiosity level

### Example
- known person → stronger greeting
- known object → less curiosity

---

## 6. Behavior Arbitration

### Inputs
- attention target
- emotional state
- stimuli
- cooldown

### Output
- ONE behavior plan

### Priority
1. user interaction
2. urgent internal need
3. attention target
4. idle

---

## 7. Idle Intelligence

Not random anymore.

### Behaviors
- look around
- follow last seen target
- micro curiosity
- boredom actions

---

## 8. Learning System (Refined)

### Person
- familiarity-based
- bond-driven

### Object
- curiosity-driven
- avoid over-learning

---

## 9. Full System Pipeline

Perception
→ Attention
→ Emotion
→ Behavior Arbitration
→ Avatar / Audio
→ Memory Update

---

## 10. Stability & Safety

### Requirements
- no crash allowed
- isolate subsystem failures
- fallback behavior

### Strategy
- local try/catch
- coroutine handler
- global exception handler

---

## 11. Performance Strategy

- adaptive FPS
- throttle ML inference
- degrade under load

---

## 12. Definition of Done

- pet maintains focus (not random)
- emotion transitions smoothly
- memory affects behavior
- no jitter reactions
- system stable under errors

---

## 13. Verification

- observe focus switching
- repeated interaction → emotion builds
- known person → different greeting
- inject exception → no crash

---

## 14. Next Phase

Proceed to Phase 2 ONLY IF:

pet feels alive WITHOUT conversation
