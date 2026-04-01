# Pet Face, Animation, Behavior, and Randomization Master Plan

Version: v1  
Purpose: Source-of-truth design for the Android Pet App visual expression layer, animation system, pet behavior surface, and bounded randomization.  
Audience: Claude/Codex/planning agents and human implementers.

---

## 1. Goal

Biến pet từ một avatar biết đổi emotion thành một **digital creature có presence liên tục** trên Home screen: có gương mặt rõ trạng thái, có animation có tầng, có phản ứng theo sự kiện, có variation có kiểm soát, và có cách hành xử dễ đoán nhưng không máy.

Plan này bám các nguyên tắc sản phẩm hiện có:
- Home là trung tâm trải nghiệm, pet avatar lớn là trọng tâm.
- Open app phải có phản ứng ngay.
- Interaction phải tạo animation/feedback + state change + event persistence.
- Emotion không được random vô nghĩa; variation chỉ là weighted variation nhỏ.
- Hành vi phải bounded: không spam audio, không flicker emotion, không lặp y hệt mãi.
- Personality chỉ bias lựa chọn, không override logic cơ bản.

---

## 2. Non-negotiable Principles

### 2.1 Product principles
- Pet là **digital creature**, không phải assistant hay chat UI.
- Visual expressiveness quan trọng hơn số lượng tính năng.
- Animation phải phục vụ pet loop, không phải chỉ để đẹp.
- Mọi phản ứng quan trọng phải có nguyên nhân từ state, event, condition, hoặc trait bias.

### 2.2 System principles
- Offline-first.
- Event-driven.
- State-aware.
- Debuggable.
- Real persistence cho state/event nơi hệ thống đã yêu cầu.

### 2.3 Behavior safety rails
- Không phát animation mạnh liên tục.
- Không đổi emotion frame-to-frame kiểu hỗn loạn.
- Không để high-energy animation xuất hiện khi pet đang sleepy/sad/hungry trừ khi có event đủ mạnh.
- Không để random phá greeting, tap reaction, feed/play/rest reaction, hay sound reaction đang chạy.

---

## 3. Architectural Model

Chuỗi chuẩn cần giữ:

**PetState + Traits + Context + Event -> PetEmotion -> AnimationIntent -> AnimationVariant -> Avatar Render + Bubble + Optional Audio**

### 3.1 Layer responsibilities

#### A. PetState / Context layer
Input chính:
- mood
- energy
- hunger
- sleepiness
- social
- bond
- derived conditions: hungry, sleepy, bored, playful, attached, overstimulated
- recent event history
- time since last interaction
- audio activity / app-open / tap / activity actions

#### B. PetEmotion layer
Là lớp biểu cảm nhìn thấy được.
Mỗi thời điểm chỉ nên có 1 **visible dominant emotion** + optional micro-modifier.

#### C. AnimationIntent layer
Biến emotion + event thành ý định hoạt ảnh:
- idle_resting
- idle_attentive
- greet_soft
- greet_excited
- react_tap_happy
- react_tap_sleepy
- react_sound_listen
- activity_feed_satisfied
- activity_play_bouncy
- activity_rest_settle
- needy_hungry_prompt
- sleepy_drift

#### D. AnimationVariant layer
Chọn authored variation cụ thể từ một pool nhỏ có trọng số và cooldown.

Ví dụ:
- `greet_soft` -> `greet_soft_nod`, `greet_soft_blink_smile`, `greet_soft_wave`
- `idle_attentive` -> `idle_look_left`, `idle_look_right`, `idle_mini_bob`, `idle_hold_and_blink`

#### E. Avatar presentation layer
Render:
- face geometry
- eye shape
- brow/eyelid posture
- mouth shape
- idle motion
- temporary reaction overlays
- talking bubble
- optional audio sync trigger

---

## 4. Face System Design

## 4.1 Face design philosophy
Vì pet hiện tại ưu tiên style đã có sẵn, face nên tiếp tục theo hướng:
- đọc được cảm xúc nhanh
- đơn giản nhưng không vô hồn
- ít element nhưng có nhiều biến thể chuyển động
- micro-expression quan trọng ngang emotion base

