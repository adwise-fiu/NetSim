package dronenet;

import com.fasterxml.jackson.databind.ObjectMapper;
import dronenet.matching.BottleneckAssignmentMatchingModel;
import dronenet.matching.MinimizeTotalMovementMatchingModel;
import geometry.AnalyticGeometry;
import network.Edge;
import network.Gateway;
import network.NetworkNode;
import utils.BinaryUtils;
import utils.DoubleUtils;
import utils.NetworkUtils;

import java.awt.geom.Point2D;
import java.io.*;
import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static network.Constants.maxDroneSpeed;


/**
 * @author fatihsenel
 * date: 09.01.23
 * WowMom Paper extension
 */
public class DroneMobilitySimulation {
    static String solverOutputPathString = "experiment_data/";
    DeploymentDto dto;

    DroneMobilitySimulation(String pathname) throws IOException {
        dto = DeploymentDto.getFromFileName(solverOutputPathString, pathname);
//        list.add(fromFileName);
        System.out.println(pathname);
    }

    public static void main(String[] args) throws IOException {

        File solverOutputPath = new File(solverOutputPathString);
        String[] pathnames = solverOutputPath.list();
        if (pathnames == null) return;
        Stream<String> pathnameListStream = Arrays.stream(pathnames).filter(u -> u.endsWith("json")).sorted(Comparator.comparingInt(a -> Integer.parseInt(a.split("_")[2])));
        // For each pathname in the pathnames array
        System.out.println("args+ " + args.length);
        int fs = -1, fe = -1;

        for (int i = 0; i < args.length; i += 2) {
            if (args[i].startsWith("-d")) {
                int f = i + 1;
                pathnameListStream = pathnameListStream.filter(u -> u.split("_")[2].equalsIgnoreCase(args[f].trim()))
                        .filter(u -> u.startsWith("mslaw"));
            } else if (args[i].startsWith("-fs")) {
                fs = Integer.parseInt(args[i + 1]);
            } else if (args[i].startsWith("-fe")) {
                fe = Integer.parseInt(args[i + 1]);
            }
        }
        if (fs != -1 && fe != -1) {
            int ffs = fs;
            int ffe = fe;
            pathnameListStream = pathnameListStream.filter(input -> {
                Pattern pattern = Pattern.compile("\\d+");
                Matcher matcher = pattern.matcher(input);

                if (matcher.find()) {
                    String numericPart = matcher.group(); // This will contain the numeric part
                    int result = Integer.parseInt(numericPart);
                    return result >= ffs && result <= ffe;
                } else {
                    return false;
                }
            });
        }
        System.out.println();

//        if (args.length == 1) {
//            if (args[0].startsWith("-d")) {
//                pathnameListStream = pathnameListStream.filter(u -> u.split("_")[2].equalsIgnoreCase(args[0].substring(3)))
//                        .filter(u -> u.startsWith("mslaw"));
//            }
//        }
        List<String> pathnameList = pathnameListStream.collect(Collectors.toList());
        System.out.println("***********\nFile To be executed (size=" + pathnameList.size() + ")");
        for (String s : pathnameList) {
            System.out.println(s);
        }
        System.out.println("***********");
        List<DroneMobilitySimulation> list = new ArrayList<>();

//        for (String pathname : pathnames) {
//            // Print the names of files and directories
//            if (pathname.endsWith(".json")) {
//                list.add(new DroneMobilitySimulation(pathname));
//                System.out.println(pathname);
//            }
//        }
        List<CoverageReachability> fixedVelocity = new ArrayList<>();
        List<CoverageReachability> varyingVelocity = new ArrayList<>();
        List<CoverageReachability> hoovering = new ArrayList<>();
        Map<String, CoverageReachability> splittedSnakeFixed = new HashMap<>();
        Map<String, CoverageReachability> bottleneck = new HashMap<>();
        int c = 1;
        for (String pathname : pathnameList) {
            if (!pathname.endsWith(".json")) continue;
            int n = Integer.parseInt(pathname.split("_")[2]);
            if (n >= 8) continue;

//            if (!pathname.contains("1500_5_out")) continue;
            if (!pathname.contains("mslaw")) continue;
            c++;
            ///************///
//            mslaw4_1500_10_out
            int id = Integer.parseInt(pathname.split("_")[0].substring(5));

            long l = System.currentTimeMillis();
//            CoverageReachability coverageReachability = minimizeTotalMovementMatchingMovement(pathname);
//            CoverageReachability coverageReachability = bottleneckAssignmentMovement(pathname, fixedVelocity, varyingVelocity, hoovering);

            CoverageReachabilityGuessed coverageReachability = moveByGuessedSchedule(pathname);


//            System.out.println("\n**************************");
            System.out.println(pathname + " Elapsed: " + (System.currentTimeMillis() - l) / 1000);
            System.out.println("**************************");
            /*
            if (coverageReachability == null) {
                continue;
            }
            bottleneck.put(pathname, coverageReachability);*/


        }

       /* CoverageReachability avg = getCoverageReachabilityAverage(bottleneck.values());
        printToCSV("bottleneck-assignment-connectivity/bottleneck_block_move_5.csv", avg.reachability, avg.coverage);
        System.out.println("DONE-- fixedVelocity.size()=" + avg.coverage.entrySet().size());*/
    }


    private static CoverageReachability snakeMovement(String pathname) throws IOException {
        Map<Double, Double> timeCoverageMap = new HashMap<>();
//        Map<Double, double[]> timeUplinkCoverageMap = new HashMap<>();
        Map<Double, Double> timeConnectivityMap = new HashMap<>();
        DroneMobilitySimulation droneMobilitySimulation = new DroneMobilitySimulation(pathname);

        DroneNet droneNet = droneMobilitySimulation.dto.droneNet;

        List<SolverOutputDto> solverOutputDtoList = droneNet.getSolverOutputDtoList();
        List<List<Drone>> listofTimeIndexedDrones = droneNet.getListofTimeIndexedDrones();
        List<List<List<Track>>> transformations = new ArrayList<>();
//        for (int i = 0; i < listofTimeIndexedDrones.size() - 1; i++) {
//            droneNet.fixOverlap(listofTimeIndexedDrones);
//        }
        droneNet.fixOverlap();
        for (int i = 0; i < listofTimeIndexedDrones.size() - 1; i++) {
            System.out.printf("********** %d **********\n", i);

            SnakeMovement snakeMovement = new SnakeMovement(listofTimeIndexedDrones.get(i),
                    listofTimeIndexedDrones.get(i + 1), solverOutputDtoList.get(0).getConfiguration().getTransmissionRange());
            List<List<Track>> trajectories = snakeMovement.trajectories;
            transformations.add(trajectories.stream()
                    .filter(list -> !list.isEmpty())
                    .collect(Collectors.toList()));
        }

        List<Integer> transformationTime = SnakeMovement.calculateTransformationTime(transformations, maxDroneSpeed);
        List<Integer> transformationTime2 = SnakeMovement.calculateStepByStepTransformationTime(transformations, maxDroneSpeed);

        droneMobilitySimulation = new DroneMobilitySimulation(pathname);
        droneNet = droneMobilitySimulation.dto.droneNet;
        int resolution = 1200;

        listofTimeIndexedDrones = droneNet.getListofTimeIndexedDrones();
        solverOutputDtoList = droneNet.getSolverOutputDtoList();
        int transmissionRange = solverOutputDtoList.get(0).getConfiguration().getTransmissionRange();
//        droneMobilitySimulation.testEdilecek(transformations, transformationTime);
        HashSet<Integer> warning = new HashSet<>();
        for (int i = 0; i < transformationTime.size(); i++) {
            System.out.printf("-----------%d--------\n", i);
            // init movingDrones
            List<Drone> gateways = listofTimeIndexedDrones.get(i);
            ArrayList<Gateway> movingDrones = new ArrayList<>();
            for (int j = 0; j < gateways.size(); j++) {
                Gateway gateway = gateways.get(j);
                Gateway drone = new Drone(j);
                drone.setLocation(gateway.getX(), gateway.getY());
                movingDrones.add(drone);
            }
            NetworkUtils.calculateActorNeighborhoods(movingDrones, transmissionRange);

            List<List<Track>> movements = transformations.get(i);
            int maxTransformationTimeInSeconds = transformationTime.get(i);
            int nextMovement = 0;
            int relativeTime = 0;
            for (int t = 0; t < resolution; t++) {
                double absoluteTime = (i * 20) + (t / 60d); //in minutes
                if (t >= 1200 - maxTransformationTimeInSeconds) {
                    relativeTime++;
                    if (nextMovement < movements.size()) {
                        List<Track> tracks = movements.get(nextMovement);
                        boolean doneWithMovement = true;
                        for (Track track : tracks) {
                            Gateway drone = findDrone(movingDrones, track);
                            if (drone == null) throw new NullPointerException("drone to be moved is null");
                            double distance = maxDroneSpeed * relativeTime;
                            if (distance > track.distance) distance = track.distance;
                            double d = AnalyticGeometry.euclideanDistance(drone.getX(), drone.getY(), track.x2, track.y2);
                            if (!DoubleUtils.equals(d, 0)) {
                                java.awt.geom.Point2D coordinates = AnalyticGeometry
                                        .getCoordinates(track.x1, track.y1, track.x2, track.y2, distance);
                                drone.setLocation(coordinates);
                            }
                            d = AnalyticGeometry.euclideanDistance(drone.getX(), drone.getY(), track.x2, track.y2);
                            if (!DoubleUtils.equals(d, 0)) {
                                doneWithMovement = false;
                            }
                        }
                        NetworkUtils.calculateActorNeighborhoods(movingDrones, transmissionRange);

                        if (doneWithMovement) {
                            System.out.println(tracks.stream().map(Track::getDrone).map(NetworkNode::getID).collect(Collectors.toList()));
                            nextMovement++;
                            relativeTime = 0;
                        }
                    }

                }
                double value = NetworkUtils.calculateConnectivityMeasure(movingDrones, transmissionRange);
                if (value < 1 && !warning.contains(nextMovement)) {
                    warning.add(nextMovement);
                    System.out.println("WARNING");
                }
                timeConnectivityMap.put(absoluteTime, value);
//                double[] doubles = droneMobilitySimulation.calculateUplinkCoverage(movingDrones, absoluteTime, transmissionRange);
//                timeUplinkCoverageMap.put(absoluteTime, doubles);
                timeCoverageMap.put(absoluteTime, droneMobilitySimulation.calculateCoverage(movingDrones, absoluteTime, transmissionRange));
            }
        }
        System.out.println();

//        Collection<double[]> values = timeUplinkCoverageMap.values();
//        Map<Double, Double> bestUplinkCoverage = getBestUplinkCoverage(timeUplinkCoverageMap);

        return new CoverageReachability(timeCoverageMap, timeConnectivityMap);
    }


