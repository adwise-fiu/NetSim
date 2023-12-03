package utils;

import geometry.AnalyticGeometry;
import network.Gateway;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * User: Fatih Senel
 * Date: Dec 27, 2008
 * Time: 6:23:01 PM
 */

/**
 * @author Christopher Fuhrman (christopher.fuhrman@gmail.com)
 * @version 2006-09-27
 */
public class PolygonUtilities {

    /**
     * Function to calculate the area of a polygon, according to the algorithm
     * defined at http://local.wasp.uwa.edu.au/~pbourke/geometry/polyarea/
     *
     * @param polyPoints array of points in the polygon
     * @return area of the polygon defined by pgPoints
     */
    private static double area(Point2D[] polyPoints) {
        int i, j, n = polyPoints.length;
        double area = 0;

        for (i = 0; i < n; i++) {
            j = (i + 1) % n;
            area += polyPoints[i].getX() * polyPoints[j].getY();
            area -= polyPoints[j].getX() * polyPoints[i].getY();
        }
        area /= 2.0;
        return (area);
    }

    /**
     * Function to calculate the area of a polygon, according to the algorithm
     * defined at http://local.wasp.uwa.edu.au/~pbourke/geometry/polyarea/
     *
     * @param list array of gateways in the polygon
     * @return area of the polygon defined by pgPoints
     */
    public static double area(ArrayList<Gateway> list) {
        Point2D[] arr = new Point2D[list.size()];
        for (int i = 0; i < list.size(); i++) {
            double x = list.get(i).getX();
            double y = list.get(i).getY();
            Point2D p = new Point2D.Double();
            p.setLocation(x, y);
            arr[i] = p;
        }
        return area(arr);
    }

    /**
     * Function to calculate the center of mass for a given polygon, according
     * ot the algorithm defined at
     * http://local.wasp.uwa.edu.au/~pbourke/geometry/polyarea/
     *
     * @param list array of gateways in the polygon
     * @return point that is the center of mass
     */
    public static Point2D centerOfMass(ArrayList<Gateway> list) {
        Point2D[] arr = new Point2D[list.size()];
        for (int i = 0; i < list.size(); i++) {
            double x = list.get(i).getX();
            double y = list.get(i).getY();
            Point2D p = new Point2D.Double();
            p.setLocation(x, y);
            arr[i] = p;
        }
        return centerOfMass(arr);
    }

    /**
     * Function to calculate the center of mass for a given polygon, according
     * ot the algorithm defined at
     * http://local.wasp.uwa.edu.au/~pbourke/geometry/polyarea/
     *
     * @param polyPoints array of points in the polygon
     * @return point that is the center of mass
     */
    private static Point2D centerOfMass(Point2D[] polyPoints) {
        double cx = 0, cy = 0;
        double area = area(polyPoints);
        // could change this to Point2D.Float if you want to use less memory
        Point2D res = new Point2D.Double();
        int i, j, n = polyPoints.length;

        double factor;
        for (i = 0; i < n; i++) {
            j = (i + 1) % n;
            factor = (polyPoints[i].getX() * polyPoints[j].getY()
                    - polyPoints[j].getX() * polyPoints[i].getY());
            cx += (polyPoints[i].getX() + polyPoints[j].getX()) * factor;
            cy += (polyPoints[i].getY() + polyPoints[j].getY()) * factor;
        }
        area *= 6.0f;
        factor = 1 / area;
        cx *= factor;
        cy *= factor;
        res.setLocation(cx, cy);
        return res;
    }

    /**
     * Given an unordered list of points which forms closed polygon, the function returns the ordered list of vertices
     * @param list undered list of points
     * @param applicationAreaWidth deployment area width
     * @param applicationAreaHeight deployment area height
     * @return ordered list of vertices which forms a closed polygon
     */
    public static ArrayList<Point2D> getClosedPolygon(ArrayList<Point2D> list, int applicationAreaWidth, int applicationAreaHeight) {
        ArrayList<Point2D> convexHull = PolygonUtilities.getConvexHullPoints(list, applicationAreaWidth, applicationAreaHeight);
        Set<Point2D> convexHullVertices = new HashSet<Point2D>(convexHull);
        Set<Point2D> allVertices = new HashSet<Point2D>(list);
        allVertices.removeAll(convexHullVertices);

        for (Point2D vertex : allVertices) {
            double mindist = Double.MAX_VALUE;
            int minIndex = -1;
            for (int i = 0; i < convexHull.size(); i++) {
                Point2D p1 = convexHull.get(i);
                Point2D p2 = convexHull.get((i + 1) % convexHull.size());

                double m = (p2.getY() - p1.getY()) / (p2.getX() - p1.getX());
                double n = p2.getY() - (m * p2.getX());
                double distance = AnalyticGeometry.distanceBetweenPointAndLine(vertex.getX(), vertex.getY(), m, n);
                if (distance < mindist) {
                    minIndex = i;
                    mindist = distance;
                }
            }
            convexHull.add(minIndex + 1, vertex);
        }
        return convexHull;
    }


    static class PPoint {
        double x, y;
        float angle;
        int index;
    }

    private static boolean NonLeftTurn(PPoint p1, PPoint p2, PPoint p3, PPoint p0) {
        double l1, l2, l4, l5, l6, angle1, angle2, angle;

        l1 = Math.sqrt(Math.pow(p2.y - p1.y, 2) + Math.pow(p2.x - p1.x, 2));
        l2 = Math.sqrt(Math.pow(p3.y - p2.y, 2) + Math.pow(p3.x - p2.x, 2));
        l4 = Math.sqrt(Math.pow(p3.y - p0.y, 2) + Math.pow(p3.x - p0.x, 2));
        l5 = Math.sqrt(Math.pow(p1.y - p0.y, 2) + Math.pow(p1.x - p0.x, 2));
        l6 = Math.sqrt(Math.pow(p2.y - p0.y, 2) + Math.pow(p2.x - p0.x, 2));

        angle1 = Math.acos(((l2 * l2) + (l6 * l6) - (l4 * l4)) / (2 * l2 * l6));
        angle2 = Math.acos(((l6 * l6) + (l1 * l1) - (l5 * l5)) / (2 * l6 * l1));

        angle = (Math.PI - angle1) - angle2;

        if (angle <= 0.0) {
            return (true);
        } else {
            return (false);
        }
    }

