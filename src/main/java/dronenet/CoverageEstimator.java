package dronenet;

import dronenet.matching.BottleneckAssignmentMatchingModel;
import utils.NetworkUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fatihsenel
 * date: 14.06.22
 */
public class CoverageEstimator {
    public static void main(String[] args) throws IOException {
        String mobilityPathString = "mobility_data/";
        File mobilityPath = new File(mobilityPathString);
        String solverOutputPathString = "solver_outputs/mslaw/";
        File solverOutputPath = new File(solverOutputPathString);
        String[] pathnames = solverOutputPath.list();
        List<String> filePrefix = new ArrayList<>();
        // For each pathname in the pathnames array
        for (String pathname : pathnames) {
            // Print the names of files and directories
            if (pathname.endsWith(".json")) {
                String e = pathname.split("\\.")[0];
                e = e.substring(0, e.lastIndexOf("_"));
                filePrefix.add(e);
            }
        }
        System.out.println();
        DroneNet droneNet = new DroneNet();
        Map<String, List<Double>> coverages = new HashMap<>();
        for (String filename : filePrefix) {
            droneNet.loadMobileNodes(mobilityPathString, filename);
            droneNet.loadSolverList(solverOutputPathString, filename);
            List<SolverOutputDto> solverOutputDtoList = droneNet.getSolverOutputDtoList();
            BottleneckAssignmentMatchingModel solver = new BottleneckAssignmentMatchingModel();


            for (int i = 0; i < droneNet.getListofTimeIndexedDrones().size() - 1; i++) {
                List<Drone> A = droneNet.getListofTimeIndexedDrones().get(i);
                List<Drone> B = droneNet.getListofTimeIndexedDrones().get(i + 1);
                if (A.size() != B.size()) continue;
                List<Drone> matched = solver.doMatching(A, B);
                if (matched != null) {
                    NetworkUtils.calculateActorNeighborhoods(matched
                            , solverOutputDtoList.get(0).getConfiguration().getTransmissionRange());
                    droneNet.getListofTimeIndexedDrones().set(i + 1, matched);
                }

            }
            List<Double> coverage = new ArrayList<>();
            for (int i = 0; i < solverOutputDtoList.size(); i++) {
                List<Drone> droneList = droneNet.getListofTimeIndexedDrones().get(i);

                SolverOutputDto solverOutputDto = solverOutputDtoList.get(i);
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
                for(int j=0;j<solverOutputDto.getCoverage().size();j++)
                {
                    List<Integer> integers = solverOutputDto.getCoverage().get(j);
                    Double aDouble = solverOutputDto.getHeatmap().get(integers.get(0)).get(integers.get(1));
                    sum += aDouble*droneNet.getMobileNodes().size();
                }
                // total population 1500
//                coverage.add(count * 100d / droneNet.getMobileNodes().size());
                coverage.add(sum * 100 / droneNet.getMobileNodes().size());


            }
            coverages.put(filename, coverage);
        }
        for(String key:coverages.keySet())
        {
            List<Double> doubles = coverages.get(key);
            double avg = 0;
            for (Double aDouble : doubles) {
                avg += aDouble;

            }
            avg /= doubles.size();
            System.out.println(key+"->"+avg);
        }


        System.out.println();



        /*
        DroneNet droneNet = new DroneNet();
        String filePrefix = "mslaw1_1500";
        String inputPath = "mobility_data/";
        String outPath = "solver_outputs/mslaw/";


         */
    }
}
