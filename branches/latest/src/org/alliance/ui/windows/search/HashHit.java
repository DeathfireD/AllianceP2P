package org.alliance.ui.windows.search;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.file.hash.Hash;
import org.alliance.ui.T;

import java.util.ArrayList;

import com.stendahls.util.TextUtils;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-29
 * Time: 19:54:53
 * To change this template use File | Settings | File Templates.
 */
public class HashHit implements Comparable {

    Hash hash;
    int hits;
    String filename;
    String folder;
    long size;
    int daysAgo;
    ArrayList<Integer> userGuids = new ArrayList<Integer>();
    public String path;
    private CoreSubsystem core;

    public HashHit(Hash hash, CoreSubsystem core) {
        this.hash = hash;
        this.core = core;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        return hash.equals(((HashHit) obj).hash);
    }

    @Override
    public int hashCode() {
        return hash.hashCode();
    }

    @Override
    public int compareTo(Object o) {
        if (T.t) {
            T.ass(o instanceof HashHit, "");
        }
        HashHit h = (HashHit) o;
        return hits - h.hits;
    }

    public void addHit(int hopsAway, long size, String fn, int guid, int daysAgo) {
        hits++;

        fn = TextUtils.makeSurePathIsMultiplatform(fn);

        path = fn;

        if (daysAgo > this.daysAgo) {
            this.daysAgo = daysAgo;
        }

        filename = fn;
        if (filename.indexOf('/') != -1) {
            filename = filename.substring(filename.lastIndexOf('/') + 1);
        }

        if (fn.length() > filename.length()) {
            folder = fn.substring(0, fn.length() - filename.length() - 1);
            if (folder.indexOf('/') != -1) {
                folder = folder.substring(folder.lastIndexOf('/') + 1);
            }
        } else {
            folder = "";
        }

        this.size = size;
        userGuids.add(guid);
    }
    private int cachedUsers = -1;
    private String cachedListOfUsers;

    public String getListOfUsers() {
        if (cachedUsers != userGuids.size()) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < userGuids.size(); i++) {
                sb.append(core.getFriendManager().nickname(userGuids.get(i)));
                if (i < userGuids.size() - 1) {
                    sb.append(", ");
                }
            }
            cachedListOfUsers = sb.toString();
        }
        return cachedListOfUsers;
    }

    public ArrayList<Integer> getUserGuids() {
        return userGuids;
    }
}
