package dronenet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import geometry.AnalyticGeometry;
import geometry.Point2D;
import network.Edge;
import network.Gateway;
import utils.DoubleUtils;
import utils.NetworkUtils;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author fatihsenel
 * date: 16.02.22
 */
public class DroneNet {

    //    TemporalHeatmapDto temporalHeatmap;
    SolverOutputDto solverOutput;
    List<SolverOutputDto> solverOutputDtoList;
    List<List<Drone>> listofTimeIndexedDrones = new ArrayList<>();
    List<MobileNode> mobileNodes;

    SolverInputDto solverInputDto;

    public List<double[][]> createHeatMaps(List<MobileNode> mobileNodes, Configuration configuration) {
        int columns = (int) Math.ceil(1f * configuration.width / configuration.cellWidth);
        int rows = (int) Math.ceil(1f * configuration.height / configuration.cellWidth);

        List<double[][]> result = new ArrayList<>();

        double t = configuration.simulationStart;
        while (DoubleUtils.lessThanOrEquals(t, configuration.simulationEnd)) {

            double[][] hm = new double[rows][columns];
            for (MobileNode mobileNode : mobileNodes) {
                TemporalLocation tloc = mobileNode.getTemporalLocationAtTime(t);
                if (tloc == null) continue;
                int c = (int) Math.floor(tloc.x / configuration.cellWidth);
                int r = (int) Math.floor(tloc.y / configuration.cellWidth);
                if (!(r < 0 || c < 0 || r >= rows || c >= columns)) {
                    hm[r][c]++;
                } else {
//                    System.out.println("Time: " + t + "\tid: " + mobileNode.id);
                }
            }
            for (int i = 0; i < hm.length; i++) {
                for (int j = 0; j < hm[i].length; j++) {
                    hm[i][j] /= mobileNodes.size();
                }
            }
            result.add(hm);
            t += configuration.timeInterval;

        }
        return result;
    }

    private List<int[][]> cropHeatMaps(List<int[][]> heatMaps) {
        boolean[][] box = new boolean[heatMaps.get(0).length][heatMaps.get(0)[0].length];
        return null;
    }

    public List<TemporalLocation> createTemporalLocations(List<Double> doubles) {
        List<TemporalLocation> list = new ArrayList<>();

        for (int i = 0; i < doubles.size(); i += 3) {
            list.add(new TemporalLocation(doubles.get(i), doubles.get(i + 1), doubles.get(i + 2)));
        }
        return list;
    }

    public List<MobileNode> createMobileNodes(List<List<TemporalLocation>> input) {
        List<MobileNode> output = new ArrayList<>();

        for (int i = 0; i < input.size(); i++) {
            List<TemporalLocation> temporalLocations = input.get(i);
            MobileNode mn = new MobileNode(i);
            mn.setTemporalLocations(temporalLocations);
            output.add(mn);
        }
        return output;
    }

    public List<MobileNode> readMobilityFile(String fileName, Configuration config) {
        String path = "mobility_heatmaps/";
        String name = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.lastIndexOf("."));
        String[] split = fileName.split("/");
//        String outputFileName = split[split.length-1].split("\\.")[0]+".json";
        String outputFileName = path + name + ".json";
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {

            /*
              Read file line by line
              Split each line delimited by space
              parse the split to Double collect List<Double>
             */
            Stream<List<Double>> lineStream = stream.map(l -> Arrays.stream(l.split(" ")).
                    map(Double::parseDouble).
                    collect(Collectors.toList()));

            List<MobileNode> mobileNodes = createMobileNodes(lineStream.map(this::createTemporalLocations).collect(Collectors.toList()));
            Point2D[] box = findBoundingBox(mobileNodes);
            List<double[][]> heatMaps = createHeatMaps(mobileNodes, config);

            int[][] nonOccupiedCells = findNonOccupiedCells(heatMaps);
            TemporalHeatmapDto dto = new TemporalHeatmapDto(heatMaps, nonOccupiedCells, config);


            dto.printToFile(outputFileName);
//            printHeatmapsToFile("outputs/heatmaps/test.txt", heatMaps);
            System.out.println("Heatmaps are created: " + outputFileName);
//            Stream<Stream<Double>> listStream = stream.map(l -> Arrays.stream(l.split(" ")).
//                    map(Double::parseDouble));

//            Stream<Stream<Double>> streamStream = stream.map(s -> Arrays.asList(s.split(" ")).stream().map(Double::parseDouble));
//            streamStream.collect(Collectors.toList());
//            System.out.println();

        } catch (IOException e) {
            e.printStackTrace();
        }

//        list.forEach(System.out::println);

