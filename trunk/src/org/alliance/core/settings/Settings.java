package org.alliance.core.settings;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 14:01:02
 */
public class Settings {

    private ArrayList<Friend> friendlist;
    private ArrayList<Share> sharelist;
    private IPBlacklist rulelist;
    private ArrayList<Plugin> pluginlist;
    private Server server;
    private My my;
    private Internal internal;

    public Settings() {
        friendlist = new ArrayList<Friend>();
        sharelist = new ArrayList<Share>();
        rulelist = new IPBlacklist();
        pluginlist = new ArrayList<Plugin>();
        server = new Server();
        internal = new Internal();
        my = new My();
    }

    public Internal getInternal() {
        return internal;
    }

    public void setInternal(Internal internal) {
        this.internal = internal;
    }

    public My getMy() {
        return my;
    }

    public void setMy(My my) {
        this.my = my;
    }

    public ArrayList<Plugin> getPluginlist() {
        return pluginlist;
    }

    public void setPluginlist(ArrayList<Plugin> pluginlist) {
        this.pluginlist = pluginlist;
    }

    public ArrayList<Friend> getFriendlist() {
        return friendlist;
    }

    public void setFriendlist(ArrayList<Friend> friendlist) {
        this.friendlist = friendlist;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Friend getFriend(int guid) {
        //this kind of alternative iteration is used to circumvent possible concurrent modification problems
        Object a[] = friendlist.toArray();
        for (Object o : a) {
            if (((Friend) o).getGuid() == guid) {
                return (Friend) o;
            }
        }
        return null;
    }

    public void addFriend(Friend f) {
        if (friendlist == null) {
            friendlist = new ArrayList<Friend>();
        }
        friendlist.add(f);
    }

    public boolean hasFriend(Friend f) {
        for (Friend f2 : friendlist) {
            if (f2 == f) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Share> getSharelist() {
        return sharelist;
    }

    public void setSharelist(ArrayList<Share> sharelist) {
        this.sharelist = sharelist;
    }

    public IPBlacklist getRulelist() {
        return rulelist;
    }
    //Method is dirty...I know, but no other way around it?  I need a IPBlacklist..not a ArrayList

    public void setRulelist(ArrayList<Routerule> ruleList) {
        this.rulelist = new IPBlacklist();
        for (int i = 0; i < ruleList.size(); i++) {
            this.rulelist.add(ruleList.get(i));
        }
    }

    public void addShare(Share share) {
        if (sharelist == null) {
            sharelist = new ArrayList<Share>();
        }
        sharelist.add(share);
    }
}
