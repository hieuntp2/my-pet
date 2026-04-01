# Thiết kế phần cứng thân robot thú cưng AI dùng Android làm “não”

## Bối cảnh, yêu cầu và giả định thiết kế

Thiết kế này coi điện thoại Android gắn trên robot là “máy tính trung tâm” chạy AI (nhận hình ảnh từ camera, xử lý giọng nói, lập kế hoạch hành vi), còn vi điều khiển đảm nhiệm lớp **thời gian thực**: đọc cảm biến, điều khiển động cơ/servo, điều khiển LED và gửi phản hồi trạng thái. Kết nối giữa Android và vi điều khiển qua **BLE hoặc Wi‑Fi**.

Các ràng buộc kỹ thuật quan trọng tác động trực tiếp đến cơ khí và điện:

- **Điện thoại là tải trọng lớn nhất**: vị trí gá phone quyết định trọng tâm và độ ổn định khi tăng/giảm tốc và quay tại chỗ. Với robot nhỏ, cần đặt phone càng gần tâm và càng thấp càng tốt (trên “tầng 2” vẫn được nhưng phải giữ bánh xe đủ rộng).  
- **Hệ điều khiển tách lớp** giúp bắt đầu dễ hơn: Android chỉ gửi “ý định” (vận tốc/servo/LED), MCU thực thi và trả telemetry (khoảng cách, IMU, bumper…). Mô hình này giảm rủi ro lag khi Android bận xử lý.  
- **Chọn bus cảm biến** ưu tiên I²C để đi dây gọn (ToF, IMU, PCA9685), còn bumper/ultrasonic dùng digital. Android ↔ MCU nên ưu tiên giao thức dễ debug (text/JSON), rồi tối ưu dần.

## So sánh và chọn vi điều khiển

### Thông số cốt lõi của từng lựa chọn

**Arduino (lấy mốc Arduino Uno Rev3 vì phổ biến và “beginner friendly”)**  
Arduino Uno Rev3 dựa trên ATmega328P, có 14 chân digital (6 chân PWM), 6 chân analog và xung nhịp 16 MHz. citeturn0search1turn0search5  
Về bản chất, Uno Rev3 **không có Wi‑Fi/BLE tích hợp**, nếu cần không dây phải thêm shield/module. Có các biến thể Arduino “có sẵn radio”, ví dụ UNO R4 WiFi tích hợp thêm ESP32‑S3 cho Wi‑Fi/Bluetooth. citeturn6search2

**ESP32 (dòng phổ thông ESP32‑WROOM/DevKit là lựa chọn thực dụng)**  
ESP32 tích hợp **Wi‑Fi + Bluetooth (Classic + BLE)**, CPU Xtensa LX6 đơn/đa lõi đến 240 MHz, bộ nhớ trong gồm 448 KB ROM và 520 KB SRAM; số GPIO lập trình được lớn, có LED PWM đến 16 kênh và có ngoại vi PWM phù hợp điều khiển động cơ/LED. citeturn9view0  
Về mức điện áp: ESP32 thiết kế cho miền 3.3 V, với giới hạn nguồn và mức logic vào/ra bám theo VDD (ví dụ VDD tối đa khuyến nghị đến 3.6 V; mức vào cao tối đa khoảng VDD + 0.3 V). Điều này đồng nghĩa **không “chịu 5 V logic” trực tiếp** trên GPIO như Arduino Uno. citeturn10view1turn10view2

**Raspberry Pi Pico (RP2040)**  
RP2040 là MCU lõi kép Arm Cortex‑M0+ với 264 kB RAM nội, và Pico nổi bật bởi I/O linh hoạt có PIO (Programmable I/O). citeturn0search2  
Tuy nhiên, về “không dây”: theo tài liệu dòng Pico, **biến thể có hậu tố W** mới có Wi‑Fi và Bluetooth. citeturn11search2 (Vì vậy “Pico” bản thường sẽ cần module ngoài nếu bắt buộc BLE/Wi‑Fi.)

### Bảng đối chiếu nhanh theo đúng bài toán robot thú cưng dùng Android

