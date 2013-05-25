package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class GetUserList extends RPC {

    public GetUserList() {
        routable = true;
    }

    @Override
    public void execute(Packet data) throws IOException {
        send(new UserList());
    }

    @Override
    public Packet serializeTo(Packet p) {
        core.increaseGULCounter();
        return p;
    }
}
