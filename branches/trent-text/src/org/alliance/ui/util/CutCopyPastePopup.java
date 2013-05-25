package org.alliance.ui.util;

import org.alliance.core.Language;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.text.JTextComponent;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2007-feb-18
 * Time: 13:02:04
 * To change this template use File | Settings | File Templates.
 */
public class CutCopyPastePopup extends JPopupMenu implements ActionListener {

    private JTextComponent target;

    public CutCopyPastePopup(JTextComponent target) {
        this.target = target;

        JMenuItem mi = new JMenuItem(Language.getLocalizedString(getClass(), "cut"));
        mi.addActionListener(this);
        mi.setActionCommand("cut");
        add(mi);

        mi = new JMenuItem(Language.getLocalizedString(getClass(), "copy"));
        mi.addActionListener(this);
        mi.setActionCommand("copy");
        add(mi);

        mi = new JMenuItem(Language.getLocalizedString(getClass(), "paste"));
        mi.addActionListener(this);
        mi.setActionCommand("paste");
        add(mi);

        add(new JSeparator());

        mi = new JMenuItem(Language.getLocalizedString(getClass(), "selectall"));
        mi.addActionListener(this);
        mi.setActionCommand("selectall");
        add(mi);

        target.addMouseListener(new MouseAdapter() {

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
                    show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if ("cut".equals(e.getActionCommand())) {
            target.cut();
        } else if ("copy".equals(e.getActionCommand())) {
            target.copy();
        } else if ("paste".equals(e.getActionCommand())) {
            target.paste();
        } else if ("selectall".equals(e.getActionCommand())) {
            target.selectAll();
        }
    }
}
