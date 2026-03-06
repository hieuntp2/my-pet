# Backlog Master Tasks — AI Pet Robot

Version: v1
Purpose: Danh sách task đầy đủ để đưa vào backlog và giao cho Codex/Claude.
Rule: Mỗi task phải đủ nhỏ để sau khi làm xong có kết quả chạy được, build không lỗi, không mock, không để hàm trống.

---

## 0. Global Rules for Every Task

### Done criteria for every task

* Build thành công.
* App hoặc firmware chạy được.
* Có cách nhìn thấy hoặc xác minh kết quả.
* Không để `TODO`, `throw NotImplementedException`, method rỗng, mock logic cốt lõi.
* Không refactor lan sang khu vực không liên quan nếu task không yêu cầu.

### Required output from agent after each task

* Summary of changes
* Files changed
* How to verify
* Build result
* Remaining risks

### Build commands

* Android: `./gradlew assembleDebug`
* Android tests (nếu có): `./gradlew test`
* Firmware: tùy framework của ESP32/Arduino, phải có lệnh build rõ trong repo sau này.

---

# Phase 0 — Repository Foundation

## P0.1 Repository bootstrap

### P0.1.1 Create repository structure

* Tạo cấu trúc thư mục:

  * `docs/`
  * `android-brain/`
  * `robot-body/`
  * `tools/`
* DoD: repo có structure rõ ràng.

### P0.1.2 Add all architecture documents into docs folder

* Copy các file markdown hiện có vào `docs/`.
* DoD: tất cả docs nằm đúng chỗ.

### P0.1.3 Create AGENTS.md

* Viết rule cho Codex/Claude.
* DoD: `AGENTS.md` có source of truth + rules + build command.

### P0.1.4 Add root README

* README mô tả project, phases, repo structure.
* DoD: mở repo là hiểu project.

---

# Phase 1 — Android Pet Brain (Offline First)

## P1.1 Android project skeleton

### P1.1.1 Create Android project in android-brain

* Kotlin + Android app.
* DoD: project build được.

### P1.1.2 Create modules

* Tạo modules:

  * `app`
  * `core-common`
  * `ui-avatar`
  * `brain`
  * `memory`
  * `perception`
* DoD: multi-module build OK.

### P1.1.3 Configure common Gradle versions

* Centralize versions/plugins.
* DoD: sync/build OK.

### P1.1.4 Add basic app theme

* Tạo theme cơ bản, không cần đẹp.
* DoD: app chạy không lỗi theme.

### P1.1.5 Create MainActivity + navigation shell

* Có màn hình chính + điều hướng cơ bản.
* DoD: app mở được và chuyển được giữa ít nhất 2 screen.

## P1.2 Core common layer

### P1.2.1 Add logger utility

* Tạo logger wrapper dùng thật.
* DoD: log hiển thị ở Logcat.

### P1.2.2 Add time provider abstraction

* Tạo time provider thật, không stub.
* DoD: dùng được trong code production.

### P1.2.3 Add result/error wrapper

* Tạo sealed result type cho app.
* DoD: compile OK, dùng được trong ít nhất 1 flow.

## P1.3 Avatar MVP

### P1.3.1 Define AvatarEmotion enum

* HAPPY, CURIOUS, SLEEPY, IDLE.
* DoD: compile OK.

### P1.3.2 Define AvatarState data model

* emotion, intensity, blinkRate, mouthState.
* DoD: compile OK.

### P1.3.3 Create Avatar composable with static face

* Hiển thị mặt cơ bản.
* DoD: mở app thấy mặt robot.

### P1.3.4 Render HAPPY state

* Thay đổi mắt/miệng theo happy.
* DoD: nhìn thấy khác.

### P1.3.5 Render CURIOUS state

* DoD: nhìn thấy khác.

### P1.3.6 Render SLEEPY state

* DoD: nhìn thấy khác.

### P1.3.7 Add manual emotion switch controls

