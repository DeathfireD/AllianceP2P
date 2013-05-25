package org.alliance.updater.core;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class JarVerifier {

    private X509Certificate[] trustedCaCerts;

    public JarVerifier(X509Certificate[] trustedCaCerts) {
        this.trustedCaCerts = trustedCaCerts;
    }

    @SuppressWarnings("empty-statement")
    public void verify(JarFile jf) throws IOException, CertificateException {
        Vector entriesVec = new Vector();

        Manifest man = jf.getManifest();
        if (man == null) {
            throw new SecurityException("JAR is not signed");
        }

        byte[] buffer = new byte[8192];
        Enumeration entries = jf.entries();

        while (entries.hasMoreElements()) {
            JarEntry je = (JarEntry) entries.nextElement();
            entriesVec.addElement(je);
            InputStream is = jf.getInputStream(je);
            int n;
            while ((n = is.read(buffer, 0, buffer.length)) != -1);
            is.close();
        }
        jf.close();

        Enumeration e = entriesVec.elements();
        while (e.hasMoreElements()) {
            JarEntry je = (JarEntry) e.nextElement();

            if (je.isDirectory()) {
                continue;
            }

            Certificate[] certs = je.getCertificates();
            if ((certs == null) || (certs.length == 0)) {
                if (!(je.getName().startsWith("META-INF"))) {
                    throw new SecurityException("Unsigned file found in jar " + je.getName() + ".");
                }

            } else {
                Certificate[] chainRoots = getChainRoots(certs);
                boolean signedAsExpected = false;

                for (int i = 0; i < chainRoots.length; ++i) {
                    if (isTrusted((X509Certificate) chainRoots[i])) {
                        signedAsExpected = true;
                        break;
                    }
                }

                if (!(signedAsExpected)) {
                    throw new SecurityException("Jar is not signed by a trusted signer.");
                }
            }
        }
    }

    private boolean isTrusted(X509Certificate cert) {
        for (int i = 0; i < trustedCaCerts.length; ++i) {
            if ((cert.getSubjectDN().equals(trustedCaCerts[i].getSubjectDN()))
                    && (cert.equals(trustedCaCerts[i]))) {
                return true;
            }

        }

        for (int i = 0; i < trustedCaCerts.length; ++i) {
            if (!(cert.getIssuerDN().equals(trustedCaCerts[i].getSubjectDN()))) {
                continue;
            }
            try {
                cert.verify(trustedCaCerts[i].getPublicKey());
                return true;
            } catch (Exception e) {
            }
        }

        return false;
    }

    private Certificate[] getChainRoots(Certificate[] certs) {
        Vector result = new Vector(3);

        for (int i = 0; i < certs.length - 1; ++i) {
            if (((X509Certificate) certs[(i + 1)]).getSubjectDN().equals(((X509Certificate) certs[i]).getIssuerDN())) {
                continue;
            }
            result.addElement(certs[i]);
        }

        result.addElement(certs[(certs.length - 1)]);
        Certificate[] ret = new Certificate[result.size()];
        result.copyInto(ret);

        return ret;
    }
}
