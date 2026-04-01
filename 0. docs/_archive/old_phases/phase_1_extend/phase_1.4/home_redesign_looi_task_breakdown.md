# Task Breakdown — Home Redesign theo hướng Looi

Version: v1  
Purpose: Bộ task breakdown đủ chi tiết để giao Claude/Codex thực thi từng bước, build-safe, không scope creep, bám đúng plan redesign Home theo Looi-like direction.

---

## Cách dùng tài liệu này

- Chỉ giao **1 task hoặc 1 batch nhỏ mỗi lần**.
- Sau mỗi task, agent phải report:
  - Summary of changes
  - Files changed
  - How to verify
  - Build result
  - Remaining risks
- Task sau phải đọc phần **Remaining risks** của task trước và chỉ dùng nó để tinh chỉnh implementation, không được đổi nhiệm vụ chính.
- Không giữ lại UI dashboard cũ dưới bất kỳ hình thức “tạm thời nhưng ẩn đi”.
- Mọi thay đổi phải giữ build xanh.

---

## Global constraints

### Goal
Biến Home hiện tại thành một màn pet sống động kiểu Looi:
- nền đen
- 1 face duy nhất
- chỉ 1 nút visible
- animation state-driven
- mini game tích hợp
- production-ready

### Must not do
- không làm POC
- không thêm manual emotion demo controls ra Home
- không giữ tab Home / Diary / Debug trên Home
- không giữ các card info cũ
- không dùng animation random vô nghĩa
- không bỏ trống logic điều phối animation hoặc interaction

### Read first for almost all tasks
- docs/project_manifest.md
- docs/development_roadmap.md
- docs/06_personality_engine.md
- docs/07_robot_memory_system.md
- docs/08_audio_interaction_architecture.md
- docs/09_pet_app_definition_full.md
- docs/home_redesign_looi_plan.md

---

# Batch H1 — Home foundation reset

## H1-01 — Audit current Home and define migration boundary

### Goal
Xác định chính xác những composable, state, navigation entry, và UI blocks hiện tại của Home cần bị thay thế hoặc di chuyển ra khỏi Home.

### Scope
- Chỉ audit và lên boundary rõ ràng trong code comments / internal notes / implementation plan output.
- Chưa redesign full UI.

### What this task changes
- xác định cấu trúc Home cũ
- xác định file nào phải thay
- xác định phần nào chuyển sang Debug / Diary / menu sheet

### Must not do
- chưa xóa sâu logic không liên quan
- chưa refactor rộng sang module khác nếu chưa cần

### Expected outputs
- danh sách file Home hiện tại
- danh sách composable cần xóa/thay
- danh sách logic nào giữ lại được
- migration path rõ ràng

### Definition of done
- Có boundary rõ để các task sau không làm lan man.
- Build vẫn pass.

### Verification
- Mở app vẫn chạy.
- Có report rõ file và migration plan.

---

## H1-02 — Replace current Home scaffold with black-stage shell

### Goal
Thay toàn bộ layout Home hiện tại bằng shell mới: fullscreen black background + top-right menu button + center stage container.

### Scope
- chỉ dựng shell mới
- chưa cần face animation đầy đủ

### What this task changes
- loại bỏ tab/header/card layout khỏi Home
- tạo stage layout mới
- giữ đường vào Debug qua menu button

### Must not do
- không để lại card cũ ẩn trong scroll
- không thêm text placeholder dài

### Definition of done
- Home mở ra thấy black stage rõ rệt.
- Chỉ có 1 nút visible trên Home.
- Card/tab cũ biến mất khỏi Home.

### Verification
- launch app
- kiểm tra Home không còn giống dashboard
- nút menu bấm được

### Build command
- ./gradlew assembleDebug

---

## H1-03 — Add HomeSceneState and scene contract

### Goal
Tạo model state trung tâm cho Home scene để ngắt Home khỏi kiểu UI hardcode.

### Scope
- model/state contract only + wiring tối thiểu

### Required fields
- currentFaceState
- currentBubble
- currentFx
- currentInteractionMode
- menuState
- currentMiniGameState
- currentSceneFlags

### Definition of done
- Home scene dùng contract mới.
- Có chỗ để gắn animation, bubble, FX, game.
- Build pass.

---

# Batch H2 — Single-face visual system

## H2-01 — Implement production base face renderer

### Goal
Tạo renderer cho một face duy nhất, chất lượng production, phù hợp nền đen.

### Scope
- base face only
- chưa full animation catalog

### Required visual qualities
- mắt có depth
- glow đẹp trên nền đen
- identity nhất quán
- không cartoon rẻ tiền
- không pixel mờ

### Must include
- left/right eye shells
- pupil blocks
- eyelid layer
- highlight layer
- scale strategy phù hợp nhiều màn hình