* Buttons/tabs đổi emotion.
* DoD: bấm đổi state được.

### P1.3.8 Add simple blink animation

* Blink chạy thật theo timer.
* DoD: mắt chớp tự động.

### P1.3.9 Add subtle idle animation

* Nhịp thở hoặc scale nhẹ.
* DoD: avatar nhìn “sống” hơn.

## P1.4 Debug shell

### P1.4.1 Create Debug screen

* Có screen riêng cho debug.
* DoD: điều hướng được tới màn debug.

### P1.4.2 Add app info panel

* build version, time, device name.
* DoD: hiển thị thật.

### P1.4.3 Add debug overlay container

* Overlay luôn hiện trên màn chính.
* DoD: overlay hoạt động.

### P1.4.4 Show last event label on overlay

* Tạm lấy từ local state.
* DoD: text đổi khi emit event test.

### P1.4.5 Add frame metrics placeholders only after real pipeline exists

* Task này chỉ tạo chỗ hiển thị, không fake giá trị.
* DoD: UI có vùng metrics, giá trị trống hợp lý.

## P1.5 Event system

### P1.5.1 Define EventEnvelope model

* eventId, type, ts, payloadJson.
* DoD: compile OK.

### P1.5.2 Define EventType enum v1

* APP_STARTED, TEST_EVENT, CAMERA_FRAME_RECEIVED, FACE_DETECTED...
* DoD: compile OK.

### P1.5.3 Create EventBus interface

* publish/observe.
* DoD: compile OK.

### P1.5.4 Implement in-memory EventBus

* SharedFlow hoặc tương đương.
* DoD: publish rồi observe được thật.

### P1.5.5 Emit APP_STARTED event on app launch

* DoD: nhìn thấy event ở log.

### P1.5.6 Add Emit Test Event action in UI

* DoD: bấm nút là event phát ra.

### P1.5.7 Connect overlay to EventBus

* last event hiển thị từ EventBus thật.
* DoD: bấm test event thấy overlay đổi.

## P1.6 Event persistence

### P1.6.1 Add Room dependency and setup database module

* DoD: build OK.

### P1.6.2 Create EventEntity

* Table events.
* DoD: DB compile/migration strategy OK.

### P1.6.3 Create EventDao

* insert/list/clear.
* DoD: compile OK.

### P1.6.4 Create AppDatabase

* DoD: DB init thành công.

### P1.6.5 Create EventStore interface

* save/query.
* DoD: compile OK.

### P1.6.6 Implement RoomEventStore

* dùng DB thật.
* DoD: save/query được.

### P1.6.7 Wire EventBus publishing to EventStore

* mọi event publish được lưu DB.
* DoD: test event xuất hiện trong DB.

### P1.6.8 Create Event Viewer screen from DB

* đọc 50 event gần nhất.
* DoD: event list hiển thị thật.

### P1.6.9 Verify persistence after app restart

* Kill app mở lại vẫn còn event.
* DoD: pass manual verification.

### P1.6.10 Add clear events action

* Xóa DB events thật.
* DoD: clear xong list rỗng.

### P1.6.11 Add export events JSON action

* Tạo file JSON thật.
* DoD: export được và file size > 0.

## P1.7 Camera foundation

### P1.7.1 Add camera permission flow

* Request permission thật.
* DoD: grant/deny đều xử lý đúng.

### P1.7.2 Create Camera screen

* Có preview container.
* DoD: mở screen không crash.

### P1.7.3 Integrate CameraX preview

* DoD: preview chạy trên device.

### P1.7.4 Handle lifecycle start/stop correctly

* Quay lại màn khác rồi mở lại không vỡ camera.
* DoD: pass manual test.

### P1.7.5 Create FrameAnalyzer class

* Analyzer nhận frame thật.
* DoD: analyze được gọi.

### P1.7.6 Log frame size every 1 second

* DoD: log hiện width/height định kỳ.

### P1.7.7 Publish CAMERA_FRAME_RECEIVED event every 1 second

