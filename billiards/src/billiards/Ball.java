package billiards;

import java.awt.*;
import java.io.Serializable;

public class Ball implements Serializable {
    private final int id;
    private volatile double x, y;
    private volatile double vx, vy;
    private volatile int radius;
    private volatile boolean alive = true;
    private final Color color;

    // color pool
    public static final Color[] COLOR_POOL = new Color[] {
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.ORANGE,
            new Color(128,0,128), Color.CYAN, Color.PINK, new Color(139,69,19),
            new Color(0,128,128), new Color(255,105,180), new Color(173,216,230),
            new Color(34,139,34), new Color(160,32,240), new Color(210,105,30),
            new Color(75,0,130), new Color(240,230,140), new Color(47,79,79),
            new Color(255,69,0), new Color(0,0,128)
    };

    public Ball(int id, double x, double y, double vx, double vy, int radius, Color color) {
        this.id = id; this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.radius = radius; this.color = color;
    }

    public int getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public int getRadius() { return radius; }
    public Color getColor() { return color; }
    public boolean isAlive() { return alive; }

    public synchronized void setPosition(double nx, double ny) { this.x = nx; this.y = ny; }
    public synchronized void setVelocity(double nvx, double nvy) { this.vx = nvx; this.vy = nvy; }
    public synchronized void setAlive(boolean a) { this.alive = a; }
}
