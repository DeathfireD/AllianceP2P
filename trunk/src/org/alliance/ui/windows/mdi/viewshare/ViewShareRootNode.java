package org.alliance.ui.windows.mdi.viewshare;

import com.stendahls.nif.util.EnumerationIteratorConverter;
import org.alliance.ui.T;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Enumeration;
import org.alliance.core.comm.rpc.ShareBaseList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-10
 * Time: 18:15:45
 */
public class ViewShareRootNode extends ViewShareTreeNode {

    private ArrayList<ViewShareShareBaseNode> shareBases = new ArrayList<ViewShareShareBaseNode>();
    private ViewShareTreeModel model;

    public ViewShareRootNode() {
        super("root", 0L, null, null);
    }

    void fill(String[] shareBaseNames) {
        shareBases.clear();
        for (int i = 0; i < shareBaseNames.length; i++) {
            //TODO: Maybe Translate
            if (!shareBaseNames[i].equals("cache") && !shareBaseNames[i].equals(ShareBaseList.HIDDEN_FOLDERS_MESSAGE)) { //For hiding unique empty folders
                shareBases.add(new ViewShareShareBaseNode(shareBaseNames[i], this, i));
            }
        }
    }

    @Override
    public ViewShareShareBaseNode getChildAt(int childIndex) {
        return shareBases.get(childIndex);
    }

    @Override
    protected int getShareBaseIndex() {
        throw new RuntimeException("This may not be called!");
    }

    @Override
    protected String getFileItemPath() {
        if (T.t) {
            T.warn("This should not be called");
        }
        return null;
    }

    @Override
    public int getChildCount() {
        return shareBases.size();
    }

    @Override
    public ViewShareTreeNode getParent() {
        return null;
    }

    @Override
    public int getIndex(TreeNode node) {
        if (!(node instanceof ViewShareShareBaseNode)) {
            return -1;
        }
        return shareBases.indexOf((ViewShareShareBaseNode) node);
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
        return EnumerationIteratorConverter.enumeration(shareBases.iterator());
    }

    public ViewShareTreeModel getModel() {
        return model;
    }

    public void setModel(ViewShareTreeModel viewShareTreeModel) {
        model = viewShareTreeModel;
    }

    public ViewShareShareBaseNode getByShareBase(int shareBaseIndex) {
        for (ViewShareShareBaseNode s : shareBases) {
            if (s.getShareBaseIndex() == shareBaseIndex) {
                return s;
            }
        }
        return null;
    }
}
