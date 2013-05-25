package org.alliance.ui.addfriendwizard;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.node.Friend;
import org.alliance.core.node.UntrustedNode;
import org.alliance.core.node.Invitation;
import org.alliance.core.Language;
import org.alliance.ui.UISubsystem;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.Collection;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-apr-19
 * Time: 12:39:16
 * To change this template use File | Settings | File Templates.
 */
public class ForwardInvitationNodesList extends JList {

    private UISubsystem ui;
    private AddFriendWizard addFriendWizard;

    public ForwardInvitationNodesList(UISubsystem ui, final AddFriendWizard addFriendWizard) {
        super(new ForwardInvitationListModel(ui.getCore()));
        this.ui = ui;
        this.addFriendWizard = addFriendWizard;

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                if (index == -1) {
                    return;
                }
                ListRow item = (ListRow) getModel().getElementAt(index);
                item.selected = !item.selected;
                if (item.selected) {
                    addFriendWizard.enableNext();
                }
                Rectangle rect = getCellBounds(index, index);
                repaint(rect);
            }
        });
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setCellRenderer(new CheckListRenderer());
    }

    public void selectAll() {
        for (int i = 0; i < getModel().getSize(); i++) {
            ((ListRow) getModel().getElementAt(i)).selected = true;
            addFriendWizard.enableNext();
        }
        repaint();
    }

    public void selectNone() {
        for (int i = 0; i < getModel().getSize(); i++) {
            ((ListRow) getModel().getElementAt(i)).selected = false;
            addFriendWizard.enableNext();
        }
        repaint();
    }

    public void selectTrusted() {
        for (int i = 0; i < getModel().getSize(); i++) {
            if (((ListRow) getModel().getElementAt(i)).trusted == 1) {
                ((ListRow) getModel().getElementAt(i)).selected = true;
            } else {
                ((ListRow) getModel().getElementAt(i)).selected = false;
            }
            addFriendWizard.enableNext();
        }
        repaint();
    }

    private static class CheckListRenderer extends JCheckBox implements ListCellRenderer {

        public CheckListRenderer() {
            setBackground(UIManager.getColor("List.textBackground"));
            setForeground(UIManager.getColor("List.textForeground"));
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean hasFocus) {
            setEnabled(list.isEnabled());
            setSelected(((ListRow) value).selected);
            setFont(list.getFont());
            setText(value.toString());
            return this;
        }
    }

    public static class ListRow {

        public String nickname, connectedThrough;
        public int guid;
        public String toString;
        public boolean selected;
        public int trusted;

        public ListRow(String nickname, String connectedThrough, int guid, int trusted) {
            this.nickname = nickname;
            this.connectedThrough = connectedThrough;
            this.guid = guid;
            this.trusted = trusted;
            toString = "<html>" + Language.getLocalizedString(getClass().getEnclosingClass(), "friend", nickname + " <font color=gray>", connectedThrough) + "</font></html>";
        }

        @Override
        public String toString() {
            return toString;
        }
    }

    public static class ForwardInvitationListModel extends DefaultListModel {

        private CoreSubsystem core;

        public ForwardInvitationListModel(final CoreSubsystem core) {
            this.core = core;

            TreeSet<Integer> secondaryNodeGuids = new TreeSet<Integer>(new Comparator<Integer>() {

                @Override
                public int compare(Integer o1, Integer o2) {
                    String n1 = core.getFriendManager().nickname(o1);
                    String n2 = core.getFriendManager().nickname(o2);
                    if (n1.compareToIgnoreCase(n2) == 0) {
                        return o1.compareTo(o2);
                    } else {
                        return n1.compareToIgnoreCase(n2);
                    }
                }
            });
            Collection<Friend> friends = core.getFriendManager().friends();
            for (Friend f : friends.toArray(new Friend[friends.size()])) {
                if (f.friendsFriends() != null) {
                    Collection<UntrustedNode> ff = f.friendsFriends();
                    for (UntrustedNode n : ff.toArray(new UntrustedNode[ff.size()])) {
                        if (core.getFriendManager().getFriend(n.getGuid()) == null
                                && core.getFriendManager().getMyGUID() != n.getGuid()) {
                            secondaryNodeGuids.add(n.getGuid());
                        }
                    }
                }
            }

            removeDoubledInvitation(secondaryNodeGuids);
            for (int guid : secondaryNodeGuids) {
                addElement(new ListRow(core.getFriendManager().nickname(guid), createConnectedThroughList(guid, friends), guid, checkTrusted(guid, friends)));
            }
        }

        private void removeDoubledInvitation(TreeSet<Integer> secondaryNodeGuids) {
            Collection<Invitation> invitations = core.getInvitationManager().allInvitations();
            for (Invitation i : invitations.toArray(new Invitation[invitations.size()])) {
                if (i.getDestinationGuid() == null) {
                    continue;
                }
                if (core.getInvitationManager().hasBeenRecentlyInvited(i)) {
                    secondaryNodeGuids.remove(i.getDestinationGuid()); //often the guid won't exist in the list - that's fine.
                }
            }
        }

        private String createConnectedThroughList(int guid, Collection<Friend> friends) {
            String s = "";
            for (Friend f : friends.toArray(new Friend[friends.size()])) {
                if (f.getFriendsFriend(guid) != null) {
                    if (s.length() > 0) {
                        s += ", ";
                    }
                    s += f.getNickname();
                }
            }
            return s;
        }

        private int checkTrusted(int guid, Collection<Friend> friends) {
            for (Friend f : friends.toArray(new Friend[friends.size()])) {
                if (f.getFriendsFriend(guid) != null && f.getTrusted() == 1) {
                    return f.getTrusted();
                }
            }
            return 0;
        }
    }

    public void forwardSelectedInvitations() {
        for (int i = 0; i < getModel().getSize(); i++) {
            final ListRow r = (ListRow) getModel().getElementAt(i);
            if (r.selected) {
                ui.getCore().invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            ui.getCore().getFriendManager().forwardInvitationTo(r.guid);
                        } catch (Exception e) {
                            ui.getCore().reportError(e, this);
                        }
                    }
                });
            }
        }
    }
}
