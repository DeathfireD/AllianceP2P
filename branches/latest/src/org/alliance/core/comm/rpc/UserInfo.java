package org.alliance.core.comm.rpc;

import org.alliance.Version;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.T;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.file.hash.Hash;
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
        if (f == null) {
            //this can happen when a friend is removed using the UI. Just blow this connection away.
            con.close();
            return;
        }
        boolean guidMismatch = false;
        int remoteGUID = in.readInt();
        if (remoteGUID != f.getGuid()) {
            if (T.t) {
                T.warn("GUID mismatch!!! Closing connection.");
            }
//            f.setGuid(remoteGUID);
            guidMismatch = true;
        }

        int port = in.readInt();
        f.updateLastKnownHostInfo(manager.getNetMan().getSocketFor(con).getInetAddress().getHostAddress(), port);
        f.setShareSize(in.readLong());
        int buildNumber = in.readInt();
        f.setAllianceBuildNumber(buildNumber);

        if (CoreSubsystem.ALLOW_TO_SEND_UPGRADE_TO_FRIENDS) {
            if (buildNumber < Version.BUILD_NUMBER && buildNumber > 1120) {
                //remote has old version
                Hash h = core.getFileManager().getAutomaticUpgrade().getMyJarHash();
                if (h != null) {
                    send(new NewVersionAvailable(h));
                }
            }
        }

        //now that we have a good connection to friend: verify that we only have ONE connection
        if (con.getRemoteFriend().hasMultipleFriendConnections()) {
            if (T.t) {
                T.trace("Has multple connections to a friend. Figuring out wich one of us should close the connection");
            }
            if ((con.getDirection() == Connection.Direction.IN && con.getRemoteUserGUID() > manager.getMyGUID()) ||
                    (con.getDirection() == Connection.Direction.OUT && manager.getMyGUID() > con.getRemoteUserGUID())) {
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
        }
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeInt(manager.getMyGUID());
        p.writeInt(core.getSettings().getServer().getPort());
        p.writeLong(core.getFileManager().getFileDatabase().getShareSize());
        p.writeInt(Version.BUILD_NUMBER);
        return p;
    }
}
