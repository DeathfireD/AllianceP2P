package org.alliance.core.comm.rpc;

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
public class Route extends RPC {

    public static int MAX_HOPS = 8;
    private int srcGuid, dstGuid;
    private Packet meAsAPacket;
    private byte[] payload;

    public Route() {
    }

    public Route(int srcGuid, int dstGuid, Packet packet) {
        this.srcGuid = srcGuid;
        this.dstGuid = dstGuid;
        payload = packet.asArray();
    }

    @Override
    public void execute(Packet in) throws IOException {
        srcGuid = in.readInt();
        dstGuid = in.readInt();

        in.mark();
        int hops = in.readUnsignedByte();
        manager.getNetMan().getPackageRouter().updateRouteTable(con.getRemoteFriend(), srcGuid, hops);

        in.reset();
        in.writeByte((byte) (hops + 1)); //patch packet to contain updated hops information

        int payloadLen = in.readUnsignedShort();

        if (dstGuid == manager.getMyGUID()) {
            if (T.t) {
                T.debug("Routed package arrived to me! It came from " + hops + " hops away.");
            }
            con.received(srcGuid, hops, in, this); //handle package in payload
            return;
        }

        if (hops > MAX_HOPS) {
            if (T.t) {
                T.warn("Dropping forwarding packet - exeeded " + MAX_HOPS + " hops!");
            }
            in.skip(payloadLen);
            return;
        }

        in.mark();

        RPC r = RPCFactory.newInstance(in.readByte());
        if (!r.isRoutable()) {
            if (T.t) {
                T.warn("Someone trying to Route non-routable package! " + r.getClass().getName());
            }
            return;
        }

        if (r instanceof UserList) {
            UserList ui = (UserList) r;
            ui.init(con, srcGuid, hops);
            ui.updateRouteTableFrom(in, hops);
        }

        in.reset();
        if (T.t) {
            T.trace("Skipping payload (" + payloadLen + " bytes, hops: " + hops + ", dstGuid: " + dstGuid + ")");
        }
        in.skip(payloadLen);
        meAsAPacket = in; //packet is ready to be forwarded.

        Friend closest = manager.getNetMan().getPackageRouter().findClosestFriend(dstGuid);
        if (closest == null) {
            if (T.t) {
                T.trace("No Route to host " + dstGuid + "!");
            }
            send(srcGuid, new NoRouteToHost(dstGuid));
        } else {
            if (T.t) {
                T.trace("Forwarding packet from " + srcGuid + " to " + dstGuid + " via " + closest + ".");
            }
            closest.getFriendConnection().send(this);
        }
    }

    @Override
    public Packet serializeTo(Packet out) {
        if (meAsAPacket == null) {
            if (dstGuid == manager.getMyGUID()) {
                if (T.t) {
                    T.error("Trying to Route package to myself!!!");
                }
            }
            if (manager.getFriend(dstGuid) != null) {
                if (T.t) {
                    T.warn("Routing package to friend - not neccesary");
                }
            }
            out.writeInt(srcGuid);
            out.writeInt(dstGuid);
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
        return "Route (" + srcGuid + " -> " + dstGuid + ")";
    }
}
