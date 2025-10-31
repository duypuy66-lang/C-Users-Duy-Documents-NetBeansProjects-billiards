package billiards;

import java.awt.*;

public class MapDef {
    private final String name;
    private final int padding;
    private final Point holePos;
    private final int holeRadius;
    private final Rectangle startZone;
    private final Rectangle goalZone;

    public MapDef(String name, int padding, Point holePos, int holeRadius, Rectangle startZone, Rectangle goalZone) {
        this.name = name; this.padding = padding; this.holePos = holePos; this.holeRadius = holeRadius;
        this.startZone = startZone; this.goalZone = goalZone;
    }

    public String getName() { return name; }
    public int getPadding() { return padding; }
    public Point getHolePos() { return holePos; }
    public int getHoleRadius() { return holeRadius; }
    public Rectangle getStartZone() { return startZone; }
    public Rectangle getGoalZone() { return goalZone; }
}
