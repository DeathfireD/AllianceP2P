package org.alliance.ui.windows.mdi.chat;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import org.alliance.core.Language;
import org.alliance.ui.UISubsystem;

/**
 *
 * @author Bastvera
 */
public class HistoryChatMessageMDIWindow extends AbstractChatMessageMDIWindow {

    private JComboBox chatCombo;
    private JCheckBox links;
    private ArrayList<String> chatTypes = new ArrayList<String>();
    private final static String PUBLIC_CHAT_ID = "Chat";

    public HistoryChatMessageMDIWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "historychat", ui);
        Language.translateXUIElements(getClass(), xui.getXUIComponents());
        setTitle(Language.getLocalizedString(getClass(), "title"));


        links = ((JCheckBox) xui.getComponent("links"));
        chatCombo = ((JComboBox) xui.getComponent("type"));
        chatCombo.setMaximumRowCount(4);
        chatCombo.addItem(Language.getLocalizedString(getClass(), "public"));

        chatCombo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                reloadHistory();
            }
        });

        chatTypes.add(PUBLIC_CHAT_ID);

        postInit();
        loadHistory(PUBLIC_CHAT_ID, null);
    }

    private void addMessage(String from, String message, long tick) {
        int n = from.hashCode();
        if (n < 0) {
            n = -n;
        }
        n %= COLORS.length;
        Color c = COLORS[n];

        ChatLine cl = new ChatLine(from, message, tick, c);
        chatLines.add(cl);
    }

    private synchronized void loadHistory(String chat, String filter) {
        chatLines.clear();
        previousChatLine = null;
        DataInputStream in = null;
        try {
            File history = new File(ui.getCore().getSettings().getInternal().getHistoryfile());
            in = new DataInputStream(new FileInputStream(history));
            in.readInt();
            try {
                while (true) {
                    String chatName = in.readUTF();
                    String from = in.readUTF();
                    String message = in.readUTF();
                    long tick = in.readLong();
                    if (!chatTypes.contains(chatName)) {
                        chatTypes.add(chatName);
                        chatCombo.addItem(chatName);
                    }
                    if (chatName.equals(chat)) {
                        if (filter == null || filter.isEmpty()) {
                            if (links.isSelected() && message.contains("a href=")) {
                                addMessage(from, message, tick);
                            } else if (!links.isSelected()) {
                                addMessage(from, message, tick);
                            }
                        } else if (message.toLowerCase().contains(filter.toLowerCase())) {
                            if (links.isSelected() && message.contains("a href=")) {
                                addMessage(from, message, tick);
                            } else if (!links.isSelected()) {
                                addMessage(from, message, tick);
                            }
                        }
                    }
                }
            } catch (EOFException ex) {
                //EOF so continue
            }
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        regenerateHtml();
        needToUpdateHtml = true;
    }

    private void reloadHistory() {
        loadHistory(chatTypes.get(chatCombo.getSelectedIndex()), chat.getText());
    }

    public void EVENT_links(ActionEvent e) throws Exception {
        reloadHistory();
    }

    @Override
    public void EVENT_chat(ActionEvent e) throws Exception {
        reloadHistory();
    }

    public void EVENT_filter(ActionEvent e) throws Exception {
        reloadHistory();
    }

    @Override
    public void send(final String text) throws Exception {
        return;
    }

    @Override
    public void EVENT_cleanup(ActionEvent a) throws Exception {
        return;
    }

    @Override
    public String getIdentifier() {
        return "historychat";
    }
}
