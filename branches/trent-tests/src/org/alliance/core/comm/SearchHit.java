package org.alliance.core.comm;

import org.alliance.core.file.hash.Hash;

/**
 * A search hit - the result of a search. One search hit per file that matched the search. Contains root tiger hash,
 * file path (incl. file name), file size and the age of the file in number of days. 
 * User: maciek
 * Date: 2006-feb-01
 * Time: 20:23:27
 * To change this template use File | Settings | File Templates.
 */
public class SearchHit {

    private Hash root;
    private String path;
    private long size;
    private int hashedDaysAgo;
    private String basepath;

    public SearchHit() {
    }

    public SearchHit(Hash root, String path, long size, int hashedDaysAgo) {
        this.root = root;
        this.path = path;
        this.size = size;
        this.hashedDaysAgo = hashedDaysAgo;
    }
    
     public SearchHit(Hash root, String path, long size, String basepath, int hashedDaysAgo) {
        this.root = root;
        this.path = path;
        this.size = size;
        this.basepath = basepath;
        this.hashedDaysAgo = hashedDaysAgo;
    }

    public Hash getRoot() {
        return root;
    }

    public void setRoot(Hash root) {
        this.root = root;
    }

    public String getPath() {
        return path;
    }

    public String getBasePath() {
        return basepath;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getHashedDaysAgo() {
        return hashedDaysAgo;
    }

    public void setHashedDaysAgo(int hashedDaysAgo) {
        this.hashedDaysAgo = hashedDaysAgo;
    }
}
