package org.alliance.core.file.share;

import com.stendahls.util.TextUtils;
import org.alliance.core.AwayManager;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.SimpleTimer;
import static org.alliance.core.CoreSubsystem.GB;
import org.alliance.core.file.FileManager;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.launchers.OSInfo;
import org.alliance.core.Language;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-06
 * Time: 15:48:44
 * To change this template use File | Settings | File Templates.
 */
public class ShareScanner extends Thread {

    private boolean alive = true;
    private boolean breakScan = false;
    private boolean restartImmediately = false;
    private ShareManager manager;
    private long bytesScanned;
    private int filesScanned;
    private CoreSubsystem core;
    private boolean shouldBeFastScan = false;
    private boolean scanInProgress = false;
    private boolean scannerHasBeenStarted = false;
    private ArrayList<String> filesQueuedForHashing = new ArrayList<String>();
    private long lastFullScanCompletedAt;
    private long lastFlushCompletedAt;

    public ShareScanner(CoreSubsystem core, ShareManager manager) {
        this.core = core;
        this.manager = manager;
        setName("ShareScanner -- " + core.getSettings().getMy().getNickname());
        setPriority(MIN_PRIORITY);
    }

    @Override
    public void run() {
        try {
            Thread.sleep(60 * 1000);
        } catch (InterruptedException e) {
        } //wait a while before starting first scan
        core.getAwayManager().addListener(new AwayManager.AwayStatusListener() {

            @Override
            public void awayStatusChanged(boolean away) throws IOException {
                if (away && System.currentTimeMillis() - lastFlushCompletedAt > 1000 * 60 * 20 && !scanInProgress) {
                    if (T.t) {
                        T.trace("Flushing database because user is away and it was a while since we did it.");
                    }
                    core.saveState();
                }
            }
        });

        scannerHasBeenStarted = true;
        if (core.getSettings().getInternal().getRescansharewhenalliancestarts() != null && core.getSettings().getInternal().getRescansharewhenalliancestarts() != 1) {
            waitForNextScan();
        }
        while (alive) {
            scanInProgress = true;
            breakScan = false;
            restartImmediately = false;
            if (filesQueuedForHashing.size() > 0) {
                processQueuedFiles();
            }
            if (System.currentTimeMillis() - lastFullScanCompletedAt > getShareManagerCycle() || shouldBeFastScan) {
                startScanCycle();
                if (isBreakScan()) {
                    manager.getCore().getUICallback().statusMessage(Language.getLocalizedString(getClass(), "break"), true);
                } else {
                    manager.getCore().getUICallback().statusMessage(Language.getLocalizedString(getClass(), "scanok"), true);
                }
                lastFullScanCompletedAt = System.currentTimeMillis();
            }
            //flush fairly often when user is away - the UI locks when you flush so we want to avoid doing that while the user is by the computer
            if (((core.getAwayManager().isAway() || !core.getUICallback().isUIVisible()) && System.currentTimeMillis() - lastFlushCompletedAt > 1000 * 60 * 20)
                    || System.currentTimeMillis() - lastFlushCompletedAt > 1000 * 60 * 60 * 2) { //if user insists on constantly beeing by the computer with alliance visible then forcefully flush every second hour - note that a flush will be made as soon as the user is away because of the awaystatuslistener
                try {
                    core.saveState();
                    lastFlushCompletedAt = System.currentTimeMillis();
                } catch (IOException e) {
                    core.reportError(e, this);
                }
            }
            shouldBeFastScan = false;
            scanInProgress = false;
            if (!alive) {
                break;
            }
            core.getShareManager().getFileDatabase().updateCacheCounters();
            if (!restartImmediately) {
                waitForNextScan();
            } else {
                shouldBeFastScan = true;
            }
        }
    }

    private void processQueuedFiles() {
        ArrayList<String> al = new ArrayList<String>(filesQueuedForHashing);
        filesQueuedForHashing.clear();
        for (String file : al) {
            try {
                //TODO hash check
                /* if (manager.getFileDatabase().contains(file.toString())) {
                if (T.t) {
                T.trace("File already is hashed: " + file);
                }
                continue;
                }*/
                File f = new File(file);
                if (!f.isDirectory() && f.canRead()) {
                    hash(file);
                }
            } catch (FileNotFoundException e) {
                if (T.t) {
                    T.error("Problem while hashing file " + file + ": " + e + ", trying again later");
                }
                queFileForHashing(file, true);
            } catch (IOException e) {
                if (T.t) {
                    T.error("Could not hash file " + file + ": " + e);
                }
            }
        }

    }

