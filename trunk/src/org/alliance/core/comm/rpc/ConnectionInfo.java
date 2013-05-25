package org.alliance.core.comm.rpc;

import org.alliance.core.T;
import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.node.Friend;

import java.io.IOException;

/**
 *
 * Sent when a friend needs information about the correct ip/port of a common friend of ours.
 *
 * Recieved when we need info about a friend that we haven't got the correct ip/port to.
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 18:42:48
 */
public class ConnectionInfo extends RPC {

    private Friend friend;

    public ConnectionInfo() {
    }

    public ConnectionInfo(Friend f) {
        friend = f;
    }

    @Override
    public void execute(Packet in) throws IOException {
        int guid = in.readInt();
        if (guid != manager.getMyGUID()) {
            friend = manager.getFriend(guid);

            String host = in.readUTF();
            int port = in.readInt();
            String dnsName = in.readUTF();

            if (friend == null) {
                if (T.t) {
                    T.error("Friend is null!");
                }
            } else {
                if (friend.isConnected()) {
                    if (T.t) {
                        T.info("Already connected to friend.");
                    }
                } else {
                    if (friend.updateLastKnownHostInfo(host, port, dnsName)) {
                        manager.getFriendConnector().wakeUp();
                    }
                }
            }
        }
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeInt(friend.getGuid());
        p.writeUTF(friend.getLastKnownHost());
        p.writeInt(friend.getLastKnownPort());
        p.writeUTF(friend.getLastKnownDns());
        return p;
    }
}
