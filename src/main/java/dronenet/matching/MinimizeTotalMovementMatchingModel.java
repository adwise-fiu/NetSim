package dronenet.matching;

import dronenet.Drone;
import ilog.concert.IloException;
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

public class MinimizeTotalMovementMatchingModel implements MatchingModel {
    static MinimizeTotalMovementMatchingModel instance;

    @Override
    public List<Drone> doMatching(List<Drone> A, List<Drone> B) {
        try {
            IloCplex cplex = new IloCplex();
            cplex.setOut(null);
            // Number of drones in each list
            int numA = A.size();
            int numB = B.size();

            // Create decision variables x_ij
            IloNumVar[][] x = new IloNumVar[numA][];
            for (int i = 0; i < numA; i++) {
                x[i] = cplex.boolVarArray(numB);
            }

            // Objective function
            IloLinearNumExpr obj = cplex.linearNumExpr();
            for (int i = 0; i < numA; i++) {
                for (int j = 0; j < numB; j++) {
                    double c_ij = NetworkUtils.Distance(A.get(i), B.get(j));
                    obj.addTerm(c_ij, x[i][j]);
                }
            }
            cplex.addMinimize(obj);

            // Constraints
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

    public static MinimizeTotalMovementMatchingModel getInstance() {
        if (instance == null) {
            instance = new MinimizeTotalMovementMatchingModel();
        }

        return instance;
    }

}
