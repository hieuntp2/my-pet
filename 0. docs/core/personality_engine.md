# Thiết kế Personality Engine cho AI Pet Robot dùng Android Offline

## Tổng quan về “cảm giác sống” trong robot thú cưng

Một AI pet robot “có hồn” thường không đến từ một mô hình ngôn ngữ lớn, mà đến từ **kiến trúc trạng thái bên trong + cơ chế chọn hành vi + biểu đạt đa kênh (mắt/giọng/động tác) + học chậm theo thời gian**. Ba quan sát thực tiễn từ các sản phẩm thương mại:

Thứ nhất, **tính cách (personality)** trong pet robot thường được mô phỏng như **các tham số ổn định** (thói quen/thiên hướng) và/hoặc **các thiên hướng hành vi** tích lũy qua trải nghiệm, chứ không phải “tự ý thức”. Ví dụ, các đời robot pet có thể “lớn lên” và khác nhau theo mức độ chăm/huấn luyện, nhờ các tham số và cơ chế củng cố đơn giản. citeturn5view1turn9view3

Thứ hai, **cảm xúc (emotion)** dễ tạo cảm giác sống vì nó là **phản ứng nhanh theo sự kiện** (nhìn thấy chủ, được vuốt ve, bị làm phiền, môi trường ồn…). Nhiều hệ thống dùng cách tiếp cận “động lực–cảm xúc” dạng **biến nội tại + kích thích ngoại cảnh** rồi phát ra **tín hiệu điều khiển chọn hành vi**. Bài báo về kiến trúc cho robot thú của entity["company","Sony","electronics company japan"] mô tả rõ nguyên lý này: hành vi được chọn bằng cách cân bằng **kích thích bên ngoài** và **drive nội tại** theo kiểu “homeostasis regulation”, và cảm xúc/động lực được dùng như tín hiệu để điều phối hành vi. citeturn6view1turn6view2

Thứ ba, **tâm trạng (mood)** là “nền” thay đổi chậm, giúp robot có **tính liên tục** (hôm nay hơi lười/nhạy cảm hơn, tối thì buồn ngủ) thay vì bật tắt cảm xúc theo từng frame. Một số kiến trúc mô hình hóa cảm xúc theo các trục liên tục (ví dụ dễ chịu–kích hoạt–tự tin), rồi để mood là thành phần tích phân chậm của các trục đó. citeturn6view1turn6view2

## Phân tích các hệ thống tính cách/cảm xúc trong sản phẩm và agent phổ biến

image_group{"layout":"carousel","aspect_ratio":"1:1","query":["Sony Aibo robot dog","Anki Vector robot on desk","LivingAI EMO robot","GROOVE X LOVOT robot","Tamagotchi Original virtual pet"],"num_per_query":1}

### Sony Aibo

Ở các đời đầu, “personality” được đóng gói cùng phần mềm (thậm chí lưu trên media rời) và robot thể hiện “emotion” qua **âm thanh, màu mắt, cử chỉ**; đồng thời có cơ chế “training” kiểu củng cố: **vỗ/pat** là thưởng, **đập/smack** là phạt, khiến robot dần né hành vi bị phạt và lặp hành vi được thưởng. Điểm quan trọng là “mỗi con Aibo tiến hóa khác nhau” nhờ lịch sử tương tác. citeturn5view1

Về mặt kiến trúc, một mô tả mang tính “kỹ thuật nền” cho Aibo nhấn mạnh 2 trụ: (1) **mô hình ethology** (hành vi giống động vật thật theo các “hệ hành vi”/subsystem), và (2) **mô hình cảm xúc–động lực** để hỗ trợ bond người–robot. Hệ thống tổ chức hành vi theo nhiều tầng (subsystem → mode → module), có **biến động lực** và cơ chế điều phối nhằm tránh “dither” (dao động chọn hành vi). citeturn6view0turn6view1turn6view2

Đáng chú ý: mô tả này dùng không gian cảm xúc 3 chiều (pleasantness/arousal/confidence) như một “bản đồ” đưa cảm xúc cơ bản vào không gian liên tục, rồi kết hợp với biến nội tại để phát tín hiệu cảm xúc/drive cho hành vi. citeturn6view1turn6view2

