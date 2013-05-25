package org.alliance.core.comm.rpc;

import org.alliance.core.comm.FriendConnection;
import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.comm.RPCFactory;
import org.alliance.core.comm.T;
import org.alliance.core.node.Friend;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 20:17:22
 * To change this template use File | Settings | File Templates.
 */
public class Broadcast extends RPC {

    public static int MAX_HOPS = 10;
    private int srcGuid;
    private short msgId;
    private byte[] payload;
    private Packet meAsAPacket;

    public Broadcast() {
    }

    public Broadcast(int srcGuid, short msgId, Packet packet) {
        this.srcGuid = srcGuid;
        payload = packet.asArray();
        this.msgId = msgId;
    }

    @Override
    public RPC init(FriendConnection rpcc, int fromGuid, int numHops) {
        super.init(rpcc, fromGuid, numHops);
        if (payload != null) {
            manager.getBroadcastManager().checkHash(srcGuid, msgId, payload.length);
        }
        return this;
    }

    @Override
    public void execute(Packet in) throws IOException {
        srcGuid = in.readInt();
        msgId = (short) in.readUnsignedShort();

        in.mark();
        int hops = in.readUnsignedByte();
        manager.getNetMan().getPackageRouter().updateRouteTable(con.getRemoteFriend(), srcGuid, hops);

        in.reset();
        in.writeByte((byte) (hops + 1)); //patch packet to contain updated hops information

        int payloadLen = in.readUnsignedShort();

        if (!manager.getBroadcastManager().checkHash(srcGuid, msgId, payloadLen)) {
            if (T.t) {
                T.debug("Dropping already broadcasted message " + this);
            }
            in.skip(payloadLen);
            return;
        }

        if (T.t) {
            T.debug("Executing broadcasted message. It came from " + hops + " hops away.");
        }
        in.mark();
        con.received(srcGuid, hops, in, this); //handle package in payload
        in.reset();

        if (hops > MAX_HOPS) {
            if (T.t) {
                T.warn("Dropping forwarding packet - exeeded " + MAX_HOPS + "hops!");
            }
            return;
        }

        in.mark();
        RPC r = RPCFactory.newInstance(in.readByte());
        if (!r.isBroadcastable()) {
            if (T.t) {
                T.warn("Someone trying to Broadcast non-broadcastable package! " + r.getClass().getName());
            }
            return;
        }
        in.reset();

        in.skip(payloadLen);

        meAsAPacket = in; //packet is ready to be forwarded.

        for (Friend f : manager.friends()) {
            if (f.isConnected() && f.getFriendConnection() != null && f.getFriendConnection() != con) {
                if (T.t) {
                    T.info("Sending Broadcast to " + f + " remote: " + con.getRemoteFriend() + " con: " + con + " rc: " + f.getFriendConnection());
                }
                FriendConnection fc = f.getFriendConnection();
                fc.send(this);
            }
        }
    }

    @Override
    public Packet serializeTo(Packet out) {
        if (meAsAPacket == null) {
            out.writeInt(srcGuid);
            out.writeShort(msgId);
            out.writeByte((byte) 0); //hops
            out.writeShort(payload.length);
            out.writeArray(payload);
            return out;
        } else {
            return meAsAPacket;
        }
    }

    @Override
    public String toString() {
        return "Broadcast (from " + srcGuid + ")";
    }
}
