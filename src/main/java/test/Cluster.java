package test;

public class Cluster {
    public String algo;
    public int num_clusters, index;

    public Cluster() {
    }

    public Cluster(String algo, int num_clusters, int index) {
        this.algo = algo;
        this.num_clusters = num_clusters;
        this.index = index;
    }

    public String getAlgo() {
        return algo;
    }

    public void setAlgo(String algo) {
        this.algo = algo;
    }

    public int getNum_clusters() {
        return num_clusters;
    }

    public void setNum_clusters(int num_clusters) {
        this.num_clusters = num_clusters;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
