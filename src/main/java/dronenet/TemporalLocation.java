package dronenet;

import java.text.DecimalFormat;
import java.util.List;

/**
 * @author fatihsenel
 * date: 16.02.22
 */
public class TemporalLocation implements Comparable<TemporalLocation> {
    public double x, y, time;
    DecimalFormat df = new DecimalFormat("0.00");

    public TemporalLocation() {
    }

    public TemporalLocation(double time, double x, double y) {
        this.x = x;
        this.y = y;
        this.time = time;
    }

    @Override
    public int compareTo(TemporalLocation o) {
        return Double.compare(this.time, o.time);
    }

    @Override
    public String toString() {
        return df.format(time) + ":(" + df.format(x) + "," + df.format(y)+")";
    }
}
