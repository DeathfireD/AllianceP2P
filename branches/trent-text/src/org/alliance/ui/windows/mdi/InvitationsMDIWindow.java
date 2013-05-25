package org.alliance.ui.windows.mdi;

import com.stendahls.nif.ui.mdi.MDIWindow;
import org.alliance.core.Language;
import org.alliance.ui.UISubsystem;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import org.alliance.core.node.Invitation;
import org.alliance.core.node.Node;

/**
 * @author Bastvera
 */
public class InvitationsMDIWindow extends AllianceMDIWindow {

    private JTable table;
    private UISubsystem ui;
    private JPopupMenu popup;
    private DefaultTableModel model;

    public InvitationsMDIWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "invitations", ui);
        this.ui = ui;
        Language.translateXUIElements(getClass(), xui.getXUIComponents());

        model = new InvitationTableModel();
        table = (JTable) xui.getComponent("table");
        table.setModel(model);
        table.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setColumnSelectionAllowed(false);
        table.removeColumn(table.getColumnModel().getColumn(table.getColumnCount() - 1));
        table.getColumnModel().getColumn(0).setPreferredWidth(35);
        table.getColumnModel().getColumn(0).setMaxWidth(35);

        fillTable();

        popup = (JPopupMenu) xui.getComponent("popup");

        table.addMouseListener(new MouseAdapter() {

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

        setTitle(Language.getLocalizedString(getClass(), "title"));
        postInit();
    }

    private void fillTable() {
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }
        Collection<Invitation> invitations = ui.getCore().getInvitationManager().allInvitations();
        int n = 1;
        DateFormat dataformat = DateFormat.getDateInstance(DateFormat.LONG);
        for (Invitation i : invitations.toArray(new Invitation[invitations.size()])) {
            String destination;
            String middleman;
            String type;
            if (i.isForwardedInvitation()) {
                type = Language.getLocalizedString(getClass(), "forwarded");
                Node node = ui.getCore().getFriendManager().getNode(i.getDestinationGuid());
                Node midNode = ui.getCore().getFriendManager().getNode(i.getMiddlemanGuid());
                middleman = midNode.getNickname();
                if (node == null) {
                    destination = Language.getLocalizedString(getClass(), "notavailable");
                } else {
                    destination = node.getNickname();
                }
            } else {
                type = Language.getLocalizedString(getClass(), "manual");
                destination = Language.getLocalizedString(getClass(), "notavailable");
                middleman = Language.getLocalizedString(getClass(), "notavailable");
            }
            String time;
            if (i.isValidOnlyOnce()) {
                time = Language.getLocalizedString(getClass(), "validonce");
            } else {
                Long lTime = i.getValidTime();
                if (lTime > Integer.MAX_VALUE * 100L) {
                    time = Language.getLocalizedString(getClass(), "validinfinite");
                } else {
                    long hours = (lTime - (System.currentTimeMillis() - i.getCreatedAt())) / (60 * 60 * 1000);
                    long days = hours / 24;
                    hours -= (days * 24);
                    time = Language.getLocalizedString(getClass(), "timeleft", Long.toString(days), Long.toString(hours));
                }
            }
            Date date = new Date(i.getCreatedAt());
            Object[] o = {n++, i.getCompleteInvitaitonString(), type, middleman, destination,
                time, dataformat.format(date), i.getInvitationPassKey()};
            model.addRow(o);
        }
    }

    public void EVENT_delete(ActionEvent e) throws Exception {
        int selectedRow = table.convertRowIndexToModel(table.getSelectedRow());
        Integer guid = (Integer) model.getValueAt(selectedRow, model.getColumnCount() - 1);
        ui.getCore().getInvitationManager().consume(guid);
        model.removeRow(selectedRow);
    }

    public void EVENT_refresh(ActionEvent e) throws Exception {
        fillTable();
    }

    @Override
    public String getIdentifier() {
        return "invitations";
    }

    @Override
    public void save() throws Exception {
    }

    @Override
    public void serialize(ObjectOutputStream out) throws IOException {
    }

    @Override
    public MDIWindow deserialize(ObjectInputStream in) throws IOException {
        return null;
    }

    @Override
    public void revert() throws Exception {
        return;
    }

    private class InvitationTableModel extends DefaultTableModel {

        protected InvitationTableModel() {
            super();
            addColumn("#");
            addColumn(Language.getLocalizedString(getClass().getEnclosingClass(), "code"));
            addColumn(Language.getLocalizedString(getClass().getEnclosingClass(), "type"));
            addColumn(Language.getLocalizedString(getClass().getEnclosingClass(), "middleman"));
            addColumn(Language.getLocalizedString(getClass().getEnclosingClass(), "destination"));
            addColumn(Language.getLocalizedString(getClass().getEnclosingClass(), "validtime"));
            addColumn(Language.getLocalizedString(getClass().getEnclosingClass(), "created"));
            addColumn("guid");
        }

        @Override
        public String getColumnName(int column) {
            return super.getColumnName(column);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}
