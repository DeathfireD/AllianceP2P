package org.alliance.core.file.filedatabase;

import com.stendahls.util.TextUtils;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.comm.SearchHit;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.file.share.ShareBase;
import org.alliance.core.file.FileManager;
import org.alliance.launchers.console.Console;
import static org.alliance.core.CoreSubsystem.KB;
import org.alliance.core.Language;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-06
 * Time: 15:49:43
 * To change this template use File | Settings | File Templates.
 */
public class FileDatabase {

    private CoreSubsystem core;
    private long shareSize = 0;
    private int numberOfShares = 0;
    private boolean priority = false;
    private ArrayList<Boolean> dbInUseQueue = new ArrayList<Boolean>();
    private static final int GROUP_REMOVE_LIMIT = 256;
    private static final String ID_ROOT_HASH = "root_hash";
    private static final String ID_BASE_PATH = "base_path";
    private static final String ID_SUB_PATH = "sub_path";
    private static final String ID_FILENAME = "filename";
    private static final String ID_SIZE = "size";
    private static final String ID_MODIFIED = "modified";
    private static final String ID_HASH = "hash";
    private static final String ID_PATH = "path";
    private static final String ID_CONTAINS = "contains";

    public FileDatabase(CoreSubsystem core) throws IOException {
        this.core = core;
        updateCacheCounters();
    }

    private synchronized void changeInUseQueue(boolean status) {
        if (status) {
            dbInUseQueue.add(0, true);
        } else {
            if (!dbInUseQueue.isEmpty()) {
                dbInUseQueue.remove(0);
            }
        }
    }

    public void addEntry(FileDescriptor fd) {
        if (!core.getFileManager().getDbCore().isConnected()) {
            return;
        }
        changeInUseQueue(true);
        FileIndex fileIndex = new FileIndex(fd.getBasePath(), mergePathParts(fd.getBasePath(), fd.getSubPath(), null));
        byte fileType = FileType.getByFileName(fileIndex.getFilename()).id();
        core.getFileManager().getDbCore().getDbSharesBases().addEntry(fileIndex.getBasePath());
        if (core.getFileManager().getDbCore().getDbShares().addEntry(fileIndex.getBasePath(), fileIndex.getSubPath(), fileIndex.getFilename(), fileType, fd.getSize(), fd.getRootHash().array(), fd.getModifiedAt())) {
            shareSize += fd.getSize();
            numberOfShares++;
            int blockNumber = 0;
            for (Hash h : fd.getHashList()) {
                core.getFileManager().getDbCore().getDbHashes().addEntry(fd.getRootHash().array(), h.array(), blockNumber);
                blockNumber++;
            }
        } else {
            core.getFileManager().getDbCore().getDbDuplicates().addEntry(mergePathParts(fd.getBasePath(), fd.getSubPath(), null), fd.getRootHash().array(), fd.getModifiedAt());
        }
        changeInUseQueue(false);
    }

