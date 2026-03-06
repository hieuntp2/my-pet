# Codex Tasks — Phase 1 (Android Pet Brain MVP)

Version: v1
Purpose: Các task sẵn để copy trực tiếp vào Codex/Claude. Chỉ giao **1 task mỗi lần**.
Rule bắt buộc:

* Không mock logic cốt lõi
* Không để hàm trống / TODO / throw NotImplementedException
* Task xong phải build được
* Task xong phải có cách verify thấy được

---

## Task Template (dùng cho mọi task về sau)

```md
Task ID: <ID>
Title: <short title>

Goal:
<1 câu rất rõ task này phải làm gì>

Read first:
- docs/project_manifest.md
- docs/development_roadmap.md
- <docs liên quan trực tiếp>

Scope:
- Chỉ làm đúng task này.
- Không refactor lan sang khu vực không liên quan.
- Không thêm mock/stub cho logic production.

Files likely touched:
- <file 1>
- <file 2>
- <file 3>

Implementation steps:
1. <step>
2. <step>
3. <step>

Definition of Done:
- <điều kiện 1>
- <điều kiện 2>
- <điều kiện 3>

How to verify:
- <manual steps>

Build command:
- ./gradlew assembleDebug

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

---

# Batch A — Foundation

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

Scope:
- Only create the Android project and module structure.
- Do not implement business logic yet.
- Do not add placeholder production code beyond what is required to compile.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
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

Scope:
- Only create the runnable shell and minimal navigation.
- Do not implement camera, database, or AI yet.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
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

Scope:
- Keep the implementation small and production-usable.
- No fake implementations.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

---

# Batch B — Avatar MVP

## C4

```md
Task ID: C4
Title: Create AvatarEmotion and AvatarState models

Goal:
Create the core avatar models in `ui-avatar`: `AvatarEmotion` and `AvatarState`.

Read first:
- docs/project_manifest.md
- docs/06_personality_engine.md
- docs/development_roadmap.md

Scope:
- Only define the models cleanly for production use.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

## C5

```md
Task ID: C5
Title: Render a static avatar on the Home screen

Goal:
Display a basic robot face on the Home screen using `AvatarState`.

Read first:
- docs/project_manifest.md
- docs/development_roadmap.md

Scope:
- Render a simple but real avatar.
- No animation yet.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

## C6

```md
Task ID: C6
Title: Add manual avatar emotion switching

Goal:
Allow the user to switch avatar emotion manually on the Home screen and see visual changes immediately.

Read first:
- docs/project_manifest.md
- docs/development_roadmap.md

Scope:
- Only manual controls.
- No brain logic yet.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

## C7

```md
Task ID: C7
Title: Add blink animation to avatar

Goal:
Add a real blink animation to the avatar that runs automatically.

Read first:
- docs/project_manifest.md
- docs/development_roadmap.md

Scope:
- Only blink animation.
- Do not add unrelated animation systems.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

---

# Batch C — Debug and Event System

## C8

```md
Task ID: C8
Title: Create Debug screen and app info panel

Goal:
Create a usable Debug screen that shows app info such as build version and current time.

Read first:
- docs/development_roadmap.md

Scope:
- Only create the screen and real info display.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

## C9

```md
Task ID: C9
Title: Define event models and EventBus interface

Goal:
Create the event foundation with `EventType`, `EventEnvelope`, and `EventBus` interface.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Scope:
- Foundation only.
- Do not add database yet.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

## C10

```md
Task ID: C10
Title: Implement in-memory EventBus and publish test events

Goal:
Implement a working in-memory EventBus and allow the UI to emit and observe test events.

Read first:
- docs/development_roadmap.md
- docs/02_android_pet_brain_architecture.md

Scope:
- Only in-memory event transport.
- No persistence yet.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

## C11

```md
Task ID: C11
Title: Add debug overlay showing last event

Goal:
Add an overlay on the Home screen that shows the latest event type from the real EventBus.

Read first:
- docs/development_roadmap.md

Scope:
- Only show the latest event.
- Do not add persistence or event list yet.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

---

# Batch D — Room Event Persistence

## C12

```md
Task ID: C12
Title: Add Room database and events table

Goal:
Set up Room in the `memory` module and create a persistent `events` table.

Read first:
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Scope:
- Only database setup and events table.
- Do not wire the EventBus yet.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

