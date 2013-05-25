package org.alliance.core.comm;

import com.stendahls.util.TextUtils;
import org.alliance.Version;
import org.alliance.core.comm.filetransfers.UploadConnection;
import org.alliance.core.comm.upnp.ReverseConnection;
import org.alliance.core.node.Friend;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 14:55:58
 */
public abstract class AuthenticatedConnection extends PacketConnection {

    protected int remoteUserGUID;

    protected AuthenticatedConnection(NetworkManager netMan, Direction direction) {
        super(netMan, direction);
    }

    protected AuthenticatedConnection(NetworkManager netMan, Direction direction, Object key) {
        super(netMan, direction, key);
    }

    protected AuthenticatedConnection(NetworkManager netMan, Object key, Direction direction, int userGUID) {
        super(netMan, direction, key);
        this.remoteUserGUID = userGUID;
    }

    protected AuthenticatedConnection(NetworkManager netMan, Direction direction, int userGUID) {
        super(netMan, direction);
        this.remoteUserGUID = userGUID;
    }

    @Override
    public void init() throws IOException {
        super.init();
        netMan.getFriendManager().connectionEstablished(this);
    }

    public int getRemoteUserGUID() {
        return remoteUserGUID;
    }

    public String getRemoteGroupName() {
        return getRemoteFriend().getUGroupName();
    }

    public void setRemoteUserGUID(int guid) {
        remoteUserGUID = guid;
    }

    public void sendConnectionIdentifier() throws IOException {
        if (T.t) {
            T.trace("Sending authentication and connection type " + getConnectionId());
        }
        Packet p = netMan.createPacketForSend();
        p.writeInt(Version.PROTOCOL_VERSION);
        p.writeByte((byte) getConnectionIdForRemote());
        p.writeInt(netMan.getFriendManager().getMyGUID());
        send(p);
    }

    public static AuthenticatedConnection newInstance(NetworkManager netMan, Object key, int connectiondId, int guid) throws IOException {
        switch (connectiondId) {
            case FriendConnection.CONNECTION_ID:
                return new FriendConnection(netMan, key, Connection.Direction.IN, guid);
            case UploadConnection.CONNECTION_ID:
                return new UploadConnection(netMan, key, Connection.Direction.IN, guid);
            case ReverseConnection.CONNECTION_ID:
                return new ReverseConnection(netMan, key, Connection.Direction.IN, guid);
            default:
                throw new IOException("Unknown connection id " + connectiondId);
        }
    }

    @Override
    public String toString() {
        return netMan.getFriendManager().getFriend(remoteUserGUID) + " (" + TextUtils.simplifyClassName(this) + ")";
    }

    public SocketAddress getSocketAddress() {
        return netMan.getSocketFor(this).getRemoteSocketAddress();
    }

    public Friend getRemoteFriend() {
        return netMan.getFriendManager().getFriend(remoteUserGUID);
    }
}

