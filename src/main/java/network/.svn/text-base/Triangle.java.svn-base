package network;

import geometry.AnalyticGeometry;
import utils.NetworkUtils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author: Fatih
 * Date: Nov 2, 2010
 * Time: 11:50:40 PM
 */
public class Triangle implements Comparable {
    double err_dif = 0.1;
    double R = Constants.RNTransmissionRange;
    static HashMap<Integer, Integer> sameComponentMap = new HashMap<Integer, Integer>();
    static int nextCCID = 1;
    public Gateway s1, s2, s3;
//    Point2D centroid;
    Point2D fermatPoint;
    Point2D ActualfermatPoint;
    int ccid = 0;
    boolean active = true;
    // number of relay nodes required if we connect the corners of the triangle at the centroid
//    int c_weight = 0;

    // number of relay nodes required if we connect the corners of the triangle at the fermat point
    int f_weight = 0;

    // number of relay nodes required if we connect the corners of the triangle by steinerizing shortest edges.
    int p_weight = 0;
    boolean b12 = false, b13 = false, b23 = false;
    int rho = 0;

    // this attribute is true if the segments are still connected even if we remove the center point
    boolean removeCenter = false;

    public Triangle(Gateway s1, Gateway s2, Gateway s3) {
        this.s1 = s1;
        this.s2 = s2;
        this.s3 = s3;
        fermatPoint = calculateFermatPoint();

        if (fermatPoint != null) {
            f_weight = getWeightAtPoint(fermatPoint);
        } else {
            System.out.println("FermatPoint null");
            System.exit(0);
        }


        double s1s2 = Math.ceil(NetworkUtils.EstimatedDistance(s1, s2) / R) - 1;
        double s1s3 = Math.ceil(NetworkUtils.EstimatedDistance(s1, s3) / R) - 1;
        double s2s3 = Math.ceil(NetworkUtils.EstimatedDistance(s2, s3) / R) - 1;

        p_weight = (int) (s1s2 + s1s3 + s2s3 - (Math.max(s1s2, Math.max(s1s3, s2s3))));

        rho = Math.min(f_weight, p_weight);
    }

