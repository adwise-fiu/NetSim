package event;

import java.awt.*;
import java.util.EventListener;

/**
 * @author : Fatih Senel
 *         Date: Nov 19, 2007
 *         Time: 2:59:46 PM
 */
public class MessageEventMulticaster extends AWTEventMulticaster implements MessageListener {
    /**
     * Creates an event multicaster instance which chains listener-a
     * with listener-b. Input parameters <code>a</code> and <code>b</code>
     * should not be <code>null</code>, though implementations may vary in
     * choosing whether or not to throw <code>NullPointerException</code>
     * in that case.
     *
     * @param a listener-a
     * @param b listener-b
     */
    protected MessageEventMulticaster(EventListener a, EventListener b) {
        super(a, b);
    }

    public void messageReceived(MessageEvent e) {
        ((MessageListener) a).messageReceived(e);
        ((MessageListener) b).messageReceived(e);
    }

    /**
     * Adds MessageListener-a with MessageListener-b and
     * returns the resulting multicast listener.
     *
     * @param a MessageListener-a
     * @param b MessageListener-b
     * @return multicast
     */
    public static MessageListener add(MessageListener a, MessageListener b) {
        return (MessageListener) addInternal(a, b);
    }

    /**
     * Removes the old MessageListener from MessageListener-l and
     * returns the resulting multicast listener.
     *
     * @param l    MessageListener-l
     * @param oldl the MessageListener being removed
     * @return multicast listener
     */
    public static MessageListener remove(MessageListener l, MessageListener oldl) {
        return (MessageListener) removeInternal(l, oldl);
    }

}
