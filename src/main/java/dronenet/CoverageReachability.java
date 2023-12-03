package dronenet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author fatihsenel
 * date: 09.05.23
 */
public class CoverageReachability {
    public Map<Double, Double> coverage = new HashMap<>();
    public Map<Double, Double> reachability = new HashMap<>();

    public CoverageReachability(Map<Double, Double> coverage, Map<Double, Double> reachability) {
        this.coverage = coverage;
        this.reachability = reachability;
    }

    public CoverageReachability() {
    }

    public Map<Double, Double> getCoverage() {
        return coverage;
    }

    public void setCoverage(Map<Double, Double> coverage) {
        this.coverage = coverage;
    }

    public Map<Double, Double> getReachability() {
        return reachability;
    }

    public void setReachability(Map<Double, Double> reachability) {
        this.reachability = reachability;
    }

    public boolean hasBetterAverageReachabilityThan(CoverageReachability other) {
        double otherAverage = other.getReachability().values().stream().mapToDouble(u -> u).average().orElse(Double.NaN);
        double thisAverage = getReachability().values().stream().mapToDouble(u -> u).average().orElse(Double.NaN);
        return otherAverage < thisAverage;
    }

    public boolean hasBetterFullConnectivityThan(CoverageReachability other) {
        double o = 1d * other.getReachability().values().stream().filter(u -> u == 1).count() / other.getReachability().size();
        double t = 1d * getReachability().values().stream().filter(u -> u == 1).count() / getReachability().size();
        return o < t;
    }

    public boolean hasBetterPairwiseReachabilityThan(CoverageReachability other) {
        Map<Double, Double> pairwiseDiff = reachability.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue() - other.getReachability().get(entry.getKey())));
        double sum = pairwiseDiff.values().stream().mapToDouble(u -> u).sum();
        return sum > 0;
    }
}
