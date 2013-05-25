package org.alliance.core.comm.filetransfers;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.Manager;
import org.alliance.core.comm.NetworkManager;
import org.alliance.core.comm.rpc.GetBlockMask;
import org.alliance.core.file.blockstorage.BlockMask;
import org.alliance.core.file.blockstorage.BlockStorage;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.node.Friend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-22
 * Time: 22:31:16
 * To change this template use File | Settings | File Templates.
 */
public class DownloadManager extends Manager implements Runnable {

    private static final int NUMBER_OF_GETBLOCKMASK_REQUESTS_TO_SEND = 250;
    private static final byte SERIALIZATION_VERSION = 3;
    private NetworkManager netMan;
    private CoreSubsystem core;
    private HashMap<Hash, Download> downloads = new HashMap<Hash, Download>();
    private ArrayList<Download> downloadQue = new ArrayList<Download>();
    //list over what hashes a friend is interested in
    private HashMap<Hash, List<Friend>> interestedInHashes = new HashMap<Hash, List<Friend>>();
    private ArrayList<BlockMaskRequest> blockMaskRequestQue = new ArrayList<BlockMaskRequest>();
    private boolean alive = true;
    private long lastSaveTick;

    public DownloadManager(CoreSubsystem core) {
        this.core = core;
    }

