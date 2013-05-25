package org.alliance.core.comm.rpc;

import org.alliance.core.comm.SearchHit;
import org.alliance.core.comm.T;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.file.share.ShareBase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * Sent when a friend needs information about the correct ip/port of a common friend of ours.
 *
 * Recieved when we need info about a friend that we haven't got the correct ip/port to.
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 18:42:48
 */
public class SearchHits extends CompressedRPC {

    public static final int MAX_SEARCH_HITS = 50;
    private ArrayList<SearchHit> hits = new ArrayList<SearchHit>();

    public SearchHits() {
    }

    public void addHit(SearchHit hit) {
        hits.add(hit);
    }

    @Override
    public void serializeCompressed(DataOutputStream out) throws IOException {
        int n = hits.size();
        if (n > MAX_SEARCH_HITS) {
            n = MAX_SEARCH_HITS;
        }
        int i = 0;

        //Get user group names and folders sbgroupname names
        Collection<ShareBase> c = manager.getCore().getShareManager().shareBases();
        String usergroupname = con.getRemoteGroupName();

        //Split Multi group names to single cell in array
        String[] dividedu = usergroupname.split(",");

        //For each search hit we check if it belongs to any of shared folders
        //and for each shared folders we check if user got permission to it
        for (SearchHit sh : hits) {
            String basepath = sh.getBasePath(); //Get base path for 1 search hit

            for (ShareBase sb : c) {

                //lets se if search hit is part of this folder (sb)
                if (sb.getPath().equalsIgnoreCase(basepath)) {
                    String sbgroupname = sb.getGroupName(); //Group name for specific folder
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
                        out.writeLong(sh.getSize());
                        out.write(sh.getRoot().array());
                        out.writeUTF(sh.getPath());
                        out.writeByte((byte) sh.getHashedDaysAgo());
                        break;
                    }
                }
            }
            i++;
            if (i > n) {
                break;
            }
        }
        out.writeLong(-1);
    }

    @Override
    public void executeCompressed(DataInputStream in) throws IOException {
        for (;;) {
            long size = in.readLong();
            if (size == -1) {
                break;
            }
            Hash h = new Hash();
            in.readFully(h.array());
            String path = in.readUTF();
            int daysAgo = in.readUnsignedByte();
            hits.add(new SearchHit(h, path, size, daysAgo));
        }
        if (T.t) {
            T.info("Received " + hits.size() + " hits.");
        }
        manager.getCore().getUICallback().searchHits(fromGuid, hops, hits);
    }

    public int getNHits() {
        return hits.size();
    }
}
