package org.alliance.ui.windows.options;

import com.stendahls.XUI.XUI;
import com.stendahls.XUI.XUIDialog;
import com.stendahls.ui.JHtmlLabel;
import org.alliance.core.Language;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 *
 * @author Bastvera
 */
public class NetworkTab extends XUIDialog implements TabHelper {

    private JPanel tab;
    private JComboBox nicBox;
    private JCheckBox ipv6;
    private JCheckBox lanMode;
    private JCheckBox dnsMode;
    private JCheckBox bindToAll;
    private JCheckBox rdnsMode;
    private JLabel externalText;
    private JLabel dnsText;
    private JLabel inter;
    private JTextField dnsField;
    private JTextField localField;
    private JTextField externalField;
    private UISubsystem ui;
    private String lastSelectedNic;
    private final static String[] OPTIONS = new String[]{
        "server.port", "server.ipv6", "server.lanmode", "server.bindnic",
        "server.dnsmode", "server.dnsname", "server.staticip", "server.bindtoall",
        "internal.skipportcheck"};

    public NetworkTab(String loading) {
        tab = new JPanel();
        tab.add(new JLabel(loading));
        tab.setName(Language.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(Language.getLocalizedString(getClass(), "tooltip"));
    }

    public NetworkTab(final UISubsystem ui) throws Exception {
        init(ui.getRl(), ui.getRl().getResourceStream("xui/optionstabs/networktab.xui.xml"));
        this.ui = ui;

        Language.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        tab = (JPanel) xui.getComponent("networktab");
        tab.setName(Language.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(Language.getLocalizedString(getClass(), "tooltip"));

        if (ui.getCore().getUpnpManager().isPortForwardSuccedeed()) {
            ((JHtmlLabel) xui.getComponent("portforward")).setText(Language.getLocalizedString(getClass(), "portupnp"));
        } else {
            ((JHtmlLabel) xui.getComponent("portforward")).setText(Language.getLocalizedString(getClass(), "xui.portforward",
                    "[a href='.']http://www.portforward.com/[/a]"));
            ((JHtmlLabel) xui.getComponent("portforward")).addHyperlinkListener(new HyperlinkListener() {

                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        ui.openURL("http://www.portforward.com");
                    }
                }
            });
        }

        bindToAll = (JCheckBox) xui.getComponent("server.bindtoall");
        ipv6 = (JCheckBox) xui.getComponent("server.ipv6");
        lanMode = (JCheckBox) xui.getComponent("server.lanmode");
        dnsMode = (JCheckBox) xui.getComponent("server.dnsmode");
        rdnsMode = (JCheckBox) xui.getComponent("internal.rdnsname");
        nicBox = (JComboBox) xui.getComponent("server.bindnic");
        inter = (JLabel) xui.getComponent("interface");

        PopupMenuListener nicPopup = new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                fillIp(lanMode.isSelected());
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        };

        nicBox.addPopupMenuListener(nicPopup);

