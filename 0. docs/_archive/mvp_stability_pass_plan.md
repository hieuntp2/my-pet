# MVP Stability Pass Plan — AI Pet Android App

Version: v1  
Owner: Hieu Le  
Phase: Post Behavior-Authoritative Experience Integration  
Status: Production implementation plan

---

# 1. Purpose

This document defines the next implementation phase after BehaviorEngine v2 has been connected to the live pet experience.

This phase is not about adding new features.

This phase exists to do one thing well:

> make the current Pet App MVP loop stable, coherent, testable, and product-ready.

The project already has advanced systems:
- real perception
- real Room persistence
- real event system
- real audio stack
- real behavior engine
- real home UX
- real diary/debug tooling

But the product is only ready to be called a complete MVP when the pet core loop works reliably as a product loop, not just as a collection of subsystems.

This phase is therefore a stability and completion phase, not an exploration phase.

---

# 2. Strategic Goal

Lock the app into a state where the following are all true at the same time:

1. Opening the app immediately feels like opening a living pet, not a static screen.
2. Core interactions always produce visible, meaningful, and stateful feedback.
3. State survives restart and changes over real time.
4. Diary / memory visibility works as part of the emotional loop.
5. Audio reaction, if enabled, behaves coherently and does not feel random or broken.
6. The app remains offline-first and usable without cloud dependencies.
7. The core pet loop is verifiable end to end by a human or AI coding agent.

---

# 3. What This Phase Is

This phase is:
- a product hardening pass
- a core loop completion pass
- a state / reaction / continuity validation pass
- a debuggability and verification pass
- a settings/reset readiness pass

This phase is not:
- a new architecture phase
- a cloud AI phase
- a robot body phase
- a full refactor phase
- a feature expansion phase

---

# 4. Source of Truth

Decision hierarchy for this phase:

1. `docs/core/project_sync_status.md`
2. `docs/core/project_manifest.md`
3. `docs/core/development_roadmap.md`
4. `docs/core/pet_app_definition.md`
5. `docs/core/personality_engine.md`
6. `docs/core/memory_system.md`
7. `docs/core/audio_architecture.md`

If anything conflicts, preserve:
- the pet as a digital creature
- offline-first behavior
- event-driven architecture
- state + memory + continuity
- behavior-led experience

---

# 5. Product Standard for Calling MVP Complete

The app should only be called Pet App MVP complete when all of these are true:

1. App open triggers an immediate pet reaction.
2. Tap or interaction triggers visible feedback.
3. The interaction also changes real internal state.
4. At least one stable audio reaction path exists.
5. Returning later changes the pet meaningfully through decay/lifecycle.
6. Diary or memory history is visible and emotionally connected to the experience.
7. The pet does not behave like a silent static animation demo.
8. State and event continuity survive restart.
9. The normal product flow does not crash.
10. The whole core loop works offline.

This phase exists specifically to achieve and verify those conditions.

---

# 6. Core Completion Targets

## 6.1 App-open alive feeling

When the app opens:
- decay/state refresh must happen
- pet condition/emotion must resolve correctly
- the greeting must feel intentional
- the first visible state must not feel delayed, blank, or debug-oriented
- audio response, if the current state supports it, must behave coherently

Success condition:
- the first 3 seconds feel alive

## 6.2 Interaction loop integrity

For each major interaction:
- user action is captured
- pet reacts visibly
- any allowed audio reaction executes or is intentionally skipped
- state changes really persist
- memory/event is recorded
- the next pet behavior reflects the changed state

Success condition:
- interaction is not merely cosmetic

## 6.3 Time continuity

When the user leaves and returns later:
- elapsed time affects state
- state changes are visible in emotion / greeting / behavior
- the pet feels like time passed
- continuity survives restart

Success condition:
- the app does not reset emotionally every session

## 6.4 Diary / memory emotional relevance

The diary should not just be a debug list of events.
It should function as:
- memory visibility
- emotional continuity
- proof that things happened

Success condition:
- memories visible to the user reinforce the pet bond

## 6.5 Audio coherence

Audio must feel integrated, not random:
- no spam
- no incoherent overlaps
- no obvious mismatch between reaction and audio category
- no repeated chirps with no behavioral meaning

Success condition:
- audio strengthens perceived aliveness

## 6.6 Product safety / reset / settings readiness

