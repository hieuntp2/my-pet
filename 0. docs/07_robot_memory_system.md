# Kiến trúc bộ nhớ cho robot thú cưng AI chạy trên Android

## Bối cảnh nghiên cứu và bài học thiết kế

### Robot đồng hành và AI companion robots
Một robot thú cưng “có ký ức” thường được người dùng cảm nhận qua ba năng lực: (i) nhận ra đúng người quen, (ii) nhớ lịch sử tương tác để cư xử khác nhau theo từng người, và (iii) tạo cảm giác “liên tục theo thời gian” (continuity of being). Những yêu cầu này xuất hiện rất rõ trong cách entity["company","Sony","electronics company"] mô tả robot chó Aibo: Aibo có thể học và nhận diện đến khoảng 100 khuôn mặt, phát triển “mức thân thuộc” theo thời gian, và trải nghiệm tương tác sẽ định hình hành vi (ví dụ gần gũi hơn với người thường xuyên thân thiện). citeturn13search0 Aibo cũng “học mặt” càng nhanh khi gặp thường xuyên, và có thể ghi nhớ khuôn mặt mà không cần người dùng thao tác gán nhãn từng lần. citeturn13search13 Đồng thời, Aibo có một giới hạn quan trọng về “khái niệm chủ”: Aibo có thể nhớ mặt dựa trên tần suất tương tác nhưng không tự xác nhận ai là chủ sở hữu. citeturn13search2  

**Bài học thiết kế:** “Chủ/khách lạ” không nên chỉ là bài toán nhận dạng khuôn mặt; cần một lớp “vai trò xã hội” (owner/household/visitor/stranger) và cơ chế xác nhận (explicit confirmation) để tránh sai lầm khó sửa.

### Agent hội thoại và conversational agents
Trong agent hiện đại (đặc biệt các agent dựa trên LLM), “ký ức dài hạn” thường được triển khai theo ba mảnh ghép: **(1) nhật ký trải nghiệm đầy đủ**, **(2) cơ chế truy hồi theo ngữ cảnh**, và **(3) tóm tắt/khái quát hoá qua thời gian**.

Một mẫu tham khảo ảnh hưởng rộng là kiến trúc “Generative Agents” (2023): agent lưu **memory stream** dưới dạng bản ghi trải nghiệm, truy hồi dựa trên **relevance–recency–importance**, và có khối **reflection** để tổng hợp ký ức thành kết luận cấp cao rồi đưa vào lập kế hoạch hành vi. citeturn7view0  
Các công trình gần đây tiếp tục hệ thống hoá ý tưởng này thành “hệ điều hành bộ nhớ” cho agent hội thoại: ví dụ MemoryOS đề xuất lưu trữ phân tầng (short/mid/long), kèm các mô-đun storage–updating–retrieval–generation để giữ tính nhất quán dài hạn. citeturn7view1  
Ngoài ra, trong thiết kế cho hội thoại dài kỳ, một hướng phổ biến là tách **short-term cache** và **long-term bank**: long-term lưu **vector của tóm tắt sự kiện/phiên**, short-term giữ ngữ cảnh “đang diễn ra”; khi vượt ngưỡng thời gian, short-term được tóm tắt thành “event record” đưa vào long-term, và truy hồi áp dụng cơ chế có **time decay**. citeturn15view0

**Bài học thiết kế:** Robot thú cưng không thể “nhét hết vào context”. Cần kiến trúc bộ nhớ có quy trình *ghi → chỉ mục → truy hồi → tóm tắt/khái quát → quay lại ảnh hưởng hành vi*.

### Game AI
Game AI lâu nay giải quyết “ký ức” theo hướng **bộ nhớ tác nghiệp** nhiều hơn là “tự truyện” (autobiographical). Hai pattern quan trọng:

- **Blackboard**: một mô hình “bảng đen” nơi nhiều mô-đun bơm/đọc trạng thái để phối hợp ra quyết định; kinh điển là blackboard architecture cho điều khiển, nhấn mạnh khả năng phối hợp nhiều nguồn tri thức và điều khiển hành vi dựa trên trạng thái chung. citeturn3search17  
- **Behavior Trees (BTs)**: thường dùng để mô hình hoá hành vi runtime; tài liệu thực hành của Game AI Pro bàn cả khía cạnh tối ưu bố trí node trong bộ nhớ để cải thiện hiệu năng. citeturn2search0  

Riêng mảng lập kế hoạch, GOAP được phổ biến mạnh trong entity["video_game","F.E.A.R.","2005 first-person shooter"] (GDC) như một cách dùng “world state” + mục tiêu để tự tìm chuỗi hành động. citeturn2search14

