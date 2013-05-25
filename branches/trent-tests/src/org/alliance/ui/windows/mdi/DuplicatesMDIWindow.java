package org.alliance.ui.windows.mdi;

import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.util.TextUtils;
import org.alliance.core.Language;
import org.alliance.ui.UISubsystem;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class DuplicatesMDIWindow extends AllianceMDIWindow {

    private JTable table;
    private ArrayList<Dup> dups = new ArrayList<Dup>();

    public DuplicatesMDIWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "duplicates", ui);
        Language.translateXUIElements(getClass(), xui.getXUIComponents());

        ui.getToolbarActionManager().getButtonBy("delete", this).setEnabled(false);

        table = (JTable) xui.getComponent("table");
        table.setModel(new DuplicatesMDIWindow.TableModel());
        table.setAutoCreateColumnsFromModel(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(0).setCellRenderer(new MyCellRenderer());
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setCellRenderer(new MyCellRenderer());
        table.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setColumnSelectionAllowed(false);

        setTitle(Language.getLocalizedString(getClass(), "title"));
        HashMap<String, String> duplicates = ui.getCore().getFileManager().getFileDatabase().getDuplicates(8196);
        for (String path : duplicates.keySet()) {
            dups.add(new Dup(path, duplicates.get(path)));
        }
        duplicates.clear();
        ((JLabel) xui.getComponent("status")).setText(Language.getLocalizedString(getClass(), "size", (dups.size() < 8196 ? Integer.toString(dups.size()) : dups.size() + "+")));
        postInit();
    }

    public void EVENT_delete(ActionEvent e) throws Exception {
        //TODO: Duplicates Deleting
        /*if (table.getSelectedColumnCount() <= 0 && table.getSelectedRowCount() <= 0) {
        return;
        }      

        ArrayList<String> filesThatNeedHashing = new ArrayList<String>();
        ArrayList<String> al = new ArrayList<String>();
        for (int i : table.getSelectedRows()) {
        if (table.getSelectedColumn() == 0) {
        al.add(dups.get(i).inShare);
        filesThatNeedHashing.add(dups.get(i).duplicate);
        } else if (table.getSelectedColumn() == 1) {
        al.add(dups.get(i).duplicate);
        }
        }

        if (OptionDialog.showQuestionDialog(ui.getMainWindow(), "Are you sure you want to delete " + al.size() + " file(s)?")) {
        int deleted = 0;

        for (String s : al) {
        if (!new File(s).delete()) {
        } else {
        deleted++;
        }
        }

        //duplicates that no longer have their corresponding file in share - make sure they are hashed now.
        /*for (String f : filesThatNeedHashing) {
        ui.getCore().getShareManager().getShareScanner().signalFileCreated(f);
        }

        OptionDialog.showInformationDialog(ui.getMainWindow(), deleted + "/" + al.size() + " files deleted. Note that it might take a while before the duplicate list is updated.");
        revert();
        }*/
    }

    @Override
    public String getIdentifier() {
        return "duplicates";
    }

    @Override
    public void save() throws Exception {
    }

    @Override
    public void revert() throws Exception {
        manager.recreateWindow(this, new DuplicatesMDIWindow(ui));
    }

    @Override
    public void serialize(ObjectOutputStream out) throws IOException {
    }

    @Override
    public MDIWindow deserialize(ObjectInputStream in) throws IOException {
        return null;
    }

    private class TableModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return dups.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return Language.getLocalizedString(getClass().getEnclosingClass(), "share");
                case 1:
                    return Language.getLocalizedString(getClass().getEnclosingClass(), "duplicate");
                default:
                    return Language.getLocalizedString(getClass().getEnclosingClass(), "undefined");
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return dups.get(rowIndex).inShare;
                case 1:
                    return dups.get(rowIndex).duplicate;
                default:
                    return Language.getLocalizedString(getClass().getEnclosingClass(), "undefined");
            }
        }
    }

    private class Dup {

        public Dup(String inShare, String duplicate) {
            this.inShare = TextUtils.makeSurePathIsMultiplatform(inShare);
            this.duplicate = TextUtils.makeSurePathIsMultiplatform(duplicate);
        }
        String inShare, duplicate;
    }

    private class MyCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String s = String.valueOf(value);
            int i = s.lastIndexOf('/');
            if (i != -1) {
                s = s.substring(i + 1) + " (" + s.substring(0, i) + ")";
            }
            setText(s);
            return this;
        }
    }
}
