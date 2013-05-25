package org.alliance.core.comm.rpc;

import com.stendahls.util.TextUtils;
import org.alliance.core.comm.T;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.CoreSubsystem;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class HashesForPath extends CompressedRPC {

    private HashMap<Hash, String> hashPath = new HashMap<Hash, String>();

    public HashesForPath() {
    }

    public HashesForPath(String path, String basePath, CoreSubsystem core) {
        if (core != null) {
            hashPath = core.getShareManager().getFileDatabase().getRootHashWithPath(path, basePath);
        }
    }

    @Override
    public void serializeCompressed(DataOutputStream out) throws IOException {
        out.writeUTF(""); //Compatibility<1.0.9
        out.writeInt(hashPath.size());
        for (Hash hash : hashPath.keySet()) {
            out.write(hash.array());
            out.writeUTF(hashPath.get(hash));
        }
        hashPath.clear();
    }

    @Override
    public void executeCompressed(DataInputStream in) throws IOException {
        String path = TextUtils.makeSurePathIsMultiplatform(in.readUTF());
        int numberOfFiles = in.readInt();

        for (int i = 0; i < numberOfFiles; i++) {
            Hash hash = new Hash();
            in.readFully(hash.array());
            hashPath.put(hash, in.readUTF());
        }

        if (T.t) {
            T.info("Loaded " + numberOfFiles);
        }

        //@todo: this is soo messed up - client starts downloading without any state
        ArrayList<Integer> guid = new ArrayList<Integer>();
        guid.add(con.getRemoteUserGUID());

        String subPath = "";
        for (Hash hash : hashPath.keySet()) {
            String commonPath = TextUtils.makeSurePathIsMultiplatform(hashPath.get(hash));

            //Compatibility<1.0.9
            if (!path.isEmpty()) {
                if (subPath.isEmpty()) {
                    subPath = path;
                    while (!commonPath.startsWith(subPath)) {
                        subPath = subPath.substring(subPath.indexOf("/") + 1, subPath.length());
                    }
                    if (subPath.endsWith("/")) {
                        subPath = subPath.substring(0, subPath.length() - 1);
                    }
                    subPath = subPath.substring(0, subPath.lastIndexOf("/") + 1);
                }
                commonPath = commonPath.replace(subPath, "");
            }

            if (core.getFileManager().containsComplete(hash)) {
                core.getUICallback().statusMessage("You already have the file " + commonPath + "!");
            } else if (core.getNetworkManager().getDownloadManager().getDownload(hash) != null) {
                core.getUICallback().statusMessage("You are already downloading " + commonPath + "!");
            } else {
                core.getNetworkManager().getDownloadManager().queDownload(hash, commonPath, guid);
            }
        }
        hashPath.clear();
    }
}
