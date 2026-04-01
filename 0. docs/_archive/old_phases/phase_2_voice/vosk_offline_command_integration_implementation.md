# Vosk Offline Command Integration for AI Pet Robot

Last updated: 2026-03-25
Status: implementation-ready research + agent execution document
Scope: Android offline English command recognition for a small fixed command set

---

## 1. Goal

Add an **offline voice-command layer** to the Android pet brain using **Vosk** so the app can recognize a small set of English commands and convert them into internal events for the behavior system.

Initial command set:

- `wakeup` => start listening / focus mode
- `learn person` => trigger camera person-learning flow only
- `learn object` => trigger object-learning flow only
- `play random` => trigger random play behavior

This document is intentionally written so an AI coding agent can use it as direct execution context.

---

## 2. Why Vosk for this phase

Vosk is a practical fit for this phase because it is:

- offline
- open source
- available on Android
- usable with streaming recognition
- able to work with a limited vocabulary/grammar, which is exactly what this command-only phase needs

For mobile, Vosk’s **small** models are the relevant choice. The official model page says small models are ideal for limited tasks on mobile applications, are around **50 MB**, and typically need about **300 MB runtime memory**. That makes model size and lifecycle management important from the start.

---

## 3. Official Vosk references

These are the primary references the implementation should follow first:

### Vosk official docs

- Main site: https://alphacephei.com/vosk/
- Android page: https://alphacephei.com/vosk/android
- Models page: https://alphacephei.com/vosk/models
- Language model adaptation page: https://alphacephei.com/vosk/lm
- Adaptation guide: https://alphacephei.com/vosk/adaptation

### Official source repositories

- Vosk API repository: https://github.com/alphacep/vosk-api
- Android demo repository: https://github.com/alphacep/vosk-android-demo
- Demo activity reference: https://github.com/alphacep/vosk-android-demo/blob/master/app/src/main/java/org/vosk/demo/VoskActivity.java

These links should be copied into any Codex / Claude / agent prompt so sub-agents can inspect the official implementation paths directly.

---

## 4. Core product decision

For this project, **do not implement general-purpose full speech transcription UX**.

Instead, implement a **command recognizer** with a **restricted grammar / restricted expected phrases**.

That is the right abstraction because:

- the product currently needs a handful of pet commands, not free-form speech
- restricted command space improves practical recognition quality
- it maps cleanly into the event-driven brain architecture
- it is cheaper in latency, logic complexity, and testing scope than general speech UI

So the implementation target is:

**microphone audio -> Vosk recognizer -> recognized phrase -> command normalization -> domain event -> existing feature flow**

---

## 5. Required architectural boundaries

The implementation must stay aligned with the project’s existing architecture:

- offline-first
- event-driven
- modular
- Android brain remains the decision-maker
- audio recognition must not directly control unrelated app state

### Required separation

Create or extend a small command pipeline with boundaries like this:

1. `AudioCommandRecognizer`
   - owns Vosk model lifecycle
   - owns speech-service lifecycle
   - produces raw recognized text / command candidates

2. `CommandNormalizer`
   - converts recognized text into canonical app commands
   - handles simple aliases like `wake up` => `wakeup`

3. `VoiceCommandMapper`
   - maps canonical command into app/domain event

4. `VoiceCommandCoordinator`
   - decides whether command should be accepted in current state
   - prevents invalid overlaps such as starting person learning while object learning is already active

5. Existing feature handlers
   - brain / camera / learning subsystems perform the actual work

### Important non-goal

Do **not** let Vosk directly invoke camera-learning code or game code from inside the recognizer adapter.

The recognizer layer must emit events, not business actions.

---

## 6. Recommended command model

Use the following canonical commands internally:

- `VOICE_CMD_WAKEUP`
- `VOICE_CMD_LEARN_PERSON`
- `VOICE_CMD_LEARN_OBJECT`
- `VOICE_CMD_PLAY_RANDOM`

