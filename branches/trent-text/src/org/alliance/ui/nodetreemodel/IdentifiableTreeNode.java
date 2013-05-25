package org.alliance.ui.nodetreemodel;

import javax.swing.tree.TreeNode;

public abstract interface IdentifiableTreeNode extends TreeNode {

    public abstract Object getIdentifier();
}
