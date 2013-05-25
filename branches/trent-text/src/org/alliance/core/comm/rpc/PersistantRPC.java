package org.alliance.core.comm.rpc;

import org.alliance.core.comm.RPC;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-apr-19
 * Time: 15:38:42
 *
 * A persistent RPC is an rpc that can be sent using NetworkManager.sendPersistantly(...). It is guaranteed to
 * arrive at its destination. Even if the destination will not be connected for several days and even if
 * the application is restarted.
 *
 */
public abstract class PersistantRPC extends RPC implements Serializable {

    private int destinationGuid;
    private long timestamp;
    protected boolean hasBeenQueuedForLaterSend;

    public int getDestinationGuid() {
        return destinationGuid;
    }

    public void setDestinationGuid(int destinationGuid) {
        this.destinationGuid = destinationGuid;
    }

    public void resetTimestamp() {
        timestamp = System.currentTimeMillis();
    }

    /**
     * Return true if this PersistantRPC is older than a month.
     * @return 
     */
    public boolean hasExpired() {
        return System.currentTimeMillis() - timestamp > ((long) 1000) * 60 * 60 * 24 * 31;
    }

    public void notifyRPCQueuedForLaterSend() {
        hasBeenQueuedForLaterSend = true;
    }
}
