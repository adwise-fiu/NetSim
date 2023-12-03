package dronenet;

import network.Gateway;
import utils.NetworkUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author fatihsenel
 * date: 21.06.22
 */
public class DeploymentDtoTest implements Comparable<DeploymentDtoTest>{
    public String mobilityModel;
    public int id;
    public int numberOfDrones;
    public int numberOfMobileNodes;
    public List<Double> coverage, averageNodeDegree, expectedPathLenth;
    public List<Integer> networkDiameter;
    public DroneNet droneNet;

    public DeploymentDtoTest(String mobilityModel, int id, int numberOfDrones, int numberOfMobileNodes) {
        this.droneNet = new DroneNet();
        this.mobilityModel = mobilityModel;
        this.id = id;
        this.numberOfDrones = numberOfDrones;
        coverage = new ArrayList<>();
        averageNodeDegree = new ArrayList<>();
        expectedPathLenth = new ArrayList<>();
        networkDiameter = new ArrayList<>();
        this.numberOfMobileNodes = numberOfMobileNodes;
    }

    public static DeploymentDtoTest getFromFileName(String path, String name) throws IOException {
//        "mslaw10_1500_5_out.json"
        String[] split = name.split("_");
        Pattern pattern = Pattern.compile("\\d");
        Matcher matcher = pattern.matcher(split[0]);
        String mobilityPath = "mobility_data/";
        int numberOfMobileNodes = Integer.parseInt(split[1]);

        if (matcher.find()) {
            int start = matcher.start();
            int id = Integer.parseInt(split[0].substring(start));
            String mobility = split[0].substring(0, start);
            int drones = Integer.parseInt(split[2]);
            DeploymentDtoTest dto = new DeploymentDtoTest(mobility, id, drones, numberOfMobileNodes);
//            String filePrefix = name.substring(0, name.indexOf("_out.json"));

//            dto.droneNet.loadMobileNodes(mobilityPath, filePrefix.substring(0, filePrefix.lastIndexOf("_")));

//            dto.droneNet.loadSolverList(path, filePrefix);
//            preprocess(dto);
//            calculateCoverage(dto);
//            topologyMetrics(dto);

            return dto;


        }

        return null;
    }



    private static void preprocess(DeploymentDtoTest dto) {

    }

    private static void topologyMetrics(DeploymentDtoTest dto) {
        for (int i = 0; i < dto.droneNet.getSolverOutputDtoList().size(); i++) {
            List<Drone> droneList = dto.droneNet.getListofTimeIndexedDrones().get(i);
            double and = 0, epl = 0;
            int diameter = 0;
            for (Gateway gateway : droneList) {
                and += gateway.getNeighborList().size();
            }
            and /= droneList.size();
            dto.averageNodeDegree.add(and);

            HashMap<String, Integer> costTable = NetworkUtils.FloydWarshall(droneList, new ArrayList<>());
            for (String key : costTable.keySet()) {
                epl += costTable.get(key);
                diameter = Math.max(diameter, costTable.get(key));
            }
            epl /= costTable.size();
            dto.expectedPathLenth.add(epl);
            dto.networkDiameter.add(diameter);
//            System.out.println();
        }
    }

    private static void calculateCoverage(DeploymentDtoTest dto) {
        for (int i = 0; i < dto.droneNet.getSolverOutputDtoList().size(); i++) {
            List<Drone> droneList = dto.droneNet.getListofTimeIndexedDrones().get(i);

            SolverOutputDto solverOutputDto = dto.droneNet.getSolverOutputDtoList().get(i);
            double transmissionRange = solverOutputDto.getConfiguration().getTransmissionRange();

            int timeInterval = solverOutputDto.getConfiguration().getTimeInterval();
            int count = 0;
//                for (int j = 0; j < droneNet.getMobileNodes().size(); j++) {
//                    MobileNode mobileNode = droneNet.getMobileNodes().get(j);
//                    TemporalLocation temporalLocation = mobileNode.getTemporalLocationAtTime(i * timeInterval);
//                    if (temporalLocation == null)
//                        continue;
//                    for (Gateway gateway : droneList) {
//                        if (AnalyticGeometry.euclideanDistance(temporalLocation.x, temporalLocation.y, gateway.getX(), gateway.getY()) <= transmissionRange) {
//                            count++;
//                            break;
//                        }
//                    }
//                }
            double sum = 0;
            for (int j = 0; j < solverOutputDto.getCoverage().size(); j++) {
                List<Integer> integers = solverOutputDto.getCoverage().get(j);
                Double aDouble = solverOutputDto.getHeatmap().get(integers.get(0)).get(integers.get(1));
//                sum += aDouble * dto.droneNet.getMobileNodes().size();
                sum += aDouble * 1500;
            }
            // total population 1500
//                coverage.add(count * 100d / droneNet.getMobileNodes().size());
//            double c = sum * 100 / dto.droneNet.getMobileNodes().size();
            double c = sum * 100 / 1500;
            dto.coverage.add(c);


        }
    }

    public double getAverageCoverage() {
        return coverage.stream().mapToDouble(d -> d).average().orElse(Double.NaN);
    }

    public double getAverageNodeDegree() {
        return averageNodeDegree.stream().mapToDouble(d -> d).average().orElse(Double.NaN);
    }

    public double getAverageEPL() {
        return expectedPathLenth.stream().mapToDouble(d -> d).average().orElse(Double.NaN);
    }

    public double getAverageNetworkDiameter() {
        return networkDiameter.stream().mapToDouble(d -> d).average().orElse(Double.NaN);
    }

    public String getMobilityModel() {
        return mobilityModel;
    }

    public void setMobilityModel(String mobilityModel) {
        this.mobilityModel = mobilityModel;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNumberOfDrones() {
        return numberOfDrones;
    }

    public void setNumberOfDrones(int numberOfDrones) {
        this.numberOfDrones = numberOfDrones;
    }

    public List<Double> getCoverage() {
        return coverage;
    }

    public void setCoverage(List<Double> coverage) {
        this.coverage = coverage;
    }


    @Override
    public String toString() {
        return "DeploymentDto{" +
                mobilityModel + '\'' +
                ", id=" + id +
                ", numberOfDrones=" + numberOfDrones +
                '}';
    }

    public static void main(String[] args) throws IOException {

        String solverOutputPathString = "experiment_data/";
        File solverOutputPath = new File(solverOutputPathString);
        String[] pathnames = solverOutputPath.list();
        // For each pathname in the pathnames array
        List<DeploymentDtoTest> list = new ArrayList<>();

        for (String pathname : pathnames) {
            // Print the names of files and directories
            if (pathname.endsWith(".json")) {
                DeploymentDtoTest fromFileName = getFromFileName(solverOutputPathString, pathname);
                list.add(fromFileName);
            }
        }

        for (int dr = 5; dr <= 10; dr++) {
            int finalDr = dr;
            List<DeploymentDtoTest> m = list.stream().filter(u -> u.numberOfDrones == finalDr).filter(u -> u.mobilityModel.equalsIgnoreCase("rwp")).sorted().collect(Collectors.toList());
            m.forEach(System.out::println);
        }





//        List<List<Double>> collect = list.stream().filter(u -> u.getId() == 5).map(DeploymentDto::getCoverage).collect(Collectors.toList());
//        System.out.println();


    }

    @Override
    public int compareTo(DeploymentDtoTest o) {
        return Integer.compare(this.id, o.id);

    }
}
