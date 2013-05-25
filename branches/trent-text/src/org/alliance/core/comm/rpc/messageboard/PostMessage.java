package org.alliance.core.comm.rpc.messageboard;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.messageboard.Message;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Pontus
 * Date: 2006-apr-18
 * Time: 20:38:52
 * This is sent when a new message is posted in the message board.
 */
public class PostMessage extends RPC {

    private Message message;

    public PostMessage(Message msg) {
        message = msg;
        routable = true;
    }

    @Override
    public void execute(Packet in) throws IOException {
    }

    @Override
    public Packet serializeTo(Packet out) {
        return null;
    }
}
