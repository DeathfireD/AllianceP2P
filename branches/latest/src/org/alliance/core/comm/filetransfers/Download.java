package org.alliance.core.comm.filetransfers;

import static org.alliance.core.CoreSubsystem.BLOCK_SIZE;
import org.alliance.core.comm.BandwidthAnalyzer;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.FriendConnection;
import org.alliance.core.comm.rpc.BlockMaskResult;
import org.alliance.core.comm.rpc.GetBlockMask;
import org.alliance.core.file.blockstorage.BlockFile;
import org.alliance.core.file.blockstorage.BlockMask;
import org.alliance.core.file.blockstorage.BlockStorage;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.node.Friend;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-22
 * Time: 22:33:24
 * To change this template use File | Settings | File Templates.
 */
public class Download {

    private static final int SEND_BLOCKMASK_UPDATE_INTERVAL_IN_BLOCKS = 3; //after X blocks are downloaded all interested friends are updated with a fresh bockmask

    public enum State {

        WAITING_TO_START, LOADING_FD, DOWNLOADING, COMPLETED
    };
    private Hash root;
    private FileDescriptor fd;
    private HashMap<Integer, BlockMask> blockMasks = new HashMap<Integer, BlockMask>();
    private DownloadManager manager;
    private HashMap<Friend, DownloadConnection> connections = new HashMap<Friend, DownloadConnection>();
    private BlockStorage storage;
    private BandwidthAnalyzer bandwidth = new BandwidthAnalyzer();
    private State state = State.WAITING_TO_START;
    private boolean invalid; //true when download has been aborted for example
    private long bytesComplete;
    private long startedAt;
    //Auxiliary information thats only used in the UI and when taking a decision whether to start another download
    private String auxInfoFilename;
    private ArrayList<Integer> auxInfoGuids;

    public Download(DownloadManager parent, Hash root, BlockStorage storage, String filename, ArrayList<Integer> guids) throws IOException {
        this.manager = parent;
        this.root = root;
        this.storage = storage;
        this.auxInfoFilename = filename;
        this.auxInfoGuids = guids;

        setFd(manager.getCore().getFileManager().getFd(root)); //not sure there is one, but when resuming there is

        if (fd != null) {
            BlockFile bf = storage.getBlockFile(fd.getRootHash());
            if (bf != null) {
                bytesComplete = ((long) bf.getNumberOfBlocksComplete()) * BLOCK_SIZE;
            }
        }
    }

    public void blockMaskReceived(int srcGuid, int hops, BlockMask bm) {
        if (hops > 0) {
            if (T.t) {
                T.debug("Ignoring block mask coming from " + hops + " hops away.");
            }
            return;
        }

        if (!isInterestedInBlockMasks()) {
            if (T.t) {
                T.info("No longer interested in block masks for this download - ignore");
            }
            return;
        }

        if (T.t) {
            T.debug("Received blockmask from " + srcGuid + ": " + bm + " size: " + bm.length());
        }
        blockMasks.put(srcGuid, bm);

        if (state == State.WAITING_TO_START) {
            return;
        }

        if (connections.size() == 0) {
            try {
                if (T.t) {
                    T.info("Got first block mask - starting download.");
                }
                openDownloadConnection(srcGuid);
            } catch (IOException e) {
                if (T.t) {
                    T.warn("Could not connect to " + srcGuid + ". Waiting for next block mask: " + e);
                }
            }
        } else {
            if (getConnectionByGUID(srcGuid) == null) {
                try {
                    if (fd != null && getInterestingBlocks(bm).isEmpty()) {
                        if (T.t) {
                            T.debug("Remote has blocks of file, but no that I'm interested of: " + srcGuid);
                        }
                    } else {
                        if (connections.size() < manager.getMaxConnectionsPerDownload()) {
                            if (T.t) {
                                T.debug("Opening dl connection to another friend");
                            }
                            openDownloadConnection(srcGuid);
                        } else {
                            if (T.t) {
                                T.trace("Not opening more connection for download " + this + ". have already: " + connections.size());
                            }
                        }
                    }
                } catch (IOException e) {
                    if (T.t) {
                        T.warn("Could not connect to " + srcGuid + ". Waiting for next block mask: " + e);
                    }
                }
            } else {
                if (T.t) {
                    T.info("Already connected to " + srcGuid + " - no need to open another download connection");
                }
            }
        }
    }

    public boolean isInterestedInBlockMasks() {
        return !invalid && !isComplete();
    }

    private Connection getConnectionByGUID(int srcGuid) {
        for (DownloadConnection c : connections.values()) {
            if (c.getRemoteUserGUID() == srcGuid) {
                return c;
            }
        }
        return null;
    }