    private Point2D calculateFermatPoint() {
        // let s1 = A, s2 = B and s3 = C
        double error = Constants.err * R - err_dif;
        Point2D A = new Point2D.Double(s1.x, s1.y);
        Point2D B = new Point2D.Double(s2.x, s2.y);
        Point2D C = new Point2D.Double(s3.x, s3.y);
        Point2D P;
        if (AnalyticGeometry.findAngle(A, B, C) > 120) {
            P = checkSavingPoint(A, B, C);
            return P;
        } else if (AnalyticGeometry.findAngle(B, A, C) > 120) {
            P = checkSavingPoint(B, A, C);
            return P;
        } else if (AnalyticGeometry.findAngle(A, C, B) > 120) {
            P = checkSavingPoint(A, C, B);
            return P;
        } else {


            Point2D F = null, D = null;
            double ds1s2 = NetworkUtils.EstimatedDistance(s1, s2);
            double ds2s3 = NetworkUtils.EstimatedDistance(s2, s3);
            ArrayList<Point2D> possible_f = AnalyticGeometry.intersectionOfTwoCircles(s1.x, s1.y, ds1s2, s2.x, s2.y, ds1s2);
            for (int i = 0; i < possible_f.size(); i++) {
                Point2D pfi = possible_f.get(i);
                int rs3 = AnalyticGeometry.findRegionWRTLine(s1.x, s1.y, s2.x, s2.y, s3.x, s3.y);
                int rpfi = AnalyticGeometry.findRegionWRTLine(s1.x, s1.y, s2.x, s2.y, pfi.getX(), pfi.getY());
                if (rs3 * rpfi == -1) {
                    F = pfi;
                }
            }


            ArrayList<Point2D> possible_d = AnalyticGeometry.intersectionOfTwoCircles(s2.x, s2.y, ds2s3, s3.x, s3.y, ds2s3);
            for (int i = 0; i < possible_d.size(); i++) {
                Point2D pdi = possible_d.get(i);
                int rs1 = AnalyticGeometry.findRegionWRTLine(s2.x, s2.y, s3.x, s3.y, s1.x, s1.y);
                int rpdi = AnalyticGeometry.findRegionWRTLine(s2.x, s2.y, s3.x, s3.y, pdi.getX(), pdi.getY());
                if (rs1 * rpdi == -1) {
                    D = pdi;
                }
            }
            if (F == null || D == null) {
                return null;
            } else {
                int WA, WA_prime = Integer.MAX_VALUE, WB, WB_prime = Integer.MAX_VALUE, WC, WC_prime = Integer.MAX_VALUE, WP;
                P = AnalyticGeometry.intersectionOfTwoLines(s1.x, s1.y, D.getX(), D.getY(), s3.x, s3.y, F.getX(), F.getY());
                WP = getWeightAtPoint(P);

                // |AP| = k1*R+m1, |BP|=k2*R+m2 and |CP| =k3*R+m3

                int k1 = (int) Math.ceil(AnalyticGeometry.EstimatedDistance(A, P) / R) - 1;
                double m1 = AnalyticGeometry.EstimatedDistance(A, P) - k1 * R;
                int k2 = (int) Math.ceil(AnalyticGeometry.EstimatedDistance(B, P) / R) - 1;
                double m2 = AnalyticGeometry.EstimatedDistance(B, P) - k2 * R;
                int k3 = (int) Math.ceil(AnalyticGeometry.EstimatedDistance(C, P) / R) - 1;
                double m3 = AnalyticGeometry.EstimatedDistance(C, P) - k3 * R;

                if (k1 == -1) {
                    P = checkSavingPoint(B, A, C);
                    return P;
                } else if (k2 == -1) {
                    P = checkSavingPoint(A, B, C);
                    return P;
                } else if (k3 == -1) {
                    P = checkSavingPoint(A, C, B);
                    return P;
                }


                Point2D PA = null, PA_prime = null, PB = null, PB_prime = null, PC = null, PC_prime = null;
                if (k1 == 0) {
                    PA_prime = P;
                    WA_prime = getWeightAtPoint(PA_prime);

                    if (((Math.ceil(AnalyticGeometry.EstimatedDistance(B, A) / R) - 1) + (Math.ceil(AnalyticGeometry.EstimatedDistance(C, A) / R) - 1)) < WA_prime) {
                        PA_prime = A;
                        WA_prime = getWeightAtPoint(PA_prime);
                    }

                } else {
                    ArrayList<Point2D> points = AnalyticGeometry.intersectionOfTwoCircles(B, error + (k2 + 1) * R, C, error + (k3 + 1) * R);
                    if (points == null) {
                        System.out.println("ERROR points null");
                        System.exit(0);
                    } else if (points.size() == 1) {
                        PA_prime = points.get(0);
                        WA_prime = getWeightAtPoint(PA_prime);
                    } else if (points.size() == 2) {
                        if (AnalyticGeometry.EstimatedDistance(A, points.get(0)) < AnalyticGeometry.EstimatedDistance(A, points.get(1))) {
                            PA_prime = points.get(0);
                        } else {
                            PA_prime = points.get(1);
                        }
//                        System.out.println(AnalyticGeometry.euclideanDistance(PA_prime,A)/100);
                        WA_prime = getWeightAtPoint(PA_prime);
                    }

                }

                ArrayList<Point2D> list = AnalyticGeometry.intersectionOfTwoCircles(B, error + k2 * R, C, error + k3 * R);
                if (list == null) {
                    WA = Integer.MAX_VALUE;
                } else {
                    if (list.isEmpty() || list.size() > 2) {
                        System.out.println("ERROR Fermat Point");
                        System.exit(0);
                    }
                    if (list.size() == 1) {
                        PA = list.get(0);
                    } else {
                        if (AnalyticGeometry.EstimatedDistance(A, list.get(0)) < AnalyticGeometry.EstimatedDistance(A, list.get(1))) {
                            PA = list.get(0);
                        } else {
                            PA = list.get(1);
                        }
                    }
                    WA = getWeightAtPoint(PA);
                    list.clear();
                }
                if (k2 == 0) {
                    PB_prime = P;
                    WB_prime = getWeightAtPoint(PB_prime);

                    if (((Math.ceil(AnalyticGeometry.EstimatedDistance(A, B) / R) - 1) + (Math.ceil(AnalyticGeometry.EstimatedDistance(C, B) / R) - 1)) < WB_prime) {
                        PB_prime = B;
                        WB_prime = getWeightAtPoint(PB_prime);
                    }


                } else {
                    ArrayList<Point2D> points = AnalyticGeometry.intersectionOfTwoCircles(A, error + (k1 + 1) * R, C, error + (k3 + 1) * R);
                    if (points == null) {
                        System.out.println("ERROR points null");
                        System.exit(0);
                    } else if (points.size() == 1) {
                        PB_prime = points.get(0);
                        WB_prime = getWeightAtPoint(PB_prime);
                    } else if (points.size() == 2) {
                        if (AnalyticGeometry.EstimatedDistance(B, points.get(0)) < AnalyticGeometry.EstimatedDistance(B, points.get(1))) {
                            PB_prime = points.get(0);
                        } else {
                            PB_prime = points.get(1);
                        }
                        WB_prime = getWeightAtPoint(PB_prime);
                    }
                }

                list = AnalyticGeometry.intersectionOfTwoCircles(A, error + k1 * R, C, error + k3 * R);
                if (list == null) {
                    WB = Integer.MAX_VALUE;
                } else {
                    if (list.isEmpty() || list.size() > 2) {
                        System.out.println("ERROR Fermat Point");
                        System.exit(0);
                    }
                    if (list.size() == 1) {
                        PB = list.get(0);
                    } else {
                        if (AnalyticGeometry.EstimatedDistance(B, list.get(0)) < AnalyticGeometry.EstimatedDistance(B, list.get(1))) {
                            PB = list.get(0);
                        } else {
                            PB = list.get(1);
                        }
                    }
                    WB = getWeightAtPoint(PB);
                    list.clear();
                }
                if (k3 == 0) {
                    PC_prime = P;
                    WC_prime = getWeightAtPoint(PC_prime);

                    if (((Math.ceil(AnalyticGeometry.EstimatedDistance(A, C) / R) - 1) + (Math.ceil(AnalyticGeometry.EstimatedDistance(B, C) / R) - 1)) < WC_prime) {
                        PC_prime = C;
                        WC_prime = getWeightAtPoint(PC_prime);
                    }


                } else {
                    ArrayList<Point2D> points = AnalyticGeometry.intersectionOfTwoCircles(A, error + (k1 + 1) * R, B, error + (k2 + 1) * R);
                    if (points == null) {
                        System.out.println("ERROR points null");
                        System.exit(0);
                    } else if (points.size() == 1) {
                        PC_prime = points.get(0);
                        WC_prime = getWeightAtPoint(PC_prime);
                    } else if (points.size() == 2) {
                        if (AnalyticGeometry.EstimatedDistance(C, points.get(0)) < AnalyticGeometry.EstimatedDistance(C, points.get(1))) {
                            PC_prime = points.get(0);
                        } else {
                            PC_prime = points.get(1);
                        }
                        WC_prime = getWeightAtPoint(PC_prime);
                    }
                }

                list = AnalyticGeometry.intersectionOfTwoCircles(A, error + k1 * R, B, error + k2 * R);
                if (list == null) {
                    WC = Integer.MAX_VALUE;
                } else {
                    if (list.isEmpty() || list.size() > 2) {
                        System.out.println("ERROR Fermat Point");
                        System.exit(0);
                    }
                    if (list.size() == 1) {
                        PC = list.get(0);
                    } else {
                        if (AnalyticGeometry.EstimatedDistance(C, list.get(0)) < AnalyticGeometry.EstimatedDistance(C, list.get(1))) {
                            PC = list.get(0);
                        } else {
                            PC = list.get(1);
                        }
                    }
                    WC = getWeightAtPoint(PC);
                    list.clear();
                }
                ActualfermatPoint = P;
                int minWeight = Math.min(WP, Math.min(WA, Math.min(WA_prime, Math.min(WB, Math.min(WB_prime, Math.min(WC, WC_prime))))));
                if (minWeight == WP) {
                    return P;
                } else if (minWeight == WA) {
                    return PA;
                } else if (minWeight == WB) {
                    return PB;
                } else if (minWeight == WC) {
                    return PC;
                } else if (minWeight == WA_prime) {
                    return PA_prime;
                } else if (minWeight == WB_prime) {
                    return PB_prime;
                } else {
                    return PC_prime;
                }
            }
        }
    }

