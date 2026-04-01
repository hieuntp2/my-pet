# PET APP DEFINITION — AI PET (ANDROID, OFFLINE-FIRST)

Version: v2  
Owner: Hieu Le  
Project: AI Pet / Android Pet Brain  
Status: Product Definition for MVP + future expansion  

---

## 1. Document Purpose

Tài liệu này định nghĩa **đầy đủ sản phẩm Pet App** để AI coding agent, planner agent, hoặc con người có thể đọc và hiểu rõ:

- app này là gì
- app này **không phải** là gì
- trải nghiệm cốt lõi cần được bảo vệ là gì
- hệ thống nào là bắt buộc cho MVP
- dữ liệu nào cần tồn tại
- các epic nên được chia như thế nào
- điều kiện nào để coi là “MVP hoàn chỉnh”

Tài liệu này được viết để dùng làm **source of truth ở mức sản phẩm** cho Android Pet App, đồng thời vẫn bám định hướng của project hiện tại:

- Android là brain và face
- offline-first
- event-driven
- Room persistence
- personality + memory + behavior là trọng tâm
- pet là một **digital creature**, không phải chatbot hay assistant  fileciteturn0file1 fileciteturn0file9

---

## 2. Product Summary

AI Pet là một **digital pet sống trên điện thoại Android**.

Pet có:

- avatar biểu cảm
- trạng thái nội tại thay đổi theo thời gian
- phản ứng với tương tác của người dùng
- ký ức về các sự kiện đã xảy ra
- phản ứng âm thanh ngắn kiểu thú cưng
- vòng đời hằng ngày
- tính cách thay đổi dần theo cách được nuôi

Pet không được thiết kế để trở thành:

- trợ lý ảo kiểu Siri / Google Assistant
- chatbot hỏi gì cũng trả lời
- công cụ productivity
- app AI hội thoại thuần túy

**Định nghĩa đúng:**

> Đây là một sinh vật số có cảm xúc, ký ức, phản ứng, và tiến hóa chậm theo tương tác.

---

## 3. Product Vision

### 3.1 Vision statement

Tạo một digital pet trên Android khiến người dùng cảm thấy:

- pet đang “sống”
- pet có phản ứng tức thì và dễ thương
- pet thay đổi theo thời gian
- pet nhớ những gì đã xảy ra
- pet có lý do để được quay lại mỗi ngày

### 3.2 Emotional target

Người dùng không mở app để “dùng chức năng”.
Người dùng mở app để:

- nhìn pet sống động
- chạm và thấy pet phản ứng
- nghe pet phát âm thanh ngắn
- thấy pet hôm nay khác hôm qua
- xem lại các khoảnh khắc đã diễn ra
- nuôi pet theo phong cách riêng

### 3.3 Product philosophy

Nguyên lý lớn nhất của sản phẩm là:

> **Believability first. Intelligence second.**

Pet đáng yêu và có hồn quan trọng hơn pet “trả lời thông minh”.
Điều tạo ra cảm giác có hồn là:

- phản ứng nhanh
- biểu cảm rõ
- trạng thái có continuity
- memory có ý nghĩa
- personality thay đổi chậm nhưng thật

Điều này phù hợp với định hướng hiện tại của project manifest, personality engine, memory system, và Android pet brain architecture. fileciteturn0file9 fileciteturn0file4 fileciteturn0file5

---

## 4. Product Goals

## 4.1 Primary goals

Pet App phải đạt được các mục tiêu chính sau:

1. **Pet feels alive immediately**
   - mở app là thấy pet đang sống
   - không được cảm giác “màn hình tĩnh chờ người dùng làm gì đó”

2. **Instant interaction**
   - người dùng có thể tương tác ngay bằng tap hoặc action đơn giản
   - phản ứng phải đủ nhanh để tạo cảm giác thật

3. **Clear feedback loop**
   - mỗi interaction nên dẫn đến ít nhất 3 lớp phản hồi:
     - visual
     - audio hoặc animation
     - state change hoặc memory event

4. **Return value over time**
   - quay lại sau vài giờ hoặc ngày khác phải thấy pet khác đi
   - không được là app “mỗi lần mở ra giống hệt nhau”

## 4.2 Secondary goals

