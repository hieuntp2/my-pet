# Object Recognition Architecture Note

## 1. Mục tiêu

Tài liệu này là bước đầu tiên để định hình lại hướng cải thiện **object recognition** cho Android AI Pet App, trước khi tách thành implementation plan và các task nhỏ cho AI coding agent.

Mục tiêu sản phẩm không chỉ là “detect object trong ảnh”, mà là:
- pet nhận ra có **đồ vật đáng chú ý** trong khung hình
- pet biết đồ vật đó có đang **gần một khuôn mặt / một người** hay không
- pet giảm hỏi sai, giảm nhiễu, giảm false positive
- pet có thể tiến dần đến việc **hỏi tên** hoặc **ghi nhớ** object mới theo đúng kiến trúc offline-first, event-driven

---

## 2. Bối cảnh hiện tại

Theo tài liệu hiện có, perception pipeline hiện tại đang đi theo hướng:
- CameraX chạy nền, không preview, 640×480, dùng `ImageAnalysis` với `STRATEGY_KEEP_ONLY_LATEST`
- `FrameAnalyzer` xử lý song song face detection và object detection
- face dùng `FaceDetectionPipeline (ML Kit)` + `TfliteFaceEmbeddingEngine` + `PersonRecognitionService`
- object dùng `ObjectDetectionEngine (TFLite)`; object lạ hiện tại đi thẳng vào luồng `UNKNOWN_OBJECT_DETECTED` rồi mở `TeachUnknownDialog("What is this?")` sau khi qua cooldown theo label【turn3file11†L1-L33】【turn3file6†L1-L34】.

Điểm mạnh của hướng hiện tại:
- đã đúng kiến trúc **Camera/Input → Perception → Events → Memory → Brain → Avatar/UI**【turn3file8†L1-L33】
- đã có luồng event-first
- đã có phân biệt sơ bộ giữa known object và unknown object
- đã có teach dialog thực tế, không phải mock【turn3file7†L1-L33】

Điểm yếu cốt lõi:
- object hiện vẫn gần như là **class detection + label cooldown**
- chưa thấy một lớp **object continuity / tracking** đủ rõ
- chưa có lớp **object candidate memory** tương đương hướng đã bắt đầu xuất hiện ở face
- chưa có logic **face-object spatial association**
- chưa có tiêu chí đủ mạnh để quyết định object nào thật sự “đáng hỏi” và object nào chỉ là background noise【turn3file10†L1-L44】

---

## 3. Vấn đề sản phẩm thực sự cần giải

Yêu cầu của project không phải chỉ là:
- “camera thấy cái cốc”

Mà gần hơn với:
- “pet thấy một vật thể đáng chú ý đang ở gần mặt/người”
- “pet biết vật đó có liên quan đến người đang xuất hiện hay chỉ là vật nền”
- “pet chỉ hỏi khi nó đủ chắc đây là một object đáng nhớ”
- “về lâu dài pet có thể nhớ object cụ thể, không chỉ label class”

Do đó, bài toán phải được tách thành 4 lớp khác nhau:

### 3.1 Object detection
Phát hiện bounding box + label + score của object trong một frame.

### 3.2 Object understanding in context
Đánh giá object đó có:
- đủ lớn
- đủ rõ
- đủ ổn định qua nhiều frame
- nằm gần face/person hay không
- đang được cầm / đặt cạnh / chỉ là background

### 3.3 Object candidate decision
Quyết định object này là:
- known object
- uncertain object
- unknown-but-interesting object
- ignorable background object

### 3.4 Object memory
Nếu sau này muốn “nhớ đúng món đồ cụ thể”, hệ phải có thêm memory layer riêng cho object instance, không thể chỉ dựa vào detector class label.

---

## 4. Chẩn đoán nguyên nhân object recognition đang yếu

Dựa trên current flow và yêu cầu nghiên cứu đã xác định, chất lượng object hiện tại nhiều khả năng yếu vì kết hợp nhiều nguyên nhân, không phải chỉ một threshold sai:

### 4.1 Mới dừng ở class-level detection
Detector TFLite chỉ cho biết nhãn tổng quát kiểu bottle, cup, cat... nhưng không đủ để biết đó là **món đồ cụ thể nào** hoặc có thật sự đáng dạy tên hay không【turn3file6†L1-L34】.

### 4.2 Thiếu temporal continuity
Nếu không có tracking / smoothing đủ mạnh, object xuất hiện chớp nhoáng vài frame cũng có thể bị coi là một detection đáng tin.

