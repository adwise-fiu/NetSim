package dronenet;

import network.Gateway;
import utils.NetworkUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author fatihsenel
 * date: 21.06.22
 */
public class DeploymentDto {
    public String mobilityModel;
    public int id;
    public int numberOfDrones;
    public int numberOfMobileNodes;
    public List<Double> coverage, averageNodeDegree, expectedPathLenth;
    List<SolutionDetails> details;
    public List<Integer> networkDiameter;
    public DroneNet droneNet;

    public DeploymentDtoStatistics stats = null;

    public DeploymentDto(String mobilityModel, int id, int numberOfDrones, int numberOfMobileNodes) {
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

    public static DeploymentDto getFromFileName(String path, String name) throws IOException {
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
            DeploymentDto dto = new DeploymentDto(mobility, id, drones, numberOfMobileNodes);
            String filePrefix = name.substring(0, name.indexOf("_out.json"));

//            dto.droneNet.loadMobileNodes(mobilityPath, filePrefix.substring(0, filePrefix.lastIndexOf("_")));

            dto.droneNet.loadSolverList(path, filePrefix);
            String userMobilityFilePrefix = mobility + id + "_" + numberOfMobileNodes;
            dto.droneNet.loadMobileNodes(mobilityPath, userMobilityFilePrefix);
            preprocess(dto);
            calculateCoverage(dto);
            topologyMetrics(dto);
            dto.details = dto.droneNet.getSolverOutputDtoList().stream().map(SolverOutputDto::getDetails).collect(Collectors.toList());
            return dto;


        }

        return null;
    }

    private static void preprocess(DeploymentDto dto) {

    }

