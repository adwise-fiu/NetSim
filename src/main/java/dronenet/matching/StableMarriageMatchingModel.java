package dronenet.matching;

import dronenet.Drone;
import geometry.AnalyticGeometry;
import network.Gateway;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author fatihsenel
 * date: 25.10.23
 */
public class StableMarriageMatchingModel implements MatchingModel {
    static StableMarriageMatchingModel instance;

    public List<Drone> doMatching(List<Drone> A, List<Drone> B) {
        int n = A.size(); // Assuming A and B have the same size

        GatewayTo[][] source = new GatewayTo[A.size()][B.size()];
        GatewayTo[][] destination = new GatewayTo[B.size()][A.size()];

        for (int i = 0; i < A.size(); i++) {
            Gateway u = A.get(i);
            for (int j = 0; j < B.size(); j++) {
                Gateway v = B.get(j);
                double distance = AnalyticGeometry.euclideanDistance(u.getX(), u.getY(), v.getX(), v.getY());
                source[i][j] = new GatewayTo(v, distance);
                destination[j][i] = new GatewayTo(u, distance);
            }
        }

        int[][] menPreferences = new int[n][n];
        int[] sourcePreferenceIndex = new int[n];
        int[] destinationPreferenceIndex = new int[n];
        int[][] womenPreferences = new int[n][n];

        setPreferences(n, source, menPreferences, sourcePreferenceIndex);
        setPreferences(n, destination, womenPreferences, destinationPreferenceIndex);

        // Create an array to store the matching pairs (A to B)
        int[] aToB = new int[n];
        Arrays.fill(aToB, -1); // Initialize to -1, indicating no match yet

        // Create an array to store the matching pairs (B to A)
        int[] bToA = new int[n];
        Arrays.fill(bToA, -1); // Initialize to -1, indicating no match yet
        int freeCount = n;
        boolean mEngaged[] = new boolean[n];
        int wPartner[] = new int[n];
        Arrays.fill(wPartner, -1);
        while (freeCount > 0) {
            int m;
            for (m = 0; m < n; m++) {
                if (!mEngaged[m]) {
                    break;
                }
            }
            // One by one go to all women
            // according to m's preferences.
            // Here m is the picked free man
            for (int i = 0; i < n && !mEngaged[m]; i++) {
                int w = menPreferences[m][i];

                // The woman of preference is free,
                // w and m become partners (Note that
                // the partnership maybe changed later).
                // So we can say they are engaged not married
                if (wPartner[w] == -1) {
                    wPartner[w] = m;
                    mEngaged[m] = true;
                    freeCount--;
                } else // If w is not free
                {
                    // Find current engagement of w
                    int m1 = wPartner[w];

                    // If w prefers m over her current engagement m1,
                    // then break the engagement between w and m1 and
                    // engage m with w.
                    if (isPossiblePartnerPreferable(womenPreferences[w], m, wPartner[w])) {
                        wPartner[w] = m;
                        mEngaged[m] = true;
                        mEngaged[m1] = false;
                    }
                } // End of Else
            } // End of the for loop that goes
            // to all women in m's list


        }
        List<Drone> result = new ArrayList<>();
        for(int i=0;i<n;i++){
            Drone drone = new Drone(i);
            for(int w=0;w<wPartner.length;w++) {
                if(wPartner[w] == i) {
                    drone.setLocation(B.get(w).getPoint2D());
                    break;
                }
            }
            result.add(drone);
        }

        return result;

//        List<Drone> C = new ArrayList<>(n); // Initialize the list of new drones C
//
//        // Create an array to store the matching pairs (A to B)
//        int[] aToB = new int[n];
//        Arrays.fill(aToB, -1); // Initialize to -1, indicating no match yet
//
//        // Create an array to store the matching pairs (B to A)
//        int[] bToA = new int[n];
//        Arrays.fill(bToA, -1); // Initialize to -1, indicating no match yet
//
//        // Perform the Gale-Shapley algorithm
//        boolean[] aProposed = new boolean[n]; // Keep track of which A has proposed
//        int aFreeCount = n; // Count of unmatched A drones
//
//        while (aFreeCount > 0) {
//            int a;
//            for (a = 0; a < n; a++) {
//                if (!aProposed[a]) {
//                    break;
//                }
//            }
//
//            for (int i = 0; i < n && !aProposed[a]; i++) {
//                int b = A.get(a).findClosestUnmatchedDrone(B, bToA); // Find closest unmatched B drone
//
//                if (bToA[b] == -1) {
//                    // B is unassigned, create a new drone C and set its XY coordinates
//                    C.add(new Drone(B.get(b).getX(), B.get(b).getY()));
//
//                    aToB[a] = b;
//                    bToA[b] = a;
//                    aFreeCount--;
//                } else {
//                    int a1 = bToA[b];
//                    if (A.get(a).isCloserTo(B.get(b), B.get(a1))) {
//                        aToB[a] = b;
//                        bToA[b] = a;
//                        aProposed[a1] = false;
//                        aProposed[a] = true;
//                    }
//                }
//            }
//        }
//
//        return C;
    }

