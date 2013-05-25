package org.alliance.core.comm.upnp;

import org.alliance.core.comm.AuthenticatedConnection;
import org.alliance.core.comm.Connection.Direction;
import org.alliance.core.comm.HandshakeConnection;
import org.alliance.core.comm.NetworkManager;
import org.alliance.core.comm.Packet;
import org.alliance.core.comm.filetransfers.T;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2007-jan-10
 * Time: 22:29:04
 * To change this template use File | Settings | File Templates.
 */
public class ReverseConnection extends AuthenticatedConnection {

    public static final int CONNECTION_ID = 5;
    private int reverseConnectionId;

    protected ReverseConnection(NetworkManager netMan, Direction direction) {
        super(netMan, direction);
    }

    protected ReverseConnection(NetworkManager netMan, Direction direction, Object key) {
        super(netMan, direction, key);
    }

    public ReverseConnection(NetworkManager netMan, Object key, Direction direction, int userGUID) {
        super(netMan, key, direction, userGUID);
    }

    public ReverseConnection(NetworkManager netMan, Direction direction, int userGUID) {
        super(netMan, direction, userGUID);
    }

    public ReverseConnection(NetworkManager netMan, Direction direction, int userGUID, int reverseConnectionId) {
        super(netMan, direction, userGUID);
        this.reverseConnectionId = reverseConnectionId;
    }

    @Override
    public void init() throws IOException {
        super.init();
        if (direction == Direction.OUT) {
            if (T.t) {
                T.debug("Sending reverse connect connectionId (" + reverseConnectionId + ")");
            }
            Packet p = netMan.createPacketForSend();
            p.writeInt(reverseConnectionId);
            send(p);
            HandshakeConnection hc = new HandshakeConnection(netMan, key);
            netMan.replaceConnection(getKey(), hc);
            hc.init();
        }
    }

    @Override
    public void packetReceived(Packet p) throws IOException {
        if (direction == Direction.IN) {
            reverseConnectionId = p.readInt();
            if (T.t) {
                T.debug("Reveived reverse connection id " + reverseConnectionId);
            }
            AuthenticatedConnection c = netMan.fetchReveresedConnection(reverseConnectionId);
            if (c == null) {
                if (T.t) {
                    T.error("Could not find reversed connection with ID " + reverseConnectionId + "!!!");
                }
                return;
            }
            netMan.replaceConnection(getKey(), c);
            c.setKey(getKey());
            c.sendConnectionIdentifier();
            c.init();
        } else {
            if (T.t) {
                T.error("Ehh! Should not receive any packets here!");
            }
        }
    }

    @Override
    protected int getConnectionId() {
        return CONNECTION_ID;
    }
}
