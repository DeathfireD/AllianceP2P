package org.alliance.ui.themes;

import org.pushingpixels.substance.api.renderers.SubstanceDefaultListCellRenderer;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 *
 * @author Bastvera
 */
public abstract class AllianceListCellRenderer {

    private DefaultListCellRenderer renderer;

    protected AllianceListCellRenderer(boolean substanceInUse) {
        if (substanceInUse) {
            renderer = new AllianceSubstanceListCellRenderer();
        } else {
            renderer = new AllianceDefaultListCellRenderer();
        }
    }

    public DefaultListCellRenderer getRenderer() {
        return renderer;
    }

    protected abstract void overrideListCellRendererComponent(DefaultListCellRenderer renderer, JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus);

    private class AllianceSubstanceListCellRenderer extends SubstanceDefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            overrideListCellRendererComponent(this, list, value, index, isSelected, cellHasFocus);
            return this;
        }
    }

    private class AllianceDefaultListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            overrideListCellRendererComponent(this, list, value, index, isSelected, cellHasFocus);
            return this;
        }
    }
}
