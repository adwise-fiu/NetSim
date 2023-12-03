package network;

import dronenet.Arrow;
import dronenet.Drone;
import holder.MessageHolder;
import inbox.IMessage;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 * @author : Fatih Senel
 * Date: Mar 6, 2007
 * Time: 10:11:35 PM
 */
public class Gateway extends NetworkNode {
    protected MessageHolder message11Holder;
    public Color color;
    public int firstVisitTime;
    public int lastVisitTime;
    public int lineID = -1;
    protected ArrayList<Integer> labels = new ArrayList<Integer>();
    protected boolean isSelected = false;
    protected Sensor sensor = null;
    protected ArrayList<Gateway> neighborList;
    protected ArrayList<Gateway> neighborDominatorList;
    protected ArrayList<Gateway> DominateeList;
    protected ArrayList<Gateway> DominatorList;
    protected ArrayList<Gateway> EssentialLinkList;

    protected ArrayList<Integer> waitList = new ArrayList<Integer>();

    protected IMessage requestInfo;

    protected Gateway terminal = null;
    public int bfsLabel = 0;
    public int bfsLevel = 0;

    //used for Kruskal only
    protected int mstID = 0;

    protected double oriX, oriY;

    public boolean representative = false;
    public boolean isRelay = false;
    Arrow arrow;

    protected ArrayList<Gateway> InterfacePoints = new ArrayList<Gateway>();

    /**
     * if it is already a dominator then null, else points to its dominator
     */
    public Gateway dominator;

    /**
     * Parent node in the spanning tree
     */
    protected Gateway stParent;

    public double totalMovement = 0;


    //    public int index;
    public boolean flag = false;
    private boolean moved = false;

    // DT_Triangle Approach
    int ccid = 0;


    public Gateway(int i) {
        diameter = 10;
        color = Color.WHITE;
        neighborList = new ArrayList<>();
        neighborDominatorList = new ArrayList<Gateway>();
        DominateeList = new ArrayList<Gateway>();
        DominatorList = new ArrayList<Gateway>();
        EssentialLinkList = new ArrayList<Gateway>();
        networkID = -1;
        stParent = null;
        id = i;
    }


    public ArrayList<Gateway> getNeighborList() {
        return neighborList;
    }


    public ArrayList<Gateway> getDominatorList() {
        return DominatorList;
    }

    public boolean isDominator() {
        return isDominator;
    }

    public ArrayList<Gateway> getDominateeList() {
        return DominateeList;
    }

    public ArrayList<Gateway> getNeighborDominatorList() {
        return neighborDominatorList;
    }

    public void addNeighborList(Gateway node) {
        this.neighborList.add(node);
    }

    public String toString() {
//        String label = "null";
//        if (!getLabels().isEmpty())
//            label = "" + getLabels().get(0);
        return id + " - " + isRelay;
    }

    public void draw(Graphics g,  boolean showEdges, boolean showID, boolean treeEdgesOnly) {
//        if (isDominator)
//            g.setColor(Color.RED);
//        else
//            g.setColor(Color.BLUE);
        //federation-begin

        if (id == 11 /*||id == 4||id ==7 || id ==10||id == 11|| id == 12*/) {
            int r = 100;
//            g.drawOval((int)x-r,(int)y-r,2*r,2*r);
//            g.drawLine((int)x,(int)y,841 ,284);
        }
        if (id == 0 && !isRelay) {
            int r = 202;
//            g.drawOval((int)x-r,(int)y-r,2*r,2*r);
//            g.drawLine((int)x,(int)y,930,726);
        }
        if (!representative)
            g.setColor(Color.LIGHT_GRAY);
        else
            g.setColor(Color.RED);
        //federation-end
        if (!isRelay) {
            if (!isSelected)
                g.fillRect((int) (x - (diameter / 2)), (int) (y - (diameter / 2)), diameter, diameter);
            else {
                int nd = diameter + 4;
                g.fillRect((int) (x - (nd / 2)), (int) (y - (nd / 2)), nd, nd);
            }
        } else {
            g.setColor(Color.BLUE);
            g.fillRect((int) (x - (diameter / 2)), (int) (y - (diameter / 2)), diameter, diameter);
        }

        if (showID) {
            g.setColor(Color.BLACK);
            g.drawString("" + id, (int) x, (int) y + 20);
        }

        if (showEdges) {
            g.setColor(Color.BLACK);
            if (treeEdgesOnly) {
                for (int i = 0; i < EssentialLinkList.size(); i++) {
                    g.drawLine((int) x, (int) y, (int) EssentialLinkList.get(i).x, (int) EssentialLinkList.get(i).y);
                }
                if (stParent != null)
                    g.drawLine((int) x, (int) y, (int) stParent.x, (int) stParent.y);
            } else {
                for (int i = 0; i < neighborList.size(); i++) {
                    g.drawLine((int) x, (int) y, (int) neighborList.get(i).x, (int) neighborList.get(i).y);
                }
                for (int i = 0; i < InterfacePoints.size(); i++) {
                    Gateway gateway = InterfacePoints.get(i);
                    g.drawLine((int) x, (int) y, (int) gateway.x, (int) gateway.y);
                }
            }
        }
    }

