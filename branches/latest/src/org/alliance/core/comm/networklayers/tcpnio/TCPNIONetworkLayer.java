package org.alliance.core.comm.networklayers.tcpnio;

import org.alliance.core.comm.T;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.AuthenticatedConnection;
import org.alliance.core.comm.HandshakeConnection;
import org.alliance.core.comm.NetworkManager;
import org.alliance.core.comm.Packet;
import org.alliance.core.comm.PacketConnection;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * TCP NIO implementation of network handling. To change to other network handling this class can be reimplemented.
 *
 * If threading is to be added to this model it should not run several threads of this class - a thread pool should be
 * used to handle the packets received from this class. Running several selectors is tricky and should be avoided (and
 * might be a can of worms).
 *
 * User: maciek
 * Date: 2005-dec-26
 * Time: 12:08:16
 */
public class TCPNIONetworkLayer implements Runnable {

    private NetworkManager netMan;
    private Selector selector;
    private LinkedBlockingQueue<Runnable> taskQue = new LinkedBlockingQueue<Runnable>(500000);
    private ByteBuffer buffer;
    private byte byteArray[];
    private static int MAXIMUM_PACKET_SIZE;
    private ServerSocket serverSocket;
    private HashSet<Socket> pendingConnectionAttempts = new HashSet<Socket>();

    public TCPNIONetworkLayer(NetworkManager netMan) throws IOException {
        this.netMan = netMan;

        MAXIMUM_PACKET_SIZE = netMan.getCore().getSettings().getInternal().getMaximumAlliancePacketSize();
        buffer = netMan.getCore().allocateBuffer(netMan.getCore().getSettings().getInternal().getSocketreadbuffer());
        byteArray = new byte[buffer.capacity()];

        selector = Selector.open();
        setupServer();
    }

    public void start() {
        Thread t = new Thread(this);
        t.setName("NetworkLayer -- " + netMan.getFriendManager().getCore().getSettings().getMy().getNickname());
        t.start();
    }

    public void shutdown() throws IOException {
        if (serverSocket.isBound()) {
            serverSocket.close();
        }
    }

    public void connect(final String host, final int port, final AuthenticatedConnection connection) throws IOException {
        try {
            if (T.t) {
                T.info("Attempting to connect to " + host + ":" + port + "... (outstanding connections: " +
                        pendingConnectionAttempts.size() + ")");
            }
            if (netMan.getCore().getSettings().getInternal().getEnableiprules() == 1) {
                if (netMan.getCore().getSettings().getRulelist().checkConnection(host)) {
                    //Connect, continue
                } else {
                    netMan.getCore().getUICallback().statusMessage("Connection to " + host + " was blocked becuase of your ip rules");
                    return;
                }
            } else {
                //Connect, continue
            }
            final SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.connect(new InetSocketAddress(host, port));

            int bs = netMan.getCore().getSettings().getInternal().getSoreceivebuf();
            if (bs != -1) {
                sc.socket().setReceiveBufferSize(bs);
            }
            bs = netMan.getCore().getSettings().getInternal().getSosendbuf();
            if (bs != -1) {
                sc.socket().setSendBufferSize(bs);
            }

            pendingConnectionAttempts.add(sc.socket());

            invokeLater(new Runnable() {

                @Override
                public void run() {
                    try {
                        sc.register(selector, SelectionKey.OP_CONNECT, connection);
                    } catch (IOException e) {
                        if (T.t) {
                            T.warn("Could not initialize open connection to " + host + ":" + port + ": " + e);
                        }
                        pendingConnectionAttempts.remove(sc.socket());
                    }
                }
            });
        } catch (UnresolvedAddressException e) {
            // Hope it's ok to just log the error and forget about it? //PM
            if (T.t) {
                T.warn("Unable to resolve address " + host + ":" + port + ": " + e);
            }
        }
    }

    public int send(Object key, ByteBuffer buf, int bytesToSend) throws IOException {
        int orig = buf.limit();
        int newPos = buf.position() + bytesToSend;
        if (newPos < buf.limit()) {
            buf.limit(newPos);
        }
        int wrote = send(key, buf);
        buf.limit(orig);
        return wrote;
    }

    public int send(Object key, ByteBuffer buf) throws IOException {
        SelectionKey k = (SelectionKey) key;
        SocketChannel sc = (SocketChannel) k.channel();
        int wrote = sc.write(buf);
        updateSent(key, wrote);
        return wrote;
    }

    /**
     * Packet must have been prepared for send using "Packet.prepareForSend"!
     */
    public int send(Object key, Packet p) throws IOException {
        if (p instanceof NIOPacket) {
            NIOPacket n = (NIOPacket) p;
            return send(key, n.getBuffer());
        } else {
            if (T.t) {
                T.warn("This has not been tested");
            }
            int len = p.getAvailable();
            p.readArray(byteArray, 0, len);
            return send(key, byteArray, 0, len);
        }
    }

