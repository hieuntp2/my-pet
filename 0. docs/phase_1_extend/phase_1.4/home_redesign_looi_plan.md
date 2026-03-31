# Home Redesign Plan — AI Pet App theo hướng Looi

Version: v1  
Owner: Hieu Le  
Context: Android Pet App / AI Pet Robot  
Target: Redesign toàn bộ Home screen thành trải nghiệm production-ready theo tinh thần **Looi-like**: tối giản, sống động, mặt pet là trung tâm, nền đen, chỉ 1 nút menu debug.

---

## 1. Mục tiêu

Thiết kế lại toàn bộ trang Home hiện tại để chuyển từ kiểu “dashboard app” sang kiểu **living creature screen**.

Home mới phải tạo cảm giác:
- mở app là thấy pet **đang sống sẵn**, không phải một app có nhiều card thông tin
- mọi thứ xoay quanh **một khuôn mặt duy nhất**
- tương tác mượt, hiện đại, pixel nhưng không thô, không demo-like
- có đủ state, animation, expression, micro-interaction để sẵn sàng đi production
- chỉ giữ **1 nút duy nhất trên màn hình** dạng sandwich/ellipsis box để mở Debug

Home mới không được là:
- trang thông tin nhiều panel
- trang có tab Home / Diary / Debug như hiện tại
- trang demo emotion switch
- trang POC animation đơn giản
- trang chỉ phát vài animation random không có state machine

---

## 2. Product direction

### 2.1 Identity

Home phải được định nghĩa như sau:

> Đây là sân khấu sống của pet, không phải dashboard điều khiển ứng dụng.

Khi user mở app:
- không thấy card
- không thấy text block chiếm phần lớn màn hình
- không thấy navigation tabs
- không thấy nhiều control

Thay vào đó chỉ thấy:
- nền đen sâu
- face của pet ở trung tâm
- các motion tinh tế
- đôi khi có bubble/talking text xuất hiện đúng ngữ cảnh
- 1 nút menu nhỏ góc trên phải để mở debug

### 2.2 UX target

Người dùng phải cảm thấy:
- pet luôn có mặt
- pet có ý thức chú ý
- pet chờ tương tác
- pet có mood riêng
- pet phản ứng tức thì khi bị chạm / mở app / có âm thanh / có event nội bộ

---

## 3. UI principles bắt buộc

### 3.1 Layout principles

- Fullscreen black background
- Face là hero element duy nhất
- Không có card dashboard trên Home
- Không có section title kiểu app business
- Không có text tĩnh dài dòng
- Không có thanh tab phía trên như màn hiện tại
- Không có nhiều button trên Home
- Debug phải bị ẩn phía sau 1 nút menu nhỏ

### 3.2 Visual principles

- Phong cách pixel hiện đại, high-fidelity
- Pixel sạch, sắc, ít màu nhưng giàu biểu cảm
- Animation mượt, easing mềm, không giật kiểu sprite lỗi thời
- Màu chủ đạo:
  - Background: near-black / black
  - Face lights: cyan, blue, soft violet, occasional pink accents
  - Warning / surprise accents: amber / coral rất tiết chế
- Tỷ lệ hiển thị face lớn hơn hiện tại rõ rệt
- Face phải có chiều sâu bằng glow, parallax nhẹ, squash/stretch rất nhỏ

### 3.3 Interaction principles

- Tap ngắn: pet acknowledge nhanh
- Hold: pet cuddle / lean / soften gaze
- Không cần button gameplay hiện sẵn trên main face area
- Reaction phải có cooldown và anti-spam
- Mọi animation phải có logic trạng thái, không random vô nghĩa

### 3.4 Production principles

- Tất cả animation phải map từ state machine thật
- Không hardcode switch demo trong Home
- Không tạo nhiều composable rời rạc không có kiến trúc
- Phải có animation catalog rõ ràng
- Phải có event → reaction pipeline
- Phải có priority / interruption rules
- Phải có idle orchestration engine

---

## 4. Mô hình Home mới

## 4.1 Cấu trúc màn hình

```text
[Top-right floating sandwich / ellipsis button]

           [breathing space]

               [Pet Face]
         [eyes only or eyes + tiny accents]

      [context bubble / talking text when needed]

           [ambient particles / subtle glow]
```

## 4.2 Thành phần hiển thị trên Home

