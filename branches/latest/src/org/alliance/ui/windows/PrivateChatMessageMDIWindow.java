package org.alliance.ui.windows;

import org.alliance.core.comm.rpc.ChatMessageV3;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.UISound;

import java.io.File;
import java.io.IOException;
import javax.swing.SwingUtilities;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class PrivateChatMessageMDIWindow extends AbstractChatMessageMDIWindow {

    private int guid;

    public PrivateChatMessageMDIWindow(UISubsystem ui, int guid) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "chatmessage", ui);
        this.guid = guid;

        setTitle("Private chat with " + ui.getCore().getFriendManager().nickname(guid));

        postInit();
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                chat.setText("Type here and then press 'send' to start chatting.");
                chat.selectAll();
            }
        });
    }

    @Override
    public void addMessage(String from, String message, long tick, boolean messageHasBeenQueuedAwayForAWhile) {
        super.addMessage(from, message, tick, messageHasBeenQueuedAwayForAWhile);
        if (!ui.getCore().getSettings().getMy().getNickname().equals(from)) {
            UISound Sound = new UISound(new File(ui.getCore().getSettings().getInternal().getPmsound()));
            Sound.start();
        }
    }

    @Override
    protected void send(final String text) throws IOException {
        if (text == null || text.trim().length() == 0) {
            return;
        }
        ui.getCore().invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    ui.getCore().getFriendManager().getNetMan().sendPersistantly(new ChatMessageV3(text, false), ui.getCore().getFriendManager().getFriend(guid));
                } catch (IOException e) {
                    ui.getCore().reportError(e, this);
                }
            }
        });
        chat.setText("");
        addMessage(ui.getCore().getFriendManager().getMe().getNickname(), text, System.currentTimeMillis(), false);
    }

    @Override
    public String getIdentifier() {
        return "msg" + guid;
    }
}
