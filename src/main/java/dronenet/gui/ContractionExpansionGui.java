package dronenet.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import dronenet.*;
import dronenet.matching.BottleneckAssignmentMatchingModel;
import dronenet.matching.MatchingModel;
import geometry.AnalyticGeometry;
import geometry.Point2D;
import network.Gateway;
import utils.NetworkUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static network.Constants.maxDroneSpeed;

/**
 * @author fatihsenel
 * date: 27.03.24
 */
public class ContractionExpansionGui extends JFrame {
    private static final int ZERO_CONTRACTION = 0;
    private static final int ALPHA_CONTRACTION = 1;
    private static final int FULL_CONTRACTION = 2;
    private int selectedTime = 0;
    DroneNet droneNet;
    int index = 0;
    ContractionExpansion contractionExpansion;
    List<AlphaContractionDto> alphaContractionDtos;
    List<AlphaContractionDto> zeroContractionDtos;
    List<AlphaContractionDto> fullContractionDtos;
    List<List<List<java.awt.geom.Point2D>>> timeIndexedWaypointsAlpha;
    List<Double> timeIndexMaxTravelDistanceAlpha;

    List<List<List<java.awt.geom.Point2D>>> timeIndexedWaypointsZero;
    List<Double> timeIndexMaxTravelDistanceZero;

    List<List<List<java.awt.geom.Point2D>>> timeIndexedWaypointsFull;
    List<Double> timeIndexMaxTravelDistanceFull;
    public static int left = 20, top = 20;
    public static double scale;
//    public JCheckBoxMenuItem chkShowDFS = new JCheckBoxMenuItem("Essential Links Only");
//    public JCheckBoxMenuItem chkShowDT = new JCheckBoxMenuItem("Show Delaunay Triangulation");
//    public JCheckBoxMenuItem chkShowMstEdges = new JCheckBoxMenuItem("Show Mst Edges");

//    public JCheckBoxMenuItem chkShowSensorEdges = new JCheckBoxMenuItem("Show Sensor Edges", false);
//    public JCheckBoxMenuItem chkShowSensors = new JCheckBoxMenuItem("Show Sensors", false);
//    public JCheckBoxMenuItem chkShowActors = new JCheckBoxMenuItem("Show Actors", true);
//    public JCheckBoxMenuItem chkShowActorEdges = new JCheckBoxMenuItem("Show Actor Edges", false);


    private MainPanel pnlMain;
    DefaultListModel<String> model = new DefaultListModel<>();
    private JList snapshots = new JList(model);
    private JPanel pnlEast = new JPanel();
    SolverOutputDto selectedSolverOutput = null;
    int selectedSolverInputHeatmap = 0;
    List<List<Point2D>> gridLines = new ArrayList<>();

    List<Cell> cells = new ArrayList<>();
    private boolean highlightCoveredCells = false;
    private boolean drawGrid = true;
    private boolean showDrones = true;
    private boolean showTransmissionRange = false;
    private boolean showEdges = true;
    private boolean showMstNeighbors = false;

    private int contraction = ZERO_CONTRACTION;

    JCheckBox chkDrawGrid = new JCheckBox("Draw Grid", drawGrid);
    JCheckBox chkHighlightCoveredCells = new JCheckBox("Highlight Coverage", highlightCoveredCells);
    JCheckBox chkShowDrones = new JCheckBox("Show Drones", showDrones);
    JCheckBox chkShowTransmissionRange = new JCheckBox("Show Transmission Range", showTransmissionRange);
    JCheckBox chkShowEdges = new JCheckBox("Show Edges", showEdges);
    JCheckBox chkShowMstNeighbors = new JCheckBox("Show MST only", showMstNeighbors);
//    JCheckBox chkShowContraction = new JCheckBox("Show Contraction", showContraction);

    JComboBox<String> jcbModel;
    JComboBox<String> jcbModelId;
    JComboBox<String> jcbDrone;
    JButton btnChangeTopology;

