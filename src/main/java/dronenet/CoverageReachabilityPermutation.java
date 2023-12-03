package dronenet;

/**
 * @author fatihsenel
 * date: 17.10.23
 */
public class CoverageReachabilityPermutation {
    String permutation, comparison;
    CoverageReachability coverageReachability;

    public CoverageReachabilityPermutation(String permutation, String comparison, CoverageReachability coverageReachability) {
        this.permutation = permutation;
        this.comparison = comparison;
        this.coverageReachability = coverageReachability;
    }

    public CoverageReachabilityPermutation() {
    }

    public String getPermutation() {
        return permutation;
    }

    public void setPermutation(String permutation) {
        this.permutation = permutation;
    }

    public String getComparison() {
        return comparison;
    }

    public void setComparison(String comparison) {
        this.comparison = comparison;
    }

    public CoverageReachability getCoverageReachability() {
        return coverageReachability;
    }

    public void setCoverageReachability(CoverageReachability coverageReachability) {
        this.coverageReachability = coverageReachability;
    }
}