    private void openDownloadConnection(int srcGuid) throws IOException {
        if (T.t) {
            T.trace("Opending download connection to " + srcGuid);
        }
        DownloadConnection d = new DownloadConnection(manager.getNetMan(), Connection.Direction.OUT, srcGuid, this);
        addConnection(d);
        manager.getNetMan().virtualConnect(srcGuid, d);
    }

    public void addConnection(DownloadConnection con) {
        connections.put(con.getRemoteFriend(), con);
    }

    public void fileDescriptorReceived(DownloadConnection source, FileDescriptor fd) throws IOException {
        if (this.fd != null) {
            if (T.t) {
                T.info("Already has FD. Ignore the new one and start download for this connection.");
            }
            if (T.t) {
                T.ass(source.readyToStartDownload(), "Not ready to start download?");
            }
            if (needsMoreDownloadConnections()) {
                source.startDownloadingBlock();
            }
            return;
        }
        if (T.t) {
            T.debug("Received file descriptor " + fd);
        }

        this.fd = fd;
        setState(State.DOWNLOADING);

        ArrayList<DownloadConnection> al = new ArrayList<DownloadConnection>();
        for (DownloadConnection c : connections.values()) {
            al.add(c);
        }
        for (DownloadConnection c : al) {
            if (c.readyToStartDownload() && needsMoreDownloadConnections()) {
                c.startDownloadingBlock();
            }
        }
    }

    private boolean needsMoreDownloadConnections() {
        boolean b = connections.size() < manager.getMaxConnectionsPerDownload();
        if (!b) {
            if (T.t) {
                T.trace("No need for more donwload connections.");
            }
        }
        return b;
    }

    public FileDescriptor getFd() {
        return fd;
    }

    public Hash getRoot() {
        return root;
    }

    public DownloadManager getManager() {
        return manager;
    }

    public boolean isDownloadingFd() {
        for (DownloadConnection c : connections.values()) {
            if (c.isDownloadingFd()) {
                return true;
            }
        }
        return false;
    }

    public void connectionEstablished(DownloadConnection downloadConnection) {
        connections.put(downloadConnection.getRemoteFriend(), downloadConnection);
        if (T.t) {
            T.trace("Connection established: " + downloadConnection + " connections: " + connections.size());
        }
    }

    public int selectBestBlockForDownload(Friend remoteFriend) throws IOException {
        if (T.t) {
            T.debug("Selecting best block for download. Remote: " + remoteFriend);
        }
        BlockMask bm = blockMasks.get(remoteFriend.getGuid());
        if (bm == null) {
            if (T.t) {
                T.info("Ehh. Don't know anything about this friends block mask. Can't download.");
            }
            return -1;
        }

        BitSet interestingBlocks = getInterestingBlocks(bm);

        //remove bocks in progress from interesting blocks:
        BlockFile bf = storage.getBlockFile(root);
        BitSet blocksInProgress = bf == null ? new BitSet() : bf.getBlocksInProgress();
        blocksInProgress.flip(0, fd.getNumberOfBlocks());
        blocksInProgress.and(interestingBlocks);

        if (blocksInProgress.cardinality() > 0) {
            //there are blocks of interest that are NOT in progress. Take one of these
            interestingBlocks = blocksInProgress;
        }  // else there are only blocks in progress. Use any of them


        int highestBlockNumber = 0;
        if (bf != null) {
            highestBlockNumber = bf.getHighestCompleteBlock();
        }
        highestBlockNumber += manager.getCore().getSettings().getInternal().getMaxfileexpandinblocks();
        //we prefer to load blocks below highestBlockNumber
        if (interestingBlocks.nextSetBit(0) < highestBlockNumber) {
            //we're good - there are interesting blocks below the highest block number.

            //remove all blocks above highest block number:
            if (highestBlockNumber + 1 < fd.getNumberOfBlocks()) {
                interestingBlocks.clear(highestBlockNumber + 1, fd.getNumberOfBlocks());
            }
        }

        //select a random block of the ones we're interested in - change this to rarest first in the future
        int c = interestingBlocks.cardinality();
        int n = (int) (Math.random() * c);
        for (int i = interestingBlocks.nextSetBit(0), j = 0; i >= 0; i = interestingBlocks.nextSetBit(i + 1), j++) {
            if (j == n) {
                return i;
            }
        }

        if (T.t) {
            T.trace("Can't find any block to download from " + remoteFriend);
        }
        return -1;
    }

    private BitSet getInterestingBlocks(BlockMask remote) throws IOException {
        if (T.t) {
            T.ass(fd != null, "Need a FD for this call to work");
        }
        BitSet interestingBlocks = new BitSet();
        BlockMask myBm = manager.getCore().getFileManager().getBlockMask(fd.getRootHash());
        if (myBm != null) {
            interestingBlocks.or(myBm);
        }
        interestingBlocks.flip(0, fd.getNumberOfBlocks());
        interestingBlocks.and(remote);
        return interestingBlocks;
    }