## 4.2 Face channels
Mỗi face được cấu thành từ các kênh sau:

### A. Eye openness
- closed
- sleepy slit
- relaxed half-open
- neutral open
- wide attentive
- excited wide

### B. Eye tilt / curvature
- upward friendly
- flat neutral
- inward curious
- downward sad
- asymmetric playful / suspicious

### C. Pupil / gaze behavior
- center hold
- soft drift
- quick glance
- look up
- look down
- look toward sound direction surrogate

### D. Brow / upper-lid tension
- neutral
- lifted curious
- softened happy
- compressed needy
- lowered annoyed/tired

### E. Mouth / muzzle line
- flat neutral
- tiny smile
- open happy
- tiny o-shape surprise
- wavy sad
- sleepy relaxed
- needy pout

### F. Head/body bob surrogate
Nếu face-only chưa có full body, dùng stage motion thay cho body language:
- vertical bob
- micro squash/stretch
- side sway
- settle down drift

---

## 5. Canonical Face Set

Đây là bộ face master nên định nghĩa trước, sau đó map sang animation pack.

## 5.1 Core visible emotions (required)

### 1. IDLE
Use when:
- không có event mạnh
- state cân bằng
- đang chờ interaction

Visual:
- mắt neutral open
- brow trung tính
- mouth flat nhẹ hoặc smile rất nhỏ
- thỉnh thoảng gaze drift

### 2. HAPPY
Use when:
- vừa được tap
- vừa feed/play thành công
- bond cao + greeting ấm

Visual:
- mắt cong nhẹ thân thiện
- mouth smile rõ hơn
- bob nhẹ tích cực

### 3. CURIOUS
Use when:
- có sound detection
- idle attentive
- vừa có context mới

Visual:
- mắt mở vừa/nhỉnh hơn bình thường
- bất đối xứng nhẹ hoặc look offset
- head tilt surrogate / side lean

### 4. SLEEPY
Use when:
- sleepiness cao
- energy thấp
- rest action
- absence dài theo circadian

Visual:
- mắt nửa khép
- blink chậm
- mouth relaxed
- motion chậm, settle downward

### 5. SAD
Use when:
- social thấp
- bị ignore lâu
- energy thấp + bond-expectation mismatch

Visual:
- mắt hơi droop
- mouth wavy/down curve nhỏ
- movement rất ít

### 6. EXCITED
Use when:
- play action
- high energy + high social
- greeting sau thời gian vắng nhưng bond cao

Visual:
- mắt mở rộng vui
- smile open hơn
- bounce/wiggle nhanh ngắn

### 7. HUNGRY
Use when:
- hunger cao
- feed-related prompt

Visual:
- needy eyes
- mouth pout nhẹ
- tiny repeated attention-seeking motion

## 5.2 Secondary micro-states (recommended)

### 8. ATTENTIVE
- idle nhưng tập trung
- sound vừa phát hiện
- user vừa mở app

### 9. PLAYFUL
- energy cao, social cao, trait playful bias
- gần HAPPY/EXCITED nhưng tinh nghịch hơn

### 10. ATTACHED / AFFECTIONATE
- bond cao
- greeting thân mật
- tap phản ứng ấm áp hơn bình thường

### 11. BORED
- lâu không activity
- social thấp vừa
- energy không thấp nhưng pet thiếu kích thích

### 12. OVERSTIMULATED
- tương tác dồn dập
- audio/tap quá nhiều trong thời gian ngắn
- dùng để giảm phản ứng / cooldown expressively

---

## 6. Animation Taxonomy

Không nên coi animation là một mớ clip rời rạc. Phải chia tầng.

## 6.1 Layer 0 — Base loops
Luôn có thể chạy khi không bị reaction khác chiếm quyền.

### Required loops
- base idle breathing / bob
- blink loop
- gaze drift loop
- subtle mouth settling

