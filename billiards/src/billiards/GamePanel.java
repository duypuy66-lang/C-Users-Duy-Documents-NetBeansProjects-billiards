package billiards;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class GamePanel extends JPanel {
    // Panel size
    private final int WIDTH;
    private final int HEIGHT;

    // Simulation parameters
    private int ballCount = 8;
    private final int DEFAULT_RADIUS = 14;
    private final int MIN_RADIUS = 4;
    private int radius = DEFAULT_RADIUS;
    private final double FRICTION = 0.995; // per tick multiplier
    private final long SIM_SECONDS = 60L;

    // Maps
    private List<MapDef> maps;
    private int currentMap = 0;

    // Objects
    private List<Ball> balls = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean running = false;
    private volatile long simEndTime = 0L; // epoch millis when motion must stop

    // Threads
    private ExecutorService exec;
    private ScheduledExecutorService scheduler;

    // Rendering
    private final int TARGET_FPS = 60;
    private javax.swing.Timer repaintTimer;
    // UI state
    private boolean paused = false;

    public GamePanel(int w, int h) {
        this.WIDTH = w;
        this.HEIGHT = h;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(30,120,30));

        initMaps();
        setFocusable(true);
        requestFocusInWindow();
        setupKeyBindings();
    }

    private void initMaps() {
        maps = new ArrayList<>();
        maps.add(new MapDef("Classic center hole",
                40,
                new Point(WIDTH/2, HEIGHT/2),
                30,
                new Rectangle(60, 60, 200, HEIGHT-120),
                new Rectangle(WIDTH-260, 60, 200, HEIGHT-120)
        ));
        maps.add(new MapDef("Left-top hole",
                20,
                new Point(WIDTH/4, HEIGHT/4),
                28,
                new Rectangle(60, HEIGHT-200, 200, 140),
                new Rectangle(WIDTH-260, 60, 200, HEIGHT-120)
        ));
        maps.add(new MapDef("Bottom hole with corridor",
                30,
                new Point(WIDTH/2, HEIGHT-80),
                26,
                new Rectangle(60, 60, 200, 120),
                new Rectangle(WIDTH-260, HEIGHT-180, 200, 120)
        ));
    }

    public void start() {
        running = true;
        simEndTime = System.currentTimeMillis() + SIM_SECONDS * 1000L;
        exec = Executors.newCachedThreadPool();
        scheduler = Executors.newScheduledThreadPool(1);

        recreateBalls();

        // start worker threads (one worker per ball)
        for (Ball b : balls) {
            exec.submit(new BallWorker(b));
        }

        // main collision and hole check scheduler at ~60Hz
        scheduler.scheduleAtFixedRate(() -> {
            if (!paused) {
                resolveCollisions();
                checkHoles();
                // stop velocities after SIM_SECONDS
                if (System.currentTimeMillis() > simEndTime) {
                    synchronized (balls) {
                        for (Ball b : balls) {
                            b.setVelocity(0,0);
                        }
                    }
                }
            }
        }, 0, 1000 / TARGET_FPS, TimeUnit.MILLISECONDS);

        // repaint swing timer
        repaintTimer = new javax.swing.Timer(1000/ TARGET_FPS, e -> repaint());
        repaintTimer.start();
    }

    public void stop() {
        running = false;
        if (exec != null) exec.shutdownNow();
        if (scheduler != null) scheduler.shutdownNow();
        if (repaintTimer != null) repaintTimer.stop();
    }

    private void recreateBalls() {
        // adjust radius when many balls
        if (ballCount > 50) {
            radius = Math.max(MIN_RADIUS, (int)(DEFAULT_RADIUS * (8.0 / Math.sqrt(ballCount))));
        } else {
            radius = DEFAULT_RADIUS;
        }
        balls.clear();
        MapDef m = maps.get(currentMap);
        Random rnd = new Random();
        Rectangle s = m.getStartZone();
        int attemptsPerBall = 1000;
        for (int i = 0; i < ballCount; i++) {
            boolean placed = false;
            for (int att = 0; att < attemptsPerBall && !placed; att++) {
                double x = s.x + radius + rnd.nextDouble() * Math.max(0, s.width - 2*radius);
                double y = s.y + radius + rnd.nextDouble() * Math.max(0, s.height - 2*radius);
                boolean ok = true;
                synchronized (balls) {
                    for (Ball other : balls) {
                        double dx = other.getX() - x;
                        double dy = other.getY() - y;
                        if (Math.hypot(dx, dy) < other.getRadius() + radius + 2) {
                            ok = false; break;
                        }
                    }
                }
                if (ok) {
                    double[] v = randAngleVelocity(150, 260);
                    Color color = Ball.COLOR_POOL[i % Ball.COLOR_POOL.length];
                    Ball b = new Ball(i+1, x, y, v[0], v[1], radius, color);
                    balls.add(b);
                    placed = true;
                }
            }
            if (!placed) { // fallback grid placement
                int col = i % Math.max(1, (s.width / (2*radius+2)));
                int row = i / Math.max(1, (s.width / (2*radius+2)));
                double x = s.x + radius + col * (2*radius + 2);
                double y = s.y + radius + row * (2*radius + 2);
                double[] v = randAngleVelocity(150, 260);
                Color color = Ball.COLOR_POOL[i % Ball.COLOR_POOL.length];
                Ball b = new Ball(i+1, x, y, v[0], v[1], radius, color);
                balls.add(b);
            }
        }
    }

    private double[] randAngleVelocity(double min, double max) {
        double angle = Math.random() * 2*Math.PI;
        double speed = min + Math.random() * (max - min);
        return new double[]{Math.cos(angle)*speed, Math.sin(angle)*speed};
    }

    private void resolveCollisions() {
        // basic pairwise elastic collisions with slight damping
        synchronized (balls) {
            for (int i = 0; i < balls.size(); i++) {
                Ball a = balls.get(i);
                if (!a.isAlive()) continue;
                for (int j = i+1; j < balls.size(); j++) {
                    Ball b = balls.get(j);
                    if (!b.isAlive()) continue;
                    // lock ordering by id to avoid deadlock if extending to per-ball locks
                    double dx = b.getX() - a.getX();
                    double dy = b.getY() - a.getY();
                    double dist = Math.hypot(dx, dy);
                    double minDist = a.getRadius() + b.getRadius();
                    if (dist == 0) {
                        dist = 0.0001;
                        dx = 0.0001;
                    }
                    if (dist < minDist) {
                        // push apart equally
                        double overlap = 0.5 * (minDist - dist + 0.0001);
                        double nx = dx / dist;
                        double ny = dy / dist;
                        a.setPosition(a.getX() - nx*overlap, a.getY() - ny*overlap);
                        b.setPosition(b.getX() + nx*overlap, b.getY() + ny*overlap);
                        // velocities along normal
                        double va_n = a.getVx()*nx + a.getVy()*ny;
                        double vb_n = b.getVx()*nx + b.getVy()*ny;
                        double restitution = 0.95;
                        double new_va_n = vb_n * restitution;
                        double new_vb_n = va_n * restitution;
                        a.setVelocity(a.getVx() + (new_va_n - va_n)*nx, a.getVy() + (new_va_n - va_n)*ny);
                        b.setVelocity(b.getVx() + (new_vb_n - vb_n)*nx, b.getVy() + (new_vb_n - vb_n)*ny);
                    }
                }
            }
        }
    }

    private void checkHoles() {
        MapDef m = maps.get(currentMap);
        Point hole = m.getHolePos();
        int hr = m.getHoleRadius();
        List<Ball> fallen = new ArrayList<>();
        synchronized (balls) {
            for (Ball b : balls) {
                if (!b.isAlive()) continue;
                double dx = b.getX() - hole.x;
                double dy = b.getY() - hole.y;
                if (Math.hypot(dx, dy) <= hr) {
                    b.setAlive(false);
                    fallen.add(b);
                    System.out.println("Ball #" + b.getId() + " fell into hole.");
                }
            }
        }
        // optionally do something with fallen list
    }

    private Rectangle boundary() {
        MapDef m = maps.get(currentMap);
        int pad = m.getPadding();
        return new Rectangle(pad, pad, WIDTH - 2*pad, HEIGHT - 2*pad);
    }

    // Swing painting
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // draw table background
        Graphics2D g2 = (Graphics2D) g.create();
        // draw table area
        Rectangle bound = boundary();
        g2.setColor(new Color(40,100,40));
        g2.fillRect(bound.x, bound.y, bound.width, bound.height);
        // border
        g2.setColor(new Color(80,50,20));
        g2.setStroke(new BasicStroke(6));
        g2.drawRect(bound.x-10, bound.y-10, bound.width+20, bound.height+20);

        // map elements
        MapDef m = maps.get(currentMap);
        // draw hole
        g2.setColor(new Color(20,20,20));
        g2.fillOval(m.getHolePos().x - m.getHoleRadius(), m.getHolePos().y - m.getHoleRadius(),
                m.getHoleRadius()*2, m.getHoleRadius()*2);
        // start/goal zones
        g2.setColor(new Color(50,50,80));
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(m.getStartZone().x, m.getStartZone().y, m.getStartZone().width, m.getStartZone().height);
        g2.setColor(new Color(80,50,50));
        g2.drawRect(m.getGoalZone().x, m.getGoalZone().y, m.getGoalZone().width, m.getGoalZone().height);

        // draw balls
        synchronized (balls) {
            for (Ball b : balls) {
                if (!b.isAlive()) continue;
                g2.setColor(b.getColor());
                int x = (int)(b.getX() - b.getRadius());
                int y = (int)(b.getY() - b.getRadius());
                int d = b.getRadius()*2;
                g2.fillOval(x, y, d, d);
                // number
                g2.setColor(Color.BLACK);
                String s = String.valueOf(b.getId());
                FontMetrics fm = g2.getFontMetrics();
                int sw = fm.stringWidth(s);
                int sh = fm.getAscent();
                g2.drawString(s, (int)b.getX() - sw/2, (int)b.getY() + sh/2 - 2);
            }
        }

        // HUD text
        g2.setColor(Color.WHITE);
        g2.drawString("Map: " + m.getName() + " | Balls: " + ballCount + " | Radius: " + radius +
                " | Time left: " + Math.max(0, (int)((simEndTime - System.currentTimeMillis())/1000)) + "s", 10, 18);

        g2.dispose();
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,0), "togglePause");
        am.put("togglePause", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                paused = !paused;
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R,0), "reset");
        am.put("reset", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                simEndTime = System.currentTimeMillis() + SIM_SECONDS*1000L;
                recreateBalls();
                // restart threads cleanly
                if (exec != null) exec.shutdownNow();
                exec = Executors.newCachedThreadPool();
                for (Ball b : balls) exec.submit(new BallWorker(b));
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_M,0), "nextMap");
        am.put("nextMap", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                currentMap = (currentMap + 1) % maps.size();
                simEndTime = System.currentTimeMillis() + SIM_SECONDS*1000L;
                recreateBalls();
                if (exec != null) exec.shutdownNow();
                exec = Executors.newCachedThreadPool();
                for (Ball b : balls) exec.submit(new BallWorker(b));
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_P,0), "predict");
        am.put("predict", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                // run headless prediction in background thread
                new Thread(() -> {
                    Predictor pred = new Predictor();
                    int[] ids = new int[1];
                    double[] t = new double[1];
                    // deep copy required
                    synchronized (balls) {
                        Predictor.Result r = pred.predictFirstToHole(balls, maps.get(currentMap), 10.0);
                        if (r != null && r.ballId >= 0) {
                            System.out.println("Prediction: ball #" + r.ballId + " at t=" + String.format("%.2f", r.timeS) + "s");
                            // Show a temporary message on HUD? Here we just print.
                            JOptionPane.showMessageDialog(GamePanel.this,
                                    "Prediction: ball #" + r.ballId + " first to fall at t=" + String.format("%.2f", r.timeS) + "s");
                        } else {
                            JOptionPane.showMessageDialog(GamePanel.this, "Prediction: no ball falls within limit");
                        }
                    }
                }).start();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS,0), "moreBalls");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,0), "moreBalls");
        am.put("moreBalls", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (ballCount < 200) ballCount = Math.min(200, ballCount + 8);
                recreateBalls();
                if (exec != null) exec.shutdownNow();
                exec = Executors.newCachedThreadPool();
                for (Ball b : balls) exec.submit(new BallWorker(b));
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,0), "lessBalls");
        am.put("lessBalls", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (ballCount > 1) ballCount = Math.max(1, ballCount - 8);
                recreateBalls();
                if (exec != null) exec.shutdownNow();
                exec = Executors.newCachedThreadPool();
                for (Ball b : balls) exec.submit(new BallWorker(b));
            }
        });

        // Exit
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0), "exit");
        am.put("exit", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                stop();
                SwingUtilities.getWindowAncestor(GamePanel.this).dispose();
            }
        });
    }

    // BallWorker inner class: updates position independently
    private class BallWorker implements Runnable {
        private final Ball ball;
        private final Random rnd = new Random();
        public BallWorker(Ball b) { this.ball = b; }
        @Override
        public void run() {
            Rectangle bnd = boundary();
            long prev = System.nanoTime();
            while (running) {
                if (paused) {
                    try { Thread.sleep(20); } catch (InterruptedException ignored) {}
                    prev = System.nanoTime();
                    continue;
                }
                if (!ball.isAlive()) {
                    try { Thread.sleep(20); } catch (InterruptedException ignored) {}
                    prev = System.nanoTime();
                    continue;
                }
                long now = System.nanoTime();
                double dt = (now - prev) / 1e9;
                prev = now;
                // integrate
                double nx = ball.getX() + ball.getVx()*dt;
                double ny = ball.getY() + ball.getVy()*dt;
                double nvx = ball.getVx() * Math.pow(FRICTION, dt*60);
                double nvy = ball.getVy() * Math.pow(FRICTION, dt*60);

                // bounce on walls
                if (nx - ball.getRadius() < bnd.x) {
                    nx = bnd.x + ball.getRadius();
                    nvx = Math.abs(nvx);
                }
                if (nx + ball.getRadius() > bnd.x + bnd.width) {
                    nx = bnd.x + bnd.width - ball.getRadius();
                    nvx = -Math.abs(nvx);
                }
                if (ny - ball.getRadius() < bnd.y) {
                    ny = bnd.y + ball.getRadius();
                    nvy = Math.abs(nvy);
                }
                if (ny + ball.getRadius() > bnd.y + bnd.height) {
                    ny = bnd.y + bnd.height - ball.getRadius();
                    nvy = -Math.abs(nvy);
                }
                ball.setPosition(nx, ny);
                ball.setVelocity(nvx, nvy);

                // if global time over, zero velocity
                if (System.currentTimeMillis() > simEndTime) {
                    ball.setVelocity(0,0);
                }

                try {
                    Thread.sleep(2);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
    }

    // shut down on finalize
    @Override
    public void addNotify() {
        super.addNotify();
        // nothing
    }
}
