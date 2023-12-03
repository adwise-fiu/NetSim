package dronenet;

import geometry.AnalyticGeometry;
import network.Edge;
import network.NetworkNode;
import utils.DoubleUtils;
import utils.MCDSUtils;
import utils.NetworkUtils;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author fatihsenel
 * date: 01.02.23
 */
public class SnakeMovement {

    List<Drone> source, dest;
    int sourceHead = -1, destinationTail = -1;
    double TR;
    int time = 0;
    List<List<Track>> trajectories;

    public SnakeMovement(List<Drone> s, List<Drone> d, double transmissionRange) {
        this.source = cloneDroneList(s);
        this.dest = cloneDroneList(d);
        TR = transmissionRange;

//        List<Drone> match = DroneMatchingModel.match(this.source, this.dest);
//        if (match != null) {
//            this.dest = match;
//        }
        NetworkUtils.calculateActorNeighborhoods(this.dest, TR);
        NetworkUtils.calculateActorNeighborhoods(this.source, TR);

//        MCDSUtils mcdsUtils = new MCDSUtils(this.dest);
//        mcdsUtils.EvaluateCDS();
        findHeadAndTail();
        if(destinationTail!=-1 &&sourceHead!=-1) {
            makeDirectedTree(this.dest, this.dest.get(this.destinationTail));
            makeDirectedTree(this.source, this.source.get(this.sourceHead));

            trajectories = simulateMovement();
            System.out.println();
        } else {
            trajectories = new ArrayList<>();
        }
    }

    private List<Drone> cloneDroneList(List<Drone> s) {
        List<Drone> list = new ArrayList<>();
        for (Drone drone : s) {
            Drone d = new Drone(drone.getID());
            d.setLocation(drone.getPoint2D());
            list.add(d);
        }
        return list;
    }

    /**
     * @param transformations list of list of list of track
     *                        [i] time snapshot
     *                        [i][j] number of cascaded movement
     *                        [i][j][k] Node movements at time i, iteration j
     * @param maxDroneSpeed   m/s
     * @return time required for topology change for each [i] time snapshot (in seconds)
     */
    public static List<Integer> calculateTransformationTime(List<List<List<Track>>> transformations, double maxDroneSpeed) {
        List<Integer> ans = new ArrayList<>();
        for (List<List<Track>> transformation : transformations) {
            double time = 0;
            for (List<Track> tracks : transformation) {
                double maxDistance = tracks.stream().mapToDouble(Track::getDistance).max().orElse(0d);
                time += maxDistance / maxDroneSpeed;
            }
            ans.add((int) Math.ceil(time));
        }
        return ans;
    }

    public static List<Integer> calculateStepByStepTransformationTime(List<List<List<Track>>> transformations, double maxDroneSpeed) {
        List<Integer> ans = new ArrayList<>();
        for (List<List<Track>> transformation : transformations) {
            double time = 0;
            for (List<Track> tracks : transformation) {
//                double maxDistance = tracks.stream().mapToDouble(Track::getDistance).max().orElse(0d);
                time += tracks.stream().mapToDouble(Track::getDistance).sum() / maxDroneSpeed;
//                time += maxDistance / maxDroneSpeed;
            }
            ans.add((int) Math.ceil(time));
        }
        return ans;
    }