* DoD: Event Viewer thấy event camera.

### P1.7.8 Show last frame width/height in debug overlay

* DoD: overlay cập nhật thật.

### P1.7.9 Measure frame processing time

* DoD: overlay/log có ms.

## P1.8 Face detection

### P1.8.1 Add face detection dependency

* ML Kit hoặc equivalent.
* DoD: build OK.

### P1.8.2 Create FaceDetectionEngine interface

* DoD: compile OK.

### P1.8.3 Implement real FaceDetectionEngine

* detectFaces(frame).
* DoD: có kết quả khi có mặt.

### P1.8.4 Map detector result to app model

* bbox, confidence, trackingId nếu có.
* DoD: compile và dùng được.

### P1.8.5 Draw face bounding boxes on camera preview

* DoD: box hiển thị đúng vị trí tương đối.

### P1.8.6 Publish FACE_DETECTED event on count change

* DoD: event xuất hiện khi có mặt.

### P1.8.7 Show current face count on debug overlay

* DoD: count cập nhật realtime.

### P1.8.8 Add face crop utility

* Crop khuôn mặt từ frame thật.
* DoD: lưu crop được.

### P1.8.9 Add Capture Face Sample button

* Chỉ bật khi có ít nhất 1 mặt.
* DoD: bấm lưu được file crop.

### P1.8.10 Show captured face thumbnail list

* DoD: thumbnails hiển thị thật.

## P1.9 Person memory schema

### P1.9.1 Create PersonEntity table

* personId, displayName, role, createdAt, lastSeenAt, seenCount, familiarity.
* DoD: migration OK.

### P1.9.2 Create PersonDao

* insert/update/list/get.
* DoD: compile OK.

### P1.9.3 Create FaceEmbeddingEntity table

* owner_type PERSON, owner_id, dim, vector_blob...
* DoD: migration OK.

### P1.9.4 Create FaceEmbeddingDao

* insert/listByPerson/listAll.
* DoD: compile OK.

### P1.9.5 Create Persons screen

* list persons from DB.
* DoD: screen mở được.

### P1.9.6 Show empty state for no persons

* DoD: UI rõ ràng.

## P1.10 Face embedding pipeline

### P1.10.1 Add face embedding model asset

* model thật trong project.
* DoD: load model OK.

### P1.10.2 Create FaceEmbeddingEngine interface

* DoD: compile OK.

### P1.10.3 Implement TFLite face embedding engine

* computeEmbedding(bitmap).
* DoD: trả vector thật.

### P1.10.4 Add normalization utility

* L2 normalize vector.
* DoD: unit test hoặc manual numeric check.

### P1.10.5 Show embedding dimension and inference time in UI

* DoD: bấm compute thấy thông tin.

### P1.10.6 Add similarity utility

* cosine similarity.
* DoD: function dùng thật.

### P1.10.7 Add centroid calculation utility

* tính centroid từ nhiều embeddings.
* DoD: dùng được trong recognition.

## P1.11 Teach person flow

### P1.11.1 Create Teach Person screen

* nhập tên + xem face samples.
* DoD: screen hoạt động.

### P1.11.2 Validate minimum sample count

* cần >= 3 face samples.
* DoD: không cho save nếu thiếu.

### P1.11.3 Persist person record

* DoD: save person thành công.

### P1.11.4 Persist embeddings linked to person

* DoD: DB có embeddings thật.

### P1.11.5 Emit USER_TAUGHT_PERSON event

* DoD: event xuất hiện.

### P1.11.6 Return to Persons screen after save

* DoD: thấy person mới.

### P1.11.7 Add person detail screen

* xem stats cơ bản và số embeddings.
* DoD: mở detail được.

## P1.12 Person recognition

### P1.12.1 Build recognition service using saved embeddings

* DoD: compile OK.

### P1.12.2 Recognize current face against known persons

* DoD: đứng trước camera nhận ra đúng người đã dạy.

