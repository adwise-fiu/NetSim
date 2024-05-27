package utils;

import geometry.AnalyticGeometry;
import network.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author : Fatih Senel
 * Date: Apr 3, 2008
 * Time: 3:02:40 AM
 */
public class NetworkUtils {

    private static int visitTime = 0;

    public static void findNexthopSensorNodes(ArrayList<Sensor> sensorList) {
        for (int i = 0; i < sensorList.size(); i++) {
            Sensor sensor = sensorList.get(i);
            if (!sensor.isDominator()) {
                boolean isInOneHop = false;
                for (int j = 0; j < sensor.getNeighborList().size(); j++) {
                    Sensor neighbor = sensor.getNeighborList().get(j);
                    if (neighbor.getID() == sensor.getDominatorID()) {
                        isInOneHop = true;
                        break;
                    }
                }
                if (!isInOneHop) {
                    double minDist = Double.MAX_VALUE;
                    int minIndex = -1;
                    Sensor dominator = sensorList.get(sensor.getDominatorID());
                    for (int j = 0; j < sensor.getNeighborList().size(); j++) {
                        Sensor neighbor = sensor.getNeighborList().get(j);
                        double dist = Distance(dominator, neighbor);
                        if (dist <= minDist) {
                            minDist = dist;
                            minIndex = j;
                        }
                    }
                    sensor.setNexthopID(sensor.getNeighborList().get(minIndex).getID());
                    sensor.getNeighborList().get(minIndex).addToForwardTable(sensor);
                } else {
                    sensor.setNexthopID(sensor.getDominatorID());
                    sensorList.get(sensor.getDominatorID()).addToForwardTable(sensor);
                }
            }
        }
    }


    /**
     * Calculates the distance between two nodes
     *
     * @param node1 starting node
     * @param node2 ending node
     * @return distance
     */
    public static double Distance(NetworkNode node1, NetworkNode node2) {
        return Math.sqrt(((node1.getX() - node2.getX()) * (node1.getX() - node2.getX())) + ((node1.getY() - node2.getY()) * (node1.getY() - node2.getY())));
    }

    public static double EstimatedDistance(NetworkNode node1, NetworkNode node2) {
        return AnalyticGeometry.EstimatedDistance(node1.getX(), node1.getY(), node2.getX(), node2.getY());
    }

    /**
     * Given the list of actor calculates the neighborhoods of each actor
     *
     * @param NodesArray the list of vertices
     * @param beginIndex first index of NodesArray
     * @param endIndex   last index of NodesArray
     * @param TR         transmission range
     */
    public static void calculateActorNeighborhoods(List<? extends Gateway> NodesArray, int beginIndex, int endIndex, double TR) {
        for (int i = beginIndex; i <= endIndex; i++) {
            NodesArray.get(i).getNeighborList().clear();
        }
        for (int i = beginIndex; i <= endIndex; i++) {
            for (int j = beginIndex; j <= endIndex; j++) {
                if (i != j) {
                    if (EstimatedDistance(NodesArray.get(i), NodesArray.get(j)) <= TR) {
                        if (!NodesArray.get(i).getNeighborList().contains(NodesArray.get(j)))
                            NodesArray.get(i).addNeighborList(NodesArray.get(j));
                        if (!NodesArray.get(j).getNeighborList().contains(NodesArray.get(i)))
                            NodesArray.get(j).addNeighborList(NodesArray.get(i));
                    }
                }
            }
        }
    }

    public static void calculateActorNeighborhoods(List<? extends Gateway> NodesArray, double TR) {
        calculateActorNeighborhoods(NodesArray, 0, NodesArray.size() - 1, TR);
    }

    // by-pass

    public static Gateway getClosestNeighborDominator(Gateway node) {
        double minDist = -1;
        int minIndex = 0;
        for (int i = 0; i < node.getNeighborDominatorList().size(); i++) {
            double distance = Distance(node, node.getNeighborDominatorList().get(i));
            if (minDist == -1 || distance < minDist) {
                minDist = distance;
                minIndex = i;
            }
        }
        if (node.getNeighborDominatorList().size() != 0)
            return node.getNeighborDominatorList().get(minIndex);
        else
            return null;
    }

    // by-pass

    public static void RemoveAllNeighborDominators(Gateway source) {
        for (int i = 0; i < source.getNeighborDominatorList().size(); i++) {
            RemoveFromNeighborDominatorList(source.getNeighborDominatorList().get(i), source);
        }
        source.getNeighborDominatorList().clear();
    }

    // by-pass

    public static void RemoveFromNeighborDominatorList(Gateway source, Gateway target) {
        for (int i = 0; i < source.getNeighborDominatorList().size(); i++) {
            if (source.getNeighborDominatorList().get(i) == target) {
                source.getNeighborDominatorList().remove(i);
                return;
            }
        }
    }

