package org.example;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App {
    private static final Object LOCK = new Object();
    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        String baseUrl = "http://localhost:" + port;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        Set<String> visited = ConcurrentHashMap.newKeySet();
        List<String> messages = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger active = new AtomicInteger(0);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            submitPath("/", baseUrl, client, visited, messages, active, executor);

            synchronized (LOCK) {
                while (active.get() > 0) {
                    LOCK.wait();
                }
            }
        }

        List<String> result;
        synchronized (messages) {
            result = new ArrayList<>(messages);
        }
        Collections.sort(result);
        for (String msg : result) {
            System.out.println(msg);
        }
    }

    private static void submitPath(
            String path,
            String baseUrl,
            HttpClient client,
            Set<String> visited,
            List<String> messages,
            AtomicInteger active,
            ExecutorService executor
    ) {
        if (!visited.add(path)) {
            return;
        }

        active.incrementAndGet();
        executor.submit(() -> {
            try {
                fetchAndProcessPath(path, baseUrl, client, visited, messages, active, executor);
            } finally {
                if (active.decrementAndGet() == 0) {
                    synchronized (LOCK) {
                        LOCK.notifyAll();
                    }
                }
            }
        });
    }

    private static void fetchAndProcessPath(
            String path,
            String baseUrl,
            HttpClient client,
            Set<String> visited,
            List<String> messages,
            AtomicInteger active,
            ExecutorService executor
    ) {
        try {
            String fullPath = normalizePath(path);
            URI uri = URI.create(baseUrl + fullPath);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return;
            }

            String body = response.body();
            String message = extractMessage(body);
            if (message != null) {
                messages.add(message);
            }

            List<String> successors = extractSuccessors(body);
            for (String succ : successors) {
                submitPath(succ, baseUrl, client, visited, messages, active, executor);
            }

        } catch (Exception e) {
        }
    }
    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        if (!path.startsWith("/")) {
            return "/" + path;
        }
        return path;
    }

    private static String extractMessage(String json) {
        Pattern p = Pattern.compile("\"message\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static List<String> extractSuccessors(String json) {
        List<String> result = new ArrayList<>();
        Pattern arrayPattern = Pattern.compile("\"successors\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
        Matcher arrayMatcher = arrayPattern.matcher(json);
        if (!arrayMatcher.find()) {
            return result;
        }
        String inside = arrayMatcher.group(1);
        Pattern itemPattern = Pattern.compile("\"(.*?)\"");
        Matcher itemMatcher = itemPattern.matcher(inside);
        while (itemMatcher.find()) {
            String value = itemMatcher.group(1).trim();
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }
}
