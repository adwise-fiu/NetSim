package geometry;


import network.Constants;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 * @author: Fatih Senel
 * Date: Oct 13, 2010
 * Time: 10:24:22 PM
 */
public class AnalyticGeometry {

    /**
     * Returns the intersection point of two lines y1 = m1.x+n1 and y2=m2.x+n2
     *
     * @param m1 slope of first line
     * @param n1 y-intercept of first line
     * @param m2 slope of second line
     * @param n2 y-intercept of second line
     * @return intersection point, NULL if lines are parallel
     */
    public static Point2D intersectionOfTwoLines(double m1, double n1, double m2, double n2) {
        if (m1 == m2) {
            //lines are parallel
            return null;
        } else {
            double x = (n2 - n1) / (m1 - m2);
            double y = m1 * x + n1;
            return new Point2D.Double(x, y);
        }
    }

    /**
     * Returns the intersection point of two lines L1 and L2
     * where L1 passes through(x01,y01) and (x11,y11)
     * where L2 passes through(x02,y02) and (x12,y12)
     *
     * @param x01 first line x0
     * @param y01 first line y0
     * @param x11 first line x1
     * @param y11 first line y1
     * @param x02 second line x0
     * @param y02 second line y0
     * @param x12 second line x1
     * @param y12 second line x1
     * @return intersection point, NULL if lines are parallel
     */
    public static Point2D intersectionOfTwoLines(double x01, double y01, double x11, double y11, double x02, double y02, double x12, double y12) {
        if (Math.abs(x11 - x01) < Math.pow(10, -4)) {
            if (x12 == x02) {
                return null;
            } else {
                double m2 = (y12 - y02) / (x12 - x02);
                double n2 = y02 - (m2 * x02);
                return new Point2D.Double(x11, m2 * x11 + n2);
            }
        } else if (Math.abs(x12 - x02) < Math.pow(10, -4)) {

            double m1 = (y11 - y01) / (x11 - x01);
            double n1 = y01 - (m1 * x01);
            return new Point2D.Double(x12, m1 * x12 + n1);

        } else {
            double m1 = (y11 - y01) / (x11 - x01);
            double n1 = y01 - (m1 * x01);
            double m2 = (y12 - y02) / (x12 - x02);
            double n2 = y02 - (m2 * x02);
            return intersectionOfTwoLines(m1, n1, m2, n2);
        }
    }


    /**
     * Wrapper function to accept the same arguments as the other examples
     *
     * @param x3
     * @param y3
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    public static double distanceBetweenPointAndLine(double x3, double y3, double x1, double y1, double x2, double y2) {
        final Point2D p3 = new Point2D.Double(x3, y3);
        final Point2D p1 = new Point2D.Double(x1, y1);
        final Point2D p2 = new Point2D.Double(x2, y2);
        return distanceBetweenPointAndLine(p1, p2, p3);
    }

    /**
     * Returns the distance of p3 to the segment defined by p1,p2;
     *
     * @param p1 First point of the line segment
     * @param p2 Second point of the line segment
     * @param p3 Point to which we want to know the distance of the segment
     *           defined by p1,p2
     * @return The distance of p3 to the segment defined by p1,p2
     */
    public static double distanceBetweenPointAndLine(Point2D p1, Point2D p2, Point2D p3) {
        final Point2D closestPoint = closestPointToALine(p1, p2, p3);
        return closestPoint.distance(p3);
    }

    /**
     * Returns the distance of p3 to the segment defined by p1,p2;
     *
     * @param p1 First point of the line segment
     * @param p2 Second point of the line segment
     * @param p3 Point to which we want to know the distance of the segment
     *           defined by p1,p2
     * @return The closest point of p3 to the segment defined by p1,p2
     */
    public static Point2D closestPointToALine(Point2D p1, Point2D p2, Point2D p3) {
        final double xDelta = p2.getX() - p1.getX();
        final double yDelta = p2.getY() - p1.getY();

        if ((xDelta == 0) && (yDelta == 0)) {
            throw new IllegalArgumentException("p1 and p2 cannot be the same point");
        }

        final double u = ((p3.getX() - p1.getX()) * xDelta + (p3.getY() - p1.getY()) * yDelta) / (xDelta * xDelta + yDelta * yDelta);

        final Point2D closestPoint;
        if (u < 0) {
            closestPoint = p1;
        } else if (u > 1) {
            closestPoint = p2;
        } else {
            closestPoint = new Point2D.Double(p1.getX() + u * xDelta, p1.getY() + u * yDelta);
        }
        return closestPoint;
    }


