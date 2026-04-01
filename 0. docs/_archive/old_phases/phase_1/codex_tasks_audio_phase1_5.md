# Codex Tasks — Audio Interaction Extension (Phase 1.5)

Version: v1
Purpose: Bộ task đầy đủ để thêm **Audio Interaction** vào AI Pet Robot theo `08_audio_interaction_architecture.md`, giữ đúng hướng **offline-first**, **event-driven**, **modular**, và **build-safe**.

Rule bắt buộc:

* Chỉ giao **1 task mỗi lần**.
* Không mock logic production.
* Không để hàm trống / TODO / `throw NotImplementedException`.
* Task xong phải **build được**.
* Task xong phải có **cách verify thấy được**.
* Không kéo cloud/LLM/conversation đầy đủ vào các task audio MVP.
* Không redesign lại kiến trúc hiện có nếu docs đã định nghĩa rõ.

---

## 0. Standard Prompt Wrapper

Dùng wrapper này cho mọi task dưới đây:

```md
Read and follow `AGENTS.md` first.
Then read only the documents listed in "Read first" below.
Implement only this task.
Do not add mock production logic.
Do not leave empty methods, TODOs, or placeholder business logic.
Run the required build command before finishing.
Report exactly:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

---

## 1. Scope of This Audio Task Set

Mục tiêu của Phase 1.5 Audio là thêm một lớp **nghe và phản ứng âm thanh** để robot “có hồn” hơn trước khi đi vào AI conversation.

Bao gồm:

* micro permission + audio capture foundation
* energy/VAD-light detection
* audio events
* debug visibility cho audio
* pre-recorded response playback
* arbitration / cooldown / self-trigger suppression
* behavior integration cho sound-reactive pet
* keyword/wake-word extension path
* command-reactive foundation mức tối thiểu, **không có LLM**

Không bao gồm trong bộ task này:

* hội thoại đa lượt
* TTS/LLM orchestration đầy đủ
* cloud voice stack
* memory-aware conversation
* full agentic AI

---

# Batch 15 — Audio Foundation and Permission Flow

## C91

```md
Task ID: C91
Title: Add microphone permission flow and Audio Debug screen shell

Goal:
Add `RECORD_AUDIO` permission handling and create an Audio Debug screen that opens without crashing and clearly shows granted/denied states.

Read first:
- docs/project_manifest.md
- docs/development_roadmap.md
- docs/08_audio_interaction_architecture.md

