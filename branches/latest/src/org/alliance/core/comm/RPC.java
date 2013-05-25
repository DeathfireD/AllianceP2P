package org.alliance.core.comm;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.node.FriendManager;

import java.io.IOException;

import com.stendahls.util.TextUtils;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:30:20
 * To change this template use File | Settings | File Templates.
 */
public abstract class RPC {

    protected transient FriendConnection con;
    protected transient FriendManager manager;
    protected transient CoreSubsystem core;
    /**
     * Can this RPC be router through several nodes to reach its destination? This is not used right now -
     * RPCs are only sent to ones friends
     */
    protected transient boolean routable = false;
    /**
     * Can this RPC be broadcasted in an exponential fashion to my friends and their friends? Not used right now 
     */
    protected transient boolean broadcastable = false;
    protected transient int fromGuid, hops;
    private transient boolean initialized;

    public abstract void execute(Packet in) throws IOException;

    public abstract Packet serializeTo(Packet out) throws IOException;

    /**
     * Initializes this RCP in a mode where it has not been received from anyone - fromGuid is 0
     * @param rpcc
     */
    public RPC init(FriendConnection rpcc) {
        return init(rpcc, 0, 0);
    }

    /**
     * Might be ran several times when broadcasting a packet..  is this bad?
     * @param rpcc
     * @param fromGuid
     * @param numHops
     */
    public RPC init(FriendConnection rpcc, int fromGuid, int numHops) {
        this.con = rpcc;
        manager = con.getNetMan().getFriendManager();
        core = con.getNetMan().getCore();
        this.fromGuid = fromGuid;
        this.hops = numHops;
        initialized = true;
        return this;
    }

    protected void send(RPC rpc) throws IOException {
        con.send(fromGuid, rpc);
    }

    protected void send(int dstGuid, RPC rpc) throws IOException {
        con.send(dstGuid, rpc);
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public String toString() {
        return TextUtils.simplifyClassName(getClass()) + "(RPC)";
    }

    public boolean isRoutable() {
        return routable;
    }

    public boolean isBroadcastable() {
        return broadcastable;
    }
}
