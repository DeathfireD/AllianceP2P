package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.interactions.PostMessageInteraction;
import org.alliance.core.interactions.PostMessageToAllInteraction;

import java.io.IOException;

/**
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 18:42:48
 */
public class ChatMessage extends PersistantRPC {

    private String message;
    private boolean messageToAll;

    public ChatMessage() {
        routable = true;
    }

    public ChatMessage(String message, boolean messageToAll) {
        this.message = message;
        this.messageToAll = messageToAll;
    }

    @Override
    public void execute(Packet in) throws IOException {
        message = in.readUTF();
        messageToAll = in.readBoolean();
        if (messageToAll) {
            manager.getCore().queNeedsUserInteraction(new PostMessageToAllInteraction(message, fromGuid));
        } else {
            manager.getCore().queNeedsUserInteraction(new PostMessageInteraction(message, fromGuid));
        }
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeUTF(message);
        p.writeBoolean(messageToAll);
        return p;
    }
}