**Bài học thiết kế:** Robot thú cưng cũng cần “world state/working memory/blackboard” để phản ứng thời gian thực, nhưng “ký ức dài hạn” nên tách khỏi lớp điều khiển hành vi để tránh coupling và khó mở rộng.

### Embodied agents và robot thể hiện
Trong robot/cognitive architecture, bộ nhớ được nhìn như thành phần trung tâm giúp **điều phối dòng dữ liệu cảm biến–sự kiện**, liên kết biểu diễn cảm giác-vận động với ngữ nghĩa, và có cấu trúc **mang tính tập–theo thời gian** (episodic). citeturn14view2  
Một số hướng nhấn mạnh episodic memory để tận dụng “one-shot” và học từ trải nghiệm thực tế: framework EPIROME mô tả episodic memory cho robot dịch vụ với “life-long memory” có thể lưu và nạp lại trải nghiệm, và nhấn mạnh lợi ích one-shot học từ sự kiện cấp cao (kỹ năng, thay đổi môi trường, tương tác người). citeturn14view3  
Ở RL/embodied learning, “episodic control” mô phỏng trực giác dùng ký ức kiểu hippocampus để khai thác nhanh trải nghiệm có thưởng cao, giúp học nhanh hơn trong một số bài toán tuần tự. citeturn3search7  

Trong HRI/social robots, episodic memory hay được thiết kế thành lớp “ghi sự kiện + phân đoạn tương tác theo khoảng thời gian + liên kết” nhằm kể lại lịch sử, suy luận sở thích, và tạo kỳ vọng. citeturn17view0

**Bài học thiết kế:** Với robot thú cưng, “episodic memory” không chỉ để trả lời câu hỏi; nó là nền để tạo hành vi có “câu chuyện đời sống” (life history) và cá tính hoá theo người.

## Các loại bộ nhớ và ánh xạ sang robot

Khung dưới đây vừa bám định nghĩa từ khoa học nhận thức, vừa ánh xạ sang triển khai phần mềm.

### Short-term memory và Working memory
Mô hình multi-store (cảm giác → ngắn hạn → dài hạn) nhấn mạnh short-term giữ thông tin trong thời gian ngắn và là đầu vào để củng cố sang long-term. citeturn10search6  
Mô hình working memory của entity["people","Alan Baddeley","psychologist working memory"] và entity["people","Graham Hitch","psychologist working memory"] xem “ngắn hạn” không chỉ là kho chứa mà là hệ thao tác đa thành phần để giữ và xử lý thông tin trong lúc đang làm việc. citeturn10search1 Bản mở rộng “episodic buffer” mô tả cơ chế tạm thời kết dính thông tin đa nguồn thành một biểu diễn tập (episode) thống nhất. citeturn10search8  

**Trong robot thú cưng:**  
Short-term/working memory = cache ngữ cảnh hiện tại (ai đang ở trước mặt, đồ vật đang cầm, tương tác 30–120 giây gần nhất), phục vụ phản xạ tức thời và tạo event.

### Long-term memory
Long-term là nơi lưu bền vững; trong multi-store, đây là kho lưu sau quá trình củng cố. citeturn10search6 Ở góc nhìn hệ thống não, quan điểm “nhiều hệ bộ nhớ” (multiple memory systems) nhấn mạnh long-term không đơn nhất mà gồm nhiều dạng (khai báo/không khai báo…). citeturn10search3  

**Trong robot thú cưng:**  
Long-term memory = cơ sở dữ liệu bền (SQLite/Room) + chỉ mục vector + tóm tắt theo ngày/tuần + hồ sơ người/đồ vật.

### Episodic memory
entity["people","Endel Tulving","cognitive psychologist"] phân biệt episodic (nhớ sự kiện có bối cảnh thời gian–không gian) và semantic (tri thức khái quát). citeturn0search0turn0search3 Trong social robotics, episodic memory thường được triển khai như “nhớ chuỗi sự kiện theo ngữ cảnh”, và là nền để tạo tương tác dài hạn tự nhiên. citeturn14view1turn17view0  

**Trong robot thú cưng:**  
Episodic memory = “tập ký ức” (episode) gồm các event liên quan, có người tham gia, đồ vật liên quan, thời lượng, cảm xúc, và tóm tắt tự nhiên.

### Semantic memory
Semantic memory là tri thức dạng “biết rằng” (facts, khái niệm) tách khỏi bối cảnh một lần trải nghiệm cụ thể. citeturn0search0turn14view1  

