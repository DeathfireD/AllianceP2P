package org.alliance.core.messageboard;

import java.rmi.dgc.VMID;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Pontus
 * Date: 2006-apr-18
 * Time: 20:44:51
 * Represents a message in the message board.
 */
public abstract class Message {

    private VMID messageId;
    private String text;
    private Date created;
    private int createdBy;

    protected Message(String text, int createdBy) {
        this.messageId = new VMID();
        this.created = new Date();
        this.text = text;
        this.createdBy = createdBy;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public VMID getMessageId() {
        return messageId;
    }

    public void setMessageId(VMID messageId) {
        this.messageId = messageId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
