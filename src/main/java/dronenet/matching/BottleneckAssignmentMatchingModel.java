package dronenet.matching;

import dronenet.Drone;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import utils.DoubleUtils;
import utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fatihsenel
 * date: 16.05.22
 */

public class BottleneckAssignmentMatchingModel implements MatchingModel{
    static BottleneckAssignmentMatchingModel instance;

    @Override
    public List<Drone> doMatching(List<Drone> A, List<Drone> B) {
        try {
            int n = A.size();
            IloCplex cplex = new IloCplex();
            cplex.setOut(null);

//            IloNumVar theta = cplex.
            IloNumVar theta = cplex.numVar(0, Double.MAX_VALUE);
            IloIntVar[][] x = new IloIntVar[n][n];
            double[][] c = new double[n][n];

            for (int i = 0; i < A.size(); i++) {
                Drone gateway = A.get(i);
                for (int j = 0; j < B.size(); j++) {
                    x[i][j] = cplex.intVar(0, 1);
                    cplex.addGe(x[i][j], 0);
                    Drone gateway1 = B.get(j);
                    c[i][j] = NetworkUtils.Distance(gateway, gateway1);
                    IloLinearNumExpr constraint = cplex.linearNumExpr();
                    constraint.addTerm(c[i][j], x[i][j]);
                    cplex.addGe(theta, constraint);
                }
            }

            for (int j = 0; j < B.size(); j++) {
                IloLinearNumExpr constraint = cplex.linearNumExpr();
                for (int i = 0; i < A.size(); i++) {
                    constraint.addTerm(x[i][j], 1);
                }
                cplex.addEq(constraint, 1);
            }

            for (int i = 0; i < A.size(); i++) {
                IloLinearNumExpr constraint = cplex.linearNumExpr();
                for (int j = 0; j < B.size(); j++) {
                    constraint.addTerm(x[i][j], 1);
                }
                cplex.addEq(constraint, 1);
            }

            cplex.addMinimize(theta);
            boolean isSolved = cplex.solve();
            List<Drone> result = new ArrayList<>();
            if (isSolved) {
                for (int i = 0; i < A.size(); i++) {
                    for (int j = 0; j < B.size(); j++) {
                        if (DoubleUtils.equals(cplex.getValue(x[i][j]), 1)) {
                            Drone gateway = new Drone(i);
                            gateway.setX(B.get(j).getX());
                            gateway.setY(B.get(j).getY());
                            result.add(gateway);

//                            System.out.println("P" + i + "-P" + j);
                        }
                    }
                }
                return result;

            } else {
                System.out.println("No");

            }

            // create model and solve it
        } catch (IloException e) {
            System.err.println("Concert exception caught: " + e);
        }
        return null;
    }

    public static BottleneckAssignmentMatchingModel getInstance() {
        if (instance == null) {
            instance = new BottleneckAssignmentMatchingModel();
        }

        return instance;
    }

}
