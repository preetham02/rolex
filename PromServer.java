import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class PromServer {

    public static void main(String[] args) throws IOException {
        int port = 9090;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/v1/query", new QueryHandler());
        server.createContext("/api/v1/query_range", new QueryRangeHandler());

        server.setExecutor(null); // creates a default executor
        System.out.println("Starting fake Prometheus server on port " + port + "...");
        server.start();
    }

    static class QueryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            logQuery(t);

            // Fake response for instant query
            // Using a text block for JSON response
            long now = Instant.now().getEpochSecond();
            String response = """
                {
                    "status": "success",
                    "data": {
                        "resultType": "vector",
                        "result": [
                            {
                                "metric": {
                                    "__name__": "fake_metric",
                                    "job": "fake_job",
                                    "instance": "fake_instance"
                                },
                                "value": [%d, "123.45"]
                            }
                        ]
                    }
                }
                """.formatted(now);

            sendResponse(t, response);
        }
    }

    static class QueryRangeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            logQuery(t);

            // Fake response for range query
            long now = Instant.now().getEpochSecond();
            String response = """
                {
                    "status": "success",
                    "data": {
                        "resultType": "matrix",
                        "result": [
                            {
                                "metric": {
                                    "__name__": "fake_metric",
                                    "job": "fake_job",
                                    "instance": "fake_instance"
                                },
                                "values": [
                                    [%d, "100"],
                                    [%d, "110"],
                                    [%d, "120"]
                                ]
                            }
                        ]
                    }
                }
                """.formatted(now - 60, now - 30, now);

            sendResponse(t, response);
        }
    }

    private static void logQuery(HttpExchange t) {
        URI requestURI = t.getRequestURI();
        String query = "";
        String rawQuery = requestURI.getRawQuery();
        
        // Simple parsing to extract 'query' param for logging, or just log the full raw query
        if (rawQuery != null) {
            // Find "query=" parameter if possible, otherwise just use raw
            // This is a naive logger, for production use a proper parser
            query = rawQuery; 
        }

        String timestamp = DateTimeFormatter.ISO_INSTANT
            .format(Instant.now());
            
        System.out.printf("[%s] Method: %s, Path: %s, Query Params: %s%n", 
            timestamp, t.getRequestMethod(), requestURI.getPath(), query);
    }

    private static void sendResponse(HttpExchange t, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        t.getResponseHeaders().set("Content-Type", "application/json");
        t.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = t.getResponseBody()) {
            os.write(bytes);
        }
    }
}

