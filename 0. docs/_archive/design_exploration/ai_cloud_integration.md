# Thiết kế Phase 2 cho AI Pet Robot: Hội thoại, trí nhớ, tính cách và hành vi

## Bối cảnh, mục tiêu và giả định

Phase 2 tập trung nâng cấp robot “thú cưng” từ mức phản ứng cơ bản sang mức có thể trò chuyện tự nhiên, trả lời câu hỏi, ghi nhớ con người và tham chiếu lại các tương tác trước đó, đồng thời duy trì một “tính cách” ổn định theo thời gian.

Các ràng buộc cứng của thiết kế:

- Android vẫn là “main brain”: mọi quyết định cuối cùng (đặc biệt là quyết định hành động vật lý) nằm ở phía Android, không nằm trực tiếp trong LLM.
- Có thể dùng cloud AI services, và hệ thống phải tích hợp với API LLM như OpenAI và Gemini (Google).
- LLM chỉ đề xuất hành động; “Brain” (Android) là nơi thẩm định và quyết định có thực thi hay không (gating + policy). Nguyên tắc này khớp với khuyến nghị chung khi xây agent/tool-using: dùng guardrails, tách quyền, kiểm soát tool/action và tránh để văn bản tuỳ ý ảnh hưởng trực tiếp tới hành động. citeturn3search5turn2search2

Giả định kỹ thuật tối thiểu để triển khai Phase 2:

- Robot có micro + loa (hoặc speaker), và có đường điều khiển cơ cấu chấp hành (servo/motor/LED) thông qua một lớp “Actuation Controller” (có thể là MCU riêng hoặc module điều khiển).
- Ứng dụng Android có quyền RECORD_AUDIO để nhận giọng nói; SpeechRecognizer là một lựa chọn truy cập dịch vụ nhận dạng giọng nói hệ thống. citeturn1search2turn1search19
- Text-to-Speech có thể dùng API TextToSpeech trên Android; hàm speak hoạt động bất đồng bộ. citeturn1search1turn1search18

## Kiến trúc tổng thể và tích hợp LLM

### Thành phần hệ thống

Thiết kế khuyến nghị theo dạng “Android-first + Cloud-optional”, gồm các khối:

**Android Brain (ứng dụng chính, Kotlin/Java)**
- Audio I/O: VAD (voice activity detection), wake-up UI/UX, ASR (speech-to-text), TTS (text-to-speech).
- Conversation Orchestrator: điều phối luồng hội thoại, quản trị context, gọi LLM, nhận phản hồi có cấu trúc.
- Policy & Safety Engine: kiểm duyệt nội dung, chặn yêu cầu nguy hiểm, kiểm tra điều kiện môi trường, phê duyệt hành động.
- Memory System: lưu trí nhớ dài hạn/ngắn hạn, truy xuất, tóm tắt, đồng bộ cloud (nếu có).
- Robot Behavior Engine: chuyển “ý định + cảm xúc + hành động” thành hành vi robot (gesture/motion/sound/light), và phát lệnh xuống lớp điều khiển phần cứng.

**Actuation / Sensor Layer (MCU hoặc module điều khiển)**
- Điều khiển servo/motor/LED/haptics; báo trạng thái (battery, nhiệt, lỗi), cảm biến (touch, IMU, khoảng cách…).
- Giao tiếp với Android qua BLE/USB/UART/Wi‑Fi. (Không bắt buộc chuẩn cụ thể; quan trọng là có lớp API rõ ràng cho hành động.)

**Cloud AI Gateway (khuyến nghị mạnh, nhưng có thể bỏ trong hobby MVP)**
- Lý do: giảm rủi ro lộ API key trên thiết bị; gom logic rate-limit, retry, logging; cho phép “multi-provider routing” (OpenAI ↔ Gemini) và fallback. Các giới hạn tốc độ (rate limits) là thực tế phải quản trị ở tầng hệ thống. citeturn0search3turn0search1
- Chức năng: token budgeting, caching câu trả lời (với Q&A), quản lý phiên, phân tích telemetry, và audit.

### LLM Integration Architecture

Thiết kế lớp tích hợp LLM theo pattern “Provider Adapter + Unified Contract”:

**LLM Provider Abstraction**
- `LLMClient` interface (Android gọi qua Gateway hoặc trực tiếp):  
  - `generateReply(context_bundle) -> structured_response`  
  - `proposeActions(context_bundle) -> action_proposals`  
