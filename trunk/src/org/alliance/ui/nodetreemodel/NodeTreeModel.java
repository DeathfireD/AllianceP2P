package org.alliance.ui.nodetreemodel;

import org.alliance.core.node.Node;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-02
 * Time: 22:16:00
 * To change this template use File | Settings | File Templates.
 */
public class NodeTreeModel extends DefaultTreeModel {

    private HashMap<Integer, NodeTreeNode> cache = new HashMap<Integer, NodeTreeNode>();

    public NodeTreeModel() {
        super(null);
    }

    public void signalNodeChanged(Node node) {
        NodeTreeNode n = cache.get(node.getGuid());
        if (n != null) {
            n.reloadChildren();
            nodeStructureChanged(n);
            nodeChanged(n);
        }
    }

    public void nodeAdded(NodeTreeNode nodeTreeNode) {
        if (nodeTreeNode.getNode() == null) {
            return;
        }
        cache.put(nodeTreeNode.getNode().getGuid(), nodeTreeNode);
    }

    public NodeTreeNode get(Node n) {
        return cache.get(n.getGuid());
    }

    public void signalNoRouteToHost(Node node) {
        NodeTreeNode n = get(node);
        if (n != null) {
            n.reportError("No Route to host");
        }

    }

    public void signalStructureChanged() {
        nodeStructureChanged((TreeNode) getRoot());
    }
}
