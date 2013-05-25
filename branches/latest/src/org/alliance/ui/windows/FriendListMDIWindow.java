package org.alliance.ui.windows;

import com.stendahls.nif.ui.OptionDialog;
import com.stendahls.nif.ui.mdi.MDIManager;
import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.util.TextUtils;
import org.alliance.core.node.Friend;
import org.alliance.core.node.MyNode;
import org.alliance.core.node.Node;
import org.alliance.ui.UISubsystem;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.TreeSet;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 16:22:07
 */
public class FriendListMDIWindow extends AllianceMDIWindow {

    private UISubsystem ui;
    private JList list;
    private ImageIcon iconFriendDimmed, iconFriendOld;
    private JLabel statusright;
    private String[] LEVEL_NAMES = {"Rookie", "True Member", "Experienced", "King"};
    private String[] LEVEL_ICONS = {"friend_lame", "friend", "friend_cool", "friend_king"};
    private ImageIcon[] friendIcons;
    private ImageIcon[] friendIconsAway;
    private ImageIcon groupIcon;
    private Object[] selectedObjects;
    private JPopupMenu popup;

    public FriendListMDIWindow() {
    }

    public FriendListMDIWindow(MDIManager manager, UISubsystem ui) throws Exception {
        super(manager, "friendlist", ui);
        this.ui = ui;

        groupIcon = new ImageIcon(ui.getRl().getResource("gfx/icons/editgroup.png"));
        friendIcons = new ImageIcon[LEVEL_ICONS.length];
        friendIconsAway = new ImageIcon[LEVEL_ICONS.length];
        for (int i = 0; i < LEVEL_ICONS.length; i++) {
            friendIcons[i] = new ImageIcon(ui.getRl().getResource("gfx/icons/" + LEVEL_ICONS[i] + ".png"));
            friendIconsAway[i] = new ImageIcon(ui.getRl().getResource("gfx/icons/" + LEVEL_ICONS[i] + "_away.png"));
        }
        iconFriendDimmed = new ImageIcon(ui.getRl().getResource("gfx/icons/friend_dimmed.png"));
        iconFriendOld = new ImageIcon(ui.getRl().getResource("gfx/icons/friend_old.png"));

        setWindowType(WINDOWTYPE_NAVIGATION);

        statusright = (JLabel) xui.getComponent("statusright");

        createUI();
        setTitle("My  Network");
    }

    public void update() {
        statusright.setText("Online: " + ui.getCore().getFriendManager().getNFriendsConnected() + "/" + ui.getCore().getFriendManager().getNFriends() + " (" + TextUtils.formatByteSize(ui.getCore().getFriendManager().getTotalBytesShared()) + ")");
        if (selectedObjects != null) {
            int[] selectedIndexes = new int[list.getModel().getSize()];
            for (int i = 0; i < list.getModel().getSize(); i++) {

                for (Object selection : selectedObjects) {
                    if (list.getModel().getElementAt(i).equals(selection)) {
                        selectedIndexes[i] = i;
                        break;
                    } else {
                        selectedIndexes[i] = -1;
                    }
                }
            }
            list.setSelectedIndices(selectedIndexes);
        }
        try {
            updateMyLevelInformation();
        } catch (IOException ex) {
        }
    }

    static {
        final SystemFlavorMap sfm =
                (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();
        final String nat = "text/plain";
        final DataFlavor df = new DataFlavor("text/plain; charset=ASCII; class=java.io.InputStream", "Plain Text");
        sfm.addUnencodedNativeForFlavor(df, nat);
        sfm.addFlavorForUnencodedNative(nat, df);
    }

    private void createUI() throws Exception {
        list = new JList(ui.getFriendListModel());
        SystemFlavorMap.getDefaultFlavorMap();
        list.setCellRenderer(new FriendListRenderer());
        ((JScrollPane) xui.getComponent("scrollpanel")).setViewportView(list);

        popup = (JPopupMenu) xui.getComponent("popup");
        updateMyLevelInformation();
        statusright.setText("Online: " + ui.getCore().getFriendManager().getNFriendsConnected() + "/" + ui.getCore().getFriendManager().getNFriends() + " (" + TextUtils.formatByteSize(ui.getCore().getFriendManager().getTotalBytesShared()) + ")");

        list.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    EVENT_viewshare(null);
                } catch (Exception ex) {
                    ui.handleErrorInEventLoop(ex);
                }
                selectedObjects = list.getSelectedValues();
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = list.locationToIndex(e.getPoint());
                    boolean b = false;
                    for (int r : list.getSelectedIndices()) {
                        if (r == row) {
                            b = true;
                            break;
                        }
                    }
                    if (!b) {
                        list.getSelectionModel().setSelectionInterval(row, row);
                    }
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        postInit();
    }

