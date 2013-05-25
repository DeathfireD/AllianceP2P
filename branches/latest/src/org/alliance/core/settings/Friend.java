package org.alliance.core.settings;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 14:01:19
 */
public class Friend {

    private String nickname, host;
    private Integer guid, port;
    private Long lastseenonlineat;
    private Integer middlemanguid; //0 if a first degree friend, guid of middleman if introduced by someone to me
    private String renamednickname; //used to display a different nickname then the "anonymous" one in ones UI
    private String ugroupname; //Bastvera (settings.xml variable)
    private Integer trusted;

    public Friend() {
    }

    public Friend(String nickname, String lasthost, Integer guid, Integer lastport) {
        this.nickname = nickname;
        this.host = lasthost;
        this.guid = guid;
        this.port = lastport;
        this.ugroupname = ""; //Bastvera (Default group name for New Friend)
        this.trusted = 0;
    }

    public Friend(String nickname, String lasthost, Integer guid, Integer lastport, Integer introducedBy) {
        this(nickname, lasthost, guid, lastport);
        this.middlemanguid = introducedBy;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getGuid() {
        return guid;
    }

    public void setGuid(Integer guid) {
        this.guid = guid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public String toString() {
        return "Friend [" + nickname + "]";
    }

    public Long getLastseenonlineat() {
        return lastseenonlineat;
    }

    public void setLastseenonlineat(Long lastseenonlineat) {
        this.lastseenonlineat = lastseenonlineat;
    }

    public Integer getMiddlemanguid() {
        return middlemanguid;
    }

    public void setMiddlemanguid(Integer middlemanguid) {
        this.middlemanguid = middlemanguid;
    }

    public String getRenamednickname() {
        return renamednickname;
    }

    public void setRenamednickname(String renamednickname) {
        this.renamednickname = renamednickname;
    }

    public void setUgroupname(String ugroupname) { //Bastvera (Renaming group name)
        this.ugroupname = ugroupname;
    }

    public String getUgroupname() {  //Bastvera (Loading group name from settings.xml)
        if (ugroupname == null) {
            ugroupname = "";
        }
        return ugroupname;
    }

    public void setTrusted(Integer trusted) {
        this.trusted = trusted;
    }

    public Integer getTrusted() {
        if (trusted == null) {
            trusted = 0;
        }
        return trusted;
    }
}