**Trong robot thú cưng:**  
Semantic memory = hồ sơ ổn định (tên chủ, thói quen chủ, tên đồ chơi, “đây là bóng màu đỏ”, luật nhà), được học dần từ episodic + gán nhãn.

### Procedural memory
Quan điểm phân chia declarative vs nondeclarative nhấn mạnh procedural là kỹ năng/habit, khó mô tả bằng hồi tưởng có ý thức nhưng định hình hành vi. citeturn0search5turn10search3  

**Trong robot thú cưng:**  
Procedural memory = policy/skill đã học (đi theo chủ, làm trò, né vật cản, phản hồi khi được vuốt ve). Nó không nhất thiết nằm trong DB; thường là tham số mô hình, cây hành vi, hoặc rule-set được cập nhật theo trải nghiệm.

## Kiến trúc bộ nhớ theo hướng sự kiện

### Lý do chọn event-based + event sourcing
Event sourcing lưu mọi thay đổi trạng thái như **chuỗi sự kiện append-only**, từ đó có thể truy vấn “đã xảy ra gì” và tái dựng trạng thái tại bất kỳ thời điểm nào. citeturn0search12turn8search1 Ở góc nhìn hệ thống dữ liệu, “log” (chuỗi bản ghi có thứ tự, chỉ thêm) được xem là một trừu tượng hợp nhất cho nhiều hệ thống (DB, replication, xử lý realtime). citeturn8search0  

**Ánh xạ sang robot thú cưng:** cảm biến và tương tác vốn là *dòng sự kiện*. Nếu kiến trúc bộ nhớ lấy event log làm “source of truth”, ta dễ:
- dựng lịch sử đời sống (life history),
- chạy “projector” tạo view: danh bạ người quen, đồ vật quen, thống kê tương tác,
- thêm cơ chế quên/tóm tắt mà không phá dữ liệu gốc.

### Ràng buộc triển khai trên Android và offline
Pha 1 offline cần một DB cục bộ ổn định; Room cung cấp lớp trừu tượng trên SQLite để lưu dữ liệu có cấu trúc và hỗ trợ offline. citeturn8search12  
Vì dữ liệu khuôn mặt/embedding mang tính sinh trắc và có thể dùng để nhận dạng duy nhất, nên phải coi như dữ liệu nhạy cảm; GDPR định nghĩa biometric data và xếp “biometric data dùng để định danh” vào nhóm dữ liệu nhạy cảm (special category). citeturn12search4turn12search0 Với template sinh trắc, hướng dẫn của EU cũng mô tả “biometric template” là đặc trưng trích xuất từ dữ liệu thô để lưu và xử lý về sau. citeturn12search5  
Trên Android, kho khoá hệ thống cho phép lưu khoá mật mã để khó trích xuất hơn và dùng cho thao tác mã hoá/giải mã mà vật liệu khoá không xuất ra ngoài. citeturn12search2  

**Hệ quả thiết kế:** Pha 1 nên mã hoá DB/embedding (hoặc ít nhất mã hoá cột embedding/ảnh mẫu), và thiết kế “xoá dữ liệu người” thành tính năng sản phẩm chứ không chỉ kỹ thuật. citeturn14view1turn12search2

### Kiến trúc logic tổng quan
Sơ đồ dưới minh hoạ luồng điển hình: cảm biến → nhận thức (perception) → phát sự kiện → ghi log → dựng chỉ mục/view → truy hồi để điều khiển hành vi + hội thoại.

```mermaid
flowchart LR
  S[Sensor streams\n(camera/mic/touch/imu)] --> P[Perception pipeline\n(face/object/speech/emotion)]
  P --> EB[Event Bus\n(Kotlin Flow / in-app pubsub)]
  EB --> EL[Event Log\nappend-only (Room/SQLite)]
  EL --> PRJ[Projectors\n(index + aggregates)]
  PRJ --> RM[Read Models\npersons/objects/episodes/summaries]
  RM --> RET[Memory Retrieval\n(context -> top-K memories)]
  RET --> ACT[Behavior & Dialogue\n(personality + policy)]
  ACT --> EB
```

## Thiết kế chi tiết các thành phần bộ nhớ

### Event log system
**Chức năng:** ghi lại mọi “điều đáng biết” dưới dạng event bất biến; coi đây là nguồn sự thật để tái dựng. citeturn0search12turn8search1  

