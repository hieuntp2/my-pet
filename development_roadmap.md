# Development Roadmap — AI Pet Robot (Android Brain → Cloud AI → Body)

Version: v1
Principle: **Mỗi task xong là chạy được, build không lỗi, không để hàm trống / mock.**

---

## 0) Definition of Done (DoD) cho mọi task

Một task được coi là DONE khi:

* App **build + run** trên thiết bị/AVD.
* Có **đường đi UI** để kích hoạt và thấy kết quả.
* Không có `TODO`, không có method rỗng, không có mock data cho logic chính.
* Có **log hoặc UI debug** để xác nhận kết quả.
* Nếu có AI inference: hiển thị **thời gian inference** (ms) và trạng thái thành công/thất bại.

---

## 1) Milestones tổng

* **M1 (Phase 1A):** Avatar + Camera + Event Log (app “sống” và có dữ liệu)
* **M2 (Phase 1B):** Face detect + Teach person + nhận diện người (offline)
* **M3 (Phase 1C):** Object detect + Teach object + phản ứng hành vi (offline)
* **M4 (Phase 1D):** Memory Views + Personality Traits + Behavior loop v1
* **M5 (Phase 2):** Cloud AI chat + memory-aware + intent bridge
* **M6 (Phase 3):** BLE link + robot controller + di chuyển + sensor feedback

Thời lượng gợi ý (side project): 10–14 tuần (tùy quỹ thời gian), nhưng mỗi milestone vẫn độc lập.

---

## 2) Phase 1 — Android Pet Brain (Offline)

### Sprint 1 — Project skeleton + Avatar UI + Debug

**Goal:** Có app chạy được, có avatar hiển thị, có màn debug.

**T1.1 — Create repo + Android project + modules**

* Create Android project (Kotlin) và tách module:

  * `app`
  * `ui-avatar`
  * `brain`
  * `memory`
  * `perception`
* Cấu hình Gradle thống nhất version + ktlint (optional).
* DoD: build debug OK.

**T1.2 — Avatar screen (Compose) với 3 trạng thái**

* Tạo `AvatarState` (emotion: HAPPY/CURIOUS/SLEEPY + intensity 0..1).
* UI hiển thị mặt đơn giản (mắt/mồm) thay đổi theo emotion.
* Có nút/toggle để chuyển emotion (không mock logic, chỉ là input UI).
* DoD: chạy app, chuyển trạng thái thấy khác nhau.

**T1.3 — Debug overlay**

* Thêm overlay (có thể là Compose top panel) hiển thị:

  * FPS (tạm thời chỉ cần frame count/second từ UI tick)
  * lastEventType (string)
  * build version
* DoD: overlay hiển thị realtime.

**T1.4 — In-app logger**

* Tạo `LogEvent()` trong `brain` (thực thi thật): lưu event vào RAM list và in Logcat.
* UI có màn `Event Viewer` (list 50 event gần nhất).
* DoD: bấm nút “Emit Test Event” → event xuất hiện trong list + logcat.

---

### Sprint 2 — Room DB + Event Store (source of truth)

**Goal:** Có event log bền vững (tắt app vẫn còn).

**T2.1 — Add Room database module**

* Tạo `AppDatabase` trong module `memory`.
* Tạo bảng `events` (event_id, ts, type, payload_json).
* Tạo DAO: insertEvent, listEvents(limit), clearEvents.
* DoD: app start không crash; migration strategy rõ ràng.

**T2.2 — EventStore implementation (no stubs)**

* Implement `EventStore` interface (save + query) dùng Room.
* `brain.LogEvent()` chuyển sang gọi EventStore (không còn RAM-only).
* UI Event Viewer đọc từ DB.
* DoD: kill app → mở lại vẫn thấy event cũ.

**T2.3 — Export events to JSON file**

* Thêm nút “Export Events” → tạo file JSON trong app storage và hiển thị path + số event.
* DoD: export thành công, file size > 0.

---

### Sprint 3 — CameraX preview + Frame pipeline

**Goal:** Hiển thị camera + có pipeline xử lý frame thật.

**T3.1 — Camera permission + CameraX preview screen**

* Tạo màn `Camera Screen` hiển thị preview.
* DoD: preview chạy trên device.

**T3.2 — Frame Analyzer (real frames)**

* Implement `ImageAnalysis.Analyzer` lấy frame.
* Convert frame sang RGB bitmap hoặc tensor input (tối thiểu: bitmap).
* Emit event `CameraFrameReceived` mỗi 1s (rate-limit) kèm width/height.
* DoD: Event Viewer thấy event camera realtime.

**T3.3 — Performance metrics**

* Log ms/frame conversion.
* Debug overlay hiển thị `lastFrameMs`.
* DoD: thấy số ms thay đổi.

---

### Sprint 4 — Face Detection (offline) + UI visualization

**Goal:** Detect mặt và vẽ bounding box.

**T4.1 — Integrate face detector**

* Chọn 1 detector on-device (ML Kit face detection hoặc TFLite face detector).
* Implement `detectFaces(frame)` trả list bounding boxes.
* DoD: không crash; có kết quả khi có mặt.

