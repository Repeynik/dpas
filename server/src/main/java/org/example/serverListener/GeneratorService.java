package org.example.serverListener;

import java.util.concurrent.CompletableFuture;

public interface GeneratorService {
    CompletableFuture<GeneratedResult> submit(String name);

    void shutdown();
}
