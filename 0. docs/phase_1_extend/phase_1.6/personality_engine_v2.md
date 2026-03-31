# Personality Engine v2 — Production Design

## Goal
Transform pet from behavior-driven → personality-driven creature.

---

## 1. Core Concept

Personality = long-term modifiers that shape:
- intention scoring
- attention bias
- animation style
- interaction tone
- initiative frequency

---

## 2. Trait Model

Core traits (0–100):

- Curiosity
- Sociability
- Playfulness
- Patience
- Attachment
- EnergyProfile

---

## 3. Trait Effects

Each trait affects:

### Curiosity
- increases INVESTIGATE
- increases gaze shifts

### Sociability
- increases SEEK_ATTENTION
- reduces WITHDRAW

### Playfulness
- increases INVITE_PLAY
- faster reactions

### Patience
- reduces irritation
- increases tolerance to spam

### Attachment
- increases user bias
- stronger greeting

### EnergyProfile
- affects base energy curve

---

## 4. Trait Evolution

Traits update based on:

- interaction frequency
- neglect duration
- game participation
- touch patterns
- voice interaction

Rules:
- slow change (days)
- bounded
- decays if no reinforcement

---

## 5. Personality Profiles

Derived from traits:

- clingy
- independent
- playful
- calm
- curious

Used for:
- UI debug
- analytics
- tuning

---

## 6. Integration

Personality feeds into:

- BehaviorEngine scoring
- Attention salience
- Animation intensity
- Invitation frequency

---

## 7. Persistence

Store:
- trait values
- last update
- evolution history (optional summary)

---

## 8. Debug

Expose:
- trait values
- recent changes
- influence on scoring

---

## 9. Definition of Done

- traits evolve over time
- two pets behave differently
- traits affect decisions clearly
