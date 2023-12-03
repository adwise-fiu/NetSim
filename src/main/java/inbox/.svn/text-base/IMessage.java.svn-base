package inbox;

/**
 * @author : Fatih Senel
 * Date: Apr 11, 2008
 * Time: 12:12:12 AM
 */
public interface IMessage {

    final int REQUEST = 0;
    final int ACK = 1;


    /**
     * request ID
     * @return requestid
     */
    public int getID();
    /**
     * the network index of the partion which sends the request
     * @return networkid
     */
    public int getNetworkID();


    /**
     * the index of the actor which passes this message to other partition (home)
     * @return actor index
     */
    public int getHomeSensorID();

    /**
     * the index of the actor that receives the message first  (foreign)
     * @return actor index
     */
    public int getForeignSensorID();


    /**
     * home gateway
     * @return index
     */
    public int getHomeGatewayID();


    /**
     * foreign gateway
     * @return index
     */
    public int getForeignGatewayID();

    /**
     * determines whether ACK or REQUEST message
     * @return type
     */
    public int getType();
}
