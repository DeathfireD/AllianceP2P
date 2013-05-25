package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.interactions.ForwardedInvitationInteraction;
import org.alliance.core.node.Node;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class ForwardedInvitation extends PersistantRPC {

    private String invitationCode;
    private int fromGuid;
    private String fromNickname;

    public ForwardedInvitation() {
    }

    public ForwardedInvitation(Node from, String invitationCode) {
        fromGuid = from.getGuid();
        this.invitationCode = invitationCode;
        this.fromNickname = from.getNickname();
    }

    @Override
    public void execute(Packet data) throws IOException {
        fromGuid = data.readInt();
        invitationCode = data.readUTF();
        fromNickname = data.readUTF();
        core.queNeedsUserInteraction(new ForwardedInvitationInteraction(con.getRemoteFriend(), fromNickname, fromGuid, invitationCode));
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeInt(fromGuid);
        p.writeUTF(invitationCode);
        p.writeUTF(fromNickname);
        return p;
    }
}
