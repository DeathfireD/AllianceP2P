package org.alliance.ui.dialogs;

import com.stendahls.XUI.XUI;
import com.stendahls.XUI.XUIException;
import com.stendahls.ui.JHtmlLabel;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.alliance.core.Language;

public class OptionDialog extends JDialog implements ActionListener {

    public static final int BORDER = 5;
    private Map<Character, String> eventKeyMap = new HashMap();
    private KeyListener keyListener;
    public static final int INFORMATION_DIALOG = 0;
    public static final int QUESTION_DIALOG = 1;
    public static final int WARNING_DIALOG = 2;
    public static final int ERROR_DIALOG = 3;
    public static final int OK_BUTTON = 0;
    public static final int YES_NO_BUTTONS = 1;
    public static final int YES_NO_CANCEL_BUTTONS = 2;
    public static final int OK_CANCEL_BUTTONS = 3;
    private static String[] imageForType = {"information", "question", "warning", "error"};
    private static String[] buttonTexts = {"ok", "yes_no", "yes_no_cancel", "ok_cancel"};
    private static ImageIcon[] imageIcons;
    private boolean result = false;
    private boolean cancelled = false;

    public OptionDialog(JFrame parent, String title, String message, int dialogType, int buttonType) throws Exception {
        super(parent, title, true);
        init(title, message, dialogType, buttonType, false);
    }

    public OptionDialog(JDialog parent, String title, String message, int dialogType, int buttonType) throws Exception {
        super(parent, title, true);
        init(title, message, dialogType, buttonType, false);
    }

    public OptionDialog(JFrame parent, String title, String message, int dialogType, int buttonType, boolean customTitle) throws Exception {
        super(parent, title, true);
        init(title, message, dialogType, buttonType, customTitle);
    }

    public OptionDialog(JDialog parent, String title, String message, int dialogType, int buttonType, boolean customTitle) throws Exception {
        super(parent, title, true);
        init(title, message, dialogType, buttonType, customTitle);
    }