    /**
     * Compares the contents of two arrays
     *
     * @param arr1 first array
     * @param arr2 second array
     * @return true ifthe size and contents of two array equal, false otherwise
     */
    public static boolean isEqual(List<Gateway> arr1, List<Gateway> arr2) {
        if (arr1.size() == arr2.size()) {
            for (int i = 0; i < arr1.size(); i++) {
                Gateway gateway = arr1.get(i);
                if (!arr2.contains(gateway))
                    return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public static boolean isConnected(ArrayList<? extends Gateway> nodes) {
        ArrayList<ArrayList<Gateway>> partitions = DephtFirstSearch(nodes);
        return partitions.size() == 1;
    }

    public static ArrayList<ArrayList<Gateway>> DephtFirstSearch(List<? extends Gateway> ActorsArray) {
        ArrayList<ArrayList<Gateway>> partitions = new ArrayList<ArrayList<Gateway>>();
        for (int j = 0; j < ActorsArray.size(); j++) {
            ActorsArray.get(j).color = Color.WHITE;
        }
        for (int j = 0; j < ActorsArray.size(); j++) {
            if (ActorsArray.get(j).color == Color.WHITE) {
                ArrayList<Gateway> tmp = new ArrayList<Gateway>();
                DephtFirstVisit(ActorsArray.get(j), tmp);
                partitions.add(tmp);
            }
        }
        visitTime = 0;
        return partitions;
    }

    private static void DephtFirstVisit(Gateway node, ArrayList<Gateway> dft) {
        node.color = Color.GRAY;
        node.firstVisitTime = ++visitTime;
        for (int i = 0; i < node.getNeighborList().size(); i++) {
            if ((node.getNeighborList().get(i)).color == Color.WHITE) {
                DephtFirstVisit(node.getNeighborList().get(i), dft);
            }
        }
        node.color = Color.BLACK;
        node.lastVisitTime = ++visitTime;
        dft.add(node);

    }


    public static double EvaluateTotalArea(ArrayList<Gateway> NodesArray) {
        double topX;
        double topY;
        double bottomX;
        double bottomY;
        double minX = -1;
        double minY = -1;
        double maxX = -1;
        double maxY = -1;
        double squareEdge = 2;


        for (int i = 0; i < NodesArray.size(); i++) {
            if (minX == -1 || minX > NodesArray.get(i).getX()) {
                minX = NodesArray.get(i).getX();
            }
            if (maxX < NodesArray.get(i).getX()) {
                maxX = NodesArray.get(i).getX();
            }

            if (minY == -1 || minY > NodesArray.get(i).getY()) {
                minY = NodesArray.get(i).getY();
            }
            if (maxY < NodesArray.get(i).getY()) {
                maxY = NodesArray.get(i).getY();
            }
        }
        topX = minX - Constants.ActorActionRange;
        if (topX < 0) {
            topX = 0;
        }
        topY = minY - Constants.ActorActionRange;
        if (topY < 0) {
            topY = 0;
        }
        bottomX = maxX + Constants.ActorActionRange;
        if (bottomX > Constants.FrameWidth) {
            bottomX = Constants.FrameWidth;
        }
        bottomY = maxY + Constants.ActorActionRange;
        if (bottomY > Constants.FrameHeight) {
            bottomY = Constants.FrameHeight;
        }
        final int w = (int) (bottomX - topX);
        final int h = (int) (bottomY - topY);

        double centerX;
        double centerY;
        int counter = 0;
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {

                centerX = topX + (squareEdge / 2) + (col * squareEdge);
                centerY = topY + (squareEdge / 2) + (row * squareEdge);
                if (IsInACoverageField(NodesArray, centerX, centerY))
                    counter++;
            }
        }
        return (squareEdge * squareEdge) * counter;


    }

    private static boolean IsInACoverageField(ArrayList<Gateway> NodesArray, double x, double y) {
        double dist;
        for (int i = 0; i < NodesArray.size(); i++) {
            dist = AnalyticGeometry.EstimatedDistance(x, y, (NodesArray.get(i)).getX(), (NodesArray.get(i)).getY());
            if (dist <= Constants.ActorActionRange)
                return true;
        }
        return false;
    }

    public static double EvaluateTotalArea(ArrayList<Gateway> NodesArray, double actionRange) {
        double topX;
        double topY;
        double bottomX;
        double bottomY;
        double minX = -1;
        double minY = -1;
        double maxX = -1;
        double maxY = -1;
        double squareEdge = 2;


        for (int i = 0; i < NodesArray.size(); i++) {
            if (minX == -1 || minX > NodesArray.get(i).getX()) {
                minX = NodesArray.get(i).getX();
            }
            if (maxX < NodesArray.get(i).getX()) {
                maxX = NodesArray.get(i).getX();
            }

            if (minY == -1 || minY > NodesArray.get(i).getY()) {
                minY = NodesArray.get(i).getY();
            }
            if (maxY < NodesArray.get(i).getY()) {
                maxY = NodesArray.get(i).getY();
            }
        }
        topX = minX - actionRange;
        if (topX < 0) {
            topX = 0;
        }
        topY = minY - actionRange;
        if (topY < 0) {
            topY = 0;
        }
        bottomX = maxX + actionRange;
        if (bottomX > Constants.ApplicationAreaWidth) {
            bottomX = Constants.ApplicationAreaWidth;
        }
        bottomY = maxY + actionRange;
        if (bottomY > Constants.ApplicationAreaHeight) {
            bottomY = Constants.ApplicationAreaHeight;
        }
        final int w = (int) (bottomX - topX);
        final int h = (int) (bottomY - topY);

        double centerX;
        double centerY;
        int counter = 0;
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {

                centerX = topX + (squareEdge / 2) + (col * squareEdge);
                centerY = topY + (squareEdge / 2) + (row * squareEdge);
                if (IsInACoverageField(NodesArray, centerX, centerY, actionRange))
                    counter++;
            }
        }
        return (squareEdge * squareEdge) * counter;


    }

    private static boolean IsInACoverageField(ArrayList<Gateway> NodesArray, double x, double y, double actionRange) {
        double dist;
        for (int i = 0; i < NodesArray.size(); i++) {
            dist = AnalyticGeometry.EstimatedDistance(x, y, (NodesArray.get(i)).getX(), (NodesArray.get(i)).getY());
            if (dist <= actionRange)
                return true;
        }
        return false;
    }

    // after federation

    /**
     * Gets Two Interface points from two different partitions
     *
     * @param list1 partition 1
     * @param list2 partition 2
     * @return the closest nodes to each other in each partition
     */
    public static ArrayList<Gateway> getInterfacePoint(ArrayList<Gateway> list1, ArrayList<Gateway> list2) {
        ArrayList<Gateway> result = new ArrayList<Gateway>();
        double minDist = Double.MAX_VALUE;
        int i1 = 0;
        int i2 = 0;
        for (int i = 0; i < list1.size(); i++) {
            Gateway gateway1 = list1.get(i);
            for (int j = 0; j < list2.size(); j++) {
                Gateway gateway2 = list2.get(j);
                double dist = Distance(gateway1, gateway2);
                if (dist < minDist) {
                    minDist = dist;
                    i1 = i;
                    i2 = j;
                }
            }
        }
        result.add(list1.get(i1));
        result.add(list2.get(i2));
        return result;
    }

    public static double Distance(ArrayList<Gateway> list1, ArrayList<Gateway> list2) {
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < list1.size(); i++) {
            Gateway gateway1 = list1.get(i);
            for (int j = 0; j < list2.size(); j++) {
                Gateway gateway2 = list2.get(j);
                double dist = Distance(gateway1, gateway2);
                if (dist < minDist) {
                    minDist = dist;
                }
            }
        }
        return minDist;
    }


