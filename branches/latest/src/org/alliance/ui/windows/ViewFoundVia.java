package org.alliance.ui.windows;

import com.stendahls.XUI.XUIDialog;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;
import org.alliance.ui.UISubsystem;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import org.alliance.core.node.Friend;

public class ViewFoundVia extends XUIDialog {

    private DefaultListModel fofListModel;
    private JList fofList;

    public ViewFoundVia(UISubsystem ui, Friend f) throws Exception {
        super(ui.getMainWindow());

        init(ui.getRl(), ui.getRl().getResourceStream(
                "xui/viewfoundvia.xui.xml"));
        fofList = (JList) xui.getComponent("fofList");
        fofListModel = new DefaultListModel();

        TreeSet<Friend> ts = new TreeSet<Friend>(new Comparator<Friend>() {

            @Override
            public int compare(Friend o1, Friend o2) {
                if (o1 == null || o2 == null) {
                    return 0;
                }
                if (o1.getNickname().equalsIgnoreCase(o2.getNickname())) {
                    return o1.getGuid() - o2.getGuid();
                }
                return o1.getNickname().compareToIgnoreCase(o2.getNickname());
            }
        });

        Collection<Friend> allfriends = ui.getCore().getFriendManager().friends();
        for (Friend friend : allfriends.toArray(new Friend[ui.getCore().getFriendManager().friends().size()])) {
            if (friend.getFriendsFriend(f.getGuid()) != null) {
                ts.add(friend);
            }
        }

        for (Friend friend : ts) {
            fofListModel.addElement(new String(friend.getNickname()));
        }

        fofList.setModel(fofListModel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        display();
    }

    public void EVENT_ok(ActionEvent a) throws Exception {
        dispose();
    }
}
