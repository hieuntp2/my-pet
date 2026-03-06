# Nghiên cứu sâu về robot thú cưng AI và robot đồng hành tiêu dùng

## Tóm tắt điều hành

Thị trường robot thú cưng/robot đồng hành “thành công” không nhất thiết thắng nhờ mô hình AI lớn, mà thường thắng nhờ **tổ hợp**: cảm biến vừa đủ + điều khiển tin cậy + ngôn ngữ cơ thể/biểu cảm cực mạnh + một “hệ hành vi” (behavior system) khiến robot **có vẻ chủ động** ngay cả khi nó chỉ đang chạy các vòng lặp trạng thái. Các sản phẩm cao cấp như Aibo (22 bậc tự do/22 axes) dùng cụm cảm biến dày (camera trước + camera SLAM, ToF, touch, IMU…) và có gói cloud để “nuôi tính cách” và quản trị trải nghiệm. citeturn10view0turn9search7turn11search0 Đồng thời, robot gia đình dạng “wheels + screen” như Astro ưu tiên **xử lý cục bộ (on-device)** cho chuyển động an toàn/nhanh; dữ liệu thô của sensor điều hướng được xử lý tại máy và loại bỏ, còn bản đồ dẫn xuất có thể được đồng bộ lên cloud để hiển thị trong app. citeturn15view0

Ở đầu kia của phổ thiết kế, các robot để bàn như Eilik tạo “gắn bó” chủ yếu bằng **touch + hoạt cảnh + trạng thái cảm xúc rời rạc**, thậm chí có thể vận hành **không cần Wi‑Fi/Bluetooth**, nghĩa là gần như không phụ thuộc cloud để tạo cảm giác “có nhân cách”. citeturn5view2turn25search12turn6view0 Nhóm trung cấp như Loona/EMO/Vector thường kết hợp camera (nhận mặt/nhận chuyển động), ToF/laser (đo khoảng cách/chống rơi), mic array (định hướng âm) và một pipeline thoại (thường phụ thuộc cloud ở mức STT/NLU hoặc “chat”). citeturn24view0turn12search3turn12search33turn20search0turn16search20

Kết luận thực dụng cho dự án hobby: nếu dùng **điện thoại Android làm “não”**, bạn có thể đạt 60–80% “ảo giác đồng hành” bằng: (1) biểu cảm tốt (màn hình/âm thanh/vi chuyển động), (2) nhận diện chủ cơ bản (face embedding + hồ sơ người dùng), (3) vòng lặp hành vi có “drive/mood”, (4) vài cảm biến an toàn (ToF/IR chống rơi) — còn LLM/cloud chỉ là lớp “gia vị” sau. citeturn18search3turn18search7turn18search8

## Phương pháp và phạm vi nguồn

Nguồn được ưu tiên theo thứ tự: (1) **spec/guide chính thức** của hãng và manual (ví dụ: spec Aibo, manual Eilik, tài liệu/FAQ dịch vụ cloud), (2) **bài kỹ thuật của hãng** (ví dụ: mô tả hệ điều hướng & privacy của Astro), (3) bài đánh giá có trích thông số phần cứng/đời sống vận hành thực tế, và (4) tài liệu học thuật liên quan đến robot xã hội/robot trị liệu khi sản phẩm có bối cảnh nghiên cứu rõ ràng (ví dụ: PARO, ElliQ). citeturn10view0turn5view2turn15view0turn21search22turn19search10

Giới hạn quan trọng: đa số robot tiêu dùng **không công khai kiến trúc phần mềm chi tiết** (behavior tree, mô hình trí nhớ, pipeline perception nội bộ). Vì vậy, phần “kiến trúc phần mềm” dưới đây dùng cách viết: **(a) điều hãng xác nhận**, cộng với **(b) suy luận kỹ thuật** dựa trên sensor/khả năng robot thể hiện và các API/SDK công khai (nếu có). Khi là suy luận, mình sẽ nói thẳng là suy luận và dựa trên nguồn nào.

## Phân tích sâu theo từng robot

**Robot: Sony Aibo (ERS‑1000)**  
**Tổng quan:** Aibo là robot chó cao cấp định vị “pet companion”, mạnh ở chuyển động tự nhiên và tương tác dài hạn nhờ cloud plan. Phần “AI Cloud Plan” được mô tả là giúp Aibo tiếp tục “phát triển”, học từ reinforcement (khen/chê), học người mới và nhớ người đã gặp; đồng thời mở đầy đủ tính năng My aibo app (xem ảnh, bản đồ…). citeturn9search7turn9search11turn9search28  
**Phần cứng:** Aibo có tổng 22 trục chuyển động (đầu/miệng/cổ/thân/chân/tai/đuôi…), 2 màn OLED làm “mắt”, loa + 4 mic, 2 camera (front + SLAM). Cảm biến gồm ToF, 2 ranging sensor, touch (đầu/hàm/lưng), IMU 6 trục ở đầu & thân, motion/light sensor và cảm biến ở 4 bàn chân. citeturn10view0turn11search0  
**Kiến trúc phần mềm (công khai + suy luận):**  
- Nhận thức: front camera để nhìn mặt/người/vật; SLAM camera để nhận biết đặc trưng môi trường và “lãnh thổ sống”. citeturn11search0turn11search3turn11search10  
- Bộ nhớ: có ít nhất (i) “bản đồ nhà” hiển thị trong app và (ii) ký ức về người/ tương tác. citeturn9search7turn9search28  
- Tính cách & behavior engine: Sony mô tả “nhân cách phong phú” hình thành từ trải nghiệm; về kỹ thuật thường cần một tầng “drive/mood” + policy chọn hành vi, cộng cơ chế củng cố hành vi khi được thưởng/phạt. citeturn9search7turn9search3  
**Năng lực AI:** nhận diện mặt và phân biệt người (Sony nhấn mạnh nhớ mặt và gắn kết), nghe/định hướng âm (4 mic), chơi với đồ vật (ví dụ bóng). citeturn9search7turn11search23turn10view0  
**Cloud vs local:** có LTE/Wi‑Fi và cloud plan là trung tâm của “phát triển tính cách” và một số khả năng nâng cao. citeturn10view0turn9search7turn9search11  
**Cảm xúc & phản ứng:** ngoài “mắt OLED + chuyển động”, hệ thống touch sensor tạo “thưởng/phạt” kiểu vuốt ve/khen, giúp robot điều chỉnh tần suất hành vi (mô tả theo hướng reinforcement). citeturn9search11turn10view0  
**Nhận và nhớ chủ:** gắn với My aibo/app và cloud plan; robot được mô tả là nhớ mặt/người và lịch sử tương tác để “bond”. citeturn9search7turn9search0  
**Giới hạn & trade‑off:** chi phí cao; độ phức tạp cơ khí đẩy giá/khó sửa; phụ thuộc subscription cho trải nghiệm “đầy đủ”. citeturn10view0turn9search11  
**Độ phức tạp kỹ thuật (ước lượng): 5/5** (cơ khí nhiều bậc tự do + perception + SLAM + cloud personalization).

