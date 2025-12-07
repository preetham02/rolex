import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            processQuery(t);

            // Fake response for instant query
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
            processQuery(t);

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

    private static void processQuery(HttpExchange t) {
        URI requestURI = t.getRequestURI();
        String rawQuery = requestURI.getRawQuery();
        Map<String, String> params = parseQueryParams(rawQuery);
        
        String promQuery = params.get("query");
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        
        System.out.printf("[%s] Method: %s, Path: %s%n", 
            timestamp, t.getRequestMethod(), requestURI.getPath());
            
        if (promQuery != null) {
            System.out.println("    PromQL: " + promQuery);
            String sql = convertToSQL(promQuery);
            System.out.println("    SQL:    " + sql);
        } else {
            System.out.println("    No query parameter found");
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        if (query == null) return Map.of();
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(
                    URLDecoder.decode(entry[0], StandardCharsets.UTF_8), 
                    URLDecoder.decode(entry[1], StandardCharsets.UTF_8)
                );
            }
        }
        return result;
    }

    private static String convertToSQL(String promQuery) {
        if (promQuery == null || promQuery.isEmpty()) return "INVALID QUERY";
        
        // Basic parser for metric{label="value"}
        // This is a simplified regex and won't handle all PromQL nuances (like nested braces in values)
        // Group 1: Metric Name
        // Group 2: Labels block (optional)
        Pattern p = Pattern.compile("^([a-zA-Z_:][a-zA-Z0-9_:]*)(\\{.*\\})?$");
        Matcher m = p.matcher(promQuery);
        
        if (m.find()) {
            String metricName = m.group(1);
            String labelPart = m.groupCount() >= 2 ? m.group(2) : null;
            
            StringBuilder sql = new StringBuilder("SELECT * FROM metrics WHERE name = '");
            sql.append(metricName).append("'");
            
            if (labelPart != null && !labelPart.isEmpty()) {
                // Remove outer {}
                String labels = labelPart.substring(1, labelPart.length() - 1);
                
                // Split by comma. Note: This breaks if label values contain commas.
                // A proper parser would be needed for full support.
                String[] pairs = labels.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split("=");
                    if (kv.length == 2) {
                        String key = kv[0].trim();
                        String value = kv[1].trim();
                        
                        // Convert double quotes to single quotes for SQL
                        // value is likely "something", we want 'something'
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = "'" + value.substring(1, value.length() - 1) + "'";
                        } else if (value.startsWith("'") && value.endsWith("'")) {
                            // already single quoted
                        } else {
                            // unquoted? treat as string
                            value = "'" + value + "'";
                        }
                        
                        // Handle operators like !=, =~, !~ later. Assuming = for now.
                        // If the split was by =, check if previous char was ! or ~
                        // For this simple parser, we assume =
                        
                        sql.append(" AND labels->>'").append(key).append("' = ").append(value);
                    }
                }
            }
            return sql.toString();
        }
        
        return "-- Complex or unsupported PromQL query: " + promQuery;
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
