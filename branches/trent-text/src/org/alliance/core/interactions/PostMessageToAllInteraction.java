package org.alliance.core.interactions;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-21
 * Time: 20:20:31
 * To change this template use File | Settings | File Templates.
 */
public class PostMessageToAllInteraction extends PostMessageInteraction {

    public PostMessageToAllInteraction(String message, int fromGuid) {
        super(message, fromGuid);
    }

    public PostMessageToAllInteraction(String message, int fromGuid, long tick) {
        super(message, fromGuid, tick);
    }

    public PostMessageToAllInteraction(String message, int fromGuid, long tick, boolean messageWasPersisted) {
        super(message, fromGuid, tick, messageWasPersisted);
    }
}
