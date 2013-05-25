package org.alliance.ui.nodetreemodel;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class JTreeFoldStateManager {

    public static Set<Object> saveState(JTree jTree) {
        HashSet expandedNodes = new HashSet();
        Enumeration tpaths = jTree.getExpandedDescendants(new TreePath(jTree.getModel().getRoot()));
        while ((tpaths != null) && (tpaths.hasMoreElements())) {
            TreePath tp = (TreePath) tpaths.nextElement();
            expandedNodes.add(extractIdentifier((TreeNode) tp.getLastPathComponent()));
        }
        return expandedNodes;
    }

    public static void restoreState(JTree jTree, Set<Object> expandedNodes) {
        if (expandedNodes == null) {
            openToDepth(jTree);
            return;
        }

        TreePath treePath = new TreePath(jTree.getModel().getRoot());
        Object[] objects = treePath.getPath();
        for (Object o : objects) {
            TreeNode gn = (TreeNode) o;
            if (expandedNodes.contains(extractIdentifier(gn))) {
                jTree.expandPath(new TreePath(o));
                List path = new ArrayList();
                path.add(o);
                restorePath(jTree, path, expandedNodes, gn.children());
            }
        }
    }

    public static Object extractIdentifier(TreeNode n) {
        if (n instanceof IdentifiableTreeNode) {
            return ((IdentifiableTreeNode) n).getIdentifier();
        }
        return n.toString();
    }

    private static void restorePath(JTree jTree, List path, Set<Object> expandedNodes, Enumeration children) {
        while (children.hasMoreElements()) {
            List changeable = new ArrayList(path);
            TreeNode genericNode = (TreeNode) children.nextElement();
            changeable.add(genericNode);
            if (expandedNodes.contains(extractIdentifier(genericNode))) {
                jTree.expandPath(new TreePath(changeable.toArray()));
                if (genericNode.children() != null) {
                    restorePath(jTree, changeable, expandedNodes, genericNode.children());
                }
            }
        }
    }

    public static void openToDepth(JTree jTree) {
        openToDepth(jTree, 1);
    }

    public static void openToDepth(JTree jTree, int depth) {
        TreePath treePath = new TreePath(jTree.getModel().getRoot());
        Object[] objects = treePath.getPath();
        for (Object o : objects) {
            TreeNode gn = (TreeNode) o;
            jTree.expandPath(new TreePath(o));
            List path = new ArrayList();
            path.add(o);
            openLevels(jTree, path, gn.children(), depth - 1);
        }
    }

    private static void openLevels(JTree jTree, List path, Enumeration children, int depth) {
        if (depth == 0) {
            return;
        }
        while (children.hasMoreElements()) {
            List changeable = new ArrayList(path);
            TreeNode genericNode = (TreeNode) children.nextElement();
            changeable.add(genericNode);
            jTree.expandPath(new TreePath(changeable.toArray()));
            if (genericNode.children() != null) {
                openLevels(jTree, changeable, genericNode.children(), depth - 1);
            }
        }
    }
}
