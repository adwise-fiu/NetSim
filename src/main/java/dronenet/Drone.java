package dronenet;

import geometry.AnalyticGeometry;
import network.Gateway;
import network.NetworkNode;

import java.awt.Graphics;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author fatihsenel
 * date: 22.04.22
 */
public class Drone extends Gateway {
    // this attribute is only used in mcds based drone movement for keeping the
    // track of the order of placement
    private Drone incoming;
    private List<Drone> outgoing = new ArrayList<>();

    private List<Double> relocation;
    private List<Track> trajectory = new ArrayList<>();

    public Drone(int i) {
        super(i);
        relocation = new ArrayList<>();
    }


    public void draw(Graphics g, boolean showEdges, boolean showID,
                     boolean treeEdgesOnly, boolean showTransmissionRange,
                     int transmissionRange, int left, int top, double scale) {
        int radius = 10;
        int x = (int) (left + scale * this.x - radius / 2);
        int y = (int) (top + scale * this.y - radius / 2);

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
            g.setColor(Color.BLACK);
        else
            g.setColor(Color.RED);
        //federation-end
        if (!isRelay) {
            if (!isSelected)
                g.fillRect(x - (diameter / 2), y - (diameter / 2), diameter, diameter);
            else {
                int nd = diameter + 4;
                g.fillRect(x - (nd / 2), y - (nd / 2), nd, nd);
            }
        } else {
            g.setColor(Color.BLUE);
            g.fillRect(x - (diameter / 2), (int) (y - (diameter / 2)), diameter, diameter);
        }

        if (showID) {
            g.setColor(Color.RED);
            g.drawString("" + id, (int) x, (int) y + 20);
        }
        if (showTransmissionRange) {
            g.setColor(Color.BLACK);
            g.drawOval(x - transmissionRange / 2, y - transmissionRange / 2, transmissionRange, transmissionRange);
        }
        g.setColor(Color.BLACK);
        if (showEdges) {
            g.setColor(Color.BLACK);
            if (treeEdgesOnly) {
                for (Gateway gateway : EssentialLinkList) {
                    int x1 = (int) (left + scale * gateway.getX() - radius / 2);
                    int y1 = (int) (top + scale * gateway.getY() - radius / 2);
                    g.drawLine(x, y, x1, y1);
                }
                if (stParent != null) {
                    int x1 = (int) (left + scale * stParent.getX() - radius / 2);
                    int y1 = (int) (top + scale * stParent.getY() - radius / 2);
                    g.drawLine(x, y, x1, y1);
                }
            } else {
                for (Gateway value : neighborList) {
                    int x1 = (int) (left + scale * value.getX() - radius / 2);
                    int y1 = (int) (top + scale * value.getY() - radius / 2);
                    g.drawLine(x, y, x1, y1);
                }
                for (Gateway gateway : InterfacePoints) {
                    int x1 = (int) (left + scale * gateway.getX() - radius / 2);
                    int y1 = (int) (top + scale * gateway.getY() - radius / 2);
                    g.drawLine(x, y, x1, y1);
                }
            }
        }
//        if (getArrow() != null) {
//            g.setColor(Color.blue);
//
//            getArrow().draw(g);
//        }
    }


    public void drawState(Graphics g, int tr, int left, int top, double scale) {
        g.setColor(Color.RED);
        int radius = diameter * 3;
        int x = (int) (left + scale * this.x - radius / 2);
        int y = (int) (top + scale * this.y - radius / 2);
        g.drawOval(x, y, radius, radius);

    }

    public Drone getIncoming() {
        return incoming;
    }

    public void setIncoming(Drone incoming) {
        this.incoming = incoming;
    }

    public List<Drone> getOutgoing() {
        return outgoing;
    }

    public void setOutgoing(List<Drone> outgoing) {
        this.outgoing = outgoing;
    }

    public void addToOutgoing(Drone drone) {
        drone.setIncoming(this);
        this.outgoing.add(drone);
    }

    public boolean hasSingleOutgoingLinks() {
        return outgoing.size() == 1;
    }

    public boolean hasMultipleOutgoingLinks() {
        return outgoing.size() > 1;
    }

    public boolean hasNoOutgoingLinks() {
        return outgoing.isEmpty();
    }

    @Override
    public String toString() {

        return "Drone{" +
                "incoming=" + (incoming == null ? "null" : incoming.getID()) +
                ", outgoing=" + outgoing.stream().map(NetworkNode::getID).collect(Collectors.toList()) +
                ", x=" + x +
                ", y=" + y +
                ", id=" + id +
                '}';
    }

    public Track goTo(double x, double y, int time, int i) {
        double distance = AnalyticGeometry.euclideanDistance(this.getX(), this.getY(), x, y);
        if (time < relocation.size()) {
            relocation.set(time, relocation.get(time) + distance);
        } else {
            relocation.add(distance);
        }
        Track track = new Track(this.getX(), this.getY(), x, y, distance, i);
        track.setDrone(this);
        System.out.println(track);
        trajectory.add(track);
        setLocation(x, y);
        return track;
    }

    public void clearOutGoingEdges() {
        outgoing.clear();
    }
}