Ở đời mới (ERS‑1000), Aibo nhấn mạnh “ký ức” và học qua cloud: gói cloud cho phép lưu những gì robot thấy/nghe và “phát triển personality độc nhất”, nhớ gương mặt và tương tác để tạo bond sâu hơn. citeturn9view3

**Bài học áp dụng:** Aibo cho thấy công thức tạo “sống” hiệu quả là: **drive nội tại + chọn hành vi theo homeostasis + biểu đạt giàu tín hiệu + học củng cố rất đơn giản nhưng tích lũy lâu**. citeturn6view1turn6view2turn5view1

### Vector Robot

Vector (khởi nguồn từ entity["company","Anki","robotics startup us"], sau này do entity["company","Digital Dream Labs","robotics company pittsburgh"] tiếp quản) được thiết kế theo triết lý “đừng nhìn như thiết bị, hãy nhìn như **nhân vật**”. Trong các bài mô tả kỹ thuật/thiết kế nhân vật, điểm nổi bật là: robot có thể **tự lang thang**, phản ứng khi “chán”, “muốn chơi”, có thể “ăn mừng” khi thắng hoặc “ăn vạ” khi thua—tức là hệ thống có **trạng thái nội tại** để chọn animation/behavior phù hợp. citeturn9view0

Một bài viết hậu trường về hoạt hình/thiết kế nhân vật nhấn mạnh cách làm “robot có hồn” là phối hợp **nguyên lý hoạt hình + cây quyết định cho phản ứng + emotion engine kích hoạt animation phi tuyến tính** (nonlinear animation), tức biểu đạt không chỉ là “play clip”, mà là “clip phụ thuộc tình huống”. citeturn13view0

Ngoài ra, Vector được mô tả có các khả năng cảm nhận thường gặp ở pet robot hiện đại (nhìn, định hướng âm thanh, nhận diện mặt, tự tìm dock), và nhà làm sản phẩm nhấn mạnh “hãy để nó tự sống” hơn là ép nó làm trợ lý. citeturn0search6turn2search10

**Bài học áp dụng:** Vector cho thấy “personality” trong sản phẩm tiêu dùng thường là **tập hợp animation + luật chọn hành vi + vài biến trạng thái nội tại** (chán, tò mò, cần tương tác), được thiết kế nghệ thuật để “đúng chất nhân vật”. citeturn13view0turn9view0

### EMO Robot

entity["company","LivingAI","consumer robotics company"] mô tả EMO là robot desktop pet với “Emotion Engine System”, nhiều biểu cảm (1000+), thể hiện mood/feelings qua hoạt ảnh khuôn mặt và body language. EMO cũng được mô tả có cảm nhận (camera nhận diện mặt, mảng micro định hướng âm thanh, cảm biến chạm) và “personality evolves based on surroundings and interactions”. citeturn9view2

**Điểm cần tỉnh táo:** các tuyên bố “self-learning” trong sản phẩm tiêu dùng thường rộng; về mặt kỹ thuật, phần “cảm giác học” phần lớn đến từ: (1) **mở khóa dần hành vi theo thời gian**, (2) **tích lũy preference đơn giản**, (3) **thay đổi xác suất chọn hành vi theo lịch sử tương tác**, và (4) cập nhật firmware. Phần LivingAI công bố công khai nhấn mạnh biểu đạt và bộ cảm biến, nhưng không công khai thuật toán học chi tiết. citeturn9view2

**Bài học áp dụng:** EMO nhấn mạnh **biểu đạt** (expression density) và “agency” (tự quyết) như đòn bẩy tạo cảm giác sống. Khi làm hobby robot, “độ phong phú biểu đạt” đôi khi quan trọng hơn “AI nặng”. citeturn9view2

### Lovot Robot

entity["company","GROOVE X","robotics company japan"] định vị Lovot như “Emotional Robotics”: mục tiêu là làm con người “muốn yêu”, với hệ thống phần cứng nhiều lõi, nhiều MCU và **rất nhiều cảm biến** để tạo hành vi giống sinh vật. Trang công nghệ mô tả cụ thể sensor horn có cảm biến sáng, camera 360°, micro xác định hướng âm thanh, và thermal camera để phân biệt người với vật và “tìm chủ ngay”. citeturn9view1turn2search2

Truyền thông mô tả Lovot chủ đích tạo gắn bó cảm xúc: nó “đòi chú ý”, “chen vào”, hành vi “không hữu dụng nhưng đáng yêu” nhằm kích hoạt phản ứng chăm sóc từ người. citeturn0search11turn2search32