**Nguyên tắc thiết kế:**
- **Append-only, có thứ tự thời gian** để truy vết và tái dựng. citeturn8search0turn8search1  
- **Tách payload gọn** (JSON/protobuf) khỏi media nặng (ảnh/audio clip). Media nên lưu file + hash + đường dẫn trong payload.
- **Có phiên (session_id)** để gom các event gần nhau thành một mạch tương tác.

**Event taxonomy gợi ý (Pha 1, offline):**
- Perception: `FaceDetected`, `FaceEmbeddingComputed`, `PersonMatched`, `PersonUnknown`, `ObjectDetected`, `SpeechRecognized`, `TouchDetected`.
- Interaction: `Greeting`, `PlayStarted`, `PlayEnded`, `CommandGiven`, `CommandSucceeded/Failed`.
- Internal state: `BatteryLow`, `ChargingStarted`, `SleepModeEntered`.

### Memory indexing system
**Chức năng:** biến event log thành các “read models” truy vấn nhanh: danh bạ người, danh sách đồ vật, lịch sử tương tác, episodic memory, và tóm tắt.

**Thiết kế theo projector/materialized view:**
- Projector chạy nền, “nghe” event bus hoặc quét log theo offset, cập nhật bảng persons/objects/interaction_history. Đây là cách tách lớp ghi (events) và lớp đọc (views), phù hợp event sourcing/CQRS. citeturn8search1turn0search12  
- Chỉ mục chính:
  - **Time index**: theo `ts` để truy hồi “lần gần nhất”.
  - **Entity index**: event liên quan person/object nào.
  - **Text index**: tóm tắt hoặc transcript để tìm theo từ khoá (FTS5).
  - **Vector index**: embedding (khuôn mặt; về sau có thể thêm embedding cho episode summaries). Mẫu “lưu vector của tóm tắt sự kiện” đã phổ biến trong agent hội thoại dài hạn. citeturn15view0  

### Person recognition memory
**Pipeline nhận diện (offline, Android):**
1. Detect face (bounding box, landmark).
2. Chuẩn hoá/cắt mặt (alignment nhẹ).
3. Tạo embedding bằng mô hình on-device.
4. So khớp embedding với kho người đã biết.
5. Phát event kết quả + cập nhật “thân thuộc”.

**Lựa chọn mô hình embedding:**  
FaceNet là ví dụ kinh điển: học ánh xạ ảnh mặt → không gian Euclid gọn, nơi khoảng cách biểu thị độ giống mặt. citeturn4search8 Với thiết bị di động, MobileFaceNets được thiết kế siêu nhẹ cho xác thực thời gian thực, báo cáo độ trễ nhanh trên điện thoại và kích thước nhỏ (cỡ vài MB). citeturn5search4  
Lưu ý: ML Kit face detection chỉ *phát hiện* mặt và đặc trưng khuôn mặt, **không** nhận dạng người; nên vẫn cần model embedding riêng. citeturn4search3  

**Cấu trúc memory cho người:**
- Hồ sơ person (tên, vai trò, mức thân thuộc).
- Nhiều embedding mẫu (đại diện các góc mặt/ánh sáng).
- 1 embedding “centroid” (trung tâm) để truy vấn nhanh.

### Object memory
Pha 1 offline thường khó làm “instance recognition” bền vững (nhận ra đúng *một quả bóng cụ thể* thay vì “bóng” nói chung) nếu không có embedding đa phương thức mạnh; nên thiết kế theo 2 tầng:

- **Semantic object (tầng khái niệm):** nhãn loại vật (ball, bowl, toy) + thuộc tính đơn giản (màu, kích thước). TFLite có hướng dẫn/khung triển khai object detection on mobile. citeturn4search14  
- **Instance object (tầng thực thể):** chỉ lưu các “đồ vật quan trọng” (đồ chơi yêu thích, bát ăn) bằng ảnh mẫu + vài fingerprint nhẹ (ví dụ embedding ảnh patch nếu có model phù hợp), và liên kết với event sử dụng/ơi gặp.

**Thực tế sản phẩm:** nếu cố làm instance-level cho mọi đồ vật ngay Pha 1, tỉ lệ nhầm lẫn và chi phí compute sẽ cao; nên “đồ vật quan trọng” phải có cơ chế gán nhãn/đặt tên (human-in-the-loop) giống “nhớ người mới”.

### Interaction history
Interaction history là lớp trung gian giữa raw events và episodic memory: nó gom các event thành “phiên tương tác” có ý nghĩa.

