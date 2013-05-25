package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.comm.T;
import org.alliance.core.file.hash.Hash;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:36:41
 * To change this template use File | Settings | File Templates.
 */
public class NewVersionAvailable extends RPC {

    private Hash hash;

    public NewVersionAvailable(Hash hash) {
        this.hash = hash;
    }

    public NewVersionAvailable() {
    }

    @Override
    public void execute(Packet p) throws IOException {
        hash = new Hash();
        p.readArray(hash.array());
        if (T.t) {
            T.info("Received new version info. Queing for download.");
        }
        core.getFileManager().getAutomaticUpgrade().setNewVersionHash(hash);
        if (core.getFileManager().getFileDatabase().contains(hash)) {
            if (T.t) {
                T.info("Upgrade already in my share. Start upgrade.");
            }
            try {
                core.getFileManager().getAutomaticUpgrade().performUpgrade();
            } catch (Exception e) {
                core.reportError(e, "Automatic Upgrade");
            }
        } else {
            ArrayList<Integer> al = new ArrayList<Integer>();
            al.add(con.getRemoteUserGUID());
            core.getNetworkManager().getDownloadManager().queDownload(hash, core.getFileManager().getCache(), "Alliance Upgrade", al, true);
        }
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeArray(hash.array());
        return p;
    }
}
