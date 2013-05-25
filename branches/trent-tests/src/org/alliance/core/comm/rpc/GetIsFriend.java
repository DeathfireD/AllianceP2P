package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.node.Node;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class GetIsFriend extends RPC {

    private Node query;

    public GetIsFriend() {
    }

    public GetIsFriend(Node f) {
        query = f;
    }

    @Override
    public void execute(Packet data) throws IOException {
        int guid = data.readInt();
        send(new IsFriend(manager.getFriend(guid) != null, guid));
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeInt(query.getGuid());
        return p;
    }
}
