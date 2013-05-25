package org.alliance.ui.windows.search;

import com.stendahls.nif.util.EnumerationIteratorConverter;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.comm.SearchHit;
import org.alliance.core.file.hash.Hash;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-apr-24
 * Time: 14:39:04
 */
public class FolderNode extends SearchTreeNode {

    private ArrayList<FileNode> children = new ArrayList<FileNode>();
    private HashMap<Hash, FileNode> mapping = new HashMap<Hash, FileNode>();
    private long totalSize;
    private double totalHits;
    private int newestDaysAgo;
    private RootNode parent;
    private String name;
    private String originalName;

    public FolderNode(RootNode parent, String name) {
        this.parent = parent;
        originalName = name;
        name = name.replace('_', ' ');
        this.name = name;
    }

    public String getOriginalName() {
        return originalName;
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
//        if (shouldDelegate()) getDelegate().getChildAt(childIndex);
        return children.get(childIndex);
    }

    @Override
    public int getChildCount() {
//        if (shouldDelegate()) getDelegate().getChildCount();
        return children.size();
    }

    @Override
    public TreeNode getParent() {
        return parent;
    }

    @Override
    public int getIndex(TreeNode node) {
//        if (shouldDelegate()) getDelegate().getIndex(node);
        return children.indexOf(node);
    }

    @Override
    public boolean getAllowsChildren() {
//        if (shouldDelegate()) getDelegate().getAllowsChildren();
        return true;
    }

    @Override
    public boolean isLeaf() {
//        if (shouldDelegate()) getDelegate().isLeaf();
        return children.size() == 0;
    }

    @Override
    public Enumeration children() {
//        if (shouldDelegate()) getDelegate().children();
        return EnumerationIteratorConverter.enumeration(children.iterator());
    }

    public void addHit(int sourceGuid, String filename, SearchHit h) {
        FileNode n = mapping.get(h.getRoot());
        if (n == null) {
            n = new FileNode(this, parent.getModel(), filename, h, sourceGuid);
            children.add(n);
            mapping.put(h.getRoot(), n);
            totalSize += h.getSize();
            newestDaysAgo = h.getHashedDaysAgo();

            Collections.sort(children);
        } else {
            n.addHit(sourceGuid);
            if (newestDaysAgo > h.getHashedDaysAgo()) {
                newestDaysAgo = h.getHashedDaysAgo();
            }
        }
        totalHits++;
    }

    @Override
    public String toString() {
//        if (shouldDelegate()) return getDelegate().getName();
        return name;
    }

    @Override
    public String getName() {
//        if (shouldDelegate()) getDelegate().getName();
        return name;
    }

    @Override
    public double getSources() {
//        if (shouldDelegate()) getDelegate().getSources();
        return totalHits / children.size();
    }

    @Override
    public double getSpeed() {
        double s = 0;
        for (FileNode n : children) {
            s += n.getSpeed();
        }
        return s / children.size();
    }

    @Override
    public long getSize() {
//        if (shouldDelegate()) getDelegate().getSize();
        return totalSize;
    }

    @Override
    public int getDaysAgo() {
//        if (shouldDelegate()) getDelegate().getDaysAgo();
        return newestDaysAgo;
    }

//    private FileNode getDelegate() {
//        if(T.t) T.ass(shouldDelegate(),"Should not delegate now!");
//        return children.get(0);
//    }
//
//    private boolean shouldDelegate() {
//        return children.size() == 1;
//    }
    public boolean containedInShare(CoreSubsystem core) {
        for (Hash h : mapping.keySet()) {
            if (!core.getFileManager().getFileDatabase().contains(h)) {
                return false;
            }
        }
        return true;
    }
}
