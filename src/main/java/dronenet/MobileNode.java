package dronenet;

import geometry.AnalyticGeometry;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author fatihsenel
 * date: 16.02.22
 */
public class MobileNode {
    int id;
    List<TemporalLocation> temporalLocations = new ArrayList<>();

    public MobileNode(int id) {
        this.id = id;
    }

    public static MobileNode createFromDoubleList(List<Double> doubles) {
        return null;
    }

    public int getId() {
        return id;
    }

    public List<TemporalLocation> getTemporalLocations() {
        return temporalLocations;
    }

    public void setTemporalLocations(List<TemporalLocation> temporalLocations) {
        this.temporalLocations = temporalLocations;
        Collections.sort(this.temporalLocations);
    }

    /**
     *
     * @param time in minutes
     * @return location at time
     */
    public TemporalLocation getTemporalLocationAtTime(double time) {
        int index = -1;
        for (int i = 0; i < temporalLocations.size() - 1; i++) {
            TemporalLocation s = temporalLocations.get(i);
            TemporalLocation e = temporalLocations.get(i + 1);
            if (time >= s.time && time <= e.time) {
                index = i;
                break;
            }
        }
        if (index == -1)
            return null;
        TemporalLocation start = temporalLocations.get(index);
        TemporalLocation end = temporalLocations.get(index + 1);

        double euclideanDistance = AnalyticGeometry.euclideanDistance(start.x, start.y, end.x, end.y);
        double duration = end.time - start.time;
        double timeMoved = time - start.time;
        double distanceMoved = (timeMoved * euclideanDistance) / duration;
        Point2D coordinates = AnalyticGeometry.getCoordinates(start.x, start.y, end.x, end.y, distanceMoved);

        return new TemporalLocation(time, coordinates.getX(), coordinates.getY());


    }
}
