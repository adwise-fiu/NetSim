package dronenet;

import geometry.AnalyticGeometry;

import java.awt.geom.Point2D;

/**
 * @author fatihsenel
 * date: 24.05.22
 */
public class Velocity {
    public Point2D start, end;
    // meter/minute
    public double speed;

    public Velocity(Point2D start, Point2D end, int duration) {
        this.start = start;
        this.end = end;

        double d = AnalyticGeometry.euclideanDistance(start, end);

        speed = d / duration;
    }

    public Velocity(Point2D start, Point2D end, double speed) {
        this.start = start;
        this.end = end;
        this.speed = speed;
    }
}
