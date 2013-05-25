package org.alliance.core.comm.rpc;

import org.alliance.core.comm.RPC;
import org.alliance.core.comm.Packet;
import org.alliance.core.comm.T;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class AwayStatus extends RPC {

    private boolean away;

    public AwayStatus() {
    }

    public AwayStatus(boolean away) {
        this.away = away;
    }

    @Override
    public void execute(Packet data) throws IOException {
        away = data.readBoolean();
        if (T.t) {
            T.info("User " + con.getRemoteFriend() + " changes away status: " + away);
        }
        con.getRemoteFriend().setAway(away);
        core.getUICallback().nodeOrSubnodesUpdated(con.getRemoteFriend());
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeBoolean(away);
        return p;
    }
}
