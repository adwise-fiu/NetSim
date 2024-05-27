package dronenet;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import network.Constants;
import network.Gateway;
import network.SensorAndActorNetwork;
import utils.NetworkUtils;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author fatihsenel
 * date: 02.04.24
 */
public class DistinctTopologies {

    public static void generate() {
        SensorAndActorNetwork sensorAndActorNetwork = new SensorAndActorNetwork();
        Map<Integer, Set<String>> map = new HashMap<>();
        for (int i = 5; i < 41; i++) {
            Set<String> set = map.getOrDefault(i, new HashSet<>());
            while (set.size() < 51) {
                sensorAndActorNetwork.generateRandomGraph(i, 0, 0, 2000, 2000);
                ArrayList<Gateway> actorsArray = sensorAndActorNetwork.getActorsArray();
                List<Drone> drones = new ArrayList<>();
                for (Gateway gateway : actorsArray) {
                    Drone d = new Drone(gateway.getID());
                    d.setLocation(gateway.getPoint2D());
                    drones.add(d);
                }
                String s = serializeNetwork(drones);
                set.add(s);
            }
            map.put(i, set);
        }
        dumpMapToJson(map);
        System.out.println();
    }

    private static String toStr(int count) {
        return count < 10 ? "0" + count : String.valueOf(count);
    }

    public static void runExperiment(int count) {
        Map<Integer, List<List<Drone>>> droneMap = loadDronesFromJson();
        ContractionExpansion c = new ContractionExpansion((int) Constants.ActorTransmissionRange);
        AlphaContractionDto alphaContractionDto;
        long last = System.currentTimeMillis();
        long start = last;

        String fileNameBottleneckOptimal = "contraction/" + toStr(count) + "_Bottleneck_Optimal.csv";
        String fileNameBottleneckZero = "contraction/" + toStr(count) + "_Bottleneck_Zero.csv";
        String fileNameBottleneckFull = "contraction/" + toStr(count) + "_Bottleneck_Full.csv";

        String fileNameMinSumOptimal = "contraction/" + toStr(count) + "_MinSum_Optimal.csv";
        String fileNameMinSumZero = "contraction/" + toStr(count) + "_MinSum_Zero.csv";
        String fileNameMinSumFull = "contraction/" + toStr(count) + "_MinSum_Full.csv";


        for (int droneCount : droneMap.keySet()) {
            if (droneCount == count) {

                List<List<Drone>> listOfNetworks = droneMap.get(droneCount);
                for (int i = 0; i < listOfNetworks.size() - 1; i++) {
//                    if (i >= a && i <= b) {
                    List<Drone> source = listOfNetworks.get(i);
                    for (int j = i + 1; j < listOfNetworks.size(); j++) {

                        // create a random contraction point
                        c.setRandomContractionPoint(2000, 2000);

                        List<Drone> destination = listOfNetworks.get(j);
                        alphaContractionDto = c.optimalContraction(source, destination, ContractionExpansion.BOTTLENECK);
                        dump(fileNameBottleneckOptimal, source.size(), alphaContractionDto, "Bottleneck", "Optimal");

                        alphaContractionDto = c.alphaContraction(source, destination, ContractionExpansion.BOTTLENECK, 0);
                        dump(fileNameBottleneckZero, source.size(), alphaContractionDto, "Bottleneck", "No-Contraction");


                        alphaContractionDto = c.alphaContraction(source, destination, ContractionExpansion.BOTTLENECK, 1);
                        dump(fileNameBottleneckFull, source.size(), alphaContractionDto, "Bottleneck", "Full-Contraction");

                        alphaContractionDto = c.optimalContraction(source, destination, ContractionExpansion.MIN_SUM);
                        dump(fileNameMinSumOptimal, source.size(), alphaContractionDto, "Min Sum", "Optimal");

                        alphaContractionDto = c.alphaContraction(source, destination, ContractionExpansion.MIN_SUM, 0);
                        dump(fileNameMinSumZero, source.size(), alphaContractionDto, "Min Sum", "No-Contraction");

                        alphaContractionDto = c.alphaContraction(source, destination, ContractionExpansion.MIN_SUM, 1);
                        dump(fileNameMinSumFull, source.size(), alphaContractionDto, "Min Sum", "Full-Contraction");

                        long current = System.currentTimeMillis();
                        System.out.println("Drone: " + source.size() + "\t(" + i + "," + j + "):\ttime: " + (current - last));
                        last = current;

                    }
//                    }
                }
                long etp = System.currentTimeMillis();
                long l = (etp - start) / 1000;
                long min = l / 60L;
                long sec = l % 60L;
                System.out.println("DONE: " + droneCount + "\tElapsed: " + min + " mins " + sec + " secs");
                start = etp;
            }
        }
    }