    private void startScanCycle() {
        manager.getCore().getUICallback().statusMessage(Language.getLocalizedString(getClass(), "backup"), true);
        manager.getCore().getFileManager().createBackup();
        if (isBreakScan()) {
            return;
        }
        manager.getCore().getUICallback().statusMessage(Language.getLocalizedString(getClass(), "removecheck"), true);
        manager.getFileDatabase().removeOldShares();
        if (isBreakScan()) {
            return;
        }
        filesScanned = 0;
        ArrayList<ShareBase> al = new ArrayList<ShareBase>(manager.shareBases());
        for (ShareBase base : al) {
            if (!alive) {
                break;
            }
            if (base.isExternal()) {
                File f = new File(base.getPath());
                if (f.getTotalSpace() == 0) {
                    continue;
                }
            }
            String basePath = base.getPath();
            manager.getFileDatabase().removeOldDirs(basePath, false);
            if (isBreakScan()) {
                return;
            }
            manager.getCore().getUICallback().statusMessage(Language.getLocalizedString(getClass(), "scanning", basePath), true);
            if (T.t) {
                T.info("Scanning " + basePath + "...");
            }
            scanPath(new File(basePath), basePath);
        }
    }

    public boolean isBreakScan() {
        return breakScan;
    }

    private void waitForNextScan() {
        try {
            if (T.t) {
                T.info("Wating for next share scan.");
            }
            if (filesQueuedForHashing.size() > 0) {
                if (T.t) {
                    T.info("Files are in hash que so don't wait for too long");
                }
                Thread.sleep(1000 * 5);
            } else {
                Thread.sleep(getShareManagerCycle());
            }
        } catch (Exception e) {
        }
    }

    private long getShareManagerCycle() {
        if (OSInfo.isWindows()) {
            return 1000 * 60 * manager.getSettings().getInternal().getSharemanagercyclewithfilesystemeventsactive();
        } else {
            return 1000 * 60 * manager.getSettings().getInternal().getSharemanagercycle();
        }
    }

    private void scanPath(File file, String basePath) {
        if (!alive) {
            return;
        }
        if (isBreakScan()) {
            return;
        }
        if (shouldSkip(file.getName())) {
            return;
        }
        if (file.isDirectory()) {
            File[] childrens = file.listFiles();
            if (childrens != null) {
                manager.getFileDatabase().removeOldFiles(basePath, file.getAbsolutePath(), childrens.length);
                for (int i = 0; i < childrens.length; i++) {
                    scanPath(childrens[i], basePath);
                }
            }
            return;
        }
        if (file.isHidden() || file.length() == 0) {
            return;
        }
        String path = file.getPath();
        try {
            if (!shouldBeFastScan && filesScanned == 100) {
                filesScanned = 0;
                Thread.sleep(250);
            }
            while (core.getShareManager().getFileDatabase().isPriority()) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
        }
        try {
            filesScanned++;
            manager.getCore().getUICallback().statusMessage(Language.getLocalizedString(getClass(), "searchnew", path));
            if (!manager.getFileDatabase().contains(basePath, path, true)) {
                hash(basePath, new File(path));
            }
        } catch (IOException e) {
            if (T.t) {
                T.warn("Could not hash file " + path + ": " + e);
            }
        }
    }

    private void hash(String file) throws IOException {
        if (manager.getShareBaseByFile(file) == null) {
            if (T.t) {
                T.info("Share base not found for " + file + " - cant hash");
            }
            return;
        }
        File f = new File(file);
        if (!f.exists()) {
            if (T.t) {
                T.warn("File " + file + " does not exist - cant hash!");
            }
            return;
        }
        hash(manager.getShareBaseByFile(file).getPath(), f);
    }

