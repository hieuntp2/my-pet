# Codex Tasks — Phase 1 Full (Android Pet Brain)

Version: v1
Purpose: Bộ task đầy đủ cho **Phase 1** ở mức copy-paste vào Codex/Claude.

Rule bắt buộc:

* Chỉ giao **1 task mỗi lần**
* Không mock logic production
* Không để hàm trống / TODO / throw NotImplementedException
* Task xong phải **build được**
* Task xong phải có **cách verify thấy được**
* Không mở rộng sang task khác nếu chưa được yêu cầu

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

# Batch 1 — App Foundation

## C1

```md
Task ID: C1
Title: Create Android multi-module project skeleton

Goal:
Create the Android project under `android-brain/` with modules `app`, `core-common`, `ui-avatar`, `brain`, `memory`, and `perception`, and make sure the project builds successfully.

Read first:
- docs/project_manifest.md
- docs/development_roadmap.md
- docs/02_android_pet_brain_architecture.md

Files likely touched:
- android-brain/settings.gradle.kts
- android-brain/build.gradle.kts
- android-brain/app/*
- android-brain/core-common/*
- android-brain/ui-avatar/*
- android-brain/brain/*
- android-brain/memory/*
- android-brain/perception/*

Implementation steps:
1. Create the root Android project in `android-brain/`.
2. Add the required modules and wire them in Gradle.
3. Configure a minimal compileSdk/minSdk/targetSdk setup consistently.
4. Ensure `app` depends on the feature modules as needed.
5. Make the project compile successfully.

Definition of Done:
- The multi-module project exists under `android-brain/`.
- Gradle sync succeeds.
- `./gradlew assembleDebug` succeeds.

How to verify:
- Run `./gradlew projects` and confirm all modules are listed.
- Run `./gradlew assembleDebug`.

Build command:
- ./gradlew assembleDebug
```

## C2

```md
Task ID: C2
Title: Add MainActivity and navigation shell

Goal:
Create a minimal runnable app with `MainActivity` and a simple navigation shell that can switch between Home and Debug screens.

Read first:
- docs/project_manifest.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../MainActivity.kt
- android-brain/app/src/main/java/.../navigation/*
- android-brain/app/src/main/java/.../ui/*

Implementation steps:
1. Create `MainActivity`.
2. Add a simple Compose navigation shell.
3. Create Home and Debug destinations.
4. Make Home the default start destination.

Definition of Done:
- App launches successfully.
- User can switch between Home and Debug screens.
- Build succeeds.

How to verify:
- Install and launch the app.
- Tap navigation UI and confirm both screens open.

Build command:
- ./gradlew assembleDebug
```

## C3

```md
Task ID: C3
Title: Add core logger and time provider

Goal:
Implement a reusable logger utility and time provider in `core-common`, and use them in the app shell.

Read first:
- docs/project_manifest.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/core-common/src/main/java/.../logging/*
- android-brain/core-common/src/main/java/.../time/*
- android-brain/app/src/main/java/.../*

Implementation steps:
1. Add a logger wrapper around Android logging.
2. Add a time provider abstraction with a real system implementation.
3. Use both in app startup code.

Definition of Done:
- Logger writes real log lines.
- Time provider returns the current system time.
- Build succeeds.

How to verify:
- Launch app and inspect Logcat for startup log.

Build command:
- ./gradlew assembleDebug
```

## C4

```md
Task ID: C4
Title: Add app theme and basic home layout

Goal:
Create a stable app theme and a basic Home screen layout that can host avatar and controls later.

Read first:
- docs/project_manifest.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../theme/*
- android-brain/app/src/main/java/.../home/HomeScreen.kt

Implementation steps:
1. Add app theme files.
2. Create a basic Home screen layout with top title and content container.
3. Ensure Home screen renders without crashes.

Definition of Done:
- Home screen is visible and stable.
- Build succeeds.

How to verify:
- Launch app and confirm Home screen layout appears.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 2 — Avatar MVP

## C5

```md
Task ID: C5
Title: Create AvatarEmotion and AvatarState models

Goal:
Create the core avatar models in `ui-avatar`: `AvatarEmotion` and `AvatarState`.

Read first:
- docs/project_manifest.md
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/ui-avatar/src/main/java/.../model/AvatarEmotion.kt
- android-brain/ui-avatar/src/main/java/.../model/AvatarState.kt

Implementation steps:
1. Add `AvatarEmotion` enum with at least IDLE, HAPPY, CURIOUS, SLEEPY.
2. Add `AvatarState` data class with emotion, intensity, blinkRate, mouthState.
3. Export the models for use from `app`.

Definition of Done:
- Models compile.
- App module can reference them.
- Build succeeds.

How to verify:
- Use the models from Home screen code and run a build.

Build command:
- ./gradlew assembleDebug
```

## C6

```md
Task ID: C6
Title: Render a static avatar on the Home screen

Goal:
Display a basic robot face on the Home screen using `AvatarState`.

Read first:
- docs/project_manifest.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/ui-avatar/src/main/java/.../AvatarFace.kt
- android-brain/app/src/main/java/.../home/HomeScreen.kt

Implementation steps:
1. Create a Compose avatar component.
2. Render eyes and mouth based on `AvatarState`.
3. Show it on the Home screen with a default IDLE state.

Definition of Done:
- App shows a visible robot face on Home screen.
- Build succeeds.

How to verify:
- Launch app and confirm the avatar is visible.

Build command:
- ./gradlew assembleDebug
```

## C7

```md
Task ID: C7
Title: Add manual avatar emotion switching

Goal:
Allow the user to switch avatar emotion manually on the Home screen and see visual changes immediately.

Read first:
- docs/project_manifest.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../home/HomeScreen.kt
- android-brain/ui-avatar/src/main/java/.../AvatarFace.kt

Implementation steps:
1. Add UI controls for emotion selection.
2. Bind selected emotion to `AvatarState`.
3. Render distinct visuals for HAPPY, CURIOUS, and SLEEPY.

Definition of Done:
- User can switch emotion from the UI.
- The avatar visibly changes for each state.
- Build succeeds.

How to verify:
- Launch app and tap each emotion control.

Build command:
- ./gradlew assembleDebug
```

## C8

```md
Task ID: C8
Title: Add blink animation to avatar

Goal:
Add a real blink animation to the avatar that runs automatically.

Read first:
- docs/project_manifest.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/ui-avatar/src/main/java/.../AvatarFace.kt

Implementation steps:
1. Add a timer or animation loop for blinking.
2. Use the existing `blinkRate` to control frequency.
3. Ensure blinking works in all current emotions.

Definition of Done:
- The avatar blinks automatically.
- No crashes or runaway recomposition issues.
- Build succeeds.

How to verify:
- Launch app and watch the avatar for several seconds.

Build command:
- ./gradlew assembleDebug
```

## C9

```md
Task ID: C9
Title: Add subtle idle animation to avatar

Goal:
Add a subtle idle animation so the avatar looks alive even when no other state changes happen.

Read first:
- docs/project_manifest.md
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/ui-avatar/src/main/java/.../AvatarFace.kt

Implementation steps:
1. Add a small breathing or bobbing animation.
2. Keep it visually subtle and safe for all current emotions.
3. Ensure it does not interfere with blink animation.

Definition of Done:
- Avatar shows a subtle idle motion.
- Build succeeds.

How to verify:
- Launch app and observe the idle face for 10–15 seconds.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 3 — Debug and Event Foundation

## C10

```md
Task ID: C10
Title: Create Debug screen and app info panel

Goal:
Create a usable Debug screen that shows app info such as build version and current time.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../debug/DebugScreen.kt

Implementation steps:
1. Create the Debug screen UI.
2. Show build version and current time using the real time provider.
3. Make sure the screen is reachable via navigation.

Definition of Done:
- Debug screen opens successfully.
- Real app info is shown.
- Build succeeds.

How to verify:
- Navigate to Debug screen and inspect values.

Build command:
- ./gradlew assembleDebug
```