**T4.2 — Render bounding boxes**

* Overlay box lên preview.
* Emit event `FaceDetected(count, maxConfidence)` mỗi lần có thay đổi count.
* DoD: nhìn thấy box đúng vị trí.

**T4.3 — Face capture button**

* Thêm nút “Capture Face Sample” khi đang có ít nhất 1 face.
* Lưu ảnh crop mặt vào file, emit event `FaceSampleCaptured(uri)`.
* DoD: file ảnh tồn tại, mở được (tối thiểu hiển thị thumbnail).

---

### Sprint 5 — Face Embedding + Teach Person + Recognition (offline)

**Goal:** Robot nhận ra người quen.

**T5.1 — Add persons + embeddings tables**

* Room tables:

  * `persons(person_id, display_name, created_at, last_seen_at, seen_count, role)`
  * `embeddings(embedding_id, owner_type, owner_id, dim, vector_blob, normalized, created_at)`
* DAOs: insertPerson, listPersons, insertEmbedding, listEmbeddingsByPerson.
* DoD: DB migration OK, app không lỗi.

**T5.2 — Integrate face embedding model**

* Dùng model on-device (FaceNet/MobileFaceNet TFLite).
* Implement `computeEmbedding(faceBitmap): FloatArray`.
* Hiển thị dim + inference ms.
* DoD: bấm “Compute Embedding” → thấy dim đúng + ms.

**T5.3 — Teach Person flow (no stubs)**

* Flow:

  1. user capture 3–5 face samples
  2. nhập tên
  3. app tạo embeddings và lưu
  4. tạo `person` record
* Emit events: `UserTaughtPerson(personId, name, sampleCount)`.
* DoD: Persons screen list ra người vừa tạo.

**T5.4 — Similarity match + Recognize**

* Implement cosine similarity (dot product nếu normalized).
* Match embedding hiện tại với centroid (tính thật từ embeddings đã lưu).
* Nếu vượt threshold → emit `PersonRecognized(personId, confidence)`.
* Update `last_seen_at`, `seen_count`.
* DoD: đứng trước camera → app nhận ra đúng người và update stats.

**T5.5 — Stranger handling**

* Nếu không match → emit `PersonUnknown(confidence)`.
* UI hiển thị “Unknown” rõ ràng.
* DoD: test với người khác/ảnh khác → Unknown.

---

### Sprint 6 — Object Detection + Teach Object + Object Memory

**Goal:** Robot nhận diện vật cơ bản (label) và nhớ alias.

**T6.1 — Integrate object detector (TFLite)**

* Implement detectObjects(frame) trả top-1 label + confidence.
* DoD: hiện label realtime.

**T6.2 — Objects table + DAO**

* Room table `objects(object_id, canonical_label, alias_name, created_at, last_seen_at, seen_count, importance)`
* Implement upsert by canonical_label.
* DoD: object stats lưu bền.

**T6.3 — Teach Object flow**

* Khi detect label, user nhập alias (ví dụ “Bóng đỏ”).
* Lưu object record.
* Emit `UserTaughtObject(objectId, label, alias)`.
* DoD: Objects screen hiển thị alias.

**T6.4 — Object seen update**

* Khi detect label, nếu đã taught → update seen_count + last_seen.
* Emit `ObjectRecognized(objectId, label, confidence)`.
* DoD: đưa vật vào camera → stats tăng.

---

### Sprint 7 — Behavior Loop v1 + Avatar reaction

**Goal:** Có “pet-like loop”: thấy chủ → vui, thấy lạ → tò mò, lâu không ai → buồn ngủ.

**T7.1 — Brain state machine (real transitions)**

* States: IDLE, CURIOUS, HAPPY, SLEEPY.
* Inputs: PersonRecognized/Unknown, ObjectRecognized, NoStimulusTimer.
* Output: AvatarState.
* DoD: state change nhìn thấy rõ trên avatar.

**T7.2 — Stimulus timer**

* Nếu 60s không có Person/Object/Interaction event → vào SLEEPY.
* Nếu có stimulus → rời SLEEPY.
* DoD: để yên 1 phút → sleepy; xuất hiện mặt → wake.

**T7.3 — Interaction input**

* Thêm nút “Pet” (tap) → emit `UserInteracted(type=PET)`.
* Rule: PET khi đang CURIOUS → HAPPY.
* DoD: bấm pet thấy avatar vui.

---

### Sprint 8 — Personality traits v1 + Relationship score

**Goal:** Robot “học” nhẹ theo thời gian.

**T8.1 — Traits model + persistence**

* Table `traits_snapshot(id, ts, curiosity, sociability, energy, patience, boldness)`.
* Load latest snapshot on app start.
* DoD: restart app vẫn giữ traits.

**T8.2 — Trait update rules (no stubs)**

* Rules:

  * UserInteracted(PET) → sociability + small, patience + tiny
  * PersonRecognized(owner) frequent → sociability + tiny
  * Long no interaction → energy down small
* Clamp 0..1.
* Emit `TraitsUpdated` event.
* DoD: xem traits screen thấy thay đổi sau vài tương tác.

