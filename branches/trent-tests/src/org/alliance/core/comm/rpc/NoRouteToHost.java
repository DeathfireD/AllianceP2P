package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.comm.T;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 22:02:32
 * To change this template use File | Settings | File Templates.
 */
public class NoRouteToHost extends RPC {

    private int hostGuid;

    public NoRouteToHost() {
        routable = true;
    }

    public NoRouteToHost(int hostGuid) {
        this.hostGuid = hostGuid;
    }

    @Override
    public void execute(Packet in) throws IOException {
        int i = in.readInt();
        if (T.t) {
            T.error("No Route to host: " + i);
        }
        manager.getCore().getUICallback().noRouteToHost(manager.getNode(i));
    }

    @Override
    public Packet serializeTo(Packet out) {
        out.writeInt(hostGuid);
        return out;
    }
}