### Definition of done
- Home có một face lớn, premium-looking.
- Không còn feel demo face nhỏ giữa card.

---

## H2-02 — Add face asset/style system

### Goal
Tách theme/style/metrics của face thành nơi cấu hình rõ ràng.

### Scope
- colors
- dimensions
- glow params
- animation constants

### Why
Để các animation sau không hardcode rải rác.

### Definition of done
- Có config/style source rõ ràng cho face.
- Build pass.

---

## H2-03 — Add responsive face stage layout

### Goal
Làm face scale tốt trên các device size khác nhau, vẫn giữ cảm giác Looi-like.

### Must handle
- small phones
- medium phones
- tall aspect ratios
- landscape safe fallback nếu có

### Definition of done
- Face không quá nhỏ, không bị lạc lõng.
- Menu button không đè lên face.

---

# Batch H3 — Face state resolver

## H3-01 — Define FaceStateSpec model

### Goal
Tạo model mô tả đầy đủ visual state của face, tách khỏi PetState thô.

### Must cover
- eye openness
- lid position
- pupil target
- glow intensity
- expression type
- motion profile
- accent fx hints

### Definition of done
- Có spec model để renderer và animation controller dùng chung.

---

## H3-02 — Implement PetState + conditions → FaceState resolver v1

### Goal
Map state hiện tại của pet sang face states meaningful.

### Required mappings
- neutral
- curious
- sleepy
- hungry
- lonely
- happy
- excited
- sad_low_energy

### Must not do
- không random vô nghĩa
- không if-else rối trong composable

### Definition of done
- đổi PetState là face state thay đổi hợp lý.

---

## H3-03 — Add event-aware resolver hooks

### Goal
Cho resolver nhận thêm event/reaction context ngoài PetState.

### Inputs cần chuẩn bị
- greeting active
- tap reaction
- long press
- audio listen
- audio surprise
- game mode

### Definition of done
- Face state không chỉ phản ánh mood dài hạn mà còn phản ứng được event ngắn hạn.

---

# Batch H4 — Core animation engine

## H4-01 — Implement blink system with variants

### Goal
Blink không chỉ một kiểu; cần catalog blink dùng thật.

### Required variants
- normal
- quick
- double
- slow sleepy

### Definition of done
- Blink tự nhiên, theo state.
- Sleepy blink khác happy/neutral blink.

---

## H4-02 — Implement idle breathing / floating motion

### Goal
Làm pet luôn “sống” ngay cả khi không có stimulus.

### Must be
- subtle
- state-sensitive
- không gây say hoặc lố

### Definition of done
- Đứng nhìn 15 giây vẫn thấy pet có life.

---

## H4-03 — Implement gaze drift system

### Goal
Tạo micro gaze movement có ý nghĩa.

### Required directions
- center hold
- left glance
- right glance
- upward hopeful
- scan light

### Definition of done
- Gaze không đứng im quá lâu.
- Không gây lác hoặc đơ.

---

## H4-04 — Implement animation controller with enter/hold/exit

### Goal
Đưa animation về một controller rõ ràng thay vì local hacks trong composable.

### Must support
- base idle loop
- transient reaction
- interruption
- settle back to idle

### Definition of done
- Có controller thật để task sau gắn reaction vào.

---

## H4-05 — Add priority and interruption rules

### Goal
Triển khai cơ chế ưu tiên animation.

### Required priorities
- P0 critical
- P1 direct interaction
- P2 need expression
- P3 ambient idle

### Definition of done
- Surprise/tap/greeting cắt được idle đúng cách.
- Không giật hoặc conflict animation.

---

# Batch H5 — Greeting and open-app feeling

## H5-01 — Replace static app-open experience with greeting sequence

### Goal
Mở app là pet có reaction mở đầu ngay.

### Required variants
- soft greeting
- excited greeting
- sleepy wake greeting

### Definition of done
- Home không còn cảm giác dead screen khi app vừa mở.

---

## H5-02 — Bind greeting sequence to real pet state

### Goal
Greeting phải đến từ PetState thật, không random demo.

### Required mappings
- bond/social high → excited
- sleepy high → sleepy wake
- otherwise soft / curious

### Definition of done
- Mỗi state mở app cho cảm giác greeting khác nhau.

---

## H5-03 — Emit greeting scene metadata/events

### Goal
Giữ debug visibility và event-driven architecture cho greeting.

### Definition of done
- Greeting có event/log phục vụ debug.
- Build pass.

---

# Batch H6 — Direct interaction gestures

## H6-01 — Implement tap gesture reaction pipeline

### Goal
Tap face tạo reaction production-ready.

### Required behavior
- focus snap
- quick blink / pulse
- optional short bubble
- cooldown

### Definition of done
- Tap luôn có phản hồi rõ, nhanh, không spam lặp vô tận.

---

## H6-02 — Implement long-press cuddle mode

