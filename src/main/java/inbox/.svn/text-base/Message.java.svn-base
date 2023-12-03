package inbox;

import network.Gateway;

/**
 * @author : Fatih Senel
 *         Date: Nov 19, 2007
 *         Time: 3:43:07 PM
 */
public class Message {

    public static final int MESSAGE_TYPE1 = 1;
    public static final int MESSAGE_TYPE2 = 2;
    public static final int MESSAGE_TYPE3 = 3;
    public static final int MESSAGE_TYPE4 = 4;


    /**
     * Message that informs the receiver node about its new networkID
     */
    public static final int MESSAGE_TYPE5 = 5;

    /**
     * Message that informs the receiver node about its new networkID and setting the partition as the super partition
     */
    public static final int MESSAGE_TYPE6 = 6;

    /**
     * Message that each actor node informs the sensor in the same cluster
     * about to start exchange process if it is in primaryt partition.
     * The sensor who receive this message forwards the message to every sensor in its forwadTable
     * And then sends #MESSAGE_TYPE8 to exchange this information with those who have different networkID
     */
    public static final int MESSAGE_TYPE7 = 7;

    /**
     * Exchange Message with the other partition, includes a #IMessage attributes
     */
    public static final int MESSAGE_TYPE8 = 8;

    /**
     * Request is accepted
     */
    public static final int MESSAGE_TYPE9 = 9;

    /**
     * leader floods the partition about we are ready to move includes IMessage
     */
    public static final int MESSAGE_TYPE10 = 10;

    /**
     * ACK message that includes IMessage  
     */
    public static final int MESSAGE_TYPE11 = 11;


    int type;
    Object source;
    Object destination;

    boolean isRead;
    private IMessage attributes;


    public Message(int type, Object source) {
        this.type = type;
        this.source = source;
        isRead = false;
    }

    public void markAsRead() {
        isRead = true;
    }


    public int getType() {
        return type;
    }


    public Object getSource() {
        return source;
    }

    public void setSource(Object object) {
        source = object;
    }


    public IMessage getAttributes() {
        return attributes;
    }

    public void setAttributes(IMessage attributes) {
        this.attributes = attributes;
    }


    public String toString() {
        if (getType() == MESSAGE_TYPE9||getType() == MESSAGE_TYPE8) {

            String t = attributes.getType()==0?"R":"A";
            t += "-- "+attributes.getNetworkID();
            return ""+t;
        } else{
            return "" + getType();
        }
    }
}
