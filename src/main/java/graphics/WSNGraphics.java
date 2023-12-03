package graphics;

import geometry.AnalyticGeometry;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * User: Fatih Senel
 * Date: Dec 30, 2008
 * Time: 2:10:01 AM
 */
public class WSNGraphics {
    public static void drawDashedLine(Graphics g, double x1, double y1, double x2, double y2, double dash, double gap) {
        if (AnalyticGeometry.euclideanDistance(x1, y1, x2, y2) <= dash + gap) {
            g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        } else {
            Point2D nextEnd = AnalyticGeometry.getCoordinates(x1, y1, x2, y2, dash);
            Point2D nextStart = AnalyticGeometry.getCoordinates(x1, y1, x2, y2, dash + gap);
            g.drawLine((int) x1, (int) y1, (int) nextEnd.getX(), (int) nextEnd.getY());
            drawDashedLine(g, nextStart.getX(), nextStart.getY(), x2, y2, dash, gap);
        }

    }

}
