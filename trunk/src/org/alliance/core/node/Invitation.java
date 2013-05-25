package org.alliance.core.node;

import com.stendahls.util.HumanReadableEncoder;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.T;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-16
 * Time: 14:20:47
 */
public class Invitation implements Serializable {

    private int invitationPassKey;
    private String completeInvitaitonString;
    private long createdAt;
    private long validTime;
    private Integer destinationGuid;
    private int middlemanGuid;

    public Invitation(CoreSubsystem core, Integer destinationGuid, Integer middlemanGuid, boolean forLan, long validTime) throws Exception {
        this.destinationGuid = destinationGuid;
        this.middlemanGuid = middlemanGuid == null ? 0 : middlemanGuid;
        this.validTime = validTime;

        String myhost;
        if (forLan) {
            myhost = core.getNetworkManager().getIpDetection().getLastLocalIp();
        } else {
            myhost = core.getNetworkManager().getIpDetection().getLastExternalIp();
        }

        if (T.t) {
            T.trace("Creating invitation for host: " + myhost);
        }
        byte[] ip = InetAddress.getByName(myhost).getAddress();
        if (T.t) {
            T.trace("Got: " + ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3]);
        }

        ByteArrayOutputStream o = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(o);

        //ip
        for (byte b : ip) {
            out.write(b);
        }

        //port
        out.writeShort(core.getSettings().getServer().getPort());

        //passkey
        invitationPassKey = new Random().nextInt();
        out.writeInt(invitationPassKey);
        if (T.t) {
            T.trace("passkey: " + invitationPassKey);
        }

        out.flush();
        completeInvitaitonString = HumanReadableEncoder.toBase64SHumanReadableString(o.toByteArray()).trim();

        createdAt = System.currentTimeMillis();
        if (T.t) {
            T.info("Created invitation. String: " + completeInvitaitonString);
        }
    }

    public int getInvitationPassKey() {
        return invitationPassKey;
    }

    public void setInvitationPassKey(int invitationPassKey) {
        this.invitationPassKey = invitationPassKey;
    }

    public String getCompleteInvitaitonString() {
        return completeInvitaitonString;
    }

    public void setCompleteInvitaitonString(String completeInvitaitonString) {
        this.completeInvitaitonString = completeInvitaitonString;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getValidTime() {
        return validTime;
    }

    public Integer getDestinationGuid() {
        return destinationGuid;
    }

    public int getMiddlemanGuid() {
        return middlemanGuid;
    }

    public boolean isForwardedInvitation() {
        return middlemanGuid != 0;
    }

    public boolean isValidOnlyOnce() {
        return validTime == -1;
    }
}
