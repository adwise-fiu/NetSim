package dronenet;

import dronenet.matching.BottleneckAssignmentMatchingModel;
import dronenet.matching.MatchingModel;
import dronenet.matching.MinimizeTotalMovementMatchingModel;
import geometry.AnalyticGeometry;
import utils.NetworkUtils;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.Constants.maxDroneSpeed;

/**
 * @author fatihsenel
 * date: 19.03.24
 */
public class ContractionExpansion {
    public static final String BOTTLENECK = "Bottleneck";
    public static final String MIN_SUM = "min_sum";
    private static final String FILE_NAME = "Experiments.csv";
    static String solverOutputPathString = "experiment_data/";
    DeploymentDto dto;
    int transmissionRange;

    Point2D randomContractionPoint;

    public ContractionExpansion(int transmissionRange) {
        this.transmissionRange = transmissionRange;
    }

    public ContractionExpansion(String pathname, String matching) throws IOException {
        dto = DeploymentDto.getFromFileName(solverOutputPathString, pathname);
        transmissionRange = dto.droneNet.getSolverOutputDtoList().get(0).getConfiguration().getTransmissionRange();
        if (matching.equals(BOTTLENECK)) {
            selectMatchingModel(new BottleneckAssignmentMatchingModel());
        } else if (matching.equalsIgnoreCase(MIN_SUM)) {
            selectMatchingModel(new MinimizeTotalMovementMatchingModel());
        }
    }

    public void selectMatchingModel(MatchingModel solver) {
        List<List<Drone>> listOfNetworks = dto.droneNet.getListofTimeIndexedDrones();


        for (int i = 0; i < listOfNetworks.size() - 1; i++) {
            List<Drone> A = listOfNetworks.get(i);
            List<Drone> B = listOfNetworks.get(i + 1);
            if (A.size() != B.size()) {
                continue;
            }
            List<Drone> matched = solver.doMatching(A, B);
            listOfNetworks.set(i + 1, matched);
            if (matched == null || A.size() != matched.size()) {
                continue;
            }
            NetworkUtils.calculateActorNeighborhoods(matched
                    , transmissionRange);
            listOfNetworks.set(i + 1, matched);
        }
    }

    public static void main(String[] args) throws IOException {
        createFile(FILE_NAME);
        List<String> fileNames = readFileNames(args);
        if (fileNames == null) return;
//        processContraction(fileNames);
        // optimal bottleneck
        /*
         * Optional.empty(): find optimal alpha
         * Optional.of(0) no contraction - pure bottleneck
         * Opional.of(1) full contraction
         */
        processContractionExpansion(fileNames, BOTTLENECK, Optional.empty());
        processContractionExpansion(fileNames, BOTTLENECK, Optional.of(0d));
        processContractionExpansion(fileNames, BOTTLENECK, Optional.of(1d));

        processContractionExpansion(fileNames, MIN_SUM, Optional.empty());
        processContractionExpansion(fileNames, MIN_SUM, Optional.of(0d));
        processContractionExpansion(fileNames, MIN_SUM, Optional.of(1d));
//        dronenet.ContractionExpansion contractionExpansion = new dronenet.ContractionExpansion();

    }

    private static void processAlphaContraction(List<String> fileNames, String matchingModel, double alpha) throws IOException {

    }