        return null;

    }

    private int[][] findNonOccupiedCells(List<double[][]> heatMaps) {
        int[][] grid = new int[heatMaps.get(0).length][heatMaps.get(0)[0].length];
        for (double[][] hm : heatMaps) {
            for (int j = 0; j < hm.length; j++) {
                for (int k = 0; k < hm[j].length; k++) {
                    grid[j][k] = hm[j][k] > 0 ? 1 : 0;
                }
            }
        }
        return grid;
    }

    /**
     * @param mobileNodes input
     * @return array of Point2D of size 2 where [0] is min(x,y) [1] is max(x, y)
     */
    private Point2D[] findBoundingBox(List<MobileNode> mobileNodes) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        for (MobileNode mn : mobileNodes) {
            List<TemporalLocation> temporalLocations = mn.getTemporalLocations();
            for (TemporalLocation tl : temporalLocations) {
                if (tl.x < minX)
                    minX = tl.x;
                if (tl.y < minY)
                    minY = tl.y;
                if (tl.x > maxX)
                    maxX = tl.x;
                if (tl.y > maxY)
                    maxY = tl.y;
            }
        }
        return new Point2D[]{new Point2D(minX, minY), new Point2D(maxX, maxY)};

    }

    private void printHeatmapsToFile(String filename, List<double[][]> heatMaps) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (int k = 0; k < heatMaps.size(); k++) {
            double[][] board = heatMaps.get(k);
            for (int i = 0; i < board.length; i++)//for each row
            {
                for (int j = 0; j < board.length; j++)//for each column
                {
                    builder.append(board[i][j]);//append to the output string
                    if (j < board.length - 1)//if this is not the last row element
                        builder.append(",");//then add comma (if you don't like commas you can use spaces)
                }
                builder.append("\n");//append new line at the end of the row
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
            writer.write(builder.toString());//save the string representation of the board
            if (k < heatMaps.size() - 1)
                writer.write("##\n");
            writer.close();
        }

    }

    /*
    public void loadHeatMaps() {
        try {
            temporalHeatmap = TemporalHeatmapDto.loadFromJsonFile("outputs/heatmaps/testjson2.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    public void loadSolver(int index) {
        String file = "outputs/heatmaps/res_ne" + (index + 1) + ".json";
        try {
            solverOutput = SolverOutputDto.loadFromJsonFile(file);
//            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        System.out.println();

    }

    public void loadSolverInput(String path, String filePrefix) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            solverInputDto = mapper.readValue(new File(path + filePrefix + ".json"),
                    new TypeReference<SolverInputDto>() {
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadSolverList(String path, String filePrefix) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        solverOutputDtoList =
                mapper.readValue(new File(path + filePrefix + "_out.json"), new TypeReference<List<SolverOutputDto>>() {
                });

        int numDrones = -1;
        for (SolverOutputDto solverOutputDto : solverOutputDtoList) {
            List<Drone> droneList = new ArrayList<>();
            int cw = solverOutputDto.getConfiguration().cellWidth;
            List<List<Integer>> drones = solverOutputDto.getDrones();

            // Deploy missing drones if any
            // Sometimes Optimization model deploys fewer than drones than available
            if (numDrones == -1) {
                // initialize
                numDrones = drones.size();
            } else {
                if (numDrones > drones.size()) {
                    int size = drones.size();
                    for (int a = 0; a < numDrones - size; a++) {
                        Coordinate cell = findPossibleCandidateCellsForDeployment(solverOutputDto.getHeatmap(), solverOutputDto.getCoverage(), solverOutputDto.getConfiguration(), drones);

                    }
                }
            }

            for (int j = 0; j < drones.size(); j++) {
                List<Integer> coord = drones.get(j);
                Drone d = new Drone(j);
                d.setX(cw / 2f + cw * coord.get(1));
                d.setY(cw / 2f + cw * coord.get(0));
                droneList.add(d);
            }


            NetworkUtils.calculateActorNeighborhoods(droneList
                    , solverOutputDto.getConfiguration().transmissionRange);
            ArrayList<Edge> edges = NetworkUtils.runKruskal(droneList);
            for (int i = 0; i < edges.size(); i++) {
                Edge edge = edges.get(i);
                edge.u.getEssentialLinkList().add(edge.v);
                edge.v.getEssentialLinkList().add(edge.u);
            }
            listofTimeIndexedDrones.add(droneList);
        }

    }

    private Coordinate findPossibleCandidateCellsForDeployment(List<List<Double>> heatmap, List<List<Integer>> coverage, Configuration configuration, List<List<Integer>> drones) {
        int maxY = heatmap.size() - 1;
        int maxX = heatmap.get(0).size() - 1;
        List<Coordinate> candidates = new ArrayList<>();
        for (int j = 0; j < drones.size(); j++) {
            List<Integer> coord = drones.get(j);
            int x = coord.get(1);
            int y = coord.get(0);

            for (int r = -1; r < 2; r++) {
                for (int c = -1; c < 2; c++) {
                    if (r == 0 && c == 0) continue;
                    int xp = x - c;
                    int yp = y - r;

                    if (xp >= 0 && xp <= maxX && yp >= 0 && yp <= maxY) {
                        candidates.add(new Coordinate(xp, yp));
                    }
                }
            }
        }
        for (int i = candidates.size() - 1; i >= 0; i--) {
            Coordinate coordinate = candidates.get(i);
            boolean remove = drones.stream().anyMatch(integers -> coordinate.x == integers.get(1) && coordinate.y == integers.get(0));
            if (remove) {
                candidates.remove(i);
            }
        }
        int maxGain = -1;
        int selected = -1;
        for (int i = 0; i < candidates.size(); i++) {
            Coordinate coordinate = candidates.get(i);
            int gain = 0;
            for (int r = -1; r < 2; r++) {
                for (int c = -1; c < 2; c++) {
                    int xp = coordinate.x - c;
                    int yp = coordinate.y - r;

                    if (xp >= 0 && xp <= maxX && yp >= 0 && yp <= maxY) {
                        boolean isCovered = coverage.stream().anyMatch(covered -> covered.get(1) == xp && covered.get(0) == yp);
                        if (!isCovered) {
                            gain += (int) (heatmap.get(yp).get(xp) * 1500);
//                            gain += (int) (heatmap.get(yp).get(xp) * getMobileNodes().size());
                        }
                    }
                }
            }
            if (gain > maxGain) {
                maxGain = gain;
                selected = i;
            }
        }
        if (selected == -1) System.exit(1);
        Coordinate cell = candidates.get(selected);
        drones.add(Arrays.asList(cell.y, cell.x));
        for (int r = -1; r < 2; r++) {
            for (int c = -1; c < 2; c++) {
                int xp = cell.x - c;
                int yp = cell.y - r;

                if (xp >= 0 && xp <= maxX && yp >= 0 && yp <= maxY) {
                    boolean isCovered = coverage.stream().anyMatch(covered -> covered.get(1) == xp && covered.get(0) == yp);
                    if (!isCovered) {
                        coverage.add(Arrays.asList(yp, xp));
                    }
                }
            }
        }


        return candidates.get(selected);
    }

    class Coordinate {
        public int x, y;

        public Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public void loadMobileNodes(String path, String filePrefix) {
//        String path = "inputs/mobility/";
        try (Stream<String> stream = Files.lines(Paths.get(path + filePrefix + ".movements"))) {

            /*
              Read file line by line
              Split each line delimited by space
              parse the split to Double collect List<Double>
             */
            Stream<List<Double>> lineStream = stream.map(l -> Arrays.stream(l.split(" ")).
                    map(Double::parseDouble).
                    collect(Collectors.toList()));

            List<List<TemporalLocation>> collect = lineStream.map(this::createTemporalLocations).collect(Collectors.toList());
            mobileNodes = createMobileNodes(collect);
        } catch (IOException ignored) {

        }
    }