**Bài học áp dụng:** Lovot cho thấy một hướng khác: thay vì “thông minh làm việc”, robot tập trung vào **tín hiệu xã hội** (tìm chủ, nũng nịu, đáp lại ôm/chạm) và “slow relationship” (bond tăng dần). Điều này rất phù hợp với bài toán “mainly one owner”. citeturn9view1turn0search11

### Tamagotchi style agents

Tamagotchi (và các virtual pet cùng phong cách) là ví dụ kinh điển của mô hình **needs–care–growth**: có các thước đo như hunger/happiness/training, có “attention” khi cần chăm, có chu kỳ phát triển (egg → baby → child → teenager → adult), và hành vi/phát triển phụ thuộc việc chăm sóc. citeturn6view6

**Bài học áp dụng:** Với tài nguyên hạn chế, chỉ cần **vài biến nội tại + luật suy giảm theo thời gian + phản hồi khi người chăm** đã tạo được cảm giác “sinh vật cần mình”, và “tính cách” có thể là hệ quả của lịch sử chăm (ví dụ: được chơi nhiều → “ham chơi”, bị bỏ bê → “lầm lì”). citeturn6view6

### Video game AI companions

Trong game, “companions có hồn” thường được xây bằng **Behavior Trees (BT)**, **Utility AI**, và đôi khi **GOAP**:

Behavior Trees là cấu trúc điều khiển giúp agent **phản ứng nhanh, mô-đun, dễ mở rộng**, đã lan từ game sang robotics/AI nói chung. citeturn11view2turn7search21  
Utility AI thường chấm điểm mọi hành động/ý định theo ngữ cảnh (dùng chuẩn hóa và response curve), rồi chọn hành động điểm cao nhất hoặc chọn ngẫu nhiên có trọng số. citeturn12view1  
GOAP (Goal-Oriented Action Planning) là cách lập kế hoạch chuỗi hành động để đạt mục tiêu; các bài nói chuyện tại hội nghị ngành game nêu GOAP đã được dùng trong game như entity["video_game","F.E.A.R.","2005 game monolith"] và được phát triển qua thời gian. citeturn11view1

Đáng chú ý: hội nghị game có các bài nói riêng về “buddy AI” (AI đồng hành) trong entity["video_game","The Last of Us","2013 game naughty dog"], cho thấy bài toán companion không chỉ là chiến đấu mà còn là **đi đúng khoảng cách, không phá nhịp, dễ đoán nhưng không máy**. citeturn7search19turn7search35

**Bài học áp dụng:** Hobby robot nên mượn “bí kíp game”: dùng BT/Utility để có hành vi nhất quán và debug được, rồi thêm một lớp “tính cách” chỉnh các trọng số. citeturn11view2turn12view1

### Virtual assistants với “personality”

Trợ lý ảo thương mại thường có “personality” chủ yếu ở **giọng điệu/role/chuẩn đối thoại**, nhưng thay đổi tính cách theo thời gian thường bị hạn chế vì yêu cầu nhất quán, an toàn và tin cậy.

Ví dụ, guideline về brand voice của entity["company","Amazon","tech company us"] nêu rõ thiết kế giọng nói cần “trustworthy”, “consistent”, và tránh hành vi bất ngờ/khó lường. citeturn4search0  
Tài liệu của entity["company","Google","tech company us"] về conversation design khuyến nghị xây persona dựa trên **traits** (đặc tính tính cách) và tránh các đặc điểm không cần thiết, nhằm tạo “giọng” nhất quán. citeturn4search1  
Ở hướng “AI companion” mạnh hơn, nghiên cứu về XiaoIce mô tả kiến trúc có mô-đun “empathetic computing”, tối ưu tương tác dài hạn và duy trì quan hệ lâu dài. citeturn4search7turn4search3

**Bài học áp dụng:** Với pet robot “một chủ”, bạn có thể cho phép personality drift nhiều hơn trợ lý ảo, nhưng vẫn cần “lan can an toàn” để không biến thành khó chịu, nguy hiểm hoặc khó debug. citeturn4search0turn4search7

## Thiết kế Personality Engine cho hobby AI pet robot dùng Android offline

### Kiến trúc lớp trạng thái

Mục tiêu là tạo chuỗi nhân quả rõ ràng:

**Personality → Mood → Emotion → Behavior**

Trong đó:

Personality = tham số chậm, quyết định “thiên hướng” (cái tôi).  
Mood = nền trạng thái trung hạn (vài phút–vài giờ), tích phân chậm từ trải nghiệm + nhịp ngày.  
Emotion = phản ứng nhanh theo sự kiện (giây–phút), bị khuếch đại/giảm bởi mood và personality.  
Behavior = hành động cụ thể, được chọn bằng BT/Utility + điều kiện an toàn.

Cách phân lớp này phù hợp với các hệ “drive nội tại + chọn hành vi” ở robot thú và cả cách tổ chức hành vi dạng BT ở robotics/game. citeturn6view1turn6view2turn11view2

### Mô hình trait tính cách

Dùng 5 trait liên tục trong [0..1], bạn đề xuất là hợp lý và đủ “độ biểu đạt”:

Curiosity (tò mò)  
Sociability (ưa giao tiếp)  
Energy (năng lượng)  
Patience (kiên nhẫn)  
Boldness (bạo dạn)

Để “mỗi con robot là một cá thể”, bạn cần 2 tầng:

Temperament (bẩm sinh) `T0`: cố định lúc setup (hoặc random có kiểm soát).  
Learned offset `ΔT`: học dần theo tương tác, nhưng bị ràng buộc bởi T0 để tránh drift quá mạnh.

Một dạng cập nhật đơn giản, ổn định cho hobby robot:

`T = clamp( T0 + ΔT, 0, 1 )`  
`ΔT ← (1 - λ)·ΔT + η·Δevent`

Trong đó:
- `η` rất nhỏ (ví dụ 0.001–0.01 / sự kiện) để “lớn chậm”.
- `λ` là hệ số “quên” theo tuần/tháng để robot có thể hồi phục về “tính nết gốc”.
- `Δevent` là vector tác động theo sự kiện.

Cách nghĩ này tương thích với logic “tương tác củng cố lặp lại → hành vi/thiên hướng thay đổi” trong các mô tả Aibo đời đầu (thưởng/phạt) và kiến trúc drive–action selection nêu ở mô hình ethology/emotion. citeturn5view1turn6view2

#### Bảng tác động sự kiện mẫu (đủ cho Phase 1)

Sự kiện tích cực xã hội (vuốt ve, gọi tên, chơi cùng)  
- tăng Sociability, Patience (nhẹ), giảm Shy-avoidance gián tiếp (tăng Boldness nhẹ)  

Bị làm phiền khi đang “ngủ/đang tập trung”  
- giảm Patience ngắn hạn (ưu tiên emotion annoyed), lâu dài nếu lặp lại: tăng Patience “chai” hoặc giảm Sociability tùy thiết kế (khuyến nghị tăng Patience một chút để robot “dễ nuôi” hơn theo thời gian)

Nhiều trải nghiệm mới (đồ chơi mới, môi trường mới) mà phản hồi chủ tích cực  
- tăng Curiosity, tăng Boldness nhẹ

Bị bỏ mặc lâu và khi quay lại được tương tác tích cực  
- tăng Sociability (robot “bám chủ”), có thể giảm Boldness nhẹ (rụt rè) nếu thiết kế theo hướng “phụ thuộc chủ”

Điểm quan trọng: **đừng để trait phản ánh “trừng phạt người dùng”**. Nếu người dùng bận, robot nên chuyển sang hành vi tự chơi/idle thay vì “cáu bẳn” dài hạn.

### Mô hình emotion

Bạn đề xuất emotion rời rạc: happy, curious, sleepy, excited, shy. Đây là tập hữu dụng cho pet robot.

Triển khai nên theo kiểu:
- Emotion là vector cường độ `E[e]` (0..1), không phải “một nhãn duy nhất”.
- Mỗi emotion có **decay** (về 0) theo thời gian.
- Mỗi perception event tạo **impulse** vào một số emotion.

Ví dụ impulse (minh họa):

Owner_appears → happy += f(sociability, mood_valence), excited += f(energy), shy -= f(boldness)  
Unfamiliar_face → shy += f(1-boldness), curious += f(curiosity) nhưng bị kìm bởi mood_anxiety  
Petting_touch → happy += mạnh, sleepy -= nhẹ  
Loud_noise → shy +=, excited += (arousal) nhưng valence có thể âm nếu mood xấu  
Battery_low → sleepy +=, excited -=