    private void hash(String basePath, File file) throws IOException {
        SimpleTimer st = new SimpleTimer();
        FileDescriptor fd;
        try {
            fd = new FileDescriptor(basePath, file, shouldBeFastScan ? 0 : core.getSettings().getInternal().getHashspeedinmbpersecond(), manager.getCore().getUICallback());
        } catch (FileDescriptor.FileModifiedWhileHashingException e) {
            manager.getCore().getUICallback().statusMessage(Language.getLocalizedString(getClass(), "hashmodified", file.getName()));
            queFileForHashing(file.toString(), true);
            return;
        }
        manager.getCore().getUICallback().statusMessage(Language.getLocalizedString(getClass(), "hashok",
                file.getName(), st.getTime(), TextUtils.formatByteSize((long) (fd.getSize() / (st.getTimeInMs() / 1000.)))));
        manager.getFileDatabase().addEntry(fd);

        bytesScanned += fd.getSize();
        if (bytesScanned > manager.getCore().getSettings().getInternal().getPolitehashingintervalingigabytes() * GB) {
            bytesScanned = 0;
            try {
                if (T.t) {
                    T.info("Polite scanning in progress. Sleeping for " + manager.getCore().getSettings().getInternal().getPolitehashingwaittimeinminutes()
                            + " minutes for harddrive to cool down.");
                }
                Thread.sleep(manager.getCore().getSettings().getInternal().getPolitehashingwaittimeinminutes() * 60 * 1000);
            } catch (InterruptedException e) {
            }
        }
    }

    private void queFileForHashing(String file, boolean lowPriority) {
        try {
            //TODO hash check
            /*  if (manager.getFileDatabase().contains(file.toString())) {
            if (T.t) {
            T.trace("File already is hashed: " + file);
            }
            return;
            }*/
        } catch (Exception e) {
            if (T.t) {
                T.warn("Problem while cheking if file already is hashed: " + e);
            }
        }
        if (filesQueuedForHashing.contains(file)) {
            return;
        }
        if (!lowPriority) {
            filesQueuedForHashing.add(0, file);
            interrupt();
        } else {
            filesQueuedForHashing.add(file);
        }
    }

    private boolean shouldSkip(String name) {
        String s = TextUtils.makeSurePathIsMultiplatform(name);
        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.endsWith(FileManager.UPDATE_FILE_NAME)) {
            return true;
        }
        if (s.endsWith(FileManager.INCOMPLETE_FOLDER_NAME)) {
            return true;
        }
        return false;
    }

    public void kill() {
        alive = false;
        interrupt();
    }

    public void startScan(boolean fastScan) {
        startScan(fastScan, false);
    }

    public void startScan(boolean fastScan, boolean restart) {
        if (scanInProgress) {
            breakScan = true;
            restartImmediately = restart;
            return;
        }
        shouldBeFastScan = fastScan;
        interrupt();
    }

    public boolean isScanInProgress() {
        return scanInProgress;
    }

    public ShareManager getManager() {
        return manager;
    }

    public void signalFileRenamed(final String basePath, final String oldFile, final String newFile) {
        if (!scannerHasBeenStarted || newFile.indexOf(FileManager.INCOMPLETE_FOLDER_NAME) != -1 || newFile.indexOf(FileManager.UPDATE_FILE_NAME) != -1) {
            return;
        }
        core.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    if (manager.getFileDatabase().contains(basePath, oldFile, false)) {
                        byte[] rootHash = manager.getFileDatabase().getRootHash(basePath, oldFile);
                        manager.getFileDatabase().removeEntry(rootHash);
                        manager.getCore().getUICallback().statusMessage(Language.getLocalizedString(getClass(), "removedfile", oldFile), true);
                        queFileForHashing(newFile, false);
                    }
                } catch (IOException e) {
                    core.reportError(e, this);
                }
            }
        });
    }

    public void signalFileDeleted(final String basePath, final String file) {
        if (!scannerHasBeenStarted || file.indexOf(FileManager.INCOMPLETE_FOLDER_NAME) != -1 || file.indexOf(FileManager.UPDATE_FILE_NAME) != -1) {
            return;
        }
        core.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    if (manager.getFileDatabase().contains(basePath, file, false)) {
                        byte[] rootHash = manager.getFileDatabase().getRootHash(basePath, file);
                        manager.getFileDatabase().removeEntry(rootHash);
                        manager.getCore().getUICallback().statusMessage(Language.getLocalizedString(getClass(), "removedfile", file), true);
                    }
                } catch (IOException e) {
                    core.reportError(e, this);
                }
            }
        });
    }

    public void signalFileCreated(final String file) {
        if (!scannerHasBeenStarted || file.indexOf(FileManager.INCOMPLETE_FOLDER_NAME) != -1 || file.indexOf(FileManager.UPDATE_FILE_NAME) != -1) {
            return;
        }
        queFileForHashing(file, false);
    }
}