    public int getDiameter() {
        return diameter;
    }


    public double getTotalMovement() {
        return totalMovement;
    }

    public void addMovement(double distance) {
        totalMovement += distance;
    }


    public Gateway getDominator() {
        return dominator;
    }

    public void setLevel(int level) {
    }

    public ArrayList<Gateway> getEssentialLinkList() {
        return EssentialLinkList;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setDominator(boolean dominator) {
        isDominator = dominator;
    }

    public void setStParent(Gateway stParent) {
        this.stParent = stParent;
    }

    public Gateway getStParent() {
        return stParent;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }


    public IMessage getRequestInfo() {
        return requestInfo;
    }

    public void setRequestInfo(IMessage requestInfo) {
        this.requestInfo = requestInfo;
    }


    public ArrayList<Integer> getWaitList() {
        return waitList;
    }


    public void setMoved(boolean b) {
        moved = b;
    }


    public boolean isMoved() {
        return moved;
    }


    public double getOriX() {
        if (!moved)
            return getX();
        return oriX;
    }

    public void setOriX(double oriX) {
        this.oriX = oriX;
    }

    public double getOriY() {
        if (!moved)
            return getY();
        return oriY;
    }

    public void setOriY(double oriY) {
        this.oriY = oriY;
    }

    public MessageHolder getMessage11Holder() {
        return message11Holder;
    }

    public void setMessage11Holder(MessageHolder message11Holder) {
        this.message11Holder = message11Holder;
    }


    public ArrayList<Gateway> getInterfacePoints() {
        return InterfacePoints;
    }

    public boolean isNeighbor(Gateway g) {
        for (int i = 0; i < neighborList.size(); i++) {
            Gateway gateway = neighborList.get(i);
            if (g == gateway)
                return true;
        }
        return false;
    }


    public int getMstID() {
        return mstID;
    }

    public void setMstID(int mstID) {
        this.mstID = mstID;
    }

    public void addLabel(int i) {
        labels.add(i);
    }


    public ArrayList<Integer> getLabels() {
        return labels;
    }

    public void removeFirstLabel() {
        if (!labels.isEmpty())
            labels.remove(0);
    }

    public boolean isRelay() {
        return isRelay;
    }


    public Gateway getTerminal() {
        return terminal;
    }

    public void setTerminal(Gateway terminal) {
        this.terminal = terminal;
    }

    public Gateway cloneGateway() {
        Gateway ng = new Gateway(id);
        ng.setX(getX());
        ng.setY(getY());
        ng.isRelay = isRelay;
        return ng;
    }


    public int getCcid() {
        return ccid;
    }

    public void setCcid(int ccid) {
        this.ccid = ccid;
    }

    public void setLocation(double x, double y) {
        setX(x);
        setY(y);
    }

    public void setLocation(Point2D p) {
        setX(p.getX());
        setY(p.getY());
    }

    public Point2D getPoint2D() {
        return new Point2D.Double(getX(), getY());
    }

    public void setArrow(Arrow arrow) {
        this.arrow = arrow;
    }

    public Arrow getArrow() {
        return arrow;
    }
}