    private void makeDirectedTree(List<Drone> drones, Drone root) {

        ArrayList<Edge> edges = NetworkUtils.runKruskal(drones);
        Map<Integer, List<Drone>> mstNeighborMap = new HashMap<>();

        for (Edge edge : edges) {
            List<Drone> uList = mstNeighborMap.getOrDefault(edge.u.getID(), new ArrayList<>());
            uList.add((Drone) edge.v);

            List<Drone> vList = mstNeighborMap.getOrDefault(edge.v.getID(), new ArrayList<>());
            vList.add((Drone) edge.u);

            mstNeighborMap.put(edge.u.getID(), uList);
            mstNeighborMap.put(edge.v.getID(), vList);

        }

        Queue<Drone> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            Drone poll = queue.poll();
            // find all neighbors of polled drone except incoming
            List<Drone> neighbors = mstNeighborMap.get(poll.getID()).stream().filter(u -> poll.getIncoming() == null || u.getID() != poll.getIncoming().getID()).collect(Collectors.toList());
            for (Drone drone : neighbors) {
                poll.addToOutgoing(drone);
                queue.add(drone);
            }

        }
    }

    private void findHeadAndTail() {
        HashMap<Point2D, Drone> map  = new HashMap<>();
        for(Drone d:dest) {
            map.put(d.getPoint2D(), d);
        }
        for(Drone d:source) {
            map.remove(d.getPoint2D());
        }
        if(map.isEmpty()) return;
        List<Drone> tobecovered = new ArrayList<>(map.values());

        double min = Double.MAX_VALUE;
        for (int i = 0; i < source.size(); i++) {
            Drone u = source.get(i);
            for (Drone v : tobecovered) {
                double distance = AnalyticGeometry.euclideanDistance(u.getX(), u.getY(), v.getX(), v.getY());
                if (distance < min) {
                    min = distance;
                    sourceHead = i;
                    destinationTail = v.getID();
                }
            }
        }
    }

    private List<Drone> findSourceBranch(Drone root) {
        //pick random child
        List<Drone> ans = new ArrayList<>();

        while (root != null) {
            ans.add(root);
            List<Drone> outgoing = root.getOutgoing();
            if (outgoing.isEmpty()) root = null;
            else {
                Random rand = new Random();
                root = outgoing.get(rand.nextInt(outgoing.size()));
            }
        }
        return ans;
    }

    private List<List<Track>> splitTrack(List<Track> tracks, Set<Point2D> forkSet) {
        List<List<Track>> ans = new ArrayList<>();
        ans.add(new ArrayList<>());
        for (Track track : tracks) {
            if (forkSet.contains(track.getFrom())) {
                ans.add(new ArrayList<>());
            }
            ans.get(ans.size() - 1).add(track);
        }
        return ans;
    }

    private List<Track> moveOneStep(List<Drone> line, Drone target, Map<Point2D, Fork> coordinateForkMap, int iteration) {
        List<Track> tracks = new ArrayList<>();
        Drone nextOutgoing = null;
        for (int i = line.size() - 1; i >= 0; i--) {
            Drone drone = line.get(i);
            if (i == 0 || drone.getIncoming() == null) {
                // head drone
                Track track = drone.goTo(target.getX(), target.getY(), time, iteration);
                tracks.add(track);
                Fork fork = coordinateForkMap.get(target.getPoint2D());
                if (fork != null) fork.resident = drone;

                if (nextOutgoing != null) {
                    drone.setOutgoing(Arrays.asList(nextOutgoing));
                    nextOutgoing.setIncoming(drone);
                }
            } else {
                Track track = drone.goTo(drone.getIncoming().getX(), drone.getIncoming().getY(), time, iteration);
                tracks.add(track);
                Fork fork = coordinateForkMap.get(drone.getIncoming().getPoint2D());
                if (fork != null) fork.resident = drone;

                List<Drone> collect = drone.getIncoming().getOutgoing().stream().filter(u -> u.getID() != drone.getID()).collect(Collectors.toList());
                drone.setOutgoing(collect);
                for (Drone d : collect) {
                    d.setIncoming(drone);
                }
                if (nextOutgoing != null) drone.getOutgoing().add(nextOutgoing);

                nextOutgoing = drone;
            }
        }

        return tracks;
    }

    private Set<Point2D> forkSet(List<Drone> drones, Drone head, Set<Point2D> deployed) {
//        Drone root = drones.get(sourceHead);
//        makeDirectedTree(drones, root);

        List<Drone> clones = new ArrayList<>();
        for (Drone drone : drones) {
            Drone d = new Drone(drone.getID());
            d.setLocation(drone.getPoint2D());
            clones.add(d);
        }


//        NetworkUtils.calculateActorNeighborhoods(clones, TR);
        ArrayList<Edge> edges = NetworkUtils.runKruskal(clones);
        Map<Integer, List<Drone>> mstNeighborMap = new HashMap<>();

        for (Edge edge : edges) {
            List<Drone> uList = mstNeighborMap.getOrDefault(edge.u.getID(), new ArrayList<>());
            uList.add((Drone) edge.v);

            List<Drone> vList = mstNeighborMap.getOrDefault(edge.v.getID(), new ArrayList<>());
            vList.add((Drone) edge.u);

            mstNeighborMap.put(edge.u.getID(), uList);
            mstNeighborMap.put(edge.v.getID(), vList);

        }

        Set<Point2D> set = new HashSet<>();
        for (Map.Entry<Integer, List<Drone>> entry : mstNeighborMap.entrySet()) {
            List<Drone> droneList = entry.getValue();
            Drone drone = clones.get(entry.getKey());
            if (!deployed.contains(drone.getPoint2D())) {
                if ((head.getID() == drone.getID() && droneList.size() == 2) || droneList.size() > 2) {
                    set.add(drone.getPoint2D());
                } else {
                    for (Drone d : droneList) {
                        if (deployed.contains(d.getPoint2D())) {
                            set.add(drone.getPoint2D());
                        }
                    }
                }
            }
        }


//        for (Drone drone : drones) {
//            if (drone.hasMultipleOutgoingLinks())
//                set.add(drone.getPoint2D());
//        }
        return set;
    }

    public List<List<Track>> simulateMovement() {
        List<List<Track>> trajectories = new ArrayList<>();
        Set<Point2D> deployed = new HashSet<>();
        Map<Point2D, Fork> coordinateForkMap = new HashMap<>();
        Drone current = dest.get(destinationTail);
        Queue<Drone> q = new LinkedList<>();
        q.add(current);
        while (!q.isEmpty()) {
            current = q.poll();
            if (current.hasMultipleOutgoingLinks()) {
                coordinateForkMap.put(current.getPoint2D(), new Fork(current));
            }
            q.addAll(current.getOutgoing());
        }
        Stack<Point2D> forkStack = new Stack<>();

        Drone tail = dest.get(destinationTail);
        Drone head = source.get(sourceHead);

//        Set<Point2D> sourceForkSet = forkSet(source);

        int iteration = 0;
        while (tail != null) {
            double distance = AnalyticGeometry.euclideanDistance(tail.getX(), tail.getY(), head.getX(), head.getY());
            if (!DoubleUtils.equals(distance, 0)) {
                List<Drone> line = findSourceBranch(head);

                System.out.println(line.stream().map(NetworkNode::getID).collect(Collectors.toList()));
                //todo burada split edecez snake i List<List<Track> return edecek
                Set<Point2D> fs = forkSet(source, head, deployed);

                List<Track> tracks = moveOneStep(line, tail, coordinateForkMap, iteration);

                List<List<Track>> splittedTracks = splitTrack(tracks, fs);
                System.out.println(splittedTracks.stream().map(u -> u.stream().map(Track::getDrone).map(NetworkNode::getID).collect(Collectors.toList())).collect(Collectors.toList()));
                trajectories.addAll(splittedTracks);
                // find next Tail if multiple options then pick one and add the others to queue
                if (tail.hasNoOutgoingLinks()) {
                    Fork fork;
                    do {
                        if (forkStack.isEmpty()) return trajectories;

                        fork = coordinateForkMap.get(forkStack.pop());
                    } while (fork.nextDrone() == null);

                    current = tail;
                    while (current != null && !current.getPoint2D().equals(fork.owner.getPoint2D())) {
                        deployed.add(current.getPoint2D());
                        current = current.getIncoming();
                    }

                    tail = fork.nextDrone();
                    fork.next++;
//                    head = fork.resident;
                    head = findForkResident(fork.owner.getPoint2D(), source, trajectories);
                    if (fork.nextDrone() != null) forkStack.push(fork.owner.getPoint2D());
                    System.out.println();

                } else if (tail.hasSingleOutgoingLinks()) {
                    tail = tail.getOutgoing().get(0);
                } else {
                    Fork fork = coordinateForkMap.get(tail.getPoint2D());
                    Drone next = fork.nextDrone();
                    if (next == null) {
                        Fork f;
                        do {
                            if (forkStack.isEmpty()) return trajectories;
                            f = coordinateForkMap.get(forkStack.pop());
                        } while (f.nextDrone() == null);

                        tail = f.nextDrone();
                        f.next++;
                        if (f.nextDrone() != null) forkStack.push(f.owner.getPoint2D());

                    } else {
                        forkStack.push(tail.getPoint2D());
                        tail = next;
                        fork.next++;
                    }

                }

            } else {
                if (tail.hasNoOutgoingLinks()) {
                    Fork fork;
                    do {
                        if (forkStack.isEmpty()) return trajectories;

                        fork = coordinateForkMap.get(forkStack.pop());
                    } while (fork.nextDrone() == null);
                    tail = fork.nextDrone();
                    fork.next++;
                    head = findForkResident(fork.owner.getPoint2D(), source, trajectories);
                    if (fork.nextDrone() != null) forkStack.push(fork.owner.getPoint2D());
                    System.out.println();


                } else if (tail.hasSingleOutgoingLinks()) {
                    tail = tail.getOutgoing().get(0);
                } else {
                    Fork fork = coordinateForkMap.get(tail.getPoint2D());
                    Drone next = fork.nextDrone();
                    if (next == null) {

                        Fork f;
                        do {
                            if (forkStack.isEmpty()) return trajectories;
                            f = coordinateForkMap.get(forkStack.pop());
                        } while (f.nextDrone() == null);
                        tail = f.nextDrone();
                        f.next++;
                        if (f.nextDrone() != null) forkStack.push(f.owner.getPoint2D());

                    } else {
                        forkStack.push(tail.getPoint2D());
                        tail = next;
                        fork.next++;
                    }

                }
            }
            iteration++;

//            tail = getNext(tail, tailQueue);
        }
        return trajectories;
    }

    private Drone findForkResident(Point2D p, List<Drone> source, List<List<Track>> trajectories) {

        for(int i=trajectories.size()-1;i>=0;i--) {
            List<Track> tracks = trajectories.get(i);
            for(int j=tracks.size()-1;j>=0;j--) {
                Track track = tracks.get(j);
                if(track.getTo().equals(p)) {
                    return track.getDrone();
                }
            }
        }

//        for (Drone drone : source) {
//            if (drone.getPoint2D().equals(p)) return drone;
//        }
        return null;
    }

//    public Drone[] getNextPair(Drone head, Drone tail, Queue<Drone> tQueue, Queue<Drone> hQueue) {
//        List<Drone> outgoing = tail.getOutgoing();
//        if (outgoing.isEmpty()) {
//            if (queue.isEmpty()) {
//                return null;
//            } else {
//                return queue.poll();
//            }
//        } else {
//            Drone next = outgoing.get(0);
//            for (int i = 1; i < outgoing.size(); i++) {
//                queue.add(outgoing.get(i));
//            }
//            return next;
//        }
//    }

    class Fork {
        Drone resident;
        Drone owner;
        int next;

        public Fork(Drone owner) {
            this.owner = owner;
            this.next = 0;
        }

        public Drone nextDrone() {
            if (next < owner.getOutgoing().size()) {
                return owner.getOutgoing().get(next);
            }
            return null;
        }
    }


}