    private static Vector<PPoint> GrahamScan(Vector<PPoint> point, int width, int height) {
        Vector<PPoint> convexHull = new Vector<PPoint>();
        Vector<PPoint> p = new Vector<PPoint>(point.size());

        PPoint alpha = new PPoint();
        alpha.x = width;
        alpha.y = height;
        int nalpha = -1;

        for (int i = 0; i < point.size(); i++) {
            PPoint npoint = (point.elementAt(i));
            if (npoint.y <= alpha.y) {
                if (npoint.y < alpha.y) {
                    alpha = npoint;
                    nalpha = i;
                } else {
                    if (npoint.x < alpha.x) {
                        alpha = npoint;
                        nalpha = i;
                    }
                }
            }
        }

        alpha.angle = 0;
        convexHull.addElement(alpha);

        for (int i = 0; i < point.size(); i++) {
            PPoint npoint = point.elementAt(i);
            // angle will be in range 0 - pi
            npoint.angle = (float) Math.atan2(npoint.y - alpha.y, npoint.x - alpha.x);

            boolean ptin = false;
            for (int j = 0; j < p.size(); j++) {
                if (i == nalpha) {
                    ptin = true;
                    break;
                }

                PPoint ppoint = p.elementAt(j);
                if (npoint.angle == ppoint.angle) {
                    // abandon nearest
                    if (Math.sqrt(ppoint.x * ppoint.x + ppoint.y * ppoint.y) <
                            Math.sqrt(npoint.x * npoint.x + npoint.y * npoint.y)) {
                        p.setElementAt(npoint, j);
                    }
                    ptin = true;
                    break;
                }
                if (npoint.angle < ppoint.angle) {
                    p.insertElementAt(npoint, j);
                    ptin = true;
                    break;
                }
            }
            if (!ptin) {
                p.addElement(npoint);
            }
        }

        // added all points to p
        // now go through them!

        nalpha = p.size();
        convexHull.addElement(p.elementAt(0));
        convexHull.addElement(p.elementAt(1));

        for (int i = 2; i < nalpha; i++) {
            PPoint p1, p2, pn;
            pn = p.elementAt(i);
            p1 = convexHull.elementAt(convexHull.size() - 2);
            p2 = convexHull.elementAt(convexHull.size() - 1);
            while (NonLeftTurn(p1, p2, pn, convexHull.firstElement())) {
                convexHull.removeElementAt(convexHull.size() - 1);
                p2 = p1;
                p1 = convexHull.elementAt(convexHull.size() - 2);
            }
            convexHull.addElement(pn);
        }

        // add the first element again to close polygon
//        convexHull.addElement(convexHull.firstElement());
        return convexHull;

    }

    public static ArrayList<Point2D> getConvexHullPoints(ArrayList<Point2D> nodeList, int width, int height) {
        Vector<PPoint> input = new Vector<PPoint>();
        for (int i = 0; i < nodeList.size(); i++) {
            Point2D p = nodeList.get(i);
            PPoint pt = new PPoint();
            pt.x = p.getX();
            pt.y = p.getY();
            pt.index = i;
            pt.angle = 0;
            input.add(pt);
        }
        Vector<PPoint> scanned = GrahamScan(input, width, height);

        // @date: 24 Septem 2010 - First Revision
        for (int i = scanned.size() - 1; i >= 0; i--) {
            PPoint p1 = scanned.elementAt(i);
            for (int j = scanned.size() - 1; j >= 0; j--) {
                if (i != j) {
                    PPoint p2 = scanned.elementAt(j);
                    if (p2.x == p1.x && p2.y == p1.y) {
                        scanned.remove(j);
                    }
                }

            }
        }


        ArrayList<Point2D> result = new ArrayList<Point2D>();
        for (int i = 0; i < scanned.size(); i++) {
            PPoint pt = scanned.elementAt(i);
            result.add(nodeList.get(pt.index));
        }
        return result;
    }

    public static ArrayList<Gateway> getConvexHull(ArrayList<Gateway> nodeList, int width, int height) {
        Vector<PPoint> input = new Vector<PPoint>();
        for (int i = 0; i < nodeList.size(); i++) {
            Gateway gateway = nodeList.get(i);
            PPoint pt = new PPoint();
            pt.x = gateway.getX();
            pt.y = gateway.getY();
            pt.index = i;
            pt.angle = 0;
            input.add(pt);
        }
        Vector<PPoint> scanned = GrahamScan(input, width, height);

        // @date: 24 Septem 2010 - First Revision
        for (int i = scanned.size() - 1; i >= 0; i--) {
            PPoint p1 = scanned.elementAt(i);
            for (int j = scanned.size() - 1; j >= 0; j--) {
                if (i != j) {
                    PPoint p2 = scanned.elementAt(j);
                    if (p2.x == p1.x && p2.y == p1.y) {
                        scanned.remove(j);
                    }
                }

            }
        }


        ArrayList<Gateway> result = new ArrayList<Gateway>();
        for (int i = 0; i < scanned.size(); i++) {
            PPoint pt = scanned.elementAt(i);
            result.add(nodeList.get(pt.index));
        }
        return result;
    }

}