    /**
     * Finds the distance between the point (x0,y0) and the line y=mx+n
     *
     * @param x0 x-coordinate of the point
     * @param y0 y-coordinate of the point
     * @param m  slope of the line
     * @param n  y-intercept of the line
     * @return distance between line and point
     */
    public static double distanceBetweenPointAndLine(double x0, double y0, double m, double n) {
        // Let (xi, yi) be the closest point on the line to (x0, y0)
        double xi = (y0 + (m * x0) - n) / (2 * m);
        double yi = m * xi + n;
        return euclideanDistance(x0, y0, xi, yi);
    }


    /**
     * Finds the intersection points of a line and circle
     * Line   : y = m.x+n
     * Circle : r^2=x0^2+y0^2
     *
     * @param m  slope of the line
     * @param n  y-intercept of the line
     * @param x0 x-coordinate of the center of the circle
     * @param y0 y-coordinate of the center of the circle
     * @param r  radius of the circle
     * @return NULL if line and circle do not intersect, single point if line is tangent to the cirlce, two points otherwise
     */
    public static Point2D[] intersectionOfLineAndCircle(double m, double n, double x0, double y0, double r) {
        double A = 1 + Math.pow(m, 2);
        double B = 2 * (m * (n - y0) - x0);
        double C = Math.pow(x0, 2) + Math.pow(y0, 2) + Math.pow(n, 2) - Math.pow(r, 2) - (2 * y0 * n);
        double delta = Math.pow(B, 2) - 4 * A * C;
        if (delta < 0) {
            // No real solution
            return null;
        } else if (delta == 0) {
            // line is tangent to circle
            Point2D[] result = new Point2D[1];
            double x = -B / (2 * A);
            double y = m * x + n;
            Point2D p = new Point.Double(x, y);
            result[0] = p;
            return result;
        } else {
            //intersection
            Point2D[] result = new Point2D[2];
            double x1 = (-B + Math.abs(Math.sqrt(delta))) / (2 * A);
            double y1 = m * x1 + n;

            double x2 = (-B - Math.abs(Math.sqrt(delta))) / (2 * A);
            double y2 = m * x2 + n;

            Point2D p1 = new Point.Double(x1, y1);
            Point2D p2 = new Point.Double(x2, y2);
            result[0] = p1;
            result[1] = p2;
            return result;
        }
    }


    /**
     * Determine the intersection points of two circle which are in a common plane.
     *
     * @param p0 center of first Circle
     * @param r0 radius of first Circle
     * @param p1 center of second Circle
     * @param r1 radius of second Circle
     * @return intersection: List of intersection Points
     */
    public static ArrayList<Point2D> intersectionOfTwoCircles(Point2D p0, double r0, Point2D p1, double r1) {
        return intersectionOfTwoCircles(p0.getX(), p0.getY(), r0, p1.getX(), p1.getY(), r1);
    }