### Goal
Hold face tạo cảm giác pet được vuốt ve/cuddle.

### Required behavior
- soften gaze
- lean-in illusion
- slower breathing
- affectionate bubble optional

### Definition of done
- Long-press cảm giác khác hẳn tap.

---

## H6-03 — Implement anti-spam / overstimulated reaction

### Goal
Ngăn spam tap phá immersion.

### Required behavior
- detect excessive tap rate
- brief annoyed/overstimulated reaction
- cooldown increase tạm thời

### Definition of done
- Spam tap không tạo farm reaction vô hạn.

---

# Batch H7 — Audio reactive face behavior

## H7-01 — Bind SOUND_DETECTED to face attention reaction

### Goal
Khi có sound, face chuyển sang attentive/listen.

### Required behavior
- curious or listen snap
- short hold
- settle to curious idle

### Definition of done
- Âm thanh tạo cảm giác pet nghe thấy.

---

## H7-02 — Bind loud-sound events to surprise reaction

### Goal
Sound lớn phải tạo interrupt reaction phù hợp.

### Required behavior
- wide eyes
- quick recoil
- short freeze
- recover cleanly

### Definition of done
- Surprise state cắt idle đúng priority.

---

## H7-03 — Add self-trigger-safe reaction policy

### Goal
Không để pet phản ứng ngu với chính âm thanh của nó.

### Definition of done
- Audio playback không spam khiến face cứ surprise/listen loop.

---

# Batch H8 — Talking message overlay

## H8-01 — Implement contextual bubble overlay component

### Goal
Tạo bubble text đẹp, nhẹ, hợp tone black stage.

### Must have
- good placement
- auto-dismiss
- subtle animation
- readable contrast

### Definition of done
- Bubble đẹp, không giống card/info panel.

---

## H8-02 — Add TalkingMessageController

### Goal
Quản lý message selection, dedupe, timeout, cooldown.

### Message types
- greeting
- tap
- cuddle
- hungry
- sleepy
- playful invite
- audio react

### Definition of done
- Bubble logic không bị spam, không hiện vô nghĩa.

---

## H8-03 — Bind real Home events to contextual bubble rules

### Goal
Bubble chỉ hiện khi đúng ngữ cảnh.

### Definition of done
- Greeting/tap/hungry/sleepy/audio/game invite có line hợp lý.

---

# Batch H9 — Need-driven ambient expressions

## H9-01 — Implement hungry ambient expression cycle

### Goal
Khi pet đói, idle không còn neutral mà đổi sang hungry feel.

### Definition of done
- Nhìn phát biết pet đang muốn ăn / cần attention.

---

## H9-02 — Implement sleepy ambient expression cycle

### Goal
Khi sleepy cao, eyes + blink + movement đều sleepy.

### Definition of done
- Sleepy state rất dễ đọc nhưng vẫn đẹp.

---

## H9-03 — Implement lonely / low-social ambient expression cycle

### Goal
Khi social thấp, pet có feel thiếu tương tác.

### Definition of done
- Không cần text nhiều vẫn đọc được loneliness nhẹ.

---

# Batch H10 — FX layer and polish

## H10-01 — Add lightweight FX overlay system

### Goal
Tạo layer chung cho hearts, sparks, zzz, exclamation.

### Definition of done
- FX có hệ thống, không hardcode vặt.

---

## H10-02 — Bind FX to specific reactions only

### Goal
FX xuất hiện tiết chế, đúng lúc.

### Bindings
- happy tap → tiny hearts/sparks
- sleepy → zzz hiếm
- surprise → exclamation
- excited greeting → spark/hearts nhẹ

### Definition of done
- FX làm giàu cảm xúc, không biến thành app trẻ con loè loẹt.

---

## H10-03 — Add premium motion polish pass

### Goal
Tinh chỉnh easing, durations, sequencing để toàn bộ Home mượt hơn.

### Definition of done
- Motion feeling đồng nhất, không chắp vá.

---

# Batch H11 — Menu button and navigation cleanup

## H11-01 — Replace visible multi-tab navigation with single menu button

### Goal
Home chỉ còn 1 nút visible.

### Scope
- xử lý đúng navigation/access tới Debug/Diary
- dọn UI cũ ra khỏi Home

### Definition of done
- Không còn tab visible trên Home.
- Menu button hoạt động ổn.

---

## H11-02 — Implement minimal menu sheet or direct Debug opening

### Goal
Từ menu button, user vẫn vào được Debug mà không phá Home aesthetics.

### Definition of done
- Debug accessible nhưng Home vẫn cực sạch.

---

## H11-03 — Move any remaining Home info blocks out of Home

### Goal
Dọn sạch các khối thông tin cũ còn sót.

### Targets
- greeting card cũ
- status card cũ
- today with your pet cũ
- any hidden text demo

