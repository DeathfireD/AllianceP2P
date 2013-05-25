package org.alliance.core.comm.rpc;

import org.alliance.core.comm.RPC;
import org.alliance.core.comm.Packet;
import org.alliance.core.comm.T;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.upnp.ReverseConnection;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class ConnectToMe extends RPC {

    private int connectionId;

    public ConnectToMe() {
    }

    public ConnectToMe(int connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public void execute(Packet data) throws IOException {
        connectionId = data.readInt();
        if (T.t) {
            T.debug(con.getRemoteFriend() + " want to rev connect with connectionId " + connectionId + ". Lets do it.");
        }

        ReverseConnection c = new ReverseConnection(core.getNetworkManager(), Connection.Direction.OUT, con.getRemoteUserGUID(), connectionId);
        manager.getNetMan().connect(con.getRemoteUserGUID(), c);
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeInt(connectionId);
        return p;
    }
}
