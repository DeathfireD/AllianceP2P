package org.alliance.core.settings;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class IPBlacklist extends ArrayList<Routerule> {

    private static final long serialVersionUID = 917458421801053394L;

    public IPBlacklist() {
    }

    public boolean checkConnection(String hostname) throws UnknownHostException {
        InetAddress out = InetAddress.getByName(hostname);
        return checkConnection(makeArrayInt(out.getAddress()));
    }

    public boolean checkConnection(byte[] addr) {
        return checkConnection(makeArrayInt(addr));
    }

    public boolean checkConnection(int ipaddr) {
        for (Routerule rule : this) {
            if (isValid(ipaddr, rule.getAddress(), rule.getMask())) {
                if (rule.getRuleType().equals(Routerule.ALLOW)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private int makeArrayInt(byte[] addr) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (addr[i] & 0x000000FF) << shift;
        }
        return value;
    }

    private boolean isValid(int address, int rule, int mask) {
        if ((address >> (32 - mask)) == (rule >> (32 - mask))) {
            return true;
        } else {
            return false;
        }
    }

    public boolean add(String human) throws Exception {
        return this.add(new Routerule(human));
    }

    public boolean checkConnection(SocketAddress socketAddress) {
        InetSocketAddress temp = (InetSocketAddress) socketAddress;
        return checkConnection(makeArrayInt(temp.getAddress().getAddress()));
    }
}
