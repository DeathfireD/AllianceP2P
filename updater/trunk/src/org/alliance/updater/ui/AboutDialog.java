package org.alliance.updater.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

/**
 *
 * @author Bastvera
 */
public class AboutDialog extends JDialog {

    private JButton button;
    private JLabel label;

    public AboutDialog(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        centerOnScreen();
    }

    private void initComponents() {

        label = new JLabel();
        button = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("About");

        label.setFont(new Font("Dialog", 0, 12));
        label.setText("Alliance updater v1.1");

        button.setText("OK");
        button.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent evt) {
                buttonMousePressed(evt);
            }
        });

        javax.swing.GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                addGroup(layout.createSequentialGroup().addContainerGap().
                addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                addComponent(label)).
                addContainerGap(41, Short.MAX_VALUE)).
                addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().
                addContainerGap(109, Short.MAX_VALUE).addComponent(button).
                addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                addGroup(layout.createSequentialGroup().addContainerGap().
                addComponent(label).addGap(1, 1, 1).
                addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).
                addComponent(button).
                addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        pack();
    }

    private void buttonMousePressed(MouseEvent evt) {
        this.dispose();
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
}
