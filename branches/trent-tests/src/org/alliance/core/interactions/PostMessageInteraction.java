package org.alliance.core.interactions;

import org.alliance.core.SynchronizedNeedsUserInteraction;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-21
 * Time: 20:20:31
 * To change this template use File | Settings | File Templates.
 */
public class PostMessageInteraction extends SynchronizedNeedsUserInteraction {

    private String message;
    private long sentAtTick;
    private int fromGuid;
    private boolean messageWasPersisted;

    public PostMessageInteraction(String message, int fromGuid) {
        this.message = message;
        this.fromGuid = fromGuid;
        sentAtTick = System.currentTimeMillis();
    }

    public PostMessageInteraction(String message, int fromGuid, long tick) {
        this.message = message;
        this.fromGuid = fromGuid;
        this.sentAtTick = tick;
    }

    public PostMessageInteraction(String message, int fromGuid, long tick, boolean messageWasPersisted) {
        this.message = message;
        this.fromGuid = fromGuid;
        this.sentAtTick = tick;
        this.messageWasPersisted = messageWasPersisted;
    }

    public String getMessage() {
        return message;
    }

    public long getSentAtTick() {
        return sentAtTick;
    }

    public int getFromGuid() {
        return fromGuid;
    }

    public boolean isMessageWasPersisted() {
        return messageWasPersisted;
    }
}
