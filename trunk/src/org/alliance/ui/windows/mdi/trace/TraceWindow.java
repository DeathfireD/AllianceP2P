package org.alliance.ui.windows.mdi.trace;

import org.alliance.core.trace.TraceHandler;
import org.alliance.core.trace.Trace;
import com.stendahls.XUI.XUIFrame;
import com.stendahls.util.resourceloader.GeneralResourceLoader;
import com.stendahls.util.resourceloader.ResourceLoader;
import org.alliance.ui.dialogs.ErrorDialog;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class TraceWindow extends XUIFrame implements TraceHandler {

    private JTable table;
    private TraceTableModel model;
    private JScrollPane scrollPane;
    private Hashtable channels;
    private JPanel channelPanel;
    private JCheckBox trace;
    private JCheckBox info;
    private JCheckBox debug;
    private JCheckBox autoscroll;
    private static ResourceLoader rl = new GeneralResourceLoader(ErrorDialog.class);
    private static final String STATE_FILENAME = System.getProperty("user.home") + "/TraceWindow_" + System.getProperty("tracewindow.id") + ".state";
    private boolean killUpdateThread;

    public TraceWindow() {
        this(true);
    }

    public TraceWindow(boolean hookTrace) {
        this.channels = new Hashtable();
        try {
            init(rl, rl.getResourceStream("res/xui/template/tracemdi.xui.xml"));
            SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());

            this.table = ((JTable) this.xui.getComponent("table"));
            this.scrollPane = ((JScrollPane) this.xui.getComponent("scroll"));
            this.channelPanel = ((JPanel) this.xui.getComponent("channels"));
            this.trace = ((JCheckBox) this.xui.getComponent("trace"));
            this.trace.setSelected(true);
            this.info = ((JCheckBox) this.xui.getComponent("info"));
            this.info.setSelected(true);
            this.debug = ((JCheckBox) this.xui.getComponent("debug"));
            this.debug.setSelected(true);
            this.autoscroll = ((JCheckBox) this.xui.getComponent("autoscroll"));
            this.autoscroll.setSelected(true);

            this.model = new TraceTableModel();
            this.table.setModel(this.model);
            this.table.setDefaultRenderer(Object.class, new ColoredRenderer(this.model));
            this.table.getColumnModel().getColumn(0).setPreferredWidth(50);
            this.table.getColumnModel().getColumn(1).setPreferredWidth(50);
            this.table.getColumnModel().getColumn(2).setPreferredWidth(100);
            this.scrollPane.getVerticalScrollBar().setUnitIncrement(24);

            this.table.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        System.err.println(TraceWindow.this.model.getTraceLine(TraceWindow.this.table.getSelectedRow()).message);
                        System.err.println("at:");
                        TraceWindow.this.model.getTraceLine(TraceWindow.this.table.getSelectedRow()).e.printStackTrace();
                    }
                }
            });
            this.xui.setEventHandler(this);

            if (hookTrace) {
                loadState();
                setVisible(true);
                Trace.handler = this;
            }
        } catch (Exception e) {
            System.err.println("Could not start trace window.");
            e.printStackTrace();
        }
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b) {
            return;
        }
        this.killUpdateThread = true;
    }

    private void saveState() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(STATE_FILENAME));
            this.model.save(out);
            out.writeInt(getLocation().x);
            out.writeInt(getLocation().y);
            out.writeInt(getSize().width);
            out.writeInt(getSize().height);
            out.writeInt(getExtendedState());
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadState() {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(STATE_FILENAME));
            this.model.load(in);

            int x = in.readInt();
            int y = in.readInt();
            int w = in.readInt();
            int h = in.readInt();
            int s = in.readInt();
            setLocation(x, y);
            setSize(w, h);
            setExtendedState(s);
            in.close();
        } catch (Exception e) {
            display();
        }
    }

    public void print(int level, Object message) {
        print(level, message, new Exception());
    }

    @Override
    public void print(final int level, final Object message, Exception stackTrace) {
        final Exception st = (stackTrace == null) ? new Exception() : stackTrace;
        if (!(this.table.isVisible())) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                String s = message.toString();
                int i = s.indexOf(32);
                String channel = s.substring(0, i);
                String m = s.substring(i + 1);

                if (TraceWindow.this.model != null) {
                    if (TraceWindow.this.channels.get(channel) == null) {
                        TraceWindow.this.addNewChannel(channel);
                    }
                    TraceWindow.this.model.print(level, channel, m, st);
                }
            }
        });
    }

    private void addNewChannel(final String channel) {
        final JCheckBox cb = new JCheckBox(channel);
        cb.setSelected(true);
        cb.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                TraceWindow.this.setChannelFilter(channel, cb.isSelected());
            }
        });
        this.channelPanel.add(cb);
        this.channels.put(channel, cb);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                TraceWindow.this.invalidate();
                TraceWindow.this.validate();
            }
        });
    }

    private void setChannelFilter(String channel, boolean selected) {
        if (selected) {
            this.model.removeChannelFilter(channel);
        } else {
            this.model.addChannelFilter(channel);
        }
        saveState();
    }

    private void setLevelFilter(int level, boolean selected) {
        if (selected) {
            this.model.removeLevelFilter(level);
        } else {
            this.model.addLevelFilter(level);
        }
        saveState();
    }

    public void EVENT_trace(ActionEvent e) {
        setLevelFilter(0, this.trace.isSelected());
    }

    public void EVENT_debug(ActionEvent e) {
        setLevelFilter(1, this.debug.isSelected());
    }

    public void EVENT_info(ActionEvent e) {
        setLevelFilter(2, this.info.isSelected());
    }

    private class ColoredRenderer extends DefaultTableCellRenderer {

        private final Color[] LEVELS_BG = {new Color(16777215), new Color(16316927), new Color(15400931), new Color(16777145), new Color(16770019)};
        private final Color[] LEVELS_FG = {new Color(10592673), new Color(5592405), new Color(0), new Color(0), new Color(0)};
        private TraceWindow.TraceTableModel model;

        public ColoredRenderer(TraceWindow.TraceTableModel paramTraceTableModel) {
            this.model = paramTraceTableModel;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            setEnabled((table == null) || (table.isEnabled()));

            TraceWindow.TraceLine tl = this.model.getTraceLine(row);
            setBackground(this.LEVELS_BG[tl.level]);
            setForeground(this.LEVELS_FG[tl.level]);

            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            return this;
        }
    }

    private class TraceTableModel extends AbstractTableModel {

        private transient ArrayList lines = new ArrayList(4096);
        private HashSet filteredChannels = new HashSet(10);
        private boolean[] filteredLevels = {false, false, false, false, false};
        private static final int MAX_N_LINES = 5000;

        public TraceTableModel() {
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    int lastNRows = TraceWindow.TraceTableModel.this.getRowCount();
                    while (!(TraceWindow.this.killUpdateThread)) {
                        final int nRows = TraceWindow.TraceTableModel.this.getRowCount() - lastNRows;
                        lastNRows = TraceWindow.TraceTableModel.this.getRowCount();
                        if (nRows > 0) {
                            SwingUtilities.invokeLater(new Runnable() {

                                @Override
                                public void run() {
                                    TraceWindow.TraceTableModel.this.fireTableRowsInserted(TraceWindow.TraceTableModel.this.getRowCount() - nRows, TraceWindow.TraceTableModel.this.getRowCount() - 1);
                                    SwingUtilities.invokeLater(new Runnable() {

                                        @Override
                                        public void run() {
                                            if (!(TraceWindow.this.autoscroll.isSelected())) {
                                                return;
                                            }
                                            TraceWindow.this.table.scrollRectToVisible(new Rectangle(0, TraceWindow.this.table.getHeight() - 1, 1, 1));
                                        }
                                    });
                                }
                            });
                        }

                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            });
            t.start();
        }

        @Override
        public int getRowCount() {
            int c = 0;
            for (int i = 0; i < this.lines.size(); ++i) {
                TraceWindow.TraceLine tl = (TraceWindow.TraceLine) this.lines.get(i);
                if ((this.filteredLevels == null) || (this.filteredChannels == null) || (tl == null) || (this.filteredChannels.contains(tl.channel)) || (this.filteredLevels[tl.level] != false)) {
                    continue;
                }

                ++c;
            }
            return c;
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if ((columnIndex < 0) || (columnIndex >= 3)) {
                return null;
            }
            if ((rowIndex < 0) || (rowIndex >= getRowCount())) {
                return null;
            }

            TraceWindow.TraceLine tl = getTraceLine(rowIndex);

            if (columnIndex == 0) {
                return "" + tl.row;
            }
            if (columnIndex == 1) {
                return tl.channel;
            }
            if (columnIndex == 2) {
                return tl.message;
            }
            return null;
        }

        public void print(int level, String channel, String message, Exception e) {
            this.lines.add(new TraceWindow.TraceLine(message, channel, level, this.lines.size(), e));
            if (this.lines.size() > 5000) {
                this.lines = new ArrayList(this.lines.subList(this.lines.size() / 4, this.lines.size() - 1));
                fireTableDataChanged();
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        if (TraceWindow.this.autoscroll.isSelected()) {
                            TraceWindow.this.table.scrollRectToVisible(new Rectangle(0, TraceWindow.this.table.getHeight() - 1, 1, 1));
                        }
                        TraceWindow.this.repaint();
                    }
                });
            }
        }

        @Override
        public String getColumnName(int col) {
            if (col == 0) {
                return "Line";
            }
            if (col == 1) {
                return "Channel";
            }
            return "Message";
        }

        public TraceWindow.TraceLine getTraceLine(int rowIndex) {
            int c = 0;
            for (int i = 0; i < this.lines.size(); ++i) {
                TraceWindow.TraceLine tl = (TraceWindow.TraceLine) this.lines.get(i);
                if ((this.filteredChannels == null) || (tl == null) || (this.filteredChannels.contains(tl.channel)) || (this.filteredLevels[tl.level] != false)) {
                    continue;
                }
                if (rowIndex == c) {
                    return tl;
                }
                ++c;
            }

            return null;
        }

        public void addChannelFilter(String channel) {
            if (!(this.filteredChannels.contains(channel))) {
                this.filteredChannels.add(channel);
                fireTableDataChanged();
            }
        }

        public void removeChannelFilter(String channel) {
            if (this.filteredChannels.contains(channel)) {
                this.filteredChannels.remove(channel);
                fireTableDataChanged();
            }
        }

        public void addLevelFilter(int level) {
            if (this.filteredLevels[level] == false) {
                this.filteredLevels[level] = true;
                fireTableDataChanged();
            }
        }

        public void removeLevelFilter(int level) {
            if (this.filteredLevels[level] != false) {
                this.filteredLevels[level] = false;
                fireTableDataChanged();
            }
        }

        public void save(ObjectOutputStream out) throws IOException {
            out.writeObject(this.filteredChannels);
            out.writeObject(this.filteredLevels);
        }

        public void load(ObjectInputStream in) throws IOException, ClassNotFoundException {
            this.filteredChannels = ((HashSet) in.readObject());
            this.filteredLevels = ((boolean[]) in.readObject());

            for (Iterator i = this.filteredChannels.iterator(); i.hasNext();) {
                String s = (String) i.next();
                TraceWindow.this.addNewChannel(s);
                ((JCheckBox) TraceWindow.this.channels.get(s)).setSelected(!(this.filteredChannels.contains(s)));
            }

            TraceWindow.this.trace.setSelected(this.filteredLevels[0] == false);
            TraceWindow.this.debug.setSelected(this.filteredLevels[1] == false);
            TraceWindow.this.info.setSelected(this.filteredLevels[2] == false);
        }
    }

    private static class TraceLine {

        String message;
        String channel;
        int level;
        Exception e;
        int row;

        public TraceLine(String message, String channel, int level, int row, Exception e) {
            this.message = message;
            this.channel = channel;
            this.level = level;
            this.row = row;
            this.e = e;
        }
    }
}