    public void updateMyLevelInformation() throws IOException {
        ((JLabel) xui.getComponent("myname")).setText(ui.getCore().getFriendManager().getMe().getNickname());
        ((JLabel) xui.getComponent("mylevel")).setText(getLevelName(getMyLevel()));
        ((JLabel) xui.getComponent("myicon")).setIcon(new ImageIcon(ui.getRl().getResource(getLevelIcon(getMyLevel(), true))));
        String s = "";
        switch (getMyNumberOfInvites()) {
            case 0:
                s = "Invite 1 friend to become ";
                break;
            case 1:
                s = "Invite 2 friends to become ";
                break;
            case 2:
                s = "Invite 1 friend to become ";
                break;
            default:
                s = "Invite " + (ui.getCore().getFriendManager().getNumberOfInvitesNeededToBeKing() - getMyNumberOfInvites()) + " friend to become ";
                break;
        }
        if (getMyLevel() < LEVEL_NAMES.length - 1) {
            s += "'" + getLevelName(getMyLevel() + 1) + "' (";
            ((JLabel) xui.getComponent("nextLevelText")).setText(s);
            ((JLabel) xui.getComponent("nextLevelIcon")).setIcon(new ImageIcon(ui.getRl().getResource(getLevelIcon(getMyLevel() + 1, false))));
            ((JLabel) xui.getComponent("levelEnding")).setText(")");
        } else {
            ((JLabel) xui.getComponent("nextLevelText")).setText("");
            ((JLabel) xui.getComponent("nextLevelIcon")).setText("");
            ((JLabel) xui.getComponent("nextLevelIcon")).setIcon(null);
            ((JLabel) xui.getComponent("levelEnding")).setText("");
        }
    }

    private String getLevelIcon(int myLevel, boolean big) {
        if (myLevel < 0) {
            myLevel = 0;
        }
        if (myLevel >= LEVEL_ICONS.length) {
            myLevel = LEVEL_ICONS.length - 1;
        }
        return "gfx/icons/" + LEVEL_ICONS[myLevel] + (big ? "_big" : "") + ".png";
    }

    private String getLevelName(int myLevel) {
        if (myLevel < 0) {
            myLevel = 0;
        }
        if (myLevel >= LEVEL_NAMES.length) {
            myLevel = LEVEL_NAMES.length - 1;
        }
        return LEVEL_NAMES[myLevel];
    }

    private int getMyLevel() {
        return getLevel(getMyNumberOfInvites());
    }

    private int getLevel(int numberOfInvites) {
        switch (numberOfInvites) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 1;
            case 3:
                return 2;
            default:
                if (numberOfInvites >= ui.getCore().getFriendManager().getNumberOfInvitesNeededToBeKing()) {
                    return 3;
                }
                return 2;
        }
    }

    private int getMyNumberOfInvites() {
        return ui.getCore().getSettings().getMy().getInvitations();
    }

    @Override
    public void save() throws Exception {
    }

    @Override
    public String getIdentifier() {
        return "friendlist";
    }

    @Override
    public void revert() throws Exception {
        ui.getCore().invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    ui.getCore().refreshFriendInfo();
                } catch (IOException e) {
                    ui.handleErrorInEventLoop(e);
                }
            }
        });
    }

    @Override
    public void serialize(ObjectOutputStream out) throws IOException {
    }

    @Override
    public MDIWindow deserialize(ObjectInputStream in) throws IOException {
        return null;
    }