    public ArrayList<Gateway> getUncoveredNodes() {
        ArrayList<Gateway> result = new ArrayList<Gateway>();

        if (s1.getCcid() == 0) {
            result.add(s1);
        }
        if (s2.getCcid() == 0) {
            result.add(s2);
        }
        if (s3.getCcid() == 0) {
            result.add(s3);
        }

        return result;
    }

    public ArrayList<Gateway> getCoveredNodes() {
        ArrayList<Gateway> result = new ArrayList<Gateway>();

        if (s1.getCcid() != 0) {
            result.add(s1);
        }
        if (s2.getCcid() != 0) {
            result.add(s2);
        }
        if (s3.getCcid() != 0) {
            result.add(s3);
        }

        return result;
    }

    /**
     * Min Weight of a gateway in a triangle is the minimum number of relay nodes to steinerize the shortest edge where one of the end point of the edge is s
     *
     * @param s source node
     * @return minimum number of relay nodes to connect s to its closest neighbor
     */
    public int getMinWeightOf(Gateway s) {
        int d1 = 0, d2 = 0;
        if (s == s1) {
            d1 = (int) Math.ceil(NetworkUtils.EstimatedDistance(s1, s2) / R) - 1;
            d2 = (int) Math.ceil(NetworkUtils.EstimatedDistance(s1, s3) / R) - 1;
        }
        if (s == s2) {
            d1 = (int) Math.ceil(NetworkUtils.EstimatedDistance(s2, s1) / R) - 1;
            d2 = (int) Math.ceil(NetworkUtils.EstimatedDistance(s2, s3) / R) - 1;
        }
        if (s == s3) {
            d1 = (int) Math.ceil(NetworkUtils.EstimatedDistance(s3, s1) / R) - 1;
            d2 = (int) Math.ceil(NetworkUtils.EstimatedDistance(s3, s2) / R) - 1;
        }
        return Math.min(d1, d2);
    }