    private void testEdilecek(List<List<List<Track>>> transformations, List<Integer> transformationTime) {
        DroneNet droneNet = dto.droneNet;
        int resolution = 1200;

        List<List<Drone>> listofTimeIndexedDrones = dto.droneNet.getListofTimeIndexedDrones();
        List<SolverOutputDto> solverOutputDtoList = droneNet.getSolverOutputDtoList();
        int transmissionRange = solverOutputDtoList.get(0).getConfiguration().getTransmissionRange();

        // init movingDrones
        List<Drone> gateways = listofTimeIndexedDrones.get(0);
        ArrayList<Gateway> movingDrones = new ArrayList<>();
        for (int j = 0; j < gateways.size(); j++) {
            Gateway gateway = gateways.get(j);
            Gateway drone = new Drone(j);
            drone.setLocation(gateway.getX(), gateway.getY());
            movingDrones.add(drone);
        }

        for (int i = 0; i < transformations.size(); i++) {
            List<List<Track>> transformation = transformations.get(i);
            int next = 0;
            int movementStartedTime = 0;
            for (int t = 0; t < resolution; t++) {
                if (t >= resolution - transformationTime.get(i)) {
                    double absoluteTime = (i * 20) + (t / 60d);
                    movementStartedTime++;
                    if (next < transformation.size()) {
                        List<Track> tracks = transformation.get(next);
                        boolean doneWithMovement = true;
                        for (Track track : tracks) {
                            Gateway drone = findDrone(movingDrones, track);
                            double distance = maxDroneSpeed * movementStartedTime;
                            double d = AnalyticGeometry.euclideanDistance(drone.getX(), drone.getY(), track.x2, track.y2);
                            if (!DoubleUtils.equals(d, 0)) {
                                java.awt.geom.Point2D coordinates = AnalyticGeometry
                                        .getCoordinates(track.x1, track.y1, track.x2, track.y2, distance);
                                drone.setLocation(coordinates);
                            }
                            d = AnalyticGeometry.euclideanDistance(drone.getX(), drone.getY(), track.x2, track.y2);
                            if (!DoubleUtils.equals(d, 0)) {
                                doneWithMovement = false;
                            }
                        }
                        if (doneWithMovement) {
                            next++;
                            movementStartedTime = 0;
                        }
                    }
                }
            }
        }

    }

    private static Gateway findDrone(ArrayList<Gateway> movingDrones, Track track) {
        List<Gateway> collect = movingDrones.stream().filter(u -> u.getID() == track.getDrone().getID()).collect(Collectors.toList());
        if (collect.size() == 1) return collect.get(0);
        else return null;
    }