    public int getPercentComplete() throws IOException {
        if (fd == null) {
            return 0;
        }
        if (fd.getSize() == 0) {
            return 0;
        }
        int c = (int) (bytesComplete * 100 / fd.getSize());
        if (c > 100) {
            c = 100;
        }
        return c;
    }

    public BlockStorage getStorage() {
        return storage;
    }

    public void setFd(FileDescriptor fd) {
        this.fd = fd;
    }

    public boolean checkIfDownloadIsComplete() {
        //may be in a state where the file is being moved into the complete directory - then it's complete by this methods definition, but not complete by the filedatabase definition
        if (fd == null) {
            return false;
        }
        boolean b = getManager().getCore().getFileManager().isRecentlyDownloadedOrComplete(fd.getRootHash());
        if (b) {
            setState(State.COMPLETED);
        }
        return b;
    }

    public boolean isBlockComplete(int blockNumber) throws IOException {
        BlockFile bf = storage.getBlockFile(fd.getRootHash());
        if (bf == null) {
            return false;
        }
        return bf.isBlockComplete(blockNumber);
    }

    public int getNConnections() {
        return connections.size();
    }

    public void removeConnection(DownloadConnection downloadConnection) throws IOException {
        connections.remove(downloadConnection.getRemoteFriend());
        if (connections.size() == 0 && isComplete()) {
            if (T.t) {
                T.info("Download is complete");
            }
            manager.downloadComplete(this);
        }
    }

    public BandwidthAnalyzer getBandwidth() {
        return bandwidth;
    }

    public boolean isActive() {
        return state != State.WAITING_TO_START && state != State.COMPLETED;
    }

    public boolean isComplete() {
        return state == State.COMPLETED;
    }

    public void startDownload() throws IOException {
        startedAt = System.currentTimeMillis();
        manager.getNetMan().sendToAllFriends(new GetBlockMask(root));
        if (fd == null) {
            setState(State.LOADING_FD);
        } else {
            setState(State.DOWNLOADING);
        }
    }

    public State getState() {
        return state;
    }

    //its up to this class, noone else
    private void setState(State state) {
        this.state = state;
    }

    public void abortAndRemovePerfmanently() throws IOException {
        for (DownloadConnection c : connections.values()) {
            if (c.isConnected()) {
                c.sendGracefulClose();
            }
        }
        storage.removePermanently(root);

    }

    public void addBytesComplete(int bytes) {
        bytesComplete += bytes;
    }

    public int getETAInMinutes() {
        if (fd == null || bandwidth.getAverageCps() == 0) {
            return -1;
        }
        return (int) Math.round((fd.getSize() - bytesComplete) / bandwidth.getAverageCps());
    }

    public long getStartedAt() {
        return startedAt;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }
    private int signalBlockCompleteCounter = 0;

    public void signalBlockComplete(int blockNumber) throws IOException {
        signalBlockCompleteCounter++;
        if (signalBlockCompleteCounter > SEND_BLOCKMASK_UPDATE_INTERVAL_IN_BLOCKS) {
            signalBlockCompleteCounter = 0;
            BlockFile bf = storage.getBlockFile(fd.getRootHash());
            if (bf == null) {
                return;
            }
            List<Friend> list = manager.getFriendsInterestedIn(root);
            if (list != null) {
                for (Friend f : list) {
                    FriendConnection fc = f.getFriendConnection();
                    if (fc != null) {
                        if (T.t) {
                            T.info("Friend " + f + " is interested in file " + root + ". Sending updated block mask to him.");
                        }
                        fc.send(new BlockMaskResult(root, bf.getBlockMask()));
                    }
                }
            }
        }
    }

    public void serializeTo(ObjectOutputStream out) throws IOException {
        out.write(root.array());
        out.writeUTF(auxInfoFilename);
        out.writeObject(auxInfoGuids);
        out.writeInt(storage.getStorageTypeId());
    }

    public static Download createFrom(ObjectInputStream in, DownloadManager m) throws IOException {
        try {
            Hash h = new Hash();
            int r = in.read(h.array());
            String fn = in.readUTF();
            ArrayList<Integer> guids = (ArrayList<Integer>) in.readObject();
            if (T.t) {
                T.ass(r == h.array().length, "Incorrect length when deserializing download");
            }
            BlockStorage bs = BlockStorage.getById(m.getCore(), in.readInt());
            Download d = new Download(m, h, bs, fn, guids);
            d.fd = d.manager.getCore().getFileManager().getFd(h);
            return d;
        } catch (ClassNotFoundException e) {
            throw new IOException("Could not find class while deserializing: " + e);
        }
    }

    public String getAuxInfoFilename() {
        return auxInfoFilename;
    }

    public ArrayList<Integer> getAuxInfoGuids() {
        return auxInfoGuids;
    }

    public Collection<DownloadConnection> connections() {
        return connections.values();
    }
}
