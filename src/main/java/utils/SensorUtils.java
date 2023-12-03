package utils;

import inbox.Message;
import network.Gateway;
import network.NetworkNode;
import network.Sensor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author : Fatih Senel
 *         Date: Nov 18, 2007
 *         Time: 8:34:53 PM
 */
public class SensorUtils {


    /**
     * Broadcasts the message
     *
     * @param gateway source node
     * @param m       message to be broadcasted
     */
    public static void broadcastMessage(Gateway gateway, Message m) {
        for (int i = 0; i < gateway.getNeighborList().size(); i++) {
            Gateway g = gateway.getNeighborList().get(i);
            g.addMessage(m);
        }
        gateway.incrementNumberOfMessagesTransmitted();
    }

    /**
     * Multicasts the message to the essential links
     *
     * @param gateway transmitter
     * @param m       message to be sent
     */
    public static void multicastMessageToEssentialLinks(Gateway gateway, Message m) {
        ArrayList<Gateway> lst = gateway.getEssentialLinkList();
        for (int i = 0; i < lst.size(); i++) {
            Gateway gateway1 = lst.get(i);
            gateway1.addMessage(m);
        }
        if (lst.size() != 0)
            gateway.incrementNumberOfMessagesTransmitted();
    }

    public static void unicast(NetworkNode source, NetworkNode destination, Message m) {
        destination.addMessage(m);
        source.incrementNumberOfMessagesTransmitted();
    }

    public static void unicastToSTParent(Gateway gateway, Message m) {
        if (gateway.getStParent() != null) {
            gateway.getStParent().addMessage(m);
            gateway.incrementNumberOfMessagesTransmitted();
        }
    }

    public static void multicastMessageToTheForwardTable(Sensor sensor, Message message) {
        sensor.incrementNumberOfMessagesTransmitted();
        for (int i = 0; i < sensor.getForwardTable().size(); i++) {
            Sensor next = sensor.getForwardTable().get(i);
            next.addMessage(message);
        }
    }

    public static void forwardMessageToClusterHead(Sensor sensor, Message message, ArrayList<Sensor> sensorArray) {
        sensorArray.get(sensor.getNexthopID()).addMessage(message);
    }

    /**
     * Will be used only for Message Type 10
     *
     * @param actorsArray array
     * @param gateway source
     * @param message message
     */
    public static void forwardMessage10ToPartition(ArrayList<Gateway> actorsArray, Gateway gateway, Message message) {
        ArrayList<Gateway> neighbors = new ArrayList<Gateway>();
        neighbors.addAll(gateway.getNeighborList());
        Collections.sort(neighbors, new NeighborComparator());
        boolean isTransmitted = false;
        for (int i = 0; i < neighbors.size(); i++) {
            Gateway n = neighbors.get(i);
            boolean NotOKToSend= !actorsArray.get(n.getNetworkID()).getWaitList().isEmpty();
//            NotOKToSend ;
//            if (n.isDominator()) {
//
//            } else {
//                if(n.getDominator()!=null)
//                    NotOKToSend = !n.getDominator().getWaitList().isEmpty();
//                else
//                    NotOKToSend = false;
//            }
            if (!(n.isDominator() || n.containsMessage(message) || n.isPrimaryPartition() || NotOKToSend)) {
                isTransmitted = true;
                n.addMessage(message);
            }
        }
        if (isTransmitted)
            gateway.incrementNumberOfMessagesTransmitted();
    }

    private static class NeighborComparator implements Comparator {


        public int compare(Object o1, Object o2) {
            Gateway g1 = (Gateway) o1;
            Gateway g2 = (Gateway) o2;
            if (g1.getID() < g2.getID())
                return -1;
            else if (g1.getID() == g2.getID())
                return 0;
            else return 1;
        }
    }
}
