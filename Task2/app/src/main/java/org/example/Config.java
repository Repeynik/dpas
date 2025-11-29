package org.example;

import java.util.Locale;

class Config {
    final Mode mode;
    final int threads;
    final long delayInsideMs;
    final long delayBetweenMs;
    final int benchmarkSeconds;
    final int seedCount;

    Config(Mode m, int th, long di, long db, int bs, int sc) {
        mode = m; threads = th; delayInsideMs = di; delayBetweenMs = db; benchmarkSeconds = bs; seedCount = sc;
    }

    static Config fromArgs(String[] args) {
        Mode m = Mode.LINKED;
        int th = 2;
        long di = 1000;
        long db = 1000;
        int bs = 0;
        int sc = 50;
        for (String a : args) {
            String x = a.trim();
            if (x.startsWith("--mode=")) m = x.substring(7).toLowerCase(Locale.ROOT).startsWith("arr") ? Mode.ARRAY : Mode.LINKED;
            else if (x.startsWith("--threads=")) th = Integer.parseInt(x.substring(10));
            else if (x.startsWith("--inside=")) di = Long.parseLong(x.substring(9));
            else if (x.startsWith("--between=")) db = Long.parseLong(x.substring(10));
            else if (x.startsWith("--benchmark=")) bs = Integer.parseInt(x.substring(12));
            else if (x.startsWith("--seed=")) sc = Integer.parseInt(x.substring(7));
        }
        return new Config(m, th, di, db, bs, sc);
    }
}