    JRadioButton rbtnZeroContraction = new JRadioButton("No Contraction", true);
    JRadioButton rbtnAlphaContraction = new JRadioButton("Alpha Contraction");
    JRadioButton rbtnFullContraction = new JRadioButton("Full Contraction");


    public ContractionExpansionGui() throws HeadlessException {


        String[] mobilityModels = {"mslaw", "rwp"};
        String[] dronesModel = {"5", "6", "7", "8", "9", "10"};
        jcbDrone = new JComboBox<>(dronesModel);
        jcbModel = new JComboBox<>(mobilityModels);
        jcbModelId = new JComboBox<>();
        jcbModel.addActionListener(e -> {
            String[] ids;
            if (jcbModel.getSelectedIndex() == 0) {
                ids = new String[20];
            } else {
                ids = new String[50];
            }
            for (int i = 1; i <= ids.length; i++) {
                ids[i - 1] = String.valueOf(i);
            }
            jcbModelId.setModel(new DefaultComboBoxModel<>(ids));
            jcbModelId.setSelectedIndex(0);
        });
        btnChangeTopology = new JButton("Change");
        btnChangeTopology.addActionListener(e -> {
            init((String) jcbModel.getSelectedItem(), Integer.parseInt(String.valueOf(jcbDrone.getSelectedItem())), Integer.parseInt(String.valueOf(jcbModelId.getSelectedItem())));
            refresh();
        });
        jcbModel.setSelectedIndex(0);


//        init(model, d, tid);
        setTitle("DroneNet");
        initializeButtons();
        this.model.addElement("---------------");
        pnlMain = new MainPanel();
        pnlEast.add(snapshots);

        setLayout(new BorderLayout());

        setSize(Constants.FrameWidth, Constants.FrameHeight);

        getContentPane().add(new JScrollPane(pnlMain), BorderLayout.CENTER);
        getContentPane().add(new JScrollPane(pnlEast), BorderLayout.EAST);

        JPanel pnlNorth = new JPanel(new GridLayout(2, 1, 5, 5));

        JPanel pnlButtons = new JPanel(new GridLayout(2, 0, 5, 5));

        JPanel pnlButtonsOuter = new JPanel(new FlowLayout(FlowLayout.LEFT));

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbtnZeroContraction);
        bg.add(rbtnAlphaContraction);
        bg.add(rbtnFullContraction);

        pnlButtons.add(chkDrawGrid);
        pnlButtons.add(chkShowDrones);
        pnlButtons.add(chkHighlightCoveredCells);
        pnlButtons.add(chkShowTransmissionRange);
        pnlButtons.add(chkShowEdges);
        pnlButtons.add(chkShowMstNeighbors);
        pnlButtons.add(rbtnZeroContraction);
        pnlButtons.add(rbtnAlphaContraction);
        pnlButtons.add(rbtnFullContraction);


        JPanel pnlSelection = new JPanel(new GridLayout(1, 4));
        pnlSelection.add(jcbModel);
        pnlSelection.add(jcbModelId);
        pnlSelection.add(jcbDrone);
        pnlSelection.add(btnChangeTopology);

