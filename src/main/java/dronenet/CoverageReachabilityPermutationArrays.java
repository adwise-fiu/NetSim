package dronenet;

import dronenet.DroneMobilitySimulation;

public class CoverageReachabilityPermutationArrays {
    CoverageReachabilityPermutation[] avg, pairwise, fullConnectivity;

    public CoverageReachabilityPermutationArrays(CoverageReachabilityPermutation[] avg, CoverageReachabilityPermutation[] pairwise, CoverageReachabilityPermutation[] fullConnectivity) {
        this.avg = avg;
        this.pairwise = pairwise;
        this.fullConnectivity = fullConnectivity;
    }

    public CoverageReachabilityPermutationArrays() {
    }

    public CoverageReachabilityPermutation[] getAvg() {
        return avg;
    }

    public void setAvg(CoverageReachabilityPermutation[] avg) {
        this.avg = avg;
    }

    public CoverageReachabilityPermutation[] getPairwise() {
        return pairwise;
    }

    public void setPairwise(CoverageReachabilityPermutation[] pairwise) {
        this.pairwise = pairwise;
    }

    public CoverageReachabilityPermutation[] getFullConnectivity() {
        return fullConnectivity;
    }

    public void setFullConnectivity(CoverageReachabilityPermutation[] fullConnectivity) {
        this.fullConnectivity = fullConnectivity;
    }
}
