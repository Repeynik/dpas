package org.example;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class App {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println(
                    "Usage: client <serverHost> <serverPort> <name> [--delay seconds]"
                            + " [--exit-before-read]");
            System.exit(2);
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String name = args[2];

        int delaySeconds = 0;
        boolean exitBeforeRead = false;
        for (int i = 3; i < args.length; i++) {
            if ("--delay".equals(args[i]) && i + 1 < args.length) {
                delaySeconds = Integer.parseInt(args[++i]);
            } else if ("--exit-before-read".equals(args[i])) {
                exitBeforeRead = true;
            }
        }

        try (Socket s = new Socket(host, port)) {
            OutputStream out = s.getOutputStream();
            java.io.DataInputStream in = new java.io.DataInputStream(s.getInputStream());
            out.write(name.getBytes(StandardCharsets.US_ASCII));
            out.write(0);
            out.flush();

            if (exitBeforeRead) {
                return;
            }

            if (delaySeconds > 0) Thread.sleep(delaySeconds * 1000L);

            int privLen = in.readInt();
            byte[] priv = in.readNBytes(privLen);
            int certLen = in.readInt();
            byte[] cert = in.readNBytes(certLen);

            try (FileOutputStream fk = new FileOutputStream(name + ".key")) {
                fk.write(priv);
            }
            try (FileOutputStream fc = new FileOutputStream(name + ".crt")) {
                fc.write(cert);
            }

            System.out.println("Saved " + name + ".key and " + name + ".crt");
        }
    }
}
