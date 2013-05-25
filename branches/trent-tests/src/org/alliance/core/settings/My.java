package org.alliance.core.settings;

import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 15:02:20
 */
public class My extends SettingClass {

    private Integer guid = new Random().nextInt();
    private String nickname = "Rookie";
    private Integer cguid = 0; //this is a checksum of the invitations property, disguised so that script kiddies won't find it
    private Integer invitations = 0; //every time this user completes an invitation successfully he gets a point

    public My() {
    }

    public My(Integer guid, String nickname) {
        this.guid = guid;
        this.nickname = nickname;
    }

    public String getNickname() {
        nickname = filterNickname(nickname);
        return nickname;
    }

    public void setNickname(String nickname) {
        nickname = filterNickname(nickname);
        this.nickname = nickname;
    }

    private String filterNickname(String nickname) {
        if (nickname != null) {
            nickname = nickname.replaceAll("<", "");
            nickname = nickname.replaceAll(">", "");
        }
        return nickname;
    }

    public Integer getGuid() {
        return guid;
    }

    public void setGuid(Integer guid) {
        this.guid = guid;
    }

    public Integer getCguid() {
        return cguid;
    }

    public void setCguid(Integer cguid) {
        this.cguid = cguid;
    }

    public Integer getInvitations() {
        if (cguid != null && cguid != 0) {
            if ((invitations ^ 234427) * 13 != cguid) {
                invitations = 0;
            }
        } else {
            createChecksumAndSetInvitations(invitations);
        }
        return invitations;
    }

    public void setInvitations(Integer invitations) {
        this.invitations = invitations;
    }

    public void createChecksumAndSetInvitations(Integer invitations) {
        this.invitations = invitations;
        cguid = (invitations ^ 234427) * 13;
    }
}
