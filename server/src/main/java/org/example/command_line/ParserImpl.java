package org.example.command_line;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ParserImpl implements ParserInterface {
    public class Config {
        private int port = -1;
        private Path issuer;
        private String issuerName;
        private int threads = -1;
        private boolean help = false;

        public int getPort() {
            return port;
        }

        public Path getIssuer() {
            return issuer;
        }

        public String getIssuerName() {
            return issuerName;
        }

        public int getThreads() {
            return threads;
        }

        public boolean isHelp() {
            return help;
        }
    }

    public Config parseArgs(String[] args) {
        Config cfg = new Config();
        List<String> pos = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--help", "-h":
                    cfg.help = true;
                    break;
                case "--port":
                    if (i + 1 < args.length) {
                        cfg.port = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--issuer":
                    if (i + 1 < args.length) cfg.issuer = Path.of(args[++i]);
                    break;
                case "--issuer-name":
                    if (i + 1 < args.length) cfg.issuerName = args[++i];
                    break;
                case "--threads":
                    if (i + 1 < args.length) cfg.threads = Integer.parseInt(args[++i]);
                    break;
                default:
                    if (a.startsWith("--")) {
                    } else {
                        pos.add(a);
                    }
                    break;
            }
        }
        if (cfg.issuer == null && pos.size() >= 2) cfg.issuer = Path.of(pos.get(1));
        if (cfg.issuerName == null && pos.size() >= 3) cfg.issuerName = pos.get(2);
        if (cfg.port <= 0 && !pos.isEmpty()) cfg.port = Integer.parseInt(pos.get(0));
        return cfg;
    }

    public void printUsage() {
        System.err.println(
                "Usage: server [--port <port>] [--issuer <path>] [--issuer-name <name>] [--threads"
                        + " <n>]");
        System.err.println("positional: server <port> <issuerKeyPemPath> <issuerName>");
    }
}