Một thiết kế phù hợp HRI là mô hình “event history + interaction episodes + liên kết”: lưu event nhiều thang thời gian và định nghĩa episode như khoảng thời gian gắn với tương tác do robot hoặc người khởi xướng, rồi tạo liên kết giữa event và các đoạn trong episode. citeturn17view0  

**Trong robot thú cưng:**
- `interaction_session`: (start_ts, end_ts, participants, summary, valence).
- `interaction_turns`: (ai làm gì, khi nào) hoặc chỉ giữ event_id range.
- “Valence/cảm xúc” có thể lấy từ tín hiệu đơn giản (giọng vui, vuốt ve nhiều, chơi lâu) trước khi dùng mô hình phức tạp.

### Memory retrieval system
**Mục tiêu:** từ ngữ cảnh hiện tại (đang thấy ai/đồ gì/đang chơi gì) truy hồi đúng ký ức để:
- chào đúng người,
- nhắc lại “lần trước mình chơi gì”,
- điều chỉnh hành vi theo lịch sử.

**Công thức truy hồi khuyến nghị:** kết hợp **relevance + recency + importance**, giống mô hình truy hồi ký ức trong Generative Agents. citeturn7view0 Ở agent hội thoại, retrieval còn thường bổ sung **time decay** để ưu tiên ký ức gần, và cơ chế “no relevant memory” nếu không đủ tin cậy. citeturn15view0  

**Retrieval pipeline (offline Pha 1):**
1. Tạo “query context” từ working memory: person_id (nếu nhận ra), object_id (nếu có), interaction_mode (play/feed), thời điểm, địa điểm trong nhà (nếu có).
2. Candidate retrieval:
   - theo person_id: lấy 5–20 episode gần nhất + top episode quan trọng,
   - theo object_id: lấy các lần dùng gần nhất,
   - theo từ khoá (nếu có transcript): FTS.
3. Rank:
   - recency score: hàm giảm theo thời gian,
   - importance score: (điểm “mốc lần đầu”, thời lượng dài, cảm xúc mạnh),
   - relevance score: match theo entity + mode.
4. Trả về top-K “memory cards” cho hệ hành vi/hội thoại.

## Lược đồ cơ sở dữ liệu và chiến lược embedding

### Database schema đề xuất (Room/SQLite)
Lược đồ dưới đây tối ưu cho: offline, event-based, dễ mở rộng sang cloud và vector search.

#### Bảng `persons`
| Cột | Kiểu | Ghi chú |
|---|---:|---|
| person_id | TEXT (UUID) | PK |
| display_name | TEXT | “Chủ”, “John”, “Cô Lan”… |
| role | TEXT | OWNER / HOUSEHOLD / VISITOR / UNKNOWN |
| created_at | INTEGER | epoch ms |
| last_seen_at | INTEGER | epoch ms |
| familiarity_score | REAL | [0..1] |
| positive_interactions | INTEGER | đếm tổng |
| negative_interactions | INTEGER | đếm tổng |
| notes | TEXT | ghi chú ngắn (optional) |

**Cơ sở thiết kế “thân thuộc”:** Aibo mô tả “familiarity phát triển theo thời gian và trải nghiệm định hình hành vi”, phù hợp để tách một trường familiarity khỏi nhận dạng thuần tuý. citeturn13search0turn13search13

#### Bảng `objects`
| Cột | Kiểu | Ghi chú |
|---|---:|---|
| object_id | TEXT (UUID) | PK |
| canonical_label | TEXT | ví dụ “ball”, “food_bowl” |
| nick_name | TEXT | “Bóng đỏ”, “Bát ăn” |
| created_at | INTEGER |  |
| last_seen_at | INTEGER |  |
| importance_score | REAL | [0..1], chỉ đồ quan trọng |
| attr_json | TEXT | màu, kích thước, v.v. |

#### Bảng `embeddings`
Một bảng “đa mục đích” để lưu embedding cho người/đồ vật/episode summary.

| Cột | Kiểu | Ghi chú |
|---|---:|---|
| embedding_id | TEXT (UUID) | PK |
| owner_type | TEXT | PERSON / OBJECT / EPISODE |
| owner_id | TEXT | FK mềm tới persons/objects/interaction_history |
| modality | TEXT | FACE / IMAGE / TEXT |
| model_name | TEXT | “MobileFaceNet”, “FaceNet”… |
| model_version | TEXT | để migrate |
| dim | INTEGER | số chiều |
| vector_blob | BLOB | float32[] (hoặc int8 nếu quantize) |
| normalized | INTEGER | 0/1 (đã L2-normalize) |
| quality | REAL | [0..1] |
| created_at | INTEGER |  |
| sample_uri | TEXT | đường dẫn ảnh mẫu (nếu lưu) |