- Adapter theo provider:
  - OpenAI adapter: dùng API Responses/Tools + Structured Outputs để nhận JSON đúng schema. Structured Outputs được mô tả là cơ chế đảm bảo phản hồi tuân JSON Schema do bạn cung cấp. citeturn0search0turn0search9
  - Gemini adapter: dùng function calling để mô hình quyết định khi nào gọi “function/tool” và cung cấp tham số, phù hợp cho “LLM đề xuất hành động”. citeturn0search10turn0search4

**Điểm mấu chốt kiến trúc**
- Không cho LLM “chạm thẳng” vào actuator.
- LLM chỉ trả về:
  1) lời nói đề xuất,  
  2) intent,  
  3) thông tin cảm xúc/tone,  
  4) danh sách hành động đề xuất dạng có cấu trúc (tool call hoặc JSON action plan).
- Android Policy Engine xác thực và quyết định.

### Luồng dữ liệu chính

Luồng hội thoại realtime (tóm tắt):

1) Mic → ASR → văn bản người dùng. (Android SpeechRecognizer yêu cầu RECORD_AUDIO và truy cập speech recognition service.) citeturn1search2turn1search19  
2) Context Manager lấy: (a) vài lượt chat gần nhất, (b) tóm tắt ngắn hạn, (c) các “memories” liên quan (retrieval), (d) sensor state.  
3) Gọi LLM (OpenAI/Gemini) với prompt template + memory bundle.  
4) Nhận structured output (JSON schema). Với OpenAI có thể dùng Structured Outputs để ép đúng schema. citeturn0search0  
5) Safety checks (moderation + rules) và Action gating. OpenAI nhấn mạnh rủi ro tăng khi agent xử lý văn bản tuỳ ý có thể ảnh hưởng tool calls; structured outputs + isolation giúp giảm rủi ro nhưng không loại bỏ hoàn toàn. citeturn3search5  
6) Nếu hợp lệ: Robot Behavior Engine tạo hành vi, phát TTS, gửi lệnh actuator.  
7) Memory Writer ghi lại: transcript, summary, event, outcome.

## Chiến lược prompt, quản trị ngữ cảnh và trích xuất intent

### Prompt strategy theo tầng

Khuyến nghị dùng “prompt template có biến chính sách” thay vì nhiều prompt rời rạc. OpenAI mô tả template-based prompting giúp giảm phức tạp và dễ bảo trì khi mở rộng use case. citeturn3search13turn2search2

**Tầng hệ thống (system / developer instructions)**
- “Robot Identity & Role”: robot là thú cưng, mục tiêu là tương tác thân thiện, ngắn gọn, an toàn.
- “Tool & Action Policy”: liệt kê action primitives; quy định hành động nào cần xác nhận; hành động nào bị cấm.
- “Memory Policy”: khi nào ghi nhớ, khi nào không; ưu tiên bảo vệ dữ liệu nhạy cảm; quy tắc tóm tắt.
- “Output Contract”: bắt buộc trả về JSON theo schema (đối với OpenAI có Structured Outputs). citeturn0search0

**Tầng ngữ cảnh (context injection)**
- `session_summary`: tóm tắt ngắn hạn các lượt trước trong phiên.
- `retrieved_memories`: top‑k memories (episodic + semantic) sau retrieval.
- `user_profile`: tên, cách xưng hô, sở thích, các điều tránh/nhạy cảm.
- `robot_state`: pin, nhiệt độ, đang sạc hay không, trạng thái motor, đang ở “quiet mode” hay không.
- `environment_state`: có người gần không, robot đang được bế không, obstacle.

**Tầng hội thoại**
- `last_turns`: giữ N lượt gần nhất nguyên văn (ví dụ 6–12 lượt).
- Các tool results / sensor events trong lượt.

### Conversation context management

Vì LLM có giới hạn “context window” và không thể mang toàn bộ lịch sử theo mọi lượt, cần cơ chế nén ngữ cảnh + truy xuất có chọn lọc. Các hướng tiếp cận “quản trị tầng nhớ” (memory tiers) được thảo luận trong các công trình về LLM agents, nhấn mạnh việc quản lý nhiều tầng bộ nhớ để vượt giới hạn context. citeturn2search0turn2search22

Thiết kế khuyến nghị:

