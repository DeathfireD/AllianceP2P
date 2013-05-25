package org.alliance.core.node;

import com.stendahls.util.TextUtils;
import org.alliance.core.T;
import org.alliance.core.comm.AuthenticatedConnection;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.FriendConnection;
import org.alliance.core.comm.rpc.GracefulClose;
import org.alliance.core.settings.Settings;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 14:30:21
 */
public class Friend extends Node {

    private ArrayList<Connection> connections = new ArrayList<Connection>();
    private String lastKnownHost;
    private int lastKnownPort;
    private FriendManager manager;
    private FriendConnection friendConnection;
    private boolean newlyDiscoveredFriend; //true when friend was recently found using invitation
    private long lastSeenOnlineAt;
    private int middlemanGuid;
    private int allianceBuildNumber;
    private long totalBytesSent, totalBytesReceived, shareSize;
    private double highestIncomingCPS, highestOutgoingCPS;
    private int numberOfFilesShared, numberOfInvitedFriends;
    private boolean isAway;
    private String nicknameToShowInUI;
    private String ugroupname;  //Network name for Friend
    private int trusted;

    public Friend(FriendManager manager, org.alliance.core.settings.Friend f) {
        this.manager = manager;
        nickname = f.getNickname();
        guid = f.getGuid();
        lastKnownHost = f.getHost();
        lastKnownPort = f.getPort();
        lastSeenOnlineAt = f.getLastseenonlineat() == null ? 0 : f.getLastseenonlineat();
        middlemanGuid = f.getMiddlemanguid() == null ? 0 : f.getMiddlemanguid();
        ugroupname = f.getUgroupname(); //Bastvera (Load net name for Friend from settings.xml)
        trusted = f.getTrusted();
    }

    public Friend(FriendManager manager, String nickname, int guid) {
        this.nickname = nickname;
        this.guid = guid;
        this.manager = manager;
    }

    public FriendConnection getFriendConnection() {
        return friendConnection;
    }

    public void addConnection(AuthenticatedConnection c) throws IOException {
        connections.add(c);
        if (c instanceof FriendConnection) {
            friendConnection = (FriendConnection) c;
            manager.getCore().getNetworkManager().getDownloadManager().signalFriendWentOnline(this);
        }
    }

    /**
     *
     * @param host
     * @param port
     * @return True if the host info did actually change
     * @throws IOException
     */
    public boolean updateLastKnownHostInfo(String host, int port) throws IOException {
        Settings s = manager.getSettings();

        if (TextUtils.isIpNumber(host) && TextUtils.isIpNumber(s.getFriend(guid).getHost())) {
            if (T.t) {
                T.info("Updating host info for " + this + ": " + host + ":" + port);
            }
            boolean hostInfoChanged = lastKnownHost == null || !lastKnownHost.equals(host) || lastKnownPort != port;
            lastKnownHost = rDNSConvert(host, s.getFriend(guid));
            lastKnownPort = port;
            s.getFriend(guid).setHost(lastKnownHost);
            s.getFriend(guid).setPort(lastKnownPort);
            return hostInfoChanged;
        } else if (!TextUtils.isIpNumber(host) && !s.getFriend(guid).getHost().equals(host)) {
            if (T.t) {
                T.info("Updating host info for " + this + ": " + host + ":" + port);
            }
            boolean hostInfoChanged = lastKnownHost == null || !lastKnownHost.equals(host) || lastKnownPort != port;
            lastKnownHost = host;
            lastKnownPort = port;
            s.getFriend(guid).setHost(lastKnownHost);
            s.getFriend(guid).setPort(lastKnownPort);
            return hostInfoChanged;
        } else {
            return false;
        }
    }

