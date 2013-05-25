package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.interactions.PostMessageToAllInteraction;
import org.alliance.core.interactions.PostMessageInteraction;

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
public class ChatMessageV2 extends PersistantRPC {

    private String message;
    private boolean messageToAll;
    private long sentAtTick;

    public ChatMessageV2() {
        routable = true;
    }

    public ChatMessageV2(String message, boolean messageToAll) {
        this.message = message;
        this.messageToAll = messageToAll;
        sentAtTick = System.currentTimeMillis();
    }

    @Override
    public void execute(Packet in) throws IOException {
        message = in.readUTF();
        messageToAll = in.readBoolean();
        sentAtTick = in.readLong();
        if (messageToAll) {
            manager.getCore().queNeedsUserInteraction(new PostMessageToAllInteraction(message, fromGuid, sentAtTick));
        } else {
            manager.getCore().queNeedsUserInteraction(new PostMessageInteraction(message, fromGuid, sentAtTick));
        }
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeUTF(message);
        p.writeBoolean(messageToAll);
        p.writeLong(sentAtTick);
        return p;
    }
}
