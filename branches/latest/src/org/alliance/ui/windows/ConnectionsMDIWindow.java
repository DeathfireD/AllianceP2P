package org.alliance.ui.windows;

import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.util.TextUtils;
import org.alliance.core.comm.Connection;
import org.alliance.ui.UISubsystem;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JTable;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class ConnectionsMDIWindow extends AllianceMDIWindow {

    private JTable table;
    private ConnectionsTableModel model;
    private ArrayList<ConnectionWrapper> rows = new ArrayList<ConnectionWrapper>();

    public ConnectionsMDIWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "connections", ui);

        table = (JTable) xui.getComponent("table");
        table.setModel(model = new ConnectionsTableModel());

        updateConnectionData();

        setTitle("TCP/IP Connections");
        postInit();
    }

    public void updateConnectionData() {
        boolean structureChanged = false;

        ArrayList<Connection> al = new ArrayList<Connection>(ui.getCore().getFriendManager().getNetMan().connections());
        for (Connection c : al) {
            ConnectionWrapper cw = getWrapperFor(c);
            if (cw == null) {
                structureChanged = true;
                cw = new ConnectionWrapper(c);
                rows.add(cw);
            }
            cw.update();
        }

        for (Iterator i = rows.iterator(); i.hasNext();) {
            ConnectionWrapper w = (ConnectionWrapper) i.next();
            if (!ui.getCore().getFriendManager().getNetMan().contains(w.connection.getKey())) {
                structureChanged = true;
                i.remove();
            }
        }

        if (structureChanged) {
            model.fireTableStructureChanged();
        } else {
            model.fireTableRowsUpdated(0, rows.size());
        }
    }

    private ConnectionWrapper getWrapperFor(Connection c) {
        for (ConnectionWrapper cw : rows) {
            if (cw.connection == c) {
                return cw;
            }
        }
        return null;
    }

    @Override
    public String getIdentifier() {
        return "connections";
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

    private class ConnectionWrapper {

        public Connection connection;
        public String name, status, sent, received, dir;

        public ConnectionWrapper(Connection connection) {
            this.connection = connection;
        }

        public void update() {
            name = connection.toString();
            status = connection.getStatusString();
            sent = TextUtils.formatByteSize(connection.getBytesSent()) + " (" + connection.getBandwidthOut().getCPSHumanReadable() + ")";
            received = TextUtils.formatByteSize(connection.getBytesReceived()) + " (" + connection.getBandwidthIn().getCPSHumanReadable() + ")";
            dir = connection.getDirection().toString();
        }
    }

    private class ConnectionsTableModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return "Connected to";
                case 1:
                    return "Status";
                case 2:
                    return "Sent";
                case 3:
                    return "Received";
                case 4:
                    return "Direction";
                default:
                    return "undefined";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return rows.get(rowIndex).name;
                case 1:
                    return rows.get(rowIndex).status;
                case 2:
                    return rows.get(rowIndex).sent;
                case 3:
                    return rows.get(rowIndex).received;
                case 4:
                    return rows.get(rowIndex).dir;
                default:
                    return "undefined";
            }
        }
    }
}