### A. Black Stage Background
- Nền đen sâu toàn màn hình
- Có ambient gradient cực nhẹ
- Có vignette rất nhẹ để focus vào face
- Có optional scanline/pixel-noise cực nhẹ nếu cần tăng chất digital creature

### B. Face Layer
- Layer chính
- Chứa eyes, lids, pupils, highlight, expression accents, blush/signal sparks nếu cần
- Có transform nhẹ theo state

### C. Reaction Layer
- Bubble text ngắn
- Tiny hearts / sparks / sleepy z / surprise glyphs
- Chỉ xuất hiện khi thực sự cần

### D. Menu Button Layer
- 1 nút duy nhất ở góc trên phải
- Bấm mở debug screen hoặc debug bottom sheet/navigation
- Style: rounded square / capsule tối, tinh gọn, phù hợp visual black stage

---

## 5. Behavioral architecture cho Home

Home không chỉ là UI, mà là runtime stage nhận đầu vào từ:
- app lifecycle
- pet state
- emotion resolver
- event bus
- audio reaction events
- interaction gestures
- greeting flow
- idle scheduler
- mini game session state

### 5.1 Luồng quyết định

```text
PetState + Conditions + Current Stimulus + Cooldowns + Random Weighted Variation
→ Face State Resolver
→ Animation Controller
→ Home Scene Renderer
→ Optional Talking Bubble / FX Layer
```

### 5.2 Layer trạng thái

Cần phân biệt rõ:

#### Persistent state
- mood
- energy
- hunger
- sleepiness
- social
- bond
- trust / familiarity (nếu có)

#### Scene state
- current face state
- current idle loop
- current focus target
- current micro-expression
- current overlay reaction
- interaction cooldowns

#### Transient reaction state
- tap reaction active
- long-press cuddle active
- greeting active
- audio-react active
- surprise interrupt active
- game active

---

## 6. Face system — chỉ 1 face duy nhất nhưng rất giàu trạng thái

## 6.1 Design philosophy

Chỉ dùng **một face duy nhất** xuyên suốt sản phẩm.

Không làm nhiều mặt khác nhau theo kiểu thay skin. Thay vào đó:
- 1 identity ổn định
- nhiều expression layers
- nhiều eyelid shapes
- pupil behaviors
- squish/stretch / glow / offsets / micro FX

Điều này giúp pet có cá tính nhất quán như Looi.

## 6.2 Base face anatomy

### Core components
- Left eye shell
- Right eye shell
- Eyelids
- Pupils / gaze blocks
- Highlights
- Expression accents
- Optional cheek/light accent
- Tiny mouth/signal slot chỉ dùng cực hạn chế hoặc không dùng

### Shape language
- Rounded digital eyes
- Slight bottom shadow band để tạo depth
- Soft neon top fill
- Dark pupil block nhìn có hồn
- Eye spacing đủ rộng để đọc emotion tốt

---

## 7. Face state catalog

Phần này là source of truth cho animation + visual behavior. Mỗi state phải có:
- trigger
- visual description
- motion description
- enter rule
- hold behavior
- exit rule
- interrupt priority

# 7.1 CORE IDLE STATES

## 7.1.1 IDLE_NEUTRAL
**Mục đích:** trạng thái mặc định khi pet ổn, đang quan sát môi trường.  
**Visual:** mắt mở vừa phải, glow cyan-blue, pupil centered hơi drift nhẹ.  
**Motion:** thở nhẹ 3–5s/chu kỳ, blink thưa, micro sway rất nhỏ.  
**Trigger:** không có nhu cầu nổi bật, không event ưu tiên.  
**Exit:** khi có tap, audio, greeting, boredom shift, sleep/hunger threshold.  
**Priority:** base.

## 7.1.2 IDLE_CURIOUS
**Mục đích:** pet chú ý môi trường, có nhu cầu social nhẹ.  
**Visual:** mắt mở to hơn 6–10%, pupil dịch nhẹ sang trái/phải, highlight sáng hơn.  
**Motion:** gaze dart nhẹ, blink ngắn, head-tilt illusion bằng eye offset.  
**Trigger:** social cao, có sound gần đây, app vừa mở, random weighted curiosity.  
**Exit:** nếu không có gì thêm thì quay về neutral; nếu user tap thì sang engaged states.

