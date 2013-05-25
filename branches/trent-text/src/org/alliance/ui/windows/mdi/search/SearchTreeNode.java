package org.alliance.ui.windows.mdi.search;

import org.alliance.core.Language;

import javax.swing.tree.TreeNode;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-apr-24
 * Time: 14:15:53
 */
public abstract class SearchTreeNode implements TreeNode {

    public Object getValueAt(int column) {
        switch (column) {
            case 0:
                return getName();
            case 1:
                return getSize();
            case 2:
                return getExtension();
            case 3:
                return getDaysAgo();
            case 4:
                return getSources();
            case 5:
                return getSpeed();
            default:
                return Language.getLocalizedString(getClass(), "undefined");
        }
    }

    public String getExtension() {
        return "";
    }

    public abstract String getName();

    public abstract double getSources();

    public abstract double getSpeed();

    public abstract long getSize();

    public abstract int getDaysAgo();
}
