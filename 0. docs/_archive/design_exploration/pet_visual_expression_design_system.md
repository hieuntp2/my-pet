# Pet Visual & Expression Design System (PVEDS)

**Version:** 1.0  
**Scope:** Android AI Pet App (offline-first), avatar face system, animation behavior, AI-generated expression governance  
**Applies to:** IDLE, HAPPY, CURIOUS, SLEEPY (current) and SAD, EXCITED, HUNGRY (future-compatible)

---

## 1. Purpose of This Document

This system defines one production-grade source of truth for how the pet looks, moves, and reacts.

It exists to prevent:
- visual inconsistency across designers, animators, and generated assets
- personality drift where expressions become generic or out-of-character
- random/unmotivated emotional changes
- style fragmentation over time as new expressions are added

**Success criteria:**
- the pet remains recognizable across all expressions and future updates
- every visible emotion can be traced to state/event logic
- new expressions can be generated without breaking visual identity

---

## 2. Core Design Philosophy

The pet is treated as a living digital creature, not a chatbot and not an assistant.

**Design principles:**
- Believability over spectacle
- Emotion and behavior precede verbal or symbolic intelligence
- Every expression must be causally linked to internal state or external event
- The avatar should feel continuously alive, even in quiet moments
- Transitions should communicate continuity of mind, not abrupt state switches

**Hard rules:**
- No random emotion changes
- No expression without a trigger or sustained state
- No hard visual style switches between states
- No mechanical animation loops

**State causality rule:**

event/internal_state → affect update → expression selection → motion realization


---

## 3. Visual Identity Foundation

### 3.1 Shape Language

**Core geometry:**
- Rounded, soft, approachable forms
- Low angularity, minimal sharp corners
- Compact, stable silhouette

**Intent:**
- Comfort, curiosity, warmth
- Never aggressive, uncanny, or hyper-realistic

---

### 3.2 Eye System (Primary Emotional Channel)

Eyes carry most emotional meaning.

**Constraints:**
- Placement and scale are fixed (identity anchor)
- Movement must be anatomically coherent
- Eyelids drive emotion intensity
- Blink timing must be organic and state-dependent

---

### 3.3 Mouth System

Secondary signal channel.

**Rules:**
- Simple shapes only (neutral, soft smile, tiny “o”)
- Never overpower eyes
- No teeth or complex articulation

---

### 3.4 Motion Language

**Global motion:**
- Soft, elastic
- Subtle idle motion
- Emotion-driven amplitude

**By state:**
- IDLE → calm breathing, gaze drift  
- HAPPY → buoyant  
- CURIOUS → attentive, scanning  
- SLEEPY → heavy, delayed  

---

### 3.5 Non-Negotiable Identity Constraints

Must never change:
- Facial proportions
- Eye system logic
- Rendering style
- Motion grammar

---

## 4. Expression System Architecture

**Formula:**

Expression = f(emotion, intensity, energy, personality_traits, context)


### 4.1 Components

| Component | Type | Range | Role |
|----------|------|------|------|
| emotion | categorical | states | base pose |
| intensity | scalar | 0–1 | magnitude |
| energy | scalar | 0–1 | speed |
| personality_traits | vector | normalized | style bias |
| context | struct | event-driven | overrides |

Traits:
- curiosity
- bond_affinity
- playfulness
- calmness

---

### 4.2 Resolution Pipeline

1. Select base emotion  
2. Apply intensity  
3. Apply energy  
4. Apply personality  
5. Apply context  
6. Output → eyes, blink, mouth, motion  

---

### 4.3 Priority Arbitration

1. Interaction relevance  
2. Physiological state  
3. Social context  
4. Ambient signals  

---

## 5. Emotion-to-Visual Mapping

### 5.1 IDLE
- Eyes: neutral
- Movement: slow drift
- Blink: natural
- Motion: breathing

---

### 5.2 HAPPY
- Eyes: softer, brighter
- Motion: bounce
- Mouth: smile
- Timing: fast in, smooth out

---

### 5.3 CURIOUS
- Eyes: widened
- Movement: scanning
- Blink: reduced
- Motion: tilt + hold

---

### 5.4 SLEEPY
- Eyes: heavy lids
- Blink: long closure
- Motion: slow sway
- Timing: very soft transitions

---

## 6. Animation Principles (CRITICAL)

### 6.1 Idle Rules
- Never fully static
- Avoid loop repetition

### 6.2 Blink Rules
- Autonomous
- Emotion-driven timing

### 6.3 Micro-Movement
- Subtle, irregular
- Low amplitude

### 6.4 Transitions
- Always eased
- Allow overlap blending

### DO
- organic variation  
- expressive eyes  

### DON’T
- robotic loops  
- hard switches  

---

## 7. Personality Encoding

### Trait Effects

| Trait | Effect |
|------|-------|
| curiosity | more scanning |
| bond_affinity | more eye contact |
| playfulness | bouncier motion |
| calmness | smoother transitions |

---

## 8. Interaction Response

### Event Priority

| Priority | Type |
|--------|------|
| P1 | high urgency |
| P2 | user interaction |
| P3 | ambient |

---

### Example Reactions

| Event | Behavior |
|------|--------|
| tap | look + curious |
| app_open | wake + greet |
| sound | orient + scan |

---

## 9. Consistency Constraints

NEVER:
- change proportions
- break eye logic
- add new style
- bypass causality

---

## 10. Variation System

### Allowed
- blink timing
- gaze offset
- micro tilt

### Forbidden
- geometry changes
- style changes

---

## 11. Implementation (Rive)

### State Machines
- Emotion
- Blink
- Gaze
- Reaction
- Breath

---

### Parameters

| Name | Type | Purpose |
|------|------|--------|
| emotion | enum | state |
| intensity | float | strength |
| energy | float | speed |
| gazeX/Y | float | direction |

---

## 12. AI Generation Rules

### MUST
- preserve identity
- follow motion grammar
- respect causality

### MUST NOT
- invent style
- exaggerate uncontrollably

---

### Validation Checklist

- Identity preserved ✅  
- Eye logic valid ✅  
- Motion correct ✅  
- Causality clear ✅  
- Personality consistent ✅  