    class Element {
        int index, val;

        Element(int i, int v) {
            index = i;
            val = v;
        }
    }

    public static void main(String[] args) throws IOException {
//        generate();

//        runExperiment(5);
        for (int i = 30; i <= 30; i++) {
            runExperiment(i);
            System.out.println();// Suggest garbage collection
        }

//        List<String> filenames = ContractionExpansion.readFileNames(args);
//        Map<Integer, Set<String>> map = new HashMap<>();
//        for (String filename : filenames) {
//            ContractionExpansion contractionExpansion = new ContractionExpansion(filename, ContractionExpansion.BOTTLENECK);
//            List<List<Drone>> listOfNetworks = contractionExpansion.dto.droneNet.getListofTimeIndexedDrones();
//            Set<String> set = map.getOrDefault(listOfNetworks.get(0).size(), new HashSet<>());
//            for (int i = 0; i < listOfNetworks.size(); i++) {
//                String s = serializeNetwork(listOfNetworks, i);
//                set.add(s);
//            }
//            map.put(listOfNetworks.get(0).size(), set);
//            System.out.println(filename + "\t -done");
//
//        }
//        dumpMapToJson(map);
//        System.out.println(map.size());
        // ********************************
//        int count = 10;
//        String fileNameBottleneckOptimal = "contraction/Experiments_Bottleneck_Optimal_" + count + ".csv";
//        String fileNameBottleneckZero = "contraction/Experiments_Bottleneck_Zero_" + count + ".csv";
//        String fileNameBottleneckFull = "contraction/Experiments_Bottleneck_Full_" + count + ".csv";
//
//        String fileNameMinSumOptimal = "contraction/Experiments_MinSum_Optimal_" + count + ".csv";
//        String fileNameMinSumZero = "contraction/Experiments_MinSum_Zero_" + count + ".csv";
//        String fileNameMinSumFull = "contraction/Experiments_MinSum_Full_" + count + ".csv";

//        createFile(fileNameBottleneckOptimal);
//        createFile(fileNameBottleneckZero);
//        createFile(fileNameBottleneckFull);
//        createFile(fileNameMinSumOptimal);
//        createFile(fileNameMinSumZero);
//        createFile(fileNameMinSumFull);
//
//        int k = 6;
//        int a = 421, b = 480;
//        Map<Integer, List<List<Drone>>> droneMap = loadDronesFromJson();
//        ContractionExpansion c = new ContractionExpansion(285);

//        Map<Integer, List<AlphaContractionDto>> optimalBottleneckMap = new HashMap<>();
//        Map<Integer, List<AlphaContractionDto>> zeroBottleneckMap = new HashMap<>();
//        Map<Integer, List<AlphaContractionDto>> fullBottleneckMap = new HashMap<>();

//        Map<Integer, List<AlphaContractionDto>> optimalMinSumMap = new HashMap<>();
//        Map<Integer, List<AlphaContractionDto>> zeroMinSumMap = new HashMap<>();
//        Map<Integer, List<AlphaContractionDto>> fullMinSumMap = new HashMap<>();

//        AlphaContractionDto alphaContractionDto;
//        long last = System.currentTimeMillis();
//        long start = last;
//        for (int droneCount : droneMap.keySet()) {
//            if (droneCount == count) {
//
//                List<List<Drone>> listOfNetworks = droneMap.get(droneCount);
//                for (int i = 0; i < listOfNetworks.size() - 1; i++) {
//                    if (i >= a && i <= b) {
//                        List<Drone> source = listOfNetworks.get(i);
//                        for (int j = i + 1; j < listOfNetworks.size(); j++) {
//                            List<Drone> destination = listOfNetworks.get(j);
//                            alphaContractionDto = c.optimalContraction(source, destination, ContractionExpansion.BOTTLENECK);
//                            dump(fileNameBottleneckOptimal, source.size(), alphaContractionDto, "Bottleneck", "Optimal");
////                    List<AlphaContractionDto> opt = optimalBottleneckMap.getOrDefault(source.size(), new ArrayList<>());
////                    opt.add(alphaContractionDto);
////                    optimalBottleneckMap.put(source.size(), opt);
//
//                            alphaContractionDto = c.alphaContraction(source, destination, ContractionExpansion.BOTTLENECK, 0);
//                            dump(fileNameBottleneckZero, source.size(), alphaContractionDto, "Bottleneck", "No-Contraction");
//
////                    List<AlphaContractionDto> zeroList = zeroBottleneckMap.getOrDefault(source.size(), new ArrayList<>());
////                    zeroList.add(alphaContractionDto);
////                    zeroBottleneckMap.put(source.size(), zeroList);
//
//                            alphaContractionDto = c.alphaContraction(source, destination, ContractionExpansion.BOTTLENECK, 1);
//                            dump(fileNameBottleneckFull, source.size(), alphaContractionDto, "Bottleneck", "Full-Contraction");
////                    List<AlphaContractionDto> fullList = fullBottleneckMap.getOrDefault(source.size(), new ArrayList<>());
////                    fullList.add(alphaContractionDto);
////                    fullBottleneckMap.put(source.size(), fullList);
//
//                            alphaContractionDto = c.optimalContraction(source, destination, ContractionExpansion.MIN_SUM);
//                            dump(fileNameMinSumOptimal, source.size(), alphaContractionDto, "Min Sum", "Optimal");
////                    List<AlphaContractionDto> optMinSum = optimalMinSumMap.getOrDefault(source.size(), new ArrayList<>());
////                    optMinSum.add(alphaContractionDto);
////                    optimalMinSumMap.put(source.size(), optMinSum);
//
//                            alphaContractionDto = c.alphaContraction(source, destination, ContractionExpansion.MIN_SUM, 0);
//                            dump(fileNameMinSumZero, source.size(), alphaContractionDto, "Min Sum", "No-Contraction");
//
////                    List<AlphaContractionDto> zeroMinSumList = zeroMinSumMap.getOrDefault(source.size(), new ArrayList<>());
////                    zeroMinSumList.add(alphaContractionDto);
////                    zeroMinSumMap.put(source.size(), zeroMinSumList);
//
//                            alphaContractionDto = c.alphaContraction(source, destination, ContractionExpansion.MIN_SUM, 1);
//                            dump(fileNameMinSumFull, source.size(), alphaContractionDto, "Min Sum", "Full-Contraction");
//
////                    List<AlphaContractionDto> fullMinSumList = fullMinSumMap.getOrDefault(source.size(), new ArrayList<>());
////                    fullMinSumList.add(alphaContractionDto);
////                    fullMinSumMap.put(source.size(), fullMinSumList);
//                            long current = System.currentTimeMillis();
//                            System.out.println("Drone: " + source.size() + "\t(" + i + "," + j + "):\ttime: " + (current - last));
//                            last = current;
//
//                        }
//                    }
//                }
//                long etp = System.currentTimeMillis();
//                long l = (etp - start) / 1000;
//                long min = l / 60L;
//                long sec = l % 60L;
//                System.out.println("DONE: " + droneCount + "\tElapsed: " + min + " mins " + sec + " secs");
//                start = etp;
//            }
//        }

//        try {
//            // Open the file in append mode
//            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
//
//            // Append a new line with comma-separated values
//            writer.write("# drones,matchingModel,contraction,alpha,distance,duration,disconnectedMoments,avg reachability\n");
//            // Close the writer
//            writer.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        for (int i = 5; i <= 10; i++) {
//            dump(fileName, optimalBottleneckMap, i, "Bottleneck", "Optimal");
//            dump(fileName, optimalMinSumMap, i, "MinSum", "Optimal");
//            dump(fileName, zeroBottleneckMap, i, "Bottleneck", "No-Contraction");
//            dump(fileName, zeroMinSumMap, i, "MinSum", "No-Contraction");
//
//            dump(fileName, fullBottleneckMap, i, "Bottleneck", "Full-Contraction");
//            dump(fileName, fullMinSumMap, i, "MinSum", "Full-Contraction");
//        }


        System.out.println("END OF SIMULATION");
    }

