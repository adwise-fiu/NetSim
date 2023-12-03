package view;

import network.SensorAndActorNetwork;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * User: Fatih Senel
 * Date: Nov 19, 2008
 * Time: 10:41:12 AM
 */
public class FrmPartitionedSegmentGenerator extends JFrame {
    SensorAndActorNetwork network;
    FrmSensorAndActorNetwork parent_view;
    JLabel lblNumOfSegments = new JLabel("Number of Segments (i.e 4-7 or 5)");
    JLabel lblNumOfNodesinaSegment = new JLabel("Number of Nodes in a segment (i.e 1-7 or 3)");
    JTextField txtNumOfSegments = new JTextField("9");
    JTextField txtNumOfNodesinaSegment = new JTextField("1-2");
    JButton btnOK = new JButton("OK");
    JButton btnCancel = new JButton("Cancel");

    JPanel pnlMain = new JPanel();


    public FrmPartitionedSegmentGenerator(SensorAndActorNetwork nt, FrmSensorAndActorNetwork view) {
        network = nt;
        parent_view=view;
        setSize(550, 180);
        setTitle("Partitioned Segment");
        pnlMain.setLayout(new BorderLayout());
        JPanel pnlNorth = new JPanel(new GridBagLayout());
        JPanel pnlSouth = new JPanel(new FlowLayout());
        JPanel pnlSouthInner = new JPanel(new GridLayout(1, 2, 5, 5));

        pnlNorth.add(lblNumOfSegments, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
        pnlNorth.add(txtNumOfSegments, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        pnlNorth.add(lblNumOfNodesinaSegment, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
        pnlNorth.add(txtNumOfNodesinaSegment, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

        pnlSouthInner.add(btnOK);
        pnlSouthInner.add(btnCancel);
        pnlSouth.add(pnlSouthInner);
        pnlMain.add(pnlNorth, BorderLayout.CENTER);
        pnlMain.add(pnlSouth, BorderLayout.SOUTH);

        addActionListeners();

        getContentPane().add(pnlMain);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void addActionListeners() {
        btnOK.addActionListener(new ButtonActionListenerHandler());
        btnCancel.addActionListener(new ButtonActionListenerHandler());
    }

    private class ButtonActionListenerHandler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == btnOK) {
                String numOfSegments = txtNumOfSegments.getText();
//                String[] arr1 = numOfSegments.split("-");
                int numOfPartitions = Integer.parseInt(numOfSegments);
//                int maxNumofSegments = Integer.parseInt(arr1[1]);

                String numOfNodes = txtNumOfNodesinaSegment.getText();
                String[] arr2 = numOfNodes.split("-");
                int minNumofNodesinaSegments = Integer.parseInt(arr2[0]);
                int maxNumofNodesinaSegments = Integer.parseInt(arr2[1]);
//                int numOfPartitions = (int)Math.round( Math.random()*maxNumofSegments+(minNumofSegments+1) );
                network.generateSegments(numOfPartitions, minNumofNodesinaSegments, maxNumofNodesinaSegments);
                dispose();
                parent_view.refresh();
            }
        }
    }

}