- Working context: vài lượt gần nhất + state hiện tại.
- Short-term summary: tóm tắt cập nhật theo phiên.
- Long-term retrieval: chỉ kéo vào prompt những memories liên quan (theo vector similarity + time relevance + người dùng).

### Intent extraction

Mục tiêu intent extraction là “giải mã người dùng muốn gì” trước khi lập kế hoạch phản hồi/hành động.

**Danh mục intent cốt lõi (gợi ý)**
- chào hỏi / smalltalk
- hỏi đáp kiến thức (Q&A)
- hỏi về quá khứ (reference past interaction)
- yêu cầu robot làm hành động (move/gesture/play sound)
- thiết lập/đổi cài đặt (volume, quiet mode)
- cập nhật trí nhớ (user says: “hãy nhớ…”, “từ nay gọi tôi là…”)
- tình huống nhạy cảm (self-harm, bạo lực, quấy rối…)

**Hai cách triển khai**
1) LLM‑based classification (1 bước): yêu cầu LLM trả cả `intent` (enum) trong JSON output. Dùng Structured Outputs để tránh sai định dạng/enum đối với OpenAI. citeturn0search0  
2) Hybrid (khuyến nghị): dùng classifier nhẹ (cục bộ hoặc cloud rẻ) cho routing intent thô, sau đó mới gọi LLM “đắt” khi cần. Hướng tối ưu “đắt‑rẻ” này được khuyến nghị chung khi xây agent: đạt độ chính xác rồi tối ưu chi phí/độ trễ bằng cách thay mô hình lớn bằng mô hình nhỏ khi có thể. citeturn3search13turn2search2

## Hệ thống trí nhớ dài hạn, truy xuất và tích hợp theo sự kiện

### Mô hình trí nhớ theo tầng

Để đáp ứng: “nhớ người”, “tham chiếu quá khứ”, “duy trì tính cách”, nên tách trí nhớ thành các lớp:

- Episodic memory (ký ức sự kiện): các sự kiện có thời gian, bối cảnh, cảm xúc.
- Semantic memory (thông tin bền): tên người, sở thích, điều kiêng kỵ, thói quen, quan hệ.
- Interaction summaries: tóm tắt theo phiên/ngày/tuần.
- Identity memory: liên kết người ↔ profile ↔ các cuộc hội thoại.

Các khảo sát về memory mechanism của LLM agents nhấn mạnh sự cần thiết của các cơ chế lưu trữ ngoài mô hình (external memory) và các chiến lược truy xuất/ghi nhớ để duy trì hành vi nhất quán dài hạn. citeturn2search22

### Lưu trữ trên Android

**Local DB**  
Dùng Room (abstraction trên SQLite) để lưu cấu trúc dữ liệu (profiles, events, summaries, pointers). Room được mô tả là lớp trừu tượng trên SQLite giúp truy cập dữ liệu robust hơn. citeturn1search3turn1search24

**Vector index (tìm kiếm ngữ nghĩa)**  
Có 2 chiến lược:
- Local vector store (trên thiết bị): phù hợp hobby/riêng tư, nhưng tối ưu ANN khó hơn.
- Cloud vector store (qua Gateway): dễ mở rộng, nhưng tăng phụ thuộc mạng và rủi ro dữ liệu.

### Retrieval system theo RAG

Retrieval‑Augmented Generation (RAG) là nền tảng tốt cho “nhớ và tham chiếu tài liệu/nhật ký”: kết hợp “parametric memory” (trong mô hình) và “non‑parametric memory” (index/vector store) để tăng tính chính xác và cập nhật. citeturn0search2turn0search27

**Pipeline truy xuất**
1) Tạo truy vấn (query) từ: user utterance + intent + người + thời gian + state.
2) Retrieve top‑k memories bằng vector similarity (embedding) + filter theo `person_id`, `time_decay`, `salience`.
3) Re-rank (tuỳ chọn) theo:
   - gần thời gian (recency),
   - mức quan trọng (salience),
   - mức liên quan chủ đề (topic match).
4) “Memory packing”: nén các memory dài thành dạng bullet ngắn, có trích dẫn timestamp/nguồn nội bộ.

### Long‑term memory summarization

Nếu chỉ lưu raw transcript, context sẽ phình nhanh và retrieval dễ bị “nhiễu”. Do đó cần cơ chế tóm tắt định kỳ.

Hai khuyến nghị thiết kế dựa trên nghiên cứu:

