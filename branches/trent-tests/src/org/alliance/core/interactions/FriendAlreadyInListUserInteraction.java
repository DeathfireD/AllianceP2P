package org.alliance.core.interactions;

import org.alliance.core.SynchronizedNeedsUserInteraction;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-apr-13
 * Time: 16:58:19
 */
public class FriendAlreadyInListUserInteraction extends SynchronizedNeedsUserInteraction {

    private int guid;

    public FriendAlreadyInListUserInteraction() {
    }

    public FriendAlreadyInListUserInteraction(int guid) {
        this.guid = guid;
    }

    public int getGuid() {
        return guid;
    }

    public void setGuid(int guid) {
        this.guid = guid;
    }
}
