package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.comm.T;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;
import org.alliance.core.node.UntrustedNode;

import java.io.IOException;
import java.util.Collection;
import org.alliance.core.comm.IpDetection;

/**
 *
 * Sent as a reply to a GetUserList
 *
 * Recived when we connect to a new user, after we've sent GetUserList
 *
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class UserList extends RPC {

    public UserList() {
        routable = true;
    }

    @Override
    public void execute(Packet in) throws IOException {
        int remoteGUID = in.readInt();
        Node n = manager.getNode(remoteGUID);
        n.removeAllFriendsOfFriend();

        int nFriends = in.readInt();
        if (T.t) {
            T.trace("Received userlist for " + n + ", has " + nFriends + " friends.");
        }
        for (int i = 0; i < nFriends; i++) {
            int guid = in.readInt();
            String nickname = in.readUTF();
            boolean connected = in.readBoolean();
            boolean internal = in.readBoolean();
            UntrustedNode unNode = UntrustedNode.loadOrCreate(manager, nickname, guid, connected);
            unNode.setInternal(internal);

            n.addFriendsFriend(unNode);

            manager.getNetMan().getPackageRouter().updateRouteTable(con.getRemoteFriend(), guid, hops + 1);

            Friend connectee = manager.getFriend(unNode.getGuid());
            if (connectee != null && connectee.isConnected() && !unNode.isConnected() && remoteGUID == con.getRemoteFriend().getGuid()) {
                //lets help this friend out. He's not connected to our common trusted friend but we are
                if (T.t) {
                    T.info("Sending connection help to " + connectee);
                }
                connectee.getFriendConnection().send(new GetIsFriend(con.getRemoteFriend()));
            }
        }
        manager.getCore().getUICallback().nodeOrSubnodesUpdated(n);
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeInt(manager.getMyGUID());

        boolean internalFriend = IpDetection.isLan(manager.getNetMan().getSocketFor(con).getInetAddress().getHostAddress(), false);

        Collection<Friend> c = manager.friends();
        int n = 0;
        for (Friend f : c) {
            if (!f.hasNotBeenOnlineForLongTime() || f.getLastSeenOnlineAt() == 0) {
                n++;
            }
        }
        p.writeInt(n);
        for (Friend f : c) {
            if (!f.hasNotBeenOnlineForLongTime() || f.getLastSeenOnlineAt() == 0) {
                p.writeInt(f.getGuid());
                p.writeUTF(f.getNickname());
                p.writeBoolean(f.isConnected());
                if (internalFriend) {
                    p.writeBoolean(f.getInternal());
                } else {
                    p.writeBoolean(false);
                }
            }
        }
        return p;
    }

    public void updateRouteTableFrom(Packet in, int hops) {
        Friend f = con.getRemoteFriend();
        int remoteGuid = in.readInt();
        if (remoteGuid != f.getGuid()) {
            manager.getNetMan().getPackageRouter().updateRouteTable(f, remoteGuid, hops);
        }

        if (T.t) {
            T.trace("Peeking at userinfo for " + remoteGuid + ": ");
        }
        int nFriends = in.readInt();
        for (int i = 0; i < nFriends; i++) {
            int guid = in.readInt();
            String nickname = in.readUTF();
            boolean connected = in.readBoolean();
            in.readBoolean(); //internal
            if (T.netTrace) {
                T.trace("  " + nickname + " " + guid + " " + connected);
            }
            if (connected) {
                manager.getNetMan().getPackageRouter().updateRouteTable(f, guid, hops + 1);
            }
        }
    }
}