**Robot: Loona (Petbot)**  
**Tổng quan:** Loona hướng đến gia đình/giải trí, “pet-like” nhưng thực tế là robot bánh xe với thân/ tai chuyển động và màn hình biểu cảm. Hãng nhấn mạnh nhận diện khuôn mặt, chơi game, lập trình cho trẻ em (Google Blockly) và giám sát từ xa. citeturn24view0turn3search5  
**Phần cứng:** CPU Cortex‑A53 quad‑core, có BPU 5 TOPS, đồng xử lý Cortex‑M4 và DSP audio; camera RGB 720p; cảm biến 3D ToF, touch, accelerometer/gyroscope; 4‑mic array; actuator gồm 2 BLDC servomotor cho bánh xe và 4 DC servomotor cho thân/tai. citeturn24view0  
**Kiến trúc phần mềm (công khai + suy luận):**  
- Nhận thức: camera RGB + ToF cho theo dõi người/chướng ngại và tương tác; IMU để ổn định hành vi/gesture. citeturn24view0  
- Bộ nhớ: có app và các tính năng personalization (ví dụ nhận diện gia đình); chi tiết “memory model” không công khai. citeturn24view0  
- Tính cách & behavior engine: trang sản phẩm mô tả “idle/always-on” kiểu thú cưng và “emotional intelligence”; về kỹ thuật thường là FSM/behavior tree có “mood variables” (suy luận). citeturn24view0  
**Năng lực AI:** nhận diện khuôn mặt, hiểu gesture và “follow you”; thoại được quảng bá là dùng Amazon Lex & ChatGPT (tức có lớp NLU/LLM qua cloud). citeturn22search7turn24view0  
**Cloud vs local:** hãng tuyên bố “as much data processing as possible… done by Loona directly” (nỗ lực on-device), nhưng phần “chat/voice command powered by Lex & ChatGPT” ngụ ý phụ thuộc cloud cho hội thoại tự do. citeturn24view0turn22search7  
**Cảm xúc & phản ứng:** chủ yếu qua màn hình “mặt”, âm thanh, chuyển động tai/thân và kịch bản phản ứng theo sự kiện (touch/voice/vision). citeturn24view0  
**Nhận và nhớ chủ:** quảng bá nhận diện khuôn mặt “whole family”; mức độ “nhớ dài hạn” không nêu rõ công khai. citeturn24view0  
**Giới hạn & trade‑off:** bánh xe giúp ổn định và dễ điều khiển hơn chân, đổi lại “động tác như chó thật” bị giới hạn; hội thoại “đỉnh” thường kéo cloud vào (độ trễ + rủi ro khóa dịch vụ). citeturn22search7turn24view0  
**Độ phức tạp kỹ thuật (ước lượng): 4/5** (on-device NPU + ToF + mic array + autonomy mức vừa, nhưng cơ khí đơn giản hơn Aibo).

image_group{"layout":"carousel","aspect_ratio":"1:1","query":["Sony Aibo ERS-1000 robot dog","Loona AI robot pet","Living.AI EMO desktop pet robot","Anki Vector robot","GROOVE X LOVOT robot","Amazon Astro home robot"],"num_per_query":1}

