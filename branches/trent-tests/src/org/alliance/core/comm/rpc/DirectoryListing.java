package org.alliance.core.comm.rpc;

import org.alliance.core.comm.T;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Comparator;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class DirectoryListing extends CompressedRPC {

    private TreeMap<String, Long> dirListMap = new TreeMap<String, Long>();
    private int shareBaseIndex;
    private String path;

    public DirectoryListing() {
    }

    public DirectoryListing(int shareBaseIndex, String path, TreeMap<String, Long> dirListMap) {
        this.dirListMap = dirListMap;
        this.shareBaseIndex = shareBaseIndex;
        this.path = path;
    }

    @Override
    public void executeCompressed(DataInputStream in) throws IOException {
        shareBaseIndex = in.readInt();
        path = in.readUTF();
        int nFiles = in.readInt();
        if (T.t) {
            T.info("Decompressing " + nFiles + " files for share base " + shareBaseIndex + " and path " + path);
        }

        dirListMap = new TreeMap<String, Long>(new Comparator<String>() {

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

        for (int i = 0; i < nFiles; i++) {
            dirListMap.put(in.readUTF(), 0L);
        }

        try {
            if (nFiles == in.readInt()) {
                for (String s : dirListMap.keySet()) {
                    dirListMap.put(s, in.readLong());
                }
            }
        } catch (EOFException e) {
            if (T.t) {
                T.info("Old DirectoryListing!");
            }
        }

        if (T.t) {
            T.info("Found the following files:");
            for (String s : dirListMap.keySet()) {
                T.info("  " + s);
            }
        }
        core.getUICallback().receivedDirectoryListing(con.getRemoteFriend(), shareBaseIndex, path, dirListMap);
    }

    @Override
    public void serializeCompressed(DataOutputStream out) throws IOException {
        if (T.t) {
            T.info("compressing directory listing and sending..");
        }
        boolean positive = false;
        //Bastvera
        //(Group names for specific user)
        String usergroupname = con.getRemoteGroupName();

        //Bastvera (Group names for specific shared folder)
        String sbgroupname = manager.getCore().getShareManager().getBaseByIndex(shareBaseIndex).getGroupName();
        if (sbgroupname.equalsIgnoreCase("public")) {
            positive = true;
        } else {

            //Split Multi sbgroupname names to single cell in array
            String[] dividedu = usergroupname.split(",");
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
        //Send matched directory listing and always send public folders
        if (positive == true) {
            out.writeInt(shareBaseIndex);
            out.writeUTF(path);
            out.writeInt(dirListMap.size());
            for (String s : dirListMap.keySet()) {
                out.writeUTF(s);
            }
            out.writeInt(dirListMap.size());
            for (Long l : dirListMap.values()) {
                out.writeLong(l);
            }
        } else {
            out.writeInt(shareBaseIndex);
            out.writeUTF(path);
            out.writeInt(0); //Do not list hidden folders
        }
        dirListMap.clear();
    }
}