### 4.3 Thiếu spatial reasoning
Yêu cầu “đồ vật bên cạnh khuôn mặt” đòi hỏi so vị trí object box với face/person box. Hiện tại tài liệu chưa cho thấy lớp suy luận này đã tồn tại trong runtime pipeline【turn3file10†L21-L44】.

### 4.4 Unknown object đang quá gần popup logic
Hiện unknown object chủ yếu theo nhánh:
- detector ra label
- kiểm tra per-label cooldown
- publish `UNKNOWN_OBJECT_DETECTED`
- bật ask dialog【turn3file7†L1-L33】

Flow này quá mỏng đối với object, vì object nhiễu nhiều hơn face.

### 4.5 Chưa có object candidate memory
Face đã có hướng `CandidatePersonSession` để gom bằng chứng trước khi hỏi. Object hiện tại chưa có một cấu trúc tương đương rõ ràng trong docs hiện có【turn3file11†L17-L33】.

### 4.6 Có thể còn vấn đề model-fit
Ngay cả khi pipeline app logic tốt hơn, model object detector hiện tại cũng có thể chưa phù hợp cho:
- đồ vật nhỏ
- đồ vật bị che một phần
- đồ vật trong indoor scene gần mặt người
- class set chưa sát với đồ vật mà pet cần quan tâm

---

## 5. Kiến trúc đề xuất

Tôi đề xuất không nhảy ngay sang một model quá nặng hoặc cloud-first. Với Phase 1, hướng đúng hơn là giữ Android offline-first, và nâng object pipeline theo 2 giai đoạn.

### 5.1 Kiến trúc ngắn hạn (nên làm ngay)

Mục tiêu: biến object detection hiện tại từ “raw detector + cooldown” thành “detector + continuity + context + candidate gating”.

#### Luồng đề xuất

```text
Camera frame
  -> Face detection / primary face region
  -> Object detection (cadence thấp hơn face)
  -> Object tracking giữa các frame
  -> Confidence smoothing + decay
  -> ROI quality filtering
  -> Face-object spatial association scoring
  -> Object candidate state machine
  -> Event publish
  -> Memory / teach decision / avatar state / debug UI
```

#### Thành phần nên có

##### A. ObjectDetectionEngine
Giữ engine detector hiện tại làm lớp phát hiện thô.

##### B. ObjectTrack
Mỗi object detection hợp lệ phải được gắn vào một track ngắn hạn:
- `trackId`
- `label`
- `avgConfidence`
- `firstSeenAt`
- `lastSeenAt`
- `stableFrameCount`
- `lastBoundingBox`
- `velocityEstimate` (nếu cần đơn giản)

##### C. Quality gating
Chỉ cho object đi tiếp nếu đạt các điều kiện tối thiểu:
- bbox đủ lớn
- không quá sát mép ảnh
- confidence detector đủ cao
- object tồn tại đủ số frame tối thiểu

##### D. Face-object association score
Nếu có face/person hiện diện, tính một điểm liên hệ giữa object và face:
- IoU / overlap nếu phù hợp
- khoảng cách tâm chuẩn hóa theo kích thước face box
- vị trí tương đối trái/phải/trên/dưới
- continuity theo thời gian

Từ đó chia object thành:
- `NEAR_FACE`
- `HELD_BY_PERSON` (heuristic)
- `BACKGROUND_OBJECT`
- `UNCERTAIN`

##### E. UnknownObjectCandidate
Không mở popup trực tiếp từ raw detector nữa.
Thay vào đó, tạo `UnknownObjectCandidate` với:
- `candidateId`
- `representativeLabel`
- `trackIds`
- `bestThumbnail`
- `firstSeenAt`
- `lastSeenAt`
- `stableFrameCount`
- `nearFaceScore`
- `status = COLLECTING | READY_TO_ASK | ASKED | SUPPRESSED | RESOLVED`
- `lastPromptAt`
- `suppressedUntil`

##### F. Event layer
Mọi chuyển trạng thái quan trọng phải đi qua event theo đúng kiến trúc project【turn3file8†L31-L33】.
Ví dụ:
- `OBJECT_TRACK_CREATED`
- `OBJECT_TRACK_UPDATED`
- `OBJECT_ASSOCIATED_WITH_FACE`
- `UNKNOWN_OBJECT_CANDIDATE_READY`
- `UNKNOWN_OBJECT_CANDIDATE_SUPPRESSED`
- `UNKNOWN_OBJECT_CANDIDATE_RESOLVED`

