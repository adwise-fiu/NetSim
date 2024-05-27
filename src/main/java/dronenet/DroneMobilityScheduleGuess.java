package dronenet;

import utils.NetworkUtils;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * @author fatihsenel
 * date: 07.01.24
 */
public class DroneMobilityScheduleGuess {

    List<Drone> source, dest;
    TopologyFeatures topologyFeatures;

    int transmissionRange;

    public DroneMobilityScheduleGuess(List<Drone> source, List<Drone> dest, int transmissionRange) {
        this.source = clone(source, transmissionRange);
        this.dest = clone(dest, transmissionRange);

        this.transmissionRange = transmissionRange;
        topologyFeatures = new TopologyFeatures(this.source, this.dest, transmissionRange);
    }

    public List<Path> guessASchedule() {
        List<Integer> notMoving = new ArrayList<>();
        List<Integer> moving = new ArrayList<>();
        List<Integer> moved = new ArrayList<>();
        List<String> movingFeature = new ArrayList<>();
        for (int j = 0; j < source.size(); j++) {
            Drone drone = source.get(j);
            Drone match = dest.get(j);
            if (drone.getPoint2D().equals(match.getPoint2D())) {
                notMoving.add(j);
            } else {
                moving.add(j);
            }
        }

        List<Path> scheduledPaths = new ArrayList<>();
        while (!moving.isEmpty()) {
            Map<Integer, List<Point2D>> safeDroneMap = new HashMap<>();
            int toBeMoved = -1;
            for (int id : moving) {
                List<Point2D> track = topologyFeatures.canMoveConnected(id);
                boolean canMoveConnected = !track.isEmpty();
                if (canMoveConnected) {
                    safeDroneMap.put(id, track);
                }
            }
            List<Point2D> track;
            if (!safeDroneMap.isEmpty()) {
                ArrayList<Integer> keys = new ArrayList<>(safeDroneMap.keySet());
                toBeMoved = keys.get(0);
                track = safeDroneMap.get(toBeMoved);
            } else {
                // todo if no drones can move without breaking the connectivity the pick randomly
                //  but we need a sophisticated algorithm here
                Random random = new Random();
                int randomIndex = random.nextInt(moving.size());
                toBeMoved = moving.get(randomIndex);
                track = new ArrayList<>();
                track.add(source.get(toBeMoved).getPoint2D());
                track.add(dest.get(toBeMoved).getPoint2D());
            }
            for (int j = 0; j < moving.size(); j++) {
                if (moving.get(j) == toBeMoved) {
                    moving.remove(j);
                    break;
                }
            }
            scheduledPaths.add(new Path(toBeMoved,track ));
            moved.add(toBeMoved);
            Drone drone = source.get(toBeMoved);
            Drone match = dest.get(toBeMoved);
            drone.setLocation(match.getPoint2D());
            NetworkUtils.calculateActorNeighborhoods(source, transmissionRange);
//            System.out.print(toBeMoved + "-");

        }
        return scheduledPaths;
    }

    public static List<Drone> clone(List<Drone> list, int transmissionRange) {
        List<Drone> cloned = new ArrayList<>();
        for (Drone drone : list) {
            Drone nd = new Drone(drone.getID());
            nd.setLocation(drone.getPoint2D());
            cloned.add(nd);
        }
        NetworkUtils.calculateActorNeighborhoods(cloned, transmissionRange);
        return cloned;
    }
}