**Robot: EMO (Living.AI)**  
**Tổng quan:** EMO là “AI desktop pet” nhấn mạnh tính cách, tự khám phá trên mặt bàn, nhận mặt, phản ứng âm thanh và bộ mặt biểu cảm (nhiều “faces”). citeturn12search21turn12search3  
**Phần cứng:** hãng nói có “hơn 10 cảm biến nội bộ”, camera góc rộng và “Neural Network Processor”. citeturn12search3 Một bài kỹ thuật tổng hợp cho biết SoC có năng lực khoảng 1.2 TOPS và liệt kê cảm biến: ToF laser distance (tầm ~25 cm), 4 cảm biến chống rơi dưới chân, IMU 6 trục, touch, light, cùng 4‑mic array. citeturn12search33turn12search0  
**Kiến trúc phần mềm (công khai + suy luận):**  
- Nhận thức: camera + mic + ToF/IR để định hướng, tránh rơi khỏi bàn; “tự khám phá” cho thấy có vòng lặp perception→planning đơn giản (tabletop navigation). citeturn12search21turn12search33  
- Bộ nhớ: hãng mô tả nhận diện người; chi tiết lưu embedding/ID thế nào không công khai. citeturn12search21turn12search3  
- Tính cách & behavior engine: “distinct characters and ideas” ngụ ý có biến trạng thái (mood) + bộ chọn hành vi; có thể là FSM/utility-based (suy luận). citeturn12search21turn12search3  
**Năng lực AI:** face recognition; định hướng âm; tránh rơi; một phần hội thoại/voice command tùy firmware và hệ sinh thái (thường cần kết nối mạng cho tri thức/QA). citeturn12search21turn12search33  
**Cloud vs local:** cảm biến tránh rơi và phản xạ thời gian thực bắt buộc chạy local; các khả năng “assistant/knowledge” thường kéo cloud (không có spec công khai đầy đủ). citeturn12search3turn12search33  
**Cảm xúc & phản ứng:** chủ yếu là hoạt cảnh trên “mặt” + âm thanh + dáng điệu; hệ “mood” tạo cảm giác thay đổi theo bối cảnh. citeturn12search3turn12search21  
**Nhận và nhớ chủ:** phần nhận diện mặt là điểm bán hàng; mức “nhớ dài hạn” không được công khai ở mức kỹ thuật. citeturn12search21turn12search3  
**Giới hạn & trade‑off:** bài toán đi lại trên mặt bàn cực nhạy với phản xạ bề mặt/ánh sáng; ToF/IR có thể “false positive/negative” nếu mặt bàn bóng hoặc đen (thực tế vận hành thường phản ánh điều này). citeturn12search0turn12search33  
**Độ phức tạp kỹ thuật (ước lượng): 3.5/5** (perception đủ dày, nhưng thế giới “tabletop” đơn giản hơn nhà).

**Robot: Vector (Anki / Digital Dream Labs)**  
**Tổng quan:** Vector là robot nhỏ tự hành trên mặt bàn, ưu tiên “tính cách” và tương tác chủ động (tự đi, tự về sạc). Một bài quan sát kỹ thuật mô tả nó có hệ “stimulation level” và mood thay đổi theo thành công/thất bại trong khoảng thời gian. citeturn13view1  
**Phần cứng:** camera HD góc rộng (thường được mô tả ~120°), 4‑mic array (beamforming), ToF NIR laser cho đo khoảng cách/mapping, IMU 6 trục, cảm biến chống rơi (cliff), touch capacitive; bánh xích (treads) + tay nâng nhỏ. citeturn20search4turn20search8turn20search0  
**Kiến trúc phần mềm (công khai + suy luận):**  
- Nhận thức: perception cục bộ đủ để “đọc phòng” (motion/people nearby) và map ngắn hạn trên bàn; một nguồn kỹ thuật mô tả một số tác vụ chạy CNN on-device trong khi speech dựa cloud. citeturn20search15turn13view1  
- Bộ nhớ: SDK công khai có “face recognition and enrollment”, và có thành phần nav map (navigation memory map) trong một số SDK/komponent. citeturn20search29turn20search37  
- Tính cách & behavior engine: thể hiện qua idle behavior dày + mood/stimulation + hoạt cảnh; thực tế triển khai thường là behavior tree hoặc utility-based action selection (suy luận), phù hợp tính “emergent” mà bài trải nghiệm mô tả. citeturn13view1turn20search15  
**Năng lực AI:** nhận diện mặt (enroll + đặt tên), tương tác thoại (wake word + command), định hướng âm, tự hành và tự về dock. citeturn20search29turn13view1turn20search0  
**Cloud vs local:** lịch sử Vector cho thấy thoại thường dựa cloud/subscription (đặc biệt sau thay đổi hạ tầng), và phía hãng có mô tả membership ảnh hưởng việc voice command hoạt động. citeturn14search23turn14search6 Một nhánh cộng đồng đã chạy server cục bộ (wire‑pod) để khôi phục voice command không phụ thuộc server hãng, cho thấy kiến trúc thực tế là robot gọi về “voice backend” qua network. citeturn14search1turn20search18  
**Cảm xúc & phản ứng:** mood system + hoạt cảnh mắt + âm thanh “beep” + chuyển động (quay đầu/nhún) tạo cảm giác hiểu/không hiểu. citeturn13view1  
**Nhận và nhớ chủ:** enrollment khuôn mặt và đặt tên là chức năng có trong SDK; do đó “owner model” tối thiểu dựa face ID. citeturn20search29  
**Giới hạn & trade‑off:** phụ thuộc backend thoại làm rủi ro “đồ vật mua rồi vẫn phụ thuộc dịch vụ”; pin nhỏ và môi trường bàn giới hạn trải nghiệm. citeturn14search23turn13view1  
**Độ phức tạp kỹ thuật (ước lượng): 4/5** (tối ưu hóa perception + personality + docking + cloud/back‑end).