## 7.1.3 IDLE_SLEEPY
**Mục đích:** pet mệt / buồn ngủ.  
**Visual:** eyelids hạ thấp, glow dịu, pupil chậm.  
**Motion:** slow blink dài, micro droop, occasional tiny “zzz” accent.  
**Trigger:** sleepiness cao, energy thấp.  
**Exit:** tap mạnh / sound lớn / game mở / state refresh tăng energy.

## 7.1.4 IDLE_HUNGRY
**Mục đích:** pet đói, cần attention kiểu feed/play.  
**Visual:** mắt nhìn mong chờ, pupil slightly up-facing, accent amber-coral rất nhẹ.  
**Motion:** blink mong chờ, tiny nudge pulse như “hey?”.  
**Trigger:** hunger cao.  
**Exit:** feed interaction hoặc decay/state change.

## 7.1.5 IDLE_LONELY
**Mục đích:** pet thiếu tương tác xã hội.  
**Visual:** mắt dịu, hơi cụp, glow yếu hơn, nhìn theo user chậm.  
**Motion:** ít blink hơn, thỉnh thoảng glance away rồi quay lại.  
**Trigger:** social quá thấp trong một khoảng thời gian.  
**Exit:** tap, hold, game, greet, sound-react tích cực.

# 7.2 GREETING STATES

## 7.2.1 GREETING_SOFT
**Dùng khi:** app mở lại sau khoảng ngắn, pet mood ổn.  
**Visual:** mắt sáng dần từ tối → mở ra mềm.  
**Motion:** wake bloom + small upward bounce.  
**Bubble:** “hey”, “oh, bạn đến rồi”, hoặc silent greeting.  
**Exit:** về idle neutral/curious.

## 7.2.2 GREETING_EXCITED
**Dùng khi:** bond cao, xa lâu mới quay lại, social đang thiếu.  
**Visual:** mắt mở lớn, glow mạnh hơn, tiny hearts/spark.  
**Motion:** double bounce + quick blink + eager gaze.  
**Bubble:** vui, ngắn, thân mật.  
**Exit:** engaged idle.

## 7.2.3 GREETING_SLEEPY_WAKE
**Dùng khi:** pet đang sleepy.  
**Visual:** mở mắt chậm, hơi ngái ngủ.  
**Motion:** long blink, nửa mở, stretch illusion.  
**Bubble:** “mmm…”, “mình đang ngủ mà…”.  
**Exit:** idle sleepy hoặc neutral nếu có tương tác.

# 7.3 TOUCH / AFFECTION STATES

## 7.3.1 TAP_ACK
**Dùng khi:** user tap ngắn.  
**Visual:** blink nhanh + pupil focus vào điểm giữa.  
**Motion:** pop reaction 120–220ms, tiny glow pulse.  
**Bubble:** optional “pip”, “hm?”, “hi!”.  
**Exit:** về state trước đó sau cooldown.

## 7.3.2 TAP_HAPPY
**Dùng khi:** pet mood tốt hoặc bond cao.  
**Visual:** mắt cong nhẹ kiểu cười, glow sáng.  
**Motion:** soft bounce, happy blink đôi.  
**FX:** tiny hearts hoặc sparkles.

## 7.3.3 LONG_PRESS_CUDDLE
**Dùng khi:** user hold.  
**Visual:** eyes soften, lids hạ nhẹ, pupils ổn định.  
**Motion:** lean-in illusion, breathing chậm và sâu hơn.  
**Bubble:** “*nuzzle*”, “mmm”, “ở đây nè”.  
**Exit:** thả tay, sau đó settle mềm về idle.

## 7.3.4 OVERSTIMULATED
**Dùng khi:** spam tap liên tục.  
**Visual:** mắt chớp khó chịu, glow giảm hoặc nháy cảnh báo nhẹ.  
**Motion:** brief recoil.  
**Bubble:** “ê từ từ”, “nhiều quá rồi”.  
**Rule:** anti-spam, không để user farm reaction vô hạn.

# 7.4 AUDIO REACTION STATES

## 7.4.1 AUDIO_LISTEN
**Dùng khi:** phát hiện âm thanh vừa phải.  
**Visual:** pupil focus lệch theo hướng giả lập, eyes mở hơn.  
**Motion:** snap attention nhẹ.  
**Exit:** về curious idle nếu không có gì thêm.

## 7.4.2 AUDIO_SURPRISED
**Dùng khi:** sound lớn đột ngột.  
**Visual:** eyes mở to, glow flash nhẹ.  
**Motion:** quick recoil + freeze 150ms.  
**FX:** tiny exclamation pixel.  
**Exit:** curious hoặc cautious idle.

