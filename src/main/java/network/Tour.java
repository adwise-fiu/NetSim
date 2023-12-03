package network;

import geometry.AnalyticGeometry;
import utils.NetworkUtils;
import utils.PolygonUtilities;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author : Fatih Senel
 *         Date: 7/19/11
 *         Time: 10:13 PM
 */
public class Tour {

    ArrayList<Gateway> tobeCollected;
    ArrayList<Gateway> orjtobeCollected;
    ArrayList<Point2D> tobeVisited;
    Point2D centroid;
    int id;
    boolean modify = true;

    public Tour(int id, ArrayList<Gateway> tobeCollected) {
        this.id = id;
        this.tobeCollected = tobeCollected;
        eliminateDuplicate();
        tobeVisited = new ArrayList<Point2D>();
        this.orjtobeCollected = new ArrayList<Gateway>(this.tobeCollected);
        init();
        if (modify)
            modifyTourIncludingNonConvexPoints();

    }

    private void modifyTourIncludingNonConvexPoints() {
        ArrayList<Gateway> interior = new ArrayList<Gateway>(orjtobeCollected);
        interior.removeAll(tobeCollected);
        ArrayList<double[]> convexLineEqs = new ArrayList<double[]>();

        for (int i = 0; i < tobeVisited.size(); i++) {
            Point2D g1 = tobeVisited.get(i);
            Point2D g2 = tobeVisited.get((i + 1) % tobeVisited.size());
            double m = (g2.getY() - g1.getY()) / (g2.getX() - g1.getX());
            double n = g2.getY() - (m * g2.getX());
            convexLineEqs.add(i, new double[]{m, n});
        }

        for (int i = 0; i < interior.size(); i++) {
            Gateway gateway = interior.get(i);
            double x = gateway.getX();
            double y = gateway.getY();
            Point2D P3 = new Point2D.Double(x, y);
            double minDist = Double.MAX_VALUE;
            int minIndex = Integer.MAX_VALUE;
            for (int j = 0; j < convexLineEqs.size(); j++) {
                double m = convexLineEqs.get(j)[0];
                double n = convexLineEqs.get(j)[1];
//                double d = AnalyticGeometry.distanceBetweenPointAndLine(x, y, m, n);
                double d = AnalyticGeometry.distanceBetweenPointAndLine(tobeVisited.get(j), tobeVisited.get((j + 1) % tobeVisited.size()), P3);
                if (d < minDist) {
                    minDist = d;
                    minIndex = j;
                }
            }
            if (minDist > Constants.ActorTransmissionRange) {
                //equation of the closest convex polygon edge
                double m = convexLineEqs.get(minIndex)[0];
                double n = convexLineEqs.get(minIndex)[1];
                Point2D px = AnalyticGeometry.closestPointToALine(tobeVisited.get(minIndex), tobeVisited.get((minIndex + 1) % tobeVisited.size()), P3);

//                double xi = (y + (m * x) - n) / (2 * m);
//                double yi = m * xi + n;
//                Point2D np = AnalyticGeometry.getCoordinates(x, y, xi, yi, Constants.ActorTransmissionRange);
                Point2D np = AnalyticGeometry.getCoordinates(x, y, px.getX(), px.getY(), Constants.ActorTransmissionRange);

                //endpoints of the polygon edges
                Point2D p1 = tobeVisited.get(minIndex);
                Point2D p2 = tobeVisited.get((minIndex + 1) % tobeVisited.size());

                convexLineEqs.remove(minIndex);

                double m1 = (np.getY() - p1.getY()) / (np.getX() - p1.getX());
                double n1 = np.getY() - (m1 * np.getX());

                convexLineEqs.add(minIndex, new double[]{m1, n1});

                double m2 = (np.getY() - p2.getY()) / (np.getX() - p2.getX());
                double n2 = np.getY() - (m2 * np.getX());

                convexLineEqs.add(minIndex + 1, new double[]{m2, n2});

                tobeVisited.add(minIndex + 1, np);
            }
        }

    }

