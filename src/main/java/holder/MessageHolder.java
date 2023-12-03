package holder;

import network.Gateway;
import inbox.Message;

/**
 * User: Fatih Senel
 * Date: Apr 27, 2008
 * Time: 5:43:04 AM
 */
public class MessageHolder {
    public Gateway source, target;
    public Message message;

    public MessageHolder(Gateway source, Gateway target, Message message) {
        this.source = source;
        this.target = target;
        this.message = message;
    }
}