**Robot: Miko (ví dụ Miko 3)**  
**Tổng quan:** Miko định vị là robot học tập/đồng hành cho trẻ em, nhấn mạnh “kid-safe moderated AI”, dashboard cho phụ huynh và cơ chế privacy (tắt mic/camera). citeturn23search0turn23search9  
**Phần cứng:** tài liệu hướng dẫn nhanh cho thấy có camera, loa, màn hình và “distance sensor” (phục vụ di chuyển an toàn). citeturn8view0 Một nguồn tổng hợp tech specs mô tả: camera góc rộng 720p, dual MEMS microphone, ToF range sensor và odometric sensors, bánh xe cao su. citeturn23search4turn23search10  
**Kiến trúc phần mềm (công khai + suy luận):**  
- Nhận thức: camera + ToF/odometry đủ cho tránh va chạm/edge trong phạm vi nhỏ; giá trị cốt lõi nằm ở hội thoại, nội dung giáo dục và kiểm soát an toàn. citeturn8view0turn23search0turn23search10  
- Bộ nhớ: có parent app, profile và theo dõi hoạt động; mô hình “nhớ chủ” thiên về tài khoản gia đình hơn là face ID (tài liệu công khai không nhấn mạnh nhận diện mặt như pet robots). citeturn23search13turn23search0  
- Tính cách & behavior engine: “personality-packed” thường là hoạt cảnh + kịch bản hội thoại theo ngữ cảnh học tập; khác với pet robot, “động lực” được dẫn bởi mục tiêu giáo dục và nhắc việc. citeturn23search13turn23search0  
**Năng lực AI:** hội thoại (được quảng bá “powered by AI and GPT” trong app), nội dung cập nhật liên tục; đa ngôn ngữ. citeturn23search13turn7view0  
**Cloud vs local:** Miko công khai sử dụng hạ tầng cloud để vận hành nền tảng dữ liệu/AI (case study), và cũng nói robot không “kết nối open internet” theo cách trực tiếp, mà qua hệ thống kiểm soát/nội dung đã kiểm duyệt. citeturn23search2turn23search9  
**Cảm xúc & phản ứng:** phần lớn là UI/màn hình + giọng nói; “emotion” (nếu có) thường là lớp UX để khuyến khích học tập. citeturn23search13turn23search0  
**Nhận và nhớ chủ:** mạnh về family account + phụ huynh kiểm soát; không thấy tuyên bố mạnh về nhận diện khuôn mặt như Aibo/Loona/EMO. citeturn23search0turn23search9  
**Giới hạn & trade‑off:** phụ thuộc cloud và hệ sinh thái nội dung khiến hệ thống “app/service first” hơn là robot autonomy; cảm giác “pet-like” thường kém hơn Aibo/Loona. citeturn23search2turn23search0  
**Độ phức tạp kỹ thuật (ước lượng): 3/5** (robotics vừa phải, nhưng độ khó lớn ở safety/moderation/nội dung/ops).

**Robot: Eilik (desktop companion)**  
**Tổng quan:** Eilik là robot để bàn thiên về cảm xúc/biểu cảm; có các mode như Heart/Idle/Sleep và “emotion engine” theo tương tác. citeturn5view0turn4view0turn5view2  
**Phần cứng:** spec nêu 3 vùng touch (đầu/bụng/lưng) + 1 infrared sensor; 4 servo; có microphone và speaker; không yêu cầu Wi‑Fi/Internet để dùng. citeturn5view2turn25search12 Manual còn mô tả infrared sensor ở đáy dùng cho “hover detection” (nhấc lên thì sợ/khóc). citeturn6view0turn25search3  
**Kiến trúc phần mềm (công khai + suy luận):**  
- Nhận thức: chủ yếu là touch + IR “off-ground” + rung/va (manual có setting “sensitivity to quake/height”), không có camera nên không có pipeline thị giác. citeturn4view0turn5view2turn6view0  
- Bộ nhớ: thiết kế offline khiến “nhớ chủ” khó/ít ý nghĩa; trải nghiệm dựa vào phản xạ tức thời và hoạt cảnh. citeturn25search12turn5view2  
- Tính cách & behavior engine: rất điển hình của mô hình FSM (“bình thường/vui/giận/buồn”) + bộ trộn hoạt cảnh theo trigger; đây là pattern tạo hiệu quả UX cao với chi phí AI thấp. citeturn5view2turn25search16  
**Năng lực AI:** không phải “AI perception” theo nghĩa CV/NLP mạnh; giá trị nằm ở animation và phản hồi cảm xúc. citeturn5view2turn25search12  
**Cloud vs local:** chạy local; có firmware update qua PC (tool cập nhật/đợt firmware). citeturn25search2turn25search5  
**Cảm xúc & phản ứng:** triển khai rõ ràng: trạng thái cảm xúc rời rạc + thư viện “faces” và motion. citeturn5view2turn25search16  
**Nhận và nhớ chủ:** hầu như không (không có camera, không cần account). citeturn25search12turn5view2  
**Giới hạn & trade‑off:** thiếu perception (mặt/giọng) nhưng đổi lại độ ổn định cao, phản hồi nhanh, rủi ro privacy thấp. citeturn25search12  
**Độ phức tạp kỹ thuật (ước lượng): 2/5** (cơ khí + animation + FSM là đủ tạo “bond”).