### P1.12.3 Add threshold config

* threshold thật trong settings/local config.
* DoD: có thể chỉnh và dùng ngay.

### P1.12.4 Publish PERSON_RECOGNIZED event

* DoD: event xuất hiện đúng personId.

### P1.12.5 Update person lastSeenAt and seenCount

* DoD: stats tăng khi nhận ra.

### P1.12.6 Show recognized person label on camera screen

* DoD: UI hiện tên đúng.

### P1.12.7 Handle unknown person state

* DoD: người lạ hiện Unknown.

### P1.12.8 Publish PERSON_UNKNOWN event

* DoD: event xuất hiện khi không match.

## P1.13 Object detection foundation

### P1.13.1 Add object detection model asset

* DoD: model load OK.

### P1.13.2 Create ObjectDetectionEngine interface

* DoD: compile OK.

### P1.13.3 Implement TFLite object detection engine

* detect top labels.
* DoD: có label thật từ camera.

### P1.13.4 Show current top object label on camera screen

* DoD: label realtime.

### P1.13.5 Publish OBJECT_DETECTED event

* DoD: event xuất hiện.

### P1.13.6 Measure object inference time

* DoD: overlay/log có ms.

## P1.14 Object memory

### P1.14.1 Create ObjectEntity table

* canonicalLabel, aliasName, createdAt, lastSeenAt, seenCount, importance.
* DoD: migration OK.

### P1.14.2 Create ObjectDao

* DoD: compile OK.

### P1.14.3 Create Objects screen

* list objects from DB.
* DoD: screen mở được.

### P1.14.4 Create Teach Object flow

* detect label → nhập alias → save.
* DoD: save object thành công.

### P1.14.5 Emit USER_TAUGHT_OBJECT event

* DoD: event xuất hiện.

### P1.14.6 Update object seen stats when detected again

* DoD: seenCount tăng.

### P1.14.7 Show alias when known object reappears

* DoD: UI hiện alias thay vì label gốc khi phù hợp.

## P1.15 Brain state machine v1

### P1.15.1 Define BrainState enum

* IDLE, CURIOUS, HAPPY, SLEEPY.
* DoD: compile OK.

### P1.15.2 Create BrainStateStore

* state thật trong app lifecycle.
* DoD: state update được.

### P1.15.3 Rule: PersonRecognized -> HAPPY

* DoD: thấy chủ thì avatar happy.

### P1.15.4 Rule: PersonUnknown -> CURIOUS

* DoD: thấy người lạ thì curious.

### P1.15.5 Rule: no stimuli for 60s -> SLEEPY

* DoD: để yên 60s thấy sleepy.

### P1.15.6 Rule: any strong stimulus wakes from SLEEPY

* DoD: xuất hiện mặt hoặc object → wake.

### P1.15.7 Publish BRAIN_STATE_CHANGED event

* DoD: event xuất hiện khi đổi state.

### P1.15.8 Bind brain state to AvatarState

* DoD: avatar phản ứng tự động.

## P1.16 User interaction events

### P1.16.1 Add Pet button in UI

* DoD: bấm được.

### P1.16.2 Publish USER_INTERACTED_PET event

* DoD: event xuất hiện.

### P1.16.3 Rule: PET while CURIOUS -> HAPPY

* DoD: avatar đổi vui.

### P1.16.4 Add Manual Sleep button for testing

* DoD: ép sleepy được.

### P1.16.5 Add Manual Wake button for testing

* DoD: ép wake được.

## P1.17 Personality traits v1

### P1.17.1 Create TraitsSnapshot table

* curiosity, sociability, energy, patience, boldness.
* DoD: migration OK.

### P1.17.2 Create TraitsDao

* insert latest/get latest.
* DoD: compile OK.

### P1.17.3 Create default traits initializer

* DoD: app first run có traits hợp lệ.

### P1.17.4 Create Traits screen

