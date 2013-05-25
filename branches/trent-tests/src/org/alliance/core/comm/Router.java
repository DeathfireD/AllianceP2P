package org.alliance.core.comm;

import org.alliance.core.node.Friend;
import org.alliance.core.node.FriendManager;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 21:22:29
 * To change this template use File | Settings | File Templates.
 */
public class Router {

    private class Route {

        int friendGuid;
        byte hops;

        public Route(int friendGuid, byte hops) {
            this.friendGuid = friendGuid;
            this.hops = hops;
        }
    }
    private FriendManager manager;
    private HashMap<Integer, Route[]> routeTable = new HashMap<Integer, Route[]>();

    public Router(FriendManager manager) {
        this.manager = manager;
    }

    public void updateRouteTable(Friend friend, int nodeGuid, int hops) {
        if (manager.getFriend(nodeGuid) != null) {
            return;
        }
        if (hops == 1) {
            if (T.netTrace) {
                T.trace("Number of hops is 1. Can find this user using the friend managers friends of friends. Don't add him to the route table - this saves a lot of memory");
            }
            return;
        }
        Route routes[] = routeTable.get(nodeGuid);
        System.out.println("Updating route table. Friend: " + friend + ", remote is friend: " + (manager.getFriend(nodeGuid) != null) + " table size: " + routeTable.size() + ", total size: " + totalSize());
        if (routes == null) {
            if (T.netTrace) {
                T.trace("Adding new Route to " + nodeGuid + " via " + friend);
            }
            routeTable.put(nodeGuid, new Route[]{new Route(friend.getGuid(), (byte) hops)});
        } else {
            if (T.netTrace) {
                T.trace("Updating existing Route array (Route to " + nodeGuid + " via " + friend + ")");
            }
            for (Route r : routes) {
                if (r.friendGuid == friend.getGuid()) {
                    if (r.hops > hops) {
                        if (T.netTrace) {
                            T.trace("Updating number of hops for existing Route to" + nodeGuid + ". Old: " + r.hops + " new: " + hops);
                        }
                        r.hops = (byte) hops;
                        return;
                    }
                }
            }

            if (T.netTrace) {
                T.trace("Adding new Route");
            }
            Route r[] = new Route[routes.length + 1];
            r[0] = new Route(friend.getGuid(), (byte) hops);
            System.arraycopy(routes, 0, r, 1, routes.length);
            routeTable.put(nodeGuid, r);
        }
    }

    private int totalSize() {
        int n = 0;
        for (Route[] routes : routeTable.values()) {
            n += routes.length;
        }
        return n;
    }

    public Friend findClosestFriend(int nodeGuid) {
        if (manager.getMyGUID() == nodeGuid) {
            if (T.t) {
                T.warn("Trying to Route to myself");
            }
        }

        if (manager.getFriend(nodeGuid) != null) {
            Friend f = manager.getFriend(nodeGuid);
            if (!f.isConnected()) {
                return null;
            }
            return f;
        }

        if (manager.getUntrustedNode(nodeGuid) != null) {
            //a friend of mine should have this node as a friend. Let's try to find him this way - this is why we don't have to save routes that are one hop away
            for (Friend friend : manager.friends()) {
                if (friend.getFriendsFriends().get(nodeGuid) != null) {
                    return friend;
                }
            }
        }

        Route routes[] = routeTable.get(nodeGuid);
        if (routes == null) {
            return null;
        }
        int minHops = 0, index = -1;
        for (int i = 0; i < routes.length; i++) {
            if (i == 0 || minHops > routes[i].hops && manager.getFriend(routes[i].friendGuid).isConnected()) {
                index = i;
                minHops = routes[i].hops;
            }
        }
        if (index == -1) {
            return null;
        }
        return manager.getFriend(routes[index].friendGuid);
    }
}