**Robot: LOVOT**  
**Tổng quan:** LOVOT là robot ôm ấp/đồng hành tập trung vào “gắn kết cảm xúc” hơn là làm việc. Hãng khẳng định hành động “không pre-programmed” theo nghĩa phản ứng thời gian thực từ >50 sensor qua deep learning/ML và có “nhân cách thay đổi theo mức độ tương tác của chủ”. citeturn22search1  
**Phần cứng:** bài giới thiệu thực tế mô tả LOVOT có ~50 sensor, 3 camera (mapping 180°, depth, thermal) và micro đặt ở “canister” trên đầu; camera/AI có thể nhận ra tới ~1,000 người; thân có nhiều sensor touch. citeturn22search16turn22search1  
**Kiến trúc phần mềm (công khai + suy luận):**  
- Nhận thức: camera + depth + thermal giúp phân biệt người/vật và tăng độ bền vững trong điều kiện ánh sáng/phông nền; sensor touch dày để đọc “skinship”. citeturn22search1turn22search16  
- Bộ nhớ: có “people model” quy mô lớn (tới ~1,000) và “owner engagement model”; chi tiết lưu trữ nội bộ vs cloud không công khai rõ, nhưng thiết bị có kết nối không dây. citeturn22search1turn22search16  
- Tính cách & behavior engine: hãng nêu nhân cách thay đổi theo engagement; về triển khai thường cần: (i) biến nội trạng (bond/curiosity/comfort…), (ii) policy chọn hành vi và (iii) animation system cực mượt, trong đó “mắt” có nhiều lớp hiển thị để tạo chiều sâu. citeturn22search1turn22search16  
**Năng lực AI:** nhận diện người (khuôn mặt), định vị trong phòng (mapping) và phản xạ thời gian thực. citeturn22search16turn22search1  
**Cloud vs local:** hãng nhấn mạnh phản ứng “little time lag” nhờ main/sub computer và deep learning FPGA; điều này hàm ý inference chính cho hành vi phải chạy local. citeturn22search1  
**Cảm xúc & phản ứng:** “mắt + giọng + thân nhiệt/ôm” (thường được mô tả ở các bài giới thiệu) tạo hiệu ứng đồng cảm rất mạnh; camera thermal góp phần cho “đọc người” và tạo phản ứng hợp lý. citeturn22search16turn22search1  
**Nhận và nhớ chủ:** điểm bán hàng rõ ràng: nhận diện nhiều người và phản ứng khác nhau theo mức gắn bó. citeturn22search16turn22search1  
**Giới hạn & trade‑off:** cảm biến dày + compute + cơ khí làm giá cao; nếu thêm voice assistant có thể phá “fiction” nhân vật (thậm chí nhiều bài đánh giá nhấn mạnh robot không cần nói). citeturn22search16  
**Độ phức tạp kỹ thuật (ước lượng): 5/5** (sensor fusion + human modeling + animation + system engineering lớn).

**Robot: Amazon Astro**  
**Tổng quan:** Astro là “smart display on wheels” kết hợp home monitoring và trợ lý giọng nói; điểm kỹ thuật nổi bật là điều hướng trong nhà + privacy-by-design (một phần xử lý on-device). citeturn15view1turn15view0  
**Phần cứng:** trang sản phẩm nêu cấu hình nhiều SoC (2×QCS605, 1×SDA660 và 1 bộ xử lý AZ1 Neural Edge), hệ loa 2.1 (2 driver 55mm + passive radiator). citeturn16search0turn26search0 Bài đánh giá mô tả có camera 5MP trên màn hình và camera 12MP dạng “periscope” bật lên để quan sát tốt hơn. citeturn26search1turn26search3  
**Kiến trúc phần mềm (công khai + suy luận):**  
- Nhận thức & điều hướng: Amazon mô tả stack SLAM + obstacle/depth sensors tạo depth map/obstacle map để path planning và các “recovery behaviors” khi gặp tình huống khó. citeturn15view0  
- Bộ nhớ: có bản đồ nhà; dữ liệu bản đồ dẫn xuất có thể lưu cloud để hiển thị/điều khiển qua app. citeturn15view0turn16search20  
- Tính cách & behavior engine: About Amazon mô tả personality qua “digital eyes”, chuyển động thân và âm sắc; thực tiễn là lớp animation + policy về “hang out where useful”. citeturn15view1  
**Năng lực AI:** nhận diện người thông qua “Visual ID” (opt‑in) để cá nhân hóa (đưa nhắc việc/call đúng người) và phát hiện “unrecognized person” cho home monitoring. citeturn15view1turn16search20  
**Cloud vs local:** Amazon Science nêu xử lý local là thiết yếu vì yêu cầu phản ứng nhanh; dữ liệu thô từ sensor điều hướng được trích xuất thành đo khoảng cách rồi bỏ; map 2D dẫn xuất gửi lên cloud sau khi khám phá xong. citeturn15view0 Visual ID được nói là lưu trên thiết bị, không lưu ảnh/video lên cloud để cung cấp Visual ID. citeturn22search6turn15view1  
**Cảm xúc & phản ứng:** “mặt” trên screen + chuyển động “proxemics” (đứng khoảng cách xã hội phù hợp) làm Astro trông “lịch sự” hơn khi tiếp cận người. citeturn15view0turn15view1  
**Nhận và nhớ chủ:** Visual ID là cơ chế explicit enrollment; có đường xóa Visual ID và điều khiển privacy. citeturn22search6turn16search2  
**Giới hạn & trade‑off:** thiết kế thiên về home monitoring khiến bài toán privacy cực nhạy; đồng thời điều hướng trong nhà đủ tốt đòi hỏi sensor + compute cao, làm chi phí hệ thống lớn hơn “pet robots tabletop”. citeturn15view0turn16search20  
**Độ phức tạp kỹ thuật (ước lượng): 4.5/5** (navigation/HRI/privacy engineering nặng, cơ khí vừa).

**Robot khác đáng chú ý: ElliQ**  
**Tổng quan:** ElliQ là robot đồng hành cho người cao tuổi, trọng tâm là **chủ động bắt chuyện** và gợi ý hoạt động, không phải mobility. Một bài học thuật mô tả ElliQ khởi tạo tương tác dựa trên sensing từ camera + microphone, và cá nhân hóa dựa trên tính cách/sở thích/hành vi học được. citeturn19search10turn21search5  
**Phần cứng:** thông cáo nêu có camera (computer vision based), quad microphones (noise/echo cancellation), cảm biến môi trường (air quality/temperature), LED arrays và 3 brushless motors cho chuyển động “đầu” (expressive motion). citeturn21search16  
**Phần mềm/AI:** thiên về conversation, proactive coaching; giọng nói có thể dùng wake word và speaker identification (một đối tác voice tech công bố). citeturn21search8turn19search10  
**Cloud vs local:** bản chất dịch vụ coaching và nội dung thường cần cloud; chi tiết triển khai không công khai đầy đủ trong tài liệu marketing. citeturn19search10turn21search16  
**Độ phức tạp (ước lượng): 3.5/5** (độ khó nằm ở AI dialogue + personalization + triển khai dịch vụ, cơ khí nhẹ).

