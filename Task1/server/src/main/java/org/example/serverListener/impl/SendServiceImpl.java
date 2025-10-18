package org.example.serverListener.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.example.serverListener.GeneratedResult;
import org.example.serverListener.SenderService;

public class SendServiceImpl implements SenderService {
    private final BlockingQueue<SendItem> queue = new LinkedBlockingQueue<>();
    private final Thread senderThread;

    public SendServiceImpl() {
        senderThread = new Thread(this::loop, "send-service");
        senderThread.setDaemon(true);
        senderThread.start();
    }

    @Override
    public void send(SocketChannel client, GeneratedResult result) {
        try {
            queue.put(new SendItem(client, result, null));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void sendError(SocketChannel client, Throwable error) {
        try {
            queue.put(new SendItem(client, null, error));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                SendItem item = queue.take();
                SocketChannel client = item.client;
                if (!client.isOpen()) continue;
                try {
                    client.configureBlocking(true);
                } catch (IOException ignore) {
                }
                if (item.error != null) {
                    ByteBuffer b =
                            ByteBuffer.wrap(
                                    ("ERROR:" + item.error.getMessage() + "\n")
                                            .getBytes(StandardCharsets.UTF_8));
                    client.write(b);
                    client.close();
                    continue;
                }
                byte[] priv = item.result.privateKeyPem.getBytes(StandardCharsets.UTF_8);
                byte[] cert = item.result.certificatePem.getBytes(StandardCharsets.UTF_8);
                ByteBuffer out = ByteBuffer.allocate(4 + priv.length + 4 + cert.length);
                out.putInt(priv.length);
                out.put(priv);
                out.putInt(cert.length);
                out.put(cert);
                out.flip();
                while (out.hasRemaining()) client.write(out);
                client.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void shutdown() {
        senderThread.interrupt();
    }

    private static class SendItem {
        final SocketChannel client;
        final GeneratedResult result;
        final Throwable error;

        SendItem(SocketChannel client, GeneratedResult result, Throwable error) {
            this.client = client;
            this.result = result;
            this.error = error;
        }
    }
}
