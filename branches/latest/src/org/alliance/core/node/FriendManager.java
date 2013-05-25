package org.alliance.core.node;

import org.alliance.core.BroadcastManager;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.Manager;
import org.alliance.core.T;
import org.alliance.core.interactions.PleaseForwardInvitationInteraction;
import org.alliance.core.settings.My;
import org.alliance.core.settings.Settings;
import org.alliance.core.comm.AuthenticatedConnection;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.FriendConnection;
import org.alliance.core.comm.InvitationConnection;
import org.alliance.core.comm.NetworkManager;
import org.alliance.core.comm.rpc.ForwardedInvitation;
import org.alliance.core.comm.rpc.GetUserList;
import org.alliance.core.comm.rpc.GracefulClose;
import org.alliance.core.comm.rpc.Ping;
import org.alliance.core.comm.rpc.PleaseForwardInvitation;
import org.alliance.core.comm.rpc.UserInfo;
import org.alliance.core.comm.rpc.UserInfoV2;
import org.alliance.core.comm.rpc.UserList;
import org.alliance.ui.addfriendwizard.ForwardInvitationNodesList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * The FriendManager keeps track of all nodes. Contains a list of friends and a list of all nodes
 * (Friend extends Node so friends are nodes too).
 * <p>
 * Launches the FriendConnector that tries to connect to disconnected friends reguraly (using a separate thread).
 * <p>
 * Use the FriendManager to manage information about nodes.
 * <p>
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 14:30:09
 */
public class FriendManager extends Manager {

    private Settings settings;
    private FriendConnector friendConnector;
    private BroadcastManager broadcastManager = new BroadcastManager();
    private HashMap<Integer, Friend> friends = new HashMap<Integer, Friend>();
    private HashMap<Integer, UntrustedNode> untrustedNodes = new HashMap<Integer, UntrustedNode>();
    private CoreSubsystem core;
    private MyNode me;
    private NetworkManager netMan;

    public FriendManager(CoreSubsystem core, Settings settings) throws Exception {
        this.settings = settings;
        this.core = core;
    }

    @Override
    public void init() throws Exception {
        netMan = core.getNetworkManager();
        setupGUID();
        setupFriends();
        setupFriendConnector();
    }

    private void setupFriendConnector() {
        friendConnector = new FriendConnector(this);
        friendConnector.start();
    }

    public int getMyGUID() {
        return settings.getMy().getGuid();
    }

    private void setupGUID() throws Exception {
        if (settings.getMy() == null || settings.getMy().getGuid() == null) {
            if (T.t) {
                T.info("Generating GUID for user.");
            }
            if (settings.getMy() == null) {
                settings.setMy(new My());
            }
            Random r = new Random(8682522807148012L + System.nanoTime());
            do {
                settings.getMy().setGuid(r.nextInt());
            } while (settings.getMy().getGuid() != 0); //guid may not be 0.
            core.saveSettings();
        } else {
            if (T.t) {
                T.info("User: " + settings.getMy().getNickname() + " - " + settings.getMy().getGuid());
            }
        }
    }

    private void setupFriends() throws Exception {
        if (T.t) {
            T.info("Setting up friends...");
        }
        me = new MyNode(settings.getMy().getNickname(), settings.getMy().getGuid(), core);

        for (Iterator<org.alliance.core.settings.Friend> it = settings.getFriendlist().iterator(); it.hasNext();) {
            org.alliance.core.settings.Friend f = it.next();
            if (f.getLastseenonlineat() != null && f.getLastseenonlineat() > 0 && System.currentTimeMillis() - f.getLastseenonlineat() > 1000L * 60 * 60 * 24 * 200) {
                if (T.t) {
                    T.info("Friend has not been online for 100 days. Remove him");
                }
                it.remove();
            }
        }
        for (org.alliance.core.settings.Friend f : settings.getFriendlist()) {
            addFriend(f, false, false);
        }
    }

    public Friend addFriend(org.alliance.core.settings.Friend f, boolean foundFriendUsingInvitation, boolean invitationWasForwarded) throws Exception {
        if (f.getGuid() == me.getGuid()) {
            if (T.t) {
                T.warn("You have yourself in your friendlist!");
            }
            return null;
        } else if (f.getNickname() == null) {
            throw new Exception("No nickname for guid: " + f.getGuid());
        } else {
            if (T.t) {
                T.info("Found " + f.getNickname() + ". GUID: " + f.getGuid() + " introducted by: " + f.getMiddlemanguid());
            }
            if (!invitationWasForwarded && foundFriendUsingInvitation) {
                if (T.t) {
                    T.info("Succesfully connected to a new friend - this was not a forwarded invitation.");
                }
                if (T.t) {
                    T.info("Award user with one invitation point");
                }
                core.incInvitationPoints();
            }
            Friend friend = new Friend(this, f);
            if (friend.getGuid() == getMyGUID()) {
                throw new Exception("You have configured a friend that has your own GUID.");
            }
            friends.put(f.getGuid(), friend);
            friend.setNewlyDiscoveredFriend(foundFriendUsingInvitation);
            core.getUICallback().signalFriendAdded(friend);
            if (foundFriendUsingInvitation) {
                sendMyInfoToAllMyFriends();
            }
            return friend;
        }
    }