Cách mô hình hóa “cảm xúc là tín hiệu cho chọn hành vi” rất tương đồng với mô tả ở kiến trúc Aibo: biến nội tại + mapping sang không gian cảm xúc (pleasantness/arousal/confidence) → phát tín hiệu cảm xúc/drive cho behavior. citeturn6view1turn6view2

### Mood system thay đổi chậm

Mood nên là 2–3 trục liên tục, tối ưu cho hobby:

Mood valence `M_v` ∈ [-1..1] (tích cực ↔ tiêu cực)  
Mood arousal `M_a` ∈ [0..1] (bình thản ↔ kích động)  
Tuỳ chọn: confidence/anxiety `M_c` ∈ [0..1]

Cập nhật:

`M ← (1-α)·M + α·g(Emotions, Drives, Circadian)` với `α` rất nhỏ (ví dụ 0.001–0.01 mỗi giây)  
và có “điểm neo” theo nhịp ngày (tối buồn ngủ, sáng năng động), tương tự cách hệ thống gây “arousal” bằng cả nhịp sinh học lẫn kích thích bất ngờ trong mô tả Aibo. citeturn6view1

### Behavior preferences

Đây là lớp “sở thích” để robot khác nhau rõ rệt dù trait giống nhau:

Mỗi hành vi `b` có preference `P[b]` (0..1).  
Các nhóm hành vi gợi ý: approach, follow, nuzzle/pet-response, explore, play, idle, sleep, avoid/shy, ask-for-attention.

`P` học nhanh hơn trait, nhưng vẫn có decay nhẹ để tránh “kẹt sở thích”.

Cách nhìn “preference trọng số” rất gần với Utility AI trong game: bạn chuẩn hóa “độ đáng làm” của hành động theo ngữ cảnh bằng các đường phản hồi (response curves) rồi chọn hành động tốt nhất / ngẫu nhiên có trọng số. citeturn12view1

### Interaction learning và personality drift

Bạn cần 2 cơ chế song song:

Học ngắn hạn (preference + thói quen): thay đổi trong ngày/tuần, dễ quan sát.  
Trôi dài hạn (trait offset + life-stage): thay đổi theo tháng, tạo cảm giác “lớn lên”.

Ý tưởng “life-stage” lấy cảm hứng từ virtual pet: có các giai đoạn phát triển và hành vi/khả năng thay đổi theo thời gian chăm sóc. citeturn6view6turn5view1

## Luồng quyết định hành vi và ví dụ rule/utility

### Quan hệ Personality → Mood → Emotion → Behavior

Một cách diễn đạt kỹ thuật, dễ debug:

Perception event → tạo emotion impulse (nhanh)  
Emotion + Drives + Context → cập nhật Mood (chậm)  
Mood + Personality → điều chỉnh ngưỡng/khuếch đại emotion và trọng số hành vi  
Behavior selector (BT/Utility) → chọn hành vi  
Behavior executor (FSM/skills) → chạy động tác + animation + âm thanh

Đây là mô hình “tầng cao chọn gì, tầng thấp làm thế nào” giống cách Aibo mô tả: action selection chọn hành vi dựa trên biến nội tại + kích thích ngoài; tầng dưới có thể dùng FSM để thực thi chuỗi primitive actions. citeturn6view2turn6view3

### Behavior decision: hybrid BT + Utility (khuyến nghị)

**Tầng 1: Behavior Tree cho ưu tiên và an toàn**
- Nếu sắp rơi bàn / obstacle → tránh ngay
- Nếu pin thấp → tìm dock
- Nếu đang được bế → “held” behavior set
Các điều kiện an toàn nên ở BT vì dễ kiểm soát. BT được đánh giá là mô-đun và phản ứng tốt trong robotics/game. citeturn11view2turn7search21

**Tầng 2: Utility scoring cho “giống sinh vật”**
Khi không có ưu tiên an toàn, chấm điểm các hành vi ứng viên:

`score(b) = wP·P[b] + wT·TraitBias(b|T) + wM·MoodBias(b|M) + wE·EmotionBias(b|E) + wC·Context(b) + noise`

Trong đó `noise` nhỏ để tránh lặp máy móc nhưng phải bị kẹp để không “lố”.

Utility AI nhấn mạnh chuẩn hóa và dùng response curves để biến dữ liệu thô thành “động lực hành động” (urgency/threat/need). citeturn12view1