    private static void topologyMetrics(DeploymentDto dto) {
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

    private static void calculateCoverage(DeploymentDto dto) {
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

    public double getAverageSolverTime() {
        return details.stream().mapToDouble(u -> u.time).average().orElse(Double.NaN);
    }

    public double getAverageMipRelativeGap() {
        return details.stream().mapToDouble(u -> u.mipRelativeGap).average().orElse(Double.NaN);
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
        List<DeploymentDto> list = new ArrayList<>();

        for (String pathname : pathnames) {
            // Print the names of files and directories
            if (pathname.endsWith(".json")) {
                DeploymentDto fromFileName = getFromFileName(solverOutputPathString, pathname);
                list.add(fromFileName);
                System.out.println(pathname);

            }
        }

        for (DeploymentDto deploymentDto : list) {
            double timeLimitPercent;
            double avgCoverageWhenTimeLimit = 0, avgCoverageWhenOptimal = 0, avgTimeWhenOptimal = 0, avgMipGapWhenTimeLimit = 0;
            int countWhenTimeLimit = 0;
            int countWhenOptimal = 0;
            for (int j = 0; j < deploymentDto.details.size(); j++) {
                SolutionDetails solutionDetails = deploymentDto.details.get(j);
                if (solutionDetails.status.contains("optimal")) {
                    avgCoverageWhenOptimal += deploymentDto.coverage.get(j);
                    avgTimeWhenOptimal += solutionDetails.time;
                    countWhenOptimal++;
                } else {
//                    if (solutionDetails.mipRelativeGap > 0.05) {
                        countWhenTimeLimit++;
                        avgCoverageWhenTimeLimit += deploymentDto.coverage.get(j);
                        avgMipGapWhenTimeLimit += 100-100*solutionDetails.mipRelativeGap;
//                    }
                }
            }

            timeLimitPercent = 100.0 * countWhenTimeLimit / deploymentDto.details.size();
            avgCoverageWhenOptimal = countWhenOptimal != 0 ? avgCoverageWhenOptimal / countWhenOptimal : 0;
            avgTimeWhenOptimal = countWhenOptimal != 0 ? avgTimeWhenOptimal / countWhenOptimal : 0;
            avgCoverageWhenTimeLimit = countWhenTimeLimit != 0 ? avgCoverageWhenTimeLimit / countWhenTimeLimit : 0;
            avgMipGapWhenTimeLimit = countWhenTimeLimit != 0 ? avgMipGapWhenTimeLimit / countWhenTimeLimit : 0;
            avgMipGapWhenTimeLimit = avgMipGapWhenTimeLimit < 0.05 ? 0 : avgMipGapWhenTimeLimit;
            deploymentDto.stats = new DeploymentDtoStatistics(timeLimitPercent, avgCoverageWhenOptimal, avgTimeWhenOptimal, avgCoverageWhenTimeLimit, avgMipGapWhenTimeLimit);
        }


//        PrintWriter pwCov = new PrintWriter(new FileWriter("coverage2.csv"));
//        PrintWriter pwAND = new PrintWriter(new FileWriter("and2.csv"));
//        PrintWriter pwEPL = new PrintWriter(new FileWriter("epl2.csv"));
//        PrintWriter pwDiameter = new PrintWriter(new FileWriter("diameter2.csv"));
        PrintWriter pwTime = new PrintWriter(new FileWriter("time2.csv"));
        PrintWriter pwGap = new PrintWriter(new FileWriter("gap2.csv"));
        PrintWriter pwStat = new PrintWriter(new FileWriter("stats.csv"));
//        pwCov.println("Drones,MSLAW,RWP");
//        pwAND.println("Drones,MSLAW,RWP");
//        pwEPL.println("Drones,MSLAW,RWP");
//        pwDiameter.println("Drones,MSLAW,RWP");
        pwTime.println("Drones,MSLAW,RWP");
        pwGap.println("Drones,MSLAW,RWP");
        pwStat.println("Drones,MSLAW-TimeLimitPercent,RWP-TimeLimitPercent,MSLAW-Coverage-When-Optimal,RWP-Coverage-When-Optimal,MSLAW-Time-WhenOptimal,RWP-Time-WhenOptimal,MSLAW-Coverage-When-TL,RWP-Coverage-When-TL,MSLAW-Gap-When-TL,RWP-Gap-When-TL");

        DecimalFormat df = new DecimalFormat("#.00");
        System.out.println("\tmslaw\trwp");
        for (int i = 5; i <= 10; i++) {
            final int d = i;
            List<DeploymentDto> mslaw = list.stream().filter(u -> u.numberOfDrones == d).filter(u -> u.mobilityModel.equalsIgnoreCase("mslaw")).collect(Collectors.toList());
            List<DeploymentDto> rwp = list.stream().filter(u -> u.numberOfDrones == d).filter(u -> u.mobilityModel.equalsIgnoreCase("rwp")).collect(Collectors.toList());

//            append(pwCov, d, DeploymentDto::getAverageCoverage, mslaw, rwp);
//            append(pwAND, d, DeploymentDto::getAverageNodeDegree, mslaw, rwp);
//            append(pwEPL, d, DeploymentDto::getAverageEPL, mslaw, rwp);
//            append(pwDiameter, d, DeploymentDto::getAverageNetworkDiameter, mslaw, rwp);
            append(pwTime, d, DeploymentDto::getAverageSolverTime, mslaw, rwp);
            append(pwGap, d, DeploymentDto::getAverageMipRelativeGap, mslaw, rwp);
            appendStat(pwStat, d, mslaw, rwp);

        }

//        pwCov.close();
//        pwAND.close();
//        pwEPL.close();
//        pwDiameter.close();
        pwStat.close();
//        List<List<Double>> collect = list.stream().filter(u -> u.getId() == 5).map(DeploymentDto::getCoverage).collect(Collectors.toList());
//        System.out.println();

        List<DeploymentDto> mslaw5 = list.stream().filter(u -> u.numberOfDrones == 5).filter(u -> u.mobilityModel.equalsIgnoreCase("mslaw")).collect(Collectors.toList());
        List<DeploymentDto> mslaw7 = list.stream().filter(u -> u.numberOfDrones == 7).filter(u -> u.mobilityModel.equalsIgnoreCase("mslaw")).collect(Collectors.toList());
        List<DeploymentDto> mslaw10 = list.stream().filter(u -> u.numberOfDrones == 10).filter(u -> u.mobilityModel.equalsIgnoreCase("mslaw")).collect(Collectors.toList());

        List<DeploymentDto> rwp5 = list.stream().filter(u -> u.numberOfDrones == 5).filter(u -> u.mobilityModel.equalsIgnoreCase("rwp")).collect(Collectors.toList());
        List<DeploymentDto> rwp7 = list.stream().filter(u -> u.numberOfDrones == 7).filter(u -> u.mobilityModel.equalsIgnoreCase("rwp")).collect(Collectors.toList());
        List<DeploymentDto> rwp10 = list.stream().filter(u -> u.numberOfDrones == 10).filter(u -> u.mobilityModel.equalsIgnoreCase("rwp")).collect(Collectors.toList());


        printTemporal("coverage", temporalCoverage(mslaw5), temporalCoverage(mslaw7), temporalCoverage(mslaw10), temporalCoverage(rwp5), temporalCoverage(rwp7), temporalCoverage(rwp10));
        printTemporal("and", temporalAnd(mslaw5), temporalAnd(mslaw7), temporalAnd(mslaw10), temporalAnd(rwp5), temporalAnd(rwp7), temporalAnd(rwp10));
        printTemporal("epl", temporalEpl(mslaw5), temporalEpl(mslaw7), temporalEpl(mslaw10), temporalEpl(rwp5), temporalEpl(rwp7), temporalEpl(rwp10));
        printTemporal("diameter", temporalDiameter(mslaw5), temporalDiameter(mslaw7), temporalDiameter(mslaw10), temporalDiameter(rwp5), temporalDiameter(rwp7), temporalDiameter(rwp10));

    }


    public static void printTemporal(String title, double[] mslaw5, double[] mslaw7, double[] mslaw10, double[] rwp5, double[] rwp7, double[] rwp10) throws IOException {
        PrintWriter pwCov = new PrintWriter(new FileWriter(title + "Temporal.csv"));

        pwCov.println("Time,MSLAW (# drones=5),MSLAW (# drones=7),MSLAW (# drones=10),RWP (# drones=5),RWP (# drones=7),RWP (# drones=10)");
        for (int i = 0; i < mslaw5.length; i++) {
            pwCov.println(i + "," + mslaw5[i] + "," + mslaw7[i] + "," + mslaw10[i] + "," + rwp5[i] + "," + rwp7[i] + "," + rwp10[i]);
        }
        pwCov.close();
    }

    public static double[] temporalCoverage(List<DeploymentDto> list) {
        double[] result = new double[list.get(0).coverage.size()];
//        dto.coverage
        for (DeploymentDto dto : list) {
            for (int j = 0; j < dto.coverage.size(); j++) {
                result[j] += dto.coverage.get(j);
            }
        }
        for (int i = 0; i < result.length; i++) {
            result[i] /= list.size();
        }
        return result;
    }

    public static double[] temporalAnd(List<DeploymentDto> list) {
        double[] result = new double[list.get(0).averageNodeDegree.size()];
//        dto.averageNodeDegree
        for (DeploymentDto dto : list) {
            for (int j = 0; j < dto.averageNodeDegree.size(); j++) {
                result[j] += dto.averageNodeDegree.get(j);
            }
        }
        for (int i = 0; i < result.length; i++) {
            result[i] /= list.size();
        }
        return result;
    }

    public static double[] temporalDiameter(List<DeploymentDto> list) {
        double[] result = new double[list.get(0).networkDiameter.size()];
        for (DeploymentDto dto : list) {
            for (int j = 0; j < dto.networkDiameter.size(); j++) {
                result[j] += dto.networkDiameter.get(j);
            }
        }
        for (int i = 0; i < result.length; i++) {
            result[i] /= list.size();
        }
        return result;
    }

    public static double[] temporalEpl(List<DeploymentDto> list) {
        double[] result = new double[list.get(0).expectedPathLenth.size()];
        for (DeploymentDto dto : list) {
            for (int j = 0; j < dto.expectedPathLenth.size(); j++) {
                result[j] += dto.expectedPathLenth.get(j);
            }
        }
        for (int i = 0; i < result.length; i++) {
            result[i] /= list.size();
        }
        return result;
    }

    public static void append(PrintWriter pw, int d, ToDoubleFunction<DeploymentDto> func, List<DeploymentDto> mslaw, List<DeploymentDto> rwp) {
        double m = mslaw.stream().mapToDouble(func).average().orElse(Double.NaN);
        double r = rwp.stream().mapToDouble(func).average().orElse(Double.NaN);
        pw.println(d + "," + m + "," + r);
    }

    private static void appendStat(PrintWriter pwStat, int d, List<DeploymentDto> mslaw, List<DeploymentDto> rwp) {
        double a1 = mslaw.stream().mapToDouble(u -> u.stats.timeLimitPercent).average().orElse(Double.NaN);
        double a2 = rwp.stream().mapToDouble(u -> u.stats.timeLimitPercent).average().orElse(Double.NaN);
        double a3 = mslaw.stream().mapToDouble(u -> u.stats.avgCoverageWhenOptimal).average().orElse(Double.NaN);
        double a4 = rwp.stream().mapToDouble(u -> u.stats.avgCoverageWhenOptimal).average().orElse(Double.NaN);

        double a5 = mslaw.stream().mapToDouble(u -> u.stats.avgTimeWhenOptimal).average().orElse(Double.NaN);
        double a6 = rwp.stream().mapToDouble(u -> u.stats.avgTimeWhenOptimal).average().orElse(Double.NaN);

        double a7 = mslaw.stream().mapToDouble(u -> u.stats.avgCoverageWhenTimeLimit).average().orElse(Double.NaN);
        double a8 = rwp.stream().mapToDouble(u -> u.stats.avgCoverageWhenTimeLimit).average().orElse(Double.NaN);

        double a9 = mslaw.stream().filter(u->u.stats.timeLimitPercent>0).mapToDouble(u -> u.stats.avgMipGapWhenTimeLimit).average().orElse(Double.NaN);
        double a10 = rwp.stream().filter(u->u.stats.timeLimitPercent>0).mapToDouble(u -> u.stats.avgMipGapWhenTimeLimit).average().orElse(Double.NaN);
        pwStat.println(d + "," + a1 + "," + a2 + "," + a3 + "," + a4 + "," + a5 + "," + a6 + "," + a7 + "," + a8 + "," + a9 + "," + a10);
    }
}
