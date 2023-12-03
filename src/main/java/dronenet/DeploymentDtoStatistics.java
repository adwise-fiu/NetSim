package dronenet;

/**
 * @author fatihsenel
 * date: 14.08.22
 */
public class DeploymentDtoStatistics {
    public double timeLimitPercent,
            avgCoverageWhenOptimal,
            avgTimeWhenOptimal,
            avgCoverageWhenTimeLimit, avgMipGapWhenTimeLimit;

    public DeploymentDtoStatistics(double timeLimitPercent, double avgCoverageWhenOptimal, double avgTimeWhenOptimal, double avgCoverageWhenTimeLimit, double avgMipGapWhenTimeLimit) {
        this.timeLimitPercent = timeLimitPercent;
        this.avgCoverageWhenOptimal = avgCoverageWhenOptimal;
        this.avgTimeWhenOptimal = avgTimeWhenOptimal;
        this.avgCoverageWhenTimeLimit = avgCoverageWhenTimeLimit;
        this.avgMipGapWhenTimeLimit = avgMipGapWhenTimeLimit;
    }
}