        pnlButtonsOuter.add(pnlButtons);
        pnlNorth.add(pnlSelection/*, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0)*/);
        pnlNorth.add(pnlButtonsOuter/*, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0)*/);
        getContentPane().add(pnlNorth, BorderLayout.NORTH);
        repaint();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public void refresh() {
//        pnlMain.repaint();

        repaint();
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(1000); // wait 1 second...
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //                        makeComputerMove();
            SwingUtilities.invokeLater(this::repaint);

        });
        thread.start();
    }

    private void init(String model, int d, int tid) {
        selectedTime = 0;
        droneNet = new DroneNet();
        this.model = new DefaultListModel<>();
        snapshots.setModel(this.model);
        String s1 = model + tid + "_1500";
        String filePrefix = s1 + "_" + d;
        String inputPath = "mobility_data/";
        String outPath = "experiment_data/";
        droneNet.loadMobileNodes(inputPath, s1);
        String solverInputPath = "mobility_heatmaps/";
        String fileNameSchedule = "scheduled/" + filePrefix + "_out.json";
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            droneNet.loadSolverList(outPath, filePrefix);
            contractionExpansion = new ContractionExpansion(filePrefix + "_out.json", ContractionExpansion.BOTTLENECK);

            System.out.println();

        } catch (IOException e) {
            droneNet.getListofTimeIndexedDrones().clear();
            System.err.println("No Solver Output");
            JOptionPane.showMessageDialog(this, "Nol Solver Output");
            droneNet.loadSolverInput(solverInputPath, s1);
            alphaContractionDtos = new ArrayList<>();
            zeroContractionDtos = new ArrayList<>();
            fullContractionDtos = new ArrayList<>();
        }

        List<SolverOutputDto> solverOutputDtoList = droneNet.getSolverOutputDtoList();
        droneNet.fixOverlap();

        if (solverOutputDtoList != null) {
            doMatching(BottleneckAssignmentMatchingModel.getInstance(),
                    droneNet.getSolverOutputDtoList().get(selectedTime).getConfiguration().getTransmissionRange());
            for (SolverOutputDto solverOutputDto : solverOutputDtoList) {
                this.model.addElement(solverOutputDto.getLabel());
            }
        } else {
            for (int i = 0; i < droneNet.getSolverInputDto().getHeatMaps().size(); i++) {
                this.model.addElement("Time at: " + i);
            }
        }

        alphaContractionDtos = contractionExpansion.processBottleneckWithAlphaContraction();
        zeroContractionDtos = contractionExpansion.processBottleneck(0);
        fullContractionDtos = contractionExpansion.processBottleneck(1);

        timeIndexedWaypointsAlpha = new ArrayList<>();
        timeIndexedWaypointsZero = new ArrayList<>();
        timeIndexedWaypointsFull = new ArrayList<>();

        timeIndexMaxTravelDistanceAlpha = new ArrayList<>();
        timeIndexMaxTravelDistanceZero = new ArrayList<>();
        timeIndexMaxTravelDistanceFull = new ArrayList<>();
        initContractionInfo(alphaContractionDtos, timeIndexedWaypointsAlpha, timeIndexMaxTravelDistanceAlpha);
        initContractionInfo(zeroContractionDtos, timeIndexedWaypointsZero, timeIndexMaxTravelDistanceZero);
        initContractionInfo(fullContractionDtos, timeIndexedWaypointsFull, timeIndexMaxTravelDistanceFull);

        snapshots.addListSelectionListener(e -> {
            if (snapshots.getSelectedIndex() == -1) return;
            selectedTime = snapshots.getSelectedIndex();
            if (solverOutputDtoList != null) {
                selectedSolverOutput = solverOutputDtoList.get(selectedTime);
            }
            initCells();
            refresh();
        });
        snapshots.setSelectedIndex(0);

        if (solverOutputDtoList != null) {

            selectedSolverOutput = solverOutputDtoList.get(selectedTime);

            int simWidth = selectedSolverOutput.getConfiguration().getWidth();
            scale = (0.8 * Constants.FrameWidth) / simWidth;
        } else {
            scale = (0.8 * Constants.FrameWidth) / droneNet.getSolverInputDto().getConfiguration().getWidth();
        }

        initCells();
    }

    private void initContractionInfo(List<AlphaContractionDto> contractionDtos,
                                List<List<List<java.awt.geom.Point2D>>> timeIndexedWaypoints,
                                List<Double> timeIndexMaxTravelDistance) {
        for (int i = 0; i < droneNet.getListofTimeIndexedDrones().size() - 1; i++) {
            List<Drone> source = droneNet.getListofTimeIndexedDrones().get(i);
            List<Drone> destination = droneNet.getListofTimeIndexedDrones().get(i + 1);
            AlphaContractionDto alphaContractionDto = contractionDtos.get(i);
            java.awt.geom.Point2D contractionPoint = alphaContractionDto.contractionPoint;
            List<List<java.awt.geom.Point2D>> waypoints = new ArrayList<>();
            double maxTravelDistance = 0;
            for (int j = 0; j < source.size(); j++) {
                List<java.awt.geom.Point2D> wps = new ArrayList<>();
                Drone drone = source.get(j);
                double travelDistance = 0;

                wps.add(drone.getPoint2D());

                if (alphaContractionDto.alpha > 0) {
                    double distance = AnalyticGeometry.euclideanDistance(drone.getPoint2D(), contractionPoint) * alphaContractionDto.alpha;
                    travelDistance = distance;
                    java.awt.geom.Point2D waypoint = AnalyticGeometry.getCoordinates(drone.getPoint2D(), contractionPoint, distance);
                    wps.add(waypoint);
                }
                travelDistance += AnalyticGeometry.euclideanDistance(wps.get(wps.size() - 1), destination.get(j).getPoint2D());
                wps.add(destination.get(j).getPoint2D());

                maxTravelDistance = Math.max(maxTravelDistance, travelDistance);
                waypoints.add(wps);

            }
            timeIndexMaxTravelDistance.add(maxTravelDistance);
            timeIndexedWaypoints.add(waypoints);
        }
    }


    private class MainPanel extends JPanel {


        public MainPanel() {
        }

        private void drawInfo(Graphics g, double distance, double alpha, long disconnectedMoments) {
            g.drawString("Max Distance Traveled: " + String.format("%.2f", distance) + " meters", (int) (left + scale * 900), (int) (top + scale * 2000));
            g.drawString("Duration:  " + String.format("%.2f", distance / maxDroneSpeed) + " seconds", (int) (left + scale * 900), (int) (top + scale * 2050));
            g.drawString("Alpha:  " + String.format("%.2f", alpha) , (int) (left + scale * 900), (int) (top + scale * 2100));
            g.drawString("Disconnected Moments:  " + disconnectedMoments + " seconds", (int) (left + scale * 900), (int) (top + scale * 2150));

        }

        private void drawArrow(Graphics g, int x1, int y1, int x2, int y2) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(Color.RED);
            // Draw line
            g2d.drawLine(x1, y1, x2, y2);

            // Calculate angle of the line
            double angle = Math.atan2(y2 - y1, x2 - x1);

            // Length of arrowhead
            int arrowLength = 10;

            // Arrowhead points
            int[] arrowX = {x2, (int) (x2 - arrowLength * Math.cos(angle - Math.PI / 6)),
                    (int) (x2 - arrowLength * Math.cos(angle + Math.PI / 6))};
            int[] arrowY = {y2, (int) (y2 - arrowLength * Math.sin(angle - Math.PI / 6)),
                    (int) (y2 - arrowLength * Math.sin(angle + Math.PI / 6))};

            // Draw arrowhead
            g2d.fillPolygon(new Polygon(arrowX, arrowY, 3));
        }

        public void paintComponent(Graphics g) {
//            for (Cell cell : cells) {
//                cell.draw(g);
//            }

            for (List<Point2D> line : gridLines) {
                int x0 = (int) line.get(0).getX();
                int x1 = (int) line.get(1).getX();
                int y0 = (int) line.get(0).getY();
                int y1 = (int) line.get(1).getY();
                Graphics2D g2d = (Graphics2D) g;
                BasicStroke originalStroke = (BasicStroke) g2d.getStroke();
                g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5, 5}, 0));
                g2d.setColor(Color.GRAY);
                g2d.drawLine(x0, y0, x1, y1);
                g2d.setStroke(originalStroke);
            }

            List<AlphaContractionDto> contractionDtos = null;
            List<List<List<java.awt.geom.Point2D>>> timeIndexedWaypoints = null;
            List<Double> timeIndexMaxTravelDistance = null;
            if(contraction == ZERO_CONTRACTION) {
                contractionDtos = zeroContractionDtos;
                timeIndexedWaypoints = timeIndexedWaypointsZero;
                timeIndexMaxTravelDistance = timeIndexMaxTravelDistanceZero;
            } else if(contraction == ALPHA_CONTRACTION) {
                contractionDtos = alphaContractionDtos;
                timeIndexedWaypoints = timeIndexedWaypointsAlpha;
                timeIndexMaxTravelDistance = timeIndexMaxTravelDistanceAlpha;
            } else if(contraction == FULL_CONTRACTION) {
                contractionDtos = fullContractionDtos;
                timeIndexedWaypoints = timeIndexedWaypointsFull;
                timeIndexMaxTravelDistance = timeIndexMaxTravelDistanceFull;
            }

            if(contractionDtos != null && timeIndexedWaypoints!= null && selectedTime >=0 && selectedTime <  timeIndexedWaypoints.size()){
                List<List<java.awt.geom.Point2D>> lists = timeIndexedWaypoints.get(selectedTime);
                for (List<java.awt.geom.Point2D> point2DS : lists) {
                    for (int j = 0; j < point2DS.size() - 1; j++) {
                        java.awt.geom.Point2D from = point2DS.get(j);
                        java.awt.geom.Point2D to = point2DS.get(j + 1);

                        int fx = (int) (left + scale * from.getX());
                        int fy = (int) (top + scale * from.getY());
                        int tx = (int) (left + scale * to.getX());
                        int ty = (int) (top + scale * to.getY());

//                        g.drawLine(fx, fy, tx, ty);
                        drawArrow(g, fx, fy, tx, ty);

                    }
                }
                Double maxDistance = timeIndexMaxTravelDistance.get(selectedTime);
                drawInfo(g, maxDistance, contractionDtos.get(selectedTime).alpha, contractionDtos.get(selectedTime).disconnectedMoments);
            }

