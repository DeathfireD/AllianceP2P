package org.alliance.ui.windows;

import com.stendahls.XUI.XUIDialog;
import com.stendahls.nif.ui.OptionDialog;
import com.stendahls.nif.util.EnumerationIteratorConverter;
import com.stendahls.ui.JHtmlLabel;
import com.stendahls.util.TextUtils;
import static org.alliance.core.CoreSubsystem.KB;
import org.alliance.core.node.Friend;
import org.alliance.core.settings.My;
import org.alliance.core.settings.Routerule;
import org.alliance.core.settings.SettingClass;
import org.alliance.core.settings.Settings;
import org.alliance.core.settings.Share;
import org.alliance.launchers.ui.Main;
import org.alliance.ui.T;
import org.alliance.ui.UISubsystem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.TreeSet;

/**
 * Created by IntelliJ IDEA. User: maciek Date: 2006-mar-20 Time: 22:33:46 To
 * change this template use File | Settings | File Templates.
 */
public class OptionsWindow extends XUIDialog {

    private final static String[] OPTIONS = new String[]{
        "internal.uploadthrottle", "internal.hashspeedinmbpersecond",
        "internal.politehashingwaittimeinminutes",
        "internal.politehashingintervalingigabytes", "my.nickname",
        "server.port", "internal.downloadfolder",
        "internal.alwaysallowfriendstoconnect",
        "internal.alwaysallowfriendsoffriendstoconnecttome",
        "internal.invitationmayonlybeusedonce", "internal.encryption",
        "internal.showpublicchatmessagesintray",
        "internal.showprivatechatmessagesintray",
        "internal.showsystemmessagesintray",
        "internal.rescansharewhenalliancestarts",
        "internal.enablesupportfornonenglishcharacters",
        "internal.alwaysautomaticallyconnecttoallfriendsoffriend",
        "server.lansupport", "internal.automaticallydenyallinvitations",
        "internal.enableiprules", "server.dnsname",
        "internal.disablenewuserpopup",
        "internal.alwaysallowfriendsoftrustedfriendstoconnecttome",
        "internal.alwaysdenyuntrustedinvitations", "internal.rdnsname",
        "internal.pmsound", "internal.downloadsound", "internal.publicsound",
        "internal.guiskin"};
    private UISubsystem ui;
    private HashMap<String, JComponent> components = new HashMap<String, JComponent>();
    private JList shareList;
    private JList ruleList;
    private DefaultListModel shareListModel;
    private DefaultListModel groupNamesModel;
    private DefaultListModel viewModel;
    private DefaultListModel ruleListModel;
    private boolean shareListHasBeenModified = false;
    private JTextField nickname, downloadFolder;
    private boolean openedWithUndefiniedNickname;
    private int uploadThrottleBefore;

    public OptionsWindow(UISubsystem ui) throws Exception {
        this(ui, false);
    }

