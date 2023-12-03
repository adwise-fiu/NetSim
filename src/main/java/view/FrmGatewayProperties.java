package view;

import network.Gateway;
import network.Constants;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Fatih Senel
 *         Date: Nov 22, 2007
 *         Time: 10:29:43 PM
 */
public class FrmGatewayProperties extends JFrame {
    Gateway node;
    JLabel lblID = new JLabel("ID");
    JLabel lblNetworkID = new JLabel("Network ID");
    JLabel lblSTParent = new JLabel("ST Parent");
    JLabel lblPrimary = new JLabel("Primary Partition");
    JLabel lblNumOfMessages = new JLabel("# of Messages Sent");
    JLabel lblMessage = new JLabel("Inbox");
    JLabel lblMSTID = new JLabel("MSTID");
    JLabel lblWaitList = new JLabel("Wait List");

    JLabel valID = new JLabel();
    JLabel valNetworkID = new JLabel();
    JLabel valSTParent = new JLabel();
    JLabel valPrimary = new JLabel();
    JLabel valMSTID = new JLabel();
    JLabel valNumOfMessages = new JLabel();
    DefaultListModel model = new DefaultListModel();
    JList valMessages = new JList(model);
    DefaultListModel model2 = new DefaultListModel();
    JList valWaitList = new JList(model2);


    public FrmGatewayProperties() {
        init();
        setVisible(false);
    }

    public FrmGatewayProperties(Gateway node) {
        init();
        this.node = node;

    }

    private void init() {
        setLocation(Constants.FrameWidth + 5, 0);
        setTitle("Gateway Properties");
        setSize(200, 250);
        setResizable(true);

        setBorders();
        JPanel pnlMain = new JPanel(new GridBagLayout());
        pnlMain.add(lblID, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
        pnlMain.add(valID, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));

        pnlMain.add(lblNetworkID, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
        pnlMain.add(valNetworkID, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));

        pnlMain.add(lblSTParent, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
        pnlMain.add(valSTParent, new GridBagConstraints(1, 2, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));

        pnlMain.add(lblMSTID, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
        pnlMain.add(valMSTID, new GridBagConstraints(1, 3, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
//        pnlMain.add(lblNumOfMessages, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
//        pnlMain.add(valNumOfMessages, new GridBagConstraints(1, 3, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
//
//        pnlMain.add(lblPrimary, new GridBagConstraints(0, 4, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
//        pnlMain.add(valPrimary, new GridBagConstraints(1, 4, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
//
//        pnlMain.add(lblMessage, new GridBagConstraints(0, 5, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
//        pnlMain.add(valMessages, new GridBagConstraints(1, 5, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
//
//        pnlMain.add(lblWaitList, new GridBagConstraints(0, 6, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
//        pnlMain.add(valWaitList, new GridBagConstraints(1, 6, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));

        getContentPane().add(pnlMain);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

    }

    private void setBorders() {
        valID.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        valNetworkID.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        valSTParent.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        valNumOfMessages.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        valPrimary.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    }

    public void setNode(Gateway gateway) {
        this.node = gateway;
        refresh();
    }

    private void updateTexts() {
        if (node != null) {
            valID.setText("" + node.getID());
            valNetworkID.setText("" + node.getNetworkID());
            valMSTID.setText(""+node.getMstID());
            if (node.getStParent() != null)
                valSTParent.setText("" + node.getStParent().getID());
            else
                valSTParent.setText("NULL");

            valNumOfMessages.setText("" + node.getNumberOfMessagesTransmitted());
            valPrimary.setText("" + node.isPrimaryPartition());
            ((DefaultListModel)valMessages.getModel()).clear();
            for(int i=0;i<node.getReceivedMessages().size();i++){
                ((DefaultListModel)valMessages.getModel()).addElement(node.getReceivedMessages().get(i));
            }

            ((DefaultListModel)valWaitList.getModel()).clear();
            for(int i=0;i<node.getWaitList().size();i++){
                ((DefaultListModel)valWaitList.getModel()).addElement(node.getWaitList().get(i));
            }

        } else {
            valID.setText("");
            valNetworkID.setText("");
            valSTParent.setText("");
            valPrimary.setText("");
            valMSTID.setText("");

            valNumOfMessages.setText("");
            ((DefaultListModel)valMessages.getModel()).clear();
            ((DefaultListModel)valWaitList.getModel()).clear();
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
