# Object Recognition — Full Task Plan

Version: v1
Scope: Android Pet Brain / Phase 1 object-recognition improvement backlog
Source basis: `AGENTS.md`, `project_manifest.md`, `development_roadmap.md`, `02_android_pet_brain_architecture.md`, `07_robot_memory_system.md`, `09_pet_app_definition_full.md`, and the existing object-recognition architecture note.

---

## 1. Goal

Turn the current object pipeline from **raw class detection + cooldown + ask dialog** into a calmer, more accurate, event-driven pipeline that can:

- detect useful objects more reliably
- suppress noisy false positives
- reason about objects near a detected face/person
- ask about genuinely new objects only when evidence is stable
- remember known objects more cleanly
- expose enough debug visibility to tune and verify behavior

This plan is intentionally split into **small, build-safe, verifiable tasks**.

---

## 2. Product target

The target is **not** “best possible generic CV benchmark score”.

The target is:
- the pet notices meaningful nearby objects
- the pet behaves as if it understands scene context a bit better
- the pet does not spam “What is this?”
- the object system fits the Phase 1 rules:
  - offline-first
  - Android as the brain
  - modular
  - event-driven
  - visible/debuggable after every task

---

## 3. Current problems to solve

Based on the project docs and the architecture note, the current weakness is likely a combination of:

- object detection is still too close to raw detector output
- class-level labels are being treated too eagerly as meaningful observations
- object continuity across frames is weak or missing
- confidence carry-over / decay behavior is not strict enough
- there is not yet a strong face-object spatial association layer
- unknown object prompting is too close to detector output
- object memory is still much weaker than person memory
- debug visibility is not rich enough to explain why the system decided to ask / suppress / ignore

---

## 4. Delivery strategy

Do the work in this order:

1. **Stabilize runtime evidence**
2. **Add contextual reasoning with face/person proximity**
3. **Upgrade unknown-object decision logic**
4. **Upgrade known-object memory and manageability**
5. **Only then consider instance-level object memory**

Do **not** jump to custom models, segmentation, or open-vocabulary systems first.

---

## 5. Locked boundaries

### Must preserve

- existing Android architecture and module boundaries
- offline-first Phase 1 behavior
- event-driven flows
- build-safe incremental implementation
- existing known-good face pipeline behavior
- existing UI routes unless a task explicitly changes one

### Must not do

- no cloud-first object understanding
- no giant one-shot rewrite of perception
- no placeholder production logic
- no dead assets or dead state machines
- no hidden threshold tweaks without debug visibility
- no broad unrelated refactors

---

## 6. Task map overview

### Core implementation batches

- **OBJ-01** Runtime debug baseline for object perception
- **OBJ-02** Short-lived object tracking foundation
- **OBJ-03** Confidence smoothing, decay, and stale-state reset
- **OBJ-04** Object quality gating and invalid-detection filtering
- **OBJ-05** Normalized object observation event layer
- **OBJ-06** Face-object spatial association scoring
- **OBJ-07** Object salience and scene-role classification
- **OBJ-08** Unknown object candidate state machine + suppression
- **OBJ-09** Teach-object flow upgrade to candidate-based prompting
- **OBJ-10** Known object memory refresh and seen-stat updates
- **OBJ-11** Debug/Object and Debug/Memory UX improvements
- **OBJ-12** Object deletion / cleanup end-to-end
- **OBJ-13** Runtime parameter tuning surface and calibration support

### Medium-term extension batches

- **OBJ-14** Auto-capture after object naming
- **OBJ-15** Optional object embedding / instance memory foundation
- **OBJ-16** Known-object re-identification beyond class label
- **OBJ-17** Priority behavior hooks for meaningful nearby objects

---

## 7. Detailed task definitions

---

# OBJ-01 — Runtime debug baseline for object perception

## Goal
Make the current object pipeline inspectable before changing decision logic.

## Why first
Without visibility, later threshold/tracking work becomes guesswork.

## Scope
Add or improve debug output for current raw object perception.

## Must change
- surface detector model name if available
- show last inference time
- show top detections per frame with:
  - label
  - confidence
  - bbox size
  - timestamp / age
- show whether the detection is currently considered known/unknown if that logic already exists
- show whether object prompting is currently suppressed by cooldown

## Must not do
- do not change recognition behavior yet
- do not redesign the full app UX

## Definition of done
- developer can see why the system is reacting to current object detections
- current behavior is easier to diagnose without reading logs only

## Verification
- run camera pipeline
- point at several scenes
- confirm debug screen/log shows current detections, confidence, and inference timing

