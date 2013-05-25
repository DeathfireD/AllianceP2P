package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.comm.T;
import org.alliance.core.file.share.ShareBase;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class ShareBaseList extends RPC {

    public ShareBaseList() {
    }

    @Override
    public void execute(Packet data) throws IOException {
        int n = data.readInt();
        String shareBaseName[] = new String[n];
        for (int i = 0; i < n; i++) {
            shareBaseName[i] = data.readUTF();
            if (T.t) {
                T.trace("Found share base name: " + shareBaseName[i]);
            }
        }

        core.getUICallback().receivedShareBaseList(con.getRemoteFriend(), shareBaseName);
    }

    @Override
    public Packet serializeTo(Packet p) {
        Collection<ShareBase> c = manager.getCore().getShareManager().shareBases();

        //Bastvera
        //(Group names for specific user)
        String usergroupname = con.getRemoteGroupName();

        //Split Multi group names to single cell in array
        String[] dividedu = usergroupname.split(",");

        p.writeInt(c.size());
        for (ShareBase sb : c) {
            String sbgroupname = sb.getSBGroupName(); //Group names for specific folder
            boolean positive = false;
            if (sbgroupname.equalsIgnoreCase("public")) {
                positive = true;
            } else {
                //Split Multi sbgroupname names to single cell in array
                String[] dividedsb = sbgroupname.split(",");
                //Compare every usergroupname with every sbgroupname break if positive match

                for (String testsb : dividedsb) {
                    for (String testu : dividedu) {
                        if (testsb.equalsIgnoreCase(testu)) {
                            positive = true;
                            break;
                        }
                    }
                    if (positive == true) {
                        break;
                    }
                }
            }
            //Send matched sharebase listing and always send public folders
            if (positive == true) {
                p.writeUTF(sb.getName());
            } else {
                p.writeUTF("You are using old version of Alliance"); //unique name for hidden folders
            }
        }
        return p;
    }
}
