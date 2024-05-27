package dronenet;


import java.awt.geom.Point2D;
import java.util.List;

/**
 * @author fatihsenel
 * date: 06.01.24
 */
public class Path {
    int id;
    List<Point2D> waypoints;

    public Path(int id, List<Point2D> waypoints) {
        this.id = id;
        this.waypoints = waypoints;
    }

//    public int time() {
//        int sum = 0;
//        Point2D prev = drone.getPoint2D();
//        for (Point2D waypoint : waypoints) {
//            double distance = AnalyticGeometry.euclideanDistance(prev, waypoint);
//            sum += Math.ceil(distance / maxDroneSpeed);
//            prev = waypoint;
//        }
//        return sum;
//    }


}