---

# OBJ-02 — Short-lived object tracking foundation

## Goal
Introduce track continuity so an object seen across nearby frames is treated as one evolving observation instead of unrelated detections.

## Scope
Add a lightweight object-track layer above raw detector output.

## Must change
- create `ObjectTrack` model with fields such as:
  - trackId
  - canonicalLabel
  - firstSeenAt
  - lastSeenAt
  - stableFrameCount
  - missedFrameCount
  - smoothedConfidence
  - lastBoundingBox
  - lastCenter
- match new detections to existing tracks using simple spatial/label heuristics
- expire tracks after a bounded miss window

## Must not do
- do not add long-term memory yet
- do not add object embeddings yet

## Definition of done
- repeated detections of the same object across nearby frames update one track instead of producing fully independent reactions

## Verification
- hold one object steady in view
- confirm the same track persists over multiple detections
- move object out of frame and confirm track expires

---

# OBJ-03 — Confidence smoothing, decay, and stale-state reset

## Goal
Stop confidence from jumping or lingering unrealistically when evidence is weak or gone.

## Scope
Add temporal smoothing and decay on top of object tracks.

## Must change
- confidence should rise only with repeated valid evidence
- confidence should decay when detections disappear
- stale tracks should not keep high confidence forever
- display/debug confidence should reflect smoothed confidence, not only last raw score
- confidence reset rules must handle broken continuity cleanly

## Must not do
- do not hide raw confidence; keep it visible for debug if possible

## Definition of done
- object confidence no longer stays spuriously high without fresh evidence
- noisy one-frame spikes have less impact on reactions

## Verification
- show an object briefly, then remove it
- confirm confidence decays cleanly
- confirm noisy frames do not create long-lived high-confidence state

---

# OBJ-04 — Object quality gating and invalid-detection filtering

## Goal
Block clearly low-value detections before they enter the meaningful object pipeline.

## Scope
Add minimum quality filters.

## Must change
- minimum bbox size gating
- edge-of-frame handling
- invalid/degenerate bbox rejection
- label confidence floor for track entry
- optional per-label stricter floors if needed
- reject observations too unstable to be meaningful

## Must not do
- do not overfit to one object category only

## Definition of done
- obvious junk detections no longer become strong candidates as easily

## Verification
- test cluttered scenes and empty scenes
- confirm low-quality detections are filtered before becoming stable tracks

---

# OBJ-05 — Normalized object observation event layer

## Goal
Move object perception from raw detector output to a cleaner event model aligned with the project architecture.

## Scope
Publish stable observation events from tracks rather than from every raw frame result.

## Must change
- define normalized object observation payload
- publish events such as:
  - `OBJECT_TRACK_CREATED`
  - `OBJECT_TRACK_UPDATED`
  - `OBJECT_TRACK_EXPIRED`
  - `OBJECT_OBSERVATION_STABLE`
- include enough debug metadata:
  - trackId
  - label
  - smoothedConfidence
  - bbox
  - stableFrameCount

## Must not do
- do not trigger teach prompts directly from raw detector output anymore where this task touches the path

## Definition of done
- downstream logic can react to stable object observations instead of noisy frame-level outputs

## Verification
- inspect event viewer/logs
- confirm events represent track lifecycle, not every raw detection frame

---

# OBJ-06 — Face-object spatial association scoring

## Goal
Allow the pet to reason about whether an object is meaningfully near a face/person instead of merely present in the scene.

## Scope
Introduce association scoring between object tracks and current face/person observations.

## Must change
- consume face/person bbox context from current perception pipeline
- compute association features such as:
  - normalized center distance
  - overlap / near-overlap heuristics
  - relative size plausibility
  - continuity across time
  - left/right/above/below relation if useful for debug
- output an association score and/or relation enum

## Suggested relation enums
- `NEAR_FACE`
- `LIKELY_HELD_BY_PERSON`
- `BACKGROUND_OBJECT`
- `UNCERTAIN_ASSOCIATION`

## Must not do
- do not claim true physical ownership or hand-contact certainty without evidence

## Definition of done
- object observations can be classified as near-person vs background with explainable heuristics

## Verification
- hold an object near a visible face
- place another object far in the background
- confirm relation scores differ clearly in debug output

---

# OBJ-07 — Object salience and scene-role classification

## Goal
Teach the runtime to care more about meaningful nearby objects than random background clutter.

## Scope
Create a salience layer above tracking and association.

## Must change
- compute `objectSalienceScore` from factors such as:
  - stability