| Tiêu chí | Arduino Uno (mốc Rev3) | ESP32 | Raspberry Pi Pico (RP2040) |
|---|---|---|---|
| Không dây (BLE/Wi‑Fi) | Cần module ngoài; có biến thể Arduino khác có radio | Tích hợp Wi‑Fi + BLE/BT | Bản Pico thường không; bản “W” mới có |
| Điều khiển động cơ/servo | Dễ, PWM phần cứng đơn giản | Dễ nhưng cần chú ý 3.3 V logic; nhiều PWM | Dễ, I/O linh hoạt; không dây là điểm nghẽn |
| Độ “beginner friendly” tổng thể cho dự án này | Tốt nếu không cần wireless; nếu thêm module sẽ phức tạp | Tốt vì tích hợp wireless và hệ sinh thái lớn | Tốt về MCU; nhưng thêm wireless làm khó |
| Rủi ro tích hợp | Thêm module BLE/Wi‑Fi + RAM/Flash hạn chế | 3.3 V logic, nhiễu nguồn do Wi‑Fi/motor | Không dây, driver/wiring phức tạp hơn |

### Khuyến nghị cuối cùng

**Khuyến nghị chọn ESP32 làm vi điều khiển chính** cho cả 3 mức robot (đơn giản → nâng cao). Lý do:

- **Tích hợp đúng hai tiêu chí bắt buộc**: Wi‑Fi và Bluetooth/BLE đã nằm trên chip (không phải “ghép thêm” như Uno Rev3 hoặc Pico bản thường). citeturn9view0  
- **Dư PWM và I/O** để điều khiển 2 motor + nhiều servo/LED (đặc biệt khi kết hợp PCA9685 cho servo), và vẫn đọc được nhiều cảm biến I²C/digital. citeturn9view0turn1search3  
- **Phù hợp mô hình “Android làm não”**: ESP32 chỉ cần làm realtime I/O + truyền thông, nên không đòi hỏi CPU kiểu SBC; nhưng vẫn có dư hiệu năng và stack BLE/Wi‑Fi. citeturn9view0  

Điểm phải nói thẳng để tránh vỡ dự án: **ESP32 là 3.3 V** và giới hạn mức vào cao tối đa dựa trên VDD, vì vậy các module 5 V (đặc biệt chân tín hiệu output 5 V) cần chia áp/level‑shifter. citeturn10view2turn10view1 Đây là “giá phải trả” hợp lý để đổi lấy wireless tích hợp và chi phí tổng thể thấp hơn so với ghép Uno/Pico + module.

## Hệ truyền động và cơ cấu servo

### Hệ động cơ di chuyển

**Kiểu truyền động khuyến nghị (beginner-friendly nhất): Differential drive (2 bánh chủ động + 1 bánh tự do/caster).**  
Ưu điểm: ít linh kiện, dễ điều khiển (chỉ cần PWM trái/phải), quay tại chỗ được, phù hợp robot thú cưng “đi theo/né vật cản”.

**Chọn động cơ theo khung cơ khí và tải điện thoại**
- **Prototype nhỏ, giá rẻ**: motor hộp số nhựa kiểu TT (3–6 V) + bánh 60–70 mm.  
- **Đầm hơn, chịu tải tốt hơn**: motor hộp số kim loại loại N20 (6–12 V) hoặc motor hộp số 37 mm (thường dùng xe robot tải vừa).  
- Nếu muốn “đi thẳng chuẩn” hoặc có hành vi mượt, nên cân nhắc **encoder bánh** (không bắt buộc trong yêu cầu, nhưng là nâng cấp giá trị cao): MCU đọc encoder để khép vòng vận tốc, giảm lệch do ma sát/mặt sàn.

### Driver động cơ DC

Ba lựa chọn thực tế (từ beginner → tối ưu):

**TB6612FNG (khuyến nghị cho dự án này vì phổ biến, dễ dùng, đủ dòng cho 2 motor nhỏ/vừa)**  
Theo thông tin sản phẩm, TB6612FNG là driver cầu H full‑bridge cho **2 motor DC**, có các chế độ CW/CCW/Brake/Stop và có **dòng ngõ ra 1.2 A (ave) / 3.2 A (peak)**, điện áp cấp **15 V**. citeturn12view0  
Điểm hợp “robot chạy pin”: dùng MOSFET nên thường hiệu quả hơn các driver BJT đời cũ (giảm sụt áp, giảm nóng). Nhận định “MOSFET-based hiệu quả hơn BJT-based như L298N” cũng được nhấn mạnh trong tài liệu của nhà cung cấp module/driver robot. citeturn2search5

