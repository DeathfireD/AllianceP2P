package org.alliance.core.interactions;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.SynchronizedNeedsUserInteraction;
import org.alliance.core.node.Friend;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-21
 * Time: 20:20:31
 * To change this template use File | Settings | File Templates.
 */
public class ForwardedInvitationInteraction extends SynchronizedNeedsUserInteraction {

    private String invitationCode;
    private int middlemanGuid;
    private String remoteName;
    private int fromGuid;

    public ForwardedInvitationInteraction(Friend middleman, String remoteName, int fromGuid, String invitationCode) {
        this.invitationCode = invitationCode;
        this.middlemanGuid = middleman.getGuid();
        this.remoteName = remoteName;
        this.fromGuid = fromGuid;
    }

    public String getInvitationCode() {
        return invitationCode;
    }

    public Friend getMiddleman(CoreSubsystem core) {
        return core.getFriendManager().getFriend(middlemanGuid);
    }

    public String getRemoteName() {
        return remoteName;
    }

    public int getFromGuid() {
        return fromGuid;
    }
}