    private void eliminateDuplicate() {
        for (int i = tobeCollected.size() - 1; i >= 0; i--) {
            boolean remove = false;
            Gateway gateway = tobeCollected.get(i);
            for (int j = 0; j < i; j++) {
                Gateway g1 = tobeCollected.get(j);
                if (gateway == g1) {
                    remove = true;
                }
            }
            if (remove) {
                tobeCollected.remove(i);
            }
        }
    }

    private void init() {

        if (tobeCollected.size() == 2) {
            Point2D p1 = new Point2D.Double(tobeCollected.get(0).getX(), tobeCollected.get(0).getY());
            Point2D p2 = new Point2D.Double(tobeCollected.get(1).getX(), tobeCollected.get(1).getY());
            double Ydelta = p2.getY() - p1.getY();
            double Xdelta = p2.getX() - p1.getX();

            if (Math.abs(Xdelta) <= Math.pow(10, -6)) {
                if (Math.abs(Ydelta) > 2 * Constants.ActorTransmissionRange) {
                    double y1, y2;
                    Point2D np1, np2;
                    if (Ydelta > 0) {
                        y1 = p2.getY() - Constants.ActorTransmissionRange;
                        y2 = p1.getY() + Constants.ActorTransmissionRange;
                    } else {
                        y1 = p2.getY() + Constants.ActorTransmissionRange;
                        y2 = p1.getY() - Constants.ActorTransmissionRange;
                    }
                    np1 = new Point2D.Double(p2.getX(), y1);
                    np2 = new Point2D.Double(p2.getX(), y2);
                    tobeVisited.add(np1);
                    tobeVisited.add(np2);
                } else {
                    Point2D np = new Point2D.Double(p2.getX(), Math.min(p1.getX(), p2.getX()) + (Math.abs(Ydelta) / 2));
                    tobeVisited.add(np);
                }
            } else {
                double m = Ydelta / Xdelta;
                double n = p2.getY() - m * p2.getX();
                double distance = AnalyticGeometry.EstimatedDistance(p1, p2);
                if (distance - (2 * Constants.ActorTransmissionRange) <= 0) {
                    Point2D middle = new Point2D.Double((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2);
                    tobeVisited.add(middle);
                } else {
                    Point2D[] intersection1 = AnalyticGeometry.intersectionOfLineAndCircle(m, n, p1.getX(), p1.getY(), Constants.ActorTransmissionRange);
                    Point2D[] intersection2 = AnalyticGeometry.intersectionOfLineAndCircle(m, n, p2.getX(), p2.getY(), Constants.ActorTransmissionRange);
                    Point2D i1, i2;

                    if (AnalyticGeometry.EstimatedDistance(intersection1[0], p2) < AnalyticGeometry.EstimatedDistance(intersection1[1], p2)) {
                        i1 = intersection1[0];
                    } else {
                        i1 = intersection1[1];
                    }
                    if (AnalyticGeometry.EstimatedDistance(intersection2[0], p1) < AnalyticGeometry.EstimatedDistance(intersection2[1], p1)) {
                        i2 = intersection2[0];
                    } else {
                        i2 = intersection2[1];
                    }

                    tobeVisited.add(i1);
                    tobeVisited.add(i2);
                }
            }
        } else if (tobeCollected.size() == 3) {
            Triangle t = new Triangle(tobeCollected.get(0), tobeCollected.get(1), tobeCollected.get(2));
            Point2D ps1 = new Point2D.Double(tobeCollected.get(0).getX(), tobeCollected.get(0).getY());
            Point2D ps2 = new Point2D.Double(tobeCollected.get(1).getX(), tobeCollected.get(1).getY());
            Point2D ps3 = new Point2D.Double(tobeCollected.get(2).getX(), tobeCollected.get(2).getY());
            double ds1 = AnalyticGeometry.EstimatedDistance(ps1, t.fermatPoint);
            double ds2 = AnalyticGeometry.EstimatedDistance(ps2, t.fermatPoint);
            double ds3 = AnalyticGeometry.EstimatedDistance(ps3, t.fermatPoint);

            if (t.f_weight == 1) {
                Point2D p1 = t.fermatPoint;
                tobeVisited.add(p1);
            } else if (t.f_weight == 2) {

                Point2D pt = null;
                if (ds1 > Constants.ActorTransmissionRange) {
                    pt = AnalyticGeometry.getCoordinates(ps1, t.fermatPoint, Constants.ActorTransmissionRange);
                } else if (ds2 > Constants.ActorTransmissionRange) {
                    pt = AnalyticGeometry.getCoordinates(ps2, t.fermatPoint, Constants.ActorTransmissionRange);
                } else if (ds3 > Constants.ActorTransmissionRange) {
                    pt = AnalyticGeometry.getCoordinates(ps3, t.fermatPoint, Constants.ActorTransmissionRange);
                }
                if (pt != null) {
                    tobeVisited.add(t.fermatPoint);
                    tobeVisited.add(pt);
                }

            } else {
                boolean includeFermat = false;
                Point2D pt1 = null, pt2 = null, pt3 = null;
                if (ds1 > Constants.ActorTransmissionRange) {
                    pt1 = AnalyticGeometry.getCoordinates(ps1, t.fermatPoint, Constants.ActorTransmissionRange);
                }
                if (ds2 > Constants.ActorTransmissionRange) {
                    pt2 = AnalyticGeometry.getCoordinates(ps2, t.fermatPoint, Constants.ActorTransmissionRange);
                }
                if (ds3 > Constants.ActorTransmissionRange) {
                    pt3 = AnalyticGeometry.getCoordinates(ps3, t.fermatPoint, Constants.ActorTransmissionRange);
                }

                if (pt1 == null || pt2 == null || pt3 == null) {
                    includeFermat = true;
                }

                if (pt1 != null) {
                    tobeVisited.add(pt1);
                }
                if (pt2 != null) {
                    tobeVisited.add(pt2);
                }
                if (pt3 != null) {
                    tobeVisited.add(pt3);
                }
                if (includeFermat) {
                    tobeVisited.add(t.fermatPoint);
                }
            }
        } else {
            ArrayList<Point2D> tbPoints = new ArrayList<Point2D>();
            for (int i = 0; i < tobeCollected.size(); i++) {
                Gateway gateway = tobeCollected.get(i);
                tbPoints.add(new Point2D.Double(gateway.getX(), gateway.getY()));
            }
            if (AnalyticGeometry.isLinear(tbPoints)) {
//                System.out.println(tobeCollected.toArray());
//                printArray(tobeCollected);
                ArrayList<Gateway> farthestPoints = getFarthestPoints(tobeCollected);
                tobeVisited = new Tour(0, farthestPoints).tobeVisited;
                modify = false;
            } else {
                ArrayList<Gateway> convexHull = PolygonUtilities.getConvexHull(tobeCollected, Constants.ApplicationAreaWidth, Constants.ApplicationAreaHeight);
                Set<Gateway> convexHullVertices = new HashSet<Gateway>(convexHull);
                Set<Gateway> allVertices = new HashSet<Gateway>(tobeCollected);
                allVertices.removeAll(convexHullVertices);
                centroid = PolygonUtilities.centerOfMass(convexHull);

                tobeCollected.clear();
                tobeCollected.addAll(convexHull);

//            centroid = PolygonUtilities.centerOfMass(convexHull);
                double R = Constants.ActorTransmissionRange;
                for (int i = 0; i < convexHull.size(); i++) {
                    Gateway gateway = convexHull.get(i);

                    double deltaX = centroid.getX() - gateway.getX();
                    double deltaY = centroid.getY() - gateway.getY();
                    double dist = AnalyticGeometry.EstimatedDistance(gateway.getX(), gateway.getY(), centroid.getX(), centroid.getY());
                    if (Math.abs(deltaX) < Math.pow(10, -6) && Math.abs(deltaY) < Math.pow(10, -6)) {
                        System.out.println("Warning - Terminal located at centroid");
                    } else if (Math.abs(deltaX) < Math.pow(10, -6)) {
                        //vertical line
                        Point2D inter = new Point2D.Double();
                        if (dist > R) {
                            if (centroid.getY() > gateway.getY()) {
                                inter.setLocation(gateway.getX(), gateway.getY() + R);
                            } else {
                                inter.setLocation(gateway.getX(), gateway.getY() - R);
                            }
                        } else {
                            inter.setLocation(centroid);
                        }
                        tobeVisited.add(inter);
                    } else if (Math.abs(deltaY) < Math.pow(10, -6)) {
                        // horizontal line
                        Point2D inter = new Point2D.Double();
                        if (dist > R) {
                            if (centroid.getX() > gateway.getX()) {
                                inter.setLocation(gateway.getX() + R, gateway.getY());
                            } else {
                                inter.setLocation(gateway.getX() - R, gateway.getY());
                            }
                        } else {
                            inter.setLocation(centroid);
                        }
                        tobeVisited.add(inter);
                    } else {

                        double m = (centroid.getY() - gateway.getY()) / (centroid.getX() - gateway.getX());
                        double n = centroid.getY() - (m * centroid.getX());

                        Point2D[] p = AnalyticGeometry.intersectionOfLineAndCircle(m, n, gateway.getX(), gateway.getY(), Constants.ActorTransmissionRange);
                        if (AnalyticGeometry.EstimatedDistance(p[0], centroid) < AnalyticGeometry.EstimatedDistance(p[1], centroid)) {
                            tobeVisited.add(p[0]);
                        } else {
                            tobeVisited.add(p[1]);
                        }
                    }

                }
            }
        }

        if (tobeVisited.size() > 2) {
            if (!AnalyticGeometry.isLinear(tobeVisited)) {
                ArrayList<Point2D> closedPolygon = PolygonUtilities.getConvexHullPoints(tobeVisited, Constants.ApplicationAreaWidth, Constants.ApplicationAreaHeight);
                checkforNonconvexPoints(closedPolygon, tobeVisited);
                tobeVisited.clear();
                tobeVisited.addAll(closedPolygon);
            }
        }
    }

    private void printArray(ArrayList<Gateway> tobeCollected) {
        System.out.print("[");
        for (int i = 0; i < tobeCollected.size(); i++) {
            Gateway gateway = tobeCollected.get(i);
            if (i != tobeCollected.size() - 1) {
                System.out.print(gateway.getID() + ",");
            } else {
                System.out.print(gateway.getID());
            }
        }
        System.out.println("]");
    }

    private ArrayList<Gateway> getFarthestPoints(ArrayList<Gateway> list) {
        int mini1 = -1, mini2 = -1;
        double minDist = -1;
        for (int i = 0; i < list.size(); i++) {
            Gateway g1 = list.get(i);
            for (int j = 0; j < list.size(); j++) {
                Gateway g2 = list.get(j);
                if (i != j) {
                    double dist = NetworkUtils.EstimatedDistance(g1, g2);
                    if (dist > minDist) {
                        mini1 = i;
                        mini2 = j;
                        minDist = dist;
                    }
                }
            }
        }
        ArrayList<Gateway> result = new ArrayList<Gateway>();
        result.add(list.get(mini1));
        result.add(list.get(mini2));

        return result;
    }

    private void checkforNonconvexPoints(ArrayList<Point2D> closedPolygon, ArrayList<Point2D> tbv) {
        if (closedPolygon.size() < tbv.size()) {
            ArrayList<Point2D> interior = new ArrayList<Point2D>(tbv);
            interior.removeAll(closedPolygon);

            ArrayList<double[]> convexLineEqs = new ArrayList<double[]>();

            for (int i = 0; i < closedPolygon.size(); i++) {
                Point2D g1 = closedPolygon.get(i);
                Point2D g2 = closedPolygon.get((i + 1) % closedPolygon.size());
                double m = (g2.getY() - g1.getY()) / (g2.getX() - g1.getX());
                double n = g2.getY() - (m * g2.getX());
                convexLineEqs.add(i, new double[]{m, n});
            }

            for (int i = 0; i < interior.size(); i++) {
                Point2D concavePoint = interior.get(i);
                double x = concavePoint.getX();
                double y = concavePoint.getY();
                double minDist = Double.MAX_VALUE;
                int minIndex = Integer.MAX_VALUE;
                for (int j = 0; j < convexLineEqs.size(); j++) {
                    double m = convexLineEqs.get(j)[0];
                    double n = convexLineEqs.get(j)[1];
                    double d = AnalyticGeometry.distanceBetweenPointAndLine(x, y, m, n);
                    if (d < minDist) {
                        minDist = d;
                        minIndex = j;
                    }
                }

                //equation of the closest convex polygon edge
                double m = convexLineEqs.get(minIndex)[0];
                double n = convexLineEqs.get(minIndex)[1];
                double xi = (y + (m * x) - n) / (2 * m);
                double yi = m * xi + n;
                Point2D np = AnalyticGeometry.getCoordinates(x, y, xi, yi, Constants.ActorTransmissionRange);

                //endpoints of the polygon edges
                Point2D p1 = closedPolygon.get(minIndex);
                Point2D p2 = closedPolygon.get((minIndex + 1) % closedPolygon.size());

                convexLineEqs.remove(minIndex);

                double m1 = (np.getY() - p1.getY()) / (np.getX() - p1.getX());
                double n1 = np.getY() - (m1 * np.getX());

                convexLineEqs.add(minIndex, new double[]{m1, n1});

                double m2 = (np.getY() - p2.getY()) / (np.getX() - p2.getX());
                double n2 = np.getY() - (m2 * np.getX());

                convexLineEqs.add(minIndex + 1, new double[]{m2, n2});

                closedPolygon.add(minIndex + 1, np);

            }


        }
    }

    public double getTourLength() {
        if (tobeVisited.size() == 1) {
            return 0;
        } else if (tobeVisited.size() == 2) {
            return AnalyticGeometry.EstimatedDistance(tobeVisited.get(0), tobeVisited.get(1)) * 2;
        } else {
            double length = 0;
            for (int i = 0; i < tobeVisited.size(); i++) {
                Point2D p1 = tobeVisited.get(i);
                Point2D p2 = tobeVisited.get((i + 1) % tobeVisited.size());
                length += AnalyticGeometry.EstimatedDistance(p1, p2);
            }
            return length;
        }
    }

    public ArrayList<Gateway> getTobeCollected() {
        return orjtobeCollected;
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLACK);
        int radius = 2;
        if (tobeVisited.size() == 1) {
            int x = (int) tobeVisited.get(0).getX();
            int y = (int) tobeVisited.get(0).getY();
            g.fillOval(x - radius, y - radius, 2 * radius, 2 * radius);
        } else if (tobeVisited.size() == 2) {
            int x1 = (int) tobeVisited.get(0).getX();
            int y1 = (int) tobeVisited.get(0).getY();
            g.fillOval(x1 - radius, y1 - radius, 2 * radius, 2 * radius);

            int x2 = (int) tobeVisited.get(1).getX();
            int y2 = (int) tobeVisited.get(1).getY();
            g.fillOval(x2 - radius, y2 - radius, 2 * radius, 2 * radius);

            g.drawLine(x1, y1, x2, y2);

        } else {
            for (int i = 0; i < tobeVisited.size(); i++) {
                Point2D p1 = tobeVisited.get(i);
                Point2D p2 = tobeVisited.get((i + 1) % tobeVisited.size());
                g.fillOval(((int) p1.getX()) - radius, ((int) p1.getY()) - radius, 2 * radius, 2 * radius);
                g.fillOval(((int) p2.getX()) - radius, ((int) p2.getY()) - radius, 2 * radius, 2 * radius);
                g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
            }
        }

        g.setColor(Color.BLUE);
        if (tobeCollected.size() == 2) {
            Edge e1 = new Edge(tobeCollected.get(0), tobeCollected.get(1));
            e1.draw(g);
        } else if (tobeCollected.size() > 2) {
            for (int i = 0; i < tobeCollected.size(); i++) {
                Edge edge = new Edge(tobeCollected.get(i), tobeCollected.get((i + 1) % tobeCollected.size()));
                edge.draw(g);
            }
        }

    }


    public static Tour merge(Tour tour1, Tour tour2, int id) {
        ArrayList<Gateway> list = new ArrayList<Gateway>(tour1.getTobeCollected());
        list.addAll(tour2.getTobeCollected());
        return new Tour(id, list);
    }

    public String toString() {
        String str = "Nodes = {";
        for (int i = 0; i < tobeCollected.size(); i++) {
            Gateway gateway = tobeCollected.get(i);
            str += gateway.id;
            if (i != tobeCollected.size() - 1) {
                str += ",";
            }
        }
        str += "}\t Length : " + getTourLength();
        return str;
    }
}