**DRV8833 (lựa chọn tốt nếu dùng pin 1S/2S, muốn driver gọn và hiện đại)**  
DRV8833 hỗ trợ dải nguồn **2.7–10.8 V**, dòng **1.5 A RMS, 2 A peak mỗi cầu H**, và có MOSFET ON‑resistance thấp. citeturn5search0  
Điểm trừ trong dự án: giới hạn 10.8 V khiến nó phù hợp nhất với 1S/2S (2S Li‑ion max 8.4 V vẫn ổn), nhưng không phù hợp nếu bạn cố tình dùng motor 12 V và pin cao hơn.

**L298N (không khuyến nghị cho robot chạy pin, trừ khi bạn đã có sẵn và chấp nhận nóng/hao pin)**  
L298 là driver cầu H “cổ điển”. Vấn đề thực dụng là **sụt áp bão hòa lớn** (ví dụ nguồn saturation voltage điển hình 1.35 V ở 1 A theo thông tin linh kiện), làm giảm điện áp thực đến motor và tăng nhiệt. citeturn5search13turn5search5 Với robot nhỏ chạy pin, điều này thường biến thành “xe yếu và nóng”.

### Hệ servo

Servo dùng để tạo tính “thú cưng”: quay đầu (pan), gật (tilt), tai/đuôi, cơ cấu “nhìn” cảm biến…

**Servo khuyến nghị cho beginner**
- **SG90** là lựa chọn kinh điển cho cơ cấu nhẹ: 9 g, điện áp hoạt động 4.8 V, tốc độ 0.1 s/60° (4.8 V), torque stall khoảng 1.8 kg·cm (4.8 V). citeturn4search1  
- Khi cần lực hơn (tai/đuôi cứng hoặc đầu gắn thêm linh kiện), cân nhắc micro servo gear kim loại (ví dụ MG90S dạng phổ biến trên thị trường).

**Driver servo (rất đáng dùng khi servo > 2 hoặc khi cần ổn định xung)**
- **PCA9685** là IC PWM 16 kênh, 12-bit, chạy qua I²C; hỗ trợ nguồn 2.3–5.5 V và I/O chịu đến 5.5 V. citeturn1search3  
- Tần số PWM điều chỉnh khoảng **24 Hz đến 1526 Hz**, phù hợp tạo xung servo (thường 50 Hz) và đồng thời điều khiển LED PWM. citeturn5search2  

**Nguồn servo là chỗ người mới hay “gãy” dự án**  
Servo có 3 dây power/ground/signal. Ground của servo **phải nối chung** với ground mạch điều khiển để tín hiệu có tham chiếu đúng. citeturn4search0 Servo không nên lấy dòng từ chân regulator nhỏ trên board MCU; cách đúng là có rail 5 V đủ dòng riêng (chi tiết ở phần nguồn).

## Hệ cảm biến

Mục tiêu cảm biến cho “AI pet robot” là: tránh va chạm, biết tư thế/độ rung, và tạo phản hồi hành vi (chạm/đụng).

### Cảm biến khoảng cách

**Ultrasonic (HC‑SR04)**
- Dải đo điển hình **2 cm – 400 cm**, sai số cỡ **3 mm**, góc hiệu dụng khoảng **15°** theo thông số module phổ biến. citeturn3search12  
Ưu điểm: rẻ và dễ mua. Nhược điểm thực dụng: góc quét rộng, dễ nhiễu ở phòng dội âm hoặc bề mặt mềm; và nhiều module dùng logic 5 V (cần chú ý nếu MCU là 3.3 V).

**Time-of-Flight**
- **VL53L0X** đo khoảng cách tuyệt đối lên đến **2 m**. citeturn7search0  
- **VL53L1X** là ToF tầm xa hơn, khoảng cách chính xác đến **4 m** và tần số đo nhanh đến **50 Hz**. citeturn3search10  
Ưu điểm: chùm đo hẹp và hành vi “giống lidar mini”, thường ổn định hơn ultrasonic khi né vật cản phía trước; đa số breakout chạy I²C nên đi dây gọn.

