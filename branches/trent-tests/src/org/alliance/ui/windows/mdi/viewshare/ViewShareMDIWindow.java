package org.alliance.ui.windows.mdi.viewshare;

import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.ui.JHtmlLabel;
import com.stendahls.util.TextUtils;
import org.alliance.core.Language;
import org.alliance.core.comm.rpc.GetHashesForPath;
import org.alliance.core.file.filedatabase.FileType;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.node.Friend;
import org.alliance.core.node.MyNode;
import org.alliance.core.node.Node;
import org.alliance.ui.T;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.windows.mdi.AllianceMDIWindow;
import org.alliance.ui.dialogs.OptionDialog;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.ToolTipManager;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class ViewShareMDIWindow extends AllianceMDIWindow {

    private Node remote;
    private JTree tree;
    private ViewShareTreeModel model;
    private JPopupMenu popup;
    private Icon iconDown;
    private final Icon iconLoading;
    private ImageIcon[] fileTypeIcons;
    private ImageIcon folderIconExpanded, folderIconCollapsed;
    private JMenu downToMenu;
    private static String LAST_USED_DIR;
    private MouseListener downloadToListener;

    public ViewShareMDIWindow(final UISubsystem ui, Node remote) throws Exception {
        super(ui.getMainWindow().getMDIManager(), (remote instanceof MyNode) ? "viewmyshare" : "viewshare", ui);
        this.remote = remote;
        Language.translateXUIElements(getClass(), xui.getXUIComponents());
        setTitle(remote.getNickname());

        iconLoading = new ImageIcon(ui.getRl().getResource("gfx/icons/loadingsharenode.png"));
        iconDown = new ImageIcon(ui.getRl().getResource("gfx/icons/download.png"));
        //@todo: this is done in other places too. AND it's a waste of reasources to load these every time
        fileTypeIcons = new ImageIcon[8];
        for (int i = 0; i < fileTypeIcons.length; i++) {
            fileTypeIcons[i] = new ImageIcon(ui.getRl().getResource("gfx/filetypes/" + i + ".png"));
        }
        folderIconExpanded = new ImageIcon(ui.getRl().getResource("gfx/icons/viewshare.png"));
        folderIconCollapsed = new ImageIcon(ui.getRl().getResource("gfx/icons/folder_closed.png"));

        model = new ViewShareTreeModel(remote, ui, this);
        tree = new JTree(model);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new ViewShareTreeRenderer());
        ToolTipManager.sharedInstance().registerComponent(tree);

        tree.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    TreePath underMouse = tree.getPathForLocation(e.getPoint().x, e.getPoint().y);
                    if (underMouse != null) {
                        ViewShareTreeNode n = (ViewShareTreeNode) underMouse.getLastPathComponent();
                        boolean mouseClickedOnASelectedNode = false;
                        if (tree.getSelectionPaths() != null) {
                            for (TreePath p : tree.getSelectionPaths()) {
                                if (p.getLastPathComponent() == n) {
                                    mouseClickedOnASelectedNode = true;
                                    break;
                                }
                            }
                        }
                        if (!mouseClickedOnASelectedNode) {
                            tree.setSelectionPath(underMouse);
                        }
                        if (LAST_USED_DIR == null) {
                            popup.show(e.getComponent(), e.getX(), e.getY());
                            return;
                        }
                        if (downToMenu.getMenuComponentCount() > 2) {
                            JMenuItem oldItem = (JMenuItem) downToMenu.getMenuComponent(downToMenu.getMenuComponentCount() - 1);
                            if (TextUtils.makeSurePathIsMultiplatform(oldItem.getToolTipText()).equals(LAST_USED_DIR)) {
                                popup.show(e.getComponent(), e.getX(), e.getY());
                                return;
                            }
                            downToMenu.remove(downToMenu.getMenuComponentCount() - 1);
                        } else {
                            downToMenu.addSeparator();
                            JMenuItem item = new JMenuItem(Language.getLocalizedString(getClass(), "lastused"));
                            item.setEnabled(false);
                            item.setFont(new Font(item.getFont().getFamily(), Font.ITALIC, item.getFont().getSize()));
                            downToMenu.add(item);
                        }
                        StringBuilder shortLastUsedDir = new StringBuilder(LAST_USED_DIR.substring(LAST_USED_DIR.lastIndexOf("/") + 1));
                      //  if (shortLastUsedDir.length() == 0) {
                            shortLastUsedDir.append((new File(LAST_USED_DIR)).getAbsolutePath());
                      //  } else {
                            shortLastUsedDir.replace(0, 1, shortLastUsedDir.substring(0, 1).toUpperCase());
                      //  }
                        JMenuItem item = new JMenuItem(shortLastUsedDir.toString());
                        item.setIcon(iconDown);
                        item.setToolTipText((new File(LAST_USED_DIR)).getAbsolutePath());
                        item.addMouseListener(downloadToListener);
                        downToMenu.add(item);
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        ((JScrollPane) xui.getComponent("treepanel")).setViewportView(tree);

        popup = (JPopupMenu) xui.getComponent(remote instanceof MyNode ? "popupme" : "popup");
        downToMenu = ((JMenu) xui.getComponent("downloadto"));
        downloadToListener = new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                download(LAST_USED_DIR);
            }
        };

        if (!(remote instanceof MyNode)) {
            JHtmlLabel l = (JHtmlLabel) xui.getComponent("chatmessage");
            StringBuilder sb = new StringBuilder();
            sb.append(Language.getLocalizedString(getClass(), "tip")).append(" ");
            sb.append("[a href=c]").append(Language.getLocalizedString(getClass(), "chat")).append("[/a]");
            l.setText(sb.toString());
            l.addHyperlinkListener(new HyperlinkListener() {

                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        try {
                            EVENT_chat(null);
                        } catch (Exception e1) {
                            ui.handleErrorInEventLoop(e1);
                        }
                    }
                }
            });
        }

        JLabel status = (JLabel) xui.getComponent("status");
        status.setText(Language.getLocalizedString(getClass(), "shares", TextUtils.formatByteSize(remote.getShareSize()),
                Integer.toString(remote.getNumberOfFilesShared())));

        status = (JLabel) xui.getComponent("status2");
        status.setText(Language.getLocalizedString(getClass(), "uptotal", TextUtils.formatByteSize(remote.getTotalBytesSent()),
                TextUtils.formatByteSize((long) remote.getHighestOutgoingCPS())));

        status = (JLabel) xui.getComponent("status3");
        status.setText(Language.getLocalizedString(getClass(), "downtotal", TextUtils.formatByteSize(remote.getTotalBytesReceived()),
                TextUtils.formatByteSize((long) remote.getHighestIncomingCPS())));

        status = (JLabel) xui.getComponent("status4");
        status.setText(Language.getLocalizedString(getClass(), "invites", Integer.toString(remote.getNumberOfInvitedFriends())));

        postInit();
    }

    public void shareBaseListReceived(String[] shareBaseNames) {
        if (T.t) {
            T.info("Callback got back - filling with " + shareBaseNames.length + " share bases.");
        }
        model.shareBaseNamesReceived(shareBaseNames);
    }

    public void directoryListingReceived(int shareBaseIndex, String path, TreeMap<String, Long> fileSize) {
        ViewShareShareBaseNode n = model.getRoot().getByShareBase(shareBaseIndex);
        if (n != null) {
            if (T.t) {
                T.info("Updating path for share base at index " + shareBaseIndex + ": " + n);
            }
            n.pathUpdated(path, fileSize);
        } else {
            if (T.t) {
                T.error("Could not find share with sharebase " + shareBaseIndex);
            }
        }
    }

    public void EVENT_openfile(ActionEvent e) {
        if (!(remote instanceof MyNode)) {
            return;
        }
        if (tree == null || tree.getSelectionPaths() == null) {
            return;
        }
        if (tree.getSelectionPaths().length > 1) {
            OptionDialog.showErrorDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "onlyone"));
            return;
        }
        ViewShareTreeNode node = (ViewShareTreeNode) tree.getSelectionPath().getLastPathComponent();
        String path = ui.getCore().getShareManager().getBaseByIndex(node.getShareBaseIndex()).getPath() + "/" + node.getFileItemPath();
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (Exception ex) {
            OptionDialog.showErrorDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "openerror"));
        }
    }

    public void EVENT_sendtochat(ActionEvent e) throws Exception {
        if (!(remote instanceof MyNode)) {
            return;
        }
        if (tree == null || tree.getSelectionPaths() == null) {
            return;
        }
        if (tree.getSelectionPaths().length > 1) {
            OptionDialog.showErrorDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "onesend"));
            return;
        }

        ViewShareTreeNode node = (ViewShareTreeNode) tree.getSelectionPath().getLastPathComponent();
        if (!(node instanceof ViewShareFileNode)) {
            OptionDialog.showErrorDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "senderror"));
            return;
        }

        String basePath = ui.getCore().getShareManager().getBaseByIndex(node.getShareBaseIndex()).getPath();
        String path = node.getFileItemPath();
        String name = node.getName();

        if (T.t) {
            T.info("Sending " + path + " to chat");
        }

        String link = "<a href=\"" + ui.getCore().getFriendManager().getMyGUID() + "|";
        long totalSize = 0;
        int numberOfFiles = 0;

        HashMap<Hash, Long> hashSize = ui.getCore().getFileManager().getFileDatabase().getRootHashWithSize(basePath, path);
        for (Hash hash : hashSize.keySet()) {
            link += hash.getRepresentation() + "|";
            totalSize += hashSize.get(hash);
            if (T.t) {
                T.debug("found: " + hash.getRepresentation());
            }
            numberOfFiles++;
        }
        hashSize.clear();

        link = link.substring(0, link.length() - 1);

        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        link += "\">" + name + "</a> (" + TextUtils.formatByteSize(totalSize) + " in " + numberOfFiles + " files)";
        if (T.t) {
            T.info("Sending link: " + link);
        }
        ui.getMainWindow().getPublicChat().send(link);
        ui.getMainWindow().getMDIManager().selectWindow(ui.getMainWindow().getPublicChat());
    }

    public void EVENT_linktohdd(ActionEvent e) throws IOException {
        if (!(remote instanceof MyNode)) {
            return;
        }
        if (tree == null || tree.getSelectionPaths() == null) {
            return;
        }
        if (tree.getSelectionPaths().length > 1) {
            OptionDialog.showErrorDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "onehdd"));
            return;
        }

        ViewShareTreeNode node = (ViewShareTreeNode) tree.getSelectionPath().getLastPathComponent();
        if (!(node instanceof ViewShareFileNode)) {
            OptionDialog.showErrorDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "hdderror"));
            return;
        }

        String basePath = ui.getCore().getShareManager().getBaseByIndex(node.getShareBaseIndex()).getPath();
        String path = node.getFileItemPath();
        String name = node.getName();

        String link = ui.getCore().getFriendManager().getMyGUID() + "|";

        HashMap<Hash, Long> hashSize = ui.getCore().getFileManager().getFileDatabase().getRootHashWithSize(basePath, path);
        for (Hash hash : hashSize.keySet()) {
            link += hash.getRepresentation() + "|";
        }
        hashSize.clear();

        link = link.substring(0, link.length() - 1);
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }

        char[] charArray = link.toCharArray();
        for (int i = 0; i < link.length(); i++) {
            charArray[i] += 33;
        }

        String wd = System.getProperty("user.dir");
        JFileChooser fc = new JFileChooser(wd);
        fc.setSelectedFile(new File(name + ".alliance"));
        int rc = fc.showOpenDialog(null);
        if (rc == JFileChooser.APPROVE_OPTION) {
            File outFile = fc.getSelectedFile();
            OutputStream out = new FileOutputStream(outFile);
            for (char c : charArray) {
                out.write(c);
            }
            out.close();
        }
    }

    public void EVENT_chat(ActionEvent e) throws Exception {
        if (remote instanceof MyNode) {
            return;
        }
        ui.getMainWindow().chatMessage(remote.getGuid(), null, 0, false);
    }

    private void download(final String downloadDir) {
        if (!(remote instanceof Friend)) {
            return; //ignore if user tries to download a file from himself (remote is instanceof MyNode then)
        }
        if (tree == null || tree.getSelectionPaths() == null) {
            return;
        }
        for (TreePath p : tree.getSelectionPaths()) {
            ViewShareTreeNode n = (ViewShareTreeNode) p.getLastPathComponent();
            if (!(n instanceof ViewShareFileNode)) {
                OptionDialog.showErrorDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "notroot"));
                return;
            }
        }

        final ArrayList<ViewShareFileNode> paths = new ArrayList<ViewShareFileNode>();
        for (TreePath p : tree.getSelectionPaths()) {
            ViewShareFileNode n = (ViewShareFileNode) p.getLastPathComponent();
            paths.add(n);
        }

        ui.getCore().invokeLater(new Runnable() {

            @Override
            public void run() {
                for (ViewShareFileNode p : paths) {
                    if (remote.isConnected()) {
                        try {
                            ui.getCore().getFileManager().getDownloadStorage().addCustomDownload(remote.getGuid(), downloadDir, p.getFileItemPath());
                            ((Friend) remote).getFriendConnection().send(new GetHashesForPath(p.getShareBaseIndex(), p.getFileItemPath()));
                        } catch (IOException e1) {
                            ui.getCore().reportError(e1, this);
                        }
                    } else {
                        if (T.t) {
                            T.error("User not connected!");
                        }
                    }
                }
            }
        });
        ui.getMainWindow().getMDIManager().selectWindow(ui.getMainWindow().getDownloadsWindow());
    }

    public void EVENT_download(ActionEvent e) {
        download(null);
    }

    public void EVENT_browse(ActionEvent e) {
        JFileChooser fc = new JFileChooser(".");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = TextUtils.makeSurePathIsMultiplatform(fc.getSelectedFile().getAbsolutePath());
            if (!new File(path).exists()) {
                download(null);
                return;
            }
            download(path);
            LAST_USED_DIR = path;
        }
    }

    @Override
    public String getIdentifier() {
        return "viewshare" + remote.getGuid();
    }

    @Override
    public void save() throws Exception {
    }

    @Override
    public void revert() throws Exception {
        ViewShareMDIWindow viewShareMDIWindow = new ViewShareMDIWindow(ui, remote);
        manager.recreateWindow(this, viewShareMDIWindow);
    }

    @Override
    public void serialize(ObjectOutputStream out) throws IOException {
    }

    @Override
    public MDIWindow deserialize(ObjectInputStream in) throws IOException {
        return null;
    }

    private class ViewShareTreeRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean sel,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {

            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            setToolTipText(null);
            if (value instanceof ViewShareLoadingNode) {
                setIcon(iconLoading);
            } else if (value instanceof ViewShareFileNode) {
                ViewShareFileNode n = (ViewShareFileNode) value;
                if (!n.isFolder()) {
                    setIcon(fileTypeIcons[FileType.getByFileName(n.getName()).id()]);
                    long size = n.getSize();
                    if (size > 0) {
                        setToolTipText(Language.getLocalizedString(getClass().getEnclosingClass(), "filesize", TextUtils.formatByteSize(n.getSize())));
                    } else {
                        setToolTipText(Language.getLocalizedString(getClass().getEnclosingClass(), "filesizena"));
                    }
                } else {
                    if (expanded) {
                        setIcon(folderIconExpanded);
                    } else {
                        setIcon(folderIconCollapsed);
                    }
                }
            } else if (value instanceof ViewShareShareBaseNode) {
                if (expanded) {
                    setIcon(folderIconExpanded);
                } else {
                    setIcon(folderIconCollapsed);
                }
            }
            return this;
        }
    }
}
