package org.alliance.ui.windows.mdi.viewshare;

import com.stendahls.nif.util.EnumerationIteratorConverter;
import org.alliance.core.Language;
import org.alliance.core.comm.rpc.GetDirectoryListing;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;
import org.alliance.core.file.share.ShareBase;
import org.alliance.ui.T;
import org.alliance.ui.dialogs.OptionDialog;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TreeMap;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-10
 * Time: 18:16:51
 */
public abstract class ViewShareTreeNode implements ViewShareIdentifiableTreeNode {

    protected String name;
    protected Long size;
    protected ViewShareRootNode root;
    protected ViewShareTreeNode parent;
    protected boolean hasSentQueryForChildren = false;
    protected ArrayList<ViewShareFileNode> children = new ArrayList<ViewShareFileNode>();

    public ViewShareTreeNode(String name, Long size, ViewShareRootNode root, ViewShareTreeNode parent) {
        this.name = name;
        this.size = size;
        this.root = root;
        this.parent = parent;
    }

    protected abstract int getShareBaseIndex();

    protected abstract String getFileItemPath();

    protected void assureChildrenAreLoaded() {
        if (!hasSentQueryForChildren) {
            final Node node = root.getModel().getNode();
            root.getModel().getCore().invokeLater(new Runnable() {

                @Override
                public void run() {
                    try {
                        if (node instanceof Friend) {
                            Friend f = (Friend) node;
                            if (f.isConnected()) {
                                f.getFriendConnection().send(new GetDirectoryListing(getShareBaseIndex(), getFileItemPath()));
                            } else {
                                OptionDialog.showInformationDialog(root.getModel().getUi().getMainWindow(),
                                        Language.getLocalizedString(getClass(), "offline", f.getNickname()));
                            }
                        } else {
                            ShareBase sb = root.getModel().getCore().getShareManager().getBaseByIndex(getShareBaseIndex());
                            String path = getFileItemPath();
                            if (path.startsWith("/")) {
                                path = path.substring(1);
                            }
                            if (T.t) {
                                T.info("Getting files for my share. ShareBase: " + getShareBaseIndex() + ", path: " + path);
                            }
                            final TreeMap<String, Long> fileSize = root.getModel().getCore().getFileManager().getFileDatabase().getDirectoryListing(sb, path);
                            for (String s : fileSize.keySet()) {
                                if (T.t) {
                                    T.info("File: " + s);
                                }
                            }
                            SwingUtilities.invokeLater(new Runnable() {

                                @Override
                                public void run() {
                                    root.getModel().getWin().directoryListingReceived(getShareBaseIndex(), getFileItemPath(), fileSize);
                                }
                            });
                        }
                    } catch (IndexOutOfBoundsException e) {
                        //Skip probably we have removed share but friend has old share view
                    } catch (Exception e) {
                        root.getModel().getCore().reportError(e, this);
                    }
                }
            });
            hasSentQueryForChildren = true;
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (children.size() == 0) {
                        children.add(new ViewShareLoadingNode(Language.getLocalizedString(getClass(), "loading"), root, ViewShareTreeNode.this));
                        root.getModel().nodeStructureChanged(ViewShareTreeNode.this);
                    }
                }
            });
        }
    }

    @Override
    public int getChildCount() {
        assureChildrenAreLoaded();
        return children.size();
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
        return children.get(childIndex);
    }

    @Override
    public ViewShareTreeNode getParent() {
        return parent;
    }

    @Override
    public int getIndex(TreeNode node) {
        assureChildrenAreLoaded();
        return children.indexOf(node);
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public boolean isLeaf() {
        if (name == null) {
            return false;
        }
        return !name.endsWith("/");
    }

    @Override
    public Enumeration children() {
        assureChildrenAreLoaded();
        return EnumerationIteratorConverter.enumeration(children.iterator());
    }

    @Override
    public String toString() {
        if (name != null && name.endsWith("/")) {
            return name.substring(0, name.length() - 1);
        }
        return name;
    }

    public void pathUpdated(String path, TreeMap<String, Long> fileSize) {
        if (T.t) {
            T.info(this + ": Path updated: " + path);
        }

        if (path.trim().length() == 0) {
            if (T.t) {
                T.info("It's our files - remove all our children and add the new ones.");
            }
            children.clear();
            for (String s : fileSize.keySet()) {
                if (T.t) {
                    T.trace("Adding: " + s);
                }
                children.add(new ViewShareFileNode(s, fileSize.get(s), root, this));
            }
            root.getModel().nodeStructureChanged(this);
        } else {
            String item = getFirstPathItem(path);
            if (T.t) {
                T.trace("Looking for path item " + item);
            }
            ViewShareFileNode node = getNodeByName(item);
            if (node == null) {
                if (T.t) {
                    T.warn("Ehh. Did not find item. Creating it.");
                }
                node = new ViewShareFileNode(item, 0L, root, this);
                children.add(node);
                root.getModel().nodeStructureChanged(this);
            }
            node.pathUpdated(path.substring(item.length()), fileSize);
        }
    }

    private ViewShareFileNode getNodeByName(String item) {
        for (ViewShareFileNode n : children) {
            if (T.t) {
                T.trace("comparing: " + n.getName() + " - " + item);
            }
            if (n.getName().equals(item)) {
                return n;
            }
        }
        return null;
    }

    protected String getFirstPathItem(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        int i = path.indexOf('/');
        if (i == -1) {
            return path;
        } else {
            return path.substring(0, i + 1);
        }
    }

    public String getName() {
        return name;
    }

    public Long getSize() {
        return size;
    }

    @Override
    public Object getIdentifier() {
        return name + getFileItemPath();
    }
}