    private int pickAFreeMen(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == -1) return i;
        }
        return -1;
    }

    private void setPreferences(int n, GatewayTo[][] source, int[][] sourcePreferences, int[] sourcePreferenceIndex) {
        for (int i = 0; i < source.length; i++) {
            List<Integer> collect = Arrays.stream(source[i]) // Stream of objects
                    .sorted()
                    .map(u -> u.to.getID()) // Replace YourObjectType with the actual type
                    .collect(Collectors.toList());
            for (int j = 0; j < n; j++) {
                sourcePreferences[i][j] = collect.get(j);
            }
            sourcePreferenceIndex[i] = 0;
        }
    }

    private boolean isPossiblePartnerPreferable(int[] preference, int possiblePartner, int currentPartner) {
        for (int i : preference) {
            if (i == possiblePartner) return true;
            if (i == currentPartner) return false;
        }
        return false;
    }

    /**
     * @Override public List<Drone> doMatching(List<Drone> A, List<Drone> B) {
     * GatewayTo[][] source = new GatewayTo[A.size()][B.size()];
     * GatewayTo[][] destination = new GatewayTo[B.size()][A.size()];
     * <p>
     * for (int i = 0; i < A.size(); i++) {
     * Gateway u = A.get(i);
     * for (int j = 0; j < B.size(); j++) {
     * Gateway v = B.get(j);
     * double distance = AnalyticGeometry.euclideanDistance(u.getX(), u.getY(), v.getX(), v.getY());
     * source[i][j] = new GatewayTo(v, distance);
     * destination[j][i] = new GatewayTo(u, distance);
     * }
     * }
     * <p>
     * List<List<Integer>> sourcePreferences = new ArrayList<>();
     * List<List<Integer>> destinationPreferences = new ArrayList<>();
     * <p>
     * <p>
     * for (GatewayTo[] gatewayTos : source) {
     * List<Integer> collect = Arrays.stream(gatewayTos).sorted().map(u -> u.to.getID()).collect(Collectors.toList());
     * sourcePreferences.add(collect);
     * }
     * <p>
     * for (GatewayTo[] gatewayTos : destination) {
     * List<Integer> collect = Arrays.stream(gatewayTos).sorted().map(u -> u.to.getID()).collect(Collectors.toList());
     * destinationPreferences.add(collect);
     * }
     * <p>
     * List<Integer> sourcesIndices = A.stream().map(Gateway::getID).collect(Collectors.toList());
     * List<Integer> destinationIndices = B.stream().map(Gateway::getID).collect(Collectors.toList());
     * Map<Integer, Integer> map = doMatching(sourcesIndices, sourcePreferences, destinationIndices, destinationPreferences);
     * List<Drone> result = new ArrayList<>();
     * for (int i = 0; i < A.size(); i++) {
     * if (!map.containsKey(i)) return null;
     * Drone matched = B.get(map.get(i));
     * Drone drone = new Drone(i);
     * drone.setLocation(matched.getPoint2D());
     * result.add(drone);
     * }
     * return result;
     * <p>
     * }
     */
    private Map<Integer, Integer> doMatching(List<Integer> sourcesIndices,
                                             List<List<Integer>> sourcePreferences,
                                             List<Integer> destinationIndices,
                                             List<List<Integer>> destinationPreferences) {

        Map<Integer, Integer> matches = new TreeMap<>();
        List<Integer> freeMen = new LinkedList<>(sourcesIndices);

        // loop until no more free men
        while (!freeMen.isEmpty()) {
            int currentMan = freeMen.remove(0);
            List<Integer> currentManPrefers = sourcePreferences.get(currentMan);

            for (int woman : currentManPrefers) {
                if (matches.get(woman) == null) { // this woman is not matched
                    // match these two
                    System.out.println("MATCH " + woman + "-" + currentMan);
                    matches.put(woman, currentMan);
                    break;
                } else {
                    int otherMan = matches.get(woman);
                    List<Integer> currentWomanRanking = destinationPreferences.get(woman);
                    if (currentWomanRanking.indexOf(currentMan) < currentWomanRanking.indexOf(otherMan)) {
                        //this woman prefers this man to the man she's engaged to
                        System.out.println("BREAK " + woman + "-" + otherMan);
                        System.out.println("MATCH " + woman + "-" + currentMan);

                        matches.put(woman, currentMan);
                        freeMen.add(otherMan);
                        break;
                    }
                }
            }
        }

        return matches;
    }


    static class GatewayTo implements Comparable<GatewayTo> {
        Gateway to;
        double distance;

        public GatewayTo(Gateway to, double distance) {
            this.to = to;
            this.distance = distance;
        }

        @Override
        public String toString() {
            return String.valueOf(to.getID());
        }

        @Override
        public int compareTo(GatewayTo o) {
            return Double.compare(this.distance, o.distance);
        }
    }

    public static StableMarriageMatchingModel getInstance() {
        if (instance == null) {
            instance = new StableMarriageMatchingModel();
        }

        return instance;
    }
}
