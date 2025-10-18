package org.example.serverListener.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import org.example.serverListener.GeneratedResult;
import org.example.serverListener.GeneratorService;
import org.example.serverListener.SenderService;
import org.example.serverListener.ServerInterface;

public class Server implements ServerInterface {
    private volatile boolean running = false;
    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    private final GeneratorService generatorService;
    private final SenderService senderService;

    public Server(int port, Path issuerKeyPath, String issuerName, int generatorThreads)
            throws Exception {
        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.configureBlocking(false);
        this.serverChannel.bind(new InetSocketAddress(port));
        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.generatorService =
                new GenerationServiceImpl(issuerKeyPath, issuerName, generatorThreads);
        this.senderService = new SendServiceImpl();
    }

    @Override
    public void startServer(int port) {
        running = true;
        Thread nioThread = new Thread(this::nioLoop, "nio-accept-loop");
        nioThread.setDaemon(true);
        nioThread.start();
    }

    @Override
    public void stopServer() {
        running = false;
        try {
            selector.wakeup();
            serverChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        generatorService.shutdown();
        senderService.shutdown();
    }

    @Override
    public boolean isServerRunning() {
        return running;
    }

    private void nioLoop() {
        while (running) {
            try {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (!key.isValid()) continue;
                    if (key.isAcceptable()) acceptConnection(key);
                    else if (key.isReadable()) readFromClient(key);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void acceptConnection(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel client = ssc.accept();
        if (client == null) return;
        client.configureBlocking(false);
        ByteBuffer buf = ByteBuffer.allocate(1024);
        client.register(selector, SelectionKey.OP_READ, buf);
    }

    private void readFromClient(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buf = (ByteBuffer) key.attachment();
        try {
            int r = client.read(buf);
            if (r == -1) {
                client.close();
                return;
            }
            int pos = buf.position();
            for (int i = 0; i < pos; i++) {
                if (buf.get(i) == 0) {
                    byte[] arr = new byte[i];
                    buf.position(0);
                    buf.get(arr);
                    String name = new String(arr, StandardCharsets.US_ASCII);
                    key.cancel();
                    CompletableFuture<GeneratedResult> future = generatorService.submit(name);
                    future.whenComplete(
                            (res, ex) -> {
                                if (ex != null) senderService.sendError(client, ex);
                                else senderService.send(client, res);
                            });
                    return;
                }
            }
            if (!buf.hasRemaining()) {
                ByteBuffer n = ByteBuffer.allocate(buf.capacity() * 2);
                buf.flip();
                n.put(buf);
                key.attach(n);
            }
        } catch (IOException e) {
            try {
                client.close();
            } catch (IOException ignore) {
            }
        }
    }
}