1. Pet dần hình thành tính cách riêng
2. Pet có diary / memory để tạo cảm xúc gắn bó
3. Pet có kiến trúc đủ tốt để mở rộng sang robot body sau này
4. Pet có debug visibility tốt để dễ phát triển bằng AI agent

---

## 5. Non-Goals

Những thứ dưới đây **không thuộc MVP** và không được kéo vào sớm nếu chưa có yêu cầu rõ:

- full speech conversation
- LLM chat assistant
- cloud sync bắt buộc
- multiplayer / social pet
- economy phức tạp / in-app currency
- object detection nâng cao kiểu scene understanding
- full owner household model
- ASR/TTS conversation loop hoàn chỉnh
- physical robot control

Nếu đưa các phần này vào quá sớm, app sẽ bị loãng trọng tâm và mất cái “pet core loop”.

---

## 6. Target User and User Motivation

## 6.1 Core user

Người dùng mục tiêu là người muốn một trải nghiệm:

- nhẹ nhàng
- cảm xúc
- có tính chăm nuôi
- có yếu tố “bonding”
- tương tác ngắn nhưng thường xuyên

## 6.2 Why users return

Người dùng quay lại vì:

- pet hôm nay khác hôm qua
- pet nhớ tương tác trước đó
- pet có dấu hiệu buồn ngủ / đói / vui / bám người
- pet phản ứng dễ thương khi được chạm hoặc được nghe thấy
- có diary để xem lại “kỷ niệm”

---

## 7. Product Identity

## 7.1 What the pet is

Pet là một **creature-like system** gồm:

- internal state
- behavior selection
- memory logging
- expressive avatar
- sound reactivity
- daily lifecycle
- personality drift

## 7.2 What the pet is not

Pet không phải:

- “assistant có hình mặt thú cưng”
- app demo animation
- app mini-game thuần túy
- event log viewer trá hình

## 7.3 Design rule

Mọi feature mới phải trả lời được câu hỏi:

> Feature này có làm pet cảm thấy sống hơn, đáng yêu hơn, hoặc có continuity hơn không?

Nếu không, feature đó không nên chen vào MVP.

---

## 8. Core Experience Loop (Most Important)

Đây là loop quan trọng nhất của toàn bộ sản phẩm.

```text
Open App
   ↓
Pet reacts immediately (greeting + current state)
   ↓
User interacts (tap / activity / sound)
   ↓
Pet reacts (animation + audio + state change)
   ↓
Memory recorded
   ↓
Internal state evolves (mood / bond / needs / traits)
   ↓
User leaves app
   ↓
Time passes
   ↓
Decay + daily lifecycle update
   ↓
User returns
   ↓
Pet is meaningfully different
```

### 8.1 Why this loop matters

Nếu loop này chạy tốt, app có hồn.
Nếu loop này hỏng, mọi tính năng khác đều trở nên vô nghĩa.

### 8.2 Minimum conditions for the loop to feel real

- mở app phải có phản ứng ngay
- interaction phải có phản hồi thấy được
- state phải thay đổi thật, không chỉ diễn animation
- event phải được lưu
- quay lại sau thời gian phải thấy state mới

---

## 9. Core Capability Map

## 9.1 Avatar & Expression System

Avatar là “gương mặt” của pet.
Đây là lớp đầu tiên tạo cảm giác sống.

### Required capabilities

- idle animation
- blink
- emotion rendering
- greeting reaction
- tap reaction animation
- sound reaction animation
- visual variation theo internal state

### Minimum emotion set for MVP

- IDLE
- HAPPY
- CURIOUS
- SLEEPY
- SAD
- EXCITED
- HUNGRY

### Rules

- emotion không được random vô nghĩa
- emotion phải có nguyên nhân từ state hoặc event
- cùng một state không nhất thiết lúc nào cũng y hệt, nên có weighted variation nhỏ

### Success condition

Người dùng nhìn avatar phải đoán được pet đang:

- vui
- buồn ngủ
- cần tương tác
- tò mò

---

## 9.2 Pet State System (Heart of the Product)

Đây là hệ thống quan trọng nhất sau core loop.

Pet phải có trạng thái nội tại thật, không chỉ animation bề ngoài.

### Core state fields

