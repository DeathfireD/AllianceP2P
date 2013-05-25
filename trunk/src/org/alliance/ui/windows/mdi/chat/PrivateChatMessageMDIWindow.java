package org.alliance.ui.windows.mdi.chat;

import org.alliance.core.Language;
import org.alliance.core.comm.rpc.ChatMessage;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.UISound;

import java.io.File;
import java.io.IOException;

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
        Language.translateXUIElements(getClass(), xui.getXUIComponents());
        setTitle(Language.getLocalizedString(getClass(), "title", ui.getCore().getFriendManager().nickname(guid)));

        postInit();
        chat.setText(Language.getLocalizedString(getClass(), "info"));
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
                    ui.getCore().getFriendManager().getNetMan().sendPersistantly(new ChatMessage(text, false), ui.getCore().getFriendManager().getFriend(guid));
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
