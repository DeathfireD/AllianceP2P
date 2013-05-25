package org.alliance.ui.nodetreemodel;

import com.stendahls.nif.util.EnumerationIteratorConverter;
import com.stendahls.util.TextUtils;
import org.alliance.core.node.Friend;
import org.alliance.core.node.FriendManager;
import org.alliance.core.node.Node;
import org.alliance.core.node.UntrustedNode;
import org.alliance.ui.UISubsystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.tree.TreeNode;
import javax.swing.SwingUtilities;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-31
 * Time: 15:18:57
 * To change this template use File | Settings | File Templates.
 */
public class NodeTreeNode extends GenericNode {

    private ArrayList<NodeTreeNode> children;
    private NodeTreeNode parent;
    private Node node;
    private UISubsystem ui;
    private FriendManager manager;
    private String dummyString;
    private NodeTreeModel model;

    public NodeTreeNode(String dummyString) {
        this.dummyString = dummyString;
    }

    public NodeTreeNode(Node node, NodeTreeNode parent, UISubsystem ui, NodeTreeModel model) {
        this.node = node;
        this.parent = parent;
        this.manager = ui.getCore().getFriendManager();
        this.ui = ui;
        this.model = model;
        model.nodeAdded(this);
    }

    private void assureChildrenAreLoaded() {
        if (children == null) {
            children = new ArrayList<NodeTreeNode>();
            if (dummyString != null) {
                return;
            }
            if (node == manager.getMe()) {
                for (Friend f : manager.friends()) {
                    children.add(new NodeTreeNode(f, this, ui, model));
                }
            } else {
                if (!node.friendsFriendsLoaded()) {
                    children.add(new NodeTreeNode("Loading ..."));
                    manager.getCore().invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                manager.loadSubnodesFor(node);
                            } catch (final IOException e) {
                                SwingUtilities.invokeLater(new Runnable() {

                                    @Override
                                    public void run() {
                                        reportError(e.toString());
                                    }
                                });
                            }
                        }
                    });
                } else {
                    for (UntrustedNode n : node.friendsFriends()) {
                        if (parent != null && parent.getNode() == n) {
                            continue;
                        }
                        if (model.get(n) != null) {
                            children.add(new NodeTreeNode("Recursion to " + n.getNickname()));
                        } else {
                            children.add(new NodeTreeNode(n, this, ui, model));
                        }
                    }
                }
            }
        }
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
        assureChildrenAreLoaded();
        return children.get(childIndex);
    }

    @Override
    public int getChildCount() {
        assureChildrenAreLoaded();
        return children.size();
    }

    @Override
    public TreeNode getParent() {
        return parent;
    }

    @Override
    public int getIndex(TreeNode node) {
        return children.indexOf((NodeTreeNode) node);
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public boolean isLeaf() {
        return dummyString != null;
    }

    @Override
    public Enumeration children() {
        assureChildrenAreLoaded();
        return EnumerationIteratorConverter.enumeration(children.iterator());
    }

    public Node getNode() {
        return node;
    }

    @Override
    public String toString() {
        if (dummyString != null) {
            return dummyString;
        }
        return node.getNickname() + (node.isConnected() ? " (" + TextUtils.formatByteSize(node.getShareSize()) + ")" : "");
    }

    public void reloadChildren() {
        children = null;
        assureChildrenAreLoaded();
    }

    @Override
    public Object getIdentifier() {
        if (node == null) {
            return null;
        }
        return node.getGuid();
    }

    public void reportError(String s) {
        children.clear();
        children.add(new NodeTreeNode(s));
        model.nodeStructureChanged(NodeTreeNode.this);
    }
}
