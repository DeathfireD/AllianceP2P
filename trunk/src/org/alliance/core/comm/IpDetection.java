package org.alliance.core.comm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Collections;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.alliance.core.settings.Server;

/**
 *
 * @author Bastvera
 */
public class IpDetection implements Runnable {

    private String lastExternalIp;
    private String lastLocalIp;
    private byte[] lastByteLocalIp;
    private boolean alive = true;
    private NetworkManager netMan;
    private static final String SITES[][] = {{"www.alliancep2p.com", "http://www.alliancep2p.com/myip/"},
        {"www.whatismyip.org", "/"},
        {"www.whatismyip.com", "http://www.whatismyip.com/automation/n09230945.asp"},
        {"checkip.dyndns.com", "http://checkip.dyndns.com"}};

    public IpDetection(NetworkManager netMan) throws IOException {
        this.netMan = netMan;
        if (!updateLocalIp(netMan.getCore().getSettings().getServer().getBindnic(), netMan.getCore().getSettings().getServer().getIpv6())) {
            InetAddress in = InetAddress.getLocalHost();
            if (in.isLoopbackAddress()) {
                //Local host is loopback, search first online nic
                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                for (NetworkInterface netIf : Collections.list(nets)) {
                    if (netIf.isUp() && !netIf.isLoopback() && !netIf.isPointToPoint()) {
                        if (updateLocalIp(netIf.getName(), netMan.getCore().getSettings().getServer().getIpv6())) {
                            break;
                        }
                    }
                }
            } else {
                lastLocalIp = in.getHostAddress();
                lastByteLocalIp = in.getAddress();
            }
        }
    }

    public boolean updateExternalIp(int siteNumber) throws IOException {
        InetAddress destAddr = InetAddress.getByName(SITES[siteNumber][0]);
        InetAddress srcAddr = InetAddress.getByAddress(lastByteLocalIp);
        int destPort = 80;
        int srcPort = Server.createRandomPort();
        Socket socket = new Socket(destAddr, destPort, srcAddr, srcPort);
        socket.setSoTimeout(5 * 1000);

        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
        wr.write("GET " + SITES[siteNumber][1] + " HTTP/1.0\r\n");
        wr.write("Host: " + SITES[siteNumber][0] + "\r\n");
        wr.write("User-Agent: Alliance\r\n\r\n");
        wr.flush();

        StringBuilder result = new StringBuilder();
        BufferedReader rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.delete(0, result.length());
            result.append(line);
        }
        wr.close();
        rd.close();

        Pattern pattern = Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");
        Matcher matcher = pattern.matcher(result.toString());
        if (matcher.find()) {
            lastExternalIp = matcher.group().trim();
            return true;
        }
        return false;
    }

    public boolean updateLocalIp(String nicName, int ipv6) throws IOException {
        NetworkInterface nic = NetworkInterface.getByName(nicName);
        if (nic == null) {
            return false;
        }
        Enumeration<InetAddress> inetAddresses = nic.getInetAddresses();
        if (!inetAddresses.hasMoreElements()) {
            return false;
        }
        for (InetAddress address : Collections.list(inetAddresses)) {
            if (ipv6 == 0 && address instanceof Inet6Address) {
                continue;
            } else if (ipv6 == 1 && address instanceof Inet6Address) {
                lastLocalIp = address.getHostAddress();
                lastByteLocalIp = address.getAddress();
                return true;
            } else {
                lastLocalIp = address.getHostAddress();
                lastByteLocalIp = address.getAddress();
                return true;
            }
        }
        return false;
    }

    @Override
    public void run() {
        int siteNumber = 0;
        boolean retry = false;
        while (alive) {
            if (netMan.getCore().getSettings().getServer().getLanmode() > 0) {
                try {
                    Thread.sleep(10 * 1000);
                } catch (InterruptedException ex) {
                }
            } else {
                try {
                    if (siteNumber > SITES.length - 1) {
                        siteNumber = 0;
                    }
                    if (!updateExternalIp(siteNumber)) {
                        retry = true;
                    }
                    siteNumber++;
                } catch (IOException ex) {
                    siteNumber++;
                    retry = true;
                }
                try {
                    if (retry) {
                        retry = false;
                        Thread.sleep(10 * 1000);
                    } else {
                        if (netMan.getCore().getSettings().getServer().getStaticip() > 0) {
                            Thread.sleep(12 * 60 * 60 * 1000);
                        } else {
                            Thread.sleep(3 * 60 * 1000);
                        }
                    }
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    public String getLastExternalIp() {
        return lastExternalIp;
    }

    public String getLastLocalIp() {
        return lastLocalIp;
    }

    public void setLastExternalIp(String lastExternalIp) {
        this.lastExternalIp = lastExternalIp;
    }

    public static boolean isLan(String ip, boolean ipv6) {
        if (ipv6) {
            return false;
        }
        //192.168.0.0 - 192.168.255.255
        if (ip.matches("\\b198\\.168\\.\\d{1,3}\\.\\d{1,3}\\b")) {
            return true;
        }
        //10.0.0.0 - 10.255.255.255
        if (ip.matches("\\b10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b")) {
            return true;
        }
        //169.254.0.0 -169.254.255.255
        if (ip.matches("\\b169\\.254\\.\\d{1,3}\\.\\d{1,3}\\b")) {
            return true;
        }
        //172.16.0.0 - 172.31.255.255
        if (ip.matches("\\b172\\.(1[6-9]|2[0-9]|3[0-1])\\.\\d{1,3}\\.\\d{1,3}\\b")) {
            return true;
        }
        return false;
    }
}
