package org.example.key_generation;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public abstract class PemUtils {
    public static String privateKeyToPem(PrivateKey key) {
        try (StringWriter sw = new StringWriter();
                JcaPEMWriter pw = new JcaPEMWriter(sw)) {
            pw.writeObject(key);
            pw.flush();
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String certificateToPem(X509Certificate cert) {
        try (StringWriter sw = new StringWriter();
                JcaPEMWriter pw = new JcaPEMWriter(sw)) {
            pw.writeObject(cert);
            pw.flush();
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
