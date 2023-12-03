package dronenet;

import geometry.AnalyticGeometry;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author fatihsenel
 * date: 01.11.23
 */
public class Trajectory {
    public Point2D start, end;
    public double speed;
    private List<Point2D> waypoints;
    public List<Point2D> inclusive;
    private Point2D currentWaypoint, currentLocation;
    private boolean nonStop = false;

    public Trajectory(Point2D start, Point2D end, double speed) {
        this(start, end, speed, false);
    }

    public Trajectory(Point2D start, Point2D end, double speed, boolean nonStop) {
        this.start = start;
        this.currentWaypoint = start;
        this.currentLocation = start;
        this.end = end;
        this.speed = speed;
        this.inclusive = new ArrayList<>();
        this.nonStop = nonStop;
        this.waypoints = new ArrayList<>();
    }

    public Trajectory(Trajectory t) {
        this.start = new Point2D.Double(t.start.getX(), t.start.getY());
        this.currentWaypoint = new Point2D.Double(t.currentWaypoint.getX(), t.currentWaypoint.getY());
        this.currentLocation = new Point2D.Double(t.currentLocation.getX(), t.currentLocation.getY());
        this.end = new Point2D.Double(t.end.getX(), t.end.getY());
        this.speed = t.speed;
        this.waypoints = new ArrayList<>(t.waypoints.size());
        for (Point2D waypoint : t.waypoints) {
            this.waypoints.add(new Point2D.Double(waypoint.getX(), waypoint.getY()));
        }
        this.inclusive = new ArrayList<>(t.inclusive.size());
        for (Point2D point : t.inclusive) {
            this.inclusive.add(new Point2D.Double(point.getX(), point.getY()));
        }
        this.nonStop = t.nonStop;
    }

    public void setWaypoints(List<Point2D> waypoints) {
        this.waypoints.addAll(waypoints);
        inclusive.add(start);
        inclusive.addAll(waypoints);
        inclusive.add(end);
    }

    public int getDuration() {
        if (start.equals(end)) return 0;
        Trajectory copy = new Trajectory(this);
        int i = 0;
        while (true) {
            Point2D nextLocation = copy.getNextLocation();
            i++;
            if (nextLocation.equals(copy.end)) break;

        }
        return i;

    }

    public Point2D getNextLocation() {
        if (currentLocation.equals(end)) return end;

        double distance = speed;

        if (nonStop || waypoints.isEmpty()) {
            currentLocation = AnalyticGeometry.getCoordinates(currentLocation, end, distance);
            return currentLocation;
        } else {
            Point2D nextWaypoint = null;
            for (int i = 0; i < inclusive.size() - 1; i++) {
                if (inclusive.get(i).equals(currentWaypoint)) {
                    nextWaypoint = inclusive.get(i + 1);
                    break;
                }
            }
            if (nextWaypoint == null) throw new RuntimeException("Next Waypoint is NULL");
            currentLocation = AnalyticGeometry.getCoordinates(currentLocation, nextWaypoint, distance);
            if (currentLocation.equals(nextWaypoint)) {
                currentWaypoint = nextWaypoint;
            }
            return currentLocation;

        }
    }

    public boolean hasWaypoints() {
        return !this.waypoints.isEmpty();
    }

    public boolean isNonStop() {
        return nonStop;
    }

    public void setNonStop(boolean nonStop) {
        this.nonStop = nonStop;
    }

    @Override
    public String toString() {
        return "Trajectory{" +
                "start=" + pointToStr(start) +
                ", end=" + pointToStr(end) +
                ", waypoints=" + waypoints.stream().map(this::pointToStr).collect(Collectors.toList()) +
                ", nonStop=" + nonStop +
                '}';
    }

    private String pointToStr(Point2D point) {
        return "[" + (int) (point.getX() / 100) + ", " + (int) (point.getY() / 100) + "]";
    }
}