    private static void processContractionExpansion(List<String> fileNames, String matchingModel, Optional<Double> alpha) throws IOException {
//        double totalSum = 0;
//        int count = 0;
        for (String filename : fileNames) {
            ContractionExpansion contractionExpansion = new ContractionExpansion(filename, matchingModel);
            List<List<Drone>> listOfNetworks = contractionExpansion.dto.droneNet.getListofTimeIndexedDrones();
            System.out.println(filename + " - " + matchingModel + " - " + (alpha.isPresent() ? alpha.get() : "optimal"));
            for (int i = 0; i < listOfNetworks.size() - 1; i++) {
                ArrayList<Drone> source = new ArrayList<>();
                ArrayList<Drone> destination = new ArrayList<>();

                for (int j = 0; j < listOfNetworks.get(i).size(); j++) {
                    Drone d1 = new Drone(j);
                    d1.setLocation(listOfNetworks.get(i).get(j).getPoint2D());
                    source.add(d1);

                    Drone d2 = new Drone(j);
                    d2.setLocation(listOfNetworks.get(i + 1).get(j).getPoint2D());
                    destination.add(d2);
                }
//                double similarity = topologySimilarity(source, destination);
//                if(similarity > 0.2) {
//                    continue;
//                }
                AlphaContractionDto alphaReachability;
                if (alpha.isPresent()) {
                    alphaReachability = contractionExpansion.alphaContraction(source, destination, matchingModel, alpha.get());
                } else {
                    alphaReachability = contractionExpansion.optimalContraction(source, destination, matchingModel);
                }
                if (alphaReachability != null) {
                    double maxTravelDistance = maxTravelDistance(source, destination, alphaReachability.contractionPoint, alphaReachability.alpha);
                    String instanceName = getInstanceName(filename, i);
                    String method = alpha.isPresent() ? "Fixed" : "Optimal";
                    dump(FILE_NAME, instanceName, matchingModel, source.size(), method, alphaReachability, maxTravelDistance);
                    System.out.printf("Progress: %d/%d\r", i, listOfNetworks.size() - 1);
//                    System.out.print("\rProgress: " + );

                }
            }
        }
    }


    private static void processContraction(List<String> fileNames) throws IOException {
        double totalSum = 0;
        int count = 0;
        for (String filename : fileNames) {
            ContractionExpansion contractionExpansion = new ContractionExpansion(filename, BOTTLENECK);
            List<List<Drone>> listOfNetworks = contractionExpansion.dto.droneNet.getListofTimeIndexedDrones();
            for (int i = 0; i < listOfNetworks.size() - 1; i++) {
                ArrayList<Drone> source = new ArrayList<>();
                ArrayList<Drone> destination = new ArrayList<>();

                for (int j = 0; j < listOfNetworks.get(i).size(); j++) {
                    Drone d1 = new Drone(j);
                    d1.setLocation(listOfNetworks.get(i).get(j).getPoint2D());
                    source.add(d1);

                    Drone d2 = new Drone(j);
                    d2.setLocation(listOfNetworks.get(i + 1).get(j).getPoint2D());
                    destination.add(d2);
                }
                double time = contractionExpansion.contract(source, destination);
                totalSum += time;
                count++;
            }
        }
        System.out.println(totalSum / count);
        System.out.println(count);
    }