**Robot khác đáng chú ý: Jibo**  
**Tổng quan:** Jibo là social robot để bàn (nay không còn là sản phẩm đại chúng), nổi bật ở “social interaction”.  
**Phần cứng:** spec công bố có stereo cameras, 6 microphones, touch sensors, 3 motor cho chuyển động mượt và chạy Linux; cung cấp JavaScript SDK. citeturn19search3turn19search15turn19search1  
**Phần mềm/AI:** định vị social interaction, theo dõi người bằng camera, định hướng âm; cloud phụ thuộc theo mô hình trợ lý. citeturn19search15turn19search3  
**Độ phức tạp (ước lượng): 3/5** (perception xã hội + animation tốt, mobility không nặng).

**Robot khác đáng chú ý: PARO**  
**Tổng quan:** PARO là robot trị liệu dạng hải cẩu, được nghiên cứu rộng rãi; điểm đáng học cho pet robots là “bond” đạt được với sensor đơn giản nhưng hành vi đúng tâm lý. citeturn21search10turn21search22  
**Phần cứng:** hãng nêu 5 loại sensor: tactile, light, audition, temperature, posture. citeturn21search0  
**Phần mềm/AI:** một bài học thuật mô tả PARO dùng hierarchical intelligent control và có chức năng học tên riêng + thích nghi personality/behavior theo tương tác. citeturn21search22turn21search7  
**Cloud vs local:** sản phẩm trị liệu thường chạy local; giá trị nằm ở phản hồi đa giác quan và “learning” mức vừa. citeturn21search0turn21search22  
**Độ phức tạp (ước lượng): 3/5** (cơ khí/AI vừa phải nhưng tối ưu UX trị liệu rất tinh).

**Robot khác đáng chú ý: Moflin**  
**Tổng quan:** Moflin là “emotional support robot pet” dạng lông mềm, ít bậc tự do nhưng nhấn mạnh mô phỏng cảm xúc và gắn bó theo thời gian. citeturn19search9turn19news40  
**Phần cứng:** trang sản phẩm liệt kê mic, illuminance sensor, touch sensors, accelerometer/gyroscope; chuyển động 2 axes (xoay + gật đầu). citeturn19search9  
**Phần mềm/AI:** mô tả “emotional simulations” và cảm xúc tiến hóa theo tương tác; một bài báo nói Moflin nhận chủ qua voice + cách chạm/ôm để tạo bond cá nhân hóa. citeturn19search9turn19news40  
**Cloud vs local:** phần cảm xúc/nhận tương tác có thể local; nhưng sản phẩm có app theo dõi trạng thái (tùy triển khai). citeturn19search9turn19news40  
**Độ phức tạp (ước lượng): 2.5/5** (sensor ít + cơ khí ít, nhưng mô hình cảm xúc/UX tinh).

## Bảng so sánh tổng hợp

| Robot | Dạng di chuyển | Camera / Depth | Audio | Sensor khác | AI nổi bật | Cloud vs local | Phức tạp (1–5) |
|---|---|---|---|---|---|---|---|
| Aibo (ERS‑1000) | 4 chân, 22 DoF citeturn10view0 | 2 camera (front + SLAM) citeturn10view0turn11search0 | 4 mic + speaker citeturn10view0 | ToF, ranging, touch, IMU… citeturn10view0 | SLAM + nhận mặt + “learn/bond” citeturn9search7turn11search3 | Cloud plan là trung tâm “phát triển” citeturn9search7turn9search11 | 5 |
| Loona | Bánh xe + body/ear servo citeturn24view0 | RGB 720p + 3D ToF citeturn24view0 | 4‑mic array citeturn24view0 | IMU, touch citeturn24view0 | Face/gesture + chat (Lex/ChatGPT) citeturn22search7turn24view0 | “Local as possible” nhưng chat cloud citeturn24view0turn22search7 | 4 |
| EMO | Tabletop walker | Camera (wide angle) citeturn12search3turn12search33 | 4‑mic array citeturn12search33 | ToF, drop sensors, IMU, touch, light citeturn12search33 | Face recognition + tabletop autonomy citeturn12search21turn12search33 | Reflex local; “assistant/knowledge” thường cần mạng citeturn12search3turn12search33 | 3.5 |
| Vector | Treads + arm | HD wide-angle + ToF NIR citeturn20search4turn20search0 | 4‑mic array citeturn20search4 | Cliff, touch, IMU citeturn20search8turn20search0 | Face enrollment + map bàn + mood system citeturn20search29turn13view1 | Voice phụ thuộc membership/backend; có giải pháp local server citeturn14search23turn14search1 | 4 |
| Miko 3 | Bánh xe nhỏ | Wide-angle camera + ToF (tổng hợp) citeturn23search4turn23search10 | Mic (dual MEMS) + speaker citeturn23search4turn8view0 | Odometry citeturn23search10 | Kid-safe conversational AI citeturn23search0turn23search13 | Nền tảng cloud + kiểm soát an toàn citeturn23search2turn23search9 | 3 |
| Eilik | Stationary | Không | Mic + speaker citeturn5view2 | Touch×3 + IR ×1 citeturn5view2turn6view0 | Emotion FSM + hoạt cảnh citeturn5view2 | Offline (không cần Wi‑Fi) citeturn25search12 | 2 |
| LOVOT | Wheels + arms | 3 camera (mapping/depth/thermal) citeturn22search16 | Mic + speaker citeturn22search16turn22search1 | >50 sensors (touch…) citeturn22search1turn22search16 | Nhận người quy mô lớn + deep learning realtime citeturn22search1turn22search16 | Inference realtime nhờ compute on-board citeturn22search1 | 5 |
| Astro | Wheels + screen | 5MP + 12MP periscope citeturn26search1turn26search3 | Far-field mics + loa 2.1 citeturn26search0turn26search3 | Depth/obstacle/nav sensors citeturn15view0turn16search20 | SLAM + Visual ID + home monitoring citeturn15view0turn15view1 | Raw nav data local; map dẫn xuất cloud; Visual ID on-device citeturn15view0turn22search6 | 4.5 |