- Recursive summarization: tóm tắt lặp, nén dần hội thoại cũ để duy trì khả năng đối thoại dài hạn và nhất quán. citeturn2search38turn2search14
- Memory tier management: quản lý nhiều tầng bộ nhớ và “đưa vào context khi cần”, tương tự cách MemGPT mô tả quản trị nhiều tầng lưu trữ để vượt giới hạn context. citeturn2search0

**Cơ chế đề xuất**
- Sau mỗi phiên: tạo `session_summary` (100–300 từ) + `facts_extracted` (json).
- Theo ngày/tuần: tạo `daily_digest` / `weekly_digest`, gắn tag theo người và chủ đề.
- Garbage collection: hạ độ ưu tiên các đoạn ít quan trọng; chỉ giữ embedding + summary.

### Event‑based memory integration

Robot “thú cưng” mạnh ở tương tác đa phương thức, nên ký ức không chỉ đến từ text mà còn từ sự kiện cảm biến.

**Event sources**
- Touch / petting event
- IMU: bị nhấc lên / rơi / rung
- Proximity: có người lại gần
- Camera emotion cues (tuỳ chọn)

Ví dụ với ML Kit Face Detection:
- ML Kit có thể phát hiện khuôn mặt, landmark/contour và một số biểu cảm như “smiling”, “eyes open/closed”, và tracking giữa các frame; nhưng không “nhận dạng người” (không làm face recognition). citeturn4search7turn4search0

**Event write rule**
- Ghi event khi: (a) có tương tác cảm xúc mạnh, (b) có câu “hãy nhớ…”, (c) có thay đổi sở thích, (d) có hành động robot thành công/thất bại.
- Event luôn kèm `actor` (ai), `valence` (tích cực/tiêu cực), `confidence`, `sensor_snapshot`.

### “Nhớ người” trong Phase 2

Có 3 mức, tăng dần độ khó/rủi ro riêng tư:

1) Mức đơn giản: “nhớ theo lời khai”  
   - Người dùng tự giới thiệu tên; robot lưu `name`, `preferred_pronoun`, `relationship`.  
   - Không cần sinh trắc, dễ nhất cho hobby.

2) Mức trung bình: speaker recognition (nhận dạng giọng nói)  
   - Ví dụ engine dạng enrollment → tạo voiceprint profile → recognition so khớp profile. Picovoice Eagle mô tả rõ 2 bước Enrollment và Recognition và cho phép lưu `Profile` để dùng về sau. citeturn4search2  
   - Lưu ý: đây là dữ liệu nhạy cảm; cần consent rõ ràng.

3) Mức nâng cao: face recognition on-device  
   - Cần mô hình embedding (FaceNet/TFLite/MediaPipe…), vì ML Kit face detection không nhận dạng danh tính. citeturn4search7  
   - Với Phase 2 hobby, nên tránh nếu chưa có kinh nghiệm bảo mật dữ liệu sinh trắc.

## Tính cách nhất quán, an toàn, và giới hạn tần suất

### Personality consistency system

Mục tiêu là robot “giống một cá thể” chứ không đổi giọng liên tục.

**Cấu trúc đề xuất: Personality Card (bền, versioned)**
- “core traits”: thân thiện, tò mò, nói ngắn, hài hước nhẹ
- “attachment style”: thích tương tác, nhưng tôn trọng không gian riêng
- “speech style”: xưng hô, câu cửa miệng, độ dài câu, emoji (nếu hiển thị text)
- “taboos”: không làm bác sĩ/luật sư; không đưa hướng dẫn nguy hiểm
- “capability boundaries”: robot không có tay thật (nếu không có), không ra quyết định thay cho người…

**Cơ chế enforcement**
- Prompt: luôn đưa Personality Card vào system/developer instructions.
- Post‑check: kiểm tra output có vi phạm style không (ví dụ: nói quá dài, đổi xưng hô).
- Memory coupling: sở thích người dùng và “mối quan hệ” được đưa vào prompt như biến.

### Safety rules

Thiết kế an toàn nên có nhiều lớp vì:
- LLM có thể tạo nội dung không phù hợp nếu bị “prompt injection” hoặc gặp tình huống nhạy cảm.
- Khi LLM được phép đề xuất tool/action, rủi ro tăng (đây là cảnh báo trực tiếp trong hướng dẫn an toàn cho agent/tool use). citeturn3search5