## 6.2 Layer 1 — Emotion posture loops
Áp cho face theo dominant emotion.

Ví dụ:
- idle_neutral_loop
- happy_soft_loop
- curious_hold_loop
- sleepy_drift_loop
- sad_still_loop
- hungry_needy_loop

## 6.3 Layer 2 — Short reactions
Clip 300–1200ms, có mở đầu/rơi về base.

Ví dụ:
- tap_happy_pop
- tap_curious_peek
- sleepy_slow_ack
- sound_listen_raise
- sound_startle_small
- feed_satisfied_smile
- play_bounce
- rest_settle
- greet_soft_nod
- greet_excited_bounce

## 6.4 Layer 3 — Transitional animations
Dùng để tránh cắt thô.

Ví dụ:
- excited_to_idle
- curious_to_idle
- sleepy_to_idle_slow
- needy_to_hungry_hold
- reaction_to_loop_blend

## 6.5 Layer 4 — Bubble-coupled animations
Animation đi cùng talking bubble.

Ví dụ:
- speak_bob_small
- think_look_up
- needy_callout
- pleased_after_message

---

## 7. Recommended Animation Pack

## 7.1 Minimum authored pack for MVP+

### Base / idle
1. `idle_breathe_soft`
2. `idle_hold_and_blink`
3. `idle_small_gaze_left`
4. `idle_small_gaze_right`
5. `idle_micro_sway`

### Happy
6. `happy_smile_bob`
7. `happy_double_blink`
8. `happy_tiny_bounce`

### Curious
9. `curious_lean_left`
10. `curious_lean_right`
11. `curious_peek`
12. `curious_blink_hold`

### Sleepy
13. `sleepy_slow_blink`
14. `sleepy_droop`
15. `sleepy_half_close_settle`

### Sad / low-energy
16. `sad_hold`
17. `sad_small_downlook`
18. `sad_recover_blink`

### Excited / play
19. `excited_bounce`
20. `excited_wiggle`
21. `excited_double_bob`

### Hungry / needy
22. `hungry_pout`
23. `hungry_callout`
24. `hungry_look_up`

### Interaction / event reactions
25. `greet_soft_nod`
26. `greet_warm_smile`
27. `greet_excited`
28. `tap_ack_happy`
29. `tap_ack_sleepy`
30. `tap_ack_attached`
31. `sound_listen`
32. `sound_small_startle`
33. `feed_satisfied`
34. `play_invite`
35. `rest_settle`

## 7.2 Nice-to-have pack
- `playful_wink`
- `attached_snuggle_face`
- `bored_look_away`
- `overstimulated_pause`
- `return_after_absence_long_greet`

---

## 8. Behavior Surface Design

“Cách hành xử” ở đây là phần người dùng nhìn thấy trên Home.

## 8.1 Core behavior categories

### A. Open-app greeting
Pet phải phản ứng ngay khi mở app.
Variants phụ thuộc:
- sleepiness
- hunger
- social
- bond
- absence duration

Examples:
- sleepy + low energy -> slow blink + soft bubble
- high bond + medium/high social -> warm smile + bounce + affectionate bubble
- hungry -> needy greet + feed hint bubble

### B. Idle presence
Khi user chưa làm gì, pet vẫn có nhịp sống:
- blink
- gaze drift
- occasional bounded idle variant
- low-frequency callout nếu state đủ mạnh (ví dụ hungry hoặc bored), có cooldown dài

### C. Tap reaction
Tap không chỉ là animation. Nó phải là full loop:
- validate cooldown
- choose reaction theo state + bond + trait bias
- visible reaction
- optional short clip
- state update
- event persistence
- optional talking bubble

### D. Activity reaction
Feed / Play / Rest phải có reaction khác nhau rõ ràng.

#### Feed
- reduce hunger
- satisfied / needy relief visuals
- bubble ngắn kiểu được chăm

#### Play
- high energy response nếu pet đủ năng lượng
- nếu sleepy thì phản ứng có thể chậm hoặc low-energy playful

