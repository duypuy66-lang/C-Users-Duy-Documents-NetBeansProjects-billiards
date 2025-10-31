# Parallel Billiards — Java (Swing) Simulation

This is a Java (Swing) billiards-like simulation demonstrating parallel programming concepts.

**Features**
- Graphical simulation of numbered balls bouncing in a rectangular table.
- Start with 8 balls of different colors, launched simultaneously at random angles.
- Balls collide with each other and with table boundaries.
- Velocity decays over time (friction); all motion stops after 60 seconds.
- Each ball has a number drawn on it.
- A hole exists on the table; when a ball falls into the hole it disappears.
- Option to scale up to 200 balls with smaller radius for stress testing.
- Three predefined maps (layouts) with different hole positions, start and goal zones.
- Headless prediction routine to estimate which ball will fall into the hole first.
- Demonstrates parallelism by running per-ball position updates in separate threads and performing collision resolution centrally.

**How to run (NetBeans)**
1. Create a new Java Application project in NetBeans.
2. Create package `billiards` and add the provided `.java` files under `src/billiards`.
3. Set `Main.java` as the main class and run.
4. Install JDK 8+.

**Controls**
- `SPACE` — Pause / Resume
- `R` — Reset/Respawn balls
- `M` — Switch to next map
- `P` — Run prediction (popup)
- `+` / `=` — Increase balls (by 8) up to 200
- `-` — Decrease balls (by 8)
- `ESC` — Exit

**To publish on GitHub**
1. Create new repo on GitHub (public).
2. Locally:
```bash
git init
git add .
git commit -m "Add Parallel Billiards Java Swing simulation"
git branch -M main
git remote add origin https://github.com/<your-username>/<repo>.git
git push -u origin main