## 7.4.3 AUDIO_CALM_RESPONSE
**Dùng khi:** giọng nói hoặc sound quen, không đe doạ.  
**Visual:** attentive, mềm, thân thiện.  
**Motion:** small nod illusion.  
**Bubble:** “hmm?”, “mình nghe nè”.

# 7.5 NEED / MOOD STATES

## 7.5.1 HUNGRY_PLEAD
**Visual:** mong chờ, hơi buồn, nhìn lên.  
**Motion:** repeat pulse thưa.  
**Bubble:** muốn ăn / đòi attention.

## 7.5.2 SAD_LOW_ENERGY
**Visual:** glow yếu, lids hạ, movement chậm.  
**Motion:** almost still, blink dài.  
**Use:** khi energy quá thấp / social xuống sâu.

## 7.5.3 PLAYFUL_BUILDUP
**Visual:** eyes lively, fast micro-shifts.  
**Motion:** anticipatory bounce.  
**Use:** trước khi mời chơi mini game hoặc khi mood rất tốt.

# 7.6 SPECIAL STATES

## 7.6.1 THINKING
**Dùng khi:** pet đang resolve event, small pause trước reaction, hoặc debug visible intelligent pause.  
**Visual:** pupil drift pattern, asymmetric lids nhẹ.  
**Motion:** subtle orbit or scan.  
**Use:** rất tiết chế.

## 7.6.2 GLITCH_CUTE
**Dùng khi:** easter egg hiếm / idle rare moment.  
**Visual:** pixel flicker controlled.  
**Motion:** 1-frame jitter with recovery.  
**Rule:** hiếm, không phá immersion.

## 7.6.3 SLEEP_TRANSITION
**Dùng khi:** pet từ awake chuyển sang nap state.  
**Visual:** glow hạ, eyelids close dần.  
**Motion:** exhale settle.  
**Exit:** wake events.

## 7.6.4 WAKE_TRANSITION
**Dùng khi:** từ nap sang awake.  
**Visual:** dim → soft light → focus.  
**Motion:** stretch illusion + blink.

---

## 8. Animation system spec

## 8.1 Animation groups

### A. Base loop animations
- breathing / float
- idle gaze drift
- blink variants
- glow pulse

### B. Expression animations
- happy widen
- sleepy droop
- curious tilt illusion
- sad soften
- hungry plead

### C. Reaction animations
- tap ack
- cuddle lean
- surprise recoil
- greeting bloom
- audio listen snap

### D. FX overlays
- hearts
- sparks
- sleepy z
- exclamation glyph
- tiny bubble pop

## 8.2 Blink catalog

Không dùng 1 blink duy nhất. Cần ít nhất:
- normal blink
- quick blink
- double blink
- slow sleepy blink
- one-eye micro blink rất hiếm

## 8.3 Gaze catalog
- center hold
- slight left look
- slight right look
- upward hopeful look
- slow scanning look
- snap-to-center focus

## 8.4 Motion quality rules
- animation duration phải rõ spec
- easing thống nhất
- không abrupt nếu không phải surprise
- idle movement biên độ rất nhỏ
- reaction movement ngắn, readable, không quá lố

---

## 9. Home talking message system

Không quay lại kiểu nhiều card text như hiện tại. Chỉ dùng **contextual talking message**.

### 9.1 Rules
- text ngắn
- chỉ hiện khi có lý do
- tự biến mất
- không chắn mặt
- không trở thành log viewer

### 9.2 Types
- greeting line
- reaction line
- need line
- game invite line
- affectionate line
- sleepy line

### 9.3 Examples
- “oh, bạn tới rồi”
- “*nuzzle*”
- “hơi đói rồi đó…”
- “chơi với mình không?”
- “nghe thấy gì đó…”
- “buồn ngủ ghê…”

---

## 10. Mini game system cho Home

Game không được làm kiểu app chuyển sang màn khác hoàn toàn trừ khi cần. Ưu tiên game bám quanh face.

## 10.1 Mục tiêu
- tăng bond
- tạo loop quay lại
- tạo state change thật
- tận dụng face animation hiện có

## 10.2 Game principles
- ngắn
- dễ hiểu
- phản hồi nhanh
- phù hợp pet identity
- không phá black-stage aesthetic

## 10.3 MVP game proposals

