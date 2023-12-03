package inbox;

import network.NetworkNode;
import network.Gateway;

/**
 * @author : Fatih Senel
 *         Date: Nov 21, 2007
 *         Time: 6:25:52 PM
 */
public class MessageFactory {
    public static Message createMessageType5(NetworkNode source) {
        return new Message(Message.MESSAGE_TYPE5, source);
    }

    public static Message createMessageType6(Gateway source) {
        return new Message(Message.MESSAGE_TYPE6, source);
    }

    public static Message creMessageType7(NetworkNode source){
        return new Message(Message.MESSAGE_TYPE7,source);
    }
    
    public static Message createMessageType8(NetworkNode node, final int snsID, final int actorID, final int contactActorID) {
        final int nid = node.getNetworkID();
        final int sid = node.getID();
        IMessage attr = new IMessage() {

            public int getID() {
                return IDGEN.getNextID();
            }

            public int getNetworkID() {
                return nid;
            }

            public int getHomeSensorID() {
                return sid;
            }

            public int getForeignSensorID() {
                return snsID;
            }

            public int getType() {
                return IMessage.REQUEST;
            }

            public int getHomeGatewayID() {
                return contactActorID;
            }

            public int getForeignGatewayID() {
                return actorID;
            }
        };
        Message m = new Message(Message.MESSAGE_TYPE8, node);
        m.setAttributes(attr);
        return m;
    }

    public static Message createMessageType9(final Gateway gateway) {
        IMessage attr = new IMessage() {

            public int getID() {
                return gateway.getRequestInfo().getID();
            }

            public int getNetworkID() {
                return gateway.getNetworkID();
            }

            public int getHomeSensorID() {
                return gateway.getRequestInfo().getForeignSensorID();
            }

            public int getForeignSensorID() {
                return gateway.getRequestInfo().getHomeSensorID();
            }

            public int getHomeGatewayID() {
                return gateway.getRequestInfo().getForeignGatewayID();
            }

            public int getForeignGatewayID() {
                return gateway.getRequestInfo().getHomeGatewayID();
            }

            public int getType() {
                return IMessage.ACK;
            }
        };
        Message m = new Message(Message.MESSAGE_TYPE9, gateway);
        m.setAttributes(attr);
        return m;
    }

    public static Message createMessageType10(Gateway gateway, IMessage requestInfo) {
        Message m = new Message(Message.MESSAGE_TYPE10, gateway);
        m.setAttributes(requestInfo);
        return m;
    }

    public static Message createMessageType11(Gateway gateway, IMessage requestInfo) {
        Message m = new Message(Message.MESSAGE_TYPE11, gateway);
        m.setAttributes(requestInfo);
        return m;
    }
}