    public static CoverageReachability minimizeTotalMovementMatchingMovement(String pathname) throws IOException {
        MinimizeTotalMovementMatchingModel solver = new MinimizeTotalMovementMatchingModel();
        DroneMobilitySimulation droneMobilitySimulation = new DroneMobilitySimulation(pathname);
        DroneNet droneNet = droneMobilitySimulation.dto.droneNet;
        droneNet.fixOverlap();
        List<SolverOutputDto> solverOutputDtoList = droneNet.getSolverOutputDtoList();
        int timeIntervalMinute = solverOutputDtoList.get(0).getConfiguration().getTimeInterval();
        int transmissionRange = solverOutputDtoList.get(0).getConfiguration().getTransmissionRange();
        List<Velocity[]> velocityVectors = new ArrayList<>();

        for (int i = 0; i < droneNet.getListofTimeIndexedDrones().size() - 1; i++) {
            List<Drone> A = droneNet.getListofTimeIndexedDrones().get(i);
            List<Drone> B = droneNet.getListofTimeIndexedDrones().get(i + 1);
            if (A.size() != B.size()) {
                velocityVectors.add(null);
                continue;
            }
            Velocity[] velocities = new Velocity[A.size()];
            List<Drone> matched = solver.doMatching(A, B);
            droneNet.getListofTimeIndexedDrones().set(i + 1, matched);
            if (matched == null || A.size() != matched.size()) {
                velocityVectors.add(null);
                continue;
            }
            NetworkUtils.calculateActorNeighborhoods(matched
                    , transmissionRange);
            droneNet.getListofTimeIndexedDrones().set(i + 1, matched);

            for (int k = 0; k < A.size(); k++) {
                Gateway s = droneNet.getListofTimeIndexedDrones().get(i).get(k);
                Gateway d = droneNet.getListofTimeIndexedDrones().get(i + 1).get(k);
                velocities[k] = new Velocity(s.getPoint2D(), d.getPoint2D(), timeIntervalMinute);
            }
            velocityVectors.add(velocities);
        }
        boolean scheduled = true;
        if (scheduled) {
            CoverageReachabilityPermutationArrays array = droneMobilitySimulation.simulateScheduledMovement2(pathname, droneNet);
            ObjectMapper objectMapper = new ObjectMapper();

            try {
                // Serialize the object to a JSON file
                objectMapper.writeValue(new File("scheduled/trajectory/" + pathname), array);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Simultaneous movement");
            CoverageReachability coverageReachability = droneMobilitySimulation.simulateDroneMovementsForFixedVelocities(velocityVectors, timeIntervalMinute, transmissionRange);
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                // Serialize the object to a JSON file
                objectMapper.writeValue(new File("block/minsum/" + pathname), coverageReachability);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(pathname + " printed");
        }
        return null;

    }

    public static CoverageReachability bottleneckAssignmentMovement(String pathname,
                                                                    List<CoverageReachability> fixedVelocity,
                                                                    List<CoverageReachability> varyingVelocity,
                                                                    List<CoverageReachability> hoovering) throws IOException {
        BottleneckAssignmentMatchingModel solver = new BottleneckAssignmentMatchingModel();
        DroneMobilitySimulation droneMobilitySimulation = new DroneMobilitySimulation(pathname);

        DroneNet droneNet = droneMobilitySimulation.dto.droneNet;
        List<SolverOutputDto> solverOutputDtoList = droneNet.getSolverOutputDtoList();
        int timeIntervalMinute = solverOutputDtoList.get(0).getConfiguration().getTimeInterval();
        int transmissionRange = solverOutputDtoList.get(0).getConfiguration().getTransmissionRange();

        List<Velocity[]> velocityVectors = new ArrayList<>();
        for (int i = 0; i < droneNet.getListofTimeIndexedDrones().size() - 1; i++) {
            List<Drone> A = droneNet.getListofTimeIndexedDrones().get(i);
            List<Drone> B = droneNet.getListofTimeIndexedDrones().get(i + 1);
            if (A.size() != B.size()) {
                velocityVectors.add(null);
                continue;
            }
            Velocity[] velocities = new Velocity[A.size()];

            List<Drone> matched = solver.doMatching(A, B);
            droneNet.getListofTimeIndexedDrones().set(i + 1, matched);
            if (matched != null && A.size() != matched.size()) {
                velocityVectors.add(null);
                continue;
            }
            if (matched != null) {
                NetworkUtils.calculateActorNeighborhoods(matched
                        , transmissionRange);
                droneNet.getListofTimeIndexedDrones().set(i + 1, matched);

                for (int k = 0; k < A.size(); k++) {
                    Gateway s = droneNet.getListofTimeIndexedDrones().get(i).get(k);
                    Gateway d = droneNet.getListofTimeIndexedDrones().get(i + 1).get(k);
                    velocities[k] = new Velocity(s.getPoint2D(), d.getPoint2D(), timeIntervalMinute);
                }
                velocityVectors.add(velocities);

            } else {
                velocityVectors.add(null);
            }

        }
        boolean scheduled = false;
        if (!scheduled) {

            CoverageReachability coverageReachability = droneMobilitySimulation.simulateDroneMovementsForFixedVelocities(velocityVectors, timeIntervalMinute, transmissionRange);
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                // Serialize the object to a JSON file
                objectMapper.writeValue(new File("block/" + pathname), coverageReachability);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(pathname + " printed");
            return null;
        } else {
            System.out.println("--------------------");
            CoverageReachabilityPermutationArrays array = droneMobilitySimulation.simulateScheduledMovement(pathname, velocityVectors, Integer.parseInt(pathname.split("_")[2]), 285);
            ObjectMapper objectMapper = new ObjectMapper();

            try {
                // Serialize the object to a JSON file
                objectMapper.writeValue(new File("scheduled/" + pathname), array);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private CoverageReachabilityPermutationArrays simulateScheduledMovement(String pathname, List<Velocity[]> velocitiesVectors, int numberOfDrones, int transmissionRange) {

        HashMap<String, List<Drone>> stateMap = new HashMap<>();
        List<HashMap<String, CoverageReachability>> map = new ArrayList<>();
        CoverageReachabilityPermutation[] avg = new CoverageReachabilityPermutation[velocitiesVectors.size()];
        CoverageReachabilityPermutation[] pairwise = new CoverageReachabilityPermutation[velocitiesVectors.size()];
        CoverageReachabilityPermutation[] fullConnectivity = new CoverageReachabilityPermutation[velocitiesVectors.size()];

        for (int i = 0; i < velocitiesVectors.size(); i++) {
            Velocity[] velocities = velocitiesVectors.get(i);
            if (velocities == null) {
                System.out.println("NULL " + i);
                continue;
            }
            List<Integer> notMoving = new ArrayList<>();
            List<Integer> moving = new ArrayList<>();
            for (int a = 0; a < velocities.length; a++) {
                if (velocities[a].start.equals(velocities[a].end))
                    notMoving.add(a);
                else moving.add(a);
            }
            int[] nums = new int[moving.size()];
            Map<Integer, Integer> permutationMap = new HashMap<>();
            for (int a = 0; a < nums.length; a++) {
                nums[a] = a;
                permutationMap.put(a, moving.get(a));
            }

            // init movingDrones
            List<Drone> gateways = dto.droneNet.getListofTimeIndexedDrones().get(i);
            ArrayList<Gateway> movingDrones = new ArrayList<>();
            for (int j = 0; j < gateways.size(); j++) {
                Gateway gateway = gateways.get(j);
                Gateway drone = new Drone(j);
                drone.setLocation(gateway.getX(), gateway.getY());
                movingDrones.add(drone);
            }
            CoverageReachability avgReachability = null;
            CoverageReachability pairwiseReachability = null;
            CoverageReachability fullConnectivityReachability = null;
            String avgPermutation = "";
            String pairwisePermutation = "";
            String fullConnectivityPermutation = "";
            int total = factorial(nums.length);
            int progress = 0;
//            for (int j = 0; j < numberOfDrones; j++) nums[j] = j;
            LocalDateTime currentTime = LocalDateTime.now();

            // Create a DateTimeFormatter to format the time
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // Format the current time as a string
            String formattedTime = currentTime.format(formatter);

            // Print the formatted time
            System.out.println("Start time[" + i + "]: " + formattedTime);
            while (true) {
                movingDrones.clear();
                for (int j = 0; j < gateways.size(); j++) {
                    Gateway gateway = gateways.get(j);
                    Gateway drone = new Drone(j);
                    drone.setLocation(gateway.getX(), gateway.getY());
                    movingDrones.add(drone);
                }
                // Print the current permutation
                // todo calculate for th
                long t = System.currentTimeMillis();
                int[] rnums = new int[numberOfDrones];
                int index = 0;
                for (int num : nums) {
                    rnums[index++] = permutationMap.get(num);
                }
                for (int a : notMoving) {
                    rnums[index++] = a;
                }
                CoverageReachability coverageReachability = moveByPermutation(rnums, movingDrones, velocities, transmissionRange, i);

                String p = Arrays.stream(rnums)
                        .mapToObj(Integer::toString)
                        .collect(Collectors.joining("-"));
                progress++;


                if (avgReachability == null || coverageReachability.hasBetterAverageReachabilityThan(avgReachability)) {
                    avgPermutation = p;
                    avgReachability = coverageReachability;
                }
                if (pairwiseReachability == null || coverageReachability.hasBetterPairwiseReachabilityThan(pairwiseReachability)) {
                    pairwisePermutation = p;
                    pairwiseReachability = coverageReachability;
                }
                if (fullConnectivityReachability == null || coverageReachability.hasBetterFullConnectivityThan(fullConnectivityReachability)) {
                    fullConnectivityPermutation = p;
                    fullConnectivityReachability = coverageReachability;
                }
                System.out.print("\rProgress: " + String.format("%.2f", (double) progress / total * 100) + "% Current: " + p + " Best: " + avgPermutation);

                // Find the largest index i such that nums[i] < nums[i+1]
                int x = moving.size() - 2;
                while (x >= 0 && nums[x] >= nums[x + 1]) {
                    x--;
                }

                // If no such index exists, all permutations have been generated
                if (x < 0) {
                    break;
                }

                // Find the largest index j such that nums[j] > nums[i]
                int y = moving.size() - 1;
                while (nums[y] <= nums[x]) {
                    y--;
                }

                // Swap nums[i] and nums[j]
                swap(nums, x, y);

                // Reverse the subarray nums[i+1...n-1]
                reverse(nums, x + 1, moving.size() - 1);
//                System.out.println(System.currentTimeMillis()-t);
            }
            System.out.println();
            avg[i] = new CoverageReachabilityPermutation(avgPermutation, "avg", avgReachability);
            pairwise[i] = new CoverageReachabilityPermutation(pairwisePermutation, "pairwise", pairwiseReachability);
            fullConnectivity[i] = new CoverageReachabilityPermutation(fullConnectivityPermutation, "avg", fullConnectivityReachability);

            double v = Arrays.stream(velocities)
                    .mapToDouble(velocity -> AnalyticGeometry.euclideanDistance(velocity.start, velocity.end))
                    .max()
                    .orElse(Double.NaN);
            int maxTransformationTimeInSeconds = (int) Math.ceil(v / maxDroneSpeed);
        }

        CoverageReachabilityPermutationArrays coverageReachabilityPermutationArrays = new CoverageReachabilityPermutationArrays();
        coverageReachabilityPermutationArrays.avg = avg;
        coverageReachabilityPermutationArrays.pairwise = pairwise;
        coverageReachabilityPermutationArrays.fullConnectivity = fullConnectivity;

        return coverageReachabilityPermutationArrays;

    }

    /**
     * This uses Min Sum Matching Model
     *
     * @param pathname
     * @param droneNet
     * @return
     */
    private CoverageReachabilityPermutationArrays simulateScheduledMovement2(String pathname, DroneNet droneNet) {

        int numberOfDrones = Integer.parseInt(pathname.split("_")[2]);
        CoverageReachabilityPermutation[] avg = new CoverageReachabilityPermutation[30];
        CoverageReachabilityPermutation[] pairwise = new CoverageReachabilityPermutation[30];
        CoverageReachabilityPermutation[] fullConnectivity = new CoverageReachabilityPermutation[30];

        for (int i = 0; i < droneNet.getListofTimeIndexedDrones().size() - 1; i++) {
            List<Drone> from = droneNet.getListofTimeIndexedDrones().get(i);
            List<Drone> to = droneNet.getListofTimeIndexedDrones().get(i + 1);
            List<Integer> moving = new ArrayList<>();
            List<Integer> notMoving = new ArrayList<>();
            for (int j = 0; j < from.size(); j++) {
                if (from.get(j).getPoint2D().equals(to.get(j).getPoint2D())) {
                    notMoving.add(j);
                } else {
                    moving.add(j);
                }
            }
            int[] nums = new int[moving.size()];
            Map<Integer, Integer> permutationMap = new HashMap<>();
            for (int a = 0; a < nums.length; a++) {
                nums[a] = a;
                permutationMap.put(a, moving.get(a));
            }
            ArrayList<Gateway> movingDrones = new ArrayList<>();
            for (int j = 0; j < from.size(); j++) {
                Drone gateway = from.get(j);
                Drone drone = new Drone(j);
                drone.setLocation(gateway.getX(), gateway.getY());
                movingDrones.add(drone);
            }
            CoverageReachability avgReachability = null;
            CoverageReachability pairwiseReachability = null;
            CoverageReachability fullConnectivityReachability = null;
            String avgPermutation = "";
            String pairwisePermutation = "";
            String fullConnectivityPermutation = "";
            int total = factorial(nums.length);
            int progress = 0;
//            for (int j = 0; j < numberOfDrones; j++) nums[j] = j;
            LocalDateTime currentTime = LocalDateTime.now();

            // Create a DateTimeFormatter to format the time
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // Format the current time as a string
            String formattedTime = currentTime.format(formatter);

            // Print the formatted time
            System.out.println("Start time[" + i + "]: " + formattedTime);
            Velocity[] velocities = new Velocity[movingDrones.size()];
            for (int k = 0; k < from.size(); k++) {
                Gateway s = from.get(k);
                Gateway d = to.get(k);
                velocities[k] = new Velocity(s.getPoint2D(), d.getPoint2D(), maxDroneSpeed);
            }

            while (true) {
                movingDrones.clear();
                for (int j = 0; j < from.size(); j++) {
                    Drone gateway = from.get(j);
                    Drone drone = new Drone(j);
                    drone.setLocation(gateway.getX(), gateway.getY());
                    movingDrones.add(drone);
                }
                // Print the current permutation
                // todo calculate for th
                long t = System.currentTimeMillis();
                int[] rnums = new int[numberOfDrones];
                int index = 0;
                for (int num : nums) {
                    rnums[index++] = permutationMap.get(num);
                }
                for (int a : notMoving) {
                    rnums[index++] = a;
                }
                int transmissionRange = droneNet.getSolverOutputDtoList().get(i).getConfiguration().getTransmissionRange();
                // todo compute trajectories --- Continue From Here 23.10.2023
                Trajectory[] trajectories = computeTrajectories(from, to, rnums, transmissionRange);
                // this list contains the indices of trajectories having two flight options: Non-Stop or visiting waypoints


                List<Integer> multipleTrajectoryOptionIndices = new ArrayList<>();
                for (int j = 0; j < trajectories.length; j++) {
                    Trajectory trajectory = trajectories[j];
                    if (trajectory.hasWaypoints()) {
                        multipleTrajectoryOptionIndices.add(j);
                    }
                }

                int n = multipleTrajectoryOptionIndices.size();
                for (int j = 0; j < Math.pow(2, n); j++) {
                    String binaryString = BinaryUtils.convertToBinaryWithLeadingZeros(j, n);
                    int[] onesIndices = BinaryUtils.findIndicesOf(binaryString, BinaryUtils.ONE);
                    int[] zerosIndices = BinaryUtils.findIndicesOf(binaryString, BinaryUtils.ZERO);
                    for (int k : onesIndices) {
                        Trajectory trajectory = trajectories[multipleTrajectoryOptionIndices.get(k)];
                        trajectory.setNonStop(true);
                    }

                    String p = Arrays.stream(rnums)
                            .mapToObj(u -> u + (trajectories[u].isNonStop() ? "N" : ""))
                            .collect(Collectors.joining("-"));

                    CoverageReachability coverageReachability = moveByPermutationTrajectory(rnums, movingDrones, trajectories, transmissionRange, i);
                    if (avgReachability == null || coverageReachability.hasBetterAverageReachabilityThan(avgReachability)) {
                        avgPermutation = p;
                        avgReachability = coverageReachability;
                    }
                    if (pairwiseReachability == null || coverageReachability.hasBetterPairwiseReachabilityThan(pairwiseReachability)) {
                        pairwisePermutation = p;
                        pairwiseReachability = coverageReachability;
                    }
                    if (fullConnectivityReachability == null || coverageReachability.hasBetterAverageReachabilityThan(fullConnectivityReachability)) {
                        fullConnectivityPermutation = p;
                        fullConnectivityReachability = coverageReachability;
                    }
                    for (Trajectory trajectory : trajectories) {
                        trajectory.setNonStop(false);
                    }

                }


                String p = Arrays.stream(rnums)
                        .mapToObj(Integer::toString)
                        .collect(Collectors.joining("-"));
                progress++;
                System.out.print("\rProgress: " + String.format("%.2f", (double) progress / total * 100) + "% Current: " + p + " Best: " + avgPermutation);

                // Find the largest index i such that nums[i] < nums[i+1]
                int x = moving.size() - 2;
                while (x >= 0 && nums[x] >= nums[x + 1]) {
                    x--;
                }

                // If no such index exists, all permutations have been generated
                if (x < 0) {
                    break;
                }

                // Find the largest index j such that nums[j] > nums[i]
                int y = moving.size() - 1;
                while (nums[y] <= nums[x]) {
                    y--;
                }

                // Swap nums[i] and nums[j]
                swap(nums, x, y);

                // Reverse the subarray nums[i+1...n-1]
                reverse(nums, x + 1, moving.size() - 1);
//                System.out.println(System.currentTimeMillis()-t);
            }

            System.out.println();
            avg[i] = new CoverageReachabilityPermutation(avgPermutation, "avg", avgReachability);
            pairwise[i] = new CoverageReachabilityPermutation(pairwisePermutation, "pairwise", pairwiseReachability);
            fullConnectivity[i] = new CoverageReachabilityPermutation(fullConnectivityPermutation, "avg", fullConnectivityReachability);


        }


        CoverageReachabilityPermutationArrays coverageReachabilityPermutationArrays = new CoverageReachabilityPermutationArrays();
        coverageReachabilityPermutationArrays.avg = avg;
        coverageReachabilityPermutationArrays.pairwise = pairwise;
        coverageReachabilityPermutationArrays.fullConnectivity = fullConnectivity;

        return coverageReachabilityPermutationArrays;

    }


    public CoverageReachability moveByPermutationTrajectory(int[] nums,
                                                            ArrayList<Gateway> movingDrones,
                                                            Trajectory[] trajectories,
                                                            int transmissionRange,
                                                            int timeStage) {
        Map<Double, Double> timeCoverageMap = new HashMap<>();
        Map<Double, Double> timeConnectivityMap = new HashMap<>();

        int[] transformationTimes = Arrays.stream(trajectories).mapToInt(Trajectory::getDuration).toArray();
        int maxTransformationTimeInSeconds = Arrays.stream(transformationTimes).sum();
        int next = 0;

        for (int t = 0; t < 1200; t++) {
            double time = (timeStage * 20) + (t / 60d);
            if (t >= 1200 - maxTransformationTimeInSeconds) {
                while (next < transformationTimes.length && transformationTimes[nums[next]] == 0) {
                    next++;
                }
                if (next >= transformationTimes.length) break;
                Gateway drone = movingDrones.get(nums[next]);
                Trajectory trajectory = trajectories[nums[next]];
                transformationTimes[nums[next]]--;
                // move drone along its trajectory
                drone.setLocation(trajectory.getNextLocation());
                NetworkUtils.calculateActorNeighborhoods(movingDrones, transmissionRange);
            }
            timeConnectivityMap.put(time, NetworkUtils.calculateConnectivityMeasure(movingDrones, transmissionRange));
            timeCoverageMap.put(time, calculateCoverage(movingDrones, time, transmissionRange));
        }
        return new CoverageReachability(timeCoverageMap, timeConnectivityMap);
    }

    public CoverageReachability moveByPermutation(int[] nums, ArrayList<Gateway> movingDrones, Velocity[] velocities, int transmissionRange, int timeStage) {
        Map<Double, Double> timeCoverageMap = new HashMap<>();
        Map<Double, Double> timeConnectivityMap = new HashMap<>();
        int[] transformationTimes = Arrays.stream(velocities).mapToInt(u -> (int) Math.ceil(AnalyticGeometry.euclideanDistance(u.start, u.end) / maxDroneSpeed)).toArray();
        int maxTransformationTimeInSeconds = Arrays.stream(transformationTimes).sum();
        int next = 0;
        int k = 0;
        for (int t = 0; t < 1200; t++) {
            double time = (timeStage * 20) + (t / 60d);
            if (t >= 1200 - maxTransformationTimeInSeconds) {
                while (next < transformationTimes.length && transformationTimes[nums[next]] == 0) {
                    next++;
                    k = 0;
                }
                k++;
                if (next >= transformationTimes.length) break;
                Gateway drone = movingDrones.get(nums[next]);
                transformationTimes[nums[next]]--;
                double d = AnalyticGeometry.euclideanDistance(drone.getX(), drone.getY(), velocities[nums[next]].end.getX(), velocities[nums[next]].end.getY());
                if (!DoubleUtils.equals(d, 0)) {
                    double distance = maxDroneSpeed * k;
                    java.awt.geom.Point2D coordinates = AnalyticGeometry.getCoordinates(velocities[nums[next]].start, velocities[nums[next]].end, distance);
                    drone.setLocation(coordinates);
                }
                NetworkUtils.calculateActorNeighborhoods(movingDrones, transmissionRange);
            }
            timeConnectivityMap.put(time, NetworkUtils.calculateConnectivityMeasure(movingDrones, transmissionRange));
            timeCoverageMap.put(time, calculateCoverage(movingDrones, time, transmissionRange));
        }
        return new CoverageReachability(timeCoverageMap, timeConnectivityMap);
    }

    private static void reverse(int[] nums, int start, int end) {
        while (start < end) {
            swap(nums, start, end);
            start++;
            end--;
        }
    }

    // Utility function to swap two elements in an array
    private static void swap(int[] nums, int i, int j) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }

    private CoverageReachability simulateDroneMovementsForFixedVelocities(List<Velocity[]> velocityVectors, int timeIntervalMinute, int transmissionRange) {

        Map<Double, Double> timeCoverageMap = new HashMap<>();
//        Map<Double, double[]> timeUplinkCoverageMap = new HashMap<>();
        Map<Double, Double> timeConnectivityMap = new HashMap<>();
        for (int i = 0; i < velocityVectors.size(); i++) {
            Velocity[] velocities = velocityVectors.get(i);
            if (velocities == null) {
                System.out.println("NULL " + i);
                continue;
            }
            // init movingDrones
            List<Drone> gateways = dto.droneNet.getListofTimeIndexedDrones().get(i);
            ArrayList<Gateway> movingDrones = new ArrayList<>();
            for (int j = 0; j < gateways.size(); j++) {
                Gateway gateway = gateways.get(j);
                Gateway drone = new Drone(j);
                drone.setLocation(gateway.getX(), gateway.getY());
                movingDrones.add(drone);
            }
            double v = Arrays.stream(velocities)
                    .mapToDouble(velocity -> AnalyticGeometry.euclideanDistance(velocity.start, velocity.end))
                    .max()
                    .orElse(Double.NaN);
            int maxTransformationTimeInSeconds = (int) Math.ceil(v / maxDroneSpeed);
            int k = 0;
            for (int t = 0; t < 1200; t++) {
                double time = (i * 20) + (t / 60d);
                if (t >= 1200 - maxTransformationTimeInSeconds) {
                    k++;
                    for (int j = 0; j < movingDrones.size(); j++) {
                        Gateway drone = movingDrones.get(j);
                        double d = AnalyticGeometry.euclideanDistance(drone.getX(), drone.getY(), velocities[j].end.getX(), velocities[j].end.getY());
                        if (!DoubleUtils.equals(d, 0)) {
                            double distance = maxDroneSpeed * k;
                            java.awt.geom.Point2D coordinates = AnalyticGeometry.getCoordinates(velocities[j].start, velocities[j].end, distance);
                            drone.setLocation(coordinates);
                        }
                    }
                    NetworkUtils.calculateActorNeighborhoods(movingDrones, transmissionRange);
                }
                timeConnectivityMap.put(time, NetworkUtils.calculateConnectivityMeasure(movingDrones, transmissionRange));
                timeCoverageMap.put(time, calculateCoverage(movingDrones, time, transmissionRange));
            }


//            System.out.println("Velocity[" + i + "]");
        }
        System.out.println();
//        Map<Double, Double> bestUplinkCoverage = getBestUplinkCoverage(timeUplinkCoverageMap);
        return new CoverageReachability(timeCoverageMap, timeConnectivityMap);
//        return new CoverageReachability(bestUplinkCoverage, timeConnectivityMap);
        //todo burda kaldim timeConnectivityMap ve timeCoverageMap'i csv dosyasina yazdiracam
    }

    private static void printToCSV(String filename, Map<Double, Double> timeConnectivityMap, Map<Double, Double> timeCoverageMap) {
        List<Double> keys = timeCoverageMap.keySet().stream().sorted().collect(Collectors.toList());
        StringBuilder stringBuilder = new StringBuilder("Time,Coverage,Reachability\n");

        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(filename, true));
            for (double key : keys) {
                String time = String.format("%,.2f", key);
                String coverage = String.format("%,.2f", timeCoverageMap.get(key));
                String connectivity = String.format("%,.2f", timeConnectivityMap.get(key));
                stringBuilder
                        .append(time).append(",")
                        .append(coverage).append(",").
                        append(connectivity).append("\n");
            }
            writer.append(stringBuilder.toString());

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    // returns [0] moving, and [1] hoovering
    private CoverageReachability[] simulateDroneMovementsForVaryingVelocities(List<Velocity[]> velocityVectors, int timeIntervalMinute, int transmissionRange, String filename) {
//        List<Gateway> drones = new ArrayList<>();
        //.replaceAll("\\D+", "")

        Map<Double, Double> timeCoverageMapForMovingDrones = new HashMap<>();
        Map<Double, Double> timeConnectivityMapForMovingDrones = new HashMap<>();
        Map<Double, Double> timeCoverageMapForHooveringDrones = new HashMap<>();
        Map<Double, Double> timeConnectivityMapForHooveringDrones = new HashMap<>();

        String[] s = filename.split("\\.")[0].split("_");
        filename = "bottleneck-assignment-connectivity/cc_" + s[0] + "_" + s[2] + ".csv";
        HashMap<Double, CoverageConnectivity> movingDroneMap = new HashMap<>();
        HashMap<Double, CoverageConnectivity> hooveringDroneMap = new HashMap<>();
        int resolution = 1200;
        double timeChange = timeIntervalMinute * 1d / resolution;

        for (int i = 0; i < velocityVectors.size(); i++) {
            Velocity[] velocities = velocityVectors.get(i);
            if (velocities == null) {
                System.out.println("NULL " + i);
                continue;
            }
            // init movingDrones
            List<Drone> gateways = dto.droneNet.getListofTimeIndexedDrones().get(i);
            ArrayList<Gateway> movingDrones = new ArrayList<>();
            ArrayList<Gateway> hooveringDrones = new ArrayList<>();
            for (int j = 0; j < gateways.size(); j++) {
                Gateway gateway = gateways.get(j);
                Gateway drone = new Drone(j);
                drone.setLocation(gateway.getX(), gateway.getY());
                movingDrones.add(drone);
                Gateway hooveringDrone = new Drone(j);
                hooveringDrone.setLocation(gateway.getX(), gateway.getY());
                hooveringDrones.add(hooveringDrone);
            }
            double t = i * timeIntervalMinute;


//            double[] coverageChangeMovingDrones = new double[resolution + 1];
//            double[] coverageChangeHooveringDrones = new double[resolution + 1];
//            double[] connectivityMetricChangeMovingDrones = new double[resolution + 1];
//            boolean[] connectivityChangeMovingDrones = new boolean[resolution + 1];
//            boolean[] connectivityChangeHooveringDrones = new boolean[resolution + 1];
            NetworkUtils.calculateActorNeighborhoods(movingDrones, transmissionRange);
            NetworkUtils.calculateActorNeighborhoods(hooveringDrones, transmissionRange);
//            coverageChangeMovingDrones[0] = calculateCoverage(movingDrones, t, transmissionRange);
            timeCoverageMapForMovingDrones.put(t, calculateCoverage(movingDrones, t, transmissionRange));

//            connectivityChangeMovingDrones[0] = NetworkUtils.isConnected(movingDrones);

            timeCoverageMapForHooveringDrones.put(t, calculateCoverage(hooveringDrones, t, transmissionRange));
//            coverageChangeHooveringDrones[0] = calculateCoverage(hooveringDrones, t, transmissionRange);
//            connectivityChangeHooveringDrones[0] = NetworkUtils.isConnected(hooveringDrones);
            timeConnectivityMapForHooveringDrones.put(t, NetworkUtils.calculateConnectivityMeasure(hooveringDrones, transmissionRange));
            timeConnectivityMapForMovingDrones.put(t, NetworkUtils.calculateConnectivityMeasure(movingDrones, transmissionRange));
//            connectivityMetricChangeMovingDrones[0] = NetworkUtils.calculateConnectivityMeasure(movingDrones, transmissionRange);
            for (int k = 0; k < resolution; k++) {
                // update movingDrones
                for (int j = 0; j < movingDrones.size(); j++) {
                    Gateway drone = movingDrones.get(j);
                    double distance = velocities[j].speed * (k + 1) * timeChange;
                    java.awt.geom.Point2D coordinates = AnalyticGeometry.getCoordinates(velocities[j].start, velocities[j].end, distance);
                    drone.setLocation(coordinates);
                }
                NetworkUtils.calculateActorNeighborhoods(movingDrones, transmissionRange);

                t += timeChange;
                timeCoverageMapForMovingDrones.put(t, calculateCoverage(movingDrones, t, transmissionRange));
                timeConnectivityMapForMovingDrones.put(t, NetworkUtils.calculateConnectivityMeasure(movingDrones, transmissionRange));

//                coverageChangeMovingDrones[k + 1] = calculateCoverage(movingDrones, t, transmissionRange);
//                connectivityChangeMovingDrones[k + 1] = NetworkUtils.isConnected(movingDrones);

//                connectivityMetricChangeMovingDrones[k + 1] = NetworkUtils.calculateConnectivityMeasure(movingDrones, transmissionRange);


                timeCoverageMapForHooveringDrones.put(t, calculateCoverage(hooveringDrones, t, transmissionRange));
//                coverageChangeHooveringDrones[k + 1] = calculateCoverage(hooveringDrones, t, transmissionRange);
//                connectivityChangeHooveringDrones[k + 1] = NetworkUtils.isConnected(hooveringDrones);
                timeConnectivityMapForHooveringDrones.put(t, NetworkUtils.calculateConnectivityMeasure(hooveringDrones, transmissionRange));
            }
//            CoverageConnectivity ccMoving = new CoverageConnectivity(t, coverageChangeMovingDrones, connectivityChangeMovingDrones);
//            ccMoving.connectivityMetric = connectivityMetricChangeMovingDrones;
//            movingDroneMap.put(t, ccMoving);
//            hooveringDroneMap.put(t, new CoverageConnectivity(t, coverageChangeHooveringDrones, connectivityChangeHooveringDrones));
        }
//        printToFile(timeIntervalMinute, filename, movingDroneMap, hooveringDroneMap, timeChange);

//        System.out.println();


//        try (PrintWriter writer = new PrintWriter("cc.csv")) {
//            writer.write(stringBuilder.toString());
//
//            for (double time : key) {
//                CoverageConnectivity ccMoving = movingDroneMap.get(time);
//                CoverageConnectivity ccHoovering = hooveringDroneMap.get(time);
//                System.out.println((int) Math.round(time));
//                StringBuilder line = toCSVString(ccMoving, ccHoovering, timeChange, timeIntervalMinute);
//                writer.write(line.toString());
////            ccMoving.print(timeChange);
////            ccHoovering.print(timeChange);
////            System.out.println("-------------");
//            }
//
//            System.out.println("CC-Created");
//        } catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
//        }
        CoverageReachability moving = new CoverageReachability(timeCoverageMapForMovingDrones, timeConnectivityMapForMovingDrones);
        CoverageReachability hoovering = new CoverageReachability(timeCoverageMapForHooveringDrones, timeConnectivityMapForHooveringDrones);
        return new CoverageReachability[]{moving, hoovering};
    }

    private void printToFile(int timeIntervalMinute, String filename, HashMap<Double, CoverageConnectivity> movingDroneMap, HashMap<Double, CoverageConnectivity> hooveringDroneMap, double timeChange) {
        //        Set<Double> keySet = movingDroneMap.keySet().stream().sorted().collect(Collectors.toSet());
        List<Double> list = new ArrayList<>(movingDroneMap.keySet());
        List<Double> key = list.stream().sorted().collect(Collectors.toList());
        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append("Time,Coverage (Moving),Connectivity (Moving),Coverage (Hoovering),Connectivity (Hoovering)\n");
        stringBuilder.append("Time,Connectivity,Reachability\n");

        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(filename, false));
            writer.append(stringBuilder.toString());
            for (double time : key) {
                CoverageConnectivity ccMoving = movingDroneMap.get(time);
                CoverageConnectivity ccHoovering = hooveringDroneMap.get(time);
//                System.out.println((int) Math.round(time));
//                StringBuilder line = toCSVString(ccMoving, ccHoovering, timeChange, timeIntervalMinute);
                StringBuilder line = toCSVString(ccMoving, timeChange, timeIntervalMinute);
                writer.append(line.toString());
//            ccMoving.print(timeChange);
//            ccHoovering.print(timeChange);
//            System.out.println("-------------");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private StringBuilder toCSVString(CoverageConnectivity ccMoving, CoverageConnectivity ccHoovering, double timeChange, int timeIntervalMinute) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ccMoving.coverage.length; i++) {
            String time = String.format("%,.2f", ccMoving.time + i * timeChange - timeIntervalMinute);
            String ccMovingCoverage = String.format("%,.2f", ccMoving.coverage[i] * 100);
            String ccHooveringCoverage = String.format("%,.2f", ccHoovering.coverage[i] * 100);
            String ccMovingConnectivity = ccMoving.connnectivity[i] ? "1" : "0";
            String ccHooveringConnectivity = ccHoovering.connnectivity[i] ? "1" : "0";
            sb.append(time).append(",").append(ccMovingCoverage).append(",").append(ccMovingConnectivity).append(",").append(ccHooveringCoverage).append(",").append(ccHooveringConnectivity).append('\n');
        }
        return sb;
    }

    private StringBuilder toCSVString(CoverageConnectivity ccMoving, double timeChange, int timeIntervalMinute) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ccMoving.coverage.length; i++) {
            String time = String.format("%,.2f", ccMoving.time + i * timeChange - timeIntervalMinute);
            String ccMovingConnectivity = ccMoving.connnectivity[i] ? "1" : "0";
            String reachability = String.format("%.2f", ccMoving.connectivityMetric[i]);
            sb.append(time).append(",").append(ccMovingConnectivity).append(",").append(reachability).append('\n');
        }
        return sb;
    }

