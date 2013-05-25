package org.alliance.core.crypto;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.comm.networklayers.tcpnio.TCPNIONetworkLayer;
import org.alliance.core.crypto.cryptolayers.SSLCryptoLayer;
import org.alliance.core.crypto.cryptolayers.TranslationCryptoLayer;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-22
 * Time: 19:30:23
 * To change this template use File | Settings | File Templates.
 */
public class CryptoManager {

    private CryptoLayer cryptoLayer;
    private CoreSubsystem core;

    public CryptoManager(CoreSubsystem core) throws Exception {
        switch (core.getSettings().getInternal().getEncryption()) {
            case 1:
                if (T.t) {
                    T.info("Launching SSL cryptolayer");
                }
                this.cryptoLayer = new SSLCryptoLayer(core);
                break;
            default:
                if (T.t) {
                    T.info("Launching translation cryptolayer");
                }
                this.cryptoLayer = new TranslationCryptoLayer(core);
                break;
        }

        this.core = core;
    }

    public CryptoLayer getCryptoLayer() {
        return cryptoLayer;
    }

    public void init() throws IOException, Exception {
        TCPNIONetworkLayer networkLayer = core.getNetworkManager().getNetworkLayer();
        cryptoLayer.setNetworkLayer(networkLayer);
        cryptoLayer.init();
    }
}
