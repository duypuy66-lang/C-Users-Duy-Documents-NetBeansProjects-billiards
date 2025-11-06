Lập trình song song, bài toán bi-a
Yêu cầu:
1. lập trình đồ họa, mô tả 1 quả bóng nẩy trong khung hình chữ nhật. (giống quả bi-a).
2. nhân bán lên cùng lúc 8 quá với 8 mầu khác nhau, cùng lúc chuyển động với góc ngẫu nhiên. Chỉ nấy tường không va chạm vào nhau.
3. Cho phép quá bóng va chạm vào nhau. Cho tốc độ của bóng giảm dần và dừng lại hẳn sau 1 phút.
4. Điền số vào cho từng quả bong
5. cho 1 cái lỗ ở giữa bàn, quả nào rơi xuống lỗ quả đó biến mất.
6. Cải tiến theo hướng tăng số bóng lện 200 quả, cho bán kính nhỏ hơn.
7. Xây dựng 3 map khác nhau. tạo điểm xuất phát và điểm đích, dự đoán quả nào rơi xuống lỗ đầu tiên.
Cuối cùng: Đầy code lên github, để public, giới thiệu về game bằng tiềng Anh.
# Parallel Billiards Simulation (Java, NetBeans)

This project is a Java graphical simulation of billiard balls, demonstrating **parallel programming** and **basic 2D graphics** using Swing.

##  Features

1. A single bouncing ball inside a rectangular frame.
2. 8 balls appear simultaneously, each with a unique color and random angle of motion.
3. Balls bounce off the walls and can collide with each other.
4. Each ball gradually slows down and stops after one minute.
5. Every ball is numbered.
6. There is a hole in the center of the table — balls that fall into it disappear.
7. Option to increase up to 200 smaller balls.
8. Three different maps with start and goal positions, and prediction of which ball will fall first.

Parallelism

Each ball runs in a separate thread, updating its position and handling wall collisions in parallel.  
The main thread handles rendering (`repaint()`) and inter-ball collisions.
How to Run

1. Open the project in **NetBeans**.
2. Build and Run (`Shift + F6`).
3. Enjoy watching the simulation!

Controls (if implemented)

- `Space` – Pause/Resume
- `R` – Reset balls
- `M` – Change map
- `+ / -` – Increase/Decrease number of balls

Technologies

- Java 8+
- Swing / AWT (graphics)
- Threads for parallel simulation

  
  -Link xem sản phẩm :
https://youtu.be/ICgUErIbzOk