Files likely touched:
- android-brain/app/src/main/AndroidManifest.xml
- android-brain/app/src/main/java/.../audio/AudioDebugScreen.kt
- android-brain/app/src/main/java/.../permissions/*
- android-brain/app/src/main/java/.../navigation/*

Implementation steps:
1. Add microphone permission to the manifest.
2. Add runtime permission request handling for audio.
3. Create an Audio Debug screen with clear granted / denied / requesting UI states.
4. Add navigation to the screen from Debug or Home.

Definition of Done:
- Microphone permission can be requested from the app.
- Granted and denied states are handled without crashing.
- Audio Debug screen opens successfully.
- Build succeeds.

How to verify:
- Open Audio Debug screen.
- Deny microphone permission and confirm a stable denial UI appears.
- Grant permission and confirm the granted state appears.

Build command:
- ./gradlew assembleDebug
```

## C92

```md
Task ID: C92
Title: Define audio event types and payload contracts

Goal:
Extend the event system with audio-related event types and typed payload contracts for Phase 1.5.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../events/EventType.kt
- android-brain/brain/src/main/java/.../events/audio/*
- android-brain/brain/src/main/java/.../events/EventEnvelope.kt

Implementation steps:
1. Add audio event types such as SOUND_ENERGY_CHANGED, SOUND_DETECTED, VOICE_ACTIVITY_STARTED, VOICE_ACTIVITY_ENDED, AUDIO_RESPONSE_REQUESTED, AUDIO_RESPONSE_STARTED, AUDIO_RESPONSE_COMPLETED, AUDIO_RESPONSE_SKIPPED.
2. Define payload models or helpers for audio event JSON generation.
3. Keep event contracts compatible with the existing event system and viewer.

Definition of Done:
- Audio event types compile and are usable from app, brain, and perception layers.
- Payload contracts exist for real audio flows.
- Build succeeds.

How to verify:
- Reference the new event types from app code and build successfully.

Build command:
- ./gradlew assembleDebug
```

## C93

```md
Task ID: C93
Title: Create audio perception interfaces and runtime state models

Goal:
Define the core audio perception contracts for frame source, detector state, and runtime audio metrics.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/02_android_pet_brain_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../audio/AudioFrameSource.kt
- android-brain/perception/src/main/java/.../audio/model/*
- android-brain/perception/src/main/java/.../audio/VoiceActivityDetector.kt

Implementation steps:
1. Define interfaces for audio frame source and detector plugins.
2. Add models for audio frame metadata, energy metrics, and VAD state.
3. Keep APIs simple and compatible with Stage A and future Stage B/C extensions.

Definition of Done:
- Audio perception interfaces compile.
- Runtime audio models compile and are usable.
- Build succeeds.

How to verify:
- Reference the contracts from app or test wiring and run a build.

Build command:
- ./gradlew assembleDebug
```

## C94

```md
Task ID: C94
Title: Implement AudioRecord-based audio frame source

Goal:
Implement a real AudioRecord-based frame source that captures PCM audio frames from the microphone.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../audio/AudioRecordFrameSource.kt
- android-brain/perception/src/main/java/.../audio/*
- android-brain/app/src/main/java/.../audio/AudioDebugScreen.kt

Implementation steps:
1. Create an AudioRecord-based capture pipeline using mono PCM 16-bit input.
2. Configure a practical sample rate and buffer sizing strategy.
3. Expose start/stop lifecycle methods and a frame callback or Flow.
4. Wire it to the Audio Debug screen behind the granted permission state.

Definition of Done:
- The app captures real audio frames from the microphone.
- Capture can start and stop without crashing.
- Build succeeds.

How to verify:
- Open Audio Debug screen with permission granted.
- Start listening and confirm capture status changes to active.
- Stop listening and confirm it stops cleanly.

Build command:
- ./gradlew assembleDebug
```

## C95

```md
Task ID: C95
Title: Add real-time audio capture metrics on Audio Debug screen

Goal:
Display real capture metrics such as sample rate, frame size, and capture active state on the Audio Debug screen.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../audio/AudioDebugScreen.kt
- android-brain/perception/src/main/java/.../audio/*

Implementation steps:
1. Surface the frame source runtime state to the UI.
2. Show sample rate, bytes/frame or samples/frame, and capture active state.
3. Keep the UI stable when capture is not running.

Definition of Done:
- Audio Debug screen shows real capture metrics.
- Metrics update while capture runs.
- Build succeeds.

How to verify:
- Start capture and confirm metrics appear and update.
- Stop capture and confirm the UI returns to a stable idle state.

Build command:
- ./gradlew assembleDebug
```

## C96

```md
Task ID: C96
Title: Emit AUDIO_CAPTURE_STARTED and AUDIO_CAPTURE_STOPPED events

Goal:
Publish real lifecycle events when microphone capture starts and stops.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../audio/*
- android-brain/brain/src/main/java/.../events/*
- android-brain/app/src/main/java/.../audio/AudioDebugScreen.kt

Implementation steps:
1. Add start/stop event emission from the audio capture pipeline.
2. Include useful metadata such as sample rate and frame size where appropriate.
3. Keep duplicate event emission controlled.

Definition of Done:
- Event Viewer shows audio capture lifecycle events.
- Start/stop capture behavior remains stable.
- Build succeeds.

How to verify:
- Start and stop microphone capture.
- Open Event Viewer and confirm the expected events appear.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 16 — Energy Detection and VAD-Light

## C97

```md
Task ID: C97
Title: Implement audio energy estimator for PCM frames

Goal:
Implement a real energy estimator that calculates RMS and peak-like metrics from captured PCM audio frames.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../audio/EnergyEstimator.kt
- android-brain/perception/src/main/java/.../audio/model/*

Implementation steps:
1. Parse PCM frame samples from the frame source.
2. Calculate RMS and peak or equivalent practical energy metrics.
3. Return stable metrics suitable for UI, events, and later VAD-light logic.

Definition of Done:
- Energy estimator returns real audio energy values from live frames.
- Build succeeds.

How to verify:
- Run microphone capture and inspect logs or UI values changing with quiet vs loud sound.

Build command:
- ./gradlew assembleDebug
```

## C98

```md
Task ID: C98
Title: Show live audio energy bar and numeric values on Audio Debug screen

Goal:
Display real-time audio energy metrics visually and numerically on the Audio Debug screen.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../audio/AudioDebugScreen.kt
- android-brain/app/src/main/java/.../audio/AudioEnergyUi.kt
- android-brain/perception/src/main/java/.../audio/*

Implementation steps:
1. Bind live energy metrics into the screen state.
2. Render an energy bar and numeric values such as RMS and peak.
3. Keep the UI smooth and stable under continuous updates.

Definition of Done:
- Audio Debug screen shows real live energy.
- Energy visibly changes with environmental sound.
- Build succeeds.

How to verify:
- Start capture, speak or clap near the mic, and observe the energy bar and values change.

Build command:
- ./gradlew assembleDebug
```

## C99

```md
Task ID: C99
Title: Publish SOUND_ENERGY_CHANGED events at a controlled rate

Goal:
Publish real SOUND_ENERGY_CHANGED events from live energy metrics at a controlled rate suitable for debug and downstream behavior logic.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../audio/*
- android-brain/brain/src/main/java/.../events/*

Implementation steps:
1. Rate-limit energy event publication to a practical interval.
2. Include smoothed and raw energy values in payload.
3. Keep event spam under control while preserving useful visibility.

Definition of Done:
- Event Viewer shows SOUND_ENERGY_CHANGED events while listening.
- Rate control is in place.
- Build succeeds.

How to verify:
- Start capture and inspect Event Viewer for SOUND_ENERGY_CHANGED entries.

Build command:
- ./gradlew assembleDebug
```

## C100

```md
Task ID: C100
Title: Implement VAD-light state machine with smoothing and hysteresis

Goal:
Implement a real VAD-light state machine using energy smoothing, hysteresis thresholds, and hangover timing.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../audio/VadLightStateMachine.kt
- android-brain/perception/src/main/java/.../audio/model/*

Implementation steps:
1. Add smoothing over incoming energy values.
2. Implement threshold-based transitions such as SILENT -> SOUND_PRESENT -> VOICE_LIKELY.
3. Add hysteresis and hangover timing to avoid rapid flicker.
4. Expose the current VAD-light state to the pipeline.

Definition of Done:
- VAD-light produces stable state transitions from live audio.
- Build succeeds.

How to verify:
- Start listening, alternate between silence and speech/noise, and inspect the changing VAD state.

Build command:
- ./gradlew assembleDebug
```

## C101

```md
Task ID: C101
Title: Publish SOUND_DETECTED and voice activity boundary events

Goal:
Publish SOUND_DETECTED, VOICE_ACTIVITY_STARTED, and VOICE_ACTIVITY_ENDED events from the VAD-light pipeline.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../audio/*
- android-brain/brain/src/main/java/.../events/*

Implementation steps:
1. Emit SOUND_DETECTED when energy crosses the practical detection threshold.
2. Emit VOICE_ACTIVITY_STARTED on entering a voice-like active state.
3. Emit VOICE_ACTIVITY_ENDED on leaving that state after hangover.
4. Keep duplicate spam controlled.

Definition of Done:
- Event Viewer shows sound and voice activity boundary events from real microphone input.
- Build succeeds.

How to verify:
- Start capture, speak near the mic, then go silent.
- Confirm start and end events appear in Event Viewer.

Build command:
- ./gradlew assembleDebug
```

## C102

```md
Task ID: C102
Title: Show VAD-light state and last sound event in Audio Debug UI

Goal:
Display the current VAD-light state and last sound-related event on the Audio Debug screen.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../audio/AudioDebugScreen.kt
- android-brain/perception/src/main/java/.../audio/*

Implementation steps:
1. Surface the current VAD-light state to the UI.
2. Show the last sound-related event and timestamp if available.
3. Keep the UI readable under continuous updates.

Definition of Done:
- Audio Debug screen shows real current VAD-light state and last sound event.
- Build succeeds.

How to verify:
- Start listening and produce sound/speech.
- Confirm the state and last-event UI updates accordingly.

Build command:
- ./gradlew assembleDebug
```

## C103

```md
Task ID: C103
Title: Persist audio perception events in the existing event store flow

Goal:
Ensure the new audio perception events are saved through the real Room-backed event persistence flow.

Read first:
- docs/07_robot_memory_system.md
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../events/*
- android-brain/memory/src/main/java/.../events/*
- android-brain/app/src/main/java/.../audio/*

Implementation steps:
1. Verify audio events are going through the normal EventBus persistence path.
2. Fix any event serialization or payload issues blocking persistence.
3. Keep existing event viewer behavior unchanged.

Definition of Done:
- Audio events survive app restarts through the existing event store.
- Build succeeds.

How to verify:
- Generate audio events, force close the app, reopen it, and confirm the events remain in Event Viewer.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 17 — Pre-recorded Audio Response Foundation

## C104

```md
Task ID: C104
Title: Add audio response asset structure and clip manifest

Goal:
Create the initial audio response asset structure and a real manifest/config describing available pet response clips.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/project_manifest.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/assets/audio/*
- android-brain/app/src/main/assets/audio/audio_clips_manifest.json
- android-brain/app/src/main/java/.../audio/response/model/*

Implementation steps:
1. Create asset folders or a naming structure for clip categories such as greeting, curious, happy, sleepy, ack, surprised, warning.
2. Add a manifest or config file describing clip ids, category, filename, and optional metadata.
3. Add models for manifest parsing.

Definition of Done:
- A real audio asset structure exists.
- Manifest parsing models compile.
- Build succeeds.

How to verify:
- Build the app and confirm the asset files package correctly.

Build command:
- ./gradlew assembleDebug
```

## C105

```md
Task ID: C105
Title: Define audio response domain models and playback contracts

Goal:
Define the production-ready contracts for requesting and reporting audio response playback.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../audio/response/*
- android-brain/app/src/main/java/.../audio/response/model/*

Implementation steps:
1. Define models for audio response category, request, playback state, and skip reason.
2. Keep the contracts aligned with AUDIO_RESPONSE_REQUESTED / STARTED / COMPLETED / SKIPPED events.
3. Make the models reusable from behavior and UI layers.

Definition of Done:
- Audio response models compile and are usable from behavior and playback layers.
- Build succeeds.

How to verify:
- Reference the models from app code and run a build.

Build command:
- ./gradlew assembleDebug
```

## C106

```md
Task ID: C106
Title: Implement SoundPool-based pet response player

Goal:
Implement a real SoundPool-based playback engine for short pet response clips.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../audio/response/SoundPoolPetResponsePlayer.kt
- android-brain/app/src/main/java/.../audio/response/*

Implementation steps:
1. Load clip assets from the manifest into a SoundPool-based player.
2. Support playing a clip by clip id.
3. Expose load status and playback result.
4. Keep the implementation focused on short low-latency clips only.

Definition of Done:
- The app can play a real packaged pet clip through SoundPool.
- Build succeeds.

How to verify:
- Add a temporary debug trigger to play one known clip and confirm audio is heard.

Build command:
- ./gradlew assembleDebug
```

## C107

```md
Task ID: C107
Title: Add Audio Response test panel with manual clip playback

Goal:
Create a debug test panel that allows manual playback of packaged audio response clips.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../audio/AudioDebugScreen.kt
- android-brain/app/src/main/java/.../audio/response/AudioResponseTestPanel.kt

Implementation steps:
1. Add a panel listing available response categories or sample clips.
2. Allow manual playback from the UI.
3. Surface playback success/failure visibly.

Definition of Done:
- User can manually play packaged pet clips from the app.
- Build succeeds.

How to verify:
- Open Audio Debug screen and manually play at least two different clips.

Build command:
- ./gradlew assembleDebug
```

## C108

```md
Task ID: C108
Title: Emit playback lifecycle events for audio responses

Goal:
Publish AUDIO_RESPONSE_STARTED, AUDIO_RESPONSE_COMPLETED, and AUDIO_RESPONSE_SKIPPED events from real playback results.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../audio/response/*
- android-brain/brain/src/main/java/.../events/*

Implementation steps:
1. Emit AUDIO_RESPONSE_STARTED when playback begins.
2. Emit AUDIO_RESPONSE_COMPLETED when playback finishes.
3. Emit AUDIO_RESPONSE_SKIPPED for valid skip conditions such as missing clip or low-priority rejection.

Definition of Done:
- Event Viewer shows real playback lifecycle events.
- Build succeeds.

How to verify:
- Manually play a clip and inspect Event Viewer.
- Trigger one known skip condition if possible and inspect the skip event.

Build command:
- ./gradlew assembleDebug
```

## C109

```md
Task ID: C109
Title: Add clip category resolution and random selection within category

Goal:
Resolve response requests by category and randomly choose a clip within that category.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../audio/response/*
- android-brain/app/src/main/assets/audio/audio_clips_manifest.json

Implementation steps:
1. Group clips by category from the manifest.
2. Implement category-based clip selection.
3. Add stable fallback behavior when a category has no available clip.

Definition of Done:
- The playback engine can play a random clip from a response category.
- Build succeeds.

How to verify:
- Trigger playback for one category multiple times and confirm more than one clip can be chosen when available.

Build command:
- ./gradlew assembleDebug
```

## C110

```md
Task ID: C110
Title: Add playback cooldown and overlap prevention

Goal:
Prevent pet voice clip spam by adding cooldown and one-at-a-time overlap protection.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../audio/response/*

Implementation steps:
1. Add a cooldown rule for repeated playback categories or requests.
2. Prevent overlapping pet voice playback.
3. Return a real skipped result when playback is blocked by cooldown or overlap policy.

Definition of Done:
- Repeated rapid requests do not spam overlapping clips.
- Skip conditions are handled cleanly.
- Build succeeds.

How to verify:
- Trigger the same manual playback repeatedly and confirm cooldown/overlap behavior works.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 18 — Audio Request Orchestration and Behavior Integration

## C111

```md
Task ID: C111
Title: Create audio response request dispatcher wired to EventBus

Goal:
Create a dispatcher that listens for AUDIO_RESPONSE_REQUESTED events and routes them to the playback engine.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../audio/AudioResponseDispatcher.kt
- android-brain/app/src/main/java/.../audio/response/*
- android-brain/brain/src/main/java/.../events/*

Implementation steps:
1. Subscribe to AUDIO_RESPONSE_REQUESTED events from the EventBus.
2. Convert request payloads into playback calls.
3. Keep the dispatcher isolated from behavior decision logic.

Definition of Done:
- AUDIO_RESPONSE_REQUESTED leads to real playback through the dispatcher.
- Build succeeds.

How to verify:
- Emit a manual AUDIO_RESPONSE_REQUESTED test path and confirm a clip plays.

Build command:
- ./gradlew assembleDebug
```

## C112

```md
Task ID: C112
Title: Add self-trigger suppression while pet audio is playing

Goal:
Reduce false microphone triggers from the robot hearing its own playback audio.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../audio/*
- android-brain/app/src/main/java/.../audio/response/*
- android-brain/brain/src/main/java/.../audio/*

Implementation steps:
1. Expose playback-active state from the audio response layer.
2. Gate or suppress sound detection while pet audio is actively playing.
3. Keep suppression simple and observable in debug state.

Definition of Done:
- Pet playback does not immediately retrigger the sound detector in obvious cases.
- Build succeeds.

How to verify:
- Start listening, trigger playback, and confirm the detector does not flood new sound events from the robot's own clip.

Build command:
- ./gradlew assembleDebug
```

## C113

```md
Task ID: C113
Title: Define sound stimulus rules in the brain layer

Goal:
Add real behavior-layer rules mapping sound stimuli to pet response requests and brain reactions.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../logic/audio/*
- android-brain/brain/src/main/java/.../state/*
- android-brain/brain/src/main/java/.../audio/*

Implementation steps:
1. Subscribe brain logic to sound-related events.
2. Define a small real ruleset such as loud sound -> surprised/curious and voice activity -> attentive/ack request.
3. Keep the rules rate-controlled and consistent with current brain state transitions.

Definition of Done:
- Real sound events cause behavior-layer reactions.
- Build succeeds.

How to verify:
- Produce sound/voice input and inspect changed brain state or emitted audio response requests in Event Viewer.

Build command:
- ./gradlew assembleDebug
```

## C114

```md
Task ID: C114
Title: Add rule loud sound triggers surprised response clip

Goal:
When a sufficiently strong sound is detected, request a surprised pet response clip and a suitable reaction state.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../logic/audio/*
- android-brain/brain/src/main/java/.../state/*

Implementation steps:
1. Define a practical loud-sound condition from current audio metrics/events.
2. Emit AUDIO_RESPONSE_REQUESTED for a surprised category.
3. Transition the brain to a sensible temporary reaction state such as CURIOUS.

Definition of Done:
- Loud sound leads to a surprised clip request and visible state reaction.
- Build succeeds.

How to verify:
- Produce a loud clap or tap near the microphone and inspect Event Viewer plus avatar state.

Build command:
- ./gradlew assembleDebug
```

## C115

```md
Task ID: C115
Title: Add rule voice activity triggers acknowledgment response

Goal:
When voice-like activity is detected, request a short acknowledgment clip from the pet.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../logic/audio/*

Implementation steps:
1. Listen for VOICE_ACTIVITY_STARTED or a comparable voice-like event.
2. Emit AUDIO_RESPONSE_REQUESTED for acknowledgment or curious response category.
3. Apply practical spam protection so repeated speech does not flood responses.

Definition of Done:
- Speaking near the robot can trigger a short acknowledgment response.
- Build succeeds.

How to verify:
- Start listening and speak several short phrases near the microphone.
- Confirm occasional acknowledgment responses happen without excessive spam.

Build command:
- ./gradlew assembleDebug
```

## C116

```md
Task ID: C116
Title: Sync avatar reactions with audio response playback

Goal:
Drive avatar expression changes from real audio response playback lifecycle events.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../home/HomeScreen.kt
- android-brain/ui-avatar/src/main/java/.../*
- android-brain/brain/src/main/java/.../logic/audio/*

Implementation steps:
1. Listen for audio response playback events.
2. Map playback categories or lifecycle states to temporary avatar expression changes.
3. Restore safely after playback completes.

Definition of Done:
- Avatar visibly reacts during real pet audio playback.
- Build succeeds.

How to verify:
- Trigger a few different response categories and observe the avatar during playback.

Build command:
- ./gradlew assembleDebug
```

## C117

```md
Task ID: C117
Title: Add audio metrics and last clip info to debug overlay

Goal:
Show the most useful audio runtime state on the shared debug overlay or debug screens.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../debug/DebugOverlay.kt
- android-brain/app/src/main/java/.../audio/*
- android-brain/app/src/main/java/.../audio/response/*

Implementation steps:
1. Surface current energy/VAD state, last sound event, and last played clip/category.
2. Add the information to the existing debug overlay or clearly related debug UI.
3. Keep the display compact and stable.

Definition of Done:
- Debug UI shows useful real-time audio state and playback state.
- Build succeeds.

How to verify:
- Start listening and trigger both sound detection and playback.
- Confirm debug UI updates accordingly.

Build command:
- ./gradlew assembleDebug
```

## C118

```md
Task ID: C118
Title: Add audio-enabled no-stimulus reset integration

Goal:
Treat real audio stimuli as meaningful stimuli for the existing no-stimulus / sleepy behavior flow.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../logic/*
- android-brain/brain/src/main/java/.../memory/*

Implementation steps:
1. Feed real audio stimuli into the existing last-stimulus tracking path.
2. Ensure meaningful sound or voice activity can wake the pet or prevent it from sleeping.
3. Keep the definition of meaningful stimulus reasonable and not too noisy.

Definition of Done:
- Real audio stimuli affect the sleepy/no-stimulus loop.
- Build succeeds.

How to verify:
- Let the pet approach SLEEPY, then produce real voice or strong sound and confirm it wakes or resets the inactivity timer.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 19 — Keyword / Wake-Word Extension (Offline, Pre-AI)

## C119

```md
Task ID: C119
Title: Define keyword spotting abstraction and event mapping

Goal:
Create the extension-point contracts for keyword spotting without yet committing the app to a full conversational stack.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../audio/keyword/KeywordSpotter.kt
- android-brain/perception/src/main/java/.../audio/keyword/model/*
- android-brain/brain/src/main/java/.../events/*

Implementation steps:
1. Define interfaces and models for keyword spotting results.
2. Map the concept into WAKE_WORD_DETECTED and KEYWORD_DETECTED events.
3. Keep the abstraction usable by future Porcupine / TFLite / other adapters.

Definition of Done:
- Keyword spotting contracts compile and are ready for a real adapter.
- Build succeeds.

How to verify:
- Reference the abstraction from the app and build successfully.

Build command:
- ./gradlew assembleDebug
```

## C120

```md
Task ID: C120
Title: Add keyword spotting provider configuration and debug controls

Goal:
Add a real configuration path for selecting or enabling a keyword spotting provider and surface it in debug UI.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/core-common/src/main/java/.../config/*
- android-brain/app/src/main/java/.../audio/AudioDebugScreen.kt

Implementation steps:
1. Add config for keyword spotting enabled/disabled and provider selection.
2. Surface the current config in Audio Debug UI.
3. Keep behavior stable when keyword spotting is disabled.

Definition of Done:
- Keyword spotting config exists and is visible in debug UI.
- Build succeeds.

How to verify:
- Open Audio Debug screen and confirm the current keyword spotting config is shown.

Build command:
- ./gradlew assembleDebug
```

## C121

```md
Task ID: C121
Title: Integrate first real keyword spotting adapter

Goal:
Integrate one real keyword spotting adapter behind the new abstraction.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../audio/keyword/*
- android-brain/perception/build.gradle.kts
- android-brain/app/src/main/java/.../audio/*

Implementation steps:
1. Add the chosen real keyword spotting dependency or adapter.
2. Wire it behind the KeywordSpotter abstraction.
3. Connect it to the microphone pipeline without breaking existing energy/VAD behavior.

Definition of Done:
- The app can detect at least one real keyword or wake word through the chosen adapter.
- Build succeeds.

How to verify:
- Run the app with the adapter enabled and trigger the configured keyword.

Build command:
- ./gradlew assembleDebug
```

## C122

```md
Task ID: C122
Title: Publish WAKE_WORD_DETECTED and KEYWORD_DETECTED from the real adapter

Goal:
Emit real keyword-related events from the integrated keyword spotting adapter.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../audio/keyword/*
- android-brain/brain/src/main/java/.../events/*

Implementation steps:
1. Publish WAKE_WORD_DETECTED or KEYWORD_DETECTED based on adapter output.
2. Include useful metadata such as keyword id or confidence when available.
3. Keep duplicate triggering controlled.

Definition of Done:
- Event Viewer shows real keyword-related events.
- Build succeeds.

How to verify:
- Trigger the configured keyword and inspect Event Viewer.

Build command:
- ./gradlew assembleDebug
```

## C123

```md
Task ID: C123
Title: Add behavior rule wake word triggers attentive acknowledgment

Goal:
When a wake word or key keyword is detected, make the pet react attentively with a short acknowledgment response.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../logic/audio/*
- android-brain/brain/src/main/java/.../state/*

Implementation steps:
1. Listen for WAKE_WORD_DETECTED or KEYWORD_DETECTED.
2. Transition the pet into an attentive/curious state.
3. Emit an acknowledgment-style AUDIO_RESPONSE_REQUESTED.

Definition of Done:
- Wake word detection causes a visible attentive reaction and acknowledgment response.
- Build succeeds.

How to verify:
- Trigger the configured wake word and observe Event Viewer, avatar state, and playback result.

Build command:
- ./gradlew assembleDebug
```

## C124

```md
Task ID: C124
Title: Add command-reactive foundation using keyword-to-intent mapping

Goal:
Create a minimal offline command-reactive layer by mapping detected keywords to structured local intent events.

Read first:
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../audio/intent/*
- android-brain/brain/src/main/java/.../events/*
- android-brain/perception/src/main/java/.../audio/keyword/*

Implementation steps:
1. Define a minimal local audio intent enum such as ATTENTION, GREET, QUIET, WAKE.
2. Map known keywords to those intents locally without LLM.
3. Publish a structured local intent event from the keyword layer.

Definition of Done:
- At least one keyword can produce a structured local intent event.
- Build succeeds.

How to verify:
- Trigger a configured keyword and inspect Event Viewer for the mapped local audio intent event.

Build command:
- ./gradlew assembleDebug
```

## C125

```md
Task ID: C125
Title: Add end-to-end audio MVP smoke test pass and fix blocking issues

Goal:
Run a full manual smoke test of the Phase 1.5 Audio MVP and fix any build-safe blocking issues found.

Read first:
- docs/project_manifest.md
- docs/08_audio_interaction_architecture.md
- docs/development_roadmap.md

Files likely touched:
- Any audio-related files needed for small blocking fixes only

Implementation steps:
1. Verify microphone permission, AudioRecord capture, energy metrics, VAD-light events, audio response playback, cooldown, self-trigger suppression, sound-driven behavior, and keyword flow if implemented.
2. Fix only blocking issues discovered during the smoke test.
3. Keep changes localized and avoid speculative refactors.

Definition of Done:
- Audio MVP smoke test passes end-to-end.
- `./gradlew assembleDebug` succeeds.
- No blocking crashes remain in the covered audio flow.

How to verify:
- Manual smoke test checklist:
  - microphone permission request works
  - Audio Debug screen opens cleanly
  - audio capture starts/stops
  - live energy metrics update
  - VAD-light state changes with silence vs sound
  - sound events appear in Event Viewer
  - manual clip playback works
  - playback lifecycle events appear
  - cooldown / overlap prevention works
  - self-trigger suppression reduces obvious false triggers
  - loud sound or voice activity can trigger a pet response
  - avatar reacts during playback
  - audio stimuli affect no-stimulus / sleepy flow
  - keyword event flow works if adapter was implemented

Build command:
- ./gradlew assembleDebug
```

---

# Recommended Execution Order

Run in this order unless a specific task depends on already-created code:

1. C91 → C96
2. C97 → C103
3. C104 → C110
4. C111 → C118
5. C119 → C124
6. C125

---

# Best Practical Starting Slice

Nếu muốn có một vertical slice audio rất sớm, chạy theo chuỗi này:

* C91
* C92
* C94
* C95
* C97
* C98
* C100
* C101
* C104
* C106
* C107
* C108
* C111
* C113
* C115

Chuỗi này sẽ cho bạn:

* micro permission + audio capture
* live sound metrics
* VAD-light events
* manual playback của pet clips
* behavior-driven acknowledgment cơ bản

---

# Codex Task Final Recommendation

Task đầu tiên nên là **C91**.
Sau khi C91 pass build, tiếp tục **C92**, rồi **C94**.
Không nên nhảy ngay vào keyword spotting hoặc command mapping trước khi AudioRecord + energy + VAD-light + SoundPool playback chạy ổn định.
