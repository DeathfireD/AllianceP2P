package org.alliance.ui.nodetreemodel;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

public class GenericNode extends DefaultMutableTreeNode implements IdentifiableTreeNode {

    private String text;
    private Icon image;

    public GenericNode() {
    }

    public GenericNode(String text, Icon image, Object userObject) {
        this.text = text;
        this.image = image;
        setUserObject(userObject);
    }

    public void setImage(Icon image) {
        this.image = image;
    }

    public Icon getImage() {
        return this.image;
    }

    @Override
    public Object getIdentifier() {
        return this.text;
    }

    @Override
    public String toString() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
