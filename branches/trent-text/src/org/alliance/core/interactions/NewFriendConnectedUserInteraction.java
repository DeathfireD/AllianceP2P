package org.alliance.core.interactions;

import org.alliance.core.NeedsUserInteraction;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-apr-13
 * Time: 16:58:19
 */
public class NewFriendConnectedUserInteraction implements NeedsUserInteraction {

    private int guid;

    public NewFriendConnectedUserInteraction() {
    }

    public NewFriendConnectedUserInteraction(int guid) {
        this.guid = guid;
    }

    public int getGuid() {
        return guid;
    }

    public void setGuid(int guid) {
        this.guid = guid;
    }

    @Override
    public boolean canRunInParallelWithOtherInteractions() {
        return true;
    }
}