    class CoverageConnectivity {
        double[] coverage;
        boolean[] connnectivity;
        double[] connectivityMetric;
        double time;

        public CoverageConnectivity(double time) {
            this.time = time;
        }

        public CoverageConnectivity(double t, double[] coverage, boolean[] connnectivity) {
            time = t;
            this.coverage = coverage;
            this.connnectivity = connnectivity;
        }

        public void print(double timeChange) {
            System.out.print("[");
            for (int i = 0; i < coverage.length; i++) {
                String connect = connnectivity[i] ? "c" : "d";
                System.out.print(String.format("%,.2f", time + i * timeChange - 20) + "-" + String.format("%,.2f", coverage[i] * 100) + "(" + connect + "),\t");
            }
            System.out.println("]");

        }
    }

    private double calculateCoverage(ArrayList<Gateway> drones, double time, double transmissionRange) {
        List<MobileNode> mobileNodes = dto.droneNet.getMobileNodes();
        int count = 0;
        for (MobileNode mobileNode : mobileNodes) {
            TemporalLocation locationAtTime = mobileNode.getTemporalLocationAtTime(time);
            if (locationAtTime == null) continue;
            for (Gateway drone : drones) {
                double distance = AnalyticGeometry.euclideanDistance(locationAtTime.x, locationAtTime.y, drone.getX(), drone.getY());
                if (distance <= transmissionRange) {
                    count++;
                    break;
                }
            }
        }
        return count * 1d / mobileNodes.size();
    }

