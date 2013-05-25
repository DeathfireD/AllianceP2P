package org.alliance.core.comm.filetransfers;

import org.alliance.core.comm.NetworkManager;
import org.alliance.core.comm.Packet;
import org.alliance.core.file.hash.Hash;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-26
 * Time: 21:38:22
 * To change this template use File | Settings | File Templates.
 */
public class UploadConnection extends TransferConnection {

    public static final int CONNECTION_ID = 3;
    private Hash root;
    private ByteBuffer buffer;
    private ArrayList<DataProvider> providerQue = new ArrayList<DataProvider>();

    public UploadConnection(NetworkManager netMan, Object key, Direction direction, int userGUID) {
        super(netMan, key, direction, userGUID);
        buffer = netMan.getCore().allocateBuffer(netMan.getCore().getSettings().getInternal().getSocketsendbuffer());
        setStatusString("Waiting for request...");
    }

    @Override
    public Hash getRoot() {
        return root;
    }

    @Override
    public void packetReceived(Packet p) throws IOException {
        int cmd = p.readByte();
        if (cmd == Command.GET_FD.value()) {
            receivedCommand_GETFD(p);
        } else if (cmd == Command.HASH.value()) {
            receivedCommand_HASH(p);
        } else if (cmd == Command.GETBLOCK.value()) {
            receivedCommand_GETBLOCK(p);
        } else if (cmd == Command.GRACEFULCLOSE.value()) {
            receivedCommand_GRACEFULCLOSE(p);
        } else {
            throw new IOException("Unknown transfer command: " + cmd);
        }
    }

    private void receivedCommand_GRACEFULCLOSE(Packet p) throws IOException {
        if (T.t) {
            T.debug("Got command: GRACEFULCLOSE");
        }
        close();
    }

    private void receivedCommand_HASH(Packet p) throws IOException {
        if (T.t) {
            T.debug("Got command: HASH");
        }
        root = new Hash();
        p.readArray(root.array());
        setStatusString(core.getFileManager().getFd(root).getSubpath());
        if (T.t) {
            T.info("Remote wants to download " + root + " from me.");
        }
    }

    private void receivedCommand_GETFD(Packet p) throws IOException {
        if (T.t) {
            T.ass(root != null, "Did not receive root before GETFD!");
        }
        if (T.t) {
            T.debug("Got command: GETFD");
        }
        providerQue.add(new FileDescriptorProvider(core.getFileManager().getFd(root)));
        switchMode(Mode.RAW);

        if (NetworkManager.DIRECTLY_CALL_READYTOSEND) {
            readyToSend();
        } else {
            netMan.signalInterestToSend(this);
        }
    }

    private void receivedCommand_GETBLOCK(Packet p) throws IOException {
        if (T.t) {
            T.ass(root != null, "Did not receive root before GETBLOCK!");
        }
        if (T.t) {
            T.debug("Got command: GETBLOCK");
        }
        int blockNumber = p.readInt();
        if (core.getFileManager().containsComplete(root)) {
            providerQue.add(new CompleteFileBlockProvider(blockNumber, root, core));
        } else {
            providerQue.add(new BlockStorageBlockProvider(blockNumber, root, core));
        }
        switchMode(Mode.RAW);

        if (NetworkManager.DIRECTLY_CALL_READYTOSEND) {
            readyToSend();
        } else {
            netMan.signalInterestToSend(this);
        }
    }

    @Override
    public void readyToSend() throws IOException {
        if (mode == Mode.PACKET) {
            super.readyToSend();
        } else if (mode == Mode.RAW) {
            for (;;) {
                if (providerQue.get(0).fill(buffer) < 0 && buffer.position() == 0) {
                    if (T.t) {
                        T.info("Done with sending from provider");
                    }
                    buffer.clear();
                    providerQue.remove(0);
                    if (providerQue.size() == 0) {
                        netMan.noInterestToSend(this);
                        switchMode(Mode.PACKET);
                        break;
                    } else {
                        continue;
                    }
                }
                //todo: erics change -- tested it and it did not work.
                //if (buffer.remaining() != 0) {
                int toSend = netMan.getUploadThrottle().request(this, buffer.position());
                if (toSend == 0) {
                    netMan.noInterestToSend(this); //not sure this is needed
                    break;
                }
                buffer.flip();

                int r = netMan.send(this, buffer, toSend);
                if (r == -1) {
                    throw new IOException("Connection ended");
                }
                netMan.getUploadThrottle().bytesProcessed(r);
                buffer.compact();

                /*                if (netMan.getUploadThrottle().update(this, r)) {
                //this means that we exeeded our bandwidth limit - return and let BandwidthThrottler kickstart us later (by invoking readyToSend)
                netMan.noInterestToSend(this); //not sure this is needed
                break;
                }
                 */
                if (r == 0) {
                    //                    if(T.t)T.trace("OS Send buffer full - buffer: "+buffer.remaining()+" - standing down and waiting from event from NetworkLayer");
                    netMan.signalInterestToSend(this);
                    break;
                } else {
                    //do nothing
                    }
                //}
            }
        }
    }

    @Override
    protected int getConnectionId() {
        return CONNECTION_ID;
    }

    @Override
    public int getConnectionIdForRemote() {
        return DownloadConnection.CONNECTION_ID;
    }

    @Override
    protected void switchMode(Mode m) {
        super.switchMode(m);
        if (m == Mode.RAW) {
        } else if (m == Mode.PACKET) {
            if (T.t) {
                T.ass(providerQue.size() <= 1, "Theres something in the que!");
            }
            providerQue.clear();
        }
    }

    @Override
    public String toString() {
        return (getRemoteFriend() == null ? "unknown" : getRemoteFriend().getNickname()) + " (upload)";
    }
}
