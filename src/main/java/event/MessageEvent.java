package event;

import inbox.Message;

import java.util.EventObject;

/**
 * @author : Fatih Senel
 *         Date: Nov 19, 2007
 *         Time: 2:50:40 PM
 */
public class MessageEvent extends EventObject {

    Message message;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public MessageEvent(Object source) {
        super(source);
    }

    /**
     * Constructs a prototypical Event.
     *
     * @param source  The object on which the Event initially occurred.
     * @param message The Message which is received
     * @throws IllegalArgumentException if source is null.
     */
    public MessageEvent(Object source, Message message) {
        super(source);
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
