package org.alliance.ui.windows.mdi.search;

import com.stendahls.nif.util.EnumerationIteratorConverter;
import com.stendahls.util.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;
import org.alliance.core.comm.SearchHit;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.PacedRunner;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-apr-24
 * Time: 14:15:33
 */
public class RootNode extends SearchTreeNode {

    private ArrayList<SearchTreeNode> children = new ArrayList<SearchTreeNode>();
    private HashMap<String, FolderNode> folderMapping = new HashMap<String, FolderNode>();
    private SearchTreeTableModel model;
    private Comparator<SearchTreeNode> comparator, secondaryComparator;
    private HashMap<Hash, FileNode> nodeCache = new HashMap<Hash, FileNode>();
    private PacedRunner pacedRunner;

    public void setModel(SearchTreeTableModel model, PacedRunner pacedRunner) {
        this.model = model;
        comparator = model.createSourcesComparator();
        secondaryComparator = model.createDaysAgoComparator();
        this.pacedRunner = pacedRunner;
        pacedRunner.setRunner(new Runnable() {

            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        resortTable();
                    }
                });
            }
        });
    }

    public void addSearchHits(int sourceGuid, int hops, List<SearchHit> hits) {
        for (SearchHit h : hits) {
                   
            String fn = TextUtils.makeSurePathIsMultiplatform(h.getPath());

            String filename = fn;
            if (filename.indexOf('/') != -1) {
                filename = filename.substring(filename.lastIndexOf('/') + 1);
            }

            String filenameWithoutExtention = filename;
            if (filenameWithoutExtention.indexOf('.') != -1) {
                filenameWithoutExtention = filenameWithoutExtention.substring(0, filenameWithoutExtention.lastIndexOf('.'));
            }

            String folder;
            if (fn.length() > filename.length()) {
                folder = fn.substring(0, fn.length() - filename.length() - 1);
                if (folder.indexOf('/') != -1) {
                    if (Character.isDigit(folder.charAt(folder.length() - 1)) && folder.length() - folder.lastIndexOf('/') < 10) {
                        //extract parent AND grandparent folder
                        int i = folder.lastIndexOf('/');
                        int j = folder.substring(0, i - 1).lastIndexOf('/');
                        if (j != -1) {
                            folder = folder.substring(j + 1);
                        }
                    } else {
                        //extract parent folder
                        folder = folder.substring(folder.lastIndexOf('/') + 1);
                    }
                }
            } else {
                folder = "";
            }

            if (folder.equalsIgnoreCase(filenameWithoutExtention)) {
                folder = ""; //skip folder if is has same name as file
            }
            if (nodeCache.get(h.getRoot()) != null) {
                FileNode n = nodeCache.get(h.getRoot());
                if (n.getParent() instanceof FolderNode) {
                    ((FolderNode) n.getParent()).addHit(sourceGuid, filename, h);
                } else {
                    n.addHit(sourceGuid);
                }
            } else {
                if (folder.length() == 0) {
                    for (SearchTreeNode n : children) {
                        if (n instanceof FileNode) {
                            FileNode f = (FileNode) n;
                            if (f.getSh().getRoot().equals(h.getRoot())) {
                                f.addHit(sourceGuid);
                                h = null;
                                break;
                            }
                        }
                    }
                    if (h != null) {
                        children.add(new FileNode(this, model, filename, h, sourceGuid));
                    }
                } else {
                    FolderNode n = folderMapping.get(folder.toLowerCase());
                    if (n == null) {
                        n = new FolderNode(this, folder);
                        children.add(n);
                        folderMapping.put(folder.toLowerCase(), n);
                    }
                    n.addHit(sourceGuid, filename, h);
                }
            }
        }

        pacedRunner.invoke(); //invokes resort table, but no more often that once a second
    }

    private void resortTable() {
        for(SearchTreeNode t : children ){

   
        }

        if (secondaryComparator != null) {
            Collections.sort(children, secondaryComparator);
        }
        Collections.sort(children, comparator);
        model.nodeStructureChanged(this);
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
        return children.get(childIndex);
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public TreeNode getParent() {
        return null;
    }

    @Override
    public int getIndex(TreeNode node) {
        return children.indexOf(node);
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Enumeration children() {
        return EnumerationIteratorConverter.enumeration(children.iterator());
    }

    @Override
    public String getName() {
        return "root";
    }

    @Override
    public double getSources() {
        return 0;
    }

    @Override
    public double getSpeed() {
        return 0;
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public int getDaysAgo() {
        return 0;
    }

    public SearchTreeTableModel getModel() {
        return model;
    }

    public void sortByName() {
        comparator = model.createNameComparator();
        secondaryComparator = null;
        resortTable();
    }

    public void sortBySize() {
        comparator = model.createSizeComparator();
        secondaryComparator = null;
        resortTable();
    }

    public void sortByDaysAgo() {
        comparator = model.createDaysAgoComparator();
        secondaryComparator = null;
        resortTable();
    }

    public void sortBySources() {
        comparator = model.createSourcesComparator();
        secondaryComparator = null;
        resortTable();
    }

    public void sortBySpeed() {
        comparator = model.createSpeedComparator();
        secondaryComparator = null;
        resortTable();
    }

    public void addToCache(FileNode fileNode) {
        nodeCache.put(fileNode.getSh().getRoot(), fileNode);
    }
}
