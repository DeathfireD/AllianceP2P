package org.alliance.ui;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.PacedRunner;
import org.alliance.core.node.Friend;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-27
 * Time: 19:50:47
 * To change this template use File | Settings | File Templates.
 */
public class FriendListModel extends DefaultListModel {

    private CoreSubsystem core;
    private UISubsystem ui;
    private boolean ignoreFires;
    private PacedRunner pacedRunner;

    public FriendListModel(CoreSubsystem core, final UISubsystem ui) {
        this.core = core;
        this.ui = ui;

        pacedRunner = new PacedRunner(new Runnable() {

            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        updateFriendList();
                        ui.getMainWindow().getFriendListMDIWindow().update();
                    }
                });
            }
        }, 1000);
        updateFriendList();
    }

    public void updateFriendList() {
        clear();
        ignoreFires = true;
        Collection<Friend> c = new ArrayList<Friend>(core.getFriendManager().friends());

        TreeSet<Friend> ts = new TreeSet<Friend>(new Comparator<Friend>() {

            @Override
            public int compare(Friend o1, Friend o2) {
                if (o1 == null || o2 == null) {
                    return 0;
                }
                String s1 = o1.getNickname();
                String s2 = o2.getNickname();
                if (s1.equalsIgnoreCase(s2)) {
                    return o1.getGuid() - o2.getGuid();
                }
                return o1.getNickname().compareToIgnoreCase(o2.getNickname());
            }
        });

        TreeSet<String> groups = new TreeSet<String>(new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                if (o1.length() == 0 || o1.length() == 0) {
                    return 0;
                }
                if (o1.equalsIgnoreCase(o2)) {
                    return 0;
                }
                return o1.compareToIgnoreCase(o2);
            }
        });

        for (Friend f : c) {
            ts.add(f);
            groups.add(f.getUGroupName());
        }

        addElement(core.getFriendManager().getMe());
        for (String group : groups) {
            if (group.equalsIgnoreCase("")) {
                continue;
            }
            addElement(group);
            drawList(ts, group);
        }
        if (groups.size() > 0) {
            addElement("Not in a Group");
            drawList(ts, "");
        }

        ignoreFires = false;
        fireIntervalAdded(this, 0, size() - 1);
    }

    private void drawList(TreeSet<Friend> ts, String group) {
        int max = core.getFriendManager().getNumberOfInvitesNeededToBeKing();
        for (Friend f : ts) {
            if (f.isConnected() && f.getNumberOfInvitedFriends() >= max && f.getUGroupName().equalsIgnoreCase(group)) {
                addElement(f);
            }
        }
        for (Friend f : ts) {
            if (f.isConnected() && f.getNumberOfInvitedFriends() >= 3 && f.getNumberOfInvitedFriends() < max && f.getUGroupName().equalsIgnoreCase(group)) {
                addElement(f);
            }
        }
        for (Friend f : ts) {
            if (f.isConnected() && f.getNumberOfInvitedFriends() > 0 && f.getNumberOfInvitedFriends() < 3 && f.getUGroupName().equalsIgnoreCase(group)) {
                addElement(f);
            }
        }
        for (Friend f : ts) {
            if (f.isConnected() && f.getNumberOfInvitedFriends() <= 0 && f.getUGroupName().equalsIgnoreCase(group)) {
                addElement(f);
            }
        }
        for (Friend f : ts) {
            if (!f.isConnected() && !f.hasNotBeenOnlineForLongTime() && f.getUGroupName().equalsIgnoreCase(group)) {
                addElement(f);
            }
        }
        for (Friend f : ts) {
            if (!f.isConnected() && f.hasNotBeenOnlineForLongTime() && f.getUGroupName().equalsIgnoreCase(group)) {
                addElement(f);
            }
        }
    }

    public void signalFriendChanged(Friend node) {
        pacedRunner.invoke();
    }

    public void signalFriendAdded(Friend friend) {
        pacedRunner.invoke();
    }

    @Override
    protected void fireContentsChanged(Object source, int index0, int index1) {
        if (ignoreFires) {
            return;
        }
        super.fireContentsChanged(source, index0, index1);
    }

    @Override
    protected void fireIntervalAdded(Object source, int index0, int index1) {
        if (ignoreFires) {
            return;
        }
        super.fireIntervalAdded(source, index0, index1);
    }

    @Override
    protected void fireIntervalRemoved(Object source, int index0, int index1) {
        if (ignoreFires) {
            return;
        }
        super.fireIntervalRemoved(source, index0, index1);
    }
}