Khuyến nghị thực tế:  
- Robot nhỏ trong nhà: **1 cảm biến ToF trước** là “đáng tiền” hơn ultrasonic nếu ngân sách cho phép.  
- Nếu muốn tiết kiệm tối đa: ultrasonic vẫn dùng được nhưng cần lọc số (median/EMA) và cẩn thận tín hiệu 5 V.

### Bump sensors (công tắc va chạm)

Bumper kiểu **microswitch** hoặc “whisker” là cảm biến “độ tin cậy cao nhất” cho beginner: nếu ToF/ultrasonic đọc sai, bumper vẫn cứu robot khỏi tông mạnh. Lắp 2–3 công tắc phía trước là đủ cho prototype.

### IMU

IMU giúp robot biết rung, nghiêng, quay… để làm hành vi “thú cưng” (ngẩng đầu khi dừng, lắc nhẹ khi bị chạm, tự cân bằng tốc độ quay) và hỗ trợ odometry hợp nhất.

**MPU‑6050** là IMU 6 trục (3‑axis gyro + 3‑axis accel) và dùng giao tiếp I²C. citeturn7search15  
Tài liệu cũng nêu MPU‑6050 hỗ trợ I²C đến 400 kHz. citeturn7search5

Khuyến nghị: dùng IMU qua I²C chung với ToF và PCA9685 (nếu có), nhưng cần quản lý địa chỉ I²C và dây/ngắt nhiễu đúng cách.

## Hệ nguồn và quản lý năng lượng

Khối nguồn quyết định **độ ổn định** hơn cả CPU. Với robot chạy động cơ + servo, thiết kế nguồn phải chấp nhận **dòng xung lớn** và nhiễu điện từ.

### Lựa chọn “beginner-friendly” theo mức độ

**Mức dễ nhất (an toàn, ít rủi ro): pin AA NiMH/Alkaline**
- 4×AA NiMH (≈4.8 V) hoặc 4×AA alkaline (≈6 V) có ưu điểm: không cần BMS/charger Li‑ion phức tạp.  
- Phù hợp prototype nhỏ (2 motor TT + 0–2 servo + ESP32 + 1 cảm biến).

**Mức cân bằng (khuyến nghị cho medium/advanced): 2S Li‑ion + buck**
- 2 cell Li‑ion nối tiếp (2S) cho điện áp danh định ≈7.4 V, phù hợp motor 6–12 V và dễ hạ áp xuống 5 V/3.3 V bằng buck.  
- Bắt buộc có **mạch bảo vệ** (BMS/Protection) để tránh overcharge/overdischarge/overcurrent.

Với Li‑ion/Li‑poly, nguyên tắc an toàn cơ bản: không sạc quá ~4.2 V/cell, không xả sâu dưới ~3.0 V/cell, và không vượt dòng cho phép; đây là mục đích của mạch bảo vệ cell và bộ sạc đúng chuẩn. citeturn8search4  
Ngay cả các pack “có protection”, vẫn cần sạc đúng kiểu CV/CC; ví dụ sản phẩm pin Li‑ion có protection cũng nhấn mạnh chỉ dùng bộ sạc CV/CC và giới hạn dòng sạc. citeturn8search0

### Kiến trúc cấp nguồn khuyến nghị

Một sơ đồ rail nguồn “đúng bài” cho robot dùng ESP32:

- **VMOTOR (pin thô)** → driver motor (TB6612FNG/DRV8833)  
- **5V_SERVO (buck 5–6 V, dòng lớn)** → servo + LED công suất (nếu có)  
- **3V3_LOGIC (buck/LDO 3.3 V ổn định)** → ESP32 + ToF + IMU (đa số cảm biến I²C)

Nguyên tắc nối đất: tất cả rail phải **chung GND** để tín hiệu điều khiển đúng tham chiếu, đặc biệt là servo. citeturn4search0

### Giảm nhiễu và chống reset khi motor/servo giật

Động cơ DC sinh nhiễu chổi than và dòng xung. Một thực hành phổ biến của nhà cung cấp phần cứng robot là:
- hàn **tụ 0.1 µF gốm** trực tiếp **ngang hai cực motor**, hoặc 3 tụ (một ngang cực + hai tụ từ mỗi cực về vỏ) để giảm nhiễu,
- dùng dây motor và dây nguồn **ngắn, to**, có thể xoắn đôi. citeturn13search20

