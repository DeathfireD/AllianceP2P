package org.alliance.ui.windows;

import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.util.TextUtils;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.filetransfers.UploadConnection;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.ui.UISubsystem;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class UploadsMDIWindow extends AllianceMDIWindow {

    private UploadsMDIWindow.UploadsTableModel model;
    private JTable table;
    private ArrayList<UploadWrapper> rows = new ArrayList<UploadsMDIWindow.UploadWrapper>();

    public UploadsMDIWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "uploads", ui);

        table = (JTable) xui.getComponent("table");
        table.setModel(model = new UploadsMDIWindow.UploadsTableModel());
        table.setAutoCreateColumnsFromModel(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(50);

        update();

        setTitle("Uploads");
        postInit();
    }

    public void update() {
        boolean structureChanged = false;

        for (UploadWrapper w : rows) {
            w.speed = "Complete";
        }

        ArrayList<Connection> al = new ArrayList<Connection>(ui.getCore().getNetworkManager().connections());
        for (Connection c : al) {
            if (c instanceof UploadConnection) {
                UploadConnection uc = (UploadConnection) c;
                UploadWrapper w = getWrapperFor(uc);
                if (w == null) {
                    w = new UploadWrapper(uc);
                    rows.add(w);
                    structureChanged = true;
                }
                w.update();
            }
        }

        if (structureChanged) {
            model.fireTableStructureChanged();
        } else {
            model.fireTableRowsUpdated(0, rows.size());
        }

        ((JLabel) xui.getComponent("status")).setText("Total bytes sent: " + TextUtils.formatByteSize(ui.getCore().getNetworkManager().getBandwidthOut().getTotalBytes()));
    }

    private UploadWrapper getWrapperFor(UploadConnection u) {
        for (UploadWrapper cw : rows) {
            if (cw.upload == u) {
                return cw;
            }
        }
        return null;
    }

    @Override
    public String getIdentifier() {
        return "uploads";
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

    private class UploadWrapper {

        public UploadConnection upload;
        public String nickname, filename, speed, sent;

        public UploadWrapper(UploadConnection uc) {
            this.upload = uc;
        }

        public void update() {
            try {
                if (upload == null || upload.getRemoteFriend() == null) {
                    return;
                }
                nickname = upload.getRemoteFriend().getNickname();
                FileDescriptor fd = ui.getCore().getFileManager().getFd(upload.getRoot());
                if (fd != null) {
                    filename = fd.getSubpath();
                }
                speed = TextUtils.formatByteSize((long) upload.getBandwidthOut().getCPS()) + "/s";
                sent = TextUtils.formatByteSize(upload.getBytesSent());
            } catch (IOException e) {
                ui.handleErrorInEventLoop(e);
            }
        }
    }

    private class UploadsTableModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return "Upload to";
                case 1:
                    return "File";
                case 2:
                    return "Speed";
                case 3:
                    return "Bytes sent";
                default:
                    return "undefined";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return rows.get(rowIndex).nickname;
                case 1:
                    return rows.get(rowIndex).filename;
                case 2:
                    return rows.get(rowIndex).speed;
                case 3:
                    return rows.get(rowIndex).sent;
                default:
                    return "undefined";
            }
        }
    }

    public void EVENT_cleanup(ActionEvent a) {
        if (rows.size() == 0) {
            return;
        }
        int n = rows.size();
        rows.clear();
        model.fireTableRowsDeleted(0, n - 1);
    }
}