    private void init(String title, String message, int dialogType, int buttonType, boolean customTitle) throws Exception {
        if (customTitle) {
            setTitle(title);
        } else {
            setTitle(Language.getLocalizedString(getClass(), title));
        }
        setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, 0));

        this.keyListener = new KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent e) {
                String event = OptionDialog.this.eventKeyMap.get(Character.valueOf(e.getKeyChar()));
                if (event != null) {
                    OptionDialog.this.actionPerformed(new ActionEvent(e.getSource(), e.getID(), event));
                }
            }
        };
        panel.addKeyListener(this.keyListener);

        if (imageIcons != null) {
            JLabel l = new JLabel(imageIcons[dialogType]);
            l.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panel.add(l);
            panel.add(Box.createVerticalStrut(5));
        }

        JHtmlLabel l = new JHtmlLabel() {

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(400, super.getPreferredSize().height);
            }
        };
        l.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        message = message.replace('[', '<');
        message = message.replace(']', '>');
        message = message.replaceAll("\r\n", "<br>");
        message = message.replaceAll("\r", "<br>");
        message = message.replaceAll("\n", "<br>");
        l.setText(message);
        panel.add(l);
        add(panel, "Center");

        panel = new JPanel();
        panel.setLayout(new FlowLayout(1, 5, 5));
        switch (buttonType) {
            case 0:
                panel.add(createButton(Language.getLocalizedString(getClass(), "ok"), "ok", "o", true));
                break;
            case 1:
                panel.add(createButton(Language.getLocalizedString(getClass(), "yes"), "yes", "y", true));
                panel.add(createButton(Language.getLocalizedString(getClass(), "no"), "no", "n", false));
                break;
            case 2:
                panel.add(createButton(Language.getLocalizedString(getClass(), "yes"), "yes", "y", true));
                panel.add(createButton(Language.getLocalizedString(getClass(), "no"), "no", "n", false));
                panel.add(createButton(Language.getLocalizedString(getClass(), "cancel"), "cancel", "c", false));
                break;
            case 3:
                panel.add(createButton(Language.getLocalizedString(getClass(), "ok"), "ok", "o", true));
                panel.add(createButton(Language.getLocalizedString(getClass(), "cancel"), "cancel", "c", false));
        }

        add(panel, "South");

        setResizable(false);
    }

    private JButton createButton(String text, String eventid, String shortcut, boolean def) {
        final JButton b = new JButton(text);
        b.setMaximumSize(new Dimension(80, b.getPreferredSize().height));
        b.setPreferredSize(new Dimension(80, b.getPreferredSize().height));
        b.setMinimumSize(new Dimension(80, b.getPreferredSize().height));
        b.setActionCommand(eventid);
        b.addActionListener(this);
        b.setMnemonic(shortcut.charAt(0));
        if ((shortcut != null) && (shortcut.length() > 0)) {
            this.eventKeyMap.put(Character.valueOf(shortcut.charAt(0)), eventid);
        }
        if (def) {
            getRootPane().setDefaultButton(b);
            b.requestFocus();
            addWindowListener(new WindowAdapter() {

                @Override
                public void windowActivated(WindowEvent e) {
                    b.requestFocusInWindow();
                }
            });
        }
        b.addKeyListener(this.keyListener);
        SubstanceThemeHelper.setComponentToGeneralArea(b);
        return b;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public boolean showAndGetResult() {

        display();
        return this.result;
    }

    private void display() {
        pack();
        Dimension ss = getParent().getSize();
        if ((ss.width < 80) || (ss.height < 80)) {
            ss = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(ss.width / 2 - (getWidth() / 2), ss.height / 2 - (getHeight() / 2));
        } else {
            setLocation(getParent().getLocation().x + ss.width / 2 - (getWidth() / 2), getParent().getLocation().y + ss.height / 2 - (getHeight() / 2));
        }
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if ("ok".equals(e.getActionCommand())) {
            this.result = true;
        } else if ("cancel".equals(e.getActionCommand())) {
            this.cancelled = true;
            this.result = false;
        } else if ("yes".equals(e.getActionCommand())) {
            this.result = true;
        } else if ("no".equals(e.getActionCommand())) {
            this.result = false;
        } else {
            throw new RuntimeException("Unknown action command for option dialog " + e.getActionCommand());
        }
        dispose();
    }

    public static void showInformationDialog(JFrame parent, String message) {
        OptionDialog od = null;
        try {
            od = new OptionDialog(parent, "information", message, 0, 0);
            od.showAndGetResult();
        } catch (Exception e) {
            couldNotOpen(e);
        }
    }

    public static void showInformationDialog(JDialog parent, String message) {
        OptionDialog od = null;
        try {
            od = new OptionDialog(parent, "information", message, 0, 0);
            od.showAndGetResult();
        } catch (Exception e) {
            couldNotOpen(e);
        }
    }

    public static boolean showQuestionDialog(JFrame parent, String message) {
        OptionDialog od = null;
        try {
            od = new OptionDialog(parent, "question", message, 1, 1);
            return od.showAndGetResult();
        } catch (Exception e) {
            couldNotOpen(e);
        }
        return false;
    }

    public static boolean showQuestionDialog(JDialog parent, String message) {
        OptionDialog od = null;
        try {
            od = new OptionDialog(parent, "question", message, 1, 1);
            return od.showAndGetResult();
        } catch (Exception e) {
            couldNotOpen(e);
        }
        return false;
    }

    public static Boolean showConfirmDialog(JFrame parent, String message) {
        OptionDialog od = null;
        try {
            od = new OptionDialog(parent, "confirmation", message, 2, 2);
            boolean res = od.showAndGetResult();
            if (od.isCancelled()) {
                return null;
            }
            return Boolean.valueOf(res);
        } catch (Exception e) {
            couldNotOpen(e);
        }
        return Boolean.valueOf(false);
    }

    public static Boolean showConfirmDialog(JDialog parent, String message) {
        OptionDialog od = null;
        try {
            od = new OptionDialog(parent, "confirmation", message, 2, 2);
            boolean res = od.showAndGetResult();
            if (od.isCancelled()) {
                return null;
            }
            return Boolean.valueOf(res);
        } catch (Exception e) {
            couldNotOpen(e);
        }
        return Boolean.valueOf(false);
    }

    public static void showErrorDialog(JFrame parent, String message) {
        OptionDialog od = null;
        try {
            od = new OptionDialog(parent, "error", message, 3, 0);
            od.showAndGetResult();
        } catch (Exception e) {
            couldNotOpen(e);
        }
    }

    public static void showErrorDialog(JDialog parent, String message) {
        OptionDialog od = null;
        try {
            od = new OptionDialog(parent, "error", message, 3, 0);
            od.showAndGetResult();
        } catch (Exception e) {
            couldNotOpen(e);
        }
    }

    private static void couldNotOpen(Throwable t) {
        try {
            new ErrorDialog(t, false);
        } catch (XUIException e) {
            e.printStackTrace();
        }
    }

    static {
        try {
            imageIcons = new ImageIcon[buttonTexts.length];
            for (int i = 0; i < imageIcons.length; ++i) {
                imageIcons[i] = new ImageIcon(XUI.getGlobalResourceLoader().getResource("res/gfx/icons/" + imageForType[i] + ".png"));
            }
        } catch (Exception e) {
            System.err.println("WARNING: Could not load image for OptionDialog. Dialogs might not work.");
            e.printStackTrace();
        }
    }
}
