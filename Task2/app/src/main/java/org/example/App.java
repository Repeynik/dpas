package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class App {
    public static void main(String[] args) throws Exception {
        Config cfg = Config.fromArgs(args);
        if (cfg.benchmarkSeconds > 0) runBenchmark(cfg); else runInteractive(cfg);
    }

    static void runInteractive(Config cfg) throws Exception {
        if (cfg.mode == Mode.LINKED) {
            runInteractiveLinked(cfg);
        } else {
            runInteractiveArray(cfg);
        }
    }

    private static void runInteractiveLinked(Config cfg) throws Exception {
        AtomicLong steps = new AtomicLong();
        ConcurrentLinkedStringList list = new ConcurrentLinkedStringList();
        List<Thread> sorters = new ArrayList<>();
        AtomicBoolean running = new AtomicBoolean(true);
        for (int i = 0; i < cfg.threads; i++) {
            Thread t = new Thread(() -> {
                while (running.get()) {
                    list.bubblePass(cfg.delayInsideMs, cfg.delayBetweenMs, steps);
                }
            }, "LinkedSorter-" + i);
            t.start();
            sorters.add(t);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) break;
            if (line.isEmpty()) {
                for (String s : list) System.out.println(s);
                System.out.println("steps=" + steps.get());
            } else {
                List<String> parts = split80(line);
                for (int i = parts.size() - 1; i >= 0; i--) list.addFirst(parts.get(i));
            }
        }
        running.set(false);
        for (Thread t : sorters) t.join();
    }

    private static void runInteractiveArray(Config cfg) throws Exception {
        AtomicLong steps = new AtomicLong();
        List<String> list = Collections.synchronizedList(new ArrayList<>());
        List<Thread> sorters = new ArrayList<>();
        AtomicBoolean running = new AtomicBoolean(true);
        for (int i = 0; i < cfg.threads; i++) {
            Thread t = new Thread(() -> {
                while (running.get()) {
                    int i1 = 0;
                    while (true) {
                        synchronized (list) {
                            int n = list.size();
                            if (n < 2 || i1 >= n - 1) break;
                            sleep(cfg.delayInsideMs);
                            steps.incrementAndGet();
                            String a = list.get(i1);
                            String b = list.get(i1 + 1);
                            if (a.compareTo(b) > 0) Collections.swap(list, i1, i1 + 1);
                        }
                        sleep(cfg.delayBetweenMs);
                        i1++;
                    }
                }
            }, "ArraySorter-" + i);
            t.start();
            sorters.add(t);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) break;
            if (line.isEmpty()) {
                synchronized (list) {
                    for (String s : list) System.out.println(s);
                }
                System.out.println("steps=" + steps.get());
            } else {
                List<String> parts = split80(line);
                synchronized (list) {
                    for (int i = parts.size() - 1; i >= 0; i--) list.add(0, parts.get(i));
                }
            }
        }
        running.set(false);
        for (Thread t : sorters) t.join();
    }

    static void runBenchmark(Config cfg) throws Exception {
        AtomicLong steps = new AtomicLong();
        if (cfg.mode == Mode.LINKED) {
            ConcurrentLinkedStringList list = new ConcurrentLinkedStringList();
            seed(list, cfg.seedCount);
            List<Thread> sorters = new ArrayList<>();
            AtomicBoolean running = new AtomicBoolean(true);
            for (int i = 0; i < cfg.threads; i++) {
                Thread t = new Thread(() -> {
                    while (running.get()) list.bubblePass(cfg.delayInsideMs, cfg.delayBetweenMs, steps);
                });
                t.start();
                sorters.add(t);
            }
            TimeUnit.SECONDS.sleep(cfg.benchmarkSeconds);
            running.set(false);
            for (Thread t : sorters) t.join();
            long theo = theoreticalLinked(cfg);
            System.out.println("mode=linked steps_actual=" + steps.get() + " steps_theoretical=" + theo);
        } else {
            List<String> list = Collections.synchronizedList(new ArrayList<>());
            seed(list, cfg.seedCount);
            List<Thread> sorters = new ArrayList<>();
            AtomicBoolean running = new AtomicBoolean(true);
            for (int i = 0; i < cfg.threads; i++) {
                Thread t = new Thread(() -> {
                    while (running.get()) {
                        int i1 = 0;
                        while (true) {
                            synchronized (list) {
                                int n = list.size();
                                if (n < 2 || i1 >= n - 1) break;
                                sleep(cfg.delayInsideMs);
                                steps.incrementAndGet();
                                String a = list.get(i1);
                                String b = list.get(i1 + 1);
                                if (a.compareTo(b) > 0) Collections.swap(list, i1, i1 + 1);
                            }
                            sleep(cfg.delayBetweenMs);
                            i1++;
                        }
                    }
                });
                t.start();
                sorters.add(t);
            }
            TimeUnit.SECONDS.sleep(cfg.benchmarkSeconds);
            running.set(false);
            for (Thread t : sorters) t.join();
            long theo = theoreticalArray(cfg);
            System.out.println("mode=arraylist steps_actual=" + steps.get() + " steps_theoretical=" + theo);
        }
    }

    static void seed(ConcurrentLinkedStringList list, int n) {
        Random r = new Random(42);
        for (int i = 0; i < n; i++) list.addFirst(randString(r));
    }

    static void seed(List<String> list, int n) {
        Random r = new Random(42);
        for (int i = 0; i < n; i++) list.add(randString(r));
    }

    static String randString(Random r) {
        int len = 4 + r.nextInt(8);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append((char) ('a' + r.nextInt(26)));
        return sb.toString();
    }

    static List<String> split80(String s) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            int end = Math.min(i + 80, s.length());
            out.add(s.substring(i, end));
            i = end;
        }
        return out;
    }

    static void sleep(long ms) {
        if (ms <= 0) return;
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    static long theoreticalLinked(Config c) {
        long T = c.benchmarkSeconds * 1000L;
        long tInside = c.delayInsideMs;
        int N = Math.max(0, c.seedCount);
        int pairs = Math.max(0, N - 1);
        int disjoint = pairs / 2;
        int k = Math.min(c.threads, disjoint);
        if (tInside <= 0) return k * T;
        return (k * T) / tInside;
    }

    static long theoreticalArray(Config c) {
        long T = c.benchmarkSeconds * 1000L;
        long tInside = c.delayInsideMs;
        if (tInside <= 0) return T;
        return T / tInside;
    }
}