    public OptionsWindow(final UISubsystem ui, boolean startInShareTab) throws Exception {
        super(ui.getMainWindow());
        this.ui = ui;

        init(ui.getRl(), ui.getRl().getResourceStream("xui/optionswindow.xui.xml"));

        xui.getComponent("server.port").setEnabled(false);

        nickname = (JTextField) xui.getComponent("my.nickname");
        downloadFolder = (JTextField) xui.getComponent("internal.downloadfolder");
        shareList = (JList) xui.getComponent("shareList");
        ruleList = (JList) xui.getComponent("ruleList");
        shareListModel = new DefaultListModel();
        viewModel = new DefaultListModel(); //Model for view: groupname name + path
        groupNamesModel = new DefaultListModel(); //Bastvera (Model for groupname names synchronized with folder path names)
        ruleListModel = new DefaultListModel();
        for (Share share : ui.getCore().getSettings().getSharelist()) {
            shareListModel.addElement(share.getPath());
            groupNamesModel.addElement(share.getSgroupname());
            viewModel.addElement("[" + share.getSgroupname() + "] " + share.getPath());
        }

        shareList.setModel(viewModel);

        for (Routerule rule : ui.getCore().getSettings().getRulelist()) {
            ruleListModel.addElement(rule);
        }
        ruleList.setModel(ruleListModel);

        openedWithUndefiniedNickname = ui.getCore().getSettings().getMy().getNickname().equals(My.UNDEFINED_NICKNAME);

        if (ui.getCore().getUpnpManager().isPortForwardSuccedeed()) {
            ((JHtmlLabel) xui.getComponent("portforward")).setText("Port successfully forwarded in your router using UPnP.");
        } else {
            ((JHtmlLabel) xui.getComponent("portforward")).addHyperlinkListener(new HyperlinkListener() {

                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        ui.openURL("http://www.portforward.com");
                    }
                }
            });
        }

        if (startInShareTab) {
            ((JTabbedPane) xui.getComponent("tab")).setSelectedIndex(1);
        }

        uploadThrottleBefore = ui.getCore().getSettings().getInternal().getUploadthrottle();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        pack();

        // we set this AFTER we pack the frame - so that the frame isnt made
        // wider because of a long download folder path
        for (String k : OPTIONS) {
            JComponent c = (JComponent) xui.getComponent(k);
            if (c != null) {
                components.put(k, c);
                setComponentValue(c, getSettingValue(k));
            }
        }

        checkCheckBoxStatus();
        configureCheckBoxListeners();

        Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(ss.width / 2 - getWidth() / 2, ss.height / 2 - getHeight() / 2);
        setVisible(true);
    }

    private String getSettingValue(String k) throws Exception {
        String clazz = k.substring(0, k.indexOf('.'));
        k = k.substring(k.indexOf('.') + 1);
        SettingClass setting = getSettingClass(clazz);
        return String.valueOf(setting.getValue(k));
    }

    private SettingClass getSettingClass(String clazz) throws Exception {
        if (clazz.equals("internal")) {
            return ui.getCore().getSettings().getInternal();
        } else if (clazz.equals("my")) {
            return ui.getCore().getSettings().getMy();
        } else if (clazz.equals("server")) {
            return ui.getCore().getSettings().getServer();
        } else {
            throw new Exception("Could not find class type: " + clazz);
        }
    }

    private void setComponentValue(JComponent c, String settingValue) {
        if (c instanceof JTextField) {
            JTextField tf = (JTextField) c;
            tf.setText(settingValue);
        } else if (c instanceof JCheckBox) {
            JCheckBox b = (JCheckBox) c;
            if ("0".equals(settingValue) || "no".equalsIgnoreCase(settingValue) || "false".equalsIgnoreCase(settingValue)) {
                b.setSelected(false);
            } else {
                b.setSelected(true);
            }

        } else if (c instanceof JComboBox) {
            JComboBox b = (JComboBox) c;
            try {
                b.setSelectedIndex(Integer.parseInt(settingValue));
            } catch (NumberFormatException e) {
                b.setSelectedItem(settingValue);
            }
        }
    }

    public void EVENT_apply(ActionEvent a) throws Exception {
        apply();
    }

    private boolean allowEmptyFields(String k) {

        if (k.equalsIgnoreCase("server.dnsname") || k.equalsIgnoreCase("internal.pmsound") || k.equalsIgnoreCase("internal.downloadsound") || k.equalsIgnoreCase("internal.publicsound")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean apply() throws Exception {
        if (!nicknameIsOk()) {
            return false;
        }

        // update primitive values
        for (String k : OPTIONS) {
            JComponent c = (JComponent) xui.getComponent(k);
            if (getComponentValue(c).toString().length() == 0 && !allowEmptyFields(k)) {
                OptionDialog.showErrorDialog(this, "One or more required fields is empty (field id: " + k + ").");
                return false;
            }
            setSettingValue(k, getComponentValue(c));
        }

        // update shares
        Settings settings = ui.getCore().getSettings();
        settings.getSharelist().clear();

        // remove paths that are subdirectories of other shares
        //while (removeDuplicateShare());

        for (String path : EnumerationIteratorConverter.iterable(shareListModel.elements(), String.class)) {
            //Bastvera (Fetching groupname names for each shared folder from our synchronized model)
            String network = groupNamesModel.getElementAt(shareListModel.indexOf(path)).toString();
            settings.getSharelist().add(new Share(path, network));
        }
        ui.getCore().getShareManager().updateShareBases();
        if (shareListHasBeenModified) {
            //Bastvera (Force Hashing after OK/Apply Button)
            ui.getCore().getShareManager().getShareScanner().startScan(true);
        }

        ui.getCore().getFriendManager().getMe().setNickname(nickname.getText());
        if (ui.getNodeTreeModel(false) != null) {
            ui.getNodeTreeModel(false).signalNodeChanged(
                    ui.getCore().getFriendManager().getMe());
        }
        // update rulelist
        settings.getRulelist().clear();
        for (Routerule rule : EnumerationIteratorConverter.iterable(
                ruleListModel.elements(), Routerule.class)) {
            settings.getRulelist().add(rule);
        }

        ui.getCore().getNetworkManager().getUploadThrottle().setRate(settings.getInternal().getUploadthrottle() * KB);
        if (uploadThrottleBefore != settings.getInternal().getUploadthrottle()) {
            ui.getCore().getNetworkManager().getBandwidthOut().resetHighestCPS();
        }

        ui.getCore().saveSettings();
        return true;
    }

    /**
     * @return True if a duplicate share was removed - in this case this method
     *         needs to be called again in order to check for more duplicated to
     *         remove. This is becase only one duplicate is removed at a time.
     */
    private boolean removeDuplicateShare() {
        for (Iterator<String> i = EnumerationIteratorConverter.iterable(
                shareListModel.elements(), String.class).iterator(); i.hasNext();) {
            String path = i.next();
            ArrayList<String> al = new ArrayList<String>();
            for (String s : EnumerationIteratorConverter.iterable(
                    shareListModel.elements(), String.class)) {
                al.add(s);
            }
            al.add(ui.getCore().getSettings().getInternal().getDownloadfolder());

            for (String s : al) {
                String pathA = TextUtils.makeSurePathIsMultiplatform(new File(
                        path).getAbsolutePath());
                String sA = TextUtils.makeSurePathIsMultiplatform(new File(s).getAbsolutePath());
                if (!sA.equals(pathA) && pathContains(pathA, sA)) {
                    OptionDialog.showInformationDialog(
                            ui.getMainWindow(),
                            "The folder " + pathA + " is already shared as " + sA + ". There is no need to add it in your shares.");
                    groupNamesModel.removeElementAt(shareListModel.indexOf(path)); //Synchro remove if duplicate
                    viewModel.removeElementAt(shareListModel.indexOf(path));
                    shareListModel.removeElement(path);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean pathContains(String path, String file) {
        String s1[] = TextUtils.makeSurePathIsMultiplatform(path).split("/");
        String s2[] = TextUtils.makeSurePathIsMultiplatform(file).split("/");
        if (s1.length < s2.length) {
            return false;
        }

        for (int i = 0; i < s2.length; i++) {
            if (!s1[i].equals(s2[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean nicknameIsOk() {
        if (nickname.getText().equals(My.UNDEFINED_NICKNAME)) {
            OptionDialog.showErrorDialog(ui.getMainWindow(),
                    "You must enter a nickname before continuing.");
            return false;
        }
        if (nickname.getText().indexOf('<') != -1 || nickname.getText().indexOf('>') != -1) {
            OptionDialog.showErrorDialog(ui.getMainWindow(),
                    "Your nickname may not contain &lt; or &gt;.");
            return false;
        }
        return true;
    }

    public void EVENT_cancel(ActionEvent a) throws Exception {
        if (!nicknameIsOk()) {
            return;
        }
        dispose();
    }

    public void EVENT_ok(ActionEvent a) throws Exception {
        if (apply()) {
            dispose();
            if (openedWithUndefiniedNickname) {
                OptionDialog.showInformationDialog(ui.getMainWindow(),
                        "It is time to connect to other users![p]Press OK to continue.[p]");
                ui.getMainWindow().openWizard();
            }
        }
    }

    private Object getComponentValue(JComponent c) {
        if (c instanceof JTextField) {
            return ((JTextField) c).getText();
        }
        if (c instanceof JCheckBox) {
            return ((JCheckBox) c).isSelected() ? 1 : 0;
        }
        if (c instanceof JComboBox) {
            if (components.get("internal.guiskin").equals(c)) {
                return ((JComboBox) c).getSelectedItem();
            } else {
                return "" + ((JComboBox) c).getSelectedIndex();
            }
        }
        return null;
    }

    private void setSettingValue(String k, Object val) throws Exception {
        String clazz = k.substring(0, k.indexOf('.'));
        k = k.substring(k.indexOf('.') + 1);
        SettingClass setting = getSettingClass(clazz);
        setting.setValue(k, val);
    }

    public void EVENT_addfolder(ActionEvent e) {
        JFileChooser fc = new JFileChooser(
                shareListModel.getSize() > 0 ? shareListModel.getElementAt(
                shareListModel.getSize() - 1).toString() : ".");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getPath();
            if (T.t) {
                T.trace("adding: " + path);
            }
            if (!new File(path).exists()) {
                path = new File(path).getParent();
            }

            //Bastvera (For preventing adding same folder couple of times)
            for (int i = 0; i < shareListModel.getSize(); i++) {
                if (shareListModel.getElementAt(i).toString().equalsIgnoreCase(path)) {
                    return;
                }
            }

            shareListModel.addElement(path);
            shareListHasBeenModified = true;
            shareList.revalidate();

            //Bastvera (Adding group name)
            String groupname = "Public";
            try {
                GroupDialogWindow dialogWindow = new GroupDialogWindow(ui, null);
                groupname = dialogWindow.getGroupname();
            } catch (Exception ex) {
            }
            if (groupname == null || groupname.trim().length() == 0) {
                groupname = "Public";
            } else {
                groupname = sortGroupName(groupname);
            }
            groupNamesModel.addElement(groupname);
            viewModel.addElement("[" + groupname + "] " + path);

            //Bastvera Here removingDuplicate start once after each add folder
            while (removeDuplicateShare()) {
            }
        }
    }

    public void EVENT_addrule(ActionEvent e) throws Exception {
        AddRuleWindow window = new AddRuleWindow(ui);
        if (window.getHuman() == null) {
            //This is here to take care of the case of a user adding a rule, then hitting cancel
            return;
        }
        ruleListModel.add(ruleListModel.size(), new Routerule(window.getHuman()));
        ruleList.revalidate();
        ruleList.setSelectedIndex(ruleListModel.size() - 1);
    }

    public void EVENT_editrule(ActionEvent e) throws Exception {
        if (ruleList.getSelectedIndex() != -1) {
            Routerule temp = (Routerule) ruleListModel.get(ruleList.getSelectedIndex());
            int ruleIndex = ruleList.getSelectedIndex();
            AddRuleWindow window = new AddRuleWindow(ui, ruleIndex, temp.getHumanreadable());
            if (window.getHuman() != null) {//Kratos
                ruleListModel.remove(ruleList.getSelectedIndex());
                ruleListModel.add(ruleIndex, new Routerule(window.getHuman()));
                ruleList.revalidate();
                ruleList.setSelectedIndex(ruleIndex);
            }
        }
    }

    public void EVENT_moveruleup(ActionEvent e) {
        if (ruleList.getSelectedIndex() != -1) {
            int ruleIndex = ruleList.getSelectedIndex();
            if (ruleIndex <= 0 || ruleIndex > ruleListModel.size()) {
                return;
            }
            Object temp = ruleListModel.get(ruleIndex);
            ruleListModel.remove(ruleList.getSelectedIndex());
            ruleListModel.add(ruleIndex - 1, temp);
            ruleList.revalidate();
            ruleList.setSelectedIndex(ruleIndex - 1);
        }
    }

    public void EVENT_moveruledown(ActionEvent e) {
        if (ruleList.getSelectedIndex() != -1) {
            int ruleIndex = ruleList.getSelectedIndex();
            if (ruleIndex < 0 || ruleIndex >= ruleListModel.size() - 1) {
                return;
            }
            Object temp = ruleListModel.get(ruleIndex);
            ruleListModel.remove(ruleList.getSelectedIndex());
            ruleListModel.add(ruleIndex + 1, temp);
            ruleList.revalidate();
            ruleList.setSelectedIndex(ruleIndex + 1);
        }
    }

    public void EVENT_removerule(ActionEvent e) {
        if (ruleList.getSelectedIndex() != -1) {
            ruleListModel.remove(ruleList.getSelectedIndex());
            ruleList.revalidate();
        }
    }

    public void EVENT_browse(ActionEvent e) {
        JFileChooser fc = new JFileChooser(downloadFolder.getText());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getPath();
            downloadFolder.setText(path);
        }
    }

    private void copyDirectory(File sourceLocation, File targetLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {
            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    private void deleteDirectory(File sourceLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                deleteDirectory(new File(sourceLocation, children[i]));
            }
        } else {
            sourceLocation.delete();
        }
    }

    public void EVENT_standalonecopy(ActionEvent e) {
        Boolean delete = OptionDialog.showQuestionDialog(ui.getMainWindow(), "This operation requires Administrator privileges, don't use it without them.\nFor security make backup of \"alliance\\data\" in your userprofile folder.\nAlliance will copy all data then close. Restart manually.");
        if (delete) {
            try {
                apply();
                ui.getCore().saveSettings();

                deleteDirectory(new File("data/"));
                new File("data/").delete();

                copyDirectory(new File(Main.localizeHomeDir() + "data/"), new File("data/"));

                new File("standaloneVersion").createNewFile();
                System.exit(0);

            } catch (IOException ex) {
                OptionDialog.showErrorDialog(new JFrame(), "Error. Be sure you have Administrator permissions.");
            } catch (Exception ex) {
            }
        }
    }

    public void EVENT_standalonedelete(ActionEvent e) {
        Boolean delete = OptionDialog.showQuestionDialog(ui.getMainWindow(), "This operation requires Administrator privileges, don't use it without them.\nFor security make backup of \"alliance\\data\" in alliance folder.\nAlliance will copy all data then close. Restart manually.");
        if (delete) {
            try {
                apply();
                ui.getCore().saveSettings();

                if (!(new File(Main.localizeHomeDir()).exists())) {
                    new File(System.getenv("APPDATA") + "/Alliance").mkdirs();
                }

                new File("standaloneVersion").delete();

                deleteDirectory(new File(Main.localizeHomeDir() + "data/"));
                new File(Main.localizeHomeDir() + "data/").delete();

                copyDirectory(new File("data/"), new File(Main.localizeHomeDir() + "data/"));
                System.exit(0);

            } catch (IOException ex) {
                OptionDialog.showErrorDialog(new JFrame(), "Error. Be sure you have Administrator permissions.");
                try {
                    new File("standaloneVersion").createNewFile();
                } catch (IOException ex1) {
                }
            } catch (Exception ex) {
            }
        }
    }

    private void browseSound(String s) {
        JFileChooser fc = new JFileChooser("");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);

        fc.addChoosableFileFilter(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (pathname.toString().endsWith("wav") || pathname.isDirectory()) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return ("Wave files");
            }
        });

        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getPath();
            ((JTextField) xui.getComponent(s)).setText(path);
        }
    }

    public void EVENT_browsesoundpm(ActionEvent e) {
        browseSound("internal.pmsound");
    }

    public void EVENT_browsesounddownload(ActionEvent e) {
        browseSound("internal.downloadsound");
    }

    public void EVENT_browsesoundpublic(ActionEvent e) {
        browseSound("internal.publicsound");
    }

    public void EVENT_sounddefault(ActionEvent e) {
        ((JTextField) xui.getComponent("internal.pmsound")).setText("sounds/chatpm.wav");
        ((JTextField) xui.getComponent("internal.downloadsound")).setText("sounds/download.wav");
        ((JTextField) xui.getComponent("internal.publicsound")).setText("sounds/chatpublic.wav");
    }

    public void EVENT_soundmute(ActionEvent e) {
        ((JTextField) xui.getComponent("internal.pmsound")).setText("");
        ((JTextField) xui.getComponent("internal.downloadsound")).setText("");
        ((JTextField) xui.getComponent("internal.publicsound")).setText("");
    }

    /**
     * Triggered when "remove folder" button is pressed in the option->share
     * menu
     *
     * @param e
     */
    public void EVENT_removefolder(ActionEvent e) {
        if (shareList.getSelectedIndex() != -1) {
            int selected = shareList.getSelectedIndex(); //Bastvera (If we remove folder index changes so we need to keep it for groupModel)
            shareListModel.remove(selected);
            groupNamesModel.remove(selected); //Bastvera (Synchronized remove group names if folder removed)
            viewModel.remove(selected);
            shareListHasBeenModified = true;
            shareList.revalidate();
        }
    }

    private String sortGroupName(String groupname) {
        TreeSet<String> groupSort = new TreeSet<String>();
        String[] dividegroup = groupname.split(",");
        for (String group : dividegroup) {
            if (group.trim().length() > 1) {
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
        if (groupname.length() == 0) {
            return "Public";
        }
        return groupname;
    }

    public void EVENT_editgroupname(ActionEvent e) { //Editing group names
        if (shareList.getSelectedIndex() != -1) {
            int[] selection = shareList.getSelectedIndices();
            String groupname = JOptionPane.showInputDialog("Edit group name for (" + selection.length + ") folders.\nDivide multiple group names with commas.\nExample 1: music,games\nExample 2: music,games,private", groupNamesModel.get(shareList.getSelectedIndex()));
            if (groupname == null) {
                return;
            }
            if (groupname.trim().length() == 0) {
                groupname = "Public";
            } else {
                groupname = sortGroupName(groupname);
            }
            for (int position : selection) {
                groupNamesModel.set(position, groupname);
                viewModel.setElementAt("[" + groupname + "] " + shareListModel.get(position), position);
            }
            shareList.revalidate();
        }
    }

    private void checkCheckBoxStatus() {
        if (getComponentValue(components.get("internal.alwaysallowfriendsoffriendstoconnecttome")).toString().equalsIgnoreCase("1")) {
            setComponentValue(components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome"), "false");
            components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome").setEnabled(false);
            setComponentValue(components.get("internal.automaticallydenyallinvitations"), "false");
            components.get("internal.automaticallydenyallinvitations").setEnabled(false);
            setComponentValue(components.get("internal.alwaysdenyuntrustedinvitations"), "false");
            components.get("internal.alwaysdenyuntrustedinvitations").setEnabled(false);
        } else if (getComponentValue(components.get("internal.automaticallydenyallinvitations")).toString().equalsIgnoreCase("1")) {
            setComponentValue(components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome"), "false");
            components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome").setEnabled(false);
            setComponentValue(components.get("internal.alwaysdenyuntrustedinvitations"), "false");
            components.get("internal.alwaysdenyuntrustedinvitations").setEnabled(false);
        } else if (getComponentValue(components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome")).toString().equalsIgnoreCase("0")) {
            setComponentValue(components.get("internal.alwaysdenyuntrustedinvitations"), "false");
            components.get("internal.alwaysdenyuntrustedinvitations").setEnabled(false);
        }

        /* if (!OSInfo.isWindows()) {
        xui.getComponent("standalonedelete").setEnabled(false);
        xui.getComponent("standalonecopy").setEnabled(false);
        } else {
        if (new File("standaloneVersion").exists()) {
        xui.getComponent("standalonedelete").setEnabled(true);
        xui.getComponent("standalonecopy").setEnabled(false);
        } else {
        xui.getComponent("standalonedelete").setEnabled(false);
        xui.getComponent("standalonecopy").setEnabled(true);
        }
        }*/
    }

    private void configureCheckBoxListeners() {
        ((JCheckBox) components.get("internal.alwaysallowfriendsoffriendstoconnecttome")).addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (getComponentValue(components.get("internal.alwaysallowfriendsoffriendstoconnecttome")).toString().equalsIgnoreCase("1")) {
                    setComponentValue(components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome"), "false");
                    components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome").setEnabled(false);
                    setComponentValue(components.get("internal.automaticallydenyallinvitations"), "false");
                    components.get("internal.automaticallydenyallinvitations").setEnabled(false);
                    setComponentValue(components.get("internal.alwaysdenyuntrustedinvitations"), "false");
                    components.get("internal.alwaysdenyuntrustedinvitations").setEnabled(false);
                } else {
                    components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome").setEnabled(true);
                    components.get("internal.automaticallydenyallinvitations").setEnabled(true);
                }
            }
        });

        ((JCheckBox) components.get("internal.automaticallydenyallinvitations")).addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (getComponentValue(components.get("internal.automaticallydenyallinvitations")).toString().equalsIgnoreCase("1")) {
                    setComponentValue(components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome"), "false");
                    components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome").setEnabled(false);
                    setComponentValue(components.get("internal.alwaysdenyuntrustedinvitations"), "false");
                    components.get("internal.alwaysdenyuntrustedinvitations").setEnabled(false);
                } else {
                    components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome").setEnabled(true);
                }
            }
        });

        ((JCheckBox) components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome")).addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (getComponentValue(components.get("internal.alwaysallowfriendsoftrustedfriendstoconnecttome")).toString().equalsIgnoreCase("0")) {
                    components.get("internal.alwaysdenyuntrustedinvitations").setEnabled(false);
                    setComponentValue(components.get("internal.alwaysdenyuntrustedinvitations"), "false");
                } else {
                    components.get("internal.alwaysdenyuntrustedinvitations").setEnabled(true);
                }
            }
        });

        ((JCheckBox) components.get("internal.rdnsname")).addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (getComponentValue(components.get("internal.rdnsname")).toString().equalsIgnoreCase("1")) {
                    Boolean confirm = OptionDialog.showQuestionDialog(ui.getMainWindow(), "This option will try to convert all friend's IPs to DNS. Conversion will hang alliance for couple of seconds so use it only when you don't upload/download anything.\nIf DNS names can't be obtained alliance will use IP and try to convert later.");
                    if (confirm) {
                        try {
                            setComponentValue(components.get("server.dnsname"), InetAddress.getByName(ui.getCore().getFriendManager().getMe().getExternalIp(ui.getCore())).getHostName());
                        } catch (UnknownHostException ex) {
                            setComponentValue(components.get("server.dnsname"), "");
                        } catch (IOException ex) {
                            setComponentValue(components.get("server.dnsname"), "");
                        }
                        ui.getCore().getSettings().getInternal().setRdnsname(1);
                        Collection<Friend> friends = ui.getCore().getFriendManager().friends();
                        for (Friend friend : friends.toArray(new Friend[friends.size()])) {
                            friend.setLastKnownHost(friend.rDNSConvert(friend.getLastKnownHost(), ui.getCore().getSettings().getFriend(friend.getGuid())));
                        }
                    } else {
                        setComponentValue(components.get("internal.rdnsname"), "0");
                    }
                } else {
                    Boolean confirm = OptionDialog.showQuestionDialog(ui.getMainWindow(), "Unconverting changes ALL DNS names to IP. Conversion will hang alliance for couple of seconds so use it only when you don't upload/download anything.");
                    if (confirm) {
                        setComponentValue(components.get("server.dnsname"), "");
                        ui.getCore().getSettings().getInternal().setRdnsname(0);
                        Collection<Friend> friends = ui.getCore().getFriendManager().friends();
                        for (Friend friend : friends.toArray(new Friend[friends.size()])) {
                            friend.setLastKnownHost(friend.rDNSConvert(friend.getLastKnownHost(), ui.getCore().getSettings().getFriend(friend.getGuid())));
                        }
                    } else {
                        setComponentValue(components.get("internal.rdnsname"), "1");
                    }
                }
            }
        });
    }
}
