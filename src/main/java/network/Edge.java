package network;

import utils.NetworkUtils;

import java.awt.*;

/**
 * @author : Fatih Senel
 *         Date: May 26, 2010
 *         Time: 12:18:31 AM
 */
public class Edge implements Comparable<Edge> {

    public Gateway u, v;
    public double distance;
    public int weight;
    public boolean isPruned;

    public Edge(Gateway u, Gateway v) {
        this.u = u;
        this.v = v;
        if (u != null && v != null) {
            distance = NetworkUtils.EstimatedDistance(u, v);
            weight = (int) (Math.ceil(distance / Constants.RNTransmissionRange) - 1);
        }
        isPruned = false;
    }

    public int compareTo(Edge e) {
        return Double.compare(this.distance, e.distance);
    }

    public String key() {
        if (u.isRelay() && v.isRelay()) {
            return "R" + u.getID() + "R" + v.getID();
        } else if (u.isRelay() && (!v.isRelay())) {
            return "T" + v.getID() + "R" + u.getID();
        } else if ((!u.isRelay()) && v.isRelay()) {
            return "T" + u.getID() + "R" + v.getID();
        } else {
            int uid = u.getID();
            int vid = v.getID();
            if (uid > vid) {
                return "T" + vid + "T" + uid;
            } else {
                return "T" + uid + "T" + vid;
            }
        }
    }

    public String reversekey() {
        if (u.isRelay() && v.isRelay()) {
            return "R" + v.getID() + "R" + u.getID();
        } else if (u.isRelay() && (!v.isRelay())) {
            return "T" + v.getID() + "R" + u.getID();
        } else if ((!u.isRelay()) && v.isRelay()) {
            return "T" + u.getID() + "R" + v.getID();
        } else {
            int uid = u.getID();
            int vid = v.getID();
            if (uid > vid) {
                return "T" + uid + "T" + vid;
            } else {
                return "T" + vid + "T" + uid;
            }
        }
    }


    public String toString() {
        String result = "[";
        if (u.isRelay) {
            result += "R" + u.getID();
        } else {
            result += "S" + u.getID();
        }
        result += ", ";
        if (v.isRelay) {
            result += "R" + v.getID();
        } else {
            result += "S" + v.getID();
        }
        result += "] = " + weight + "\t D = " + distance;
        return result;
    }

    public Gateway[] getNodes() {
        return new Gateway[]{u, v};
    }

    public boolean equals(Object obj) {
        Edge e = (Edge) obj;
        return ((u.getID() == e.u.getID() && v.getID() == e.v.getID()) || (u.getID() == e.v.getID() && v.getID() == e.u.getID()));
    }


    public void draw(Graphics g) {
        g.drawLine((int) this.u.getX(), (int) this.u.getY(), (int) this.v.getX(), (int) this.v.getY());
    }
}
