package org.example.serverListener;

import java.nio.channels.SocketChannel;

public interface SenderService {
    void send(SocketChannel client, GeneratedResult result);

    void sendError(SocketChannel client, Throwable error);

    void shutdown();
}