**Vì sao lưu blob + metadata:** FaceNet mô tả embedding như không gian Euclid gọn để so khoảng cách; việc lưu model_name/version giúp tránh “so sai không gian” khi cập nhật model. citeturn4search8

#### Bảng `events` (event log)
| Cột | Kiểu | Ghi chú |
|---|---:|---|
| event_id | TEXT (UUID) | PK |
| ts | INTEGER | epoch ms |
| event_type | TEXT | enum string |
| session_id | TEXT | để gom |
| payload_json | TEXT | dữ liệu sự kiện |
| person_id | TEXT | nullable (gợi ý index) |
| object_id | TEXT | nullable |
| embedding_id | TEXT | nullable |
| importance | REAL | [0..1] |

**Nguyên tắc:** event sourcing lưu thay đổi dưới dạng chuỗi event append-only. citeturn0search12turn8search1

#### Bảng `interaction_history` (episodes/sessions)
| Cột | Kiểu | Ghi chú |
|---|---:|---|
| episode_id | TEXT (UUID) | PK |
| start_ts | INTEGER |  |
| end_ts | INTEGER |  |
| primary_person_id | TEXT | nullable |
| episode_type | TEXT | PLAY / GREET / FEED / TRAIN / OTHER |
| valence | REAL | [-1..1] |
| importance | REAL | [0..1] |
| summary_text | TEXT | câu tóm tắt tự nhiên |
| summary_embedding_id | TEXT | nullable (text embedding) |

Thiết kế episode dựa trên khoảng thời gian tương tác và liên kết event↔episode là hướng đã được mô tả rõ trong episodic memory cho social robots. citeturn17view0

#### Bảng `memory_summaries`
| Cột | Kiểu | Ghi chú |
|---|---:|---|
| summary_id | TEXT (UUID) | PK |
| period_type | TEXT | DAILY / WEEKLY / IMPORTANT |
| period_start | INTEGER |  |
| period_end | INTEGER |  |
| summary_text | TEXT |  |
| embedding_id | TEXT | nullable |
| created_at | INTEGER |  |

### Lưu trữ và so khớp face embeddings
#### Cách lưu
- Lưu vector float32 đã **L2-normalize** (normalized=1). Với embedding đã chuẩn hoá, cosine similarity trở thành dot product; thực hành này rất phổ biến trong face recognition. citeturn5search2turn5search1  
- Lưu **nhiều mẫu**/một người (5–30 embeddings tuỳ), vì ánh sáng/góc nhìn thay đổi; MobileFaceNets được thiết kế cho xác thực thời gian thực trên di động nên phù hợp tạo nhiều mẫu mà vẫn nhẹ. citeturn5search4  
- Duy trì **centroid embedding** (trung bình có chuẩn hoá lại) để prefilter nhanh.

#### Cách so khớp
1. Từ frame hiện tại tạo embedding `e`.
2. Tính cosine similarity với centroid (hoặc top mẫu) của mọi person:
   - `sim = dot(e, centroid_p)` nếu đã normalize.
3. Lấy `best = argmax sim`.
4. Quyết định theo 2 ngưỡng:
   - `sim >= T_accept`: nhận là người đã biết.
   - `sim <= T_reject`: coi là người lạ.
   - vùng giữa: yêu cầu thêm bằng chứng (thêm vài frame, hoặc hỏi xác nhận).

FaceNet mô tả khoảng cách trong không gian embedding tương ứng độ giống để làm verification/recognition. citeturn4search8 Việc chọn ngưỡng cosine ảnh hưởng mạnh tới trade-off false accept/false reject; các phân tích về ngưỡng cosine thường nhấn mạnh tính bất biến theo độ dài vector và sự nhạy của threshold. citeturn5search2  

#### Lưu ý bảo mật
Embedding khuôn mặt có thể được xem như “biometric template”; nên cần mã hoá và quản lý vòng đời (xem/xoá) rõ ràng. citeturn12search5turn12search2

### Hệ thống nhớ người mới, cập nhật thân thuộc, phân biệt người lạ

#### Nhớ người mới (onboarding flow)
- Khi `sim <= T_reject` trong nhiều frame liên tiếp, phát `PersonUnknownObserved`.
- Robot hỏi: “Mình chưa gặp bạn bao giờ, bạn tên gì?” (hoặc chủ robot xác nhận).
- Khi nhận tên:
  - tạo `persons` record,
  - lưu 5–10 embedding chất lượng cao trong vòng 10–20 giây,
  - đặt `role=VISITOR` (hoặc HOUSEHOLD nếu chủ chọn),
  - khởi tạo `familiarity_score` thấp (ví dụ 0.1).