//            if (contraction == ALPHA_CONTRACTION) {
//                if (alphaContractionDtos != null && selectedTime >= 0 && selectedTime < timeIndexedWaypointsAlpha.size()) {
//                    List<List<java.awt.geom.Point2D>> lists = timeIndexedWaypointsAlpha.get(selectedTime);
//                    for (List<java.awt.geom.Point2D> point2DS : lists) {
//                        for (int j = 0; j < point2DS.size() - 1; j++) {
//                            java.awt.geom.Point2D from = point2DS.get(j);
//                            java.awt.geom.Point2D to = point2DS.get(j + 1);
//
//                            int fx = (int) (left + scale * from.getX());
//                            int fy = (int) (top + scale * from.getY());
//                            int tx = (int) (left + scale * to.getX());
//                            int ty = (int) (top + scale * to.getY());
//
////                        g.drawLine(fx, fy, tx, ty);
//                            drawArrow(g, fx, fy, tx, ty);
//
//                        }
//                    }
//                    Double maxDistance = timeIndexMaxTravelDistanceAlpha.get(selectedTime);
//                    drawInfo(g, maxDistance, alphaContractionDtos.get(selectedTime).alpha, alphaContractionDtos.get(selectedTime).disconnectedMoments);
//                }
//            } else if(contraction == FULL_CONTRACTION) {
//
//            }
            if (showDrones && selectedSolverOutput != null && !droneNet.getListofTimeIndexedDrones().isEmpty()) {
                List<Drone> droneList = droneNet.getListofTimeIndexedDrones().get(selectedTime);
                int tr = selectedSolverOutput.getConfiguration().getTransmissionRange();

                for (Gateway gateway : droneList) {
                    Drone drone = (Drone) gateway;
                    drone.draw(g, showEdges, true, showMstNeighbors, showTransmissionRange, tr, 20, 20, scale);
                }

                if (selectedTime + 1 < droneNet.getListofTimeIndexedDrones().size()) {
                    List<Drone> nextState = droneNet.getListofTimeIndexedDrones().get(selectedTime + 1);
                    for (Drone gateway : nextState) {
                        gateway.drawState(g, tr, 15, 15, scale);
                    }

                    g.setColor(Color.RED);
                    double max = 0;
//                    if (contraction == ZERO_CONTRACTION) {
//                        for (int i = 0; i < droneList.size(); i++) {
//                            if (i < nextState.size()) {
//                                Drone a = droneList.get(i);
//                                Drone b = nextState.get(i);
//
//                                int x1 = (int) (left + scale * a.getX());
//                                int y1 = (int) (top + scale * a.getY());
//                                int x2 = (int) (left + scale * b.getX());
//                                int y2 = (int) (top + scale * b.getY());
//                                double distance = AnalyticGeometry.euclideanDistance(a.getX(), a.getY(), b.getX(), b.getY());
//                                max = Math.max(distance, max);
////                                g.drawLine(x1, y1, x2, y2);
//                                if (x1 != x2 || y1 != y2)
//                                    drawArrow(g, x1, y1, x2, y2);
//                            }
//                        }
//
//                        drawInfo(g, max, 0, zeroContractionDtos.get(selectedTime).disconnectedMoments);
////                        g.drawString("Max Distance Traveled: " + String.format("%.2f", max) + " meters", (int) (left + scale * 900), (int) (top + scale * 2000));
////                        g.drawString("Duration:  " + String.format("%.2f", max / maxDroneSpeed) + " seconds", (int) (left + scale * 900), (int) (top + scale * 2100));
//
//                        g.setColor(Color.BLACK);
//                    }
                }
            }
        }
    }

    private void initializeButtons() {

        chkHighlightCoveredCells.addActionListener(e -> {
            highlightCoveredCells = chkHighlightCoveredCells.isSelected();
            refresh();
        });
        chkShowDrones.addActionListener(e -> {
            showDrones = chkShowDrones.isSelected();
            chkShowEdges.setEnabled(showDrones);
            chkShowMstNeighbors.setEnabled(showDrones);
            refresh();
        });

        chkDrawGrid.addActionListener(e -> {
            drawGrid = chkDrawGrid.isSelected();
            chkHighlightCoveredCells.setEnabled(drawGrid);
            refresh();
        });

        chkShowTransmissionRange.addActionListener(e -> {
            showTransmissionRange = chkShowTransmissionRange.isSelected();
            refresh();
        });

        rbtnZeroContraction.addActionListener(e -> {
            contraction = ZERO_CONTRACTION;
            refresh();
        });
        rbtnAlphaContraction.addActionListener(e -> {
            contraction = ALPHA_CONTRACTION;
            refresh();
        });
        rbtnFullContraction.addActionListener(e -> {
            contraction = FULL_CONTRACTION;
            refresh();
        });


        chkShowEdges.addItemListener(e -> {
            showEdges = chkShowEdges.isSelected();
            chkShowMstNeighbors.setEnabled(showEdges);
            refresh();
        });

        chkShowMstNeighbors.addItemListener(e -> {
            showMstNeighbors = chkShowMstNeighbors.isSelected();
            refresh();
        });

    }

    private void initCells() {
        cells.clear();
        List<List<Double>> hm;
        Configuration config;
        if (selectedSolverOutput != null) {
            hm = selectedSolverOutput.getHeatmap();
            config = selectedSolverOutput.getConfiguration();
        } else {
            hm = droneNet.getSolverInputDto().getHeatMaps().get(selectedTime);
            config = droneNet.getSolverInputDto().getConfiguration();
        }


        int cw = (int) (config.getCellWidth() * scale);
        int leftx = left;
        int rightx = left + cw * hm.get(0).size();

        int topy = top;
        int bottomy = top + cw * hm.size();

        for (int i = 0; i < hm.size(); i++) {
            int y = top + i * cw;
            gridLines.add(Arrays.asList(new Point2D(leftx, y), new Point2D(rightx, y)));
        }
        gridLines.add(Arrays.asList(new Point2D(leftx, top + hm.size() * cw), new Point2D(rightx, top + hm.size() * cw)));


        for (int j = 0; j < hm.get(0).size(); j++) {
            int x = left + j * cw;
            gridLines.add(Arrays.asList(new Point2D(x, topy), new Point2D(x, bottomy)));
        }
        gridLines.add(Arrays.asList(new Point2D(left + hm.get(0).size() * cw, topy), new Point2D(left + hm.get(0).size() * cw, bottomy)));


        for (int i = 0; i < hm.size(); i++) {
            for (int j = 0; j < hm.get(i).size(); j++) {
                Cell cell = new Cell(top + i * cw, left + j * cw, cw);
                cell.setRowCol(i, j, j + i * hm.get(i).size());
                cell.population = (int) (hm.get(i).get(j) * droneNet.getMobileNodes().size());
                cells.add(cell);
            }
        }


        if (selectedSolverOutput != null) {
            List<List<Integer>> drones = selectedSolverOutput.getDrones();
            for (int i = 0; i < drones.size(); i++) {
                List<Integer> integers = drones.get(i);
                List<Cell> collect = cells.stream().filter(u -> u.row == integers.get(0) && u.col == integers.get(1)).collect(Collectors.toList());
                if (collect.size() == 1) {
                    collect.get(0).hasDrone = true;
                    collect.get(0).name = "d" + (i + 1);
                } else {
                    System.out.println("error");
                }
            }


            List<List<Integer>> coverage = selectedSolverOutput.getCoverage();
            for (List<Integer> integers : coverage) {
                for (Cell cell : cells) {
                    if (cell.row == integers.get(0) && cell.col == integers.get(1)) {
                        cell.isCovered = true;
                    }
                }
            }
        }
    }

    class Cell {
        int row, col, index;
        int top, left, width;
        boolean hasDrone = false;
        String name = "";
        boolean isCovered = false;
        int population;

        public Cell(int top, int left, int width) {
            this.top = top;
            this.left = left;
            this.width = width;
        }

        void setRowCol(int r, int c, int i) {
            row = r;
            col = c;
            index = i;
        }

        public void draw(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            if (drawGrid) {
                if (isCovered && highlightCoveredCells) {
                    g.setColor(Color.PINK);
                    g.fillRect(left, top, width, width);
                }
                BasicStroke originalStroke = (BasicStroke) g2d.getStroke();
                g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
                g2d.setColor(Color.BLACK);
                g2d.drawRect(left, top, width, width);
                g2d.setStroke(originalStroke);
            }
        }
    }

    private void doMatching(MatchingModel matchingModel, int transmissionRange) {
        for (int i = 0; i < droneNet.getListofTimeIndexedDrones().size() - 1; i++) {
            List<Drone> A = droneNet.getListofTimeIndexedDrones().get(i);
            List<Drone> B = droneNet.getListofTimeIndexedDrones().get(i + 1);
            if (A.size() != B.size()) continue;
            List<Drone> matched = matchingModel.doMatching(A, B);
            if (matched != null) {

                for (int j = 0; j < A.size(); j++) {
                    A.get(j).setArrow(new Arrow(A.get(j).getX(), A.get(j).getY(), B.get(j).getX(), B.get(j).getY()));
                }

                NetworkUtils.calculateActorNeighborhoods(matched, transmissionRange);
                droneNet.getListofTimeIndexedDrones().set(i + 1, matched);
            }

        }
    }

    public static void main(String[] args) throws IOException {
        new ContractionExpansionGui().setVisible(true);
    }
}