    private static void createFile(String fileName) {
        try {
            // Open the file in append mode
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));

            // Append a new line with comma-separated values
            writer.write("# drones,matchingModel,contraction,alpha,distance,duration,disconnectedMoments,avg reachability\n");
            // Close the writer
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void dump(String filename, int droneCount, AlphaContractionDto dto, String matchingModel, String contraction) {
        try {
            // Open the file in append mode
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
            double avgReachability = dto.reachability.stream().mapToDouble(u -> u).average().getAsDouble();
            // Append a new line with comma-separated values
            writer.write(droneCount + "," + matchingModel + "," + contraction + "," +
                    dto.alpha + "," + dto.maxTravelDistance + "," + dto.reachability.size() + "," + dto.disconnectedMoments + "," + avgReachability + "\n");

            // Close the writer
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void dump(String filename, Map<Integer, List<AlphaContractionDto>> optimalBottleneckMap, int i, String matchingModel, String contraction) {
        List<AlphaContractionDto> alphaContractionDtos = optimalBottleneckMap.get(i);
        double alpha = alphaContractionDtos.stream().mapToDouble(u -> u.alpha).average().getAsDouble();
        double travelDistance = alphaContractionDtos.stream().mapToDouble(u -> u.maxTravelDistance).average().getAsDouble();
        double duration = alphaContractionDtos.stream().mapToInt(u -> u.reachability.size()).average().getAsDouble();
        double disconnectedMoments = alphaContractionDtos.stream().mapToDouble(u -> u.disconnectedMoments).average().getAsDouble();
        double avgReachability = alphaContractionDtos.stream().mapToDouble(u -> u.reachability.stream().mapToDouble(v -> v.doubleValue()).average().getAsDouble()).average().getAsDouble();

        try {
            // Open the file in append mode
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));

            // Append a new line with comma-separated values
            writer.write(i + "," + matchingModel + "," + contraction + "," +
                    alpha + "," + travelDistance + "," + duration + "," + disconnectedMoments + "," + avgReachability + "\n");

            // Close the writer
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String serializeNetwork(List<List<Drone>> listOfNetworks, int i) {
        return serializeNetwork(listOfNetworks.get(i));
    }

    private static String serializeNetwork(List<Drone> drones) {
        StringBuilder sb = new StringBuilder();
        drones.sort((o1, o2) -> {
            if (o1.getX() == o2.getX()) {
                return Double.compare(o1.getY(), o2.getY());
            } else
                return Double.compare(o1.getX(), o2.getX());
        });
        for (int j = 0; j < drones.size(); j++) {
            Drone drone = drones.get(j);
            sb.append(drone.getPoint2D().toString());
            if (j < drones.size() - 1) sb.append(":");
        }
        return sb.toString();
    }

    public static List<Drone> loadFromString(String s) {
        String[] split = s.split(":");
        List<Drone> list = new ArrayList<>();
        for (int i = 0; i < split.length; i++) {
            Drone d = new Drone(i);
            d.setLocation(strToPoint2D(split[i]));
            list.add(d);
        }
        return list;
    }

    public static Point2D strToPoint2D(String s) {
        s = s.substring(s.indexOf("[") + 1, s.length() - 1);

        // Split the string into x and y coordinates
        String[] coordinates = s.split(",");
        double x = Double.parseDouble(coordinates[0]);
        double y = Double.parseDouble(coordinates[1]);

        // Create a Point2D object
        return new Point2D.Double(x, y);
    }

    private static void dumpMapToJson(Map<Integer, Set<String>> map) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Serialize the object to a JSON file
            objectMapper.writeValue(new File("distinctTopologies2.json"), map);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<Integer, List<List<Drone>>> loadDronesFromJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<Integer, List<List<Drone>>> droneMap = new HashMap<>();
        try {
            Map<Integer, Set<String>> map = objectMapper
                    .readValue(new File("distinctTopologies2.json"),
                            new TypeReference<Map<Integer, Set<String>>>() {
                            });

            for (int n : map.keySet()) {
                List<List<Drone>> drones = droneMap.getOrDefault(n, new ArrayList<>());
                Set<String> points = map.get(n);

                for (String str : points) {
                    List<Drone> network = loadFromString(str);
                    NetworkUtils.calculateConnectivityMeasure(network, 285);
                    drones.add(network);
                }
                droneMap.put(n, drones);
            }
            return droneMap;


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}


