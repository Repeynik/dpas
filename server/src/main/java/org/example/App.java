package org.example;

import org.example.command_line.ParserImpl;
import org.example.serverListener.impl.Server;

public class App {

    public static void main(String[] args) throws Exception {
        ParserImpl parser = new ParserImpl();
        ParserImpl.Config cfg = parser.parseArgs(args);
        if (cfg.isHelp()
                || cfg.getPort() <= 0
                || cfg.getIssuer() == null
                || cfg.getIssuerName() == null) {
            parser.printUsage();
            System.exit(cfg.isHelp() ? 0 : 2);
        }

        int threadsToUse =
                cfg.getThreads() > 0
                        ? cfg.getThreads()
                        : Runtime.getRuntime().availableProcessors();
        Server server =
                new Server(cfg.getPort(), cfg.getIssuer(), cfg.getIssuerName(), threadsToUse);
        server.startServer(cfg.getPort());
        System.out.println("Server started on port " + cfg.getPort());
        Thread.currentThread().join();
    }
}
