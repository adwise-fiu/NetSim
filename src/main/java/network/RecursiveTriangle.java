package network;

import geometry.AnalyticGeometry;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Stack;

/**
 * @author : Fatih Senel
 *         Date: 5/13/11
 *         Time: 12:41 AM
 */
public class RecursiveTriangle implements Comparable {

    Triangle triangle;
    int gain = 0;
    Stack<Triangle> innerTriangles = new Stack<Triangle>();
    ArrayList<Point2D> listOfFPs = new ArrayList<Point2D>();
    ArrayList<PointPair> listOfEdges = new ArrayList<PointPair>();

    public RecursiveTriangle(Triangle triangle) {
        this.triangle = triangle;
        gain = this.triangle.getGain();
        buildStack();
        System.out.println();
//        ArrayList<Triangle> t = new ArrayList<Triangle>();
//        t.add(triangle);
//        findInnerFPs(triangle, t);
    }

    private void buildStack() {


        Gateway A = this.triangle.s1;
        Gateway B = this.triangle.s2;
        Gateway C = this.triangle.s3;
        Gateway F = new Gateway(4);
        F.setX(triangle.fermatPoint.getX());
        F.setY(triangle.fermatPoint.getY());
        listOfEdges.add(new PointPair(A.getX(), A.getY(), F.getX(), F.getY()));
        listOfEdges.add(new PointPair(B.getX(), B.getY(), F.getX(), F.getY()));
        listOfEdges.add(new PointPair(C.getX(), C.getY(), F.getX(), F.getY()));

        listOfFPs.add(new Point.Double(F.getX(), F.getY()));

        Triangle tt1 = new Triangle(A, F, B);
        Triangle tt2 = new Triangle(A, F, C);
        Triangle tt3 = new Triangle(B, F, C);
        int max = Math.max(tt1.getGain(), Math.max(tt2.getGain(), tt3.getGain()));

        while (max != 0) {
            if (max == tt1.getGain()) {

                for (int i = listOfEdges.size() - 1; i >= 0; i--) {
                    PointPair pp = listOfEdges.get(i);
                    if ((pp.p1.getX() == A.getX() && pp.p1.getY() == A.getY() && pp.p2.getX() == F.getX() && pp.p2.getY() == F.getY()) ||
                            (pp.p1.getX() == F.getX() && pp.p1.getY() == F.getY() && pp.p2.getX() == A.getX() && pp.p2.getY() == A.getY()) ||
                            (pp.p1.getX() == B.getX() && pp.p1.getY() == B.getY() && pp.p2.getX() == F.getX() && pp.p2.getY() == F.getY()) ||
                            (pp.p1.getX() == F.getX() && pp.p1.getY() == F.getY() && pp.p2.getX() == B.getX() && pp.p2.getY() == B.getY())) {
                        listOfEdges.remove(i);
                    }
                }


                A = tt1.s1;
                B = tt1.s2;
                C = tt1.s3;
                F = new Gateway(4);
                F.setX(tt1.fermatPoint.getX());
                F.setY(tt1.fermatPoint.getY());


                max = Math.max(tt1.getGain(), Math.max(tt2.getGain(), tt3.getGain()));
            } else if (max == tt2.getGain()) {
                for (int i = listOfEdges.size() - 1; i >= 0; i--) {
                    PointPair pp = listOfEdges.get(i);
                    if ((pp.p1.getX() == A.getX() && pp.p1.getY() == A.getY() && pp.p2.getX() == F.getX() && pp.p2.getY() == F.getY()) ||
                            (pp.p1.getX() == F.getX() && pp.p1.getY() == F.getY() && pp.p2.getX() == A.getX() && pp.p2.getY() == A.getY()) ||
                            (pp.p1.getX() == C.getX() && pp.p1.getY() == C.getY() && pp.p2.getX() == F.getX() && pp.p2.getY() == F.getY()) ||
                            (pp.p1.getX() == F.getX() && pp.p1.getY() == F.getY() && pp.p2.getX() == C.getX() && pp.p2.getY() == C.getY())) {
                        listOfEdges.remove(i);
                    }
                }


                A = tt2.s1;
                B = tt2.s2;
                C = tt2.s3;
                F = new Gateway(4);
                F.setX(tt2.fermatPoint.getX());
                F.setY(tt2.fermatPoint.getY());
                max = Math.max(tt1.getGain(), Math.max(tt2.getGain(), tt3.getGain()));
            } else {
                for (int i = listOfEdges.size() - 1; i >= 0; i--) {
                    PointPair pp = listOfEdges.get(i);
                    if ((pp.p1.getX() == B.getX() && pp.p1.getY() == B.getY() && pp.p2.getX() == F.getX() && pp.p2.getY() == F.getY()) ||
                            (pp.p1.getX() == F.getX() && pp.p1.getY() == F.getY() && pp.p2.getX() == B.getX() && pp.p2.getY() == B.getY()) ||
                            (pp.p1.getX() == C.getX() && pp.p1.getY() == C.getY() && pp.p2.getX() == F.getX() && pp.p2.getY() == F.getY()) ||
                            (pp.p1.getX() == F.getX() && pp.p1.getY() == F.getY() && pp.p2.getX() == C.getX() && pp.p2.getY() == C.getY())) {
                        listOfEdges.remove(i);
                    }
                }


                A = tt3.s1;
                B = tt3.s2;
                C = tt3.s3;
                F = new Gateway(4);
                F.setX(tt3.fermatPoint.getX());
                F.setY(tt3.fermatPoint.getY());
                max = Math.max(tt1.getGain(), Math.max(tt2.getGain(), tt3.getGain()));
            }

            listOfFPs.add(new Point.Double(F.getX(), F.getY()));

            listOfEdges.add(new PointPair(A.getX(), A.getY(), F.getX(), F.getY()));
            listOfEdges.add(new PointPair(B.getX(), B.getY(), F.getX(), F.getY()));
            listOfEdges.add(new PointPair(C.getX(), C.getY(), F.getX(), F.getY()));
        }
    }

    public int compareTo(Object o) {
        return 0;
    }


    public int getCumulativeGain() {
        int w = listOfFPs.size();

        for (int i = 0; i < listOfEdges.size(); i++) {
            PointPair pp = listOfEdges.get(i);
            w += Math.ceil(AnalyticGeometry.EstimatedDistance(pp.p1, pp.p2) / Constants.RNTransmissionRange) - 1;
        }

        return gain;
    }

    public static void main(String[] args) {
        Gateway A = new Gateway(0);
        A.setX(70);
        A.setY(20);
        Gateway B = new Gateway(1);
        B.setX(50);
        B.setY(116.60);
        Gateway C = new Gateway(2);
        C.setX(90);
        C.setY(116.6);

        Triangle t = new Triangle(A, B, C);
        RecursiveTriangle rt = new RecursiveTriangle(t);


    }

    public int gain() {

        return 0;
    }


    private class PointPair {
        Point2D p1;
        Point2D p2;

        private PointPair(Point2D p1, Point2D p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        private PointPair(double x1, double y1, double x2, double y2) {
            this.p1 = new Point2D.Double(x1, y1);
            this.p2 = new Point2D.Double(x2, y2);
        }
    }
}
