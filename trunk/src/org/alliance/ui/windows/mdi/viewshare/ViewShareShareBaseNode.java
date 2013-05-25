package org.alliance.ui.windows.mdi.viewshare;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-10
 * Time: 18:16:51
 */
public class ViewShareShareBaseNode extends ViewShareTreeNode {

    private int shareBaseIndex;

    public ViewShareShareBaseNode(String name, ViewShareRootNode root, int shareBaseIndex) {
        super(name, 0L, root, root);
        this.shareBaseIndex = shareBaseIndex;
    }

    @Override
    protected int getShareBaseIndex() {
        return shareBaseIndex;
    }

    @Override
    protected String getFileItemPath() {
        return "";
    }

    @Override
    public boolean isLeaf() {
        return false;
    }
}
