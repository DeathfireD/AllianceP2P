package org.alliance.core.comm;

import org.alliance.core.comm.rpc.Broadcast;
import org.alliance.core.comm.rpc.GetMyExternalIp;
import org.alliance.core.comm.rpc.GetUserInfo;
import org.alliance.core.comm.rpc.GetUserInfoV2;
import org.alliance.core.comm.rpc.GetUserList;
import org.alliance.core.comm.rpc.Route;
import org.alliance.core.interactions.NewFriendConnectedUserInteraction;
import org.alliance.core.node.Friend;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:22:56
 * To change this template use File | Settings | File Templates.
 */
public class FriendConnection extends AuthenticatedConnection {

    public static final int CONNECTION_ID = 1;
    private int rpcsSent, rpcsReceived;
    private long lastPingSentAt;
    private long lastPongReceivedAt;

    public FriendConnection(NetworkManager netMan, Direction direction, int userGUID) {
        super(netMan, direction, userGUID);
    }

    public FriendConnection(NetworkManager netMan, Object key, Direction direction, int guid) throws IOException {
        super(netMan, key, direction, guid);
    }

    @Override
    public void init() throws IOException {
        super.init();
        send(new GetUserList());
        send(new GetUserInfo());
        send(new GetUserInfoV2());
        send(new GetMyExternalIp());
        if (direction == Direction.OUT && getRemoteFriend().isNewlyDiscoveredFriend()) {
            core.queNeedsUserInteraction(new NewFriendConnectedUserInteraction(remoteUserGUID));
            getRemoteFriend().setNewlyDiscoveredFriend(false);
        }
    }

    @Override
    public void packetReceived(Packet p) throws IOException {
        received(getRemoteUserGUID(), 0, p);
    }

    public void received(int fromGuid, int hops, Packet packet) throws IOException {
        received(fromGuid, hops, packet, null);
    }

    public void received(int fromGuid, int hops, Packet packet, RPC outerRPC) throws IOException {
        int id = packet.readByte();
        RPC rpc = RPCFactory.newInstance(id);
        if (rpc == null) {
            if (T.t) {
                T.warn("Skipping unknown RPC ID: " + id + "!!!");
            }
            packet.skip(packet.getAvailable()); //skip contents of packet
            return;
        } else {
            if (T.t) {
                if (outerRPC == null) {
                    T.debug("<-- Recived " + rpc + " (" + (packet.getAvailable() + 1) + " bytes) from " + netMan.getFriendManager().getNode(fromGuid) + " (con: " + getRemoteFriend() + ")"); //+1 because we read one byte above
                } else {
                    T.debug("<----- Recived " + rpc + " (" + (packet.getAvailable() + 1) + " bytes) from " + netMan.getFriendManager().getNode(fromGuid) + " (con: " + getRemoteFriend() + ", outer: " + outerRPC + ")"); //+1 because we read one byte above
                }
            }
            rpc.init(this, fromGuid, hops);
            rpc.execute(packet);
            signalReceived(rpc);
        }
    }

    private void signalReceived(RPC rpc) {
        rpcsReceived++;
        setStatusString("s:" + rpcsSent + " r: " + rpcsReceived + " (last: <-" + rpc + ")");
        if (rpcsReceived % 10 == 0) {
            updateLastSeenOnline();
        }
    }

    private void signalSent(RPC rpc) {
        rpcsSent++;
        setStatusString("s:" + rpcsSent + " r: " + rpcsReceived + " (last: ->" + rpc + ")");
        if (rpcsSent % 10 == 0) {
            updateLastSeenOnline();
        }
    }

    private void updateLastSeenOnline() {
        if (core.getSettings().getFriend(remoteUserGUID) != null) {
            core.getSettings().getFriend(remoteUserGUID).setLastseenonlineat(System.currentTimeMillis());
        }
    }

    @Override
    protected int getConnectionId() {
        return CONNECTION_ID;
    }

    public void send(RPC rpc) throws IOException {
        send(getRemoteUserGUID(), rpc);
        signalSent(rpc);
    }

    public void send(int dstGuid, RPC rpc) throws IOException {
        if (dstGuid != getRemoteUserGUID()) {
            if (T.t) {
                T.debug("Rerouting package " + rpc + " to " + dstGuid);
            }
            route(dstGuid, rpc);
        } else {
            if (!rpc.isInitialized()) {
                rpc.init(this); //under certain conditions a rpc can be reused and sent a second time - then it's the first init call that counts. Don't make it twice.
            }
            Packet p = createRPCPacket(rpc);
            if (T.t) {
                T.debug("--> Sending " + rpc + " (" + p.getPos() + " bytes) to " + getRemoteFriend());
            }
            send(p);
        }
    }

    public void route(int dstGuid, RPC rpc) throws IOException {
        Friend f = netMan.getFriendManager().getFriend(dstGuid);
        if (f != null && f.getFriendConnection() != null) {
            if (T.t) {
                T.info("Rerouting package to friend");
            }
            f.getFriendConnection().send(rpc);
        } else {
            //note that one of our friends COULD actually be a closer Route to the destination. But this way we send
            //the rpc the same way back as be got it. No biggie probably.
            rpc.init(this);
            Packet p = createRPCPacket(rpc);
            send(new Route(netMan.getFriendManager().getMyGUID(), dstGuid, p));
        }
    }

    public void broadcast(short msgId, RPC rpc) throws IOException {
        rpc.init(this);
        Packet p = createRPCPacket(rpc);
        send(new Broadcast(netMan.getFriendManager().getMyGUID(), msgId, p));
    }

    public Packet createRPCPacket(RPC rpc) throws IOException {
        return createRPCPacket(netMan, rpc);
    }

    public static Packet createRPCPacket(NetworkManager m, RPC rpc) throws IOException {
        if (!rpc.isInitialized()) {
            if (T.t) {
                T.error("RPC not initialized!");
            }
        }
        Packet p = m.createPacketForSend();
        p.writeByte(RPCFactory.getPacketIdFor(rpc));
        p = rpc.serializeTo(p);
        return p;
    }

    @Override
    public String toString() {
        if (getRemoteFriend() == null) {
            return super.toString();
        }
        return getRemoteFriend().getNickname() + " (communication)";
    }

    public void pingSent() {
        lastPingSentAt = System.currentTimeMillis();
    }

    public void pongReceived() {
        lastPongReceivedAt = System.currentTimeMillis();
    }

    public int getNetworkLatency() {
        if (lastPingSentAt == 0) {
            return 0;
        }
        if (lastPingSentAt > lastPongReceivedAt) {
            return (int) (System.currentTimeMillis() - lastPingSentAt);
        }
        return (int) (lastPongReceivedAt - lastPingSentAt);
    }
}