- `mood` — trạng thái cảm xúc tổng quát
- `energy` — năng lượng hiện tại
- `hunger` — mức đói
- `sleepiness` — độ buồn ngủ
- `social` — nhu cầu tương tác
- `bond` — độ gắn bó với user
- `lastUpdatedAt` — thời điểm cập nhật cuối

### Suggested ranges

- energy: 0–100
- hunger: 0–100
- sleepiness: 0–100
- social: 0–100
- bond: non-negative score hoặc bounded score theo design

### Derived states

Từ core state có thể suy ra:

- hungry
- sleepy
- bored
- playful
- attached
- overstimulated

### State behavior rules

- state phải decay theo thời gian
- interaction phải thay đổi state
- activities phải thay đổi state
- state phải ảnh hưởng emotion mapping
- state phải ảnh hưởng audio / greeting / behavior selection

### Example

- energy thấp + sleepiness cao → sleepy greeting
- hunger cao → hungry mood bias
- social thấp → pet kém hứng thú
- bond cao → greeting thân mật hơn

---

## 9.3 Interaction System

Interaction là cổng chính giữa user và pet.

### MVP inputs

- tap pet
- long press pet
- app open
- idle presence
- sound detection
- activity button actions

### Future inputs

- camera-based person presence
- face recognition
- object recognition
- keyword detection

### Required outputs per interaction

Mỗi interaction nên có tối thiểu:

- animation hoặc visual reaction
- audio reaction hoặc silent expressive feedback
- state update
- memory event

### Rules

- mỗi interaction có cooldown
- pet không được spam phản ứng liên tục
- interaction giống nhau lặp quá nhanh nên giảm hiệu ứng hoặc bị ignore có kiểm soát
- bond và mood nên bị ảnh hưởng khác nhau theo loại interaction

### Example interaction effects

- tap nhẹ → mood +1, social +1, bond +1
- play action → mood +3, energy -4, social +5, bond +2
- feed action → hunger -20, mood +2, bond +1
- long press lúc pet đang sleepy → có thể bị “khó chịu nhẹ” hoặc phản ứng chậm

---

## 9.4 Audio Reaction System

Audio là lớp giúp pet bớt “câm” và có phản xạ sinh học.

### MVP audio scope

- nghe tiếng động ở mức energy/VAD-light
- phát event âm thanh
- phát clip ngắn phản ứng bằng SoundPool hoặc hệ tương đương
- tránh tự kích bởi tiếng của chính pet

### Capability levels

- Level 1: sound-reactive
- Level 2: keyword-reactive
- Level 3: command-reactive
- Level 4: conversational

MVP chỉ cần Level 1, có extension path cho Level 2. Điều này bám theo tài liệu audio interaction architecture hiện có. fileciteturn0file13

### Example behavior

- có tiếng gần mic → pet quay sang + “hmm?”
- user tap → pet phát “pip!” / “mew!”
- greeting open app → phát 1 short clip phù hợp mood

### Audio rules

- clip phải ngắn
- có cooldown
- không overlap loạn
- audio phải phục vụ cảm giác pet, không biến thành assistant voice UI

---

## 9.5 Memory System

Pet phải nhớ các sự kiện đủ để tạo continuity.

### Memory types for MVP

1. **Interaction memory**
   - pet was tapped
   - pet was fed
   - pet reacted to sound

2. **Daily summary**
   - hôm nay pet được chơi bao nhiêu lần
   - pet vui hay mệt
   - hunger/energy trend

3. **Notable moments**
   - first feed of the day
   - long absence return
   - mood extremes
   - special reaction chains

### Storage model

- Event log trong Room DB
- no raw audio/image storage by default
- derived memory views được build từ event stream

### UI surfaces

- diary timeline
- memory cards
- last notable moments

### Rules

- memory phải đọc được bởi UI
- memory phải ảnh hưởng được đến feeling của user
- memory không nên chỉ là technical logs

### Guiding principle

> Event log là source of truth, diary là human-readable emotional layer.

Điều này phù hợp với memory architecture hiện tại của project. fileciteturn0file5

---

## 9.6 Daily Lifecycle System

Pet phải “sống theo thời gian”, không đứng yên.

### Required mechanics

- `lastActiveTimestamp`
- app open computes delta time
- decay được áp dụng theo thời gian đã trôi qua
- greeting được tạo từ state mới sau decay

