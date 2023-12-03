package inbox;

import java.util.ArrayList;

/**
 * @author : Fatih Senel
 *         Date: Nov 19, 2007
 *         Time: 3:55:18 PM
 */
public class MessageBox {
    private ArrayList<Message> messages = new ArrayList<Message>();
    /**
     * Points to the next unread message;
     */
    int ptrMessage = 0;

    public MessageBox() {
    }


    /**
     * If the inbox contains the message m, ignore it.
     * Default implementation of #addMessage (Message m, boolean force)
     *
     * @param m message that is received
     * @return true if message is added to message box, false ow
     */
    public boolean addMessage(Message m) {
        return addMessage(m, false);
    }


    /**
     * @param m     message that is received
     * @param force if inbox contains the same message, add the message anyway
     * @return true if message is added to message box, false ow
     */
    public boolean addMessage(Message m, boolean force) {
        if (!force) {
            if (!messages.contains(m)) {
                messages.add(m);
                return true;
            }
            return false;
        } else {
            messages.add(m);
            return true;
        }
    }

    /**
     * Retrieves next Unread Message
     * @return next unread message
     */
    public Message getNextUnreadMessage() {
        if (ptrMessage < messages.size()) {
            Message message = messages.get(ptrMessage);
            message.markAsRead();
            ptrMessage++;
            return message;
        } else {
            return null;
        }
    }

    /**
     * Clears the message box
     */
    public void empty() {
        messages.clear();
    }


    public ArrayList<Message> getMessages() {
        return messages;
    }
}