* hiển thị 5 trait hiện tại.
* DoD: mở screen thấy số thật.

### P1.17.5 Add rule: PET increases sociability slightly

* DoD: sau pet traits tăng nhẹ.

### P1.17.6 Add rule: frequent recognition increases familiarity and tiny sociability

* DoD: stats thay đổi theo thời gian.

### P1.17.7 Add rule: no interaction reduces energy slightly

* DoD: energy giảm sau idle dài.

### P1.17.8 Persist new trait snapshots on change

* DoD: restart app vẫn giữ latest.

### P1.17.9 Publish TRAITS_UPDATED event

* DoD: event xuất hiện.

## P1.18 Person relationship modeling

### P1.18.1 Define familiarity update formula

* DoD: formula chạy thật.

### P1.18.2 Update familiarity on recognized session

* DoD: familiarity tăng có kiểm soát.

### P1.18.3 Update familiarity on PET linked to current person

* DoD: current person score tăng.

### P1.18.4 Show familiarity in Persons list

* DoD: visible.

### P1.18.5 Sort persons by familiarity

* DoD: list sắp đúng.

## P1.19 Working memory and memory cards

### P1.19.1 Create WorkingMemory model

* currentPersonId, currentObjectId, lastStimulusTs...
* DoD: compile OK.

### P1.19.2 Update WorkingMemory from events

* DoD: values thay đổi thật.

### P1.19.3 Show working memory on debug screen

* DoD: có dữ liệu thật.

### P1.19.4 Create Recent Interactions card

* đọc từ DB events.
* DoD: card hiển thị 5 event gần nhất.

### P1.19.5 Create Top Persons card

* DoD: card hiển thị top familiarity.

### P1.19.6 Create Recent Objects card

* DoD: card hiển thị object vừa thấy.

## P1.20 Stability and polish for offline MVP

### P1.20.1 Handle camera permission denial state gracefully

* DoD: app không crash.

### P1.20.2 Handle model load failure gracefully

* DoD: hiển thị error rõ ràng.

### P1.20.3 Handle empty DB states on all screens

* DoD: không màn nào trắng/lỗi.

### P1.20.4 Add app startup restoration

* restore latest traits and brain state defaults.
* DoD: app start ổn định.

### P1.20.5 Add release-safe logging strategy

* DoD: debug logs hợp lý.

### P1.20.6 Verify full Phase 1 MVP build and manual smoke test

* DoD: checklist smoke test pass.

---

# Phase 2 — Cloud AI / Conversation Layer

## P2.1 Foundation

### P2.1.1 Add settings screen for AI provider config

* DoD: nhập config được.

### P2.1.2 Add secure local config loading for API keys

* DoD: không hardcode key.

### P2.1.3 Create Chat screen shell

* list message + input.
* DoD: UI hoạt động.

## P2.2 Provider integration

### P2.2.1 Create LLMClient interface

* DoD: compile OK.

### P2.2.2 Implement OpenAI client or Gemini client first

* chỉ chọn 1 provider đầu tiên.
* DoD: gửi prompt nhận response.

### P2.2.3 Add network timeout handling

* DoD: lỗi timeout không crash.

### P2.2.4 Add invalid key handling

* DoD: báo lỗi rõ.

### P2.2.5 Add rate-limit handling

* DoD: 429 được xử lý rõ ràng.

## P2.3 Chat flow

### P2.3.1 Persist chat messages locally

* DoD: mở lại app còn lịch sử chat.

### P2.3.2 Show loading state while awaiting AI response

* DoD: UX rõ ràng.

### P2.3.3 Add cancel request action

* DoD: cancel hoạt động.

## P2.4 Context builder

### P2.4.1 Build recent events context pack

* top 3–5 event gần nhất.
* DoD: context string/json tạo được thật.

### P2.4.2 Build traits context pack

* DoD: traits đưa vào prompt thật.

### P2.4.3 Build current person context pack

* recognized person + familiarity.
* DoD: có data thật khi nhận diện được người.

