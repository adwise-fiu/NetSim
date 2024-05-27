package dronenet;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * @author fatihsenel
 * date: 27.03.24
 */
public class AlphaContractionDto {
    public double alpha;
    public List<Double> reachability;
    public long disconnectedMoments;
    public double maxTravelDistance;

    public Point2D contractionPoint;

    public AlphaContractionDto(double alpha, List<Double> reachability) {
        this.alpha = alpha;
        this.reachability = reachability;
        this.disconnectedMoments = this.reachability.stream().filter(u -> u != 1).count();
    }


}