    private double[] calculateUplinkCoverage(ArrayList<Gateway> drones, double time, double transmissionRange) {
        ArrayList<ArrayList<Gateway>> dfs = NetworkUtils.DephtFirstSearch(drones);
        HashMap<Gateway, Integer> droneGroupMap = new HashMap<>();

        IntStream.range(0, dfs.size())
                .forEach(i -> dfs.get(i).forEach(gateway -> droneGroupMap.put(gateway, i)));


        List<MobileNode> mobileNodes = dto.droneNet.getMobileNodes();
        int[] count = new int[drones.size()];
        for (MobileNode mobileNode : mobileNodes) {
            TemporalLocation locationAtTime = mobileNode.getTemporalLocationAtTime(time);
            if (locationAtTime == null) continue;
            Set<Integer> visited = new HashSet<>();

            for (Gateway drone : drones) {

                int group = droneGroupMap.get(drone);
                if (visited.contains(group)) continue;
                double distance = AnalyticGeometry.euclideanDistance(locationAtTime.x, locationAtTime.y, drone.getX(), drone.getY());

                if (distance <= transmissionRange) {
                    visited.add(group);
                    ArrayList<Gateway> reachable = dfs.get(group);
                    for (Gateway g : reachable) {
                        count[g.getID()]++;
                    }
                }
            }

        }
        return Arrays.stream(count).mapToDouble(j -> j * 1d / mobileNodes.size()).toArray();
    }