### Example decay rules

- energy giảm theo thời gian
- hunger tăng theo thời gian
- sleepiness tăng theo thời gian
- social giảm nếu user vắng lâu
- mood bị bias theo unmet needs

### Important behavior

Khi user quay lại sau nhiều giờ:

- pet phải có state khác
- greeting phải phản ánh state này
- diary có thể ghi “you were away for a while” kiểu nhẹ nhàng

### Anti-pattern

Không được update state chỉ theo “tick trong lúc app mở”.
Phải support delta time on open.

---

## 9.7 Activities System

Activities là các action rõ ràng mà user chủ động làm với pet.

### MVP activities

- Feed
- Play
- Rest

### Optional small extensions

- Toy interaction
- Gentle petting mode
- Wake / nap toggle

### Activity effects

Mỗi activity phải:

- thay đổi state
- tạo event
- có phản hồi visual
- thường có phản hồi audio
- tăng hoặc giảm bond tùy context

### Example

#### Feed
- hunger giảm mạnh
- mood cải thiện nếu pet đang đói
- memory event created

#### Play
- social tăng
- mood tăng
- energy giảm
- bond tăng

#### Rest
- energy hồi dần
- sleepiness giảm dần
- pet chuyển mood dịu hơn

---

## 9.8 Personality System

Personality không phải thứ đổi liên tục theo từng interaction.
Nó phải thay đổi chậm và tạo khác biệt về xu hướng hành vi.

### Trait set for MVP

- playful
- lazy
- curious
- social

### Mechanism

Traits tăng/giảm chậm theo repeated patterns:

- chơi nhiều → playful tăng
- nghỉ nhiều → lazy tăng
- phản ứng nhiều với sound/new events → curious tăng
- tương tác thường xuyên → social tăng

### Personality effects

Traits không trực tiếp thay state core, mà bias:

- greeting selection
- reaction weighting
- activity preference
- idle animation tendency
- frequency of certain behaviors

### Rule

Personality phải là **slow drift**, không phải immediate state.

### Example

Hai pet đều energy 70 và mood happy:

- pet playful sẽ dễ phản ứng ham chơi hơn
- pet lazy sẽ phản ứng êm hơn, ưu tiên rest-like behavior hơn

Điều này bám với định hướng personality engine trong repo. fileciteturn0file4

---

## 9.9 Teach / Recognition (Optional V1)

Khả năng dạy người quen / nhận diện khuôn mặt là hướng mở rộng tốt, nhưng không phải trọng tâm cứng của Pet App MVP nếu mục tiêu là hoàn thiện pet loop trước.

### If included in V1+

Capabilities có thể gồm:

- teach person
- recognize face
- greet known person
- relationship bias

### Product rule

Recognition không được phá focus của pet loop.
Nếu thêm, nó phải làm pet “sống hơn”, không biến app thành demo computer vision.

---

## 10. Functional Requirements

## 10.1 On app open

Khi mở app, hệ thống phải:

1. load persisted pet profile
2. load persisted pet state
3. compute time delta từ lần active gần nhất
4. apply lifecycle decay
5. derive current mood / derived flags
6. choose greeting reaction
7. render avatar
8. optionally play short greeting clip
9. log open-related event(s)

## 10.2 On tap interaction

Khi user tap pet, hệ thống phải:

1. check cooldown
2. select tap reaction based on current state + traits
3. update visual expression
4. optionally play clip
5. update mood/social/bond or other relevant state
6. persist event
7. refresh UI state

## 10.3 On activity action

Khi user feed/play/rest, hệ thống phải:

1. validate action is allowed
2. apply state changes
3. render response
4. write event
5. update memory view if needed

## 10.4 On sound reaction

Khi audio perception detect sound:

1. emit sound event
2. brain/behavior decide whether to react
3. if react, trigger avatar/audio response
4. apply optional state effect
5. persist event

## 10.5 On returning after absence

Sau khi user vắng lâu:

1. decay phải đủ để tạo khác biệt
2. greeting phải phản ánh absence impact
3. diary có thể hiển thị notable moment
4. pet không được quay về trạng thái mặc định một cách giả tạo

---

## 11. State Model

## 11.1 PetProfile

