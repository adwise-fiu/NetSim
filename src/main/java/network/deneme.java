package network;

import geometry.AnalyticGeometry;

import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * User: Fatih
 * Date: Mar 3, 2009
 * Time: 6:17:32 PM
 */
public class deneme {
    public static void main(String[] args) {
//        double a = 152.0 / 75.0;
//        System.out.println(a);
//        System.out.println(Math.ceil(a));
        DecimalFormat df = new DecimalFormat("#.##");
//        double a = (double) minDist / Constants.RNTransmissionRange;
//        int req = (int) Math.ceil(a) - 1;
        SensorAndActorNetwork network = new SensorAndActorNetwork();
        for (int p = 15; p <= 15; p++) {
            for (int e = 1; e <= 50; e++) {
                String filename = p + "_" + e + ".dat";
                network.reload(filename);
                ArrayList<Gateway> terminals = network.getActorsArray();
                ArrayList<Triangle> dts = network.findDelaunayTriangles(terminals);
//                System.out.println(" num of triangles = " + dts.size() + "\n-------------------------");
//                System.out.println("------------------------------------------------------");
                for (int i = 0; i < dts.size(); i++) {
                    Triangle triangle = dts.get(i);
                    Point2D A = new Point2D.Double(triangle.s1.getX(), triangle.s1.getY());
                    Point2D B = new Point2D.Double(triangle.s2.getX(), triangle.s2.getY());
                    Point2D C = new Point2D.Double(triangle.s3.getX(), triangle.s3.getY());
                    double abc = AnalyticGeometry.findAngle(A, B, C);
                    double acb = AnalyticGeometry.findAngle(A, C, B);
                    double bac = AnalyticGeometry.findAngle(B, A, C);
                    double ab = AnalyticGeometry.EstimatedDistance(A, B);
                    double ac = AnalyticGeometry.EstimatedDistance(A, C);
                    double bc = AnalyticGeometry.EstimatedDistance(B, C);
                    int gain = triangle.getGain();

                    double maxangle = Math.max(abc, Math.max(acb, bac));
                    double maxedge = Math.max(ab, Math.max(ac, bc));
                    if (ab <= maxedge && ac <= maxedge && bc >= maxedge) {
                        System.out.println(df.format(maxangle) + " - " + df.format(ab) + " - " + df.format(ac) + " - " + gain);
                    }
                    else if (bc <= maxedge && ac <= maxedge && ab >= maxedge) {
                        System.out.println(df.format(maxangle) + " - " + df.format(bc) + " - " + df.format(ac) + " - " + gain);
                    }
                    else if (ab <= maxedge && bc <= maxedge && ac >= maxedge) {
                        System.out.println(df.format(maxangle) + " - " + df.format(ab) + " - " + df.format(bc) + " - " + gain);
                    }


//                    System.out.println("[ " + df.format(abc) + ", " + df.format(acb) + ", " + df.format(bac) + " ] - [ " + df.format(ab) + ", " + df.format(ac) + ", " + df.format(bc) + " ] - " + gain);
//                    if (triangle.getGain() > 0) {
//                        Point2D A = new Point2D.Double(triangle.s1.getX(), triangle.s1.getY());
//                        Point2D B = new Point2D.Double(triangle.s2.getX(), triangle.s2.getY());
//                        Point2D C = new Point2D.Double(triangle.s3.getX(), triangle.s3.getY());
//                        double afb = AnalyticGeometry.findAngle(A, triangle.fermatPoint, B);
//                        double afc = AnalyticGeometry.findAngle(A, triangle.fermatPoint, C);
//                        double bfc = AnalyticGeometry.findAngle(B, triangle.fermatPoint, C);
//                        double sum = afb + afc + bfc;
//                        System.out.println("[" + triangle.s1.getID() + "," + triangle.s2.getID() + "," + triangle.s3.getID() + "] = [" + df.format(afb) + ", " + df.format(afc) + ", " + df.format(bfc) + ", " + sum + "]  -> Gain = "+triangle.getGain());
//
//                        Gateway fermat = new Gateway(-1);
//                        fermat.setLocation(triangle.fermatPoint);
//
//                        Triangle t1 = new Triangle(triangle.s1, fermat, triangle.s2);
//                        Triangle t2 = new Triangle(triangle.s1, fermat, triangle.s3);
//                        Triangle t3 = new Triangle(triangle.s2, fermat, triangle.s3);
//                        if (t1.getGain() > 0) {
//                            System.out.println("t1 positive gain");
//                        }
//                        if (t2.getGain() > 0) {
//                            System.out.println("t2 positive gain");
//                        }
//                        if (t3.getGain() > 0) {
//                            System.out.println("t3 positive gain");
//                        }
//                    }

                }
            }
        }


//        String filename = "15_21.dat";

    }
}