    public org.alliance.core.settings.Friend getConfigurationItemFor(Friend friend) {
        return settings.getFriend(friend.getGuid());
    }

    private void sendMyInfoToAllMyFriends() throws IOException {
        netMan.sendToAllFriends(new UserInfo());
        netMan.sendToAllFriends(new UserInfoV2());
        netMan.sendToAllFriends(new UserList());
    }

    /**
     * Callback from netMan
     */
    public void connectionEstablished(AuthenticatedConnection c) throws IOException {
        if (getFriend(c.getRemoteUserGUID()) != null) {
            getFriend(c.getRemoteUserGUID()).addConnection(c);
            getNetMan().getPackageRouter().updateRouteTable(getFriend(c.getRemoteUserGUID()), c.getRemoteUserGUID(), 0);
            /*Code below automatically connects to all friends of friends, however it does not work on startup due to objects
            not being sufficiantly initialized, which is ok because the next time any user connects it runs successfully*/
            if (settings.getInternal().getAlwaysautomaticallyconnecttoallfriendsoffriend() == 1) {
                connectToAllFriendsOfFriends();
            }

        } else {
            if (T.t) {
                T.ass(c instanceof InvitationConnection, "Not an invitation connection!");
            }
        }
    }

    //TODO separate core/ui
    public void connectToAllFriendsOfFriends() {
        core.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    final ArrayList<ForwardInvitationNodesList.ListRow> al = new ArrayList<ForwardInvitationNodesList.ListRow>();
                    ForwardInvitationNodesList.ForwardInvitationListModel m = new ForwardInvitationNodesList.ForwardInvitationListModel(core);
                    for (int j = 0; j < m.getSize(); j++) {
                        al.add((ForwardInvitationNodesList.ListRow) m.getElementAt(j));
                    }
                    for (ForwardInvitationNodesList.ListRow r : al) {
                        forwardInvitationTo(r.guid);
                    }
                } catch (Exception e) {
                    core.reportError(e, this);
                }
            }
        });
    }

    public Friend getFriend(int guid) {
        return friends.get(guid);
    }

    public Friend getFriend(String nickname) {
        for (Friend f : friends.values()) {
            if (f.getNickname().equals(nickname)) {
                return f;
            }
        }
        return null;
    }

    /**
     * Callback from netMan
     */
    public void connectionClosed(Connection connection) {
        if (connection instanceof AuthenticatedConnection) {
            AuthenticatedConnection c = (AuthenticatedConnection) connection;
            if (getFriend(c.getRemoteUserGUID()) != null) {
                getFriend(c.getRemoteUserGUID()).removeConnection(c);
                core.getUICallback().nodeOrSubnodesUpdated(getFriend(c.getRemoteUserGUID()));
            }
        }
    }

    public void ping() throws IOException {
        netMan.sendToAllFriends(new Ping());
    }

    public NetworkManager getNetMan() {
        return netMan;
    }

    public Collection<Friend> friends() {
        return friends.values();
    }

    public Settings getSettings() {
        return settings;
    }

    public void connect(Friend f) throws IOException {
        if (f.isConnected()) {
            if (T.t) {
                T.warn("Already connected!");
            }
            return;
        }
        netMan.connect(f.getLastKnownHost(), f.getLastKnownPort(), new FriendConnection(netMan, Connection.Direction.OUT, f.getGuid()));
    }

    public UntrustedNode getUntrustedNode(int guid) {
        return untrustedNodes.get(guid);
    }

    public void addUntrustedNode(UntrustedNode n) {
        untrustedNodes.put(n.getGuid(), n);
    }

    public Node getNode(int guid) {
        Node n = friends.get(guid);
        if (n == null) {
            n = untrustedNodes.get(guid);
        }
        if (n == null && guid == me.getGuid()) {
            n = me;
        }
        return n;
    }

    public void loadSubnodesFor(Node node) throws IOException {
        netMan.route(node.getGuid(), new GetUserList());
    }

    public MyNode getMe() {
        return me;
    }

    public CoreSubsystem getCore() {
        return core;
    }

    public BroadcastManager getBroadcastManager() {
        return broadcastManager;
    }

    public String nicknameWithContactPath(int guid) {
        String s = nickname(guid);
        String s2 = contactPath(guid);
        if (s2.length() > 0) {
            s += " (" + s2 + ")";
        }
        return s;
    }

    public String contactPath(int guid) {
        String s = "";
        int count = 0;

        Friend fr = getFriend(guid);
        if (fr != null && fr.getMiddlemanGuid() != 0) {
            s = "via ";
            Collection<Friend> allfriends = friends();
            for (Friend f : allfriends.toArray(new Friend[friends().size()])) {
                if (f.getFriendsFriend(guid) != null) {
                    if (count < 3) {
                        if (s.length() > 4) {
                            s += ", ";
                        }
                        s += f.getNickname();
                    }
                    count++;
                }
            }
            if (count > 3) {
                s += " and " + (count - 3) + " more friends";
            }
        }
        return s;
    }

    public void shutdown() throws IOException {
        netMan.sendToAllFriends(new GracefulClose(GracefulClose.SHUTDOWN));
    }

    public long getTotalBytesShared() {
        long n = 0;
        for (Friend f : friends.values()) {
            if (f.isConnected()) {
                n += f.getShareSize();
            }
        }
        n += me.getShareSize();
        return n;
    }

    public void forwardInvitation(PleaseForwardInvitationInteraction fi) throws IOException {
        forwardInvitation(fi.getFromGuid(), fi.getToGuid(), fi.getInvitationCode());
    }

    public void forwardInvitation(int fromGuid, int toGuid, String invitationCode) throws IOException {
        Node from = getNode(fromGuid);
        Friend to = getFriend(toGuid);
        if (T.t) {
            T.ass(from != null, "From is null");
        }
        if (T.t) {
            T.ass(to != null, "To is null");
        }
        netMan.sendPersistantly(new ForwardedInvitation(from, invitationCode), to);
    }

    public int getNFriendsConnected() {
        int n = 0;
        Friend fa[] = new Friend[friends.size()];
        fa = friends.values().toArray(fa);
        for (Friend f : fa) {
            if (f.isConnected()) {
                n++;
            }
        }
        return n;
    }

    public FriendConnector getFriendConnector() {
        return friendConnector;
    }
    private ArrayList<Integer> latelyForwardedInvitaionGuids = new ArrayList<Integer>();

    public void forwardInvitationTo(final int guid) throws Exception {
        if (latelyForwardedInvitaionGuids.contains(guid)) {
            if (T.t) {
                T.info("Tried to re-forward a recent invitation. Ignoring this invitation attempt");
            }
            return;
        }
        latelyForwardedInvitaionGuids.add(guid);
        if (latelyForwardedInvitaionGuids.size() > 20) {
            latelyForwardedInvitaionGuids.remove(0);
        }

        if (T.t) {
            T.trace("Forwarding invitaiton to " + nickname(guid));
        }
        Friend route = null;
        for (Friend f : friends.values()) {
            if (f.getFriendsFriend(guid) != null && f.isConnected()) {
                route = f;
                break;
            }
        }

        if (route == null) {
            for (Friend f : friends.values()) {
                if (f.getFriendsFriend(guid) != null) {
                    route = f;
                    break;
                }
            }
        }

        if (route == null) {
            if (T.t) {
                T.error("Could not find friend that is connected to guid " + guid + "!");
            }
            //throw new Exception("Could not find friend that is connected to guid "+guid+"!");
        } else {
            core.getNetworkManager().sendPersistantly(new PleaseForwardInvitation(getNode(guid)), route);
        }
    }

    public void permanentlyRemove(Friend f) {
        try {
            if (f.isConnected()) {
                f.disconnect(GracefulClose.DELETED);
            }
        } catch (IOException e) {
            if (T.t) {
                T.warn("Nonfatal: " + e);
            }
        }
        friends.remove(f.getGuid());
        for (Iterator i = settings.getFriendlist().iterator(); i.hasNext();) {
            if (((org.alliance.core.settings.Friend) i.next()).getGuid() == f.getGuid()) {
                i.remove();
            }
        }
    }

    public int getNFriends() {
        return friends.size();
    }

    public String nicknameWithoutLocalRename(int guid) {
        Node n = getNode(guid);
        if (n == null) {
            return "unknown (" + Integer.toHexString(guid).toUpperCase() + ")";
        }
        return n.getNickname();
    }

    public String nickname(int guid) {
        if (getFriend(guid) != null) {
            return getFriend(guid).getNickname();
        }
        return nicknameWithoutLocalRename(guid);
    }

    public int getNumberOfInvitesNeededToBeKing() {
        int n = 0;

        Friend fa[] = new Friend[friends.size()];
        fa = friends.values().toArray(fa);
        for (Friend f : fa) {
            if (f.getNumberOfInvitedFriends() > n) {
                n = f.getNumberOfInvitedFriends();
            }
        }
        if (me.getNumberOfInvitedFriends() > n) {
            n = me.getNumberOfInvitedFriends();
        }
        if (n <= 3) {
            return 4; //three invities is "experienced" - if noone has more then 4 are needed to become king
        }
        return n;
    }
}
