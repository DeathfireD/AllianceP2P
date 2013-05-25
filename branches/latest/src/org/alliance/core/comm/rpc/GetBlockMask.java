package org.alliance.core.comm.rpc;

import org.alliance.core.T;
import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.file.blockstorage.BlockFile;
import org.alliance.core.file.blockstorage.BlockMask;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.file.share.ShareBase;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class GetBlockMask extends RPC {

    private Hash root;

    public GetBlockMask() {
    }

    public GetBlockMask(Hash root) {
        this();
        this.root = root;
    }

    @Override
    public void execute(Packet data) throws IOException {
        root = new Hash();
        data.readArray(root.array());

        core.logNetworkEvent("GetBlockMast for " + core.getFileManager().getFd(root) + " from " + con.getRemoteFriend());

        Collection<ShareBase> c = manager.getCore().getShareManager().shareBases();
        String usergroupname = con.getRemoteGroupName();

        //Split Multi sbgroupname names to single cell in array
        String[] dividedu = usergroupname.split(",");

        if (manager.getCore().getFileManager().containsComplete(root) && manager.getCore().getFileManager().getFd(root) != null) {
            if (T.t) {
                T.info("Found complete file for root " + root);
            }

            String basepath = manager.getCore().getFileManager().getFd(root).getBasePath();

            for (ShareBase sb : c) {

                //lets se if search hit is part of this folder (sb)
                if (sb.getPath().equalsIgnoreCase(basepath)) {
                    String sbgroupname = sb.getSBGroupName(); //Group name for specific folder
                    boolean positive = false;
                    if (sbgroupname.equalsIgnoreCase("public")) {
                        positive = true;
                    } else {
                        //Split Multi sbgroupname names to single cell in array
                        String[] dividedsb = sbgroupname.split(",");
                        //Compare every usergroupname with every sbgroupname break if positive match

                        for (String testsb : dividedsb) {
                            for (String testu : dividedu) {
                                if (testsb.equalsIgnoreCase(testu)) {
                                    positive = true;
                                    break;
                                }
                            }
                            if (positive == true) {
                                break;
                            }
                        }
                    }
                    //If friend (usergroupname) has permission to folder (sbgroupname) or folder is public
                    if (positive == true) {
                        send(new BlockMaskResult(root, true,
                                BlockFile.getNumberOfBlockForSize(manager.getCore().getFileManager().getFd(root).getSize()))); //will automatically route to correct person
                    }
                }
            }
        } else {
            if (usergroupname.contains("cache")) {
                BlockMask bm = manager.getCore().getFileManager().getBlockMask(root);
                if (bm != null) {
                    if (T.t) {
                        T.info("Found incomplete file for root " + root);
                    }
                    send(new BlockMaskResult(root, bm));
                } else {
                    if (T.t) {
                        T.info("Root " + root + " not found.");
                    }
                }
            }
        }
        core.getNetworkManager().getDownloadManager().interestedInHash(con.getRemoteFriend(), root);
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeArray(root.array());
        return p;
    }
}