    public int intersectionCardinality(Triangle t) {
        HashSet<Gateway> set1 = new HashSet<Gateway>();
        set1.add(s1);
        set1.add(s2);
        set1.add(s3);

        HashSet<Gateway> set2 = new HashSet<Gateway>();
        set2.add(t.s1);
        set2.add(t.s2);
        set2.add(t.s3);

        set1.retainAll(set2);
        return set1.size();

    }

    public boolean isAdjacent(Triangle t) {
        if (t.s1 == s1) {
            return t.s2 == s2 || t.s2 == s3 || t.s3 == s2 || t.s3 == s3;
        } else if (t.s1 == s2) {
            return t.s2 == s1 || t.s2 == s3 || t.s3 == s1 || t.s3 == s3;
        } else if (t.s1 == s3) {
            return t.s2 == s1 || t.s2 == s2 || t.s3 == s1 || t.s3 == s2;
        } else return (t.s2 == s2 && t.s3 == s3) && (t.s2 == s3 && t.s3 == s2);
    }

    public int compareTo(Object o) {
        Triangle t = (Triangle) o;
        if (rho < t.rho)
            return -1;
        else if (rho > t.rho)
            return 1;
        else
            return 0;
    }

    public ArrayList<Gateway> steinerizeTriangle(ArrayList<Gateway> ActorsArray, ArrayList<Gateway> RelayNodeArray) {
        if (p_weight == rho) {
            return p_steinerizeTriangle(ActorsArray, RelayNodeArray);
        } else {
            return c_steinerizeTriangle(ActorsArray, RelayNodeArray);
        }
    }

    private ArrayList<Gateway> p_steinerizeTriangle(ArrayList<Gateway> ActorsArray, ArrayList<Gateway> RelayNodeArray) {
        ArrayList<Gateway> result = new ArrayList<Gateway>();
        int ccid = getCCID();
        s1.setCcid(ccid);
        s2.setCcid(ccid);
        s3.setCcid(ccid);
        double d12 = NetworkUtils.EstimatedDistance(s1, s2);
        double d13 = NetworkUtils.EstimatedDistance(s1, s3);
        double d23 = NetworkUtils.EstimatedDistance(s2, s3);

        if (d12 == d13 && d12 == d23) {
            //equilateral triangle
            b12 = true;
            b13 = true;
            b23 = false;
        } else if (d12 == d13) {
            if (d23 < d12) {
                b12 = true;
                b13 = false;
                b23 = true;
            } else {
                b12 = true;
                b13 = true;
                b23 = false;
            }
        } else if (d12 == d23) {
            if (d13 < d12) {
                b12 = true;
                b13 = true;
                b23 = false;
            } else {
                b12 = true;
                b13 = false;
                b23 = true;
            }
        } else if (d13 == d23) {
            if (d12 < d13) {
                b12 = true;
                b13 = true;
                b23 = false;
            } else {
                b12 = false;
                b13 = true;
                b23 = true;
            }
        } else {

            b12 = Math.max(d12, Math.max(d13, d23)) != d12;
            b13 = Math.max(d12, Math.max(d13, d23)) != d13;
            b23 = Math.max(d12, Math.max(d13, d23)) != d23;
        }

        if (b12) {
            if (NetworkUtils.EstimatedDistance(s1, s2) > R) {
                result.addAll(NetworkUtils.fillGap(s1, s2, true, ccid, ActorsArray, RelayNodeArray));
            }
        }
        if (b13) {
            if (NetworkUtils.EstimatedDistance(s1, s3) > R) {
                result.addAll(NetworkUtils.fillGap(s1, s3, true, ccid, ActorsArray, RelayNodeArray));
            }
        }
        if (b23) {
            if (NetworkUtils.EstimatedDistance(s2, s3) > R) {
                result.addAll(NetworkUtils.fillGap(s2, s3, true, ccid, ActorsArray, RelayNodeArray));
            }
        }
        return result;

    }