### Recommended spoken phrase list

Use these phrases in the initial restricted grammar and normalizer:

#### Wake command
- `wakeup`
- `wake up`

#### Learn person command
- `learn person`
- `learn face`

#### Learn object command
- `learn object`
- `learn item`

#### Play command
- `play random`
- `play`

### Recommendation

Start with the **smallest possible phrase list** first:

- `wakeup`
- `learn person`
- `learn object`
- `play random`

Only add aliases after end-to-end verification. If you add too many variants too early, you make debugging harder.

---

## 7. Event contract to add

Add a dedicated event contract instead of overloading generic audio events.

### Proposed event

`VOICE_COMMAND_DETECTED`

Suggested payload:

- `timestamp`
- `rawText`
- `normalizedText`
- `command`
- `confidenceLikeHint` (optional; Vosk result structures may not map to a clean confidence score in the same way as cloud APIs, so keep this optional)
- `source = VOSK`

### Optional supporting events

- `VOICE_COMMAND_LISTENING_STARTED`
- `VOICE_COMMAND_LISTENING_STOPPED`
- `VOICE_COMMAND_REJECTED`
- `VOICE_COMMAND_NOT_RECOGNIZED`

These make the debug UI and event timeline much easier to verify.

---

## 8. Recommended Android integration shape

The official Android demo shows the typical structure:

- request `RECORD_AUDIO`
- unpack/load the model with `StorageService.unpack(...)`
- create a `Recognizer(model, sampleRate)` or `Recognizer(model, sampleRate, grammarJson)`
- create `SpeechService(recognizer, sampleRate)`
- start listening with `speechService.startListening(listener)`

That is the correct baseline to follow.

### Project-specific recommendation

Wrap the Vosk-specific classes behind a project-owned interface so the rest of the app never depends directly on:

- `org.vosk.Model`
- `org.vosk.Recognizer`
- `org.vosk.android.SpeechService`
- `org.vosk.android.StorageService`

This protects the architecture and keeps replacement possible later.

---

## 9. Model strategy

### Use English small model first

For this feature, use a **small English model** suitable for Android/mobile experimentation.

Selection rules:

- prefer official English small model from Vosk model catalog
- keep the first version asset-based and local
- do not start with dynamic model download in the first batch
- do not start with multilingual support in the first batch

### Packaging recommendation

Phase 1 implementation should prefer:

- model bundled in app assets for a first vertical slice
- unpacked locally through the same pattern as the official Android demo

Later optimization can evaluate:

- downloadable model packs
- model selection by locale
- moving model outside base APK if size becomes unacceptable

---

## 10. Grammar strategy for command mode

The most important practical implementation choice is this:

**Use a restricted grammar / restricted command phrase list.**

The official Android demo includes a recognizer example that passes a JSON phrase list to the `Recognizer` constructor for a constrained recognition case. That is exactly the pattern to adapt for this command feature.

### Initial grammar recommendation

```json
[
  "wakeup",
  "wake up",
  "learn person",
  "learn object",
  "play random",
  "[unk]"
]
```

### Why include `[unk]`

Include an unknown token path so the recognizer can fail more safely instead of forcing every sound into one of the known commands.

### Important caution

Do not assume the grammar alone is enough. Still normalize recognized text before mapping to domain commands.

Example:

- `wake up` -> `wakeup`
- `learn person` -> `learn person`
- `learn object` -> `learn object`
- `play random` -> `play random`

---

## 11. Proposed implementation design

### 11.1 New interfaces

#### `AudioCommandRecognizer`
Responsibilities:
- initialize model
- start listening
- stop listening
- expose recognizer state
- emit raw results / command candidates

Suggested shape:

```kotlin
interface AudioCommandRecognizer {
    suspend fun initialize()
    fun startListening()
    fun stopListening()
    fun release()
}
```

#### `VoiceCommandSink`
Responsibilities:
- receive recognizer outputs
- keep Vosk adapter independent of app core

