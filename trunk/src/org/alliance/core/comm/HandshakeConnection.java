package org.alliance.core.comm;

import org.alliance.Version;
import org.alliance.core.interactions.NewFriendConnectedUserInteraction;
import org.alliance.core.node.Friend;
import org.alliance.core.Language;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import org.alliance.core.node.Invitation;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-26
 * Time: 11:56:47
 * To change this template use File | Settings | File Templates.
 */
public class HandshakeConnection extends PacketConnection {

    public static final int MAX_UPGRADEUPLOAD_CONNECTIONS = 3;

    public HandshakeConnection(NetworkManager netMan, Object key) {
        super(netMan, Direction.IN, key);
    }

    @Override
    public void packetReceived(Packet p) throws IOException {
        if (T.t) {
            T.trace("packetReceived in HandhshakeConnection - " + p);
        }
        if (netMan.isAddressBlocked(netMan.getSocketFor(this).getInetAddress())) {
            if (T.t) {
                T.warn("Banned host tried to connect to us. Ignroing: " + netMan.getSocketFor(this).getInetAddress());
            }
            close();
            netMan.getSocketFor(this).close();
            return;
        }
        if (netMan.getCore().getSettings().getInternal().getEnableiprules() == 1) {
            if (!core.getSettings().getRulelist().checkConnection(netMan.getSocketFor(this).getInetAddress().getAddress())) {
                core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "conblocked"));
                close();
                netMan.getSocketFor(this).close();
                return;
            }
        }

        int protocolVersion;
        try {
            protocolVersion = p.readInt();
        } catch (BufferUnderflowException e) {
            //this was reported by several users..  somethings fishy going on here    
            throw new IOException("Received truncated package at start of connection?!");
        }
        byte connectionType = p.readByte();
        int guid = p.readInt();

        if (netMan.getFriendManager().getFriend(guid) == null) {
            if (core.getInvitationManager().containsKey(guid)) {
                if (T.t) {
                    T.info("Aha! Some I invited is trying to connect!");
                }
                if (core.getInvitationManager().isValid(guid)) {
                    if (T.t) {
                        T.info("And the key is valid! Let's go!");
                    }
                    Invitation invi =  core.getInvitationManager().getInvitation(guid);
                    InvitationConnection c = new InvitationConnection(netMan, Connection.Direction.IN, key, guid,
                            invi.getMiddlemanGuid());
                    netMan.replaceConnection(key, c);
                    c.init();

                    if (invi.isForwardedInvitation() || invi.isValidOnlyOnce()) {
                        //Consume FoF invitations and onetime
                        core.getInvitationManager().consume(guid);
                    }
                    return;
                } else {
                    core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "codeexpired"));
                    core.getInvitationManager().consume(guid);
                }
            } else {
                if (T.t) {
                    T.error("Someone tried to connect to me that's not my friend (" + netMan.getSocketFor(this).getRemoteSocketAddress() + " guid: " + guid + ").");
                }
            }

            if (T.t) {
                T.info("Adding to banned hosts: " + netMan.getSocketFor(this).getInetAddress());
            }
            netMan.blockConnectionsTemporarilyFrom(this);
            close();
            netMan.getSocketFor(this).close();
        } else {
            Friend friend = netMan.getFriendManager().getFriend(guid);

            if (protocolVersion > Version.PROTOCOL_VERSION) {
                throw new RuntimeException("User " + netMan.getFriendManager().getFriend(guid).getNickname() + " has a newer version of Alliance then you. It cannot be updated automatically. Talk to him about it.");
            }
            if (protocolVersion < Version.PROTOCOL_VERSION) {
                throw new RuntimeException("User " + netMan.getFriendManager().getFriend(guid).getNickname() + " has an old (incompatible) version of Alliance. Talk to him about it.");
            }

            //successfully connected to friend
            if (friend.isNewlyDiscoveredFriend()) {
                core.queNeedsUserInteraction(new NewFriendConnectedUserInteraction(friend.getGuid()));
                friend.setNewlyDiscoveredFriend(false);
            }

            AuthenticatedConnection c = AuthenticatedConnection.newInstance(netMan, key, connectionType, guid);
            if (T.t) {
                T.info("Replacing handshake connection with " + c);
            }
            netMan.replaceConnection(key, c); //closed this handshake connection from netMan and add the new connection instance instead
            c.init();
        }
    }

    @Override
    protected int getConnectionId() {
        return 0;
    }
}