    /**
     * Determine the intersection points of two circle which are in a common plane.
     *
     * @param x0 x-coordinate of first Circle
     * @param y0 y-coordinate of first Circle
     * @param r0 radius of first Circle
     * @param x1 x-coordinate of second Circle
     * @param y1 y-coordinate of second Circle
     * @param r1 radius of second Circle
     * @return intersection: List of intersection Points
     */
    public static ArrayList<Point2D> intersectionOfTwoCircles(double x0, double y0, double r0, double x1, double y1, double r1) {
        ArrayList<Point2D> intersection = new ArrayList<Point2D>();

        double a, dx, dy, d, h, rx, ry;
        double x2, y2;

        /* dx and dy are the vertical and horizontal distances between
         * the circle centers.
         */
        dx = x1 - x0;
        dy = y1 - y0;

        /* Determine the straight-line distance between the centers. */
        d = euclideanDistance(x0, y0, x1, y1);
        /* Check for solvability. */
        if (d > (r0 + r1)) {
            /* no solution. circles do not intersect. */
            return null;
        }
        if (d < Math.abs(r0 - r1)) {
            /* no solution. one circle is contained in the other */
            return null;
        }

        /* 'point 2' is the point where the line through the circle
         * intersection points crosses the line between the circle
         * centers.
         */

        /* Determine the distance from point 0 to point 2. */
        a = ((r0 * r0) - (r1 * r1) + (d * d)) / (2.0 * d);

        /* Determine the coordinates of point 2. */
        x2 = x0 + (dx * a / d);
        y2 = y0 + (dy * a / d);

        /* Determine the distance from point 2 to either of the
         * intersection points.
         */
        h = Math.sqrt((r0 * r0) - (a * a));

        /* Now determine the offsets of the intersection points from
         * point 2.
         */
        rx = -dy * (h / d);
        ry = dx * (h / d);

        /* Determine the absolute intersection points. */
        double x_i1 = x2 + rx;
        double x_i2 = x2 - rx;
        double y_i1 = y2 + ry;
        double y_i2 = y2 - ry;

        Point2D p1 = new Point2D.Double();
        Point2D p2 = new Point2D.Double();
        p1.setLocation(x_i1, y_i1);
        p2.setLocation(x_i2, y_i2);
        intersection.add(p1);
        intersection.add(p2);
        return intersection;
    }

    /**
     * Calculates the distance between two points
     *
     * @param p1 starting point
     * @param p2 ending point
     * @return distance
     */
    public static double euclideanDistance(Point2D p1, Point2D p2) {
        return Math.sqrt(((p1.getX() - p2.getX()) * (p1.getX() - p2.getX())) + ((p1.getY() - p2.getY()) * (p1.getY() - p2.getY())));
    }

