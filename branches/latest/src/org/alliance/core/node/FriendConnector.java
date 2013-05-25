package org.alliance.core.node;

import org.alliance.core.T;
import org.alliance.core.CoreSubsystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 17:31:19
 */
public class FriendConnector extends Thread {

    private static class Action {

        private Action(Friend f, Integer sleepDelay) {
            this.sleepDelay = sleepDelay;
            this.f = f;
        }
        Integer sleepDelay;
        Friend f;
    }
    private FriendManager manager;
    private boolean alive = true;
    private List<Action> actionQue = Collections.synchronizedList(new ArrayList<Action>());

    public FriendConnector(FriendManager manager) {
        this.manager = manager;
        setDaemon(true);
        setName("FriendConnector -- " + manager.getMe().getNickname());
        setPriority(MIN_PRIORITY);
    }

    @Override
    public void run() {
        if (CoreSubsystem.isRunningAsTestSuite()) {
            //all hell breaks loose if all clients attempt to connect to each other at the same time
            try {
                Thread.sleep((long) (1000 + Math.random() * 3000));
            } catch (InterruptedException e) {
            }
        }
        while (alive) {
            //get all friends and sort them by last seen online
            ArrayList<Friend> al = new ArrayList<Friend>(manager.friends());
            Collections.sort(al, new Comparator<Friend>() {

                @Override
                public int compare(Friend o1, Friend o2) {
                    long diff = o2.getLastSeenOnlineAt() - o1.getLastSeenOnlineAt();
                    if (diff > 0xffffff) {
                        diff = 0xffffff;
                    }
                    if (diff < -0xffffff) {
                        diff = -0xffffff;
                    }
                    return (int) diff;
                }
            });

            for (Friend f : al) {
                if (T.t) {
                    T.trace("Attempting to connect to another friend...");
                }
                //first take care of any queued actions
                while (actionQue.size() > 0) {
                    if (T.t) {
                        T.trace("Found something in action que!");
                    }
                    Action a = actionQue.get(0);
                    actionQue.remove(0);
                    if (a.sleepDelay != null && a.sleepDelay > 0) {
                        if (T.t) {
                            T.trace("Sleeping " + a.sleepDelay + "ms...");
                        }
                        try {
                            Thread.sleep(a.sleepDelay);
                        } catch (InterruptedException e) {
                        }
                    }
                    connect(a.f);
                }

                connect(f);
            }
            try {
                Thread.sleep(manager.getSettings().getInternal().getReconnectinterval() * 1000);
            } catch (InterruptedException e) {
            }
        }
    }

    private void connect(Friend f) {
        while (manager.getCore().getNetworkManager().getNetworkLayer().getNumberOfPendingConnections() > manager.getSettings().getInternal().getMaxpendingconnections()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        if (!f.isConnected()) {
            try {
                if (T.t) {
                    T.trace("Friendconnector trying to connect to " + f + " " + f.getFriendConnection());
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                manager.connect(f);
            } catch (IOException e) {
                if (T.t) {
                    T.trace("Friend unreachable: " + e);
                }
            }
        }
    }

    public void kill() {
        alive = false;
    }

    public void queHighPriorityConnectTo(Friend f, int dealyInMs) {
        if (f.isConnected()) {
            if (T.t) {
                T.info("Already connected to " + f + ", not connecting again.");
            }
            return;
        }
        if (T.t) {
            T.info("Queuing action: connect to " + f + " in " + dealyInMs + " ms.");
        }
        actionQue.add(new Action(f, dealyInMs));
        wakeUp();
    }

    public void queHighPriorityConnectTo(Friend f) {
        queHighPriorityConnectTo(f, 0);
    }

    public void wakeUp() {
        interrupt();
    }
}