**Lớp an toàn nội dung**
- Pre‑moderation (input): kiểm duyệt lời người dùng trước khi gửi LLM khi cần.
- Post‑moderation (output): kiểm duyệt câu trả lời trước khi nói ra/hiển thị.
- OpenAI khuyến nghị dùng Moderation API (miễn phí) như một biện pháp giảm nội dung không an toàn. citeturn3search1turn3search3  
- OpenAI Usage Policies là “đường biên” pháp lý/chính sách cho các lớp chặn (ví dụ nội dung tự hại, bạo lực, gian lận…). citeturn3search0turn3search11  

**Lớp an toàn theo nền tảng Gemini**
- Gemini API có “Safety settings” điều chỉnh mức lọc theo các nhóm nội dung; và tài liệu nhấn mạnh chủ ứng dụng chịu trách nhiệm đánh giá rủi ro và dùng LLM an toàn. citeturn3search2turn3search31  
- Nếu chạy Gemini trên Vertex AI, có cấu hình safety filters/harm block thresholds theo phương pháp severity/probability. citeturn3search6  

**Lớp an toàn hành động vật lý**
- “Hard constraints”: không cho phép hành động có nguy cơ gây hại (chạy nhanh khi có người sát bên, cắn/giật mạnh, quay servo quá giới hạn…).
- “Soft constraints”: hành động lạ/hiếm → yêu cầu xác nhận người dùng.
- “Contextual constraints”: không di chuyển nếu đang bị bế, pin yếu, lỗi motor, hoặc cảm biến báo nguy cơ.

**Quy tắc riêng tư/consent, đặc biệt với micro**
- Android có cơ chế “privacy indicators” hiển thị khi ứng dụng dùng camera/microphone (ví dụ Android 12). citeturn1search0  
- Android cũng có thay đổi về foreground services liên quan truy cập micro/camera và yêu cầu khai báo “foreground service types” khi truy cập trong foreground service. citeturn1search6turn1search13  
Trong thiết kế robot, điều này nên được phản ánh thành UX rõ ràng: trạng thái “đang nghe” (LED/âm báo) và setting tắt mic nhanh.

### Rate limiting và retry

Vì hệ thống phụ thuộc API cloud, rate limiting phải là thành phần kiến trúc, không phải “chi tiết triển khai”.

- OpenAI có trang hướng dẫn rate limits cho API. citeturn0search3  
- Gemini API cũng công bố rate limits. citeturn0search1  
- OpenAI Cookbook khuyến nghị retry với random exponential backoff khi gặp rate limit errors. citeturn0search13  

**Thiết kế Rate Limit Manager (ở Gateway, hoặc Android nếu không có Gateway)**
- Token bucket theo:
  - user/session (tránh một người “spam”)
  - provider/model (mỗi model có giới hạn khác nhau)
  - loại tác vụ (Q&A vs chit-chat vs action planning)
- Backpressure UX: nếu sắp vượt quota → robot nói “mình cần chút thời gian” và chuyển sang chế độ offline fallback (smalltalk/động tác đơn giản).
- Circuit breaker: nếu provider A lỗi/429 liên tục → chuyển provider B, hoặc giảm tính năng.

## Dịch đầu ra LLM thành hành vi robot và vòng quyết định hành động

### Hợp đồng đầu ra chuẩn hoá từ LLM

Để “LLM đề xuất – Brain quyết định”, cần một output schema cố định để:

- parse chắc chắn,
- validate enum/range,
- kiểm soát quyền hành động,
- log/audit.

Với OpenAI, Structured Outputs có thể đảm bảo đáp ứng JSON Schema (giảm rủi ro thiếu key/enum sai). citeturn0search0turn3search5  
Với Gemini, function calling cho phép mô hình trả về cấu trúc tham số để “gọi hàm” (đề xuất hành động) thay vì text tự do. citeturn0search10  

Schema gợi ý (ví dụ):

```json
{
  "intent": "answer_question | greet | smalltalk | command_robot | recall_memory | update_profile | safety_sensitive",
  "emotional_tone": {
    "valence": "positive | neutral | negative",
    "energy": "low | medium | high",
    "style": "playful | calm | empathic | formal"
  },
  "suggested_speech": {
    "text": "string",
    "language": "vi",
    "tts_hints": {
      "rate": 1.0,
      "pitch": 1.0,
      "volume": 1.0
    }
  },
  "suggested_actions": [
    {
      "type": "gesture | move | light | sound | ask_user_confirm | tool_call",
      "name": "wag_tail | nod | look_at_user | play_chime | ...",
      "args": { "duration_ms": 1200 },
      "risk": "low | medium | high",
      "requires_confirmation": true
    }
  ],
  "memory_ops": [
    {
      "op": "write | update | no_op",
      "category": "episodic | semantic | preference",
      "content": "string",
      "tags": ["string"],
      "person_id_hint": "string"
    }
  ]
}
```

