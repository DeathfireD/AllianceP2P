package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.interactions.PostMessageInteraction;
import org.alliance.core.interactions.PostMessageToAllInteraction;

import java.io.IOException;

/**
 *
 * version 2 of chat message
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 18:42:48
 */
public class ChatMessage extends PersistantRPC {

    private String message;
    private boolean messageToAll;
    private long sentAtTick;

    public ChatMessage() {
        routable = true;
    }

    public ChatMessage(String message, boolean messageToAll) {
        this.message = message;
        this.messageToAll = messageToAll;
        sentAtTick = System.currentTimeMillis();
    }

    @Override
    public void execute(Packet in) throws IOException {
        message = in.readUTF();
        messageToAll = in.readBoolean();
        sentAtTick = in.readLong();
        hasBeenQueuedForLaterSend = in.readBoolean();
        if (messageToAll) {
            manager.getCore().queNeedsUserInteraction(new PostMessageToAllInteraction(message, fromGuid, sentAtTick, hasBeenQueuedForLaterSend));
        } else {
            manager.getCore().queNeedsUserInteraction(new PostMessageInteraction(message, fromGuid, sentAtTick, hasBeenQueuedForLaterSend));
        }
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeUTF(message);
        p.writeBoolean(messageToAll);
        p.writeLong(sentAtTick);
        p.writeBoolean(hasBeenQueuedForLaterSend);
        return p;
    }
}
