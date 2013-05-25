package org.alliance.core;

import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-07
 * Time: 17:03:42
 * To change this template use File | Settings | File Templates.
 */
public class BroadcastManager {
    //@todo: NEED TO REMOVE HASHES!

    private HashSet<Integer> alreadyBroadcasted = new HashSet<Integer>();

    public boolean checkHash(int srcGuid, short msgId, int payloadLen) {
        return checkHash(createHash(srcGuid, msgId, payloadLen));
    }

    public boolean checkHash(int broadcastHash) {
        if (alreadyBroadcasted.contains(broadcastHash)) {
            return false;
        }
        alreadyBroadcasted.add(broadcastHash);
        if (T.t) {
            T.trace("Added Broadcast hash " + broadcastHash + ". " + alreadyBroadcasted.size() + " hashes in table.");
        }
        return true;
    }

    public int createHash(int srcGuid, short msgId, int payloadLen) {
        return payloadLen ^ ((int) msgId << 16) ^ srcGuid;
    }
}