    public void removeEntry(byte[] rootHash) {
        if (!core.getFileManager().getDbCore().isConnected()) {
            return;
        }
        changeInUseQueue(true);
        ResultSet result = core.getFileManager().getDbCore().getDbShares().getEntry(rootHash);
        try {
            while (result.next()) {
                core.getFileManager().getDbCore().getDbShares().deleteEntry(rootHash);
                shareSize -= result.getLong(ID_SIZE);
                numberOfShares--;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        changeInUseQueue(false);
    }

    public boolean removeEntries(String basePath, String subPath) {
        if (!core.getFileManager().getDbCore().isConnected()) {
            return false;
        }
        if (core.getShareManager().getShareScanner().isBreakScan()) {
            return false;
        }
        changeInUseQueue(true);
        boolean moreEntries = false;
        core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "removedir", basePath + "/" + subPath));
        ResultSet results = core.getFileManager().getDbCore().getDbShares().getEntries(basePath, subPath, true, GROUP_REMOVE_LIMIT);
        try {
            while (results.next()) {
                shareSize -= results.getLong(ID_SIZE);
                numberOfShares--;
                moreEntries = true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        core.getFileManager().getDbCore().getDbShares().deleteEntries(basePath, subPath, GROUP_REMOVE_LIMIT);
        changeInUseQueue(false);
        return moreEntries;
    }

    public void removeOldFiles(String basePath, String subPath, int size) {
        if (!core.getFileManager().getDbCore().isConnected()) {
            return;
        }
        changeInUseQueue(true);
        subPath = TextUtils.makeSurePathIsMultiplatform(subPath);
        if (basePath.equals(subPath)) {
            subPath = "";
        } else {
            subPath = subPath.substring(basePath.length() + 1) + "/";
        }
        if (size == 0) {
            boolean recheck = true;
            while (recheck) {
                recheck = removeEntries(basePath, subPath);
            }
            changeInUseQueue(false);
            return;
        }
        ResultSet results = core.getFileManager().getDbCore().getDbShares().getEntries(basePath, subPath, false, 0);
        try {
            while (results.next()) {
                File f = new File((mergePathParts(results.getString(ID_BASE_PATH), results.getString(ID_SUB_PATH), results.getString(ID_FILENAME))));
                if (!f.exists()) {
                    core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "removefile", results.getString(ID_FILENAME)));
                    removeEntry(results.getBytes(ID_ROOT_HASH));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        changeInUseQueue(false);
    }

    public void removeOldDirs(String basePath, boolean removeExisted) {
        if (!core.getFileManager().getDbCore().isConnected()) {
            return;
        }
        if (core.getShareManager().getShareScanner().isBreakScan()) {
            return;
        }
        changeInUseQueue(true);
        ResultSet resultsDir = core.getFileManager().getDbCore().getDbShares().getSubPaths(basePath);
        try {
            while (resultsDir.next()) {
                if (!resultsDir.getString(ID_SUB_PATH).isEmpty()) {
                    File f = new File((mergePathParts(basePath, resultsDir.getString(ID_SUB_PATH), null)));
                    if (!f.exists() || removeExisted) {
                        boolean recheck = true;
                        while (recheck) {
                            recheck = removeEntries(basePath, resultsDir.getString(ID_SUB_PATH));
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        changeInUseQueue(false);
    }

    public void removeOldShares() {
        if (!core.getFileManager().getDbCore().isConnected()) {
            return;
        }
        if (core.getShareManager().getShareScanner().isBreakScan()) {
            return;
        }
        changeInUseQueue(true);
        try {
            ArrayList<String> shares = new ArrayList<String>();
            for (ShareBase sharebase : core.getFileManager().getShareManager().shareBases()) {
                shares.add(sharebase.getPath());
            }
            ResultSet results = core.getFileManager().getDbCore().getDbSharesBases().getBasePaths();
            while (results.next()) {
                String basePath = results.getString(ID_BASE_PATH);
                if (!shares.contains(basePath)) {
                    core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "removeshare", results.getString(ID_BASE_PATH)), true);
                    removeOldDirs(basePath, true);
                    if (!core.getFileManager().getDbCore().isConnected()) {
                        break;
                    }
                    if (core.getShareManager().getShareScanner().isBreakScan()) {
                        break;
                    }
                    core.getFileManager().getDbCore().getDbSharesBases().deleteEntryBy(basePath);
                    updateCacheCounters();
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        changeInUseQueue(false);
    }

    public FileDescriptor getFd(Hash rootHash) {
        if (!core.getFileManager().getDbCore().isConnected()) {
            return null;
        }
        changeInUseQueue(true);
        // long time = System.currentTimeMillis();
        ResultSet result = core.getFileManager().getDbCore().getDbShares().getEntry(rootHash.array());
        try {
            while (result.next()) {
                String basePath = result.getString(ID_BASE_PATH);
                String subPath = mergePathParts(null, result.getString(ID_SUB_PATH), result.getString(ID_FILENAME));
                long size = result.getLong(ID_SIZE);
                long modifiedAt = result.getLong(ID_MODIFIED);

                File f = new File(mergePathParts(basePath, subPath, null));
                if (f.lastModified() != modifiedAt) {
                    removeEntry(rootHash.array());
                    changeInUseQueue(false);
                    return null;
                }

                result = core.getFileManager().getDbCore().getDbHashes().getEntriesBy(rootHash.array());
                ArrayList<Hash> hashArray = new ArrayList<Hash>();
                while (result.next()) {
                    hashArray.add(new Hash(result.getBytes(ID_HASH)));
                }
                Hash[] hashList = hashArray.toArray(new Hash[hashArray.size()]);
                // System.out.println("getFD - " + (System.currentTimeMillis() - time));             
                FileDescriptor fd = new FileDescriptor(basePath, subPath, size, rootHash, hashList, modifiedAt);
                changeInUseQueue(false);
                return fd;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        changeInUseQueue(false);
        return null;
    }

    public byte[] getRootHash(String basePath, String path) throws IOException {
        if (!core.getFileManager().getDbCore().isConnected()) {
            return null;
        }
        changeInUseQueue(true);
        FileIndex fileIndex = new FileIndex(basePath, path);
        ResultSet result = core.getFileManager().getDbCore().getDbShares().getEntry(fileIndex.getBasePath(), fileIndex.getSubPath(), fileIndex.getFilename());
        try {
            while (result.next()) {
                changeInUseQueue(false);
                return result.getBytes(ID_ROOT_HASH);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        changeInUseQueue(false);
        return null;
    }

    public ArrayList<FileDescriptor> getHashesForPath(String path, String basePath) {
        ArrayList<FileDescriptor> fdList = new ArrayList<FileDescriptor>();
        if (!core.getFileManager().getDbCore().isConnected()) {
            return fdList;
        }
        changeInUseQueue(true);
        try {
            FileIndex fileIndex = new FileIndex(basePath, mergePathParts(basePath, path, null));
            if (!path.endsWith("/")) {
                ResultSet result = core.getFileManager().getDbCore().getDbShares().getEntry(fileIndex.getBasePath(), fileIndex.getSubPath(), fileIndex.getFilename());
                while (result.next()) {
                    File f = new File(mergePathParts(result.getString(ID_BASE_PATH), result.getString(ID_SUB_PATH), result.getString(ID_FILENAME)));
                    if (f.lastModified() != result.getLong(ID_MODIFIED)) {
                        removeEntry(result.getBytes(ID_ROOT_HASH));
                    } else {
                        FileDescriptor fd = new FileDescriptor();
                        fd.setRootHash(new Hash(result.getBytes(ID_ROOT_HASH)));
                        fd.setSubPath(mergePathParts(null, null, result.getString(ID_FILENAME)));
                        fdList.add(fd);
                    }
                }
            } else {
                ResultSet results = core.getFileManager().getDbCore().getDbShares().getEntries(fileIndex.getBasePath(), fileIndex.getSubPath(), true, 512);
                path = path.substring(0, path.length() - 1);
                path = path.substring(0, path.lastIndexOf("/") + 1);
                path = basePath + "/" + path;
                while (results.next()) {
                    File f = new File(mergePathParts(results.getString(ID_BASE_PATH), results.getString(ID_SUB_PATH), results.getString(ID_FILENAME)));
                    if (f.lastModified() != results.getLong(ID_MODIFIED)) {
                        removeEntry(results.getBytes(ID_ROOT_HASH));
                    } else {
                        FileDescriptor fd = new FileDescriptor();
                        fd.setRootHash(new Hash(results.getBytes(ID_ROOT_HASH)));
                        fd.setSubPath(mergePathParts(results.getString(ID_BASE_PATH), results.getString(ID_SUB_PATH), results.getString(ID_FILENAME)).replace(path, ""));
                        fdList.add(fd);
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        changeInUseQueue(false);
        return fdList;
    }

    public HashMap<Hash, Long> getRootHashWithSize(String basePath, String path) {
        HashMap<Hash, Long> hashSize = new HashMap<Hash, Long>();
        if (!core.getFileManager().getDbCore().isConnected()) {
            return hashSize;
        }
        changeInUseQueue(true);
        try {
            FileIndex fileIndex = new FileIndex(basePath, mergePathParts(basePath, path, null));
            if (!path.endsWith("/")) {
                ResultSet results = core.getFileManager().getDbCore().getDbShares().getEntry(fileIndex.getBasePath(), fileIndex.getSubPath(), fileIndex.getFilename());
                while (results.next()) {
                    hashSize.put(new Hash(results.getBytes(ID_ROOT_HASH)), results.getLong(ID_SIZE));
                }
            } else {
                ResultSet results = core.getFileManager().getDbCore().getDbShares().getEntries(fileIndex.getBasePath(), fileIndex.getSubPath(), true, 512);
                while (results.next()) {
                    hashSize.put(new Hash(results.getBytes(ID_ROOT_HASH)), results.getLong(ID_SIZE));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        changeInUseQueue(false);
        return hashSize;
    }

    public synchronized ArrayList<SearchHit> getSearchHits(String query, byte type, int limit) {
        long time = System.currentTimeMillis();
        ArrayList<SearchHit> hitList = new ArrayList<SearchHit>();
        if (!core.getFileManager().getDbCore().isConnected()) {
            return hitList;
        }
        changeInUseQueue(true);
        priority = true;
        try {
            ResultSet results = core.getFileManager().getDbCore().getDbShares().getFilenames(query, limit);
            ArrayList<String> filenames = new ArrayList<String>();
            while (results.next()) {
                filenames.add(results.getString(ID_FILENAME));
            }
            for (String filename : filenames) {
                results = core.getFileManager().getDbCore().getDbShares().getEntries(filename, type);
                while (results.next()) {
                    Hash root = new Hash(results.getBytes(ID_ROOT_HASH));
                    String basePath = results.getString(ID_BASE_PATH);
                    String path = mergePathParts(null, results.getString(ID_SUB_PATH), results.getString(ID_FILENAME));
                    long size = results.getLong(ID_SIZE);
                    int hashedDaysAgo = (int) ((System.currentTimeMillis() - results.getLong(ID_MODIFIED)) / 1000 / 60 / 60 / 24);
                    if (hashedDaysAgo > 255) {
                        hashedDaysAgo = 255;
                    }
                    hitList.add(new SearchHit(root, path, size, basePath, hashedDaysAgo));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        System.out.println("getsearchhit - " + (System.currentTimeMillis() - time));
        priority = false;
        changeInUseQueue(false);
        return hitList;
    }

    public HashMap<String, String> getDuplicates(int limit) {
        HashMap<String, String> duplicates = new HashMap<String, String>();
        if (!core.getFileManager().getDbCore().isConnected()) {
            return duplicates;
        }
        changeInUseQueue(true);
        ResultSet results = core.getFileManager().getDbCore().getDbDuplicates().getAllEntries(limit);
        try {
            while (results.next()) {
                duplicates.put(results.getString(ID_PATH), mergePathParts(results.getString(ID_BASE_PATH), results.getString(ID_SUB_PATH), results.getString(ID_FILENAME)));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        changeInUseQueue(false);
        return duplicates;
    }

    public boolean contains(String basePath, String path, boolean checkDuplicates) {
        if (!core.getFileManager().getDbCore().isConnected()) {
            return false;
        }
        changeInUseQueue(true);
        FileIndex fileIndex = new FileIndex(basePath, path);
        ResultSet result = core.getFileManager().getDbCore().getDbShares().contains(fileIndex.getBasePath(), fileIndex.getSubPath(), fileIndex.getFilename());
        try {
            while (result.next()) {
                if (result.getBoolean(ID_CONTAINS)) {
                    changeInUseQueue(false);
                    return true;
                } else if (checkDuplicates) {
                    result = core.getFileManager().getDbCore().getDbDuplicates().getEntryBy(mergePathParts(fileIndex.getBasePath(), fileIndex.getSubPath(), fileIndex.getFilename()));
                    changeInUseQueue(false);
                    return result.next();
                } else {
                    changeInUseQueue(false);
                    return false;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        changeInUseQueue(false);
        return false;
    }

    public boolean contains(Hash rootHash) {
        if (!core.getFileManager().getDbCore().isConnected()) {
            return false;
        }
        changeInUseQueue(true);
        ResultSet result = core.getFileManager().getDbCore().getDbShares().getEntry(rootHash.array());
        try {
            changeInUseQueue(false);
            return result.next();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        changeInUseQueue(false);
        return false;
    }

    public void updateCacheCounters() {
        if (!core.getFileManager().getDbCore().isConnected()) {
            return;
        }
        changeInUseQueue(true);
        try {
            ResultSet results = core.getFileManager().getDbCore().getDbShares().getNumberOfShares();
            while (results.next()) {
                numberOfShares = results.getInt(1);
            }
            results = core.getFileManager().getDbCore().getDbShares().getTotalSizeOfFiles();
            while (results.next()) {
                shareSize = results.getLong(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        changeInUseQueue(false);
    }

    public long getShareSize() {
        return shareSize;
    }

    public int getNumberOfShares() {
        return numberOfShares;
    }

    public synchronized TreeMap<String, Long> getDirectoryListing(ShareBase base, String subPath) {
        TreeMap<String, Long> fileAndSize = new TreeMap<String, Long>(new Comparator<String>() {

            @Override
            public int compare(String s1, String s2) {
                if (s1 == null || s2 == null) {
                    return 0;
                }
                if (s1.equalsIgnoreCase(s2)) {
                    return s1.compareTo(s2);
                }
                if (s1.endsWith("/") && !s2.endsWith("/")) {
                    return -1;
                }
                if (!s1.endsWith("/") && s2.endsWith("/")) {
                    return 1;
                }
                return s1.compareToIgnoreCase(s2);
            }
        });
        if (!core.getFileManager().getDbCore().isConnected()) {
            return fileAndSize;
        }
        changeInUseQueue(true);
        priority = true;
        int limiter = 0;
        ResultSet results = core.getFileManager().getDbCore().getDbShares().getEntries(base.getPath(), subPath, false, 8196);
        try {
            int politeCounter = 0;
            while (results.next()) {
                if (limiter > 96 * KB) {
                    //Limit to prevent BufferOverload when browsing directory with plenty of files with long names (Windows/winsxs is good example)
                    fileAndSize.put("!!!YOU REACHED DISPLAY LIMIT FOR THIS DIRECTORY. YOU SHOULD REORGANIZE YOUR SHARE!!!/", 0L);
                    break;
                }
                File f = new File(mergePathParts(results.getString(ID_BASE_PATH), results.getString(ID_SUB_PATH), results.getString(ID_FILENAME)));
                if (f.exists()) {
                    fileAndSize.put(f.getName(), f.length());
                    limiter += f.getName().length();
                }
                politeCounter++;
                if (politeCounter == 500) {
                    politeCounter = 0;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        if (limiter < 96 * KB) {
            File dirPath = new File(mergePathParts(base.getPath(), subPath, null));
            File[] list = dirPath.listFiles();
            if (list != null) {
                for (File f : list) {
                    if (limiter > 96 * KB) {
                        //Limit to prevent BufferOverload when browsing directory with plenty of directories with long names (Windows/winsxs is good example)
                        fileAndSize.put("!!!YOU REACHED DISPLAY LIMIT FOR THIS DIRECTORY. YOU SHOULD REORGANIZE YOUR SHARE!!!/", 0L);
                        break;
                    }
                    if (f.isDirectory() && !f.getName().contains(FileManager.INCOMPLETE_FOLDER_NAME)) {
                        fileAndSize.put(f.getName() + "/", 0L);
                        limiter += f.getName().length();
                    }
                }
            }
        }
        priority = false;
        changeInUseQueue(false);
        return fileAndSize;
    }

    private String mergePathParts(String basePath, String subPath, String filename) {
        StringBuilder path = new StringBuilder();
        if (basePath != null) {
            path.append(basePath);
            path.append("/");
        }
        if (subPath != null) {
            path.append(subPath);
        }
        if (filename != null) {
            path.append(filename);
        }
        return path.toString();
    }

    public boolean isPriority() {
        return priority;
    }

    public boolean isDbInUse() {
        if (dbInUseQueue.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void printStats(Console.Printer printer) throws IOException {
        printer.println("Not yet implemented...");
    }

    private class FileIndex {

        private String basePath;
        private String subPath;
        private String filename;

        public FileIndex(String basePath, String path) {
            this.basePath = TextUtils.makeSurePathIsMultiplatform(basePath);
            subPath = TextUtils.makeSurePathIsMultiplatform(path).substring(this.basePath.length() + 1);
            filename = subPath.substring(subPath.lastIndexOf("/") + 1, subPath.length());
            subPath = subPath.substring(0, subPath.length() - filename.length());
        }

        public String getBasePath() {
            return basePath;
        }

        public String getFilename() {
            return filename;
        }

        public String getSubPath() {
            return subPath;
        }
    }
}
