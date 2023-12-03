package test;

import java.util.ArrayList;

public class Node {

    double x, y;
    ArrayList<Cluster> clusters = new ArrayList<>();

    public Node() {
    }

    public Node(double x, double y, ArrayList<Cluster> clusters) {
        this.x = x;
        this.y = y;
        this.clusters = clusters;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public ArrayList<Cluster> getClusters() {
        return clusters;
    }

    public void setClusters(ArrayList<Cluster> clusters) {
        this.clusters = clusters;
    }
}