### Game 1 — Catch the Spark
- Những spark nhỏ xuất hiện quanh face
- User tap đúng spark trước khi biến mất
- Pet reaction theo kết quả
- Win: happy/excited, bond+, social+
- Miss: curious/sad nhẹ

### Game 2 — Follow My Eyes
- Pet nhìn theo một hướng ngắn rồi “giấu” mục tiêu
- User tap theo hướng/pattern đúng
- Dùng gaze animation như core mechanic

### Game 3 — Calm Pet
- Khi pet overstimulated hoặc anxious nhẹ, user giữ nhịp hold đúng thời gian để calm pet
- Tạo bonding feel tốt

### Game 4 — Feed Timing
- Hạt thức ăn / light pellet rơi chậm, user tap đúng timing
- Hợp với hungry flow

## 10.4 Game integration rules
- Game start từ idle/game invite state
- Có cooldown
- Không spam liên tục
- Game thắng/thua phải ảnh hưởng pet state thật
- Game phải dùng lại face expression system, không tạo UI lệch tone

---

## 11. Navigation + debug access

## 11.1 Trên Home chỉ có 1 nút

- Nút duy nhất: sandwich/ellipsis box
- Góc trên phải
- Style dark glass / dark pill / soft border
- Size vừa đủ bấm, không làm hỏng thẩm mỹ

## 11.2 Khi bấm
Có thể chọn 1 trong 2 hướng:
- mở thẳng Debug screen
- mở mini menu bottom sheet với mục Debug / Diary

Khuyến nghị:
- nếu mục tiêu Home cực sạch, vẫn giữ 1 nút và mở **Menu Sheet**
- trong sheet mới có Debug, Diary, Event Viewer nếu cần

Nhưng trên Home chỉ được hiển thị 1 nút.

---

## 12. Kiến trúc kỹ thuật đề xuất

## 12.1 Các khối cần có

### HomeSceneState
Nguồn trạng thái render của Home, gồm:
- currentFaceState
- currentBubble
- currentFx
- currentGameState
- menuVisible
- sceneBrightness/glow parameters
- currentInteractionMode

### FaceStateResolver
Nhận vào:
- PetState
- PetConditions
- recentEvents
- currentInteraction
- audioState
- cooldowns

Trả ra:
- FaceStateSpec

### HomeAnimationController
- điều phối enter / hold / exit
- interruption rules
- blend idle với reaction
- quản lý queued reactions

### IdleDirector
- chọn idle variant theo thời gian và mood
- không để pet đứng im quá lâu
- có weighted randomness có kiểm soát

### InteractionController
- tap/hold gesture
- cooldowns
- anti-spam
- affection score hooks

### TalkingMessageController
- chọn message ngắn đúng context
- timeout
- dedupe spam

### MiniGameController
- state machine riêng cho game
- integration với pet state

---

## 13. Interruption / priority rules

Mọi animation đều phải obey priority:

### P0 — Critical interrupt
- app open greeting
- loud sound surprise
- major state transition

### P1 — Direct user interaction
- tap
- long press
- game interaction

### P2 — Need expression
- hungry
- sleepy
- lonely

### P3 — ambient idle
- blink
- gaze drift
- subtle idle pulse

Rule:
- P0 cắt P2/P3
- P1 cắt P2/P3
- P2 có thể thay idle nhưng không nên cắt trực tiếp P1 giữa chừng
- bubble text phải obey cooldown riêng

---

## 14. Visual production spec

## 14.1 Typography
- tối giản
- bubble text mềm, hiện đại
- nếu dùng pixel font thì chỉ accent, không dùng toàn UI nếu readability kém
- ưu tiên readable modern font + pixel visual assets

## 14.2 Pixel spec
- vẽ theo grid nhất quán
- retina-friendly scaling
- không blur asset pixel
- nếu dùng vector để giả pixel thì phải đảm bảo snap rõ

## 14.3 Performance spec
- animation 60fps nếu thiết bị cho phép
- tránh recomposition thừa
- asset/state pipeline tối ưu
- không để Home lag vì debug logic

## 14.4 Accessibility / usability
- nút menu đủ touch target
- nền đen nhưng contrast bubble/text đủ rõ
- animation không flicker quá mạnh

---

## 15. Điều phải xoá khỏi Home hiện tại