## Mẫu thiết kế, kiến trúc tối thiểu và khả thi cho dự án hobby

**SECTION: Key design patterns used by successful pet robots**

Một số pattern lặp đi lặp lại ở các robot “được yêu thích”:

Thứ nhất là **biểu cảm đa kênh** (mắt/mặt trên màn hình, âm thanh, micro‑motions) được ưu tiên hơn “trả lời đúng”. Astro mô tả personality qua “digital eyes” + body movement + tone; Vector được thiết kế để người dùng nhìn động tác mà hiểu robot “đang nghe/đang hiểu/không hiểu”; Aibo/LOVOT thì dùng chuyển động sinh học (head/ear/tail) tạo cảm giác sống. citeturn15view1turn13view1turn10view0turn22search16

Thứ hai là **vòng lặp autonomy khi rảnh** (idle behaviors): Eilik có Heart/Idle/Sleep; Vector nhấn mạnh robot có thể “just hang out”; Aibo/Loona cũng quảng bá tương tác “nở dần” theo ngày. Điều này thường triển khai bằng một “scheduler” chọn hành vi nhỏ theo thời gian + sự kiện môi trường, thay vì phải có AI mạnh. citeturn5view0turn13view1turn9search7turn24view0  

Thứ ba là **an toàn điều hướng chạy local**: Astro nói thẳng local processing là thiết yếu vì phản ứng nhanh; Aibo có SLAM camera + ToF/ranging để tránh rơi; EMO/Vector dùng ToF/laser + drop/cliff sensors để không rớt khỏi bàn. Đây là lớp “đừng làm hỏng trải nghiệm” quan trọng hơn cả hội thoại. citeturn15view0turn10view0turn12search33turn20search0

Thứ tư là **“nhận chủ” được thiết kế như một nghi thức (ritual) enrollment**: Astro dùng Visual ID opt‑in và lưu on‑device; Vector có face enrollment qua SDK; Aibo gắn với app + cloud plan. Về UX, việc “dạy robot biết mình” là một khoảnh khắc tạo gắn kết. citeturn15view1turn22search6turn20search29turn9search7

Cuối cùng là **cloud được dùng như tăng cường, không phải nền chuyển động**: cloud hợp cho LLM/tri thức rộng, cập nhật nội dung, phân tích dài hạn; còn thời gian thực (motors, safety, phản xạ) phải local. citeturn15view0turn23search2turn22search7

**SECTION: Minimal architecture needed for a hobby AI pet robot**

Với mục tiêu “hobby nhưng có cảm giác đồng hành”, kiến trúc tối thiểu nên chia làm 2 lớp rõ ràng:

Lớp A (Real-time control, MCU) trên Arduino/ESP32: điều khiển motor/servo theo loop 50–200 Hz; đọc cảm biến an toàn (ToF/IR chống rơi, bumper/touch, IMU); thực thi lệnh vận động cấp thấp (set speed, go-to heading) và báo telem về điện thoại. Giao tiếp MCU↔phone dùng UART (USB-serial) hoặc BLE/Wi‑Fi; UART là con đường “đơn giản & ổn định” khi bạn có thể cắm OTG. citeturn18search13

Lớp B (Perception + behavior + memory, Android phone):  
- Vision: dùng pipeline on-device (TFLite) cho object detection nếu cần, và face detection để lấy bbox/landmarks; lưu ý ML Kit “detect faces” **không** “recognize people”, nên muốn “nhớ chủ” bạn cần thêm bước face embedding (FaceNet/ArcFace‑style) + vector store cục bộ. citeturn18search3turn18search0  
- Audio: chạy STT offline (Vosk) hoặc dùng cloud STT tùy mục tiêu; Vosk hỗ trợ offline và có demo trên Android. citeturn18search8turn18search16  
- Behavior: một state machine + vài biến liên tục (mood/energy/curiosity/bond) + scheduler idle.  
- Memory: SQLite/Room lưu “owner profile”, last-seen timestamps, sở thích đơn giản, vài “events” (được vuốt ve nhiều, được gọi tên…).  
- LLM: optional; nếu dùng, bọc bằng policy an toàn (giới hạn prompt/response, tránh hành vi nguy hiểm, cache câu trả lời phổ biến).

**SECTION: Recommended features for a first prototype**

Nếu mục tiêu là “đụng vào thấy đáng yêu và muốn tương tác tiếp”, ưu tiên các tính năng có ROI cao giống pattern Eilik/Vector/Loona:

Một là màn hình “mặt” + animation system: 10–20 trạng thái mặt cơ bản, micro‑expressions (nháy mắt, nhìn theo người), âm thanh “beep/voice” đồng bộ nhịp chuyển động. Astro cho thấy “digital eyes + body movement” đã đủ tạo persona. citeturn15view1turn26search1  

