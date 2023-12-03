package dronenet.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import dronenet.*;
import dronenet.matching.BottleneckAssignmentMatchingModel;
import dronenet.matching.MatchingModel;
import dronenet.matching.MinimizeTotalMovementMatchingModel;
import dronenet.matching.StableMarriageMatchingModel;
import geometry.AnalyticGeometry;
import network.Gateway;
import utils.DoubleUtils;
import utils.NetworkUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author fatihsenel
 * date: 07.04.22
 */
public class DroneNetGui extends JFrame {
    private int selectedTime = 0;
    DroneNet droneNet;
    int index = 0;

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
    //    private SolutionDetailsPanel pnlSouth = new SolutionDetailsPanel();
    private MoveCommandPanel pnlSouth = new MoveCommandPanel();
    SolverOutputDto selectedSolverOutput = null;
    HashMap<String, List<String>> bestSchedules = null;
    int selectedSolverInputHeatmap = 0;

    private JButton btn1;// = new JButton("Generate");
    private JButton btn3;// = new JButton("MCDS");
    private JButton btn4;// = new JButton("MCDS");
    private JButton btn5;// = new JButton("Network Identification");
    private JButton btn2;// = new JButton("Replace");
    private JButton btn6;// = new JButton("Replace");
    private JButton btn7;// = new JButton("Replace");
    private JButton btn8;// = new JButton("Replace");
    private JButton btn9;// = new JButton("Replace");

    String[] items = new String[]{"Bottleneck Assignment", "Minimize Total Sum", "Stable Marriage"};
    JComboBox<String> comboBox = new JComboBox<>(items);


    List<Cell> cells = new ArrayList<>();
    private boolean highlightCoveredCells = false;
    private boolean showPopulation = false;
    private boolean showCellDensity = true;
    private boolean drawGrid = true;
    private boolean showDrones = true;
    private boolean showTransmissionRange = false;
    private boolean showEdges = true;
    private boolean showMstNeighbors = false;

    JCheckBox chkDrawGrid = new JCheckBox("Draw Grid", drawGrid);
    JCheckBox chkHighlightCoveredCells = new JCheckBox("Highlight Coverage", highlightCoveredCells);
    JCheckBox chkShowDrones = new JCheckBox("Show Drones", showDrones);
    JCheckBox chkShowTransmissionRange = new JCheckBox("Show Transmission Range", showTransmissionRange);
    JCheckBox chkShowEdges = new JCheckBox("Show Edges", showEdges);
    JCheckBox chkShowMstNeighbors = new JCheckBox("Show MST only", showMstNeighbors);
    JRadioButton chkShowPopulation = new JRadioButton("Population", showPopulation);
    JRadioButton chkShowCellDensity = new JRadioButton("Cell Density", showCellDensity);

    JComboBox<String> jcbModel;
    JComboBox<String> jcbModelId;
    JComboBox<String> jcbDrone;
    JButton btnChangeTopology;

    public DroneNetGui() throws HeadlessException {


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
        comboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {

                match();
                refresh();
            }
        });

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
        getContentPane().add(new JScrollPane(pnlSouth), BorderLayout.SOUTH);

        JPanel pnlNorth = new JPanel(new GridLayout(2, 1, 5, 5));

        JPanel pnlButtons = new JPanel(new GridLayout(2, 0, 5, 5));

        JPanel pnlButtonsOuter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlButtons.add(chkDrawGrid);
        pnlButtons.add(chkShowDrones);
        pnlButtons.add(chkHighlightCoveredCells);
        pnlButtons.add(chkShowTransmissionRange);
        pnlButtons.add(chkShowEdges);
        pnlButtons.add(chkShowMstNeighbors);
        pnlButtons.add(chkShowPopulation);
        pnlButtons.add(chkShowCellDensity);
        pnlButtons.add(comboBox);


        JPanel pnlSelection = new JPanel(new GridLayout(1, 4));
        pnlSelection.add(jcbModel);
        pnlSelection.add(jcbModelId);
        pnlSelection.add(jcbDrone);
        pnlSelection.add(btnChangeTopology);