        MouseAdapter il = new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource().equals(dnsMode)) {
                    changeDnsState();
                    return;
                }
                if (e.getSource().equals(bindToAll)) {
                    changeBinsState();
                }
                fillIp(lanMode.isSelected());
            }
        };

        ipv6.addMouseListener(il);
        lanMode.addMouseListener(il);
        dnsMode.addMouseListener(il);
        bindToAll.addMouseListener(il);

        localField = (JTextField) xui.getComponent("localfield");
        localField.setEditable(false);
        externalField = (JTextField) xui.getComponent("externalfield");
        externalField.setEditable(false);
        externalText = (JLabel) xui.getComponent("externalip");
        dnsField = (JTextField) xui.getComponent("server.dnsname");
        dnsText = (JLabel) xui.getComponent("dnsname");
        ((JTextField) xui.getComponent("server.port")).setEditable(false);
    }

    private void changeDnsState() {
        if (dnsMode.isSelected()) {
            dnsField.setEnabled(true);
            dnsText.setEnabled(true);
        } else {
            dnsField.setEnabled(false);
            dnsText.setEnabled(false);
        }
    }

    private void changeBinsState() {
        if (bindToAll.isSelected()) {
            nicBox.setEnabled(false);
            inter.setEnabled(false);
        } else {
            nicBox.setEnabled(true);
            inter.setEnabled(true);
        }
        fillInterfaces();
    }

    private void notSupported() {
        nicBox.removeAllItems();
        try {
            NetworkInterface net = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            if (net != null) {
                nicBox.addItem("(" + net.getName() + ") " + net.getDisplayName());
            } else {
                nicBox.addItem(Language.getLocalizedString(getClass(), "support"));
            }
        } catch (Exception e) {
            nicBox.addItem(Language.getLocalizedString(getClass(), "support"));
        }
        nicBox.setEnabled(false);
        inter.setEnabled(false);
        bindToAll.setEnabled(false);
        bindToAll.setSelected(true);
        externalField.setText(ui.getCore().getNetworkManager().getIpDetection().getLastExternalIp());
        localField.setText(ui.getCore().getNetworkManager().getIpDetection().getLastLocalIp());
    }

    private void fillInterfaces() {
        try {
            Object selected = nicBox.getSelectedItem();
            nicBox.removeAllItems();
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netIf : Collections.list(nets)) {
                if (netIf.isUp() && !netIf.isLoopback() && !netIf.isPointToPoint()) {
                    Enumeration<InetAddress> inetAddresses = netIf.getInetAddresses();
                    if (inetAddresses.hasMoreElements()) {
                        nicBox.addItem("(" + netIf.getName() + ") " + netIf.getDisplayName());
                    }
                }
            }
            nicBox.setSelectedItem(selected);
        } catch (Exception ex) {
            notSupported();
        }
    }

    private void fillIp(final boolean isLanMode) {
        try {
            externalField.setText(null);
            localField.setText(null);
            externalField.setEnabled(!isLanMode);
            externalText.setEnabled(!isLanMode);
            //ipv6.setEnabled(!isLanMode);

            String nicName = nicBox.getSelectedItem().toString();
            nicName = nicName.substring(1, nicName.indexOf(")"));
            try {
                if (ui.getCore().getNetworkManager().getIpDetection().updateLocalIp(nicName, ipv6.isSelected() ? 1 : 0)) {
                    localField.setText(ui.getCore().getNetworkManager().getIpDetection().getLastLocalIp());
                }
            } catch (Exception ex) {
                //Skip
            }
            if (localField.getText().isEmpty()) {
                localField.setText(Language.getLocalizedString(getClass(), "noobtain"));
                externalField.setText(Language.getLocalizedString(getClass(), "noobtain"));
                return;
            }
            if (isLanMode) {
                //ipv6.setSelected(!isLanMode);
                externalField.setText(Language.getLocalizedString(getClass(), "unused"));
                return;
            } else {
                try {
                    if (lastSelectedNic == null) {
                        lastSelectedNic = ui.getCore().getSettings().getServer().getBindnic();
                    }
                    if (!nicName.equals(lastSelectedNic)) {
                        if (ui.getCore().getNetworkManager().getIpDetection().updateExternalIp(0)) {
                            externalField.setText(ui.getCore().getNetworkManager().getIpDetection().getLastExternalIp());
                        }
                    } else {
                        externalField.setText(ui.getCore().getNetworkManager().getIpDetection().getLastExternalIp());
                    }
                } catch (Exception ex) {
                    //Skip
                }
            }
            if (externalField.getText().isEmpty()) {
                externalField.setText(Language.getLocalizedString(getClass(), "noobtain"));
            }
            lastSelectedNic = nicName;
        } catch (Exception ex) {
            notSupported();
        }
    }

    /*    ((JCheckBox) components.get("internal.rdnsname")).addActionListener(new ActionListener() {

    @Override
    public void actionPerformed(ActionEvent e) {
    if (getComponentValue(components.get("internal.rdnsname")).toString().equalsIgnoreCase("1")) {
    Boolean confirm = OptionDialog.showQuestionDialog(ui.getMainWindow(), "This option will try to convert all friend's IPs to DNS. Conversion will hang alliance for couple of seconds so use it only when you don't upload/download anything.\nIf DNS names can't be obtained alliance will use IP and try to convert later.");
    if (confirm) {
    try {
    setComponentValue(components.get("server.dnsname"), InetAddress.getByName(ui.getCore().getFriendManager().getMe().getExternalIp(ui.getCore())).getHostName());
    } catch (UnknownHostException ex) {
    setComponentValue(components.get("server.dnsname"), "");
    } catch (IOException ex) {
    setComponentValue(components.get("server.dnsname"), "");
    }
    ui.getCore().getSettings().getInternal().setRdnsname(1);
    Collection<Friend> friends = ui.getCore().getFriendManager().friends();
    for (Friend friend : friends.toArray(new Friend[friends.size()])) {
    friend.setLastKnownHost(friend.rDNSConvert(friend.getLastKnownHost(), ui.getCore().getSettings().getFriend(friend.getGuid())));
    }
    } else {
    setComponentValue(components.get("internal.rdnsname"), "0");
    }
    } else {
    Boolean confirm = OptionDialog.showQuestionDialog(ui.getMainWindow(), "Unconverting changes ALL DNS names to IP. Conversion will hang alliance for couple of seconds so use it only when you don't upload/download anything.");
    if (confirm) {
    setComponentValue(components.get("server.dnsname"), "");
    ui.getCore().getSettings().getInternal().setRdnsname(0);
    Collection<Friend> friends = ui.getCore().getFriendManager().friends();
    for (Friend friend : friends.toArray(new Friend[friends.size()])) {
    friend.setLastKnownHost(friend.rDNSConvert(friend.getLastKnownHost(), ui.getCore().getSettings().getFriend(friend.getGuid())));
    }
    } else {
    setComponentValue(components.get("internal.rdnsname"), "1");
    }
    }
    }
    });
     */
    @Override
    public JPanel getTab() {
        return tab;
    }

    @Override
    public String[] getOptions() {
        return OPTIONS;
    }

    @Override
    public XUI getXUI() {
        return super.getXUI();
    }

    @Override
    public boolean isAllowedEmpty(String option) {
        if (option.equals("server.dnsname")) {
            return true;
        }
        return false;
    }

    @Override
    public String getOverridedSettingValue(String option, String value) {
        if (option.equals("server.bindnic")) {
            try {
                NetworkInterface net = NetworkInterface.getByName(value);
                if (net != null) {
                    nicBox.addItem("(" + net.getName() + ") " + net.getDisplayName());
                    nicBox.setSelectedIndex(0);
                    return nicBox.getItemAt(0).toString();
                }
            } catch (SocketException ex) {
            }
        }
        if (option.equals("server.dnsname")) {
            if (!dnsMode.isSelected()) {
                value = "";
            }
        }
        return value;
    }

    @Override
    public Object getOverridedComponentValue(String option, Object value) {
        if (value == null || value.toString().isEmpty()) {
            if (!isAllowedEmpty(option)) {
                return null;
            }
        }
        if (option.equals("server.bindnic")) {
            try {
                String nic = nicBox.getSelectedItem().toString();
                return nic.substring(1, nic.indexOf(")"));
            } catch (Exception ex) {
                value = "";
            }
        }
        return value;
    }

    @Override
    public void postOperation() {
        ipv6.setEnabled(false); // Disabled
        rdnsMode.setEnabled(false); // Disabled    
        changeDnsState();
        changeBinsState();
        fillIp(lanMode.isSelected());
    }
}