```kotlin
interface VoiceCommandSink {
    fun onPartialText(text: String)
    fun onFinalText(text: String)
    fun onCommandRejected(rawText: String)
    fun onRecognizerError(message: String)
}
```

### 11.2 Vosk adapter

Create an adapter such as:

- `VoskAudioCommandRecognizer`

Responsibilities:
- request/use already granted audio permission path
- load model once
- create recognizer with grammar JSON
- translate Vosk callbacks to project events/sink calls
- manage start/stop state safely

### 11.3 Normalization layer

Create:

- `VoiceCommandNormalizer`

Rules v1:
- lowercase
- trim
- collapse multiple spaces
- normalize `wake up` to `wakeup`
- only accept exact known canonical phrases in v1

Do not implement fuzzy matching in the first version.

### 11.4 Mapping layer

Create:

- `VoiceCommandMapper`

Mapping table:

- `wakeup` -> `VOICE_CMD_WAKEUP`
- `learn person` -> `VOICE_CMD_LEARN_PERSON`
- `learn object` -> `VOICE_CMD_LEARN_OBJECT`
- `play random` -> `VOICE_CMD_PLAY_RANDOM`

### 11.5 Coordinator / gatekeeper

Create:

- `VoiceCommandCoordinator`

Responsibilities:
- reject command if the app is not in a state where it should execute
- avoid duplicate command spam
- enforce cooldowns if needed
- publish domain events to the existing event bus

---

## 12. How each command should behave

### 12.1 `wakeup`
Behavior:
- transition pet into focused/listening/awake attention state
- publish event for brain/personality layer
- do not automatically start person learning or object learning

Suggested resulting internal action:
- `StartFocusMode`
- or equivalent behavior event already consistent with current brain design

### 12.2 `learn person`
Behavior:
- trigger camera-based learn person flow only
- should not also enter learn object flow
- if another teaching session is active, reject or stop according to app rules

Suggested resulting internal action:
- `StartTeachPersonFlow`

### 12.3 `learn object`
Behavior:
- trigger learn-object camera flow only
- should not affect person-learning flow

Suggested resulting internal action:
- `StartTeachObjectFlow`

### 12.4 `play random`
Behavior:
- trigger pet play behavior selection only
- should not force a specific game implementation if the product currently routes to a play-mode orchestrator

Suggested resulting internal action:
- `StartRandomPlayBehavior`

---

## 13. Lifecycle requirements

The Vosk integration must define strict lifecycle behavior.

### Initialize
- initialize only after audio permission is granted
- initialize model on background thread
- do not block UI thread during model unpack/load

### Start listening
- only when model is ready
- no duplicate start if already listening

### Stop listening
- stop speech service cleanly
- clear references as needed

### Release
- release service and model cleanly when feature owner is destroyed
- avoid leaking recognizer/model into long-lived UI objects

---

## 14. UI / UX recommendation for MVP

Do not make this feature invisible.

A hidden audio recognizer is hard to debug and easy to mistrust.

### Minimum visible debug affordances

Add at least:

- recognizer status: `idle / loading / ready / listening / error`
- last recognized raw text
- last mapped command
- last rejection reason

### Optional but useful

- manual start/stop listening button in debug mode
- waveform or mic activity indicator if already available from current audio layer

---

## 15. Error handling rules

The recognizer layer should fail clearly, not silently.

### Handle explicitly

- permission denied
- model unpack failure
- model missing/corrupt
- recognizer start failure
- microphone busy/unavailable
- unknown phrase recognized
- recognized phrase rejected by state gate

### Requirement

Every failure path must produce one of:

- visible debug state
- structured log
- event-bus event for diagnostics

---

## 16. Build-safe incremental execution plan

This should be implemented in **small vertical slices**.

### Batch A — Infrastructure slice
Goal:
- add Vosk dependency and adapter shell
- add model asset packaging path
- add recognizer state model
- no business command routing yet