    @Override
    public void init() throws IOException {
        netMan = core.getNetworkManager();
        load();

        Thread thread = new Thread(this, "DownloadManager -- " + core.getSettings().getMy().getNickname());

        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    @Override
    public void run() {
        while (alive) {
            for (int i = 0; i < 4; i++) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                }
                core.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            flushBlockMaskRequestQue();
                        } catch (IOException e) {
                            core.reportError(e, this);
                        }
                    }
                });
            }

            core.invokeLater(new Runnable() {

                @Override
                public void run() {
                    try {
                        checkForDownloadsToStart();
                    } catch (IOException e) {
                        core.reportError(e, this);
                    }
                }
            });

            if (System.currentTimeMillis() - lastSaveTick > 1000 * 10) {
                core.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            save();
                            core.saveSettings();
                            core.saveState();
                        } catch (Exception e) {
                            if (T.t) {
                                T.error(e);
                            }
                        }
                    }
                });
            }
        }
    }

    private void flushBlockMaskRequestQue() throws IOException {
        int sent = 0;
        while (sent < NUMBER_OF_GETBLOCKMASK_REQUESTS_TO_SEND && blockMaskRequestQue.size() > 0) {
            BlockMaskRequest r = blockMaskRequestQue.get(blockMaskRequestQue.size() - 1);
            blockMaskRequestQue.remove(blockMaskRequestQue.size() - 1);
            if (r.friend.isConnected()
                    && downloads.containsKey(r.download.getRoot())
                    && r.download.isInterestedInBlockMasks()) {
                r.friend.getFriendConnection().send(new GetBlockMask(r.download.getRoot()));
                sent++;
            }
        }
        if (sent != 0) {
            if (T.t) {
                T.trace("Sent " + sent + " GetBlockMasks to friend(s)");
            }
        }
    }

    public void queDownload(Hash root, String filename, ArrayList<Integer> guids) throws IOException {
        queDownload(root, core.getFileManager().getDownloadStorage(), filename, guids, false, null);
    }

    public void queDownload(Hash root, String filename, ArrayList<Integer> guids, String downloadDir) throws IOException {
        queDownload(root, core.getFileManager().getDownloadStorage(), filename, guids, false, downloadDir);
    }

    public void queDownload(Hash root, BlockStorage storage, String filename, ArrayList<Integer> guids, boolean highPrio, String downloadDir) throws IOException {
        Download dl = new Download(this, root, storage, filename, guids, downloadDir);
        queDownload(dl, highPrio);
    }

    public void queDownload(Download dl, boolean highPrio) throws IOException {
        if (T.t) {
            T.info("Queing download for " + dl);
        }
        if (core.getFileManager().containsComplete(dl.getRoot())) {
            if (T.t) {
                T.info("Already has file " + dl);
            }
            return;
        }

        if (downloads.containsKey(dl.getRoot())) {
            if (T.t) {
                T.info("Already queued this item. Ignoring.");
            }
            return;
        }

        downloads.put(dl.getRoot(), dl);
        if (highPrio) {
            downloadQue.add(0, dl);
        } else {
            downloadQue.add(dl);
        }
    }

    private void checkForDownloadsToStart() throws IOException {
        HashSet<Integer> guidsCurrentlyDownloadingFrom = new HashSet<Integer>();
        HashSet<Integer> guidsTryingToDownloadFrom = new HashSet<Integer>();
        for (Download d : downloadQue) {
            if (d.isActive()) {
                for (DownloadConnection dc : d.connections()) {
                    guidsCurrentlyDownloadingFrom.add(dc.getRemoteUserGUID());
                }
                guidsTryingToDownloadFrom.addAll(d.getAuxInfoGuids());
            }
        }

        for (int i : guidsCurrentlyDownloadingFrom) {
            guidsTryingToDownloadFrom.remove(i);
        }

        if (guidsCurrentlyDownloadingFrom.size() < core.getSettings().getInternal().getMaxdownloadconnections()) {
            for (Download d : downloadQue) {
                if (d.getState() == Download.State.WAITING_TO_START) {
                    if (d.getAuxInfoGuids() == null || d.getAuxInfoGuids().isEmpty()) {//if we don't know whos got the file then start downloading immidiatly
                        startDownload(d);
                        return;
                    }

                    for (int guid : d.getAuxInfoGuids()) {
                        if (!guidsCurrentlyDownloadingFrom.contains(guid) && !guidsTryingToDownloadFrom.contains(guid)) {
                            startDownload(d);
                            return;
                        }
                    }
                }
            }
        }
    }

    public void startDownload(Download d) throws IOException {
        d.startDownload();

    }

    public void blockMaskReceived(int srcGuid, int hops, Hash root, BlockMask bm) {
        Download d = downloads.get(root);
        if (d != null) {
            d.blockMaskReceived(srcGuid, hops, bm);
        } else {
            if (T.t) {
                T.trace("Ignoring blockmask for " + root);
            }
        }
    }

    public NetworkManager getNetMan() {
        return netMan;
    }

    public CoreSubsystem getCore() {
        return core;
    }

    public int getMaxConnectionsPerDownload() {
        return core.getSettings().getInternal().getMaxdownloadconnections();
    }

    public Collection<Download> downloads() {
        return downloadQue;
    }

    public boolean contains(Download download) {
        return downloads.containsKey(download.getRoot());
    }

    public void downloadComplete(Download download) throws IOException {
        core.getUICallback().firstDownloadEverFinished();
    }

    public void removeCompleteDownloads() {
        for (Iterator i = downloadQue.iterator(); i.hasNext();) {
            Download d = ((Download) i.next());
            if (d.isComplete()) {
                i.remove();
                downloads.remove(d.getRoot());
            }
        }
    }

    public void remove(Download d) {
        Object o = downloads.remove(d.getRoot());
        if (o == null) {
            if (T.t) {
                T.error("Could not remove download " + d + " hash: " + d.getRoot());
            }
        }
        o = downloadQue.remove(d);
        if (o == null) {
            if (T.t) {
                T.error("Could not remove download for que " + d + " hash: " + d.getRoot());
            }
        }
    }

    public void deleteDownload(Download d) throws IOException {
        try {
            if (downloads.containsKey(d.getRoot())) {
                d.abortAndRemovePerfmanently();
            }
        } finally {
            d.setInvalid(true);
            downloads.remove(d.getRoot());
            downloadQue.remove(d);
        }
    }

    public Download getDownload(Hash hash) {
        return downloads.get(hash);
    }

    public void shutdown() throws IOException {
        save();
        alive = false;
    }

    public void signalFriendWentOnline(Friend friend) throws IOException {
        for (Download d : downloadQue) {
            if (d.isInterestedInBlockMasks()) {
                blockMaskRequestQue.add(new BlockMaskRequest(friend, d));
            }
        }
    }

    public void interestedInHash(Friend remoteFriend, Hash root) {
        List<Friend> l = interestedInHashes.get(root);
        if (l == null) {
            l = new ArrayList<Friend>();
            interestedInHashes.put(root, l);
        }
        l.add(remoteFriend);
    }

    public List<Friend> getFriendsInterestedIn(Hash root) {
        return interestedInHashes.get(root);
    }

    public void save() throws IOException {
        File file = new File(core.getSettings().getInternal().getDownloadquefile());
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
        ArrayList<Download> al = new ArrayList<Download>();
        for (Download d : downloadQue) {
            if (!d.isComplete()) {
                al.add(d);
            }
        }
        if (T.t) {
            T.trace("Saving download que with " + al.size() + " downloads in it.");
        }
        out.writeByte(SERIALIZATION_VERSION);
        out.writeInt(al.size());
        for (Download d : al) {
            d.serializeTo(out);
        }
        out.flush();
        out.close();
        lastSaveTick = System.currentTimeMillis();
    }

    public void load() {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(core.getSettings().getInternal().getDownloadquefile()));
            if (in.readByte() != SERIALIZATION_VERSION) {
                if (T.t) {
                    T.error("Incorrect version for download queue! Ignoring queue.");
                }
                return;
            }
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                queDownload(Download.createFrom(in, this), false);
            }
            in.close();
        } catch (FileNotFoundException e) {
            if (T.t) {
                T.info("Assuming there's no download info. Starting from scratch.");
            }
        } catch (Exception e) {
            if (T.t) {
                T.error("Could not load download queue: " + e);
            }
        }
    }

    public void moveUp(Download d) {
        int i = downloadQue.indexOf(d);
        if (T.t) {
            T.ass(i != -1, "Cant find download!");
        }
        if (i == 0) {
            return;
        }
        downloadQue.remove(d);
        downloadQue.add(i - 1, d);
    }

    public void moveDown(Download d) {
        int i = downloadQue.indexOf(d);
        if (T.t) {
            T.ass(i != -1, "Cant find download!");
        }
        if (i == downloadQue.size() - 1) {
            return;
        }
        downloadQue.remove(d);
        downloadQue.add(i + 1, d);
    }

    public void moveTop(Download d) {
        int i = downloadQue.indexOf(d);
        if (T.t) {
            T.ass(i != -1, "Cant find download!");
        }
        if (i == 0) {
            return;
        }
        downloadQue.remove(d);
        downloadQue.add(0, d);
    }

    public void moveBottom(Download d) {
        int i = downloadQue.indexOf(d);
        if (T.t) {
            T.ass(i != -1, "Cant find download!");
        }
        if (i == downloadQue.size() - 1) {
            return;
        }
        downloadQue.remove(d);
        downloadQue.add(d);
    }

    public void movePos(int pos, Download d) {
        int i = downloadQue.indexOf(d);
        if (T.t) {
            T.ass(i != -1, "Cant find download!");
        }
        downloadQue.remove(d);
        if (pos > i) {
            pos--;
        }
        downloadQue.add(pos, d);
    }

    private static class BlockMaskRequest {

        public BlockMaskRequest(Friend friend, Download download) {
            this.friend = friend;
            this.download = download;
        }
        Friend friend;
        Download download;
    }
}
