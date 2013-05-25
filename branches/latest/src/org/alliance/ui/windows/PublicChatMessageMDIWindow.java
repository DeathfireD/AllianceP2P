package org.alliance.ui.windows;

import org.alliance.core.comm.rpc.ChatMessageV3;
import org.alliance.core.node.Friend;
import org.alliance.ui.UISubsystem;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class PublicChatMessageMDIWindow extends AbstractChatMessageMDIWindow {

    public PublicChatMessageMDIWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "publicchat", ui);

        setTitle("Chat");

        postInit();
    }

    @Override
    public void send(final String text) throws Exception {
        if (text == null || text.trim().length() == 0) {
            return;
        }
        ui.getCore().invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    for (Friend f : ui.getCore().getFriendManager().friends()) {
                        ui.getCore().getFriendManager().getNetMan().sendPersistantly(new ChatMessageV3(text, true), f);
                    }
                } catch (IOException e) {
                    ui.getCore().reportError(e, this);
                }
            }
        });
        chat.setText("");
        ui.getMainWindow().publicChatMessage(ui.getCore().getFriendManager().getMe().getGuid(), text, System.currentTimeMillis(), false);
    }

    @Override
    public String getIdentifier() {
        return "publicchat";
    }
}
