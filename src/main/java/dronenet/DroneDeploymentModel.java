package dronenet;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

/**
 * @author fatihsenel
 * date: 05.07.23
 * ****** TEST ********
 */
public class DroneDeploymentModel {

    public void createModel(){
        try {
            IloCplex cplex = new IloCplex();
            
        } catch (IloException e) {
            e.printStackTrace();
        }
    }
}
