package view;

import network.Gateway;
import network.Constants;
import network.Sensor;

import javax.swing.*;
import java.awt.*;

/**
 * User: Fatih Senel
 * Date: Apr 2, 2008
 * Time: 9:36:26 PM
 */
public class FrmSensorProperties extends JFrame {
    Sensor node;
    JLabel lblID = new JLabel("ID");
    JLabel lblNetworkID = new JLabel("Network ID");
    JLabel lblPrimaryPartition = new JLabel("Primary ");
    JLabel lblParent = new JLabel("Parent");
    JLabel lblForwardTable = new JLabel("Forward Table");
    JLabel lblNumOfMessages = new JLabel("# of Messages Sent");
    JLabel lblMessage = new JLabel("Message");

    JLabel valID = new JLabel();
    JLabel valNetworkID = new JLabel();
    JLabel valPrimaryPartition = new JLabel();
    JLabel valParent = new JLabel();
    DefaultListModel model = new DefaultListModel();
    JList valForwardTable = new JList(model);
    JLabel valNumOfMessages = new JLabel();

    DefaultListModel mdl = new DefaultListModel();
    JList valMessages = new JList(mdl);


    public FrmSensorProperties() {
        init();
        setVisible(false);
    }

    public FrmSensorProperties(Sensor node) {
        init();
        this.node = node;

    }

    private void init() {
        setLocation(Constants.FrameWidth + 5, 300);
        setTitle("Sensor Properties");
        setSize(200, 250);
        setResizable(true);

        setBorders();
        JPanel pnlMain = new JPanel(new GridBagLayout());
        pnlMain.add(lblID, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
        pnlMain.add(valID, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));

        pnlMain.add(lblNetworkID, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
        pnlMain.add(valNetworkID, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));

        pnlMain.add(lblPrimaryPartition, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
        pnlMain.add(valPrimaryPartition, new GridBagConstraints(1, 2, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));

        pnlMain.add(lblParent, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
        pnlMain.add(valParent, new GridBagConstraints(1, 3, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));

        pnlMain.add(lblNumOfMessages, new GridBagConstraints(0, 4, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
        pnlMain.add(valNumOfMessages, new GridBagConstraints(1, 4, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));

        pnlMain.add(lblMessage, new GridBagConstraints(0, 6, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
        pnlMain.add(valMessages, new GridBagConstraints(1, 6, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));


        JScrollPane sclPane = new JScrollPane(valForwardTable);
        valForwardTable.setVisibleRowCount(5);
        pnlMain.add(lblForwardTable, new GridBagConstraints(0, 5, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
        pnlMain.add(sclPane, new GridBagConstraints(1, 5, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));

        getContentPane().add(pnlMain);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

    }

    private void setBorders() {
        valID.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        valNetworkID.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        valParent.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        valNumOfMessages.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        valForwardTable.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        valPrimaryPartition.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    }

    public void setNode(Sensor sensor) {
        this.node = sensor;
        refresh();
    }

    private void updateTexts() {
        if (node != null) {
            valID.setText("" + node.getID());
            valNetworkID.setText("" + node.getNetworkID());
            valPrimaryPartition.setText("" + node.isPrimaryPartition());
            if (node.getNexthopID() != -1)
                valParent.setText("" + node.getNexthopID());
            else
                valParent.setText("NULL");

            valNumOfMessages.setText("" + node.getNumberOfMessagesTransmitted());
            ((DefaultListModel)valForwardTable.getModel()).clear();
            for (int i = 0; i < node.getForwardTable().size(); i++) {
                Sensor sns = node.getForwardTable().get(i);
                ((DefaultListModel)valForwardTable.getModel()).addElement(sns.getID());
            }

            ((DefaultListModel)valMessages.getModel()).clear();
            for(int i=0;i<node.getReceivedMessages().size();i++){
                ((DefaultListModel)valMessages.getModel()).addElement(node.getReceivedMessages().get(i));
            }

        } else {
            valID.setText("");
            valNetworkID.setText("");
            valPrimaryPartition.setText("");
            valParent.setText("");
            ((DefaultListModel)valForwardTable.getModel()).clear();
            ((DefaultListModel)valMessages.getModel()).clear();

            valNumOfMessages.setText("");
        }
    }


    public void refresh() {
        updateTexts();
        repaint();
        Thread thread = new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000); // wait 1 second...
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
//                        makeComputerMove();
                        repaint();
                    }
                });

            }
        };
        thread.start();
    }

}
