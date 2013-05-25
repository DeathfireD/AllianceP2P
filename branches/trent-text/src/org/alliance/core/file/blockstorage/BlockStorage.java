package org.alliance.core.file.blockstorage;

import org.alliance.core.CoreSubsystem;
import static org.alliance.core.CoreSubsystem.BLOCK_SIZE;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.Language;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * The BlockStorage keeps track of a list of incomplete files. Information about these files (what blocks of the file
 * are complete and where they're located on disk) is contained here.
 * <p>
 * This information can be serialized/deserialized to disk in order to resume downloads
 * after a restart.
 * <p>
 * When downloading a file the portions of the file (called slices) that are received over the netword are directly sent
 * here using the saveSlice method. The caller of saveSlice needs very little information about the file and what's
 * complete in it. It just sends slices of the file here until there's nothing more to send.
 * <p>
 * The _incomplete_ directory is the representation of the BlockStorage on disk.
 * <p>
 * It's the job of this class to keep track of when the file is complete (the check is made at the end of saveSlice)
 * and to move the file out of the incomplete files directory.
 * <p>
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-20
 * Time: 13:01:14
 */
public abstract class BlockStorage extends Thread {

    private static final int SAVE_INTERVAL_IN_BLOCKS_COMPLETED = 10;
    private HashSet<Hash> blockFiles = new HashSet<Hash>();
    private File storagePath, completeFilePath;
    private HashMap<Hash, BlockFile> blockFileCache = new HashMap<Hash, BlockFile>(); //@todo: clean this up once in a while
    protected CoreSubsystem core;
    private ArrayBlockingQueue<BlockFile> queue = new ArrayBlockingQueue<BlockFile>(1000);
    private boolean alive = true;
    private int blocksCompletedCounter;
    private HashSet<Hash> recentlyDownloaded = new HashSet<Hash>();
    private boolean currentlyDefragmentingFile;
    protected boolean isSequential;

    public BlockStorage(String storagePath, String completeFilePath, CoreSubsystem core) throws IOException {
        if (T.t) {
            T.info("BlockStorage <init> - " + storagePath + " " + completeFilePath);
        }
        this.core = core;
        this.storagePath = new File(storagePath);
        this.completeFilePath = new File(completeFilePath);
        if (!this.storagePath.exists() && !this.storagePath.mkdirs()) {
            throw new IOException("Permission problem: can't create " + storagePath + ".");
        }
        this.completeFilePath.mkdirs();

        loadHashes();

        setName("Defragmenter -- " + core.getSettings().getMy().getNickname());
        start();
    }

    protected abstract void signalFileComplete(BlockFile bf);

    public abstract int getStorageTypeId();

    @Override
    public void run() {
        while (alive) {
            try {
                final BlockFile bf = queue.take();
                currentlyDefragmentingFile = true;
                try {
                    if (T.t) {
                        T.info("Took from BlockStorage finishing que: " + bf);
                    }
                    core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "verify", bf.getFd().getSubPath()));
                    bf.moveToComplete(bf.getFd().getBasePath());
                    core.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            if (T.t) {
                                T.ass(!bf.isOpen(), "Open!");
                            }
                            remove(bf.getFd().getRootHash());
                            bf.getFd().updateModifiedAt();
                            core.getFileManager().getFileDatabase().addEntry(bf.getFd());
                            signalFileComplete(bf);
                        }
                    });
                    core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "filecomplete", bf.getFd().getSubPath()));
                } catch (Exception e) {
                    core.reportError(e, bf.getFd() == null ? bf : bf);
                }
            } catch (InterruptedException e) {
            } finally {
                currentlyDefragmentingFile = false;
                synchronized (this) {
                    notifyAll(); //notifies if someone is waiting for us to complete
                }
            }
        }
    }

    private void loadHashes() {
        File hashes[] = storagePath.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name != null && name.endsWith(".dir");
            }
        });

        if (hashes != null) {
            for (File h : hashes) {
                if (h == null) {
                    continue;
                }
                String s = h.getName();
                s = s.substring(0, s.lastIndexOf('.'));
                if (T.t) {
                    T.trace("Found hash in blockstorage: " + s);
                }
                Hash hash = Hash.createFrom(s);
                blockFiles.add(hash);
            }
        }
        if (T.t) {
            T.info("Loaded " + blockFiles.size() + " hashes from " + storagePath);
        }
    }

    /**
     * @param root
     * @param blockNumber
     * @param sliceOffset The offset into this block at wich the slice should be saved
     * @param fd 
     * @param slice 
     * @param dir
     * @return Number of bytes written
     * @throws IOException
     */
    public synchronized int saveSlice(Hash root, int blockNumber, int sliceOffset, ByteBuffer slice, FileDescriptor fd, String downDir) throws IOException {
        if (T.t) {
            T.ass(fd.getRootHash().equals(root), "Root hash mismatch in block storage! " + root + " " + fd.getRootHash());
        }

        BlockFile bf;

        if (blockFiles.contains(root)) {
            bf = getBlockFile(root);
            if (bf == null) {
                throw new IOException("Could not load BlockFile - maybe you have you removed files from the incomplete folder?");
            }
            if (T.t) {
                T.ass(bf.getFd().getRootHash().equals(root), "Root hash mismatch in block storage! " + bf.getFd().getRootHash() + " " + root + " " + fd.getRootHash());
            }
        } else {
            if (T.t) {
                T.trace("New BlockFile created");
            }
            bf = new BlockFile(fd, this);
            bf.save();
            blockFiles.add(bf.getFd().getRootHash());
            blockFileCache.put(root, bf);
        }

        if (T.t) {
            T.ass(slice.remaining() <= BLOCK_SIZE, "Wow. About to write too much data. This is NOT good.");
        }

        //save slice
        bf.assureOpen();
        int r = bf.write(blockNumber, sliceOffset, slice);
        if (sliceOffset + r >= BlockFile.getBlockSize(blockNumber, fd.getSize())) {
            //block is complete
            if (T.t) {
                T.ass(sliceOffset + r <= BlockFile.getBlockSize(blockNumber, fd.getSize()), "Writing outside of block!!!");
            }
            if (T.t) {
                T.debug("Block complete for " + fd.getRootHash());
            }

            //@todo: this is a stupid way of verifying hash - should do it while downloading - this very bad for performance when download at higher speelds too
            //verify that hash is correct on disk.
            Hash h = bf.calculateHash(blockNumber);
            if (!h.equals(bf.getFd().getSubHash(blockNumber))) {
                bf.blockCorrupted(blockNumber);
                core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "filecorrupt"));
                if (T.t) {
                    T.error("Tiger hash incorrect for block " + blockNumber + " when saved to disk!!!");
                }
            } else {
                bf.blockCompleted(blockNumber);
                blocksCompletedCounter++;
                if (blocksCompletedCounter > SAVE_INTERVAL_IN_BLOCKS_COMPLETED) {
                    blocksCompletedCounter = 0;
                    bf.save();
                }

                if (bf.isComplete()) {
                    if (T.t) {
                        T.info("Download complete!");
                    }
                    bf.save();
                    String dir = completeFilePath.toString();
                    bf.getFd().setBasePath(dir);
                    if (downDir != null && !downDir.isEmpty()) {
                        File dirCheck = new File(downDir);
                        if (dirCheck.exists()) {
                            bf.getFd().setBasePath(downDir);
                        }
                    }
                    recentlyDownloaded.add(root);
                    queForCompletion(bf);
                }
            }
        }
        return r;
    }

    private void queForCompletion(BlockFile bf) {
        if (queue.contains(bf)) {
            return;
        }
        queue.add(bf);
    }

    public synchronized void waitForUnfinishedTasks() {
        //if there is a save in progress we won't be let into this method before its done (because of 'synchornized')
        while (queue.size() > 0 || currentlyDefragmentingFile) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
    }

    private synchronized void remove(Hash root) {
        if (T.t) {
            T.info("Removing hash " + root + " from BlockStorage");
        }
        blockFileCache.remove(root);
        blockFiles.remove(root);
        File f = new File(storagePath + "/" + root.getRepresentation() + ".dat");
        if (f.exists()) {
            if (!f.delete()) {
                if (T.t) {
                    T.error("Could not delete " + f);
                }
            }
        }
        f = new File(storagePath + "/" + root.getRepresentation() + ".dir");

        if (f.exists()) {
            if (!f.delete()) {
                if (T.t) {
                    T.error("Could not delete " + f);
                }
            }
        } else {
            if (T.t) {
                T.warn("File " + f + " did not exists. This should be because we aborted a download that had not started yet.");
            }
        }
    }

    private BlockFile load(Hash root) throws IOException {
        return BlockFile.loadFrom(this, root, core);
    }

    public boolean containsBlock(Hash rootHash, int blockNumber) throws IOException {
        if (blockFiles.contains(rootHash)) {
            return getBlockFile(rootHash).isBlockComplete(blockNumber);
        }
        return false;
    }

    public BlockFile getCachedBlockFile(Hash root) {
        return blockFileCache.get(root);
    }

    public BlockFile getBlockFile(Hash root) throws IOException {
        BlockFile bf = blockFileCache.get(root);
        if (bf == null) {
            if (T.t) {
                T.trace("Cache miss for " + root + " - loading.");
            }
            bf = load(root);
            blockFileCache.put(root, bf);
        }
        return bf;
    }

    private void close() throws IOException {
        ArrayList<BlockFile> al = new ArrayList<BlockFile>(blockFileCache.values());
        for (BlockFile f : al) {
            f.close();
        }
    }

    public boolean contains(Hash root) {
        return blockFiles.contains(root);
    }

    public Set<Hash> rootHashes() {
        return blockFiles;
    }

    public BlockMask getBlockMaskFor(Hash root) throws IOException {
        if (!contains(root)) {
            return null;
        }
        BlockFile f = getBlockFile(root);
        if (f != null) {
            return f.getBlockMask();
        }
        return null;
    }

    public FileDescriptor getFD(Hash root) throws IOException {
        if (!contains(root)) {
            return null;
        }
        BlockFile f = getBlockFile(root);
        if (f != null) {
            return f.getFd();
        }
        return null;
    }

    public void shutdown() throws IOException {
        waitForUnfinishedTasks();
        for (BlockFile bf : blockFileCache.values()) {
            if (bf != null) {
                bf.save();
            }
        }
        close();
        alive = false;
    }

    public boolean isRecentlyDownloaded(Hash rootHash) {
        return recentlyDownloaded.contains(rootHash);
    }

    public void removePermanently(Hash root) throws IOException {
        BlockFile bf = getBlockFile(root);
        if (T.t) {
            T.info("Block file did not exist.");
        }
        if (bf != null) {
            bf.close();
        }
        remove(root);
    }

    public CoreSubsystem getCore() {
        return core;
    }

    public File getStoragePath() {
        return storagePath;
    }

    public File getCompleteFilesFilePath() {
        return completeFilePath;
    }

    public File getIncompleteFilesFilePath() {
        return storagePath;
    }

    public boolean isSequential() {
        return isSequential;
    }

    public static BlockStorage getById(CoreSubsystem core, int id) {
        if (id == DownloadStorage.TYPE_ID) {
            return core.getFileManager().getDownloadStorage();
        } else if (id == CacheStorage.TYPE_ID) {
            return core.getFileManager().getCache();
        } else {
            if (T.t) {
                T.ass(false, "Unknown block storage type! " + id);
            }
            return null;
        }
    }
}