n  - size
  - confidence
  - near-face association
  - recurrence over a short window
- classify objects into roles such as:
  - meaningful nearby object
  - stable background object
  - transient noise
  - uncertain object
- bias downstream reactions toward meaningful nearby objects only

## Must not do
- do not hardcode all salience to near-face only; some large important non-face objects may still matter later

## Definition of done
- the runtime has a clearer notion of which object is worth attention now

## Verification
- show multiple objects in frame
- confirm debug UI indicates one or a few salient objects instead of treating all detections equally

---

# OBJ-08 — Unknown object candidate state machine + suppression

## Goal
Replace label-only unknown prompting with a more reliable candidate-based decision flow.

## Scope
Create `UnknownObjectCandidate` and suppression logic.

## Must change
- add candidate model fields such as:
  - candidateId
  - representativeLabel
  - sourceTrackIds
  - firstSeenAt
  - lastSeenAt
  - stableFrameCount
  - salienceScore
  - nearFaceScore
  - status = `COLLECTING | READY_TO_ASK | ASKED | SUPPRESSED | RESOLVED`
  - lastPromptAt
  - suppressedUntil
- merge repeated observations into the same candidate when appropriate
- do not ask immediately from a single noisy observation
- suppress re-prompting for the same candidate
- add encounter-level guard so the same ongoing scene does not spam prompts

## Must not do
- do not rely only on generic per-label cooldown anymore for candidate-ready decisions

## Definition of done
- the pet only prepares to ask about objects that stay stable long enough and matter enough
- repeated immediate “What is this?” spam is reduced sharply

## Verification
- keep an unknown object in frame
- confirm the system collects evidence before asking
- dismiss/skip the prompt
- confirm it does not immediately re-ask for the same candidate

---

# OBJ-09 — Teach-object flow upgrade to candidate-based prompting

## Goal
Connect the teach dialog to the new candidate pipeline instead of raw detection events.

## Scope
Upgrade the existing ask/teach object flow.

## Must change
- trigger object ask dialog from `UnknownObjectCandidate` readiness, not from raw detection
- pass candidate context into the dialog
- preserve single-active-dialog guardrails
- resolve candidate on successful naming
- mark candidate suppressed or skipped appropriately on dismiss/skip
- emit clear events for ask/resolve/suppress transitions

## Must not do
- do not remove the existing teach UX unless replaced by equivalent working behavior

## Definition of done
- object teach behavior is calmer, more explainable, and tied to stable candidates

## Verification
- trigger an unknown object candidate
- confirm dialog appears only after stable evidence
- name the object and confirm candidate resolves cleanly

---

# OBJ-10 — Known object memory refresh and seen-stat updates

## Goal
Make the system behave more coherently when a known object reappears.

## Scope
Upgrade known-object handling in memory/repository layer.

## Must change
- update known object `lastSeenAt`
- increment seen counters if such fields exist or add them if aligned with data model
- refresh alias/name display on reappearance
- connect stable observation events to object repository updates
- ensure known-object recognition prefers the stored alias over generic label in surfaces that should feel pet-like

## Must not do
- do not pretend instance-level recognition exists if it still only knows class + alias mapping

## Definition of done
- known objects feel remembered and refreshed when they reappear

## Verification
- teach a known object
- show it again later
- confirm seen stats and alias display update correctly

---

# OBJ-11 — Debug/Object and Debug/Memory UX improvements

## Goal
Make object behavior diagnosable and memory state manageable.

## Scope
Improve relevant debug screens for object tracking, candidates, and known objects.

## Must change
- create or improve a Debug/Object surface showing:
  - current active tracks
  - label
  - smoothed confidence
  - salience score
  - face association relation/score
  - candidate state if linked
  - suppression state
- improve known-object list readability
- add useful empty states and refresh behavior

## Must not do
- do not overdesign a polished consumer UI; keep it practical and developer-facing

## Definition of done
- a developer can understand current object reasoning from the debug UI without diving into code

## Verification
- run object scenes with and without faces
- confirm active tracks, candidates, and relations are visible in debug UI

---

# OBJ-12 — Object deletion / cleanup end-to-end

## Goal
Allow deleting wrongly taught or stale object memories safely.

## Scope
Add real object deletion from debug/manage screens.

## Must change
- add delete action with confirmation
- remove object record from repository/storage
- clean up related aliases/linked samples/related memory rows if supported by current model
- refresh UI after delete
- ensure deleted object no longer behaves as known in recognition path

## Must not do
- do not make delete cosmetic only

