package org.alliance.ui.windows.mdi.friends;

import com.stendahls.util.TextUtils;
import org.alliance.core.Language;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.themes.util.SubstanceThemeHelper;
import org.alliance.ui.themes.AllianceListCellRenderer;
import static org.alliance.ui.windows.mdi.friends.FriendListMDIWindow.LEVEL_ICONS;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.io.IOException;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author Bastvera
 */
public class FriendListCellRenderer extends AllianceListCellRenderer {

    private ImageIcon groupIcon;
    private ImageIcon iconFriendDimmed, iconFriendOld;
    private ImageIcon[] friendIcons;
    private ImageIcon[] friendIconsAway;
    private FriendListMDIWindow friendWindow;
    private UISubsystem ui;
    private static Color GROUPS_BG;
    private static Font GROUPS_FONT;
    private static Border MARGIN_BORDER;

    FriendListCellRenderer(FriendListMDIWindow friendWindow, UISubsystem ui) throws IOException {
        super(SubstanceThemeHelper.isSubstanceInUse());
        this.friendWindow = friendWindow;
        this.ui = ui;
        groupIcon = new ImageIcon(ui.getRl().getResource("gfx/icons/editgroup.png"));
        iconFriendDimmed = new ImageIcon(ui.getRl().getResource("gfx/icons/friend_dimmed.png"));
        iconFriendOld = new ImageIcon(ui.getRl().getResource("gfx/icons/friend_old.png"));
        friendIcons = new ImageIcon[LEVEL_ICONS.length];
        friendIconsAway = new ImageIcon[LEVEL_ICONS.length];
        for (int i = 0; i < LEVEL_ICONS.length; i++) {
            friendIcons[i] = new ImageIcon(ui.getRl().getResource("gfx/icons/" + LEVEL_ICONS[i] + ".png"));
            friendIconsAway[i] = new ImageIcon(ui.getRl().getResource("gfx/icons/" + LEVEL_ICONS[i] + "_away.png"));
        }
        GROUPS_FONT = new Font(getRenderer().getFont().getFontName(), Font.BOLD, getRenderer().getFont().getSize());
        GROUPS_BG = createGroupBackground(getRenderer().getBackground().getRed(), getRenderer().getBackground().getGreen(), getRenderer().getBackground().getBlue(), 0.85);
        if (getRenderer().getBorder() instanceof EmptyBorder) {
            Insets i = ((EmptyBorder) getRenderer().getBorder()).getBorderInsets();
            MARGIN_BORDER = new EmptyBorder(i.top, 17, i.bottom, i.right);
        } else {
            MARGIN_BORDER = getRenderer().getBorder();
        }
    }

    @Override
    protected void overrideListCellRendererComponent(DefaultListCellRenderer renderer, JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value instanceof String) {
            paintGroup(renderer, value.toString());
            return;
        }
        Node n = (Node) value;
        paintFriend(renderer, n, isSelected);
        renderer.setToolTipText(setupTooltip(n));
        if (n instanceof Friend) {
            renderer.setBorder(MARGIN_BORDER);
        }
    }

    private Color createGroupBackground(int r, int g, int b, double factor) {
        return new Color(Math.max((int) (r * factor), 0), Math.max((int) (g * factor), 0), Math.max((int) (b * factor), 0));
    }

    private void paintGroup(DefaultListCellRenderer renderer, String group) {
        renderer.setIcon(groupIcon);
        renderer.setFont(GROUPS_FONT);
        renderer.setText(group);
        renderer.setBackground(GROUPS_BG);
    }

    private void paintFriend(DefaultListCellRenderer renderer, Node n, boolean isSelected) {
        String trusted;
        if (n instanceof Friend) {
            Friend f = (Friend) n;
            if (f.getTrusted() == 1) {
                trusted = "(T) ";
            } else {
                trusted = "";
            }
        } else {
            trusted = "";
        }

        if (n.isConnected()) {
            if (!n.isAway()) {
                renderer.setIcon(friendIcons[friendWindow.getLevel(n.getNumberOfInvitedFriends())]);
            } else {
                renderer.setIcon(friendIconsAway[friendWindow.getLevel(n.getNumberOfInvitedFriends())]);
            }
            if (isSelected) {
                renderer.setForeground(Color.white);
            } else {
                renderer.setForeground(Color.black);
            }
            String nodeString;
            if (n instanceof Friend) {
                nodeString = trusted + friendWindow.getNickname(n.getGuid());
            } else {
                nodeString = Language.getLocalizedString(getClass(), "myself") + " - ";
                nodeString += n.getNickname();
            }
            nodeString += " (" + TextUtils.formatByteSize(n.getShareSize()) + ")";
            renderer.setText(nodeString);
        } else if (n.hasNotBeenOnlineForLongTime()) {
            renderer.setIcon(iconFriendOld);
            renderer.setForeground(Color.GRAY);
            if (n.getLastSeenOnlineAt() != 0) {
                renderer.setText(trusted + Language.getLocalizedString(getClass(), "seen",
                        friendWindow.getNickname(n.getGuid()), Long.toString((System.currentTimeMillis() - n.getLastSeenOnlineAt()) / 1000 / 60 / 60 / 24)));
            } else {
                renderer.setText(trusted + friendWindow.getNickname(n.getGuid()));
            }
        } else {
            renderer.setIcon(iconFriendDimmed);
            renderer.setForeground(Color.GRAY);
            renderer.setText(trusted + friendWindow.getNickname(n.getGuid()));
        }
    }

    private String setupTooltip(Node n) {
        String groupname;
        if (n instanceof Friend) {
            Friend f = (Friend) n;
            groupname = f.getUGroupName();
            if (groupname.isEmpty()) {
                groupname = Language.getLocalizedString(getClass(), "nogroup");
            }
        } else {
            groupname = Language.getLocalizedString(getClass(), "wronggroup");
        }

        String cp = ui.getCore().getFriendManager().contactPath(n.getGuid());//TODO Cache This and delay refresh
        StringBuilder sb = new StringBuilder("<html>");
        if (cp.trim().length() > 0) {
            sb.append(Language.getLocalizedString(getClass(), "subfriends", cp)).append("<br>");
        }
        sb.append(Language.getLocalizedString(getClass(), "share", TextUtils.formatByteSize(n.getShareSize()),
                Integer.toString(n.getNumberOfFilesShared()))).append("<br>");
        sb.append(Language.getLocalizedString(getClass(), "invites", Integer.toString(n.getNumberOfInvitedFriends()))).append("<br>");
        sb.append(Language.getLocalizedString(getClass(), "upspeed", TextUtils.formatByteSize((long) n.getHighestOutgoingCPS()))).append("<br>");
        sb.append(Language.getLocalizedString(getClass(), "downspeed", TextUtils.formatByteSize((long) n.getHighestIncomingCPS()))).append("<br>");
        sb.append(Language.getLocalizedString(getClass(), "uptotal", TextUtils.formatByteSize(n.getTotalBytesSent()))).append("<br>");
        sb.append(Language.getLocalizedString(getClass(), "downtotal", TextUtils.formatByteSize(n.getTotalBytesReceived()))).append("<br>");
        sb.append(Language.getLocalizedString(getClass(), "ratio", n.calculateRatio())).append("<br>");
        sb.append(Language.getLocalizedString(getClass(), "group", groupname)).append("</html>");
        return sb.toString();
    }
}
