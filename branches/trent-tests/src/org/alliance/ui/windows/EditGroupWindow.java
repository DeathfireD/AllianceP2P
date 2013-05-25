package org.alliance.ui.windows;

import com.stendahls.XUI.XUIDialog;
import org.alliance.core.settings.Friend;
import org.alliance.core.settings.Share;
import org.alliance.core.Language;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.dialogs.AddGroupDialog;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;

/**
 *
 * @author Bastvera
 */
public class EditGroupWindow extends XUIDialog {

    public String groupString;
    private DefaultListModel groupListModel;
    private JList groupList;
    private UISubsystem ui;
    private TreeSet<String> groupsTree;
    private String groupsOfItem;
    private final static String PUBLIC_GROUP = "Public";
    private final static String GROUP_SEPARATOR = ",";

    public EditGroupWindow(UISubsystem ui, String groupsOfItem) throws Exception {
        this(ui, groupsOfItem, null);
    }

    public EditGroupWindow(UISubsystem ui, String groupsOfItem, ArrayList<String> customGroups) throws Exception {
        super(ui.getMainWindow());
        this.ui = ui;
        this.groupsOfItem = groupsOfItem;

        init(ui.getRl(), ui.getRl().getResourceStream("xui/editgroupwindow.xui.xml"));
        Language.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        setTitle(Language.getLocalizedString(getClass(), "title"));
        groupList = (JList) xui.getComponent("groupList");

        groupListModel = new DefaultListModel();

        groupsTree = new TreeSet<String>(new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                if (o1 == null || o2 == null) {
                    return 0;
                }
                String s1 = o1.toString();
                String s2 = o2.toString();
                return s1.compareToIgnoreCase(s2);
            }
        });
        for (Share share : ui.getCore().getSettings().getSharelist()) {
            for (String group : share.getSgroupname().split(GROUP_SEPARATOR)) {
                if (!group.equals(PUBLIC_GROUP) && !group.isEmpty()) {
                    groupsTree.add(formatGroupNames(group));
                }
            }
        }
        for (Friend friend : ui.getCore().getSettings().getFriendlist()) {
            for (String group : friend.getUgroupname().split(GROUP_SEPARATOR)) {
                if (!group.equals(PUBLIC_GROUP) && !group.isEmpty()) {
                    groupsTree.add(formatGroupNames(group));
                }
            }
        }

        if (customGroups != null) {
            for (String group : customGroups) {
                groupsTree.add(group);
            }
        }

        for (String group : groupsTree) {
            groupListModel.addElement(group);
        }

        groupList.setModel(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        groupList.setSelectionModel(new DefaultListSelectionModel() {

            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (isSelectedIndex(index0)) {
                    super.removeSelectionInterval(index0, index1);
                } else {
                    super.addSelectionInterval(index0, index1);
                }
            }
        });

        selectGroups();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        display();
    }

    private void reFillList() {
        groupListModel.clear();
        for (String group : groupsTree) {
            groupListModel.addElement(group);
        }
    }

    private String createNewGroup() throws Exception {
        AddGroupDialog groupDialog = new AddGroupDialog(ui, null);
        String group = groupDialog.getGroupName();
        if (group == null || group.trim().length() == 0) {
            return null;
        }
        if (group.contains(GROUP_SEPARATOR)) {
            group = group.replace(GROUP_SEPARATOR, "");
        }
        return group.trim();
    }

    private String formatGroupNames(String groupString) {
        TreeSet<String> groupSorted = new TreeSet<String>();
        String[] groupsSplit = groupString.split(GROUP_SEPARATOR);
        for (String group : groupsSplit) {
            group = group.trim().toLowerCase();
            if (group.length() > 1) {
                //Uppercase 1st letter rest Lowercase
                groupSorted.add(Character.toUpperCase(group.charAt(0)) + group.substring(1));
            } else if (group.length() == 1) {
                groupSorted.add(group.toUpperCase());
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String group : groupSorted) {
            sb.append(group.trim());
            sb.append(GROUP_SEPARATOR);
        }
        try {
            sb.deleteCharAt(sb.length() - 1);
        } catch (StringIndexOutOfBoundsException ex) {
            //Nothing selected
            return "";
        }
        return sb.toString();
    }

    private void selectGroups() {
        ArrayList<Integer> selectionList = new ArrayList<Integer>();
        for (int i = 0; i < groupListModel.getSize(); i++) {
            for (String group : groupsOfItem.split(GROUP_SEPARATOR)) {
                if (group.equals(PUBLIC_GROUP)) {
                    continue;
                }
                if (groupListModel.get(i).toString().equalsIgnoreCase(group)) {
                    selectionList.add(i);
                    break;
                }
            }
        }
        int[] selectedIndices = new int[selectionList.size()];
        for (int i = 0; i < selectedIndices.length; i++) {
            selectedIndices[i] = selectionList.get(i);
        }
        groupList.setSelectedIndices(selectedIndices);
    }

    private void constructGroupString() {
        String groups = "";
        for (Object group : groupList.getSelectedValues()) {
            groups += group + ",";
        }
        groupString = formatGroupNames(groups);
    }

    public String getGroupString() {
        return groupString;
    }

    public ArrayList<String> getAllGroups() {
        ArrayList<String> allGroups = new ArrayList<String>();
        for (String group : groupsTree) {
            allGroups.add(group);
        }
        return allGroups;
    }

    public void EVENT_addnew(ActionEvent e) throws Exception {
        String group = createNewGroup();
        if (group != null) {
            groupsTree.add(group);
            reFillList();
            groupsOfItem = formatGroupNames(groupsOfItem + "," + group);
            selectGroups();
        }
    }

    public void EVENT_ok(ActionEvent e) throws Exception {
        constructGroupString();
        dispose();
    }

    public void EVENT_cancel(ActionEvent e) throws Exception {
        dispose();
    }
}
