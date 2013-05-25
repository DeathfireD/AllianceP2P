package org.alliance.ui.windows.shares;

import javax.swing.DefaultListCellRenderer;
import org.alliance.ui.themes.util.SubstanceThemeHelper;
import org.alliance.ui.themes.AllianceListCellRenderer;

import javax.swing.JList;
import org.alliance.core.Language;
import org.alliance.core.settings.Share;

/**
 *
 * @author Bastvera
 */
public class SharesListCellRenderer extends AllianceListCellRenderer {

    private static String GROUP_TEXT;
    private static String PATH_TEXT;
    private static String INFO_TEXT;
    private boolean showDetails;

    public SharesListCellRenderer(boolean showDetails) {
        super(SubstanceThemeHelper.isSubstanceInUse());
        this.showDetails = showDetails;
        GROUP_TEXT = " --> " + Language.getLocalizedString(getClass(), "group") + " ";
        PATH_TEXT = " --> " + Language.getLocalizedString(getClass(), "path") + " ";
        INFO_TEXT = " --> " + Language.getLocalizedString(getClass(), "info") + " ";
    }

    @Override
    protected void overrideListCellRendererComponent(DefaultListCellRenderer renderer, JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        String path = ((Share) value).getPath();
        int lastDirPosition = path.lastIndexOf("/");
        if (lastDirPosition == -1) {
            lastDirPosition = path.lastIndexOf("\\");
        }
        if (lastDirPosition == -1) {
            return;
        }
        String head = path.substring(lastDirPosition + 1);
        String group = ((Share) value).getSgroupname();
        String info = null;
        if (((Share) value).getExternal() > 0) {
            info = Language.getLocalizedString(getClass(), "external");
        }
        if (info == null) {
            info = Language.getLocalizedString(getClass(), "none");
        }
        if (showDetails) {
            renderer.setText("<html><b>" + head
                    + "</b><font size=-" + 2 + "><br>" + PATH_TEXT + path
                    + "<br>" + GROUP_TEXT + group
                    + "<br>" + INFO_TEXT + info + "</font></html>");
        } else {
            renderer.setText("<html><b>" + head + "</b></html>");
        }
    }
}
