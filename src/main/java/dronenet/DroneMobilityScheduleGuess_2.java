package dronenet;


import com.fasterxml.jackson.databind.ObjectMapper;
import dronenet.matching.MinimizeTotalMovementMatchingModel;
import geometry.AnalyticGeometry;
import network.Gateway;
import utils.NetworkUtils;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author fatihsenel
 * date: 13.12.23
 */
public class DroneMobilityScheduleGuess_2 {
    private static final Pattern droneCountPattern = Pattern.compile(".*_(\\d+)_out.json");
    static String solverOutputPathString = "experiment_data/";
    static String scheduled = "scheduled/";
    static String matching = "min-sum/";

    public static void main(String[] args) {

        MinimizeTotalMovementMatchingModel solver = new MinimizeTotalMovementMatchingModel();
        File directory = new File(scheduled + matching);
        File[] files = directory.listFiles();
        if (files == null) return;

        List<String> filenames = Arrays.stream(files).map(File::getName).filter(u -> u.endsWith(".json")).collect(Collectors.toList());
        HashMap<Integer, List<String>> groupedFileNames = parseFilenames(filenames);

        ObjectMapper objectMapper = new ObjectMapper();
        CoverageReachabilityPermutationArrays obj = null;
        Map<String, List<String>> schedules = new HashMap<>();
        for (String filename : filenames) {
            if (!filename.contains("_7_")) continue;
            try {
                CoverageReachabilityPermutationArrays yourObject = objectMapper.readValue(new File(scheduled + matching + filename), CoverageReachabilityPermutationArrays.class);
                DeploymentDto dto = DeploymentDto.getFromFileName(solverOutputPathString, filename);
                if (dto == null) return;
                int timeIntervalMinute = dto.droneNet.getSolverOutputDtoList().get(0).getConfiguration().getTimeInterval();
                int transmissionRange = dto.droneNet.getSolverOutputDtoList().get(0).getConfiguration().getTransmissionRange();

                //dto.droneNet.getListofTimeIndexedDrones()
                List<String> collect = Arrays.stream(yourObject.getAvg()).map(CoverageReachabilityPermutation::getPermutation).collect(Collectors.toList());
                schedules.put(filename, collect);

                for (int i = 0; i < dto.droneNet.getListofTimeIndexedDrones().size() - 1; i++) {
                    List<Drone> A = dto.droneNet.getListofTimeIndexedDrones().get(i);
                    List<Drone> B = dto.droneNet.getListofTimeIndexedDrones().get(i + 1);

                    List<Drone> matched = solver.doMatching(A, B);
                    dto.droneNet.getListofTimeIndexedDrones().set(i + 1, matched);

                    NetworkUtils.calculateActorNeighborhoods(matched, transmissionRange);
                    dto.droneNet.getListofTimeIndexedDrones().set(i + 1, matched);
                }
                System.out.println(filename);
                findFeatures(dto, schedules.get(filename));

                System.out.println();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        System.out.println();
    }

    private static void findFeatures(DeploymentDto dto, List<String> schedules) {
        List<List<Drone>> listOfTimeIndexedDrones = dto.droneNet.getListofTimeIndexedDrones();
        List<TopologyFeatures> features = new ArrayList<>();
        int transmissionRange = dto.droneNet.getSolverOutputDtoList().get(0).getConfiguration().getTransmissionRange();
        for (int i = 0; i < listOfTimeIndexedDrones.size() - 1; i++) {
            List<Drone> source = listOfTimeIndexedDrones.get(i);
            List<Drone> dest = listOfTimeIndexedDrones.get(i + 1);
            TopologyFeatures topologyFeatures = new TopologyFeatures(source, dest, transmissionRange);
//            features.add(topologyFeatures);
            System.out.println("********** " + i + " ***********");
            System.out.println(schedules.get(i));
//            System.out.println("id\t|moving\t|safe\t|canMove");
//            System.out.println("-----------------------------");

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

            while (!moving.isEmpty()) {
//                List<Integer> safeDrones = new ArrayList<>();
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
                    track.add(dest.get(toBeMoved).getPoint2D());
                }
                for (int j = 0; j < moving.size(); j++) {
                    if (moving.get(j) == toBeMoved) {
                        moving.remove(j);
                        break;
                    }
                }
                moved.add(toBeMoved);
                Drone drone = source.get(toBeMoved);
                Drone match = dest.get(toBeMoved);
                drone.setLocation(match.getPoint2D());
                NetworkUtils.calculateActorNeighborhoods(source, transmissionRange);
                System.out.print(toBeMoved + "-");

            }
            if (!notMoving.isEmpty()) {
                System.out.print("[");
                for (int j : notMoving) {
                    System.out.print(j + "-");
                }
                System.out.println("]");
            } else {
                System.out.println();
            }


//            for (int j = 0; j < source.size(); j++) {
//                Drone drone = source.get(j);
//                Drone match = dest.get(j);
//                boolean isMoving = !drone.getPoint2D().equals(match.getPoint2D());
//                boolean inSafeRegion = topologyFeatures.isInSafeRegion(drone.getID());
//                boolean canMoveConnected = topologyFeatures.canMoveConnected(drone.getID());
//                System.out.println(drone.getID() + "\t|" + isMoving + "\t|" + inSafeRegion + "\t|" + canMoveConnected);
//            }
            System.out.print("");
        }
        System.out.println();

    }

    public static LinkedHashMap<Integer, List<String>> parseFilenames(List<String> filenames) {
        LinkedHashMap<Integer, List<String>> countToFilenames = new LinkedHashMap<>();
        for (String filename : filenames) {
            Matcher matcher = droneCountPattern.matcher(filename);
            if (matcher.find()) {
                int count = Integer.parseInt(matcher.group(1));
                if (!countToFilenames.containsKey(count)) {
                    countToFilenames.put(count, new ArrayList<>());
                }
                countToFilenames.get(count).add(filename);
            }
        }
        // Sort the value lists (list of filenames) based on the count
        for (Map.Entry<Integer, List<String>> entry : countToFilenames.entrySet()) {
            entry.getValue().sort((o1, o2) -> {
                String s1 = o1.split("_")[0];
                String s2 = o2.split("_")[0];
                Pattern pattern = Pattern.compile("[0-9]");
                Matcher m1 = pattern.matcher(s1);
                Matcher m2 = pattern.matcher(s2);
                StringBuilder sb1 = new StringBuilder();
                StringBuilder sb2 = new StringBuilder();
                while (m1.find()) {
                    sb1.append(m1.group());
                }
                while (m2.find()) {
                    sb2.append(m2.group());
                }
                int g1 = Integer.parseInt(sb1.toString());
                int g2 = Integer.parseInt(sb2.toString());
                return Integer.compare(g1, g2);
            });
        }
        return countToFilenames;
    }


}
