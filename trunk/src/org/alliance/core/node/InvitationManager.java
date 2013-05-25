package org.alliance.core.node;

import com.stendahls.util.HumanReadableEncoder;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.T;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.InvitationConnection;
import org.alliance.core.settings.Settings;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-16
 * Time: 14:20:39
 */
public class InvitationManager {

    private CoreSubsystem core;
    private HashMap<Integer, Invitation> invitations = new HashMap<Integer, Invitation>();

    public InvitationManager(CoreSubsystem core, Settings settings) {
        this.core = core;
    }

    public Invitation createInvitation(boolean forLan, long validTime) throws Exception {
        return createInvitation(null, null, forLan, validTime);
    }

    public Invitation createInvitation(Integer destinationGuid, Integer middlemanGuid, boolean forLan, long validTime) throws Exception {
        Invitation i = new Invitation(core, destinationGuid, middlemanGuid, forLan, validTime);
        invitations.put(i.getInvitationPassKey(), i);
        return i;
    }

    public boolean containsKey(int key) {
        return invitations.containsKey(key);
    }

    public boolean isValid(int key) {
        Invitation invit = invitations.get(key);
        if (invit.isValidOnlyOnce()) {
            return true;
        }
        return System.currentTimeMillis() - invit.getCreatedAt() < invit.getValidTime();
    }

    public void attemptToBecomeFriendWith(String invitation, Friend middleman) throws IOException {
        attemptToBecomeFriendWith(invitation, middleman, null);
    }

    public void attemptToBecomeFriendWith(String invitation, Friend middleman, final Integer fromGuid) throws IOException {
        byte data[] = HumanReadableEncoder.fromBase64String(invitation);
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));

        byte iparray[] = new byte[4];
        for (int i = 0; i < iparray.length; i++) {
            iparray[i] = in.readByte();
        }

        InetAddress ip = InetAddress.getByAddress(iparray);
        int port = in.readUnsignedShort();
        int passkey = in.readInt();

        if (T.t) {
            T.info("Deserialized invitation: " + ip + ", " + port + ", " + passkey);
        }

        InvitationConnection ic = new InvitationConnection(core.getNetworkManager(), Connection.Direction.OUT, passkey, middleman);
        ic.setConnectionFailedEvent(new Runnable() {

            @Override
            public void run() {
                if (T.t) {
                    T.info("Attemted to connect using invitation but failed. From guid: " + fromGuid);
                }
                if (fromGuid != null) {
                    try {
                        if (T.t) {
                            T.info(" - trying to send an invitation the other way around (in order to get around a firewall)");
                        }
                        core.getFriendManager().forwardInvitationTo(fromGuid);
                    } catch (Exception e) {
                        core.reportError(e, this);
                    }
                }
            }
        });
        core.getNetworkManager().connect(ip.getHostAddress(), port, ic);
    }

    public Invitation getInvitation(int key) {
        return invitations.get(key);
    }

    public void consume(int key) {
        invitations.remove(key);
    }

    public void save(ObjectOutputStream out) throws IOException {
        out.writeObject(invitations);
    }

    public void load(ObjectInputStream in) throws IOException {
        try {
            invitations = (HashMap<Integer, Invitation>) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Could not instance class " + e);
        }
        //remove old, invalid invitations
        for (Iterator<Integer> it = invitations.keySet().iterator(); it.hasNext();) {
            Integer key = it.next();
            if (!isValid(key)) {
                it.remove();
            }
        }
    }

    public boolean hasBeenRecentlyInvited(int guid) {
        if (getMostRecentByGuid(guid) == null) {
            return false;
        }
        if (System.currentTimeMillis() - getMostRecentByGuid(guid).getCreatedAt() < core.getSettings().getInternal().getMinimumtimebetweeninvitations() * 1000 * 60) {
            return true;
        }
        return false;
    }

    public boolean hasBeenRecentlyInvited(Invitation i) {
        return System.currentTimeMillis() - i.getCreatedAt() < core.getSettings().getInternal().getMinimumtimebetweeninvitations() * 1000 * 60;
    }

    private Invitation getMostRecentByGuid(int guid) {
        Invitation mostRecent = null;
        Collection<Invitation> invitations = this.invitations.values();
        for (Invitation i : invitations.toArray(new Invitation[invitations.size()])) {
            if (i.getDestinationGuid() != null && i.getDestinationGuid() == guid) {
                if (mostRecent == null) {
                    mostRecent = i;
                } else {
                    if (mostRecent.getCreatedAt() < i.getCreatedAt()) {
                        mostRecent = i;
                    }
                }
            }
        }
        return mostRecent;
    }

    public Collection<Invitation> allInvitations() {
        return invitations.values();
    }
}
