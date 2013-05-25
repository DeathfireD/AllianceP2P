package org.alliance.core.comm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-25
 * Time: 16:10:48
 */
public abstract class PacketConnection extends Connection {

    private ArrayBlockingQueue<Packet> packetsToSend = new ArrayBlockingQueue<Packet>(1000);
    private Packet packetCurrentlyInSending;
    private Packet receivePacket;
    private long lastPacketSentAt;
    private int packetsReceived = 0;

    protected PacketConnection(NetworkManager netMan, Direction direction) {
        super(netMan, direction);
    }

    protected PacketConnection(NetworkManager netMan, Direction direction, Object key) {
        super(netMan, direction, key);
    }

    @Override
    public void init() throws IOException {
        super.init();
        receivePacket = netMan.createPacketForReceive(netMan.getCore().getSettings().getInternal().getMaximumAlliancePacketSize() * 11 / 10);
    }

    public abstract void packetReceived(Packet p) throws IOException;

    public void send(Packet p) throws IOException {
        if (T.netTrace) {
            T.trace("Sending packet " + p);
        }
        p.prepareForSend();
        if (!packetsToSend.offer(p)) {
            throw new IOException("Send packet que overflow!");
        }
        if (T.netTrace) {
            T.trace("Queing packet for later send");
        }

        if (NetworkManager.DIRECTLY_CALL_READYTOSEND) {
            readyToSend();
        } else {
            netMan.signalInterestToSend(this);
        }

        lastPacketSentAt = System.currentTimeMillis();
    }

    @Override
    public void readyToSend() throws IOException {
//          looks like alliance can hang in an infitite loop here sometimes
//        if(T.t)T.traceNoDup("OS send buffer space is available - try to send sometihing");
        if (T.netTrace) {
            T.trace("OS send buffer space is available - try to send sometihing. Packets in que: " +
                    packetsToSend.size() + "packetCurrentlySending: " + packetCurrentlyInSending);
        }

        while (true) {
            if (packetCurrentlyInSending == null) {
                if (packetsToSend.size() == 0) {
                    break;
                }
                packetCurrentlyInSending = packetsToSend.poll();
            }
            if (T.t) {
                T.ass(packetCurrentlyInSending != null, "Internal error! Packet is null!");
            }

            int r = netMan.send(this, packetCurrentlyInSending);
            if (r == -1) {
                throw new IOException("Connection ended");
            }
            if (r == 0) {
                netMan.signalInterestToSend(this);
                break;
            }
            if (packetCurrentlyInSending.getAvailable() == 0) {
                if (T.netTrace) {
                    T.trace("Succesfully sent packet " + packetCurrentlyInSending);
                }
                packetCurrentlyInSending = null; //packet has been successfully sent
            }
        }

        if (packetCurrentlyInSending == null && packetsToSend.size() == 0) {
            if (T.netTrace) {
                T.trace("No more interest to send.");
            }
            netMan.noInterestToSend(this);
        }
    }

    @Override
    public void received(ByteBuffer buf) throws IOException {
        if (T.netTrace) {
            T.trace("Received " + buf.remaining() + " bytes - " + receivePacket.getAvailable() + " bytes available in buffer.");
        }
//        Packet.printBuf(buf);
        receivePacket.writeBuffer(buf);
        while (receivePacket.getPos() >= 2) { //there's more than two bytes to read in packet - that mean we can read the packet length
            int mark = receivePacket.getPos();
            receivePacket.setPos(0);
            int len = receivePacket.readUnsignedShort();
            receivePacket.setPos(mark);
            if (T.netTrace) {
                T.trace("Packet length " + len + " received. In packet buffer: " + receivePacket.getPos());
            }
            if (len > core.getSettings().getInternal().getMaximumAlliancePacketSize()) {
                throw new IOException("Received too large Alliance packet! Max: " +
                        core.getSettings().getInternal().getMaximumAlliancePacketSize() + ", received: " + len);
            }
            if (receivePacket.getPos() >= len + 2) {
                if (T.netTrace) {
                    T.trace("Packet successfully received. Length: " + len);
                }
                int receiveLen = receivePacket.getPos();
                receivePacket.setPos(2); //packet begins just after the length info
                int size = receivePacket.getSize();
                receivePacket.setSize(2 + len);
                Connection c = netMan.getConnection(getKey());
                if (c == null) {
                    if (T.t) {
                        T.debug("Connection got closed. Probably because of duplicate connection.");
                    }
                    receivePacket = null;
                    return;
                }
                if (T.t) {
                    T.ass(c == this, "We're about to send a packet to a swapped connection. The right way to go is to send the raw data to the swapped connection using received(...).");
                }
                packetsReceived++;
                ((PacketConnection) c).packetReceived(receivePacket);
                c = netMan.getConnection(getKey());
                if (T.netTrace) {
                    T.trace("Setting size: " + receiveLen);
                }
                receivePacket.setSize(receiveLen);
                if (T.netTrace) {
                    T.trace("Setting pos: " + (len + 2));
                }
                receivePacket.setPos(len + 2);
                receivePacket.compact();
                if (c != this && receivePacket.getPos() > 0) {
                    if (c != null) {
                        if (T.netTrace) {
                            T.trace("Connection swapped and we've got stuff left in buffer. Send in to the new connection as raw data.");
                        }
                        ByteBuffer b = ByteBuffer.allocate(receivePacket.getPos());
                        b.put(receivePacket.asArray());
                        b.flip();
                        c.received(b);
                    } else {
                        if (T.netTrace) {
                            T.warn("Connection is null. I think this is ok.");
                        }
                    }
                    return; //we don't want to receive anything here. The swapped connection will take care of the rest.
                }
            } else {
                //partial read. Wait for more data
                break;
            }
        }
    }

    public long getLastPacketSentAt() {
        return lastPacketSentAt;
    }

    public int getPacketsReceived() {
        return packetsReceived;
    }
}
