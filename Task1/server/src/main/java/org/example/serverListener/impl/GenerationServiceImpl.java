package org.example.serverListener.impl;

import org.example.key_generation.KeyGenerationImpl;
import org.example.key_generation.KeyGenerationInterface;
import org.example.key_generation.PemUtils;
import org.example.serverListener.GeneratedResult;
import org.example.serverListener.GeneratorService;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.*;

public class GenerationServiceImpl implements GeneratorService {
    private final KeyGenerationInterface keyGen = new KeyGenerationImpl();
    private final PrivateKey issuerKey;
    private final String issuerName;
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final ConcurrentMap<String, CompletableFuture<GeneratedResult>> results =
            new ConcurrentHashMap<>();
    private final ExecutorService pool;

    public GenerationServiceImpl(Path issuerKeyPath, String issuerName, int threads) {
        this.issuerKey = keyGen.loadIssuerPrivateKey(issuerKeyPath);
        this.issuerName = issuerName;
        this.pool = Executors.newFixedThreadPool(Math.max(1, threads));
        for (int i = 0; i < Math.max(1, threads); i++) {
            pool.submit(this::loop);
        }
    }

    @Override
    public CompletableFuture<GeneratedResult> submit(String name) {
        return results.computeIfAbsent(
                name,
                n -> {
                    CompletableFuture<GeneratedResult> f = new CompletableFuture<>();
                    try {
                        queue.put(n);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return f;
                });
    }

    private void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String name = queue.take();
                CompletableFuture<GeneratedResult> f = results.get(name);
                if (f == null || f.isDone()) continue;
                KeyPair kp = keyGen.generateKeyPair();
                X509Certificate cert = keyGen.generateCertificate(kp, issuerKey, issuerName, name);
                String priv = PemUtils.privateKeyToPem(kp.getPrivate());
                String certPem = PemUtils.certificateToPem(cert);
                f.complete(new GeneratedResult(priv, certPem));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void shutdown() {
        pool.shutdownNow();
    }
}