```json
{
  "id": "pet-001",
  "name": "Cún",
  "createdAt": "2026-03-17T08:00:00Z",
  "bondBase": 0
}
```

## 11.2 PetState

```json
{
  "mood": "HAPPY",
  "energy": 70,
  "hunger": 30,
  "sleepiness": 20,
  "social": 80,
  "bond": 12,
  "lastUpdatedAt": "2026-03-17T08:30:00Z"
}
```

## 11.3 TraitState

```json
{
  "playful": 0.62,
  "lazy": 0.18,
  "curious": 0.51,
  "social": 0.74,
  "lastRecomputedAt": "2026-03-17T08:30:00Z"
}
```

## 11.4 InteractionEvent

```json
{
  "type": "PET_TAPPED",
  "timestamp": "2026-03-17T08:35:10Z",
  "payload": {
    "source": "USER",
    "cooldownBypassed": false,
    "reaction": "HAPPY_BOUNCE"
  },
  "effect": {
    "bondDelta": 1,
    "moodDelta": 1,
    "socialDelta": 1
  }
}
```

## 11.5 DailySummary

```json
{
  "date": "2026-03-17",
  "feedCount": 2,
  "playCount": 4,
  "restCount": 1,
  "tapCount": 13,
  "soundReactionCount": 6,
  "dominantMood": "HAPPY",
  "notableMoments": [
    "Long return greeting",
    "Very playful evening"
  ]
}
```

---

## 12. UX Surface Definition

## 12.1 Home Screen

Home là trung tâm trải nghiệm.

### Required sections

- pet avatar lớn, là trọng tâm
- state indicators đơn giản
- quick actions: Feed / Play / Rest
- optional subtle debug toggle in dev builds
- last notable memory teaser hoặc mini diary card

### UX rule

Home screen phải ưu tiên “pet presence”, không biến thành dashboard nặng số liệu.

## 12.2 Diary / Memory Screen

### Purpose

Biến event history thành trải nghiệm cảm xúc.

### Required content

- timeline events readable by human
- daily summaries
- notable moments

### UX rule

Không hiển thị raw technical payload là lớp chính cho user.
Technical event viewer có thể tồn tại ở debug, nhưng diary phải thân thiện.

## 12.3 Activity Controls

- phải đơn giản
- chạm 1 phát là kích hoạt được
- không cần menu sâu

## 12.4 Debug Visibility

Trong dev build, hệ thống phải cho thấy:

- current state
- last event
- audio metrics nếu có
- persistence status
- recent event list

Điều này phù hợp với triết lý debuggability là feature trong tài liệu AGENTS và roadmap. fileciteturn0file8 fileciteturn0file10

---

## 13. Architecture Constraints

Những ràng buộc dưới đây là bắt buộc giữ nguyên.

### 13.1 Offline-first

- app phải chạy được core loop khi không có mạng
- state, memory, diary, reactions core không phụ thuộc cloud

### 13.2 Event-driven

- interaction quan trọng phải phát event
- audio perception phải phát event
- memory nên xây từ event stream

### 13.3 Modular

- app không được dồn hết logic vào UI layer
- tách rõ avatar / brain / memory / perception / app shell

### 13.4 Real persistence

- dữ liệu survive restart phải đi qua Room hoặc persistence thật
- không mô phỏng persistence bằng in-memory list

### 13.5 No fake production logic

- không TODO ở core flow
- không mock core behavior để “cho xong UI”

### 13.6 Debuggable

- phải có cách nhìn thấy state/event thực tế

Các constraint này bám đúng tài liệu dự án hiện có. fileciteturn0file8 fileciteturn0file9 fileciteturn0file10

---

## 14. Verification Philosophy

Mỗi feature của Pet App chỉ được coi là hoàn thành khi nó đạt đủ càng nhiều càng tốt trong các tiêu chí sau:

- **nhìn thấy được** — UI / animation / visible reaction
- **nghe được** — audio clip / sound response nếu có áp dụng
- **lưu được** — event / state / memory được persist
- **thay đổi được** — state hoặc trait thực sự đổi
- **quay lại thấy được** — restart hoặc quay lại sau thời gian có continuity

### Example

Một tap interaction tốt phải verify được rằng:

- avatar đổi biểu cảm
- có event được lưu
- state có thay đổi
- quay màn khác rồi quay lại không mất state mới

---

## 15. MVP Scope (Locked)

## 15.1 MVP includes

### Core systems

- avatar sống động
- pet state system + time decay
- tap interaction + reaction
- audio reaction cơ bản
- memory log + simple diary
- daily lifecycle update
- basic activities: feed / play / rest
- bond progression cơ bản
- personality traits cơ bản

### Supporting systems

- Room persistence
- event viewer / debug visibility
- greeting on app open
- state indicators cơ bản

## 15.2 MVP excludes

- full speech / ASR / TTS loop
- LLM chat
- cloud sync bắt buộc
- complex virtual economy
- multiplayer / social sharing
- advanced recognition as core dependency
- advanced object detection as MVP blocker
- physical robot body

---

## 16. Epic Breakdown

## Epic A — Pet Core Identity

### Goal

Biến app thành một pet có danh tính cơ bản.

### Includes

- create/load pet profile
- home screen pet presence
- greeting on open
- pet naming

### Done when

- app mở ra là thấy pet ngay
- pet có tên / profile tồn tại
- greeting chạy theo state

---

## Epic B — Pet State Simulation

### Goal

Tạo trái tim state cho pet.

### Includes

- state model
- state persistence
- decay engine
- derived states
- state indicators

### Done when

- state survive restart
- time delta affect state
- UI phản ánh state chính

---

## Epic C — Interaction System

### Goal

Cho phép user tương tác trực tiếp và thấy phản ứng.

### Includes

- tap detection
- long press behavior
- cooldown
- reaction mapping
- bond update
- event persistence

### Done when

- tap tạo phản ứng rõ
- repeated tap được kiểm soát
- interaction tạo event và state change

---

## Epic D — Audio Reaction

### Goal

Làm pet “không câm”.

### Includes

- mic permission
- audio capture foundation
- sound energy / VAD-light
- pre-recorded clip playback
- arbitration / cooldown
- self-trigger suppression

### Done when

- pet nghe được tiếng động
- pet phản ứng audio/visual ổn định
- audio events được persist

---

## Epic E — Daily Lifecycle

### Goal

Làm pet sống theo thời gian.

### Includes

- lastActiveTimestamp
- delta time compute
- decay application
- greeting logic by absence
- daily state shift

### Done when

- quay lại sau vài giờ pet khác trước
- greeting phản ánh actual state

---

## Epic F — Memory & Diary

### Goal

Tạo continuity có thể xem lại.

### Includes

- event to memory mapping
- diary timeline
- daily summary
- notable moment rules

### Done when

- user xem được lịch sử meaningful
- diary không chỉ là technical log

---

## Epic G — Activities

### Goal

Cho user chủ động chăm pet.

### Includes

- feed
- play
- rest
- activity effects
- activity memories

### Done when

- mỗi activity có phản ứng, effect, persistence

---

## Epic H — Personality & Progression

### Goal

Làm pet thay đổi theo cách được nuôi.

### Includes

- trait model
- trait update rules
- behavior weighting
- differentiated reactions

### Done when

- lối nuôi khác nhau dẫn đến xu hướng phản ứng khác nhau

---

## Epic I — Recognition (Later)

### Goal

Bổ sung khả năng nhớ người / greeting contextual.

### Includes

- teach person
- recognition
- known-person greeting
- relationship memory

### Done when

- pet nhận ra người quen và phản ứng phù hợp

---

## Epic J — Productization

### Goal

Đưa app đến mức usable hơn như một sản phẩm thực tế.

### Includes

- onboarding
- settings
- privacy info
- backup/restore local options nếu cần sau
- polish / stability

---

## 17. Behavior Design Rules

## 17.1 Reactions must be state-aware

Không được phản ứng kiểu hardcoded tách rời state.
Ví dụ:

- pet sleepy thì tap reaction dịu/chậm hơn
- pet hungry thì greeting có chút needy hơn
- pet high bond thì greeting ấm áp hơn

## 17.2 Reactions must not be identical every time

Nên có variation nhẹ theo weighted selection.

## 17.3 Behavior must be bounded

- không spam audio
- không flicker emotion loạn
- không update bond quá dễ gây inflation

## 17.4 Personality must bias, not dominate