//        pnlButtonsOuter.add(pnlSelection);
        pnlButtonsOuter.add(pnlButtons);
        pnlNorth.add(pnlSelection/*, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0)*/);
        pnlNorth.add(pnlButtonsOuter/*, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0)*/);
        getContentPane().add(pnlNorth, BorderLayout.NORTH);
        repaint();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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
            // Read and parse the JSON file into a Java object
            CoverageReachabilityPermutationArrays yourObject = objectMapper.readValue(new File(fileNameSchedule), CoverageReachabilityPermutationArrays.class);

            // Now, 'yourObject' contains the data from the JSON file
            System.out.println(yourObject);
            bestSchedules = new HashMap<>();
            bestSchedules.put("avg",
                    Arrays.stream(yourObject.getAvg())
                            .map(CoverageReachabilityPermutation::getPermutation)
                            .collect(Collectors.toList()));
            bestSchedules.put("pairwise",
                    Arrays.stream(yourObject.getPairwise())
                            .map(CoverageReachabilityPermutation::getPermutation)
                            .collect(Collectors.toList()));
            bestSchedules.put("fullConnectivity",
                    Arrays.stream(yourObject.getFullConnectivity())
                            .map(CoverageReachabilityPermutation::getPermutation)
                            .collect(Collectors.toList()));
        } catch (IOException e) {
            bestSchedules = null;
            System.out.println("*** File Not Found ***");
        }

        try {
            droneNet.loadSolverList(outPath, filePrefix);
        } catch (IOException e) {
            droneNet.getListofTimeIndexedDrones().clear();
            System.err.println("No Solver Output");
            JOptionPane.showMessageDialog(this, "Nol Solver Output");
            droneNet.loadSolverInput(solverInputPath, s1);
        }

        List<SolverOutputDto> solverOutputDtoList = droneNet.getSolverOutputDtoList();