#### Rest
- settle animation
- sleepy resolver bias tăng

### E. Sound reaction
Chỉ ở mức emotional reaction MVP:
- attentive listen
- small startle
- optional bubble như “hmm?”

### F. Long absence return
Nếu absence đủ dài:
- greeting nên reflect time delta
- không chỉ là variant random
- có thể dùng notable bubble hoặc demeanor shift

---

## 9. Talking Bubble Design Rules

### 9.1 Bubble purpose
Bubble là biểu hiện pet đang “nói/nhĩ/đòi hỏi nhẹ”, không phải chat window.

### 9.2 Bubble source priorities
Chỉ lấy từ flow thật:
1. greeting resolver
2. tap reaction resolver
3. activity resolver
4. sound reaction resolver
5. idle need prompt resolver

### 9.3 Bubble rules
- ngắn
- có cooldown
- không overlap hỗn loạn
- bubble phải cùng mood với animation hiện tại
- bubble không được xuất hiện quá dày làm pet thành notification engine

### 9.4 Bubble types
- greeting bubble
- reaction bubble
- need bubble
- affectionate bubble
- sleepy bubble
- playful invite bubble

---

## 10. Randomization Model

Phần này là trọng tâm vì user yêu cầu “random” nhưng sản phẩm hiện tại cấm random vô nghĩa.

## 10.1 Golden rule
Random chỉ được dùng để **chọn variation trong một tập hợp hợp lệ**, không được dùng để quyết định emotion chính một cách vô căn cứ.

## 10.2 What can be randomized
- idle animation variant trong cùng emotion family
- gaze direction nhỏ
- blink interval trong range hợp lý
- lựa chọn giữa 2–4 greeting variants hợp lệ
- lựa chọn bubble wording trong cùng semantic meaning
- small timing jitter để bớt máy móc

## 10.3 What must not be randomized freely
- dominant emotion không dựa state
- reaction category không dựa event
- large animation energy level trái với state
- audio spam probability
- bond/trait updates

## 10.4 Inputs to selector
- dominant emotion
- derived conditions
- triggering event
- recent animation history
- recent bubble history
- cooldown map
- trait bias
- energy band
- sleepiness band
- social band

## 10.5 Output contract
Selector phải trả về:
- chosen animation id
- chosen bubble id (optional)
- reason/debug tags
- priority
- duration estimate

## 10.6 Weighted selection policy
Mỗi context có một danh sách ứng viên:

```text
candidate = {
  id,
  baseWeight,
  allowedEmotions,
  allowedConditions,
  minCooldownMs,
  repeatPenalty,
  traitBiases,
  stateBiasRules,
  priority
}
```

Trọng số cuối:

`effectiveWeight = baseWeight * emotionMatch * conditionMatch * traitBias * stateBias * antiRepeatPenalty * cooldownGate`

## 10.7 Anti-repeat rules
- Không lặp lại cùng một reaction variant quá 2 lần gần nhau.
- Idle pool phải có memory 3–5 lựa chọn gần nhất.
- Greeting variants cần cooldown dài hơn idle variants.
- High-energy variants cần cooldown riêng.

## 10.8 Bounded randomness examples

### Example A — Sleepy idle
Allowed:
- sleepy_slow_blink
- sleepy_droop
- sleepy_half_close_settle
- idle_hold_and_blink (weight thấp)

Disallowed:
- excited_bounce
- play_invite

### Example B — Tap while attached + medium energy
Allowed:
- tap_ack_attached
- happy_smile_bob
- happy_double_blink
- playful_wink (nếu trait playful cao)

### Example C — Hungry greeting
Allowed:
- hungry_callout
- greet_soft_nod + hungry bubble
- hungry_look_up

---

## 11. Trait Bias Model

Traits chỉ bias chứ không làm pet “đổi loài”.

## 11.1 Recommended trait axes
- playful
- lazy
- affectionate
- curious
- calm
- needy

## 11.2 Where traits may bias
- idle animation tendency
- greeting warmth level
- tap reaction family
- activity preference
- bubble wording tone

