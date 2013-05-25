package org.alliance.core.comm.rpc;

import org.alliance.core.comm.FriendConnection;
import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.interactions.PleaseForwardInvitationInteraction;
import org.alliance.core.node.Node;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class PleaseForwardInvitation extends PersistantRPC {

    private String invitationCode;
    private int toGuid;

    public PleaseForwardInvitation() {
    }

    public PleaseForwardInvitation(Node toNode) throws Exception {
        toGuid = toNode.getGuid();
    }

    @Override
    public RPC init(FriendConnection rpcc) {
        super.init(rpcc);
        try {
            invitationCode = core.getInvitaitonManager().createInvitation(toGuid, con.getRemoteUserGUID()).getCompleteInvitaitonString();
        } catch (Exception e) {
            core.reportError(e, this);
        }
        return this;
    }

    public PleaseForwardInvitation(String invitationCode, int toGuid) {
        this.invitationCode = invitationCode;
        this.toGuid = toGuid;
    }

    @Override
    public void execute(Packet data) throws IOException {
        toGuid = data.readInt();
        invitationCode = data.readUTF();
        core.queNeedsUserInteraction(new PleaseForwardInvitationInteraction(con.getRemoteUserGUID(), toGuid, invitationCode));
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeInt(toGuid);
        p.writeUTF(invitationCode);
        return p;
    }
}