Hai là “idle autonomy”: đứng yên vẫn có việc làm (nhìn quanh, thở, buồn ngủ, gọi nhẹ), tương tự Heart/Idle/Sleep của Eilik. citeturn5view0turn5view2  

Ba là an toàn: edge detection tối thiểu (ToF/IR hướng xuống) và “panic behavior” khi gần rơi như EMO/Vector; nếu không có, bạn sẽ mất người dùng ngay lần rơi đầu tiên. citeturn12search33turn20search0

Bốn là nhận chủ bước đầu: enrollment 1 người bằng face embedding (local) + phản ứng khác nhau khi thấy “chủ” vs “người lạ”, giống hướng Visual ID/face enrollment ở Astro/Vector. citeturn15view1turn20search29

Năm là thoại mức thấp: wake word + 10–30 command cố định (đến đây/nhảy múa/chụp ảnh/kể chuyện ngắn). Nếu muốn offline, Vosk là lựa chọn thực dụng. citeturn18search8turn18search1

**SECTION: Technical feasibility analysis for a project with these constraints**

Với ràng buộc “Android phone làm não + Arduino/ESP32 làm controller + camera điện thoại làm vision + cloud optional + side project”, tính khả thi chia thành 3 tầng:

Tầng dễ (khả thi cao trong vài tuần): MCU điều khiển bánh xe/servo, đọc ToF/IR + touch; điện thoại chạy state machine, animation và điều khiển; vision chỉ cần face detection (bbox) để tạo “eye contact”. ML Kit cho face detection chạy on-device và đủ nhanh cho realtime (nhưng không phải face recognition). citeturn18search3turn18search0  

Tầng trung bình (khả thi nếu kiên trì 1–3 tháng): face recognition (embedding + threshold + lưu vector), object detection TFLite (SSD/EfficientDet‑Lite), STT offline (Vosk) + TTS (Android). TensorFlow Lite Task Library/Model Maker giúp triển khai object detection trên mobile theo quy trình rõ ràng. citeturn18search7turn18search15turn18search8  

Tầng khó (dễ “đốt thời gian”): SLAM đầy đủ + tự điều hướng trong nhà kiểu Astro/Aibo. Astro mô tả đây là hệ thống depth map + SLAM + path planning + recovery behaviors và cả “proxemics” (đứng đúng khoảng cách xã hội), tức khối lượng kỹ thuật lớn hơn một side project. citeturn15view0turn15view1

Kết luận feasibility: với kiến trúc phone+MCU, bạn hoàn toàn có thể làm một robot đạt “cảm giác đồng hành” kiểu tabletop/wheels (Loona/Vector‑lite), nhưng không nên đặt mục tiêu ngay từ đầu là “đi khắp nhà an toàn” như Astro hoặc “chó 4 chân chuyển động mượt” như Aibo. citeturn15view0turn10view0turn24view0  

## Khuyến nghị triển khai cho dự án side project

### What features should be built first

1. **Biểu cảm và timing**: mặt trên màn hình + âm thanh + 5–10 micro‑motions (ngó, gật, giật mình, thở, ngủ). Đây là thứ tạo “bond” nhanh nhất, đã được chứng minh ở Astro/Vector/Eilik. citeturn15view1turn13view1turn5view2  
2. **Vòng lặp hành vi tối thiểu**: state machine + biến mood/energy + idle scheduler; bảo đảm robot “sống” khi người dùng không làm gì. citeturn5view0turn13view1  
3. **An toàn chống rơi/va**: ToF/IR hướng xuống + hành vi lùi lại; nếu chạy trên bàn thì đây là “tính năng sống còn”. citeturn12search33turn20search0turn6view0  
4. **Nhận chủ 1 người**: enrollment + phản ứng chào; triển khai bằng face detection + embedding lưu local. (Nhắc lại: face detection không tự nhận dạng người.) citeturn18search3turn15view1turn20search29  

### What features should be avoided early

1. **SLAM toàn nhà / tự đi khắp nhà**: khối lượng như Astro cho thấy cần depth mapping, SLAM, planner và recovery behaviors; quá nặng cho side project. citeturn15view0turn15view1  
2. **Hội thoại “tự do” ngay từ đầu**: LLM làm bạn “tưởng” robot thông minh, nhưng sẽ kéo theo chi phí cloud, latency, moderation và failure mode khó kiểm soát; hãy bắt đầu bằng command nhỏ + phản ứng cảm xúc. citeturn22search7turn23search9  
3. **Cơ khí phức tạp (nhiều DoF)**: Aibo đạt cảm giác sinh học nhờ 22 axes, nhưng đây là “đốt ngân sách + đốt thời gian” nếu bạn chưa có nền tảng cơ khí/điều khiển. citeturn10view0  

### What architectural mistakes to avoid

1. **Trộn lẫn realtime control và logic AI trong MCU**: hãy giữ MCU như “motion/safety box”, còn mọi thứ khó (vision, NLP, memory) chạy trên phone. Đây là cách các hệ lớn cũng tách lớp: local realtime cho motion, cloud/on-device compute cho nhận thức cao hơn. citeturn15view0turn15view1  
2. **Không thiết kế “degrade gracefully” khi mất mạng**: Vector là ví dụ điển hình về rủi ro backend thoại; nên bảo đảm robot vẫn vui/interactive khi offline (ít nhất như Eilik). citeturn14search23turn25search12turn14search1  
3. **Bỏ qua privacy/consent cho camera/mic**: Astro và Miko đều đặt nút tắt mic/camera và cơ chế kiểm soát dữ liệu là thành phần cốt lõi; hobby project cũng nên có “kill switch” và hiển thị rõ khi đang ghi/stream. citeturn15view1turn23search0turn16search20