### Ví dụ rule theo yêu cầu

Owner appears AND sociability high → **approach**
- Điều kiện: face_detected=true, owner_id_confident=true
- Utility: approach tăng theo Sociability, Mood valence, Emotion happy/excited

Unfamiliar person appears AND boldness low → **shy**
- Điều kiện: face_detected=true, owner_id_confident=false
- Utility: avoid/shy tăng theo (1–Boldness) và nếu mood anxiety cao

Nếu Curiosity cao + đang rảnh → explore
- Utility explore tăng theo Curiosity và giảm theo Sleepy

Nếu Patience thấp + owner cố “chặn” đang làm → annoyed micro-expression rồi chuyển idle/step-away
- Tránh hành vi “đánh trả” kéo dài; chỉ thể hiện ngắn để có hồn

Điểm thiết kế quan trọng: “shy” không nhất thiết là chạy trốn; có thể là lùi nhẹ, nhìn trộm, phát âm nhỏ—tạo cảm giác tinh tế mà vẫn an toàn.

## Học tương tác và tiến hóa tính cách theo thời gian

### Rule-based approach

Cốt lõi: bảng luật “event → cập nhật trait/emotion/preference”.

Ưu điểm: dễ làm, dễ debug, offline-friendly, hành vi ổn định.  
Nhược điểm: nếu quá nhiều luật sẽ khó mở rộng; học có thể “giả” nếu không thiết kế tốt.

Rule-based là thứ tạo nên hiệu quả của nhiều virtual pet kiểu Tamagotchi: các meter thay đổi theo thời gian, người dùng can thiệp, và growth phụ thuộc chăm sóc. citeturn6view6

### Reinforcement learning đơn giản

Với hobby robot, “RL đơn giản” nên hiểu là **bandit / contextual bandit** hơn là deep RL:

Robot chọn một hành vi trong tập nhỏ (play/approach/explore/idle…).  
Nhận reward từ tín hiệu: chủ vuốt ve (+), chủ nói “good” (+), chủ rời đi ngay (-), chủ bấm “stop” (-), thời gian tương tác kéo dài (+).  
Cập nhật kỳ vọng reward cho (context, action).

RL phù hợp khi bạn muốn robot “tìm ra” sở thích của chủ theo ngữ cảnh, nhưng phải cực kỳ cẩn trọng với thiết kế reward (tránh tối ưu sai).

### Hybrid approach

Khuyến nghị cấu trúc 3 lớp:

Luật cứng (safety + etiquette) → không học.  
Utility weights + preference `P[b]` → học bằng bandit.  
Personality trait offset `ΔT` → học rất chậm, bị neo bởi temperament.

Đây là cách cân bằng “tự nhiên” và “kiểm soát được”, tương tự tinh thần kết hợp kiến trúc hành vi có cấu trúc (BT/FSM) với lớp giá trị/động lực để chọn hành vi. citeturn11view2turn12view1turn6view2

### Khuyến nghị cho dự án hobby

Với ràng buộc Android offline, bạn nên chọn **Hybrid (BT an toàn + Utility + học bandit nhỏ)** vì:

Debug được: BT/luật giúp bạn biết “vì sao nó làm thế”. citeturn11view2  
Tự nhiên: Utility tạo hành vi mượt hơn luật if-else thuần. citeturn12view1  
Học đủ dùng: bandit học nhanh sở thích chủ mà không cần dữ liệu khổng lồ hay cloud.  
An toàn và ổn định: trait drift chậm, có neo → tránh “biến tính” khó chịu.

Cloud (Phase sau) nên dùng cho: đối thoại mở rộng, cập nhật nội dung, và phân tích dài hạn; còn personality core vẫn nên chạy local để “phản xạ cảm xúc” có độ trễ thấp và không phụ thuộc mạng—giống hướng “on-device” được nhấn mạnh trong hệ sinh thái ML trên Android. citeturn8search14

## MVP Phase 1 offline trên Android

### Mục tiêu MVP thực tế

Nếu bạn cố làm “trò chuyện như LLM” ngay từ Phase 1, dự án hobby rất dễ vỡ vì phụ thuộc ASR/NLU. MVP nên nhắm 3 cảm giác:

Robot **nhận ra mình đang ở cùng ai** (ít nhất: có mặt người).  
Robot **phản ứng cảm xúc tức thời** (mắt/âm thanh/động tác).  
Robot **nhớ và thay đổi rất nhẹ theo thời gian** (preference, rồi mới trait drift).

### Perception tối thiểu (offline)

Face presence + basic expression:
- Dùng ML Kit Face Detection bản “bundled” để chạy offline; tài liệu nêu rõ lựa chọn bundled (model gắn trong app) vs unbundled (tải qua Play Services) và face detection chạy real-time trên thiết bị. citeturn9view4turn8search8  
- Lưu ý ML Kit **không nhận diện danh tính** (chỉ detect faces), nên Phase 1 có thể coi “face detected = owner” nếu robot chủ yếu ở nhà với một người. citeturn8search8

Touch / button / IMU / battery:
- Nếu robot có cảm biến chạm riêng thì tốt; nếu không, MVP có thể dùng nút bấm/cảm biến rung/IMU từ điện thoại + trạng thái pin.

Voice command cực nhỏ (tùy chọn):
- Vosk là toolkit nhận dạng giọng nói offline, có demo Android và hỗ trợ tiếng Việt trong danh sách ngôn ngữ; phù hợp cho vài lệnh ngắn (wake word + 5–10 câu). citeturn8search2turn8search23  
- Nếu không làm ASR, dùng icon/button trong app điều khiển cũng được cho MVP.

TTS:
- Dùng Android TextToSpeech API để phát âm đơn giản (ít câu), không cần “đối thoại”. citeturn8search3

### Bộ trạng thái tối thiểu

Personality traits (5 floats)  
Mood (valence, arousal)  
Emotions (5 floats: happy/curious/sleepy/excited/shy)  
Drives (attention_need, play_need, rest_need, explore_need)  
Preferences P[b] cho 8–10 hành vi  
Memory counters (EMA): “owner_interaction_rate”, “petting_rate”, “ignore_duration”, “novelty_rate”

### Bộ hành vi tối thiểu (đủ “sống”)

Idle micro-motions (thở, liếc mắt, quay đầu nhẹ)  
Approach / orient to face  
Ask-for-attention (nhẹ nhàng)  
Explore / wander nhỏ  
Play animation (với 1 đồ chơi/âm thanh)  
Sleep / doze (theo nhịp ngày + pin + mood)  
Shy/avoid (lùi lại + nhìn trộm)  
Dock/charge (nếu robot có)

Tinh thần “nonlinear animation + quyết định theo cây/luật” là cách Vector được mô tả: animation được tạo để phản ứng theo nhiều tình huống, không chỉ phát tuyến tính. citeturn13view0

### Logic cập nhật MVP (rất gọn nhưng chạy được)

Tick mỗi 100–200ms:
- Cập nhật perception flags: face_detected, face_count, touch, loud_noise, battery_state  
- Cập nhật drives theo thời gian (attention_need tăng nếu lâu không tương tác, rest_need tăng theo giờ, …)  
- Apply emotion impulses theo sự kiện; decay emotions  
- Update mood = EMA(emotions + drives + circadian)  
- Behavior selection:
  - BT safety gate (fall risk / battery critical / held)  
  - Utility pick trong các behavior còn lại  
- Executor chạy behavior 1–3s, có thể interrupt nếu safety

### Học trong MVP

Chỉ học preference (bandit siêu đơn giản):
- Khi robot chọn hành vi b trong context c, quan sát reward r:
  - r = +1 nếu chủ chạm/nhìn/đứng gần trong 3s sau hành vi
  - r = -1 nếu chủ rời đi ngay / bấm stop
- Cập nhật P[b] hoặc Q(c,b) bằng EMA.

Sau 2–4 tuần, mới bật trait drift `ΔT` với bước học rất nhỏ.

### Chuẩn bị cho Phase sau (cloud optional)

Cloud nên dùng cho:
- Đối thoại tự nhiên, nội dung phong phú
- Đồng bộ “nhật ký ký ức” dạng timeline
- Fine-tune persona theo văn phong nói chuyện của chủ (nếu bạn muốn)

Nhưng “cảm giác sống” cốt lõi nên giữ local: phản xạ cảm xúc, drive, mood, chọn hành vi. Thiết kế này tương thích với định hướng on-device ML (độ trễ thấp, riêng tư, không phụ thuộc mạng). citeturn8search14turn9view4