Traits chỉ bias lựa chọn, không nên override hoàn toàn logic cơ bản.

---

## 18. Memory Design Rules

## 18.1 Store enough to reconstruct experience

Nên lưu:

- event type
- timestamp
- relevant payload
- state effect snapshot nếu cần

## 18.2 Do not store unnecessary sensitive raw data

- no raw audio by default
- no raw image by default cho Pet App MVP

## 18.3 Memory should support both machine and human views

- machine view: event log
- human view: diary cards / summaries

## 18.4 Important memories should be derived

Không phải mọi event đều là memorable moment.
Nên có rules để tạo notable moments từ event patterns.

---

## 19. Audio Design Rules

## 19.1 Audio is emotional, not conversational in MVP

Clip ngắn để tạo cảm giác pet.
Không dùng audio để thay thế conversation system.

## 19.2 Avoid noise fatigue

- có cooldown
- tránh phát quá dày
- ưu tiên ít nhưng đúng lúc

## 19.3 Playback and behavior must stay aligned

Nếu có clip phản ứng, avatar và mood nên phản ánh cùng ngữ cảnh.

---

## 20. Data Persistence Requirements

### Must persist

- pet profile
- pet state
- trait state hoặc sufficient source data to recompute
- interaction events
- activity events
- audio perception/reaction events cần thiết
- daily summaries nếu đã generate

### Should persist

- last notable memories
- last greeting context

### Must survive

- app restart
- process death
- normal lifecycle changes

---

## 21. Success Criteria for MVP

Pet App được coi là **MVP hoàn chỉnh** khi đồng thời đạt các điều kiện sau:

1. **Open app → pet reacts immediately**
2. **Tap pet → pet reacts with visible feedback**
3. **Tap/activity changes internal state for real**
4. **At least one audio reaction path works reliably**
5. **Return after hours → pet state is meaningfully different**
6. **Diary / memory history is visible**
7. **Pet is not silent and not static**
8. **State and events survive restart**
9. **App is stable and does not crash in normal flows**
10. **Core pet loop works offline**

---

## 22. Product Quality Bar

## 22.1 A feature is not done if

- chỉ có UI mà không có state change
- chỉ có animation mà không có persistence
- chỉ có event log mà user không cảm nhận được gì
- chỉ có debug logic mà không có actual pet experience

## 22.2 A feature is good when

- vừa thấy được
- vừa lưu được
- vừa ảnh hưởng behavior/state
- vừa support continuity

---

## 23. Suggested AI-Agent Implementation Guidance

Tài liệu này cũng được viết để AI agent có thể dùng trực tiếp khi planning hoặc coding.

### Every implementation task should preserve

- pet core loop
- offline-first architecture
- event-driven persistence
- modular boundaries
- no fake production logic

### Every task should answer

- task này thay đổi phần nào của pet loop?
- task này tạo visible result gì?
- task này persist cái gì?
- task này ảnh hưởng state gì?
- task này verify bằng cách nào?

### Preferred task shape

Một task tốt nên là vertical slice nhỏ, ví dụ:

- tap input → reaction mapping → state update → event persistence → visible UI
- feed action → hunger update → diary event → home UI refresh
- app open → delta decay → greeting selection → avatar response

Điều này phù hợp với AGENTS.md và backlog strategy đang có. fileciteturn0file8 fileciteturn0file7

---

## 24. Final Definition

Nếu phải tóm gọn toàn bộ Pet App chỉ bằng một đoạn:

> AI Pet là một digital creature offline-first trên Android, có avatar biểu cảm, trạng thái nội tại, ký ức sự kiện, phản ứng âm thanh, vòng đời theo thời gian, và tính cách thay đổi chậm theo cách được nuôi. Trải nghiệm cốt lõi không phải là nói chuyện với AI, mà là nuôi một sinh vật số có continuity, cảm xúc, phản ứng, và kỷ niệm.

---

## 25. One-Line Product Test

Một câu test nhanh để xem sản phẩm có đi đúng hướng không:

> Nếu người dùng mở app, chạm vào pet, nghe pet phản ứng, quay lại sau vài giờ và thấy pet khác trước, rồi xem lại diary thấy lịch sử có ý nghĩa — thì Pet App đang đúng.