Before calling MVP complete, the product needs:
- at least minimal sound settings or mute handling
- safe reset or clear-pet-data path
- stable first-run / post-reset reinitialization

Success condition:
- the product can recover from user reset cleanly

---

# 7. Required Workstreams

This phase should be executed in five workstreams.

## Workstream A — Core loop audit and closure

Goal:
Find every remaining place where the loop is incomplete, fake, fragile, or inconsistent.

Focus:
- app-open reaction
- home idle/alive state
- tap and activity interactions
- state update persistence
- event logging
- memory visibility
- restart continuity

Output:
- all critical gaps closed, not just documented

## Workstream B — Stability hardening

Goal:
Make core pet flows robust enough to be called product-ready.

Focus:
- race conditions
- duplicate reactions
- stale UI state
- recomposition side effects
- event spam
- restart issues
- lifecycle inconsistencies

Output:
- calmer, more trustworthy runtime behavior

## Workstream C — Diary and memory validation

Goal:
Ensure memory visibility supports the product loop.

Focus:
- diary loading correctness
- memory card derivation stability
- meaningful labels/timestamps/content
- empty state quality
- post-interaction appearance of memories when expected

Output:
- diary works as a visible emotional feature

## Workstream D — Settings and reset readiness

Goal:
Add the minimum product controls needed for a usable MVP.

Focus:
- sound on/off or mute mode
- reset pet data flow
- reset event/data consistency
- post-reset pet recreation / onboarding / default state safety

Output:
- product can be reset and re-used safely

## Workstream E — Verification system

Goal:
Make the MVP completion state easy to verify repeatedly.

Focus:
- create an explicit in-repo checklist
- optionally add a lightweight in-app MVP verification surface if useful
- make manual verification deterministic
- make AI-agent verification straightforward

Output:
- clear MVP pass/fail process

---

# 8. Implementation Boundaries

## 8.1 Allowed

This phase may:
- fix incomplete or inconsistent product flows
- tighten behavior/audio/avatar coordination
- improve diary visibility if needed
- add minimal settings/reset UI and backing persistence
- add debug/verification hooks
- do small targeted refactors to stabilize execution paths

## 8.2 Not allowed

This phase must not:
- add cloud AI / LLM conversation
- add robot body control
- redesign the whole architecture
- migrate the app to Hilt/Koin
- rewrite navigation broadly
- build new major gameplay systems
- add full assistant/chatbot UX
- start object-teaching UX unless a direct blocker appears
- expand scope into nice-to-have systems unrelated to MVP completion

---

# 9. Quality Bar

Every task in this phase must satisfy:
- real implementation
- no mock production logic
- no TODO placeholders
- visible or verifiable result
- build-safe
- limited scope
- no adjacent feature creep

A task is not complete if it:
- only adds UI without state/persistence when persistence is required
- only logs behavior without changing product experience
- only fixes debug tools but leaves user-facing loop broken
- leaves reset/settings flows half-connected

---

# 10. Engineering Priorities

Order of priority:
1. close the core pet loop
2. stabilize current behavior
3. preserve continuity across time/restart
4. make diary/memory emotionally useful
5. add minimal settings/reset product readiness
6. add explicit MVP verification checklist

This order matters.
Do not start with settings, refactor, or low-value polish before core loop closure is proven.

---

# 11. Completion Definition for This Phase

This phase is done when:
- the current app behaves like a stable digital pet MVP
- the core loop works end to end
- continuity across time and restart is real
- diary contributes to the experience
- sound can be controlled and reset safely
- a checklist exists to decide whether MVP is complete
- remaining risks are genuinely post-MVP, not hidden MVP gaps

---

# 12. Expected Deliverables

This phase should produce:
1. closed product gaps in the running app
2. stable settings/reset handling
3. a written MVP verification checklist document
4. accurate remaining risks after the pass
5. no major drift from the product vision

---

# 13. Review Questions

At the end of this phase, the reviewer should be able to answer yes to these questions:

- Does the pet feel alive immediately on app open?
- Do interactions change more than visuals?
- Does the pet feel different after time away?
- Does the diary help the user feel continuity?
- Can the app survive restart/reset without breaking the pet fantasy?
- Could a new contributor verify MVP readiness without guessing?

If any answer is no, the phase is not complete.
