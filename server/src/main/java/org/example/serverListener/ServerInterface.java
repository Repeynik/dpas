package org.example.serverListener;

public interface ServerInterface {
    void startServer(int port);

    void stopServer();

    boolean isServerRunning();
}