## C13

```md
Task ID: C13
Title: Implement Room-backed EventStore

Goal:
Create a real `EventStore` implementation backed by Room for saving and querying events.

Read first:
- docs/07_robot_memory_system.md
- docs/development_roadmap.md

Scope:
- Only EventStore implementation.
- No UI changes beyond what is needed to compile.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

## C14

```md
Task ID: C14
Title: Persist published events to EventStore

Goal:
Wire the real EventBus so every published event is also saved to the Room-backed EventStore.

Read first:
- docs/development_roadmap.md
- docs/07_robot_memory_system.md

Scope:
- Only event persistence wiring.
- Keep the current EventBus behavior working.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

## C15

```md
Task ID: C15
Title: Create Event Viewer screen backed by Room

Goal:
Create a real Event Viewer screen that reads the latest events from Room and displays them.

Read first:
- docs/development_roadmap.md

Scope:
- Use real DB data only.
- Do not use mock lists.

Files likely touched:
- android-brain/app/src/main/java/.../debug/EventViewerScreen.kt
- android-brain/memory/src/main/java/.../events/*

Implementation steps:
1. Create a query/use-case to fetch the latest events.
2. Render them in a simple list.
3. Link the Event Viewer from the Debug screen.

Definition of Done:
- The Event Viewer displays real saved events.
- App restart does not lose prior events.
- Build succeeds.

How to verify:
- Emit events.
- Open Event Viewer.
- Kill app and reopen, then confirm events still appear.

Build command:
- ./gradlew assembleDebug

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

---

# Batch E — Camera and Frame Pipeline

## C16

```md
Task ID: C16
Title: Add camera permission flow and Camera screen shell

Goal:
Add camera permission handling and create a Camera screen that opens without crashing.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/development_roadmap.md

Scope:
- Only permission and screen shell.
- No CameraX preview yet.

Files likely touched:
- android-brain/app/src/main/java/.../camera/CameraScreen.kt
- android-brain/app/src/main/java/.../permissions/*
- AndroidManifest.xml

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

## C17

```md
Task ID: C17
Title: Integrate CameraX preview

Goal:
Show a real CameraX preview on the Camera screen.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/development_roadmap.md

Scope:
- Only preview.
- No analysis yet.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

## C18

```md
Task ID: C18
Title: Add FrameAnalyzer and log frame size

Goal:
Implement a real frame analyzer that logs frame width and height once per second.

Read first:
- docs/02_android_pet_brain_architecture.md
- docs/development_roadmap.md

Scope:
- Only analysis hook and logging.
- No ML inference yet.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

## C19

```md
Task ID: C19
Title: Publish CAMERA_FRAME_RECEIVED events from analyzer

Goal:
Publish a real `CAMERA_FRAME_RECEIVED` event from the analyzer at a rate-limited interval.

Read first:
- docs/development_roadmap.md
- docs/07_robot_memory_system.md

Scope:
- Only event publishing from the analyzer.
- Keep rate-limited behavior.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

## C20

```md
Task ID: C20
Title: Show frame metrics on debug overlay

Goal:
Display the latest real frame width, height, and processing time on the debug overlay.

Read first:
- docs/development_roadmap.md

Scope:
- Only surface existing real metrics.
- Do not fake values.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

---

# Final Codex Task Recommendation

Nếu bắt đầu ngay hôm nay, task đầu tiên nên giao là **C1**.

Lý do:

* Đây là nền của toàn bộ project.
* Không đụng AI/model sớm.
* Kết quả kiểm chứng rất rõ: multi-module project build thành công.
* Sau C1 → C2 → C4 → C5 → C9 → C10 là bạn sẽ có một vertical slice đầu tiên chạy được.

## Codex Task Final (copy trực tiếp)

```md
Task ID: C1
Title: Create Android multi-module project skeleton

Goal:
Create the Android project under `android-brain/` with modules `app`, `core-common`, `ui-avatar`, `brain`, `memory`, and `perception`, and make sure the project builds successfully.

Read first:
- docs/project_manifest.md
- docs/development_roadmap.md
- docs/02_android_pet_brain_architecture.md

Scope:
- Only create the Android project and module structure.
- Do not implement business logic yet.
- Do not add placeholder production code beyond what is required to compile.
- Do not create mock logic.
- Do not leave TODO or empty production methods.

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

Output required:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```