#### Kết quả mong muốn
- giảm false positive
- pet chỉ chú ý đến object đủ ổn định
- object gần người được ưu tiên hơn object nền
- ask flow bớt spam và có ngữ cảnh hơn

---

### 5.2 Kiến trúc trung hạn (khi muốn pet nhớ object tốt hơn)

Mục tiêu: hỗ trợ **instance-level memory** cho object cụ thể.

Khi đó cần thêm:

#### A. Object embedding / re-identification layer
Từ crop ROI của object đã ổn định, sinh embedding để so khớp object instance theo thời gian.

#### B. Object memory store
Lưu:
- object profile
- aliases / displayName
- class label gốc
- embeddings mẫu
- seenCount
- lastSeenAt
- optional owner/person association

#### C. Memory decision
Phân biệt rõ:
- class known: “đây là cái cốc”
- instance known: “đây là cái cốc màu xanh của bố”

#### D. Teach flow nâng cao
Khi user đặt tên object mới, không chỉ lưu label text; cần một capture window ngắn để lưu thêm nhiều object crops tốt hơn, tương tự logic đã đúng hướng ở face.

---

## 6. Những gì chưa nên làm ngay

Có vài hướng nghe hấp dẫn nhưng chưa nên kéo vào ở bước này:

### 6.1 Không nhảy ngay sang cloud-first object understanding
Sai với Phase 1 offline-first【turn3file8†L1-L8】.

### 6.2 Không cố giải “open vocabulary everything” ngay
Rất dễ nặng máy, khó kiểm soát latency, khó verify.

### 6.3 Không gộp toàn bộ object memory + custom training + segmentation vào một task lớn
Trái với nguyên tắc task granularity của repo【turn3file18†L1-L27】.

### 6.4 Không biến detector confidence thành “trí thông minh giả”
Nếu app-layer continuity và context chưa tốt, tăng threshold đơn thuần sẽ chỉ làm bỏ sót nhiều hơn chứ chưa chắc thông minh hơn.

---

## 7. Đề xuất phạm vi MVP cho object giai đoạn này

Đây là scope MVP hợp lý nhất cho đợt cải thiện đầu:

### In scope
- object detection ổn định hơn
- object tracking ngắn hạn
- smoothing + decay
- face-object association
- unknown object candidate gating
- suppression thông minh hơn trước khi hỏi
- debug visibility tốt hơn

### Out of scope tạm thời
- object instance recognition hoàn chỉnh
- segmentation nâng cao
- custom-trained detector toàn diện
- open-vocabulary recognition
- cloud reasoning cho realtime loop

---

## 8. Nguyên tắc tách task sau tài liệu này

Theo `AGENTS.md`, task nên là vertical slice nhỏ, build-safe, verify được, không gom speculative refactor lớn【turn3file18†L1-L27】. Vì vậy, bước tiếp theo không nên là “implement all object intelligence”.

Nên chia thành các task kiểu này:
1. audit + expose debug metrics hiện tại của object pipeline
2. thêm object track continuity ngắn hạn
3. thêm confidence smoothing + decay/reset đúng
4. thêm face-object association scoring
5. thêm unknown object candidate state machine
6. đổi ask flow từ raw unknown event sang candidate-ready event
7. thêm object teach capture window / memory improvements
8. nếu cần mới sang custom model / embedding

---

## 9. Kết luận kiến trúc

Kết luận ngắn gọn:

- Hướng hiện tại đúng ở chỗ đã có Android offline-first, event-driven perception pipeline và teach dialog thực【turn3file8†L1-L33】【turn3file7†L1-L33】.
- Nhưng object recognition hiện còn yếu vì mới dừng ở **detector label + cooldown**, trong khi yêu cầu thật sự là **object in context**: ổn định theo thời gian, có liên hệ với face/person, và đủ quan trọng mới hỏi【turn3file10†L21-L44】.
- Kiến trúc nên nâng cấp theo thứ tự đúng là:
  1. detector
  2. tracking
  3. smoothing/decay
  4. face-object association
  5. unknown candidate memory
  6. sau đó mới nghĩ đến object embedding / instance memory

Đây là con đường phù hợp nhất để vừa cải thiện chất lượng thực tế, vừa không phá kiến trúc dự án, vừa dễ chia nhỏ thành task an toàn cho Codex.
