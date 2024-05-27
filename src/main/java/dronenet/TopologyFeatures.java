package dronenet;


import geometry.AnalyticGeometry;
import network.Gateway;
import utils.NetworkUtils;

import java.awt.geom.Point2D;
import java.util.*;

import static network.Constants.maxDroneSpeed;

/**
 * @author fatihsenel
 * date: 04.12.23
 */
public class TopologyFeatures {
    List<Drone> sources;
    List<Drone> destinations;

    Drone[][] macthings;
    int transmissionRange;
    int[] nodeDegrees;

    public TopologyFeatures(List<Drone> sources, List<Drone> destinations, int transmissionRange) {
        macthings = new Drone[sources.size()][2];
        this.sources = sources;
        this.destinations = destinations;
        nodeDegrees = new int[sources.size()];
        for (int i = 0; i < sources.size(); i++) {
            macthings[i][0] = sources.get(i);
            macthings[i][1] = destinations.get(i);
            nodeDegrees[i] = sources.get(i).getNeighborList().size();
        }
        this.transmissionRange = transmissionRange;

    }

    /**
     * Return true if target is in the intersection of transmission range of drone[id]'s neighbors
     *
     * @param id of the
     * @return
     */
    public boolean isInSafeRegion(int id) {
        Drone drone = macthings[id][0];
        Point2D to = macthings[id][1].getPoint2D();

        ArrayList<Gateway> neighbors = drone.getNeighborList();

        for (Gateway d : neighbors) {
            Point2D from = d.getPoint2D();
            if (AnalyticGeometry.EstimatedDistance(from, to) > transmissionRange) return false;
        }
        return true;
    }