    public String rDNSConvert(String host, org.alliance.core.settings.Friend f) {
        if (manager.getSettings().getInternal().getRdnsname() > 0) {
            if (isValidIP(host)) {
                //Convert IP to DNS
                try {
                    InetAddress addr = InetAddress.getByAddress(getByteArray(host));
                    host = addr.getHostName();
                    f.setHost(host);
                    return host;
                } catch (UnknownHostException e) {
                    return host;
                }
            } else {
                return host;
            }
        } else {
            if (!isValidIP(host)) {
                //Convert DNS to IP
                try {
                    InetAddress addr = InetAddress.getByName(host);
                    host = addr.getHostAddress();
                    f.setHost(host);
                    return host;

                } catch (UnknownHostException e) {
                    return host;
                }
            } else {
                return host;
            }
        }
    }

    /**
     * Updates host settings of the friend
     * @param host New hostname or IP-Address
     */
    public void setLastKnownHost(String host) {
        lastKnownHost = host;
        manager.getSettings().getFriend(guid).setHost(lastKnownHost);
    }

    public void setUGroupName(String ugroupname) { //Bastvera (Network name setting for friend
        this.ugroupname = ugroupname;
        manager.getSettings().getFriend(guid).setUgroupname(ugroupname);
    }

    public String getUGroupName() { //Bastvera (Used by AuthenticatedConnection.java)
        return ugroupname;
    }

    public void setTrusted(int trusted) {
        this.trusted = trusted;
        manager.getSettings().getFriend(guid).setTrusted(trusted);
    }

    public int getTrusted() {
        return trusted;
    }

    @Override
    public boolean isConnected() {
        return friendConnection != null;
    }

    public void removeConnection(AuthenticatedConnection ac) {
        connections.remove(ac);
        if (ac == friendConnection) {
            friendConnection = null;
            for (Connection c : connections) {
                if (c instanceof FriendConnection) {
                    friendConnection = (FriendConnection) c;
                }
            }
            if (friendConnection == null) {
                if (T.t) {
                    T.info("Lost connection to " + this + ". Closing all other connections too. Have to do this in order to be able to start a new donwload connnection to this friend (if he reconnects)");
                }
                ArrayList<Connection> al = new ArrayList<Connection>(connections);
                for (Connection c : al) {
                    try {
                        if (T.t) {
                            T.info("Closing: " + c);
                        }
                        c.close();
                    } catch (IOException e) {
                        manager.getCore().reportError(e, this);
                    }
                }
            }
        }
    }

    public String getLastKnownHost() {
        return lastKnownHost;
    }

    public int getLastKnownPort() {
        return lastKnownPort;
    }

    public boolean hasMultipleFriendConnections() {
        int n = 0;
        for (Connection c : connections) {
            if (c instanceof FriendConnection) {
                n++;
            }
        }
        return n > 1;
    }

    public boolean isNewlyDiscoveredFriend() {
        return newlyDiscoveredFriend;
    }

    public void setNewlyDiscoveredFriend(boolean newlyDiscoveredFriend) {
        this.newlyDiscoveredFriend = newlyDiscoveredFriend;
    }

    public void disconnect(byte reason) throws IOException {
        if (friendConnection != null) {
            friendConnection.send(new GracefulClose(reason));
        }
    }

    @Override
    public long getLastSeenOnlineAt() {
        return lastSeenOnlineAt;
    }

    @Override
    public boolean hasNotBeenOnlineForLongTime() {
        return System.currentTimeMillis() - lastSeenOnlineAt >
                manager.getCore().getSettings().getInternal().getDaysnotconnectedwhenold() * 24 * 60 * 60 * 1000;
    }

    public int getMiddlemanGuid() {
        return middlemanGuid;
    }

    @Override
    public int getAllianceBuildNumber() {
        return allianceBuildNumber;
    }

    public void setAllianceBuildNumber(int allianceBuildNumber) {
        this.allianceBuildNumber = allianceBuildNumber;
    }

