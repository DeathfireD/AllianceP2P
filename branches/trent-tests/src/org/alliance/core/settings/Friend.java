package org.alliance.core.settings;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 14:01:19
 */
public class Friend {

    private String nickname;
    private String host; //obtained from friend
    private String fixedhost = ""; //manual edited by user
    private String dns = ""; //obtained from friend or trough rDNS
    private Integer guid, port;
    private Long lastseenonlineat;
    private Integer middlemanguid; //0 if a first degree friend, guid of middleman if introduced by someone to me
    private String renamednickname; //used to display a different nickname then the "anonymous" one in ones UI
    private String ugroupname = "";
    private Integer trusted = 0; //0 untrusted

    public Friend() {
    }

    public Friend(String nickname, String lasthost, Integer guid, Integer lastport) {
        this.nickname = nickname;
        this.host = lasthost;
        this.guid = guid;
        this.port = lastport;
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

    public void setUgroupname(String ugroupname) {
        this.ugroupname = ugroupname;
    }

    public String getUgroupname() {
        return ugroupname;
    }

    public void setTrusted(Integer trusted) {
        this.trusted = trusted;
    }

    public Integer getTrusted() {
        return trusted;
    }

    public String getDns() {
        return dns;
    }

    public void setDns(String dns) {
        this.dns = dns;
    }

    public String getFixedhost() {
        return fixedhost;
    }

    public void setFixedhost(String fixedhost) {
        this.fixedhost = fixedhost;
    }
}