Điểm quan trọng: `suggested_actions` không phải “mệnh lệnh”, mà là “đề xuất”.

### Action gating: Brain quyết định có thực thi không

Policy Engine thực hiện đánh giá theo nhiều cổng:

1) Schema validation (đúng JSON, đúng enum, đúng range).  
2) Moderation/safety:
   - nếu intent thuộc nhóm nhạy cảm (self-harm, bạo lực, quấy rối…) → chặn hành động, chuyển sang kịch bản an toàn (supportive + khuyến nghị tìm trợ giúp). Các tài liệu safety best practices nhấn mạnh moderation + human oversight. citeturn3search1turn3search3  
3) Capability check: robot có action đó không? có đang bận không?
4) Environment check: obstacle, pin, nhiệt, chế độ im lặng.
5) Human confirmation: nếu `risk=high` hoặc `requires_confirmation=true` → hỏi người dùng lại trước khi làm.
6) Execution sandbox: chỉ gọi các action primitives đã whitelist.

Cách tiếp cận này phù hợp tinh thần “agent loop có tool/guardrails” và thực tế rủi ro prompt injection khi tool calls bị ảnh hưởng bởi text tuỳ ý; do đó cần cách ly và kiểm soát. citeturn3search5turn2search2  

### Robot Behavior Engine: dịch “ý định” thành hành vi

Đề xuất xây dựng một “Behavior Library” gồm các hành vi nguyên tử và hành vi tổ hợp:

**Atomic behaviors**
- `LOOK_AT_USER(angle)`
- `WAG_TAIL(speed, duration)`
- `NOD(times)`
- `LED_BREATHE(color?, tempo)`
- `PLAY_SOUND(id)`
- `SPEAK(text, tts_params)` (dùng Android TextToSpeech; lưu ý speak bất đồng bộ và nên dùng listener để biết khi nói xong). citeturn1search1turn1search18  

**Composite behaviors (macro)**
- `GREET_USER`: nhìn + vẫy đuôi + chime + nói
- `THINKING`: LED breathe + quay đầu nhẹ + “ừm…”
- `EMPATHY`: tone chậm + cúi đầu + nói ngắn

**Mapping gợi ý**
- `intent=greet` → `GREET_USER`
- `intent=answer_question` → `THINKING` (ngắn) → `SPEAK`
- `intent=recall_memory` → `SPEAK` + chèn mốc thời gian (“lần trước bạn nói…”) nếu retrieval confidence cao
- `intent=command_robot` → nếu action low‑risk: thực thi; nếu medium/high: xác nhận

### Vòng agent “Reasoning + Acting” nhưng có kiểm soát

Bạn có thể triển khai vòng lặp kiểu “reasoning + acting” (ReAct) ở mức kiến trúc: LLM xen kẽ lập luận và đề xuất hành động/tool, rồi nhận observation (kết quả tool) để tiếp tục. ReAct được mô tả là khung kết hợp reasoning và acting để tăng hiệu quả và giảm hallucination bằng cách tương tác công cụ/nguồn ngoài. citeturn2search1turn2search11

Trong robot, “tools” có thể là:
- `get_robot_state()`
- `get_user_profile(person_id)`
- `search_memory(query)`
- `execute_behavior(name,args)` (chỉ được gọi sau khi Brain phê duyệt, không để LLM gọi trực tiếp)

## Tradeoffs giữa cloud, hybrid, local và đề xuất tối thiểu cho hobby

### Fully cloud AI

**Ưu điểm**
- Chất lượng hội thoại cao nhất nhờ mô hình mạnh.
- Dễ dùng function calling/structured output của provider.
- Mở rộng nhanh (đổi model, thêm tool).

**Nhược điểm**
- Độ trễ mạng ảnh hưởng “tính thú cưng” (robot bị khựng).
- Phụ thuộc quota/rate limits (cần Rate Limit Manager nghiêm túc). citeturn0search3turn0search1  
- Rủi ro riêng tư: audio/transcript phải gửi lên cloud (cần consent, logging policies).

