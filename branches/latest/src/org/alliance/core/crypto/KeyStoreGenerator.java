package org.alliance.core.crypto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 *
 *
 * not used
 *
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-22
 * Time: 19:25:21
 * To change this template use File | Settings | File Templates.
 */
public class KeyStoreGenerator {

    public static void generate(String alias, char[] password, String outputFilename) throws Exception {
        if (T.t) {
            T.info("Generating a new keystore to " + outputFilename);
        }

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, password);
        sun.security.x509.CertAndKeyGen keypair = new sun.security.x509.CertAndKeyGen("RSA", "MD5WithRSA");

        sun.security.x509.X500Name x500Name = new sun.security.x509.X500Name("Alliance", "Alliance", "Alliance", "SE");

        if (T.t) {
            T.debug("Generating 1024 bit keypair...");
        }
        keypair.generate(1024);
        if (T.t) {
            T.debug("done.");
        }
        PrivateKey privKey = keypair.getPrivateKey();
        X509Certificate[] chain = new X509Certificate[1];
        chain[0] = keypair.getSelfCertificate(x500Name, 100l * 1000 * 24 * 60 * 60);

        keyStore.setKeyEntry(alias, privKey, password, chain);
        File file = new File(outputFilename);
        file.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(file);
        keyStore.store(out, password);

        if (T.t) {
            T.debug("Ketstore saved.");
        }
    }
}