Trong thực tế với beginner, chỉ cần “tụ 0.1 µF ngang cực motor + tụ điện phân lớn gần driver motor” đã giảm đáng kể hiện tượng ESP32 reset khi đổi hướng hoặc thắng gấp.

### Cấp nguồn cho điện thoại Android

Bạn có hai lựa chọn kiến trúc:

- **Đơn giản nhất**: để điện thoại dùng pin riêng (robot chỉ cấp nguồn cho phần robot). Ưu điểm: ít rủi ro nhiễu và ít phụ thuộc chuẩn sạc USB‑C/PD.  
- **Dùng lâu**: cấp 5 V cho điện thoại từ rail 5 V buck. Khi làm cách này, cần coi điện thoại là “tải nhạy”, nên tách đường 5 V của phone khỏi đường servo/motor bằng lọc (LC/RC) và tụ cục bộ để giảm sụt áp/nhấp nhô.

## Kiến trúc giao tiếp và giao thức Android ↔ vi điều khiển

Bạn có thể làm theo 2 hướng. Với tiêu chí beginner-friendly, mình khuyến nghị bắt đầu bằng BLE; Wi‑Fi để như “tùy chọn nâng cấp”.

### Kiến trúc BLE khuyến nghị

**Vai trò**: ESP32 làm thiết bị BLE (peripheral/GATT server), Android làm trung tâm (central/GATT client). BLE ứng dụng dựa trên GATT, trao đổi dữ liệu dạng “attributes” qua services/characteristics. citeturn0search3

**Bố cục GATT (đơn giản mà đủ dùng)**
- 1 service tùy biến: `RobotControlService`
- 2 characteristic chính:
  - `cmd_rx`: Android **Write** (ưu tiên “Write Without Response” để giảm latency)
  - `telemetry_tx`: ESP32 **Notify** để đẩy sensor/state về Android
- (tùy chọn) `config_rw`: Read/Write cấu hình (PID, đảo chiều motor, offset IMU…)

Về write type trên Android, thực tế có sự khác nhau giữa “write request” và “write command (write without response)” và lập trình viên thường phải chọn writeType phù hợp. citeturn0search11  

**Đóng gói thông điệp (2 chế độ)**
- **Chế độ dễ debug (khuyên dùng giai đoạn 1)**: JSON một dòng, kết thúc `\n`.  
- **Chế độ tối ưu (giai đoạn 2)**: khung nhị phân có `seq`, `len`, `crc16` để giảm overhead và ổn định với BLE MTU nhỏ.

### Kiến trúc Wi‑Fi tùy chọn

ESP32 có Wi‑Fi tích hợp, phù hợp nếu bạn muốn điều khiển robot qua LAN hoặc muốn băng thông telemetry lớn hơn BLE. citeturn9view0  
Mô hình beginner-friendly nhất là: **điện thoại bật hotspot** → ESP32 kết nối → dùng **UDP** cho lệnh điều khiển (latency thấp) và **TCP/WebSocket** cho telemetry đáng tin cậy hơn. (Wi‑Fi phức tạp hơn ở phần “kết nối ban đầu”, nhưng rất mạnh khi mở rộng.)

### Giao thức ứng dụng Android ↔ MCU

Dưới đây là một bộ lệnh tối thiểu nhưng “đủ sống” để làm pet robot.

**Quy ước chung**
- Mỗi gói có `v` (version), `seq` (tăng dần), `type` (`cmd`/`telemetry`/`ack`/`err`).
- MCU trả `ack` cho các lệnh quan trọng (đổi mode, dừng khẩn, đổi PID…), còn lệnh vận tốc có thể không cần ack mọi gói để giảm tải.

Ví dụ (JSON line, để minh họa — cùng dùng được cho BLE/Wi‑Fi):