    private ArrayList<Gateway> c_steinerizeTriangle(ArrayList<Gateway> ActorsArray, ArrayList<Gateway> RelayNodeArray) {
        ArrayList<Gateway> result = new ArrayList<Gateway>();
        int ccid = getCCID();
        s1.setCcid(ccid);
        s2.setCcid(ccid);
        s3.setCcid(ccid);
//        Gateway fer = new Gateway(RelayNodeArray.size());
//        fer.isRelay = true;
//        fer.setX(ActualfermatPoint.getX());
//        fer.setY(ActualfermatPoint.getY());
//        fer.setCcid(ccid);
//        RelayNodeArray.add(fer);


        Gateway center_rn = new Gateway(RelayNodeArray.size());
        center_rn.isRelay = true;
//        boolean cntr = false;
//        if (cntr) {
//            center_rn.setX(centroid.getX());
//            center_rn.setY(centroid.getY());
//        } else {
        center_rn.setX(fermatPoint.getX());
        center_rn.setY(fermatPoint.getY());
//        }
        center_rn.setCcid(ccid);
//            if (!removeCenter) {
        RelayNodeArray.add(center_rn);
        result.add(center_rn);
//            }
        if (NetworkUtils.EstimatedDistance(s1, center_rn) > R) {
            result.addAll(NetworkUtils.fillGap(s1, center_rn, true, ccid, ActorsArray, RelayNodeArray));
        }
        if (NetworkUtils.EstimatedDistance(s2, center_rn) > R) {
            result.addAll(NetworkUtils.fillGap(s2, center_rn, true, ccid, ActorsArray, RelayNodeArray));
        }
        if (NetworkUtils.EstimatedDistance(s3, center_rn) > R) {
            result.addAll(NetworkUtils.fillGap(s3, center_rn, true, ccid, ActorsArray, RelayNodeArray));
        }
        active = false;
        return result;
    }

    private int getCCID() {
        if (s1.getCcid() == 0 && s2.getCcid() == 0 && s3.getCcid() == 0) {
            nextCCID++;
            return nextCCID - 1;
        } else if (s1.getCcid() == 0 && s2.getCcid() == 0 && s3.getCcid() != 0) {
            return s3.getCcid();
        } else if (s1.getCcid() == 0 && s2.getCcid() != 0 && s3.getCcid() == 0) {
            return s2.getCcid();
        } else if (s1.getCcid() != 0 && s2.getCcid() == 0 && s3.getCcid() == 0) {
            return s1.getCcid();
        } else if (s1.getCcid() != 0 && s2.getCcid() != 0 && s3.getCcid() == 0) {
            int min = Math.min(s1.getCcid(), s2.getCcid());
            int max = Math.max(s1.getCcid(), s2.getCcid());
            sameComponentMap.put(max, min);
            return min;
        } else if (s1.getCcid() != 0 && s2.getCcid() == 0 && s3.getCcid() != 0) {
            int min = Math.min(s1.getCcid(), s3.getCcid());
            int max = Math.max(s1.getCcid(), s3.getCcid());
            sameComponentMap.put(max, min);
            return min;
        } else if (s1.getCcid() == 0 && s2.getCcid() != 0 && s3.getCcid() != 0) {
            int min = Math.min(s2.getCcid(), s3.getCcid());
            int max = Math.max(s2.getCcid(), s3.getCcid());
            sameComponentMap.put(max, min);
            return min;
        } else {
            int min = Math.min(s1.getCcid(), Math.min(s2.getCcid(), s3.getCcid()));
            if (s1.getCcid() == min) {
                sameComponentMap.put(s2.getCcid(), min);
                sameComponentMap.put(s3.getCcid(), min);
            } else if (s2.getCcid() == min) {
                sameComponentMap.put(s1.getCcid(), min);
                sameComponentMap.put(s3.getCcid(), min);
            } else {
                sameComponentMap.put(s2.getCcid(), min);
                sameComponentMap.put(s3.getCcid(), min);
            }
            return min;
        }
    }


