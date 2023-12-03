package test;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Nodes {

    ArrayList<Node> nodes = new ArrayList<>();

    public Nodes() {
    }

    public Nodes(ArrayList<Node> nodes) {
        this.nodes = nodes;
    }

    public ArrayList<Node> getNodes() {
        return nodes;
    }

    public void setNodes(ArrayList<Node> nodes) {
        this.nodes = nodes;
    }
}
