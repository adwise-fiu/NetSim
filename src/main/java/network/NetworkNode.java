package network;

import inbox.MessageBox;
import inbox.Message;
import event.MessageEvent;
import event.MessageListener;
import event.MessageEventMulticaster;

import java.util.EventListener;
import java.util.ArrayList;

/**
 * @author : Fatih Senel
 *         Date: Nov 21, 2007
 *         Time: 4:59:54 PM
 */
public class NetworkNode {

    protected MessageBox inbox = new MessageBox();
    transient MessageListener messageListener;
    protected int numberOfMessagesTransmitted = 0;
    protected double x;
    protected double y;
    protected int id;
    protected int diameter;
    protected int networkID=-1;
    protected boolean isPrimaryPartition =false;
    /**
     * 1 if the node is dominator, 0 otherwise
     */
    protected boolean isDominator;

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getID() {
        return id;
    }


    public void setId(int id) {
        this.id = id;
    }

    public void addMessage(Message m) {
        inbox.addMessage(m);
        fireMessageReceivedEvent(new MessageEvent(this, m));
    }

    /**
     * Adds the specified message listener to receive message events from
     * this button. inbox.Message events occur when a gateway receives a message
     *
     * @param l the message listener
     * @see #removeMessageListener
     * @see event.MessageListener
     */
    public synchronized void addMessageListener(MessageListener l) {
        if (l == null) {
            return;
        }
        messageListener = MessageEventMulticaster.add(messageListener, l);
    }

    /**
     * Removes the specified action listener so that it no longer
     * receives action events from this button. Action events occur
     * when a user presses or releases the mouse over this button.
     * If l is null, no exception is thrown and no action is performed.
     *
     * @param l the action listener
     * @see #addMessageListener(event.MessageListener)
     * @see java.awt.event.ActionListener
     */
    public synchronized void removeMessageListener(MessageListener l) {
        if (l == null) {
            return;
        }
        messageListener = MessageEventMulticaster.remove(messageListener, l);
    }

    public synchronized MessageListener[] getActionListeners() {
        return getListeners(MessageListener.class);
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        EventListener l = messageListener;
        return MessageEventMulticaster.getListeners(l, listenerType);
    }

    public void fireMessageReceivedEvent(MessageEvent e) {
        if (messageListener != null)
            messageListener.messageReceived(e);
    }

    public void incrementNumberOfMessagesTransmitted() {
        numberOfMessagesTransmitted++;
    }

    public int getNumberOfMessagesTransmitted() {
        return numberOfMessagesTransmitted;
    }

    public boolean isIn(int x, int y) {
        return x >= this.x - (diameter / 2) && x <= this.x + (diameter / 2) && y >= this.y - (diameter / 2) && y <= this.y + (diameter / 2);
    }

    public int getNetworkID() {
        return networkID;
    }

    public void setNetworkID(int networkID) {
        this.networkID = networkID;
    }

    public boolean isPrimaryPartition() {
        return isPrimaryPartition;
    }

    public void setPrimaryPartition(boolean primaryPartition) {
        this.isPrimaryPartition = primaryPartition;
    }

    public ArrayList<Message> getReceivedMessages(){
        return inbox.getMessages();
    }

    public void removeMessage(Message message) {
        for(int i=0;i<inbox.getMessages().size();i++){
            if(message == inbox.getMessages().get(i)){
                inbox.getMessages().remove(i);
                break;
            }
        }
    }

    public boolean containsMessage(Message m){
        for (int i = 0; i < inbox.getMessages().size(); i++) {
            Message message = inbox.getMessages().get(i);
            if(message==m)
                return true;
        }
        return false;
    }
}
