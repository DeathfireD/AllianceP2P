package org.alliance.ui.windows.mdi.viewshare;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-10
 * Time: 18:16:51
 */
public class ViewShareLoadingNode extends ViewShareFileNode {

    public ViewShareLoadingNode(String text, ViewShareRootNode root, ViewShareTreeNode parent) {
        super(text, 0L, root, parent);
    }

    @Override
    protected int getShareBaseIndex() {
        return getParent().getShareBaseIndex();
    }

    @Override
    protected String getFileItemPath() {
        return getParent().getFileItemPath() + getName();
    }

    @Override
    public boolean isLeaf() {
        return true;
    }
}
