package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.comm.T;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:36:41
 * To change this template use File | Settings | File Templates.
 */
public class Pong extends RPC {

    @Override
    public void execute(Packet p) {
        String s = "Received Pong from " + con.getRemoteFriend().getNickname() + ". RTT: " + (System.currentTimeMillis() - con.getLastPacketSentAt()) + "ms";
        if (T.t) {
            T.info(s);
        }
        con.pongReceived();
        core.updateLastSeenOnlineFor(con.getRemoteFriend());
    }

    @Override
    public Packet serializeTo(Packet p) {
        return p;
    }
}
