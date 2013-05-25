package org.alliance.updater.ui;

import org.alliance.updater.core.Core;
import org.alliance.updater.core.OSInfo;
import org.alliance.updater.core.Update.Step;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.StyledDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

/**
 *
 * @author Bastvera
 */
public class MainWindow extends JFrame {

    private JButton button;
    private JMenu menuHelp;
    private JMenuBar menu;
    private JMenuItem menuHelpAbout;
    private JTextPane textPane;
    private final Core core;

    public MainWindow(Core core) {
        this.core = core;
        initComponents();
        centerOnScreen();
    }

    public void appendHeadText(String text) {
        StyledDocument doc = (StyledDocument) textPane.getDocument();
        try {
            doc.insertString(doc.getLength(), "\n* " + text + "\n", null);
            textPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException ex) {
        }
    }

    public void appendText(String text) {
        StyledDocument doc = (StyledDocument) textPane.getDocument();
        try {
            doc.insertString(doc.getLength(), "     " + text + "\n", null);
            textPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException ex) {
        }
    }

    public void finish() {
        try {
            StyledDocument doc = (StyledDocument) textPane.getDocument();
            Style style = doc.addStyle("bolded", null);
            StyleConstants.setBold(style, true);
            doc.insertString(doc.getLength(), "\n* All done. Press OK to start Alliance.\n", style);
            textPane.setCaretPosition(doc.getLength());
            button.setText("OK");
            button.setEnabled(true);
        } catch (BadLocationException ex) {
        }
    }

    public void showErrorDialog(Exception ex) {
        appendText(ex.toString());
        for (StackTraceElement element : ex.getStackTrace()) {
            appendText(element.toString());
        }
        if (OSInfo.isWindows()) {
            showDialog("Updating ... failed\nTry running again or update manually.\n", "Error", "error");
        } else {
            showDialog("Updating ... failed\nTry running updater as Administrator or update manually.\n", "Error", "error");
        }
    }

    public static File localizeFile(String filename, final String filter, final String filterText) {
        showDialog("Could not open \"" + filename + "\". Please click OK and select the file.", "Warning", "warning");
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(true);
        fc.addChoosableFileFilter(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (pathname.toString().endsWith(filter) || pathname.isDirectory()) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return (filterText);
            }
        });
        int returnVal = fc.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return new File(fc.getSelectedFile().getPath());
        } else {
            showDialog("File not selected. Updater will now close.", "Info", "info");
            System.exit(0);
            return null;
        }
    }

    public static void showDialog(String text, String header, String type) {
        if (type.equals("info")) {
            JOptionPane.showMessageDialog(null, text, header, JOptionPane.INFORMATION_MESSAGE);
        } else if (type.equals("warning")) {
            JOptionPane.showMessageDialog(null, text, header, JOptionPane.WARNING_MESSAGE);
        } else if (type.equals("error")) {
            JOptionPane.showMessageDialog(null, text, header, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void centerOnScreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = this.getSize();

        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }

        this.setLocation((screenSize.width - frameSize.width) / 2,
                (screenSize.height - frameSize.height) / 2);
    }

    private void menuHelpAboutMousePressed(MouseEvent evt) {
        new AboutDialog(this, false).setVisible(true);
    }

    private void buttonMousePressed(MouseEvent evt) {
        if (button.getText().equals("Update")) {
            button.setEnabled(false);
            button.setToolTipText(null);
            core.getUpdate().updateStep(Step.UNZIP);
        } else {
            core.restart();
        }
    }

    private void initComponents() {
        this.setTitle("Alliance Updater");
        this.setPreferredSize(new Dimension(640, 480));
        this.setResizable(false);

        this.setIconImage(new ImageIcon(getClass().getResource("/res/gfx/updater.png")).getImage());

        textPane = new JTextPane();
        textPane.setFont(new Font("Dialog", 0, 10));
        textPane.setEditable(false);
        textPane.setText("Ready to update.\n");


        JScrollPane scroll = new JScrollPane();
        scroll.setViewportView(textPane);
        scroll.setBorder(BorderFactory.createTitledBorder("Status:"));

        button = new JButton();
        menu = new JMenuBar();
        menuHelp = new JMenu();
        menuHelpAbout = new JMenuItem();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        button.setText("Update");
        button.setToolTipText("Press to begin updating");
        button.setMaximumSize(new Dimension(75, 25));
        button.setMinimumSize(new Dimension(75, 25));
        button.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent evt) {
                buttonMousePressed(evt);
            }
        });

        menuHelp.setText("Help");
        menuHelpAbout.setText("About");
        menuHelp.add(menuHelpAbout);
        menu.add(menuHelp);
        setJMenuBar(menu);
        menuHelpAbout.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent evt) {
                menuHelpAboutMousePressed(evt);
            }
        });

        javax.swing.GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                addGroup(layout.createSequentialGroup().
                addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                addGroup(GroupLayout.Alignment.CENTER, layout.createSequentialGroup().
                addComponent(button)).
                addGroup(layout.createSequentialGroup().
                addComponent(scroll, GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)))));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                addGroup(layout.createSequentialGroup().
                addComponent(scroll, GroupLayout.PREFERRED_SIZE, 390, GroupLayout.PREFERRED_SIZE).
                addGap(5).
                addComponent(button)));

        pack();
    }
}