    public int send(Object key, byte buf[]) throws IOException {
        return send(key, buf, 0, buf.length);
    }

    public int send(Object key, byte buf[], int offset, int length) throws IOException {
        buffer.reset();
        buffer.put(buf, offset, length);
        buffer.flip();

        SelectionKey k = (SelectionKey) key;
        SocketChannel sc = (SocketChannel) k.channel();

        int wrote = sc.write(buffer);
        updateSent(key, wrote);
        return wrote;
    }

    private void updateSent(Object key, int wrote) {
        if (netMan.contains(key)) {
            netMan.getConnection(key).bytesSent(wrote);
        }
    }

    private void updateReceived(Object key, int n) {
        if (netMan.contains(key)) {
            netMan.getConnection(key).bytesReceived(n);
        }
    }

    public void removeConnection(Object key) {
        SelectionKey k = (SelectionKey) key;
        k.cancel();
    }

    public Packet createPacketForSend() {
        return new NIOPacket(netMan.getCore().allocateBuffer(MAXIMUM_PACKET_SIZE), true);
    }

    public Packet createPacketForReceive(int size) {
        if (T.t) {
            T.info("Creating new packet - this uses a lot of resources");
        }
        return new NIOPacket(netMan.getCore().allocateBuffer(size), false);
    }