Definition of done:
- app builds
- model loads successfully
- debug UI shows recognizer ready state

### Batch B — Listening slice
Goal:
- start/stop microphone listening through Vosk
- surface raw recognized text in debug UI/log

Definition of done:
- app builds
- speaking test phrase shows recognized raw text

### Batch C — Command slice
Goal:
- add grammar-restricted recognizer
- add normalization + mapping
- publish `VOICE_COMMAND_DETECTED`

Definition of done:
- each supported phrase maps to the correct internal command event
- unsupported phrase does not trigger supported command

### Batch D — Feature routing slice
Goal:
- route canonical command events into existing feature flows:
  - wake/focus
  - learn person
  - learn object
  - play random

Definition of done:
- each command visibly triggers the correct target flow
- no cross-trigger between person/object learning

### Batch E — Hardening slice
Goal:
- cooldowns
- duplicate prevention
- debug events
- lifecycle cleanup
- unit tests for normalizer/mapper

Definition of done:
- repeated speech does not spam invalid duplicate behavior
- tests cover normalizer and mapper

---

## 17. Recommended agent / sub-agent execution split

Use multiple agents, but keep one owner agent responsible for final architecture integrity.

### Main agent
Responsibilities:
- read AGENTS.md and architecture docs
- preserve module boundaries
- integrate all pieces
- run build and final verification

### Sub-agent 1 — Vosk research + dependency integration
Responsibilities:
- inspect official Vosk docs and Android demo
- add dependency/config/package path
- confirm model asset/unpack strategy

### Sub-agent 2 — Recognizer adapter
Responsibilities:
- implement `VoskAudioCommandRecognizer`
- manage `Model`, `Recognizer`, `SpeechService`, lifecycle
- expose raw text callbacks/state

### Sub-agent 3 — Command domain mapping
Responsibilities:
- implement normalizer
- implement mapper
- add tests for phrase-to-command mapping

### Sub-agent 4 — App flow routing
Responsibilities:
- connect command events into brain / camera / play flows
- ensure `learn person` and `learn object` are isolated correctly

### Sub-agent 5 — Debug / verification
Responsibilities:
- add debug state surface
- confirm event timeline/log visibility
- run verification scenarios

### Rule

Sub-agents must not independently redesign architecture. They implement only their slice.

---

## 18. Suggested files to read before implementation

Minimum required reading for agent execution:

1. `AGENTS.md`
2. `project_manifest.md`
3. `development_roadmap.md`
4. `02_android_pet_brain_architecture.md`
5. `08_audio_interaction_architecture.md`
6. this document

Then inspect official Vosk references listed in section 3.

---

## 19. Verification plan

### Manual verification matrix

#### Case 1 — model load success
- launch app
- grant mic permission
- confirm recognizer reaches `ready`

#### Case 2 — wakeup command
- start listening
- say `wakeup`
- verify command event is emitted
- verify app enters focus/awake behavior only

#### Case 3 — learn person command
- say `learn person`
- verify person-learning flow starts
- verify object-learning flow does not start

#### Case 4 — learn object command
- say `learn object`
- verify object-learning flow starts
- verify person-learning flow does not start

#### Case 5 — play random command
- say `play random`
- verify random play behavior starts

#### Case 6 — unsupported speech
- say unrelated sentence
- verify no supported command is executed
- verify debug view shows not recognized or rejected state

#### Case 7 — duplicate speech spam
- repeat one command rapidly
- verify cooldown or de-duplication policy prevents repeated unwanted triggers

### Unit test targets

At minimum test:

- normalizer exact-match behavior
- alias normalization behavior
- mapper behavior
- rejection of unsupported phrases

---

## 20. Risks and design cautions

### Risk 1 — APK/app size growth
Vosk model packaging can noticeably increase app size. Keep first implementation simple, but track this explicitly.

### Risk 2 — Runtime memory pressure
Even small models are not free. Keep model singleton/lifecycle disciplined.

