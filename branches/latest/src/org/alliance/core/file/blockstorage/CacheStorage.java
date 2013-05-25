package org.alliance.core.file.blockstorage;

import org.alliance.core.CoreSubsystem;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-feb-06
 * Time: 22:57:21
 * To change this template use File | Settings | File Templates.
 */
public class CacheStorage extends BlockStorage {

    public static final int TYPE_ID = 2;

    public CacheStorage(String storagePath, String completeFilePath, CoreSubsystem core) throws IOException {
        super(storagePath, completeFilePath, core);
        isSequential = false;
    }

    @Override
    protected void signalFileComplete(BlockFile bf) {
        String path = bf.getFd().getFullPath();
        if (T.t) {
            T.info("File in cache complete: " + path);
        }

        if (bf.getFd().getRootHash().equals(core.getFileManager().getAutomaticUpgrade().getNewVersionHash())) {
            try {
                core.getFileManager().getAutomaticUpgrade().performUpgrade();
            } catch (Exception e) {
                core.reportError(new Exception("Automatic upgrade failed!", e), null);
            }
        }
    }

    @Override
    public int getStorageTypeId() {
        return TYPE_ID;
    }
}