Cách “học mặt không cần can thiệp nhiều, gặp càng thường càng nhận nhanh” là hành vi người dùng đã quen trong Aibo, nên onboarding nên tối giản và ưu tiên “mặc định tự học, khi cần thì hỏi tên”. citeturn13search13turn13search2

#### Cập nhật familiarity score (gợi ý công thức)
Ta cần vừa “tăng khi tương tác tốt”, vừa “giảm dần khi lâu không gặp” (time decay). Agent hội thoại dài hạn cũng thường kết hợp time decay trong retrieval và cập nhật bank. citeturn15view0  

Gợi ý:
- `familiarity = clamp01( familiarity * exp(-Δt/τ) + α * interaction_strength )`
- `interaction_strength = w_dur * log(1+minutes) + w_val * max(0,valence) + w_freq * 1{seen_today}`

Trong đó:
- `τ` có thể 30–90 ngày cho visitor, 180+ ngày cho owner/household.
- `valence` lấy từ tín hiệu đơn giản (giọng vui, vuốt ve, chơi lâu) ở Pha 1; về sau có thể dùng mô hình affect.

#### Phát hiện stranger vs known persons
Dùng 3 lớp quyết định:
1. **Known** nếu `sim >= T_accept` ổn định N frame (giảm false accept).
2. **Stranger** nếu `sim <= T_reject` ổn định N frame.
3. **Uncertain** (vùng xám):  
   - đợi thêm bằng chứng (thêm mẫu),  
   - hoặc hỏi chủ: “Có phải bạn A không?”

Lý do cần vùng xám: social robot interaction dài hạn nhấn mạnh yêu cầu truy cập đúng “trải nghiệm quá khứ” để tương tác tự nhiên; sai người sẽ phá niềm tin nhanh. citeturn14view1turn17view0

## Ký ức theo tập, tóm tắt và tích hợp tính cách

### Cấu trúc episodic memory cho trải nghiệm quan trọng
#### Khi nào tạo episode?
Không phải mọi event đều đáng thành “kỷ niệm”. Social robotics đặt câu hỏi “làm sao gán mức liên quan cho episode theo novelty/cảm xúc” như một vấn đề mở. citeturn14view1 Vì vậy cần heuristic rõ ràng:

Trigger importance cao nếu:
- lần đầu gặp người (first-seen),
- lần đầu thấy đồ vật quan trọng,
- tương tác dài bất thường,
- cảm xúc rất tích cực/tiêu cực,
- mốc thay đổi (chuyển nhà, thêm thành viên).

#### Cách lưu episode (ví dụ “Met John…”)
Ví dụ yêu cầu: “Met John at 7pm. John played with me for 10 minutes.”

**Cách biểu diễn đề xuất:**
- `interaction_history` row:
  - `start_ts = 19:00`, `end_ts = 19:10`
  - `primary_person_id = John`
  - `episode_type = PLAY`
  - `valence = +0.7`
  - `importance = 0.6`
  - `summary_text = "Gặp John lúc 19:00. John chơi với mình 10 phút."`
- Liên kết event:
  - events trong khoảng [start_ts, end_ts] (FaceDetected/PersonMatched/PlayStarted/PlayEnded/TouchDetected…)
  - có thể lưu danh sách event_id hoặc lưu range theo thời gian/session_id.

Thiết kế này bám sát mô hình “event history + interaction episodes + links” để tạo “narrative” và truy vết. citeturn17view0  

#### Cách truy hồi episodic memory
Truy hồi theo 3 đường:
1. **Theo người:** `primary_person_id = John` → lấy episode gần nhất + episode quan trọng.
2. **Theo thời gian:** “hôm qua” → lấy daily summary + episode trong ngày.
3. **Theo ngữ nghĩa:** nếu có embedding cho `summary_text`, truy vấn vector để tìm “chơi”, “đi dạo”, “tập lệnh”… (Pha 2 mạnh hơn).

Cách chấm điểm relevance–recency–importance đã được mô tả rõ trong kiến trúc agent có memory stream. citeturn7view0

### Memory summarization system
Tóm tắt là bắt buộc vì “đời sống” có quá nhiều event. Agent hội thoại và social robots đều dùng tóm tắt (summarization/diary/reflection) để biến log thành ký ức cấp cao. citeturn7view0turn11search9  

