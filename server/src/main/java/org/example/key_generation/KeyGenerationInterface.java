package org.example.key_generation;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public interface KeyGenerationInterface {
    X509Certificate generateCertificate(
            KeyPair subjectKeyPair,
            PrivateKey issuerPrivateKey,
            String issuerName,
            String subjectName);

    PrivateKey loadIssuerPrivateKey(Path path);

    KeyPair generateKeyPair();
}
