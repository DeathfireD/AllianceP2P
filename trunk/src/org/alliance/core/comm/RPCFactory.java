package org.alliance.core.comm;

import org.alliance.core.comm.rpc.AwayStatus;
import org.alliance.core.comm.rpc.BlockMaskResult;
import org.alliance.core.comm.rpc.Broadcast;
import org.alliance.core.comm.rpc.ChatMessage;
import org.alliance.core.comm.rpc.ConnectToMe;
import org.alliance.core.comm.rpc.ConnectionInfo;
import org.alliance.core.comm.rpc.DirectoryListing;
import org.alliance.core.comm.rpc.ForwardedInvitation;
import org.alliance.core.comm.rpc.GetBlockMask;
import org.alliance.core.comm.rpc.GetDirectoryListing;
import org.alliance.core.comm.rpc.GetHashesForPath;
import org.alliance.core.comm.rpc.GetIsFriend;
import org.alliance.core.comm.rpc.GetMyExternalIp;
import org.alliance.core.comm.rpc.GetShareBaseList;
import org.alliance.core.comm.rpc.GetUserInfo;
import org.alliance.core.comm.rpc.GetUserList;
import org.alliance.core.comm.rpc.GracefulClose;
import org.alliance.core.comm.rpc.HashesForPath;
import org.alliance.core.comm.rpc.IsFriend;
import org.alliance.core.comm.rpc.MyExternalIp;
import org.alliance.core.comm.rpc.NoRouteToHost;
import org.alliance.core.comm.rpc.Ping;
import org.alliance.core.comm.rpc.PleaseForwardInvitation;
import org.alliance.core.comm.rpc.PlugInCommunication;
import org.alliance.core.comm.rpc.Pong;
import org.alliance.core.comm.rpc.Route;
import org.alliance.core.comm.rpc.Search;
import org.alliance.core.comm.rpc.SearchHits;
import org.alliance.core.comm.rpc.ShareBaseList;
import org.alliance.core.comm.rpc.UserInfo;
import org.alliance.core.comm.rpc.UserList;

public class RPCFactory { //Old RPC can be reused after increasing PROTOCOL_VERSION

    private RPCFactory() {
    }

    public static RPC newInstance(int packetId) {
        RPC rpc = null;
        switch (packetId) {
            case 1:
                rpc = new BlockMaskResult();
                break;
            case 2:
                rpc = new Broadcast();
                break;
            case 3:
                rpc = new ConnectionInfo();
                break;
            case 4:
                rpc = new GetBlockMask();
                break;
            case 5:
                rpc = new GetIsFriend();
                break;
            case 6:
                rpc = new MyExternalIp();
                break;
            case 7:
                rpc = new GetUserList();
                break;
            case 8:
                rpc = new GracefulClose();
                break;
            case 9:
                rpc = new IsFriend();
                break;
            case 10:
                //Old RPC NewVersionAvailable; v1.0.6
                break;
            case 11:
                rpc = new NoRouteToHost();
                break;
            case 12:
                rpc = new Ping();
                break;
            case 13:
                rpc = new Pong();
                break;
            case 15:
                rpc = new Route();
                break;
            case 16:
                rpc = new Search();
                break;
            case 17:
                //Old RPC SearchHits; v1.0.6
                break;
            case 18:
                //Old RPC UserInfo(); v1.0.6
                break;
            case 19:
                rpc = new UserList();
                break;
            case 20:
                rpc = new GetMyExternalIp();
                break;
            case 21:
                rpc = new PleaseForwardInvitation();
                break;
            case 22:
                rpc = new ForwardedInvitation();
                break;
            case 23:
                rpc = new GetDirectoryListing();
                break;
            case 24:
                rpc = new DirectoryListing();
                break;
            case 25:
                rpc = new GetShareBaseList();
                break;
            case 26:
                rpc = new ShareBaseList();
                break;
            case 27:
                rpc = new GetHashesForPath();
                break;
            case 28:
                rpc = new HashesForPath();
                break;
            case 29:
                rpc = new ConnectToMe();
                break;
            case 30:
                //Old RPC ChatMessage(); v1.0.6
                break;
            case 31:
                rpc = new GetUserInfo();
                break;
            case 32:
                rpc = new UserInfo();
                break;
            case 33:
                rpc = new SearchHits();
                break;
            case 34:
                rpc = new AwayStatus();
                break;
            case 35:
                //Old RPC ChatMessageV2(); v1.0.6
                break;
            case 36:
                rpc = new ChatMessage();
                break;
            case 37:
                rpc = new PlugInCommunication();
                break;
        }
        if (rpc == null) {
            if (T.t) {
                T.error("UNRECOGNIZED or OLD rpc id: " + packetId);
            }
        }
        return rpc;
    }

    public static byte getPacketIdFor(RPC rpc) {
        if (rpc instanceof BlockMaskResult) {
            return 1;
        }
        if (rpc instanceof Broadcast) {
            return 2;
        }
        if (rpc instanceof ConnectionInfo) {
            return 3;
        }
        if (rpc instanceof GetBlockMask) {
            return 4;
        }
        if (rpc instanceof GetIsFriend) {
            return 5;
        }
        if (rpc instanceof MyExternalIp) {
            return 6;
        }
        if (rpc instanceof GetUserList) {
            return 7;
        }
        if (rpc instanceof GracefulClose) {
            return 8;
        }
        if (rpc instanceof IsFriend) {
            return 9;
        }
        //Old RPC NewVersionAvailable return 10; v1.0.6
        if (rpc instanceof NoRouteToHost) {
            return 11;
        }
        if (rpc instanceof Ping) {
            return 12;
        }
        if (rpc instanceof Pong) {
            return 13;
        }
        if (rpc instanceof Route) {
            return 15;
        }
        if (rpc instanceof Search) {
            return 16;
        }
        //Old RPC SearchHits return 17;v 1.0.6
        //Old RPC UserInfo return 18; v1.0.6
        if (rpc instanceof UserList) {
            return 19;
        }
        if (rpc instanceof GetMyExternalIp) {
            return 20;
        }
        if (rpc instanceof PleaseForwardInvitation) {
            return 21;
        }
        if (rpc instanceof ForwardedInvitation) {
            return 22;
        }
        if (rpc instanceof GetDirectoryListing) {
            return 23;
        }
        if (rpc instanceof DirectoryListing) {
            return 24;
        }
        if (rpc instanceof GetShareBaseList) {
            return 25;
        }
        if (rpc instanceof ShareBaseList) {
            return 26;
        }
        if (rpc instanceof GetHashesForPath) {
            return 27;
        }
        if (rpc instanceof HashesForPath) {
            return 28;
        }
        if (rpc instanceof ConnectToMe) {
            return 29;
        }
        //Old RPC ChatMessage return 30; v1.0.6
        if (rpc instanceof GetUserInfo) {
            return 31;
        }
        if (rpc instanceof UserInfo) {
            return 32;
        }
        if (rpc instanceof SearchHits) {
            return 33;
        }
        if (rpc instanceof AwayStatus) {
            return 34;
        }
        //Old RPC ChatMessageV2 return 35; v1.0.6  
        if (rpc instanceof ChatMessage) {
            return 36;
        }
        if (rpc instanceof PlugInCommunication) {
            return 37;
        }
        if (T.t) {
            T.error("Could not identify or old RPC: " + rpc);
        }
        return -1;
    }
}