    public static CoverageReachability getCoverageReachabilityAverage(Collection<CoverageReachability> list) {
        CoverageReachability averages = new CoverageReachability();
        Map<Double, Double> coverageSum = new HashMap<>();
        Map<Double, Double> reachabilitySum = new HashMap<>();

        // Calculate the sum of each element in the coverage and reachability maps
        for (CoverageReachability cr : list) {
            for (Map.Entry<Double, Double> entry : cr.getCoverage().entrySet()) {
                Double key = entry.getKey();
                Double value = entry.getValue();
                coverageSum.put(key, coverageSum.getOrDefault(key, 0.0) + value);
            }
            for (Map.Entry<Double, Double> entry : cr.getReachability().entrySet()) {
                Double key = entry.getKey();
                Double value = entry.getValue();
                reachabilitySum.put(key, reachabilitySum.getOrDefault(key, 0.0) + value);
            }
        }

        // Calculate the average of each element in the coverage and reachability maps
        int size = list.size();
        Map<Double, Double> coverageAvg = new HashMap<>();
        Map<Double, Double> reachabilityAvg = new HashMap<>();
        for (Map.Entry<Double, Double> entry : coverageSum.entrySet()) {
            Double key = entry.getKey();
            Double value = entry.getValue() / size;
            coverageAvg.put(key, value);
        }
        for (Map.Entry<Double, Double> entry : reachabilitySum.entrySet()) {
            Double key = entry.getKey();
            Double value = entry.getValue() / size;
            reachabilityAvg.put(key, value);
        }

        // Set the average maps to the averages object and return it
        averages.setCoverage(coverageAvg);
        averages.setReachability(reachabilityAvg);
        return averages;
    }

