package utils;

import network.Gateway;
import network.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author : Fatih Senel
 * Date: Nov 18, 2007
 * Time: 9:22:11 PM
 */
public class MCDSUtils {

    public List<Gateway> NodesArray;
    public List<Gateway> NodesArrayBackup;


    public MCDSUtils(List<? extends Gateway> nodesArray) {
        NodesArray = new ArrayList<>();
        for (Gateway gateway : nodesArray) {
            NodesArray.add(gateway);
        }
        NodesArrayBackup = new ArrayList<>();
    }

    /**
     * Evaluates Minimum Connected Dominating Set of the run.
     */
    public void EvaluateCDS() {
        for (Gateway gateway : NodesArray) {
            gateway.setDominator(HasTwoUnconnectedNeighbors(gateway));
        }
        if (IsAllUnmarkedAsDominator()) {
            (NodesArray.get(NodesArray.size() - 1)).setDominator(true);
        } else {
            PruneRule1();
            PruneRule2();
        }
        InitDominatorNeighborhood();
        AttachDomineesToClosestDominators();
    }

    /**
     * Determines whether the given node has two neighbor u and v such that u and v does not have a link between them
     *
     * @param node source node
     * @return true if there is not a link between u and v, false otherwise
     */
    private boolean HasTwoUnconnectedNeighbors(Gateway node) {

        for (int i = 0; i < node.getNeighborList().size(); i++) {
            for (int j = 0; j < node.getNeighborList().size(); j++) {
                if (i != j) {
                    if (!IsNeighbor((node.getNeighborList().get(i)), (node.getNeighborList().get(j))))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * @param node1 first vertex
     * @param node2 seond vertex
     * @return true if there is alink between them node1 and node2
     */
    private boolean IsNeighbor(Gateway node1, Gateway node2) {
        for (int i = 0; i < node1.getNeighborList().size(); i++) {
            if (node1.getNeighborList().get(i).getID() == node2.getID())
                return true;
        }
        return false;
    }

    /**
     * The algorithm we used to calculate CDS is a distributed approach.
     * That's why it does not evaluate the optimal solution. Sometimes it may not found a dominator. (case triangle)
     * This method is required to detect this case
     *
     * @return true if at least one node is dominator, false otherwise
     */
    private boolean IsAllUnmarkedAsDominator() {
        for (int i = 0; i < NodesArray.size(); i++) {
            if ((NodesArray.get(i)).isDominator())
                return false;
        }
        return true;
    }

    /**
     * Wu's algorithmPrune Rule 1
     */
    private void PruneRule1() {
        for (int i = 0; i < NodesArray.size(); i++) {
            if (NodesArray.get(i).isDominator()) {
                Gateway gateway = NodesArray.get(i);
                ArrayList<Integer> closed_neighborhood = new ArrayList<Integer>();
                closed_neighborhood.add(gateway.getID());
                for (int j = 0; j < gateway.getNeighborList().size(); j++) {
                    closed_neighborhood.add((gateway.getNeighborList().get(j)).getID());
                }
                for (int k = 0; k < gateway.getNeighborList().size(); k++) {
                    Gateway neighbor = gateway.getNeighborList().get(k);
                    if (neighbor.isDominator()) {
                        ArrayList<Integer> neighborsClosed = new ArrayList<Integer>();
                        neighborsClosed.add(neighbor.getID());
                        for (int m = 0; m < neighbor.getNeighborList().size(); m++) {
                            neighborsClosed.add(neighbor.getNeighborList().get(m).getID());
                        }

                        if (IsSubsetOf(closed_neighborhood, neighborsClosed)) {
                            gateway.setDominator(false);
                        }
                    }
                }
            }
        }
    }

    private boolean IsSubsetOf(ArrayList<Integer> arr1, ArrayList<Integer> arr2) {
        if (arr2.size() >= arr1.size()) {
            for (int i = 0; i < arr1.size(); i++) {
                boolean ex = false;
                for (int j = 0; j < arr2.size(); j++) {
                    if (arr1.get(i) == arr2.get(j)) {
                        ex = true;
                        break;
                    }
                }
                if (!ex)
                    return false;
            }
            return true;
        } else {
            return false;

        }
    }

    /**
     * Wu's algorithmPrune Rule 2
     */
    private void PruneRule2() {
        for (int i = 0; i < NodesArray.size(); i++) {
            if (NodesArray.get(i).isDominator()) {
                Gateway gateway = NodesArray.get(i);
                ArrayList<Integer> closed_neighborhood;
                closed_neighborhood = findClosedNeighborSet(gateway);
                for (int k = 0; k < gateway.getNeighborList().size(); k++) {
                    Gateway neighbor = gateway.getNeighborList().get(k);
                    if (neighbor.isDominator()) {
                        ArrayList<Integer> neighborsClosed;
                        neighborsClosed = findClosedNeighborSet(neighbor);
                        for (int m = 0; m < neighbor.getNeighborList().size(); m++) {
                            Gateway node1 = neighbor.getNeighborList().get(m);
                            neighborsClosed.add(node1.getID());
                            if (IsNeighbor(node1, gateway) && node1.isDominator()) {
                                unionSet(neighborsClosed, findClosedNeighborSet(node1));
                            }
                        }
                        if (IsSubsetOf(closed_neighborhood, neighborsClosed)) {
                            gateway.setDominator(false);
                        }
                    }
                }
            }
        }
    }

    private ArrayList<Integer> findClosedNeighborSet(Gateway gateway) {
        ArrayList<Integer> closed_neighborhood = new ArrayList<Integer>();
        closed_neighborhood.add(gateway.getID());
        for (int j = 0; j < gateway.getNeighborList().size(); j++) {
            closed_neighborhood.add(gateway.getNeighborList().get(j).getID());
        }
        return closed_neighborhood;
    }

    // adds the elements of set2 to set1 if not exists in set1
    private void unionSet(ArrayList<Integer> set1, ArrayList<Integer> set2) {
        for (int i = 0; i < set2.size(); i++) {
            boolean exists = false;
            for (int j = 0; j < set1.size(); j++) {
                if (set1.get(j) == set2.get(i)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                set1.add(set2.get(i));
            }
        }
    }

    /**
     * Connects neighbor dominators
     */
    private void InitDominatorNeighborhood() {
        ArrayList<Gateway> dominatorList = new ArrayList<Gateway>();
        Gateway tmp;
        for (int k = 0; k < NodesArray.size(); k++) {
            if ((NodesArray.get(k)).isDominator()) {
                tmp = NodesArray.get(k);
                dominatorList.add(tmp);
            }
        }
        for (int i = 0; i < dominatorList.size(); i++) {
            for (int j = 0; j < dominatorList.size(); j++) {
                if (i != j) {
                    Gateway ptr1;
                    ptr1 = dominatorList.get(i);
                    Gateway ptr2;
                    ptr2 = dominatorList.get(j);
                    if (IsNeighbor(ptr1, ptr2)) {
                        (dominatorList.get(i)).getNeighborDominatorList().add(dominatorList.get(j));
                    }
                }
            }
        }
    }

    /**
     * finds dominator node for each of the nodes in the non-dominator set
     */
    private void AttachDomineesToClosestDominators() {
        ArrayList<Gateway> dominatorList = new ArrayList<Gateway>();
        ArrayList<Gateway> domineeList = new ArrayList<Gateway>();
        for (int k = 0; k < NodesArray.size(); k++) {
            if (NodesArray.get(k).isDominator())
                dominatorList.add((NodesArray.get(k)));
            else
                domineeList.add((NodesArray.get(k)));
        }

        for (int i = 0; i < domineeList.size(); i++) {
            Gateway ptrDominee = domineeList.get(i);
            double minDist = -1;
            double tmpDist;
            for (int j = 0; j < dominatorList.size(); j++) {
                Gateway ptrDominator = dominatorList.get(j);
                tmpDist = NetworkUtils.Distance(ptrDominee, ptrDominator);
                if (minDist == -1 || minDist > tmpDist) {
                    minDist = tmpDist;
                    ptrDominee.dominator = ptrDominator;
                }
            }
        }

        for (int m = 0; m < domineeList.size(); m++) {
            Gateway ptrDominee = domineeList.get(m);
            Gateway ptrDominator = ptrDominee.dominator;
            ptrDominator.getDominateeList().add(ptrDominee);
        }
    }

    public ArrayList<Gateway> getDominatorList() {
        ArrayList<Gateway> result = new ArrayList<Gateway>();
        for (int i = 0; i < NodesArray.size(); i++) {
            if (NodesArray.get(i).isDominator())
                result.add(NodesArray.get(i));
        }
        return result;
    }

    /**
     * @return all dominatees in the list
     */
    public ArrayList<Gateway> getDominateeList() {
        ArrayList<Gateway> result = new ArrayList<Gateway>();
        for (int i = 0; i < NodesArray.size(); i++) {
            if (!NodesArray.get(i).isDominator())
                result.add(NodesArray.get(i));
        }
        return result;
    }

    /**
     * Iteratively runs the CDS on the network of dominators until only one dominator remains
     * HashMap holds dominate->dominator (key-value) pairs. For instance in the first iteration of MCDS
     * some of the nodes will be dominatees. We will keep the dominators of these dominatees,
     * because in later iteration the a dominator can be converted to a dominatee.
     * For example:Assume levels array contains the following
     * 1st : {1->3,2->3,4->5,6->7,8->7,10->9} (3,5,7,9 are on a line and all of them are dominators in the first run)
     * 2nd : {3->5,9->7} (in the second run of MCDS 3 and 9 converted to dominatees)
     * 3rd : {5->7}
     *
     * @return the list of nodes which converted to dominatee's in ith iteration
     */
    public ArrayList<HashMap<Integer, Integer>> runHierarchicalCDS() {
        NodesArrayBackup.addAll(NodesArray);
        ArrayList<HashMap<Integer, Integer>> levels = new ArrayList<HashMap<Integer, Integer>>();
        ArrayList<Gateway> dominatorList;
        int iteration = 0;
        while (true) {
            EvaluateCDS();
            dominatorList = getDominatorList();
            // by-pass
            if (NetworkUtils.isEqual(NodesArray, dominatorList)) {
                if (dominatorList.size() != 1) {
                    Gateway g = NodesArray.get(0);
                    g.setDominator(false);
                    g.dominator = NetworkUtils.getClosestNeighborDominator(g);
                    NetworkUtils.RemoveAllNeighborDominators(g);
                    g.dominator.getDominateeList().add(g);
                    dominatorList = getDominatorList();
                }
            }
            if (dominatorList.size() == 1) {
                HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
                ArrayList<Gateway> dominatees = getDominateeList();
                for (int i = 0; i < dominatees.size(); i++) {
                    map.put(dominatees.get(i).getID(), dominatees.get(i).dominator.getID());
                }
                levels.add(map);

                HashMap<Integer, Integer> map1 = new HashMap<Integer, Integer>();
                map1.put(dominatorList.get(0).getID(), -1);
                levels.add(map1);
                break;
            } else {
                HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
                ArrayList<Gateway> dominatees = getDominateeList();
                for (int i = 0; i < dominatees.size(); i++) {
                    map.put(dominatees.get(i).getID(), dominatees.get(i).dominator.getID());
                }
                levels.add(map);
                NodesArray.clear();
                for (int i = 0; i < dominatorList.size(); i++) {
                    Gateway gateway = dominatorList.get(i);
                    gateway.setDominator(false);
//                    reset lists
                    gateway.getNeighborDominatorList().clear();
                    NodesArray.add(gateway);
                }
                NetworkUtils.calculateActorNeighborhoods(NodesArray, 0, NodesArray.size() - 1, Constants.ActorTransmissionRange);
            }
            iteration++;
        }
        NetworkUtils.calculateActorNeighborhoods(NodesArrayBackup, Constants.ActorTransmissionRange);
        return levels;
    }
}