## 11.3 Example biases
- high playful -> tăng weight `excited_bounce`, `play_invite`, `playful_wink`
- high lazy -> tăng weight `sleepy_droop`, `rest_settle`, giảm high-energy repeats
- high affectionate -> tăng weight `greet_warm_smile`, `tap_ack_attached`
- high curious -> tăng weight `curious_peek`, `sound_listen`, idle glance variants

---

## 12. State-to-Behavior Mapping Rules

## 12.1 Energy bands
- 0–20: low-energy only
- 21–45: subdued
- 46–75: normal
- 76–100: can use playful/excited

## 12.2 Sleepiness bands
- 0–25: no sleepy bias
- 26–55: mild sleepy bias
- 56–100: dominant sleepy family unless strong event

## 12.3 Hunger bands
- 0–30: none
- 31–60: occasional food-seeking bubble allowed
- 61–100: hungry emotion family heavily weighted

## 12.4 Social bands
- low social: fewer outward invitations, more flat/withdrawn reactions
- high social: more greeting warmth, more play invites, more attentive reactions

## 12.5 Bond bands
- low bond: neutral reactions
- medium bond: warm greeting unlocks
- high bond: affectionate variants and attached micro-states more available

---

## 13. Runtime Orchestration Rules

## 13.1 Priority order
1. safety / blocking transitions
2. app-open greeting
3. direct user interaction reaction
4. activity reaction
5. sound reaction
6. need prompt
7. idle variation

## 13.2 Interrupt rules
- Greeting có thể bị tap interrupt nếu user tương tác trực tiếp.
- Idle variation không được interrupt reaction mạnh.
- Bubble-only prompt không được cắt animation chính trừ khi timeout xong.
- High-priority event có thể cancel idle, không cancel settle transition ở frame đầu nếu gây giật.

## 13.3 Duration guidance
- idle variants: 1.5–4s
- short reactions: 0.3–1.2s
- greetings: 0.8–2s
- need prompts: ngắn, hiếm

---

## 14. Debuggability Requirements

Phải thấy và verify được:
- dominant emotion
- current derived conditions
- current animation intent
- chosen animation variant
- chosen bubble reason
- cooldown status
- recent variant history

Không nhất thiết show hết ở Home, nhưng debug flow phải xem được.

---

## 15. Verification Matrix

Một feature face/animation chỉ được coi là done khi verify được càng nhiều càng tốt:
- nhìn thấy được
- có event source rõ
- có state condition rõ
- không spam
- quay lại vẫn đúng continuity

### Core manual checks
1. Open app nhiều lần ở các state khác nhau -> greeting khác nhau có lý do.
2. Tap pet liên tục -> vẫn phản ứng nhưng bị bounded cooldown.
3. Để idle 60s -> có variation, không lặp máy móc, không hỗn loạn.
4. Set sleepy state -> không xuất hiện excited bounce bừa bãi.
5. Set hungry state -> needy/hungry variants xuất hiện đúng lúc.
6. Tăng bond -> greeting/tap reaction ấm hơn.
7. Bật sound reaction -> attentive/startle hoạt động nhưng không spam.

---

## 16. Suggested Deliverable Strategy

Không nên giao Claude làm tất cả trong một task. Nên chia theo vertical slices:
1. Face/emotion model expansion
2. Animation contract + runtime selection model
3. Home talking-bubble integration
4. Greeting animation pack
5. Tap reaction pack
6. Activity reaction pack
7. Idle bounded variation engine
8. Trait-biased weighting
9. Stability + verification pass

---

## 17. Final Quality Bar

Khi làm đúng, user phải cảm thấy:
- pet mở app đã sống sẵn
- pet có gương mặt đọc được
- pet không lặp y chang như GIF cứng
- pet không random lộn xộn
- mỗi phản ứng có “lý do”
- pet khác nhau theo state, bond, trait và lịch sử gần

Nếu chưa đạt các điểm trên, chưa nên coi là hoàn chỉnh.
