package billiards;

import java.util.ArrayList;
import java.util.List;

public class Predictor {

    public static class Result {
        public final int ballId;
        public final double timeS;
        public Result(int id, double t) { this.ballId = id; this.timeS = t; }
    }

    // Deep-simulate quickly to predict first ball to fall (headless)
    public Result predictFirstToHole(List<Ball> balls, MapDef map, double simLimitSec) {
        // copy balls
        List<SimBall> sim = new ArrayList<>();
        for (Ball b : balls) {
            SimBall sb = new SimBall(b.getId(), b.getX(), b.getY(), b.getVx(), b.getVy(), b.getRadius());
            sim.add(sb);
        }
        double dt = 0.005; // small step
        int steps = (int) Math.ceil(simLimitSec / dt);
        int left = map.getPadding();
        int top = map.getPadding();
        int right = left + (1000 - 2*map.getPadding()); // note: Main panel width assumed 1000
        int bottom = top + (600 - 2*map.getPadding());
        double hx = map.getHolePos().x;
        double hy = map.getHolePos().y;
        double hr = map.getHoleRadius();
        for (int s = 0; s < steps; s++) {
            for (SimBall sb : sim) {
                if (!sb.alive) continue;
                sb.x += sb.vx * dt;
                sb.y += sb.vy * dt;
                sb.vx *= Math.pow(0.995, dt*60);
                sb.vy *= Math.pow(0.995, dt*60);
                if (sb.x - sb.radius < left) { sb.x = left + sb.radius; sb.vx = Math.abs(sb.vx); }
                if (sb.x + sb.radius > right) { sb.x = right - sb.radius; sb.vx = -Math.abs(sb.vx); }
                if (sb.y - sb.radius < top) { sb.y = top + sb.radius; sb.vy = Math.abs(sb.vy); }
                if (sb.y + sb.radius > bottom) { sb.y = bottom - sb.radius; sb.vy = -Math.abs(sb.vy); }
            }
            // simple collisions
            for (int i = 0; i < sim.size(); i++) {
                SimBall a = sim.get(i);
                if (!a.alive) continue;
                for (int j = i+1; j < sim.size(); j++) {
                    SimBall b = sim.get(j);
                    if (!b.alive) continue;
                    double dx = b.x - a.x;
                    double dy = b.y - a.y;
                    double dist = Math.hypot(dx,dy);
                    double min = a.radius + b.radius;
                    if (dist == 0) dist = 0.0001;
                    if (dist < min) {
                        double overlap = 0.5*(min - dist + 0.0001);
                        double nx = dx / dist;
                        double ny = dy / dist;
                        a.x -= nx*overlap; a.y -= ny*overlap;
                        b.x += nx*overlap; b.y += ny*overlap;
                        double va_n = a.vx*nx + a.vy*ny;
                        double vb_n = b.vx*nx + b.vy*ny;
                        double r = 0.95;
                        double new_va = vb_n*r;
                        double new_vb = va_n*r;
                        a.vx += (new_va - va_n)*nx; a.vy += (new_va - va_n)*ny;
                        b.vx += (new_vb - vb_n)*nx; b.vy += (new_vb - vb_n)*ny;
                    }
                }
            }
            // check holes
            double t = s*dt;
            for (SimBall sb : sim) {
                if (!sb.alive) continue;
                double dx = sb.x - hx, dy = sb.y - hy;
                if (Math.hypot(dx, dy) <= hr) {
                    return new Result(sb.id, t);
                }
            }
            // early break if all stopped
            boolean allStopped = true;
            for (SimBall sb : sim) {
                if (sb.alive && Math.hypot(sb.vx, sb.vy) > 0.1) { allStopped = false; break; }
            }
            if (allStopped) break;
        }
        return null;
    }

    private static class SimBall {
        int id; double x,y,vx,vy; int radius; boolean alive=true;
        SimBall(int id,double x,double y,double vx,double vy,int r){this.id=id;this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.radius=r;}
    }
}
