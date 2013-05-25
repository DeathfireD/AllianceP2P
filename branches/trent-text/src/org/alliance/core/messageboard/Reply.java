package org.alliance.core.messageboard;

/**
 * Created by IntelliJ IDEA.
 * User: Pontus
 * Date: 2006-apr-18
 * Time: 21:40:37
 * To change this template use File | Settings | File Templates.
 */
public class Reply extends Message {

    private int replyToMessageId;

    public Reply(String text, int createdBy, int replyToMessageId) {
        super(text, createdBy);
        this.replyToMessageId = replyToMessageId;
    }

    public int getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(int replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }
}
