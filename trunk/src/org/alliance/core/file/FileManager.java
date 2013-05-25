package org.alliance.core.file;

import org.alliance.core.CoreSubsystem;
import static org.alliance.core.CoreSubsystem.KB;
import org.alliance.core.Manager;
import org.alliance.core.comm.siteupdater.SiteUpdate;
import org.alliance.core.file.blockstorage.BlockMask;
import org.alliance.core.file.blockstorage.BlockStorage;
import org.alliance.core.file.blockstorage.CacheStorage;
import org.alliance.core.file.blockstorage.DownloadStorage;
import org.alliance.core.file.filedatabase.FileDatabase;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.file.h2database.DatabaseCore;
import org.alliance.core.file.share.ShareManager;
import org.alliance.core.settings.Settings;
import org.alliance.core.Language;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.TreeSet;

/**
 * Keeps track of all files used in Alliance. Contains the ShareManager, DownloadStorage and AutomaticUpgrade. Should
 * contain FileDatabase but it's actually contained in the ShareManager. Also contains the CacheStorage (that's hardly
 * used right now).
 * <P>
 * This is the place to check if we have a certain root hash (complete or incomplete), to get FileDescriptors and to
 * do file searches.
 * <p>
 * @see DownloadStorage, ShareManager, AutomaticUpgrade, FileDatabase
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-22
 * Time: 19:02:07
 * To change this template use File | Settings | File Templates.
 */
public class FileManager extends Manager {

    private DatabaseCore dbCore;
    private CacheStorage cache;
    private DownloadStorage downloads;
    private ShareManager shareManager;
    private CoreSubsystem core;
    private Settings settings;
    private SiteUpdate siteUpdater;
    public static final String INCOMPLETE_FOLDER_NAME = "_incomplete_";
    public static final String UPDATE_FILE_NAME = "alliance.update";
    public static final int MAKE_BACKUP = 0;
    public static final int RESTORE_BACKUP = 1;

    public FileManager(CoreSubsystem core, Settings settings) throws IOException {
        this.core = core;
        this.settings = settings;
    }

    @Override
    public void init() throws IOException, Exception {
        dbCore = new DatabaseCore(core);
        dbCore.connect(null);
        cache = new CacheStorage(settings.getInternal().getCachefolder() + "/" + FileManager.INCOMPLETE_FOLDER_NAME, settings.getInternal().getCachefolder(), core);
        downloads = new DownloadStorage(settings.getInternal().getDownloadfolder() + "/" + FileManager.INCOMPLETE_FOLDER_NAME, settings.getInternal().getDownloadfolder(), core);
        shareManager = new ShareManager(core, settings);
        siteUpdater = new SiteUpdate(core);
        Thread siteUpdaterT = new Thread(siteUpdater);
        siteUpdaterT.setName("Site Updater");
        siteUpdaterT.start();
    }

    public ShareManager getShareManager() {
        return shareManager;
    }

    public DatabaseCore getDbCore() {
        return dbCore;
    }

    public SiteUpdate getSiteUpdater() {
        return siteUpdater;
    }

    public CacheStorage getCache() {
        return cache;
    }

    public DownloadStorage getDownloadStorage() {
        return downloads;
    }

    public void shutdown() throws IOException {
        shareManager.shutdown();
        cache.shutdown();
        downloads.shutdown();
        core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "compacting"), true);
        dbCore.shutdown();
    }

    public boolean contains(Hash root) {
        if (cache.contains(root)) {
            return true;
        }
        if (downloads.contains(root)) {
            return true;
        }
        if (shareManager.getFileDatabase().contains(root)) {
            return true;
        }
        return false;
    }

    public boolean containsComplete(Hash root) {
        return shareManager.getFileDatabase().contains(root);
    }

    public BlockMask getBlockMask(Hash root) throws IOException {
        BlockMask bm = downloads.getBlockMaskFor(root);
        if (bm != null) {
            return bm;
        }
        return cache.getBlockMaskFor(root);
    }

    public FileDescriptor getFd(Hash root) throws IOException {
        FileDescriptor fd = shareManager.getFileDatabase().getFd(root);
        if (fd != null) {
            return fd;
        }
        fd = downloads.getFD(root);
        if (fd != null) {
            return fd;
        }
        fd = cache.getFD(root);
        return fd;
    }

    public BlockStorage getBlockStorageFor(Hash root) {
        if (downloads.contains(root)) {
            return downloads;
        }
        if (cache.contains(root)) {
            return cache;
        }
        return null;
    }

    public boolean hasBlock(Hash rootHash, int blockNumber) throws IOException {
        if (shareManager.getFileDatabase().contains(rootHash)) {
            return true;
        }
        if (downloads.contains(rootHash) && downloads.getBlockMaskFor(rootHash).get(blockNumber)) {
            return true;
        }
        return cache.contains(rootHash) && cache.getBlockMaskFor(rootHash).get(blockNumber);
    }

    public FileDatabase getFileDatabase() {
        return shareManager.getFileDatabase();
    }

    public boolean isRecentlyDownloadedOrComplete(Hash rootHash) {
        if (containsComplete(rootHash)) {
            return true;
        }
        if (downloads.isRecentlyDownloaded(rootHash)) {
            return true;
        }
        return cache.isRecentlyDownloaded(rootHash);
    }

    public static void copyFile(File src, File dst) throws IOException {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);
        byte buf[] = new byte[128 * KB];
        int read;
        while ((read = in.read(buf)) != -1) {
            out.write(buf, 0, read);
        }
        out.flush();
        out.close();
        in.close();
    }

    public void createBackup() {
        core.getFileManager().getDbCore().backup();
        TreeSet<File> backupFiles = getBackups();
        while (backupFiles.size() > 3) {
            File f = backupFiles.pollFirst();
            if (f != null) {
                f.delete();
            }
        }
    }

    public String prepareToRestore(String backup) {
        if (core.getProgress() != null) {
            core.getProgress().updateProgress(Language.getLocalizedString(getClass(), "backup"));
        }
        dbCore.shutdown();
        File dbFile = new File(core.getSettings().getInternal().getDatabasefile() + ".h2.db");
        dbFile.renameTo(new File(dbFile.getAbsolutePath() + "-" + System.currentTimeMillis() + ".old"));
        if (backup != null) {
            return backup;
        }
        TreeSet<File> backupFiles = getBackups();
        return backupFiles.last().getAbsolutePath();
    }

    private TreeSet<File> getBackups() {
        String dbPath = core.getSettings().getInternal().getDatabasefile();
        dbPath = dbPath.substring(0, dbPath.lastIndexOf("/"));
        File dbDir = new File(dbPath);
        TreeSet<File> backupFiles = new TreeSet<File>();
        for (File f : dbDir.listFiles()) {
            if (f.getName().startsWith("alliance-script-") && f.getName().endsWith(".zip")) {
                backupFiles.add(f);
            }
        }
        return backupFiles;
    }
}