### P2.4.4 Combine context packs into prompt builder

* DoD: prompt hoàn chỉnh build được.

## P2.5 Structured AI output

### P2.5.1 Define AI response schema model

* intent, utterance, emotion, suggestedAction.
* DoD: compile OK.

### P2.5.2 Request structured JSON output from provider

* DoD: parse được trong case bình thường.

### P2.5.3 Add schema validation

* DoD: JSON sai được reject/fallback.

### P2.5.4 Add fallback plain text handling

* DoD: app vẫn usable nếu parse fail.

## P2.6 Intent bridge

### P2.6.1 Define Intent enum v1

* GREET, IDLE_CHAT, ANSWER_QUESTION.
* DoD: compile OK.

### P2.6.2 Map AI intent to BrainAction

* DoD: code chạy thật.

### P2.6.3 Drive avatar emotion from AI output

* DoD: chat xong avatar đổi cảm xúc.

### P2.6.4 Log AI decision event

* DoD: Event Viewer thấy AI decision.

## P2.7 Speech I/O

### P2.7.1 Integrate Android TextToSpeech

* DoD: app nói được.

### P2.7.2 Add Speak AI response action

* DoD: phản hồi AI được đọc ra.

### P2.7.3 Add basic speech toggle setting

* DoD: bật/tắt được.

### P2.7.4 Integrate speech-to-text input option

* DoD: nói vào mic thành text.

## P2.8 Memory-aware conversation

### P2.8.1 Add memory recall card for chat context

* DoD: chat dùng recent memory thật.

### P2.8.2 Add semantic facts memory table

* user facts/preferences.
* DoD: DB có facts.

### P2.8.3 Support user phrase “remember that ...”

* lưu fact thật.
* DoD: lần sau AI tham chiếu lại được.

### P2.8.4 Add fact review screen

* DoD: xem facts đã lưu.

## P2.9 Phase 2 stability

### P2.9.1 Handle no internet state

* DoD: offline gracefully.

### P2.9.2 Disable AI features when key missing

* DoD: UI rõ ràng.

### P2.9.3 Smoke test Phase 2 end-to-end

* DoD: chat + memory + avatar reaction hoạt động.

---

# Phase 3 — Physical Body / ESP32 / Arduino

## P3.1 Firmware foundation

### P3.1.1 Create robot-body firmware project

* ESP32 preferred.
* DoD: firmware build được.

### P3.1.2 Add serial logging

* DoD: boot log thấy được.

### P3.1.3 Add config header for pins and features

* DoD: pin mapping tập trung 1 chỗ.

## P3.2 BLE communication

### P3.2.1 Define BLE service UUIDs

* DoD: compile OK.

### P3.2.2 Implement BLE peripheral advertising

* DoD: thấy device bằng scanner.

### P3.2.3 Implement command characteristic write

* DoD: nhận write từ app.

### P3.2.4 Implement telemetry notify characteristic

* DoD: gửi notify được.

### P3.2.5 Add Android BLE client connect screen

* DoD: app connect/disconnect được.

### P3.2.6 Add BLE reconnect handling

* DoD: mất kết nối nối lại được.

## P3.3 Protocol v1

### P3.3.1 Define command model on Android

* MOVE, SERVO, LED.
* DoD: compile OK.

### P3.3.2 Define parser on firmware

* parse command thật.
* DoD: nhận đúng command.

### P3.3.3 Implement ACK response

* DoD: Android thấy ack.

### P3.3.4 Implement error response for invalid command

* DoD: invalid command không treo firmware.

## P3.4 LED and servo bring-up

### P3.4.1 Wire single LED control

* DoD: Android điều khiển LED được.

### P3.4.2 Add LED mode command

* OFF/ON/BLINK.
* DoD: 3 mode chạy thật.

### P3.4.3 Wire first servo control

* DoD: Android chỉnh góc được.

### P3.4.4 Add servo angle clamping

