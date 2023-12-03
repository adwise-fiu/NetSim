package network;

import java.awt.*;
import java.util.ArrayList;

/**
 * @author : Fatih Senel
 *         Date: Nov 21, 2007
 *         Time: 4:59:23 PM
 */
public class Sensor extends NetworkNode {
    private int status = 0;
    private ArrayList<Sensor> neighborList = new ArrayList<Sensor>();
    private Gateway actor = null;
    private int dominatorID = -1;
    private int nexthopID = -1;
    private ArrayList<Sensor> forwardTable = new ArrayList<Sensor>();


    public Sensor(int i) {
        diameter = 6;
        id = i;
    }

    public int getStatus() {
        return status;
    }

    public int getDominatorID() {
        return dominatorID;
    }

    public void draw(Graphics g, boolean showClusters) {
        //if dominator
        if (status == 1) {
            g.setColor(Color.RED);
        }
        //if is border
        else if (status == 3) {
            g.setColor(Color.BLUE);
        } else {
            if (showClusters)
                g.setColor(Color.BLACK);
            else
                g.setColor(Color.GRAY);
        }
        g.fillOval((int) x - (diameter / 2), (int) y - (diameter / 2), diameter, diameter);
        g.setColor(Color.GRAY);
        if (showClusters) {
            if (status != 1) {
                for (int i = 0; i < neighborList.size(); i++) {
                    Sensor sensor = neighborList.get(i);
                    if (nexthopID == sensor.getID())
                        g.drawLine((int) x, (int) y, (int) sensor.getX(), (int) sensor.getY());                    
                }
            }
        }

    }

    public void setStatus(int i) {
        status = i;
    }


    public void setDominatorID(int i) {
        dominatorID = i;
    }

    public void addNeighbor(Sensor sns) {
        neighborList.add(sns);
    }


    public ArrayList<Sensor> getNeighborList() {
        return neighborList;
    }

    public boolean isDominator() {
        return status == 1;
    }


    public int getNexthopID() {
        return nexthopID;
    }

    public void setNexthopID(int nexthopID) {
        this.nexthopID = nexthopID;
    }


    public Gateway getActor() {
        return actor;
    }

    public void setActor(Gateway actor) {
        this.actor = actor;
    }


    public ArrayList<Sensor> getForwardTable() {
        return forwardTable;
    }

    public void addToForwardTable(Sensor sns) {
        forwardTable.add(sns);
    }


    
}
