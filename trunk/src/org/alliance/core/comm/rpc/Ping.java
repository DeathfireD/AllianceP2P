package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.comm.T;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class Ping extends RPC {

    @Override
    public void execute(Packet data) throws IOException {
        if (T.t) {
            T.debug("received Ping!");
        }
        send(new Pong());
    }

    @Override
    public Packet serializeTo(Packet p) {
        con.pingSent();
        return p;
    }
}