* DoD: quá ngưỡng không làm hỏng servo.

## P3.5 Motor bring-up

### P3.5.1 Integrate motor driver

* DoD: firmware điều khiển motor cơ bản.

### P3.5.2 Implement forward/backward/stop

* DoD: robot chạy tiến/lùi/dừng.

### P3.5.3 Implement left/right rotation

* DoD: robot quay trái/phải.

### P3.5.4 Add movement timeout safety

* DoD: hết TTL tự stop.

## P3.6 Sensor feedback

### P3.6.1 Integrate distance sensor

* DoD: đọc được cm/mm thật.

### P3.6.2 Send distance telemetry to Android

* DoD: app hiển thị distance realtime.

### P3.6.3 Integrate battery telemetry

* DoD: app thấy battery value.

### P3.6.4 Integrate bumper or cliff sensor if available

* DoD: event sensor gửi được.

## P3.7 Safety layer

### P3.7.1 Auto stop when obstacle too close

* DoD: robot dừng thật.

### P3.7.2 Notify obstacle event to Android

* DoD: event/log có obstacle.

### P3.7.3 Add emergency stop button on app

* DoD: bấm E-stop dừng ngay.

### P3.7.4 Add watchdog on firmware

* DoD: mất command lâu thì stop.

## P3.8 Brain drives body

### P3.8.1 Create ActionExecutor on Android

* map BrainAction -> BLE command.
* DoD: compile OK.

### P3.8.2 Implement APPROACH action

* forward slow trong thời gian ngắn.
* DoD: trigger được từ app.

### P3.8.3 Implement TURN_TO_FACE action

* quay nhẹ theo face offset.
* DoD: khi lệch mặt robot quay nhẹ.

### P3.8.4 Implement IDLE_WIGGLE action

* servo/LED animation đơn giản.
* DoD: có idle body motion.

### P3.8.5 Connect PersonRecognized -> APPROACH or HAPPY motion

* DoD: thấy chủ thì body phản ứng.

## P3.9 Phase 3 stability

### P3.9.1 Manual end-to-end BLE smoke test

* DoD: connect, move, servo, LED, sensor pass.

### P3.9.2 Manual brain-to-body smoke test

* DoD: recognition → action pass.

### P3.9.3 Document calibration settings

* DoD: có file/config rõ ràng.

---

# Cross-cutting Backlog

## X1 Testing

### X1.1 Add unit tests for cosine similarity

### X1.2 Add unit tests for centroid calculation

### X1.3 Add unit tests for trait update rules

### X1.4 Add unit tests for familiarity formula

### X1.5 Add unit tests for protocol parser

## X2 Security and privacy

### X2.1 Add local data delete action for persons and embeddings

### X2.2 Add export user data action

### X2.3 Add warning text for biometric data usage

### X2.4 Encrypt sensitive local data if implemented later

## X3 Tooling

### X3.1 Add code formatting config

### X3.2 Add CI build for Android assembleDebug

### X3.3 Add firmware build command documentation

## X4 Docs maintenance

### X4.1 Keep changelog for major architecture changes

### X4.2 Add implementation notes after each milestone

### X4.3 Update project_manifest.md when scope changes

---

# Milestone Mapping

## Milestone M1

* P1.1 to P1.7

## Milestone M2

* P1.8 to P1.12

## Milestone M3

* P1.13 to P1.16

## Milestone M4

* P1.17 to P1.20

## Milestone M5

* P2.1 to P2.9

## Milestone M6

* P3.1 to P3.9

---

# Recommended Execution Order for Codex

1. Phase 0 foundation
2. P1.1 → P1.7
3. P1.8 → P1.12
4. P1.13 → P1.20
5. Phase 2 only after Phase 1 MVP stable
6. Phase 3 only after offline brain stable

---

# Final Note

This backlog is intentionally granular.
Do not batch too many tasks into one Codex request.
Best practice: 1 task per request, or at most 2 very small related tasks.