**T8.3 — Relationship per person**

* Add to `persons`: familiarity_score.
* Update familiarity theo seen + PET.
* DoD: Persons screen hiển thị familiarity tăng dần.

---

### Sprint 9 — Working memory + Retrieval cards (offline)

**Goal:** Brain truy hồi “mấy lần gần đây” để phản ứng nhất quán.

**T9.1 — Working memory cache**

* Lưu `currentPersonId`, `lastSeenPersonId`, `lastSeenObjectId`, `lastStimulusTs`.
* DoD: debug panel show working memory.

**T9.2 — Memory cards for UI**

* Tạo màn “Memory Cards” hiển thị:

  * last 5 interactions (events)
  * top 5 persons by familiarity
  * last seen objects
* DoD: dữ liệu thật từ DB.

---

## 3) Phase 2 — Cloud AI / Local AI Integration

### Sprint 10 — LLM connector + Chat UI (minimal, production-like)

**Goal:** Chat được, có rate limit, có error handling.

**T10.1 — Secret management**

* Load API key từ local config (không hardcode).
* DoD: chạy được khi có key; thiếu key thì UI báo rõ.

**T10.2 — Chat screen**

* UI: message list + input.
* Implement real network call to OpenAI/Gemini (choose one first).
* DoD: gửi câu hỏi → nhận câu trả lời.

**T10.3 — Rate limit + retry**

* Client side: debounce + max requests/min.
* Handle errors: timeout, 401, 429.
* DoD: lỗi hiển thị rõ, không crash.

---

### Sprint 11 — Memory-aware prompting + Intent bridge

**Goal:** LLM đọc “memory cards” và đề xuất intent.

**T11.1 — Context builder**

* Build prompt context từ:

  * current recognized person
  * top 3 recent events
  * traits snapshot
* DoD: prompt log ra (safe, no secrets).

**T11.2 — Structured output**

* LLM output JSON:

  * intent
  * utterance
  * emotion
  * suggested_action
* Validate JSON; nếu fail → fallback to plain text.
* DoD: 80% responses parse OK (manual testing).

**T11.3 — Intent to BrainAction**

* Map 3 intents đầu tiên:

  * GREET
  * ASK_QUESTION
  * IDLE_CHAT
* Brain quyết định thực thi; update AvatarState.
* DoD: chat → avatar đổi emotion hợp lý.

---

## 4) Phase 3 — Physical Body (ESP32/Arduino)

### Sprint 12 — BLE link + command protocol

**Goal:** Android gửi lệnh thật, MCU nhận và phản hồi.

**T12.1 — MCU firmware: BLE GATT service**

* Define service + characteristics:

  * cmd write
  * telemetry notify
* DoD: dùng app BLE scanner thấy service.

**T12.2 — Android BLE client**

* Connect, discover service, write command.
* UI: connect/disconnect + send test command.
* DoD: gửi lệnh → MCU log/ack.

**T12.3 — Protocol v1**

* Implement commands:

  * MOVE v,w
  * SERVO id,angle
  * LED mode
* Telemetry:

  * DIST cm
  * BATT percent
* DoD: Android nhận telemetry realtime.

---

### Sprint 13 — Mobility + sensors (real movement)

**Goal:** robot chạy được và tránh vật cản đơn giản.

**T13.1 — Motor driver + move basic**

* MCU control motors (PWM).
* Android joystick UI gửi MOVE.
* DoD: robot chạy tiến/lùi/quay.

**T13.2 — Distance sensor integration**

* Read sensor (ToF/ultrasonic) → telemetry.
* DoD: distance hiển thị trên app.

**T13.3 — Safety stop**

* Nếu distance < threshold → MCU auto stop + notify.
* Android emit `ObstacleDetected` event.
* DoD: robot tự dừng trước vật.

---

### Sprint 14 — Brain drives body (pet-like movement)

**Goal:** thấy chủ → tiến gần nhẹ; không thấy → idle roam (tối giản).

**T14.1 — Action executor**

* BrainAction → BLE command.
* Implement:

  * APPROACH (forward slow)
  * TURN_TO_FACE (rotate small)
  * IDLE_WIGGLE
* DoD: nhận diện mặt → robot có phản ứng chuyển động.

**T14.2 — Basic follow mode (optional)**

* Follow face center offset (P-control simple).
* DoD: chủ di chuyển nhẹ → robot quay theo.

---

## 5) Backlog “nice to have” (sau khi M4 ổn định)

* On-device STT/TTS (offline)
* Better object instance recognition
* Episode summarization (daily)
* Skill system (tricks)
* Multi-user roles: owner/household/visitor
* Data encryption for embeddings + delete/export UX

---

## 6) Daily workflow gợi ý (để Codex/Claude làm hiệu quả)

Mỗi lần giao cho agent, dùng format:

* Goal (1 câu)
* Files to touch (list)
* Steps (3–8 bullet)
* DoD (bullet)
* Build command (./gradlew assembleDebug)

Kết thúc mỗi task: agent phải chạy build và báo "build succeeded".
