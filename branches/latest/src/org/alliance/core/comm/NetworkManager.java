package org.alliance.core.comm;

import org.alliance.core.CoreSubsystem;
import static org.alliance.core.CoreSubsystem.KB;
import static org.alliance.core.CoreSubsystem.MB;
import org.alliance.core.Manager;
import org.alliance.core.comm.filetransfers.DownloadManager;
import org.alliance.core.comm.networklayers.tcpnio.NIOPacket;
import org.alliance.core.comm.networklayers.tcpnio.TCPNIONetworkLayer;
import org.alliance.core.comm.rpc.ConnectToMe;
import org.alliance.core.comm.rpc.PersistantRPC;
import org.alliance.core.comm.rpc.Ping;
import org.alliance.core.comm.rpc.Search;
import org.alliance.core.comm.throttling.BandwidthThrottle;
import org.alliance.core.crypto.CryptoLayer;
import org.alliance.core.file.filedatabase.FileType;
import org.alliance.core.node.Friend;
import org.alliance.core.node.FriendManager;
import org.alliance.core.settings.Server;
import org.alliance.core.settings.Settings;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-26
 * Time: 11:38:32
 * To change this template use File | Settings | File Templates.
 */
public class NetworkManager extends Manager {
    //public static final boolean DIRECTLY_CALL_READYTOSEND = false;
    //Setting false here produces weird results - not sure anymore now that encryption is implemented - anyway.
    //It seems to be working well with this flag on.

    public static final boolean DIRECTLY_CALL_READYTOSEND = true;
    private int serverPort;
    private boolean alive = true;
    private TCPNIONetworkLayer networkLayer;
    private CryptoLayer cryptoLayer;
    private FriendManager friendManager;
    private CoreSubsystem core;
    private DownloadManager downloadManager;
    private BandwidthThrottle uploadThrottle;
    private HashMap<Object, Connection> connections = new HashMap<Object, Connection>();
    private Router router;
    private HashSet<InetAddress> bannedHosts = new HashSet<InetAddress>();
    private long lastClearOfBannedHostsTick = System.currentTimeMillis();
    protected BandwidthAnalyzer bandwidthIn, bandwidthOut, bandwidthInHighRefresh, bandwidthOutHighRefresh;
    private ArrayList<PersistantRPC> queuedPersistantRPCs = new ArrayList<PersistantRPC>();
    private HashMap<Integer, AuthenticatedConnection> connectionsWaitingForReverseConnect = new HashMap<Integer, AuthenticatedConnection>();