#### Daily summary (offline Pha 1)
- Input: tất cả episode trong ngày + top-N event quan trọng.
- Output template (rule-based):
  - “Hôm nay mình gặp {A,B}. Chơi với {A} {10 phút}. Mình học {1 trò mới}…”
- Lưu vào `memory_summaries` với `period_type=DAILY`.

#### Weekly summary (offline Pha 1, đơn giản)
- Tổng hợp từ 7 daily summary + thống kê:
  - người gặp nhiều nhất,
  - đồ chơi yêu thích,
  - tổng thời gian chơi.
- Output: 5–10 câu template.

#### Important life events
- Khi `importance >= T_life_event`, tạo `memory_summaries(period_type=IMPORTANT)` và “pin” không bị xoá theo retention thông thường.

Trong các hệ dùng LLM, “reflection” là bước tổng hợp ký ức thành kết luận dài hạn; dù Pha 1 chưa dùng LLM, vẫn nên giữ khung dữ liệu để Pha 2 gắn vào. citeturn7view0turn7view1  

### Tương tác giữa memory và personality system
Một hệ social robot hiện đại thường nối “memory ↔ affect/personality” để hành vi có tính xã hội và nhất quán. Trong mô tả về Aibo, “familiarity và trải nghiệm định hình hành vi” là ví dụ trực tiếp của memory → hành vi/xã hội tính. citeturn13search0turn13search13  
Ở robot xã hội dùng LLM, Nadine mô tả việc truy hồi episodic memory theo user đã nhận ra, đồng thời tạo trạng thái cảm xúc nội tại bị kích bởi tương tác và dùng nó để sinh hành vi. citeturn20view0  

**Thiết kế đề xuất cho robot thú cưng:**
- Tách 2 lớp:
  - **Global personality** (mức năng lượng, tò mò, nhút nhát…): thay đổi chậm.
  - **Relationship profile theo person**: `affinity`, `trust`, `playfulness_bias`, `avoidance`.
- Luật cập nhật:
  - mỗi episode cập nhật profile theo `valence`, `duration`, `pattern`,
  - `affinity` tăng khi tương tác tích cực lặp lại,
  - `avoidance` tăng khi episode tiêu cực (la mắng, giật đồ) lặp lại.
- Khi policy quyết định hành vi: dùng `global_personality ⊕ relationship_profile(person)` để chọn mức thân mật, khoảng cách, thời lượng tương tác.

### Minimal Memory System cho Phase 1 MVP (offline)
Pha 1 cần tối thiểu hoá phạm vi nhưng vẫn tạo “ảo giác ký ức” rõ rệt như Aibo: nhận ra chủ, nhớ khách quen, và cư xử khác theo mức thân thuộc. citeturn13search0turn13search2  

**MVP đề xuất (đủ dùng, ít rủi ro):**
- Event log append-only trong Room/SQLite (bảng `events`), chỉ lưu payload gọn; ảnh mẫu mặt lưu file cục bộ (tuỳ chọn). citeturn8search12turn8search1  
- Person memory:
  - Face detection + embedding on-device (MobileFaceNets/FaceNet TFLite),
  - Lưu 5–20 embedding/người,
  - Matching tuyến tính (brute force) vì quy mô nhỏ (≤100 người là mục tiêu thực tế của Aibo). citeturn13search0turn5search4turn4search8  
- Familiarity score + role (OWNER/VISITOR/UNKNOWN) + xác nhận thủ công khi vùng xám.
- Interaction history cơ bản:
  - tự động tạo session khi phát hiện người + có tương tác (chạm/nói/chơi),
  - lưu `summary_text` bằng template,
  - cho phép truy vấn “lần cuối gặp ai”.
- Daily summary template (không cần LLM), chạy khi robot “ngủ/đang sạc”.
- Bảo mật tối thiểu:
  - dùng Android Keystore để quản lý khoá và mã hoá dữ liệu nhạy cảm (embedding/ảnh mẫu), kèm tính năng xoá người khỏi bộ nhớ. citeturn12search2turn12search5  

**Để Phase 2 (cloud optional)**
- Sync event log lên cloud (opt-in), chạy LLM để:
  - tóm tắt giàu ngữ nghĩa (weekly/life story),
  - embedding cho episode summaries để truy hồi theo ngữ nghĩa,
  - học sở thích dài hạn tinh hơn.
- Nhưng vẫn giữ nguyên “event log là source of truth” để không khoá chặt vào cloud và giảm rủi ro riêng tư. citeturn0search12turn8search0turn14view1