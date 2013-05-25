package org.alliance.core.comm;

import org.alliance.Version;
import org.alliance.core.interactions.FriendAlreadyInListUserInteraction;
import org.alliance.core.node.Friend;
import org.alliance.core.node.MyNode;
import org.alliance.core.settings.Server;

import java.io.IOException;

/**
 * This connection swings both ways - it's used by invitor and invited
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-16
 * Time: 19:47:04
 * To change this template use File | Settings | File Templates.
 */
public class InvitationConnection extends AuthenticatedConnection {

    public static final int CONNECTION_ID = 4;
    private int passkey;
    private Friend middleman;
    private Runnable connectionFailedEvent;

    public InvitationConnection(NetworkManager netMan, Direction direction, int passkey, Friend middleman) {
        super(netMan, direction);
        this.passkey = passkey;
        this.middleman = middleman;
    }

    public InvitationConnection(NetworkManager netMan, Direction direction, Object key, int passkey, Integer middlemanGuid) {
        super(netMan, direction, key);
        this.passkey = passkey;
        if (middlemanGuid != null && middlemanGuid != 0) {
            middleman = core.getFriendManager().getFriend(middlemanGuid);
            if (middleman == null) {
                if (T.t) {
                    T.error("Could not find middleman: " + middlemanGuid);
                }
            }
        }

        if (T.t) {
            T.ass(direction == Direction.IN, "Only supports incoming connections!");
        }
        sendMyInfoWrapped();
    }

    @Override
    public void sendConnectionIdentifier() throws IOException {
        if (T.t) {
            T.trace("InvitationConnection succeded - remove connection error event");
        }
        connectionFailedEvent = null;

        if (T.t) {
            T.trace("Sending special authentication for InvitationConnection");
        }
        Packet p = netMan.createPacketForSend();
        p.writeInt(Version.PROTOCOL_VERSION);
        p.writeByte((byte) getConnectionIdForRemote());
        p.writeInt(passkey);
        send(p);

        if (T.t) {
            T.ass(direction == Direction.OUT, "Only supports outgoing connections!");
        }
        sendMyInfoWrapped();
    }

    private void sendMyInfoWrapped() {
        try {
            sendMyInfo();
        } catch (IOException e) {
            core.reportError(e, this);
        }
    }

    private void sendMyInfo() throws IOException {
        if (T.t) {
            T.info("Sending my info because remote had correct invitation passkey");
        }
        Packet p = netMan.createPacketForSend();
        p.writeInt(core.getFriendManager().getMyGUID());

        Server server = core.getSettings().getServer();
        MyNode me = core.getFriendManager().getMe();
        p.writeInt(server.getPort());
        p.writeUTF(me.getNickname());
        p.writeUTF(server.getDnsname());

        send(p);
    }
    private boolean friendInfoReceived = false;

    @Override
    public void packetReceived(Packet p) throws IOException {
        if (friendInfoReceived) {
            if (T.t) {
                T.info("InvitationConnection complete - both side has agreed on closing connection - so lets close it");
            }
            close();
        } else {
            if (T.t) {
                T.info("Received info of new friend!");
            }
            friendInfoReceived = true;
            int guid = p.readInt();
            int port = p.readInt();
            String name = p.readUTF();
            String dnsName = p.readUTF();
            String host = netMan.getSocketFor(this).getInetAddress().getHostAddress();

            org.alliance.core.settings.Friend newFriend = new org.alliance.core.settings.Friend(name, host, guid, port, middleman == null ? null : middleman.getGuid());
            for (org.alliance.core.settings.Friend f : core.getSettings().getFriendlist()) {
                if (f.getGuid() == guid) {
                    //friend already my friend, update ip number
                    org.alliance.core.node.Friend friend = core.getFriendManager().getFriend(f.getGuid());
                    if (friend != null) {
                        if (friend.updateLastKnownHostInfo(host, port, dnsName)) {
                            if (!friend.isConnected()) {
                                core.getFriendManager().getFriendConnector().queHighPriorityConnectTo(friend);
                            } else {
                                friend.reconnect();
                            }
                        }
                    }
                    core.queNeedsUserInteraction(new FriendAlreadyInListUserInteraction(newFriend.getGuid()));
                    send(netMan.createPacketForSend()); //send an empty packet to trigger packetReceived on other end again.
                    return;
                }
            }
            //new friend connected!
            core.getSettings().getFriendlist().add(newFriend);
            try {
                core.saveSettings();
                Friend f = core.getFriendManager().addFriend(newFriend, true, middleman != null);
                core.getFriendManager().getFriendConnector().queHighPriorityConnectTo(f, (int) (Math.random() * 1000 + 1000));
            } catch (Exception e) {
                core.reportError(e, this);
            }
            send(netMan.createPacketForSend()); //send an empty packet to trigger packetReceived on other end again.
        }
    }

    @Override
    protected int getConnectionId() {
        return CONNECTION_ID;
    }

    @Override
    public void signalConnectionAttemtError() {
        super.signalConnectionAttemtError();
        if (connectionFailedEvent != null) {
            connectionFailedEvent.run();
            connectionFailedEvent = null;
        }
    }

    public void setConnectionFailedEvent(Runnable connectionFailedEvent) {
        this.connectionFailedEvent = connectionFailedEvent;
    }
}