    public List<Point2D> canMoveConnected(int id) {
        ArrayList<Gateway> allDrones = new ArrayList<>();
        // All drones except the moving
        ArrayList<Gateway> otherDrones = new ArrayList<>();
        Drone moving = null;
        for (Drone drone : sources) {
            Drone nd = new Drone(drone.getID());
            nd.setLocation(drone.getPoint2D());
            if (nd.getID() == id) moving = nd;
            allDrones.add(nd);

            Drone nd2 = new Drone(drone.getID());
            nd2.setLocation(drone.getPoint2D());
            if (nd2.getID() != id)
                otherDrones.add(nd2);
        }
        NetworkUtils.calculateActorNeighborhoods(allDrones, transmissionRange);
        NetworkUtils.calculateActorNeighborhoods(otherDrones, transmissionRange);
        ArrayList<ArrayList<Gateway>> partitions = NetworkUtils.DephtFirstSearch(otherDrones);
        HashMap<Integer, List<Drone>> partitionMap = new HashMap<>();

        for (int ccid = 0; ccid < partitions.size(); ccid++) {
            for (Gateway g : partitions.get(ccid)) {
                allDrones.get(g.getID()).setCcid(ccid);
                g.setCcid(ccid);
                List<Drone> list = partitionMap.getOrDefault(ccid, new ArrayList<>());
                list.add((Drone) allDrones.get(g.getID()));
                partitionMap.put(ccid, list);
            }
        }

        if (moving == null) {
            return new ArrayList<>();
        }
        HashMap<Integer, List<Drone>> neighborClusterMap = new HashMap<>();
        for (Gateway neighbor : moving.getNeighborList()) {
            List<Drone> list = neighborClusterMap.getOrDefault(neighbor.getCcid(), new ArrayList<>());
            list.add((Drone) neighbor);
            neighborClusterMap.put(neighbor.getCcid(), list);
        }
        Point2D p = destinations.get(moving.getID()).getPoint2D();
        HashSet<Point2D> visited = new HashSet<>();

//        if (isSafeToMove(neighborClusterMap, p)) {
//            return true;
//        }
//        Stack<Point2D> track = new Stack<>();
        Queue<Point2D> queue = new LinkedList<>();
        Point2D current = moving.getPoint2D();
        visited.add(current);
        HashMap<Point2D, Integer> dfs = new HashMap<>();
        dfs.put(current, 1);
//        track.push(current);
        queue.add(current);
        // find all locations that moving drone can move safely
        Set<Point2D> intersection = new HashSet<>();
        for (Integer key : partitionMap.keySet()) {
            List<Drone> list = partitionMap.get(key);
            Set<Point2D> union = new HashSet<>();
            for (Drone drone : list) {
                union.addAll(find8Neighbors(drone.getPoint2D()));
                union.addAll(getCellCorners(drone.getPoint2D()));
            }
            if (intersection.isEmpty()) {
                intersection.addAll(union);
            } else {
                intersection.retainAll(union);
            }
        }

        HashMap<Point2D, Point2D> parent = new HashMap<>();

        while (!queue.isEmpty()) {
            Point2D poll = queue.poll();
            List<Point2D> eightNeighbors = find8Neighbors(poll);
            for (Point2D cell : eightNeighbors) {

                Point2D corner = null;
                boolean isSafe = intersection.contains(cell);
                if (isSafe && cell.getX() != poll.getX() && cell.getY() != poll.getY()) {
                    corner = new Point2D.Double((cell.getX() + poll.getX()) / 2, (cell.getY() + poll.getY()) / 2);
                    isSafe = intersection.contains(corner);
                }

                if (isSafe && !visited.contains(cell)) {
                    parent.put(cell, poll);
                    if (cell.equals(p)) {
                        ArrayList<Point2D> result = new ArrayList<>();

                        while (cell != null) {
                            result.add(cell);
                            cell = parent.getOrDefault(cell, null);
                        }
                        Collections.reverse(result);
                        return result;
                    }
                    visited.add(cell);
                    queue.offer(cell);
                }
            }
        }


//        while (!current.equals(p)) {
//            List<Point2D> eightNeighbors = find8Neighbors(current);
//            boolean added = false;
//            for (Point2D cell : eightNeighbors) {
//                if (intersection.contains(cell) && !visited.contains(cell)) {
//                    current = cell;
//                    track.push(current);
//                    visited.add(current);
//                    added = true;
//                    break;
//                }
//            }
//            if (!added) {
//                if (track.isEmpty() || track.peek().equals(current)) return false;
//                else current = track.pop();
//            }


//            if (AnalyticGeometry.euclideanDistance(moving.getPoint2D(), p) <= maxDroneSpeed) {
//                moving.setLocation(p);
//            } else {
//                Point2D coordinates = AnalyticGeometry.getCoordinates(moving.getPoint2D(), p, maxDroneSpeed);
//                moving.setLocation(coordinates);
//            }
//            NetworkUtils.calculateActorNeighborhoods(allDrones, transmissionRange);
//            double reachability = NetworkUtils.calculateConnectivityMeasure(allDrones, transmissionRange);
//            if (!DoubleUtils.equals(reachability, 1)) {
//                return false;
//            }
//        }
        return new ArrayList<>();

    }

    private List<? extends Point2D> getCellCorners(Point2D p) {
        return Arrays.asList(new Point2D.Double(p.getX() - 100, p.getY() - 100),
                new Point2D.Double(p.getX() - 100, p.getY() + 100),
                new Point2D.Double(p.getX() + 100, p.getY() - 100),
                new Point2D.Double(p.getX() + 100, p.getY() + 100));
    }

    private List<Point2D> find8Neighbors(Point2D p) {
        return Arrays.asList(new Point2D.Double(p.getX() + 200, p.getY()),
                new Point2D.Double(p.getX() - 200, p.getY()),
                new Point2D.Double(p.getX(), p.getY() + 200),
                new Point2D.Double(p.getX(), p.getY() - 200),
                new Point2D.Double(p.getX() + 200, p.getY() + 200),
                new Point2D.Double(p.getX() + 200, p.getY() - 200),
                new Point2D.Double(p.getX() - 200, p.getY() + 200),
                new Point2D.Double(p.getX() - 200, p.getY() - 200)
        );
    }

    private boolean isSafeToMove(HashMap<Integer, List<Drone>> neighborCluster, Point2D to) {
        for (int ccid : neighborCluster.keySet()) {
            List<Drone> list = neighborCluster.get(ccid);
            boolean check = false;
            for (Drone d : list) {
                Point2D from = d.getPoint2D();
                if (AnalyticGeometry.EstimatedDistance(from, to) <= transmissionRange) {
                    check = true;
                    break;
                }
            }
            if (!check) return false;
        }
        return true;
    }

}