    public boolean isEligible() {
        return active && (s1.getCcid() == 0 && s2.getCcid() == 0 && s3.getCcid() == 0 || !(s1.getCcid() == s2.getCcid() || s1.getCcid() == s3.getCcid() || s2.getCcid() == s3.getCcid()));
    }


    public void setActive(boolean active) {
        this.active = active;
    }


    public String toString() {
        String result = "[ ";
        if (s1.isRelay) {
            result += "R" + s1.getID();
        } else {
            result += "S" + s1.getID();
        }
        result += ", ";
        if (s2.isRelay) {
            result += "R" + s2.getID();
        } else {
            result += "S" + s2.getID();
        }
        result += ", ";
        if (s3.isRelay) {
            result += "R" + s3.getID();
        } else {
            result += "S" + s3.getID();
        }
        result += "] F = " + f_weight + " - P = " + p_weight;

        return result;
    }

    private Point2D findMeetingPoint() {
        double d1, d2, d3;
        int minX = (int) Math.round(Math.min(s1.getX(), Math.min(s2.getX(), s3.getX())));
        int maxX = (int) Math.round(Math.max(s1.getX(), Math.max(s2.getX(), s3.getX())));

        int minY = (int) Math.round(Math.min(s1.getY(), Math.min(s2.getY(), s3.getY())));
        int maxY = (int) Math.round(Math.max(s1.getY(), Math.max(s2.getY(), s3.getY())));

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;

//            int weight[][] = new int[height][width];
        Point2D center = new Point2D.Double();
        int minWeight = Integer.MAX_VALUE;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {

                double x = minX + j;
                double y = minY + i;

                d1 = AnalyticGeometry.EstimatedDistance(s1.getX(), s1.getY(), x, y);
                d2 = AnalyticGeometry.EstimatedDistance(s2.getX(), s2.getY(), x, y);
                d3 = AnalyticGeometry.EstimatedDistance(s3.getX(), s3.getY(), x, y);


                int w1 = (int) Math.ceil(d1 / R) - 1;
                int w2 = (int) Math.ceil(d2 / R) - 1;
                int w3 = (int) Math.ceil(d3 / R) - 1;

                int w = w1 + w2 + w3 + 1;
                if (w <= minWeight) {
                    minWeight = w;
                    center.setLocation(x, y);
                }
            }
        }

        d1 = AnalyticGeometry.EstimatedDistance(s1.getX(), s1.getY(), center.getX(), center.getY());
        d2 = AnalyticGeometry.EstimatedDistance(s2.getX(), s2.getY(), center.getX(), center.getY());
        d3 = AnalyticGeometry.EstimatedDistance(s3.getX(), s3.getY(), center.getX(), center.getY());

        double rd1 = d1 % R;
        double rd2 = d2 % R;
        double rd3 = d3 % R;

        Point2D q1 = AnalyticGeometry.getCoordinates(center.getX(), center.getY(), s1.getX(), s1.getY(), rd1);
        Point2D q2 = AnalyticGeometry.getCoordinates(center.getX(), center.getY(), s2.getX(), s2.getY(), rd2);
        Point2D q3 = AnalyticGeometry.getCoordinates(center.getX(), center.getY(), s3.getX(), s3.getY(), rd3);

        boolean b12 = AnalyticGeometry.EstimatedDistance(q1.getX(), q1.getY(), q2.getX(), q2.getY()) <= R;
        boolean b13 = AnalyticGeometry.EstimatedDistance(q1.getX(), q1.getY(), q3.getX(), q3.getY()) <= R;
        boolean b23 = AnalyticGeometry.EstimatedDistance(q2.getX(), q2.getY(), q3.getX(), q3.getY()) <= R;