## Definition of done
- objects can be removed safely and stop affecting recognition/memory behavior

## Verification
- teach an object
- delete it
- verify it disappears from list and is no longer treated as known afterward

---

# OBJ-13 — Runtime parameter tuning surface and calibration support

## Goal
Make the object system tunable without repeated hidden code edits.

## Scope
Expose the key runtime knobs used by the new pipeline.

## Must change
- centralize tunable parameters such as:
  - detection confidence floor
  - track match threshold
  - track expiry window
  - stable frame minimum
  - salience threshold
  - near-face threshold
  - prompt readiness threshold
  - suppression durations
- ensure debug surfaces show current values and perhaps decision traces if feasible
- keep defaults safe and production-sensible

## Must not do
- do not create a giant uncontrolled settings surface for end users

## Definition of done
- later tuning does not require hunting for scattered magic numbers

## Verification
- inspect debug output and confirm tuned values affect behavior in expected ways

---

# OBJ-14 — Auto-capture after object naming

## Goal
Strengthen newly taught object memory after naming instead of relying on a single frame/crop.

## Priority
Medium-term, after OBJ-08 to OBJ-13 are stable.

## Scope
After a user names a new object, collect a short burst of additional high-quality crops/observations.

## Must change
- open a brief capture window after successful object naming
- collect several best crops/observations from the stable track
- save additional sample references/metadata as allowed by current storage design

## Definition of done
- object memory for newly named objects is less brittle than single-frame teaching

---

# OBJ-15 — Optional object embedding / instance memory foundation

## Goal
Prepare for remembering specific physical items, not just generic labels.

## Priority
Medium-term / optional.

## Scope
Lay the groundwork for object-instance embeddings or lightweight re-identification.

## Must change
- define abstraction for optional object embedding engine
- define storage model for object-instance features
- keep current architecture compatible even if engine is not yet enabled in product mode

## Must not do
- do not force a heavy model into current MVP path unless verified acceptable on-device

## Definition of done
- project can evolve toward instance-level object memory without redoing the entire object pipeline

---

# OBJ-16 — Known-object re-identification beyond class label

## Goal
Allow the pet to distinguish “this specific object I know” from only “some object of that class”.

## Priority
After OBJ-15.

## Scope
Use stored object-instance features to improve re-identification of named objects.

## Must change
- add matching logic between object crops and stored object-instance features
- separate class-known from instance-known in decision logic and debug output

## Definition of done
- runtime can represent both:
  - generic class recognition
  - specific object recognition

---

# OBJ-17 — Priority behavior hooks for meaningful nearby objects

## Goal
Let object understanding influence the pet’s expressions/behavior in a controlled way.

## Priority
After core object correctness is stable.

## Scope
Connect meaningful nearby object events to avatar/behavior logic.

## Must change
- create behavior hooks for salient nearby objects
- ensure object-driven reactions obey cooldowns and state-priority rules
- optionally trigger the ASKING state when a stable unknown candidate is readying

## Definition of done
- object understanding is not just a debug feature; it influences pet behavior safely

---

## 8. Recommended execution order

### Phase A — make detections trustworthy
1. OBJ-01
2. OBJ-02
3. OBJ-03
4. OBJ-04
5. OBJ-05

### Phase B — add scene context
6. OBJ-06
7. OBJ-07

### Phase C — fix unknown-object behavior
8. OBJ-08
9. OBJ-09

### Phase D — strengthen memory + manageability
10. OBJ-10
11. OBJ-11
12. OBJ-12
13. OBJ-13

### Phase E — only if needed after validation
14. OBJ-14
15. OBJ-15
16. OBJ-16
17. OBJ-17

---

## 9. What should happen next

For Codex execution, do **not** ask it to implement all of this in one prompt.

The correct next step is:
- start at **OBJ-01** or **OBJ-02** depending on current debug visibility quality
- complete one batch at a time
- require build verification after each task
- carry only relevant remaining risks into the next prompt

If the team wants the first implementation prompt now, the best first engineering slice is usually:

- **OBJ-02 + OBJ-03 together** if current debug visibility is already acceptable
- otherwise **OBJ-01 first**

---

## 10. Definition of success for the whole object-improvement track

This entire track is successful only if, by the end:

- object observations are calmer and less noisy
- the pet stops reacting to random background clutter too often
- nearby human-related objects are prioritized over irrelevant background detections
- object prompting becomes candidate-based and non-spammy
- known objects feel remembered
- the system is debuggable and tunable
- future extension toward object instance memory remains possible without architectural rewrite