## C11

```md
Task ID: C11
Title: Define event models and EventBus interface

Goal:
Create the event foundation with `EventType`, `EventEnvelope`, and `EventBus` interface.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../events/EventType.kt
- android-brain/brain/src/main/java/.../events/EventEnvelope.kt
- android-brain/brain/src/main/java/.../events/EventBus.kt

Implementation steps:
1. Define initial event types.
2. Define an event envelope with id, type, timestamp, payload JSON.
3. Define the EventBus interface for publish and observe.

Definition of Done:
- The event models compile.
- Other modules can depend on them.
- Build succeeds.

How to verify:
- Reference the models from `app` and build.

Build command:
- ./gradlew assembleDebug
```

## C12

```md
Task ID: C12
Title: Implement in-memory EventBus and publish test events

Goal:
Implement a working in-memory EventBus and allow the UI to emit and observe test events.

Read first:
- docs/development_roadmap.md
- docs/02_android_pet_brain_architecture.md

Files likely touched:
- android-brain/brain/src/main/java/.../events/InMemoryEventBus.kt
- android-brain/app/src/main/java/.../debug/DebugScreen.kt
- android-brain/app/src/main/java/.../home/HomeScreen.kt

Implementation steps:
1. Implement the EventBus using SharedFlow or similar.
2. Add a UI action to emit a TEST_EVENT.
3. Observe the latest event from the app layer.

Definition of Done:
- Tapping the test button emits a real event.
- The latest event is observable from the UI.
- Build succeeds.

How to verify:
- Open the app, emit a test event, and verify that the UI/log reflects it.

Build command:
- ./gradlew assembleDebug
```

## C13

```md
Task ID: C13
Title: Add debug overlay showing last event

Goal:
Add an overlay on the Home screen that shows the latest event type from the real EventBus.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../home/HomeScreen.kt
- android-brain/app/src/main/java/.../debug/DebugOverlay.kt

Implementation steps:
1. Create a debug overlay component.
2. Bind it to the latest event from the EventBus.
3. Render the event type on the Home screen.

Definition of Done:
- The latest event label updates after emitting a test event.
- Build succeeds.

How to verify:
- Emit TEST_EVENT and confirm overlay text changes.

Build command:
- ./gradlew assembleDebug
```

## C14

```md
Task ID: C14
Title: Add Event Viewer screen shell

Goal:
Create a screen shell for viewing events so it is ready to be connected to persistent data later.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../debug/EventViewerScreen.kt
- android-brain/app/src/main/java/.../navigation/*

Implementation steps:
1. Create the Event Viewer screen and navigation route.
2. Show a real empty state message instead of fake data.
3. Link to it from the Debug screen.

Definition of Done:
- Event Viewer screen opens.
- Empty state is shown clearly.
- Build succeeds.

How to verify:
- Open Debug screen and navigate to Event Viewer.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 4 — Room Event Persistence

## C15

```md
Task ID: C15
Title: Add Room database and events table

Goal:
Set up Room in the `memory` module and create a persistent `events` table.

Read first:
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/memory/src/main/java/.../db/AppDatabase.kt
- android-brain/memory/src/main/java/.../db/EventEntity.kt
- android-brain/memory/src/main/java/.../db/EventDao.kt

Implementation steps:
1. Add Room dependencies.
2. Create `EventEntity`.
3. Create `EventDao` with insert/list/clear methods.
4. Create the `AppDatabase`.

Definition of Done:
- Database compiles and initializes.
- Events table exists.
- Build succeeds.

How to verify:
- Run app and confirm DB initialization does not crash.

Build command:
- ./gradlew assembleDebug
```

## C16

```md
Task ID: C16
Title: Implement Room-backed EventStore

Goal:
Create a real `EventStore` implementation backed by Room for saving and querying events.