```json
{"v":1,"seq":1001,"type":"cmd","cmd":"move","v_mps":0.25,"w_rps":0.0,"ttl_ms":200}
{"v":1,"seq":1002,"type":"cmd","cmd":"rotate","w_rps":1.2,"ttl_ms":300}
{"v":1,"seq":1003,"type":"cmd","cmd":"servo","ch":0,"pos_deg":35,"speed":0.8}
{"v":1,"seq":1004,"type":"cmd","cmd":"led","mode":"rgb","r":20,"g":80,"b":255,"ttl_ms":5000}
{"v":1,"seq":1101,"type":"cmd","cmd":"sensor_subscribe","period_ms":50,"fields":["range_mm","bumper","imu"]}
```

**Giải thích lệnh cốt lõi**
- `move`: điều khiển theo vận tốc tuyến tính `v_mps` và vận tốc góc `w_rps` (phù hợp robot 2 bánh). MCU chuyển thành PWM trái/phải.  
- `rotate`: thực chất là `move` với `v_mps = 0`. Tách riêng để app dễ dùng.  
- `servo`: điều khiển kênh servo (0…15 nếu dùng PCA9685), vị trí theo độ.  
- `led`: hỗ trợ ít nhất 2 mode: đơn sắc (debug) và RGB/pattern (thể hiện “cảm xúc”).  
- `sensor_subscribe`: Android chọn tần số và danh sách telemetry cần nhận.

**Telemetry từ MCU về Android**
- Dạng notify định kỳ:

```json
{"v":1,"seq":9001,"type":"telemetry","t_ms":123456,"range_mm":640,"bumper":0,
 "imu":{"ax":0.02,"ay":-0.01,"az":0.99,"gx":0.3,"gy":-0.1,"gz":0.0},
 "bat_mv":7400,"mot":{"l":120,"r":118}}
```

Bạn có thể mở rộng thêm:
- `estop` (dừng khẩn)
- `calib_imu`, `trim_motor`, `set_pid`
- `behavior_mode` để Android nói robot đang ở “idle / follow / avoid / play”.

## Các phương án thân robot theo mức độ

Dưới đây là 3 cấu hình thân robot (đều bám cùng một kiến trúc: Android trên lưng + ESP32 điều khiển realtime). Khác nhau ở cơ khí, cảm biến và mức “biểu cảm”.

image_group{"layout":"carousel","aspect_ratio":"1:1","query":["2WD robot car chassis acrylic","pan tilt bracket SG90 servo","robot car chassis 4WD metal gear motor","robot pet ears tail servo mechanism"] ,"num_per_query":1}

### Prototype tối giản

**Mục tiêu**: chạy được, nhận lệnh từ Android, tránh vật cản cơ bản, ít linh kiện nhất.

- **Cơ khí**: chassis 2WD (2 motor + caster), tấm đế acrylic/nhôm mỏng, gá điện thoại dạng kẹp lò xo + pad cao su, đặt gần giữa trục bánh.  
- **Vi điều khiển**: ESP32 (DevKit). citeturn9view0  
- **Motor driver**: TB6612FNG (dễ mua, đủ dòng cho motor nhỏ). citeturn12view0  
- **Cảm biến**: 1 ultrasonic HC‑SR04 phía trước để “không đâm tường” (rẻ, dễ test); dải đo/độ chính xác phổ biến 2–400 cm / ~3 mm. citeturn3search12  
- **LED**: 1–2 LED báo trạng thái (kết nối BLE, pin yếu, mode).  
- **Nguồn**: ưu tiên 4×AA NiMH/Alkaline để tránh rủi ro Li‑ion; rail 3.3 V cho ESP32 bằng buck/LDO (tùy board).  
- **Giao tiếp**: BLE GATT service 2 characteristic (cmd/telemetry). citeturn0search3turn0search11  

Điểm “đủ dùng” cho pet robot: Android làm nhận diện khuôn mặt/giọng nói, MCU chỉ cần chạy/đứng + né đơn giản.

### Robot mức trung bình

**Mục tiêu**: “có tính cách” hơn: quay đầu nhìn, phản hồi chạm, chạy mượt, telemetry ổn định.

