package org.alliance.core.file.blockstorage;

import org.alliance.core.CoreSubsystem;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-feb-06
 * Time: 22:57:05
 * To change this template use File | Settings | File Templates.
 */
public class DownloadStorage extends BlockStorage {

    public static final int TYPE_ID = 1;
    private ArrayList<CustomDownload> downloadDirList = new ArrayList<CustomDownload>();

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

    public void addCustomDownload(int guid, String dir, String selectedDir) {
        if (selectedDir == null) {
            return;
        }
        downloadDirList.add(new CustomDownload(guid, dir, selectedDir));
        if (downloadDirList.size() > 5) {
            downloadDirList.remove(0);
        }
    }

    public String getCustomDownloadDir(int guid, String dir) {
        for (CustomDownload down : downloadDirList) {
            if (down.getGuid() == guid && dir.startsWith(down.getSelectedNode())) {
                String downloadDir = down.getDownloadDir();
                downloadDirList.remove(down);
                return downloadDir;
            }
        }
        return null;
    }

    private class CustomDownload {

        private int guid;
        private String downloadDir;
        private String selectedNode;

        public CustomDownload(int guid, String downloadDir, String selectedNode) {
            this.guid = guid;
            this.downloadDir = downloadDir;
            if (selectedNode.lastIndexOf("/") != -1) {
                if (selectedNode.lastIndexOf("/") != selectedNode.length() - 1) {
                    selectedNode = selectedNode.substring(selectedNode.lastIndexOf("/") + 1);
                } else {
                    selectedNode = selectedNode.substring(0, selectedNode.length() - 1);
                    selectedNode = selectedNode.substring(selectedNode.lastIndexOf("/") + 1);
                    selectedNode += "/";
                }
            }
            this.selectedNode = selectedNode;
        }

        public String getDownloadDir() {
            return downloadDir;
        }

        public int getGuid() {
            return guid;
        }

        public String getSelectedNode() {
            return selectedNode;
        }
    }
}