Bắt buộc loại bỏ khỏi Home:
- top tabs Home / Diary / Debug
- title Buddy to, subtitle dạng app profile
- card Greeting
- card How Buddy is doing
- card Today with your pet
- kiểu bố cục nhiều rounded cards xếp dọc
- text giải thích tĩnh dưới face như demo instruction
- toàn bộ feel “admin card app”

Những thứ này có thể chuyển:
- sang Diary screen
- sang Debug screen
- sang menu sheet
- hoặc biến thành contextual bubble / timeline riêng ngoài Home

---

## 16. Kết quả mong muốn sau redesign

Sau khi xong, Home phải đạt:
- nhìn phát là khác hẳn app cũ
- không còn cảm giác dashboard
- pet là trung tâm tuyệt đối
- chỉ 1 face duy nhất nhưng rất giàu cảm xúc
- background đen kiểu premium robot pet
- animation mượt, có chiều sâu, có cá tính
- có gameplay nhẹ tích hợp tự nhiên
- production-ready, không phải demo

---

## 17. Prompt đầy đủ cho Claude — triển khai theo plan này

```md
Read and follow `AGENTS.md` first.

Then read these files first:
- docs/project_manifest.md
- docs/development_roadmap.md
- docs/06_personality_engine.md
- docs/07_robot_memory_system.md
- docs/08_audio_interaction_architecture.md
- docs/09_pet_app_definition_full.md
- this plan file: docs/home_redesign_looi_plan.md

Mission:
Redesign the entire Home experience of the Android Pet App into a production-ready, Looi-inspired living pet screen.

Non-negotiable goals:
- Home must no longer look like a dashboard or debug app.
- Use a fullscreen black-stage UI.
- Keep only one pet face identity across the app.
- The face must be the central interactive element.
- Keep only one visible button on Home: a small sandwich / ellipsis menu button that opens Debug (directly or through a minimal menu sheet).
- Remove Home tabs/cards/info-panel style UI from the Home screen.
- Do not build a POC, demo, or placeholder animation system.
- Build a production-ready state-driven animation architecture.
- Add a mini-game layer integrated with pet state and reactions.

Required implementation direction:
1. Rebuild Home around a single face scene on a black background.
2. Create a proper Home scene state model.
3. Create a face-state resolver driven by PetState, derived conditions, recent events, interactions, and cooldowns.
4. Implement a structured animation catalog for the face states defined in the plan.
5. Implement idle orchestration so the pet is always subtly alive.
6. Implement direct user interaction gestures: tap and long-press.
7. Implement contextual talking-message overlays, not dashboard cards.
8. Implement a mini-game integrated into Home without breaking the black-stage aesthetic.
9. Keep debug visibility available through the single menu button, not as visible dashboard content.
10. Keep everything build-safe, modular, and consistent with the offline-first event-driven architecture.

Strict scope rules:
- Do not silently redesign unrelated app flows outside what is needed for Home, menu access, and the required supporting architecture.
- Do not add placeholder TODOs or fake production logic.
- Do not keep old Home cards hidden below the fold; remove/replace the old structure cleanly.
- Do not keep manual emotion-switch demo controls on the production Home surface.
- Do not use random animations without state meaning.

Expected technical output:
- Production-ready Home composables and state models
- Face animation system with clear state mapping
- Interaction handling and cooldown rules
- Menu button flow for Debug access
- Mini-game integration
- Updated Home behavior tied to pet state

Verification expectations:
- App opens to a black-background premium Home scene.
- Only one visible button exists on Home.
- Face reacts to tap, long-press, greeting, and idle state changes.
- Old dashboard/card UI is gone from Home.
- Mini-game can be entered and completed.
- Build succeeds.

Build command:
- ./gradlew assembleDebug

Output exactly:
- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks
```

---

## 18. Review checklist

Dùng checklist này để review sau mỗi lần Claude/Codex sửa:

- Home còn giống dashboard không?
- Có còn tab/card/text block kiểu app cũ không?
- Nền đã là black stage chưa?
- Có chỉ còn 1 nút visible trên Home không?
- Face đã đủ lớn và là trung tâm tuyệt đối chưa?
- Animation có state-driven thật không?
- Idle có luôn sống không?
- Tap / hold có khác nhau rõ không?
- Greeting có đọc được mood không?
- Bubble text có contextual, ngắn, đẹp không?
- Game có hòa vào Home hay bị lệch app-style?
- Debug có bị lộ UI ra Home quá nhiều không?
- Toàn bộ trải nghiệm đã đạt mức production-ready chưa?