    public NetworkManager(CoreSubsystem core, Settings settings) throws IOException {
        this.core = core;
        this.friendManager = core.getFriendManager();
        uploadThrottle = new BandwidthThrottle(core, settings.getInternal().getUploadthrottle() * KB);
        Integer p = settings.getServer().getPort();
        if (p == null) {
            p = new Integer(Server.createRandomPort());
            if (T.t) {
                T.info("Generated random port for new installation: " + p);
            }
            settings.getServer().setPort(p);
            try {
                core.saveSettings();
            } catch (Exception e) {
                if (T.t) {
                    T.error("Could not save settings: " + e);
                }
            }
        }
        this.serverPort = p;

        networkLayer = new TCPNIONetworkLayer(this);
        cryptoLayer = core.getCryptoManager().getCryptoLayer();
        downloadManager = new DownloadManager(friendManager.getCore());
        router = new Router(friendManager);
        bandwidthIn = new BandwidthAnalyzer(BandwidthAnalyzer.OUTER_INTERVAL, settings.getInternal().getRecordinspeed(), ((long) settings.getInternal().getTotalmegabytesdownloaded() * MB));
        bandwidthOut = new BandwidthAnalyzer(BandwidthAnalyzer.OUTER_INTERVAL, settings.getInternal().getRecordoutspeed(), ((long) settings.getInternal().getTotalmegabytesuploaded() * MB));
        bandwidthInHighRefresh = new BandwidthAnalyzer(1500);
        bandwidthOutHighRefresh = new BandwidthAnalyzer(1500);

        // keep-alive thread
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                while (alive) {
                    final int ms = NetworkManager.this.core.getSettings().getInternal().getConnectionkeepaliveinterval() * 1000;
                    try {
                        Thread.sleep(ms);
                    } catch (InterruptedException e) {
                    }
                    invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                for (Connection c : connections.values()) {
                                    if (c instanceof FriendConnection) {
                                        FriendConnection fc = (FriendConnection) c;
                                        if (fc.getNetworkLatency() > 15 * 1000) {
                                            if (T.t) {
                                                T.error(fc.getRemoteFriend().getNickname() + " has a very high network latency - this is probably a bug. Reconnecting to friend.");
                                            }
                                            fc.getRemoteFriend().reconnect();
                                        } else {
                                            if (System.currentTimeMillis() - fc.getLastPacketSentAt() > ms) {
                                                fc.send(new Ping());
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                if (T.t) {
                                    T.error("Error in keep-alive loop: " + e);
                                }
                            }
                        }
                    });
                }
            }
        });
        t.setName("Network connection keep-alive");
        t.start();
    }

    @Override
    public void init() throws IOException {
        downloadManager.init();
        networkLayer.start();
    }

    public void shutdown() throws IOException {
        core.getSettings().getInternal().setRecordinspeed((int) bandwidthIn.getHighestCPS());
        core.getSettings().getInternal().setRecordoutspeed((int) bandwidthOut.getHighestCPS());
        core.getSettings().getInternal().setTotalmegabytesdownloaded((int) (bandwidthIn.getTotalBytes() / MB));
        core.getSettings().getInternal().setTotalmegabytesuploaded((int) (bandwidthOut.getTotalBytes() / MB));
        alive = false;
        networkLayer.shutdown();
        downloadManager.shutdown();
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }

    public void bytesReceived(int n) {
        bandwidthIn.update(n);
        bandwidthInHighRefresh.update(n);
    }

    public void bytesSent(int sent) {
        bandwidthOut.update(sent);
        bandwidthOutHighRefresh.update(sent);
    }

    public void reportError(String source, Object key, Exception e) {
        if (!(e instanceof ConnectException) && !(e instanceof NullPointerException)) {
            core.reportError(e, source + ": " + connections.get(key));
        }
        if (connections.containsKey(key)) {
            Connection c = connections.get(key);
            if (c != null) {
                try {
                    c.close();
                } catch (IOException e1) {
                    core.reportError(e1, c);
                }
            }
        }

        if (!(e instanceof IOException)) {
            if (e.toString().indexOf("Connection refused: no further information") == -1) {
                if (T.t) {
                    T.warn("Error for " + source + ": " + e, e);
                }
                if (!(e instanceof NullPointerException)) {
                    System.err.println("Error for " + friendManager.getMe() + ": ");
                    e.printStackTrace();
                }
            }
        }
    }

    public void closed(Connection c) {
        friendManager.connectionClosed(c);
        connections.remove(c.getKey());
        networkLayer.close(c.getKey());
        cryptoLayer.closed(c);
    }

    public Connection replaceConnection(Object key, Connection connection) {
        if (!connections.containsKey(key)) {
            if (T.t) {
                T.warn("Could not find connection!");
            }
        }
        if (T.t) {
            T.trace("Connection before: " + connections.get(key));
        }
        connections.remove(key);
        addConnection(key, connection);
        if (T.t) {
            T.trace("Connection after: " + connections.get(key));
        }
        return connection;
    }

    public Packet createPacketForSend() {
        return networkLayer.createPacketForSend();
    }

    public Packet createPacketForReceive(int size) {
        return networkLayer.createPacketForReceive(size);
    }

    /**
     * Opens a connection of type <code>connection</code> to dstGuid. If there is a FriendConnection to dstGuid already
     * and that connection is incoming then a reverse connect operation is performed. This way we circumvent a lot of
     * firewall problems.
     * @param dstGuid
     * @param connection
     * @throws IOException
     */
    public void virtualConnect(int dstGuid, AuthenticatedConnection connection) throws IOException {
        Friend f = router.findClosestFriend(dstGuid);

        if (f.getFriendConnection() != null && f.getFriendConnection().getDirection() == Connection.Direction.IN && core.getSettings().getInternal().getEncryption() == 0) {
//        if (f.getFriendConnection() != null && true) {
            if (T.t) {
                T.info("Attemting reverse connect to circument firewall");
            }
            f.getFriendConnection().send(new ConnectToMe(registerForReverseConnect(connection)));
        } else {
            networkLayer.connect(f.getLastKnownHost(), f.getLastKnownPort(), connection);
        }
    }

    public void connect(int dstGuid, AuthenticatedConnection connection) throws IOException {
        Friend f = router.findClosestFriend(dstGuid);
        networkLayer.connect(f.getLastKnownHost(), f.getLastKnownPort(), connection);
    }

    private int registerForReverseConnect(AuthenticatedConnection connection) {
        if (T.t) {
            T.debug("Registering " + connection + " for reverse connect.");
        }
        int id = connection.hashCode();
        connectionsWaitingForReverseConnect.put(id, connection);
        return id;
    }

    public AuthenticatedConnection fetchReveresedConnection(int reverseConnectionId) {
        if (connectionsWaitingForReverseConnect.containsKey(reverseConnectionId)) {
            AuthenticatedConnection c = connectionsWaitingForReverseConnect.get(reverseConnectionId);
            connectionsWaitingForReverseConnect.remove(reverseConnectionId);
            return c;
        } else {
            return null;
        }
    }

    public void connect(String host, int port, AuthenticatedConnection connection) throws IOException {
        networkLayer.connect(host, port, connection);
    }

    public Collection<Connection> connections() {
        return connections.values();
    }

    public void addConnection(Object key, Connection connection) {
        if (T.t) {
            T.info("Adding new connection: " + connection);
        }
        connections.put(key, connection);
    }

    public boolean contains(Object key) {
        return connections.containsKey(key);
    }

    public Connection getConnection(Object key) {
        return connections.get(key);
    }

    public boolean isAlive() {
        return alive;
    }

    public int getServerPort() {
        return serverPort;
    }

    /* **** methods below are part of interface between Connection classes and the network layer ***** */
    public int send(Connection c, Packet p) throws IOException {
        if (!(p instanceof NIOPacket)) {
            throw new IOException("Internal error: unknown type of packet: " + p.getClass().getName());
        }
        return cryptoLayer.send(c, ((NIOPacket) p).getBuffer());
    }

    public int send(Connection c, ByteBuffer buf, int bytesToSend) throws IOException {
        int orig = buf.limit();
        int newPos = buf.position() + bytesToSend;
        if (newPos < buf.limit()) {
            buf.limit(newPos);
        }

        int wrote = cryptoLayer.send(c, buf);

        buf.limit(orig);
        return wrote;
    }

    public void received(Object key, ByteBuffer buffer) throws IOException {
        cryptoLayer.received(getConnection(key), buffer);
    }

    public void readyToSend(Object key) throws IOException {
        cryptoLayer.readyToSend(getConnection(key));
    }

    public void signalInterestToSend(final Connection c) throws IOException {
        cryptoLayer.signalInterestToSend(c);
    }

    public void noInterestToSend(final Connection c) {
        cryptoLayer.noInterestToSend(c);
    }
    /* ************************************************************************************************* */

    public void sendToAllFriends(RPC rpc) throws IOException {
        ArrayList<Connection> al = new ArrayList<Connection>();
        for (Connection c : connections.values()) {
            al.add(c);
        }

        for (Connection c : al) {
            if (c instanceof FriendConnection) {
                if (T.t) {
                    T.trace("Sending rpc " + rpc + " to: " + c);
                }
                ((FriendConnection) c).send(rpc);
            }
        }
    }

    public void broadcast(RPC rpc) throws IOException {
        if (T.t) {
            T.info("Broadcasting rpc: " + rpc + "!");
        }
        short msgId = (short) (Math.random() * 0xffff);
        for (Connection c : connections.values()) {
            if (c instanceof FriendConnection) {
                if (T.t) {
                    T.info("Sending to " + c);
                }
                ((FriendConnection) c).broadcast(msgId, rpc);
            }
        }
    }

    public Socket getSocketFor(Connection connection) {
        return networkLayer.getSocketFor(connection);
    }

    public Router getPackageRouter() {
        return router;
    }

    public void route(int dstGuid, RPC rpc) throws IOException {
        Friend f = router.findClosestFriend(dstGuid);
        if (f == null) {
            throw new IOException("No Route to host: " + dstGuid);
        }
        if (f.getFriendConnection() == null) {
            throw new IOException("No Route to host: " + dstGuid);
        }
        f.getFriendConnection().send(dstGuid, rpc);
    }

    public void sendSearch(String text, FileType ft) throws IOException {
        sendToAllFriends(new Search(text, ft.id()));
//        broadcast(new Search(text));
    }

    public DownloadManager getDownloadManager() {
        return downloadManager;
    }

    public TCPNIONetworkLayer getNetworkLayer() {
        return networkLayer;
    }

    public void invokeLater(Runnable runnable) {
        networkLayer.invokeLater(runnable);
    }

    public BandwidthAnalyzer getBandwidthIn() {
        return bandwidthIn;
    }

    public BandwidthAnalyzer getBandwidthOut() {
        return bandwidthOut;
    }

    public BandwidthAnalyzer getBandwidthInHighRefresh() {
        return bandwidthInHighRefresh;
    }

    public BandwidthAnalyzer getBandwidthOutHighRefresh() {
        return bandwidthOutHighRefresh;
    }

    public CoreSubsystem getCore() {
        return core;
    }

    public int getNConnectionsOfType(Class<? extends Connection> clazz) {
        int n = 0;
        for (Connection c : connections.values()) {
            if (c.getClass() == clazz) {
                n++;
            }
        }
        return n;
    }

    public BandwidthThrottle getUploadThrottle() {
        return uploadThrottle;
    }

    public void blockConnectionsTemporarilyFrom(Connection connection) {
        bannedHosts.add(getSocketFor(connection).getInetAddress());
    }

    public boolean isAddressBlocked(InetAddress a) {
        if (System.currentTimeMillis() - lastClearOfBannedHostsTick > 1000 * 30) {
            bannedHosts.clear();
            lastClearOfBannedHostsTick = System.currentTimeMillis();
        }
        return bannedHosts.contains(a);
    }

    /**
     * Send a PersistantRPC to a friend. Friend might be offline. The RPC is then queued and saved in a persistant
     * queue. Once the friend goes online the RPC will be sent.
     *
     * @param rpc
     * @param destination
     * @throws IOException
     */
    public void sendPersistantly(PersistantRPC rpc, Friend destination) throws IOException {
        if (rpc == null || destination == null) {
            return;
        }
        rpc.setDestinationGuid(destination.getGuid());
        rpc.resetTimestamp();
        if (destination.getFriendConnection() != null) {
            if (T.t) {
                T.trace("Sending persistant RPC immidiatly.");
            }
            destination.getFriendConnection().send(rpc);
        } else {
            if (T.t) {
                T.trace("Queueing persistant RPC: " + rpc + ", destination " + destination);
            }
            rpc.notifyRPCQueuedForLaterSend();
            queuedPersistantRPCs.add(rpc);
        }
    }

    public void save(ObjectOutputStream out) throws IOException {
        out.writeObject(queuedPersistantRPCs);
    }

    public void load(ObjectInputStream in) throws IOException {
        try {
            queuedPersistantRPCs = (ArrayList<PersistantRPC>) in.readObject();
            for (Iterator i = queuedPersistantRPCs.iterator(); i.hasNext();) {
                PersistantRPC r = (PersistantRPC) i.next();
                if (r.hasExpired()) {
                    if (T.t) {
                        T.trace("Removing expired PersistantRPC " + r);
                    }
                    i.remove();
                }
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("wtf: " + e);
        }
    }

    public void signalFriendConnected(Friend friend) {
        if (T.t) {
            T.debug("SignalFriendConnected: " + friend);
        }
        for (Iterator i = queuedPersistantRPCs.iterator(); i.hasNext();) {
            PersistantRPC r = (PersistantRPC) i.next();
            if (r.getDestinationGuid() == friend.getGuid()) {
                try {
                    if (T.t) {
                        T.debug("Found persistant RPC that needs to be sent. Sending. " + r);
                    }
                    friend.getFriendConnection().send(r);
                    i.remove();
                } catch (IOException e) {
                    core.reportError(e, friend.getFriendConnection());
                }
            }
        }
    }
}