Phù hợp nếu mục tiêu chính là “trò chuyện thông minh” và bạn chấp nhận phụ thuộc mạng.

### Hybrid AI

**Ý tưởng**: chạy những thứ cần realtime và riêng tư trên Android, còn “lập luận sâu” gửi cloud.

Ví dụ phân chia:
- On-device: wake word/VAD, một số intent đơn giản, cache, memory store cục bộ.
- Cloud: LLM trả lời câu hỏi, lập kế hoạch hội thoại, đề xuất hành động phức tạp.

Hybrid thường là “điểm cân bằng” tốt cho robot: giảm độ trễ cảm nhận và giảm chi phí cloud bằng routing/tiers. Tài liệu agent building khuyến nghị tối ưu chi phí/latency bằng cách dùng mô hình nhỏ khi có thể sau khi đạt target accuracy. citeturn3search13turn2search2

### Local models (chạy trên thiết bị)

**Ưu điểm**
- Offline, riêng tư tốt hơn.
- Độ trễ ổn định.

**Nhược điểm**
- Hạn chế compute/bộ nhớ trên Android (đặc biệt nếu không phải flagship).
- Khó đạt chất lượng hội thoại như cloud LLM.
- Vẫn cần chiến lược memory tiers vì context window hữu hạn; các công trình về memory/agent nhấn mạnh việc quản trị bộ nhớ để vượt giới hạn context. citeturn2search0turn2search22  

Local phù hợp khi bạn ưu tiên offline/riêng tư, hoặc khi robot ở môi trường mạng kém. Tuy nhiên, với Phase 2 “hobby” muốn nhanh ra giá trị, hybrid thường thực dụng hơn.

### Tính năng tối thiểu Phase 2 cho hobby project

Mục tiêu MVP: hoạt động được, vui, ít rủi ro, ít phụ thuộc.

**Đề xuất tối thiểu (khuyến nghị theo thứ tự ưu tiên)**

1) Hội thoại voice cơ bản
- ASR: dùng SpeechRecognizer (nhanh triển khai), yêu cầu RECORD_AUDIO. citeturn1search2turn1search19  
- TTS: dùng Android TextToSpeech speak bất đồng bộ + listener. citeturn1search1turn1search18  

2) Tích hợp 1 provider LLM trước, nhưng thiết kế sẵn adapter
- Bắt đầu với OpenAI *hoặc* Gemini, nhưng code theo interface để sau này thêm provider còn lại.
- Dùng output JSON schema ngay từ đầu (OpenAI Structured Outputs nếu chọn OpenAI). citeturn0search0  

3) Memory tối thiểu nhưng “thấy ngay giá trị”
- Lưu `user_name`, `preferred_calling`, 5–20 “facts/preferences” (semantic).
- Lưu `session_summary` sau mỗi phiên (short summary).
- Lưu trong Room để đơn giản hóa persistence. citeturn1search3turn1search24  

4) “Nhớ người” ở mức đơn giản
- Người dùng nói “Mình là …” → lưu.
- (Tuỳ chọn) sau này mới thêm speaker recognition; Picovoice Eagle mô tả rõ enrollment/recognition nếu bạn muốn nâng cấp. citeturn4search2  

5) Hành vi robot tối thiểu (action library nhỏ)
- 5–10 hành vi: wag tail / nod / look / LED breathe / play chime.
- LLM chỉ đề xuất; Brain chỉ cho phép hành vi “low risk”.

6) Safety + rate limiting ở mức đủ dùng
- Moderation (ít nhất cho output nói ra). OpenAI mô tả Moderation endpoint dùng để phát hiện nội dung có hại và giúp can thiệp. citeturn3search3turn3search1  
- Rate limit + exponential backoff retry theo khuyến nghị cookbook. citeturn0search13turn0search3  

7) Event-based memory tối giản
- Ghi event mỗi khi: người dùng đặt tên, khen/chê, hoặc robot thực hiện hành động.
- Chỉ cần: timestamp, intent, outcome, valence.

Với bộ tối thiểu này, bạn đã đạt các yêu cầu Phase 2 cốt lõi: nói chuyện, trả lời câu hỏi, nhớ tên/ngữ cảnh, tham chiếu tương tác gần đây, và có tính cách nhất quán—nhưng vẫn giữ rủi ro thấp nhờ “LLM đề xuất, Android quyết định” và hợp đồng output có cấu trúc. citeturn3search5turn0search0turn0search10