package dronenet;

import java.awt.geom.Point2D;

/**
 * @author fatihsenel
 * date: 11.05.23
 */
public class Track {
    double x1, y1, x2, y2, distance;
    int iteration;
    Drone drone;

    public Track(double x1, double y1, double x2, double y2, double distance, int iteration) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.distance = distance;
        this.iteration = iteration;
    }

    public Point2D getFrom() {
        return new Point2D.Double(x1, y1);
    }

    public Point2D getTo() {
        return new Point2D.Double(x2, y2);
    }

    public double getX1() {
        return x1;
    }

    public void setX1(double x1) {
        this.x1 = x1;
    }

    public double getY1() {
        return y1;
    }

    public void setY1(double y1) {
        this.y1 = y1;
    }

    public double getX2() {
        return x2;
    }

    public void setX2(double x2) {
        this.x2 = x2;
    }

    public double getY2() {
        return y2;
    }

    public void setY2(double y2) {
        this.y2 = y2;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public Drone getDrone() {
        return drone;
    }

    public void setDrone(Drone drone) {
        this.drone = drone;
    }

    @Override
    public String toString() {
        String format = "Drone [%s] moved from (%.1f,%.1f) to (%.1f,%.1f) distance=%.2f at %d";
        String id = drone == null ? "null" : String.valueOf(drone.getID());
        return String.format(format, id,
                x1, y1,
                x2, y2, distance, iteration);

    }
}
