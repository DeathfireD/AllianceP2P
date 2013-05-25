package org.alliance.ui.windows.viewshare;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-10
 * Time: 18:16:51
 */
public class ViewShareFileNode extends ViewShareTreeNode {

    public ViewShareFileNode(String name, Long size, ViewShareRootNode root, ViewShareTreeNode parent) {
        super(name, size, root, parent);
    }

    @Override
    protected int getShareBaseIndex() {
        return getParent().getShareBaseIndex();
    }

    @Override
    protected String getFileItemPath() {
        return getParent().getFileItemPath() + getName();
    }

    public boolean isFolder() {
        return getName().endsWith("/");
    }
}
