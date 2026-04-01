# Phase 1 Completion Guide — AI Pet Robot

Version: v1  
Purpose:  
Đây là tài liệu **kim chỉ nam cuối cùng** để:

- Đánh giá Phase 1 đã hoàn thành hay chưa
- Hướng dẫn AI agents (Codex / Claude) thực thi đúng trọng tâm
- Tránh lệch hướng sang feature không cần thiết
- Đảm bảo sản phẩm đạt “pet feels alive” thay vì chỉ là demo kỹ thuật

---

# 1. Core Principle (Non-Negotiable)

> Phase 1 thành công KHÔNG phải khi có đủ feature  
> Phase 1 thành công khi pet **có cảm giác sống**

---

## Absolute rule:

Nếu một task không làm pet:
- sống hơn
- phản ứng rõ hơn
- có continuity hơn

→ task đó không thuộc Phase 1

---

# 2. Current State Assessment

## System completeness:
- ~80–85% technical completion
- ~60–65% product experience completion

## Meaning:
- Nền tảng đã đủ
- Thiếu **connection + expression + behavior loop**

---

# 3. Definition of DONE (STRICT)

Phase 1 chỉ được coi là DONE khi đạt đủ **4 điều kiện sau**

---

## 3.1 Instant Life on App Open

When app opens:
- Pet reacts immediately (<300ms perceived)
- Không có trạng thái “đứng yên chờ”
- Greeting phụ thuộc state thật

---

## 3.2 Full Interaction Loop

Every interaction must produce:

Input →
- animation
- audio (optional nhưng nên có)
- state change
- event logged

---

## 3.3 Idle is Alive

Khi không có interaction:

Pet phải:
- tự animation nhẹ
- tự attention shift
- tự bored / curious
- không đứng yên quá 3–5s

---

## 3.4 Time Has Meaning

Sau khi rời app:

Quay lại:
- state thay đổi thật (decay)
- greeting khác
- behavior khác

---

❗ Nếu thiếu bất kỳ điều nào → Phase 1 chưa DONE

---

# 4. Critical Gaps (Must Fix)

---

## GAP 1 — Behavior → Avatar Binding (BLOCKER)

### Problem:
- BehaviorEngine tồn tại nhưng không drive avatar
- Avatar chỉ dựa vào emotion resolver

### Required:
Avatar phải reflect:
- intention
- attention
- stimulus
- urgency

---

## GAP 2 — Idle Life System

### Problem:
- idle còn “dead”

### Required:
- random attention shift
- micro animation
- curiosity triggers
- idle event emission

---

## GAP 3 — State Visibility

### Problem:
- user không “cảm nhận” state

### Required mapping:

| State | Effect |
|------|--------|
| hunger | animation + audio |
| energy | reaction speed |
| sleepiness | eye + animation |
| bond | greeting + reaction |

---

## GAP 4 — Weak Greeting System

### Problem:
- greeting generic

### Required:
- context-aware greeting:
  - time since last open
  - mood
  - relationship
  - last interaction

---

## GAP 5 — Low Variation

### Problem:
- same state → same output

### Required:
- weighted randomness
- anti-repeat guard
- micro variation

---

# 5. Execution Plan (Final Phase 1 Push)

---

## Stage 1 — Behavior Integration (Highest Priority)

Goal:
Behavior drives visible pet

Tasks:
- Connect BehaviorEngine → Avatar state
- Map intention → animation
- Map attention → gaze/pose
- Introduce priority system (reaction vs idle vs greeting)

---

## Stage 2 — Idle Life System

Goal:
Pet is alive even without input

Tasks:
- Idle scheduler (randomized intervals)
- Idle actions:
  - look around
  - blink variations
  - micro movement
- Emit IDLE_ACTIVITY events

---

## Stage 3 — State → Expression Mapping

Goal:
User FEELS pet state

Tasks:
- Map hunger → visual/audio signals
- Map energy → animation speed
- Map sleepiness → eye state
- Map social → reaction enthusiasm

---

## Stage 4 — Greeting System v2

Goal:
App open = emotional moment

Tasks:
- Create GreetingResolver
- Inputs:
  - time gap
  - state
  - bond
- Output:
  - animation
  - optional audio
  - text bubble

---

## Stage 5 — Variation Engine

Goal:
Pet not repetitive

Tasks:
- Add randomness layer
- Anti-repeat guard
- Weighted selection

---

# 6. What NOT to Do (Strict)

❌ Không thêm:
- Cloud AI
- LLM conversation
- Robot body integration
- Complex refactor unrelated to behavior
- New perception features

❌ Không:
- optimize prematurely
- rewrite architecture
- add unused abstraction

---

# 7. Verification Checklist (Final Gate)

Before declaring Phase 1 DONE:

---

## Test 1 — First Impression

Open app:

- Pet reacts immediately
- Không có dead state
- Greeting hợp lý

---

## Test 2 — Interaction Loop

Tap pet:

- animation xảy ra
- state thay đổi
- event được log

---

## Test 3 — Idle Observation

Không làm gì 30s:

- pet có hành vi
- không đứng yên

---

## Test 4 — Time Gap

Close app → reopen sau 2–6h:

- state khác
- greeting khác

---

## Test 5 — Emotional Consistency

Test nhiều trạng thái:

- hungry → biểu hiện rõ
- sleepy → biểu hiện rõ
- happy → biểu hiện rõ

---

## Test 6 — Non-Repetition

Lặp lại interaction:

- không bị spam giống nhau

---

# 8. Definition of Success

Phase 1 thành công khi:

> Người dùng mở app không phải để test feature  
> mà để “xem pet của mình hôm nay như thế nào”

---

# 9. Output Requirement for AI Agents

Mỗi task phải trả về:

- Summary of changes
- Files changed
- How to verify
- Build result
- Remaining risks

---

# 10. Final Note

This is NOT:

- AI assistant
- chatbot
- feature demo

This IS:

> A digital creature that feels alive.

If it doesn’t feel alive → Phase 1 is not done.