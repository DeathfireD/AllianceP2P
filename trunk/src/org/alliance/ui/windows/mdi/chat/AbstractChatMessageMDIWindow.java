package org.alliance.ui.windows.mdi.chat;

import com.stendahls.nif.ui.mdi.MDIManager;
import com.stendahls.nif.ui.mdi.MDIWindow;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.Language;
import org.alliance.ui.T;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.util.CutCopyPastePopup;
import org.alliance.ui.dialogs.OptionDialog;
import org.alliance.ui.windows.mdi.AllianceMDIWindow;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.TreeSet;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractChatMessageMDIWindow extends AllianceMDIWindow implements Runnable {

    private static final int MAXIMUM_NUMBER_OF_CHAT_LINES = 50;
    protected final static DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    protected final static DateFormat SHORT_FORMAT = new SimpleDateFormat("HH:mm");
    protected final static Color COLORS[] = {
        new Color(0x0068a7),
        new Color(0x009606),
        new Color(0xa13eaa),
        new Color(0x008b76),
        new Color(0xb77e24),
        new Color(0xef0000),
        new Color(0xb224b7),
        new Color(0xb77e24)
    };
    protected JEditorPane textarea;
    protected JTextField chat;
    protected String html = "";
    protected TreeSet<ChatLine> chatLines;
    protected boolean needToUpdateHtml;
    protected ChatLine previousChatLine = null;
    private boolean alive = true;

    protected AbstractChatMessageMDIWindow(MDIManager manager, String mdiWindowIdentifier, UISubsystem ui) throws Exception {
        super(manager, mdiWindowIdentifier, ui);
        this.ui = ui;
        chatLines = new TreeSet<ChatLine>(new Comparator<ChatLine>() {

            @Override
            public int compare(ChatLine o1, ChatLine o2) {
                long diff = o1.tick - o2.tick;
                if (diff <= Integer.MIN_VALUE) {
                    diff = Integer.MIN_VALUE + 1;
                }
                if (diff >= Integer.MAX_VALUE) {
                    diff = Integer.MAX_VALUE - 1;
                }
                return (int) diff;
            }
        });
    }

    protected abstract void send(final String text) throws IOException, Exception;

    @Override
    public abstract String getIdentifier();

    @Override
    protected void postInit() {
        textarea = new JEditorPane("text/html", "");
        if (ui.getCore().getSettings().getInternal().getChatfont() != null && !ui.getCore().getSettings().getInternal().getChatfont().isEmpty()) {
            textarea.setFont(new Font(ui.getCore().getSettings().getInternal().getChatfont(), Font.PLAIN,
                    ui.getCore().getSettings().getInternal().getChatsize()));
        }
        new CutCopyPastePopup(textarea);

        JScrollPane sp = (JScrollPane) xui.getComponent("scrollpanel");
        sp.setViewportView(textarea);

        chat = (JTextField) xui.getComponent("chat");
        chat.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent e) {
                chat.selectAll();
            }
        });

        new CutCopyPastePopup(chat);

        textarea.setEditable(false);
        textarea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        textarea.setBackground(Color.white);
        textarea.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        String link = e.getDescription();
                        if (link.startsWith("https://") || link.startsWith("http://") || link.startsWith("ftp://")) {
                            String allowedChars = "abcdefghijklmnopqrstuvwxyz\u00e5\u00e4\u00f60123456789-%.;/?:@&=+$_.!~*'()#";
                            for (int i = 0; i < link.length(); i++) {
                                if (allowedChars.indexOf(link.toLowerCase().charAt(i)) == -1) {
                                    OptionDialog.showInformationDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "invalid", Character.toString(link.charAt(i))));
                                    return;
                                }
                            }
                            ui.openURL(link);
                        } else if (link.contains("|")) {
                            String[] hashes = link.split("\\|");
                            for (String s : hashes) {
                                if (T.t) {
                                    T.trace("Part: " + s);
                                }
                                if (s.length() < 2) {
                                    OptionDialog.showInformationDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "notallowed"));
                                    return;
                                }
                            }
                            int guid = Integer.parseInt(hashes[0]);
                            if (OptionDialog.showQuestionDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "addlink", Integer.toString(hashes.length - 1)))) {
                                ArrayList<Integer> al = new ArrayList<Integer>();
                                al.add(guid);
                                for (int i = 1; i < hashes.length; i++) {
                                    ui.getCore().getNetworkManager().getDownloadManager().queDownload(new Hash(hashes[i]), "Link from chat", al);//TODO: LOCALIZE
                                }
                                ui.getMainWindow().getMDIManager().selectWindow(ui.getMainWindow().getDownloadsWindow());
                            }
                        } else {
                            OptionDialog.showInformationDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "notallowed"));
                            return;
                        }
                    } catch (IOException e1) {
                        ui.getCore().reportError(e1, this);
                    } catch (NumberFormatException e1) {
                        OptionDialog.showInformationDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "notallowed"));
                        return;
                    }
                }
            }
        });

        Thread t = new Thread(this);
        t.start();

        super.postInit();
    }

    public void EVENT_send(ActionEvent e) throws Exception {
        chatMessage();
    }

    public void EVENT_chat(ActionEvent e) throws Exception {
        chatMessage();
    }

    private void chatClear() {
        chatLines.clear();
        regenerateHtml();
        needToUpdateHtml = true;
        chat.setText("");
    }

    private void chatMessage() throws Exception {
        if (chat.getText().trim().equals("")) {//Empty
            return;
        } else if (chat.getText().contains(" ")) {//(Alt+255)
            return;
        } else if (chat.getText().trim().equals("/clear")) {
            chatClear();
            return;
        } else if (chat.getText().trim().equals("/clearlog")) {
            chatClear();
            ui.getCore().getPublicChatHistory().clearHistory();
            return;
        }
        send(chat.getText().replace("<", "&lt;")); //unHTML
    }

    private String checkLinks(String text, String pString) {
        int i = 0;
        while ((i = text.indexOf(pString, i)) != -1) {
            int end = text.indexOf(' ', i);
            if (end == -1) {
                end = text.length();
            }
            String s = text.substring(0, i);
            s += "<a href=\"" + text.substring(i, end) + "\">" + text.substring(i, end) + "</a>";
            i = s.length();
            if (end < text.length() - 1) {
                s += text.substring(end);
            }
            text = s;
        }
        return text;
    }

    private String escapeHTML(String text) { //Https/ftp support;
        text = text.replace("<", "&lt;"); //unHTML
        text = text.replace(">", "&gt;");
        text = text.replace("&lt;a href=\"", "");

        if (text.endsWith(" files)") && text.contains("|") && text.contains(" in ")) {
            text = text.replace("\"&gt;", " ");
            text = text.replace("&lt;/a&gt;", "");
            String link = text.substring(0, text.indexOf(" "));
            String link2 = text.substring(text.indexOf(" ") + 1, text.lastIndexOf(" ("));
            text = text.replace(link, "<a href=\"" + link + "\">");
            text = text.replace(" " + link2, link2 + "</a>");
        } else {
            text = text.replaceAll("\"&gt;\\S*&lt;/a&gt;", "");
            text = checkLinks(text, "https://");
            text = checkLinks(text, "http://");
            text = checkLinks(text, "ftp://");
        }
        text = text.trim();
        return text;
    }

    public void addMessage(String from, String message, long tick, boolean messageHasBeenQueuedAwayForAWhile) {
        addMessage(from, message, tick, messageHasBeenQueuedAwayForAWhile, true);
    }

    public void addMessage(String from, String message, long tick, boolean messageHasBeenQueuedAwayForAWhile, boolean saveToHistory) {
        if (chatLines != null && chatLines.size() > 0) {
            ChatLine l = chatLines.last();
            if (!messageHasBeenQueuedAwayForAWhile) {
                //A message gets queued away when a user is offline and cannot receive the message.
                //If this message has NOT been queued then it should always be displayed as the last message received 
                //in the chat
                tick = System.currentTimeMillis();
            }
        }
        if (tick > System.currentTimeMillis()) {
            //this can happen when another user has an incorrectly set clock. We dont't allow timestamps in the future.
            tick = System.currentTimeMillis();
        }

        int n = from.hashCode();
        if (n < 0) {
            n = -n;
        }
        n %= COLORS.length;
        Color c = COLORS[n];

        while (chatLinesContainTick(tick)) {
            tick++;
        }

        ChatLine cl = new ChatLine(from, message, tick, c);
        chatLines.add(cl);
        boolean removeFirstLine = chatLines.size() > MAXIMUM_NUMBER_OF_CHAT_LINES;
        if (removeFirstLine) {
            chatLines.remove(chatLines.first());
        }
        if (chatLines.last() == cl && !removeFirstLine) {
            html += createHtmlChatLine(cl);
        } else {
            regenerateHtml();
        }

        needToUpdateHtml = true;

        if (saveToHistory) {
            String chatID = this.toString().replaceAll(".*\\{|\\}.*|Private chat with ", "");
            ui.getCore().addToHistory(chatID, from, message, tick);
        }
    }

    private boolean chatLinesContainTick(long tick) {
        for (ChatLine cl : chatLines) {
            if (cl.tick == tick) {
                return true;
            }
        }
        return false;
    }

    protected void regenerateHtml() {
        if (T.t) {
            T.info("Regenerating entire html for chat");
        }
        html = "";
        for (ChatLine chatLine : chatLines) {
            html += createHtmlChatLine(chatLine);
        }
    }

    @Override
    public void run() {
        while (alive) {
            if (needToUpdateHtml) {
                needToUpdateHtml = false;
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        textarea.setText(html);
                    }
                });
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }

    private String createHtmlChatLine(ChatLine cl) {
        String s;
        DateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        if (previousChatLine != null
                && f.format(new Date(cl.tick)).equals(
                f.format(new Date(previousChatLine.tick)))) {
            s = "[" + SHORT_FORMAT.format(new Date(cl.tick)) + "]";
        } else {
            s = "<b>[" + FORMAT.format(new Date(cl.tick)) + "]</b>";
        }
        s = "<font color=\"#9f9f9f\">" + s + " <font color=\"" + toHexColor(cl.color) + "\">" + cl.from + ":</font> <font color=\"" + toHexColor(cl.color.darker()) + "\">" + cl.message + "</font><br>";

        previousChatLine = cl;
        return s;
    }

    protected String toHexColor(Color color) {
        return "#" + Integer.toHexString(color.getRGB() & 0xffffff);
    }

    protected class ChatLine {

        String from, message;
        long tick;
        Color color;

        public ChatLine(String from, String message, long tick, Color color) {
            this.from = from;
            this.message = escapeHTML(message);
            this.tick = tick;
            this.color = color;
        }
    }

    @Override
    public void windowClosed() {
        super.windowClosed();
        alive = false;
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

    public void EVENT_cleanup(ActionEvent a) throws Exception {
        chatClear();
        ui.getCore().getPublicChatHistory().clearHistory();
    }

    public void EVENT_cleanscreen(ActionEvent a) throws Exception {
        chatClear();
    }
}