Read first:
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/memory/src/main/java/.../events/EventStore.kt
- android-brain/memory/src/main/java/.../events/RoomEventStore.kt
- android-brain/memory/src/main/java/.../db/*

Implementation steps:
1. Define the `EventStore` interface.
2. Implement `RoomEventStore`.
3. Provide mapping between `EventEnvelope` and `EventEntity`.

Definition of Done:
- Events can be saved and queried from Room through EventStore.
- Build succeeds.

How to verify:
- Add a temporary integration call from app startup or debug action and inspect DB-backed results in logs.

Build command:
- ./gradlew assembleDebug
```

## C17

```md
Task ID: C17
Title: Persist published events to EventStore

Goal:
Wire the real EventBus so every published event is also saved to the Room-backed EventStore.

Read first:
- docs/development_roadmap.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/brain/src/main/java/.../events/InMemoryEventBus.kt
- android-brain/memory/src/main/java/.../events/RoomEventStore.kt
- android-brain/app/src/main/java/.../*

Implementation steps:
1. Inject or connect EventStore where events are published.
2. Save each published event to Room.
3. Keep event observation behavior unchanged.

Definition of Done:
- A TEST_EVENT is both observable in-memory and saved in DB.
- Build succeeds.

How to verify:
- Emit a test event.
- Confirm the latest event is still shown in UI.
- Confirm it exists in DB-backed queries or logs.

Build command:
- ./gradlew assembleDebug
```

## C18

```md
Task ID: C18
Title: Back Event Viewer with Room data

Goal:
Connect the Event Viewer screen to real Room data and show the latest saved events.

Read first:
- docs/development_roadmap.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/app/src/main/java/.../debug/EventViewerScreen.kt
- android-brain/memory/src/main/java/.../events/*

Implementation steps:
1. Add a query/use-case to fetch the latest events.
2. Render them in a simple list.
3. Replace the empty state when data exists.

Definition of Done:
- The Event Viewer displays real saved events.
- Build succeeds.

How to verify:
- Emit events and open Event Viewer.
- Confirm new rows appear.

Build command:
- ./gradlew assembleDebug
```

## C19

```md
Task ID: C19
Title: Verify event persistence across app restart

Goal:
Ensure saved events survive app restarts and the viewer still loads them correctly.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../debug/EventViewerScreen.kt
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Review initialization flow.
2. Ensure Event Viewer query loads persisted events on cold start.
3. Fix any lifecycle or repository issue blocking persistence.

Definition of Done:
- Events still appear after app is force-closed and reopened.
- Build succeeds.

How to verify:
- Emit several events.
- Force close the app.
- Reopen app and verify the events are still visible.

Build command:
- ./gradlew assembleDebug
```

## C20

```md
Task ID: C20
Title: Add clear events action and real empty state recovery

Goal:
Add a real clear action to remove saved events and return the Event Viewer to its empty state cleanly.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../debug/EventViewerScreen.kt
- android-brain/memory/src/main/java/.../db/EventDao.kt
- android-brain/memory/src/main/java/.../events/*

Implementation steps:
1. Add a clear events action in the UI.
2. Delete all saved events from Room.
3. Refresh the Event Viewer state.

Definition of Done:
- Clear action deletes saved events.
- Event Viewer returns to empty state.
- Build succeeds.

How to verify:
- Save a few events.
- Use clear action.
- Confirm Event Viewer is empty.

Build command:
- ./gradlew assembleDebug
```

## C21

```md
Task ID: C21
Title: Add export events to JSON file

Goal:
Export real saved events to a JSON file from the app and show the result path or success message.

Read first:
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../debug/EventViewerScreen.kt
- android-brain/memory/src/main/java/.../events/*
- android-brain/core-common/src/main/java/.../file/*

Implementation steps:
1. Query saved events from Room.
2. Serialize them to JSON.
3. Write the file to app-accessible storage.
4. Show success information in the UI.

Definition of Done:
- Export action creates a non-empty JSON file.
- User gets a visible success state.
- Build succeeds.

How to verify:
- Emit a few events.
- Export events.
- Verify that the file exists and is non-empty.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 5 — Camera and Frame Pipeline

## C22

```md
Task ID: C22
Title: Add camera permission flow and Camera screen shell

Goal:
Add camera permission handling and create a Camera screen that opens without crashing.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../camera/CameraScreen.kt
- android-brain/app/src/main/java/.../permissions/*
- android-brain/app/src/main/AndroidManifest.xml

Implementation steps:
1. Add camera permission to the manifest.
2. Create runtime permission handling.
3. Create a Camera screen that handles granted and denied states.

Definition of Done:
- Permission can be requested from the app.
- Granted and denied states are handled without crashing.
- Build succeeds.

How to verify:
- Launch Camera screen on a device.
- Deny and grant permission to confirm both flows work.

Build command:
- ./gradlew assembleDebug
```

## C23

```md
Task ID: C23
Title: Integrate CameraX preview

Goal:
Show a real CameraX preview on the Camera screen.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../camera/*
- android-brain/app/src/main/java/.../camera/CameraScreen.kt

Implementation steps:
1. Add CameraX dependencies.
2. Bind a preview use case to lifecycle.
3. Render the preview on the Camera screen.

Definition of Done:
- The device camera preview is visible.
- Leaving and reopening the screen does not break the camera.
- Build succeeds.

How to verify:
- Launch Camera screen and verify preview.
- Navigate away and back to verify lifecycle correctness.

Build command:
- ./gradlew assembleDebug
```

## C24

```md
Task ID: C24
Title: Add FrameAnalyzer and log frame size

Goal:
Implement a real frame analyzer that logs frame width and height once per second.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../camera/FrameAnalyzer.kt
- android-brain/app/src/main/java/.../camera/CameraScreen.kt

Implementation steps:
1. Create a `FrameAnalyzer` class.
2. Attach it to CameraX `ImageAnalysis`.
3. Log frame size at a rate-limited interval.

Definition of Done:
- The analyzer receives real camera frames.
- Width and height are logged once per second.
- Build succeeds.

How to verify:
- Open Camera screen and inspect Logcat.

Build command:
- ./gradlew assembleDebug
```

## C25

```md
Task ID: C25
Title: Publish CAMERA_FRAME_RECEIVED events from analyzer

Goal:
Publish a real `CAMERA_FRAME_RECEIVED` event from the analyzer at a rate-limited interval.

Read first:
- docs/development_roadmap.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/perception/src/main/java/.../camera/FrameAnalyzer.kt
- android-brain/brain/src/main/java/.../events/*

Implementation steps:
1. Inject or connect EventBus into the frame analyzer pipeline.
2. Publish `CAMERA_FRAME_RECEIVED` once per second.
3. Include frame width and height in payload.

Definition of Done:
- Event Viewer shows real camera frame events.
- Build succeeds.

How to verify:
- Open Camera screen.
- Open Event Viewer and confirm frame events appear.

Build command:
- ./gradlew assembleDebug
```

## C26

```md
Task ID: C26
Title: Show frame metrics on debug overlay

Goal:
Display the latest real frame width, height, and processing time on the debug overlay.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../debug/DebugOverlay.kt
- android-brain/perception/src/main/java/.../camera/FrameAnalyzer.kt

Implementation steps:
1. Track latest frame width and height from analyzer events.
2. Measure frame processing time.
3. Display the real metrics on the overlay.

Definition of Done:
- Overlay shows real frame size and timing metrics.
- Values update while the camera runs.
- Build succeeds.

How to verify:
- Open Camera screen and confirm overlay values change in real time.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 6 — Face Detection and Samples

## C27

```md
Task ID: C27
Title: Add face detection dependency and engine interface

Goal:
Add the chosen face detection dependency and define a `FaceDetectionEngine` interface in the perception layer.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/build.gradle.kts
- android-brain/perception/src/main/java/.../face/FaceDetectionEngine.kt

Implementation steps:
1. Add the real face detection dependency.
2. Define a face detection engine interface.
3. Keep the module compiling cleanly.

Definition of Done:
- Dependency is added.
- Interface compiles and is usable.
- Build succeeds.

How to verify:
- Build the app and confirm perception module compiles.

Build command:
- ./gradlew assembleDebug
```

## C28

```md
Task ID: C28
Title: Implement real FaceDetectionEngine

Goal:
Implement a real on-device face detector that returns a mapped app-level face result.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../face/RealFaceDetectionEngine.kt
- android-brain/perception/src/main/java/.../face/model/*

Implementation steps:
1. Implement the engine using the chosen library.
2. Map detector output to app-level face result models.
3. Return bounding box and any stable metadata available.

Definition of Done:
- Face detection runs on real camera frames.
- Build succeeds.

How to verify:
- Temporarily call the engine from the camera pipeline and log detection count.

Build command:
- ./gradlew assembleDebug
```

## C29

```md
Task ID: C29
Title: Draw face bounding boxes on camera preview

Goal:
Draw real face bounding boxes on top of the camera preview.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../camera/CameraScreen.kt
- android-brain/app/src/main/java/.../camera/FaceOverlay.kt
- android-brain/perception/src/main/java/.../face/*

Implementation steps:
1. Call face detection from the camera analysis pipeline.
2. Transform detection coordinates correctly for preview overlay.
3. Render face boxes in the UI.

Definition of Done:
- Bounding boxes appear over detected faces.
- Build succeeds.

How to verify:
- Show a face to the camera and confirm boxes track it.

Build command:
- ./gradlew assembleDebug
```

## C30

```md
Task ID: C30
Title: Publish FACE_DETECTED events and show face count

Goal:
Publish real `FACE_DETECTED` events on face count changes and display current face count in the UI.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../face/*
- android-brain/app/src/main/java/.../camera/CameraScreen.kt
- android-brain/app/src/main/java/.../debug/DebugOverlay.kt

Implementation steps:
1. Detect changes in face count.
2. Publish `FACE_DETECTED` with useful payload.
3. Display current face count in overlay or camera UI.

Definition of Done:
- Events appear when face count changes.
- UI shows the current count.
- Build succeeds.

How to verify:
- Move into and out of frame and inspect Event Viewer plus UI count.

Build command:
- ./gradlew assembleDebug
```

## C31

```md
Task ID: C31
Title: Add face crop utility from detected face boxes

Goal:
Crop face bitmaps from real camera frames using detected face boxes.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../face/FaceCropper.kt
- android-brain/perception/src/main/java/.../camera/*

Implementation steps:
1. Create a utility to crop face regions from frames.
2. Handle bounds safely.
3. Return usable bitmaps for later embedding or saving.

Definition of Done:
- A face crop can be produced from a real detected face.
- Build succeeds.

How to verify:
- Log crop size or render a single cropped preview in debug UI.

Build command:
- ./gradlew assembleDebug
```

## C32

```md
Task ID: C32
Title: Add Capture Face Sample action and thumbnail list

Goal:
Allow the user to capture a real face sample from the camera screen and display captured thumbnails.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../camera/CameraScreen.kt
- android-brain/app/src/main/java/.../camera/FaceSampleUi.kt
- android-brain/core-common/src/main/java/.../file/*

Implementation steps:
1. Add a Capture Face Sample button enabled only when a face is present.
2. Save the face crop to app storage.
3. Show captured sample thumbnails in the UI.

Definition of Done:
- User can capture a face sample.
- A real file is saved.
- Thumbnail list updates.
- Build succeeds.

How to verify:
- Detect a face.
- Tap Capture Face Sample.
- Confirm thumbnail appears and file exists.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 7 — Person Tables and Embeddings

## C33

```md
Task ID: C33
Title: Create persons table and DAO

Goal:
Add Room persistence for persons with fields needed for Phase 1 recognition and relationship stats.

Read first:
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/memory/src/main/java/.../db/PersonEntity.kt
- android-brain/memory/src/main/java/.../db/PersonDao.kt
- android-brain/memory/src/main/java/.../db/AppDatabase.kt

Implementation steps:
1. Create `PersonEntity` with id, displayName, role, createdAt, lastSeenAt, seenCount, familiarity.
2. Create DAO methods for insert, update, get, list.
3. Add table to AppDatabase.

Definition of Done:
- Persons table exists.
- DAO compiles and works.
- Build succeeds.

How to verify:
- Insert one person through a temporary integration path and query it back.

Build command:
- ./gradlew assembleDebug
```

## C34

```md
Task ID: C34
Title: Create face embeddings table and DAO

Goal:
Add Room persistence for face embeddings linked to persons.

Read first:
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/memory/src/main/java/.../db/FaceEmbeddingEntity.kt
- android-brain/memory/src/main/java/.../db/FaceEmbeddingDao.kt
- android-brain/memory/src/main/java/.../db/AppDatabase.kt

Implementation steps:
1. Create `FaceEmbeddingEntity` with owner id, dim, vector blob, normalized flag, createdAt.
2. Create DAO methods for insert and query by person.
3. Register table in AppDatabase.

Definition of Done:
- Face embeddings table exists.
- DAO compiles and works.
- Build succeeds.

How to verify:
- Insert one sample embedding and query it back through a temporary integration path.

Build command:
- ./gradlew assembleDebug
```

## C35

```md
Task ID: C35
Title: Create Persons screen with real empty state

Goal:
Create a Persons screen backed by Room query state, showing a clear empty state when there are no saved persons.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../persons/PersonsScreen.kt
- android-brain/app/src/main/java/.../navigation/*
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Add navigation route for Persons.
2. Query saved persons.
3. Render either empty state or list.

Definition of Done:
- Persons screen opens.
- Empty state is shown when DB has no persons.
- Build succeeds.

How to verify:
- Open Persons screen before any saved people exist.

Build command:
- ./gradlew assembleDebug
```

## C36

```md
Task ID: C36
Title: Add face embedding model asset and engine interface

Goal:
Add the face embedding model asset and define a `FaceEmbeddingEngine` interface.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/assets/*
- android-brain/perception/src/main/java/.../face/FaceEmbeddingEngine.kt

Implementation steps:
1. Add the chosen TFLite embedding model to assets.
2. Define an interface for generating embeddings from face bitmaps.
3. Keep the module build-safe.

Definition of Done:
- Model asset is present.
- Interface compiles.
- Build succeeds.

How to verify:
- Build the app and confirm the module packages the asset correctly.

Build command:
- ./gradlew assembleDebug
```

## C37

```md
Task ID: C37
Title: Implement TFLite face embedding engine

Goal:
Implement a real TFLite-based face embedding engine that returns a FloatArray from a face bitmap.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../face/TfLiteFaceEmbeddingEngine.kt
- android-brain/perception/src/main/java/.../ml/*

Implementation steps:
1. Load the TFLite model.
2. Preprocess face bitmaps.
3. Run inference and return a real vector.

Definition of Done:
- Embedding engine returns a real vector.
- Build succeeds.

How to verify:
- Run embedding on a captured face sample and log vector dimension.

Build command:
- ./gradlew assembleDebug
```

## C38

```md
Task ID: C38
Title: Add vector normalization and cosine similarity utilities

Goal:
Implement real vector normalization and cosine similarity helpers for face matching.

Read first:
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/core-common/src/main/java/.../math/VectorMath.kt

Implementation steps:
1. Add L2 normalization utility.
2. Add cosine similarity utility.
3. Keep the API simple and production-usable.

Definition of Done:
- Utilities compile and are used by later embedding code.
- Build succeeds.

How to verify:
- Add a minimal verification path or unit test to confirm identical vectors yield high similarity.

Build command:
- ./gradlew assembleDebug
```

## C39

```md
Task ID: C39
Title: Add centroid calculation for multiple embeddings

Goal:
Implement a real centroid calculation utility for multiple embeddings to support person matching.

Read first:
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/core-common/src/main/java/.../math/VectorMath.kt

Implementation steps:
1. Add centroid calculation over multiple vectors.
2. Normalize the centroid output appropriately.
3. Make it usable from the recognition flow.

Definition of Done:
- Centroid utility compiles and works.
- Build succeeds.

How to verify:
- Add a small verification path or unit test using a few known vectors.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 8 — Teach Person Flow

## C40

```md
Task ID: C40
Title: Create Teach Person screen shell

Goal:
Create a Teach Person screen that receives captured face samples and allows a user to name the person.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../persons/TeachPersonScreen.kt
- android-brain/app/src/main/java/.../navigation/*

Implementation steps:
1. Create navigation route for Teach Person.
2. Display captured sample thumbnails.
3. Add a text input for person name.

Definition of Done:
- Teach Person screen opens and shows real captured samples.
- Build succeeds.

How to verify:
- Capture face samples and navigate to Teach Person.

Build command:
- ./gradlew assembleDebug
```

## C41

```md
Task ID: C41
Title: Enforce minimum face sample count before save

Goal:
Require at least 3 captured face samples before saving a new person.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../persons/TeachPersonScreen.kt

Implementation steps:
1. Validate sample count.
2. Disable or block save when samples are insufficient.
3. Show a clear validation message.

Definition of Done:
- Saving is blocked when fewer than 3 samples exist.
- Validation message is visible.
- Build succeeds.

How to verify:
- Try saving with 1–2 samples and confirm it is rejected.

Build command:
- ./gradlew assembleDebug
```

## C42

```md
Task ID: C42
Title: Save new person record to Room

Goal:
Persist a new person record from the Teach Person screen.

Read first:
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../persons/TeachPersonScreen.kt
- android-brain/memory/src/main/java/.../db/PersonDao.kt
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Create a save use-case or repository method.
2. Persist a person record with name and timestamps.
3. Navigate back or update UI after successful save.

Definition of Done:
- Saving creates a real person in DB.
- Persons screen shows the new person.
- Build succeeds.

How to verify:
- Save a person and open Persons screen.

Build command:
- ./gradlew assembleDebug
```

## C43

```md
Task ID: C43
Title: Compute and save embeddings for captured face samples

Goal:
Compute real embeddings for each captured sample and save them linked to the new person.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../persons/TeachPersonScreen.kt
- android-brain/perception/src/main/java/.../face/*
- android-brain/memory/src/main/java/.../db/FaceEmbeddingDao.kt

Implementation steps:
1. Load each captured sample.
2. Compute embeddings using the real engine.
3. Normalize and save embeddings to Room linked to the person.

Definition of Done:
- A saved person has real face embeddings in DB.
- Build succeeds.

How to verify:
- Save a person and inspect embedding count via logs or the person detail screen later.

Build command:
- ./gradlew assembleDebug
```

## C44

```md
Task ID: C44
Title: Emit USER_TAUGHT_PERSON event after successful save

Goal:
Publish a real `USER_TAUGHT_PERSON` event after a new person and their embeddings are saved successfully.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../persons/TeachPersonScreen.kt
- android-brain/brain/src/main/java/.../events/*

Implementation steps:
1. Publish the event only after save succeeds.
2. Include person id, display name, and sample count in payload.
3. Keep UI flow stable after event emission.

Definition of Done:
- Event Viewer shows `USER_TAUGHT_PERSON` after a successful save.
- Build succeeds.

How to verify:
- Save a new person and inspect Event Viewer.

Build command:
- ./gradlew assembleDebug
```

## C45

```md
Task ID: C45
Title: Add Person Detail screen with stats and embedding count

Goal:
Create a Person Detail screen showing a person's saved metadata and the count of linked face embeddings.

Read first:
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../persons/PersonDetailScreen.kt
- android-brain/app/src/main/java/.../navigation/*
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Add navigation from Persons list to detail screen.
2. Load person metadata and embedding count.
3. Render a simple but real detail UI.

Definition of Done:
- Person Detail screen opens for a saved person.
- Real stats are visible.
- Build succeeds.

How to verify:
- Save a person, open Persons, tap the person, inspect detail screen.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 9 — Person Recognition

## C46

```md
Task ID: C46
Title: Create recognition service using saved person embeddings

Goal:
Create a real person recognition service that compares a current face embedding against saved person embeddings.

Read first:
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../recognition/PersonRecognitionService.kt
- android-brain/memory/src/main/java/.../*
- android-brain/core-common/src/main/java/.../math/*

Implementation steps:
1. Load saved embeddings grouped by person.
2. Compute person centroids or nearest comparisons.
3. Return the best candidate and similarity score.

Definition of Done:
- Recognition service can score a current face against known persons.
- Build succeeds.

How to verify:
- Run the service against one known sample and inspect the output in logs.

Build command:
- ./gradlew assembleDebug
```

## C47

```md
Task ID: C47
Title: Add threshold configuration for person recognition

Goal:
Add a real configurable threshold used by person recognition.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/core-common/src/main/java/.../config/*
- android-brain/app/src/main/java/.../debug/*
- android-brain/brain/src/main/java/.../recognition/*

Implementation steps:
1. Add a default threshold config.
2. Make the recognition service use the threshold.
3. Surface the current threshold in the Debug UI.

Definition of Done:
- Recognition threshold is configurable and actually used.
- Build succeeds.

How to verify:
- Change threshold and confirm recognition behavior changes in test runs.

Build command:
- ./gradlew assembleDebug
```

## C48

```md
Task ID: C48
Title: Recognize current face on the camera screen

Goal:
Run person recognition on the current detected face from the camera pipeline and determine whether it matches a known person.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../camera/CameraScreen.kt
- android-brain/perception/src/main/java/.../face/*
- android-brain/brain/src/main/java/.../recognition/*

Implementation steps:
1. Use the current face crop from the camera pipeline.
2. Compute a live embedding.
3. Compare it against saved people using the recognition service.

Definition of Done:
- The app attempts real recognition against saved persons.
- Build succeeds.

How to verify:
- Train a person first.
- Show the same face to the camera and inspect logs/UI.

Build command:
- ./gradlew assembleDebug
```

## C49

```md
Task ID: C49
Title: Show recognized person label on camera screen

Goal:
Display the recognized person's name on the camera screen when recognition succeeds.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../camera/CameraScreen.kt

Implementation steps:
1. Bind recognition result state into the Camera screen.
2. Render the matched person's display name.
3. Keep UI stable when no person is recognized.

Definition of Done:
- A known person's name appears on screen when recognized.
- Build succeeds.

How to verify:
- Train a person and then show them to the camera.

Build command:
- ./gradlew assembleDebug
```

## C50

```md
Task ID: C50
Title: Publish PERSON_RECOGNIZED event and update person stats

Goal:
Publish a `PERSON_RECOGNIZED` event and update the recognized person's `lastSeenAt` and `seenCount` fields.

Read first:
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../recognition/*
- android-brain/memory/src/main/java/.../db/PersonDao.kt
- android-brain/app/src/main/java/.../camera/CameraScreen.kt

Implementation steps:
1. Update person stats on successful recognition.
2. Publish `PERSON_RECOGNIZED` with useful payload.
3. Keep duplicate spam controlled reasonably.

Definition of Done:
- Recognition updates person stats in DB.
- Event Viewer shows `PERSON_RECOGNIZED`.
- Build succeeds.

How to verify:
- Recognize a known person, then inspect Event Viewer and Persons screen stats.

Build command:
- ./gradlew assembleDebug
```

## C51

```md
Task ID: C51
Title: Handle unknown person state and publish PERSON_UNKNOWN

Goal:
Handle the unknown-person branch properly and publish `PERSON_UNKNOWN` when no saved person matches above threshold.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../recognition/*
- android-brain/app/src/main/java/.../camera/CameraScreen.kt

Implementation steps:
1. Detect below-threshold recognition cases.
2. Show `Unknown` on the camera UI.
3. Publish `PERSON_UNKNOWN` with confidence-related payload.

Definition of Done:
- Unknown people are shown as `Unknown`.
- `PERSON_UNKNOWN` appears in Event Viewer.
- Build succeeds.

How to verify:
- Show a different face or untrained face to the camera.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 10 — Object Detection and Object Memory

## C52

```md
Task ID: C52
Title: Add object detection model asset and engine interface

Goal:
Add the object detection model asset and define an `ObjectDetectionEngine` interface.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/assets/*
- android-brain/perception/src/main/java/.../object/ObjectDetectionEngine.kt

Implementation steps:
1. Add the chosen object detection model asset.
2. Define the object detection engine interface.
3. Keep the module build-safe.

Definition of Done:
- Model asset is present.
- Interface compiles.
- Build succeeds.

How to verify:
- Build the app and confirm the asset packages correctly.

Build command:
- ./gradlew assembleDebug
```

## C53

```md
Task ID: C53
Title: Implement real object detection engine

Goal:
Implement a real object detection engine returning at least the top label and confidence from current frames.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../object/RealObjectDetectionEngine.kt
- android-brain/perception/src/main/java/.../object/model/*

Implementation steps:
1. Load the object detection model.
2. Run inference on current frames.
3. Return mapped detection results with label and confidence.

Definition of Done:
- The engine returns real object detection results.
- Build succeeds.

How to verify:
- Call it from the camera pipeline and inspect logs.

Build command:
- ./gradlew assembleDebug
```

## C54

```md
Task ID: C54
Title: Show current top object label on camera screen

Goal:
Display the current top object label from the real object detector on the camera screen.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../camera/CameraScreen.kt
- android-brain/perception/src/main/java/.../object/*

Implementation steps:
1. Run object detection in the camera analysis pipeline.
2. Extract the best label.
3. Render it in the UI.

Definition of Done:
- The camera screen shows a real object label.
- Build succeeds.

How to verify:
- Point the camera at common objects and inspect label changes.

Build command:
- ./gradlew assembleDebug
```

## C55

```md
Task ID: C55
Title: Publish OBJECT_DETECTED events

Goal:
Publish `OBJECT_DETECTED` events based on real object detection results.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../object/*
- android-brain/brain/src/main/java/.../events/*

Implementation steps:
1. Publish an event from the detection pipeline.
2. Include label and confidence in payload.
3. Keep rate or duplication under control.

Definition of Done:
- Event Viewer shows `OBJECT_DETECTED` events.
- Build succeeds.

How to verify:
- Use the camera with visible objects and inspect Event Viewer.

Build command:
- ./gradlew assembleDebug
```

## C56

```md
Task ID: C56
Title: Create objects table and DAO

Goal:
Persist objects seen or taught by the user with fields needed for Phase 1 behavior and memory.

Read first:
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/memory/src/main/java/.../db/ObjectEntity.kt
- android-brain/memory/src/main/java/.../db/ObjectDao.kt
- android-brain/memory/src/main/java/.../db/AppDatabase.kt

Implementation steps:
1. Create `ObjectEntity` with canonicalLabel, aliasName, createdAt, lastSeenAt, seenCount, importance.
2. Create DAO methods.
3. Register the table in AppDatabase.

Definition of Done:
- Objects table exists.
- DAO compiles and works.
- Build succeeds.

How to verify:
- Insert and query at least one object through a temporary integration path.

Build command:
- ./gradlew assembleDebug
```

## C57

```md
Task ID: C57
Title: Create Objects screen with empty state

Goal:
Create an Objects screen backed by Room query state with a clear empty state.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../objects/ObjectsScreen.kt
- android-brain/app/src/main/java/.../navigation/*
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Add a route for Objects screen.
2. Query saved objects.
3. Render either an empty state or the real list.

Definition of Done:
- Objects screen opens.
- Empty state is shown when there are no objects.
- Build succeeds.

How to verify:
- Open Objects before teaching any object.

Build command:
- ./gradlew assembleDebug
```

## C58

```md
Task ID: C58
Title: Create Teach Object flow

Goal:
Allow the user to save an alias for a detected object label and persist it as a known object.

Read first:
- docs/development_roadmap.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/app/src/main/java/.../objects/TeachObjectScreen.kt
- android-brain/app/src/main/java/.../camera/CameraScreen.kt
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Add a UI path from current detected object label to Teach Object screen.
2. Let the user enter an alias name.
3. Save the object record.

Definition of Done:
- A detected object can be taught with an alias.
- Objects screen shows the saved object.
- Build succeeds.

How to verify:
- Detect an object, teach it, and open Objects screen.

Build command:
- ./gradlew assembleDebug
```

## C59

```md
Task ID: C59
Title: Update object seen stats when known object reappears

Goal:
Update `lastSeenAt` and `seenCount` when a detected object matches a previously taught canonical label.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../object/*
- android-brain/memory/src/main/java/.../db/ObjectDao.kt
- android-brain/app/src/main/java/.../camera/CameraScreen.kt

Implementation steps:
1. Check detected label against saved objects.
2. Update object stats when a match is found.
3. Keep the flow stable under repeated detections.

Definition of Done:
- Known object stats increase when it is detected again.
- Build succeeds.

How to verify:
- Teach an object, detect it again, and inspect Objects screen.

Build command:
- ./gradlew assembleDebug
```

## C60

```md
Task ID: C60
Title: Show known object alias instead of raw label when available

Goal:
Display the taught alias on the camera screen for known objects instead of only the raw detector label.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../camera/CameraScreen.kt
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Resolve detected label to saved object if available.
2. Prefer alias in the UI.
3. Fall back safely to raw label when not known.

Definition of Done:
- Known object aliases appear on the camera screen.
- Build succeeds.

How to verify:
- Teach an object and then detect it again.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 11 — Brain State Machine and Interaction Loop

## C61

```md
Task ID: C61
Title: Define BrainState enum and store

Goal:
Create the basic brain state model and store for Phase 1: IDLE, CURIOUS, HAPPY, SLEEPY.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../state/BrainState.kt
- android-brain/brain/src/main/java/.../state/BrainStateStore.kt

Implementation steps:
1. Define BrainState enum.
2. Create a state holder or store.
3. Make it observable from the UI/app layer.

Definition of Done:
- Brain state compiles and can be observed.
- Build succeeds.

How to verify:
- Temporarily drive a fixed state and display it in debug UI.

Build command:
- ./gradlew assembleDebug
```

## C62

```md
Task ID: C62
Title: Map BrainState to AvatarState

Goal:
Map the current brain state to the avatar state so the avatar is driven by brain logic rather than manual-only controls.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../state/*
- android-brain/app/src/main/java/.../home/HomeScreen.kt
- android-brain/ui-avatar/src/main/java/.../*

Implementation steps:
1. Create a mapper from BrainState to AvatarState.
2. Bind Home screen avatar rendering to the brain state.
3. Keep a manual override only if necessary for debug, not for production flow.

Definition of Done:
- Avatar visibly changes when brain state changes.
- Build succeeds.

How to verify:
- Trigger a brain state update and inspect the avatar.

Build command:
- ./gradlew assembleDebug
```

## C63

```md
Task ID: C63
Title: Add rule PersonRecognized -> HAPPY

Goal:
When a known person is recognized, set the brain state to HAPPY.

Read first:
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../logic/*
- android-brain/brain/src/main/java/.../state/*

Implementation steps:
1. Subscribe brain logic to person recognition events.
2. Set brain state to HAPPY on successful recognition.
3. Publish a state-changed event if the state actually changes.

Definition of Done:
- Recognizing a known person changes the avatar to HAPPY.
- Build succeeds.

How to verify:
- Train a person, recognize them, and inspect Home/Camera UI plus Event Viewer.

Build command:
- ./gradlew assembleDebug
```

## C64

```md
Task ID: C64
Title: Add rule PersonUnknown -> CURIOUS

Goal:
When an unknown person is seen, set the brain state to CURIOUS.

Read first:
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../logic/*

Implementation steps:
1. Subscribe brain logic to unknown-person events.
2. Set state to CURIOUS when appropriate.
3. Avoid noisy repeated transitions.

Definition of Done:
- Seeing an unknown face changes the avatar to CURIOUS.
- Build succeeds.

How to verify:
- Show an untrained face to the camera.

Build command:
- ./gradlew assembleDebug
```

## C65

```md
Task ID: C65
Title: Add no-stimulus timer leading to SLEEPY

Goal:
Set the brain state to SLEEPY after 60 seconds without meaningful stimulus.

Read first:
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../logic/*
- android-brain/brain/src/main/java/.../state/*

Implementation steps:
1. Track the last meaningful stimulus time.
2. Add a timer or check loop.
3. Enter SLEEPY after 60 seconds of inactivity.

Definition of Done:
- After 60 seconds of no stimulus, avatar becomes SLEEPY.
- Build succeeds.

How to verify:
- Launch app, avoid interactions and detections, wait 60 seconds, inspect state.

Build command:
- ./gradlew assembleDebug
```

## C66

```md
Task ID: C66
Title: Wake from SLEEPY on strong stimulus

Goal:
Wake the brain from SLEEPY when a face, object, or user interaction occurs.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../logic/*

Implementation steps:
1. Subscribe to meaningful stimulus events.
2. Exit SLEEPY when stimulus occurs.
3. Choose a sensible destination state such as CURIOUS or HAPPY.

Definition of Done:
- SLEEPY state is exited after a meaningful stimulus.
- Build succeeds.

How to verify:
- Wait until SLEEPY, then show a face or interact.

Build command:
- ./gradlew assembleDebug
```

## C67

```md
Task ID: C67
Title: Publish BRAIN_STATE_CHANGED events

Goal:
Publish a `BRAIN_STATE_CHANGED` event whenever the brain state changes.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../state/*
- android-brain/brain/src/main/java/.../events/*

Implementation steps:
1. Detect actual state changes.
2. Publish an event with from/to state payload.
3. Avoid duplicate spam when state remains the same.

Definition of Done:
- Event Viewer shows state transitions.
- Build succeeds.

How to verify:
- Trigger several state changes and inspect Event Viewer.

Build command:
- ./gradlew assembleDebug
```

## C68

```md
Task ID: C68
Title: Add Pet interaction button and event

Goal:
Add a user interaction button representing petting/tapping the robot and publish a real event when pressed.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../home/HomeScreen.kt
- android-brain/brain/src/main/java/.../events/*

Implementation steps:
1. Add a Pet button in the Home UI.
2. Publish `USER_INTERACTED_PET` on press.
3. Surface the interaction in Event Viewer.

Definition of Done:
- Pressing the Pet button emits a real event.
- Build succeeds.

How to verify:
- Tap Pet and inspect Event Viewer.

Build command:
- ./gradlew assembleDebug
```

## C69

```md
Task ID: C69
Title: Add rule Pet while CURIOUS -> HAPPY

Goal:
When the user pets the robot while it is CURIOUS, transition the brain state to HAPPY.

Read first:
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../logic/*

Implementation steps:
1. Listen for `USER_INTERACTED_PET`.
2. Check current brain state.
3. Transition CURIOUS -> HAPPY.

Definition of Done:
- Pet while CURIOUS changes the avatar to HAPPY.
- Build succeeds.

How to verify:
- Make the robot CURIOUS, then press Pet.

Build command:
- ./gradlew assembleDebug
```

## C70

```md
Task ID: C70
Title: Add manual sleep and wake debug actions

Goal:
Add simple debug controls to force SLEEPY and wake transitions for testing the brain loop.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../debug/DebugScreen.kt
- android-brain/brain/src/main/java/.../state/*

Implementation steps:
1. Add debug controls for Sleep and Wake.
2. Trigger real brain state transitions.
3. Keep these controls clearly marked as debug-only.

Definition of Done:
- Sleep and Wake debug actions work.
- Build succeeds.

How to verify:
- Open Debug screen and use both actions.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 12 — Traits and Relationship Scores

## C71

```md
Task ID: C71
Title: Create traits snapshot table and DAO

Goal:
Persist the Phase 1 personality traits snapshot in Room.

Read first:
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/memory/src/main/java/.../db/TraitsSnapshotEntity.kt
- android-brain/memory/src/main/java/.../db/TraitsSnapshotDao.kt
- android-brain/memory/src/main/java/.../db/AppDatabase.kt

Implementation steps:
1. Add a traits snapshot entity for curiosity, sociability, energy, patience, boldness.
2. Add DAO methods for insert and latest query.
3. Register the table in AppDatabase.

Definition of Done:
- Traits snapshot table exists.
- DAO compiles and works.
- Build succeeds.

How to verify:
- Insert one snapshot through a temporary integration path and query it back.

Build command:
- ./gradlew assembleDebug
```

## C72

```md
Task ID: C72
Title: Initialize default traits on first app run

Goal:
Create and persist a default Phase 1 traits snapshot on first app run if none exists.

Read first:
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../traits/*
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Query the latest traits snapshot on startup.
2. Insert sensible defaults if none exists.
3. Reuse the saved values afterwards.

Definition of Done:
- First app run creates default traits.
- Later app runs reuse persisted traits.
- Build succeeds.

How to verify:
- Launch on a clean install and inspect the traits state.

Build command:
- ./gradlew assembleDebug
```

## C73

```md
Task ID: C73
Title: Create Traits screen showing real current values

Goal:
Create a Traits screen that shows the latest persisted personality trait values.

Read first:
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../traits/TraitsScreen.kt
- android-brain/app/src/main/java/.../navigation/*
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Add a route for Traits screen.
2. Query the latest traits snapshot.
3. Render the real values clearly.

Definition of Done:
- Traits screen opens and shows real current values.
- Build succeeds.

How to verify:
- Open Traits screen after first app run.

Build command:
- ./gradlew assembleDebug
```

## C74

```md
Task ID: C74
Title: Add rule Pet increases sociability slightly

Goal:
Update sociability by a small amount when the user pets the robot and persist a new traits snapshot.

Read first:
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../traits/*
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Listen for pet interaction events.
2. Adjust sociability slightly with clamping.
3. Persist a new traits snapshot.

Definition of Done:
- Sociability increases after pet interactions.
- Traits screen reflects the updated value.
- Build succeeds.

How to verify:
- Note current sociability, press Pet multiple times, reopen Traits screen.

Build command:
- ./gradlew assembleDebug
```

## C75

```md
Task ID: C75
Title: Add no-interaction rule reducing energy slightly

Goal:
Reduce energy slightly after prolonged inactivity and persist the updated traits snapshot.

Read first:
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../traits/*

Implementation steps:
1. Detect prolonged inactivity.
2. Lower energy by a small clamped amount.
3. Persist the updated traits snapshot.

Definition of Done:
- Energy decreases after prolonged inactivity.
- Build succeeds.

How to verify:
- Observe energy before and after an inactivity window.

Build command:
- ./gradlew assembleDebug
```

## C76

```md
Task ID: C76
Title: Publish TRAITS_UPDATED event on trait changes

Goal:
Publish a `TRAITS_UPDATED` event whenever persisted trait values change.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../traits/*
- android-brain/brain/src/main/java/.../events/*

Implementation steps:
1. Detect trait changes before persisting.
2. Publish a `TRAITS_UPDATED` event with changed fields.
3. Avoid duplicate no-op updates.

Definition of Done:
- Event Viewer shows `TRAITS_UPDATED` after real trait changes.
- Build succeeds.

How to verify:
- Trigger pet or inactivity-based trait changes and inspect Event Viewer.

Build command:
- ./gradlew assembleDebug
```

## C77

```md
Task ID: C77
Title: Update familiarity score on successful person recognition

Goal:
Increase a person's familiarity score when they are recognized successfully and persist the update.

Read first:
- docs/06_personality_engine.md
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../relationship/*
- android-brain/memory/src/main/java/.../db/PersonDao.kt

Implementation steps:
1. Define a small familiarity update rule.
2. Apply it on successful person recognition.
3. Persist the updated person record.

Definition of Done:
- Familiarity increases after repeated recognition.
- Build succeeds.

How to verify:
- Recognize the same trained person multiple times and inspect Persons screen.

Build command:
- ./gradlew assembleDebug
```

## C78

```md
Task ID: C78
Title: Update familiarity on pet interaction linked to current person

Goal:
When the current recognized person is present and the user pets the robot, increase that person's familiarity slightly.

Read first:
- docs/06_personality_engine.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../relationship/*
- android-brain/brain/src/main/java/.../memory/*
- android-brain/memory/src/main/java/.../db/PersonDao.kt

Implementation steps:
1. Resolve the current recognized person from working state.
2. Update their familiarity on pet events.
3. Persist the change.

Definition of Done:
- Petting while a known person is present increases that person's familiarity.
- Build succeeds.

How to verify:
- Recognize a person, press Pet, and inspect familiarity before/after.

Build command:
- ./gradlew assembleDebug
```

## C79

```md
Task ID: C79
Title: Show familiarity in Persons list and sort by it

Goal:
Show each person's familiarity score in the Persons list and sort descending by familiarity.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../persons/PersonsScreen.kt
- android-brain/memory/src/main/java/.../db/PersonDao.kt

Implementation steps:
1. Query persons sorted by familiarity.
2. Render familiarity in the list.
3. Keep empty state and detail navigation working.

Definition of Done:
- Persons list shows familiarity values and is sorted correctly.
- Build succeeds.

How to verify:
- Train multiple persons and inspect list order after interactions.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 13 — Working Memory and Memory Cards

## C80

```md
Task ID: C80
Title: Create WorkingMemory model and store

Goal:
Create a working memory model for current runtime context such as current person, current object, and last stimulus time.

Read first:
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../memory/WorkingMemory.kt
- android-brain/brain/src/main/java/.../memory/WorkingMemoryStore.kt

Implementation steps:
1. Define the working memory model.
2. Create a store or holder for runtime values.
3. Make it observable where needed.

Definition of Done:
- WorkingMemory exists and can be updated.
- Build succeeds.

How to verify:
- Log or inspect the model in debug UI.

Build command:
- ./gradlew assembleDebug
```

## C81

```md
Task ID: C81
Title: Update WorkingMemory from person and object events

Goal:
Update working memory when person recognition, object detection, and user interaction events happen.

Read first:
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Files likely touched:
- android-brain/brain/src/main/java/.../memory/*
- android-brain/brain/src/main/java/.../events/*

Implementation steps:
1. Subscribe working memory updates to relevant events.
2. Track current person, current object, and last stimulus time.
3. Keep state consistent when data becomes unavailable.

Definition of Done:
- Working memory updates in response to real app events.
- Build succeeds.

How to verify:
- Recognize a person or detect an object and inspect working memory state.

Build command:
- ./gradlew assembleDebug
```

## C82

```md
Task ID: C82
Title: Show WorkingMemory on Debug screen

Goal:
Display the current working memory values on the Debug screen.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../debug/DebugScreen.kt
- android-brain/brain/src/main/java/.../memory/*

Implementation steps:
1. Query or observe working memory from the Debug screen.
2. Render its current fields clearly.
3. Keep the UI stable when values are null or empty.

Definition of Done:
- Debug screen shows real working memory values.
- Build succeeds.

How to verify:
- Trigger detections and inspect the Debug screen.

Build command:
- ./gradlew assembleDebug
```

## C83

```md
Task ID: C83
Title: Add Recent Interactions memory card

Goal:
Show a card listing the 5 most recent saved interactions from the event store.

Read first:
- docs/development_roadmap.md
- docs/07_robot_memory_system.md

Files likely touched:
- android-brain/app/src/main/java/.../home/HomeScreen.kt
- android-brain/app/src/main/java/.../home/MemoryCards.kt
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Query recent saved events.
2. Render them as a compact card on Home or Debug.
3. Keep the UI empty-state safe.

Definition of Done:
- A Recent Interactions card shows real event data.
- Build succeeds.

How to verify:
- Trigger a few events and inspect the card.

Build command:
- ./gradlew assembleDebug
```

## C84

```md
Task ID: C84
Title: Add Top Persons memory card

Goal:
Show a card listing the top known persons by familiarity.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../home/MemoryCards.kt
- android-brain/memory/src/main/java/.../db/PersonDao.kt

Implementation steps:
1. Query top persons by familiarity.
2. Render them in a compact card.
3. Keep empty state clear.

Definition of Done:
- Top Persons card shows real data.
- Build succeeds.

How to verify:
- Train at least one person and inspect the card.

Build command:
- ./gradlew assembleDebug
```

## C85

```md
Task ID: C85
Title: Add Recent Objects memory card

Goal:
Show a card listing recently seen known objects.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../home/MemoryCards.kt
- android-brain/memory/src/main/java/.../db/ObjectDao.kt

Implementation steps:
1. Query recent known objects.
2. Render them as a compact card.
3. Keep empty state clear.

Definition of Done:
- Recent Objects card shows real data.
- Build succeeds.

How to verify:
- Teach at least one object and detect it again, then inspect the card.

Build command:
- ./gradlew assembleDebug
```

---

# Batch 14 — Stability and MVP Finalization

## C86

```md
Task ID: C86
Title: Handle camera permission denial gracefully across the app

Goal:
Make sure the app behaves cleanly when camera permission is denied, including clear UI states and no crashes.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../camera/*
- android-brain/app/src/main/java/.../permissions/*

Implementation steps:
1. Review camera access paths.
2. Ensure denied permission state is handled everywhere.
3. Show user-facing guidance where needed.

Definition of Done:
- Denying camera permission does not crash the app.
- Camera UI shows a clear denial state.
- Build succeeds.

How to verify:
- Deny camera permission and navigate through the app.

Build command:
- ./gradlew assembleDebug
```

## C87

```md
Task ID: C87
Title: Handle model load failures gracefully

Goal:
Surface meaningful UI or log state when face or object models fail to load.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/perception/src/main/java/.../*
- android-brain/app/src/main/java/.../camera/*
- android-brain/app/src/main/java/.../debug/*

Implementation steps:
1. Detect model load failures.
2. Surface a clear error state.
3. Avoid crashes and keep the rest of the app usable.

Definition of Done:
- Model failures are visible and do not crash the app.
- Build succeeds.

How to verify:
- Temporarily simulate a missing model file or invalid load path in a controlled way and inspect behavior.

Build command:
- ./gradlew assembleDebug
```

## C88

```md
Task ID: C88
Title: Ensure all main screens have real empty states

Goal:
Review Home, Event Viewer, Persons, Objects, Traits, and Debug screens and ensure all have real, non-crashing empty states.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../*

Implementation steps:
1. Review each screen.
2. Replace any fragile or missing empty-state handling.
3. Keep behavior and data flow unchanged.

Definition of Done:
- No main screen crashes on an empty database or missing state.
- Build succeeds.

How to verify:
- Run a clean install and visit all main screens.

Build command:
- ./gradlew assembleDebug
```

## C89

```md
Task ID: C89
Title: Restore app startup state cleanly from persistence

Goal:
Restore the latest persisted app state needed for Phase 1 startup, including traits and saved entities, without broken flows.

Read first:
- docs/development_roadmap.md

Files likely touched:
- android-brain/app/src/main/java/.../*
- android-brain/brain/src/main/java/.../*
- android-brain/memory/src/main/java/.../*

Implementation steps:
1. Review startup initialization.
2. Restore required persisted state.
3. Ensure the app starts cleanly after prior use.

Definition of Done:
- App starts cleanly after prior sessions with saved data.
- Build succeeds.

How to verify:
- Use the app, save people/objects/events, restart app, inspect main screens.

Build command:
- ./gradlew assembleDebug
```

## C90

```md
Task ID: C90
Title: Run Phase 1 MVP smoke test pass and fix blocking issues

Goal:
Run a full manual smoke test of the Phase 1 MVP and fix any build-safe blocking issues found.

Read first:
- docs/project_manifest.md
- docs/development_roadmap.md
- docs/backlog_master_tasks.md

Files likely touched:
- Any Phase 1 files needed for small blocking fixes only

Implementation steps:
1. Verify avatar, event system, Room persistence, camera preview, face detection, teach person, person recognition, object detection, teach object, brain state loop, traits, and memory cards.
2. Fix only blocking issues discovered during the smoke test.
3. Keep changes localized and avoid refactoring.

Definition of Done:
- Phase 1 smoke test passes end-to-end.
- `./gradlew assembleDebug` succeeds.
- No blocking crashes remain in the covered MVP flow.

How to verify:
- Manual smoke test checklist:
  - app launches
  - navigation works
  - avatar renders and changes state
  - test events work and persist
  - Event Viewer works
  - camera preview works
  - face boxes render
  - face sample capture works
  - Teach Person works
  - recognition works for known vs unknown
  - object label appears
  - Teach Object works
  - brain state changes are visible
  - traits update and persist
  - memory cards show real data

Build command:
- ./gradlew assembleDebug
```

---

# Recommended Execution Order

Run in this exact order unless a specific task depends on already-created code:

1. C1 → C4
2. C5 → C10
3. C11 → C21
4. C22 → C32
5. C33 → C45
6. C46 → C51
7. C52 → C60
8. C61 → C70
9. C71 → C79
10. C80 → C85
11. C86 → C90

---

# Best Practical Starting Slice

Nếu muốn có một vertical slice rất sớm, chạy theo chuỗi này:

* C1
* C2
* C3
* C5
* C6
* C10
* C11
* C12
* C13
* C15
* C17
* C18

Chuỗi này sẽ cho bạn:

* app chạy
* avatar hiện
* event hoạt động
* debug overlay có
* event được lưu DB
* event viewer có dữ liệu thật

---

# Codex Task Final Recommendation

Task đầu tiên vẫn là **C1**.
Task tiếp theo tốt nhất là **C2**.
Sau khi C1 và C2 pass build, chuyển sang **C5** và **C6** thay vì vội nhảy vào camera/AI.
