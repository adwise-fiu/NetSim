package network;

import event.MessageEvent;
import event.MessageListener;
import geometry.AnalyticGeometry;
import geometry.delaunay.DTAdapter;
import graphics.WSNGraphics;
import utils.KConnectedGraph;
import utils.NetworkUtils;
import utils.PolygonUtilities;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.*;

/**
 * @author : Fatih Senel
 *         Date: Nov 18, 2007
 *         Time: 10:57:41 PM
 */
public class SensorAndActorNetwork {

    ArrayList<Gateway> convexHull;
    private ArrayList<Gateway> ActorsArray = new ArrayList<Gateway>();
    private ArrayList<Gateway> RelayNodeArray = new ArrayList<Gateway>();
    private ArrayList<Sensor> SensorArray = new ArrayList<Sensor>();
    HashMap<String, int[]> tour_experiment_check = new HashMap<String, int[]>();
    ArrayList<Tour> tours = new ArrayList<Tour>();
    public static int visitTime = 0;
    boolean isLeaderSelected = false;
    int superLeaderID = 0;
    int numberOfPartitions;
    Point2D CoM = null;
    Square[][] grid;
    ArrayList<Line> sortedLines = new ArrayList<Line>();
    //    double squareSize = 75;
    Point2D[] polygon;
    Line[] lines;
    //true if there is a RN located at center of mass
    boolean com_deployed = false;
    private boolean firstApproach = false, secondApproach = false;

    ArrayList<ArrayList<Gateway>> interfacePoints = new ArrayList<ArrayList<Gateway>>();
    HashMap<String, Edge> mstEdgeMap = new HashMap<String, Edge>();
    ArrayList<Triangle> updatedDT = new ArrayList<Triangle>();
    // used in triangle approach

    /**
     * int[0] is the row value
     * int[1] is the column value
     */
    ArrayList<int[]> corp_junctions = new ArrayList<int[]>();

    String input_data_path = "data/JNCAdata/e4/";

    public SensorAndActorNetwork() {
    }

    public ArrayList<Gateway> generateRandomGraph(int beginIndex, int endIndex, Square sq) {
        ArrayList<Gateway> tmpArray = new ArrayList<Gateway>();
        for (int i = beginIndex; i <= endIndex; i++) {
            Gateway g = new Gateway(i);
            g.x = sq.topX + Math.random() * sq.width;
            g.y = sq.topY + Math.random() * sq.width;
            g.setNetworkID(sq.snum);
            tmpArray.add(g);
        }
        NetworkUtils.calculateActorNeighborhoods(tmpArray, beginIndex, endIndex, Constants.ActorTransmissionRange);
        makeConnected(tmpArray, sq);
        return tmpArray;
    }

    public void generateRandomGraph(int numberOfNodes, int tx, int ty, int width, int height) {
        ArrayList<Gateway> tmpArray = new ArrayList<Gateway>();
        for (int i = 0; i < numberOfNodes; i++) {
            Gateway g = new Gateway(i);
            g.x = tx + Math.random() * width;
            g.y = ty + Math.random() * height;
            tmpArray.add(g);
        }
        NetworkUtils.calculateActorNeighborhoods(tmpArray, 0, numberOfNodes - 1, Constants.ActorTransmissionRange);
        makeConnected(tmpArray, new Square(tx, ty, width, 0));
        ActorsArray.clear();
        ActorsArray.addAll(tmpArray);
    }

    public void drawDelaunay(Graphics g) {
        ArrayList<Gateway> list = new ArrayList<Gateway>(ActorsArray);
//        list.addAll(RelayNodeArray);
        ArrayList<Triangle> allDts = findDelaunayTriangles(list);

        for (int i = 0; i < allDts.size(); i++) {
            allDts.get(i).draw(g);
        }
    }

    public void drawActors(Graphics g, boolean showEdges, boolean showID, boolean dominatorOnly) {
        for (int i = 0; i < ActorsArray.size(); i++) {
            ActorsArray.get(i).draw(g, showEdges, showID, dominatorOnly);
        }
        for (int i = 0; i < RelayNodeArray.size(); i++) {
            RelayNodeArray.get(i).draw(g, showEdges, showID, dominatorOnly);
        }
    }

    public void drawTours(Graphics g) {
        for (int i = 0; i < tours.size(); i++) {
            tours.get(i).draw(g);
        }
    }

    public void drawSensors(Graphics g, boolean showClusters) {
        for (int i = 0; i < SensorArray.size(); i++) {
            SensorArray.get(i).draw(g, showClusters);

        }
    }


    public ArrayList<Gateway> getActorsArray() {
        return ActorsArray;
    }

/*    public void generate() {
        isLeaderSelected = false;
        ActorsArray.clear();
        generateRandomGraph(0, 50, 0, 0, 500, 500);
    }*/

    public int toggleSensorMouseOver(int x, int y) {
        int index = -1;
        for (int i = 0; i < SensorArray.size(); i++) {
            Sensor sensor = SensorArray.get(i);
            if (sensor.isIn(x, y)) {
                index = i;
            }
        }
        return index;
    }

    public int toggleGatewayMouseOver(int x, int y) {

        boolean b = false;
        int index = -1;
        for (int i = 0; i < ActorsArray.size(); i++) {
            Gateway gateway = ActorsArray.get(i);
            if (gateway.isIn(x, y)) {
                b = true;
                gateway.setSelected(!gateway.isSelected());
                index = i;
            }
        }
        for (int i = 0; i < ActorsArray.size(); i++) {
            if (i != index) {
                Gateway gateway = ActorsArray.get(i);
                gateway.setSelected(false);
            }

        }
        if (!b) {
            for (int i = 0; i < ActorsArray.size(); i++) {
                ActorsArray.get(i).setSelected(false);
            }
        }
        return index;
    }

    public Gateway getGateway(int index) {
        return ActorsArray.get(index);
    }

    public Sensor getSensor(int index) {
        return SensorArray.get(index);
    }

    public void loadIDSOutput(File file) {
        ActorsArray.clear();
        SensorArray.clear();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            int nodeCount;//, width, height, k, ActorRange;
            String line = br.readLine();
            String[] firstline = line.split("\t");
            nodeCount = Integer.parseInt(firstline[0]);
//            k = Integer.parseInt(firstline[3]);
//            ActorRange = Integer.parseInt(firstline[4]);
            String[] lines;
            for (int i = 0; i < nodeCount; i++) {
                line = br.readLine();
                lines = line.split("\t");
                Sensor sns = new Sensor(i);
                sns.setX(Double.parseDouble(lines[1]));
                sns.setY(Double.parseDouble(lines[2]));
                sns.setStatus(Integer.parseInt(lines[3]));
                sns.setDominatorID(Integer.parseInt(lines[5]));
                SensorArray.add(sns);
            }
            br.readLine();
            for (int i = 0; i < nodeCount; i++) {
                line = br.readLine();
                lines = line.split("\t");
                if (lines.length > 1) {
                    int id = Integer.parseInt(lines[0]);
                    Sensor sns = SensorArray.get(id);
                    for (int j = 2; j < lines.length; j++) {
                        sns.addNeighbor(SensorArray.get(Integer.parseInt(lines[j])));
                    }
                }
            }
            NetworkUtils.findNexthopSensorNodes(SensorArray);
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        addSensorMessageActionListener();
    }

    public void addSensorMessageActionListener() {
        for (int i = 0; i < SensorArray.size(); i++) {
            final Sensor sensor = SensorArray.get(i);
            sensor.addMessageListener(new MessageListener() {
                public void messageReceived(MessageEvent e) {
                }
            });
        }
    }

    public void addActorMessageListeners() {
        for (int i = 0; i < ActorsArray.size(); i++) {
            final Gateway gateway1 = ActorsArray.get(i);
            gateway1.addMessageListener(new MessageListener() {
                public void messageReceived(MessageEvent e) {

                }
            });
        }
    }

    public void printStatistics() {
        double TMN = 0;
        double totalNumOfMessagesForActors = 0;
        double totalNumOfMessagesForSensors = 0;
        double MMI = -1;


        for (int i = 0; i < ActorsArray.size(); i++) {
            Gateway gateway = ActorsArray.get(i);
            double actorMovement = gateway.getTotalMovement();
            TMN += actorMovement;
            totalNumOfMessagesForActors += gateway.getNumberOfMessagesTransmitted();
            if (actorMovement > MMI) {
                MMI = actorMovement;
            }
        }
        for (int i = 0; i < SensorArray.size(); i++) {
            Sensor sensor = SensorArray.get(i);
            totalNumOfMessagesForSensors += sensor.getNumberOfMessagesTransmitted();
        }
        double avergaeActorMovement = TMN / ActorsArray.size();
        double averageMessagesPerActor = totalNumOfMessagesForActors / ActorsArray.size();
        double averageMessagesPerSensor = totalNumOfMessagesForSensors / SensorArray.size();
        ArrayList<String> lines = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("result.csv"));
            String line = br.readLine();
            int i = 0;
            while (line != null) {
                line += ",";
                if (i == 0) {
                    line += ActorsArray.size();
                } else if (i == 1) {
                    line += SensorArray.size();
                } else if (i == 2) {
                    line += numberOfPartitions;
                } else if (i == 3) {
                    line += TMN;
                } else if (i == 4) {
                    line += MMI;
                } else if (i == 5) {
                    line += avergaeActorMovement;
                } else if (i == 6) {
                    line += totalNumOfMessagesForActors;
                } else if (i == 7) {
                    line += averageMessagesPerActor;
                } else if (i == 8) {
                    line += totalNumOfMessagesForSensors;
                } else if (i == 9) {
                    line += averageMessagesPerSensor;
                }
                lines.add(line);
                line = br.readLine();
                i++;
            }
            br.close();


            PrintWriter pw = new PrintWriter(new FileWriter("result.csv"));
            for (int j = 0; j < lines.size(); j++) {
                String s = lines.get(j);
                pw.println(s);
            }
            pw.close();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void makeConnected(ArrayList<Gateway> list, Square sq) {
        ArrayList<ArrayList<Gateway>> partitions = NetworkUtils.DephtFirstSearch(list);

        while (partitions.size() != 1) {
            connect(partitions.get(0), partitions.get(1), sq);
            NetworkUtils.calculateActorNeighborhoods(list, Constants.ActorTransmissionRange);
            partitions = NetworkUtils.DephtFirstSearch(list);
        }
//
    }

    public void connect(ArrayList<Gateway> list1, ArrayList<Gateway> list2, Square sq) {


        double dist = -1;
        Gateway closestNode1 = new Gateway(0), closestNode2 = new Gateway(0);
        for (int i = 0; i < list1.size(); i++) {
            for (int j = 0; j < list2.size(); j++) {
                if (dist == -1 || NetworkUtils.Distance(list1.get(i), list2.get(j)) < dist) {
                    dist = NetworkUtils.Distance(list1.get(i), list2.get(j));
                    closestNode1 = list1.get(i);
                    closestNode2 = list2.get(j);
                }
            }
        }
        double xDiff = closestNode2.x - closestNode1.x;
        double yDiff = closestNode2.y - closestNode1.y;
        double cosTeta = Math.abs(xDiff) / dist;
        double sinTeta = Math.abs(yDiff) / dist;
        double xAmount = Math.abs(xDiff) - cosTeta * Constants.ActorTransmissionRange;
        double yAmount = Math.abs(yDiff) - sinTeta * Constants.ActorTransmissionRange;
        xAmount += 10;
        yAmount += 10;
        if (xDiff > 0) {
            xAmount *= -1;
        }
        if (yDiff > 0) {
            yAmount *= -1;
        }
        for (int i = 0; i < list2.size(); i++) {
            Gateway gateway = list2.get(i);
            gateway.x += xAmount;
            gateway.y += yAmount;

            if (gateway.x > sq.topX + sq.width)
                gateway.x = sq.topX + sq.width;
            if (gateway.x < sq.topX)
                gateway.x = sq.topX;
            if (gateway.y > sq.topY + sq.width)
                gateway.y = sq.topY + sq.width;
            if (gateway.y < sq.topY)
                gateway.y = sq.topY;
        }
    }


    public void generateExperimentData() {
        for (int p = 41; p <= 50; p++) {
            for (int e = 1; e <= 100; e++) {
                if(p==41 && e < 58)continue;
                boolean redo = false, first = true;
                while (first || redo) {
                    first = false;
                    redo = false;
                    String filename = p + "_" + e + ".dat";
                    fix(p, e);
                    save(filename);

                    reload(filename);
                    ArrayList<ArrayList<Gateway>> segments = NetworkUtils.DephtFirstSearch(ActorsArray);

                    if (segments.size() != p || ActorsArray.size() != p) {
                        redo = true;
                        System.out.println("redo");
                    } else {
                        System.out.println(filename + " is generated");
                    }


                }
            }
        }
    }


    public void generateSegments(int numofSegments, int minNumofNodesinaSegments, int maxNumofNodesinaSegments) throws ArrayIndexOutOfBoundsException {
        ActorsArray.clear();
        RelayNodeArray.clear();
        generateGrid();
        int n = grid.length;
        int numOfCells = n * n;
//        System.out.println("");
        int active[][] = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                active[i][j] = 1;
            }
        }

        for (int i = 0; i < numofSegments; i++) {
            int cell;
            int row;
            int column;
            while (true) {
                cell = (int) Math.round(Math.random() * numOfCells);
                row = cell / n;
                column = cell % n;
                if (active[row][column] == 1)
                    break;
            }
            int numOfNodes = 1;
//            int numOfNodes = (int) Math.round((Math.random() * maxNumofNodesinaSegments) + (minNumofNodesinaSegments + 1));
            ActorsArray.addAll(generateRandomGraph(0, numOfNodes - 1, grid[row][column]));
            int pr = row - 1;//previous row
            int nr = row + 1;//next row
            int pc = column - 1;//prev column
            int nc = column + 1;//next column
            for (int k = pr; k <= nr; k++) {
                for (int h = pc; h <= nc; h++) {
                    if (k >= 0 && k < n && h >= 0 && h < n) {
                        active[k][h] = 0;
                    }
                }
            }
        }

        NetworkUtils.calculateActorNeighborhoods(ActorsArray, Constants.ActorTransmissionRange);
        reAssignGatewayIDs();
        calculateCenterOfMass();

    }


    public void generateSegments2(int numofSegments, int minNumofNodesinaSegments, int maxNumofNodesinaSegments) throws ArrayIndexOutOfBoundsException {
        ActorsArray.clear();
        RelayNodeArray.clear();
        generateGrid();
        int n = grid.length;
        int m = grid[0].length;
        int numOfCells = n * m;
//        System.out.println("");
        int active[][] = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                active[i][j] = 1;
            }
        }

        for (int i = 0; i < numofSegments; i++) {
            int cell;
            int row;
            int column;
            while (true) {
                cell = (int) Math.round(Math.random() * numOfCells);
                row = cell / m;
                column = cell % m;
                if (active[row][column] == 1)
                    break;
            }
            int numOfNodes = 1;//(int) Math.round((Math.random() * maxNumofNodesinaSegments) + (minNumofNodesinaSegments + 1));
            ActorsArray.addAll(generateRandomGraph(0, numOfNodes - 1, grid[row][column]));
            int pr = row - 1;//previous row
            int nr = row + 1;//next row
            int pc = column - 1;//prev column
            int nc = column + 1;//next column
            for (int k = pr; k <= nr; k++) {
                for (int h = pc; h <= nc; h++) {
                    if (k >= 0 && k < n && h >= 0 && h < m) {
                        active[k][h] = 0;
                    }
                }
            }
        }

        NetworkUtils.calculateActorNeighborhoods(ActorsArray, Constants.ActorTransmissionRange);
        reAssignGatewayIDs();
        calculateCenterOfMass();

    }


    public void calculateCenterOfMass() {
        ArrayList<ArrayList<Gateway>> partitions = NetworkUtils.DephtFirstSearch(ActorsArray);


        ArrayList<Gateway> temp = new ArrayList<Gateway>();
        for (int i = 0; i < partitions.size(); i++) {
            ArrayList<Gateway> gateways = partitions.get(i);
            temp.add(gateways.get(0));
        }
        convexHull = PolygonUtilities.getConvexHull(temp, Constants.ApplicationAreaWidth, Constants.ApplicationAreaHeight);
        CoM = PolygonUtilities.centerOfMass(convexHull);
        /*
        Point2D tmpCOM = PolygonUtilities.centerOfMass(convexHull);

        ArrayList<Gateway> tmpConvexHull = new ArrayList<Gateway>(convexHull);
        convexHull.clear();

        for (int i = 0; i < tmpConvexHull.size(); i++) {
            Gateway gateway = tmpConvexHull.get(i);
            for (int j = 0; j < partitions.size(); j++) {
                ArrayList<Gateway> p1 = partitions.get(j);
                if (p1.get(0).getNetworkID() == gateway.getNetworkID()) {
                    Gateway g = getActualRepresentative(p1, tmpCOM);
                    g.representative = true;
                    convexHull.add(g);
                }
            }
        }


        CoM = PolygonUtilities.centerOfMass(convexHull);
        */
        polygon = new Point2D[convexHull.size()];
        lines = new Line[convexHull.size()];
        for (int i = 0; i < convexHull.size(); i++) {
            Gateway g = convexHull.get(i);
            Point2D p = new Point2D.Double();
            p.setLocation(g.getX(), g.getY());
            polygon[i] = p;
            lines[i] = new Line(g, CoM, i);
        }

        for (int i = 0; i < lines.length; i++) {
            Line line = lines[i];
            if (i == 0) {
                line.left = lines[lines.length - 1];
                line.right = lines[i + 1];
            } else if (i == lines.length - 1) {
                line.left = lines[i - 1];
                line.right = lines[0];
            } else {
                line.left = lines[i - 1];
                line.right = lines[i + 1];
            }
        }


        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
        for (int i = 0; i < lines.length; i++) {
            Line line = lines[i];
            if (map.get(line.representative.getID()) == null) {
                map.put(line.representative.getID(), 1);
            } else {
                int c = map.get(line.representative.getID());
                c++;
                map.put(line.representative.getID(), c);
            }
        }
        Line[] tmp = new Line[map.keySet().size()];
        Iterator<Integer> iter = map.keySet().iterator();
        int counter = 0;
        while (iter.hasNext()) {
            Integer key = iter.next();
//            int value = map.get(key);
            for (Line line : lines) {
                if (line.representative.getID() == key) {
                    tmp[counter++] = line;
                    break;
                }
            }
        }
//        lines = new Line[tmp.length];
//        for (int i = 0; i < tmp.length; i++) {
//            lines[i] = tmp[i];
//        }

    }

    public ArrayList<ArrayList<Gateway>> orderNeigborPartitions(ArrayList<ArrayList<Gateway>> p) {
        ArrayList<ArrayList<Gateway>> partitions = new ArrayList<ArrayList<Gateway>>();
        partitions.addAll(p);
        ArrayList<ArrayList<Gateway>> tmp = new ArrayList<ArrayList<Gateway>>();
        for (int i = 0; i < grid[0].length; i++) {
            Square square = grid[0][i];
            for (int j = 0; j < partitions.size(); j++) {
                Gateway gateway = partitions.get(j).get(0);
                if (gateway.getX() >= square.topX && gateway.getX() <= square.topX + square.width && gateway.getY() >= square.topY && gateway.getY() <= square.topY + square.width) {
                    tmp.add(partitions.get(j));
                    partitions.remove(j);
                    break;
                }
            }
        }
        for (int i = 1; i < grid.length; i++) {
            Square square = grid[i][grid[i].length - 1];
            for (int j = 0; j < partitions.size(); j++) {
                Gateway gateway = partitions.get(j).get(0);
                if (gateway.getX() >= square.topX && gateway.getX() <= square.topX + square.width && gateway.getY() >= square.topY && gateway.getY() <= square.topY + square.width) {
                    tmp.add(partitions.get(j));
                    partitions.remove(j);
                    break;
                }
            }
        }

        for (int i = grid[0].length - 2; i >= 0; i--) {
            Square square = grid[grid.length - 1][i];
            for (int j = 0; j < partitions.size(); j++) {
                Gateway gateway = partitions.get(j).get(0);
                if (gateway.getX() >= square.topX && gateway.getX() <= square.topX + square.width && gateway.getY() >= square.topY && gateway.getY() <= square.topY + square.width) {
                    tmp.add(partitions.get(j));
                    partitions.remove(j);
                    break;
                }
            }
        }
        for (int i = grid.length - 1; i > 0; i--) {
            Square square = grid[i][0];
            for (int j = 0; j < partitions.size(); j++) {
                Gateway gateway = partitions.get(j).get(0);
                if (gateway.getX() >= square.topX && gateway.getX() <= square.topX + square.width && gateway.getY() >= square.topY && gateway.getY() <= square.topY + square.width) {
                    tmp.add(partitions.get(j));
                    partitions.remove(j);
                    break;
                }
            }
        }
        return tmp;
    }

    private Gateway getActualRepresentative(ArrayList<Gateway> list, Point2D com) {
        double minDist = Double.MAX_VALUE;
        int minIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            Gateway gateway = list.get(i);
            double dist = AnalyticGeometry.euclideanDistance(gateway.getX(), gateway.getY(), com.getX(), com.getY());
            if (dist < minDist) {
                minDist = dist;
                minIndex = i;
            }
        }
        return list.get(minIndex);
    }

    private void reAssignGatewayIDs() {
        for (int i = 0; i < ActorsArray.size(); i++) {
            Gateway gateway = ActorsArray.get(i);
            gateway.setId(i);
        }
    }

    public void drawCoM(Graphics g) {
        drawConvexHull(g);
        if (firstApproach) {

            if (CoM != null) {
                int radius = 5;
                double x = CoM.getX();
                double y = CoM.getY();
                g.fillOval((int) (x - radius), (int) (y - radius), 2 * radius, 2 * radius);
            }

            ArrayList<Gateway> rep = getRepresentatives();
            for (int i = 0; i < rep.size(); i++) {
                Gateway gateway = rep.get(i);
                double cx = CoM.getX();
                double cy = CoM.getY();
                WSNGraphics.drawDashedLine(g, gateway.getX(), gateway.getY(), cx, cy, 5, 5);
            }
        }
    }

    private ArrayList<Gateway> getRepresentatives() {
        ArrayList<Gateway> rep = new ArrayList<Gateway>();
        for (int i = 0; i < ActorsArray.size(); i++) {
            if (ActorsArray.get(i).representative) {
                rep.add(ActorsArray.get(i));
            }
        }
        return rep;
    }

    public void save() {
        NetworkUtils.printToFile(new File("output.dat"), ActorsArray);
    }

    public void save(String filename) {
//        NetworkUtils.printToFile(new File("data/" + filename), ActorsArray);
        NetworkUtils.printToFile(new File(input_data_path + (int) Constants.ActorTransmissionRange + "/" + filename), ActorsArray);
    }

    public void saveSegment(String filename, int N_seg, int[][] segment) {
        try {

            PrintWriter pw = new PrintWriter(new FileWriter(filename));

            pw.println(N_seg);
            for (int i = 0; i < N_seg; i++) {
                pw.println("" + segment[i][0] + "\t" + segment[i][1]);
            }
            pw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        com_deployed = false;
        firstApproach = false;
        secondApproach = false;
        interfacePoints.clear();
        RelayNodeArray.clear();
        ActorsArray.clear();
        SensorArray.clear();
        ActorsArray = NetworkUtils.readFromFile(new File("output.dat"));
        generateGrid();

    }

    public void reload(String filename) {
        com_deployed = false;
        firstApproach = false;
        secondApproach = false;
        interfacePoints.clear();
        RelayNodeArray.clear();
        ActorsArray.clear();
        SensorArray.clear();
        ActorsArray = NetworkUtils.readFromFile(new File(input_data_path + (int) Constants.ActorTransmissionRange + "/" + filename));
        generateGrid();

    }


    public void drawGrid(Graphics g) {
        double squareSize = Constants.RNTransmissionRange / Math.sqrt(2);

        int MAX_ROW = (int) Math.ceil(Constants.ApplicationAreaHeight / squareSize);
        int MAX_COL = (int) Math.ceil(Constants.ApplicationAreaWidth / squareSize);

        for (int i = 0; i < MAX_ROW; i++) {
            int y = (int) (i * squareSize);
            int x1 = 0;
            int x2 = Constants.FrameWidth;
            g.drawLine(x1, y, x2, y);
        }
        for (int i = 0; i < MAX_COL; i++) {
            int x = (int) (i * squareSize);
            int y1 = 0;
            g.drawLine(x, y1, x, 1000);
        }
    }

    public ArrayList<Triangle> findDelaunayTriangles(ArrayList<Gateway> list) {
        DTAdapter adapter = new DTAdapter(list);
        ArrayList<Gateway[]> dts = adapter.getDelaunayTriangles();
        ArrayList<Triangle> delaunayTriangles = new ArrayList<Triangle>();
        for (int i = 0; i < dts.size(); i++) {
            Gateway[] gateways = dts.get(i);
            delaunayTriangles.add(new Triangle(gateways[0], gateways[1], gateways[2]));
        }
        return delaunayTriangles;
    }


    /**
     * The gain of the triangle will be calculated by actual mst edges
     */
    public void secondDTApproach() {
        ArrayList<Edge> edges = NetworkUtils.runKruskal(ActorsArray);
        mstEdgeMap = buildMSTHashMap(edges);

        ArrayList<Triangle> delaunayTriangles = findDelaunayTriangles(ActorsArray);

        ArrayList<TriangleVertex> tag = new ArrayList<TriangleVertex>();
        for (int i = 0; i < delaunayTriangles.size(); i++) {
            Triangle current = delaunayTriangles.get(i);
            ArrayList<Gateway> path12 = FindTreePath(current.s1, current.s2, ActorsArray, mstEdgeMap);
            ArrayList<Gateway> path13 = FindTreePath(current.s1, current.s3, ActorsArray, mstEdgeMap);

            ArrayList<Gateway> path12_prime = new ArrayList<Gateway>(path12);
            path12_prime.remove(path12_prime.size() - 1);
            path12_prime.remove(0);
            ArrayList<Gateway> path13_prime = new ArrayList<Gateway>(path13);
            path13_prime.remove(path13_prime.size() - 1);
            path13_prime.remove(0);

            if (!path12_prime.isEmpty() && !path13_prime.isEmpty()) {
                if (containsAll(path12_prime, path13_prime)) {
                    path12 = FindTreePath(current.s3, current.s1, ActorsArray, mstEdgeMap);
                    path13 = FindTreePath(current.s3, current.s2, ActorsArray, mstEdgeMap);
                } else if (containsAll(path13_prime, path12_prime)) {
                    path12 = FindTreePath(current.s2, current.s1, ActorsArray, mstEdgeMap);
                    path13 = FindTreePath(current.s2, current.s3, ActorsArray, mstEdgeMap);
                }
            } else {
                if (containsAll(path12, path13)) {
                    path12 = FindTreePath(current.s3, current.s1, ActorsArray, mstEdgeMap);
                    path13 = FindTreePath(current.s3, current.s2, ActorsArray, mstEdgeMap);
                } else if (containsAll(path13, path12)) {
                    path12 = FindTreePath(current.s2, current.s1, ActorsArray, mstEdgeMap);
                    path13 = FindTreePath(current.s2, current.s3, ActorsArray, mstEdgeMap);
                }
            }
            ArrayList<Edge> epath12 = new ArrayList<Edge>();
            ArrayList<Edge> epath13 = new ArrayList<Edge>();
            for (int j = 0; j < path12.size() - 1; j++) {
                epath12.add(new Edge(path12.get(j), path12.get(j + 1)));
            }
            for (int j = 0; j < path13.size() - 1; j++) {
                epath13.add(new Edge(path13.get(j), path13.get(j + 1)));
            }
            Collections.sort(epath12);
            Collections.sort(epath13);
            Edge mst1 = epath12.get(epath12.size() - 1);
            Edge mst2 = epath13.get(epath13.size() - 1);
            TriangleVertex tv = new TriangleVertex(current, i, mst1, mst2);
            if (tv.getActualWeight() > 0) {
                tag.add(tv);
            }
        }
        ArrayList<ArrayList<TriangleVertex>> isets = findMaximumWeightedIndependentFromTAG(tag, true);
        int[] weights = new int[isets.size()];
        ArrayList<ArrayList<Gateway>> alternativeRelays = new ArrayList<ArrayList<Gateway>>();
        ArrayList<HashMap<String, Edge>> alternativeMstMap = new ArrayList<HashMap<String, Edge>>();
        if (isets.isEmpty()) {
            weights = new int[1];
            ArrayList<Gateway> relayList = new ArrayList<Gateway>(RelayNodeArray);
            HashMap<String, Edge> mstEdgeMapClone = new HashMap<String, Edge>();
            Iterator<String> iter = mstEdgeMap.keySet().iterator();
            for (int i = 0; i < ActorsArray.size(); i++) {
                Gateway gateway = ActorsArray.get(i);
                gateway.setCcid(0);
            }
            while (iter.hasNext()) {
                String key = iter.next();
                mstEdgeMapClone.put(key, mstEdgeMap.get(key));
            }

            ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
            allNodes.addAll(relayList);
            int nextCCID = relayList.size() + 1;
            for (int i = 0; i < ActorsArray.size(); i++) {
                Gateway gateway = ActorsArray.get(i);
                if (gateway.ccid == 0) {
                    gateway.setCcid(nextCCID++);
                }
            }

            RN_MST result = federateCC(findDelaunayTriangles(allNodes), relayList, mstEdgeMapClone);
            result = optimizeMstEdgeMap(result.relayList, result.map);
            relayList = result.relayList;
            mstEdgeMapClone = result.map;

            weights[0] = relayList.size() + findMstEdgeMapCost(mstEdgeMapClone);
            alternativeRelays.add(0, relayList);
            alternativeMstMap.add(0, mstEdgeMapClone);

        } else {
            for (int index = 0; index < isets.size(); index++) {
                ArrayList<TriangleVertex> iset = isets.get(index);
                ArrayList<Gateway> actorList = new ArrayList<Gateway>(ActorsArray);
                ArrayList<Gateway> relayList = new ArrayList<Gateway>(RelayNodeArray);
                HashMap<String, Edge> mstEdgeMapClone = new HashMap<String, Edge>();
                Iterator<String> iter = mstEdgeMap.keySet().iterator();
                for (int i = 0; i < ActorsArray.size(); i++) {
                    Gateway gateway = ActorsArray.get(i);
                    gateway.setCcid(0);
                }
                while (iter.hasNext()) {
                    String key = iter.next();
                    mstEdgeMapClone.put(key, mstEdgeMap.get(key));
                }
                deployRNatFermatPoints(iset, relayList, mstEdgeMapClone, true);
                ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
                allNodes.addAll(relayList);
                int nextCCID = relayList.size() + 1;
                for (int i = 0; i < ActorsArray.size(); i++) {
                    Gateway gateway = ActorsArray.get(i);
                    if (gateway.ccid == 0) {
                        gateway.setCcid(nextCCID++);
                    }
                }
                RN_MST result = federateCC(findDelaunayTriangles(allNodes), relayList, mstEdgeMapClone);
                if (result != null) {
                    result = optimizeMstEdgeMap(result.relayList, result.map);
                    relayList = result.relayList;
                    mstEdgeMapClone = result.map;

                    weights[index] = relayList.size() + findMstEdgeMapCost(mstEdgeMapClone);
                    alternativeRelays.add(index, relayList);
                    alternativeMstMap.add(index, mstEdgeMapClone);
                } else {
                    weights[index] = Integer.MAX_VALUE;
                    alternativeRelays.add(index, new ArrayList<Gateway>());
                    alternativeMstMap.add(index, new HashMap<String, Edge>());
                }
            }
        }

        int minIndex = -1;
        int minWeight = Integer.MAX_VALUE;
        for (int i = 0; i < weights.length; i++) {
            int weight = weights[i];
            if (weight < minWeight) {
                minIndex = i;
                minWeight = weight;
            }
        }
        RelayNodeArray = new ArrayList<Gateway>(alternativeRelays.get(minIndex));
        mstEdgeMap = alternativeMstMap.get(minIndex);
        steinerizeMstEdges(mstEdgeMap);
//        System.out.println();
    }

    public void run_DT_MST_DT_Approach(boolean steinerize) {
        ArrayList<Gateway> terminals = new ArrayList<Gateway>(ActorsArray);
        runDTMSTApproach(false);
        ArrayList<Gateway> relays = new ArrayList<Gateway>(RelayNodeArray);
        ArrayList<Gateway> allnodes = new ArrayList<Gateway>(ActorsArray);
        allnodes.addAll(relays);
        ActorsArray.clear();
        ActorsArray.addAll(allnodes);
        RelayNodeArray.clear();

        runDelaunayTriangulationApproach(false);
        RelayNodeArray.addAll(relays);
        ActorsArray.clear();
        ActorsArray.addAll(terminals);
        allnodes.clear();
        allnodes.addAll(ActorsArray);
        allnodes.addAll(RelayNodeArray);
        mstEdgeMap = buildMSTHashMap(NetworkUtils.runKruskal(allnodes));

        if (steinerize) {
            steinerizeMstEdges(mstEdgeMap);
        }
    }

    public void run_MST_DT_MST_Approach(boolean steinerize) {
        ArrayList<Gateway> terminals = new ArrayList<Gateway>(ActorsArray);
        runMSTDTApproach(false);
        ArrayList<Gateway> relays = new ArrayList<Gateway>(RelayNodeArray);
        ArrayList<Gateway> allnodes = new ArrayList<Gateway>(ActorsArray);
        allnodes.addAll(relays);
        ActorsArray.clear();
        ActorsArray.addAll(allnodes);
        RelayNodeArray.clear();

        for (int i = ActorsArray.size() - 1; i >= 0; i--) {
            Gateway gateway = allnodes.get(i);
            boolean remove = false;
            if (gateway.isRelay()) {
                for (int j = 0; j < ActorsArray.size(); j++) {
                    Gateway g1 = ActorsArray.get(j);
                    if (i != j && NetworkUtils.EstimatedDistance(gateway, g1) == 0) {
                        remove = true;
                    }
                }
            }
            if (remove) {
                ActorsArray.remove(gateway);
                relays.remove(gateway);
                allnodes.remove(gateway);
            }

        }

        runMSTTriangulationApproach(false);
        RelayNodeArray.addAll(relays);
        ActorsArray.clear();
        ActorsArray.addAll(terminals);
        allnodes.clear();
        allnodes.addAll(ActorsArray);
        allnodes.addAll(RelayNodeArray);
        mstEdgeMap = buildMSTHashMap(NetworkUtils.runKruskal(allnodes));

        if (steinerize) {
            steinerizeMstEdges(mstEdgeMap);
        }
    }

    public void runDTMSTApproach(boolean steinerize) {
        ArrayList<Gateway> terminals = new ArrayList<Gateway>(ActorsArray);
        runDelaunayTriangulationApproach(false);
        ArrayList<Gateway> relays = new ArrayList<Gateway>(RelayNodeArray);
        ArrayList<Gateway> allnodes = new ArrayList<Gateway>(ActorsArray);
        allnodes.addAll(relays);

        ActorsArray.addAll(relays);
        RelayNodeArray.clear();
        runMSTTriangulationApproach(false);
        relays.addAll(RelayNodeArray);

        ActorsArray.clear();
        ActorsArray.addAll(terminals);
        RelayNodeArray.clear();
        RelayNodeArray.addAll(relays);
        if (steinerize) {
            steinerizeMstEdges(mstEdgeMap);
        }
    }

    /**
     * Two mst edges form a triangle if they share an end point. We list all possible such triangles in this approach.
     * But we filter out the triangles having the following property
     * Let ABC be triangle whe AB and BC are mst edges and also let ABD is a triangle where AB and BD are mst edges
     * Assume the angle ABC is less then 180 and equal to the the sum ABC = ABD + CBD we filter out ABC
     */
    public void runMSTDTApproach(boolean steinerize) {
        ArrayList<Edge> edges = NetworkUtils.runKruskal(ActorsArray);
        mstEdgeMap = buildMSTHashMap(edges);
        HashMap<Gateway, ArrayList<Triangle>> mstTriangulationMap = formMSTTriangulation();
        Iterator<Gateway> iterator;
        ArrayList<Triangle> triangles = new ArrayList<Triangle>();
        iterator = mstTriangulationMap.keySet().iterator();
        while (iterator.hasNext()) {
            Gateway next = iterator.next();
            triangles.addAll(mstTriangulationMap.get(next));
        }
        ArrayList<TriangleVertex> tag = new ArrayList<TriangleVertex>();

        for (int i = 0; i < triangles.size(); i++) {
            Triangle current = triangles.get(i);
            if (current.p_weight - current.f_weight > 0) {
                TriangleVertex vertex = new TriangleVertex(current, i);
                tag.add(vertex);
            }
        }
        //new version
        ArrayList<ArrayList<TriangleVertex>> isets = findMaximumWeightedIndependentFromTAG(tag, false);

        int[] weights = new int[isets.size()];
        ArrayList<ArrayList<Gateway>> alternativeRelays = new ArrayList<ArrayList<Gateway>>();
        ArrayList<HashMap<String, Edge>> alternativeMstMap = new ArrayList<HashMap<String, Edge>>();
        if (isets.isEmpty()) {
            weights = new int[1];
            ArrayList<Gateway> relayList = new ArrayList<Gateway>(RelayNodeArray);
            HashMap<String, Edge> mstEdgeMapClone = new HashMap<String, Edge>();
            Iterator<String> iter = mstEdgeMap.keySet().iterator();
            for (int i = 0; i < ActorsArray.size(); i++) {
                Gateway gateway = ActorsArray.get(i);
                gateway.setCcid(0);
            }
            while (iter.hasNext()) {
                String key = iter.next();
                mstEdgeMapClone.put(key, mstEdgeMap.get(key));
            }

            ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
            allNodes.addAll(relayList);
            int nextCCID = relayList.size() + 1;
            for (int i = 0; i < ActorsArray.size(); i++) {
                Gateway gateway = ActorsArray.get(i);
                if (gateway.ccid == 0) {
                    gateway.setCcid(nextCCID++);
                }
            }

            RN_MST result = federateCC(findDelaunayTriangles(allNodes), relayList, mstEdgeMapClone);
            result = optimizeMstEdgeMap(result.relayList, result.map);
            relayList = result.relayList;
            mstEdgeMapClone = result.map;

            weights[0] = relayList.size() + findMstEdgeMapCost(mstEdgeMapClone);
            alternativeRelays.add(0, relayList);
            alternativeMstMap.add(0, mstEdgeMapClone);

        } else {
            for (int index = 0; index < isets.size(); index++) {
                ArrayList<TriangleVertex> iset = isets.get(index);
                ArrayList<Gateway> actorList = new ArrayList<Gateway>(ActorsArray);
                ArrayList<Gateway> relayList = new ArrayList<Gateway>(RelayNodeArray);
                HashMap<String, Edge> mstEdgeMapClone = new HashMap<String, Edge>();
                Iterator<String> iter = mstEdgeMap.keySet().iterator();
                for (int i = 0; i < ActorsArray.size(); i++) {
                    Gateway gateway = ActorsArray.get(i);
                    gateway.setCcid(0);
                }
                while (iter.hasNext()) {
                    String key = iter.next();
                    mstEdgeMapClone.put(key, mstEdgeMap.get(key));
                }
                deployRNatFermatPoints(iset, relayList, mstEdgeMapClone, false);
                ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
                allNodes.addAll(relayList);
                int nextCCID = relayList.size() + 1;
                for (int i = 0; i < ActorsArray.size(); i++) {
                    Gateway gateway = ActorsArray.get(i);
                    if (gateway.ccid == 0) {
                        gateway.setCcid(nextCCID++);
                    }
                }
                RN_MST result = federateCC(findDelaunayTriangles(allNodes), relayList, mstEdgeMapClone);
//                result = optimizeMstEdgeMap(result.relayList, result.map);
                relayList = result.relayList;
                mstEdgeMapClone = result.map;

                weights[index] = relayList.size() + findMstEdgeMapCost(mstEdgeMapClone);
                alternativeRelays.add(index, relayList);
                alternativeMstMap.add(index, mstEdgeMapClone);
            }
        }

        int minIndex = -1;
        int minWeight = Integer.MAX_VALUE;
        for (int i = 0; i < weights.length; i++) {
            int weight = weights[i];
            if (weight < minWeight) {
                minIndex = i;
                minWeight = weight;
            }
        }
        RelayNodeArray = new ArrayList<Gateway>(alternativeRelays.get(minIndex));
        mstEdgeMap = alternativeMstMap.get(minIndex);
        if (steinerize) {
            steinerizeMstEdges(mstEdgeMap);
        }
//        analyzeMstEdgeMap();
    }

    // mst-triangulation approach
    public void runMSTTriangulationApproach(boolean steinerize) {
        ArrayList<Edge> edges = NetworkUtils.runKruskal(ActorsArray);
        mstEdgeMap = buildMSTHashMap(edges);
        HashMap<Gateway, ArrayList<Triangle>> mstTriangulationMap = formMSTTriangulation();
        Iterator<Gateway> iterator;
        ArrayList<Triangle> triangles = new ArrayList<Triangle>();
        iterator = mstTriangulationMap.keySet().iterator();
        while (iterator.hasNext()) {
            Gateway next = iterator.next();
            triangles.addAll(mstTriangulationMap.get(next));
        }
        ArrayList<TriangleVertex> tag = new ArrayList<TriangleVertex>();

        for (int i = 0; i < triangles.size(); i++) {
            Triangle current = triangles.get(i);
            if (current.p_weight - current.f_weight > 0) {
                TriangleVertex vertex = new TriangleVertex(current, i);
                tag.add(vertex);
            }
        }
        ArrayList<ArrayList<TriangleVertex>> isets = findMaximumWeightedIndependentFromTAG(tag, false);
        if (!isets.isEmpty()) {

            ArrayList<TriangleVertex> iset = isets.get(0);
            Iterator<String> iter = mstEdgeMap.keySet().iterator();
            for (int i = 0; i < ActorsArray.size(); i++) {
                Gateway gateway = ActorsArray.get(i);
                gateway.setCcid(0);
            }

            deployRNatFermatPoints(iset, RelayNodeArray, mstEdgeMap, false);
            ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
            allNodes.addAll(RelayNodeArray);
            int nextCCID = RelayNodeArray.size() + 1;
            for (int i = 0; i < ActorsArray.size(); i++) {
                Gateway gateway = ActorsArray.get(i);
                if (gateway.ccid == 0) {
                    gateway.setCcid(nextCCID++);
                }
            }
        }
        if (steinerize) {
            steinerizeMstEdges(mstEdgeMap);
        }

        for (int i = RelayNodeArray.size() - 1; i >= 0; i--) {
            Gateway gateway = RelayNodeArray.get(i);
            boolean remove = false;
            for (int j = 0; j < ActorsArray.size(); j++) {
                Gateway g1 = ActorsArray.get(j);
                if (NetworkUtils.EstimatedDistance(gateway, g1) == 0) {
                    remove = true;
                }
            }
            if (remove) {
                RelayNodeArray.remove(gateway);
            }

        }

    }

    public void runIterativeMSTTriangulationApproach(int counter) {
        System.out.print(counter + "\t");
        ArrayList<Gateway> allnodes = new ArrayList<Gateway>(ActorsArray);
        allnodes.addAll(RelayNodeArray);
        ArrayList<Edge> edges = NetworkUtils.runKruskal(allnodes);
        mstEdgeMap = buildMSTHashMap(edges);
        HashMap<Gateway, ArrayList<Triangle>> mstTriangulationMap = formMSTTriangulation();
        Iterator<Gateway> iterator;
        ArrayList<Triangle> triangles = new ArrayList<Triangle>();
        iterator = mstTriangulationMap.keySet().iterator();
        while (iterator.hasNext()) {
            Gateway next = iterator.next();
            triangles.addAll(mstTriangulationMap.get(next));
        }
        ArrayList<TriangleVertex> tag = new ArrayList<TriangleVertex>();

        for (int i = 0; i < triangles.size(); i++) {
            Triangle current = triangles.get(i);
            if (current.p_weight - current.f_weight > 0) {
                TriangleVertex vertex = new TriangleVertex(current, i);
                tag.add(vertex);
            }
        }
        ArrayList<ArrayList<TriangleVertex>> isets = findMaximumWeightedIndependentFromTAG(tag, false);
        if (!isets.isEmpty()) {

            ArrayList<TriangleVertex> iset = isets.get(0);

            deployRNatFermatPoints2(iset, RelayNodeArray, mstEdgeMap);

            runIterativeMSTTriangulationApproach(++counter);
        } else {
            return;
        }

    }

    public void runHybrid(String filename) {
        System.out.print("MST-DT\t");
        reload(filename);
        runMSTDTApproach(true);
//                            report(path + "mstdt.csv", p, ar);
        int n_mst_dt = RelayNodeArray.size();


        System.out.print("DT-MST\t");
        reload(filename);
        runDTMSTApproach(true);
        int n_dt_mst = RelayNodeArray.size();

        if (n_mst_dt < n_dt_mst) {
            System.out.println("Once more");
            reload(filename);
            runMSTDTApproach(true);

        }
    }

    public void runDelaunayTriangulationApproach(boolean steinerize) {
        ArrayList<Edge> edges = NetworkUtils.runKruskal(ActorsArray);
        mstEdgeMap = buildMSTHashMap(edges);
        RelayNodeArray.clear();
        int nextCCID = 1;
        for (int i = 0; i < ActorsArray.size(); i++) {
            ActorsArray.get(i).setCcid(nextCCID++);
        }
        RN_MST result = federateCC(findDelaunayTriangles(ActorsArray), RelayNodeArray, mstEdgeMap);
        RelayNodeArray = result.relayList;
        mstEdgeMap = result.map;
        if (steinerize) {
            steinerizeMstEdges(mstEdgeMap);
        }
    }


    private HashMap<Gateway, ArrayList<Triangle>> formMSTTriangulation() {
        HashMap<Gateway, ArrayList<Triangle>> mstTriangulationMap = new HashMap<Gateway, ArrayList<Triangle>>();

        for (String s : mstEdgeMap.keySet()) {
            Edge e1 = mstEdgeMap.get(s);
            for (String value : mstEdgeMap.keySet()) {
                Edge e2 = mstEdgeMap.get(value);
                if (e1 != e2) {
                    if (e1.u.getID() == e2.u.getID() || e1.u.getID() == e2.v.getID()) {
                        Triangle t;
                        ArrayList<Triangle> list;
                        if (e1.u.getID() == e2.u.getID()) {
                            t = new Triangle(e1.v, e1.u, e2.v);
                        } else {
                            t = new Triangle(e1.v, e1.u, e2.u);
                        }
                        if (mstTriangulationMap.get(e1.u) == null) {
                            list = new ArrayList<Triangle>();
                        } else {
                            list = mstTriangulationMap.get(e1.u);
                        }
                        list.add(t);
                        mstTriangulationMap.put(e1.u, list);
                    } else if (e1.v.getID() == e2.u.getID() || e1.v.getID() == e2.v.getID()) {
                        Triangle t;
                        ArrayList<Triangle> list;
                        if (e1.v.getID() == e2.u.getID()) {
                            t = new Triangle(e1.u, e1.v, e2.v);
                        } else {
                            t = new Triangle(e1.u, e1.v, e2.u);
                        }
                        if (mstTriangulationMap.get(e1.v) == null) {
                            list = new ArrayList<Triangle>();
                        } else {
                            list = mstTriangulationMap.get(e1.v);
                        }
                        list.add(t);
                        mstTriangulationMap.put(e1.v, list);
                    }
                }
            }
        }
        Iterator<Gateway> iterator = mstTriangulationMap.keySet().iterator();
        while (iterator.hasNext()) {
            Gateway next = iterator.next();
            ArrayList<Triangle> list = mstTriangulationMap.get(next);
            for (int i = list.size() - 1; i >= 0; i--) {
                boolean remove = false;
                for (int j = i - 1; j >= 0; j--) {
                    Triangle ti = list.get(i);
                    Triangle tj = list.get(j);
                    if ((ti.s1.getID() == tj.s1.getID() && ti.s3.getID() == tj.s3.getID()) || (ti.s1.getID() == tj.s3.getID() && ti.s3.getID() == tj.s1.getID())) {
                        remove = true;
                    }
                }
                if (remove) {
                    list.remove(i);
                }
            }

        }

        iterator = mstTriangulationMap.keySet().iterator();
        while (iterator.hasNext()) {
            Gateway next = iterator.next();
            ArrayList<Triangle> list = mstTriangulationMap.get(next);
            for (int i = list.size() - 1; i >= 0; i--) {
                Triangle ptr = list.get(i);
                boolean removePtr = false;
                for (int j = 0; j < list.size(); j++) {
                    if (j != i) {
                        Triangle tj = list.get(j);
                        for (int k = 0; k < list.size(); k++) {
                            if (k != i && k != j) {
                                Triangle tk = list.get(k);
                                if (ptr.intersectionCardinality(tj) == 2 && ptr.intersectionCardinality(tk) == 2) {
                                    double theta = AnalyticGeometry.findAngle(ptr.s1.getPoint2D(), ptr.s2.getPoint2D(), ptr.s3.getPoint2D());
                                    double alpha = AnalyticGeometry.findAngle(tj.s1.getPoint2D(), tj.s2.getPoint2D(), tj.s3.getPoint2D());
                                    double beta = AnalyticGeometry.findAngle(tk.s1.getPoint2D(), tk.s2.getPoint2D(), tk.s3.getPoint2D());
                                    if (theta <= 180 && Math.abs(theta - (alpha + beta)) < Math.pow(10, -4)) {
                                        removePtr = true;
                                    }
                                }
                            }
                        }
                    }
                }
                if (removePtr) {
                    list.remove(i);
                    mstTriangulationMap.put(next, list);
                }
            }
        }
        return mstTriangulationMap;
    }

    public boolean testDelaunay() {

        ArrayList<Edge> edges = NetworkUtils.runKruskal(ActorsArray);
        mstEdgeMap = buildMSTHashMap(edges);

        ArrayList<Triangle> delaunayTriangles = findDelaunayTriangles(ActorsArray);

        ArrayList<TriangleVertex> tag = new ArrayList<TriangleVertex>();

        for (int i = 0; i < delaunayTriangles.size(); i++) {
            Triangle current = delaunayTriangles.get(i);
            if (current.p_weight - current.f_weight > 0) {
                // todo if triangle does not contain two mst edges should we need to store the triangle in somewherelese for later processing???
                if (containsTwoMstEdges(current, mstEdgeMap)) {
                    TriangleVertex vertex = new TriangleVertex(current, i);
                    tag.add(vertex);
                }
            }
        }
        //new version
        ArrayList<ArrayList<TriangleVertex>> isets = findMaximumWeightedIndependentFromTAG(tag, false);
        int[] weights = new int[isets.size()];
        ArrayList<ArrayList<Gateway>> alternativeRelays = new ArrayList<ArrayList<Gateway>>();
        ArrayList<HashMap<String, Edge>> alternativeMstMap = new ArrayList<HashMap<String, Edge>>();
        if (isets.isEmpty()) {
            weights = new int[1];
            ArrayList<Gateway> relayList = new ArrayList<Gateway>(RelayNodeArray);
            HashMap<String, Edge> mstEdgeMapClone = new HashMap<String, Edge>();
            Iterator<String> iter = mstEdgeMap.keySet().iterator();
            for (int i = 0; i < ActorsArray.size(); i++) {
                Gateway gateway = ActorsArray.get(i);
                gateway.setCcid(0);
            }
            while (iter.hasNext()) {
                String key = iter.next();
                mstEdgeMapClone.put(key, mstEdgeMap.get(key));
            }

            ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
            allNodes.addAll(relayList);
            int nextCCID = relayList.size() + 1;
            for (int i = 0; i < ActorsArray.size(); i++) {
                Gateway gateway = ActorsArray.get(i);
                if (gateway.ccid == 0) {
                    gateway.setCcid(nextCCID++);
                }
            }

            RN_MST result = federateCC(findDelaunayTriangles(allNodes), relayList, mstEdgeMapClone);
            result = optimizeMstEdgeMap(result.relayList, result.map);
            relayList = result.relayList;
            mstEdgeMapClone = result.map;

            weights[0] = relayList.size() + findMstEdgeMapCost(mstEdgeMapClone);
            alternativeRelays.add(0, relayList);
            alternativeMstMap.add(0, mstEdgeMapClone);

        } else {
            for (int index = 0; index < isets.size(); index++) {
                ArrayList<TriangleVertex> iset = isets.get(index);
                ArrayList<Gateway> actorList = new ArrayList<Gateway>(ActorsArray);
                ArrayList<Gateway> relayList = new ArrayList<Gateway>(RelayNodeArray);
                HashMap<String, Edge> mstEdgeMapClone = new HashMap<String, Edge>();
                Iterator<String> iter = mstEdgeMap.keySet().iterator();
                for (int i = 0; i < ActorsArray.size(); i++) {
                    Gateway gateway = ActorsArray.get(i);
                    gateway.setCcid(0);
                }
                while (iter.hasNext()) {
                    String key = iter.next();
                    mstEdgeMapClone.put(key, mstEdgeMap.get(key));
                }
                deployRNatFermatPoints(iset, relayList, mstEdgeMapClone, false);
                ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
                allNodes.addAll(relayList);
                int nextCCID = relayList.size() + 1;
                for (int i = 0; i < ActorsArray.size(); i++) {
                    Gateway gateway = ActorsArray.get(i);
                    if (gateway.ccid == 0) {
                        gateway.setCcid(nextCCID++);
                    }
                }
                RN_MST result = federateCC(findDelaunayTriangles(allNodes), relayList, mstEdgeMapClone);
                result = optimizeMstEdgeMap(result.relayList, result.map);
                relayList = result.relayList;
                mstEdgeMapClone = result.map;

                weights[index] = relayList.size() + findMstEdgeMapCost(mstEdgeMapClone);
                alternativeRelays.add(index, relayList);
                alternativeMstMap.add(index, mstEdgeMapClone);
            }
        }

        int minIndex = -1;
        int minWeight = Integer.MAX_VALUE;
        for (int i = 0; i < weights.length; i++) {
            int weight = weights[i];
            if (weight < minWeight) {
                minIndex = i;
                minWeight = weight;
            }
        }
        RelayNodeArray = new ArrayList<Gateway>(alternativeRelays.get(minIndex));
        mstEdgeMap = alternativeMstMap.get(minIndex);
        steinerizeMstEdges(mstEdgeMap);

// old version
//        ArrayList<TriangleVertex> iset = findMaximumWeightedIndependentFromTAG(tag);
//        ArrayList<TriangleVertex> iset = new ArrayList<TriangleVertex>();
//        iset.add(tag.get(1));
//        iset.add(tag.get(2));
        /*HashMap<Integer, ArrayList<Gateway>> ccMap_tmp = deployRNatFermatPoints(iset);
        HashMap<Integer, ArrayList<Gateway>> ccMap = new HashMap<Integer, ArrayList<Gateway>>();
        for (int i = 0; i < ActorsArray.size(); i++) {
            Gateway g = ActorsArray.get(i);
            if (g.ccid == 0) {
                g.ccid = ActorsArray.size() + 1 + i;
                ArrayList<Gateway> list = new ArrayList<Gateway>();
                list.add(g);
                ccMap_tmp.put(g.ccid, list);
            }
        }

        Iterator<Integer> iter = ccMap_tmp.keySet().iterator();
        int counter = 1;
        while (iter.hasNext()) {
            ArrayList<Gateway> gateways = ccMap_tmp.get(iter.next());
            for (int i = 0; i < gateways.size(); i++) {
                Gateway gateway = gateways.get(i);
                gateway.setCcid(counter);
            }
            ccMap.put(counter, gateways);
            counter++;
        }*/

//        Edge[][] ccMST = new Edge[ccMap_tmp.keySet().size()][ccMap_tmp.keySet().size()];
//
//        iter = ccMap.keySet().iterator();
//
//        while (iter.hasNext()) {
//            int c1 = iter.next();
//            Iterator<Integer> iter2 = ccMap.keySet().iterator();
//            while (iter2.hasNext()) {
//                int c2 = iter2.next();
//                if (c1 != c2) {
//                    ccMST[c1 - 1][c2 - 1] = findMstEdgeConnectingCC(c1, c2, mstEdgeMap);
//                }
//
//            }
//
//        }

//        ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
//        allNodes.addAll(RelayNodeArray);
//        updatedDT = findDelaunayTriangles(allNodes);
//        federateCC(updatedDT, /*ccMST,*/ mstEdgeMap);
//        optimizeMstEdgeMap(mstEdgeMap);
//        steinerizeMstEdges(mstEdgeMap);
        return true;
    }

    public boolean analyzeMstEdgeMap(String filename, double mdtc_percentage, boolean exp) {
        if (tour_experiment_check.get(filename) == null)
            tour_experiment_check.put(filename, new int[30]);
        tours.clear();
        ArrayList<Edge> edge_tour = new ArrayList<Edge>();
//        HashMap<Gateway, ArrayList<Gateway>> triangle_tour = new HashMap<Gateway, ArrayList<Gateway>>();
        int[][] link = new int[1000][1000];
        ArrayList<Edge> edges = NetworkUtils.runKruskal(ActorsArray);
        mstEdgeMap = buildMSTHashMap(edges);

        for (String key : mstEdgeMap.keySet()) {
            Edge edge = mstEdgeMap.get(key);
            edge_tour.add(edge);
        }

        for (int i = 0; i < edge_tour.size(); i++) {
            Edge edge = edge_tour.get(i);
            ArrayList<Gateway> nodes = new ArrayList<Gateway>();
            nodes.add(edge.u);
            nodes.add(edge.v);
            tours.add(new Tour(tours.size(), nodes));
        }
        NetworkUtils.calculateActorNeighborhoods(ActorsArray, Constants.ActorTransmissionRange);
        int k = calculate_mdtc(mstEdgeMap, mdtc_percentage);
        System.out.println("MDTC : " + k);
        if (exp) {
            int[] chk = tour_experiment_check.get(filename);
            if (chk[k] == 0 && k <= tours.size()) {
                chk[k] = 1;
                tour_experiment_check.put(filename, chk);
                tours = Phase1UsingEncoding(k);
            } else {
                return false;
            }
        } else {
            ICCPhase1(k);
        }

        double cost = 0;
        for (int i = 0; i < tours.size(); i++) {
            cost += tours.get(i).getTourLength();
        }
//        System.out.println("Tour Count =" + tours.size() + "\t Cost = " + cost);
//        System.out.println();
        return true;
    }

    /**
     * This is an extension on IO-DT instead using only triangles DT we consider all possible triangles
     *
     * @param steinerize
     */
    public void runAllTriangulationApproach(boolean steinerize) {
        ArrayList<Edge> edges = NetworkUtils.runKruskal(ActorsArray);
        mstEdgeMap = buildMSTHashMap(edges);
        RelayNodeArray.clear();
        int nextCCID = 1;
        for (int i = 0; i < ActorsArray.size(); i++) {
            ActorsArray.get(i).setCcid(nextCCID++);
        }
        RN_MST result = federateCC(findAllTriangles(ActorsArray), RelayNodeArray, mstEdgeMap);
        RelayNodeArray = result.relayList;
        mstEdgeMap = result.map;
        if (steinerize) {
            steinerizeMstEdges(mstEdgeMap);
        }
    }

    private ArrayList<Triangle> findAllTriangles(ArrayList<Gateway> actorsArray) {
        ArrayList<Triangle> all_triangles = new ArrayList<Triangle>();
        HashMap<String, Triangle> map = new HashMap<String, Triangle>();
        for (Gateway g1 : actorsArray) {
            for (Gateway g2 : actorsArray) {
                if (g1 != g2) {
                    for (Gateway g3 : actorsArray) {
                        if (g3 != g1 && g3 != g2) {
                            String key = getKeyDescriptionOfTriangle(g1, g2, g3);
                            if (!map.containsKey(key)) {
                                Triangle t = new Triangle(g1, g2, g3);
                                map.put(key, t);
                                all_triangles.add(t);
                            }
                        }
                    }
                }
            }
        }
        return all_triangles;
    }

    private String getKeyDescriptionOfTriangle(Gateway g1, Gateway g2, Gateway g3) {
        int i1 = g1.getID();
        int i2 = g2.getID();
        int i3 = g3.getID();
        int sum = i1 + i2 + i3;
        int min = Math.min(i1, Math.min(i2, i3));
        int max = Math.max(i1, Math.max(i2, i3));
        int mid = sum - (min + max);

        return min + ";" + mid + ";" + max;
    }

    public void reportTourOutput(int p, int e, double percentage, String filename) {
        double ttl = 0; // total tour lengths
        double mtl = -1; // maximum tour length
        for (int i = 0; i < tours.size(); i++) {
            double tl = tours.get(i).getTourLength();
            ttl += tl;
            if (tl > mtl) {
                mtl = tl;
            }
        }

        int N = p;
        int topologyID = e;
        int M = calculate_mdtc(mstEdgeMap, percentage);

        try {

            PrintWriter pw = new PrintWriter(new FileWriter(new File(filename), true));
            pw.print(N + ",");
            pw.print(topologyID + ",");
            pw.print(tours.size() + ",");
            pw.print(percentage + ",");
            pw.print(M + ",");
            pw.print(ttl + ",");
            pw.print(mtl + ",");
            for (int i = 0; i < tours.size(); i++) {
                pw.print(tours.get(i).getTourLength());
                if (i != tours.size() - 1) {
                    pw.print(",");
                }
            }
            pw.println();
            pw.close();

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }


    private ArrayList<Tour> Phase1UsingEncoding(int k) {
        int n = tours.size();

        int[] s = new int[n];
        int[] m = new int[n];
        int[] minEncoding = new int[n];
        for (int i = 0; i < n; i++) {
            s[i] = 1;
            m[i] = 1;
            minEncoding[i] = 1;
        }
        int counter = 0;
        double minCost = Double.MAX_VALUE;


        long lStartTime = new Date().getTime();

        while (nextEncoding(s, m, n, k)) {
            if (checkPartionCountofEncoding(s, n, k)) {

                ArrayList<Tour> tour_parts = find_tour_partitioning_by_decoding(s, n);
                double cost = find_total_cost_of_tours(tour_parts);
//
                if (cost < minCost) {
                    minCost = cost;
                    for (int i = 0; i < s.length; i++) {
                        minEncoding[i] = s[i];
                    }
                }
                counter++;
//                System.out.print(counter+" ");
//                if(counter%50==0)
//                    System.out.println();
//                printp(s, n);
            }
        }

        long lEndTime = new Date().getTime(); //end time
        long difference = lEndTime - lStartTime; //check different
        System.out.println("Time Elapsed : " + difference);
        return find_tour_partitioning_by_decoding(minEncoding, n);
//        System.out.println(counter);
//        return new ArrayList<Tour>();
    }

    private ArrayList<Tour> find_tour_partitioning_by_decoding(int[] s, int n) {
        int part_num = 1;
        int i;
        for (i = 0; i < n; ++i) {
            if (s[i] > part_num) {
                part_num = s[i];
            }
        }
        HashMap<Integer, ArrayList<Tour>> tour_partitioning = new HashMap<Integer, ArrayList<Tour>>();
        ArrayList<Tour> list_of_tours = new ArrayList<Tour>();
        for (int p = part_num; p >= 1; --p) {
            ArrayList<Tour> l = new ArrayList<Tour>();
            tour_partitioning.put(p, l);
            /* If s[i] == p, then i + 1 is part of the pth partition. */
            for (i = 0; i < n; ++i) {
                if (s[i] == p) {
                    Tour tour = tours.get(i);
                    tour_partitioning.get(p).add(tour);
                }
            }
            ArrayList<Tour> tour_list = tour_partitioning.get(p);
            ArrayList<Gateway> tobeCollected = new ArrayList<Gateway>();
            for (int j = 0; j < tour_list.size(); j++) {
                Tour tour = tour_list.get(j);
                tobeCollected.addAll(tour.getTobeCollected());
            }
            list_of_tours.add(new Tour(findMaxTourid() + list_of_tours.size() + 1, tobeCollected));

        }
        return list_of_tours;
    }

    private double find_total_cost_of_tours(ArrayList<Tour> list) {
        double cost = 0;
        for (int i = 0; i < list.size(); i++) {
            cost += list.get(i).getTourLength();
        }
        return cost;
    }


    void printp(int[] s, int n) {
        /* Get the total number of partitions. In the exemple above, 2.*/
        int part_num = 1;
        int i;
        for (i = 0; i < n; ++i)
            if (s[i] > part_num)
                part_num = s[i];

        /* Print the p partitions. */
        int p;
        for (p = part_num; p >= 1; --p) {
            System.out.print("{");
            /* If s[i] == p, then i + 1 is part of the pth partition. */
            for (i = 0; i < n; ++i)
                if (s[i] == p)
                    System.out.print("" + (i + 1));
            System.out.print("} ");
        }
//        System.out.println();
    }

    private boolean checkPartionCountofEncoding(int[] s, int n, int k) {
        int part_num = 1;
        for (int i = 0; i < n; ++i) {
            if (s[i] > part_num) {
                part_num = s[i];
            }
        }
        return part_num == k;
    }

    private boolean nextEncoding(int[] s, int[] m, int n, int k) {
        int i = 0;
        ++s[i];
        while ((i < n - 1) && ((s[i] > m[i] + 1) || (s[i] > k))) {
            s[i] = 1;
            ++i;
            ++s[i];
        }
        /* If i is has reached n-1 th element, then the last unique partitiong has been found*/
        if (i == n - 1) {
            return false;
        }

        /* Because all the first i elements are now 1, s[i] (i + 1 th element)
          is the largest. So we update max by copying it to all the first i
          positions in m.*/


        int max = Math.max(s[i], m[i]);

        for (i = i - 1; i >= 0; --i) {
            m[i] = max;
        }

        return true;

    }

    private ArrayList<MergedTours> findMinCostListOfMergedTours(ArrayList<ArrayList<MergedTours>> list_of_merged_tours) {
        double minCost = Double.MAX_VALUE;
        int minIndex = -1;
        for (int i = 0; i < list_of_merged_tours.size(); i++) {
            ArrayList<MergedTours> mergedTourses = list_of_merged_tours.get(i);
            double cost = 0;
            for (int j = 0; j < mergedTourses.size(); j++) {
                cost += mergedTourses.get(j).mergedTour.getTourLength();
            }
            if (cost < minCost) {
                minCost = cost;
                minIndex = i;
            }
        }
        return list_of_merged_tours.get(minIndex);
    }

    private int calculate_mdtc(HashMap<String, Edge> mstMap, double percentage) {
        double mstcost = 0;
        for (String key : mstMap.keySet()) {
            Edge edge = mstMap.get(key);
            mstcost += edge.weight;
        }

        int value = (int) Math.round(mstcost * percentage);
        if (value == 0)
            value = 1;
        return value;
    }

    private void ICCPhase1(int k) {
        ArrayList<MergedTours> listOfMergedTours = calculateMergedTourList(tours);

        int m = tours.size();
        while (m > k) {
//            double cost = 0;
//            for (int i = 0; i < tours.size(); i++) {
//                cost += tours.get(i).getTourLength();
//            }
//            System.out.println("Tour Count =" + tours.size() + "\t Cost = " + cost + "\t Expected cost per tour = " + cost / tours.size());
            MergedTours mt = listOfMergedTours.get(0);
            removeTourFromList(mt.tour_ids, tours);
            tours.add(mt.mergedTour);
            listOfMergedTours = calculateMergedTourList(tours);
            m = tours.size();
        }
//        double cost = 0;
//        for (int i = 0; i < tours.size(); i++) {
//            cost += tours.get(i).getTourLength();
//        }
//        System.out.println("Tour Count =" + tours.size() + "\t Cost = " + cost + "\t Expected cost per tour = " + cost / tours.size());
//        return tours;
    }

    private ArrayList<ArrayList<ArrayList<Tour>>> exponantialTourMerge(ArrayList<Tour> list, int M) {
//        System.out.println(list.size()+"  "+M);
        ArrayList<ArrayList<ArrayList<Tour>>> list_of_results = new ArrayList<ArrayList<ArrayList<Tour>>>();
        ArrayList<ArrayList<Tour>> list_of_groups1 = new ArrayList<ArrayList<Tour>>();
        if (M == 1) {
            list_of_groups1.add(list);
            list_of_results.add(list_of_groups1);
            return list_of_results;
        } else if (list.size() == M) {
            for (int i = 0; i < list.size(); i++) {
                Tour tour = list.get(i);
                ArrayList<Tour> list_of_subsets = new ArrayList<Tour>();
                list_of_subsets.add(tour);

                list_of_groups1.add(list_of_subsets);
            }
            list_of_results.add(list_of_groups1);
            return list_of_results;

        } else {
//            for (int i = 0; i < list.size(); i++) {
            Tour t = list.get(list.size() - 1);
            ArrayList<Tour> list_tmp = new ArrayList<Tour>(list);
            list_tmp.remove(t);
            ArrayList<ArrayList<ArrayList<Tour>>> tmpList2 = exponantialTourMerge(list_tmp, M);
            ArrayList<ArrayList<ArrayList<Tour>>> tmpList1 = exponantialTourMerge(list_tmp, M - 1);

            for (int j = 0; j < tmpList1.size(); j++) {
                ArrayList<ArrayList<Tour>> list_of_groups = tmpList1.get(j);
                ArrayList<ArrayList<Tour>> result = new ArrayList<ArrayList<Tour>>(list_of_groups);
                ArrayList<Tour> t_tour = new ArrayList<Tour>();
                t_tour.add(t);
                result.add(t_tour);
                list_of_results.add(result);
            }

            for (int j = 0; j < tmpList2.size(); j++) {
                ArrayList<ArrayList<Tour>> list_of_groups = tmpList2.get(j);
                for (int k = 0; k < list_of_groups.size(); k++) {
                    ArrayList<Tour> group = list_of_groups.get(k);
                    ArrayList<ArrayList<Tour>> list_of_groups_diff = new ArrayList<ArrayList<Tour>>(list_of_groups);
                    list_of_groups_diff.remove(group);

                    ArrayList<Tour> groupunion = new ArrayList<Tour>(group);
                    group.add(t);
                    ArrayList<ArrayList<Tour>> gu = new ArrayList<ArrayList<Tour>>();
                    gu.add(groupunion);
                    gu.addAll(list_of_groups_diff);
                    list_of_results.add(gu);

                }
            }
//            }

            return list_of_results;
        }
    }

    private void removeTourFromList(ArrayList<Integer> tour_ids, ArrayList<Tour> tours) {
        for (int i = 0; i < tour_ids.size(); i++) {
            int id = tour_ids.get(i);
            int index = -1;
            for (int j = 0; j < tours.size(); j++) {
                Tour tour = tours.get(j);
                if (tour.id == id) {
                    index = j;
                }
            }
            if (index != -1) {
                Tour tour = tours.get(index);
                boolean stop = true;
                for (int j = 0; j < tour.tobeCollected.size(); j++) {
                    Gateway gateway = tour.tobeCollected.get(j);
                    stop = (gateway.getID() == 2 || gateway.getID() == 8 || gateway.getID() == 1) && stop;
                }
                if (stop) {
                    System.out.println();
                }
                tours.remove(index);
            }
        }
    }

    private int findMaxTourid() {
        int max = -1;
        int maxIndex = -1;
        for (int i = 0; i < tours.size(); i++) {
            Tour tour = tours.get(i);
            if (tour.id > max) {
                max = tour.id;
                maxIndex = i;
            }
        }
        return tours.get(maxIndex).id;
    }

    private ArrayList<MergedTours> calculateMergedTourList(ArrayList<Tour> tours) {
        Tour[][] tour_merge = new Tour[tours.size()][tours.size()];
        ArrayList<MergedTours> listOfMergedTours = new ArrayList<MergedTours>();
        int counter = findMaxTourid() + 1;
        for (int i = 0; i < tours.size(); i++) {
            Tour t1 = tours.get(i);
            for (int j = 0; j < tours.size(); j++) {
                Tour t2 = tours.get(j);
                if (i == j) {
                    tour_merge[i][j] = null;
                } else if (tour_merge[i][j] == null) {
                    tour_merge[i][j] = Tour.merge(t1, t2, counter++);

                    tour_merge[j][i] = tour_merge[i][j];
                    listOfMergedTours.add(new MergedTours(tour_merge[i][j], t1.id, t2.id));
                    if (tour_merge[i][j].getTourLength() < 0 || tour_merge[i][j].getTourLength() > 1000000) {
                        System.out.println("tour error t1=" + t1.toString() + " \t t2=" + t2.toString());
                    }
                }
            }
        }
        Collections.sort(listOfMergedTours);
        return listOfMergedTours;
    }

    private class MergedTours implements Comparable {
        ArrayList<Integer> tour_ids = new ArrayList<Integer>();
        Tour mergedTour;

        private MergedTours(Tour t, int... ids) {
            for (int id : ids) {
                tour_ids.add(id);
            }
            mergedTour = t;
        }

        private MergedTours(Tour t) {
            mergedTour = t;
        }

        public void setTour_ids(ArrayList<Integer> tour_ids) {
            this.tour_ids = tour_ids;
        }

        public int compareTo(Object o) {
            MergedTours mt = (MergedTours) o;
            double tourLength = mergedTour.getTourLength();
            double aTourLength = mt.mergedTour.getTourLength();
            if (tourLength < aTourLength) {
                return -1;
            } else if (tourLength == aTourLength) {
                return 0;
            } else {
                return 1;
            }
        }

        public String toString() {
            return mergedTour.toString();
        }
    }


    private class RN_MST {
        ArrayList<Gateway> relayList;
        HashMap<String, Edge> map;

        private RN_MST(ArrayList<Gateway> relayList, HashMap<String, Edge> map) {
            this.relayList = relayList;
            this.map = map;
        }
    }

    private int findMstEdgeMapCost(HashMap<String, Edge> mstedges) {
        Iterator<String> iter = mstedges.keySet().iterator();
        int weight = 0;
        while (iter.hasNext()) {
            String next = iter.next();
            Edge e = mstedges.get(next);
            weight += e.weight;
        }
        return weight;
    }

    private ArrayList<ArrayList<TriangleVertex>> findMaximumWeightedIndependentFromTAG(ArrayList<TriangleVertex> tag, boolean newversion) {
        if (newversion) {
            for (int i = 0; i < tag.size(); i++) {
                TriangleVertex u = tag.get(i);
                for (int j = 0; j < tag.size(); j++) {
                    if (i != j) {
                        TriangleVertex v = tag.get(j);
                        if (u.mstCardinality(v) == 1) {
                            u.addNeighbor(v);
                        } else if (u.mstCardinality(v) == 2) {
                            System.out.println("mst cardinality 2");
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < tag.size(); i++) {
                TriangleVertex u = tag.get(i);
                for (int j = 0; j < tag.size(); j++) {
                    if (i != j) {
                        TriangleVertex v = tag.get(j);
                        if (u.node.intersectionCardinality(v.node) == 2) {
                            u.addNeighbor(v);
                        }
                    }
                }
            }
        }
        for (int i = 0; i < tag.size(); i++) {
            tag.get(i).id = i;
        }


        //start dynamic programing

        ArrayList<ArrayList<TriangleVertex>> forest = TV_DFS(tag);
        ArrayList<TriangleVertex> iset = new ArrayList<TriangleVertex>();
        ArrayList<ArrayList<ArrayList<TriangleVertex>>> alternatives = new ArrayList<ArrayList<ArrayList<TriangleVertex>>>();
        for (int i = 0; i < forest.size(); i++) {
            ArrayList<TriangleVertex> dft = forest.get(i);
            int[] Min = new int[dft.size()];
            int[] Mout = new int[dft.size()];
            TriangleVertex root = null;
            for (int j = 0; j < dft.size(); j++) {
                dft.get(j).id = j;
                if (dft.get(j).isRoot) {
                    root = dft.get(j);
                }
            }
            if (root != null) {
                postOrder(root, Min, Mout);
                ArrayList<ArrayList<TriangleVertex>> alternative = new ArrayList<ArrayList<TriangleVertex>>();
                ArrayList<ArrayList<TriangleVertex>> tmpalternative = new ArrayList<ArrayList<TriangleVertex>>();
                ArrayList<TriangleVertex> solution = new ArrayList<TriangleVertex>();
                alternative.add(solution);
                findDPSolution(root, Min, Mout, 0, 0, alternative);
                alternatives.add(alternative);

//                tmpalternative.addAll(alternative);
//                ArrayList<TriangleVertex> bestSet = alternative.get(processAlternativeSets(tmpalternative, tag));
//                iset.addAll(bestSet);
//                System.out.println();
            }
        }
        while (alternatives.size() > 1) {
            ArrayList<ArrayList<TriangleVertex>> set1 = alternatives.get(0);
            ArrayList<ArrayList<TriangleVertex>> set2 = alternatives.get(1);
            ArrayList<ArrayList<TriangleVertex>> setProduct = cartesian(set1, set2);
            alternatives.remove(0);
            alternatives.remove(0);
            alternatives.add(setProduct);
        }
        if (alternatives.isEmpty()) {
            return new ArrayList<ArrayList<TriangleVertex>>();
        } else {
            return alternatives.get(0);
        }
    }

    private ArrayList<ArrayList<TriangleVertex>> cartesian(ArrayList<ArrayList<TriangleVertex>> list1, ArrayList<ArrayList<TriangleVertex>> list2) {
        ArrayList<ArrayList<TriangleVertex>> result = new ArrayList<ArrayList<TriangleVertex>>();
        for (int i = 0; i < list1.size(); i++) {
            ArrayList<TriangleVertex> set1 = list1.get(i);
            for (int j = 0; j < list2.size(); j++) {
                ArrayList<TriangleVertex> set2 = list2.get(j);
                ArrayList<TriangleVertex> all = new ArrayList<TriangleVertex>(set1);
                all.addAll(set2);
                result.add(all);
            }
        }
        return result;
    }

    private RN_MST optimizeMstEdgeMap(ArrayList<Gateway> RNList, HashMap<String, Edge> mstEdgeMap) {
        ArrayList<Edge> edges = new ArrayList<Edge>(mstEdgeMap.values());
        ArrayList<TriangleVertex> tag = new ArrayList<TriangleVertex>();
        int idCounter = 0;
        for (int i = 0; i < edges.size() - 1; i++) {
            Edge ei = edges.get(i);
            for (int j = i + 1; j < edges.size(); j++) {
                Edge ej = edges.get(j);
                Triangle t = null;
                if (ei.u == ej.u) {
                    t = new Triangle(ei.v, ei.u, ej.v);
                } else if (ei.u == ej.v) {
                    t = new Triangle(ei.v, ei.u, ej.u);
                } else if (ei.v == ej.u) {
                    t = new Triangle(ei.u, ei.v, ej.v);
                } else if (ei.v == ej.v) {
                    t = new Triangle(ei.u, ei.v, ej.u);
                }
                if (t != null && t.p_weight > t.f_weight) {
                    TriangleVertex vertex = new TriangleVertex(t, idCounter++);
                    tag.add(vertex);
                }
            }
        }
        //new version
        if (tag.isEmpty()) {
            return new RN_MST(RNList, mstEdgeMap);
        }
        ArrayList<ArrayList<TriangleVertex>> isets = findMaximumWeightedIndependentFromTAG(tag, false);
        int[] weights = new int[isets.size()];
        ArrayList<ArrayList<Gateway>> alternativeRelays = new ArrayList<ArrayList<Gateway>>();
        ArrayList<HashMap<String, Edge>> alternativeMstMap = new ArrayList<HashMap<String, Edge>>();
        for (int index = 0; index < isets.size(); index++) {
            ArrayList<TriangleVertex> iset = isets.get(index);
            ArrayList<Gateway> actorList = new ArrayList<Gateway>(ActorsArray);
            ArrayList<Gateway> relayList = new ArrayList<Gateway>(RNList);
            HashMap<String, Edge> mstEdgeMapClone = new HashMap<String, Edge>();
            Iterator<String> iter = mstEdgeMap.keySet().iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                mstEdgeMapClone.put(key, mstEdgeMap.get(key));
            }


            for (int i = 0; i < iset.size(); i++) {
                Triangle triangle = iset.get(i).node;

                Edge e12 = mstEdgeMapClone.get(new Edge(triangle.s1, triangle.s2).key());
                Edge e13 = mstEdgeMapClone.get(new Edge(triangle.s1, triangle.s3).key());
                Edge e23 = mstEdgeMapClone.get(new Edge(triangle.s2, triangle.s3).key());
                if (e12 == null && e13 != null && e23 != null) {
                    mstEdgeMapClone.remove(e13.key());
                    mstEdgeMapClone.remove(e23.key());
                } else if (e12 != null && e13 == null && e23 != null) {
                    mstEdgeMapClone.remove(e12.key());
                    mstEdgeMapClone.remove(e23.key());
                } else if (e12 != null && e13 != null && e23 == null) {
                    mstEdgeMapClone.remove(e12.key());
                    mstEdgeMapClone.remove(e13.key());
                }
                Gateway rn = new Gateway(actorList.size() + relayList.size());
                rn.setLocation(triangle.fermatPoint);
                rn.isRelay = true;
                relayList.add(rn);
                Edge e1 = new Edge(triangle.s1, rn);
                Edge e2 = new Edge(triangle.s2, rn);
                Edge e3 = new Edge(triangle.s3, rn);
                mstEdgeMapClone.put(e1.key(), e1);
                mstEdgeMapClone.put(e2.key(), e2);
                mstEdgeMapClone.put(e3.key(), e3);
            }
            weights[index] = relayList.size() + findMstEdgeMapCost(mstEdgeMapClone);
            alternativeRelays.add(index, relayList);
            alternativeMstMap.add(index, mstEdgeMapClone);
        }
        int minIndex = -1;
        int minWeight = Integer.MAX_VALUE;
        for (int i = 0; i < weights.length; i++) {
            int weight = weights[i];
            if (weight < minWeight) {
                minIndex = i;
                minWeight = weight;
            }
        }
        RNList = new ArrayList<Gateway>(alternativeRelays.get(minIndex));
        mstEdgeMap = alternativeMstMap.get(minIndex);
        RN_MST result = new RN_MST(RNList, mstEdgeMap);
        return result;

        //oldversion
        /*ArrayList<TriangleVertex> iset = findMaximumWeightedIndependentFromTAG(tag);
        for (int i = 0; i < iset.size(); i++) {
            Triangle triangle = iset.get(i).node;

            Edge e12 = mstEdgeMap.get(new Edge(triangle.s1, triangle.s2).key());
            Edge e13 = mstEdgeMap.get(new Edge(triangle.s1, triangle.s3).key());
            Edge e23 = mstEdgeMap.get(new Edge(triangle.s2, triangle.s3).key());
            if (e12 == null && e13 != null && e23 != null) {
                mstEdgeMap.remove(e13.key());
                mstEdgeMap.remove(e23.key());
            } else if (e12 != null && e13 == null && e23 != null) {
                mstEdgeMap.remove(e12.key());
                mstEdgeMap.remove(e23.key());
            } else if (e12 != null && e13 != null && e23 == null) {
                mstEdgeMap.remove(e12.key());
                mstEdgeMap.remove(e13.key());
            }
            Gateway rn = new Gateway(ActorsArray.size() + RelayNodeArray.size());
            rn.setLocation(triangle.fermatPoint);
            rn.isRelay = true;
            RelayNodeArray.add(rn);
            Edge e1 = new Edge(triangle.s1, rn);
            Edge e2 = new Edge(triangle.s2, rn);
            Edge e3 = new Edge(triangle.s3, rn);
            mstEdgeMap.put(e1.key(), e1);
            mstEdgeMap.put(e2.key(), e2);
            mstEdgeMap.put(e3.key(), e3);
        }*/
//        System.out.println();
    }

    private void steinerizeMstEdges(HashMap<String, Edge> mstEdgeMap) {
        Iterator<String> iter = mstEdgeMap.keySet().iterator();
        while (iter.hasNext()) {
            Edge e = mstEdgeMap.get(iter.next());
            if (e.weight > 0) {
                NetworkUtils.fillGap(e.u, e.v, true, ActorsArray, RelayNodeArray);
            }
        }
    }

    private RN_MST federateCC(ArrayList<Triangle> DelaunayT, ArrayList<Gateway> relayList, HashMap<String, Edge> mstEdgeMap) {

        ArrayList<Triangle> DT = new ArrayList<Triangle>(DelaunayT);
//        for (int i = DT.size() - 1; i >= 0; i--) {
//            Triangle triangle = DT.get(i);
//            if (triangle.p_weight - triangle.f_weight <= 0) {
//                DT.remove(i);
//            }
//        }
        Collections.sort(DT);
        for (int i = 0; i < DT.size(); i++) {
            Triangle triangle = DT.get(i);
            int c1 = triangle.s1.ccid;
            int c2 = triangle.s2.ccid;
            int c3 = triangle.s3.ccid;

            if (c1 != c2 && c1 != c3 && c2 != c3) {
                ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
                allNodes.addAll(relayList);
                ArrayList<Gateway> path12 = FindTreePath(triangle.s1, triangle.s2, allNodes, mstEdgeMap);
                ArrayList<Gateway> path13 = FindTreePath(triangle.s1, triangle.s3, allNodes, mstEdgeMap);

                //new version  begin
                if (path12 == null || path13 == null)
                    return null;
                // new version end

                ArrayList<Gateway> path12_prime = new ArrayList<Gateway>(path12);
                path12_prime.remove(path12_prime.size() - 1);
                path12_prime.remove(0);
                ArrayList<Gateway> path13_prime = new ArrayList<Gateway>(path13);
                path13_prime.remove(path13_prime.size() - 1);
                path13_prime.remove(0);

                if (!path12_prime.isEmpty() && !path13_prime.isEmpty()) {
                    if (/*containsAll(path12, path13) ||*/ containsAll(path12_prime, path13_prime)) {
                        path12 = FindTreePath(triangle.s3, triangle.s1, allNodes, mstEdgeMap);
                        path13 = FindTreePath(triangle.s3, triangle.s2, allNodes, mstEdgeMap);
                    } else if (/*containsAll(path13, path12) ||*/ containsAll(path13_prime, path12_prime)) {
                        path12 = FindTreePath(triangle.s2, triangle.s1, allNodes, mstEdgeMap);
                        path13 = FindTreePath(triangle.s2, triangle.s3, allNodes, mstEdgeMap);
                    }
                } else {
                    if (containsAll(path12, path13)) {
                        path12 = FindTreePath(triangle.s3, triangle.s1, allNodes, mstEdgeMap);
                        path13 = FindTreePath(triangle.s3, triangle.s2, allNodes, mstEdgeMap);
                    } else if (containsAll(path13, path12)) {
                        path12 = FindTreePath(triangle.s2, triangle.s1, allNodes, mstEdgeMap);
                        path13 = FindTreePath(triangle.s2, triangle.s3, allNodes, mstEdgeMap);
                    }
                }


                Edge[] edges12 = findTwoLargestInterCCMstEdge(path12);
                Edge[] edges13 = findTwoLargestInterCCMstEdge(path13);
                Edge e1 = null, e2 = null;

                if (edges12[0] != null && edges13[0] != null && !edges12[0].equals(edges13[0])) {
                    e1 = edges12[0];
                    e2 = edges13[0];
                } else if (edges12[0] != null && edges13[0] != null && edges12[0].equals(edges13[0])) {
                    if (edges12[0].weight + edges13[1].weight > edges12[1].weight + edges13[0].weight) {
                        e1 = edges12[0];
                        e2 = edges13[1];
                    } else {
                        e1 = edges12[1];
                        e2 = edges13[0];
                    }
                }

                if (e1 != null && e2 != null) {
                    int mstCost = e1.weight + e2.weight;
                    int tCost = triangle.f_weight;
                    if (mstCost > tCost) {
                        federateThreeCCs(/*ccMST,*/ mstEdgeMap, triangle, c1, c2, c3, e1, e2, relayList);
                    }
                }
            } else if ((c1 == c2 && c1 != c3)) {
                federateTwoCCs(/*ccMST, */mstEdgeMap, triangle, c1, c3, triangle.s1, triangle.s2, relayList);
            } else if (c1 == c3 && c1 != c2) {
                federateTwoCCs(/*ccMST, */mstEdgeMap, triangle, c1, c2, triangle.s1, triangle.s3, relayList);
            } else if (c2 == c3 && c1 != c3) {
                federateTwoCCs(/*ccMST, */mstEdgeMap, triangle, c1, c3, triangle.s2, triangle.s3, relayList);
            } else if (c1 == c2 && c2 == c3) {
                ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
                allNodes.addAll(relayList);
                ArrayList<Gateway> path12 = FindTreePath(triangle.s1, triangle.s2, allNodes, mstEdgeMap);
                ArrayList<Gateway> path13 = FindTreePath(triangle.s1, triangle.s3, allNodes, mstEdgeMap);

                if (containsAll(path12, path13)) {
                    path12 = FindTreePath(triangle.s3, triangle.s1, allNodes, mstEdgeMap);
                    path13 = FindTreePath(triangle.s3, triangle.s2, allNodes, mstEdgeMap);
                } else if (containsAll(path13, path12)) {
                    path12 = FindTreePath(triangle.s2, triangle.s1, allNodes, mstEdgeMap);
                    path13 = FindTreePath(triangle.s2, triangle.s3, allNodes, mstEdgeMap);
                }

                ArrayList<Edge> e_path12 = new ArrayList<Edge>();
                ArrayList<Edge> e_path13 = new ArrayList<Edge>();

                for (int j = 0; j < path12.size() - 1; j++) {
                    int k = j + 1;
                    Edge e = new Edge(path12.get(j), path12.get(k));
                    e_path12.add(e);
                }
                for (int j = 0; j < path13.size() - 1; j++) {
                    int k = j + 1;
                    Edge e = new Edge(path13.get(j), path13.get(k));
                    e_path13.add(e);
                }

                Collections.sort(e_path12);
                Collections.sort(e_path13);
                if (!e_path12.isEmpty() && !e_path13.isEmpty()) {
                    Edge e1 = e_path12.get(e_path12.size() - 1);
                    Edge e2 = e_path13.get(e_path13.size() - 1);
                    if (e1.weight + e2.weight > triangle.f_weight) {
                        mstEdgeMap.remove(e1.key());
                        mstEdgeMap.remove(e2.key());
                        Gateway rn = new Gateway(ActorsArray.size() + relayList.size());
                        rn.setLocation(triangle.fermatPoint);
                        rn.isRelay = true;
                        rn.ccid = c1;
                        relayList.add(rn);
                        Edge edge1 = new Edge(triangle.s1, rn);
                        Edge edge2 = new Edge(triangle.s2, rn);
                        Edge edge3 = new Edge(triangle.s3, rn);
                        mstEdgeMap.put(edge1.key(), edge1);
                        mstEdgeMap.put(edge2.key(), edge2);
                        mstEdgeMap.put(edge3.key(), edge3);
                    }
                }
            }
        }

        RN_MST result = new RN_MST(relayList, mstEdgeMap);
        return result;
    }

    /**
     * returns true if list1 contains all the elements in list 2
     *
     * @param list1
     * @param list2
     * @return
     */
    private boolean containsAll(ArrayList<Gateway> list1, ArrayList<Gateway> list2) {
        int[] check = new int[list2.size()];
        for (int i = 0; i < list2.size(); i++) {
            Gateway gateway = list2.get(i);
            for (int j = 0; j < list1.size(); j++) {
                Gateway g1 = list1.get(j);
                if (gateway.getID() == g1.getID()) {
                    check[i] = 1;
                }
            }
        }
        int mul = 1;
        for (int i = 0; i < check.length; i++) {
            mul *= check[i];
        }
        return mul == 1;
    }


    private void federateCC2(ArrayList<Triangle> DelaunayT, Edge[][] ccMST, HashMap<String, Edge> mstEdgeMap) {
        ArrayList<Triangle> DT = new ArrayList<Triangle>(DelaunayT);
        for (int i = DT.size() - 1; i >= 0; i--) {
            Triangle triangle = DT.get(i);
            if (triangle.p_weight - triangle.f_weight <= 0) {
                DT.remove(i);
            }
        }
        Collections.sort(DT);
        for (int i = 0; i < DT.size(); i++) {
            Triangle triangle = DT.get(i);
            int c1 = triangle.s1.ccid;
            int c2 = triangle.s2.ccid;
            int c3 = triangle.s3.ccid;

            if (c1 != c2 && c1 != c3 && c2 != c3) {
                Edge e12 = ccMST[c1 - 1][c2 - 1];
                Edge e13 = ccMST[c1 - 1][c3 - 1];
                Edge e23 = ccMST[c2 - 1][c3 - 1];
                if (e12 == null && e13 != null && e23 != null) {
                    int mstCost = e13.weight + e23.weight;
                    int tCost = triangle.f_weight;
                    if (tCost < mstCost) {
                        federateThreeCCs(/*ccMST,*/ mstEdgeMap, triangle, c1, c2, c3, e13, e23, RelayNodeArray);
                    }
                }
                if (e12 != null && e13 == null && e23 != null) {
                    int mstCost = e12.weight + e23.weight;
                    int tCost = triangle.f_weight;
                    if (tCost < mstCost) {
                        federateThreeCCs(/*ccMST,*/ mstEdgeMap, triangle, c1, c2, c3, e12, e23, RelayNodeArray);
                    }
                }
                if (e12 != null && e13 != null && e23 == null) {
                    int mstCost = e12.weight + e13.weight;
                    int tCost = triangle.f_weight;
                    if (tCost < mstCost) {
                        federateThreeCCs(/*ccMST, */mstEdgeMap, triangle, c1, c2, c3, e12, e13, RelayNodeArray);
                    }
                }
            } else if ((c1 == c2 && c1 != c3)) {
                federateTwoCCs2(ccMST, mstEdgeMap, triangle, c1, c3, triangle.s1, triangle.s2);
            } else if (c1 == c3 && c1 != c2) {
                federateTwoCCs2(ccMST, mstEdgeMap, triangle, c1, c2, triangle.s1, triangle.s3);
            } else if (c2 == c3 && c1 != c3) {
                federateTwoCCs2(ccMST, mstEdgeMap, triangle, c1, c3, triangle.s2, triangle.s3);
            }

        }
    }

    private void federateThreeCCs(HashMap<String, Edge> mstEdgeMap, Triangle triangle, int c1, int c2, int c3, Edge edge1, Edge edge2, ArrayList<Gateway> relayList) {
        int mstCost = edge1.weight + edge2.weight;
        int tCost = triangle.f_weight;
        if (tCost < mstCost) {
            ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
            allNodes.addAll(relayList);
            mstEdgeMap.remove(edge1.key());
            mstEdgeMap.remove(edge2.key());

            Gateway rn = new Gateway(ActorsArray.size() + relayList.size());
            rn.setLocation(triangle.fermatPoint);
            rn.ccid = Math.min(c1, Math.min(c2, c3));
            rn.isRelay = true;
            relayList.add(rn);

            Edge e1 = new Edge(triangle.s1, rn);
            Edge e2 = new Edge(triangle.s2, rn);
            Edge e3 = new Edge(triangle.s3, rn);
            mstEdgeMap.put(e1.key(), e1);
            mstEdgeMap.put(e2.key(), e2);
            mstEdgeMap.put(e3.key(), e3);
            mergeCC(c1, c2, c3, allNodes);
        }
    }


    /**
     * returns the two largest inter-connected component mst edge along the path specified
     *
     * @param path
     */
    private Edge[] findTwoLargestInterCCMstEdge(ArrayList<Gateway> path) {
        Edge[] result = new Edge[2];
        Edge F12 = null, S12 = null;
        int FW12 = -1, SW12 = -1;

        for (int j = 0; j < path.size() - 1; j++) {
            int k = j + 1;
            Gateway s = path.get(j);
            Gateway t = path.get(k);
            Edge e = new Edge(s, t);
//            if (e.u.ccid != e.v.ccid) {
            if (e.weight > FW12) {
                SW12 = FW12;
                FW12 = e.weight;
                S12 = F12;
                F12 = e;
            } else if (e.weight < FW12 && e.weight > SW12) {
                S12 = e;
                SW12 = e.weight;
            }
        }
//        }
        result[0] = F12;
        result[1] = S12;
        return result;
    }

    private ArrayList<Gateway> FindTreePath(Gateway u, Gateway v, ArrayList<Gateway> listOfNodes, HashMap<String, Edge> mstEdgeMap) {
        if (u == null || v == null) {
            return null;
        }
        ArrayList<Edge> mstEdges = new ArrayList<Edge>(mstEdgeMap.values());
        ArrayList<Gateway> tmplistOfNodes = new ArrayList<Gateway>();
        for (int i = 0; i < listOfNodes.size(); i++) {
            Gateway gateway = listOfNodes.get(i);
            Gateway clone = new Gateway(gateway.getID());
            clone.setX(gateway.getX());
            clone.setY(gateway.getY());
            clone.isRelay = gateway.isRelay;
            clone.ccid = gateway.ccid;
            tmplistOfNodes.add(clone);
        }
        Gateway source = null, destination = null;
        for (int i = 0; i < mstEdges.size(); i++) {
            Edge edge = mstEdges.get(i);
            Gateway a = null, b = null;
            for (int j = 0; j < tmplistOfNodes.size(); j++) {
                Gateway gateway = tmplistOfNodes.get(j);
                if (edge.u.getID() == gateway.getID()) {
                    a = gateway;
                }
                if (edge.v.getID() == gateway.getID()) {
                    b = gateway;
                }
            }
            a.addNeighborList(b);
            b.addNeighborList(a);
            if (a.getID() == u.getID()) {
                source = a;
            }
            if (a.getID() == v.getID()) {
                destination = a;
            }
            if (b.getID() == u.getID()) {
                source = b;
            }
            if (b.getID() == v.getID()) {
                destination = b;
            }
        }
        if (source == null || destination == null) {
            return null;
        }
        LinkedList<Gateway> q = new LinkedList<Gateway>();
        q.add(source);
        ArrayList<Gateway> path = new ArrayList<Gateway>();
        source.bfsLabel = 1;
        while (!q.isEmpty()) {
            Gateway g = q.removeFirst();
            for (int i = 0; i < g.getNeighborList().size(); i++) {
                Gateway n = g.getNeighborList().get(i);
                if (n.bfsLabel == 0) {
                    n.bfsLabel = 1;
                    n.bfsLevel = g.bfsLevel + 1;
                    q.addLast(n);
                }
            }
        }
        Gateway current = destination;

        while (current != source) {
            path.add(current);
            for (int i = 0; i < current.getNeighborList().size(); i++) {
                Gateway n = current.getNeighborList().get(i);
                if (n.bfsLevel == current.bfsLevel - 1) {
                    current = n;
                    break;
                }
            }
        }
        path.add(source);
        return path;
    }

    /**
     * @param mstEdgeMap all edges in mst
     * @param triangle   current triangle
     * @param c1         connected component id 1
     * @param c2         connected component id 2 (c1 is not equal to c2)
     * @param s1         gateway
     * @param s2         gateway (s1 and s2 are in same CC)
     */
    private void federateTwoCCs(/*Edge[][] ccMST, */HashMap<String, Edge> mstEdgeMap, Triangle triangle, int c1, int c2, Gateway s1, Gateway s2, ArrayList<Gateway> relayList) {
        ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
        allNodes.addAll(relayList);
        Edge largestMstEdge = findLargestMstEdgeBetween(s1, s2, allNodes, mstEdgeMap);
        Edge e_in = mstEdgeMap.get(largestMstEdge.key());
        if (e_in == null) {
            e_in = mstEdgeMap.get(largestMstEdge.reversekey());
        }


        Gateway g1 = null, g2 = null;
        if (triangle.s1.ccid != s1.ccid) {
            g1 = triangle.s1;
            g2 = s1;
        }
        if (triangle.s2.ccid != s1.ccid) {
            g1 = triangle.s2;
            g2 = s1;
        }
        if (triangle.s3.ccid != s1.ccid) {
            g1 = triangle.s3;
            g2 = s1;
        }
        Edge[] twoLargestEdges = findTwoLargestInterCCMstEdge(FindTreePath(g1, g2, allNodes, mstEdgeMap));
        Edge e_out = twoLargestEdges[0];
        if (e_in.key().equalsIgnoreCase(e_out.key()) || e_in.reversekey().equalsIgnoreCase(e_out.key())) {
            e_out = twoLargestEdges[1];
        }
        if ((e_out != null) && (e_out.weight + e_in.weight > triangle.f_weight)) {
            Gateway rn = new Gateway(ActorsArray.size() + relayList.size());
            rn.setLocation(triangle.fermatPoint);
            rn.isRelay = true;
            relayList.add(rn);
            rn.setCcid(Math.min(c1, c2));
            Edge e1 = new Edge(triangle.s1, rn);
            Edge e2 = new Edge(triangle.s2, rn);
            Edge e3 = new Edge(triangle.s3, rn);
            mstEdgeMap.remove(e_in.key());
            mstEdgeMap.remove(e_out.key());
            mstEdgeMap.put(e1.key(), e1);
            mstEdgeMap.put(e2.key(), e2);
            mstEdgeMap.put(e3.key(), e3);
            mergeCC(c1, c2, allNodes);
        }
    }

    /**
     * @param ccMST      Connected component minimum spanning tree (if there is an mst edge in between cc's i and j then ccMST[i-1][j-1]=1)
     * @param mstEdgeMap all edges in mst
     * @param triangle   current triangle
     * @param c1         connected component id 1
     * @param c2         connected component id 2 (c1 is not equal to c2)
     * @param s1         gateway
     * @param s2         gateway (s1 and s2 are in same CC)
     * @deprecated old version
     */
    private void federateTwoCCs2(Edge[][] ccMST, HashMap<String, Edge> mstEdgeMap, Triangle triangle, int c1, int c2, Gateway s1, Gateway s2) {
        ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
        allNodes.addAll(RelayNodeArray);
        Edge e = mstEdgeMap.get(findLargestMstEdgeBetween(s1, s2, allNodes, mstEdgeMap).key());
        if ((ccMST[c1 - 1][c2 - 1] != null) && (ccMST[c1 - 1][c2 - 1].weight + e.weight > triangle.f_weight)) {
            Gateway rn = new Gateway(ActorsArray.size() + RelayNodeArray.size());
            rn.setLocation(triangle.fermatPoint);
            rn.isRelay = true;
            RelayNodeArray.add(rn);

            Edge e1 = new Edge(triangle.s1, rn);
            Edge e2 = new Edge(triangle.s2, rn);
            Edge e3 = new Edge(triangle.s3, rn);
            mstEdgeMap.remove(e.key());
            mstEdgeMap.remove(ccMST[c1 - 1][c2 - 1].key());
            mstEdgeMap.put(e1.key(), e1);
            mstEdgeMap.put(e2.key(), e2);
            mstEdgeMap.put(e3.key(), e3);
//            mergeCC(c1, c2, ccMST, allNodes);
        }
    }

    private void mergeCC(int c1, int c2, ArrayList<Gateway> NodeList) {
        if (c1 != -1 && c2 != -1) {
            int min = Math.min(c1, c2);
            for (int i = 0; i < NodeList.size(); i++) {
                Gateway gateway = NodeList.get(i);
                if (gateway.ccid == c1 || gateway.ccid == c2) {
                    gateway.setCcid(min);
                }
            }
        }
    }

    private void mergeCC(int c1, int c2, int c3, ArrayList<Gateway> NodeList) {
        int max = Math.max(c1, Math.max(c2, c3));
        int min;
        if (c1 == max) {
            min = Math.min(c2, c3);
            mergeCC(c2, c3, NodeList);
        } else if (c2 == max) {
            min = Math.min(c1, c3);
            mergeCC(c1, c3, NodeList);
        } else {
            min = Math.min(c1, c2);
            mergeCC(c1, c2, NodeList);
        }
        mergeCC(min, max, NodeList);
    }


    private Edge findLargestMstEdgeBetween(Gateway u, Gateway v, ArrayList<Gateway> listOfNodes, HashMap<String, Edge> mstEdgeMap) {
        ArrayList<Edge> mstEdges = new ArrayList<Edge>(mstEdgeMap.values());
        ArrayList<Gateway> tmplistOfNodes = new ArrayList<Gateway>();
        for (int i = 0; i < listOfNodes.size(); i++) {
            Gateway gateway = listOfNodes.get(i);
            Gateway clone = new Gateway(gateway.getID());
            clone.setX(gateway.getX());
            clone.setY(gateway.getY());
            clone.isRelay = gateway.isRelay;
            tmplistOfNodes.add(clone);
        }
        Gateway source = null, destination = null;
        for (int i = 0; i < mstEdges.size(); i++) {
            Edge edge = mstEdges.get(i);
            Gateway a = null, b = null;
            for (int j = 0; j < tmplistOfNodes.size(); j++) {
                Gateway gateway = tmplistOfNodes.get(j);
                if (edge.u.getID() == gateway.getID()) {
                    a = gateway;
                }
                if (edge.v.getID() == gateway.getID()) {
                    b = gateway;
                }
            }
            a.addNeighborList(b);
            b.addNeighborList(a);
            if (a.getID() == u.getID()) {
                source = a;
            }
            if (a.getID() == v.getID()) {
                destination = a;
            }
            if (b.getID() == u.getID()) {
                source = b;
            }
            if (b.getID() == v.getID()) {
                destination = b;
            }
        }
        LinkedList<Gateway> q = new LinkedList<Gateway>();
        q.add(source);
        ArrayList<Gateway> path = new ArrayList<Gateway>();
//        System.out.println(u.getID() + "-" + v.getID());
        source.bfsLabel = 1;
        while (!q.isEmpty()) {
            Gateway g = q.removeFirst();
            for (int i = 0; i < g.getNeighborList().size(); i++) {
                Gateway n = g.getNeighborList().get(i);
                if (n.bfsLabel == 0) {
                    n.bfsLabel = 1;
                    n.bfsLevel = g.bfsLevel + 1;
                    q.addLast(n);
                }
            }
        }
        Gateway current = destination;

        while (current != source) {
            path.add(current);
            for (int i = 0; i < current.getNeighborList().size(); i++) {
                Gateway n = current.getNeighborList().get(i);
                if (n.bfsLevel == current.bfsLevel - 1) {
                    current = n;
                    break;
                }
            }
        }
        path.add(source);
        Edge maxEdge = null;
        int maxWeight = -1;
        for (int i = 0; i < path.size() - 1; i++) {
            int j = i + 1;
            Gateway s = path.get(i);
            Gateway t = path.get(j);
            Edge e = new Edge(s, t);
            if (maxWeight < e.weight) {
                maxWeight = e.weight;
                maxEdge = e;
            }
        }
        return maxEdge;
    }

    private HashMap<Integer, ArrayList<Gateway>> deployRNatFermatPoints(ArrayList<TriangleVertex> iset, ArrayList<Gateway> RelayList, HashMap<String, Edge> mstedges, boolean newversion) {
        ArrayList<TriangleVertex> tmp_iset = new ArrayList<TriangleVertex>(iset);
        ArrayList<TriangleVertex> actual_iset = new ArrayList<TriangleVertex>();
        HashMap<Integer, ArrayList<Gateway>> ccMap = new HashMap<Integer, ArrayList<Gateway>>();
        int nextCCID = 1;
        int[] track = new int[tmp_iset.size()];
        for (int i = tmp_iset.size() - 1; i >= 0; i--) {
            if (track[i] == 0) {
                TriangleVertex u = tmp_iset.get(i);
                track[i] = 1;
//            tmp_iset.remove(0);
                TriangleVertex tvu = new TriangleVertex(u.node, u.id);
                tvu.mst1 = u.mst1;
                tvu.mst2 = u.mst2;
                for (int j = tmp_iset.size() - 1; j >= 0; j--) {
                    if (i != j && track[j] == 0) {
                        TriangleVertex v = tmp_iset.get(j);
                        if ((!newversion && u.node.intersectionCardinality(v.node) == 2) || (newversion && u.mstCardinality(v) == 1)) {
                            TriangleVertex tvv = new TriangleVertex(v.node, v.id);
                            tvv.mst1 = v.mst1;
                            tvv.mst2 = v.mst2;
                            tvu.addNeighbor(tvv);
                            track[j] = 1;
                        }
                    }
                }


                actual_iset.add(tvu);
            }
        }

        int counter = ActorsArray.size() + RelayList.size();

        for (int i = 0; i < actual_iset.size(); i++) {
            TriangleVertex tu = actual_iset.get(i);
            Gateway s1 = tu.node.s1;
            Gateway s2 = tu.node.s2;
            Gateway s3 = tu.node.s3;

            int c1 = s1.getCcid();
            int c2 = s2.getCcid();
            int c3 = s3.getCcid();
            if (newversion) {
                mstedges.remove(tu.mst1.key());
                mstedges.remove(tu.mst2.key());
                mstedges.remove(tu.mst1.reversekey());
                mstedges.remove(tu.mst2.reversekey());
            } else {
                Edge s1s2 = mstedges.get(new Edge(s1, s2).key());
                Edge s1s3 = mstedges.get(new Edge(s1, s3).key());
                Edge s2s3 = mstedges.get(new Edge(s2, s3).key());

                if (s1s2 != null) {
                    mstedges.remove(s1s2.key());
                }
                if (s1s3 != null) {
                    mstedges.remove(s1s3.key());
                }
                if (s2s3 != null) {
                    mstedges.remove(s2s3.key());
                }
            }
            Gateway rn = new Gateway(counter++);
            rn.setLocation(tu.node.fermatPoint);

            rn.isRelay = true;
            RelayList.add(rn);

            Edge s1rn = new Edge(s1, rn);
            Edge s2rn = new Edge(s2, rn);
            Edge s3rn = new Edge(s3, rn);

            mstedges.put(s1rn.key(), s1rn);
            mstedges.put(s2rn.key(), s2rn);
            mstedges.put(s3rn.key(), s3rn);

            if (c1 == 0 && c2 == 0 && c3 == 0) {
                ArrayList<Gateway> list = new ArrayList<Gateway>();
                s1.setCcid(nextCCID);
                s2.setCcid(nextCCID);
                s3.setCcid(nextCCID);
                rn.setCcid(nextCCID);
                list.add(s1);
                list.add(s2);
                list.add(s3);
                list.add(rn);
                ccMap.put(nextCCID, list);
                nextCCID++;
            } else {
                int ccid = Math.max(c1, Math.max(c2, c3));
                rn.setCcid(ccid);
                ccMap.get(ccid).add(rn);
                if (c1 != ccid && c1 != 0) {
                    ArrayList<Gateway> list = ccMap.get(c1);
                    if (list != null) {
                        for (int j = 0; j < list.size(); j++) {
                            Gateway gateway = list.get(j);
                            gateway.setCcid(ccid);
                        }

                    } else {
                        list = new ArrayList<Gateway>();
                        s1.setCcid(ccid);
                        list.add(s1);
                    }
                    ccMap.get(ccid).addAll(list);
                } else if (c1 != ccid && c1 == 0) {
                    s1.setCcid(ccid);
                    ccMap.get(ccid).add(s1);
                }
                if (c2 != ccid && c2 != 0) {
                    ArrayList<Gateway> list = ccMap.get(c2);
                    if (list != null) {
                        for (int j = 0; j < list.size(); j++) {
                            Gateway gateway = list.get(j);
                            gateway.setCcid(ccid);
                        }
                    } else {
                        list = new ArrayList<Gateway>();
                        s2.setCcid(ccid);
                        list.add(s2);
                    }
                    ccMap.get(ccid).addAll(list);
                } else if (c2 != ccid && c2 == 0) {
                    s2.setCcid(ccid);
                    ccMap.get(ccid).add(s2);
                }
                if (c3 != ccid && c3 != 0) {
                    ArrayList<Gateway> list = ccMap.get(c3);
                    if (list != null) {
                        for (int j = 0; j < list.size(); j++) {
                            Gateway gateway = list.get(j);
                            gateway.setCcid(ccid);
                        }
                    } else {
                        list = new ArrayList<Gateway>();
                        s3.setCcid(ccid);
                        list.add(s3);
                    }
                    ccMap.get(ccid).addAll(list);

                } else if (c3 != ccid && c3 == 0) {
                    s3.setCcid(ccid);
                    ccMap.get(ccid).add(s3);
                }
            }

        }
        return ccMap;
    }

    private void deployRNatFermatPoints2(ArrayList<TriangleVertex> iset, ArrayList<Gateway> RelayList, HashMap<String, Edge> mstedges) {
        ArrayList<TriangleVertex> tmp_iset = new ArrayList<TriangleVertex>(iset);
        ArrayList<TriangleVertex> actual_iset = new ArrayList<TriangleVertex>();
        int[] track = new int[tmp_iset.size()];
        for (int i = tmp_iset.size() - 1; i >= 0; i--) {
            if (track[i] == 0) {
                TriangleVertex u = tmp_iset.get(i);
                track[i] = 1;
//            tmp_iset.remove(0);
                TriangleVertex tvu = new TriangleVertex(u.node, u.id);
                tvu.mst1 = u.mst1;
                tvu.mst2 = u.mst2;
                for (int j = tmp_iset.size() - 1; j >= 0; j--) {
                    if (i != j && track[j] == 0) {
                        TriangleVertex v = tmp_iset.get(j);
                        if (u.node.intersectionCardinality(v.node) == 2) {
                            TriangleVertex tvv = new TriangleVertex(v.node, v.id);
                            tvv.mst1 = v.mst1;
                            tvv.mst2 = v.mst2;
                            tvu.addNeighbor(tvv);
                            track[j] = 1;
                        }
                    }
                }
                actual_iset.add(tvu);
            }
        }

        int counter = ActorsArray.size() + RelayList.size();

        for (int i = 0; i < actual_iset.size(); i++) {
            TriangleVertex tu = actual_iset.get(i);
            Gateway s1 = tu.node.s1;
            Gateway s2 = tu.node.s2;
            Gateway s3 = tu.node.s3;

            int c1 = s1.getCcid();
            int c2 = s2.getCcid();
            int c3 = s3.getCcid();

            Edge s1s2 = mstedges.get(new Edge(s1, s2).key());
            Edge s1s3 = mstedges.get(new Edge(s1, s3).key());
            Edge s2s3 = mstedges.get(new Edge(s2, s3).key());

            if (s1s2 != null) {
                mstedges.remove(s1s2.key());
            }
            if (s1s3 != null) {
                mstedges.remove(s1s3.key());
            }
            if (s2s3 != null) {
                mstedges.remove(s2s3.key());
            }

            Gateway rn = new Gateway(counter++);
            rn.setLocation(tu.node.fermatPoint);

            rn.isRelay = true;
            RelayList.add(rn);

            Edge s1rn = new Edge(s1, rn);
            Edge s2rn = new Edge(s2, rn);
            Edge s3rn = new Edge(s3, rn);

            mstedges.put(s1rn.key(), s1rn);
            mstedges.put(s2rn.key(), s2rn);
            mstedges.put(s3rn.key(), s3rn);
        }
    }

    private HashMap<String, Edge> buildMSTHashMap(ArrayList<Edge> edges) {
        HashMap<String, Edge> map = new HashMap<String, Edge>();
        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            map.put(edge.key(), edge);
        }
        return map;
    }

    private boolean containsTwoMstEdges(Triangle t, HashMap<String, Edge> map) {
        String key1 = new Edge(t.s1, t.s2).key();
        String key2 = new Edge(t.s1, t.s3).key();
        String key3 = new Edge(t.s2, t.s3).key();

        int count = 0;

        if (map.get(key1) != null) {
            count++;
        }
        if (map.get(key2) != null) {
            count++;
        }
        if (map.get(key3) != null) {
            count++;
        }

        if (count == 3) {
            System.out.println("MST Error Cycle Found: Method Name = ContainsTwoMstEdges");
        }
        return count == 2;


    }

    private int processAlternativeSets(ArrayList<ArrayList<TriangleVertex>> alternative, ArrayList<TriangleVertex> tag) {
        int N = tag.size();
        int[][] adjacencyMatrix = new int[N][N];

        for (int i = 0; i < tag.size(); i++) {
            TriangleVertex u = tag.get(i);
            for (int j = 0; j < u.neighbors.size(); j++) {
                TriangleVertex v = u.neighbors.get(j);
                adjacencyMatrix[u.id][v.id] = 1;
                adjacencyMatrix[v.id][u.id] = 1;
            }
        }

        for (int i = 0; i < alternative.size(); i++) {
            ArrayList<TriangleVertex> iset = alternative.get(i);
            for (int j = iset.size() - 1; j >= 0; j--) {
                int ru = -1;
                TriangleVertex u = iset.get(j);
                for (int k = iset.size() - 1; k >= 0; k--) {
                    int rv = -1;
                    if (j != k) {
                        TriangleVertex v = iset.get(k);
                        if (u.node.intersectionCardinality(v.node) == 2) {
                            if (u.node.getGain() < v.node.getGain()) {
                                ru = j;
                            } else {
                                rv = k;
                            }
                        }
                        if (rv != -1) {
                            iset.remove(rv);

                        }
                    }
                }
                if (ru != -1) {
                    iset.remove(ru);
                }
            }
        }
        int[] weights = new int[alternative.size()];
        int max = -1, maxIndex = 0;
        for (int i = 0; i < alternative.size(); i++) {
            ArrayList<TriangleVertex> iset = alternative.get(i);
            for (int j = 0; j < iset.size(); j++) {
                TriangleVertex u = iset.get(j);
                weights[i] += u.node.getGain();
            }
            if (max < weights[i]) {
                max = weights[i];
                maxIndex = i;
            }
        }
        return maxIndex;


//        ArrayList<ArrayList<Set<TriangleVertex>>> allSetGroups = new ArrayList<ArrayList<Set<TriangleVertex>>>();
//        for (int i = 0; i < alternative.size(); i++) {
//            ArrayList<Set<TriangleVertex>> setGroup = new ArrayList<Set<TriangleVertex>>();
//            ArrayList<TriangleVertex> iset = alternative.get(i);
//            for (int j = 0; j < iset.size(); j++) {
//                Set<TriangleVertex> group = new HashSet<TriangleVertex>();
//                TriangleVertex u = iset.get(j);
//                group.add(u);
//                setGroup.add(group);
//            }
//            allSetGroups.add(setGroup);
//        }
//
//        for (int i = 0; i < allSetGroups.size(); i++) {
//            ArrayList<Set<TriangleVertex>> sets = allSetGroups.get(i);
//            for (int j = sets.size()-1; j >=0; j--) {
//                Set<TriangleVertex> set1 = sets.get(j);
//                for (int k = sets.size()-1; k >=0; k--) {
//                    if(j!=k){
//                        Set<TriangleVertex> set2 = sets.get(k);
//
//                    }
//                }
//            }
//        }


//        return null;
    }

    public TriangleVertex postOrder(TriangleVertex node, int[] Min, int[] Mout) {
        if (node.isLeaf) {
//            System.out.println("Stack " + node);
            Mout[node.id] = 0;
            Min[node.id] = node.node.getGain();
            return node;
        } else {
            TriangleVertex tmp = node.getNextNeighbor();
            while (tmp != null) {
                postOrder(tmp, Min, Mout);
                tmp = node.getNextNeighbor();
            }
//            System.out.println("Stack " + node);
            Min[node.id] = node.node.getGain();
            for (int i = 0; i < node.neighbors.size(); i++) {
                TriangleVertex v = node.neighbors.get(i);
                Mout[node.id] += Math.max(Mout[v.id], Min[v.id]);
                Min[node.id] += Mout[v.id];
            }
            return node;
        }

    }

    public void drawMSTEdges(Graphics g) {
        ArrayList<Gateway> list = new ArrayList<Gateway>(ActorsArray);
        list.addAll(RelayNodeArray);
        HashMap<String, Edge> mstEdgeHashMap = buildMSTHashMap(NetworkUtils.runKruskal(list));
        Iterator<String> iter = mstEdgeHashMap.keySet().iterator();
        while (iter.hasNext()) {
            Edge e = mstEdgeHashMap.get(iter.next());
            e.draw(g);
        }
    }

    private class TriangleVertex implements Comparable {
        int id;
        Triangle node;
        ArrayList<TriangleVertex> neighbors;
        Color color = Color.WHITE;
        boolean isRoot = false;
        boolean isLeaf = true;
        int nextNeighborID = 0;

        Edge mst1, mst2;

        private TriangleVertex(Triangle node, int id, Edge mst1, Edge mst2) {
            this(node, id);
            this.mst1 = mst1;
            this.mst2 = mst2;
        }

        public TriangleVertex(Triangle node, int id) {
            this.node = node;
            neighbors = new ArrayList<TriangleVertex>();
            this.id = id;
        }

        public TriangleVertex getNextNeighbor() {
            if (nextNeighborID < neighbors.size()) {
                return neighbors.get(nextNeighborID++);
            }
            return null;
        }

        public void addNeighbor(TriangleVertex n) {
            this.neighbors.add(n);
        }

        public int mstCardinality(TriangleVertex tv) {
            if (tv.mst1 != null && tv.mst2 != null && mst1 != null && mst2 != null) {
                if ((mst1.equals(tv.mst1) && mst2.equals(tv.mst2)) || (mst1.equals(tv.mst2) && mst2.equals(tv.mst1))) {
                    return 2;
                } else if ((mst1.equals(tv.mst1) && !mst2.equals(tv.mst2)) || (!mst1.equals(tv.mst1) && mst2.equals(tv.mst2)) || (mst1.equals(tv.mst2) && !mst2.equals(tv.mst1)) || (!mst1.equals(tv.mst2) && mst2.equals(tv.mst1))) {
                    return 1;
                } else {
                    return 0;
                }
            } else {
                return -1;
            }
        }

        public int getActualWeight() {
            if (mst1 != null && mst2 != null) {
                return mst1.weight + mst2.weight - node.f_weight;
            }
            return Integer.MIN_VALUE;
        }

        public int getWeight() {
            return node.getGain();
        }

        //sorts in descending order
        public int compareTo(Object o) {
            TriangleVertex t = (TriangleVertex) o;
            int gain = node.getGain();
            int t_gain = (t.node.getGain());
            if (gain < t_gain)
                return 1;
            else if (gain > t_gain)
                return -1;
            else
                return 0;
        }

        @Override
        public String toString() {
            return id + "[" + node.s1.getID() + ", " + node.s2.getID() + ", " + node.s3.getID() + "]\t Gain:" + node.getGain();
        }
    }

    public class Square {
        double topX, topY, width;
        int snum;

        public Square(double topX, double topY, double width, int snum) {
            this.topX = topX;
            this.topY = topY;
            this.width = width;
            this.snum = snum;
        }

        public void draw(Graphics g) {
            g.drawRect((int) topX, (int) topY, (int) width, (int) width);
        }
    }

    public void generateGrid() {
        int column = (int) (Constants.ApplicationAreaWidth / Constants.squareSize);
        double gap_column = (Constants.ApplicationAreaWidth % Constants.squareSize) / column;
        int row = (int) (Constants.ApplicationAreaHeight / Constants.squareSize);
        double gap_row = (Constants.ApplicationAreaHeight % Constants.squareSize) / row;
        grid = new Square[row][column];
        int counter = 0;
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                grid[i][j] = new Square(j * (Constants.squareSize + gap_column), i * (Constants.squareSize + gap_row), Constants.squareSize, counter);
                counter++;
            }
        }
    }

    public void determineInterfacePoints() {
        ArrayList<ArrayList<Gateway>> partitions = NetworkUtils.DephtFirstSearch(ActorsArray);
        InterfaceNodePairs[][] ipMap = new InterfaceNodePairs[partitions.size()][partitions.size()];
//        HashMap<Integer, ArrayList<InterfaceNodePairs>> ipMap = new HashMap<Integer, ArrayList<InterfaceNodePairs>>();
        for (int i = 0; i < partitions.size(); i++) {
            ArrayList<Gateway> list1 = partitions.get(i);
            for (int j = i + 1; j < partitions.size(); j++) {
                ArrayList<Gateway> list2 = partitions.get(j);
                if ((!list1.isEmpty()) && (!list2.isEmpty())) {
//                    int nid1 = list1.get(0).getNetworkID();
//                    int nid2= list2.get(0).getNetworkID();
                    ArrayList<Gateway> interfacePoints = NetworkUtils.getInterfacePoint(list1, list2);
                    ipMap[i][j] = new InterfaceNodePairs(interfacePoints.get(0), interfacePoints.get(1));

                }

            }
        }
//        System.out.println("");

    }

    private class InterfaceNodePairs implements Comparable {
        Gateway node1, node2;
        double distance;

        public InterfaceNodePairs(Gateway node1, Gateway node2) {
            this.node1 = node1;
            this.node2 = node2;
            distance = NetworkUtils.Distance(node1, node2);
        }

        public int compareTo(Object o) {
            InterfaceNodePairs inp = (InterfaceNodePairs) o;
            if (distance == inp.distance)
                return 0;
            else if (distance < inp.distance)
                return -1;
            else
                return 1;
        }

        public String toString() {
            return node1.toString() + "[" + node1.getNetworkID() + "]->" + node2.toString() + "[" + node2.getNetworkID() + "]";
        }
    }

    //ADT for representing the line between a represntative node and Center of Mass

    private class Line implements Comparable {
        Gateway representative;
        Point2D com;
        Point2D direction;
        boolean isDirectionChanged = false;
        int num;
        ArrayList<Gateway> nodeList = new ArrayList<Gateway>();
        boolean stop = false;
        double distance = 0;
        Line left = null;
        Line right = null;

        public Line(Gateway representative, Point2D com, int num) {
            this.representative = representative;
            this.com = com;
            this.direction = com;
            this.num = num;
            distance = AnalyticGeometry.euclideanDistance(representative.getX(), representative.getY(), this.com.getX(), this.com.getY());
        }

        public boolean isConnected(Line line) {
            ArrayList<Gateway> nextList = line.nodeList;
            for (Gateway gateway : nodeList) {
                for (Gateway gateway1 : nextList) {
                    if (NetworkUtils.EstimatedDistance(gateway, gateway1) <= Constants.RNTransmissionRange) {
                        return true;
                    }
                }
            }
            return false;
        }

        public int compareTo(Object o) {
            Line l = (Line) o;
            if (distance == l.distance)
                return 0;
            else if (distance > l.distance)
                return -1;
            else
                return 1;
        }


        public String toString() {
            return "" + representative.getID();
        }
    }

    public void runSpiderWebApproach() throws ArrayIndexOutOfBoundsException {
        boolean is2Connected = false;
//        System.out.println("First Approach");
        ArrayList<ArrayList<Gateway>> partitions = NetworkUtils.DephtFirstSearch(ActorsArray);
//        System.out.println("Number of Partitions : " + partitions.size());
        firstApproach = true;
        calculateCenterOfMass();
        boolean b = true;
        sortedLines.clear();
        sortedLines.addAll(Arrays.asList(lines));
        Collections.sort(sortedLines);
//        lines = new Line[sortedLines.size()];
//        for (int i = 0; i < lines.length; i++) {
//            lines[i] = sortedLines.get(i);
//        }

        while (b) {
            b = deployRelayNodesAlongTheLines();
        }

        if (is2Connected) {
            classifyRelayNodes();
            make2Connected();
            connectUnattachedPartitionFor2CSpider();

        } else {
            connectUnattachedPartitionFor1CSpider();
        }
//        connectUnattachedPartitionFor2CSpider();
//        attachRelayWithGateways();
        HashMap<Integer, Boolean> map = NetworkUtils.attachRelayWithGateways(ActorsArray, RelayNodeArray);
//        if (is2Connected) {
//            TwoConnectUnattachedPartitions(map);
//        }

    }

    private void find_max_distance_from_rn_to_terminal(String algorithmName) {
//        double sum_max_cost = 0;
//        for (int i = 0; i < ActorsArray.size(); i++) {
        Gateway terminal = ActorsArray.get(0);
        double max_cost = -1;
        for (int j = 0; j < RelayNodeArray.size(); j++) {
            Gateway rn = RelayNodeArray.get(j);
            double d = NetworkUtils.EstimatedDistance(terminal, rn);
            if (max_cost < d) {
                max_cost = d;
            }
        }
//            sum_max_cost += max_cost;
//        }

//        double avg = sum_max_cost / ActorsArray.size();
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(new File(algorithmName + "_cost_map.csv"), true));
            pw.println(ActorsArray.size() + "," + max_cost);
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void map_relays_to_segments(String algorithmName) {
        HashMap<Gateway, ArrayList<Gateway>> num_map = new HashMap<Gateway, ArrayList<Gateway>>();
        for (int i = 0; i < ActorsArray.size(); i++) {
            Gateway gateway = ActorsArray.get(i);
            num_map.put(gateway, new ArrayList<Gateway>());
        }
        for (int i = 0; i < RelayNodeArray.size(); i++) {
            Gateway RNi = RelayNodeArray.get(i);
            double minDist = Double.MAX_VALUE;
            Gateway minTerminal = null;
            for (int j = 0; j < ActorsArray.size(); j++) {
                Gateway Tj = ActorsArray.get(j);
                double d = NetworkUtils.EstimatedDistance(RNi, Tj);
                if (d <= minDist) {
                    minDist = d;
                    minTerminal = Tj;
                }
            }
            if (minTerminal != null) {
                num_map.get(minTerminal).add(RNi);
            }

        }
        double[] cost_array = new double[RelayNodeArray.size()];
        int counter = 0;
        for (int i = 0; i < ActorsArray.size(); i++) {
            Gateway Ti = ActorsArray.get(i);
            ArrayList<Gateway> listOfAssocRelays = num_map.get(Ti);
            for (int j = 0; j < listOfAssocRelays.size(); j++) {
                Gateway rn = listOfAssocRelays.get(j);
                cost_array[counter++] = NetworkUtils.EstimatedDistance(Ti, rn);
            }
        }
        double max_cost = -1;
        for (int i = 0; i < cost_array.length; i++) {
            double v = cost_array[i];
            if (v > max_cost) {
                max_cost = v;
            }
        }
        try {
            PrintWriter pw1 = new PrintWriter(new FileWriter(new File(algorithmName + "_num_map.csv"), true));
            PrintWriter pw2 = new PrintWriter(new FileWriter(new File(algorithmName + "_cost_map.csv"), true));
            pw1.print(ActorsArray.size() + ",");
            pw2.println(ActorsArray.size() + "," + max_cost);
            for (int i = 0; i < ActorsArray.size(); i++) {
                Gateway gateway = ActorsArray.get(i);
                if (i != ActorsArray.size() - 1) {
                    pw1.print(num_map.get(gateway).size() + ",");
                } else {
                    pw1.println(num_map.get(gateway).size());
                }
            }
            pw1.close();
            pw2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void fix_data() {
        for (int p = 3; p <= 15; p++) {
            for (int e = 1; e <= 50; e++) {
                boolean redo = false, first = true;
                while (first || redo) {
                    redo = false;
                    first = false;
                    String filename = p + "_" + e + ".dat";
                    reload(filename);
                    ArrayList<ArrayList<Gateway>> segments = NetworkUtils.DephtFirstSearch(ActorsArray);

                    if (segments.size() == p && ActorsArray.size() == p) {
                        System.out.println("OK");
                        continue;
                    }


                    if (segments.size() == p) {
                        System.out.println("rearranged " + filename);
                        ActorsArray.clear();
                        for (int i = 0; i < segments.size(); i++) {
                            ArrayList<Gateway> segment_i = segments.get(i);
                            ActorsArray.add(segment_i.get(0));
                        }
                        save(filename);
                    } else {
                        System.out.println("fixed " + filename);
                        fix(p, e);
                        redo = true;
                    }
                }
            }
        }
    }


    private void connectUnattachedPartitionFor1CSpider() {
        for (int i = 0; i < ActorsArray.size(); i++) {
            Gateway terminal = ActorsArray.get(i);
            ArrayList<Gateway> neighbors = new ArrayList<Gateway>();
            for (int j = 0; j < RelayNodeArray.size(); j++) {
                Gateway relay = RelayNodeArray.get(j);
                if (NetworkUtils.EstimatedDistance(terminal, relay) <= Constants.RNTransmissionRange) {
                    neighbors.add(relay);
                }
            }
            if (neighbors.isEmpty()) {
                int minIndex = -1;
                double minDist = Double.MAX_VALUE;
                for (int j = 0; j < RelayNodeArray.size(); j++) {
                    Gateway relay = RelayNodeArray.get(j);
                    double distance = NetworkUtils.Distance(terminal, relay);
                    if (distance < minDist) {
                        minDist = distance;
                        minIndex = j;
                    }
                }
                NetworkUtils.fillGap(RelayNodeArray.get(minIndex), terminal, true, ActorsArray, RelayNodeArray);
            }
        }
    }

    private void connectUnattachedPartitionFor2CSpider() {
        for (int i = 0; i < ActorsArray.size(); i++) {
            Gateway terminal = ActorsArray.get(i);
            ArrayList<Gateway> neighbors = new ArrayList<Gateway>();
            for (int j = 0; j < RelayNodeArray.size(); j++) {
                Gateway relay = RelayNodeArray.get(j);
                if (NetworkUtils.Distance(terminal, relay) <= Constants.RNTransmissionRange + 2) {
                    neighbors.add(relay);
                }
            }
            if (neighbors.isEmpty()) {
                int fMinIndex = Integer.MAX_VALUE, sMinIndex = Integer.MAX_VALUE;
                double fMinVal = Double.MAX_VALUE, sMinVal = Double.MAX_VALUE;
                for (int j = 0; j < RelayNodeArray.size(); j++) {
                    Gateway relay = RelayNodeArray.get(j);
                    double distance = NetworkUtils.Distance(terminal, relay);
                    if (distance <= fMinVal) {
                        sMinVal = fMinVal;
                        sMinIndex = fMinIndex;
                        fMinVal = distance;
                        fMinIndex = j;
                    } else {
                        if (distance <= sMinVal) {
                            sMinVal = distance;
                            sMinIndex = j;
                        }
                    }
                }
                Gateway r1 = RelayNodeArray.get(fMinIndex);
                Gateway r2 = RelayNodeArray.get(sMinIndex);
                NetworkUtils.fillGap(r1, terminal, true, ActorsArray, RelayNodeArray);
                Gateway last1 = RelayNodeArray.get(RelayNodeArray.size() - 1);
                NetworkUtils.fillGap(r2, terminal, true, ActorsArray, RelayNodeArray);
                Gateway last2 = RelayNodeArray.get(RelayNodeArray.size() - 1);
                if (NetworkUtils.Distance(last1, last2) > Constants.RNTransmissionRange) {
                    NetworkUtils.fillGap(last1, last2, true, ActorsArray, RelayNodeArray);
                }
//                for (int j = 0; j < RelayNodeArray.size(); j++) {
//                    Gateway relay = RelayNodeArray.get(j);
//                    if (NetworkUtils.Distance(terminal, relay) <= Constants.ActorTransmissionRange) {
//                        neighbors.add(relay);
//                    }
//                }
            } else if (neighbors.size() == 1) {
                Gateway ne = neighbors.get(0);
                double minDist = Double.MAX_VALUE;
                int minIndex = Integer.MAX_VALUE;
                for (int j = 0; j < RelayNodeArray.size(); j++) {
                    Gateway relay = RelayNodeArray.get(j);
                    if (relay.getID() != ne.getID()) {
                        double dist = NetworkUtils.Distance(terminal, relay);
                        if (dist < minDist) {
                            minIndex = j;
                            minDist = dist;
                        }
                    }
                }
                NetworkUtils.fillGap(RelayNodeArray.get(minIndex), terminal, true, ActorsArray, RelayNodeArray);
                Gateway last = RelayNodeArray.get(RelayNodeArray.size() - 1);
                if (NetworkUtils.Distance(last, ne) > Constants.RNTransmissionRange) {
                    NetworkUtils.fillGap(last, ne, true, ActorsArray, RelayNodeArray);
                }
            }
        }
    }

    /**
     * @param map map
     * @deprecated use connectUnattachedPartitionFor2CSpider
     */
    private void ConnectUnattachedPartitions(HashMap<Integer, Boolean> map) {
        HashMap<Integer, ArrayList<Gateway>> unattachedNodeMap = new HashMap<Integer, ArrayList<Gateway>>();
        for (int i = 0; i < ActorsArray.size(); i++) {
            Gateway gateway = ActorsArray.get(i);
            if (map.get(gateway.getNetworkID()) == null) {
                if (unattachedNodeMap.get(gateway.getNetworkID()) == null) {
                    ArrayList<Gateway> lst = new ArrayList<Gateway>();
                    lst.add(gateway);
                    unattachedNodeMap.put(gateway.getNetworkID(), lst);
                } else {
                    unattachedNodeMap.get(gateway.getNetworkID()).add(gateway);
                }
            }
        }

        Set<Integer> keyset = unattachedNodeMap.keySet();

        for (Integer nid : keyset) {
            ArrayList<Gateway> lst = unattachedNodeMap.get(nid);
            int pi = -1, ri = -1;
            double minDist = Integer.MAX_VALUE;
            for (int i = 0; i < lst.size(); i++) {
                Gateway gateway = lst.get(i);
                for (int j = 0; j < RelayNodeArray.size(); j++) {
                    Gateway rn = RelayNodeArray.get(j);
                    double dist = NetworkUtils.EstimatedDistance(gateway, rn);
                    if (dist <= minDist) {
                        minDist = dist;
                        pi = i;
                        ri = j;
                    }
                }
            }

            double a = minDist / Constants.ActorTransmissionRange;
            int req = (int) Math.ceil(a) - 1;
            Gateway actor = lst.get(pi);
            Gateway relay = RelayNodeArray.get(ri);
            for (int k = 0; k < req; k++) {
                Point2D p = AnalyticGeometry.getCoordinates(actor.getX(), actor.getY(), relay.getX(), relay.getY(), minDist / (req + 1));
                Gateway rn = new Gateway(RelayNodeArray.size());
                rn.isRelay = true;
                rn.setX(p.getX());
                rn.setY(p.getY());
                RelayNodeArray.add(rn);
                NetworkUtils.calculateActorNeighborhoods(RelayNodeArray, Constants.RNTransmissionRange);
            }
        }
    }

    private void TwoConnectUnattachedPartitions(HashMap<Integer, Boolean> map) {
        HashMap<Integer, ArrayList<Gateway>> unattachedNodeMap = new HashMap<Integer, ArrayList<Gateway>>();
        for (int i = 0; i < ActorsArray.size(); i++) {
            Gateway gateway = ActorsArray.get(i);
            if (map.get(gateway.getNetworkID()) == null) {
                if (unattachedNodeMap.get(gateway.getNetworkID()) == null) {
                    ArrayList<Gateway> lst = new ArrayList<Gateway>();
                    lst.add(gateway);
                    unattachedNodeMap.put(gateway.getNetworkID(), lst);
                } else {
                    unattachedNodeMap.get(gateway.getNetworkID()).add(gateway);
                }
            }
        }

        Set<Integer> keyset = unattachedNodeMap.keySet();
        Iterator<Integer> iter = keyset.iterator();

        while (iter.hasNext()) {
            int nid = iter.next();
            ArrayList<Gateway> lst = unattachedNodeMap.get(nid);
            int pi = -1, ri = -1, ri2 = -1, pi2 = -1;
            double minDist1 = Integer.MAX_VALUE, minDist2 = Integer.MAX_VALUE;
            for (int i = 0; i < lst.size(); i++) {
                Gateway gateway = lst.get(i);
                for (int j = 0; j < RelayNodeArray.size(); j++) {
                    Gateway rn = RelayNodeArray.get(j);
                    double dist = NetworkUtils.Distance(gateway, rn);
                    if (dist <= minDist1) {
                        minDist1 = dist;
                        pi = i;
                        ri = j;
                    }
                }
            }

            for (int i = 0; i < lst.size(); i++) {
                Gateway gateway = lst.get(i);
                for (int j = 0; j < RelayNodeArray.size(); j++) {
                    Gateway rn = RelayNodeArray.get(j);
                    double dist = NetworkUtils.Distance(gateway, rn);
                    if (dist <= minDist2 && dist != minDist1) {
                        minDist2 = dist;
                        pi2 = i;
                        ri2 = j;
                    }
                }
            }

            Gateway g1 = lst.get(pi);
            Gateway r1 = RelayNodeArray.get(ri);
            NetworkUtils.fillGap(g1, r1, false, ActorsArray, RelayNodeArray);
            Gateway g2 = lst.get(pi2);
            Gateway r2 = RelayNodeArray.get(ri2);
            NetworkUtils.fillGap(g2, r2, false, ActorsArray, RelayNodeArray);

//            double a = minDist1 / Constants.ActorTransmissionRange;
//            int req = (int) Math.ceil(a) - 1;
//            Gateway actor = lst.get(pi);
//            Gateway relay = RelayNodeArray.get(ri);
//            for (int k = 0; k < req; k++) {
//                Point2D p = NetworkUtils.getCoordinates(actor.getX(), actor.getY(), relay.getX(), relay.getY(), minDist1 / (req + 1));
//                Gateway rn = new Gateway(RelayNodeArray.size());
//                rn.isRelay = true;
//                rn.setX(p.getX());
//                rn.setY(p.getY());
//                RelayNodeArray.add(rn);
//                NetworkUtils.calculateActorNeighborhoods(RelayNodeArray, Constants.RNTransmissionRange);
//            }
        }
    }


    public boolean deployRelayNodesAlongTheLines() throws ArrayIndexOutOfBoundsException {
        boolean updated = false;
        for (int i = 0; i < sortedLines.size(); i++) {
            Line line = sortedLines.get(i);
            if (!line.stop) {
                updated = true;
                if (line.nodeList.isEmpty()) {
                    Point2D p = AnalyticGeometry.getCoordinates(line.representative.getX(), line.representative.getY(), line.direction.getX(), line.direction.getY(), Constants.ActorTransmissionRange);
                    if (p.getX() == line.com.getX() && p.getY() == line.com.getY()) {
                        if (!com_deployed) {
                            Gateway rn = new Gateway(RelayNodeArray.size());
                            rn.isRelay = true;
                            rn.setX(p.getX());
                            rn.setY(p.getY());
                            rn.lineID = line.num;
                            rn.addLabel(Constants.FIRST);
                            rn.setTerminal(line.representative);
                            RelayNodeArray.add(rn);
                            line.nodeList.add(rn);
                            com_deployed = true;
                            updateStopCondition();
                        }
                    } else {
                        Gateway rn = new Gateway(RelayNodeArray.size());
                        rn.isRelay = true;
                        rn.setX(p.getX());
                        rn.setY(p.getY());
                        rn.lineID = line.num;
                        rn.addLabel(Constants.FIRST);
                        rn.setTerminal(line.representative);
                        RelayNodeArray.add(rn);
                        line.nodeList.add(rn);
                        updateStopCondition();
                    }
                } else {
                    Point2D p = AnalyticGeometry.getCoordinates(line.nodeList.get(line.nodeList.size() - 1).getX(), line.nodeList.get(line.nodeList.size() - 1).getY(), line.direction.getX(), line.direction.getY(), Constants.RNTransmissionRange - 2);
                    if (p.getX() == line.com.getX() && p.getY() == line.com.getY()) {
                        if (!com_deployed) {
                            Gateway rn = new Gateway(RelayNodeArray.size());
                            rn.isRelay = true;
                            rn.setX(p.getX());
                            rn.setY(p.getY());
                            rn.lineID = line.num;
                            if (line.isDirectionChanged) {
                                rn.addLabel(Constants.RING);
                                rn.setTerminal(null);
                            }
                            RelayNodeArray.add(rn);
                            line.nodeList.add(rn);
                            com_deployed = true;
                            updateStopCondition();
                        }
                    } else {
                        Gateway rn = new Gateway(RelayNodeArray.size());
                        rn.isRelay = true;
                        rn.setX(p.getX());
                        rn.setY(p.getY());
                        rn.lineID = line.num;
                        if (line.isDirectionChanged) {
                            rn.addLabel(Constants.RING);
                            rn.setTerminal(null);
                        }
                        RelayNodeArray.add(rn);
                        line.nodeList.add(rn);
                        updateStopCondition();
                    }
                }
            }
        }
        //updateStopCondition();
        return updated;
    }

    private void updateStopCondition() {
        NetworkUtils.calculateActorNeighborhoods(RelayNodeArray, Constants.RNTransmissionRange);
        for (int i = 0; i < lines.length; i++) {
            updateLineStopCondition(i);
        }

    }
    /**
     * new version updateLineStop
     */
    /*
    public void updateLineStopCondition(int index) {
        if (lines[index].stop) {
            return;
        }
        int leftIndex, rightIndex;
        if (index == 0) {
            leftIndex = lines.length - 1;
        } else {
            leftIndex = index - 1;
        }
        if (index == lines.length - 1) {
            rightIndex = 0;
        } else {
            rightIndex = index + 1;
        }
        boolean lc = lines[index].isConnected(lines[leftIndex]);
        boolean rc = lines[index].isConnected(lines[rightIndex]);
        boolean updated = false;
        if (lc && rc) {
            lines[index].stop = true;
        } else if (lc) {
            // if right line neighbor is also righ-connected
            if (lines[rightIndex].isConnected(lines[(rightIndex + 1) % lines.length])) {
                lines[index].stop = true;
                lines[rightIndex].stop = true;
                updated = true;

                ArrayList<Gateway> cList = lines[index].nodeList;
                ArrayList<Gateway> rList = lines[rightIndex].nodeList;
                int cIndex = -1, rIndex = -1;
                double minDist = Double.MAX_VALUE;
                for (int i = 0; i < cList.size(); i++) {
                    Gateway ci = cList.get(i);
                    for (int j = 0; j < rList.size(); j++) {
                        Gateway ri = rList.get(j);
                        double dist = NetworkUtils.Distance(ci, ri);
                        if (dist < minDist) {
                            minDist = dist;
                            cIndex = i;
                            rIndex = j;
                        }
                    }
                }
                double a = minDist / Constants.RNTransmissionRange;
                int req = (int) Math.ceil(a) - 1;
                Gateway ci = cList.get(cIndex);
                Gateway ri = rList.get(rIndex);
//                if (!ri.getLabels().isEmpty()) {
//                    if (ri.getLabels().get(0) == Constants.FIRST) {
//                        ri.removeFirstLabel();
//                        ri.addLabel(Constants.RING);
//                    }
//                }
//                if (!ci.getLabels().isEmpty()) {
//                    if (ci.getLabels().get(0) == Constants.FIRST) {
//                        ci.removeFirstLabel();
//                        ci.addLabel(Constants.RING);
//                    }
//                }
                for (int k = 0; k < req; k++) {
                    Point2D p = WSNGraphics.getCoordinates(ci.getX(), ci.getY(), ri.getX(), ri.getY(), minDist / (req + 1));
                    Gateway rn = new Gateway(RelayNodeArray.size());
                    rn.isRelay = true;
                    rn.setX(p.getX());
                    rn.setY(p.getY());
//                    rn.addLabel(Constants.RING);
                    RelayNodeArray.add(rn);
                    lines[index].nodeList.add(rn);
                    NetworkUtils.calculateActorNeighborhoods(RelayNodeArray, Constants.RNTransmissionRange);
                }


            } else {

                lines[index].stop = false;
                if (lines[rightIndex].nodeList.size() != 0) {
                    Gateway n = lines[rightIndex].nodeList.get(lines[rightIndex].nodeList.size() - 1);
                    Point2D p = new Point2D.Double();
                    p.setLocation(n.getX(), n.getY());
                    lines[index].direction = p;
                }
            }
        } else if (rc) {
            lines[index].stop = false;
            Gateway n;
            if (!lines[leftIndex].nodeList.isEmpty()) {
                n = lines[leftIndex].nodeList.get(lines[leftIndex].nodeList.size() - 1);
            } else {
                n = lines[leftIndex].representative;
            }
            Point2D p = new Point2D.Double();
            p.setLocation(n.getX(), n.getY());
            lines[index].direction = p;
        }
        if (!updated)
            lines[index].stop = lc && rc;
    }
    */

    /**
     * old version updateLineStop
     *
     * @param index the index of the line to be updated
     */
    public void updateLineStopCondition(int index) {
        if (lines[index].stop) {
            return;
        }

        boolean lc = lines[index].isConnected(lines[index].left);
        boolean rc = lines[index].isConnected(lines[index].right);
        boolean updated = false;
        if (lc && rc) {
            lines[index].stop = true;
        } else if (lc) {
            // if right line neighbor is also righ-connected
            if (lines[index].right.isConnected(lines[index].right.right)) {
                lines[index].stop = true;
                lines[index].right.stop = true;
                updated = true;

                ArrayList<Gateway> cList = lines[index].nodeList;
                ArrayList<Gateway> rList = lines[index].right.nodeList;
                int cIndex = -1, rIndex = -1;
                double minDist = Double.MAX_VALUE;
                for (int i = 0; i < cList.size(); i++) {
                    Gateway ci = cList.get(i);
                    for (int j = 0; j < rList.size(); j++) {
                        Gateway ri = rList.get(j);
                        double dist = NetworkUtils.Distance(ci, ri);
                        if (dist < minDist) {
                            minDist = dist;
                            cIndex = i;
                            rIndex = j;
                        }
                    }
                }
                double a = minDist / Constants.RNTransmissionRange;
                int req = (int) Math.ceil(a) - 1;
                Gateway ci = cList.get(cIndex);
                Gateway ri = rList.get(rIndex);
                if (!ri.getLabels().isEmpty()) {
                    if (ri.getLabels().get(0) == Constants.FIRST) {
                        ri.removeFirstLabel();
                        ri.addLabel(Constants.RING);
                        ri.setTerminal(null);
                    }
                }
                if (!ci.getLabels().isEmpty()) {
                    if (ci.getLabels().get(0) == Constants.FIRST) {
                        ci.removeFirstLabel();
                        ci.addLabel(Constants.RING);
                        ci.setTerminal(null);
                    }
                }
                for (int k = 0; k < req; k++) {
                    Point2D p = AnalyticGeometry.getCoordinates(ci.getX(), ci.getY(), ri.getX(), ri.getY(), (k + 1) * minDist / (req + 1));
                    Gateway rn = new Gateway(RelayNodeArray.size());
                    rn.isRelay = true;
                    rn.setX(p.getX());
                    rn.setY(p.getY());
                    rn.lineID = lines[index].num;
                    rn.addLabel(Constants.RING);
                    rn.setTerminal(null);
                    RelayNodeArray.add(rn);
                    lines[index].nodeList.add(rn);
                    NetworkUtils.calculateActorNeighborhoods(RelayNodeArray, Constants.RNTransmissionRange);
                }


            } else {

                lines[index].stop = false;
                Gateway n;
                if (lines[index].right.nodeList.size() != 0) {
                    n = lines[index].right.nodeList.get(lines[index].right.nodeList.size() - 1);
                } else {
                    n = lines[index].right.representative;
                }
                Point2D p = new Point2D.Double();
                p.setLocation(n.getX(), n.getY());
                lines[index].direction = p;
                lines[index].isDirectionChanged = true;
            }
        } else if (rc) {
            lines[index].stop = false;
            Gateway n;
            if (lines[index].left.nodeList.size() != 0) {
                n = lines[index].left.nodeList.get(lines[index].left.nodeList.size() - 1);
            } else {
                n = lines[index].left.representative;
            }
            Point2D p = new Point2D.Double();
            p.setLocation(n.getX(), n.getY());
            lines[index].direction = p;
            lines[index].isDirectionChanged = true;
        }
        if (!updated)
            lines[index].stop = lc && rc;
    }

    public void calculateOuterPolygon() {
        ArrayList<ArrayList<Gateway>> partitions = NetworkUtils.DephtFirstSearch(ActorsArray);
//        ArrayList<Gateway> represetatives;
//        ArrayList<Gateway> temp = new ArrayList<Gateway>();
//        for (int i = 0; i < partitions.size(); i++) {
//            ArrayList<Gateway> gateways = partitions.get(i);
//            temp.add(gateways.get(0));
//        }
//        represetatives = PolygonUtilities.getConvexHull(temp);
//        for (int i = 0; i < partitions.size(); i++) {
//            ArrayList<Gateway> gateways = partitions.get(i);
//            Gateway g = represetatives.get(i);
//            for (int j = 0; j < partitions.size(); j++) {
//                ArrayList<Gateway> arrayList = partitions.get(j);
//                if(arrayList.get(0).getNetworkID()==g.getNetworkID()){
//                    partitions.set(i, arrayList);
//                    partitions.set(j, gateways);
//                }
//            }
//        }
//        ArrayList<ArrayList<Gateway>> partitions = orderNeigborPartitions(NetworkUtils.DephtFirstSearch(ActorsArray));
        for (int i = 0; i < partitions.size(); i++) {
            if (i == partitions.size() - 1) {
                interfacePoints.add(NetworkUtils.getInterfacePoint(partitions.get(i), partitions.get(0)));
            } else {
                interfacePoints.add(NetworkUtils.getInterfacePoint(partitions.get(i), partitions.get(i + 1)));
            }
        }
    }

    public void drawConvexHull(Graphics g) {
        if (firstApproach) {
            for (int i = 0; i < convexHull.size(); i++) {
                Gateway gateway = convexHull.get(i);
                Gateway dest;
                if (i == convexHull.size() - 1) {
                    dest = convexHull.get(0);

                } else {
                    dest = convexHull.get(i + 1);
                }
                WSNGraphics.drawDashedLine(g, gateway.getX(), gateway.getY(), dest.getX(), dest.getY(), 5, 5);
            }
        }
    }

    public void drawOuterPolygon(Graphics g) {
        if (secondApproach) {
            for (int i = 0; i < interfacePoints.size(); i++) {
                ArrayList<Gateway> ip = interfacePoints.get(i);
                WSNGraphics.drawDashedLine(g, ip.get(0).getX(), ip.get(0).getY(), ip.get(1).getX(), ip.get(1).getY(), 5, 5);
            }
        }
    }

    public void runSMSTApproach() {
//        System.out.println("Second Approach");
        ArrayList<ArrayList<Gateway>> partitions = NetworkUtils.DephtFirstSearch(ActorsArray);

        ArrayList<Gateway> representatives = new ArrayList<Gateway>();
        for (int i = 0; i < partitions.size(); i++) {
            ArrayList<Gateway> p = partitions.get(i);
            representatives.add(p.get(0));
        }

        ArrayList<Edge> edges = NetworkUtils.runKruskal(representatives);
//        ArrayList<ArrayList<Gateway>> updatedEdges = NetworkUtils.updateInterfaceEdges(edges, representatives, ActorsArray);

        for (Edge e : edges) {
            Gateway u = e.u;
            Gateway v = e.v;
            NetworkUtils.fillGap(u, v, true, ActorsArray, RelayNodeArray);
        }
    }


    /**
     * Finds and returns RNs which are already deployed along the edge between u and v, If the edge is not steinerized returns and empty list
     *
     * @param u           segment
     * @param v           segment
     * @param isBothRelay : true
     * @return list of nodes
     */
    private ArrayList<Gateway> getRNsConnectingUV(Gateway u, Gateway v, boolean isBothRelay) {
        ArrayList<Point2D> deploymentPoints = NetworkUtils.getDeploymentPoints(u, v, isBothRelay);
        ArrayList<Gateway> result = new ArrayList<Gateway>();
        for (int i = 0; i < RelayNodeArray.size(); i++) {
            Gateway rn = RelayNodeArray.get(i);
            for (int j = 0; j < deploymentPoints.size(); j++) {
                Point2D p = deploymentPoints.get(j);
                if (Math.abs(rn.getX() - p.getX()) < Math.pow(10, -4) && Math.abs(rn.getY() - p.getY()) < Math.pow(10, -4)) {
                    //if (rn.getX() == p.getX() && rn.getY() == p.getY()) {
                    result.add(rn);
                }
            }
        }
        if (deploymentPoints.size() != result.size()) {
            result.clear();
        }
        return result;
    }


    public void deployNodeSecondApproach() {
        for (int i = 0; i < interfacePoints.size(); i++) {
            ArrayList<Gateway> ip = interfacePoints.get(i);
            double dist = NetworkUtils.Distance(ip.get(0), ip.get(1));
            int minNumOfNodesRequires;
            double di = dist - 2 * Constants.ActorTransmissionRange;
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
                Point2D p = AnalyticGeometry.getCoordinates(ip.get(0).getX(), ip.get(0).getY(), ip.get(1).getX(), ip.get(1).getY(), dist / 2);
                deploymentPoints.add(p);
            } else if (minNumOfNodesRequires == 2) {
                Point2D p1 = AnalyticGeometry.getCoordinates(ip.get(0).getX(), ip.get(0).getY(), ip.get(1).getX(), ip.get(1).getY(), Constants.ActorTransmissionRange);
                Point2D p2 = AnalyticGeometry.getCoordinates(ip.get(1).getX(), ip.get(1).getY(), ip.get(0).getX(), ip.get(0).getY(), Constants.ActorTransmissionRange);
                deploymentPoints.add(p1);
                deploymentPoints.add(p2);
            } else {
                Point2D p1 = AnalyticGeometry.getCoordinates(ip.get(0).getX(), ip.get(0).getY(), ip.get(1).getX(), ip.get(1).getY(), Constants.ActorTransmissionRange);
                Point2D p2 = AnalyticGeometry.getCoordinates(ip.get(1).getX(), ip.get(1).getY(), ip.get(0).getX(), ip.get(0).getY(), Constants.ActorTransmissionRange);
                deploymentPoints.add(p1);
                deploymentPoints.add(p2);
                int n = minNumOfNodesRequires - 2;
                for (int j = 1; j <= n; j++) {
                    double d = (di / (n + 1));
                    Point2D pi = AnalyticGeometry.getCoordinates(p1.getX(), p1.getY(), p2.getX(), p2.getY(), j * d);
                    deploymentPoints.add(pi);
                }
            }
            for (int j = 0; j < deploymentPoints.size(); j++) {
                Point2D pj = deploymentPoints.get(j);
                Gateway rn = new Gateway(RelayNodeArray.size());
                rn.isRelay = true;
                rn.setX(pj.getX());
                rn.setY(pj.getY());
                RelayNodeArray.add(rn);
            }
            NetworkUtils.calculateActorNeighborhoods(RelayNodeArray, Constants.RNTransmissionRange);


        }
    }

    public void ratio3Approximation() {
        ArrayList<ArrayList<Gateway>> partitions = NetworkUtils.DephtFirstSearch(ActorsArray);

        ArrayList<Gateway> representatives = new ArrayList<Gateway>();
        for (int i = 0; i < partitions.size(); i++) {
            ArrayList<Gateway> p = partitions.get(i);
            representatives.add(p.get(0));
        }
    }


    public void report() {
        eliminateDuplicateEdges();
//        int numOfCutVertices = findNumberofCutVertices();
        System.out.println("Number of Relay Nodes : " + RelayNodeArray.size());
        int[] cutVertices = findNumberofAllCutVertices();
        System.out.println("Number of Terminal Cut Vertices : " + cutVertices[0]);
        System.out.println("Number of Relay Cut Vertices : " + cutVertices[1]);
        double avg_node_degree = findAverageNodeDegreeOfRelayNodes();
        System.out.println("Average Node Degree is : " + avg_node_degree);
        HashMap<String, Integer> Table = NetworkUtils.FloydWarshall(ActorsArray, RelayNodeArray);
        Iterator iter = Table.keySet().iterator();

        double sum = 0, max = -1000;
        while (iter.hasNext()) {
            String key = (String) iter.next();
            Integer value = Table.get(key);
            sum += value;
            if (value > max)
                max = value;
        }
        double coverage = NetworkUtils.EvaluateTotalArea(RelayNodeArray);
        System.out.println("Average Distance : " + sum / Table.size());
        System.out.println("Max Distance : " + max);
        System.out.println("Coverage : " + coverage);
        System.out.println("");
    }

    public void report(String filename, int numOfPartitions) {
        eliminateDuplicateEdges();
        int numOfCutVertices = findNumberofCutVertices();
//        System.out.println("Number of Relay Nodes : " + RelayNodeArray.size());
//        System.out.println("Number of Cut Vertices : " + numOfCutVertices);
        double avg_node_degree = findAverageNodeDegreeOfRelayNodes();
//        System.out.println("Average Node Degree is : " + avg_node_degree);
        HashMap<String, Integer> Table = NetworkUtils.FloydWarshall(ActorsArray, RelayNodeArray);
        Iterator iter = Table.keySet().iterator();

        double sum = 0, max = -1000;
        while (iter.hasNext()) {
            String key = (String) iter.next();
            Integer value = Table.get(key);
            sum += value;
            if (value > max)
                max = value;
        }
        double coverage = NetworkUtils.EvaluateTotalArea(RelayNodeArray);
        double avg_distance = sum / Table.size();
//        System.out.println("Average Distance : " + avg_distance);
//        System.out.println("Max Distance : " + max);
//        System.out.println("Coverage : " + coverage);
        System.out.println("");


        try {

            PrintWriter pw = new PrintWriter(new FileWriter(new File(filename), true));

            pw.print(numOfPartitions + ",");
            pw.print(RelayNodeArray.size() + ",");
            pw.print(numOfCutVertices + ",");
            pw.print(avg_node_degree + ",");
            pw.print(avg_distance + ",");
            pw.print(max + ",");
            pw.println(coverage);

            pw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void report(String filename, int numOfPartitions, double ar) {
        eliminateDuplicateEdges();
//        int[] allCutVertices = findNumberofAllCutVertices();
        double avg_node_degree = findAverageNodeDegreeOfRelayNodes();
//        System.out.println("Average Node Degree is : " + avg_node_degree);
//        HashMap<String, Integer> Table = NetworkUtils.FloydWarshall(ActorsArray, RelayNodeArray);
//        Iterator iter = Table.keySet().iterator();
//
//        double sum = 0, max = -1000;
//        while (iter.hasNext()) {
//            String key = (String) iter.next();
//            Integer value = Table.get(key);
//            sum += value;
//            if (value > max)
//                max = value;
//        }
//        double coverage = NetworkUtils.EvaluateTotalArea(RelayNodeArray, ar);
//        double avg_distance = sum / Table.size();
//        System.out.println("Average Distance : " + avg_distance);
//        System.out.println("Max Distance : " + max);
//        System.out.println("Coverage : " + coverage);
//        System.out.println("");


        try {

            PrintWriter pw = new PrintWriter(new FileWriter(new File(filename), true));
            /*
            pw.print(numOfPartitions + ";");
//            pw.print(ar + ",");
            pw.print(RelayNodeArray.size() + ";");
            pw.print(allCutVertices[0] + ";");
            pw.print(allCutVertices[1] + ";");
            pw.print(avg_node_degree + ";");
            pw.print(avg_distance + ";");
            pw.print(max + ";");
            pw.println(coverage);
            */
            String str = avg_node_degree + "";
            String replace = str.replace('.', ',');
            pw.print(numOfPartitions + ";");
//            pw.print(ar + ",");
            pw.print(RelayNodeArray.size() + ";");
            pw.print(0 + ";");
            pw.print(0 + ";");
            pw.print(replace + ";");
            pw.print(0 + ";");
            pw.print(0 + ";");
            pw.println(0);

            pw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void eliminateDuplicateEdges() {
        for (int i = 0; i < RelayNodeArray.size(); i++) {
            Gateway rn = RelayNodeArray.get(i);
            HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
            for (int j = rn.getNeighborList().size() - 1; j >= 0; j--) {
                Gateway ne = rn.getNeighborList().get(j);
                if (!ne.isRelay) {
                    if (map.get(ne.getID()) == null) {
                        map.put(ne.getID(), 1);
                    } else {
                        rn.getNeighborList().remove(ne);
                    }
                }
            }
        }


        for (int i = 0; i < ActorsArray.size(); i++) {
            Gateway gateway = ActorsArray.get(i);
            HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
            for (int j = gateway.getNeighborList().size() - 1; j >= 0; j--) {
                Gateway ne = gateway.getNeighborList().get(j);
                if (ne.isRelay) {
                    if (map.get(ne.getID()) == null) {
                        map.put(ne.getID(), 1);
                    } else {
                        gateway.getNeighborList().remove(ne);
                    }
                }
            }
        }
    }

    /**
     * Only looks at the relay network
     *
     * @return numberofcutvertices
     */
    private int findNumberofCutVertices() {
        int result = 0;
        for (int i = 0; i < RelayNodeArray.size(); i++) {
            Gateway gateway = RelayNodeArray.get(i);
            if (isCutVertex(gateway)) {
                result++;
            }
        }

        return result;
    }


    private boolean isCV(Gateway rn) {
        ArrayList<Gateway> allNodes = new ArrayList<Gateway>();
        ArrayList<ArrayList<Gateway>> partitions;


        for (int i = 0; i < ActorsArray.size(); i++) {
            Gateway gateway = ActorsArray.get(i);
            if ((!rn.isRelay) && rn.getID() == gateway.getID()) {
                continue;
            }
            Gateway ng = gateway.cloneGateway();
            allNodes.add(ng);
        }
        for (int i = 0; i < RelayNodeArray.size(); i++) {
            Gateway gateway = RelayNodeArray.get(i);
            if ((rn.isRelay) && rn.getID() == gateway.getID()) {
                continue;
            }
            Gateway ng = gateway.cloneGateway();
            allNodes.add(ng);
        }
        NetworkUtils.calculateActorNeighborhoods(allNodes, Constants.RNTransmissionRange);
        partitions = NetworkUtils.DephtFirstSearch(allNodes);
        return partitions.size() != 1;


    }


    /**
     * gets all cutvertices
     * result is array of cut vertices
     *
     * @return result
     */
    private ArrayList<Gateway> getAllCutVertices() {
        ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
        ArrayList<Gateway> result = new ArrayList<Gateway>();
        allNodes.addAll(RelayNodeArray);
        for (int i = 0; i < allNodes.size(); i++) {
            Gateway gateway = allNodes.get(i);
            if (isCV(gateway)) {
                result.add(gateway);
            }
        }
        return result;
    }


    /**
     * Finds the number of all cutvertices
     * result[0] = number of terminal cut vertices
     * result[1] = number of relay cut vertices
     *
     * @return result
     */
    private int[] findNumberofAllCutVertices() {
        ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
        allNodes.addAll(RelayNodeArray);
        int tresult = 0;
        int rresult = 0;
        for (int i = 0; i < allNodes.size(); i++) {
            Gateway gateway = allNodes.get(i);
            if (isCV(gateway)) {
                if (gateway.isRelay()) {
                    rresult++;
                } else {
                    tresult++;
                }
            }
        }
        int[] result = new int[2];
        result[0] = tresult;
        result[1] = rresult;
        return result;
    }

    // DO not use, instead use isCV

    private boolean isCutVertex(Gateway rn) {
        ArrayList<Gateway> tmpRelay = new ArrayList<Gateway>();
        ArrayList<ArrayList<Gateway>> partitions;
        ArrayList<Gateway> modifiedNodes = new ArrayList<Gateway>();

        tmpRelay.addAll(ActorsArray);
        tmpRelay.addAll(RelayNodeArray);

        tmpRelay.remove(rn);

        for (int i = 0; i < tmpRelay.size(); i++) {
            Gateway gateway = tmpRelay.get(i);
            for (int j = 0; j < gateway.getNeighborList().size(); j++) {
                Gateway ne = gateway.getNeighborList().get(j);
                if (ne.isRelay && ne.getID() == rn.getID()) {
                    gateway.getNeighborList().remove(rn);
                    modifiedNodes.add(gateway);
                }
            }

        }

        partitions = NetworkUtils.DephtFirstSearch(tmpRelay);
        for (int i = 0; i < modifiedNodes.size(); i++) {
            modifiedNodes.get(i).getNeighborList().add(rn);
        }
        return partitions.size() != 1;
    }

    public double findAverageNodeDegreeOfRelayNodes() {
        double node_degree = 0;
        for (int i = 0; i < RelayNodeArray.size(); i++) {
            Gateway gateway = RelayNodeArray.get(i);
            node_degree += gateway.getNeighborList().size();
        }
        return node_degree / RelayNodeArray.size();
    }

    public void runExperiments(int[] in) {
        printHeader("TriangleResults/triangle.csv");
        printHeader("TriangleResults/ratio3.csv");
        printHeader("TriangleResults/SMST.csv");
        printHeader("TriangleResults/CORP.csv");
        printHeader("TriangleResults/klca.csv");

        int p = 9;
        double ar = 40;

        for (int i = 0; i < in.length; i++) {
            String filename = p + "_" + in[i] + ".dat";

            System.out.println("Partition : " + p + "\tExperiment :" + in[i]);

            reload(filename);
            runRatio3();
            report("TriangleResults/ratio3.csv", p, ar);


            reload(filename);
            runSMSTApproach();
            report("TriangleResults/SMST.csv", p, ar);

            reload(filename);
            runTriangleAproach();
            report("TriangleResults/triangle.csv", p, ar);

            //k-LCA (uses Sookyoungs's simulator's output)
            process_kLCAOutput(filename, p);
            report("TriangleResults/klca.csv", p, ar);

            //CORP (uses Sookyoungs's simulator's output)
            process_CORPOutput(filename);
            report("TriangleResults/CORP.csv", p, ar);

        }
    }


    public void startTourExperiment() {
        for (int p = 3; p <= 15; p++) {
            for (int e = 1; e <= 50; e++) {
                String filename = p + "_" + e + ".dat";
//                System.out.println(filename);
                reload(filename);
//                runMSTDTApproach();
                for (double percentage = 0.05; percentage <= 0.4; percentage += 0.05) {
                    System.out.println(filename + " started percenatge = " + percentage);
                    reload(filename);
                    analyzeMstEdgeMap(filename, percentage, false);
                    reportTourOutput(p, e, percentage, "TourResults/regular.csv");
//                    reload(filename);
//                    System.out.println(filename + " started percenatge = " + percentage);
//                    boolean bool = analyzeMstEdgeMap(filename, percentage, true);
//                    if (bool) {
//                        reportTourOutput(p, e, percentage, "TourResults/exponantial_15_49-50.csv");
//                    }
                }
            }
        }
        System.out.println("done");
    }

    public void startExperiment() {
        String path = "TriangleResults/revision/e2/200/";
        boolean printheader = false;
        if (printheader) {
//            printHeader(path + "mstdtmst.csv");
//            printHeader(path + "dtmstdt.csv");
//            printHeader(path + "dtmst.csv");

//            printHeader(path + "mstonly.csv");
//            printHeader(path + "dtonly.csv");
//            printHeader(path + "mstdt.csv");
            printHeader(path + "hybrid.csv");
//            printHeader(path + "festa.csv");
//            printHeader(path + "ratio3.csv");
//            printHeader(path + "smst.csv");
//            printHeader(path + "spider.csv");
        }

        long lStartTime = new Date().getTime();
//        int rp = 2;
//        int re = 1;
        String str = "";
        for (int p = 41; p <= 50; p++) {
            for (double ar = 45; ar <= 45; ar += 10) {
                for (int e = 1; e <= 100; e++) {
                    if ((p == 10 && e == 1) || (p == 11 && e == 21) || (p == 11 && e == 44) || (p == 12 && e == 45)||(p == 12 && e == 47)||
                            (p == 12 && e == 73)||(p == 12 && e == 75)||(p == 13 && e == 30)||(p == 13 && e == 51)||(p == 14 && e == 27)||(p == 14 && e == 41)
                            ||(p == 14 && e == 51)||(p == 14 && e == 61)||(p == 14 && e == 76)||(p == 15 && e == 9)||(p == 15 && e == 44)||(p == 15 && e == 56)
                            ||(p == 15 && e == 86)||(p == 15 && e == 7)||(p == 16 && e == 7)||(p == 16 && e == 32)||(p == 17 && e == 24)||(p == 18 && e == 8)
                            ||(p == 18 && e == 30)||(p == 18 && e == 1)||(p == 18 && e == 3)||(p == 18 && e == 41)||(p == 19 && e == 45)||(p == 19 && e == 79)
                            ||(p == 20 && e == 78)||(p == 20 && e == 81)||(p == 21 && e == 4)||(p == 21 && e == 56)||(p == 21 && e == 93)||(p == 22 && e == 77)
                            ||(p == 22 && e == 80)||(p == 23 && e == 5)||(p == 23 && e == 24)||(p == 23 && e == 61)||(p == 23 && e == 99)
                            ||(p == 24 && e == 5)||(p == 24 && e == 77)||(p == 24 && e == 99)||(p == 25 && e == 15)||(p == 25 && e == 70)||(p == 25 && e == 8)||(p == 25 && e == 99)
                            ||(p == 26 && e == 14)||(p == 26 && e == 64)||(p == 26 && e == 34)||(p == 26 && e == 66)||(p == 26 && e == 84)||(p == 26 && e == 93)
                            ||(p == 27 && e == 1)||(p == 27 && e == 8)||(p == 27 && e == 55)||(p == 27 && e == 56)||(p == 27 && e == 70)
                            ||(p == 28 && e == 17)||(p == 30 && e == 3)||(p == 30 && e == 8)||(p == 30 && e == 33)||(p == 30 && e == 34)||(p == 30 && e == 82)||(p == 30 && e == 84)
                            ||(p == 30 && e == 89)||(p == 30 && e == 44)||(p == 31 && e == 28)||(p == 32 && e == 11)||(p == 32 && e == 35)||(p == 32 && e == 39)
                            ||(p == 32 && e == 58)||(p == 32 && e == 68)||(p == 33 && e == 23)||(p == 33 && e == 35)||(p == 33 && e == 38)||(p == 33 && e == 47)
                            ||(p == 33 && e == 63)||(p == 33 && e == 60)||(p == 33 && e == 69)||(p == 34 && e == 1)||(p == 34 && e == 18)||(p == 34 && e == 30)
                            ||(p == 34 && e == 32)||(p == 34 && e == 34)||(p == 34 && e == 38)||(p == 34 && e == 54)||(p == 34 && e == 84)||(p == 34 && e == 50)
                            ||(p == 35 && e == 35)||(p == 35 && e == 49)||(p == 35 && e == 66)||(p == 36 && e == 30)||(p == 36 && e == 53)||(p == 36 && e == 59)
                            ||(p == 36 && e == 70)||(p == 36 && e == 77)||(p == 37 && e == 37)||(p == 38 && e == 10)||(p == 38 && e == 12)||(p == 38 && e == 22)
                            ||(p == 38 && e == 47)||(p == 39 && e == 29)||(p == 39 && e == 9)||(p == 40 && e == 45)||(p == 40 && e == 72)||(p == 40 && e == 80)||(p == 40 && e == 90)
                            ||(p == 41 && e == 31)||(p == 42 && e == 5)||(p == 42 && e == 15)||(p == 42 && e == 86)||(p == 42 && e == 66)||(p == 42 && e == 40)||(p == 42 && e == 82)
                            ||(p == 42 && e == 96)||(p == 42 && e == 64)||(p == 43 && e == 1)||(p == 43 && e == 3)||(p == 43 && e == 39)||(p == 43 && e == 47)
                            ||(p == 44 && e == 17)||(p == 44 && e == 23)||(p == 46 && e == 24)||(p == 46 && e == 38)||(p == 46 && e == 57)||(p == 46 && e == 96)
                            ||(p == 47 && e == 27)||(p == 47 && e == 28)||(p == 47 && e == 43)||(p == 47 && e == 45)||(p == 47 && e == 64)
                            ||(p == 48 && e == 42)||(p == 48 && e == 59)||(p == 48 && e == 85)||(p == 49 && e == 25)||(p == 50 && e == 90)||(p == 28 && e == 79)||(p == 34 && e == 61)||(p == 32 && e == 44)
                            ||(p == 41 && e == 60))
                        continue;

                    boolean first = true;
                    boolean ex = false;

                    while (first) {
                        String filename = p + "_" + e + ".dat";

                        System.out.println(input_data_path + (int) Constants.ActorTransmissionRange + "/" + filename);
                        first = false;
                        System.out.println("\nPartition : " + p + "\tExperiment :" + e);
//                        reload(filename);

                        try {

                            System.out.print("OTS-MST\t");
                            reload(filename);
                            runMSTTriangulationApproach(true);
                            int ots = RelayNodeArray.size();

                            System.out.print("IO-DT\t");
                            reload(filename);
                            runDelaunayTriangulationApproach(true);
                            int iodt = RelayNodeArray.size();

                            System.out.print("Hybrid\t");
                            runHybrid(filename);
                            int hybrid = RelayNodeArray.size();

//                            System.out.print("All Triangles\t");
//                            runAllTriangulationApproach(true);
//                            int all = RelayNodeArray.size();

                            System.out.print("SMST\t");
                            reload(filename);
                            runSMST2();
                            int smst = RelayNodeArray.size();

                            System.out.print("Ratio3\n");
                            reload(filename);
                            runRatio3();
                            int r3 = RelayNodeArray.size();

                            str += p + ";" + ots + ";" + iodt + ";" + hybrid + ";" + smst + ";" + r3 + "\n";
//                            System.out.println(p + ";" + ots + ";" + iodt + ";" + hybrid + ";" + smst + ";" + r3);

                            /*
                            System.out.print("OTS-MST\t");
                            reload(filename);
                            runMSTTriangulationApproach(true);
                            report(path + "mstonly.csv", p, ar);

                            System.out.print("IO-DT\t");
                            reload(filename);
                            runDelaunayTriangulationApproach(true);
                            report(path + "dtonly.csv", p, ar);
                            *//*

                            report(path + "hybrid.csv", p, ar);


                            System.out.print("SMST\t");
                            reload(filename);
                            runSMST2();
                            report(path + "smst.csv", p, ar);

                            System.out.print("Ratio3\t");
                            reload(filename);
                            runRatio3();
                            report(path + "ratio3.csv", p, ar);
                            */
 /*
                            System.out.print("Festa\t");
                            reload(filename);
                            runTriangleAproach3();
                            report(path + "festa.csv", p, ar);

                            System.out.print("1C-Spider\n");
                            reload(filename);
                            runSpiderWebApproach();
                            report(path + "spider.csv", p, ar);

                            System.out.print("MST-DT-MST\t");
                            reload(filename);
                            run_MST_DT_MST_Approach(true);
                            report(path + "mstdtmst.csv", p, ar);

                            System.out.print("DT-MST\t");
                            reload(filename);
                            runDTMSTApproach(true);
                            report(path + "dtmst.csv", p, ar);

                            System.out.print("DT-MST-DT\n");
                            reload(filename);
                            run_DT_MST_DT_Approach(true);
                            report(path + "dtmstdt.csv", p, ar);

                            System.out.print("Spider\t");
                            reload(filename);
                            runSpiderWebApproach();
                            find_max_distance_from_rn_to_terminal("TVT_revision/recoverytime/"+(int)Constants.RNTransmissionRange+"_Spider");
                            map_relays_to_segments("TVT_revision/Spider");
                            report("TVT_revision/spider.csv", p, ar);
                            report("TriangleResults/spider.csv", p, ar);


                            System.out.println("2C-SSG");
                            reload(filename);
                            runKConnectedMinimumSubgraphApproach();
                            find_max_distance_from_rn_to_terminal("TVT_revision/recoverytime/"+(int)Constants.RNTransmissionRange+"ssg");
*/
//                            report("TVT_revision/two_c_ssg.csv", p, ar);
//

//
//                            System.out.print("runDTMSTApproach\n");
//                            reload(filename);
//                            runMSTDTApproach();
//
//                            report("TriangleResults/dtmst.csv", p, ar);

//                            find_max_distance_from_rn_to_terminal("TVT_revision/recoverytime/"+(int)Constants.RNTransmissionRange+"_ratio3");


//                            report("TVT_revision/ratio3.csv", p, ar);

//                            map_relays_to_segments("TVT_revision/ratio3");
//                            report("TriangleResults/ratio3.csv", p, ar);
//                            System.out.println("FermatPoint");
//                            reload(filename);
//                            testFermatPoint();
                            /*
                         System.out.print("SMST\n");
                         reload(filename);
                         runSMST2();
                         find_max_distance_from_rn_to_terminal("TVT_revision/recoverytime/"+(int)Constants.RNTransmissionRange+"_smst");
                            */
//                            report("TriangleResults/osmst2.csv", p, ar);

//                            map_relays_to_segments("TriangleResults/smst");

                            //System.out.println("");
//                            report("TriangleResults/SMST.csv", p, ar);

//                            System.out.print("Triangle3\t");
//                            reload(filename);
//                            runTriangleAproach3();
//                            report("TriangleResults/triangle3.csv", p, ar);

                            /*
                            System.out.print("Dynamic DT_Triangle\t");
                            reload(filename);
                            runDynamicallyGrowingTriangleApproach();
                            report("TriangleResults/dynamictriangle.csv", p, ar);
                            
                            //k-LCA (uses Sookyoungs's simulator's output)

                            System.out.print("klca\n");
                            process_kLCAOutput(filename, p);
                            report("TriangleResults/klca.csv", p, ar);
                             */
                            /*
                            //CORP (uses Sookyoungs's simulator's output)
                            System.out.print("corp\n");
                            process_CORPOutput(filename);
                            report("TriangleResults/CORP.csv", p, ar);
                            */

                        } catch (Exception exception) {
                            exception.printStackTrace();
                            ex = true;
//                            fix(p, e);
                            System.out.println(filename + " exception");
                            System.exit(0);
                        }

                        ex = false;
                    }

                }
            }
        }
        System.out.println("*****************");
        System.out.println(str);
        try {
            PrintWriter pw = new PrintWriter(new FileWriter("C:\\Users\\itg\\Desktop\\jnca test\\p41_50_r" + (int) Constants.ActorTransmissionRange + ".csv"));
            pw.print(str);
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        long lEndTime = new Date().getTime(); //end time
        long difference = lEndTime - lStartTime; //check different
        System.out.println("Time Elapsed : " + difference);
    }

    public void create50Samples() {

        for (int p = 3; p <= 4; p++) {
            create50Samples(p);
        }
        System.out.println("Done");
    }

    //todo tobe deleted after the experiments

    public void fix(int numOfPartitions, int experiment) {
        // i is from 1 to 50
        for (int i = experiment; i <= experiment; i++) {
            boolean first = true;
            boolean redo = false;
            boolean ex = false;
            while (first || redo) {

                if (redo)
//                    System.out.println("Redo : ");
                    first = false;
                try {
                    generateSegments(numOfPartitions, 1, 1);
                    ArrayList<ArrayList<Gateway>> partitions = NetworkUtils.DephtFirstSearch(ActorsArray);
                    if (partitions.size() != numOfPartitions)
                        ex = true;
                } catch (ArrayIndexOutOfBoundsException exception) {
                    ex = true;
                }
                if (!ex) {

                    String filename = numOfPartitions + "_" + i + ".dat";
                    save(filename);
                }
                redo = ex;
                ex = false;
            }
        }
    }

    public void create50Samples(int numOfPartitions) {
        // i is from 1 to 50
        for (int i = 1; i <= 100; i++) {
            boolean first = true;
            boolean redo = false;
            boolean ex = false;
            while (first || redo) {

                if (redo)
                    System.out.println("Redo : ");
                first = false;
                try {
                    generateSegments(numOfPartitions, 1, 2);
                    ArrayList<ArrayList<Gateway>> partitions = NetworkUtils.DephtFirstSearch(ActorsArray);
                    if (partitions.size() != numOfPartitions)
                        ex = true;
                } catch (ArrayIndexOutOfBoundsException exception) {
                    ex = true;
                }
                if (!ex) {

                    String filename = numOfPartitions + "_" + i + ".dat";
                    save(filename);
                }
                redo = ex;
                ex = false;
            }
        }
    }

    private void printHeader(String filename) {
        try {

            PrintWriter pw = new PrintWriter(new FileWriter(new File(filename), true));

            pw.print("Number Of Partitions;");
//            pw.print("Sensing Range,");
            pw.print("Number Of Relay Nodes;");
            pw.print("Number Of Terminal Cut Vertices;");
            pw.print("Number Of Relay Cut Vertices;");
            pw.print("Average Node Degree;");
            pw.print("Average Distance;");
            pw.print("MaximumDistance;");
            pw.println("Coverage");

            pw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private int runRatio3Step1(ArrayList<Gateway> NodeList, double R, int nextID) {
        for (int i = 0; i < NodeList.size() - 1; i++) {
            Gateway a = NodeList.get(i);
            for (int j = i + 1; j < NodeList.size(); j++) {
                Gateway b = NodeList.get(j);
                if (NetworkUtils.Distance(a, b) <= R) {
                    if (a.getMstID() == 0 && b.getMstID() == 0) {
                        a.setMstID(nextID);
                        b.setMstID(nextID);
                        nextID++;
                    } else if (a.getMstID() != 0 && b.getMstID() == 0) {
                        b.setMstID(a.getMstID());
                    } else if (b.getMstID() != 0 && a.getMstID() == 0) {
                        a.setMstID(b.getMstID());
                    } else {
                        int max = Math.max(a.getMstID(), b.getMstID());
                        for (int k = 0; k < NodeList.size(); k++) {
                            Gateway gateway = NodeList.get(k);
                            if (gateway.getMstID() == a.getMstID() || gateway.getMstID() == b.getMstID()) {
                                gateway.setMstID(max);
                            }
                        }
                    }
                }

            }
        }
        return nextID;
    }

    public int runRatio3Step2(ArrayList<Gateway> NodeList, double R, int nextID) {

//        int nextID = 1;
        for (int i = 0; i < NodeList.size() - 2; i++) {
            Gateway a = NodeList.get(i);
            for (int j = i + 1; j < NodeList.size() - 1; j++) {
                Gateway b = NodeList.get(j);
//                if (((a.getMstID() == 0 && b.getMstID() == 0)) || (a.getMstID() != b.getMstID())) {
                for (int k = j + 1; k < NodeList.size(); k++) {
                    Gateway c = NodeList.get(k);
                    boolean NotallEq = !(b.getMstID() == a.getMstID() && c.getMstID() == a.getMstID());
                    if (((a.getMstID() == 0 && b.getMstID() == 0 && c.getMstID() == 0)) || NotallEq) {
                        Point2D p = NetworkUtils.check3Star(a, b, c, R);
                        if (p != null) {
                            // 3-star found
                            if (a.getMstID() == 0 && b.getMstID() == 0 && c.getMstID() == 0) {
                                a.setMstID(nextID);
                                b.setMstID(nextID);
                                c.setMstID(nextID);
                                Gateway rn = new Gateway(RelayNodeArray.size());
                                rn.setX(p.getX());
                                rn.setY(p.getY());
                                rn.isRelay = true;
                                RelayNodeArray.add(rn);
                                nextID++;
                            } else {
                                Gateway rn = new Gateway(RelayNodeArray.size());
                                rn.setX(p.getX());
                                rn.setY(p.getY());
                                rn.isRelay = true;
                                RelayNodeArray.add(rn);
//                                    nextID++;
                                int max = Math.max(Math.max(a.getMstID(), b.getMstID()), c.getMstID());
                                if (a.getMstID() == 0)
                                    a.setMstID(max);
                                if (b.getMstID() == 0)
                                    b.setMstID(max);
                                if (c.getMstID() == 0)
                                    c.setMstID(max);
                                int prevA = a.getMstID();
                                int prevB = b.getMstID();
                                int prevC = c.getMstID();
                                for (int l = 0; l < NodeList.size(); l++) {
                                    Gateway gateway = NodeList.get(l);
                                    if (gateway.getMstID() == prevA || gateway.getMstID() == prevB || gateway.getMstID() == prevC) {
                                        gateway.setMstID(max);
                                    }
                                }
                            }
                        }
                    }
                }
//                }
            }
        }
        return nextID;

    }

    private void runRatio3Step3(ArrayList<Gateway> NodeList, double R, int nextID, ArrayList<Edge> sortedEdges) {
        for (Edge edge : sortedEdges) {
            Gateway a = edge.u;
            Gateway b = edge.v;
            if ((a.getMstID() == 0 && b.getMstID() == 0) || (a.getMstID() != b.getMstID())) {
                NetworkUtils.fillGap(a, b, true, ActorsArray, RelayNodeArray);
                if (a.getMstID() == 0 && b.getMstID() == 0) {
                    a.setMstID(nextID);
                    b.setMstID(nextID);
                    nextID++;
                } else if (a.getMstID() != 0 && b.getMstID() == 0) {
                    b.setMstID(a.getMstID());
                } else if (b.getMstID() != 0 && a.getMstID() == 0) {
                    a.setMstID(b.getMstID());
                } else {
                    int max = Math.max(a.getMstID(), b.getMstID());
                    int min = Math.min(a.getMstID(), b.getMstID());
                    for (int k = 0; k < NodeList.size(); k++) {
                        Gateway gateway = NodeList.get(k);
                        if (gateway.getMstID() == min) {
                            gateway.setMstID(max);
                        }
                    }
                }
            }
        }
    }

    public void runRatio3() {
        RelayNodeArray.clear();
//        HashMap<String, Double> unsortedEdgeMap = new HashMap<String, Double>();
        ArrayList<Edge> allEdges = new ArrayList<Edge>();
        for (int i = 0; i < ActorsArray.size(); i++) {
            Gateway gateway = ActorsArray.get(i);
            gateway.setMstID(0);
        }
        for (int i = 0; i < ActorsArray.size(); i++) {
            Gateway g1 = ActorsArray.get(i);
            for (int j = i; j < ActorsArray.size(); j++) {
                if (i != j) {
                    Gateway g2 = ActorsArray.get(j);
//                    unsortedEdgeMap.put(i + "->" + j, NetworkUtils.Distance(g1, g2));
                    allEdges.add(new Edge(g1, g2));
                }
            }
        }
//        HashMap<String, Double> sortedEdgeMap = NetworkUtils.sortHashMap(unsortedEdgeMap);
        Collections.sort(allEdges);
        int nextID = 1;
        nextID = runRatio3Step1(ActorsArray, Constants.RNTransmissionRange, nextID);
        nextID = runRatio3Step2(ActorsArray, Constants.RNTransmissionRange, nextID);
        runRatio3Step3(ActorsArray, Constants.RNTransmissionRange, nextID, allEdges);

        ArrayList<Gateway> allNodes = new ArrayList<Gateway>(RelayNodeArray);
        allNodes.addAll(ActorsArray);
        NetworkUtils.calculateActorNeighborhoods(allNodes, Constants.RNTransmissionRange);
        NetworkUtils.attachRelayWithGateways(ActorsArray, RelayNodeArray);
    }

    private void classifyRelayNodes() {
        for (int i = 0; i < lines.length; i++) {
            Line currentLine = lines[i];
            Line left = lines[(i - 1) < 0 ? i - 1 + lines.length : i - 1];
            Line right = lines[(i + 1) % lines.length];
            int prevLabel = -1;

            for (int j = 0; j < currentLine.nodeList.size(); j++) {
                Gateway RN = currentLine.nodeList.get(j);
                boolean alreadyLabelled = false;
                if (RN.getLabels().size() == 1 && RN.getLabels().get(0) == Constants.RING) {
                    prevLabel = Constants.RING;
                    alreadyLabelled = true;
                }
                if (!alreadyLabelled) {
                    boolean lc = false, rc = false;
                    for (int k = 0; k < left.nodeList.size(); k++) {
                        Gateway leftRN = left.nodeList.get(k);
                        if (NetworkUtils.Distance(RN, leftRN) <= Constants.RNTransmissionRange) {
                            lc = true;
                            break;
                        }
                    }

                    for (int k = 0; k < right.nodeList.size(); k++) {
                        Gateway rightRN = right.nodeList.get(k);
                        if (NetworkUtils.Distance(RN, rightRN) <= Constants.RNTransmissionRange) {
                            rc = true;
                            break;
                        }
                    }

                    if (lc || rc) {
                        if (RN.getLabels().isEmpty()) {
                            if (prevLabel == Constants.FIRST || prevLabel == Constants.CUT) {
                                RN.addLabel(Constants.RING_ENTRANCE);
                                prevLabel = Constants.RING_ENTRANCE;
                            } else if (prevLabel == Constants.RING_ENTRANCE || prevLabel == Constants.RING) {
                                RN.addLabel(Constants.RING);
                                RN.setTerminal(null);
                                prevLabel = Constants.RING;
                            }
                        } else {
                            if (prevLabel == -1) {
                                RN.removeFirstLabel();
                                RN.addLabel(Constants.RING);
                                prevLabel = Constants.RING;
                            }
                        }
                    } else {
                        if (RN.getLabels().isEmpty()) {
                            RN.addLabel(Constants.CUT);
                            prevLabel = Constants.CUT;
                        } else {
                            prevLabel = Constants.FIRST;
                        }
                    }
                }
            }
        }

//        for (int i = 0; i < RelayNodeArray.size(); i++) {
//            Gateway gateway = RelayNodeArray.get(i);
//            if (!gateway.getLabels().isEmpty())
//                System.out.println("Node id : " + gateway.getID() + "\tLabel : " + gateway.getLabels().get(0) + "\tLine id : " + gateway.lineID);
//            else
//                System.out.println("Error -> Node ID : " + gateway.getID());
//        }
    }

    private void make2Connected() {
        // List of the relay nodes which are labelled with RING_ENTRANCE and FIRST

        ArrayList<Gateway> listOfRingEntrances = new ArrayList<Gateway>();
        ArrayList<Gateway> listOfFirst = new ArrayList<Gateway>();
        for (int i = 0; i < RelayNodeArray.size(); i++) {
            Gateway gateway = RelayNodeArray.get(i);
            if (gateway.getLabels().size() == 1) {
                if (gateway.getLabels().get(0) == Constants.RING_ENTRANCE) {
                    listOfRingEntrances.add(gateway);
                }
            }
        }
        for (int i = 0; i < listOfRingEntrances.size(); i++) {
            Gateway gateway = listOfRingEntrances.get(i);
            for (int j = 0; j < RelayNodeArray.size(); j++) {
                Gateway g1 = RelayNodeArray.get(j);
                if (gateway.lineID == g1.lineID && g1.getLabels().get(0) == Constants.FIRST) {
                    listOfFirst.add(g1);
                }
            }
        }
        if (!listOfRingEntrances.isEmpty()) {


            for (int j = 0; j < listOfRingEntrances.size(); j++) {
                Gateway re = listOfRingEntrances.get(j);

                int minIndex1 = -1;
                double minDist1 = -1;
                for (int i = 0; i < RelayNodeArray.size(); i++) {
                    Gateway rn = RelayNodeArray.get(i);
                    if (rn.getID() != re.getID()) {
                        double d = NetworkUtils.Distance(re, rn);
                        if ((!rn.getLabels().isEmpty()) && (rn.getLabels().get(0) == Constants.RING || rn.getLabels().get(0) == Constants.RING_ENTRANCE)) {
                            if (minIndex1 == -1) {
                                minIndex1 = i;
                                minDist1 = NetworkUtils.Distance(re, rn);
                            } else if (d < minDist1) {
                                minDist1 = d;
                                minIndex1 = i;
                            }
                        }
                    }
                }

                Gateway n1 = RelayNodeArray.get(minIndex1);
                Gateway f = listOfFirst.get(j);
                if (f.getTerminal() != null) {
                    NetworkUtils.fillGap(n1, f.getTerminal(), true, ActorsArray, RelayNodeArray);
                }
                Gateway last = RelayNodeArray.get(RelayNodeArray.size() - 1);
                if (NetworkUtils.Distance(last, f) > Constants.RNTransmissionRange) {
                    NetworkUtils.fillGap(last, f, true, ActorsArray, RelayNodeArray);
                }
            }
        }
    }


    private void make2Connected2() {
        // List of the relay nodes which are labelled with RING_ENTRANCE and FIRST

        ArrayList<Gateway> listOfRingEntrances = new ArrayList<Gateway>();
        ArrayList<Gateway> listOfFirst = new ArrayList<Gateway>();
        for (int i = 0; i < RelayNodeArray.size(); i++) {
            Gateway gateway = RelayNodeArray.get(i);
            if (gateway.getLabels().size() == 1) {
                if (gateway.getLabels().get(0) == Constants.RING_ENTRANCE) {
                    listOfRingEntrances.add(gateway);
                }
            }
        }
        for (int i = 0; i < listOfRingEntrances.size(); i++) {
            Gateway gateway = listOfRingEntrances.get(i);
            for (int j = 0; j < RelayNodeArray.size(); j++) {
                Gateway g1 = RelayNodeArray.get(j);
                if (gateway.lineID == g1.lineID && g1.getLabels().get(0) == Constants.FIRST) {
                    listOfFirst.add(g1);
                }
            }
        }
        if (!listOfRingEntrances.isEmpty()) {


            for (int j = 0; j < listOfRingEntrances.size(); j++) {
                Gateway re = listOfRingEntrances.get(j);
                Gateway f = listOfFirst.get(j);
                Gateway t = f.getTerminal();
                if (t != null) {
                    int minIndex1 = -1;
                    double minDist1 = -1;
                    for (int i = 0; i < RelayNodeArray.size(); i++) {
                        Gateway rn = RelayNodeArray.get(i);
                        if (rn.getID() != re.getID()) {
                            double d = NetworkUtils.Distance(t, rn);
                            if ((!rn.getLabels().isEmpty()) && (rn.getLabels().get(0) == Constants.RING || rn.getLabels().get(0) == Constants.RING_ENTRANCE)) {
                                if (minIndex1 == -1) {
                                    minIndex1 = i;
                                    minDist1 = NetworkUtils.Distance(re, rn);
                                } else if (d < minDist1) {
                                    minDist1 = d;
                                    minIndex1 = i;
                                }
                            }
                        }
                    }

                    Gateway n1 = RelayNodeArray.get(minIndex1);

                    NetworkUtils.fillGap(n1, t, true, ActorsArray, RelayNodeArray);
                }
            }
        }
    }

    /**
     * This Approach is developped by Abhishek Kashyap
     * It forms minimum k-connected spanning subgraph (G=(V,E)). In this project k=2
     * Then it steinerizes the edges in E
     */
    public void runKConnectedMinimumSubgraphApproach() {
        KConnectedGraph kcssg = new KConnectedGraph(ActorsArray, 2);
        double[][] coordinates = kcssg.getXycoordAllVertices();
        RelayNodeArray.clear();
        for (int i = 0; i < coordinates.length; i++) {
            Gateway rn = new Gateway(RelayNodeArray.size());
            rn.isRelay = true;
            rn.setX(coordinates[i][0]);
            rn.setY(coordinates[i][1]);
            RelayNodeArray.add(rn);
        }
        ArrayList<Gateway> cutVertices = getAllCutVertices();
        for (int i = 0; i < cutVertices.size(); i++) {
            Gateway cv = cutVertices.get(i);
            Gateway relay = cv.cloneGateway();
            relay.setId(RelayNodeArray.size());
            relay.isRelay = true;
            RelayNodeArray.add(relay);

        }
        NetworkUtils.calculateActorNeighborhoods(RelayNodeArray, Constants.RNTransmissionRange);
        NetworkUtils.attachRelayWithGateways(ActorsArray, RelayNodeArray);
    }

    // Started 16 April 2010 after IEEE TVT submission

    private class DeploymentDirection {
        Gateway source;
        ArrayList<Point2D> HPPoints = new ArrayList<Point2D>();
        ArrayList<Point2D> VPPoints = new ArrayList<Point2D>();
        Point2D centroid;

        public DeploymentDirection(Gateway source) {
            this.source = source;
        }

        public void addHPPoint(Point2D p) {
            HPPoints.add(p);
        }

        public void addVPPoint(Point2D p) {
            VPPoints.add(p);
        }

        public void calculateCentroid() {
            double xsum = 0, ysum = 0;
            int n = HPPoints.size();
            for (int i = 0; i < n; i++) {
                Point2D hp = HPPoints.get(i);
                xsum += hp.getX();
                ysum += hp.getY();

                Point2D vp = VPPoints.get(i);
                xsum += vp.getX();
                ysum += vp.getY();
            }


            xsum /= 2 * n;
            ysum /= 2 * n;
            ArrayList<Point2D> PPoints = new ArrayList<Point2D>();
            for (int i = 0; i < n; i++) {
                Point2D hp = HPPoints.get(i);
                Point2D vp = VPPoints.get(i);
                if (AnalyticGeometry.euclideanDistance(xsum, ysum, hp.getX(), hp.getY()) < AnalyticGeometry.euclideanDistance(xsum, ysum, vp.getX(), vp.getY())) {
                    PPoints.add(hp);
                } else {
                    PPoints.add(vp);
                }
            }
            xsum = 0;
            ysum = 0;
            n = PPoints.size();
            for (int i = 0; i < n; i++) {
                Point2D p = PPoints.get(i);
                xsum += p.getX();
                ysum += p.getY();
            }
            xsum /= n;
            ysum /= n;
            centroid = new Point2D.Double();
            centroid.setLocation(xsum, ysum);
        }
    }

    public void calculateDeploymentDirection() {
        calculateDeploymentDirection(ActorsArray);
    }

    public void calculateDeploymentDirection(ArrayList<Gateway> NodeList) {
//        ArrayList<DeploymentDirection> directions = new ArrayList<DeploymentDirection>();
        HashMap<Gateway, Gateway> map = new HashMap<Gateway, Gateway>();
        Point2D p;
        for (int i = 0; i < NodeList.size(); i++) {
            Gateway gi = NodeList.get(i);
            DeploymentDirection dir = new DeploymentDirection(gi);
            for (int j = 0; j < NodeList.size(); j++) {
                if (i != j) {
                    Gateway gj = NodeList.get(j);
                    p = new Point2D.Double();
                    p.setLocation(gi.getX(), gj.getY());
                    dir.addVPPoint(p);
                    p = new Point2D.Double();
                    p.setLocation(gj.getX(), gi.getY());
                    dir.addHPPoint(p);
                }
            }
            dir.calculateCentroid();

            Gateway tmp = new Gateway(dir.source.getID());
            tmp.setX(dir.centroid.getX());
            tmp.setY(dir.centroid.getY());
            tmp.isRelay = true;
            RelayNodeArray.add(tmp);
            map.put(gi, tmp);
//            directions.add(dir);

        }
        NetworkUtils.calculateActorNeighborhoods(RelayNodeArray, Constants.RNTransmissionRange);
        NetworkUtils.attachRelayWithGateways(ActorsArray, RelayNodeArray);
    }

    // 24 April 2010 DT_Triangle Approach

    //private class DT_Triangle implements Comparable

    public void deactivateAdjacencies(ArrayList<Triangle> listOfTriangles, Triangle t) {
        for (int i = 0; i < listOfTriangles.size(); i++) {
            Triangle ti = listOfTriangles.get(i);
            if (t.isAdjacent(ti)) {
                ti.setActive(false);
            }
        }
    }

    public void runTriangleAproach() {
        TriangleApproch(ActorsArray);
    }

    public void TriangleApproch(ArrayList<Gateway> NodeList) {

        boolean newversion = true;

        HashSet<Gateway> allCoveredNodes = new HashSet<Gateway>();
        ArrayList<Triangle> list3UncoveredTriangles = new ArrayList<Triangle>();
        ArrayList<Triangle> list2UncoveredTriangles = new ArrayList<Triangle>();
        ArrayList<Triangle> list1UncoveredTriangles = new ArrayList<Triangle>();

        int nextCCID = 1;
        ArrayList<Edge> mst_of_terminals = NetworkUtils.runKruskal(NodeList);

        for (int i = 0; i < NodeList.size() - 2; i++) {
            Gateway si = NodeList.get(i);
            for (int j = i + 1; j < NodeList.size() - 1; j++) {
                Gateway sj = NodeList.get(j);
                for (int k = j + 1; k < NodeList.size(); k++) {
                    Gateway sk = NodeList.get(k);
                    Triangle t = new Triangle(si, sj, sk);
                    list3UncoveredTriangles.add(t);
                }
            }
        }
        Collections.sort(list3UncoveredTriangles);

        ArrayList<Triangle> threeStars = new ArrayList<Triangle>();
        for (int i = 0; i < list3UncoveredTriangles.size(); i++) {
            Triangle triangle = list3UncoveredTriangles.get(i);
            if (triangle.rho == 1) {
                threeStars.add(triangle);
            } else {
                break;
            }
        }

        threeStars = eliminateEquivalent3Stars(threeStars);

        for (int i = 0; i < threeStars.size(); i++) {
            Triangle triangle = threeStars.get(i);
            int cid1 = triangle.s1.getCcid();
            int cid2 = triangle.s2.getCcid();
            int cid3 = triangle.s3.getCcid();
            if (!(cid1 == cid2 && cid1 == cid3 && cid1 != 0 && cid2 != 0 && cid3 != 0)) {
                triangle.steinerizeTriangle(ActorsArray, RelayNodeArray);
                rearrangeLists(triangle, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);
            }
        }

        while (!list3UncoveredTriangles.isEmpty()) {
//            DT_Triangle ti = list3UncoveredTriangles.get(0);
            Triangle ti = getBestCandidateTriangle(list3UncoveredTriangles, allCoveredNodes);
            int N1 = ti.rho;


            int N1_prime = Integer.MAX_VALUE;
//            if (!allCoveredNodes.isEmpty()) {
//                ArrayList<Pair> edges_to_connect_triangle = findPairsOfNodes(ti, allCoveredNodes);
//                for (int i = 0; i < edges_to_connect_triangle.size(); i++) {
//                    Pair pairOfNodes = edges_to_connect_triangle.get(i);
////                double d = NetworkUtils.EstimatedDistance(pairOfNodes[0], pairOfNodes[1]);
////                int c = ((int) Math.ceil(d / Constants.RNTransmissionRange) - 1);
//                    if (pairOfNodes.weight < N1_prime) {
//                        N1_prime = pairOfNodes.weight;
//                    }
//                }
//
//                N1 += N1_prime;
//            }

            for (int i = 0; i < mst_of_terminals.size(); i++) {
                Edge edge = mst_of_terminals.get(i);

                Gateway u = edge.u;
                Gateway v = edge.v;
                if (((u == ti.s1 || u == ti.s2 || u == ti.s3) && (v != ti.s1 && v != ti.s2 && v != ti.s3)) || ((v == ti.s1 || v == ti.s2 || v == ti.s3) && (u != ti.s1 && u != ti.s2 && u != ti.s3))) {
                    if (edge.weight < N1_prime) {
                        N1_prime = edge.weight;
                    }
                }
            }
            N1 += N1_prime;


            TriangleAndPairOfNodes bestTriangleAndPair = findBestTriangleAndPairOfNodes(ti, list2UncoveredTriangles, allCoveredNodes);
            int N2, N2_prime = Integer.MAX_VALUE;
            if (bestTriangleAndPair.triangle == null /*|| bestTriangleAndPair.pair == null*/) {
                N2 = Integer.MAX_VALUE;
            } else {
                if (newversion) {

//                    N2_prime = bestTriangleAndPair.weightOfEdge;

                    Gateway remainingUncoveredNode = bestTriangleAndPair.pair[0];
                    for (int i = 0; i < mst_of_terminals.size(); i++) {
                        Edge edge = mst_of_terminals.get(i);

                        Gateway u = edge.u;
                        Gateway v = edge.v;
                        if (u == remainingUncoveredNode || v == remainingUncoveredNode) {
                            if (edge.weight < N2_prime) {
                                N2_prime = edge.weight;
                            }
                        }
                    }
                    N2 = bestTriangleAndPair.weightOfTriangle + N2_prime;

                } else {
                    N2 = bestTriangleAndPair.weight;
                }
            }
            HashSet<Gateway> tmpAllCoveredNodes = new HashSet<Gateway>(allCoveredNodes);
            ArrayList<Gateway> uncoveredterminals = ti.getUncoveredNodes();
            ArrayList<Gateway[]> oneUncovered = findPairsOfNodes(uncoveredterminals, tmpAllCoveredNodes);
            int counter = 1;

            int N3;
            if (oneUncovered == null || oneUncovered.size() < 2) {
                N3 = Integer.MAX_VALUE;
            } else {
                N3 = 0;

                while (counter <= 3) {


                    ArrayList<Edge> edges = new ArrayList<Edge>();
                    for (int i = 0; i < oneUncovered.size(); i++) {
                        Gateway[] pairs = oneUncovered.get(i);
                        if (pairs != null && pairs[0] != null && pairs[1] != null) {
                            edges.add(new Edge(pairs[0], pairs[1]));
                        }
                    }
                    Collections.sort(edges);
                    Integer w = edges.get(0).weight;
                    N3 += w;
                    Gateway[] pairs = edges.get(0).getNodes();
                    tmpAllCoveredNodes.add(pairs[0]);
                    uncoveredterminals.remove(pairs[0]);
                    oneUncovered = findPairsOfNodes(uncoveredterminals, tmpAllCoveredNodes);

                    counter++;

                }
            }


            int min = Math.min(N1, Math.min(N2, N3));


            if (N1 == min && N1 == N2) {
                if (N1 - N1_prime <= N2 - N2_prime) {
                    N1 = N1 - N1_prime;
                } else {
                    N2 = N2 - N2_prime;
                }
            }
            min = Math.min(N1, Math.min(N2, N3));
            if (N2 == min) {
                //go with twoUncovered array


                if (bestTriangleAndPair.triangle != null /*&& bestTriangleAndPair.pair != null*/) {

                    //new version
                    if (newversion) {
                        int r2_p = 0;

                        ArrayList<Gateway> coveredNodes = bestTriangleAndPair.triangle.getCoveredNodes();
                        ArrayList<Gateway> uncoveredNodes = bestTriangleAndPair.triangle.getUncoveredNodes();
                        ArrayList<Gateway[]> pairs = findPairsOfNodes(uncoveredNodes, allCoveredNodes);
                        for (int i = 0; i < pairs.size(); i++) {
                            Gateway[] gateways = pairs.get(i);
                            if (gateways != null && gateways[0] != null && gateways[1] != null) {
                                double d = NetworkUtils.EstimatedDistance(gateways[0], gateways[1]);
                                r2_p += ((int) Math.ceil(d / Constants.RNTransmissionRange) - 1);
                            }
                        }

                        r2_p += (int) (Math.ceil(NetworkUtils.EstimatedDistance(bestTriangleAndPair.pair[0], bestTriangleAndPair.pair[1]) / Constants.RNTransmissionRange) - 1);
                        if (N2 <= r2_p) {
                            bestTriangleAndPair.triangle.steinerizeTriangle(ActorsArray, RelayNodeArray);
                        } else {

                            for (int i = 0; i < pairs.size(); i++) {
                                Gateway[] gateways = pairs.get(i);
                                if (gateways != null && gateways[0] != null && gateways[1] != null) {
                                    int ccid = Math.max(gateways[0].getCcid(), gateways[1].getCcid());
                                    gateways[0].setCcid(ccid);
                                    gateways[1].setCcid(ccid);
                                    NetworkUtils.fillGap(gateways[0], gateways[1], true, ccid, ActorsArray, RelayNodeArray);
                                    double d = NetworkUtils.EstimatedDistance(gateways[0], gateways[1]);

                                }
                            }


                        }
                        rearrangeLists(bestTriangleAndPair.triangle, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);

                    } else {

                        // old version begin
                        if (Constants.LOG_TRIANGLE) {
                            System.out.println("T : [" + bestTriangleAndPair.triangle.s1 + ", " + bestTriangleAndPair.triangle.s2 + ", " + bestTriangleAndPair.triangle.s3 + "]\nPair : [" + bestTriangleAndPair.pair[0] + ", " + bestTriangleAndPair.pair[1] + "]");
                            System.out.println("-----------------------------------");
                        }
                        bestTriangleAndPair.triangle.steinerizeTriangle(ActorsArray, RelayNodeArray);
                        ArrayList<Gateway> coveredNodes = bestTriangleAndPair.triangle.getCoveredNodes();
                        ArrayList<Gateway> uncoveredNodes = bestTriangleAndPair.triangle.getUncoveredNodes();
                        if (coveredNodes.size() == 1) {
                            for (int i = 0; i < uncoveredNodes.size(); i++) {
                                uncoveredNodes.get(i).setCcid(coveredNodes.get(0).getCcid());
                            }
                        } else {
                            if (Constants.LOG_TRIANGLE) {
                                System.out.println("ERROR in R2 " + ti);
                            }
                        }

                        NetworkUtils.fillGap(bestTriangleAndPair.pair[0], bestTriangleAndPair.pair[1], true, ActorsArray, RelayNodeArray);
                        if (bestTriangleAndPair.pair[0].getCcid() == 0 && bestTriangleAndPair.pair[1].getCcid() == 0) {
                            bestTriangleAndPair.pair[0].setCcid(nextCCID);
                            bestTriangleAndPair.pair[1].setCcid(nextCCID);
                            nextCCID++;
                        } else if (bestTriangleAndPair.pair[0].getCcid() == 0) {
                            bestTriangleAndPair.pair[0].setCcid(bestTriangleAndPair.pair[1].getCcid());
                        } else if (bestTriangleAndPair.pair[1].getCcid() == 0) {
                            bestTriangleAndPair.pair[1].setCcid(bestTriangleAndPair.pair[0].getCcid());
                        } else {
                            if (Constants.LOG_TRIANGLE) {
                                System.out.println("ERROR in R2 " + ti);
                            }
                        }
                        rearrangeLists(ti, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);
                        // old version end

                    }
                }
            } else if (N3 == min) {

                tmpAllCoveredNodes = new HashSet<Gateway>(allCoveredNodes);
                uncoveredterminals = ti.getUncoveredNodes();
                oneUncovered = findPairsOfNodes(uncoveredterminals, tmpAllCoveredNodes);


                ArrayList<Edge> edges = new ArrayList<Edge>();
                for (int i = 0; i < oneUncovered.size(); i++) {
                    Gateway[] pairs = oneUncovered.get(i);
                    if (pairs != null && pairs[0] != null && pairs[1] != null) {
                        edges.add(new Edge(pairs[0], pairs[1]));
                    }
                }
                Collections.sort(edges);
                Integer w = edges.get(0).weight;
                N3 += w;
                Gateway[] pairs = edges.get(0).getNodes();
                int ccid = pairs[1].getCcid();
                pairs[0].setCcid(ccid);
                NetworkUtils.fillGap(pairs[0], pairs[1], true, ccid, ActorsArray, RelayNodeArray);
                updateLists(list3UncoveredTriangles, list2UncoveredTriangles, list1UncoveredTriangles, allCoveredNodes, pairs[0]);


            } else if (N1 == min) {
                // go with ti
                ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                if (Constants.LOG_TRIANGLE) {
                    System.out.println("R1 : " + ti);
                }
                rearrangeLists(ti, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);

            }

        }

        Collections.sort(list2UncoveredTriangles);
        while (!list2UncoveredTriangles.isEmpty()) {
            Triangle ti = list2UncoveredTriangles.get(0);
            int r1 = ti.rho;
            ArrayList<Gateway> uncovered = ti.getUncoveredNodes();
            double distanceBetweenUncoveredNodes = NetworkUtils.EstimatedDistance(uncovered.get(0), uncovered.get(1));
            //uc stands for uncovered-covered pairs
            Gateway[][] ucpairs = new Gateway[2][2];
            int[] ucweights = new int[2];
            for (int i = 0; i < uncovered.size(); i++) {
                Gateway uncoveredNode = uncovered.get(i);
                double minDist = Double.MAX_VALUE;
                Gateway bestMatch = null;
                for (Gateway g : allCoveredNodes) {
                    double d = NetworkUtils.EstimatedDistance(uncoveredNode, g);
                    if (d <= minDist) {
                        minDist = d;
                        bestMatch = g;
                    }
                }
                ucpairs[i][0] = uncoveredNode;
                ucpairs[i][1] = bestMatch;
                ucweights[i] = (int) Math.floor(minDist / Constants.RNTransmissionRange);
            }

            int sum1 = ucweights[0] + ucweights[1];

            int sum2 = Math.min(ucweights[0], ucweights[1]) + (int) Math.floor(distanceBetweenUncoveredNodes / Constants.RNTransmissionRange);
            int r2 = Math.min(sum1, sum2);

            if (r1 < r2) {
                ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
//                int ccid = ti.getCoveredNodes().get(0).getCcid();
//                for (int i = 0; i < uncovered.size(); i++) {
//                    Gateway g = uncovered.get(i);
//                    g.setCcid(ccid);
//                }
            } else {
                if (sum1 < sum2) {
                    int ccid0 = ucpairs[0][1].getCcid();
                    int ccid1 = ucpairs[1][1].getCcid();

                    NetworkUtils.fillGap(ucpairs[0][0], ucpairs[0][1], true, ccid0, ActorsArray, RelayNodeArray);
                    NetworkUtils.fillGap(ucpairs[1][0], ucpairs[1][1], true, ccid1, ActorsArray, RelayNodeArray);

                    ucpairs[0][0].setCcid(ccid0);
                    ucpairs[1][0].setCcid(ccid1);
                } else {
                    int ccid;
                    NetworkUtils.fillGap(uncovered.get(0), uncovered.get(1), true, ActorsArray, RelayNodeArray);
                    if (ucweights[0] < ucweights[1]) {
                        NetworkUtils.fillGap(ucpairs[0][0], ucpairs[0][1], true, ActorsArray, RelayNodeArray);
                        ccid = ucpairs[0][1].getCcid();
                    } else {
                        NetworkUtils.fillGap(ucpairs[1][0], ucpairs[1][1], true, ActorsArray, RelayNodeArray);
                        ccid = ucpairs[1][1].getCcid();
                    }
                    uncovered.get(0).setCcid(ccid);
                    uncovered.get(1).setCcid(ccid);
                }
            }
            list2UncoveredTriangles.remove(0);
            rearrangeLists(ti, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);

        }
        federatePartitions(mst_of_terminals);


    }

    private ArrayList<Triangle> eliminateEquivalent3Stars(ArrayList<Triangle> threeStars) {
        double R = Constants.RNTransmissionRange;
        double squareSize = R / Math.sqrt(2);
        ArrayList<Triangle> result = new ArrayList<Triangle>();
        int[][] grid = new int[(int) Math.floor(Constants.ApplicationAreaHeight / squareSize)][(int) Math.floor(Constants.ApplicationAreaWidth / squareSize)];

        for (int i = 0; i < threeStars.size(); i++) {
            Triangle ti = threeStars.get(i);
            int row = (((int) Math.ceil(ti.fermatPoint.getY() / squareSize)) - 1);
            int col = (((int) Math.ceil(ti.fermatPoint.getX() / squareSize)) - 1);
            if (grid[row][col] != 1) {
                result.add(ti);
                grid[row][col] = 1;
            }
        }
        return result;
    }

    private void federatePartitions(ArrayList<Edge> mstedges) {
        /*ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
        allNodes.addAll(RelayNodeArray);

        ArrayList<ArrayList<Gateway>> partitions = NetworkUtils.DephtFirstSearch(allNodes);
        for (int i = 0; i < partitions.size(); i++) {
            ArrayList<Gateway> partition = partitions.get(i);
            for (int j = 0; j < partition.size(); j++) {
                partition.get(j).setCcid(i + 1);
            }
        }*/

//        ArrayList<Edge> mstedges = NetworkUtils.runKruskal(ActorsArray);
        Collections.sort(mstedges);
//        ArrayList<Edge> possibleEdges = new ArrayList<Edge>();
        for (int i = 0; i < mstedges.size(); i++) {
            Edge edge = mstedges.get(i);
            Gateway u = edge.u;
            Gateway v = edge.v;
            int min = Math.min(u.getCcid(), v.getCcid());
            int max = Math.max(u.getCcid(), v.getCcid());
            if (u.getCcid() != v.getCcid()) {
//                possibleEdges.add(edge);
                NetworkUtils.fillGap(u, v, true, max, ActorsArray, RelayNodeArray);
                if (u.getMstID() == 0 || v.getCcid() == 0) {
                    if (u.getMstID() == 0 && v.getCcid() == 0) {
                        if (Constants.LOG_TRIANGLE) {
                            System.out.println("ERROR in Federation");
                        }
                    } else if (u.getCcid() == 0) {
                        u.setCcid(v.getCcid());
                    } else if (v.getCcid() == 0) {
                        v.setCcid(u.getCcid());
                    }
                } else {

                    for (int j = 0; j < ActorsArray.size(); j++) {
                        Gateway g = ActorsArray.get(j);
                        if (g.getCcid() == min) {
                            g.setCcid(max);
                        }
                    }
                }
            }
        }

//        Collections.sort(possibleEdges);
//
//
//        for (int i = possibleEdges.size() - 1; i >= 0; i--) {
//            Edge edge_i = possibleEdges.get(i);
//            boolean remove_i = false;
//            for (int j = 0; j < possibleEdges.size(); j++) {
//                if (i != j) {
//                    Edge edge_j = possibleEdges.get(j);
//
//                }
//            }
//        }

//        System.out.println("");
    }

    private Triangle getBestCandidateTriangle(ArrayList<Triangle> list3UncoveredTriangles, HashSet<Gateway> allCoveredNodes) {
        if (allCoveredNodes.size() == 0) {
            return list3UncoveredTriangles.get(0);
        }
        ArrayList<Triangle> candidates = new ArrayList<Triangle>();
        int w = 0;
        for (int i = 0; i < list3UncoveredTriangles.size(); i++) {
            Triangle ti = list3UncoveredTriangles.get(i);
            if (i == 0) {
                w = ti.rho;
                candidates.add(ti);
            } else if (ti.rho == w) {
                candidates.add(ti);
            } else if (ti.rho > w) {
                break;
            }
        }
        int[] candidateDistances = new int[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            Triangle cti = candidates.get(i);
            ArrayList<Edge> list = findPairsOfNodes(cti, allCoveredNodes);
            candidateDistances[i] = Integer.MAX_VALUE;
            for (int j = 0; j < list.size(); j++) {
                Edge pair = list.get(j);
                if (pair.weight < candidateDistances[i]) {
                    candidateDistances[i] = pair.weight;
                }
            }
        }
        int minIndex = -1;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < candidateDistances.length; i++) {
            if (candidateDistances[i] < minDist) {
                minDist = candidateDistances[i];
                minIndex = i;
            }
        }

        return candidates.get(minIndex);
    }


    private void rearrangeLists(Triangle ti, HashSet<Gateway> allCoveredNodes, ArrayList<Triangle> list1UncoveredTriangles, ArrayList<Triangle> list2UncoveredTriangles, ArrayList<Triangle> list3UncoveredTriangles) {

        if ((!allCoveredNodes.contains(ti.s1)) && ti.s1.getCcid() != 0) {
            allCoveredNodes.add(ti.s1);
        }
        if ((!allCoveredNodes.contains(ti.s2)) && ti.s2.getCcid() != 0) {
            allCoveredNodes.add(ti.s2);
        }
        if ((!allCoveredNodes.contains(ti.s3)) && ti.s3.getCcid() != 0) {
            allCoveredNodes.add(ti.s3);
        }

        for (int i = list2UncoveredTriangles.size() - 1; i >= 0; i--) {
            Triangle triangle = list2UncoveredTriangles.get(i);


            if (ti.intersectionCardinality(triangle) == 2 || ti.intersectionCardinality(triangle) == 3) {
                list2UncoveredTriangles.remove(i);
            } else if (ti.intersectionCardinality(triangle) == 1) {
                list1UncoveredTriangles.add(triangle);
                list2UncoveredTriangles.remove(i);
            }
        }
        for (int i = list3UncoveredTriangles.size() - 1; i >= 0; i--) {
            Triangle triangle = list3UncoveredTriangles.get(i);


            if (ti.intersectionCardinality(triangle) == 3) {
                list3UncoveredTriangles.remove(i);
            } else if (ti.intersectionCardinality(triangle) == 2) {
                list1UncoveredTriangles.add(triangle);
                list3UncoveredTriangles.remove(i);
            } else if (ti.intersectionCardinality(triangle) == 1) {
                list2UncoveredTriangles.add(triangle);
                list3UncoveredTriangles.remove(i);
            }

        }
    }


    /**
     * This function finds the two combination adjacent triangles to t, which have two smallest rho values
     *
     * @param t          triangle whose corners are all uncovered
     * @param allCovered set of covered nodes
     * @return first index is the smallest who value, secondindex is the second smallest rho, third index is third smalest rho
     */
    private ArrayList<Edge> findPairsOfNodes(Triangle t, HashSet<Gateway> allCovered) {

        HashSet<Gateway> tobeCovered = new HashSet<Gateway>(t.getCorners());

        ArrayList<Gateway[]> pairs = new ArrayList<Gateway[]>();
        int row = 0;
        for (Gateway gateway : tobeCovered) {
//            HashSet<Gateway> remaining = new HashSet<Gateway>(t.getCorners());
//            remaining.remove(gateway);
            HashSet<Gateway> tmpAllCovered = new HashSet<Gateway>(allCovered);
//            tmpAllCovered.addAll(remaining);
            double minDist = Double.MAX_VALUE;
            Gateway bestMatch = null;
            for (Gateway match : tmpAllCovered) {
                double d = NetworkUtils.EstimatedDistance(gateway, match);
                if (d <= minDist) {
                    minDist = d;
                    bestMatch = match;
                }
            }
            if (bestMatch != null) {
                Gateway[] pair = new Gateway[2];
                pair[0] = gateway;
                pair[1] = bestMatch;
                pairs.add(pair);
            }
        }
        // eliminate duplicate pairs
        for (int i = pairs.size() - 1; i > 0; i--) {
            Gateway[] pi = pairs.get(i);
            for (int j = i - 1; j >= 0; j--) {
                Gateway[] pj = pairs.get(j);
                if ((pi[0] == pj[0] && pi[1] == pj[1]) || (pi[0] == pj[1] && pi[1] == pj[0])) {
                    pairs.remove(i);
                    break;
                }
            }

        }
        ArrayList<Edge> result = new ArrayList<Edge>();
        for (int i = 0; i < pairs.size(); i++) {
            Gateway[] gateways = pairs.get(i);
            result.add(new Edge(gateways[0], gateways[1]));
        }
        return result;

    }

    /**
     * This function finds the two combination adjacent triangles to t, which have two smallest rho values
     *
     * @param tc         list of uncovered nodes
     * @param allCovered set of covered nodes
     * @return first index is the smallest who value, secondindex is the second smallest rho, third index is third smalest rho
     */
    private ArrayList<Gateway[]> findPairsOfNodes(ArrayList<Gateway> tc, HashSet<Gateway> allCovered) {

        HashSet<Gateway> tobeCovered = new HashSet<Gateway>();
        for (int i = 0; i < tc.size(); i++) {
            tobeCovered.add(tc.get(i));
        }

        ArrayList<Gateway[]> pairs = new ArrayList<Gateway[]>();
        int row = 0;
        for (Gateway gateway : tobeCovered) {
//            HashSet<Gateway> remaining = new HashSet<Gateway>(t.getCorners());
//            remaining.remove(gateway);
            HashSet<Gateway> tmpAllCovered = new HashSet<Gateway>(allCovered);
//            tmpAllCovered.addAll(remaining);
            double minDist = Double.MAX_VALUE;
            Gateway bestMatch = null;
            for (Gateway match : tmpAllCovered) {
                double d = NetworkUtils.EstimatedDistance(gateway, match);
                if (d <= minDist) {
                    minDist = d;
                    bestMatch = match;
                }
            }
            if (bestMatch != null) {
                Gateway[] pair = new Gateway[2];
                pair[0] = gateway;
                pair[1] = bestMatch;
                pairs.add(pair);
            }
        }
        // eliminate duplicate pairs
        for (int i = pairs.size() - 1; i > 0; i--) {
            Gateway[] pi = pairs.get(i);
            for (int j = i - 1; j >= 0; j--) {
                Gateway[] pj = pairs.get(j);
                if ((pi[0] == pj[0] && pi[1] == pj[1]) || (pi[0] == pj[1] && pi[1] == pj[0])) {
                    pairs.remove(i);
                    break;
                }
            }

        }

        return pairs;

    }


    /**
     * This function finds the two combination adjacent triangles to t, which have two smallest rho values
     *
     * @param t                       triangle whose corners are all uncovered
     * @param list2UncoveredTriangles list all triangles having two corners uncovered (one corner is already covered)
     * @param allCoveredNodes         set of covered nodes
     * @return first index is the smallest who value, secondindex is the second smallest rho
     */
    private TriangleAndPairOfNodes findBestTriangleAndPairOfNodes(Triangle t, ArrayList<Triangle> list2UncoveredTriangles, HashSet<Gateway> allCoveredNodes) {
        HashSet<Gateway> tobeCovered = new HashSet<Gateway>(t.getCorners());
//        if (list2UncoveredTriangles.isEmpty()) {
//            return null;
//        }
        ArrayList<Triangle> listOfPossibleTriangles = new ArrayList<Triangle>();
        for (int i = 0; i < list2UncoveredTriangles.size(); i++) {
            Triangle ti = list2UncoveredTriangles.get(i);
            if (t.intersectionCardinality(ti) == 2) {
                listOfPossibleTriangles.add(ti);
            }
        }
        int minWeight = Integer.MAX_VALUE;
        Triangle bestTriangle = null;
        Gateway[] bestPair = new Gateway[2];

        /*for (int i = 0; i < listOfPossibleTriangles.size(); i++) {
            //tp is possible triangle
            DT_Triangle tp = listOfPossibleTriangles.get(i);
            HashSet<Gateway> pos = new HashSet<Gateway>(tp.getCorners());

            // intersectionttp is the intersection set of t and tp
            HashSet<Gateway> intersectionttp = new HashSet<Gateway>(tobeCovered);
            intersectionttp.retainAll(pos);

            //diffAP is the difference set of t from tp  t/tp(supposed to contain one element)
            HashSet<Gateway> diffttp = new HashSet<Gateway>(tobeCovered);
            diffttp.removeAll(pos);
            Gateway remaining = diffttp.iterator().next();

            HashSet<Gateway> tmpAllCovered = new HashSet<Gateway>(allCoveredNodes);
            tmpAllCovered.addAll(intersectionttp);
            Gateway bestMatchWithRemaining = null;
            double mindDist = Double.MAX_VALUE;

            for (Gateway gateway : tmpAllCovered) {
                double d = NetworkUtils.EstimatedDistance(remaining, gateway);
                if (d <= mindDist) {
                    mindDist = d;
                    bestMatchWithRemaining = gateway;
                }
            }
            if (bestMatchWithRemaining == null)
                continue;
            int weight = tp.rho + (int) Math.floor(mindDist / Constants.RNTransmissionRange);
            if (weight < minWeight) {
                minWeight = weight;
                bestTriangle = tp;
                bestPair[0] = remaining;
                bestPair[1] = bestMatchWithRemaining;
            }
        }*/
        if (listOfPossibleTriangles.isEmpty()) {
            return new TriangleAndPairOfNodes(null, null);
        } else {

            Collections.sort(listOfPossibleTriangles);
            bestTriangle = listOfPossibleTriangles.get(0);
            HashSet<Gateway> tmpAllCoveredNodes = new HashSet<Gateway>(allCoveredNodes);

            HashSet<Gateway> pos = new HashSet<Gateway>(bestTriangle.getCorners());
            //diffAP is the difference set of t from tp  t/tp(supposed to contain one element)
            HashSet<Gateway> diffttp = new HashSet<Gateway>(tobeCovered);
            diffttp.removeAll(pos);
            Gateway remaining = diffttp.iterator().next();
            ArrayList<Gateway> rem = new ArrayList<Gateway>();
            rem.add(remaining);
            tmpAllCoveredNodes.addAll(bestTriangle.getUncoveredNodes());
            ArrayList<Gateway[]> list = findPairsOfNodes(rem, tmpAllCoveredNodes);

            return new TriangleAndPairOfNodes(listOfPossibleTriangles.get(0), list.get(0));
        }
    }

    private void pruneRNs(ArrayList<Gateway> nodeList) {
        for (int i = nodeList.size() - 1; i >= 0; i--) {
            Gateway g = nodeList.get(i);
            if (!isCV(g)) {
                nodeList.remove(i);
            }
        }
    }

    private class TriangleAndPairOfNodes {
        Triangle triangle;
        Gateway[] pair = new Gateway[2];
        int weight = Integer.MAX_VALUE;
        int weightOfTriangle = Integer.MAX_VALUE, weightOfEdge = Integer.MAX_VALUE;

        public TriangleAndPairOfNodes(Triangle triangle, Gateway[] pair) {
            this.triangle = triangle;
            this.pair = pair;
            if (triangle != null && pair != null && pair[0] != null && pair[1] != null) {
                weightOfTriangle = this.triangle.rho;
                weightOfEdge = (int) (Math.ceil(NetworkUtils.EstimatedDistance(pair[0], pair[1]) / Constants.RNTransmissionRange) - 1);
                weight = weightOfTriangle + weightOfEdge;
            } else {
                weight = Integer.MAX_VALUE;
            }
        }
    }

    /**
     * Converts actual coordinates into row/column index model (to be feeded Sookyoungs Simulator as input topology)
     * segm[i][0] is the row index
     * segm[i][1] is the column index
     */
    public void convert() {
        double R = Constants.RNTransmissionRange;
        double squareSize = R / Math.sqrt(2);
        for (int p = 9; p <= 9; p++) {
            System.out.println("Partition = " + p);
            for (int e = 1; e <= 50; e++) {

                boolean redo = true;

                while (redo) {
                    redo = false;

                    String filename = p + "_" + e + ".dat";
                    reload(filename);

                    ArrayList<ArrayList<Gateway>> partitions = NetworkUtils.DephtFirstSearch(ActorsArray);
                    if (/*ActorsArray.size() != p &&*/ partitions.size() == p) {
                        ActorsArray.clear();
                        for (int i = 0; i < partitions.size(); i++) {
                            ArrayList<Gateway> pi = partitions.get(i);
                            Gateway gi = pi.get(0);
                            ActorsArray.add(gi);
                        }
                    } else if (/*ActorsArray.size() != p ||*/ partitions.size() != p) {
                        System.out.println("P : " + p + " E : " + e + " is fixed");
                        redo = true;
                        fix(p, e);
                        continue;
                    }

                    int[][] segm = new int[ActorsArray.size()][2];
                    for (int i = 0; i < ActorsArray.size(); i++) {
                        Gateway gateway = ActorsArray.get(i);
                        segm[i][0] = ((int) Math.ceil(gateway.getY() / squareSize)) - 1;
                        segm[i][1] = ((int) Math.ceil(gateway.getX() / squareSize)) - 1;
                        double y = squareSize * (segm[i][0] + 0.5);
                        double x = squareSize * (segm[i][1] + 0.5);
                        gateway.setY(y);
                        gateway.setX(x);
                    }
                    saveSegment("segmentData/" + filename, ActorsArray.size(), segm);
                    save(filename);
                }

            }
        }

        System.out.println("");

    }

    public int[][] readSegmentFile(String filename) {
        int[][] result = null;
        int counter = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            result = new int[Integer.parseInt(br.readLine())][2];
            String line = br.readLine();
            while (line != null) {
                String[] tokens = line.split("\t");
                result[counter][0] = (Integer.parseInt(tokens[0]));
                result[counter][1] = (Integer.parseInt(tokens[1]));
                counter++;
                line = br.readLine();
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;

    }


    public ArrayList<Gateway> loadSegment(String filename) {
        double R = Constants.RNTransmissionRange;
        double squareSize = R / Math.sqrt(2);
        ArrayList<Gateway> nodesArray = new ArrayList<Gateway>();

        int[][] segments = readSegmentFile(filename);
        int counter = 0;
        for (int i = 0; i < segments.length; i++) {
            double X = (segments[i][1] + 0.5) * squareSize;
            double Y = (segments[i][0] + 0.5) * squareSize;

            Gateway g = new Gateway(counter++);
            g.setX(X);
            g.setY(Y);
            nodesArray.add(g);

        }
        return nodesArray;
    }

    public void checkSegmentConnectivity() {
        for (int p = 3; p <= 15; p++) {
            System.out.println("Partition = " + p);
            for (int e = 1; e <= 50; e++) {

                checkSegmentConnectivity(p, e);
            }
        }
    }

    public boolean isAllSegmentsDisjoint(int[][] segments) {
        for (int i = 0; i < segments.length - 1; i++) {
            for (int j = i; j < segments.length; j++) {
                if (Math.max(Math.abs(segments[i][0] - segments[j][0]), Math.abs(segments[i][1] - segments[j][1])) == 1) {
                    return false;
                }
            }
        }
        return true;
    }

    public void fixSegmentAndActualData(int p, int e) {

        String actualFilename = p + "_" + e + ".dat";
        String segmentFilename = "./segmentData/" + actualFilename;
        fixSegment(p, e);
        ArrayList<Gateway> list = loadSegment(segmentFilename);
        ActorsArray.clear();
        ActorsArray.addAll(list);
        save(actualFilename);
        System.out.println("File : " + actualFilename + " is fixed");
    }

    public void checkSegmentConnectivity(int p, int e) {
        String actualFilename = p + "_" + e + ".dat";
        String segmentFilename = "./segmentData/" + actualFilename;
        int[][] segments = readSegmentFile(segmentFilename);

        if (segments != null) {
            if (!isAllSegmentsDisjoint(segments)) {
                System.out.println("File " + actualFilename + " is fixed");
                fixSegment(p, e);
                ArrayList<Gateway> list = loadSegment(segmentFilename);
                ActorsArray.clear();
                ActorsArray.addAll(list);
                save(actualFilename);
            }
        }
    }

    private int[][] fixSegment(int p, int e) {
        String filename = "./segmentData/" + p + "_" + e + ".dat";
        double squareSize = Constants.RNTransmissionRange / Math.sqrt(2);
        int[][] segment = new int[p][2];
        int row = (int) Math.ceil(Constants.ApplicationAreaHeight / squareSize);
        int column = (int) Math.ceil(Constants.ApplicationAreaWidth / squareSize);
        int[][] gridCells = new int[row][column];

        int pCounter = 0;
        while (pCounter < p) {
            int rand = (int) Math.round(Math.random() * ((row * column) - 1));
            int r = rand / row;
            int c = rand % row;
            boolean left = true, right = true, top = true, bottom = true;
            if (gridCells[r][c] == 0) {
                segment[pCounter][0] = r;
                segment[pCounter][1] = c;
                gridCells[r][c] = 1;
                if (r == 0) {
                    top = false;
                }
                if (r == (row - 1)) {
                    bottom = false;
                }
                if (c == 0) {
                    left = false;
                }
                if (c == column - 1) {
                    right = false;
                }
                if (top && left) {
                    gridCells[r - 1][c - 1] = 1;
                }
                if (top) {
                    gridCells[r - 1][c] = 1;
                }
                if (top && right) {
                    gridCells[r - 1][c + 1] = 1;
                }
                if (left) {
                    gridCells[r][c - 1] = 1;
                }
                if (right) {
                    gridCells[r][c + 1] = 1;
                }
                if (bottom && left) {
                    gridCells[r + 1][c - 1] = 1;
                }
                if (bottom) {
                    gridCells[r + 1][c] = 1;
                }
                if (bottom && right) {
                    gridCells[r + 1][c + 1] = 1;
                }
                pCounter++;
            }

        }
        saveSegment(filename, p, segment);
        return segment;
    }

    public void fixSegments() {
        String[] filenames = {"3_46.dat"};


        for (int i = 0; i < filenames.length; i++) {
            String str = filenames[i].substring(0, filenames[i].length() - 4);
            String[] tokens = str.split("_");
            int p = Integer.parseInt(tokens[0]);
            int e = Integer.parseInt(tokens[1]);
            fixSegment(p, e);
            ArrayList<Gateway> list = loadSegment("segmentData/" + filenames[i]);
            ActorsArray.clear();
            ActorsArray.addAll(list);
            save(filenames[i]);
            System.out.println(filenames[i] + " is fixed!!!");

        }
    }

    /**
     * Only load relay nodes because corp output only contains relay nodes
     *
     * @param filename file to be loaded
     * @return list of relay nodes (actual coordinates not row column numbers)
     */
    public ArrayList<Gateway> Load_CORPOutput(String filename) {
        double squareSize = Constants.RNTransmissionRange / Math.sqrt(2);
        ArrayList<Gateway> list = new ArrayList<Gateway>();
        int i = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line = br.readLine();
            while (line != null) {

                String[] tokens = line.split("\t");
                Gateway g = new Gateway(i++);
                g.setY((Integer.parseInt(tokens[0].trim()) + 0.5) * squareSize);
                g.setX((Integer.parseInt(tokens[1].trim()) + 0.5) * squareSize);
                g.isRelay = true;
                list.add(g);
                line = br.readLine();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        return list;
    }

    public ArrayList<int[]> Load_CORPJunction(String filename) {
        ArrayList<int[]> junctions = new ArrayList<int[]>();
//        int[][] grid = new int[1000][1000];
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line = br.readLine();
            while (line != null) {

                String[] tokens = line.split("\t");
                int row = Integer.parseInt(tokens[0].trim());
                int col = Integer.parseInt(tokens[1].trim());
//                if (grid[row][col] == 0) {
//                    grid[row][col] = 1;
                junctions.add(new int[]{row, col});
                line = br.readLine();
//                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        return junctions;
    }

    public ArrayList<Gateway> Load_KLCAOutput(String filename) {
        int[] occupied = new int[100];
        double squareSize = Constants.RNTransmissionRange / Math.sqrt(2);
        ArrayList<Gateway> list = new ArrayList<Gateway>();
        ArrayList<Gateway[]> tobefilled = new ArrayList<Gateway[]>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line = br.readLine();
            while (line != null) {
                String[] tokens = line.split(";");
                int cellDist = Integer.parseInt(tokens[1]);
                String[] tokens2 = tokens[0].split("-");
                Gateway[] gis = new Gateway[2];
                for (int i = 0; i < tokens2.length; i++) {
                    tokens2[i] = tokens2[i].substring(0, tokens2[i].length() - 1);
                    boolean first = true, second = false, third = false;
                    String sid = "", sr = "", sc = "";
                    for (int j = 0; j < tokens2[i].length(); j++) {
                        if (tokens2[i].charAt(j) == '(') {
                            second = true;
                            first = false;
                        } else if (tokens2[i].charAt(j) == ',') {
                            second = false;
                            third = true;
                        } else {
                            if (first) {
                                sid += tokens2[i].charAt(j);
                            } else if (second) {
                                sr += tokens2[i].charAt(j);
                            } else if (third) {
                                sc += tokens2[i].charAt(j);
                            }
                        }
                    }
                    int id = Integer.parseInt(sid);
                    int row = Integer.parseInt(sr);
                    int column = Integer.parseInt(sc);
                    double y = squareSize * (row + 0.5);
                    double x = squareSize * (column + 0.5);
                    Gateway gi = new Gateway(id);
                    gi.setX(x);
                    gi.setY(y);
                    gis[i] = gi;
                    if (occupied[id] == 0) {
                        list.add(gi);
                        occupied[id] = 1;
                    }
                }
                if (gis[0] != null && gis[1] != null && cellDist > 0) {
                    NetworkUtils.fillGap(gis[0], gis[1], true, ActorsArray, RelayNodeArray);
//                    tobefilled.add(gis);
                }
                line = br.readLine();
            }
            br.close();
        } catch (FileNotFoundException e) {
//            e.printStackTrace();
            try {
                PrintWriter pr = new PrintWriter(new FileWriter("badinputnames.txt", true));
                String str = filename.substring(11, filename.length());
                pr.print("\"" + str + "\", ");
                pr.close();
                System.out.println("FILE :" + str + " is printed");
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void process_kLCAOutput(String filename, int p) {
        String DIR = "kLCAoutput/";
        filename = DIR + filename;
        RelayNodeArray.clear();
        ActorsArray.clear();

        ArrayList<Gateway> list = Load_KLCAOutput(filename);
        ArrayList<Gateway> tmpRNs = new ArrayList<Gateway>(RelayNodeArray);
        RelayNodeArray.clear();
        for (int i = 0; i < list.size(); i++) {
            Gateway gateway = list.get(i);
            if (gateway.getID() >= p) {
                gateway.isRelay = true;
                RelayNodeArray.add(gateway);
            } else {
                ActorsArray.add(gateway);
            }
        }
        int offset = ActorsArray.size() + RelayNodeArray.size();
        for (int i = 0; i < tmpRNs.size(); i++) {
            Gateway gateway = tmpRNs.get(i);
            gateway.setId(offset + gateway.getID());
            RelayNodeArray.add(gateway);
        }
        eliminateDuplicateNodes();
        ArrayList<Gateway> allNodes = new ArrayList<Gateway>(RelayNodeArray);
        allNodes.addAll(ActorsArray);
        NetworkUtils.calculateActorNeighborhoods(allNodes, Constants.RNTransmissionRange);
    }

    public void process_CORPOutput(String filename) {
        String DIR = "./CORPoutput/";
        String corpfilename = DIR + filename;
        RelayNodeArray.clear();
        ActorsArray.clear();
        ArrayList<Gateway> list = Load_CORPOutput(corpfilename);
        RelayNodeArray.addAll(list);
        ActorsArray.addAll(loadSegment("./segmentData/" + filename));
        ArrayList<Gateway> allNodes = new ArrayList<Gateway>(RelayNodeArray);
        allNodes.addAll(ActorsArray);
        NetworkUtils.calculateActorNeighborhoods(allNodes, Constants.RNTransmissionRange);
    }

    private class corp_square {
        int row, column;
        final static int TERMINAL = 0;
        final static int JUNCTION = 1;
        final static int REGULAR = 2;

        int status;

        private corp_square(int row, int column, int status) {
            this.row = row;
            this.column = column;
            this.status = status;
        }

        public String toString() {
            return "(" + row + "," + column + ")";
        }

        public boolean equals(Object obj) {
            corp_square s_obj = (corp_square) obj;
            return this.row == s_obj.row && this.column == s_obj.column;
        }
    }

    private class corp_path {
        ArrayList<corp_square> path = new ArrayList<corp_square>();

        private corp_path() {
        }

        public void add(corp_square sq) {
            path.add(sq);
        }

        public String toString() {
            String result = "{";
            for (int i = 0; i < path.size(); i++) {
                if (i != path.size() - 1)
                    result += path.get(i) + ",";
                else
                    result += path.get(i);

            }
            return result + "}";
        }

        public boolean equals(Object obj) {
            corp_path p_obj = (corp_path) obj;
            if (this.path.size() != p_obj.path.size())
                return false;
            else {
                for (int i = 0; i < this.path.size(); i++) {
                    if (!this.path.get(i).equals(p_obj.path.get(i)))
                        return false;
                }
            }
            return true;
        }
    }

    private class corp_subgraph {
        corp_square terminal;
        ArrayList<corp_path> paths = new ArrayList<corp_path>();

        private corp_subgraph(corp_square terminal) {
            this.terminal = terminal;
        }

        public void add(corp_path path) {
            paths.add(path);
        }

        public corp_path getLastPath() {
            if (paths.isEmpty())
                return null;
            return paths.get(paths.size() - 1);
        }
    }

    public void process_CORP_OPT(String filename) {
        double squareSize = Constants.RNTransmissionRange / Math.sqrt(2);
        RelayNodeArray.clear();
        ActorsArray.clear();
        corp_junctions.clear();
        String SEGMENT_DIR = "./segmentData/";
        String JUNCTION_DIR = "./junction/";
//        ActorsArray.addAll(loadSegment(SEGMENT_DIR + filename));
//        corp_junctions.addAll(Load_CORPJunction(JUNCTION_DIR + filename));

        ArrayList<corp_subgraph> allSubgraphs = new ArrayList<corp_subgraph>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(JUNCTION_DIR + filename));
            String line = br.readLine();
            String type = "", prevType = "";
            corp_square from = null, to = null;
            while (line != null) {
                prevType = type;
                String[] arr = line.split("\t");
                type = arr[0].trim();
                if (type.equals("N")) {
                    if (arr.length == 3) {
                        int row = Integer.parseInt(arr[1].trim());
                        int col = Integer.parseInt(arr[2].trim());
                        from = new corp_square(row, col, corp_square.TERMINAL);
                        corp_subgraph sg = new corp_subgraph(from);
                        corp_path path = new corp_path();
                        path.add(from);
                        sg.add(path);
                        allSubgraphs.add(sg);
                    }
                } else if (type.equals("I")) {
                    if (prevType.equals("N") || prevType.equals("I")) {
                        int row = Integer.parseInt(arr[3].trim());
                        int col = Integer.parseInt(arr[4].trim());
                        corp_subgraph sg = allSubgraphs.get(allSubgraphs.size() - 1);
                        sg.getLastPath().add(new corp_square(row, col, corp_square.REGULAR));
                    } else if (prevType.equals("E")) {
                        corp_subgraph sg = allSubgraphs.get(allSubgraphs.size() - 1);
                        corp_path path = new corp_path();
                        path.add(from);
                        int row = Integer.parseInt(arr[3].trim());
                        int col = Integer.parseInt(arr[4].trim());
                        path.add(new corp_square(row, col, corp_square.REGULAR));
                        sg.add(path);
                    }
                } else { //type = E
                    if (prevType.equals("N") || prevType.equals("I")) {
                        int row = Integer.parseInt(arr[3].trim());
                        int col = Integer.parseInt(arr[4].trim());
                        corp_subgraph sg = allSubgraphs.get(allSubgraphs.size() - 1);
                        from = new corp_square(row, col, corp_square.JUNCTION);
                        sg.getLastPath().add(from);
                    } else { // E after E
                        corp_path p = new corp_path();
                        p.add(from);
                        int row = Integer.parseInt(arr[3].trim());
                        int col = Integer.parseInt(arr[4].trim());
                        to = new corp_square(row, col, corp_square.JUNCTION);
                        p.add(to);
                        from = to;
                        corp_subgraph sg = allSubgraphs.get(allSubgraphs.size() - 1);
                        sg.add(p);
                    }
                }

                line = br.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("");
        ArrayList<corp_path> unique_paths = new ArrayList<corp_path>();
        ArrayList<corp_square> unique_cells = new ArrayList<corp_square>();
        for (int i = 0; i < allSubgraphs.size(); i++) {
            ArrayList<corp_path> corp_paths = allSubgraphs.get(i).paths;
            for (int j = 0; j < corp_paths.size(); j++) {
                corp_path cp = corp_paths.get(j);
                boolean contains = false;
                for (int k = 0; k < unique_paths.size(); k++) {
                    corp_path cp1 = unique_paths.get(k);
                    if (cp.equals(cp1)) {
                        contains = true;
                        break;
                    }
                }
                if (!contains) {
                    unique_paths.add(cp);
                }
            }

        }
        for (int i = 0; i < unique_paths.size(); i++) {
            ArrayList<corp_square> cp_path = unique_paths.get(i).path;
            for (int j = 0; j < cp_path.size(); j++) {
                corp_square cs = cp_path.get(j);
                boolean contains = false;
                for (int k = 0; k < unique_cells.size(); k++) {
                    corp_square cs1 = unique_cells.get(k);
                    if (cs.equals(cs1)) {
                        contains = true;
                        break;
                    }
                }
                if (!contains) {
                    unique_cells.add(cs);
                }
            }
        }

        RelayNodeArray.clear();
        ActorsArray.clear();
        ActorsArray.addAll(loadSegment(SEGMENT_DIR + filename));

        String CORP_DIR = "./CORPoutput/";
        String corpfilename = CORP_DIR + filename;
        RelayNodeArray.clear();
        ArrayList<Gateway> list = Load_CORPOutput(corpfilename);
        RelayNodeArray.addAll(list);

        ArrayList<Gateway> tobeRemoved = new ArrayList<Gateway>();

        for (int i = 0; i < unique_paths.size(); i++) {
            ArrayList<corp_square> cp_path = unique_paths.get(i).path;
            if (cp_path.size() > 2) {
                for (int j = 1; j < cp_path.size() - 1; j++) {
                    corp_square cs = cp_path.get(j);
                    for (int k = RelayNodeArray.size() - 1; k >= 0; k--) {
                        Gateway rn = RelayNodeArray.get(k);
                        int rn_r = (int) (rn.getY() / squareSize);
                        int rn_c = (int) (rn.getX() / squareSize);
                        if (rn_r == cs.row && rn_c == cs.column) {
                            RelayNodeArray.remove(k);
                        }
                    }

                }
                Gateway from = new Gateway(1000);
                double from_y = (cp_path.get(0).row + 0.5) * squareSize;
                double from_x = (cp_path.get(0).column + 0.5) * squareSize;
                from.setX(from_x);
                from.setY(from_y);
                double to_y = (cp_path.get(cp_path.size() - 1).row + 0.5) * squareSize;
                double to_x = (cp_path.get(cp_path.size() - 1).column + 0.5) * squareSize;
                Gateway to = new Gateway(1001);
                to.setX(to_x);
                to.setY(to_y);
                NetworkUtils.fillGap(from, to, true, ActorsArray, RelayNodeArray);
            }
        }

        /*for (int i = 0; i < unique_cells.size(); i++) {
            corp_square cs = unique_cells.get(i);
            Gateway g = new Gateway(i);
            g.setY((cs.row + 0.5) * squareSize);
            g.setX((cs.column + 0.5) * squareSize);

            if (cs.status == corp_square.JUNCTION) {
                g.isRelay = true;
                boolean isTerminal = false;
                for (int j = 0; j < ActorsArray.size(); j++) {
                    Gateway gateway = ActorsArray.get(j);
                    int row = (int) (gateway.getY() / squareSize);
                    int col = (int) (gateway.getX() / squareSize);
                    if (cs.row == row && cs.column == col) {
                        isTerminal = true;
                    }
                }
                if (!isTerminal) {
                    RelayNodeArray.add(g);
                }
            }
        }*/

        /*for (int i = 0; i < unique_paths.size(); i++) {
            ArrayList<corp_square> cp_path = unique_paths.get(i).path;
            if (cp_path.size() > 2) {
                Gateway from = new Gateway(1000);
                double from_y = (cp_path.get(0).row+0.5)*squareSize;
                double from_x = (cp_path.get(0).column+0.5)*squareSize;
                from.setX(from_x);
                from.setY(from_y);
                double to_y = (cp_path.get(cp_path.size()-1).row+0.5)*squareSize;
                double to_x = (cp_path.get(cp_path.size()-1).column+0.5)*squareSize;
                Gateway to = new Gateway(1001);
                to.setX(to_x);
                to.setY(to_y);
                fillGap(from,to,true);
            }
        }*/
        ArrayList<Gateway> allNodes = new ArrayList<Gateway>(RelayNodeArray);
        allNodes.addAll(ActorsArray);
        NetworkUtils.calculateActorNeighborhoods(allNodes, Constants.RNTransmissionRange);

        System.out.println("");
//        for (int i = 0; i < corp_junctions.size(); i++) {
//            int[] js = corp_junctions.get(i);
////            double y = js[0]
//            Gateway g = new Gateway(i);
//            g.setY((js[0] + 0.5) * squareSize);
//            g.setX((js[1] + 0.5) * squareSize);
//            g.isRelay = true;
//            RelayNodeArray.add(g);
//
//        }
//
//        for (int i = 0; i < ActorsArray.size(); i++) {
//            Gateway segm_i = ActorsArray.get(i);
//
//        }
    }

    // October 18, 2010

    public void runDynamicallyGrowingTriangleApproach() {
        runDynamicallyGrowingTriangleApproach(ActorsArray);
    }

    public void runDynamicallyGrowingTriangleApproach(ArrayList<Gateway> NodeList) {
        boolean newversion = true;

        HashSet<Gateway> allCoveredNodes = new HashSet<Gateway>();
        HashSet<Gateway> AllUncoveredNodes = new HashSet<Gateway>(NodeList);
        ArrayList<Triangle> list3UncoveredTriangles = new ArrayList<Triangle>();
        ArrayList<Triangle> list2UncoveredTriangles = new ArrayList<Triangle>();
        ArrayList<Triangle> list1UncoveredTriangles = new ArrayList<Triangle>();
        ArrayList<Triangle> listAllcoveredTriangles = new ArrayList<Triangle>();
        int nextCCID = 1;


        for (int i = 0; i < NodeList.size() - 2; i++) {
            Gateway si = NodeList.get(i);
            for (int j = i + 1; j < NodeList.size() - 1; j++) {
                Gateway sj = NodeList.get(j);
                for (int k = j + 1; k < NodeList.size(); k++) {
                    Gateway sk = NodeList.get(k);
                    Triangle t = new Triangle(si, sj, sk);
                    list3UncoveredTriangles.add(t);
                }
            }
        }
        ArrayList<Gateway> ListOfAllTerminals = new ArrayList<Gateway>(NodeList);
        ArrayList<Gateway> ListOfAllNodes = new ArrayList<Gateway>(NodeList);
        Collections.sort(list3UncoveredTriangles);

        ArrayList<Triangle> threeStars = new ArrayList<Triangle>();
        for (int i = 0; i < list3UncoveredTriangles.size(); i++) {
            Triangle triangle = list3UncoveredTriangles.get(i);
            if (triangle.rho == 1) {
                threeStars.add(triangle);
            } else {
                break;
            }
        }

        threeStars = eliminateEquivalent3Stars(threeStars);
        ArrayList<Gateway> firstRelayNodes = new ArrayList<Gateway>();
        for (int i = 0; i < threeStars.size(); i++) {
            Triangle triangle = threeStars.get(i);
            firstRelayNodes.addAll(triangle.steinerizeTriangle(ActorsArray, RelayNodeArray));
            rearrangeLists(triangle, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);
            AllUncoveredNodes.removeAll(allCoveredNodes);
        }

        allCoveredNodes.addAll(firstRelayNodes);
        ListOfAllNodes.addAll(firstRelayNodes);

        for (int i = 0; i < firstRelayNodes.size(); i++) {
            Gateway rn = firstRelayNodes.get(i);
            for (int j = 0; j < ListOfAllTerminals.size() - 1; j++) {
                Gateway s1 = ListOfAllTerminals.get(j);
                for (int k = j + 1; k < ListOfAllTerminals.size(); k++) {
                    Gateway s2 = ListOfAllTerminals.get(k);
                    Triangle t = new Triangle(s1, s2, rn);
                    if (s1.getCcid() == 0 && s2.getCcid() == 0) {
                        list2UncoveredTriangles.add(t);
                    } else if ((s1.getCcid() == 0 && s2.getCcid() != 0) && (s1.getCcid() != 0 && s2.getCcid() == 0)) {
                        list1UncoveredTriangles.add(t);
                    } else if (s1.getCcid() != 0 && s2.getCcid() != 0 && s1.getCcid() != s2.getCcid()) {
                        //todo for now do nothing
                    }
                }
            }
        }

        ArrayList<Edge> mst_of_terminals = NetworkUtils.runKruskal(ListOfAllNodes);


        while (!list3UncoveredTriangles.isEmpty()) {
            Triangle ti = getBestCandidateTriangle(list3UncoveredTriangles, allCoveredNodes);
            int N1 = ti.rho;


            int N1_prime = Integer.MAX_VALUE;


            for (int i = 0; i < mst_of_terminals.size(); i++) {
                Edge edge = mst_of_terminals.get(i);

                Gateway u = edge.u;
                Gateway v = edge.v;
                if (((u == ti.s1 || u == ti.s2 || u == ti.s3) && (v != ti.s1 && v != ti.s2 && v != ti.s3)) || ((v == ti.s1 || v == ti.s2 || v == ti.s3) && (u != ti.s1 && u != ti.s2 && u != ti.s3))) {
                    if (edge.weight < N1_prime) {
                        N1_prime = edge.weight;
                    }
                }
            }
            N1 += N1_prime;


            TriangleAndPairOfNodes bestTriangleAndPair = findBestTriangleAndPairOfNodes(ti, list2UncoveredTriangles, allCoveredNodes);
            int N2, N2_prime = Integer.MAX_VALUE;
            if (bestTriangleAndPair.triangle == null /*|| bestTriangleAndPair.pair == null*/) {
                N2 = Integer.MAX_VALUE;
            } else {
                if (newversion) {

//                    N2_prime = bestTriangleAndPair.weightOfEdge;

                    Gateway remainingUncoveredNode = bestTriangleAndPair.pair[0];
                    for (int i = 0; i < mst_of_terminals.size(); i++) {
                        Edge edge = mst_of_terminals.get(i);

                        Gateway u = edge.u;
                        Gateway v = edge.v;
                        if (u == remainingUncoveredNode || v == remainingUncoveredNode) {
                            if (edge.weight < N2_prime) {
                                N2_prime = edge.weight;
                            }
                        }
                    }
                    N2 = bestTriangleAndPair.weightOfTriangle + N2_prime;

                } else {
                    N2 = bestTriangleAndPair.weight;
                }
            }
            HashSet<Gateway> tmpAllCoveredNodes = new HashSet<Gateway>(allCoveredNodes);
            ArrayList<Gateway> uncoveredterminals = ti.getUncoveredNodes();
            ArrayList<Gateway[]> oneUncovered = findPairsOfNodes(uncoveredterminals, tmpAllCoveredNodes);
            int counter = 1;

            int N3;
            if (oneUncovered == null || oneUncovered.size() < 2) {
                N3 = Integer.MAX_VALUE;
            } else {
                N3 = 0;

                while (counter <= 3) {


                    ArrayList<Edge> edges = new ArrayList<Edge>();
                    for (int i = 0; i < oneUncovered.size(); i++) {
                        Gateway[] pairs = oneUncovered.get(i);
                        if (pairs != null && pairs[0] != null && pairs[1] != null) {
                            edges.add(new Edge(pairs[0], pairs[1]));
                        }
                    }
                    Collections.sort(edges);
                    Integer w = edges.get(0).weight;
                    N3 += w;
                    Gateway[] pairs = edges.get(0).getNodes();
                    tmpAllCoveredNodes.add(pairs[0]);
                    uncoveredterminals.remove(pairs[0]);
                    oneUncovered = findPairsOfNodes(uncoveredterminals, tmpAllCoveredNodes);

                    counter++;

                }
            }


            int min = Math.min(N1, Math.min(N2, N3));


            if (N1 == min && N1 == N2) {
                if (N1 - N1_prime <= N2 - N2_prime) {
                    N1 = N1 - N1_prime;
                } else {
                    N2 = N2 - N2_prime;
                }
            }
            min = Math.min(N1, Math.min(N2, N3));

            // relay nodes which are deployed in this iteration
            ArrayList<Gateway> ith_relays = new ArrayList<Gateway>();


            if (N2 == min) {
                //go with twoUncovered array


                if (bestTriangleAndPair.triangle != null /*&& bestTriangleAndPair.pair != null*/) {


                    int r2_p = 0;

                    ArrayList<Gateway> coveredNodes = bestTriangleAndPair.triangle.getCoveredNodes();
                    ArrayList<Gateway> uncoveredNodes = bestTriangleAndPair.triangle.getUncoveredNodes();
                    ArrayList<Gateway[]> pairs = findPairsOfNodes(uncoveredNodes, allCoveredNodes);
                    for (int i = 0; i < pairs.size(); i++) {
                        Gateway[] gateways = pairs.get(i);
                        if (gateways != null && gateways[0] != null && gateways[1] != null) {
                            double d = NetworkUtils.EstimatedDistance(gateways[0], gateways[1]);
                            r2_p += ((int) Math.ceil(d / Constants.RNTransmissionRange) - 1);
                        }
                    }

                    r2_p += (int) (Math.ceil(NetworkUtils.EstimatedDistance(bestTriangleAndPair.pair[0], bestTriangleAndPair.pair[1]) / Constants.RNTransmissionRange) - 1);
                    if (N2 <= r2_p) {
                        ith_relays.addAll(bestTriangleAndPair.triangle.steinerizeTriangle(ActorsArray, RelayNodeArray));
                    } else {

                        for (int i = 0; i < pairs.size(); i++) {
                            Gateway[] gateways = pairs.get(i);
                            if (gateways != null && gateways[0] != null && gateways[1] != null) {
                                int ccid = Math.max(gateways[0].getCcid(), gateways[1].getCcid());
                                gateways[0].setCcid(ccid);
                                gateways[1].setCcid(ccid);
                                ith_relays.addAll(NetworkUtils.fillGap(gateways[0], gateways[1], true, ccid, ActorsArray, RelayNodeArray));
                                double d = NetworkUtils.EstimatedDistance(gateways[0], gateways[1]);

                            }
                        }


                    }
                    rearrangeLists(bestTriangleAndPair.triangle, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);
                    allCoveredNodes.addAll(ith_relays);
                    ListOfAllNodes.addAll(ith_relays);


                    for (int i = 0; i < ith_relays.size(); i++) {
                        Gateway rn = ith_relays.get(i);
                        for (int j = 0; j < ListOfAllTerminals.size() - 1; j++) {
                            Gateway s1 = ListOfAllTerminals.get(j);
                            for (int k = j + 1; k < ListOfAllTerminals.size(); k++) {
                                Gateway s2 = ListOfAllTerminals.get(k);
                                Triangle t = new Triangle(s1, s2, rn);
                                if (s1.getCcid() == 0 && s2.getCcid() == 0) {
                                    list2UncoveredTriangles.add(t);
                                } else if ((s1.getCcid() == 0 && s2.getCcid() != 0) && (s1.getCcid() != 0 && s2.getCcid() == 0)) {
                                    list1UncoveredTriangles.add(t);
                                } else if (s1.getCcid() != 0 && s2.getCcid() != 0 && s1.getCcid() != s2.getCcid()) {
                                    //todo for now do nothing
                                }
                            }
                        }
                    }

                    mst_of_terminals = NetworkUtils.runKruskal(ListOfAllNodes);


                }
            } else if (N3 == min) {

                tmpAllCoveredNodes = new HashSet<Gateway>(allCoveredNodes);
                uncoveredterminals = ti.getUncoveredNodes();
                oneUncovered = findPairsOfNodes(uncoveredterminals, tmpAllCoveredNodes);


                ArrayList<Edge> edges = new ArrayList<Edge>();
                for (int i = 0; i < oneUncovered.size(); i++) {
                    Gateway[] pairs = oneUncovered.get(i);
                    if (pairs != null && pairs[0] != null && pairs[1] != null) {
                        edges.add(new Edge(pairs[0], pairs[1]));
                    }
                }
                Collections.sort(edges);
                Integer w = edges.get(0).weight;
                N3 += w;
                Gateway[] pairs = edges.get(0).getNodes();
                int ccid = pairs[1].getCcid();
                pairs[0].setCcid(ccid);
                ith_relays.addAll(NetworkUtils.fillGap(pairs[0], pairs[1], true, ccid, ActorsArray, RelayNodeArray));
                updateLists(list3UncoveredTriangles, list2UncoveredTriangles, list1UncoveredTriangles, allCoveredNodes, pairs[0]);
                allCoveredNodes.add(pairs[0]);
                allCoveredNodes.addAll(ith_relays);
                ListOfAllNodes.addAll(ith_relays);


                for (int i = 0; i < ith_relays.size(); i++) {
                    Gateway rn = ith_relays.get(i);
                    for (int j = 0; j < ListOfAllTerminals.size() - 1; j++) {
                        Gateway s1 = ListOfAllTerminals.get(j);
                        for (int k = j + 1; k < ListOfAllTerminals.size(); k++) {
                            Gateway s2 = ListOfAllTerminals.get(k);
                            Triangle t = new Triangle(s1, s2, rn);
                            if (s1.getCcid() == 0 && s2.getCcid() == 0) {
                                list2UncoveredTriangles.add(t);
                            } else if ((s1.getCcid() == 0 && s2.getCcid() != 0) && (s1.getCcid() != 0 && s2.getCcid() == 0)) {
                                list1UncoveredTriangles.add(t);
                            } else if (s1.getCcid() != 0 && s2.getCcid() != 0 && s1.getCcid() != s2.getCcid()) {
                                //todo for now do nothing
                            }
                        }
                    }
                }

                mst_of_terminals = NetworkUtils.runKruskal(ListOfAllNodes);


            } else if (N1 == min) {
                // go with ti
                ith_relays.addAll(ti.steinerizeTriangle(ActorsArray, RelayNodeArray));
                if (Constants.LOG_TRIANGLE) {
                    System.out.println("R1 : " + ti);
                }
                rearrangeLists(ti, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);
                allCoveredNodes.addAll(ith_relays);
                ListOfAllNodes.addAll(ith_relays);


                for (int i = 0; i < ith_relays.size(); i++) {
                    Gateway rn = ith_relays.get(i);
                    for (int j = 0; j < ListOfAllTerminals.size() - 1; j++) {
                        Gateway s1 = ListOfAllTerminals.get(j);
                        for (int k = j + 1; k < ListOfAllTerminals.size(); k++) {
                            Gateway s2 = ListOfAllTerminals.get(k);
                            Triangle t = new Triangle(s1, s2, rn);
                            if (s1.getCcid() == 0 && s2.getCcid() == 0) {
                                list2UncoveredTriangles.add(t);
                            } else if ((s1.getCcid() == 0 && s2.getCcid() != 0) || (s1.getCcid() != 0 && s2.getCcid() == 0)) {
                                list1UncoveredTriangles.add(t);
                            } else if (s1.getCcid() != 0 && s2.getCcid() != 0 && s1.getCcid() != s2.getCcid()) {
                                //todo for now do nothing
                            }
                        }
                    }
                }

                mst_of_terminals = NetworkUtils.runKruskal(ListOfAllNodes);

            }

        }

        Collections.sort(list2UncoveredTriangles);
        while (!list2UncoveredTriangles.isEmpty()) {
            ArrayList<Gateway> ith_relays = new ArrayList<Gateway>();
            Triangle ti = list2UncoveredTriangles.get(0);
            int r1 = ti.rho;
            ArrayList<Gateway> uncovered = ti.getUncoveredNodes();
            double distanceBetweenUncoveredNodes = NetworkUtils.EstimatedDistance(uncovered.get(0), uncovered.get(1));
            //uc stands for uncovered-covered pairs
            Gateway[][] ucpairs = new Gateway[2][2];
            int[] ucweights = new int[2];
            for (int i = 0; i < uncovered.size(); i++) {
                Gateway uncoveredNode = uncovered.get(i);
                double minDist = Double.MAX_VALUE;
                Gateway bestMatch = null;
                for (Gateway g : allCoveredNodes) {
                    double d = NetworkUtils.EstimatedDistance(uncoveredNode, g);
                    if (d <= minDist) {
                        minDist = d;
                        bestMatch = g;
                    }
                }
                ucpairs[i][0] = uncoveredNode;
                ucpairs[i][1] = bestMatch;
                ucweights[i] = (int) Math.floor(minDist / Constants.RNTransmissionRange);
            }

            int sum1 = ucweights[0] + ucweights[1];

            int sum2 = Math.min(ucweights[0], ucweights[1]) + (int) Math.floor(distanceBetweenUncoveredNodes / Constants.RNTransmissionRange);
            int r2 = Math.min(sum1, sum2);

            if (r1 < r2) {
                ith_relays.addAll(ti.steinerizeTriangle(ActorsArray, RelayNodeArray));
//                int ccid = ti.getCoveredNodes().get(0).getCcid();
//                for (int i = 0; i < uncovered.size(); i++) {
//                    Gateway g = uncovered.get(i);
//                    g.setCcid(ccid);
//                }
            } else {
                if (sum1 < sum2) {
                    int ccid0 = ucpairs[0][1].getCcid();
                    int ccid1 = ucpairs[1][1].getCcid();

                    ith_relays.addAll(NetworkUtils.fillGap(ucpairs[0][0], ucpairs[0][1], true, ccid0, ActorsArray, RelayNodeArray));
                    ith_relays.addAll(NetworkUtils.fillGap(ucpairs[1][0], ucpairs[1][1], true, ccid1, ActorsArray, RelayNodeArray));

                    ucpairs[0][0].setCcid(ccid0);
                    ucpairs[1][0].setCcid(ccid1);
                } else {
                    int ccid;

                    if (ucweights[0] < ucweights[1]) {
                        ccid = ucpairs[0][1].getCcid();
                        ith_relays.addAll(NetworkUtils.fillGap(ucpairs[0][0], ucpairs[0][1], true, ccid, ActorsArray, RelayNodeArray));
                    } else {
                        ccid = ucpairs[1][1].getCcid();
                        ith_relays.addAll(NetworkUtils.fillGap(ucpairs[1][0], ucpairs[1][1], true, ccid, ActorsArray, RelayNodeArray));
                    }
                    ith_relays.addAll(NetworkUtils.fillGap(uncovered.get(0), uncovered.get(1), true, ccid, ActorsArray, RelayNodeArray));
                    uncovered.get(0).setCcid(ccid);
                    uncovered.get(1).setCcid(ccid);
                }
            }
            list2UncoveredTriangles.remove(0);
            rearrangeLists(ti, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);

            allCoveredNodes.addAll(ith_relays);
            ListOfAllNodes.addAll(ith_relays);


            for (int i = 0; i < ith_relays.size(); i++) {
                Gateway rn = ith_relays.get(i);
                for (int j = 0; j < ListOfAllTerminals.size() - 1; j++) {
                    Gateway s1 = ListOfAllTerminals.get(j);
                    for (int k = j + 1; k < ListOfAllTerminals.size(); k++) {
                        Gateway s2 = ListOfAllTerminals.get(k);
                        Triangle t = new Triangle(s1, s2, rn);
                        if (s1.getCcid() == 0 && s2.getCcid() == 0) {
                            list2UncoveredTriangles.add(t);
                        } else if ((s1.getCcid() == 0 && s2.getCcid() != 0) && (s1.getCcid() != 0 && s2.getCcid() == 0)) {
                            list1UncoveredTriangles.add(t);
                        } else if (s1.getCcid() != 0 && s2.getCcid() != 0 && s1.getCcid() != s2.getCcid()) {
                            //todo for now do nothing
                        }
                    }
                }
            }

            mst_of_terminals = NetworkUtils.runKruskal(ListOfAllNodes);

        }
        federatePartitions(mst_of_terminals);
    }

    public void runTriangleAproach2() {
        TriangleAproach2(ActorsArray);
    }

    public void runTriangleAproach3() {
        TriangleAproach3(ActorsArray);
    }

    public void TriangleAproach2(ArrayList<Gateway> NodeList) {

        boolean newversion = true;

        HashSet<Gateway> allCoveredNodes = new HashSet<Gateway>();
        ArrayList<Triangle> list3UncoveredTriangles = new ArrayList<Triangle>();
        ArrayList<Triangle> list3UncoveredTriangles_backup = new ArrayList<Triangle>();
        ArrayList<Triangle> list2UncoveredTriangles = new ArrayList<Triangle>();
        ArrayList<Triangle> list1UncoveredTriangles = new ArrayList<Triangle>();

        int nextCCID = 1;
        ArrayList<Edge> mst_of_terminals = NetworkUtils.runKruskal(NodeList);

        for (int i = 0; i < NodeList.size() - 2; i++) {
            Gateway si = NodeList.get(i);
            for (int j = i + 1; j < NodeList.size() - 1; j++) {
                Gateway sj = NodeList.get(j);
                for (int k = j + 1; k < NodeList.size(); k++) {
                    Gateway sk = NodeList.get(k);
                    Triangle t = new Triangle(si, sj, sk);
                    list3UncoveredTriangles.add(t);
                }
            }
        }
        Collections.sort(list3UncoveredTriangles);
        list3UncoveredTriangles_backup.addAll(list3UncoveredTriangles);

        ArrayList<Triangle> threeStars = new ArrayList<Triangle>();
        for (int i = 0; i < list3UncoveredTriangles.size(); i++) {
            Triangle triangle = list3UncoveredTriangles.get(i);
            if (triangle.rho == 1) {
                threeStars.add(triangle);
            } else {
                break;
            }
        }

        threeStars = eliminateEquivalent3Stars(threeStars);

        for (int i = 0; i < threeStars.size(); i++) {
            Triangle triangle = threeStars.get(i);
            int cid1 = triangle.s1.getCcid();
            int cid2 = triangle.s2.getCcid();
            int cid3 = triangle.s3.getCcid();
            if (!(cid1 == cid2 && cid1 == cid3 && cid1 != 0 && cid2 != 0 && cid3 != 0)) {
                triangle.steinerizeTriangle(ActorsArray, RelayNodeArray);
                rearrangeLists(triangle, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);
            }
        }
        Collections.sort(list2UncoveredTriangles);
        while (!(list3UncoveredTriangles.isEmpty() && list2UncoveredTriangles.isEmpty())) {
//            DT_Triangle ti = list3UncoveredTriangles.get(0);
            Triangle ti = null, tj3 = null, tj2 = null;
            boolean coverThreeterminals = false;
            if (!list3UncoveredTriangles.isEmpty())
                tj3 = getBestCandidateTriangle(list3UncoveredTriangles, allCoveredNodes);
            if (!list2UncoveredTriangles.isEmpty()) {
                tj2 = list2UncoveredTriangles.get(0);
            }
            if (tj3 != null && tj2 != null) {
                if (tj3.rho <= tj2.rho) {
                    ti = tj3;
                    coverThreeterminals = true;
                } else {
                    ti = tj2;
                    coverThreeterminals = false;
                }
            } else {
                if (tj3 != null && tj2 == null) {
                    ti = tj3;
                    coverThreeterminals = true;
                } else if (tj3 == null && tj2 != null) {
                    ti = tj2;
                    coverThreeterminals = false;
                } else {
                    System.out.println("ERROR in triangles");
                    System.exit(0);
                }
            }
            if (coverThreeterminals) {
                int N1 = ti.rho;


                int N1_prime = Integer.MAX_VALUE;

                for (int i = 0; i < mst_of_terminals.size(); i++) {
                    Edge edge = mst_of_terminals.get(i);

                    Gateway u = edge.u;
                    Gateway v = edge.v;
                    if (((u == ti.s1 || u == ti.s2 || u == ti.s3) && (v != ti.s1 && v != ti.s2 && v != ti.s3)) || ((v == ti.s1 || v == ti.s2 || v == ti.s3) && (u != ti.s1 && u != ti.s2 && u != ti.s3))) {
                        if (edge.weight < N1_prime) {
                            N1_prime = edge.weight;
                        }
                    }
                }
                N1 += N1_prime;


                TriangleAndPairOfNodes bestTriangleAndPair = findBestTriangleAndPairOfNodes(ti, list2UncoveredTriangles, allCoveredNodes);
                int N2, N2_prime = Integer.MAX_VALUE;
                if (bestTriangleAndPair.triangle == null /*|| bestTriangleAndPair.pair == null*/) {
                    N2 = Integer.MAX_VALUE;
                } else {
                    if (newversion) {
                        Gateway remainingUncoveredNode = bestTriangleAndPair.pair[0];
                        for (int i = 0; i < mst_of_terminals.size(); i++) {
                            Edge edge = mst_of_terminals.get(i);

                            Gateway u = edge.u;
                            Gateway v = edge.v;
                            if (u == remainingUncoveredNode || v == remainingUncoveredNode) {
                                if (edge.weight < N2_prime) {
                                    N2_prime = edge.weight;
                                }
                            }
                        }
                        N2 = bestTriangleAndPair.weightOfTriangle + N2_prime;

                    } else {
                        N2 = bestTriangleAndPair.weight;
                    }
                }
                HashSet<Gateway> tmpAllCoveredNodes = new HashSet<Gateway>(allCoveredNodes);
                ArrayList<Gateway> uncoveredterminals = ti.getUncoveredNodes();
                ArrayList<Gateway[]> oneUncovered = findPairsOfNodes(uncoveredterminals, tmpAllCoveredNodes);
                int counter = 1;

                int N3;
                if (oneUncovered == null || oneUncovered.size() < 2) {
                    N3 = Integer.MAX_VALUE;
                } else {
                    N3 = 0;

                    while (counter <= 3) {


                        ArrayList<Edge> edges = new ArrayList<Edge>();
                        for (int i = 0; i < oneUncovered.size(); i++) {
                            Gateway[] pairs = oneUncovered.get(i);
                            if (pairs != null && pairs[0] != null && pairs[1] != null) {
                                edges.add(new Edge(pairs[0], pairs[1]));
                            }
                        }
                        Collections.sort(edges);
                        Integer w = edges.get(0).weight;
                        N3 += w;
                        Gateway[] pairs = edges.get(0).getNodes();
                        tmpAllCoveredNodes.add(pairs[0]);
                        uncoveredterminals.remove(pairs[0]);
                        oneUncovered = findPairsOfNodes(uncoveredterminals, tmpAllCoveredNodes);
                        counter++;
                    }
                }

                int min = Math.min(N1, Math.min(N2, N3));

                if (N1 == min && N1 == N2) {
                    if (N1 - N1_prime <= N2 - N2_prime) {
                        N1 = N1 - N1_prime;
                    } else {
                        N2 = N2 - N2_prime;
                    }
                }
                min = Math.min(N1, Math.min(N2, N3));
                if (N2 == min) {
                    //go with twoUncovered array

                    if (bestTriangleAndPair.triangle != null /*&& bestTriangleAndPair.pair != null*/) {

                        //new version
                        int r2_p = 0;

                        ArrayList<Gateway> coveredNodes = bestTriangleAndPair.triangle.getCoveredNodes();
                        ArrayList<Gateway> uncoveredNodes = bestTriangleAndPair.triangle.getUncoveredNodes();
                        ArrayList<Gateway[]> pairs = findPairsOfNodes(uncoveredNodes, allCoveredNodes);
                        for (int i = 0; i < pairs.size(); i++) {
                            Gateway[] gateways = pairs.get(i);
                            if (gateways != null && gateways[0] != null && gateways[1] != null) {
                                double d = NetworkUtils.EstimatedDistance(gateways[0], gateways[1]);
                                r2_p += ((int) Math.ceil(d / Constants.RNTransmissionRange) - 1);
                            }
                        }

                        r2_p += (int) (Math.ceil(NetworkUtils.EstimatedDistance(bestTriangleAndPair.pair[0], bestTriangleAndPair.pair[1]) / Constants.RNTransmissionRange) - 1);
                        if (N2 <= r2_p) {
                            bestTriangleAndPair.triangle.steinerizeTriangle(ActorsArray, RelayNodeArray);
                        } else {

                            for (int i = 0; i < pairs.size(); i++) {
                                Gateway[] gateways = pairs.get(i);
                                if (gateways != null && gateways[0] != null && gateways[1] != null) {
                                    int ccid = Math.max(gateways[0].getCcid(), gateways[1].getCcid());
                                    gateways[0].setCcid(ccid);
                                    gateways[1].setCcid(ccid);
                                    NetworkUtils.fillGap(gateways[0], gateways[1], true, ccid, ActorsArray, RelayNodeArray);
                                    double d = NetworkUtils.EstimatedDistance(gateways[0], gateways[1]);
                                }
                            }
                        }
                        rearrangeLists(bestTriangleAndPair.triangle, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);


                    }
                } else if (N3 == min) {

                    tmpAllCoveredNodes = new HashSet<Gateway>(allCoveredNodes);
                    uncoveredterminals = ti.getUncoveredNodes();
                    oneUncovered = findPairsOfNodes(uncoveredterminals, tmpAllCoveredNodes);


                    ArrayList<Edge> edges = new ArrayList<Edge>();
                    for (int i = 0; i < oneUncovered.size(); i++) {
                        Gateway[] pairs = oneUncovered.get(i);
                        if (pairs != null && pairs[0] != null && pairs[1] != null) {
                            edges.add(new Edge(pairs[0], pairs[1]));
                        }
                    }
                    Collections.sort(edges);
                    Integer w = edges.get(0).weight;
                    N3 += w;
                    Gateway[] pairs = edges.get(0).getNodes();
                    int ccid = pairs[1].getCcid();
                    pairs[0].setCcid(ccid);
                    NetworkUtils.fillGap(pairs[0], pairs[1], true, ccid, ActorsArray, RelayNodeArray);
                    updateLists(list3UncoveredTriangles, list2UncoveredTriangles, list1UncoveredTriangles, allCoveredNodes, pairs[0]);

                } else if (N1 == min) {
                    // go with ti
                    ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    if (Constants.LOG_TRIANGLE) {
                        System.out.println("R1 : " + ti);
                    }
                    rearrangeLists(ti, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);

                }

            } else {

                int r1 = ti.rho;
                ArrayList<Gateway> uncovered = ti.getUncoveredNodes();
                double distanceBetweenUncoveredNodes = NetworkUtils.EstimatedDistance(uncovered.get(0), uncovered.get(1));
                //uc stands for uncovered-covered pairs
                Gateway[][] ucpairs = new Gateway[2][2];
                int[] ucweights = new int[2];
                for (int i = 0; i < uncovered.size(); i++) {
                    Gateway uncoveredNode = uncovered.get(i);
                    double minDist = Double.MAX_VALUE;
                    Gateway bestMatch = null;
                    for (Gateway g : allCoveredNodes) {
                        double d = NetworkUtils.EstimatedDistance(uncoveredNode, g);
                        if (d <= minDist) {
                            minDist = d;
                            bestMatch = g;
                        }
                    }
                    ucpairs[i][0] = uncoveredNode;
                    ucpairs[i][1] = bestMatch;
                    ucweights[i] = (int) Math.ceil(minDist / Constants.RNTransmissionRange) - 1;
                }

                int sum1 = ucweights[0] + ucweights[1];

                int sum2 = Math.min(ucweights[0], ucweights[1]) + (int) Math.ceil(distanceBetweenUncoveredNodes / Constants.RNTransmissionRange) - 1;
                int r2 = Math.min(sum1, sum2);

                if (r1 < r2) {
                    ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                } else {
                    if (sum1 < sum2) {
                        int ccid0 = ucpairs[0][1].getCcid();
                        int ccid1 = ucpairs[1][1].getCcid();

                        NetworkUtils.fillGap(ucpairs[0][0], ucpairs[0][1], true, ccid0, ActorsArray, RelayNodeArray);
                        NetworkUtils.fillGap(ucpairs[1][0], ucpairs[1][1], true, ccid1, ActorsArray, RelayNodeArray);

                        ucpairs[0][0].setCcid(ccid0);
                        ucpairs[1][0].setCcid(ccid1);
                    } else {
                        int ccid;
                        NetworkUtils.fillGap(uncovered.get(0), uncovered.get(1), true, ActorsArray, RelayNodeArray);
                        if (ucweights[0] < ucweights[1]) {
                            NetworkUtils.fillGap(ucpairs[0][0], ucpairs[0][1], true, ActorsArray, RelayNodeArray);
                            ccid = ucpairs[0][1].getCcid();
                        } else {
                            NetworkUtils.fillGap(ucpairs[1][0], ucpairs[1][1], true, ActorsArray, RelayNodeArray);
                            ccid = ucpairs[1][1].getCcid();
                        }
                        uncovered.get(0).setCcid(ccid);
                        uncovered.get(1).setCcid(ccid);
                    }
                }
                list2UncoveredTriangles.remove(0);
                rearrangeLists(ti, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);
            }

            Collections.sort(list2UncoveredTriangles);
        }

        // federating partitions

        ArrayList<Gateway> tobePruned = new ArrayList<Gateway>();

        for (int i = 0; i < list3UncoveredTriangles_backup.size(); i++) {
            ArrayList<Integer> component_id_map = new ArrayList<Integer>();
            Triangle ti = list3UncoveredTriangles_backup.get(i);
            Gateway s1 = ti.s1, s2 = ti.s2, s3 = ti.s3;
            int cid1 = s1.getCcid();
            int cid2 = s2.getCcid();
            int cid3 = s3.getCcid();
            if (cid1 == cid2 && cid1 == cid3) {
                //do nothing they are all in same component
            } else {
                if (cid1 == 0) {
                    if (cid2 == cid3) {
                        //compare triangle and mst(single edge)
                        Edge s1s2 = new Edge(s1, s2);
                        Edge s1s3 = new Edge(s1, s3);
                        Edge minEdge = (s1s2.weight < s1s3.weight) ? s1s2 : s1s3;
                        Edge minMSTEdge = findMinMSTEdgeInducedAt(mst_of_terminals, s1);
                        ArrayList<Gateway> rns = getRNsConnectingUV(s2, s3, true);
                        if (ti.rho <= minEdge.weight + rns.size()) {
                            if (ti.p_weight > ti.f_weight) {
                                tobePruned.addAll(rns);
                                ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                            } else {

                                if (minMSTEdge.weight < minEdge.weight) {
                                    if (minMSTEdge.u == s1) {
                                        s1.setCcid(minMSTEdge.v.getCcid());
                                        NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s1.getCcid(), ActorsArray, RelayNodeArray);
                                    } else {
                                        s1.setCcid(minMSTEdge.u.getCcid());
                                        NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s1.getCcid(), ActorsArray, RelayNodeArray);
                                    }
                                } else {
                                    s1.setCcid(cid2);
                                    NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                                }
                            }
                        } else {
                            if (minMSTEdge.weight < minEdge.weight) {
                                if (minMSTEdge.u == s1) {
                                    s1.setCcid(minMSTEdge.v.getCcid());
                                    NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s1.getCcid(), ActorsArray, RelayNodeArray);
                                } else {

                                }
                            } else {
                                s1.setCcid(cid2);
                                NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                            }
                        }

                    } else {
                        //steinerize ti
                        component_id_map.add(cid2);
                        component_id_map.add(cid3);
                        ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    }
                } else if (cid2 == 0) {
                    if (cid1 == cid3) {
                        //compare triangle and mst(single edge)
                        Edge s1s2 = new Edge(s1, s2);
                        Edge s2s3 = new Edge(s2, s3);
                        Edge minEdge = (s1s2.weight < s2s3.weight) ? s1s2 : s2s3;
                        Edge minMSTEdge = findMinMSTEdgeInducedAt(mst_of_terminals, s2);
                        ArrayList<Gateway> rns = getRNsConnectingUV(s1, s3, true);
                        if (ti.rho <= minEdge.weight + rns.size()) {
                            if (ti.p_weight > ti.f_weight) {
                                tobePruned.addAll(rns);
                                ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                            } else {

                                if (minMSTEdge.weight < minEdge.weight) {
                                    if (minMSTEdge.u == s2) {
                                        s2.setCcid(minMSTEdge.v.getCcid());
                                        NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s2.getCcid(), ActorsArray, RelayNodeArray);
                                    } else {
                                        s2.setCcid(minMSTEdge.u.getCcid());
                                        NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s2.getCcid(), ActorsArray, RelayNodeArray);
                                    }
                                } else {
                                    s2.setCcid(cid1);
                                    NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid1, ActorsArray, RelayNodeArray);
                                }
                            }
                        } else {
                            if (minMSTEdge.weight < minEdge.weight) {
                                if (minMSTEdge.u == s2) {
                                    s2.setCcid(minMSTEdge.v.getCcid());
                                    NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s2.getCcid(), ActorsArray, RelayNodeArray);
                                } else {
                                    s2.setCcid(minMSTEdge.u.getCcid());
                                    NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s2.getCcid(), ActorsArray, RelayNodeArray);
                                }
                            } else {
                                s2.setCcid(cid1);
                                NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid1, ActorsArray, RelayNodeArray);
                            }
                        }
                    } else {
                        //steinerize ti
                        component_id_map.add(cid1);
                        component_id_map.add(cid3);
                        ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    }
                } else if (cid3 == 0) {
                    if (cid1 == cid2) {
                        //compare triangle and mst(single edge)
                        Edge s1s3 = new Edge(s1, s3);
                        Edge s2s3 = new Edge(s2, s3);
                        Edge minEdge = (s1s3.weight < s2s3.weight) ? s1s3 : s2s3;
                        Edge minMSTEdge = findMinMSTEdgeInducedAt(mst_of_terminals, s3);
                        ArrayList<Gateway> rns = getRNsConnectingUV(s1, s2, true);
                        if (ti.rho <= minEdge.weight + rns.size()) {
                            if (ti.p_weight > ti.f_weight) {
                                tobePruned.addAll(rns);
                                ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                            } else {
                                if (minMSTEdge.weight < minEdge.weight) {
                                    if (minMSTEdge.u == s3) {
                                        s3.setCcid(minMSTEdge.v.getCcid());
                                        NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s3.getCcid(), ActorsArray, RelayNodeArray);
                                    } else {
                                        s3.setCcid(minMSTEdge.u.getCcid());
                                        NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s3.getCcid(), ActorsArray, RelayNodeArray);
                                    }
                                } else {
                                    s3.setCcid(cid2);
                                    NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                                }
                            }
                        } else {
                            if (minMSTEdge.weight < minEdge.weight) {
                                if (minMSTEdge.u == s3) {
                                    s3.setCcid(minMSTEdge.v.getCcid());
                                    NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s3.getCcid(), ActorsArray, RelayNodeArray);
                                } else {
                                    s3.setCcid(minMSTEdge.u.getCcid());
                                    NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s3.getCcid(), ActorsArray, RelayNodeArray);
                                }
                            } else {
                                s3.setCcid(cid2);
                                NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                            }
                        }
                    } else {
                        //steinerize ti
                        component_id_map.add(cid1);
                        component_id_map.add(cid2);
                        ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    }
                } else { // the segments are all covered by a component
                    if (cid1 == cid2 && cid1 != cid3) {
                        component_id_map.add(cid1);
                        component_id_map.add(cid3);

                        Edge s1s3 = new Edge(s1, s3);
                        Edge s2s3 = new Edge(s2, s3);
                        Edge minEdge = (s1s3.weight < s2s3.weight) ? s1s3 : s2s3;

                        Edge mst_edge_cid1_cid3 = get_mst_edge_having_different_ccid(mst_of_terminals, cid1, cid3);

                        ArrayList<Edge> mst_edges_of_s3 = get_mst_edges_induced_at(mst_of_terminals, s3);
                        int mst_cost = Integer.MAX_VALUE;
                        Gateway corresponding_node = null;
                        for (int j = 0; j < mst_edges_of_s3.size(); j++) {
                            Edge edge = mst_edges_of_s3.get(j);
                            if (edge.u == s3) {
                                if (edge.v.getCcid() == cid2 && mst_cost > edge.weight) {
                                    mst_cost = edge.weight;
                                    corresponding_node = edge.v;
                                }
                            } else {
                                if (edge.u.getCcid() == cid2 && mst_cost > edge.weight) {
                                    mst_cost = edge.weight;
                                    corresponding_node = edge.u;
                                }
                            }
                        }


                        ArrayList<Gateway> rns = getRNsConnectingUV(s1, s2, true);
                        if (ti.rho <= minEdge.weight + rns.size()) {
                            if (ti.p_weight > ti.f_weight) {
                                if (mst_cost < ti.f_weight) {
                                    if (mst_edge_cid1_cid3 != null && mst_edge_cid1_cid3.weight < mst_cost) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid3.u, mst_edge_cid1_cid3.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(s3, corresponding_node, true, cid2, ActorsArray, RelayNodeArray);
                                    }
                                } else {
                                    if (mst_edge_cid1_cid3 != null && mst_edge_cid1_cid3.weight < ti.f_weight) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid3.u, mst_edge_cid1_cid3.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        tobePruned.addAll(rns);
                                        ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                                    }
                                }
                            } else {
                                if (mst_cost < minEdge.weight) {
                                    if (mst_edge_cid1_cid3 != null && mst_edge_cid1_cid3.weight < mst_cost) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid3.u, mst_edge_cid1_cid3.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(s3, corresponding_node, true, cid2, ActorsArray, RelayNodeArray);
                                    }
                                } else {
                                    if (mst_edge_cid1_cid3 != null && mst_edge_cid1_cid3.weight < minEdge.weight) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid3.u, mst_edge_cid1_cid3.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                                    }
                                }
                            }
                        } else {
                            if (mst_cost < minEdge.weight) {
                                if (mst_edge_cid1_cid3 != null && mst_edge_cid1_cid3.weight < mst_cost) {
                                    NetworkUtils.fillGap(mst_edge_cid1_cid3.u, mst_edge_cid1_cid3.v, true, cid2, ActorsArray, RelayNodeArray);
                                } else {
                                    NetworkUtils.fillGap(s3, corresponding_node, true, cid2, ActorsArray, RelayNodeArray);
                                }
                            } else {
                                if (mst_edge_cid1_cid3 != null && mst_edge_cid1_cid3.weight < minEdge.weight) {
                                    NetworkUtils.fillGap(mst_edge_cid1_cid3.u, mst_edge_cid1_cid3.v, true, cid2, ActorsArray, RelayNodeArray);
                                } else {
                                    NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                                }
                            }
                        }


                    } else if (cid1 == cid3 && cid1 != cid2) {
                        component_id_map.add(cid2);
                        component_id_map.add(cid3);
                        Edge s1s2 = new Edge(s1, s2);
                        Edge s2s3 = new Edge(s2, s3);
                        Edge minEdge = (s1s2.weight < s2s3.weight) ? s1s2 : s2s3;

                        Edge mst_edge_cid1_cid2 = get_mst_edge_having_different_ccid(mst_of_terminals, cid1, cid2);

                        ArrayList<Edge> mst_edges_of_s2 = get_mst_edges_induced_at(mst_of_terminals, s2);
                        int mst_cost = Integer.MAX_VALUE;
                        Gateway corresponding_node = null;
                        for (int j = 0; j < mst_edges_of_s2.size(); j++) {
                            Edge edge = mst_edges_of_s2.get(j);
                            if (edge.u == s2) {
                                if (edge.v.getCcid() == cid1 && mst_cost > edge.weight) {
                                    mst_cost = edge.weight;
                                    corresponding_node = edge.v;
                                }
                            } else {
                                if (edge.u.getCcid() == cid1 && mst_cost > edge.weight) {
                                    mst_cost = edge.weight;
                                    corresponding_node = edge.u;
                                }
                            }
                        }

                        ArrayList<Gateway> rns = getRNsConnectingUV(s1, s3, true);
                        if (ti.rho <= minEdge.weight + rns.size()) {
                            if (ti.p_weight > ti.f_weight) {
                                if (mst_cost < ti.f_weight) {
                                    if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < mst_cost) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(s2, corresponding_node, true, cid1, ActorsArray, RelayNodeArray);
                                    }
                                } else {

                                    if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < ti.f_weight) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid1, ActorsArray, RelayNodeArray);
                                    } else {
                                        tobePruned.addAll(rns);
                                        ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                                    }
                                }
                            } else {
                                if (mst_cost < minEdge.weight) {
                                    if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < mst_cost) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid1, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(s2, corresponding_node, true, cid1, ActorsArray, RelayNodeArray);
                                    }


                                } else {
                                    if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < minEdge.weight) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid1, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid1, ActorsArray, RelayNodeArray);
                                    }
                                }
                            }
                        } else {
                            if (mst_cost < minEdge.weight) {
                                if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < mst_cost) {
                                    NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid1, ActorsArray, RelayNodeArray);
                                } else {
                                    NetworkUtils.fillGap(s2, corresponding_node, true, cid1, ActorsArray, RelayNodeArray);
                                }

                            } else {
                                if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < minEdge.weight) {
                                    NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid1, ActorsArray, RelayNodeArray);
                                } else {
                                    NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid1, ActorsArray, RelayNodeArray);
                                }
                            }
                        }


                    } else if (cid2 == cid3 && cid2 != cid1) {

                        //compare triangle and mst(single edge)
                        component_id_map.add(cid1);
                        component_id_map.add(cid2);
                        Edge s1s2 = new Edge(s1, s2);
                        Edge s1s3 = new Edge(s1, s3);
                        Edge minEdge = (s1s2.weight < s1s3.weight) ? s1s2 : s1s3;

                        Edge mst_edge_cid1_cid2 = get_mst_edge_having_different_ccid(mst_of_terminals, cid1, cid2);

                        ArrayList<Edge> mst_edges_of_s1 = get_mst_edges_induced_at(mst_of_terminals, s1);
                        int mst_cost = Integer.MAX_VALUE;
                        Gateway corresponding_node = null;
                        for (int j = 0; j < mst_edges_of_s1.size(); j++) {
                            Edge edge = mst_edges_of_s1.get(j);
                            if (edge.u == s1) {
                                if (edge.v.getCcid() == cid2 && mst_cost > edge.weight) {
                                    mst_cost = edge.weight;
                                    corresponding_node = edge.v;
                                }
                            } else {
                                if (edge.u.getCcid() == cid2 && mst_cost > edge.weight) {
                                    mst_cost = edge.weight;
                                    corresponding_node = edge.u;
                                }
                            }
                        }

                        ArrayList<Gateway> rns = getRNsConnectingUV(s2, s3, true);
                        if (ti.rho <= minEdge.weight + rns.size()) {
                            if (ti.p_weight > ti.f_weight) {
                                if (mst_cost < ti.f_weight) {
                                    if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < mst_cost) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(s1, corresponding_node, true, cid2, ActorsArray, RelayNodeArray);
                                    }
                                } else {
                                    if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < ti.f_weight) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        tobePruned.addAll(rns);
                                        ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                                    }
                                }
                            } else {
                                if (mst_cost < minEdge.weight) {
                                    if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < mst_cost) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(s1, corresponding_node, true, cid2, ActorsArray, RelayNodeArray);
                                    }

                                } else {
                                    if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < minEdge.weight) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                                    }
                                }
                            }
                        } else {
                            if (mst_cost < minEdge.weight) {
                                if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < mst_cost) {
                                    NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid2, ActorsArray, RelayNodeArray);
                                } else {
                                    NetworkUtils.fillGap(s1, corresponding_node, true, cid2, ActorsArray, RelayNodeArray);
                                }

                            } else {
                                if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < minEdge.weight) {
                                    NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid2, ActorsArray, RelayNodeArray);
                                } else {
                                    NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                                }

                            }
                        }

                    } else {
                        // all in different connected components
                        component_id_map.add(cid1);
                        component_id_map.add(cid2);
                        component_id_map.add(cid3);
                        ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    }
                }
            }

            Collections.sort(component_id_map);
            int min = Integer.MAX_VALUE, m1 = Integer.MAX_VALUE, m2 = Integer.MAX_VALUE;
            if (component_id_map.size() == 3) {
                min = component_id_map.get(0);
                m1 = component_id_map.get(1);
                m2 = component_id_map.get(2);
            } else if (component_id_map.size() == 2) {
                min = component_id_map.get(0);
                m1 = component_id_map.get(1);
            }
            if (min != Integer.MAX_VALUE) {
                for (int j = 0; j < NodeList.size(); j++) {
                    Gateway gateway = NodeList.get(j);
                    if (gateway.getCcid() == m1 || gateway.getCcid() == m2) {
                        gateway.setCcid(min);
                    }
                }
            }

        }
        RelayNodeArray.removeAll(tobePruned);
        System.out.println("");
        optimizeMSTEdges(NodeList, RelayNodeArray);
        //federatePartitions(mst_of_terminals);


    }

    private Edge findMinMSTEdgeInducedAt(ArrayList<Edge> mst_of_terminals, Gateway s) {
        int minWeight = Integer.MAX_VALUE;
        Edge minEdge = null;
        for (int i = 0; i < mst_of_terminals.size(); i++) {
            Edge edge = mst_of_terminals.get(i);
            if (edge.u == s || edge.v == s) {
                if (edge.weight < minWeight) {
                    minEdge = edge;
                    minWeight = edge.weight;
                }
            }
        }
        return minEdge;
    }


    private void optimizeMSTEdges(ArrayList<Gateway> segmentList, ArrayList<Gateway> RelayList) {
//        ArrayList<Edge> edges = NetworkUtils.runKruskal(segmentList);
//        ArrayList<Edge> edges = new ArrayList<Edge>();
        HashMap<Edge, Integer> pruned = new HashMap<Edge, Integer>();
        ArrayList<HashSet> sedges = new ArrayList<HashSet>();
        ArrayList<HashSet<Gateway>> possible_triangle_set = new ArrayList<HashSet<Gateway>>();
        ArrayList<Triangle> possible_triangles = new ArrayList<Triangle>();

        for (int i = 0; i < segmentList.size() - 1; i++) {
            Gateway u = segmentList.get(i);
            for (int j = i + 1; j < segmentList.size(); j++) {
                Gateway v = segmentList.get(j);
                ArrayList<Gateway> connectingRNS = getRNsConnectingUV(u, v, true);
                if (connectingRNS.size() != 0) {
                    pruned.put(new Edge(u, v), 0);
                    HashSet<Gateway> s = new HashSet<Gateway>();
                    s.add(u);
                    s.add(v);
                    sedges.add(s);
                }
            }
        }


        for (int i = 0; i < sedges.size() - 1; i++) {
            HashSet s1 = sedges.get(i);
            for (int j = i + 1; j < sedges.size(); j++) {
                HashSet<Gateway> unionSet = new HashSet<Gateway>();
                HashSet s2 = sedges.get(j);
                unionSet.addAll(s1);
                unionSet.addAll(s2);
                possible_triangle_set.add(unionSet);
            }
        }

        for (int i = possible_triangle_set.size() - 1; i >= 0; i--) {
            HashSet<Gateway> s = possible_triangle_set.get(i);
            if (s.size() == 3) {
                Object[] gateways = s.toArray();
                Triangle t = new Triangle((Gateway) gateways[0], (Gateway) gateways[1], (Gateway) gateways[2]);
                int diff_t = t.p_weight - t.f_weight;
                int index = 0;
                for (int j = 0; j < possible_triangles.size(); j++) {

                    Triangle ti = possible_triangles.get(j);
                    int diff_ti = ti.p_weight - ti.f_weight;
                    if (diff_t <= diff_ti) {
                        index++;
                    } else {
                        break;
                    }
                }
                if (diff_t > 0) {
                    possible_triangles.add(index, t);
                }
            }
        }

        for (int i = 0; i < possible_triangles.size(); i++) {
            Triangle t = possible_triangles.get(i);
            Edge s1s2 = new Edge(t.s1, t.s2);
            Edge s1s3 = new Edge(t.s1, t.s3);
            Edge s2s3 = new Edge(t.s2, t.s3);

            Integer b_s1s2 = is_edge_pruned(pruned, s1s2);
            Integer b_s1s3 = is_edge_pruned(pruned, s1s3);
            Integer b_s2s3 = is_edge_pruned(pruned, s2s3);

            if (b_s1s2 == -1) {
                if (b_s1s3 == 0 && b_s2s3 == 0) {
                    t.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    set_edge_pruned(pruned, s1s3);
                    set_edge_pruned(pruned, s2s3);
                }
            } else if (b_s1s3 == -1) {
                if (b_s1s2 == 0 && b_s2s3 == 0) {
                    t.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    set_edge_pruned(pruned, s1s2);
                    set_edge_pruned(pruned, s2s3);
                }
            } else if (b_s2s3 == -1) {
                if (b_s1s2 == 0 && b_s1s3 == 0) {
                    t.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    set_edge_pruned(pruned, s1s2);
                    set_edge_pruned(pruned, s1s3);
                }
            } else {
                System.out.println("ERROR-CYCLE DETECTED");
            }
            System.out.println("");
        }
        Iterator iter = pruned.keySet().iterator();
        while (iter.hasNext()) {
            Edge edge = (Edge) iter.next();
            if (pruned.get(edge) == 1) {
                ArrayList<Gateway> nodes = getRNsConnectingUV(edge.u, edge.v, true);
                RelayNodeArray.removeAll(nodes);
            }
        }
//        for (Edge edge1 : pruned.keySet()) {
//            if (pruned.get(edge1) == 1) {
//                RelayNodeArray.removeAll(getRNsConnectingUV(edge1.u, edge1.v, true));
//            }
//        }
        ArrayList<Gateway> allNodes = new ArrayList<Gateway>(RelayNodeArray);
        allNodes.addAll(ActorsArray);
        NetworkUtils.calculateActorNeighborhoods(allNodes, Constants.RNTransmissionRange);
        NetworkUtils.attachRelayWithGateways(ActorsArray, RelayNodeArray);

        System.out.println("");

    }

    private int is_edge_pruned(HashMap<Edge, Integer> pruned, Edge edge) {
        for (Edge edge1 : pruned.keySet()) {
            if ((edge.u == edge1.u && edge.v == edge1.v) || (edge.u == edge1.v && edge.v == edge1.u))
                return pruned.get(edge1);
        }
        return -1;
    }

    /**
     * Old version but still has some usages
     *
     * @param pruned
     * @param edge
     */
    private void set_edge_pruned(HashMap<Edge, Integer> pruned, Edge edge) {
        for (Edge edge1 : pruned.keySet()) {
            if ((edge.u == edge1.u && edge.v == edge1.v) || (edge.u == edge1.v && edge.v == edge1.u))
                pruned.put(edge1, 1);
        }
    }

    private void set_edge_pruned(ArrayList<Edge> listOfEdges, Edge edge) {
        for (int i = 0; i < listOfEdges.size(); i++) {
            Edge edge1 = listOfEdges.get(i);
            if ((edge.u == edge1.u && edge.v == edge1.v) || (edge.u == edge1.v && edge.v == edge1.u))
                edge1.isPruned = true;
        }
    }

    private ArrayList<Edge> get_mst_edges_induced_at(ArrayList<Edge> mst, Gateway u) {
        ArrayList<Edge> result = new ArrayList<Edge>();
        for (int i = 0; i < mst.size(); i++) {
            Edge edge = mst.get(i);
            if (edge.u == u || edge.v == u)
                result.add(edge);
        }
        return result;
    }

    private Edge get_mst_edge_having_different_ccid(ArrayList<Edge> mst, int ccid1, int ccid2) {
        for (int i = 0; i < mst.size(); i++) {
            Edge edge = mst.get(i);
            if ((edge.u.getCcid() == ccid1 && edge.v.getCcid() == ccid2) || (edge.u.getCcid() == ccid2 && edge.v.getCcid() == ccid1))
                return edge;
        }
        return null;
    }

    public void check_topologies() {
        for (int p = 3; p <= 15; p++) {
            for (int e = 1; e <= 50; e++) {
                String filename = p + "_" + e + ".dat";
                process_kLCAOutput(filename, p);
                ArrayList<Gateway> terminals_klca = new ArrayList<Gateway>(ActorsArray);
//                System.out.println("Processing : "+filename);
                reload(filename);

                ArrayList<Gateway> terminals_festa = new ArrayList<Gateway>(ActorsArray);

                if (terminals_festa.size() != terminals_klca.size()) {
                    System.out.println(filename + " is changed");
                } else {
                    for (int i = 0; i < terminals_festa.size(); i++) {
                        Gateway g_f = terminals_festa.get(i);
                        boolean b_i = false;
                        for (int j = 0; j < terminals_klca.size(); j++) {
                            Gateway g_k = terminals_klca.get(j);
                            if (Math.abs(g_k.getX() - g_f.getX()) < Math.pow(10, -4) && Math.abs(g_k.getY() - g_f.getY()) < Math.pow(10, -4)) {
                                b_i = true;
                            }
                        }
                        if (!b_i) {
                            System.out.println(filename + " is changed");
                            break;
                        }
                    }
                }

            }
        }
        System.out.println("Done");
    }

    // Third version of Festa 25 October 2010

    public void TriangleAproach3(ArrayList<Gateway> NodeList) {

        boolean newversion = true;

        HashSet<Gateway> allCoveredNodes = new HashSet<Gateway>();
        ArrayList<Triangle> list3UncoveredTriangles = new ArrayList<Triangle>();
        ArrayList<Triangle> list3UncoveredTriangles_backup = new ArrayList<Triangle>();
        ArrayList<Triangle> list2UncoveredTriangles = new ArrayList<Triangle>();
        ArrayList<Triangle> list1UncoveredTriangles = new ArrayList<Triangle>();

        int nextCCID = 1;
        ArrayList<Edge> mst_of_terminals = NetworkUtils.runKruskal(NodeList);

        for (int i = 0; i < NodeList.size() - 2; i++) {
            Gateway si = NodeList.get(i);
            for (int j = i + 1; j < NodeList.size() - 1; j++) {
                Gateway sj = NodeList.get(j);
                for (int k = j + 1; k < NodeList.size(); k++) {
                    Gateway sk = NodeList.get(k);
                    Triangle t = new Triangle(si, sj, sk);
                    list3UncoveredTriangles.add(t);
                }
            }
        }
        Collections.sort(list3UncoveredTriangles);
        list3UncoveredTriangles_backup.addAll(list3UncoveredTriangles);

        ArrayList<Triangle> threeStars = new ArrayList<Triangle>();
        for (int i = 0; i < list3UncoveredTriangles.size(); i++) {
            Triangle triangle = list3UncoveredTriangles.get(i);
            if (triangle.rho == 1) {
                threeStars.add(triangle);
            } else {
                break;
            }
        }

        threeStars = eliminateEquivalent3Stars(threeStars);

        for (int i = 0; i < threeStars.size(); i++) {
            Triangle triangle = threeStars.get(i);
            int cid1 = triangle.s1.getCcid();
            int cid2 = triangle.s2.getCcid();
            int cid3 = triangle.s3.getCcid();
            if ((!(cid1 == cid2 && cid1 == cid3 && cid1 != 0 && cid2 != 0 && cid3 != 0))) {
                if (!((cid1 == 0 && cid2 != 0 && cid2 == cid3) || (cid2 == 0 && cid1 != 0 && cid1 == cid3) || (cid3 == 0 && cid2 != 0 && cid2 == cid1))) {
                    triangle.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    rearrangeLists(triangle, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);
                }
            }
        }
        Collections.sort(list2UncoveredTriangles);
        while (!(list3UncoveredTriangles.isEmpty() && list2UncoveredTriangles.isEmpty())) {
//            DT_Triangle ti = list3UncoveredTriangles.get(0);
            Triangle ti = null, tj3 = null, tj2 = null;
            boolean coverThreeterminals = false;
            if (!list3UncoveredTriangles.isEmpty())
                tj3 = getBestCandidateTriangle(list3UncoveredTriangles, allCoveredNodes);
            if (!list2UncoveredTriangles.isEmpty()) {
                tj2 = list2UncoveredTriangles.get(0);
            }
            if (tj3 != null && tj2 != null) {
                if (tj3.rho < tj2.rho) {
                    ti = tj3;
                    coverThreeterminals = true;
                } else {
                    ti = tj2;
                    coverThreeterminals = false;
                }
            } else {
                if (tj3 != null && tj2 == null) {
                    ti = tj3;
                    coverThreeterminals = true;
                } else if (tj3 == null && tj2 != null) {
                    ti = tj2;
                    coverThreeterminals = false;
                } else {
                    System.out.println("ERROR in triangles");
                    System.exit(0);
                }
            }
            if (coverThreeterminals) {
                int N1 = ti.rho;


                int N1_prime = Integer.MAX_VALUE;

                for (int i = 0; i < mst_of_terminals.size(); i++) {
                    Edge edge = mst_of_terminals.get(i);

                    Gateway u = edge.u;
                    Gateway v = edge.v;
                    if (((u == ti.s1 || u == ti.s2 || u == ti.s3) && (v != ti.s1 && v != ti.s2 && v != ti.s3)) || ((v == ti.s1 || v == ti.s2 || v == ti.s3) && (u != ti.s1 && u != ti.s2 && u != ti.s3))) {
                        if (edge.weight < N1_prime) {
                            N1_prime = edge.weight;
                        }
                    }
                }
                N1 += N1_prime;


                TriangleAndPairOfNodes bestTriangleAndPair = findBestTriangleAndPairOfNodes(ti, list2UncoveredTriangles, allCoveredNodes);
                int N2, N2_prime = Integer.MAX_VALUE;
                if (bestTriangleAndPair.triangle == null /*|| bestTriangleAndPair.pair == null*/) {
                    N2 = Integer.MAX_VALUE;
                } else {
                    if (newversion) {
                        Gateway remainingUncoveredNode = bestTriangleAndPair.pair[0];
                        for (int i = 0; i < mst_of_terminals.size(); i++) {
                            Edge edge = mst_of_terminals.get(i);

                            Gateway u = edge.u;
                            Gateway v = edge.v;
                            if (u == remainingUncoveredNode || v == remainingUncoveredNode) {
                                if (edge.weight < N2_prime) {
                                    N2_prime = edge.weight;
                                }
                            }
                        }
                        N2 = bestTriangleAndPair.weightOfTriangle + N2_prime;

                    } else {
                        N2 = bestTriangleAndPair.weight;
                    }
                }
                HashSet<Gateway> tmpAllCoveredNodes = new HashSet<Gateway>(allCoveredNodes);
                ArrayList<Gateway> uncoveredterminals = ti.getUncoveredNodes();
                ArrayList<Gateway[]> oneUncovered = findPairsOfNodes(uncoveredterminals, tmpAllCoveredNodes);
                int counter = 1;

                int N3;
                if (oneUncovered == null || oneUncovered.size() < 2) {
                    N3 = Integer.MAX_VALUE;
                } else {
                    N3 = 0;

                    while (counter <= 3) {


                        ArrayList<Edge> edges = new ArrayList<Edge>();
                        for (int i = 0; i < oneUncovered.size(); i++) {
                            Gateway[] pairs = oneUncovered.get(i);
                            if (pairs != null && pairs[0] != null && pairs[1] != null) {
                                edges.add(new Edge(pairs[0], pairs[1]));
                            }
                        }
                        Collections.sort(edges);
                        Integer w = edges.get(0).weight;
                        N3 += w;
                        Gateway[] pairs = edges.get(0).getNodes();
                        tmpAllCoveredNodes.add(pairs[0]);
                        uncoveredterminals.remove(pairs[0]);
                        oneUncovered = findPairsOfNodes(uncoveredterminals, tmpAllCoveredNodes);
                        counter++;
                    }
                }

                int min = Math.min(N1, Math.min(N2, N3));

                if (N1 == min && N1 == N2) {
                    if (N1 - N1_prime <= N2 - N2_prime) {
                        N1 = N1 - N1_prime;
                    } else {
                        N2 = N2 - N2_prime;
                    }
                }
                min = Math.min(N1, Math.min(N2, N3));
                if (N2 == min) {
                    //go with twoUncovered array

                    if (bestTriangleAndPair.triangle != null /*&& bestTriangleAndPair.pair != null*/) {

                        //new version
                        int r2_p = 0;

                        ArrayList<Gateway> coveredNodes = bestTriangleAndPair.triangle.getCoveredNodes();
                        ArrayList<Gateway> uncoveredNodes = bestTriangleAndPair.triangle.getUncoveredNodes();
                        ArrayList<Gateway[]> pairs = findPairsOfNodes(uncoveredNodes, allCoveredNodes);
                        for (int i = 0; i < pairs.size(); i++) {
                            Gateway[] gateways = pairs.get(i);
                            if (gateways != null && gateways[0] != null && gateways[1] != null) {
                                double d = NetworkUtils.EstimatedDistance(gateways[0], gateways[1]);
                                r2_p += ((int) Math.ceil(d / Constants.RNTransmissionRange) - 1);
                            }
                        }

                        r2_p += (int) (Math.ceil(NetworkUtils.EstimatedDistance(bestTriangleAndPair.pair[0], bestTriangleAndPair.pair[1]) / Constants.RNTransmissionRange) - 1);
                        if (N2 <= r2_p) {
                            bestTriangleAndPair.triangle.steinerizeTriangle(ActorsArray, RelayNodeArray);
                        } else {

                            for (int i = 0; i < pairs.size(); i++) {
                                Gateway[] gateways = pairs.get(i);
                                if (gateways != null && gateways[0] != null && gateways[1] != null) {
                                    int ccid = Math.max(gateways[0].getCcid(), gateways[1].getCcid());
                                    gateways[0].setCcid(ccid);
                                    gateways[1].setCcid(ccid);
                                    NetworkUtils.fillGap(gateways[0], gateways[1], true, ccid, ActorsArray, RelayNodeArray);
                                    double d = NetworkUtils.EstimatedDistance(gateways[0], gateways[1]);
                                }
                            }
                        }
                        rearrangeLists(bestTriangleAndPair.triangle, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);


                    }
                } else if (N3 == min) {

                    tmpAllCoveredNodes = new HashSet<Gateway>(allCoveredNodes);
                    uncoveredterminals = ti.getUncoveredNodes();
                    oneUncovered = findPairsOfNodes(uncoveredterminals, tmpAllCoveredNodes);


                    ArrayList<Edge> edges = new ArrayList<Edge>();
                    for (int i = 0; i < oneUncovered.size(); i++) {
                        Gateway[] pairs = oneUncovered.get(i);
                        if (pairs != null && pairs[0] != null && pairs[1] != null) {
                            edges.add(new Edge(pairs[0], pairs[1]));
                        }
                    }
                    Collections.sort(edges);
                    Integer w = edges.get(0).weight;
                    N3 += w;
                    Gateway[] pairs = edges.get(0).getNodes();
                    int ccid = pairs[1].getCcid();
                    pairs[0].setCcid(ccid);
                    NetworkUtils.fillGap(pairs[0], pairs[1], true, ccid, ActorsArray, RelayNodeArray);
                    updateLists(list3UncoveredTriangles, list2UncoveredTriangles, list1UncoveredTriangles, allCoveredNodes, pairs[0]);

                } else if (N1 == min) {
                    // go with ti
                    ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    if (Constants.LOG_TRIANGLE) {
                        System.out.println("R1 : " + ti);
                    }
                    rearrangeLists(ti, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);

                }

            } else {

                int r1 = ti.rho;
                ArrayList<Gateway> uncovered = ti.getUncoveredNodes();
                double distanceBetweenUncoveredNodes = NetworkUtils.EstimatedDistance(uncovered.get(0), uncovered.get(1));
                //uc stands for uncovered-covered pairs
                Gateway[][] ucpairs = new Gateway[2][2];
                int[] ucweights = new int[2];
                for (int i = 0; i < uncovered.size(); i++) {
                    Gateway uncoveredNode = uncovered.get(i);
                    double minDist = Double.MAX_VALUE;
                    Gateway bestMatch = null;
                    for (Gateway g : allCoveredNodes) {
                        double d = NetworkUtils.EstimatedDistance(uncoveredNode, g);
                        if (d <= minDist) {
                            minDist = d;
                            bestMatch = g;
                        }
                    }
                    ucpairs[i][0] = uncoveredNode;
                    ucpairs[i][1] = bestMatch;
                    ucweights[i] = (int) Math.ceil(minDist / Constants.RNTransmissionRange) - 1;
                }

                int sum1 = ucweights[0] + ucweights[1];

                int sum2 = Math.min(ucweights[0], ucweights[1]) + (int) Math.ceil(distanceBetweenUncoveredNodes / Constants.RNTransmissionRange) - 1;
                int r2 = Math.min(sum1, sum2);

                if (r1 < r2) {
                    ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    list2UncoveredTriangles.remove(0);
                    rearrangeLists(ti, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);
                } else {
                    int ccid;
                    Gateway u, v;
                    if (ucweights[0] < ucweights[1]) {
                        u = ucpairs[0][0];
                        v = ucpairs[0][1];
                    } else {
                        u = ucpairs[1][0];
                        v = ucpairs[1][1];
                    }
                    ccid = v.getCcid();

                    if (sum1 < sum2) {
                        NetworkUtils.fillGap(u, v, true, ccid, ActorsArray, RelayNodeArray);
                        u.setCcid(ccid);
                        updateLists(list3UncoveredTriangles, list2UncoveredTriangles, list1UncoveredTriangles, allCoveredNodes, u);

                    } else {
                        if (((int) Math.ceil(distanceBetweenUncoveredNodes / Constants.RNTransmissionRange) - 1) < Math.min(ucweights[0], ucweights[1])) {

                            NetworkUtils.fillGap(uncovered.get(0), uncovered.get(1), true, ActorsArray, RelayNodeArray);
                            NetworkUtils.fillGap(u, v, true, ccid, ActorsArray, RelayNodeArray);
                            u.setCcid(ccid);
                            uncovered.get(0).setCcid(ccid);
                            uncovered.get(1).setCcid(ccid);
                            list2UncoveredTriangles.remove(0);
                            rearrangeLists(ti, allCoveredNodes, list1UncoveredTriangles, list2UncoveredTriangles, list3UncoveredTriangles);
                        } else {
                            NetworkUtils.fillGap(u, v, true, ccid, ActorsArray, RelayNodeArray);
                            u.setCcid(ccid);
                            updateLists(list3UncoveredTriangles, list2UncoveredTriangles, list1UncoveredTriangles, allCoveredNodes, u);
                        }
                    }
                }

            }

            Collections.sort(list2UncoveredTriangles);
        }

        // federating partitions

        ArrayList<Gateway> tobePruned = federateConnectedComponents(list3UncoveredTriangles_backup, mst_of_terminals, NodeList);
        RelayNodeArray.removeAll(tobePruned);
//        System.out.println("");
        optimizeMSTEdges(NodeList, RelayNodeArray);
        //federatePartitions(mst_of_terminals);


    }

    private ArrayList<Gateway> federateConnectedComponents(ArrayList<Triangle> list3UncoveredTriangles, ArrayList<Edge> mst_of_terminals, ArrayList<Gateway> NodeList) {
        ArrayList<Gateway> tobePruned = new ArrayList<Gateway>();

        for (int i = 0; i < list3UncoveredTriangles.size(); i++) {
            ArrayList<Integer> component_id_map = new ArrayList<Integer>();
            Triangle ti = list3UncoveredTriangles.get(i);
            Gateway s1 = ti.s1, s2 = ti.s2, s3 = ti.s3;
            int cid1 = s1.getCcid();
            int cid2 = s2.getCcid();
            int cid3 = s3.getCcid();
            if (cid1 == cid2 && cid1 == cid3) {
                //do nothing they are all in same component
            } else {
                if (cid1 == 0) {
                    if (cid2 == cid3) {
                        //compare triangle and mst(single edge)
                        Edge s1s2 = new Edge(s1, s2);
                        Edge s1s3 = new Edge(s1, s3);
                        Edge minEdge = (s1s2.weight < s1s3.weight) ? s1s2 : s1s3;
                        Edge minMSTEdge = findMinMSTEdgeInducedAt(mst_of_terminals, s1);
                        ArrayList<Gateway> rns = getRNsConnectingUV(s2, s3, true);
                        if (ti.rho <= minEdge.weight + rns.size()) {
                            if (ti.p_weight > ti.f_weight) {
                                if (minMSTEdge.weight >= ti.f_weight) {
                                    tobePruned.addAll(rns);
                                    ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                                } else {
                                    s1.setCcid(cid2);
                                    NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                                }
                            } else {

                                if (minMSTEdge.weight < minEdge.weight) {
                                    if (minMSTEdge.u == s1) {
                                        s1.setCcid(minMSTEdge.v.getCcid());
                                        NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s1.getCcid(), ActorsArray, RelayNodeArray);
                                    } else {
                                        s1.setCcid(minMSTEdge.u.getCcid());
                                        NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s1.getCcid(), ActorsArray, RelayNodeArray);
                                    }
                                } else {
                                    s1.setCcid(cid2);
                                    NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                                }
                            }
                        } else {
                            if (minMSTEdge.weight < minEdge.weight) {
                                if (minMSTEdge.u == s1) {
                                    s1.setCcid(minMSTEdge.v.getCcid());
                                    NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s1.getCcid(), ActorsArray, RelayNodeArray);
                                } else {

                                }
                            } else {
                                s1.setCcid(cid2);
                                NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                            }
                        }

                    } else {
                        //steinerize ti
                        component_id_map.add(cid2);
                        component_id_map.add(cid3);
                        ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    }
                } else if (cid2 == 0) {
                    if (cid1 == cid3) {
                        //compare triangle and mst(single edge)
                        Edge s1s2 = new Edge(s1, s2);
                        Edge s2s3 = new Edge(s2, s3);
                        Edge minEdge = (s1s2.weight < s2s3.weight) ? s1s2 : s2s3;
                        Edge minMSTEdge = findMinMSTEdgeInducedAt(mst_of_terminals, s2);
                        ArrayList<Gateway> rns = getRNsConnectingUV(s1, s3, true);
                        if (ti.rho <= minEdge.weight + rns.size()) {
                            if (ti.p_weight > ti.f_weight) {
                                if (minMSTEdge.weight >= ti.f_weight) {
                                    tobePruned.addAll(rns);
                                    ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                                } else {
                                    s2.setCcid(cid1);
                                    NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, cid1, ActorsArray, RelayNodeArray);
                                }
                            } else {

                                if (minMSTEdge.weight < minEdge.weight) {
                                    if (minMSTEdge.u == s2) {
                                        s2.setCcid(minMSTEdge.v.getCcid());
                                        NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s2.getCcid(), ActorsArray, RelayNodeArray);
                                    } else {
                                        s2.setCcid(minMSTEdge.u.getCcid());
                                        NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s2.getCcid(), ActorsArray, RelayNodeArray);
                                    }
                                } else {
                                    s2.setCcid(cid1);
                                    NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid1, ActorsArray, RelayNodeArray);
                                }
                            }
                        } else {
                            if (minMSTEdge.weight < minEdge.weight) {
                                if (minMSTEdge.u == s2) {
                                    s2.setCcid(minMSTEdge.v.getCcid());
                                    NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s2.getCcid(), ActorsArray, RelayNodeArray);
                                } else {
                                    s2.setCcid(minMSTEdge.u.getCcid());
                                    NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s2.getCcid(), ActorsArray, RelayNodeArray);
                                }
                            } else {
                                s2.setCcid(cid1);
                                NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid1, ActorsArray, RelayNodeArray);
                            }
                        }
                    } else {
                        //steinerize ti
                        component_id_map.add(cid1);
                        component_id_map.add(cid3);
                        ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    }
                } else if (cid3 == 0) {
                    if (cid1 == cid2) {
                        //compare triangle and mst(single edge)
                        Edge s1s3 = new Edge(s1, s3);
                        Edge s2s3 = new Edge(s2, s3);
                        Edge minEdge = (s1s3.weight < s2s3.weight) ? s1s3 : s2s3;
                        Edge minMSTEdge = findMinMSTEdgeInducedAt(mst_of_terminals, s3);
                        ArrayList<Gateway> rns = getRNsConnectingUV(s1, s2, true);
                        if (ti.rho <= minEdge.weight + rns.size()) {
                            if (ti.p_weight > ti.f_weight) {
                                if (minMSTEdge.weight >= ti.f_weight) {
                                    tobePruned.addAll(rns);
                                    ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                                } else {
                                    s3.setCcid(cid1);
                                    NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, cid1, ActorsArray, RelayNodeArray);
                                }
                            } else {
                                if (minMSTEdge.weight < minEdge.weight) {
                                    if (minMSTEdge.u == s3) {
                                        s3.setCcid(minMSTEdge.v.getCcid());
                                        NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s3.getCcid(), ActorsArray, RelayNodeArray);
                                    } else {
                                        s3.setCcid(minMSTEdge.u.getCcid());
                                        NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s3.getCcid(), ActorsArray, RelayNodeArray);
                                    }
                                } else {
                                    s3.setCcid(cid2);
                                    NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                                }
                            }
                        } else {
                            if (minMSTEdge.weight < minEdge.weight) {
                                if (minMSTEdge.u == s3) {
                                    s3.setCcid(minMSTEdge.v.getCcid());
                                    NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s3.getCcid(), ActorsArray, RelayNodeArray);
                                } else {
                                    s3.setCcid(minMSTEdge.u.getCcid());
                                    NetworkUtils.fillGap(minMSTEdge.u, minMSTEdge.v, true, s3.getCcid(), ActorsArray, RelayNodeArray);
                                }
                            } else {
                                s3.setCcid(cid2);
                                NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                            }
                        }
                    } else {
                        //steinerize ti
                        component_id_map.add(cid1);
                        component_id_map.add(cid2);
                        ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    }
                } else { // the segments are all covered by a component
                    if (cid1 == cid2 && cid1 != cid3) {
                        component_id_map.add(cid1);
                        component_id_map.add(cid3);

                        Edge s1s3 = new Edge(s1, s3);
                        Edge s2s3 = new Edge(s2, s3);
                        Edge minEdge = (s1s3.weight < s2s3.weight) ? s1s3 : s2s3;

                        Edge mst_edge_cid1_cid3 = get_mst_edge_having_different_ccid(mst_of_terminals, cid1, cid3);

                        if (mst_edge_cid1_cid3 == null)
                            continue;

                        ArrayList<Edge> mst_edges_of_s3 = get_mst_edges_induced_at(mst_of_terminals, s3);
                        int mst_cost = Integer.MAX_VALUE;
                        Gateway corresponding_node = null;
                        for (int j = 0; j < mst_edges_of_s3.size(); j++) {
                            Edge edge = mst_edges_of_s3.get(j);
                            if (edge.u == s3) {
                                if (edge.v.getCcid() == cid2 && mst_cost > edge.weight) {
                                    mst_cost = edge.weight;
                                    corresponding_node = edge.v;
                                }
                            } else {
                                if (edge.u.getCcid() == cid2 && mst_cost > edge.weight) {
                                    mst_cost = edge.weight;
                                    corresponding_node = edge.u;
                                }
                            }
                        }


                        ArrayList<Gateway> rns = getRNsConnectingUV(s1, s2, true);
                        if (ti.rho <= minEdge.weight + rns.size()) {
                            if (ti.p_weight > ti.f_weight) {
                                if (mst_cost < ti.f_weight) {
                                    if (mst_edge_cid1_cid3 != null && mst_edge_cid1_cid3.weight < mst_cost) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid3.u, mst_edge_cid1_cid3.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(s3, corresponding_node, true, cid2, ActorsArray, RelayNodeArray);
                                    }
                                } else {
                                    if (mst_edge_cid1_cid3 != null && mst_edge_cid1_cid3.weight < ti.f_weight) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid3.u, mst_edge_cid1_cid3.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        tobePruned.addAll(rns);
                                        ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                                    }
                                }
                            } else {
                                if (mst_cost < minEdge.weight) {
                                    if (mst_edge_cid1_cid3 != null && mst_edge_cid1_cid3.weight < mst_cost) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid3.u, mst_edge_cid1_cid3.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(s3, corresponding_node, true, cid2, ActorsArray, RelayNodeArray);
                                    }
                                } else {
                                    if (mst_edge_cid1_cid3 != null && mst_edge_cid1_cid3.weight < minEdge.weight) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid3.u, mst_edge_cid1_cid3.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                                    }
                                }
                            }
                        } else {
                            if (mst_cost < minEdge.weight) {
                                if (mst_edge_cid1_cid3 != null && mst_edge_cid1_cid3.weight < mst_cost) {
                                    NetworkUtils.fillGap(mst_edge_cid1_cid3.u, mst_edge_cid1_cid3.v, true, cid2, ActorsArray, RelayNodeArray);
                                } else {
                                    NetworkUtils.fillGap(s3, corresponding_node, true, cid2, ActorsArray, RelayNodeArray);
                                }
                            } else {
                                if (mst_edge_cid1_cid3 != null && mst_edge_cid1_cid3.weight < minEdge.weight) {
                                    NetworkUtils.fillGap(mst_edge_cid1_cid3.u, mst_edge_cid1_cid3.v, true, cid2, ActorsArray, RelayNodeArray);
                                } else {
                                    NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                                }
                            }
                        }


                    } else if (cid1 == cid3 && cid1 != cid2) {
                        component_id_map.add(cid2);
                        component_id_map.add(cid3);
                        Edge s1s2 = new Edge(s1, s2);
                        Edge s2s3 = new Edge(s2, s3);
                        Edge minEdge = (s1s2.weight < s2s3.weight) ? s1s2 : s2s3;

                        Edge mst_edge_cid1_cid2 = get_mst_edge_having_different_ccid(mst_of_terminals, cid1, cid2);
                        if (mst_edge_cid1_cid2 == null)
                            continue;
                        ArrayList<Edge> mst_edges_of_s2 = get_mst_edges_induced_at(mst_of_terminals, s2);
                        int mst_cost = Integer.MAX_VALUE;
                        Gateway corresponding_node = null;
                        for (int j = 0; j < mst_edges_of_s2.size(); j++) {
                            Edge edge = mst_edges_of_s2.get(j);
                            if (edge.u == s2) {
                                if (edge.v.getCcid() == cid1 && mst_cost > edge.weight) {
                                    mst_cost = edge.weight;
                                    corresponding_node = edge.v;
                                }
                            } else {
                                if (edge.u.getCcid() == cid1 && mst_cost > edge.weight) {
                                    mst_cost = edge.weight;
                                    corresponding_node = edge.u;
                                }
                            }
                        }

                        ArrayList<Gateway> rns = getRNsConnectingUV(s1, s3, true);
                        if (ti.rho <= minEdge.weight + rns.size()) {
                            if (ti.p_weight > ti.f_weight) {
                                if (mst_cost < ti.f_weight) {
                                    if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < mst_cost) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(s2, corresponding_node, true, cid1, ActorsArray, RelayNodeArray);
                                    }
                                } else {

                                    if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < ti.f_weight) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid1, ActorsArray, RelayNodeArray);
                                    } else {
                                        tobePruned.addAll(rns);
                                        ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                                    }
                                }
                            } else {
                                if (mst_cost < minEdge.weight) {
                                    if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < mst_cost) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid1, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(s2, corresponding_node, true, cid1, ActorsArray, RelayNodeArray);
                                    }


                                } else {
                                    if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < minEdge.weight) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid1, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid1, ActorsArray, RelayNodeArray);
                                    }
                                }
                            }
                        } else {
                            if (mst_cost < minEdge.weight) {
                                if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < mst_cost) {
                                    NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid1, ActorsArray, RelayNodeArray);
                                } else {
                                    NetworkUtils.fillGap(s2, corresponding_node, true, cid1, ActorsArray, RelayNodeArray);
                                }

                            } else {
                                if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < minEdge.weight) {
                                    NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid1, ActorsArray, RelayNodeArray);
                                } else {
                                    NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid1, ActorsArray, RelayNodeArray);
                                }
                            }
                        }


                    } else if (cid2 == cid3 && cid2 != cid1) {

                        //compare triangle and mst(single edge)
                        component_id_map.add(cid1);
                        component_id_map.add(cid2);
                        Edge s1s2 = new Edge(s1, s2);
                        Edge s1s3 = new Edge(s1, s3);
                        Edge minEdge = (s1s2.weight < s1s3.weight) ? s1s2 : s1s3;

                        Edge mst_edge_cid1_cid2 = get_mst_edge_having_different_ccid(mst_of_terminals, cid1, cid2);
                        if (mst_edge_cid1_cid2 == null)
                            continue;
                        ArrayList<Edge> mst_edges_of_s1 = get_mst_edges_induced_at(mst_of_terminals, s1);
                        int mst_cost = Integer.MAX_VALUE;
                        Gateway corresponding_node = null;
                        for (int j = 0; j < mst_edges_of_s1.size(); j++) {
                            Edge edge = mst_edges_of_s1.get(j);
                            if (edge.u == s1) {
                                if (edge.v.getCcid() == cid2 && mst_cost > edge.weight) {
                                    mst_cost = edge.weight;
                                    corresponding_node = edge.v;
                                }
                            } else {
                                if (edge.u.getCcid() == cid2 && mst_cost > edge.weight) {
                                    mst_cost = edge.weight;
                                    corresponding_node = edge.u;
                                }
                            }
                        }

                        ArrayList<Gateway> rns = getRNsConnectingUV(s2, s3, true);
                        if (ti.rho <= minEdge.weight + rns.size()) {
                            if (ti.p_weight > ti.f_weight) {
                                if (mst_cost < ti.f_weight) {
                                    if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < mst_cost) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(s1, corresponding_node, true, cid2, ActorsArray, RelayNodeArray);
                                    }
                                } else {
                                    if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < ti.f_weight) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        tobePruned.addAll(rns);
                                        ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                                    }
                                }
                            } else {
                                if (mst_cost < minEdge.weight) {
                                    if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < mst_cost) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(s1, corresponding_node, true, cid2, ActorsArray, RelayNodeArray);
                                    }

                                } else {
                                    if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < minEdge.weight) {
                                        NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid2, ActorsArray, RelayNodeArray);
                                    } else {
                                        NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                                    }
                                }
                            }
                        } else {
                            if (mst_cost < minEdge.weight) {
                                if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < mst_cost) {
                                    NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid2, ActorsArray, RelayNodeArray);
                                } else {
                                    NetworkUtils.fillGap(s1, corresponding_node, true, cid2, ActorsArray, RelayNodeArray);
                                }

                            } else {
                                if (mst_edge_cid1_cid2 != null && mst_edge_cid1_cid2.weight < minEdge.weight) {
                                    NetworkUtils.fillGap(mst_edge_cid1_cid2.u, mst_edge_cid1_cid2.v, true, cid2, ActorsArray, RelayNodeArray);
                                } else {
                                    NetworkUtils.fillGap(minEdge.u, minEdge.v, true, cid2, ActorsArray, RelayNodeArray);
                                }

                            }
                        }

                    } else {
                        // all in different connected components
                        component_id_map.add(cid1);
                        component_id_map.add(cid2);
                        component_id_map.add(cid3);
                        ti.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    }
                }
            }

            Collections.sort(component_id_map);
            int min = Integer.MAX_VALUE, m1 = Integer.MAX_VALUE, m2 = Integer.MAX_VALUE;
            if (component_id_map.size() == 3) {
                min = component_id_map.get(0);
                m1 = component_id_map.get(1);
                m2 = component_id_map.get(2);
            } else if (component_id_map.size() == 2) {
                min = component_id_map.get(0);
                m1 = component_id_map.get(1);
            }
            if (min != Integer.MAX_VALUE) {
                for (int j = 0; j < NodeList.size(); j++) {
                    Gateway gateway = NodeList.get(j);
                    if (gateway.getCcid() == m1 || gateway.getCcid() == m2) {
                        gateway.setCcid(min);
                    }
                }
            }

        }
        return tobePruned;
    }

    private void updateLists(ArrayList<Triangle> list3UncoveredTriangles, ArrayList<Triangle> list2UncoveredTriangles, ArrayList<Triangle> list1UncoveredTriangles, HashSet<Gateway> allCoveredNodes, Gateway coveredNode) {
        for (int i = list1UncoveredTriangles.size() - 1; i >= 0; i--) {
            Triangle triangle = list1UncoveredTriangles.get(i);
            if (triangle.getCorners().contains(coveredNode)) {
                list1UncoveredTriangles.remove(i);
            }
        }

        for (int i = list2UncoveredTriangles.size() - 1; i >= 0; i--) {
            Triangle triangle = list2UncoveredTriangles.get(i);
            if (triangle.getCorners().contains(coveredNode)) {
                list1UncoveredTriangles.add(triangle);
                list2UncoveredTriangles.remove(i);
            }
        }

        for (int i = list3UncoveredTriangles.size() - 1; i >= 0; i--) {
            Triangle triangle = list3UncoveredTriangles.get(i);
            if (triangle.getCorners().contains(coveredNode)) {
                list2UncoveredTriangles.add(triangle);
                list3UncoveredTriangles.remove(i);
            }
        }
        allCoveredNodes.add(coveredNode);
    }

    private void eliminateDuplicateNodes() {

        for (int i = RelayNodeArray.size() - 1; i >= 1; i--) {
            Gateway gi = RelayNodeArray.get(i);
            for (int j = i - 1; j >= 0; j--) {
                Gateway gj = RelayNodeArray.get(j);
                if (Math.abs(gi.getX() - gj.getX()) < Math.pow(10, -4) && Math.abs(gi.getY() - gj.getY()) < Math.pow(10, -4)) {
                    RelayNodeArray.remove(j);
                }
            }

        }
    }


    public void optimizedMSTEdges() {
        ArrayList<Edge> mst = NetworkUtils.runKruskal(ActorsArray);
        HashMap<Edge, Integer> pruned = new HashMap<Edge, Integer>();
        ArrayList<Triangle> possible_triangles = new ArrayList<Triangle>();
        ArrayList<Triangle> sorted_possible_triangles = new ArrayList<Triangle>();

        for (int i = 0; i < mst.size() - 1; i++) {
            Edge e1 = mst.get(i);
            pruned.put(e1, 0);
            for (int j = i + 1; j < mst.size(); j++) {
                Edge e2 = mst.get(j);
                Triangle t = null;
                if (e1.u == e2.u || e1.v == e2.u) {
                    t = new Triangle(e1.u, e1.v, e2.v);
                } else if (e1.u == e2.v || e1.v == e2.v) {
                    t = new Triangle(e1.u, e1.v, e2.u);
                }
                if (t != null) {
                    possible_triangles.add(t);
                }
            }
        }
        pruned.put(mst.get(mst.size() - 1), 0);


        for (int i = 0; i < possible_triangles.size(); i++) {
            Triangle t = possible_triangles.get(i);

            int diff_t = t.p_weight - t.f_weight;
            int index = 0;
            for (int j = 0; j < sorted_possible_triangles.size(); j++) {

                Triangle ti = sorted_possible_triangles.get(j);
                int diff_ti = ti.p_weight - ti.f_weight;
                if (diff_t <= diff_ti) {
                    index++;
                } else {
                    break;
                }
            }
            if (diff_t != 0) {
                sorted_possible_triangles.add(index, t);
            }
        }


        for (int i = 0; i < sorted_possible_triangles.size(); i++) {
            Triangle t = sorted_possible_triangles.get(i);
            Edge s1s2 = new Edge(t.s1, t.s2);
            Edge s1s3 = new Edge(t.s1, t.s3);
            Edge s2s3 = new Edge(t.s2, t.s3);

            Integer b_s1s2 = is_edge_pruned(pruned, s1s2);
            Integer b_s1s3 = is_edge_pruned(pruned, s1s3);
            Integer b_s2s3 = is_edge_pruned(pruned, s2s3);

            if (b_s1s2 == -1) {
                if (b_s1s3 == 0 && b_s2s3 == 0) {
                    t.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    set_edge_pruned(pruned, s1s3);
                    set_edge_pruned(pruned, s2s3);
                }
            } else if (b_s1s3 == -1) {
                if (b_s1s2 == 0 && b_s2s3 == 0) {
                    t.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    set_edge_pruned(pruned, s1s2);
                    set_edge_pruned(pruned, s2s3);
                }
            } else if (b_s2s3 == -1) {
                if (b_s1s2 == 0 && b_s1s3 == 0) {
                    t.steinerizeTriangle(ActorsArray, RelayNodeArray);
                    set_edge_pruned(pruned, s1s2);
                    set_edge_pruned(pruned, s1s3);
                }
            } else {
                System.out.println("ERROR-CYCLE DETECTED");
            }
//            System.out.println("");
        }
        Iterator iter = pruned.keySet().iterator();
        while (iter.hasNext()) {
            Edge edge = (Edge) iter.next();
            if (pruned.get(edge) == 1) {
                ArrayList<Gateway> nodes = getRNsConnectingUV(edge.u, edge.v, true);
                RelayNodeArray.removeAll(nodes);
            }
        }
        ArrayList<Gateway> allNodes = new ArrayList<Gateway>(RelayNodeArray);
        allNodes.addAll(ActorsArray);
        NetworkUtils.calculateActorNeighborhoods(allNodes, Constants.RNTransmissionRange);
        NetworkUtils.attachRelayWithGateways(ActorsArray, RelayNodeArray);


    }

    private class TriangleComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            Triangle t1 = (Triangle) o1;
            Triangle t2 = (Triangle) o2;
            int gain1 = t1.p_weight - t1.f_weight;
            int gain2 = t2.p_weight - t2.f_weight;
            if (gain1 > gain2) {
                return -1;
            } else if (gain1 < gain2) {
                return 1;
            } else {
                return (t1.rho >= t2.rho) ? -1 : 1;
            }
        }
    }

    public void runOptimizedSMSTApproach() {

        ArrayList<Edge> mst = NetworkUtils.runKruskal(ActorsArray);
        ArrayList<Edge> listOfAllEdges = new ArrayList<Edge>(mst);
        ArrayList<Triangle> possible_triangles = new ArrayList<Triangle>();

        for (int i = 0; i < mst.size() - 1; i++) {
            Edge e1 = mst.get(i);
            for (int j = i + 1; j < mst.size(); j++) {
                Edge e2 = mst.get(j);
                Triangle t = null;
                if (e1.u == e2.u || e1.v == e2.u) {
                    t = new Triangle(e1.u, e1.v, e2.v);
                } else if (e1.u == e2.v || e1.v == e2.v) {
                    t = new Triangle(e1.u, e1.v, e2.u);
                }
                if (t != null && t.p_weight > t.f_weight) {
                    possible_triangles.add(t);
                }
            }
        }

        eliminateOverlappingTriangles(possible_triangles, listOfAllEdges);

        Collections.sort(possible_triangles, new TriangleComparator());
        while (!possible_triangles.isEmpty()) {
            Triangle triangle = possible_triangles.get(0);

            Gateway s1 = triangle.s1;
            Gateway s2 = triangle.s2;
            Gateway s3 = triangle.s3;

            Edge s1s2 = findEdge(listOfAllEdges, new Edge(s1, s2));
            Edge s1s3 = findEdge(listOfAllEdges, new Edge(s1, s3));
            Edge s2s3 = findEdge(listOfAllEdges, new Edge(s2, s3));
            boolean valid = false;
            if (s1s2 != null && s1s3 != null && s2s3 == null) {
                if (!(s1s2.isPruned || s1s3.isPruned)) {
                    valid = true;
                    s1s2.isPruned = true;
                    s1s3.isPruned = true;
                }
            } else if (s1s2 != null && s1s3 == null && s2s3 != null) {
                if (!(s1s2.isPruned || s2s3.isPruned)) {
                    valid = true;
                    s1s2.isPruned = true;
                    s2s3.isPruned = true;
                }
            } else if (s1s2 == null && s1s3 != null && s2s3 != null) {
                if (!(s1s3.isPruned || s2s3.isPruned)) {
                    valid = true;
                    s1s3.isPruned = true;
                    s2s3.isPruned = true;
                }
            } else {
                System.out.println("ERROR-CYCLE");
            }
            if (valid) {

                Point2D articulation = triangle.fermatPoint;
                Gateway rn = new Gateway(RelayNodeArray.size());
                rn.setX(articulation.getX());
                rn.setY(articulation.getY());
                rn.isRelay = true;
                ArrayList<Gateway> allNodes = new ArrayList<Gateway>(ActorsArray);
                allNodes.addAll(RelayNodeArray);
                listOfAllEdges.add(new Edge(s1, rn));
                listOfAllEdges.add(new Edge(s2, rn));
                listOfAllEdges.add(new Edge(s3, rn));

                updateTriangList(possible_triangles, listOfAllEdges, allNodes, triangle, rn);

                RelayNodeArray.add(rn);
            }
            Collections.sort(possible_triangles, new TriangleComparator());
        }

        for (int i = 0; i < listOfAllEdges.size(); i++) {
            Edge edge = listOfAllEdges.get(i);
            if (!edge.isPruned && edge.weight > 0) {
                NetworkUtils.fillGap(edge.u, edge.v, true, ActorsArray, RelayNodeArray);
            }
        }
//        runSMSTApproach();
//        optimizedMSTEdges();
    }

    private void eliminateOverlappingTriangles(ArrayList<Triangle> possible_triangles, ArrayList<Edge> listOfAllEdges) {
        int[] pruned = new int[possible_triangles.size()];
        for (int i = possible_triangles.size() - 1; i > 0; i--) {
            Triangle ti = possible_triangles.get(i);

            Gateway s1 = ti.s1;
            Gateway s2 = ti.s2;
            Gateway s3 = ti.s3;

            Edge i_s1s2 = findEdge(listOfAllEdges, new Edge(s1, s2));
            Edge i_s1s3 = findEdge(listOfAllEdges, new Edge(s1, s3));
            Edge i_s2s3 = findEdge(listOfAllEdges, new Edge(s2, s3));

            Gateway ci = null, li = null, ri = null;

            if (i_s1s2 == null) {
                ci = s3;
                li = s1;
                ri = s2;
            }
            if (i_s1s3 == null) {
                ci = s2;
                li = s1;
                ri = s3;
            }
            if (i_s2s3 == null) {
                ci = s1;
                li = s2;
                ri = s3;
            }
            for (int j = i - 1; j >= 0; j--) {
                Triangle tj = possible_triangles.get(j);

                Edge j_s1s2 = findEdge(listOfAllEdges, new Edge(tj.s1, tj.s2));
                Edge j_s1s3 = findEdge(listOfAllEdges, new Edge(tj.s1, tj.s3));
                Edge j_s2s3 = findEdge(listOfAllEdges, new Edge(tj.s2, tj.s3));

                Gateway cj = null, lj = null, rj = null;

                if (j_s1s2 == null) {
                    cj = tj.s3;
                    lj = tj.s1;
                    rj = tj.s2;
                }
                if (j_s1s3 == null) {
                    cj = tj.s2;
                    lj = tj.s1;
                    rj = tj.s3;
                }
                if (j_s2s3 == null) {
                    cj = tj.s1;
                    lj = tj.s2;
                    rj = tj.s3;
                }

                if (ci == cj) {
                    if ((li == lj || ri == lj) && NetworkUtils.isSplitting(li, ci, ri, rj)) {
                        pruned[i] = 1;
                    }

                    if ((li == rj || ri == rj) && NetworkUtils.isSplitting(li, ci, ri, lj)) {
                        pruned[i] = 1;
                    }
                    if ((li != rj || ri != rj) || (li != lj || ri != lj)) {
                        if (NetworkUtils.isSplitting(li, ci, ri, lj) || NetworkUtils.isSplitting(li, ci, ri, rj)) {
                            pruned[i] = 1;
                            pruned[j] = 1;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < pruned.length; i++) {
            if (pruned[i] == 1)
                System.out.print("1\t");
        }
//        System.out.println("");
    }

    private Edge findEdge(ArrayList<Edge> listOfEdges, Edge edge) {
        for (int i = 0; i < listOfEdges.size(); i++) {
            Edge edge1 = listOfEdges.get(i);
            if ((edge.u == edge1.u && edge.v == edge1.v) || (edge.u == edge1.v && edge.v == edge1.u))
                return edge1;
        }
        return null;
    }

    private void updateTriangList(ArrayList<Triangle> listOfTriangles, ArrayList<Edge> listOfEdges, ArrayList<Gateway> listOfNodes, Triangle t, Gateway rn) {
        Gateway s1 = t.s1;
        Gateway s2 = t.s2;
        Gateway s3 = t.s3;
        Triangle t1 = new Triangle(s1, rn, s3);
        Triangle t2 = new Triangle(s1, rn, s2);
        Triangle t3 = new Triangle(s2, rn, s3);

        if (t1.p_weight > t1.f_weight) {
            listOfTriangles.add(t1);
        }
        if (t2.p_weight > t2.f_weight) {
            listOfTriangles.add(t2);
        }
        if (t3.p_weight > t3.f_weight) {
            listOfTriangles.add(t3);
        }

        for (int i = 0; i < listOfNodes.size(); i++) {
            Gateway gateway = listOfNodes.get(i);
            if (gateway != s1 && gateway != s2 && gateway != s3 && gateway != rn) {
                Edge s1g = findEdge(listOfEdges, new Edge(s1, gateway));
                Edge s2g = findEdge(listOfEdges, new Edge(s2, gateway));
                Edge s3g = findEdge(listOfEdges, new Edge(s3, gateway));
                Triangle nt = null;
                if (s1g != null && !s1g.isPruned) {
                    nt = new Triangle(s1, rn, gateway);
                } else if (s2g != null && !s2g.isPruned) {
                    nt = new Triangle(s2, rn, gateway);
                } else if (s3g != null && !s3g.isPruned) {
                    nt = new Triangle(s3, rn, gateway);
                }
                if (nt != null && nt.p_weight > nt.f_weight) {
                    listOfTriangles.add(nt);
                }
            }
        }
        for (int i = listOfTriangles.size() - 1; i >= 0; i--) {
            Triangle ti = listOfTriangles.get(i);
            Gateway si1 = ti.s1;
            Gateway si2 = ti.s2;
            Gateway si3 = ti.s3;

            Edge si1si2 = findEdge(listOfEdges, new Edge(si1, si2));
            Edge si1si3 = findEdge(listOfEdges, new Edge(si1, si3));
            Edge si2si3 = findEdge(listOfEdges, new Edge(si2, si3));
            if ((si1si2 != null && si1si2.isPruned) || (si1si3 != null && si1si3.isPruned) || (si2si3 != null && si2si3.isPruned)) {
                listOfTriangles.remove(i);
            }
        }

    }

    public void runSMST2() {
        runSMSTApproach();
//        optimizedMSTEdges();
    }

    public void testFermatPoint() {
        for (int i = 0; i < ActorsArray.size() - 2; i++) {
            Gateway si = ActorsArray.get(i);
            for (int j = i + 1; j < ActorsArray.size() - 1; j++) {
                Gateway sj = ActorsArray.get(j);
                for (int k = j + 1; k < ActorsArray.size(); k++) {
                    Gateway sk = ActorsArray.get(k);
                    Triangle t = new Triangle(si, sj, sk);
                }
            }
        }
    }

    public boolean containCycles(ArrayList<TriangleVertex> list) {
        for (int i = 0; i < list.size(); i++) {
            TriangleVertex u = list.get(i);
            u.color = Color.WHITE;
        }
        for (int i = 0; i < list.size(); i++) {
            TriangleVertex u = list.get(i);
            if (u.color == Color.WHITE) {
                if (tvVisit(u, null)) {
                    return true;
                }
            }
        }
        return false;

    }

    public boolean tvVisit(TriangleVertex v, TriangleVertex p) {
        v.color = Color.GRAY;
        for (int i = 0; i < v.neighbors.size(); i++) {
            TriangleVertex u = v.neighbors.get(i);
            if (u != p) {
                if (u.color == Color.GRAY) {
                    return true;
                } else {
                    if (u.color == Color.WHITE) {
                        if (tvVisit(u, v)) {
                            return true;
                        }
                    }
                }
            }
        }
        v.color = Color.BLACK;
        return false;
    }

    public ArrayList<ArrayList<TriangleVertex>> TV_DFS(ArrayList<TriangleVertex> list) {
        ArrayList<ArrayList<TriangleVertex>> partitions = new ArrayList<ArrayList<TriangleVertex>>();
        for (int i = 0; i < list.size(); i++) {
            TriangleVertex u = list.get(i);
            u.color = Color.WHITE;
        }
        for (int i = 0; i < list.size(); i++) {
            TriangleVertex u = list.get(i);
            if (u.color == Color.WHITE) {
                ArrayList<TriangleVertex> tmp = new ArrayList<TriangleVertex>();
                TV_DFV(u, null, tmp);
                partitions.add(tmp);
            }
        }
        return partitions;
    }

    public void TV_DFV(TriangleVertex u, TriangleVertex p, ArrayList<TriangleVertex> dft) {
        u.color = Color.GRAY;
        TriangleVertex current = new TriangleVertex(u.node, u.id);
        current.mst1 = u.mst1;
        current.mst2 = u.mst2;
        if (p == null) {
            current.isRoot = true;
        } else {
            p.isLeaf = false;
            p.addNeighbor(current);
        }
        for (int i = 0; i < u.neighbors.size(); i++) {
            if ((u.neighbors.get(i)).color == Color.WHITE) {
                TV_DFV(u.neighbors.get(i), current, dft);
            }
        }
        u.color = Color.BLACK;
        dft.add(current);


    }

    public void findDPSolution(TriangleVertex node, int[] Min, int[] Mout, int control, int branchIndex, ArrayList<ArrayList<TriangleVertex>> alternative) {
        if (node == null) {
            return;
        }

        if (node.isLeaf) {
            if (control == 0) {
                alternative.get(branchIndex).add(node);
            }
            return;
        }

        if (control == 0) {
            if (Min[node.id] > Mout[node.id]) {
                alternative.get(branchIndex).add(node);
                for (int i = 0; i < node.neighbors.size(); i++) {
                    TriangleVertex v = node.neighbors.get(i);
                    findDPSolution(v, Min, Mout, 1, branchIndex, alternative);
                }
            } else if (Min[node.id] < Mout[node.id]) {
                for (int i = 0; i < node.neighbors.size(); i++) {
                    TriangleVertex v = node.neighbors.get(i);
                    findDPSolution(v, Min, Mout, 0, branchIndex, alternative);
                }
            } else {
                //alternative solution

                ArrayList<TriangleVertex> solution_tmp1 = new ArrayList<TriangleVertex>(alternative.get(branchIndex));
                solution_tmp1.add(node);
                alternative.add(solution_tmp1);

                int newBranchIndex = alternative.size() - 1;

                for (int i = 0; i < node.neighbors.size(); i++) {
                    TriangleVertex v = node.neighbors.get(i);
                    findDPSolution(v, Min, Mout, 0, branchIndex, alternative);
                }


                for (int i = 0; i < node.neighbors.size(); i++) {
                    TriangleVertex v = node.neighbors.get(i);
                    findDPSolution(v, Min, Mout, 1, newBranchIndex, alternative);
                }

            }
        } else if (control == 1) {
            for (int i = 0; i < node.neighbors.size(); i++) {
                TriangleVertex v = node.neighbors.get(i);
                findDPSolution(v, Min, Mout, 0, branchIndex, alternative);
            }
        }
    }

    private Edge findMstEdgeConnectingCC(int ccid1, int ccid2, HashMap<String, Edge> mstEdgeMap) {
        Iterator<String> iterator = mstEdgeMap.keySet().iterator();
        while (iterator.hasNext()) {
            Edge e = mstEdgeMap.get(iterator.next());
            if ((e.u.ccid == ccid1 && e.v.ccid == ccid2) || (e.v.ccid == ccid1 && e.u.ccid == ccid2))
                return e;
        }
        return null;
    }

    /**
     * Searches all the triangles in the list finds minimum weighted triangles connecting connected commponents having ccid={c1,c2,c3}
     *
     * @param c1
     * @param c2
     * @param c3
     * @param list
     * @return
     */
    private Triangle findTriangleConnectingCC(int c1, int c2, int c3, ArrayList<Triangle> list) {
        int minWeight = Integer.MAX_VALUE;
        Triangle result = null;
        for (int i = 0; i < list.size(); i++) {
            Triangle triangle = list.get(i);
            int s1 = triangle.s1.ccid;
            int s2 = triangle.s2.ccid;
            int s3 = triangle.s3.ccid;
            if ((s1 == c1 && s2 == c2 && s3 == c3) || (s1 == c1 && s2 == c3 && s3 == c2) || (s1 == c2 && s2 == c1 && s3 == c3) || (s1 == c2 && s2 == c3 && s3 == c1) || (s1 == c3 && s2 == c1 && s3 == c2) || (s1 == c3 && s2 == c2 && s3 == c1)) {
                if (triangle.f_weight < minWeight) {
                    minWeight = triangle.f_weight;
                    result = triangle;
                }
            }

        }
        return result;
    }

    public ArrayList<Gateway> getRelayNodeArray() {
        return RelayNodeArray;
    }

    public void testFermatPointAngle() {
        ArrayList<Triangle> dts = findDelaunayTriangles(ActorsArray);

        for (int i = 0; i < dts.size(); i++) {
            Triangle t = dts.get(i);
            int counter = 1;
            if (t.s1.getID() == 1 && t.s2.getID() == 8 && t.s3.getID() == 9) {
                Gateway g = new Gateway(ActorsArray.size() + (counter++));
                g.isRelay = true;
                g.setLocation(t.fermatPoint);
                ActorsArray.add(g);
                System.out.println(NetworkUtils.getAngle(t.s1, g, t.s2));
                System.out.println(NetworkUtils.getAngle(t.s2, g, t.s3));
                System.out.println(NetworkUtils.getAngle(t.s1, g, t.s3));

            }
        }
    }
}