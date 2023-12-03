package utils;

import geometry.AnalyticGeometry;
import network.Constants;
import network.Gateway;

import java.util.ArrayList;

/**
 * User: Fatih
 * Date: Dec 19, 2009
 * Time: 2:43:38 AM
 */
public class KConnectedGraph {

    int N, K;
    double TR;
    double[][] xycoordAllVertices;
    double[][] relay_coordinates;
    boolean log = false;

    public static void main(String[] args) {
//        new KConnectedGraph();
    }

    public KConnectedGraph(ArrayList<Gateway> terminals, int K) {
        this.K = K;
        this.TR = Constants.ActorTransmissionRange;
        N = terminals.size();
        double[][] location = new double[N][2];
        int[][] weight;

        try {
            for (int i = 0; i < N; i++) {
                location[i][0] = terminals.get(i).getX();
                location[i][1] = terminals.get(i).getY();
            }


            weight = AssignWeightInitial(location);
            if (log) {
                System.out.println("");
            }

            int maxWeight = FindMax2D(weight, N);
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (weight[i][j] >= 0) {
                        weight[i][j] = maxWeight - weight[i][j] + 1; /*As FindKConnectedSubGraph finds maximum weight subgraph*/
                    } else {
                        weight[i][j] = 0;
                    }
                }
            }
            int[][] I = new int[N][N];
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    I[i][j] = 0;
                }
            }
            int flag = FindKConnectedSubgraph(weight, I, K);
            /*System.out.println("------------------------");
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (I[i][j] != 0) {
                        I[j][i] = I[i][j];
                    }
                }
                System.out.println("");
            }
            printArray(I, N);
            System.out.println("");*/
            if (log) {
                System.out.println("flag after FindKConnectedSubgraph = " + flag);
            }

            for (int i = 0; i < N; i++) {
                for (int j = i + 1; j < N; j++) {
                    if ((I[i][j] == 1) || (I[j][i] == 1)) {
                        I[i][j] = 1;
                        I[j][i] = 1;
                    }
                }
            }
            if (log) {
                printArray(I, N);
            }

            flag = NetworkUtils.CheckForKConnectivity(I, N, K, N);
            if (log) {
                System.out.println("flag after CheckForKConnectivity = " + flag);
            }

            int numVertices;
            int numNewVertices;
            if (flag == 1) /*Subgraph exists*/ {
                int[][] weight1;
                int[][] neigh;
                int[] removed;
                int newVertIndex = 0;

                weight1 = new int[N][N];
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        weight1[i][j] = 0;
                    }
                }
                for (int i = 0; i < N; i++) {
                    for (int j = i + 1; j < N; j++) {
                        if ((weight[i][j] > 0) && ((I[i][j] == 1) || (I[j][i] == 1))) {
                            weight1[i][j] = maxWeight - weight[i][j] + 1;
                            weight1[j][i] = maxWeight - weight[i][j] + 1;
                        }
                    }
                }
                numVertices = N + NetworkUtils.FindSum2D(weight1, N) / 2;
                numNewVertices = NetworkUtils.FindSum2D(weight1, N) / 2;
                if (log) {
                    System.out.println("numNewVertices = " + numNewVertices);
                }

                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        if (weight[i][j] == 0) {
                            weight[i][j] = -1;
                        } else {
                            weight[i][j] = maxWeight - weight[i][j] + 1;
                        }
                        if (weight[i][j] == 0) {
                            I[i][j] = 1;
                        }
                    }
                }

                /*Sequential Removal of Beads*/
                /*Assume all nodes are in the first quadrant*/
                xycoordAllVertices = new double[numVertices][2];
                for (int i = 0; i < numVertices; i++) {
                    xycoordAllVertices[i][0] = -1;
                    xycoordAllVertices[i][1] = -1;
                }
                neigh = new int[numVertices][numVertices];
                for (int i = 0; i < numVertices; i++) {
                    for (int j = 0; j < numVertices; j++) {
                        neigh[i][j] = 0;
                    }
                }
                removed = new int[numNewVertices];

                for (int i = 0; i < N; i++) {
                    xycoordAllVertices[i][0] = location[i][0];
                    xycoordAllVertices[i][1] = location[i][1];
                    for (int j = i + 1; j < N; j++) {
                        if (weight[i][j] == 0) {
                            if (!((I[i][j] == 1) || (I[j][i] == 1))) {
                                if (log) {
                                    System.out.println("Serror = 1");
                                }

                            }
                            I[i][j] = 1;
                            I[j][i] = 1;
                        } else if ((I[i][j] == 1) || (I[j][i] == 1)) {
                            for (int k = 0; k < weight[i][j]; k++) {
                                xycoordAllVertices[N + newVertIndex][0] = location[i][0] + ((k + 1) / ((double) (weight[i][j] + 1))) * (location[j][0] - location[i][0]);
                                xycoordAllVertices[N + newVertIndex][1] = location[i][1] + ((k + 1) / ((double) (weight[i][j] + 1))) * (location[j][1] - location[i][1]);
                                newVertIndex = newVertIndex + 1;
                            }
                        }
                    }
                }

                for (int i = 0; i < numVertices; i++) {
                    for (int j = i + 1; j < numVertices; j++) {
                        double distance = AnalyticGeometry.euclideanDistance(xycoordAllVertices[i][0], xycoordAllVertices[i][1], xycoordAllVertices[j][0], xycoordAllVertices[j][1]);
                        if (distance <= TR) {
                            neigh[i][j] = 1;
                            neigh[j][i] = 1;
                        } else if ((i < N) && (j < N) && (weight[i][j] == 0)) {
                            distance = AnalyticGeometry.euclideanDistance(location[i][0], location[i][1], location[j][0], location[j][1]);
                            if (log) {
                                System.out.println("Serror = 1\ndistance = " + distance);
                            }
                        }
                    }
                }

                flag = NetworkUtils.CheckForKConnectivity(neigh, numVertices, K, N);
                if (log) {
                    System.out.println("flag after constructing neigh on vertices and beads = " + flag);
                }
                HeurRemoval(neigh, removed, numVertices, K);
                int numVerticesTemp = numVertices - NetworkUtils.sum(removed, numNewVertices);
                int numNewVerticesTemp = numVerticesTemp - N;
                if (log) {
                    System.out.println("numNewVertices after removal= " + numNewVerticesTemp);
                }

                relay_coordinates = new double[numNewVerticesTemp][2];
                int ptr = 0;
                for (int i = 0; i < xycoordAllVertices.length - N; i++) {
                    if (removed[i] == 0) {
                        relay_coordinates[ptr][0] = xycoordAllVertices[i + N][0];
                        relay_coordinates[ptr][1] = xycoordAllVertices[i + N][1];
                        ptr++;
                    }
                }

            } else {
                numNewVertices = Constants.Inf;
                if (log) {
                    System.out.println("numNewVertices after removal= " + numNewVertices);
                }
//		numNewVerticesTemp = Inf;
            }


        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }


    }

    private void printArray(int[][] arr, int size) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                System.out.print("" + arr[i][j] + " ");
            }
            System.out.println("");
        }
    }

    private int[][] AssignWeightInitial(double[][] location) {
        int[][] weight = new int[N][N];
        double length;
        for (int i = 0; i < N; i++) {
            weight[i][i] = -1;
        }

        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                length = Math.sqrt(Math.pow((location[i][0] - location[j][0]), 2) + Math.pow((location[i][1] - location[j][1]), 2));
                weight[i][j] = (int) Math.ceil(length / TR) - 1;
                weight[j][i] = weight[i][j];
            }
        }
        return weight;
    }


    /**
     * Maximum of an 2D array
     *
     * @param list  is the 2D square array
     * @param size1 is the length of the array
     * @return maximum of array entries
     */
    private int FindMax2D(int[][] list, int size1) {
        int val = list[0][0];
        for (int i = 0; i < size1; i++) {
            for (int j = 0; j < size1; j++) {
                if (list[i][j] > val) {
                    val = list[i][j];
                }
            }
        }
        return val;
    }


    private int FindKConnectedSubgraph(int[][] weight, int[][] I, int K) {
        ForestI[] forestI = new ForestI[K];
        int[][] s1 = new int[N][N];
        int[][] s2 = new int[N][N];

        int i, j, k, exitFlag = 0, iteration = 0, flag;

        for (i = 0; i < N; i++) {
            for (j = 0; j < N; j++) {
                s1[i][j] = 0;
            }
        }
        for (i = 0; i < N; i++) {
            for (j = 0; j < N; j++) {
                s2[i][j] = weight[i][j];
            }
        }

        for (i = 0; i < K; i++) {
            forestI[i] = new ForestI();
            forestI[i].tree = new int[N][N];
            for (j = 0; j < N; j++) {
                for (k = 0; k < N; k++) {
                    forestI[i].tree[j][k] = 0;
                }
            }
        }

        while ((exitFlag == 0) && (NetworkUtils.FindSum2D(I, N) < K * (N - 1))) {
            Circuit[][] circuit1 = new Circuit[N][N];
            Circuit[][] circuit2 = new Circuit[N][N];
            X[] x1 = new X[N * N];
            X[] x2 = new X[N * N];
            int[][] label = new int[N * N][N * N];
            int[][] G = new int[N * N][N * N];

            int m1 = 0, m2 = 0;
            int[][] belongsToI1 = new int[N][N];
            int[][] belongsToI2 = new int[N][N];
            int indX1 = 0, indX2 = 0, pathExists = 0;
            int[] lengthPath;
            int[] pathIndex;
            int[] paths;
            iteration++;

            for (i = 0; i < N; i++) {
                for (j = 0; j < N; j++) {
                    circuit1[i][j] = new Circuit();
                    circuit1[i][j].member = null;
                    circuit2[i][j] = new Circuit();
                    circuit2[i][j].member = null;
                }
            }

            /*Find x1, x2*/
            for (i = 0; i < N * N; i++) {
                x1[i] = new X();
                x1[i].tail = -1;
                x1[i].head = -1;
                x2[i] = new X();
                x2[i].tail = -1;
                x2[i].head = -1;
                for (j = 0; j < N * N; j++) {
                    label[i][j] = -1;
                    G[i][j] = 0;
                }
            }
            for (i = 0; i < N; i++) {
                for (j = 0; j < N; j++) {
                    belongsToI1[i][j] = 0;
                    belongsToI2[i][j] = 0;
                }
            }

            /** seems redundant
             for (i=0; i<N; i++)
             {
             for (j=0; j<N; j++)
             {
             circuit1[i][j].member = NULL;
             circuit2[i][j].member = NULL;
             }
             }
             */

            for (i = 0; i < N; i++) {
                for (j = 0; j < N; j++) {
                    if ((i != j) && (weight[i][j] > 0) && (I[i][j] == 0)) {
                        belongsToI1[i][j] = BelongsToI1(circuit1, forestI, i, j, K);
                        /*1 means yes, 0 no*/
                        belongsToI2[i][j] = BelongsToI2(circuit2, I, i, j, 0, K); /*Last argument is the root, which we fix at 0*/
                        /*1 means yes, 0 no*/

                        if ((belongsToI1[i][j] == 1) && (s1[i][j] > m1)) {
                            m1 = s1[i][j];
                            for (k = 0; k < N * N; k++) {
                                x1[k].tail = -1;
                                x1[k].head = -1;
                            }

                            indX1 = 0;
                            x1[indX1].tail = i;
                            x1[indX1].head = j;
                            indX1++;
                        } else if ((belongsToI1[i][j] == 1) && (s1[i][j] == m1)) {
                            x1[indX1].tail = i;
                            x1[indX1].head = j;
                            indX1++;
                        } else if (belongsToI1[i][j] == 0) {
                            /*circuit1(i,j,:,:) = circuit1temp(:,:);*//*Passing the pointer, so gets done*/
                            /*Will use this information to construct the auxiliary graph*/
                        }

                        if ((belongsToI2[i][j] == 1) && (s2[i][j] > m2)) {
                            m2 = s2[i][j];
                            for (k = 0; k < N * N; k++) {
                                x2[k].tail = -1;
                                x2[k].head = -1;
                            }

                            indX2 = 0;
                            x2[indX2].tail = i;
                            x2[indX2].head = j;
                            indX2++;
                        } else if ((belongsToI2[i][j] == 1) && (s2[i][j] == m2)) {
                            x2[indX2].tail = i;
                            x2[indX2].head = j;
                            indX2++;
                        } else if (belongsToI2[i][j] == 0) {
                            /*circuit2(i,j,:,:) = circuit2temp(:,:);*//*Passing the pointer, so gets done*/
                            /*Will use this information to construct the auxiliary graph*/
                        }
                    }
                }
            }
            if (m1 > 0) {
                // ERROR
                if (log) {
                    System.out.println("FindKCSGerror = 12\tm1 = " + m1);
                }
            }

            lengthPath = new int[indX1];
            pathIndex = new int[indX1];
            paths = new int[(indX1 + indX2 - 1)];

            /*Construct Auxiliary Graph G*/
            for (i = 0; i < N; i++) {
                for (j = 0; j < N; j++) {
                    if ((j != i) && (weight[i][j] > 0) && (I[i][j] == 0)) {
                        if (belongsToI1[i][j] == 0) /*I+x does not belong to I1*/ {
                            CircuitMember circuitMemberPointer;

                            circuitMemberPointer = circuit1[i][j].member;
                            while (circuitMemberPointer != null) {
                                int yi = circuitMemberPointer.tail;
                                int yj = circuitMemberPointer.head;
                                if (s1[i][j] == s1[yi][yj]) {
                                    G[N * i + j][N * yi + yj] = 1; /*xy is an edge now*/
                                }
                                circuitMemberPointer = circuitMemberPointer.nextMember;
                            }
                        }
                        if (belongsToI2[i][j] == 0) /*I+x does not belong to I2*/ {
                            CircuitMember circuitMemberPointer;
                            circuitMemberPointer = circuit2[i][j].member;
                            while (circuitMemberPointer != null) {
                                int yi = circuitMemberPointer.tail;
                                int yj = circuitMemberPointer.head;
                                if (s2[i][j] == s2[yi][yj]) {
                                    G[N * yi + yj][N * i + j] = 1; /*yx is an edge now*/
                                }
                                circuitMemberPointer = circuitMemberPointer.nextMember;
                            }
                        }
                    }
                }
            }

            /*Find path U in G from X2 to X1
            Compute labels from all vertices in X2*/
            for (i = 0; i < indX2; i++) {
                label[N * x2[i].tail + x2[i].head][i] = N * x2[i].tail + x2[i].head;
                label = FindPaths(G, label, N * x2[i].tail + x2[i].head, i);
            }
            /*Check which vertices in X1 have a label*/
            for (i = 0; i < indX1; i++) {
                lengthPath[i] = (N * N) + 1; /*Indexed like in X1, not as in G*/
                pathIndex[i] = -1;
                /*Contains the X2 vertex from which to start
                It will contain vertices in reverse order, but that does not matter as we are only
                concerned with what vertices belong to the paths*/
            }
            for (i = 0; i < indX1; i++) {
                j = 0;
                while ((j < indX2) && (pathExists == 0)) {
                    if (label[N * x1[i].tail + x1[i].head][j] >= 0) {
                        pathExists = 1;
                    }
                    j++;
                }
                if (pathExists == 1) {
                    for (j = 0; j < indX2; j++) {
                        int m = N * x1[i].tail + x1[i].head;
                        if (label[m][j] >= 0) {
                            int ind = 0;
                            while (label[m][j] != m) {
                                //paths[i][j][ind] = m;
                                m = label[m][j];
                                if (m == -1) {
                                    // ERROR
                                    if (log) {
                                        System.out.println("FindKCSGerror = 6, N*x2[j].tail+x2[j].head = " + N * x2[j].tail + x2[j].head + ", N*x1[i].tail+x1[i].head = " + N * x1[i].tail + x1[i].head + ", m = " + m + ", j = " + j);
                                    }
                                }
                                ind++;
                            }
                            if (m != N * x2[j].tail + x2[j].head) {
                                // ERROR
                                if (log) {
                                    System.out.println("FindKCSGerror = 7");
                                }
                            }
                            if (lengthPath[i] > ind + 1) {
                                lengthPath[i] = ind + 1;
                                pathIndex[i] = j;
                            }
                        }
                    }
                }

            }
            if (pathExists == 1) {
                int chosenPathIndex;
                int ind1 = 0;
                int[][] augPath;

                chosenPathIndex = NetworkUtils.findMin(lengthPath, indX1);
                j = pathIndex[chosenPathIndex];

                if (label[N * x1[chosenPathIndex].tail + x1[chosenPathIndex].head][j] == -1) {
                    //ERROR
                    if (log) {
                        System.out.println("FindKCSGerror = 10");
                    }
                } else {
                    int m = N * x1[chosenPathIndex].tail + x1[chosenPathIndex].head;
                    int ind = 0;
                    while (label[m][j] != m) {
                        paths[ind] = m;
                        m = label[m][j];
                        if (m == 0) {
                            if (log) {
                                System.out.println("FindKCSGerror = 6");
                            }
                        }

                        ind++;
                    }
                    if (m != N * x2[j].tail + x2[j].head) {
                        if (log) {
                            System.out.println("FindKCSGerror = 7");
                        }
                    }
                    paths[ind] = m;
                    if (lengthPath[chosenPathIndex] != ind + 1) {
                        if (log) {
                            System.out.println("FindKCSGerror = 11");
                        }
                    }
                }
                augPath = new int[(indX1 + indX2 - 1)][2];
                for (i = 0; i < indX1 + indX2 - 1; i++) {
                    augPath[i][0] = -1;
                    augPath[i][1] = -1;
                }
                for (i = lengthPath[chosenPathIndex] - 1; i >= 0; i--) {
                    int pathAuxNode = paths[i];
                    augPath[ind1][0] = pathAuxNode / N;
                    augPath[ind1][1] = pathAuxNode % N;
                }

                for (i = 0; i < K; i++) {
                    for (j = 0; j < N; j++) {
                        for (k = 0; k < N; k++) {
                            forestI[i].tree[j][k] = 0;
                        }
                    }
                }
                AddToI(I, forestI, augPath, K);
                /*label = zeros(N^2,1); %Reset labels*/

            } else {
                int delta1, delta2, delta3, delta4, delta;
                /*Calculate delta1, delta2, delta3, delta4 - delta is min of these*/
                delta1 = Constants.Inf;
                delta2 = Constants.Inf;
                delta3 = Constants.Inf;
                delta4 = Constants.Inf;
                for (i = 0; i < N; i++) {
                    for (j = 0; j < N; j++) {
                        if ((j != i) && (weight[i][j] > 0)) {
                            int flag1 = 0;
                            k = 0;
                            while ((k < indX2) && (flag1 == 0)) {
                                if (label[i * N + j][k] >= 0) {
                                    flag1 = 1;
                                }
                                k++;
                            }
                            /*Update delta1, delta2*/
                            if ((flag1 == 1) && (I[i][j] == 0)) {
                                if (belongsToI1[i][j] == 0) {
                                    CircuitMember circuitMemberPointer;
                                    circuitMemberPointer = circuit1[i][j].member;
                                    while (circuitMemberPointer != null) {
                                        int flag2 = 0, i1, j1;
                                        i1 = circuitMemberPointer.tail;
                                        j1 = circuitMemberPointer.head;
                                        k = 0;
                                        while ((k < indX2) && (flag2 == 0)) {
                                            if (label[i1 * N + j1][k] >= 0) {
                                                flag2 = 1;
                                            }
                                            k++;
                                        }
                                        if (flag2 == 0) {
                                            if (s1[i1][j1] - s1[i][j] < delta1) {
                                                delta1 = s1[i1][j1] - s1[i][j];
                                            }
                                            if (s1[i1][j1] - s1[i][j] <= 0) {
                                                if (log) {
                                                    System.out.println("FindKCSGerror = 1");
                                                    System.out.println("" + i1 + " " + j1 + " " + I[i1][j1]);
                                                    System.out.println("" + i + " " + j + " " + s1[i][j] + " " + s1[i1][j1]);
                                                }
                                            }
                                        }
                                        circuitMemberPointer = circuitMemberPointer.nextMember;
                                    }
                                } else if (m1 - s1[i][j] < delta2) {
                                    delta2 = m1 - s1[i][j];
                                    if (m1 - s1[i][j] <= 0) {
                                        if (log) {
                                            System.out.println("FindKCSGerror = 2");
                                        }
                                    }
                                }
                            }
                            /*Update delta3, delta4*/
                            else if ((flag1 == 0) && (I[i][j] == 0)) {
                                if (belongsToI2[i][j] == 0) {
                                    CircuitMember circuitMemberPointer;
                                    circuitMemberPointer = circuit2[i][j].member;
                                    while (circuitMemberPointer != null) {
                                        int flag2 = 0, i1, j1;
                                        i1 = circuitMemberPointer.tail;
                                        j1 = circuitMemberPointer.head;
                                        k = 0;
                                        while ((k < indX2) && (flag2 == 0)) {
                                            if (label[i1 * N + j1][k] >= 0) {
                                                flag2 = 1;
                                            }
                                            k++;
                                        }
                                        if (flag2 == 1) {
                                            if (s2[i1][j1] - s2[i][j] < delta3) {
                                                delta3 = s2[i1][j1] - s2[i][j];
                                            }
                                            if (s2[i1][j1] - s2[i][j] <= 0) {
                                                if (log) {
                                                    System.out.println("FindKCSGerror = 3");
                                                }
                                            }
                                        }
                                        circuitMemberPointer = circuitMemberPointer.nextMember;
                                    }
                                } else if (m2 - s2[i][j] < delta4) {
                                    delta4 = m2 - s2[i][j];
                                    if (m2 - s2[i][j] <= 0) {
                                        if (log) {
                                            System.out.println("FindKCSGerror = 4");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (delta2 < Constants.Inf) {
                    if (log) {
                        System.out.println("FindKCSGerror = 13");
                        System.out.println("delta2 = " + delta2);
                    }
                }
                delta = Math.min(delta1, delta2);
                delta = Math.min(delta, delta3);
                delta = Math.min(delta, delta4);
                if (log) {
                    System.out.println("iteration " + iteration + " delta1 " + delta1 + " delta2 " + delta2 + " delta3 " + delta3 + " delta4 " + delta4 + " delta " + delta);
                }
                if (delta == Constants.Inf) {
                    exitFlag = 1;
                } else {
                    for (i = 0; i < N; i++) {
                        for (j = 0; j < N; j++) {
                            int flag2 = 0;
                            k = 0;
                            while ((k < indX2) && (flag2 == 0)) {
                                if (label[i * N + j][k] >= 0) {
                                    flag2 = 1;
                                }
                                k++;
                            }
                            if ((j != i) && (weight[i][j] > 0) && (flag2 == 1)) {
                                s1[i][j] = s1[i][j] + delta;
                                s2[i][j] = s2[i][j] - delta;
                            }
                        }
                    }
                }
            }

/*
            for (i = 0; i < N; i++) {
                for (j = 0; j < N; j++) {
                    CircuitMember circuitMember;
                    CircuitMember circuitMember1;
                    CircuitMember * circuitMember2;
                    CircuitMember * circuitMember12;
                    circuitMember = circuit1[i][j].member;
                    while (circuitMember != null) {
                        circuitMember1 = circuitMember;
                        circuitMember = circuitMember.nextMember;
                        free(circuitMember1);

                    }
                    circuitMember2 = (CircuitMember *)                    circuit2[i][j].member;
                    while (circuitMember != NULL) {
                        circuitMember12 = circuitMember2;
                        circuitMember2 = (CircuitMember *)
                        circuitMember2 - > nextMember;
                        free(circuitMember12);
                    }
                }
//                free(circuit1[i]);
//                free(circuit2[i]);
            }*/

        }

        if (exitFlag == 0) {
            int exitFlag1 = 0;
            flag = 1;

            for (i = 0; i < K; i++) {
                for (j = 0; j < N; j++) {
                    for (k = 0; k < N; k++) {
                        forestI[i].tree[j][k] = 0;
                    }
                }
            }
            i = 0;
            j = 0;
            while ((exitFlag1 == 0) && (i < N)) {
                while ((exitFlag1 == 0) && (j < N)) {
                    if (I[i][j] == 1) {
                        int belongsToI1 = AddToForests(forestI, i, j, K);
                        if (belongsToI1 == 0) {
                            if (log) {
                                System.out.println("FindKCSGerror = 5");
                            }
                            exitFlag1 = 1;
                        }
                    }
                    j++;
                }
                i++;
            }
        } else {
            flag = 0;
        }
        return flag;
    }


    /*
Used by FindKConnectedSubgraph to find path from X2 to X1
graph: (N, N) m,n = 1 => m,n exists in the graph.
Note: size is N^2 when we use this for the auxiliary graph
label: The calling program can find the paths using the labels
Finds paths from i to all vertices using BFS*/
    private int[][] FindPaths(int[][] graph, int[][] label, int root, int indX2) {
//	FILE *fp;
        int[] queue = new int[N * N];
        int i, j, queueIndex = 0, queueEndIndex = 0;
        if (graph[root][root] == 1) {
            if (log) {
                System.out.println("FindPathserror = 1");
            }
        }
        for (i = 0; i < N * N; i++) {
            queue[i] = -1;
        }
        queue[0] = root;
        while ((queueIndex < N * N) && (queue[queueIndex] >= 0)) {
            int temp, pickedNode;
            pickedNode = queue[queueIndex];
            temp = queueEndIndex + 1;
            for (j = 0; j < N * N; j++) {
                if ((graph[pickedNode][j] == 1) && (label[j][indX2] == -1)) {
                    if (temp >= N * N) {
                        if (log) {
                            System.out.println("FindPathserror = 2");
                        }
                    }
                    label[j][indX2] = pickedNode;
                    queue[temp] = j;
                    temp++;
                }
            }
            queueIndex++;
            queueEndIndex = temp - 1;
        }
        return label;
//	free(queue);
    }

    /*Called by FindKConnectedSubGraph to add the augmenting path to I and
decompose I into K edge-disjoint forests using Roskind-Tarjan Algorithm
 I should be decomposable, else there is an error!*/
    private int[][] AddToI(int[][] I, ForestI[] forestI, int[][] augPath, int K) {
        int i, j, ind = 0, exitFlag = 0;

        /*Add and remove from I*/
        while ((ind < N * N) && (augPath[ind][0] >= 0)) {
            if (I[augPath[ind][0]][augPath[ind][1]] == 1) /*ind should be odd*/ {
                if (ind % 2 == 0) {
                    if (log) {
                        System.out.println("AddToIerror = 1");
                    }
                }
                I[augPath[ind][0]][augPath[ind][1]] = 0;
            } else                                     /*ind should be even*/ {
                if (ind % 2 != 0) {
                    if (log) {
                        System.out.println("AddToIerror = 2");
                    }
                }
                I[augPath[ind][0]][augPath[ind][1]] = 1;
            }
            ind++;
        }

        /*Decompose I into K forests*/

        i = 0;
        while ((exitFlag == 0) && (i < N)) {
            j = 0;
            while ((exitFlag == 0) && (j < N)) {
                if (I[i][j] == 1) {
                    int belongsToI1 = AddToForests(forestI, i, j, K);

                    if (belongsToI1 == 0) {
                        if (log) {
                            System.out.println("AddToIerror = 3");
                        }
                        exitFlag = 1;

                    }
                }
                j++;
            }
            i++;
        }
        return I;
    }


    /**
     * Used by AddToI Implement Roskind and Tarjan's Algorithm to decompose a graph into forests
     * Most code common with BelongsToI1
     *
     * @param forestI forestI (modified)
     * @param tail    tail
     * @param head    head
     * @param K       connectivity
     * @return belongsToI1
     */
    private int AddToForests(ForestI[] forestI, int tail, int head, int K) {
        ForestIU[] forestITemp;
        ForestIU forestTemp = new ForestIU();
        ForestIU forest = new ForestIU();
        Circuit[][] circuit1Temp;

//	FILE *fp;
        int i, j, k, exitFlag = 0;
        int[][] queue;
        int queueIndex = 0, belongsToI1 = 0, temp;
        BelongsToI1Label[][] label = new BelongsToI1Label[N][N];
        /*convert edges of forestI to undirected*/
        /*(i,j) and (j,i) will lead to two different undirected edges between the same pair!*/
        forestITemp = new ForestIU[K];
        for (i = 0; i < K; i++) {
            if (forestITemp[i] == null) {
                forestITemp[i] = new ForestIU();
            }
            forestITemp[i].tree = new int[N][N][2];
            for (j = 0; j < N; j++) {
                for (k = 0; k < N; k++) {
                    forestITemp[i].tree[j][k][0] = 0;
                    forestITemp[i].tree[j][k][1] = 0;
                }
            }
        }
        for (i = 0; i < N; i++) {
            for (j = 0; j < N; j++) {
                if (label[i][j] == null) {
                    label[i][j] = new BelongsToI1Label();
                }
                label[i][j].value[0][0] = -1;
                label[i][j].value[0][1] = -1;
                label[i][j].value[0][2] = 0;
                label[i][j].value[1][0] = -1;
                label[i][j].value[1][1] = -1;
                label[i][j].value[1][2] = 0;
            }
        }

        for (i = 0; i < K; i++) {
            for (j = 0; j < N; j++) {
                for (k = 0; k < N; k++) {
                    if (forestI[i].tree[j][k] == 1) {
                        if (j < k) {
                            forestITemp[i].tree[j][k][0] = 1;
                            forestITemp[i].tree[k][j][0] = 1;
                        } else if (j > k) {
                            forestITemp[i].tree[j][k][1] = 1;
                            forestITemp[i].tree[k][j][1] = 1;
                        }
                    }
                }
            }
        }
        queue = new int[2 * N * N][3];
        for (i = 0; i < 2 * N * N; i++) {
            queue[i][0] = -1;
            queue[i][1] = -1;
            queue[i][2] = 0;
        }
        /*Third element indicates the copy being used (1/2 (ij/ji) for i<j)*/
        queue[0][0] = tail;
        queue[0][1] = head;
        if (tail < head) {
            queue[0][2] = 1;
        } else {
            queue[0][2] = 2;
        }
        forest.tree = new int[N][N][2];
        for (j = 0; j < N; j++) {
            for (k = 0; k < N; k++) {
                forest.tree[j][k][0] = -1;
                forest.tree[j][k][1] = -1;
                /*Contains forest number. enter info in #1 for edge
                  corresponding to i->j, #2 for j->i, i<j*/
            }
        }
        if (tail < head) {
            temp = 0;
        } else {
            temp = 1;
        }
        label[tail][head].value[temp][0] = tail;
        label[tail][head].value[temp][1] = head;
        label[tail][head].value[temp][2] = temp + 1;
        forestTemp.tree = new int[N][N][2];

        while ((exitFlag == 0) && (queueIndex < 2 * N * N) && (queue[queueIndex][0] >= 0)) {
            int[] edge = new int[3];
            int forestIndex, forestIndexPlus;
            int[][] label1 = new int[N][2];
            for (i = 0; i < N; i++) {
                label1[i][0] = -1;
                label1[i][1] = -1;
            }
            edge[0] = queue[queueIndex][0];
            edge[1] = queue[queueIndex][1];
            edge[2] = queue[queueIndex][2];
            queueIndex++;
            forestIndex = forest.tree[edge[0]][edge[1]][edge[2] - 1];
            forestIndexPlus = (forestIndex % K) + 1;
            if (forestIndexPlus >= K) {
                if (forestIndexPlus > K) {
                    if (log) {
                        System.out.println("AddToForestserror = 0");
                    }
                }
                forestIndexPlus = 0;
            }
            /*Check if edge(1),edge(2) are in the same tree.
             #1: labeling vertex index, #2: what copy of the edge is being used (1
             or 2)*/

            label1[edge[0]][0] = edge[0];
            label1[edge[0]][1] = -1;

            for (j = 0; j < N; j++) {
                for (k = 0; k < N; k++) {
                    forestTemp.tree[j][k][0] = forestITemp[forestIndexPlus].tree[j][k][0];
                    forestTemp.tree[j][k][1] = forestITemp[forestIndexPlus].tree[j][k][1];
                }
            }
            FindPath(forestTemp, label1, edge[0], edge[1]);

            if (label1[edge[1]][0] == -1) /*There is an augmenting sequence, so return 1*/ {
                int m, n, linkType;
                belongsToI1 = 1;
                exitFlag = 1;

                /*Augment the forests*/
                m = edge[0];
                n = edge[1];
                linkType = edge[2];
                while (!((label[m][n].value[linkType - 1][0] == m) && (label[m][n].value[linkType - 1][1] == n) && (label[m][n].value[linkType - 1][2] == linkType))) {
                    int forestIndex1, forestIndexPlus1, m1, n1;
                    forestIndex1 = forest.tree[m][n][linkType - 1];
                    forestIndexPlus1 = (forestIndex1 % K) + 1;
                    if (forestIndexPlus1 >= K) {
                        if (forestIndexPlus1 > K) {
                            if (log) {
                                System.out.println("AddToForestserror = 10");
                            }
                        }
                        forestIndexPlus1 = 0;
                    }
                    if (((m < n) && (linkType == 1)) || ((m > n) && (linkType == 2))) {
                        if (forestI[forestIndexPlus1].tree[m][n] == 1) {
                            if (log) {
                                System.out.println("AddToForestserror = 1");
                            }
                        }
                        if (forestI[forestIndex1].tree[m][n] == 0) {
                            if (log) {
                                System.out.println("AddToForestserror = 2");
                            }
                        }
                        forestI[forestIndexPlus1].tree[m][n] = 1;
                        forestI[forestIndex1].tree[m][n] = 0;
                    } else {
                        if (forestI[forestIndexPlus1].tree[n][m] == 1) {
                            if (log) {
                                System.out.println("AddToForestserror = 3");
                            }
                        }
                        if (forestI[forestIndex1].tree[n][m] == 0) {
                            if (log) {
                                System.out.println("AddToForestserror = 4");
                            }
                        }
                        forestI[forestIndexPlus1].tree[n][m] = 1;
                        forestI[forestIndex1].tree[n][m] = 0;
                    }

                    m1 = label[m][n].value[linkType - 1][0];
                    n1 = label[m][n].value[linkType - 1][1];
                    linkType = label[m][n].value[linkType - 1][2];
                    m = m1;
                    n = n1;
                }
                if (!((m == tail) && (n == head))) {
                    if (log) {
                        System.out.println("AddToForestserror = 5");
                    }
                } else {
                    if (((m < n) && (linkType == 1)) || ((m > n) && (linkType == 2))) {
                        if (forestI[0].tree[m][n] == 1) {
                            if (log) {
                                System.out.println("AddToForestserror = 6");
                            }
                        }
                        forestI[0].tree[m][n] = 1;
                    } else {
                        if (forestI[0].tree[n][m] == 1) {
                            if (log) {
                                System.out.println("AddToForestserror = 7");
                            }
                        }
                        forestI[0].tree[n][m] = 1;
                    }
                }
            } else {
                temp = queueIndex;
                CircuitMember circuitMemberPointer;
                circuit1Temp = new Circuit[N][N];
                for (i = 0; i < N; i++) {
                    for (j = 0; j < N; j++) {
                        if (circuit1Temp[i][j] == null) {
                            circuit1Temp[i][j] = new Circuit();
                        }
                        circuit1Temp[i][j].member = null;
                    }
                }

                while (queue[temp][0] >= 0) {
                    temp++;
                }
                /*Label the edges in circuit*/
                /*forestTemp(1:N,1:N,1:2) = forestITemp(forestIndexPlus,1:N,1:N,1:2);Done Before!*/
                FindCircuit1(forestTemp, circuit1Temp, edge[0], edge[1]);
                circuitMemberPointer = circuit1Temp[edge[0]][edge[1]].member;
                while (circuitMemberPointer != null) {
                    int i1, j1, linkType;
                    i1 = circuitMemberPointer.tail;
                    j1 = circuitMemberPointer.head;
                    linkType = circuitMemberPointer.type; /*1 or 2*/
                    if (label[i1][j1].value[linkType - 1][0] == -1) {
                        queue[temp][0] = i1;
                        queue[temp][1] = j1;
                        queue[temp][2] = linkType;
                        temp++;
                        label[i1][j1].value[linkType - 1][0] = edge[0];
                        label[i1][j1].value[linkType - 1][1] = edge[1];
                        label[i1][j1].value[linkType - 1][2] = edge[2];
                        label[j1][i1].value[linkType - 1][0] = edge[0];
                        label[j1][i1].value[linkType - 1][1] = edge[1];
                        label[j1][i1].value[linkType - 1][2] = edge[2];
                        forest.tree[i1][j1][linkType - 1] = forestIndexPlus;
                        forest.tree[j1][i1][linkType - 1] = forestIndexPlus;
                    }
                    circuitMemberPointer = circuitMemberPointer.nextMember;
                }
//			for (i=0; i<N; i++)
//			{
//				for (j=0; j<N; j++)
//				{
//					CircuitMember *circuitMember;
//					CircuitMember *circuitMember1;
//					circuitMember = (CircuitMember *)circuit1Temp[i][j].member;
//					while (circuitMember!=NULL)
//					{
//						circuitMember1 = circuitMember;
//						circuitMember = (CircuitMember *)circuitMember->nextMember;
//						free(circuitMember1);
//					}
//				}
//				free(circuit1Temp[i]);
//			}
            }


        }
        return belongsToI1;
    }

    /*Used by FindKConnectedSubgraph
Check whether I+(i,j) belongs to I1 (i.e., is independent)
Implement Roskind and Tarjan's labeling Algorithm*/
    private int BelongsToI1(Circuit[][] circuit1, ForestI[] forestI, int tail, int head, int K) {
        ForestIU[] forestITemp;
        ForestIU forest = new ForestIU();
        ForestIU forestTemp = new ForestIU();

        Circuit[][] circuit1Temp;

        CircuitMember circuitMember;
        int i, j, k, exitFlag = 0, queueIndex = 0, belongsToI1 = 0;
        int[][] queue;
        BelongsToI1Label[][] label;
        /*convert edges of forestI to undirected*/
        /*(i,j) and (j,i) will lead to two different undirected edges between the same pair!*/
        forestITemp = new ForestIU[K];
        for (i = 0; i < K; i++) {
            forestITemp[i] = new ForestIU();
            forestITemp[i].tree = new int[N][N][2];
            for (j = 0; j < N; j++) {
                for (k = 0; k < N; k++) {
                    forestITemp[i].tree[j][k][0] = 0;
                    forestITemp[i].tree[j][k][1] = 0;
                }
            }
        }
        forestTemp.tree = new int[N][N][2];

        label = new BelongsToI1Label[N][N];
        for (i = 0; i < N; i++) {
            for (j = 0; j < N; j++) {
                if (label[i][j] == null) {
                    label[i][j] = new BelongsToI1Label();
                }
                label[i][j].value[0][0] = -1;
                label[i][j].value[0][1] = -1;
                label[i][j].value[0][2] = 0;
                label[i][j].value[1][0] = -1;
                label[i][j].value[1][1] = -1;
                label[i][j].value[1][2] = 0;
            }
        }

        for (i = 0; i < K; i++) {
            for (j = 0; j < N; j++) {
                for (k = 0; k < N; k++) {
                    if (forestI[i].tree[j][k] == 1) {
                        if (j < k) {
                            forestITemp[i].tree[j][k][0] = 1;
                            forestITemp[i].tree[k][j][0] = 1;
                        } else if (j > k) {
                            forestITemp[i].tree[j][k][1] = 1;
                            forestITemp[i].tree[k][j][1] = 1;
                            //printf("here=1\n");
                        }
                    }
                }
            }
        }

        queue = new int[N * N][3];
        for (i = 0; i < N * N; i++) {
            queue[i][0] = -1;
            queue[i][1] = -1;
            queue[i][2] = 0;
        }
        /*Third element indicates the copy being used (1/2 (ij/ji) for i<j)*/
        queue[0][0] = tail;
        queue[0][1] = head;
        if (tail < head) {
            queue[0][2] = 1;
        } else {
            queue[0][2] = 2;
        }
        //printf("tail=%d\n", tail);
        //printf("head=%d\n", head);
        forest.tree = new int[N][N][2];
        for (j = 0; j < N; j++) {
            for (k = 0; k < N; k++) {
                forest.tree[j][k][0] = -1;
                forest.tree[j][k][1] = -1;
                /*Contains forest number. enter info in #1 for edge
                  corresponding to i->j, #2 for j->i, i<j*/
            }
        }


        while ((exitFlag == 0) && (queueIndex < N * N) && (queue[queueIndex][0] >= 0)) {
            int[] edge = new int[3];
            int forestIndex, forestIndexPlus;
            int[][] label1 = new int[N][2];

            for (i = 0; i < N; i++) {
                label1[i][0] = -1;
                label1[i][1] = -1;
            }
            edge[0] = queue[queueIndex][0];
            edge[1] = queue[queueIndex][1];
            edge[2] = queue[queueIndex][2];

            queueIndex++;
            forestIndex = forest.tree[edge[0]][edge[1]][edge[2] - 1];
            forestIndexPlus = (forestIndex % K) + 1;
            if (forestIndexPlus >= K) {
                if (forestIndexPlus > K) {
                    if (log) {
                        System.out.println("BelongsToI1error = 0");
                    }
                }
                forestIndexPlus = 0;
            }
            //printf("forestIndexPlus=%d\n", forestIndexPlus);
            /*Check if edge(1),edge(2) are in the same tree.
             #1: labeling vertex index, #2: what copy of the edge is being used (1
             or 2)*/

            label1[edge[0]][0] = edge[0];
            label1[edge[0]][1] = -1;

            for (j = 0; j < N; j++) {
                for (k = 0; k < N; k++) {
                    forestTemp.tree[j][k][0] = forestITemp[forestIndexPlus].tree[j][k][0];
                    forestTemp.tree[j][k][1] = forestITemp[forestIndexPlus].tree[j][k][1];
                }
            }
            FindPath(forestTemp, label1, edge[0], edge[1]);

            if (label1[edge[1]][0] == -1) /*There is an augmenting sequence, so return 1*/ {
                belongsToI1 = 1;
                exitFlag = 1;
                //printf("Aug Seq\n");
            } else {
                int temp = queueIndex;
                CircuitMember circuitMemberPointer;
                circuit1Temp = new Circuit[N][N];
                for (i = 0; i < N; i++) {
                    for (j = 0; j < N; j++) {
                        if (circuit1Temp[i][j] == null) {
                            circuit1Temp[i][j] = new Circuit();
                        }
                        circuit1Temp[i][j].member = null;
                    }
                }
                while ((temp < N * N) && (queue[temp][0] >= 0)) {
                    temp++;
                }
                /*Label the edges in circuit*/
                /*forestTemp(1:N,1:N,1:2) = forestITemp(forestIndexPlus,1:N,1:N,1:2);Done Before!*/
                FindCircuit1(forestTemp, circuit1Temp, edge[0], edge[1]);

                circuitMemberPointer = circuit1Temp[edge[0]][edge[1]].member;
                while (circuitMemberPointer != null) {
                    int i1, j1, linkType;
                    i1 = circuitMemberPointer.tail;
                    j1 = circuitMemberPointer.head;
                    linkType = circuitMemberPointer.type; /*1 or 2*/
                    if (label[i1][j1].value[linkType - 1][0] == -1) {
                        queue[temp][0] = i1;
                        queue[temp][1] = j1;
                        queue[temp][2] = linkType;
                        temp++;
                        label[i1][j1].value[linkType - 1][0] = edge[0];
                        label[i1][j1].value[linkType - 1][1] = edge[1];
                        label[i1][j1].value[linkType - 1][2] = edge[2];
                        label[j1][i1].value[linkType - 1][0] = edge[0];
                        label[j1][i1].value[linkType - 1][1] = edge[1];
                        label[j1][i1].value[linkType - 1][2] = edge[2];
                        forest.tree[i1][j1][linkType - 1] = forestIndexPlus;
                        forest.tree[j1][i1][linkType - 1] = forestIndexPlus;
                    }
                    circuitMemberPointer = circuitMemberPointer.nextMember;
                }
                for (i = 0; i < N; i++) {
                    for (j = 0; j < N; j++) {
                        circuitMember = circuit1Temp[i][j].member;
                        while (circuitMember != null) {
                            circuitMember = circuitMember.nextMember;
                        }
                    }
                }
            }
        }


        if (exitFlag == 0) {
            int ind = 1; //First queue element is the edge itself, so start with second
            CircuitMember circuitMemberPtr;

            belongsToI1 = 0;
            circuitMemberPtr = circuit1[tail][head].member;
            if (queue[ind][0] < 0) {
                if (log) {
                    System.out.println("BelongsToI1error = 1");
                }
            }
            while ((ind < N * N) && (queue[ind][0] >= 0)) {
                CircuitMember newMember;
                newMember = new CircuitMember();
                newMember.type = 1;
                newMember.nextMember = null;
                if (circuit1[tail][head].member == null) {
                    circuit1[tail][head].member = newMember;
                } else {
                    circuitMemberPtr.nextMember = newMember;
                }
                circuitMemberPtr = newMember;

                if (queue[ind][2] == 1) {
                    if (queue[ind][0] < queue[ind][1]) {
                        newMember.tail = queue[ind][0];
                        newMember.head = queue[ind][1];
                    } else {
                        newMember.tail = queue[ind][1];
                        newMember.head = queue[ind][0];
                    }
                } else if (queue[ind][2] == 2) {
                    if (queue[ind][0] > queue[ind][1]) {
                        newMember.tail = queue[ind][0];
                        newMember.head = queue[ind][1];
                    } else {
                        newMember.tail = queue[ind][1];
                        newMember.head = queue[ind][0];
                    }
                } else {
                    if (log) {
                        System.out.println("BelongsToI1error = 2");
                    }
                }
                ind++;
            }
        }

        return belongsToI1;
    }


    private void FindCircuit1(ForestIU forest, Circuit[][] circuit1, int tail, int head) {
        int[][] label = new int[N][2];
        int i, j;
        for (i = 0; i < N; i++) {
            for (j = 0; j < 2; j++) {
                label[i][j] = -1;
            }
        }
        /*2nd element indicates the edge used to get to this vertex (undir corresponding to ij or ji)*/
        label[head][0] = head;
        label[head][1] = -1;
        FindPath(forest, label, head, tail);
        if (label[tail][0] >= 0) {
            int m = tail;
            CircuitMember circuitMember;
            circuitMember = circuit1[tail][head].member;
            while (label[m][0] != m) {
                int edgeTail, edgeHead = m;
                m = label[m][0];
                edgeTail = m;
                if ((forest.tree[edgeTail][edgeHead][0] == 1) && (forest.tree[edgeTail][edgeHead][1] == 1)) {
                    if (log) {
                        System.out.println("FindCircuit1error = 1");
                    }
                } else if (forest.tree[edgeTail][edgeHead][0] == 1) {
                    CircuitMember newMember = new CircuitMember();
                    newMember.tail = edgeTail;
                    newMember.head = edgeHead;
                    newMember.type = 1;
                    newMember.nextMember = null;
                    if (circuit1[tail][head].member == null) {
                        circuit1[tail][head].member = newMember;
                    } else {
                        circuitMember.nextMember = newMember;
                    }
                    circuitMember = newMember;
                } else if (forest.tree[edgeTail][edgeHead][1] == 1) {
                    CircuitMember newMember = new CircuitMember();
                    newMember.tail = edgeHead;
                    newMember.head = edgeTail;
                    newMember.type = 2;
                    newMember.nextMember = null;
                    if (circuit1[tail][head].member == null) {
                        circuit1[tail][head].member = newMember;
                    } else {
                        circuitMember.nextMember = newMember;
                    }
                    circuitMember = newMember;
                } else {
                    if (log) {
                        System.out.println("FindCircuit1error = 2");
                    }
                }
            }
        } else {
            if (log) {
                System.out.println("FindCircuit1error = 3");
            }
        }

    }

    /*Used by BelongsToI1, AddToI
graph: (N, N, 2) m,n = 1 => m,n exists in the graph.
#1: i,j i<j #2: j,i j<i : As we have a multigraph
label: Can find the paths using the labels
Input: label of i is initialized. First time, it is set to i.
Finds path from i to j*/
    private void FindPath(ForestIU graph, int[][] label, int tail, int head) {
        int[] queue = new int[N];
        int i, j, queueIndex = 0, queueEndIndex = 0;

        if ((graph.tree[tail][tail][0] == 1) || (graph.tree[tail][tail][1] == 1)) {
            if (log) {
                System.out.println("FindPatherror = 1");
            }
        }
        for (i = 0; i < N; i++) {
            queue[i] = -1;
        }
        queue[0] = tail;
        while ((queueIndex < N) && (queue[queueIndex] >= 0) && (label[head][0] == -1)) {
            int temp, pickedNode;
            pickedNode = queue[queueIndex];
            temp = queueEndIndex + 1;
            for (j = 0; j < N; j++) {
                if (((graph.tree[pickedNode][j][0] == 1) || (graph.tree[pickedNode][j][1] == 1)) && (label[j][0] == -1) && (label[head][0] == -1)) {
                    if (temp >= N) {
                        if (log) {
                            System.out.println("FindPatherror = 2");
                        }
                    }
                    if ((graph.tree[pickedNode][j][0] == 1) && (graph.tree[pickedNode][j][1] == 1)) {
                        if (log) {
                            System.out.println("FindPatherror = 3");
                        }
                    } else if (graph.tree[pickedNode][j][0] == 1) {
                        label[j][1] = 1;
                    } else {
                        label[j][1] = 2;
                    }
                    label[j][0] = pickedNode;
                    queue[temp] = j;
                    temp++;
                }
            }
            queueIndex++;
            queueEndIndex = temp - 1;
        }


    }

    private int BelongsToI2(Circuit[][] circuit2, int[][] I, int tail, int head, int root, int K) {
        int belongsToI2, i, j;
        int sum = 0;
        CircuitMember circuitMember;
        circuitMember = circuit2[tail][head].member;

        //printf("here\n");
        for (i = 0; i < N; i++) {
            sum += I[i][head];
        }
        if (head == root) {
            belongsToI2 = 0;
        } else if (sum == K) {
            belongsToI2 = 0;
            for (j = 0; j < N; j++) {
                if (I[j][head] == 1) {
                    CircuitMember newMember = new CircuitMember();
                    newMember.tail = j;
                    newMember.head = head;
                    newMember.type = 1;
                    newMember.nextMember = null;
                    if (circuit2[tail][head].member == null) {
                        circuit2[tail][head].member = newMember;
                    } else {
                        circuitMember.nextMember = newMember;
                    }
                    circuitMember = newMember;
                }
            }
        } else if (sum > K) {
            belongsToI2 = 0;
            if (log) {
                System.out.println("BelongsToI2error = 1");
            }
        } else {
            belongsToI2 = 1;
        }

        return belongsToI2;
    }

    private void HeurRemoval(int[][] neigh, int[] removed, int numVertices, int K) {
        int i, j, numVerticesTemp, numNewVerticesTemp, minProcessed, pickedNode;
        int[] processed;
        int[] vertList;
        int numNewVertices = numVertices - N;

//        numVerticesTemp ;
//        numNewVerticesTemp = numNewVertices;

        processed = new int[numNewVertices];
        vertList = new int[numNewVertices];
        for (j = 0; j < numNewVertices; j++) {
            processed[j] = 0;/*1 means processed*/
            removed[j] = 0;
        }
        pickedNode = NetworkUtils.findMin(processed, numNewVertices);
        minProcessed = processed[pickedNode];

        while (minProcessed == 0) {
            int[][] neighTemp;
            int vertListInd = 0, flag;
            processed[pickedNode] = 1;
            numVerticesTemp = numVertices - NetworkUtils.sum(removed, numNewVertices) - 1;
            numNewVerticesTemp = numVerticesTemp - N;

            neighTemp = new int[numVerticesTemp][numVerticesTemp];
            for (i = 0; i < numVerticesTemp; i++) {
                for (j = 0; j < numVerticesTemp; j++) {
                    neighTemp[i][j] = 0;
                }
            }
            for (i = 0; i < N; i++) {
                for (j = 0; j < N; j++) {
                    neighTemp[i][j] = neigh[i][j];
                }
            }

            for (i = 0; i < numNewVertices; i++) {
                vertList[i] = -1;
            }
            for (i = 0; i < numNewVertices; i++) {
                if ((removed[i] == 0) && (i != pickedNode)) {
                    vertList[vertListInd] = i;
                    vertListInd = vertListInd + 1;
                }
            }
            if (numNewVerticesTemp > 0) {
                if (vertListInd != numNewVerticesTemp) {
                    if (log) {
                        System.out.println("HeurRemovalerror = 1");
                    }
                }
                for (i = 0; i < numNewVerticesTemp; i++) {
                    for (j = 0; j < numNewVerticesTemp; j++) {
                        neighTemp[N + i][N + j] = neigh[N + vertList[i]][N + vertList[j]];
                    }
                    for (j = 0; j < N; j++) {
                        neighTemp[N + i][j] = neigh[N + vertList[i]][j];
                        neighTemp[j][N + i] = neigh[j][N + vertList[i]];
                    }
                }
            }
            flag = NetworkUtils.CheckForKConnectivity(neighTemp, numVerticesTemp, K, N); /*1 means K-edge connected*/
            if (flag == 1) {
                removed[pickedNode] = 1;
            }

            pickedNode = NetworkUtils.findMin(processed, numNewVertices);
            minProcessed = processed[pickedNode];
        }
    }


    /**
     * Inner Class Definitions
     */
    class CircuitMember {
        int tail;
        int head;
        int type;
        /*Used only in BelongsToI1. =1 if undirected for ij, i<j, 2 if for ji, i<j*/
        CircuitMember nextMember;
    }

    class Circuit {
        CircuitMember member;
    }

    class X {
        int tail;
        int head;
    }

    class ForestI {
        /*NxN array (i,j)*/
        int[][] tree;
    }

    class ForestIU {
        /*NxNx2 array (i,j, 0/1)*/
        // 1 in #1- for edge corresponding to i->j, #2- for j->i, i<j
        int[][][] tree;
    }

    class BelongsToI1Label {
        int[][] value = new int[2][3];
        /*Third element in last dimension indicates the copy being used (1/2 (ij/ji) for i<j)
    	3rd dimension is for 1st/2nd copy of this edge - ij's copy, i<j: 1st one
	    is filled, else 2nd is filled (both may be filled too!)*/
    }


    public double[][] getXycoordAllVertices() {
        return relay_coordinates;
    }
}