//    public TemporalHeatmapDto getTemporalHeatmap() {
//        return temporalHeatmap;
//    }

    public SolverOutputDto getSolverOutput() {
        return solverOutput;
    }

    public List<SolverOutputDto> getSolverOutputDtoList() {
        return solverOutputDtoList;
    }

    public List<MobileNode> getMobileNodes() {
        return mobileNodes;
    }

    public static void main(String[] args) {
        String inputMovementFileName = "inputs/mobility/test3_1500.movements";
        DroneNet dn = new DroneNet();
        Configuration config = new Configuration(2200, 1800, 200, 285, 0, 600, 20);
        dn.readMobilityFile(inputMovementFileName, config);
//        dn.processSolverOutput();

//        dn.stableMatchingTest();

/*
        DroneNet droneNet = new DroneNet();

        String filePrefix = "test1_1500";
        droneNet.loadMobileNodes(filePrefix);
        droneNet.loadSolverList(filePrefix);
        List<SolverOutputDto> solverOutputDtoList = droneNet.getSolverOutputDtoList();
        DroneMatchingModel solver = new DroneMatchingModel();
        int timeIntervalMinute = solverOutputDtoList.get(0).getConfiguration().getTimeInterval();
        int transmissionRange = solverOutputDtoList.get(0).getConfiguration().getTransmissionRange();

        List<Velocity[]> velocityVectors = new ArrayList<>();
        for (int i = 0; i < droneNet.getListofTimeIndexedDrones().size() - 1; i++) {
            List<Gateway> A = droneNet.getListofTimeIndexedDrones().get(i);
            List<Gateway> B = droneNet.getListofTimeIndexedDrones().get(i + 1);
            if (A.size() != B.size()) {
                velocityVectors.add(null);
                continue;
            }
            Velocity[] velocities = new Velocity[A.size()];

            List<Gateway> matched = solver.createModel(A, B);
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


        droneNet.simulateDroneMovements(velocityVectors, timeIntervalMinute, transmissionRange);
        System.out.println();*/

    }


    public void stableMatchingTest() throws IOException {
        String path = "inputs/mobility/";
        String outputPath = "outputs/heatmaps/";
        String filePrefix = "test1_1500";
        loadMobileNodes(path, filePrefix);
        loadSolverList(outputPath, filePrefix);

        List<Drone> drones_t3 = listofTimeIndexedDrones.get(4);
        List<Drone> drones_t4 = listofTimeIndexedDrones.get(5);

//        Map<Integer, Integer> matching = foo(drones_t3, drones_t4);

//        for (int key : matching.keySet()) {
//            System.out.println(key);
//        }

//        Arrays.sort(source[0]);
//        System.out.println("Source");
//        print2DList(sourcePreferences);
//        System.out.println("Dest");
//        print2DList(destinationPreferences);

//        System.out.println();
    }


    void print2DList(List<List<Integer>> list) {
        for (int i = 0; i < list.size(); i++) {
            System.out.print(i + ": [");
            List<Integer> integers = list.get(i);
            for (Integer integer : integers) {
                System.out.print(integer + ", ");
            }
            System.out.println("]");
        }
    }




    public List<List<Drone>> getListofTimeIndexedDrones() {
        return listofTimeIndexedDrones;
    }

    public void fixOverlap() {
        for (int i = 0; i < listofTimeIndexedDrones.size(); i++) {
            List<Drone> drones = listofTimeIndexedDrones.get(i);
            List<List<Drone>> overlaps = new ArrayList<>();
            HashMap<java.awt.geom.Point2D, List<Drone>> map = new HashMap<>();

            for (Drone drone : drones) {
                List<Drone> list = map.getOrDefault(drone.getPoint2D(), new ArrayList<>());
                list.add(drone);
                map.put(drone.getPoint2D(), list);
            }

            List<java.awt.geom.Point2D> collect = map.entrySet()
                    .stream().filter(u -> u.getValue().size() > 1)
                    .map(Map.Entry::getKey).collect(Collectors.toList());
            for (java.awt.geom.Point2D key : collect) {
                overlaps.add(map.get(key));
            }
            if (!overlaps.isEmpty()) {
                List<List<Double>> heatmap = getSolverOutputDtoList().get(i).getHeatmap();
                int tr = getSolverOutputDtoList().get(i).getConfiguration().getTransmissionRange();
                List<Drone> toBeMoved = new ArrayList<>();
                for (List<Drone> overlap : overlaps) {
                    for (int k = 1; k < overlap.size(); k++) {
                        toBeMoved.add(overlap.get(k));
                    }
                }
                Set<java.awt.geom.Point2D> set = new HashSet<>(map.keySet());
                for (Drone d : toBeMoved) {
                    HeatMapCellCoordinate cell = getPossibleCell(set, heatmap);
                    set.add(cell.coordinate);
                    d.setLocation(cell.coordinate);
                }

//                List<HeatMapCellCoordinate> possibleCells = getPossibleCells(map, heatmap);
//
//
//                int nextLocation = 0;
//                for (java.awt.geom.Point2D key : collect) {
//                    List<Drone> droneList = map.get(key);
//                    for (int index = 1; index < droneList.size(); index++) {
//                        droneList.get(index).setLocation(possibleCells.get(nextLocation).coordinate);
//                    }
//                }
                NetworkUtils.calculateActorNeighborhoods(drones, tr);
                System.out.println();
            }
        }
    }

    private HeatMapCellCoordinate getPossibleCell(Set<java.awt.geom.Point2D> set, List<List<Double>> heatmap) {
        List<HeatMapCellCoordinate> possibleCells = new ArrayList<>();
        Set<java.awt.geom.Point2D> check = new HashSet<>();
        for (java.awt.geom.Point2D point2D : set) {
            for (int yy = -1; yy <= 1; yy++) {
                for (int xx = -1; xx <= 1; xx++) {
                    if (yy == 0 && xx == 0) continue;
                    double x = point2D.getX() + xx * 200;
                    double y = point2D.getY() + yy * 200;
                    if (x >= 0 && y >= 0) {
                        java.awt.geom.Point2D p = new java.awt.geom.Point2D.Double(x, y);
                        if (!set.contains(p)) {
                            int xcoord = (int) (x - 100) / 200;
                            int ycoord = (int) (y - 100) / 200;
                            double value = 0;
                            if (ycoord < heatmap.size() && xcoord < heatmap.get(ycoord).size())
                                value = heatmap.get(ycoord).get(xcoord);
                            else {
                                System.out.println("");
                            }
                            if (!check.contains(p)) {
                                possibleCells.add(new HeatMapCellCoordinate(value, p));
                                check.add(p);
                            }
                        }
                    }
                }
            }
        }
        Collections.sort(possibleCells);
        return possibleCells.get(0);
    }

    class HeatMapCellCoordinate implements Comparable<HeatMapCellCoordinate> {
        double value;
        java.awt.geom.Point2D coordinate;

        public HeatMapCellCoordinate(double value, java.awt.geom.Point2D coordinate) {
            this.value = value;
            this.coordinate = coordinate;
        }

        @Override
        public int compareTo(HeatMapCellCoordinate o) {
            return Double.compare(o.value, value);
        }
    }



    public SolverInputDto getSolverInputDto() {
        return solverInputDto;
    }
}