//        droneNet.fixOverlap();

        if (solverOutputDtoList != null) {

            // todo matching model
            match();

            for (SolverOutputDto solverOutputDto : solverOutputDtoList) {
                this.model.addElement(solverOutputDto.getLabel());
            }
        } else {
            for (int i = 0; i < droneNet.getSolverInputDto().getHeatMaps().size(); i++) {
                this.model.addElement("Time at: " + i);
            }
        }
        snapshots.addListSelectionListener(e -> {
            if (snapshots.getSelectedIndex() == -1) return;
            selectedTime = snapshots.getSelectedIndex();
            if (solverOutputDtoList != null) {
                selectedSolverOutput = solverOutputDtoList.get(selectedTime);
                if (selectedSolverOutput.getDetails() != null) {
                    //todo
//                    pnlSouth.setDetails(selectedSolverOutput.getDetails());
                }
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

    private void match() {
        if(droneNet == null || droneNet.getSolverOutputDtoList() == null || selectedTime >= droneNet.getSolverOutputDtoList().size())
            return;
        int transmissionRange = droneNet.getSolverOutputDtoList().get(selectedTime).getConfiguration().getTransmissionRange();
        if (comboBox.getSelectedIndex() == 0) {
            // bottleneck
            doMatching(BottleneckAssignmentMatchingModel.getInstance(), transmissionRange);
        } else if (comboBox.getSelectedIndex() == 1) {
            doMatching(MinimizeTotalMovementMatchingModel.getInstance(), transmissionRange);
        }else if (comboBox.getSelectedIndex() == 2) {
            doMatching(StableMarriageMatchingModel.getInstance(), transmissionRange);
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

    private void initializeButtons() {


        btn1 = new JButton("Btn1");
        btn2 = new JButton("Btn2");
        btn3 = new JButton("Btn3");
        btn4 = new JButton("Btn4");
        btn5 = new JButton("Btn5");
        btn6 = new JButton("Btn6");
        btn7 = new JButton("Btn7");
        btn8 = new JButton("Btn8");
        btn9 = new JButton("Btn9");

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

        ButtonGroup bg = new ButtonGroup();
        bg.add(chkShowPopulation);
        bg.add(chkShowCellDensity);

        chkShowTransmissionRange.addActionListener(e -> {
            showTransmissionRange = chkShowTransmissionRange.isSelected();
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
        chkShowPopulation.addItemListener(e -> {
            showPopulation = chkShowPopulation.isSelected();
            showCellDensity = !showPopulation;
            refresh();

        });
        chkShowCellDensity.addActionListener(e -> {
            showCellDensity = chkShowCellDensity.isSelected();
            showPopulation = !showCellDensity;
            refresh();

        });
    }

    private void initCells() {
        cells.clear();
//        TemporalHeatmapDto temporalHeatmap = droneNet.getTemporalHeatmap();
//        SolverOutputDto solverOutput = droneNet.getSolverOutput();
        List<List<Double>> hm;
        Configuration config;
        if (selectedSolverOutput != null) {
            hm = selectedSolverOutput.getHeatmap();
            config = selectedSolverOutput.getConfiguration();
        } else {
            hm = droneNet.getSolverInputDto().getHeatMaps().get(selectedTime);
            config = droneNet.getSolverInputDto().getConfiguration();
        }
//        double[][] hm = temporalHeatmap.getS().get(index);
//        Configuration configuration = temporalHeatmap.getConfiguration();

        int cw = (int) (config.getCellWidth() * scale);
//        int top = 20, left = 20;
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
            if (drawGrid) {
                if (isCovered && highlightCoveredCells) {
                    g.setColor(Color.PINK);
                    g.fillRect(left, top, width, width);
                }
                g.drawRect(left, top, width, width);
                g.setColor(Color.BLACK);
                if (showCellDensity) {
                    Graphics2D g2d = (Graphics2D) g.create();

//                    g2d.setColor(Color.RED);
//                    g2d.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());
//                    g2d.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);

                    Font font = new Font("Serif", Font.BOLD, 12);
                    g2d.setFont(font);
                    FontMetrics fm = g2d.getFontMetrics();
                    String text = "" + population;
                    int x = ((left + width - fm.stringWidth(text)) / 2);
                    int y = ((top + width - fm.getHeight()) / 2) + fm.getAscent();

                    g2d.setColor(Color.BLACK);
                    g2d.drawString(text, left + width / 2, top + width / 2);

                    g2d.dispose();
                }
            }
//            if (showDrones && hasDrone) {
//                g.setColor(Color.RED);
//                int cx = left + width / 2;
//                int cy = top + width / 2;
//                int radius = 4;
//                g.fillRect(cx - radius, cy - radius, radius, radius);
//                g.drawString(name, cx, 10 + cy);
//                g.setColor(Color.BLACK);
//
//            }


        }
    }


    private class MainPanel extends JPanel {


        public MainPanel() {
            /*
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
//                    int clickCount = e.getClickCount();
                    int index = network.toggleGatewayMouseOver(e.getX(), e.getY());
                    refresh();
                    if (index != -1) {
                        frmSensorProperties.setNode(null);
                        frmGatewayProperties.setNode(network.getGateway(index));
                    } else {
                        frmGatewayProperties.setNode(null);
                        int snsIndex = network.toggleSensorMouseOver(e.getX(), e.getY());
                        if (snsIndex != -1) {
                            frmSensorProperties.setNode(network.getSensor(snsIndex));
                        } else {
                            frmSensorProperties.setNode(null);
                        }
                    }

                }

                public void mousePressed(MouseEvent e) {
                }


                public void mouseReleased(MouseEvent e) {
                }


            });*/
        }

        public void paintComponent(Graphics g) {
            for (Cell cell : cells) {
                cell.draw(g);
            }

            if (showDrones && selectedSolverOutput != null && !droneNet.getListofTimeIndexedDrones().isEmpty()) {
                List<Drone> droneList = droneNet.getListofTimeIndexedDrones().get(selectedTime);
                int tr = selectedSolverOutput.getConfiguration().getTransmissionRange();

                for (Gateway gateway : droneList) {
                    Drone drone = (Drone) gateway;
                    drone.draw(g, showEdges, true, showMstNeighbors, showTransmissionRange, tr, 20, 20, scale);
                }

                if (selectedTime + 1 < droneNet.getListofTimeIndexedDrones().size()) {
                    List<Drone> nextState = droneNet.getListofTimeIndexedDrones().get(selectedTime + 1);
                    if (true) {
                        for (Drone gateway : nextState) {
                            gateway.drawState(g, tr, 20, 20, scale);
                        }

                        g.setColor(Color.RED);
                        double max = 0;
                        for (int i = 0; i < droneList.size(); i++) {
                            if (i < nextState.size()) {
                                Drone a = droneList.get(i);
                                Drone b = nextState.get(i);

                                int x1 = (int) (left + scale * a.getX());
                                int y1 = (int) (top + scale * a.getY());
                                int x2 = (int) (left + scale * b.getX());
                                int y2 = (int) (top + scale * b.getY());
                                double distance = AnalyticGeometry.euclideanDistance(a.getX(), a.getY(), b.getX(), b.getY());
                                max = Math.max(distance, max);
                                g.drawLine(x1, y1, x2, y2);
                            }
                        }

                        g.drawString(max + "", (int) (left + scale * 900), (int) (left + scale * 2000));
                        if (bestSchedules != null) {
                            String avg = bestSchedules.get("avg").get(selectedTime);
                            String pairwise = bestSchedules.get("pairwise").get(selectedTime);
                            String fullConnectivity = bestSchedules.get("fullConnectivity").get(selectedTime);
                            g.drawString("AVG: " + avg, (int) (left + scale * 900), (int) (2 * left + scale * 2000));
                            g.drawString("Pairwise: " + pairwise, (int) (left + scale * 900), (int) (3 * left + scale * 2000));
                            g.drawString("FullConnectivity: " + fullConnectivity, (int) (left + scale * 900), (int) (4 * left + scale * 2000));
                        }
                    }
                    g.setColor(Color.BLACK);
                }
            }


            if (showPopulation) {
                int timeInterval;
                if (selectedSolverOutput != null) {
                    timeInterval = selectedSolverOutput.getConfiguration().getTimeInterval();
                } else {
                    timeInterval = droneNet.getSolverInputDto().getConfiguration().getTimeInterval();
                }
                g.setColor(Color.LIGHT_GRAY);
                System.out.println(selectedTime);
                int r = 5;
                for (int i = 0; i < droneNet.getMobileNodes().size(); i++) {
                    MobileNode mobileNode = droneNet.getMobileNodes().get(i);
                    TemporalLocation temporalLocation = mobileNode.getTemporalLocationAtTime(selectedTime * timeInterval);
                    if (temporalLocation == null)
                        continue;
                    int x = (int) (left + scale * temporalLocation.x - r / 2);
                    int y = (int) (top + scale * temporalLocation.y - r / 2);
                    g.fillRect(x, y, r, r);
                }
                g.setColor(Color.BLACK);
            }
        }

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

    class MoveCommandPanel extends JPanel {
        JLabel lblDroneId = new JLabel("Drone");
        JTextField txtDroneId = new JTextField("");
        JTextField txtX = new JTextField("");
        JTextField txtY = new JTextField("");

        JComboBox cmbX = new JComboBox(new String[]{"WEST", "EAST"});
        JComboBox cmbY = new JComboBox(new String[]{"NORTH", "SOUTH"});
        JButton btnMove = new JButton("Move");
        JButton btnSchedule = new JButton("Schedule Simulate");

        public MoveCommandPanel() {
            setLayout(new GridBagLayout());
            add(lblDroneId, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
            add(txtDroneId, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
            add(txtX, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
            add(cmbX, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
            add(txtY, new GridBagConstraints(2, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
            add(cmbY, new GridBagConstraints(3, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
            add(btnMove, new GridBagConstraints(4, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
            add(btnSchedule, new GridBagConstraints(4, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
            btnMove.addActionListener(e -> {
                try {
                    int droneIndex = Integer.parseInt(txtDroneId.getText());
                    int x = Integer.parseInt(txtX.getText());
                    int y = Integer.parseInt(txtY.getText());
                    btnMoveActionPerformed(droneIndex, x, cmbX.getItemAt(cmbX.getSelectedIndex()).toString(), y, cmbY.getItemAt(cmbY.getSelectedIndex()).toString());
                } catch (NumberFormatException ignored) {
                }
            });
            btnSchedule.addActionListener(e -> {
                if (selectedTime != -1 && bestSchedules != null) {
                    String avg = bestSchedules.get("avg").get(selectedTime);
                    simulateSchedule(Arrays.stream(avg.split("-")).mapToInt(Integer::parseInt).toArray(), selectedTime);
                }

            });

        }
    }

    class SolutionDetailsPanel extends JPanel {
        SolutionDetails details;
        JLabel lblStatus = new JLabel("Status: ");
        JLabel valStatus = new JLabel("N/A");

        JLabel lblTime = new JLabel("Time: ");
        JLabel valTime = new JLabel("N/A");

        JLabel lblBestBound = new JLabel("Best Bound: ");
        JLabel valBestBound = new JLabel("N/A");

        JLabel lblMipRelativeGap = new JLabel("Mip Relative Gap: ");
        JLabel valMipRelativeGap = new JLabel("N/A");

        JLabel lblGap = new JLabel("Gap: ");
        JLabel valGap = new JLabel("N/A");
        private final DecimalFormat df = new DecimalFormat("0.00");

        public SolutionDetailsPanel() {
            setLayout(new GridBagLayout());
            add(lblStatus, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
            add(valStatus, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

            add(lblTime, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
            add(valTime, new GridBagConstraints(3, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

            add(lblBestBound, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
            add(valBestBound, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

            add(lblMipRelativeGap, new GridBagConstraints(2, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
            add(valMipRelativeGap, new GridBagConstraints(3, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

            add(lblGap, new GridBagConstraints(4, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
            add(valGap, new GridBagConstraints(5, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
        }

        public void setDetails(SolutionDetails details) {
            this.details = details;

            if (details == null) {
                valStatus.setText("N/A");
                valTime.setText("N/A");
                valBestBound.setText("N/A");
                valMipRelativeGap.setText("N/A");
                valGap.setText("N/A");
            } else {
                valStatus.setText(details.getStatus());
                valTime.setText("" + df.format(details.getTime()));
                valBestBound.setText("" + df.format(details.getBestBound()));
                valMipRelativeGap.setText("" + df.format(details.getMipRelativeGap()));
                valGap.setText("" + df.format(details.getGap()));
            }


        }
    }

    public void btnMoveActionPerformed(int droneIndex, int x, String xdir, int y, String ydir) {
        System.out.println(xdir);
        System.out.println(ydir);
        System.out.println(selectedTime);
        List<Drone> droneList = droneNet.getListofTimeIndexedDrones().get(selectedTime);
        if (droneIndex >= 0 && droneIndex < droneList.size()) {
            Gateway drone = droneList.get(droneIndex);
            if (xdir.equalsIgnoreCase("WEST")) {
                drone.setX(drone.getX() - 200 * x);
            } else {
                drone.setX(drone.getX() + 200 * x);
            }
            if (ydir.equalsIgnoreCase("NORTH")) {
                drone.setY(drone.getY() - 200 * y);
            } else {
                drone.setY(drone.getY() + 200 * y);
            }
            NetworkUtils.calculateActorNeighborhoods(droneList, droneNet.getSolverOutputDtoList().get(0).getConfiguration().getTransmissionRange());
            refresh();
        }

    }


    public CoverageReachability simulateSchedule(int[] nums, int timeStage) {

        int n = droneNet.getListofTimeIndexedDrones().get(timeStage).size();
        int timeIntervalMinute = droneNet.getSolverOutputDtoList().get(0).getConfiguration().getTimeInterval();
        Velocity[] velocities = new Velocity[n];
        int transmissionRange = droneNet.getSolverOutputDtoList().get(0).getConfiguration().getTransmissionRange();
        if (timeStage < 0 || timeStage == droneNet.getListofTimeIndexedDrones().size() - 1) return null;

        for (int k = 0; k < n; k++) {
            Gateway s = droneNet.getListofTimeIndexedDrones().get(timeStage).get(k);
            Gateway d = droneNet.getListofTimeIndexedDrones().get(timeStage + 1).get(k);
            velocities[k] = new Velocity(s.getPoint2D(), d.getPoint2D(), timeIntervalMinute);
        }

        Map<Double, Double> timeCoverageMap = new HashMap<>();
        Map<Double, Double> timeConnectivityMap = new HashMap<>();
        int[] transformationTimes = Arrays.stream(velocities).mapToInt(u -> (int) Math.ceil(AnalyticGeometry.euclideanDistance(u.start, u.end) / DroneMobilitySimulation.maxDroneSpeed)).toArray();
        int maxTransformationTimeInSeconds = Arrays.stream(transformationTimes).sum();
        int next = 0;
        int k = 0;
        for (int t = 0; t < 1200; t++) {
            double time = (timeStage * 20) + (t / 60d);
            if (t >= 1200 - maxTransformationTimeInSeconds) {
                while (next < transformationTimes.length && transformationTimes[next] == 0) {
                    next++;
                    k = 0;
                }
                k++;
                if (next >= transformationTimes.length) break;
                Gateway drone = droneNet.getListofTimeIndexedDrones().get(timeStage).get(nums[next]);
                transformationTimes[next]--;
                double d = AnalyticGeometry.euclideanDistance(drone.getX(), drone.getY(), velocities[next].end.getX(), velocities[next].end.getY());
                if (!DoubleUtils.equals(d, 0)) {
                    double distance = DroneMobilitySimulation.maxDroneSpeed * k;
                    java.awt.geom.Point2D coordinates = AnalyticGeometry.getCoordinates(velocities[next].start, velocities[next].end, distance);
                    drone.setLocation(coordinates);
                    NetworkUtils.calculateActorNeighborhoods(droneNet.getListofTimeIndexedDrones().get(timeStage), transmissionRange);
                    refresh();
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
//            timeConnectivityMap.put(time, NetworkUtils.calculateConnectivityMeasure(droneNet.getListofTimeIndexedDrones().get(timeStage), transmissionRange));
//            timeCoverageMap.put(time, calculateCoverage(movingDrones, time, transmissionRange));
        }
        return new CoverageReachability(timeCoverageMap, timeConnectivityMap);
    }


    public static void main(String[] args) throws IOException {
        new DroneNetGui().setVisible(true);
    }
}