### Definition of done
- Home chỉ còn stage-centric content.

---

# Batch H12 — Mini game integration

## H12-01 — Define mini game architecture for Home-integrated play

### Goal
Tạo contract/state machine cho game layer không phá face system.

### Required states
- idle
- invite
- active
- success
- fail
- cooldown

### Definition of done
- Có game contract rõ ràng.

---

## H12-02 — Implement Game 1: Catch the Spark

### Goal
Làm game đầu tiên thật, gọn, hợp aesthetic.

### Game loop
- spark xuất hiện quanh face
- user tap đúng trước timeout
- pet phản ứng theo kết quả
- state/bond/social update

### Definition of done
- Chơi được end-to-end.
- Win/fail có reaction và state effect.

---

## H12-03 — Add game invitation behavior

### Goal
Pet đôi khi mời chơi dựa trên mood/social/state.

### Definition of done
- Game không chỉ mở bằng debug; có lối vào tự nhiên từ pet behavior.

---

## H12-04 — Add game cooldown and anti-fatigue rules

### Goal
Không để pet mời chơi hoặc game spam liên tục.

### Definition of done
- Game cadence hợp lý, believable.

---

# Batch H13 — Production hardening

## H13-01 — Performance pass for Home scene

### Goal
Tối ưu recomposition, state churn, animation overhead.

### Definition of done
- Home mượt hơn, không lag thấy rõ trên device dev.

---

## H13-02 — Debug visibility pass for new Home architecture

### Goal
Đảm bảo có thể debug face state, current reaction, current cooldowns, current game state.

### Definition of done
- Debug screen thấy được thông tin hữu ích cho Home mới.

---

## H13-03 — Final art/consistency pass

### Goal
Rà soát visual consistency toàn Home.

### Checklist
- menu button tone đúng
- bubble đẹp
- FX tiết chế
- face size đúng
- black-stage premium
- animation đồng nhất

### Definition of done
- Home đạt mức production-ready về cảm giác tổng thể.

---

# Master prompt để giao Claude cho từng task

```md
Read and follow `AGENTS.md` first.

Then read only:
- docs/project_manifest.md
- docs/development_roadmap.md
- docs/06_personality_engine.md
- docs/07_robot_memory_system.md
- docs/08_audio_interaction_architecture.md
- docs/09_pet_app_definition_full.md
- docs/home_redesign_looi_plan.md
- docs/home_redesign_looi_task_breakdown.md

Task ID: <PASTE TASK ID>
Title: <PASTE TASK TITLE>

Goal:
<PASTE GOAL>

Scope:
- Only implement this task.
- Do not expand into adjacent tasks.
- Keep the Home redesign aligned with the single-face black-stage Looi-like direction.
- Do not add mock production logic.
- Do not leave TODOs, empty methods, or placeholder animation logic.

What the task changes:
<PASTE WHAT THIS TASK CHANGES>

What it must not do:
<PASTE MUST NOT DO>

Current context / risks:
- The previous Home looked like a dashboard and that must not remain on the redesigned Home surface.
- The redesign must stay production-ready, state-driven, and build-safe.
- Remaining risks from the previous task must be considered, but must not change the task itself.

Expected outcomes:
- Implement the requested slice fully.
- Keep the app buildable.
- Keep verification visible.

Definition of done:
<PASTE DOD>

How to verify:
<PASTE VERIFICATION>

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

# Suggested execution order

Thứ tự nên làm:
1. H1-01
2. H1-02
3. H1-03
4. H2-01
5. H2-02
6. H2-03
7. H3-01
8. H3-02
9. H3-03
10. H4-01
11. H4-02
12. H4-03
13. H4-04
14. H4-05
15. H5-01
16. H5-02
17. H5-03
18. H6-01
19. H6-02
20. H6-03
21. H7-01
22. H7-02
23. H7-03
24. H8-01
25. H8-02
26. H8-03
27. H9-01
28. H9-02
29. H9-03
30. H10-01
31. H10-02
32. H10-03
33. H11-01
34. H11-02
35. H11-03
36. H12-01
37. H12-02
38. H12-03
39. H12-04
40. H13-01
41. H13-02
42. H13-03

---

# Review checklist sau toàn bộ batch

- Home có còn dấu vết dashboard cũ không?
- Chỉ còn 1 nút visible chưa?
- Face có đủ premium và đủ lớn không?
- Face có 1 identity nhất quán chưa?
- State resolver có rõ ràng không?
- Animation controller có thật không hay chỉ hack trong UI?
- Greeting có làm app feel alive ngay không?
- Tap và hold có khác biệt rõ ràng không?
- Audio reaction có believable không?
- Bubble có contextual và không spam không?
- Game có hòa hợp với Home không?
- Debug vẫn vào được mà không phá Home chưa?
- Tổng thể đã đạt mức production-ready chưa?