- **Cơ khí**: chassis 2WD lớn hơn hoặc 4WD (nếu cần lực kéo); có “cổ” pan‑tilt để điện thoại/cảm biến hướng về mục tiêu.  
- **Motor**: nâng lên N20 hoặc gear motor lực hơn; cân nhắc encoder nếu bạn muốn hành vi theo người mượt và dừng đúng vị trí.  
- **Motor driver**: DRV8833 (gọn) hoặc TB6612FNG; nếu dùng nguồn 2S, DRV8833 vẫn phù hợp dải 2.7–10.8 V. citeturn5search0turn12view0  
- **Servo**: 2 micro servo cho pan/tilt (SG90 là baseline: 4.8 V, 9 g, torque stall ~1.8 kg·cm). citeturn4search1  
- **Driver servo**: PCA9685 để xung servo ổn định và mở rộng tới 16 kênh; chạy I²C, 12‑bit PWM. citeturn1search3turn5search2  
- **Cảm biến**:
  - ToF VL53L0X ở phía trước (né vật thể chắc hơn ultrasonic), đo đến 2 m. citeturn7search0  
  - 2 bumper switch (trái/phải) để phản hồi “bị chạm”.  
  - IMU MPU‑6050 (I²C, 6 trục) để biết rung/nghiêng và làm hành vi. citeturn7search15turn7search5  
- **Nguồn**: 2S Li‑ion + buck 5 V (servo/LED) + buck/LDO 3.3 V (logic). Với Li‑ion cần bảo vệ và sạc đúng chuẩn CV/CC. citeturn8search4turn8search0  
- **Chống nhiễu**: tụ 0.1 µF ngang cực motor + dây ngắn/to để giảm reset do nhiễu. citeturn13search20  

### Robot thú cưng nâng cao

**Mục tiêu**: “đáng yêu” và tương tác cao: biểu cảm qua tai/đuôi, chuyển động mượt, cảm biến tốt hơn, sẵn sàng chạy lâu.

- **Cơ khí**: thân dạng “pet” (vỏ 3D print hoặc foam + khung), bên ngoài mềm nhưng bên trong có khung cứng bắt motor/gearbox; bố trí điện thoại như “đầu” hoặc “lưng” với giảm chấn nhẹ.  
- **Truyền động**: ưu tiên 2WD chất lượng (ít lỗi hơn) nhưng dùng motor + bánh tốt; hoặc tracked nếu bạn chấp nhận hao pin/ồn.  
- **Servo**: 6–12 DOF nhỏ (tai trái/phải, đuôi, “thở”, gật, nghiêng…) — bắt buộc dùng PCA9685 và rail 5–6 V đủ dòng; ground chung. citeturn1search3turn4search0turn5search2  
- **Cảm biến**:
  - ToF VL53L1X tầm xa hơn, đến 4 m và tốc độ đo đến 50 Hz (tốt cho né nhanh). citeturn3search10  
  - Cặp bumper + “touch strip” (tự thiết kế) để robot phản ứng “được vuốt”.  
  - IMU + (tùy chọn) encoder để robot “biết mình đang làm gì” thay vì chỉ chạy open-loop.  
- **Nguồn & thời lượng**:
  - 2S Li‑ion dung lượng cao hơn + buck hiệu suất tốt.
  - Tách riêng rail servo và rail cấp sạc điện thoại bằng lọc để tránh servo làm sụt áp khiến điện thoại ngắt sạc.
  - Áp dụng thực hành giảm nhiễu motor như tụ 0.1 µF và tối ưu dây nguồn. citeturn13search20turn8search4turn8search0  
- **Giao tiếp**:
  - BLE cho điều khiển gần và pairing nhanh theo GATT. citeturn0search3turn0search11  
  - Wi‑Fi như tùy chọn nếu muốn điều khiển từ xa qua mạng (ESP32 có sẵn Wi‑Fi). citeturn9view0  

## Kết luận lựa chọn chính

Với ràng buộc “Android mounted + AI chạy trên phone + MCU điều khiển motor/sensor + BLE/Wi‑Fi”, **ESP32** là lựa chọn tối ưu nhất về tổng thể vì tích hợp sẵn 2 chuẩn truyền thông, đủ PWM/I/O và hệ sinh thái lớn. citeturn9view0turn0search3  
Điều kiện đi kèm bắt buộc là tuân thủ miền **3.3 V logic** và thiết kế nguồn/giảm nhiễu cẩn thận để robot không reset khi motor/servo giật tải. citeturn10view2turn13search20turn4search0