package org.alliance.core.file.blockstorage;

import org.alliance.core.CoreSubsystem;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-feb-06
 * Time: 22:57:05
 * To change this template use File | Settings | File Templates.
 */
public class DownloadStorage extends BlockStorage {

    public static final int TYPE_ID = 1;

    public DownloadStorage(String storagePath, String completeFilePath, CoreSubsystem core) throws IOException {
        super(storagePath, completeFilePath, core);
        isSequential = true;
    }

    @Override
    protected void signalFileComplete(BlockFile bf) {
        if (T.t) {
            T.info("File downloaded successfully: " + bf.getFd());
        }
    }

    @Override
    public int getStorageTypeId() {
        return TYPE_ID;
    }
}
