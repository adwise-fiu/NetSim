package dronenet;

import java.util.List;

/**
 * @author fatihsenel
 * date: 07.01.24
 */
public class CoverageReachabilityGuessed {
    CoverageReachability coverageReachability;
    List<String> schedules;

    public CoverageReachabilityGuessed(CoverageReachability coverageReachability, List<String> schedules) {
        this.coverageReachability = coverageReachability;
        this.schedules = schedules;
    }

    public CoverageReachabilityGuessed() {
    }

    public CoverageReachability getCoverageReachability() {
        return coverageReachability;
    }

    public void setCoverageReachability(CoverageReachability coverageReachability) {
        this.coverageReachability = coverageReachability;
    }

    public List<String> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<String> schedules) {
        this.schedules = schedules;
    }
}