//    private static int cnt = 0;
    private class FriendListRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            //Groups painting
            if (value instanceof String) {
                setIcon(groupIcon);
                setFont(new Font(this.getFont().getFontName(), Font.BOLD, 12));
                setText(value.toString());
                setBackground(new Color(230, 230, 233));
                return this;
            }

            Node n = (Node) value;

            //Bastvera for displaying group names/trusted status in friend list + popup
            String groupname = "";
            String trusted = "";
            String sharesize = TextUtils.formatByteSize(n.getShareSize());
            if (n instanceof Friend) {
                Friend f = (Friend) n;
                groupname = f.getUGroupName();
                if (groupname.length() == 0) {
                    groupname = "Not in a Group";
                }
                if (f.getTrusted() == 1) {
                    trusted = "(T) ";
                }
            } else {
                groupname = "Not Allowed";
            }

            if (n.isConnected()) {

                if (!n.isAway()) {
                    setIcon(friendIcons[getLevel(n.getNumberOfInvitedFriends())]);
                } else {
                    setIcon(friendIconsAway[getLevel(n.getNumberOfInvitedFriends())]);
                }
                if (isSelected) {
                    setForeground(Color.white);
                } else {
                    setForeground(Color.black);
                }
//                setText(f.getNickname()+" ("+ TextUtils.formatByteSize(f.getShareSize())+")");
                String s = "";
                if (n instanceof Friend) {
                    s = trusted + nickname(n.getGuid());
                    //s += FriendListMDIWindow.this.ui.getCore().getFriendManager().contactPath(n.getGuid());
                } else {
                    s = "[Myself] - ";
                    s += n.getNickname();
                }
                s += " (" + sharesize + ")";
                setText(s);
            } else if (n.hasNotBeenOnlineForLongTime()) {
                setIcon(iconFriendOld);
                setForeground(Color.lightGray);
                if (n.getLastSeenOnlineAt() != 0) {
                    setText(trusted + nickname(n.getGuid()) + " (offline for " +
                            ((System.currentTimeMillis() - n.getLastSeenOnlineAt()) / 1000 / 60 / 60 / 24) + " days)");
                } else {
                    setText(trusted + nickname(n.getGuid()));
                }
            } else {
                setIcon(iconFriendDimmed);
                setForeground(Color.lightGray);
                setText(trusted + nickname(n.getGuid()));
            }

            String cp = FriendListMDIWindow.this.ui.getCore().getFriendManager().contactPath(n.getGuid());
            if (cp.trim().length() > 0) {
                cp = "Found " + cp + "<br>";
            }
            setToolTipText("<html>" + cp +
                    "Share: " + sharesize + " in " + n.getNumberOfFilesShared() + " files<br>" +
                    "Invited friends: " + n.getNumberOfInvitedFriends() + "<br>" +
                    "Upload speed record: " + TextUtils.formatByteSize((long) n.getHighestOutgoingCPS()) + "/s<br>" +
                    "Download speed record: " + TextUtils.formatByteSize((long) n.getHighestIncomingCPS()) + "/s<br>" +
                    "Bytes uploaded: " + TextUtils.formatByteSize(n.getTotalBytesSent()) + "<br>" +
                    "Bytes downloaded: " + TextUtils.formatByteSize(n.getTotalBytesReceived()) + "<br>" +
                    "Ratio (ul:dl): " + n.calculateRatio() + "<br>" +
                    "Group name: " + groupname + "</html>");
            return this;
        }
    }

    private String nickname(int guid) {
        return ui.getCore().getFriendManager().nickname(guid);
    }

    public void EVENT_editname(ActionEvent e) {
        if (list.getSelectedValue() instanceof MyNode) {
            OptionDialog.showInformationDialog(ui.getMainWindow(), "If you want to change your nickname you need to open the Options (View->Options)");
        } else if (list.getSelectedValue() instanceof Friend) {
            Friend f = (Friend) list.getSelectedValue();
            if (f != null) {
                String pi = JOptionPane.showInputDialog("Enter nickname for friend: " + nickname(f.getGuid()), nickname(f.getGuid()));
                if (pi != null) {
                    f.setNicknameToShowInUI(pi);
                }
                ui.getFriendListModel().signalFriendChanged(f);
            }
        } else {
            return;
        }
    }

    public void EVENT_chat(ActionEvent e) throws Exception {
        if (list.getSelectedValue() instanceof Friend) {
            Friend f = (Friend) list.getSelectedValue();
            if (f != null) {
                ui.getMainWindow().chatMessage(f.getGuid(), null, 0, false);
            }
        } else {
            return;
        }
    }

    public void EVENT_reconnect(ActionEvent e) throws Exception {
        if (list.getSelectedValue() instanceof Friend) {
            final Friend f = (Friend) list.getSelectedValue();
            if (f.isConnected()) {
                f.reconnect();
            }
        } else {
            return;
        }
    }

    public void EVENT_viewshare(ActionEvent e) throws Exception {
        if (list.getSelectedValue() instanceof MyNode) {
            ui.getMainWindow().EVENT_myshare(null);
        } else if (list.getSelectedValue() instanceof Friend) {
            Friend f = (Friend) list.getSelectedValue();
            if (f != null) {
                if (!f.isConnected()) {
//                  just ignore the request
//                OptionDialog.showErrorDialog(ui.getMainWindow(), "User must be online in order to view his share.");
                } else {
                    ui.getMainWindow().viewShare(f);
                }
            }
        } else {
            return;
        }
    }

    public void EVENT_addfriendwizard(ActionEvent e) throws Exception {
        ui.getMainWindow().EVENT_addfriendwizard(e);
    }

    public void EVENT_removefriend(ActionEvent e) throws Exception {
        if (list.getSelectedValue() instanceof Friend) {
            Object[] friends = list.getSelectedValues();
            if (friends != null && friends.length > 0) {
                Boolean delete = OptionDialog.showQuestionDialog(ui.getMainWindow(), "Are you sure you want to permanently delete these (" + friends.length + ") connections?");
                if (delete == null) {
                    return;
                }
                if (delete) {
                    for (Object friend : friends) {
                        if (friend instanceof Friend) {
                            Node f = (Node) friend;
                            if (f != null && f instanceof Friend) {
                                ui.getCore().getFriendManager().permanentlyRemove((Friend) f);
                            }
                        }
                    }
                    revert();
                }
            } else {
                return;
            }
        }
    }

    /**
     * Changes the hostname of a friend you have in your friendlist via the GUI. Can
     * be used to configure a hostname instead of the IP that's set via an invitation.
     * @param e
     * @author jpluebbert
     */
    public void EVENT_edithostname(ActionEvent e) {
        if (list.getSelectedValue() instanceof Friend) {
            Friend friend = (Friend) list.getSelectedValue();
            if (friend != null) {
                String hostname = friend.getLastKnownHost();

                if (hostname == null) {
                    hostname = "";
                }

                String input = JOptionPane.showInputDialog("Enter hostname for friend: " + nickname(friend.getGuid()), hostname);
                if (input != null && !hostname.equalsIgnoreCase(input)) {
                    if (input.length() == 0) {
                        OptionDialog.showErrorDialog(ui.getMainWindow(), "Hostname can not be empty. Changes ignored.");
                    } else {
                        friend.setLastKnownHost(input);
                        try {
                            if (friend.isConnected()) {
                                friend.reconnect();
                            } else {
                                friend.connect();
                            }
                        } catch (IOException e1) {
                        }
                    }
                }
            } else {
                return;
            }
        }
    }

    public void EVENT_edittrusted(ActionEvent e) {
        if (list.getSelectedValue() instanceof Friend) {
            Object[] friends = list.getSelectedValues();
            if (friends != null && friends.length > 0) {
                for (Object friend : friends) {
                    if (friend instanceof Friend) {
                        Friend f = (Friend) friend;
                        if (f != null) {
                            if (f.getTrusted() == 0) {
                                f.setTrusted(1);
                            } else {
                                f.setTrusted(0);
                            }
                        }
                    }
                }
                try {
                    ui.getCore().saveSettings();
                } catch (Exception ex) {
                }
            }
        } else {
            return;
        }
    }

    public void EVENT_editgroupname(ActionEvent e) { //Editing friends group names
        if (list.getSelectedValue() instanceof Friend) {
            Object[] friends = list.getSelectedValues();
            if (friends != null && friends.length > 0) {
                String groupname = JOptionPane.showInputDialog("Edit group name for (" + friends.length + ") friend.\nDivide multiple group names with commas.\nExample 1: music,games\nExample 2: music,games,private", ((Friend) friends[0]).getUGroupName());
                if (groupname == null) {
                    return;
                }
                if (groupname.trim().length() == 0) {
                    groupname = "";
                } else {
                    TreeSet<String> groupSort = new TreeSet<String>();
                    String[] dividegroup = groupname.split(",");
                    for (String group : dividegroup) {
                        if (group.trim().length() > 0) {
                            groupSort.add(group.trim().toUpperCase().substring(0, 1) + group.trim().toLowerCase().substring(1));//Uppercase 1st letter rest Lowercase
                        } else if (group.trim().length() == 1) {
                            groupSort.add(group.trim().toUpperCase());
                        }
                    }
                    groupname = "";
                    for (String group : groupSort) {
                        groupname += group.trim() + ",";
                    }
                    if (groupname.lastIndexOf(",") == groupname.length() - 1 && groupname.length() > 0) {
                        groupname = groupname.substring(0, groupname.length() - 1);
                    }
                }
                for (Object friend : friends) {
                    if (friend instanceof Friend) {
                        Friend f = (Friend) friend;
                        if (f != null) {
                            f.setUGroupName(groupname);
                        }
                    }
                }
                try {
                    ui.getCore().saveSettings();
                    revert();
                } catch (Exception ex) {
                }
            }
        } else {
            return;
        }
    }

    public void EVENT_viewvia(ActionEvent e) {
        if (list.getSelectedValue() instanceof Friend) {
            Friend friend = (Friend) list.getSelectedValue();
            try {
                new ViewFoundVia(ui, friend);
            } catch (Exception ex) {
            }
        } else {
            return;
        }
    }

    public void EVENT_searchfriend(ActionEvent e) {
        String text = ((JTextField) xui.getComponent("searchfield")).getText();
        int nhit = 0;
        for (int i = 0; i < list.getModel().getSize(); i++) {
            Object friend = list.getModel().getElementAt(i);
            if (friend instanceof Friend) {
                for (int x = 1; x <= text.length(); x++) {
                    if (((Friend) friend).getNickname().toLowerCase().startsWith(text.substring(0, x).toLowerCase()) && x > nhit) {
                        list.setSelectedValue(friend, true);
                        selectedObjects = list.getSelectedValues();
                        nhit++;
                    }
                }
            }
        }       
    }
}
