Parallel Programming, Billiards Problem  
Requirements:  
1. Create a graphical program that depicts a ball bouncing within a rectangular frame (similar to a billiard ball).  
2. Simultaneously generate 8 balls of different colors, each moving at a random angle. Ensure they do not collide with each other.  
3. Allow the balls to collide with one another. Gradually decrease their speed until they come to a complete stop after 1 minute.  
4. Assign a number to each ball.  
5. Include a hole in the center of the table; any ball that falls into the hole disappears.  
6. Improve the program to increase the number of balls to 200, with a smaller radius.  
7. Create 3 different maps, establishing a starting point and a destination, and predict which ball will fall into the hole first.  
Finally: Upload the code to GitHub for public access and introduce the game in English.  
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


