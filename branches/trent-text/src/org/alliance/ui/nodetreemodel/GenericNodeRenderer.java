package org.alliance.ui.nodetreemodel;

import java.awt.Component;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

public class GenericNodeRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        if (value instanceof GenericNode) {
            GenericNode nd = (GenericNode) value;
            if (nd.getImage() != null) {
                setIcon(nd.getImage());
            }
        }
        return this;
    }
}