        removeCenter = (b12 && b13) || (b12 && b23) || (b13 && b23);

        return center;
    }

    /**
     * Finds a steiner point to connect 3 points by calculating Gain/Loss Ratio
     *
     * @return Steiner Point
     * @deprecated This function is compared with the result findMeetingPoint() and findMeetingPoint() outperformed this function
     *             We keep this function for our records and will never use, we will use findMeetingPoint instead
     */
    public Point2D findSPWithkLCA() {
        int minX = (int) Math.round(Math.min(s1.getX(), Math.min(s2.getX(), s3.getX())));
        int maxX = (int) Math.round(Math.max(s1.getX(), Math.max(s2.getX(), s3.getX())));

        int minY = (int) Math.round(Math.min(s1.getY(), Math.min(s2.getY(), s3.getY())));
        int maxY = (int) Math.round(Math.max(s1.getY(), Math.max(s2.getY(), s3.getY())));

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;


        double s1s2 = NetworkUtils.EstimatedDistance(s1, s2);
        double s1s3 = NetworkUtils.EstimatedDistance(s1, s3);
        double s2s3 = NetworkUtils.EstimatedDistance(s2, s3);

        int w12 = (int) Math.ceil(s1s2 / R) - 1;
        int w13 = (int) Math.ceil(s1s3 / R) - 1;
        int w23 = (int) Math.ceil(s2s3 / R) - 1;

        int mstWeight = w12 + w13 + w23 - Math.max(w12, Math.max(w13, w23));
//            double mstLength = s1s2 + s1s3 + s2s3 - Math.max(s1s2, Math.max(s1s3, s2s3));

//            int weight[][] = new int[height][width];
        Point2D center = new Point2D.Double();
        double maxRatio = -1000000;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                double x = minX + j;
                double y = minY + i;
                double d1 = AnalyticGeometry.euclideanDistance(s1.getX(), s1.getY(), x, y);
                double d2 = AnalyticGeometry.euclideanDistance(s2.getX(), s2.getY(), x, y);
                double d3 = AnalyticGeometry.euclideanDistance(s3.getX(), s3.getY(), x, y);

                int w1 = (int) Math.ceil(d1 / R);
                int w2 = (int) Math.ceil(d2 / R);
                int w3 = (int) Math.ceil(d3 / R);


                int gain = mstWeight - (w1 + w2 + w3 - 2);
                int loss = Math.min(w1, Math.min(w2, w3));

//                    double gain = mstLength - (d1+d2+d3);
//                    double loss = Math.min(d1, Math.min(d2, d3));

                double gain_loss_ratio = gain / loss;
                if (gain_loss_ratio > maxRatio) {
                    maxRatio = gain_loss_ratio;
                    center.setLocation(x, y);
                }
            }
        }
        return center;
    }

    public ArrayList<Gateway> getCorners() {
        ArrayList<Gateway> result = new ArrayList<Gateway>();
        result.add(s1);
        result.add(s2);
        result.add(s3);
        return result;
    }

    private int getWeightAtPoint(Point2D p) {
        Point2D A = new Point2D.Double(s1.x, s1.y);
        Point2D B = new Point2D.Double(s2.x, s2.y);
        Point2D C = new Point2D.Double(s3.x, s3.y);
        if (Math.abs(p.getX() - A.getX()) < Math.pow(10, -4) && Math.abs(p.getY() - A.getY()) < Math.pow(10, -4)) {
            double d1 = AnalyticGeometry.EstimatedDistance(A, B);
            double d2 = AnalyticGeometry.EstimatedDistance(A, C);
            return (int) (Math.ceil(d1 / R) + Math.ceil(d2 / R) - 2);
        } else if (Math.abs(p.getX() - B.getX()) < Math.pow(10, -4) && Math.abs(p.getY() - B.getY()) < Math.pow(10, -4)) {
            double d1 = AnalyticGeometry.EstimatedDistance(A, B);
            double d2 = AnalyticGeometry.EstimatedDistance(B, C);
            return (int) (Math.ceil(d1 / R) + Math.ceil(d2 / R) - 2);
        } else if (Math.abs(p.getX() - C.getX()) < Math.pow(10, -4) && Math.abs(p.getY() - C.getY()) < Math.pow(10, -4)) {
            double d1 = AnalyticGeometry.EstimatedDistance(B, C);
            double d2 = AnalyticGeometry.EstimatedDistance(A, C);
            return (int) (Math.ceil(d1 / R) + Math.ceil(d2 / R) - 2);
        } else {

            double d1 = AnalyticGeometry.EstimatedDistance(s1.getX(), s1.getY(), p.getX(), p.getY());
            double d2 = AnalyticGeometry.EstimatedDistance(s2.getX(), s2.getY(), p.getX(), p.getY());
            double d3 = AnalyticGeometry.EstimatedDistance(s3.getX(), s3.getY(), p.getX(), p.getY());

            return (int) (Math.ceil(d1 / R) + Math.ceil(d2 / R) + Math.ceil(d3 / R) - 2);
        }
    }

    // we consider angle at B point

    private Point2D checkSavingPoint(Point2D A, Point2D B, Point2D C) {
        double error = Constants.err * R - err_dif;
        int k1 = (int) Math.ceil(AnalyticGeometry.EstimatedDistance(A, B) / R) - 1;
        double m1 = AnalyticGeometry.EstimatedDistance(A, B) - k1 * R;
        int k2 = (int) Math.ceil(AnalyticGeometry.EstimatedDistance(C, B) / R) - 1;
        double m2 = AnalyticGeometry.EstimatedDistance(C, B) - k2 * R;

        if (m1 == 0 && m2 == 0) {
            return B;
        } else if ((m1 != 0 && m2 == 0) || (m1 == 0 && m2 != 0)) {
            ArrayList<Point2D> intersection = AnalyticGeometry.intersectionOfTwoCircles(A, error + k1 * R, C, error + k2 * R);
            if (intersection == null) {
                return B;
            } else {
                if (AnalyticGeometry.EstimatedDistance(intersection.get(0), B) <= R) {
                    return intersection.get(0);
                } else if (AnalyticGeometry.EstimatedDistance(intersection.get(1), B) <= R) {
                    return intersection.get(1);
                } else {
                    return B;
                }
            }
        } else {
            ArrayList<Point2D> intersection = AnalyticGeometry.intersectionOfTwoCircles(A, error + k1 * R, C, error + k2 * R);
            if (intersection != null) {
                Point2D X;
                if (AnalyticGeometry.EstimatedDistance(intersection.get(0), B) < AnalyticGeometry.EstimatedDistance(intersection.get(1), B)) {
                    X = intersection.get(0);
                } else {
                    X = intersection.get(1);
                }

                if (AnalyticGeometry.EstimatedDistance(X, B) <= 2 * R) {
                    return X;
                } else {
                    return B;
                }
            } else {
                intersection = AnalyticGeometry.intersectionOfTwoCircles(A, error + (k1 + 1) * R, C, error + k2 * R);
                if (intersection != null) {
                    Point2D X;
                    if (AnalyticGeometry.EstimatedDistance(intersection.get(0), B) < AnalyticGeometry.EstimatedDistance(intersection.get(1), B)) {
                        X = intersection.get(0);
                    } else {
                        X = intersection.get(1);
                    }
                    if (AnalyticGeometry.EstimatedDistance(X, B) <= R) {
                        return X;
                    } else {
                        return B;
                    }
                } else {
                    intersection = AnalyticGeometry.intersectionOfTwoCircles(A, error + k1 * R, C, error + (k2 + 1) * R);
                    if (intersection != null) {
                        Point2D X;
                        if (AnalyticGeometry.EstimatedDistance(intersection.get(0), B) < AnalyticGeometry.EstimatedDistance(intersection.get(1), B)) {
                            X = intersection.get(0);
                        } else {
                            X = intersection.get(1);
                        }
                        if (AnalyticGeometry.EstimatedDistance(X, B) <= R) {
                            return X;
                        } else {
                            return B;
                        }
                    } else {
                        return B;
                    }
                }
            }
        }
    }

    public int getGain(){
        return p_weight-f_weight;
    }

    public void draw(Graphics g) {
        g.drawLine((int) this.s1.getX(), (int) this.s1.getY(), (int) this.s2.getX(), (int) this.s2.getY());
        g.drawLine((int) this.s1.getX(), (int) this.s1.getY(), (int) this.s3.getX(), (int) this.s3.getY());
        g.drawLine((int) this.s2.getX(), (int) this.s2.getY(), (int) this.s3.getX(), (int) this.s3.getY());
    }

}