    public static Map<Double, Double> getBestUplinkCoverage(Map<Double, double[]> inputMap) {
        Collection<double[]> values = inputMap.values();
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Collection cannot be null or empty.");
        }

        int columnCount = values.iterator().next().length;
        int maxIndex = -1;
        double maxAverage = Double.MIN_VALUE;

        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            double sum = 0;
            int rowCount = 0;

            for (double[] row : values) {
                if (row.length <= columnIndex) {
                    throw new IllegalArgumentException("Invalid column index: " + columnIndex);
                }

                sum += row[columnIndex];
                rowCount++;
            }

            double average = sum / rowCount;

            if (average > maxAverage) {
                maxAverage = average;
                maxIndex = columnIndex;
            }
        }
        Map<Double, Double> outputMap = new HashMap<>();
        for (Double key : inputMap.keySet()) {
            outputMap.put(key, inputMap.get(key)[maxIndex]);
        }

        return outputMap;
    }

    private Trajectory[] computeTrajectories(List<Drone> from,
                                             List<Drone> to,
                                             int[] permutation,
                                             double transmissionRange) {
        //todo
        Trajectory[] trajectories = new Trajectory[from.size()];
        ArrayList<Drone> movingDrones = new ArrayList<>();
        for (int j = 0; j < from.size(); j++) {
            Drone gateway = from.get(j);
            Drone drone = new Drone(j);
            drone.setLocation(gateway.getX(), gateway.getY());
            movingDrones.add(drone);
        }

        for (int p : permutation) {
            Drone drone = movingDrones.get(p);
            Point2D end = to.get(p).getPoint2D();
            List<Drone> closestDrones = findClosestDroneToLocation(movingDrones, end);
            boolean pathSearch = true;
            for (Drone d : closestDrones) {
                if (d.getID() == drone.getID()) {
                    pathSearch = false;
                    break;
                }
            }
            trajectories[p] = new Trajectory(drone.getPoint2D(), end, maxDroneSpeed);
            if (pathSearch) {
                List<Drone> path = findPath(movingDrones, drone, closestDrones, transmissionRange);
                if (path != null && !path.isEmpty()) {
                    List<Point2D> waypoints = path.stream().map(Gateway::getPoint2D).collect(Collectors.toList());
                    trajectories[p].setWaypoints(waypoints);
                }
            }
            drone.setLocation(end);
        }

        return trajectories;
    }

    private List<Drone> findPath(ArrayList<Drone> movingDrones, Drone start, List<Drone> endList, double transmissionRange) {
        List<Drone> shortest = null;
        for (Drone end : endList) {
            List<Drone> path = findPath(movingDrones, start, end, transmissionRange);
            if (shortest == null || shortest.size() > path.size()) {
                shortest = path;
            }
        }
        return shortest;
    }

    private List<Drone> findPath(ArrayList<Drone> movingDrones, Drone start, Drone end, double transmissionRange) {
        ArrayList<Gateway> list = new ArrayList<>();
        Map<Integer, Drone> map = new HashMap<>();
        for (Drone drone : movingDrones) {
            map.put(drone.getID(), drone);
            Gateway g = new Gateway(drone.getID());
            g.setLocation(drone.getPoint2D());
            list.add(g);
        }
        NetworkUtils.calculateActorNeighborhoods(list, transmissionRange);
        ArrayList<ArrayList<Gateway>> dfs = NetworkUtils.DephtFirstSearch(list);
        ArrayList<Gateway> connectedComponent = null;
        for (ArrayList<Gateway> cc : dfs) {
            boolean sf = false, ef = false;
            for (Gateway gateway : cc) {
                if (gateway.getID() == start.getID()) sf = true;
                if (gateway.getID() == end.getID()) ef = true;
            }
            if (sf && ef) {
                connectedComponent = cc;
                break;
            } else {
                if (sf || ef) {
                    break;
                }
            }
        }
        if (connectedComponent != null) {
            ArrayList<Edge> edges = NetworkUtils.runKruskal(connectedComponent);
            List<Gateway> mstPath = NetworkUtils.findMSTPath(start, end, edges);
            unify(mstPath);
            mstPath.removeIf(u -> u.getID() == start.getID());

            List<Drone> ans = new ArrayList<>();
            for (Gateway g : mstPath) {
                if (g.getPoint2D().equals(end.getPoint2D())) break;
                ans.add(map.get(g.getID()));
            }
            return ans;
        } else return new ArrayList<>();

    }

    private void unify(List<Gateway> mstPath) {
        List<Integer> tobedeleted = new ArrayList<>();
        Gateway p = mstPath.get(0);
        for (int i = 1; i < mstPath.size(); i++) {
            Gateway c = mstPath.get(i);
            if (p.getPoint2D().equals(c.getPoint2D())) {
                tobedeleted.add(i);
            } else {
                p = c;
            }
        }
        for (int i = tobedeleted.size() - 1; i >= 0; i--) {
            mstPath.remove(mstPath.get(tobedeleted.get(i)));
        }

    }


    private List<Drone> findClosestDroneToLocation(List<Drone> movingDrones, Point2D end) {
        List<Drone> closest = new ArrayList<>();
        Map<Double, List<Drone>> map = new HashMap<>();
        double minDist = Double.MAX_VALUE;
        for (Drone drone : movingDrones) {
            double distance = AnalyticGeometry.euclideanDistance(drone.getX(), drone.getY(), end.getX(), end.getY());
            boolean keyFound = false;
            double key = -1;
            for (double mapKey : map.keySet()) {
                if (DoubleUtils.equals(mapKey, distance)) {
                    keyFound = true;
                    key = mapKey;
                    break;
                }
            }
            if (keyFound) {
                List<Drone> list = map.get(key);
                list.add(drone);
                map.put(key, list);
                minDist = Math.min(minDist, key);

            } else {
                List<Drone> list = new ArrayList<>();
                list.add(drone);
                map.put(distance, list);
                minDist = Math.min(minDist, distance);
            }


        }
        return map.get(minDist);
    }

    private int factorial(int n) {
        int f = 1;
        for (int i = 1; i <= n; i++) f *= i;
        return f;

    }


    public static CoverageReachabilityGuessed moveByGuessedSchedule(String pathname) throws IOException {
        MinimizeTotalMovementMatchingModel solver = new MinimizeTotalMovementMatchingModel();
        DroneMobilitySimulation droneMobilitySimulation = new DroneMobilitySimulation(pathname);
        DroneNet droneNet = droneMobilitySimulation.dto.droneNet;
        droneNet.fixOverlap();
        List<SolverOutputDto> solverOutputDtoList = droneNet.getSolverOutputDtoList();
        int timeIntervalMinute = solverOutputDtoList.get(0).getConfiguration().getTimeInterval();
        int transmissionRange = solverOutputDtoList.get(0).getConfiguration().getTransmissionRange();
        List<Velocity[]> velocityVectors = new ArrayList<>();

        for (int i = 0; i < droneNet.getListofTimeIndexedDrones().size() - 1; i++) {
            List<Drone> A = droneNet.getListofTimeIndexedDrones().get(i);
            List<Drone> B = droneNet.getListofTimeIndexedDrones().get(i + 1);
            if (A.size() != B.size()) {
                velocityVectors.add(null);
                continue;
            }
            List<Drone> matched = solver.doMatching(A, B);
            droneNet.getListofTimeIndexedDrones().set(i + 1, matched);
            if (matched == null || A.size() != matched.size()) {
                continue;
            }
            NetworkUtils.calculateActorNeighborhoods(matched
                    , transmissionRange);
            droneNet.getListofTimeIndexedDrones().set(i + 1, matched);
        }
        String matching = "min-sum/";
        List<String> optimalSchedule = getOptimalSchedule(matching, pathname);
        CoverageReachability coverageReachability = new CoverageReachability();
        List<String> schedules = new ArrayList<>();
        for (int i = 0; i < droneNet.getListofTimeIndexedDrones().size() - 1; i++) {
            List<Drone> source = droneNet.getListofTimeIndexedDrones().get(i);
            List<Drone> dest = droneNet.getListofTimeIndexedDrones().get(i + 1);
            DroneMobilityScheduleGuess droneMobilityScheduleGuess =
                    new DroneMobilityScheduleGuess(source, dest, transmissionRange);
            List<Path> paths = droneMobilityScheduleGuess.guessASchedule();
            List<Integer> collect = paths.stream().map(u -> u.id).collect(Collectors.toList());
            StringBuilder guessedPath = new StringBuilder();
            for (Integer integer : collect) {
                guessedPath.append(integer).append("-");
            }


            StringBuilder sb = new StringBuilder();
            Set<Integer> movingSet = new HashSet<>();
            for (Path path : paths) {
                sb.append(path.id).append("-");
                movingSet.add(path.id);
            }
            for (Drone drone : source) {
                if (!movingSet.contains(drone.getID())) {
                    sb.append(drone.getID()).append("-");
                    movingSet.add(drone.getID());
                }
            }
            String schedule = sb.toString();
            schedule = schedule.substring(0, schedule.length() - 1);
            schedules.add(schedule);

            ArrayList<Gateway> movingDrones = new ArrayList<>();
            for (Drone drone : source) {
                Drone nd = new Drone(drone.getID());
                nd.setLocation(drone.getPoint2D());
                movingDrones.add(nd);
            }
            droneMobilitySimulation.move(
                    movingDrones,
                    paths,
                    i, transmissionRange, coverageReachability);

        }
        CoverageReachabilityGuessed result = new CoverageReachabilityGuessed(coverageReachability, schedules);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Serialize the object to a JSON file
            objectMapper.writeValue(new File("scheduled/guessed/" + pathname), result);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private void move(ArrayList<Gateway> movingDrones,
                      List<Path> paths,
                      int timeStage,
                      int transmissionRange,
                      CoverageReachability coverageReachability) {
//        Map<Double, Double> timeCoverageMap = new HashMap<>();
//        Map<Double, Double> timeConnectivityMap = new HashMap<>();
        int transformationTime = getTime(paths);
        int nextPath = 0;
        int nextWaypoint = 1;
        int k = 0;
        for (int t = 0; t < 1200; t++) {
            double time = (timeStage * 20) + (t / 60d);
            if (t >= 1200 - transformationTime) {
                Path path = paths.get(nextPath);
                Gateway drone = movingDrones.get(path.id);
                Point2D nextLoc = AnalyticGeometry.getCoordinates(drone.getPoint2D(), path.waypoints.get(nextWaypoint), maxDroneSpeed);
                drone.setLocation(nextLoc);
                if (nextLoc.equals(path.waypoints.get(nextWaypoint))) {
                    if (path.waypoints.size() - 1 == nextWaypoint) {
                        nextWaypoint = 1;
                        nextPath++;
                    } else {
                        nextWaypoint++;
                    }
                }

                NetworkUtils.calculateActorNeighborhoods(movingDrones, transmissionRange);
            }
            coverageReachability.getReachability().put(time, NetworkUtils.calculateConnectivityMeasure(movingDrones, transmissionRange));
            coverageReachability.getCoverage().put(time, calculateCoverage(movingDrones, time, transmissionRange));
        }
    }

    public static int getTime(List<Path> paths) {
        int time = 0;
        for (Path path : paths) {
            for (int i = 0; i < path.waypoints.size() - 1; i++) {
                Point2D from = path.waypoints.get(i);
                Point2D to = path.waypoints.get(i + 1);
                double distance = AnalyticGeometry.euclideanDistance(from, to);
                time += Math.ceil(distance / maxDroneSpeed);
            }
        }
        return time;
    }

    public static List<String> getOptimalSchedule(String matching, String filename) {
        String path = "scheduled/" + matching + filename;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            CoverageReachabilityPermutationArrays yourObject = objectMapper.readValue(new File(path), CoverageReachabilityPermutationArrays.class);
            return Arrays.stream(yourObject.getAvg()).map(CoverageReachabilityPermutation::getPermutation).collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();


    }

}