    public static List<String> readFileNames(String[] args) {
        File solverOutputPath = new File(solverOutputPathString);
        String[] pathnames = solverOutputPath.list();
        if (pathnames == null) return null;
        Stream<String> pathnameListStream = Arrays.stream(pathnames)
                .filter(u -> u.endsWith("json") && u.startsWith("mslaw"))
                .sorted(Comparator.comparingInt(a -> Integer.parseInt(a.split("_")[2])));
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

        List<String> pathnameList = pathnameListStream.collect(Collectors.toList());
        Collections.sort(pathnameList, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                String[] s1 = o1.split("_");
                String[] s2 = o2.split("_");

                int d1 = Integer.parseInt(s1[2]);
                int d2 = Integer.parseInt(s2[2]);

                int id1 = extractId(s1[0]);
                int id2 = extractId(s2[0]);

                if (d1 == d2) return Integer.compare(id1, id2);

                return Integer.compare(d1, d2);
            }
        });
        System.out.println("***********\nFile To be executed (size=" + pathnameList.size() + ")");
        for (String s : pathnameList) {
            System.out.println(s);
        }
        System.out.println("***********");
        return pathnameList;
    }

    public double contract(ArrayList<Drone> source, ArrayList<Drone> destination) {
        Point2D sourceCenter = computeContractionPoint(source);
        int transmission = 285;

        double maxDistance = 0;
        double time = 0;
        // calculate time required for contraction
        for (Drone d : source) {
            double distance = AnalyticGeometry.euclideanDistance(d.getPoint2D(), sourceCenter);
            maxDistance = Math.max(maxDistance, distance);
        }
        // time in terms of seconds
        time = maxDistance / maxDroneSpeed;

        // contract
        while (true) {
            boolean done = true;

            for (Drone d : source) {
                Point2D p = d.getPoint2D();
                if (!p.equals(sourceCenter)) {
                    done = false;
                    Point2D coordinates = AnalyticGeometry.getCoordinates(p.getX(), p.getY(), sourceCenter.getX(), sourceCenter.getY(), maxDistance);
                    d.setLocation(coordinates);
                }
            }

            NetworkUtils.calculateActorNeighborhoods(source, transmission);
            boolean connected = NetworkUtils.isConnected(source);
            if (!connected) System.out.println("CONTRACTION WARNING!!!!!!!!!");
            if (done) break;
        }

        // expand
        while (true) {
            boolean done = true;

            for (int i = 0; i < source.size(); i++) {
                Drone d = source.get(i);
                Point2D destLocation = destination.get(i).getPoint2D();
                Point2D p = d.getPoint2D();
                if (!p.equals(destLocation)) {
                    done = false;
                    Point2D coordinates = AnalyticGeometry.getCoordinates(p.getX(), p.getY(), destLocation.getX(), destLocation.getY(), maxDistance);
                    d.setLocation(coordinates);
                }
            }
            NetworkUtils.calculateActorNeighborhoods(source, transmission);
            boolean connected = NetworkUtils.isConnected(source);
            if (!connected) System.out.println("EXPANSION WARNING!!!!!!!!!");
            if (done) break;
        }

        return time;

    }

    public void setRandomContractionPoint(int width, int height) {
        Random random = new Random();
        double x = random.nextDouble() * width;
        double y = random.nextDouble() * height;
        randomContractionPoint = new Point2D.Double(x, y);
    }

    public Point2D computeContractionPoint(List<Drone> list) {
//        return centerOfMass(list);
        return randomContractionPoint;
    }

    public Point2D centerOfMass(List<Drone> list) {
        double x = 0, y = 0;
        for (Drone d : list) {
            x += d.getX();
            y += d.getY();
        }
        return new Point2D.Double(x / list.size(), y / list.size());
    }

    public Point2D randomContractionPoint(List<Drone> list) {
        double x = 0, y = 0;
        for (Drone d : list) {
            x += d.getX();
            y += d.getY();
        }
        return new Point2D.Double(x / list.size(), y / list.size());
    }


    /**
     * used by gui
     *
     * @param alpha alpha
     * @return
     */
    public List<AlphaContractionDto> processBottleneck(double alpha) {
        List<List<Drone>> listOfNetworks = dto.droneNet.getListofTimeIndexedDrones();
        List<AlphaContractionDto> ans = new ArrayList<>();

        for (int i = 0; i < listOfNetworks.size() - 1; i++) {
            AlphaContractionDto alphaContractionDto = alphaContraction(listOfNetworks.get(i), listOfNetworks.get(i + 1), BOTTLENECK, alpha);
            ans.add(alphaContractionDto);
        }
        return ans;
    }

    /**
     * used by gui
     *
     * @return
     */
    public List<AlphaContractionDto> processBottleneckWithAlphaContraction() {
        List<List<Drone>> listOfNetworks = dto.droneNet.getListofTimeIndexedDrones();
        List<AlphaContractionDto> ans = new ArrayList<>();

        for (int i = 0; i < listOfNetworks.size() - 1; i++) {
            AlphaContractionDto alphaContractionDto = optimalContraction(listOfNetworks.get(i), listOfNetworks.get(i + 1), BOTTLENECK);
            ans.add(alphaContractionDto);
        }
        return ans;
    }

    public AlphaContractionDto alphaContraction(List<Drone> source, List<Drone> destination, String matchingModelName, double alpha) {
        Point2D contractionPoint = computeContractionPoint(source);
        List<Drone> matched = doMatching(source, destination, matchingModelName);
        return alphaContractAndExpand(source, matched, contractionPoint, alpha);
    }

    public AlphaContractionDto optimalContraction(List<Drone> source, List<Drone> destination, String matchingModelName) {
        double alpha = 0;
        AlphaContractionDto alphaReachability = null;
        Point2D contractionPoint = computeContractionPoint(source);
        List<Drone> matched = doMatching(source, destination, matchingModelName);

        while (alpha <= 1) {
            AlphaContractionDto ar = alphaContractAndExpand(source, matched, contractionPoint, alpha);
            if (alphaReachability == null) {
                alphaReachability = ar;
            } else {
                if (alphaReachability.disconnectedMoments > ar.disconnectedMoments) {
                    alphaReachability = ar;
                } else {
                    if (alphaReachability.reachability.size() > ar.reachability.size()) {
                        alphaReachability = ar;
                    }
                }
            }
            alpha += 0.05;
        }

        return alphaReachability;
    }

    private List<Drone> doMatching(List<Drone> source, List<Drone> destination, String matchingModelName) {
        MatchingModel matchingModel;
        if (matchingModelName == null || !matchingModelName.equals(MIN_SUM)) {
            matchingModel = new MinimizeTotalMovementMatchingModel();
        } else {
            matchingModel = new BottleneckAssignmentMatchingModel();
        }
        List<Drone> matched = matchingModel.doMatching(source, destination);
        NetworkUtils.calculateActorNeighborhoods(matched, transmissionRange);
        return matched;
    }

    private AlphaContractionDto alphaContractAndExpand(List<Drone> source,
                                                       List<Drone> destination,
                                                       Point2D contractionPoint,
                                                       double alpha) {
        AlphaContractionDto alphaReachability = null;
        List<Drone> cpySource = copyList(source);
        List<Drone> cpyDestination = copyList(destination);

        List<Double> reachability = alphaContractAndExpand(cpySource, alpha, contractionPoint);
        moveToDestination(cpySource, cpyDestination, reachability);

        alphaReachability = new AlphaContractionDto(alpha, reachability);
        alphaReachability.contractionPoint = contractionPoint;
        alphaReachability.maxTravelDistance = maxTravelDistance(source, destination, contractionPoint, alpha);
        return alphaReachability;
    }

    private void moveToDestination(List<Drone> source, List<Drone> destination, List<Double> reachability) {
        while (true) {
            boolean done = true;

            for (int i = 0; i < source.size(); i++) {
                Drone drone = source.get(i);
                Point2D p = destination.get(i).getPoint2D();
                if (!drone.getPoint2D().equals(p)) {
                    done = false;
                    Point2D coordinates = AnalyticGeometry.getCoordinates(drone.getPoint2D(), p, maxDroneSpeed);
                    drone.setLocation(coordinates);
                }
            }
            NetworkUtils.calculateActorNeighborhoods(source, transmissionRange);
            double v = NetworkUtils.calculateConnectivityMeasure(source, transmissionRange);
            reachability.add(v);
            if (done) break;
        }
    }

    private List<Double> alphaContractAndExpand(List<Drone> list, double alpha, Point2D contractionPoint) {
        Point2D[] points = new Point2D[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Drone drone = list.get(i);
            double distance = AnalyticGeometry.euclideanDistance(drone.getPoint2D(), contractionPoint);
            points[i] = AnalyticGeometry.getCoordinates(drone.getPoint2D(), contractionPoint, distance * alpha);
        }
        List<Double> reachability = new ArrayList<>();
        while (true) {
            boolean done = true;
            for (int i = 0; i < list.size(); i++) {
                Drone drone = list.get(i);
                Point2D p = points[i];
                if (!drone.getPoint2D().equals(p)) {
                    Point2D coordinates = AnalyticGeometry.getCoordinates(drone.getPoint2D(), p, maxDroneSpeed);
                    drone.setLocation(coordinates);
                    done = false;
                }
            }
            NetworkUtils.calculateActorNeighborhoods(list, transmissionRange);
            double v = NetworkUtils.calculateConnectivityMeasure(list, transmissionRange);
            reachability.add(v);
            if (done) break;
        }
        return reachability;
    }

    private List<Drone> copyList(List<Drone> list) {
        List<Drone> copy = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Drone drone = list.get(i);
            Drone nd = new Drone(i);
            nd.setLocation(drone.getPoint2D());
            copy.add(nd);
        }
        return copy;
    }

    private static String getInstanceName(String filename, int i) {
        // "mslaw2_1500_10_out"
        String[] split = filename.split("_");
        String name = addDash(split[0]);
        name += "-" + i;
        return name;
    }

    /**
     * Add a dash in between the text the number at the end of text
     * For example if text = "mslaw12" add dash between mslaw and 12 and return mslaw-12
     *
     * @param input text
     * @return dash added value
     */
    private static String addDash(String input) {
        String[] parts = input.split("(?<=\\D)(?=\\d)");
        if (parts.length == 2) {
            return parts[0] + "-" + parts[1];
        } else {
            return input;
        }
    }

    private static int extractId(String text) {
        // Define the pattern to match digits at the end of the string
        Pattern pattern = Pattern.compile("\\d+$");
        Matcher matcher = pattern.matcher(text);

        // Check if the pattern is found
        if (matcher.find()) {
            // Extract and parse the matched digits as an integer
            return Integer.parseInt(matcher.group());
        } else {
            // Return a default value or handle the case when no number is found
            return -1; // For example, return -1 if no number is found
        }
    }

    public static double maxTravelDistance(List<Drone> source, List<Drone> destination, Point2D contraction,
                                           double alpha) {
        double max = 0;
        for (int i = 0; i < source.size(); i++) {
            Drone u = source.get(i);
            Drone v = destination.get(i);
            double flightDistance;
            if (alpha > 0) {
                flightDistance = AnalyticGeometry.euclideanDistance(u.getPoint2D(), contraction) * alpha;
                Point2D coordinates = AnalyticGeometry.getCoordinates(u.getPoint2D(), contraction, flightDistance);
                flightDistance += AnalyticGeometry.euclideanDistance(coordinates, v.getPoint2D());
            } else {
                flightDistance = AnalyticGeometry.euclideanDistance(u.getPoint2D(), v.getPoint2D());
            }
            max = Math.max(max, flightDistance);
        }
        return max;
    }

    private static void createFile(String filename) {
        try {
            // Open the file in append mode
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

            // Append a new line with comma-separated values
            writer.write("instance-id-time,matchingModel,numberOfDrones,method,alpha,distance,duration,disconnectedMoments\n");
            // Close the writer
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void dump(String filename, String instanceName, String matchingModel, int numberOfDrones, String method,
                             AlphaContractionDto dto, double distance) {
        double alpha = dto.alpha;
        double duration = dto.reachability.size();
        double disconnectedMoments = dto.disconnectedMoments;
        try {
            // Open the file in append mode
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));

            // Append a new line with comma-separated values
            writer.write(instanceName + "," + matchingModel + "," + numberOfDrones + "," + method + "," +
                    alpha + "," + distance + "," + duration + "," + disconnectedMoments + "\n");

            // Close the writer
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    class TimeReachability {
        double time;
        List<Double> reachability;

        public TimeReachability(double time, List<Double> reachability) {
            this.time = time;
            this.reachability = reachability;
        }
    }

    private static double topologySimilarity(List<Drone> A, List<Drone> B) {
        Set<Point2D> aSet = new HashSet<>();
        Set<Point2D> bSet = new HashSet<>();
        for (int i = 0; i < A.size(); i++) {
            aSet.add(A.get(i).getPoint2D());
            bSet.add(B.get(i).getPoint2D());
        }
        aSet.retainAll(bSet);
        return (1d * aSet.size()) / A.size();
    }
}
