package org.alliance.core.comm.rpc;

import org.alliance.core.T;
import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.node.Friend;

import java.io.IOException;

/**
 *
 * Sent when replying to GetIsFriend. Replies with the guid of friend and whether he's a trusted friend or not
 *
 * Received when trying to help a friend with a host/port pair that is out of date.
 *
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class IsFriend extends RPC {

    private boolean reply;
    private int guid;

    public IsFriend() {
    }

    public IsFriend(boolean reply, int guid) {
        this.reply = reply;
        this.guid = guid;
    }

    @Override
    public void execute(Packet data) throws IOException {
        int guid = data.readInt();
        boolean res = data.readBoolean();

        Friend wantsToConnectToRemote = manager.getFriend(guid);
        if (wantsToConnectToRemote == null) {
            if (T.t) {
                T.warn("Should be our trusted friend, but he's not");
            }
            return;
        }

        if (!res) {
            if (T.t) {
                T.warn("Hacked client? " + wantsToConnectToRemote + " said " + con.getRemoteFriend() + " was his friend. But he's not!");
            }
        } else {
            if (wantsToConnectToRemote.getFriendsFriend(con.getRemoteUserGUID()) != null &&
                    !wantsToConnectToRemote.getFriendsFriend(con.getRemoteUserGUID()).isConnected()) {
                if (wantsToConnectToRemote.getFriendConnection() == null) {
                    if (T.t) {
                        T.warn("No connection for friend!");
                    }
                } else {
                    wantsToConnectToRemote.getFriendConnection().send(new ConnectionInfo(con.getRemoteFriend()));
                }
            } else {
                if (T.t) {
                    T.warn("Wanted to send connection info to remote but he already connected to connectee (" + wantsToConnectToRemote.getFriendsFriend(guid) + ", " + guid + ")");
                }
            }
        }
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeInt(guid);
        p.writeBoolean(reply);
        return p;
    }
}
