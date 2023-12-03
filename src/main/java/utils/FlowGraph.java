package utils;

public class FlowGraph {
    public static final int WHITE = 0, GRAY = 1, BLACK = 2;
    private double[][] capacity;
    private double[][] res_capacity;
    private int[] parent, color, queue;
    private double[] min_capacity;
    private int size;
    private int source;
    private int sink;
    private double max_flow;
    final int Inf = 100000;

    public FlowGraph(int size, int source, int sink, double[][] capacity) {
        this.size = size;
        this.source = source;
        this.sink = sink;
        this.capacity = capacity;
        maxFlow();
    }


    public FlowGraph(int size, int source, int sink) {
        this.size = size;
        this.source = source;
        this.sink = sink;

    }

    // Edmonds-Karp algorithm with O(V+E) complexity
    private void maxFlow() {
        double[][] flow = new double[size][size];
        res_capacity = new double[size][size];
        parent = new int[size];
        min_capacity = new double[size];
        color = new int[size];
        queue = new int[size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                res_capacity[i][j] = capacity[i][j];
            }
        }

        while (BFS(source)) {
            max_flow += min_capacity[sink];
            int v = sink, u;
            while (v != source) {
                u = parent[v];
                flow[u][v] += min_capacity[sink];
                flow[v][u] -= min_capacity[sink];
                res_capacity[u][v] -= min_capacity[sink];
                res_capacity[v][u] += min_capacity[sink];
                v = u;
            }
        }
    }

    private boolean BFS(int source)  // Breadth First Search in O(V)
    {
        for (int i = 0; i < size; i++) {
            color[i] = WHITE;
            min_capacity[i] = Double.MAX_VALUE;
        }

        int last;
        int first = last = 0;
        queue[last++] = source;
        color[source] = GRAY;

        while (first != last)  // While "queue" not empty..
        {
            int v = queue[first++];
            for (int u = 0; u < size; u++)
                if (color[u] == WHITE && res_capacity[v][u] > 0) {
                    min_capacity[u] = Math.min(min_capacity[v], res_capacity[v][u]);
                    parent[u] = v;
                    color[u] = GRAY;
                    if (u == sink) return true;
                    queue[last++] = u;
                }
        }
        return false;
    }

    public void toFile(String fileName) {
        // Write the results ("flow" matrix and "max_flow" value) to output file.
        // To be called in the "main()" method.
    }


    public double getMax_flow() {
        return max_flow;
    }

    // New Code



}