    public void reconnect() throws IOException {
        disconnect(GracefulClose.RECONNECT);
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e1) {
                }
                manager.getCore().invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        if (isConnected()) {
                            try {
                                getFriendConnection().close();
                            } catch (IOException e1) {
                                if (org.alliance.ui.T.t) {
                                    org.alliance.ui.T.warn("Error when closing connection: " + e1);
                                }
                            }
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e1) {
                        }
                        manager.getCore().getFriendManager().getFriendConnector().queHighPriorityConnectTo(Friend.this);
                    }
                });
            }
        });
        t.start();
    }

    public void connect() throws IOException {
        manager.getCore().getFriendManager().getFriendConnector().queHighPriorityConnectTo(this, 500);
    }

    @Override
    public String getNickname() {
        if (nicknameToShowInUI == null) {
            org.alliance.core.settings.Friend f = manager.getCore().getSettings().getFriend(guid);
            if (f != null && f.getRenamednickname() != null && f.getRenamednickname().trim().length() > 0) {
                nicknameToShowInUI = f.getRenamednickname();
            } else {
                nicknameToShowInUI = nickname;
            }
        }
        return nicknameToShowInUI;
    }

    public void setNicknameToShowInUI(String nn) {
        org.alliance.core.settings.Friend f = manager.getCore().getSettings().getFriend(guid);
        if (f != null) {
            f.setRenamednickname(nn);
            nicknameToShowInUI = nn;
        }
    }

    @Override
    public int getNumberOfFilesShared() {
        return numberOfFilesShared;
    }

    @Override
    public long getShareSize() {
        return shareSize;
    }

    @Override
    public double getHighestOutgoingCPS() {
        return highestOutgoingCPS;
    }

    @Override
    public double getHighestIncomingCPS() {
        return highestIncomingCPS;
    }

    @Override
    public long getTotalBytesReceived() {
        return totalBytesReceived;
    }

    @Override
    public long getTotalBytesSent() {
        return totalBytesSent;
    }

    public void setShareSize(long shareSize) {
        this.shareSize = shareSize;
    }

    public void setTotalBytesSent(long totalBytesSent) {
        this.totalBytesSent = totalBytesSent;
    }

    public void setTotalBytesReceived(long totalBytesReceived) {
        this.totalBytesReceived = totalBytesReceived;
    }

    public void setHighestIncomingCPS(double highestIncomingCPS) {
        this.highestIncomingCPS = highestIncomingCPS;
    }

    public void setHighestOutgoingCPS(double highestOutgoingCPS) {
        this.highestOutgoingCPS = highestOutgoingCPS;
    }

    public void setNumberOfFilesShared(int numberOfFilesShared) {
        this.numberOfFilesShared = numberOfFilesShared;
    }

    @Override
    public int getNumberOfInvitedFriends() {
        return numberOfInvitedFriends;
    }

    public void setNumberOfInvitedFriends(int numberOfInvitedFriends) {
        this.numberOfInvitedFriends = numberOfInvitedFriends;
    }

    @Override
    public boolean isAway() {
        return isAway;
    }

    public void setAway(boolean away) {
        isAway = away;
    }

    //Kratos
    private boolean isValidIP(String host) {
        Integer temp;
        if (host.length() > 15) {
            return false;
        }
        String tempString = host;
        for (int i = 0; i < 3; i++) {
            int divider = tempString.indexOf('.');
            if (divider == -1) {
                return false;
            }
            try {
                temp = new Integer(tempString.substring(0, divider));
            } catch (NumberFormatException e) {
                return false;
            }
            if (temp > 255 || temp < 0) {
                return false;
            }
            tempString = tempString.substring(divider + 1);
        }
        return true;
    }

    //Will convert IP into byte array, note there is no checking, and it is expected
    //you've checked it already
    private byte[] getByteArray(String lasthost) {
        byte[] array = new byte[4];
        Integer temp;
        String tempString = lasthost;
        for (int i = 0; i < 3; i++) {
            int divider = tempString.indexOf('.');
            try {
                temp = new Integer(tempString.substring(0, divider));
            } catch (NumberFormatException e) {
                //This means we have a dns address, you didn't check did you?
                return null;
            }
            array[i] = temp.byteValue();
            tempString = tempString.substring(divider + 1);
        }
        array[3] = new Integer(tempString).byteValue();
        return array;
    }
}