    /**
     * Calculates the distance between two locations
     *
     * @param x1 x-coordinate of 1st location
     * @param y1 y-coordinate of 1st location
     * @param x2 x-coordinate of 2nd location
     * @param y2 y-coordinate of 2nd location
     * @return distance
     */
    public static double euclideanDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }


    /**
     * Let T be the triangle formed by the corners left, c, right
     * Assume there is an edge between c and p1 and c and p2 (c, left) and (c, right)
     * Let ang be the angle left-c-right where ang < 180
     * Returns true if (c,x) splits the the ang in to two parts
     *
     * @param X      center corner
     * @param left   leaf corner
     * @param right  leaf corner
     * @param target node
     * @return true if (c,x) splits the the ang in to two parts
     */
    public static boolean isSplitting(Point2D left, Point2D X, Point2D right, Point2D target) {
        double distA, distB, distC;

        distA = euclideanDistance(left, X);
        distB = euclideanDistance(right, X);
        distC = euclideanDistance(right, left);

        double theta = Math.acos((distA * distA + distB * distB - distC * distC) / (2 * distA * distB));
        theta = 180 * theta / Math.PI;
        if (theta == 180)
            return true;
        else if (theta > 180) {
            theta = 360 - theta;
        }

        distA = euclideanDistance(target, X);
        distB = euclideanDistance(left, X);
        distC = euclideanDistance(target, left);

        double alpha = Math.acos((distA * distA + distB * distB - distC * distC) / (2 * distA * distB));
        alpha = 180 * alpha / Math.PI;
        if (alpha == 180)
            return false;
        else if (alpha > 180) {
            alpha = 360 - alpha;
        }

        distA = euclideanDistance(target, X);
        distB = euclideanDistance(right, X);
        distC = euclideanDistance(target, right);

        double beta = Math.acos((distA * distA + distB * distB - distC * distC) / (2 * distA * distB));
        beta = 180 * beta / Math.PI;
        if (beta == 180)
            return false;
        else if (beta > 180) {
            beta = 360 - beta;
        }

        return (Math.abs(alpha + beta - theta) < Math.pow(10, -3));
    }

    /**
     * Let f(x) = mx + n be a line, if f(xt) > yt return -1 if f(xt)<yt return 1, if (xt, yt) is on the line return 0
     *
     * @param m  slope
     * @param n  y-intercept of the line
     * @param xt x-coordinate
     * @param yt y-coordinate
     * @return if f(xt) > yt return -1 if f(xt) < yt return 1, if (xt, yt) is on the line return 0
     */
    public static int findRegionWRTLine(double m, double n, double xt, double yt) {
        double y_prime = m * xt + n;
        if (y_prime > yt) {
            return -1;
        } else if (y_prime < yt) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Let L be the line passing through (x0,y0) and (x1,y1)
     * m is the slope and n is the y-intercept of this line
     * Let f(x) = mx + n be a line, if f(xt) > yt return -1 if f(xt)<yt return 1, if (xt, yt) is on the line return 0
     *
     * @param x0 x-coordinate of line end point 1
     * @param y0 y-coordinate of line end point 1
     * @param x1 x-coordinate of line end point 2
     * @param y1 y-coordinate of line end point 2
     * @param xt x-coordinate
     * @param yt y-coordinate
     * @return if f(xt) > yt return -1 if f(xt) < yt return 1, if (xt, yt) is on the line return 0
     */
    public static int findRegionWRTLine(double x0, double y0, double x1, double y1, double xt, double yt) {
        double deltaX = x1 - x0;
        double deltaY = y1 - y0;

        if (deltaX == 0) {
            if (xt < x1) {
                return 1;
            } else if (xt > x1) {
                return -1;
            } else {
                return 0;
            }
        } else {
            double m = deltaY / deltaX;
            double n = y1 - (m * x1);
            return findRegionWRTLine(m, n, xt, yt);
        }
    }

    /**
     * Finds ABC angle (angle at B point)
     *
     * @param A left
     * @param B center
     * @param C right
     * @return angle
     */
    public static double findAngle(Point2D A, Point2D B, Point2D C) {

        double distA, distB, distC;

        distA = euclideanDistance(A, B);
        distB = euclideanDistance(C, B);
        distC = euclideanDistance(A, C);

        //check linearity
        if (Math.abs(distA + distB - distC) < Math.pow(10, -4)) {
            return 180;
        }

        double theta = Math.acos((distA * distA + distB * distB - distC * distC) / (2 * distA * distB));
        theta = 180 * theta / Math.PI;

        return theta;
    }

    public static Point2D getCoordinates(Point2D p1, Point2D p2, double distance) {
        return getCoordinates(p1.getX(), p1.getY(), p2.getX(), p2.getY(), distance);
    }


    /**
     * Consider the line passing through (x1,y1) and (x2,y2).
     * The function calculates the new coordinate (xi, yi) which is on the line and "distance" meters apart from (x1,y1)
     *
     * @param x1       x coordinate of first point
     * @param y1       y coordinate of first point
     * @param x2       x coordinate of second point
     * @param y2       y coordinate of second point
     * @param distance distance
     * @return new coordinate (xi, yi) which is on the line and "distance" meters apart from (x1,y1)
     */
    public static Point2D getCoordinates(double x1, double y1, double x2, double y2, double distance) {
        double nx, ny;
        Point2D result = new Point2D.Double();
        double D = EstimatedDistance(x1, y1, x2, y2);
        if (distance >= D) {
            result.setLocation(x2, y2);
            return result;
        }

        double abs_diff_y = Math.abs(y2 - y1) * distance / D;
        double abs_diff_x = Math.abs(x2 - x1) * distance / D;

        if (y2 > y1) {
            ny = y1 + abs_diff_y;
        } else if (y2 < y1) {
            ny = y1 - abs_diff_y;
        } else {
            ny = y1;
        }
        if (x2 > x1) {
            nx = x1 + abs_diff_x;
        } else if (x2 < x1) {
            nx = x1 - abs_diff_x;
        } else {
            nx = x1;
        }


        result.setLocation(nx, ny);
        return result;
    }

    private static double Estimate(double distance, double error) {
        double R = Constants.RNTransmissionRange;
        double result = distance;
        double dd1 = distance / R;
        int id1 = (int) Math.floor(dd1);

        if (Math.abs(dd1 - id1) <= error) {
            result = id1 * R;
        }
        return result;
    }

    public static double EstimatedDistance(Point2D p1, Point2D p2) {
        return Estimate(euclideanDistance(p1, p2), Constants.err);
    }

    public static double EstimatedDistance(double x1, double y1, double x2, double y2) {
        return Estimate(euclideanDistance(x1, y1, x2, y2), Constants.err);
    }

    public static boolean isLinear(ArrayList<Point2D> listofpoints) {
        if (listofpoints.size() == 2) {
            return true;
        } else if (listofpoints.size() > 2) {
            Point2D p0 = listofpoints.get(0);
            Point2D p1 = listofpoints.get(1);
            double deltaY = p0.getY() - p1.getY();
            double deltaX = p0.getX() - p1.getX();
            double err = Math.pow(10, -6);
            if (Math.abs(deltaY) < err && Math.abs(deltaX) < err) {
                return false;
            } else if (Math.abs(deltaY) < err) {
                boolean check = true;
                for (int i = 2; i < listofpoints.size(); i++) {
                    Point2D pi = listofpoints.get(i);
                    double deltaYi = Math.abs(pi.getY() - p0.getY());
                    if (deltaYi > err) {
                        check = false;
                    }
                }
                return check;
            } else if (Math.abs(deltaX) < err) {
                boolean check = true;
                for (int i = 2; i < listofpoints.size(); i++) {
                    Point2D pi = listofpoints.get(i);
                    double deltaXi = Math.abs(pi.getX() - p0.getX());
                    if (deltaXi > err) {
                        check = false;
                    }
                }
                return check;
            } else {
                double m = deltaY / deltaX;
                double n = p0.getY() - m * p0.getX();
                boolean check = true;
                for (int i = 2; i < listofpoints.size(); i++) {
                    Point2D pi = listofpoints.get(i);
                    double eps = Math.abs(pi.getY() - (m * pi.getX()) - n);
                    if (eps > err) {
                        check = false;
                    }
                }
                return check;
            }

        } else {
            return false;
        }
    }

    /**
     * Consider the line passing through (x1,y1) and (x2,y2).
     * The function calculates the new coordinate (xi, yi) and (xj, yj) which is on the perpendicular line and "distance" meters apart from (x2,y2)
     *
     * @param x1       x coordinate of first point
     * @param y1       y coordinate of first point
     * @param x2       x coordinate of second point
     * @param y2       y coordinate of second point
     * @param distance distance
     * @return new coordinate (xi, yi) which is on the line and "distance" meters apart from (x1,y1)
     */
    public static Point2D[] getCoordinatesOnAPerpendicularLine(double x1, double y1, double x2, double y2, double distance) {
        Point2D p1 = new Point();
        Point2D p2 = new Point();
        Point2D[] result = {p1, p2};
        double deltaY = y2 - y1;
        double deltaX = x2 - x1;
        if (deltaX == 0) {
            result[0].setLocation(x2 - distance, y2);
            result[1].setLocation(x2 + distance, y2);
            return result;
        } else {
            if (deltaY == 0) {
                result[0].setLocation(x2, y2 - distance);
                result[1].setLocation(x2, y2 + distance);
                return result;
            }

            double m = deltaY / deltaX;
            double n = y2 - m * x2;

            // perpendicular line slope
            double mp = -1 / m;
            double np = y2 - mp * x2;
            double a = mp * mp + 1;
            double t = np - y2;
            double b = 2 * t * mp - 2 * x2;
            double c = t * t + x2 * x2 - distance * distance;

            double delta = b * b - 4 * a * c;
            double xs1 = (-b + Math.sqrt(delta)) / 2 * a;
            double ys1 = mp * xs1 + np;
            double xs2 = (-b - Math.sqrt(delta)) / 2 * a;
            double ys2 = mp * xs2 + np;
            result[0].setLocation(xs1, ys1);
            result[1].setLocation(xs2, ys2);
            return result;

        }
    }
}