    @Override
    public void run() {

        while (netMan.isAlive()) {
            try {
                selector.select();
                //process all keys
                for (Iterator it = selector.selectedKeys().iterator(); it.hasNext();) {
                    SelectionKey key = (SelectionKey) it.next();
                    it.remove();

                    Thread.currentThread().setName("NetworkLayer " + key + " -- " +
                            netMan.getFriendManager().getCore().getSettings().getMy().getNickname());

                    try {
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isConnectable()) {
                            handleConnect(key);
                        } else {
                            if (key.isReadable()) {
                                handleRead(key);
                            }
                            if (key.isValid() && key.isWritable()) {
                                handleWrite(key);
                            }
                        }
                    } catch (CancelledKeyException e) {
                        //reportError(key, e);
                    } catch (Exception e) {
                        reportError(key, e);
                    }
                }

                //process all tasks
                Runnable r;
                while ((r = taskQue.poll()) != null) {
                    r.run();
                }

            } catch (Exception e) {
                if (T.t) {
                    T.error("Error in network loop: " + e + " - pausing for a while and trying again.");
                }
                System.err.println("Error for " + netMan.getFriendManager().getMe() + ": ");
                e.printStackTrace();
                netMan.getCore().reportError(e, this);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }
            }
        }
    }

    private void setupServer() {
        try {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            serverSocket = ssc.socket();
            int bs = netMan.getCore().getSettings().getInternal().getSoreceivebuf();
            if (bs != -1) {
                serverSocket.setReceiveBufferSize(bs);
            }
            serverSocket.setReuseAddress(true);
            InetSocketAddress address;
            if (netMan.getCore().getSettings().getInternal().getServerlistenip().trim().length() > 0) {
                address = new InetSocketAddress(netMan.getCore().getSettings().getInternal().getServerlistenip(),
                        netMan.getServerPort());
            } else {
                address = new InetSocketAddress(netMan.getServerPort());
            }
            serverSocket.bind(address);
            ssc.register(selector, SelectionKey.OP_ACCEPT);

            //maybe a fix for a problem when the binding lingers.
            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    if (serverSocket.isBound()) {
                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                            if (T.t) {
                                T.warn("Error while trying to close server: " + e);
                            }
                        }
                    }
                }
            });

            if (T.t) {
                T.info("Server listening on port " + netMan.getServerPort());
            }
        } catch (IOException e) {
            if (T.t) {
                T.error("COULDN'T BIND PORT! Server not listening!");
            }
        }
    }

    public void invokeLater(Runnable r) {
        taskQue.add(r);
        selector.wakeup();
    }

    private void reportError(SelectionKey key, Exception e) {
        String addr = "";
        if (key.channel() instanceof SocketChannel) {
            try {
                Socket s = ((SocketChannel) key.channel()).socket();
                if (s.getRemoteSocketAddress() != null) {
                    addr = "" + s.getRemoteSocketAddress();
                }
                pendingConnectionAttempts.remove(s);
                if (T.t) {
                    T.trace("removing socketaddress: " + s + " contains: " + pendingConnectionAttempts.contains(s));
                }
            } catch (Exception e2) {
                if (T.t) {
                    T.warn("Could not remove pending connection: " + e);
                }
            }
        }
        if (netMan.getConnection(key) != null) {
            addr = netMan.getConnection(key) + " (" + addr + ")";
        }
        netMan.reportError(addr, key, e);
        key.cancel();
        if (key.channel() instanceof SocketChannel) {
            try {
//                if (netMan.getConnection(key) != null) netMan.getConnection(key).close();
                ((SocketChannel) key.channel()).close();
            } catch (IOException e1) {
                if (T.t) {
                    T.trace("Could not close socket: " + e1);
                }
            }
        }
    }

    private void handleConnect(SelectionKey key) throws IOException {
        if (T.t) {
            T.trace("handleConnect");
        }
        AuthenticatedConnection connection = (AuthenticatedConnection) key.attachment();

        SocketChannel sc = (SocketChannel) key.channel();
        try {
            sc.finishConnect();
        } catch (ConnectException e) {
            connection.signalConnectionAttemtError();
            throw e;
        }
        SelectionKey newKey = sc.register(selector, SelectionKey.OP_READ);
        if (T.t) {
            T.info("Established connection to " + sc.socket().getRemoteSocketAddress() + "!");
        }
        connection.setKey(newKey);

        pendingConnectionAttempts.remove(sc.socket());

        netMan.addConnection(newKey, connection);

        connection.sendConnectionIdentifier();
        connection.init();
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();

        if (!netMan.contains(key)) {
            if (T.t) {
                T.warn("Could not find connection for key! Closing connection. " + key);
            }
            new Exception().printStackTrace();
            sc.close();
            return;
        }

        buffer.clear();
        int read = sc.read(buffer);
        if (read == -1) {
            if (netMan.getConnection(key) instanceof PacketConnection && ((PacketConnection) netMan.getConnection(key)).getPacketsReceived() == 0) {
                throw new IOException(netMan.getConnection(key) + " ended prematurely - remote probably does not have you as friend.");
            } else {
                throw new IOException("Connection ended - " + netMan.getConnection(key));
            }
        }

        if (read != 0) {
            buffer.flip();
//            buffer.get(byteArray, 0, read);
            netMan.received(key, buffer);
            if (T.t) {
                T.ass(buffer.remaining() == 0, "There's stuff left in the buffer! Every connection must read the buffer clean! The network layer would throw the unused data otherwise.");
            }
        }

        updateReceived(key, read);
    }

    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();

        if (!netMan.contains(key)) {
            if (T.t) {
                T.warn("Could not find connection in WRITE! Closing connection. " + key);
            }
            sc.close();
            return;
        }

        netMan.readyToSend(key);
    }

    private void handleAccept(final SelectionKey key) throws IOException {
        if (T.t) {
            T.trace("handleAccept");
        }
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        final SocketChannel sc = ssc.accept();
        sc.configureBlocking(false);

        // Add the new connection to the selector
        SelectionKey newKey = sc.register(selector, SelectionKey.OP_READ);
        Connection c = new HandshakeConnection(netMan, newKey);
        netMan.addConnection(newKey, c);
        c.init();
        if (T.t) {
            T.info("New connection from " + sc.socket().getRemoteSocketAddress());
        }
    }

    public void addInterestForWrite(Object key) {
        try {
            SocketChannel sc = (SocketChannel) ((SelectionKey) key).channel();
            Object o = sc.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
            if (T.t) {
                T.ass(o == key, "Different keys received!");
            }
        } catch (Exception e) {
            //this happens when a channel is closed
            if (T.t) {
                T.info("Could not get interest for write, channel probably closed: " + e);
            }
            //e.printStackTrace();
        }
    }

    public void removeInterestForWrite(Object key) {
        try {
            SocketChannel sc = (SocketChannel) ((SelectionKey) key).channel();
            Object o = sc.register(selector, SelectionKey.OP_READ);
            if (T.t) {
                T.ass(o == key, "Different keys received!");
            }
        } catch (Exception e) {
            //this happens when a channel is closed
            if (T.t) {
                T.info("Could not remove interest for write, channel probably closed: " + e);
            }
        }
    }

    public SocketAddress getSocketAddressFor(Connection connection) {
        return ((SocketChannel) ((SelectionKey) connection.getKey()).channel()).socket().getRemoteSocketAddress();
    }

    public Socket getSocketFor(Connection connection) {
        return ((SocketChannel) ((SelectionKey) connection.getKey()).channel()).socket();
    }

    public void close(Object key) {
        ((SelectionKey) key).cancel();
    }

    /**
     * Thread safe
     */
    public synchronized int getNumberOfPendingConnections() {
        return pendingConnectionAttempts.size();
    }
}
