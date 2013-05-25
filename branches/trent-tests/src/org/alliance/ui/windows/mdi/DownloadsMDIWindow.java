package org.alliance.ui.windows.mdi;

import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.util.TextUtils;
import org.alliance.core.Language;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.filetransfers.Download;
import org.alliance.core.comm.filetransfers.DownloadConnection;
import org.alliance.core.comm.filetransfers.UploadConnection;
import org.alliance.core.file.hash.Hash;
import org.alliance.launchers.ui.Main;
import org.alliance.ui.JDownloadGrid;
import org.alliance.ui.T;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.dialogs.OptionDialog;

import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JFileChooser;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class DownloadsMDIWindow extends AllianceMDIWindow {

    private DownloadsTableModel model;
    private JDownloadGrid downloadGrid;
    private JTable table;
    private JPopupMenu popup;
    private JLabel status, downloadingFromText, uploadingToText;
    private ArrayList<DownloadWrapper> rows = new ArrayList<DownloadWrapper>();
    private DownloadWrapper interestingDownloadWrapper;

    public DownloadsMDIWindow(final UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "downloads", ui);
        Language.translateXUIElements(getClass(), xui.getXUIComponents());

        table = (JTable) xui.getComponent("tableDownload");
        table.setModel(model = new DownloadsTableModel());
        table.setAutoCreateColumnsFromModel(false);
        table.getColumnModel().getColumn(1).setCellRenderer(new ProgressBarCellRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);

        status = (JLabel) xui.getComponent("status");
        downloadingFromText = (JLabel) xui.getComponent("downloadingfromtext");
        uploadingToText = (JLabel) xui.getComponent("uploadingtotext");

        setFixedColumnSize(table.getColumnModel().getColumn(2), 60);
        setFixedColumnSize(table.getColumnModel().getColumn(3), 60);
        setFixedColumnSize(table.getColumnModel().getColumn(4), 60);
        setFixedColumnSize(table.getColumnModel().getColumn(5), 10);

        downloadGrid = (JDownloadGrid) xui.getComponent("downloadgrid");
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getFirstIndex() < 0 || e.getFirstIndex() >= rows.size()) {
                    downloadGrid.setDownload(null);
                    updateDownloadingFromAndUploadingToText();
                    return;
                }
                selectDownloadToShowOnDownloadGrid();
                updateDownloadingFromAndUploadingToText();
            }
        });

        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    boolean b = false;
                    for (int r : table.getSelectedRows()) {
                        if (r == row) {
                            b = true;
                            break;
                        }
                    }
                    if (!b) {
                        table.getSelectionModel().setSelectionInterval(row, row);
                    }
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        table.setColumnSelectionAllowed(false);

        popup = (JPopupMenu) xui.getComponent("popup");

        update();
        setTitle(Language.getLocalizedString(getClass(), "title"));
        listenExternalLinks();
        postInit();
    }

    private void selectDownloadToShowOnDownloadGrid() {
        DownloadWrapper d = null;
        if (table.getSelectedRow() == -1) {
            d = selectDownloadToShowOnDownloadGridIgnoringSelection();
        } else {
            DownloadWrapper dw = rows.get(table.getSelectedRow());
            if (dw != null && dw.download != null && !dw.download.isComplete() && dw.download.isActive()) {
                d = dw;
            } else {
                d = selectDownloadToShowOnDownloadGridIgnoringSelection();
            }
        }
        if (d != null && downloadGrid != null) {
            downloadGrid.setDownload(d.download);
        }
        interestingDownloadWrapper = d;
    }

    private DownloadWrapper selectDownloadToShowOnDownloadGridIgnoringSelection() {
        for (DownloadWrapper dw : rows) {
            if (dw.download != null && !dw.download.isComplete() && dw.download.isActive()) {
                return dw;
            }
        }
        return null;
    }

    private void showTotalBytesReceived() {
        status.setText(Language.getLocalizedString(getClass(), "bytetotal", TextUtils.formatByteSize(ui.getCore().getNetworkManager().getBandwidthIn().getTotalBytes())));
    }

    private String getDownloadingFromText(DownloadWrapper w) {
        String text = null;
        final String s;
        for (DownloadConnection c : w.download.connections()) {
            if (text == null) {
                text = Language.getLocalizedString(getClass(), "from") + " ";
            }
            if (c.getRemoteFriend() != null) {
                text += c.getRemoteFriend().getNickname() + " (" + c.getBandwidthIn().getCPSHumanReadable() + "), ";
            } else {
                text += Language.getLocalizedString(getClass(), "unknown") + ", ";
            }
        }
        if (text != null) {
            text = text.substring(0, text.length() - 2);
            s = text;
        } else {
            s = " ";
        }
        return s;
    }

    private String getUploadingToText(DownloadWrapper w) {
        String text = null;
        final String s;
        ArrayList<Connection> al = new ArrayList<Connection>(ui.getCore().getFriendManager().getNetMan().connections());
        for (Connection c : al) {
            if (c instanceof UploadConnection) {
                UploadConnection uc = (UploadConnection) c;
                if (uc.getRoot() != null && uc.getRoot().equals(w.download.getRoot())) {
                    if (text == null) {
                        text = Language.getLocalizedString(getClass(), "to") + " ";
                    }
                    if (uc.getRemoteFriend() != null) {
                        text += uc.getRemoteFriend().getNickname() + " (" + c.getBandwidthOut().getCPSHumanReadable() + "), ";
                    } else {
                        text += Language.getLocalizedString(getClass(), "unknown") + ", ";
                    }
                }
            }
        }
        if (text != null) {
            text = text.substring(0, text.length() - 2);
            s = text;
        } else {
            s = " ";
        }
        return s;
    }

    private void setFixedColumnSize(TableColumn column, int i) {
        column.setPreferredWidth(i);
        column.setMaxWidth(i);
        column.setMinWidth(i);
    }

    public void update() {
        boolean structureChanged = false;

        ArrayList<Download> al = new ArrayList<Download>(ui.getCore().getNetworkManager().getDownloadManager().downloads());
        for (Download d : al) {
            DownloadWrapper dw = getWrapperFor(d);
            if (dw == null) {
                structureChanged = true;
                dw = new DownloadWrapper(d);
                rows.add(dw);
            }
            dw.update();
        }

        for (Iterator i = rows.iterator(); i.hasNext();) {
            DownloadWrapper w = (DownloadWrapper) i.next();
            if (!ui.getCore().getNetworkManager().getDownloadManager().contains(w.download)) {
                structureChanged = true;
                i.remove();
            }
        }

        if (structureChanged) {
            model.fireTableStructureChanged();
        } else {
            model.fireTableRowsUpdated(0, rows.size());
        }

        showTotalBytesReceived();

        selectDownloadToShowOnDownloadGrid();

        updateDownloadingFromAndUploadingToText();

        downloadGrid.repaint();
    }

    private void updateDownloadingFromAndUploadingToText() {
        if (interestingDownloadWrapper != null) {
            downloadingFromText.setText(getDownloadingFromText(interestingDownloadWrapper));
            uploadingToText.setText(getUploadingToText(interestingDownloadWrapper));
        } else {
            downloadingFromText.setText("");
            uploadingToText.setText("");
        }
    }

    private DownloadWrapper getWrapperFor(Download d) {
        for (DownloadWrapper cw : rows) {
            if (cw.download == d) {
                return cw;
            }
        }
        return null;
    }

    @Override
    public String getIdentifier() {
        return "downloads";
    }

    @Override
    public void save() throws Exception {
    }

    @Override
    public void revert() throws Exception {
    }

    @Override
    public void serialize(ObjectOutputStream out) throws IOException {
    }

    @Override
    public MDIWindow deserialize(ObjectInputStream in) throws IOException {
        return null;
    }

    private class DownloadWrapper {

        public Download download;
        public String name, speed, size;
        public int percentComplete, numberOfConnections;
        public boolean complete;
        public Download.State state;
        public String eta;

        public DownloadWrapper(Download download) {
            this.download = download;
        }

        public void update() {
            try {
                if (download.getFd() == null) {
                    if (download.getNConnections() == 0) {
                        name = download.getAuxInfoFilename();
                    } else {
                        name = Language.getLocalizedString(getClass().getEnclosingClass(), "starting", download.getAuxInfoFilename());
                    }
                    size = "?";
                } else {
                    name = download.getFd().getSubPath();
                    size = TextUtils.formatByteSize(download.getFd().getSize());
                }
                percentComplete = download.getPercentComplete();
                numberOfConnections = download.getNConnections();
                speed = download.getBandwidth().getCPSHumanReadable();
                complete = download.isComplete();
                if (complete) {
                    downloadGrid.setDownload(null);
                }
                state = download.getState();
                if (download.getBandwidth().hasGoodAverage()) {
                    eta = formatETA(download.getETAInMinutes());
                } else {
                    eta = "?";
                }
            } catch (IOException e) {
                if (T.t) {
                    T.error("Exception while updating downloadwrapper: " + e);
                }
            }
        }

        private String formatETA(int eta) {
            if (eta < 0) {
                return "?";
            } else if (eta <= 60) {
                return eta + " sec";
            } else if (eta / 60 < 60) {
                return eta / 60 + " min";
            } else {
                return (eta / 60 / 60) + "h " + (eta / 60 % 60) + "m";
            }
        }
    }

    private class DownloadsTableModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return Language.getLocalizedString(getClass().getEnclosingClass(), "name");
                case 1:
                    return Language.getLocalizedString(getClass().getEnclosingClass(), "progress");
                case 2:
                    return Language.getLocalizedString(getClass().getEnclosingClass(), "size");
                case 3:
                    return Language.getLocalizedString(getClass().getEnclosingClass(), "eta");
                case 4:
                    return Language.getLocalizedString(getClass().getEnclosingClass(), "speed");
                case 5:
                    return "#";
                default:
                    return Language.getLocalizedString(getClass().getEnclosingClass(), "undefined");
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return rows.get(rowIndex).name;
                case 1:
                    return rows.get(rowIndex).percentComplete;
                case 2:
                    return rows.get(rowIndex).size;
                case 3:
                    return rows.get(rowIndex).eta;
                case 4:
                    return rows.get(rowIndex).speed;
                case 5:
                    return rows.get(rowIndex).numberOfConnections;
                default:
                    return Language.getLocalizedString(getClass().getEnclosingClass(), "undefined");
            }
        }
    }

    public void EVENT_cleanup(ActionEvent e) {
        ui.getCore().invokeLater(new Runnable() {

            @Override
            public void run() {
                ui.getCore().getNetworkManager().getDownloadManager().removeCompleteDownloads();
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        update();
                    }
                });
            }
        });
    }

    public void EVENT_movedown(ActionEvent e) {
        int selection[] = table.getSelectedRows();
        if (selection != null && selection.length > 0) {
            for (int i : selection) {
                DownloadWrapper dw = rows.get(i);
                final Download d = dw.download;
                ui.getCore().invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        ui.getCore().getNetworkManager().getDownloadManager().moveDown(d);
                    }
                });
                moveDown(i, dw);
            }
            model.fireTableStructureChanged();
            for (int i : selection) {
                if (i < rows.size() - 1) {
                    table.getSelectionModel().addSelectionInterval(i + 1, i + 1);
                }
            }
        }
    }

    public void EVENT_moveup(ActionEvent e) {
        int selection[] = table.getSelectedRows();
        if (selection != null && selection.length > 0) {
            for (int i : selection) {
                DownloadWrapper dw = rows.get(i);
                final Download d = dw.download;
                ui.getCore().invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        ui.getCore().getNetworkManager().getDownloadManager().moveUp(d);
                    }
                });
                moveUp(i, dw);
            }
            model.fireTableStructureChanged();
            for (int i : selection) {
                if (i > 0) {
                    table.getSelectionModel().addSelectionInterval(i - 1, i - 1);
                }
            }
        }
    }

    public void EVENT_movetop(ActionEvent e) {
        int selection[] = table.getSelectedRows();
        int offset = 0;
        if (selection != null && selection.length > 0) {
            for (int j = selection.length - 1; j >= 0; j--) {
                int i = selection[j];
                DownloadWrapper dw = rows.get(i + offset);
                final Download d = dw.download;
                ui.getCore().invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        ui.getCore().getNetworkManager().getDownloadManager().moveTop(d);
                    }
                });
                moveTop(i + offset, dw);
                offset++;
            }
            model.fireTableStructureChanged();
            table.getSelectionModel().addSelectionInterval(0, selection.length - 1);
        }
    }

    public void EVENT_movebottom(ActionEvent e) {
        int selection[] = table.getSelectedRows();

        int offset = 0;
        if (selection != null && selection.length > 0) {
            for (int i : selection) {
                DownloadWrapper dw = rows.get(i - offset);
                final Download d = dw.download;
                ui.getCore().invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        ui.getCore().getNetworkManager().getDownloadManager().moveBottom(d);
                    }
                });
                moveBottom(i - offset, dw);
                offset++;
            }
            model.fireTableStructureChanged();
            ListSelectionModel sm = table.getSelectionModel();
            sm.addSelectionInterval(rows.size() - selection.length, rows.size() - 1);
        }
    }

    private void moveUp(int i, DownloadWrapper dw) {
        if (i == 0) {
            return;
        }
        rows.remove(i);
        rows.add(i - 1, dw);
    }

    private void moveDown(int i, DownloadWrapper dw) {
        if (i == rows.size() - 1) {
            return;
        }
        rows.remove(dw);
        rows.add(i + 1, dw);
    }

    private void moveTop(int i, DownloadWrapper dw) {
        if (i == 0) {
            return;
        }
        rows.remove(i);
        rows.add(0, dw);
    }

    private void movePos(int pos, int i, DownloadWrapper dw) {
        rows.remove(i);
        if (pos > i) {
            pos--;
        }
        rows.add(pos, dw);
    }

    private void moveBottom(int i, DownloadWrapper dw) {
        if (i == rows.size() - 1) {
            return;
        }
        rows.remove(dw);
        rows.add(dw);
    }

    public void EVENT_openfile(ActionEvent e) {
        int selection[] = table.getSelectedRows();

        if (selection == null || selection.length == 0) {
            return;
        }
        if (selection.length > 1) {
            OptionDialog.showErrorDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "onlyone"));
            return;
        }

        Download d = rows.get(selection[0]).download;
        if (d.isComplete() == true) {
            String path = ui.getCore().getSettings().getInternal().getDownloadfolder() + "\\" + d.getAuxInfoFilename();
            try {
                Desktop.getDesktop().open(new File(path));
            } catch (Exception ex) {
                OptionDialog.showErrorDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "openerror"));
            }
        } else {
            OptionDialog.showErrorDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "nodownload"));
        }
    }

    public void EVENT_opendownloaddir(ActionEvent e) {
        try {
            Desktop.getDesktop().open(new File(ui.getCore().getSettings().getInternal().getDownloadfolder()));
        } catch (Exception ex) {
        }
    }

    public void EVENT_openhddlink(ActionEvent e) {
        JFileChooser fc = new JFileChooser("");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (pathname.toString().endsWith("alliance") || pathname.isDirectory()) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return Language.getLocalizedString(getClass(), "alliancefile");
            }
        });
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getPath();
            downloadHddLink(path);
        }
    }

    private void downloadHddLink(String path) {
        try {
            File file = new File(path);
            InputStream reader = new FileInputStream(file);
            StringBuffer contents = new StringBuffer();
            byte[] charArray = new byte[(int) file.length()];

            reader.read(charArray);

            for (int i = 0; i < (int) file.length(); i++) {
                contents.append((char) ((charArray[i] & 0xFF) - 33));
            }

            String link = new String(contents);
            if (link.contains("|")) {
                String[] hashes = link.split("\\|");
                for (String s : hashes) {
                    if (s.length() < 2) {
                        OptionDialog.showInformationDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "notallowed"));
                        return;
                    }
                }
                int guid = Integer.parseInt(hashes[0]);
                if (OptionDialog.showQuestionDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "addlink", Integer.toString(hashes.length - 1)))) {
                    ArrayList<Integer> al = new ArrayList<Integer>();
                    al.add(guid);
                    for (int i = 1; i < hashes.length; i++) {
                        ui.getCore().getNetworkManager().getDownloadManager().queDownload(new Hash(hashes[i]), "Link from chat", al);//TODO: Translate
                    }
                    ui.getMainWindow().getMDIManager().selectWindow(ui.getMainWindow().getDownloadsWindow());
                }
            }
        } catch (IOException ex) {
        }
    }

    public void EVENT_remove(ActionEvent e) {
        int selection[] = table.getSelectedRows();

        if (selection != null && selection.length > 0) {
            for (int i : selection) {
                Download d = rows.get(i).download;
                if (!d.isComplete()) {
                    if (OptionDialog.showQuestionDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "remove"))) {
                        break;
                    } else {
                        return;
                    }
                }
            }

            final ArrayList<Download> dls = new ArrayList<Download>();
            for (int i : selection) {
                dls.add(rows.get(i).download);
            }
            ui.getCore().invokeLater(new Runnable() {

                @Override
                public void run() {
                    for (Download d : dls) {
                        if (d.isComplete()) {
                            ui.getCore().getNetworkManager().getDownloadManager().remove(d);
                        } else {
                            try {
                                ui.getCore().getNetworkManager().getDownloadManager().deleteDownload(d);
                            } catch (IOException e1) {
                                ui.handleErrorInEventLoop(e1);
                            }
                        }
                    }
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            update();
                        }
                    });
                }
            });
        }
    }

    public class ProgressBarCellRenderer extends JProgressBar implements TableCellRenderer {

        public ProgressBarCellRenderer() {
            super(0, 100);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
            if (value != null && value instanceof Integer) {
                setStringPainted(true);
                int v = (Integer) value;
                DownloadWrapper w = rows.get(rowIndex);
                if (w.state == Download.State.WAITING_TO_START) {
                    setString(Language.getLocalizedString(getClass().getEnclosingClass(), "queue"));
                    setValue(0);
                } else if (w.state == Download.State.COMPLETED) {
                    setString(Language.getLocalizedString(getClass().getEnclosingClass(), "complete"));
                    setValue(100);
                } else {
                    setValue(v);
                    setString(v + "%");
                }
                setToolTipText(getDownloadingFromText(w));
            }

            return this;
        }

        @Override
        public void validate() {
        }

        @Override
        public void revalidate() {
        }

        @Override
        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        }

        @Override
        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        }
    }
    private Thread listenThread;

    private void listenExternalLinks() {
        listenThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(2500);
                        if (!Main.getLink().isEmpty()) {
                            downloadHddLink(Main.getLink());
                            Main.clearLink();
                        }
                    } catch (InterruptedException ex) {
                    }
                }
            }
        });
        listenThread.setDaemon(true);
        listenThread.start();
    }
}