### Risk 3 — Over-accepting near-miss phrases
A small command grammar helps, but business logic should still verify canonical matches.

### Risk 4 — State conflicts
If the app already has a teach flow or play flow active, blindly triggering new flows can break UX. Use coordinator gating.

### Risk 5 — Hidden failures
If the feature runs silently in background with no status/debug feedback, troubleshooting will become painful.

---

## 21. Explicit non-goals for first implementation

Do not include these in the first delivery:

- cloud STT
- LLM parsing of voice commands
- multilingual support
- dynamic model download
- fuzzy semantic matching
- always-on wake word detection separate from Vosk command listening
- speaker identification
- conversational voice assistant behavior

---

## 22. Implementation recommendation summary

### Recommended first delivery

Build a **restricted-grammar Vosk command recognizer** for Android that:

- loads an English small model offline
- listens only in a controlled app-owned listening mode
- recognizes this minimal command set:
  - `wakeup`
  - `learn person`
  - `learn object`
  - `play random`
- normalizes text into canonical commands
- emits domain events
- routes those events into the existing brain/camera/play flows
- exposes debug state for verification

This is the right first slice because it is small, testable, offline, and aligned with the current pet-brain architecture.

---

## 23. Ready-to-use agent prompt seed

Use this as the starting point for Codex / Claude / sub-agent execution:

```text
Read and follow AGENTS.md first.

Goal:
Implement an offline Android voice-command layer using Vosk for a fixed English command set.

Scope:
Implement only the smallest complete vertical slice for Vosk-based command recognition and routing.
Do not redesign unrelated architecture.
Do not add cloud STT, LLM parsing, multilingual support, or dynamic model download.

Commands to support:
- wakeup => start listening / focus mode
- learn person => trigger camera learn-person flow only
- learn object => trigger learn-object flow only
- play random => trigger random play behavior

Required architecture:
- Keep offline-first, event-driven, modular boundaries.
- Add a recognizer adapter layer, command normalizer, mapper, and event routing.
- Do not let the recognizer directly call camera or play business logic.
- Emit domain events and let the brain/app flow handle the action.

Read first:
- AGENTS.md
- project_manifest.md
- development_roadmap.md
- 02_android_pet_brain_architecture.md
- 08_audio_interaction_architecture.md
- vosk_offline_command_integration_implementation.md

Official Vosk references:
- https://alphacephei.com/vosk/
- https://alphacephei.com/vosk/android
- https://alphacephei.com/vosk/models
- https://github.com/alphacep/vosk-api
- https://github.com/alphacep/vosk-android-demo
- https://github.com/alphacep/vosk-android-demo/blob/master/app/src/main/java/org/vosk/demo/VoskActivity.java

Implementation requirements:
1. Add Vosk dependency/integration and model asset loading.
2. Implement a project-owned Vosk recognizer adapter.
3. Use a restricted grammar for these phrases only:
   ["wakeup", "wake up", "learn person", "learn object", "play random", "[unk]"]
4. Normalize recognized text into canonical commands.
5. Emit a VOICE_COMMAND_DETECTED event.
6. Route commands into the correct existing feature flows with isolation between learn-person and learn-object.
7. Add visible debug status and last recognized command.
8. Add unit tests for normalizer and mapper.

Definition of done:
- App builds successfully.
- Saying each supported command triggers the correct flow.
- Unsupported phrases do not trigger supported commands.
- Debug UI/logs show recognizer state and last recognized command.
- No unrelated architecture changes.

Build and verify:
- ./gradlew assembleDebug
- ./gradlew test

Required output:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

---

## 24. Notes for future evolution

After this command MVP is stable, the next sensible upgrades are:

1. add explicit start/stop listening policy tied to pet state
2. improve phrase aliases carefully
3. add cooldown and anti-repeat behavior
4. evaluate wake-word layer before command listening
5. only then consider broader speech interaction

Do not skip straight from this MVP to general conversational voice.
