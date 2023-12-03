package test;

import dronenet.Trajectory;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class TrajectoryTest {
    public static void main(String[] args) {
        TrajectoryTest t = new TrajectoryTest();
        t.testNextLocationWithWaypoints();

        System.out.println("**********");
        t.testNextLocationWithNonStop();
    }

    public void testNextLocationWithNonStop() {
        Point2D start = new Point2D.Double(0, 0);
        Point2D waypoint1 = new Point2D.Double(5, 5);
        Point2D waypoint2 = new Point2D.Double(10, 5);
        Point2D end = new Point2D.Double(15, 10);
        List<Point2D> waypoints = new ArrayList<>();
        waypoints.add(waypoint1);
        waypoints.add(waypoint2);
        double speed = 1.0;
        int i = 1;

        Trajectory trajectory = new Trajectory(start, end, speed, true);
        trajectory.setWaypoints(waypoints);
        System.out.println(trajectory.getDuration());
        while (true) {
            Point2D nextLocation = trajectory.getNextLocation();
            System.out.println(i++ + "-" + nextLocation);
            if (nextLocation.equals(trajectory.end)) break;

        }

    }

    public void testNextLocationWithWaypoints() {
        Point2D start = new Point2D.Double(0, 0);
        Point2D waypoint1 = new Point2D.Double(5, 5);
        Point2D waypoint2 = new Point2D.Double(10, 5);
        Point2D end = new Point2D.Double(15, 10);
        List<Point2D> waypoints = new ArrayList<>();
        waypoints.add(waypoint1);
        waypoints.add(waypoint2);
        double speed = 1.0;
        int i = 1;
        Trajectory trajectory = new Trajectory(start, end, speed);
        trajectory.setWaypoints(waypoints);
        System.out.println(trajectory.getDuration());
        while (true) {
            Point2D nextLocation = trajectory.getNextLocation();
            System.out.println(i++ + "-" + nextLocation);

            if (nextLocation.equals(trajectory.end)) break;

        }
    }
}
