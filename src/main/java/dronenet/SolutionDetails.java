package dronenet;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author fatihsenel
 * date: 17.07.22
 */
public class SolutionDetails {
    @JsonProperty("status")
    String status;
    @JsonProperty("time")
    double time;
    @JsonProperty("best_bound")
    double bestBound;
    @JsonProperty("mip_relative_gap")
    double mipRelativeGap;
    @JsonProperty("gap")
    double gap;

    public SolutionDetails() {
    }

    public SolutionDetails(String status, double time, double bestBound, double mipRelativeGap, double gap) {
        this.status = status;
        this.time = time;
        this.bestBound = bestBound;
        this.mipRelativeGap = mipRelativeGap;
        this.gap = gap;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public double getBestBound() {
        return bestBound;
    }

    public void setBestBound(double bestBound) {
        this.bestBound = bestBound;
    }

    public double getMipRelativeGap() {
        return mipRelativeGap;
    }

    public void setMipRelativeGap(double mipRelativeGap) {
        this.mipRelativeGap = mipRelativeGap;
    }

    public double getGap() {
        return gap;
    }

    public void setGap(double gap) {
        this.gap = gap;
    }
}