    public static void printToFile(File outputFile, ArrayList<Gateway> NodesArray) {
        try {

            PrintWriter pw = new PrintWriter(new FileWriter(outputFile));

            pw.println(NodesArray.size());
            for (int i = 0; i < NodesArray.size(); i++) {
                Gateway gateway = NodesArray.get(i);
                pw.println("" + gateway.getX());
                pw.println("" + gateway.getY());
                pw.println("" + gateway.getNetworkID());
            }
            pw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Gateway> readFromFile(File inputFile) {
        ArrayList<Gateway> result = new ArrayList<Gateway>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            int size = Integer.parseInt(br.readLine());
            for (int i = 0; i < size; i++) {
                Gateway gateway = new Gateway(i);
                gateway.setX(Double.parseDouble(br.readLine()));
                gateway.setY(Double.parseDouble(br.readLine()));
                gateway.setNetworkID(Integer.parseInt(br.readLine()));
                result.add(gateway);
            }
            br.close();
            calculateActorNeighborhoods(result, Constants.ActorTransmissionRange);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static HashMap<String, Integer> FloydWarshall(List<? extends Gateway> ActorArray, List<Gateway> RelayNodeArray) {
        ArrayList<Gateway> allNodes = new ArrayList<Gateway>();
        allNodes.addAll(ActorArray);
        allNodes.addAll(RelayNodeArray);
        calculateActorNeighborhoods(allNodes, Constants.ActorTransmissionRange);
        int path[][] = new int[allNodes.size()][allNodes.size()];
        for (int i = 0; i < allNodes.size(); i++) {
            Gateway g1 = allNodes.get(i);
            for (int j = 0; j < allNodes.size(); j++) {
                if (i == j) {
                    path[i][j] = 0;
                } else {
                    Gateway g2 = allNodes.get(j);
                    if (g1.isNeighbor(g2)) {
                        path[i][j] = 1;
                    } else {
                        path[i][j] = (Integer.MAX_VALUE - 1) / 2;
                    }
                }
            }
        }

        boolean isStable = false;
        while (!isStable) {
            boolean updated = false;
            for (int i = 0; i < path.length; i++) {
                for (int j = 0; j < path.length; j++) {
                    if (i != j) {
                        for (int k = 0; k < path.length; k++) {
                            if (i != k && j != k) {
                                if (path[i][k] + path[k][j] < path[i][j]) {
                                    path[i][j] = path[i][k] + path[k][j];
                                    updated = true;
                                }
                            }
                        }
                    }
                }

            }
            isStable = !updated;

        }
        HashMap<String, Integer> costTable = new HashMap<String, Integer>();
        for (int i = 0; i < ActorArray.size(); i++) {
            for (int j = 0; j < ActorArray.size(); j++) {
                if (i != j) {
                    Integer c = costTable.get(allNodes.get(i).getNetworkID() + "->" + allNodes.get(j).getNetworkID());
                    if (c == null || (path[i][j] < c)) {
                        costTable.put(allNodes.get(i).getID() + "->" + allNodes.get(j).getID(), path[i][j]);
                    }
                }
            }

        }
        return costTable;
    }

    public static ArrayList<Edge> runKruskal(List<? extends Gateway> NodeList) {

        for (Gateway gateway : NodeList) {
            gateway.setMstID(0);
        }

        ArrayList<Edge> allEdges = new ArrayList<>();

        for (int i = 0; i < NodeList.size(); i++) {
            Gateway g1 = NodeList.get(i);
            for (int j = i; j < NodeList.size(); j++) {
                if (i != j) {
                    Gateway g2 = NodeList.get(j);
                    allEdges.add(new Edge(g1, g2));
                }
            }
        }

        Collections.sort(allEdges);
        int nextID = 1;
        ArrayList<Edge> edges = new ArrayList<>();

        for (Edge edge : allEdges) {
            Gateway u = edge.u;
            Gateway v = edge.v;

            if (u.getMstID() == 0 && v.getMstID() == 0) {
                u.setMstID(nextID);
                v.setMstID(nextID);
                nextID++;
                edges.add(edge);
            } else if (u.getMstID() == 0 && v.getMstID() != 0) {
                u.setMstID(v.getMstID());
                edges.add(edge);
            } else if (u.getMstID() != 0 && v.getMstID() == 0) {
                v.setMstID(u.getMstID());
                edges.add(edge);
            } else {
                if (u.getMstID() != v.getMstID()) {
                    int min = Math.min(u.getMstID(), v.getMstID());
                    int max = Math.max(u.getMstID(), v.getMstID());
                    for (int i = 0; i < NodeList.size(); i++) {
                        Gateway aNode = NodeList.get(i);
                        if (aNode.getMstID() == max) {
                            aNode.setMstID(min);
                        }
                    }
                    edges.add(edge);
                }
            }
        }

        return edges;
    }

    public static ArrayList<ArrayList<Gateway>> updateInterfaceEdges(ArrayList<Edge> edges, ArrayList<Gateway> representatives, ArrayList<Gateway> ActorsArray) {
        ArrayList<ArrayList<Gateway>> result = new ArrayList<ArrayList<Gateway>>();

        for (Edge edge : edges) {
            Gateway u = edge.u;
            Gateway v = edge.v;
            ArrayList<Gateway> uList = new ArrayList<Gateway>();
            ArrayList<Gateway> vList = new ArrayList<Gateway>();

            for (Gateway g : ActorsArray) {
                if (g.getNetworkID() == u.getNetworkID()) {
                    uList.add(g);
                }
                if (g.getNetworkID() == v.getNetworkID()) {
                    vList.add(g);
                }
            }

            ArrayList<Gateway> interfacePoints = getInterfacePoint(uList, vList);
            result.add(interfacePoints);
        }
        return result;
    }


    public static Point2D check3Star(Gateway a, Gateway b, Gateway c, double R) {
        double angleA = getAngle(b, a, c);
        double angleB = getAngle(a, b, c);
        double angleC = getAngle(a, c, b);

        if (angleA == 0 || angleB == 0 || angleC == 0) {
            return null;
        }
        double dist;
        Point2D oMidPoint;
        if (angleA < 90) {
            if (angleB < 90) {
                if (angleC < 90) {
                    //acute
                    double theta = angleA * Math.PI / 180;
                    Point2D center = getCenterOfCircle(b, a, c, theta);
                    double d1 = AnalyticGeometry.EstimatedDistance(center.getX(), center.getY(), a.getX(), a.getY());
                    double d2 = AnalyticGeometry.EstimatedDistance(center.getX(), center.getY(), b.getX(), b.getY());
                    double d3 = AnalyticGeometry.EstimatedDistance(center.getX(), center.getY(), c.getX(), c.getY());
                    if (d1 <= R && d2 <= R && d3 <= R) {
                        return center;
                    } else {
                        return null;
                    }
                } else {
                    if (angleC > 0) {
                        dist = Distance(a, b);
                        oMidPoint = getMidPoint(a, b);
                    } else {
                        return null;
                    }
                }
            } else {
                if (angleB > 0) {
                    dist = Distance(a, c);
                    oMidPoint = getMidPoint(a, c);
                } else {
                    return null;
                }
            }
        } else {
            if (angleA > 0) {
                dist = Distance(b, c);
                oMidPoint = getMidPoint(b, c);
            } else {
                return null;
            }
        }
        if (dist <= 2 * R) {
            return oMidPoint;
        } else {
            return null;
        }

    }

    private static Point2D getMidPoint(Gateway a, Gateway b) {
        Point2D mid = new Point2D.Double();
        double mx = a.getX() - ((a.getX() - b.getX()) / 2);

        double my = a.getY() - ((a.getY() - b.getY()) / 2);
        mid.setLocation(mx, my);
        return mid;
    }

    public static double getAngle(Gateway left, Gateway X, Gateway right) {

        Point2D lp = new Point2D.Double(left.getX(), left.getY());
        Point2D Xp = new Point2D.Double(X.getX(), X.getY());
        Point2D rp = new Point2D.Double(right.getX(), right.getY());

        return AnalyticGeometry.findAngle(lp, Xp, rp);
//        return 0;
    }

    /**
     * Let the corners of the triangle left-X-right  be points on Circle C whose raidus is atmost R
     *
     * @param left  left corner
     * @param X     top corner
     * @param right right corner
     * @param theta angle at point X
     * @return coordinates of circle that circumscribes left-X-right points
     */
    public static Point2D getCenterOfCircle(Gateway left, Gateway X, Gateway right, double theta) {
        Point2D m = getMidPoint(left, right);

        double sinX = Math.abs(Math.sin(theta / 2));
        double cosX = Math.abs(Math.cos(theta / 2));
        double sin2X = 2 * sinX * cosX;
        double hd = (Distance(left, right) / 2);
        double R = hd / sin2X;
        double b = R * Math.abs(Math.cos(theta));
        return AnalyticGeometry.getCoordinates(m.getX(), m.getY(), X.getX(), X.getY(), b);
    }

    // todo Max flow and connectivity is move to utilities

    private static SPFlagPair FindSP(int[][] neigh, int[][] cap, int source, int dest, int size) {

        int[] SP = new int[size];

        int flag, i, j;
        int[][] neighTemp = new int[size][size];
        int[] SPTemp;
        int[] S = new int[size];
        int[] Q = new int[size];
        int[] d = new int[size];
        int[] dQ = new int[size];
        int[] pi = new int[size];
        int[][] w = new int[size][size];

        for (i = 0; i < size; i++) {
            for (j = 0; j < size; j++) {
                neighTemp[i][j] = neigh[i][j];
            }
        }

        for (i = 0; i < size; i++) {
            for (j = 0; j < size; j++) {
                if (cap[i][j] < 1) {
                    neighTemp[i][j] = 0;
                }
            }
        }
        for (i = 0; i < size; i++) {
            S[i] = 0;
            Q[i] = 1;
            d[i] = Constants.Inf;
            dQ[i] = Constants.Inf;
            pi[i] = 0;
            SP[i] = -1;
        }
        for (i = 0; i < size; i++) {
            for (j = 0; j < size; j++) {
                w[i][j] = 1;
            }
        }
        d[source] = 0;
        dQ[source] = 0;

        while ((Q[dest] == 1) && (sum(Q, size) > 0)) {
            int u = findMin(dQ, size);

            S[u] = 1;
            Q[u] = 0;
            dQ[u] = Constants.Inf + 1;
            for (j = 0; j < size; j++) {
                if (neighTemp[u][j] == 1) {
                    if (d[j] > d[u] + w[u][j]) {
                        d[j] = d[u] + w[u][j];
                        dQ[j] = d[j];
                        pi[j] = u;
                    }
                }
            }
        }


        if (d[dest] == Constants.Inf) {
            flag = 0;
        } else {
            int k;
            SPTemp = new int[size];
            i = dest;
            j = 0;
            SPTemp[j] = i;
            j = j + 1;
            while (i != source) {
                SPTemp[j] = pi[i];
                i = pi[i];
                j = j + 1;
            }
            for (k = j - 1; k >= 0; k--) {
                SP[j - 1 - k] = SPTemp[k];
            }
            flag = 1;
//            free(SPTemp);
        }

        return new SPFlagPair(flag, SP);
    }

    public static int FindMaxFlow(int[][] neigh, int size, int u, int v) {
        int conn = 0, exitFlag = 0;
        int[][] cap = new int[size][size];
        int[][] capInit = new int[size][size];
        int[][] f = new int[size][size];
        int i, j;

        for (i = 0; i < size; i++) {
            for (j = 0; j < size; j++) {
                cap[i][j] = neigh[i][j];
            }
        }
        for (i = 0; i < size; i++) {
            for (j = 0; j < size; j++) {
                capInit[i][j] = neigh[i][j];
            }
        }

        for (i = 0; i < size; i++) {
            for (j = 0; j < size; j++) {
                f[i][j] = 0;
            }
        }


        while (exitFlag == 0) {
            int flag;
            int[] SP; //= (int *) malloc(size * sizeof(int));

            SPFlagPair spfp = FindSP(neigh, cap, u, v, size);

            flag = spfp.flag;
            SP = spfp.SP;

            if (flag == 1) {
                int temp = 0, maxFlow = 100;
                while ((temp < size - 1) && (SP[temp + 1] >= 0)) {
                    if (maxFlow > cap[SP[temp]][SP[temp + 1]]) {
                        maxFlow = cap[SP[temp]][SP[temp + 1]];
                    }
                    temp++;
                }
                temp = 0;
                while ((temp < size - 1) && (SP[temp + 1] >= 0)) {
                    cap[SP[temp]][SP[temp + 1]] = cap[SP[temp]][SP[temp + 1]] - maxFlow;
                    f[SP[temp]][SP[temp + 1]] = f[SP[temp]][SP[temp + 1]] + maxFlow;
                    f[SP[temp + 1]][SP[temp]] = -f[SP[temp]][SP[temp + 1]];
                    cap[SP[temp + 1]][SP[temp]] = capInit[SP[temp + 1]][SP[temp]] + f[SP[temp]][SP[temp + 1]];
                    temp++;
                }
            } else {
                exitFlag = 1;
                for (i = 0; i < size; i++) {
                    conn += f[u][i];
                }
            }
        }

        return conn;
    }

    public static int CheckForKConnectivity(int[][] neigh, int size, int K, int N) {
        int connectivity, flag, u = 0, v, i;
        int[] conn = new int[size];
        for (i = 0; i < size; i++) {
            conn[i] = 0;
        }
        conn[u] = Constants.Inf;
        for (v = 0; v < N; v++) /*As we check for connectivity between first N vertices only*/ {
            if (v != u) {
                conn[v] = FindMaxFlow(neigh, size, u, v);
            }
        }
        int minIndex = findMin(conn, N);
        connectivity = conn[minIndex];

        if (connectivity >= K) {
            flag = 1;
        } else {
            flag = 0;
        }

        return flag;
    }

    /**
     * @param list  array to be searched
     * @param size1 length of the array
     * @return index of the minimum value
     */
    public static int findMin(int[] list, int size1) {
        int val = list[0];
        int index = 0;
        for (int i = 1; i < size1; i++) {
            if (list[i] < val) {
                val = list[i];
                index = i;
            }
        }

        return index;
    }

    public static int sum(int[] list, int size) {
        int sum1, i;
        sum1 = 0;
        for (i = 0; i < size; i++) {
            sum1 += list[i];
        }
        return sum1;
    }

    /**
     * Minimum of an 2D array
     *
     * @param list  is the 2D square array
     * @param size1 is the length of the array
     * @return minimum of array entries
     */
    public static int FindMin2D(int[][] list, int size1) {
        int val = list[0][0];
        for (int i = 0; i < size1; i++) {
            for (int j = 0; j < size1; j++) {
                if (list[i][j] < val) {
                    val = list[i][j];
                }
            }
        }
        return val;
    }

    /**
     * Sum of an 2D array
     *
     * @param list  is the 2D square array
     * @param size1 is the length of the array
     * @return sum of array entries
     */
    public static int FindSum2D(int[][] list, int size1) {
        int sum = 0;
        for (int i = 0; i < size1; i++) {
            for (int j = 0; j < size1; j++) {
                sum += list[i][j];
            }
        }
        return sum;
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
    public static boolean isSplitting(Gateway left, Gateway X, Gateway right, Gateway target) {
        Point2D lp = new Point2D.Double(left.getX(), left.getY());
        Point2D rp = new Point2D.Double(right.getX(), right.getY());
        Point2D xp = new Point2D.Double(X.getX(), X.getY());
        Point2D tp = new Point2D.Double(target.getX(), target.getY());
        return AnalyticGeometry.isSplitting(lp, xp, rp, tp);

    }

    /**
     * Inner class definition - 2
     */
//    static class IndexValuePair {
//        int index;
//        int value;
//
//        public IndexValuePair(int index, int value) {
//            this.index = index;
//            this.value = value;
//        }
//    }

    static class SPFlagPair {
        int flag;
        int[] SP;

        public SPFlagPair(int flag, int[] SP) {
            this.flag = flag;
            this.SP = SP;
        }
    }


    public static ArrayList<Gateway> fillGap(Gateway u, Gateway v, boolean isBothRelay, ArrayList<Gateway> ActorsArray, ArrayList<Gateway> RelayNodeArray) {
        return fillGap(u, v, isBothRelay, 0, ActorsArray, RelayNodeArray);
    }

    public static ArrayList<Gateway> fillGap(Gateway u, Gateway v, boolean isBothRelay, int ccid, ArrayList<Gateway> ActorsArray, ArrayList<Gateway> RelayNodeArray) {
        ArrayList<Gateway> result = new ArrayList<Gateway>();
        ArrayList<Point2D> deploymentPoints = getDeploymentPoints(u, v, isBothRelay);
        for (int j = 0; j < deploymentPoints.size(); j++) {
            Point2D pj = deploymentPoints.get(j);
            Gateway rn = new Gateway(ActorsArray.size() + RelayNodeArray.size());
            rn.isRelay = true;
            rn.setX(pj.getX());
            rn.setY(pj.getY());
            rn.setCcid(ccid);
            RelayNodeArray.add(rn);
            result.add(rn);
        }
        ArrayList<Gateway> allNodes = new ArrayList<Gateway>(RelayNodeArray);
        allNodes.addAll(ActorsArray);
        NetworkUtils.calculateActorNeighborhoods(allNodes, Constants.RNTransmissionRange);
        attachRelayWithGateways(ActorsArray, RelayNodeArray);
        return result;
    }

    public static ArrayList<Point2D> getDeploymentPoints(Gateway u, Gateway v, boolean isBothRelay) {
        double dist = NetworkUtils.EstimatedDistance(u, v);
        int minNumOfNodesRequires;
        double di;
        if (!isBothRelay)
            di = dist - 2 * Constants.ActorTransmissionRange;
        else
            di = dist - 2 * Constants.RNTransmissionRange;
        if (di <= 0) {
            minNumOfNodesRequires = 1;
        } else if (di > 0 && di <= Constants.RNTransmissionRange) {
            minNumOfNodesRequires = 2;
        } else {
            int n = ((int) Math.ceil(di / Constants.RNTransmissionRange)) - 1;
            minNumOfNodesRequires = n + 2;
        }
        ArrayList<Point2D> deploymentPoints = new ArrayList<Point2D>();
        if (minNumOfNodesRequires == 1) {
            Point2D p = AnalyticGeometry.getCoordinates(u.getX(), u.getY(), v.getX(), v.getY(), dist / 2);
            deploymentPoints.add(p);
        } else if (minNumOfNodesRequires == 2) {
            Point2D p1, p2;
            if (!isBothRelay) {
                p1 = AnalyticGeometry.getCoordinates(u.getX(), u.getY(), v.getX(), v.getY(), Constants.ActorTransmissionRange);
                p2 = AnalyticGeometry.getCoordinates(v.getX(), v.getY(), u.getX(), u.getY(), Constants.ActorTransmissionRange);
            } else {
                p1 = AnalyticGeometry.getCoordinates(u.getX(), u.getY(), v.getX(), v.getY(), dist / 3);
                p2 = AnalyticGeometry.getCoordinates(v.getX(), v.getY(), u.getX(), u.getY(), dist / 3);
            }
            deploymentPoints.add(p1);
            deploymentPoints.add(p2);
        } else {
            Point2D p1, p2;
            if (!isBothRelay) {
                p1 = AnalyticGeometry.getCoordinates(u.getX(), u.getY(), v.getX(), v.getY(), Constants.ActorTransmissionRange);
                p2 = AnalyticGeometry.getCoordinates(v.getX(), v.getY(), u.getX(), u.getY(), Constants.ActorTransmissionRange);

                deploymentPoints.add(p1);
                deploymentPoints.add(p2);
                int n = minNumOfNodesRequires - 2;
                double d = (di / (n + 1));

                for (int j = 1; j <= n; j++) {
                    Point2D pi = AnalyticGeometry.getCoordinates(p1.getX(), p1.getY(), p2.getX(), p2.getY(), j * d);
                    deploymentPoints.add(pi);
                }
            } else {
                double d = (dist / (minNumOfNodesRequires + 1));
                for (int j = 1; j <= minNumOfNodesRequires; j++) {
                    Point2D pi = AnalyticGeometry.getCoordinates(u.getX(), u.getY(), v.getX(), v.getY(), j * d);
                    deploymentPoints.add(pi);
                }
            }
        }
        return deploymentPoints;
    }

    /**
     * @param ActorsArray    array of terminals
     * @param RelayNodeArray array of steiner points
     * @return key = networkID, Value = true if that partition is attached
     */
    public static HashMap<Integer, Boolean> attachRelayWithGateways(ArrayList<Gateway> ActorsArray, ArrayList<Gateway> RelayNodeArray) {
        HashMap<Integer, Boolean> result = new HashMap<Integer, Boolean>();
        for (int i = 0; i < ActorsArray.size(); i++) {
            Gateway gateway = ActorsArray.get(i);
            for (int j = 0; j < RelayNodeArray.size(); j++) {
                Gateway rn = RelayNodeArray.get(j);
                if (EstimatedDistance(gateway, rn) <= Constants.ActorTransmissionRange) {
                    gateway.addNeighborList(rn);
                    rn.addNeighborList(gateway);
                    if (result.get(gateway.getNetworkID()) == null) {
                        result.put(gateway.getNetworkID(), true);
                    }
                }
            }
        }
        return result;
    }


    public static ArrayList<ArrayList<Tour>> findKSubsetofTours(ArrayList<Tour> tours, int k) {
        ArrayList<Tour> orj = new ArrayList<Tour>(tours);
        int i, j;
        ArrayList<ArrayList<Tour>> result = new ArrayList<ArrayList<Tour>>();
        if (k > tours.size()) {
            return null;
        }
        if (k == tours.size()) {
            ArrayList<Tour> s1 = new ArrayList<Tour>();
            s1.addAll(tours);
            result.add(s1);
            return result;
        }
        if (k == 1) {
            for (i = 0; i < tours.size(); i++) {
                ArrayList<Tour> s1 = new ArrayList<Tour>();
                s1.add(tours.get(i));
                result.add(s1);
            }
            return result;
        }

        for (i = tours.size() - 1; i >= 0; i--) {
            Tour u = tours.get(i);
            tours.remove(u);
            ArrayList<Tour> nodes_tmp = new ArrayList<Tour>();
            nodes_tmp.addAll(tours);
            ArrayList<ArrayList<Tour>> ss = findKSubsetofTours(nodes_tmp, k - 1);
            for (j = 0; j < ss.size(); j++) {
                ss.get(j).add(u);
            }
            result.addAll(ss);
        }
        tours.clear();
        tours.addAll(orj);
        return result;
    }


    public static ArrayList<ArrayList<Gateway>> findKSubsetofGateways(ArrayList<Gateway> nodes, int k) {
        ArrayList<Gateway> orj = new ArrayList<Gateway>(nodes);
        int i, j;
        ArrayList<ArrayList<Gateway>> result = new ArrayList<ArrayList<Gateway>>();
        if (k > nodes.size()) {
            return null;
        }
        if (k == nodes.size()) {
            ArrayList<Gateway> s1 = new ArrayList<Gateway>();
            s1.addAll(nodes);
            result.add(s1);
            return result;
        }
        if (k == 1) {
            for (i = 0; i < nodes.size(); i++) {
                ArrayList<Gateway> s1 = new ArrayList<Gateway>();
                s1.add(nodes.get(i));
                result.add(s1);
            }
            return result;
        }

        for (i = nodes.size() - 1; i >= 0; i--) {
            Gateway u = nodes.get(i);
            nodes.remove(u);
            ArrayList<Gateway> nodes_tmp = new ArrayList<Gateway>();
            nodes_tmp.addAll(nodes);
            ArrayList<ArrayList<Gateway>> ss = findKSubsetofGateways(nodes_tmp, k - 1);
            for (j = 0; j < ss.size(); j++) {
                ss.get(j).add(u);
            }
            result.addAll(ss);
        }
        nodes.clear();
        nodes.addAll(orj);
        return result;

    }

    public static double calculateConnectivityMeasure(List<? extends Gateway> list, double transmissionRange) {
        calculateActorNeighborhoods(list, transmissionRange);
        ArrayList<ArrayList<Gateway>> partitions = DephtFirstSearch(list);
        Map<Gateway, Integer> partitionMap = new HashMap<>();


        IntStream.range(0, partitions.size())
                .forEach(i -> {
                    ArrayList<Gateway> partition = partitions.get(i);
                    partitionMap.putAll(partition.stream()
                            .collect(Collectors.toMap(u -> u, u -> i)));
                });
        int reachable = 0, notReachable = 0;

        for (int i = 0; i < list.size() - 1; i++) {
            Gateway u = list.get(i);
            for (int j = i + 1; j < list.size(); j++) {
                Gateway v = list.get(j);
                if (Objects.equals(partitionMap.get(u), partitionMap.get(v))) {
                    reachable++;
                } else {
                    notReachable++;
                }
            }
        }
        return 1d * (reachable) / (reachable + notReachable);
    }

    public static ArrayList<Gateway> getClosedPolygon(ArrayList<Gateway> list, int applicationAreaWidth, int applicationAreaHeight) {
        ArrayList<Point2D> plist = new ArrayList<Point2D>();
        HashMap<Point2D, Integer> point_index_map = new HashMap<Point2D, Integer>();
        for (int i = 0; i < list.size(); i++) {
            Gateway gateway = list.get(i);
            Point2D p = new Point2D.Double(gateway.getX(), gateway.getY());
            point_index_map.put(p, i);
            plist.add(p);
        }
        ArrayList<Point2D> res = PolygonUtilities.getClosedPolygon(plist, applicationAreaWidth, applicationAreaHeight);
        ArrayList<Gateway> result = new ArrayList<Gateway>();
        for (int i = 0; i < res.size(); i++) {
            Point2D p = res.get(i);
            result.add(list.get(point_index_map.get(p)));
        }
        return result;
//            Set<Gateway> convexHullVertices = new HashSet<Gateway>(convexHull);
//            Set<Gateway> allVertices = new HashSet<Gateway>(list);
//            allVertices.removeAll(convexHullVertices);
//
//            for (Gateway vertex : allVertices) {
//                double mindist = Double.MAX_VALUE;
//                int minIndex = -1;
//                for (int i = 0; i < convexHull.size(); i++) {
//                    Gateway g1 = convexHull.get(i);
//                    Gateway g2 = convexHull.get((i + 1) % convexHull.size());
//                    Point2D p1 = new Point2D.Double(g1.getX(), g1.getY());
//                    Point2D p2 = new Point2D.Double(g2.getX(), g2.getY());
//
//                    double m = (p2.getY() - p1.getY()) / (p2.getX() - p1.getX());
//                    double n = p2.getY() - (m * p2.getX());
//                    double distance = AnalyticGeometry.distanceBetweenPointAndLine(vertex.getX(), vertex.getY(), m, n);
//                    if (distance < mindist) {
//                        minIndex = i;
//                        mindist = distance;
//                    }
//                }
//                convexHull.add(minIndex+1, vertex);
//            }

    }

    public static List<Gateway> findMSTPath(Gateway u, Gateway v, ArrayList<Edge> mstEdges) {
        Map<Integer, Gateway> idToGateway = new HashMap<>();
        for (Edge edge : mstEdges) {
            if (!idToGateway.containsKey(edge.u.getID())) {
                idToGateway.put(edge.u.getID(), edge.u);
            }
            if (!idToGateway.containsKey(edge.v.getID())) {
                idToGateway.put(edge.v.getID(), edge.v);
            }
        }

        Map<Integer, List<Gateway>> adjacencies = new HashMap<>();
        for (Edge edge : mstEdges) {
            List<Gateway> uNeighbors = adjacencies.getOrDefault(edge.u.getID(), new ArrayList<>());
            List<Gateway> vNeighbors = adjacencies.getOrDefault(edge.v.getID(), new ArrayList<>());

            uNeighbors.add(edge.v);
            vNeighbors.add(edge.u);

            adjacencies.put(edge.u.getID(), uNeighbors);
            adjacencies.put(edge.v.getID(), vNeighbors);
        }

        Set<Integer> visited = new HashSet<>();
        Stack<Gateway> stack = new Stack<>();
        Map<Integer, Integer> parent = new HashMap<>();

        stack.push(u);
        visited.add(u.getID());

        while (!stack.isEmpty()) {
            Gateway current = stack.pop();
            for (Gateway neighbor : adjacencies.getOrDefault(current.getID(), new ArrayList<>())) {
                if (!visited.contains(neighbor.getID())) {
                    stack.push(neighbor);
                    visited.add(neighbor.getID());
                    parent.put(neighbor.getID(), current.getID());
                }
            }
        }

        List<Gateway> path = new ArrayList<>();
        if (!visited.contains(v.getID())) {
            return path; // No path from u to v
        }

        int currentId = v.getID();
        while (currentId != u.getID()) {
            Gateway currentVertex = idToGateway.get(currentId);
            path.add(currentVertex);
            currentId = parent.get(currentId);
        }
        path.add(idToGateway.get(u.getID()));
        Collections.reverse(path);

        return path;
    }


}
