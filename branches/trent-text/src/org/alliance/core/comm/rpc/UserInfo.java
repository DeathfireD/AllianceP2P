package org.alliance.core.comm.rpc;

import org.alliance.Version;
import org.alliance.core.T;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.node.Friend;

import java.io.IOException;

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
public class UserInfo extends RPC {

    public UserInfo() {
    }

    @Override
    public void execute(Packet in) throws IOException {
        Friend f = con.getRemoteFriend();

        boolean guidMismatch = false;
        int remoteGUID = in.readInt();
        if (remoteGUID != f.getGuid()) {
            if (T.t) {
                T.warn("GUID mismatch!!! Closing connection.");
            }
            guidMismatch = true;
        }

        int port = in.readInt();
        String host = manager.getNetMan().getSocketFor(con).getInetAddress().getHostAddress();
        f.setShareSize(in.readLong());
        f.setAllianceBuildNumber(in.readInt());
        f.setTotalBytesReceived(in.readLong());
        f.setTotalBytesSent(in.readLong());
        f.setHighestIncomingCPS(in.readInt());
        f.setHighestOutgoingCPS(in.readInt());
        f.setNumberOfFilesShared(in.readInt());
        f.setNumberOfInvitedFriends(in.readInt());
        String dnsName = in.readUTF();
        f.updateLastKnownHostInfo(host, port, dnsName);

        //now that we have a good connection to friend: verify that we only have ONE connection
        if (con.getRemoteFriend().hasMultipleFriendConnections()) {
            if (T.t) {
                T.trace("Has multple connections to a friend. Figuring out wich one of us should close the connection");
            }
            if ((con.getDirection() == Connection.Direction.IN && con.getRemoteUserGUID() > manager.getMyGUID())
                    || (con.getDirection() == Connection.Direction.OUT && manager.getMyGUID() > con.getRemoteUserGUID())) {
                //serveral connections. Its up to us to close one
                if (T.t) {
                    T.info("Already connected to " + con.getRemoteFriend() + ". Closing connection");
                }
                send(new GracefulClose(GracefulClose.DUPLICATE_CONNECTION));
                //con.close();
                //close connection when we in turn receive a graceful close
            }
        }
        if (guidMismatch) {
            send(new GracefulClose(GracefulClose.GUID_MISMATCH));
        } else {
            //this is the place for an event: "FriendSuccessfullyConnected".
            core.getNetworkManager().signalFriendConnected(con.getRemoteFriend());
            core.getUICallback().nodeOrSubnodesUpdated(con.getRemoteFriend());
            core.updateLastSeenOnlineFor(con.getRemoteFriend());
            System.setProperty("alliance.network.friendsonline", "" + manager.getNFriendsConnected());
        }
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeInt(manager.getMyGUID());
        p.writeInt(core.getSettings().getServer().getPort());
        p.writeLong(core.getFileManager().getFileDatabase().getShareSize());
        p.writeInt(Version.BUILD_NUMBER);
        p.writeLong(core.getNetworkManager().getBandwidthIn().getTotalBytes());
        p.writeLong(core.getNetworkManager().getBandwidthOut().getTotalBytes());
        p.writeInt((int) Math.round(core.getNetworkManager().getBandwidthIn().getHighestCPS()));
        p.writeInt((int) Math.round(core.getNetworkManager().getBandwidthOut().getHighestCPS()));
        p.writeInt(core.getFileManager().getFileDatabase().getNumberOfShares());
        p.writeInt(core.getSettings().getMy().getInvitations());
        p.writeUTF(core.getSettings().getServer().getDnsname());
        return p;
    }
}
