import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.ArrayList;

public class PromServer {

    private static String BACKEND_TYPE;
    private static String BACKEND_URI;
    private static String BACKEND_USER;
    private static String BACKEND_PASSWORD;

    public static void main(String[] args) throws IOException {
        loadConfig();
        int port = 9090;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/v1/query", new QueryHandler());
        server.createContext("/api/v1/query_range", new QueryRangeHandler());

        server.setExecutor(null); // creates a default executor
        System.out.println("Starting fake Prometheus server on port " + port + "...");
        server.start();
    }

    private static void loadConfig() {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream("config.properties")) {
            prop.load(input);
            BACKEND_TYPE = prop.getProperty("backend", "couchbase");
            BACKEND_URI = prop.getProperty("uri", "http://localhost:9600/query/service");
            BACKEND_USER = prop.getProperty("username", "");
            BACKEND_PASSWORD = prop.getProperty("password", "");
            System.out.println("Loaded config: backend=" + BACKEND_TYPE + ", uri=" + BACKEND_URI);
        } catch (IOException ex) {
            System.err.println("Could not load config.properties, using defaults.");
            BACKEND_TYPE = "couchbase";
            BACKEND_URI = "http://localhost:9600/query/service";
            BACKEND_USER = "couchbase";
            BACKEND_PASSWORD = "couchbase";
        }
    }

    static class QueryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            processQuery(t, "vector");
        }
    }

    static class QueryRangeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            processQuery(t, "matrix");
        }
    }

    private static void processQuery(HttpExchange t, String resultType) throws IOException {
        URI requestURI = t.getRequestURI();
        String rawQuery = requestURI.getRawQuery();
        Map<String, String> params = new HashMap<>();
        if (rawQuery != null) {
            params.putAll(parseQueryParams(rawQuery));
        }
        
        // Handle POST body parameters
        if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
            try (InputStream is = t.getRequestBody()) {
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                if (!body.isEmpty()) {
                    Map<String, String> bodyParams = parseQueryParams(body);
                    params.putAll(bodyParams);
                }
            }
        }
        
        String promQuery = params.get("query");
        String start = params.get("start");
        String end = params.get("end");
        String step = params.get("step");
        
        Long startTs = parseTimestamp(start);
        Long endTs = parseTimestamp(end);
        Long stepDuration = parseStep(step);

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        
        System.out.printf("[%s] Method: %s, Path: %s%n", 
            timestamp, t.getRequestMethod(), requestURI.getPath());
            
        if (promQuery != null) {
            System.out.println("    PromQL: " + promQuery);
            if (start != null) System.out.println("    Start:  " + start + " -> " + startTs);
            if (end != null)   System.out.println("    End:    " + end + " -> " + endTs);
            if (step != null)  System.out.println("    Step:   " + step + " -> " + stepDuration);

            String sql = convertPromToSQL(promQuery, startTs, endTs, stepDuration);
            System.out.println("    SQL:    " + sql);

            try {
                String responseJson = executeQuery(sql);
                String promResponse = transformToPromResponse(responseJson, resultType);
                sendResponse(t, promResponse);
            } catch (Exception e) {
                e.printStackTrace();
                String errorResponse = "{\"status\":\"error\",\"errorType\":\"execution\",\"error\":\"" + e.getMessage() + "\"}";
                sendResponse(t, errorResponse);
            }

        } else {
            System.out.println("    No query parameter found");
            String errorResponse = "{\"status\":\"error\",\"errorType\":\"bad_data\",\"error\":\"missing query parameter\"}";
            sendResponse(t, errorResponse);
        }
    }

    private static Long parseTimestamp(String ts) {
        if (ts == null) return null;
        try {
            // Try parsing as double (unix timestamp in seconds)
            return (long) (Double.parseDouble(ts) * 1000);
        } catch (NumberFormatException e) {
            // Try parsing as ISO 8601
            try {
                return Instant.parse(ts).toEpochMilli();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private static Long parseStep(String step) {
        if (step == null) return null;
        try {
             // Try parsing as seconds (float)
             return (long) (Double.parseDouble(step) * 1000);
        } catch (NumberFormatException e) {
             // Parse duration string (e.g. "1m", "5s")
             return AverageSQLSubquery.parseDuration(step); 
        }
    }

    public static String convertPromToSQL(String promQuery, Long startTs, Long endTs, Long stepDuration) {

        PromQLNode root = new PromQLParser().parse(promQuery);
        AbstractSQLSubQuery sqlSubQuery = visitRoot(root, startTs, endTs, stepDuration);

        if(startTs == null || endTs == null || stepDuration == null) {
            return sqlSubQuery + ";";
        }


        SelectSQLSubQuery sqlSubQuery1 = new SelectSQLSubQuery(
                List.of(
                        new SelectSQLSubQuery.WhereCondition("timestamp", ">=", String.valueOf(startTs)),
                        new SelectSQLSubQuery.WhereCondition("timestamp", "<=", String.valueOf(endTs)),
                        new SelectSQLSubQuery.WhereCondition( String.format(" ( timestamp - %s) %% %s ",startTs, stepDuration) , "=", "0")

                ),
                sqlSubQuery
        );


        return sqlSubQuery1 + ";";

    }

    public static AbstractSQLSubQuery averageQuery(AbstractSQLSubQuery subQuery, MetricNode metricNode)
    {
        return new AverageSQLSubquery(subQuery, metricNode.getRangeDuration());


    }


    public static AbstractSQLSubQuery maxQuery(AbstractSQLSubQuery subQuery, MetricNode metricNode)
    {
        return new MaxSQLSubQuery(subQuery, metricNode.getRangeDuration());
    }


    private static AbstractSQLSubQuery visitRoot(PromQLNode node, Long startTs, Long endTs, Long stepDuration) {


        if(node instanceof  FunctionNode) {
            FunctionNode fn = (FunctionNode) node;
            String functionName = fn.getFunctionName();
            List<PromQLNode> args = fn.getArgs();
            if(args.size() !=1) {
                return DummySQLSubquery.INSTANCE;
            }

            if(!(args.get(0) instanceof MetricNode)) {
                return DummySQLSubquery.INSTANCE;
            }

            MetricNode metricNode = (MetricNode) args.get(0);


            AbstractSQLSubQuery  sqlSubQuery= convertMetricToSQL(metricNode,startTs,endTs,stepDuration);

            if(functionName.equals("avg_over_time")){
                return averageQuery(sqlSubQuery,metricNode);
            }else if(functionName.equals("max_over_time")){
                return maxQuery(sqlSubQuery,metricNode);
            }



            StringBuilder builder = new StringBuilder();

            builder.append("-- Function: ").append(functionName).append("\n");


            for(PromQLNode arg : fn.getArgs()) {
                builder.append("-- Arg: \n");
                builder.append(visitRoot(arg,startTs,endTs,startTs)).append("\n");
            }

            return DummySQLSubquery.INSTANCE;


        }else if(node instanceof MetricNode){

            AbstractSQLSubQuery  sqlSubQuery= convertMetricToSQL((MetricNode) node,startTs,endTs,stepDuration);
            return sqlSubQuery;
        } else if (node instanceof StringLiteralNode ) {
            return DummySQLSubquery.INSTANCE;
        } else if (node instanceof ScalarNode) {
            return DummySQLSubquery.INSTANCE;
        }


        return DummySQLSubquery.INSTANCE;
    }

    public static AbstractSQLSubQuery convertMetricToSQL(MetricNode metricNode, Long startTs, Long endTs, Long stepDuration) {

        if(metricNode.getLabels().isEmpty()) {

            CollectionSQLSubQuery subQuery = new CollectionSQLSubQuery(metricNode.getMetricName());


            if(startTs == null || endTs == null || stepDuration == null) {
                return subQuery;
            }


            return new SelectSQLSubQuery(
                    List.of(
                            new SelectSQLSubQuery.WhereCondition("timestamp", ">=", String.valueOf(startTs- stepDuration)),
                            new SelectSQLSubQuery.WhereCondition("timestamp", "<=", String.valueOf(endTs))
                    ),
                   subQuery
            );


        }


        return null;




    }


//    SELECT
//            timestamp ,
//    `value`,
//    AVG(`value`) OVER (
//    PARTITION BY symbol
//    ORDER BY timestamp
//    RANGE BETWEEN 3600000 PRECEDING
//    AND CURRENT ROW
//    ) AS avg_1h
//    FROM stock_market_price_usd
//    ORDER BY timestamp;




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

        // Check for function call: func(arg)
        Pattern funcPattern = Pattern.compile("^([a-zA-Z_]+)\\((.+)\\)$");
        Matcher funcMatcher = funcPattern.matcher(promQuery);

        if (funcMatcher.find()) {
            String funcName = funcMatcher.group(1);
            String args = funcMatcher.group(2);
            return convertSelectorToSQL(args, funcName);
        }
        
        // Default to selection
        return convertSelectorToSQL(promQuery, null);
    }

    private static String convertSelectorToSQL(String selector, String function) {
        // Regex for metric name, optional labels, optional range
        // Group 1: Metric Name
        // Group 2: Labels block (optional, includes {})
        // Group 3: Range block (optional, includes [])
        // Group 4: Range duration (inside [])
        Pattern p = Pattern.compile("^([a-zA-Z_:][a-zA-Z0-9_:]*)(\\{.*\\})?(\\[([0-9]+[smhdwy])\\])?$");
        Matcher m = p.matcher(selector);
        
        if (m.find()) {
            String metricName = m.group(1);
            String labelPart = m.group(2);
            String rangeDuration = m.group(4);
            
            StringBuilder sql = new StringBuilder("SELECT ");
            
            // Determine SELECT clause based on function
            if (function != null) {
                switch (function) {
                    case "avg_over_time" -> sql.append("AVG(value)");
                    case "max_over_time" -> sql.append("MAX(value)");
                    case "stddev_over_time" -> sql.append("STDDEV(value)");
                    case "sum_over_time" -> sql.append("SUM(value)");
                    case "count_over_time" -> sql.append("COUNT(value)");
                    case "delta" -> sql.append("value - LAG(value) OVER (ORDER BY time)"); // Simplified representation
                    case "deriv" -> sql.append("REGR_SLOPE(value, EXTRACT(EPOCH FROM time))");
                    default -> sql.append(function.toUpperCase()).append("(value)");
                }
            } else {
                sql.append("*");
            }
            
            sql.append(" FROM metrics WHERE name = '").append(metricName).append("'");
            
            if (labelPart != null && !labelPart.isEmpty()) {
                String labels = labelPart.substring(1, labelPart.length() - 1);
                String[] pairs = labels.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split("=");
                    if (kv.length == 2) {
                        String key = kv[0].trim();
                        String value = kv[1].trim();
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = "'" + value.substring(1, value.length() - 1) + "'";
                        } else if (!value.startsWith("'")) {
                            value = "'" + value + "'";
                        }
                        sql.append(" AND labels->>'").append(key).append("' = ").append(value);
                    }
                }
            }
            
            if (rangeDuration != null) {
                // Convert duration to SQL interval approx
                sql.append(" AND time >= NOW() - INTERVAL '").append(rangeDuration).append("'");
            }
            
            return sql.toString();
        }
        
        return "-- Complex or unsupported PromQL query: " + selector;
    }

    private static void sendResponse(HttpExchange t, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        t.getResponseHeaders().set("Content-Type", "application/json");
        t.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = t.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String executeQuery(String sql) throws IOException, InterruptedException {
        if ("asterixdb".equalsIgnoreCase(BACKEND_TYPE)) {
            return executeAsterixDBQuery(sql);
        } else {
            return executeCouchbaseQuery(sql);
        }
    }

    private static String executeCouchbaseQuery(String sql) throws IOException, InterruptedException {
        // Escape quotes and backslashes in SQL for JSON
        String escapedSql = sql.replace("\\", "\\\\").replace("\"", "\\\"");
        
        String jsonBody = """
            {
               "statement": "%s",
                "pretty": true,
                "client_context_id": "xyz"
            }
            """.formatted(escapedSql);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(BACKEND_URI))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (BACKEND_USER != null && !BACKEND_USER.isEmpty()) {
             requestBuilder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((BACKEND_USER + ":" + BACKEND_PASSWORD).getBytes(StandardCharsets.UTF_8)));
        }

        HttpRequest request = requestBuilder.build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Couchbase query failed: HTTP " + response.statusCode() + " " + response.body());
        }
        
        return response.body();
    }

    private static String executeAsterixDBQuery(String sql) throws IOException, InterruptedException {
        String escapedSql = sql.replace("\\", "\\\\").replace("\"", "\\\"");
        
        String jsonBody = """
            {
               "statement": "%s",
                "pretty": true
            }
            """.formatted(escapedSql);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BACKEND_URI))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("AsterixDB query failed: HTTP " + response.statusCode() + " " + response.body());
        }
        
        return response.body();
    }

    private static String transformToPromResponse(String couchbaseJson, String resultType) {
        // Extract results array
        Pattern resultsPattern = Pattern.compile("\"results\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher resultsMatcher = resultsPattern.matcher(couchbaseJson);
        
        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append("[");
        
        if (resultsMatcher.find()) {
            String resultsContent = resultsMatcher.group(1);
            if (!resultsContent.trim().isEmpty()) {
                List<String> rows = extractJsonObjects(resultsContent);
                
                // Group by metric labels
                Map<Map<String, String>, List<Map<String, Object>>> groupedSeries = new HashMap<>();

                for (String row : rows) {
                    Map<String, String> fields = extractFields(row);
                    
                    // Identify timestamp, value, and labels
                    String timestampStr = null;
                    String valueStr = null;
                    Map<String, String> labels = new HashMap<>();
                    
                    for (Map.Entry<String, String> entry : fields.entrySet()) {
                        String key = entry.getKey();
                        String val = entry.getValue();
                        
                        // Handle nested object if "SELECT *" (key is bucket name)
                        if (val.trim().startsWith("{")) {
                            Map<String, String> innerFields = extractFields(val);
                            for (Map.Entry<String, String> inner : innerFields.entrySet()) {
                                String innerKey = inner.getKey();
                                String innerVal = inner.getValue();
                                if (isTimestamp(innerKey)) timestampStr = innerVal;
                                else if (isValue(innerKey)) valueStr = innerVal;
                                else labels.put(innerKey, innerVal.replace("\"", ""));
                            }
                             labels.put("__name__", key);
                        } else {
                            if (isTimestamp(key)) timestampStr = val;
                            else if (isValue(key)) valueStr = val;
                            else labels.put(key, val.replace("\"", ""));
                        }
                    }
                    
                    if (timestampStr == null) timestampStr = String.valueOf(Instant.now().getEpochSecond());
                    if (valueStr == null) valueStr = "0";

                    double ts = 0;
                    try {
                        String cleanTs = timestampStr.replace("\"", "");
                        if (cleanTs.contains("-")) {
                             ts = Instant.parse(cleanTs).getEpochSecond();
                        } else {
                            ts = Double.parseDouble(cleanTs);
                            if (ts > 100000000000L) ts /= 1000.0;
                        }
                    } catch (Exception e) {
                        ts = Instant.now().getEpochSecond();
                    }
                    
                    String cleanVal = valueStr.replace("\"", "");

                    // Add to group
                    groupedSeries.computeIfAbsent(labels, k -> new ArrayList<>())
                        .add(Map.of("ts", ts, "val", cleanVal));
                }

                // Build JSON from groups
                boolean firstGroup = true;
                for (Map.Entry<Map<String, String>, List<Map<String, Object>>> entry : groupedSeries.entrySet()) {
                    if (!firstGroup) resultBuilder.append(",");
                    firstGroup = false;

                    Map<String, String> labels = entry.getKey();
                    List<Map<String, Object>> values = entry.getValue();
                    
                    // Sort values by timestamp
                    values.sort((a, b) -> Double.compare((Double) a.get("ts"), (Double) b.get("ts")));

                    StringBuilder metricBuilder = new StringBuilder();
                    metricBuilder.append("{");
                    metricBuilder.append("\"metric\":{");
                    boolean firstLabel = true;
                    for (Map.Entry<String, String> label : labels.entrySet()) {
                        if (!firstLabel) metricBuilder.append(",");
                        metricBuilder.append("\"").append(label.getKey()).append("\":\"").append(label.getValue()).append("\"");
                        firstLabel = false;
                    }
                    metricBuilder.append("},");
                    
                    if ("matrix".equals(resultType)) {
                        metricBuilder.append("\"values\":[");
                        boolean firstVal = true;
                        for (Map<String, Object> point : values) {
                            if (!firstVal) metricBuilder.append(",");
                            firstVal = false;
                            metricBuilder.append("[").append(point.get("ts")).append(",\"").append(point.get("val")).append("\"]");
                        }
                        metricBuilder.append("]");
                    } else {
                        // For vector, just take the last value (most recent)
                        if (!values.isEmpty()) {
                            Map<String, Object> point = values.get(values.size() - 1);
                            metricBuilder.append("\"value\":[")
                                .append(point.get("ts")).append(",\"").append(point.get("val")).append("\"]");
                        }
                    }
                    metricBuilder.append("}");
                    
                    resultBuilder.append(metricBuilder);
                }
            }
        }
        
        resultBuilder.append("]");
        
        return """
            {
                "status": "success",
                "data": {
                    "resultType": "%s",
                    "result": %s
                }
            }
            """.formatted(resultType, resultBuilder.toString());
    }

    private static boolean isTimestamp(String key) {
        return key.equalsIgnoreCase("timestamp") || key.equalsIgnoreCase("time") || key.equalsIgnoreCase("t");
    }

    private static boolean isValue(String key) {
        return key.equalsIgnoreCase("value") || key.equalsIgnoreCase("v") || key.equalsIgnoreCase("val");
    }

    private static List<String> extractJsonObjects(String jsonArrayContent) {
        List<String> objects = new java.util.ArrayList<>();
        int depth = 0;
        int start = -1;
        boolean inQuote = false;
        
        for (int i = 0; i < jsonArrayContent.length(); i++) {
            char c = jsonArrayContent.charAt(i);
            
            if (c == '"' && (i == 0 || jsonArrayContent.charAt(i - 1) != '\\')) {
                inQuote = !inQuote;
            }
            
            if (!inQuote) {
                if (c == '{') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start != -1) {
                        objects.add(jsonArrayContent.substring(start, i + 1));
                        start = -1;
                    }
                }
            }
        }
        return objects;
    }

    private static Map<String, String> extractFields(String jsonObject) {
        Map<String, String> fields = new HashMap<>();
        String content = jsonObject.trim();
        if (content.startsWith("{")) content = content.substring(1);
        if (content.endsWith("}")) content = content.substring(0, content.length() - 1);
        
        int i = 0;
        int len = content.length();
        while (i < len) {
            int keyStart = content.indexOf("\"", i);
            if (keyStart == -1) break;
            int keyEnd = content.indexOf("\"", keyStart + 1);
            if (keyEnd == -1) break;
            String key = content.substring(keyStart + 1, keyEnd);
            
            i = content.indexOf(":", keyEnd);
            if (i == -1) break;
            i++; 
            
            while (i < len && Character.isWhitespace(content.charAt(i))) i++;
            
            int valStart = i;
            int valEnd = -1;
            
            if (i < len && content.charAt(i) == '{') {
                int depth = 0;
                for (int j = i; j < len; j++) {
                    char c = content.charAt(j);
                    if (c == '{') depth++;
                    else if (c == '}') {
                        depth--;
                        if (depth == 0) {
                            valEnd = j + 1;
                            break;
                        }
                    }
                }
            } else if (i < len && content.charAt(i) == '"') {
                for (int j = i + 1; j < len; j++) {
                    if (content.charAt(j) == '"' && content.charAt(j - 1) != '\\') {
                        valEnd = j + 1;
                        break;
                    }
                }
            } else {
                for (int j = i; j < len; j++) {
                    char c = content.charAt(j);
                    if (c == ',' || c == '}') {
                        valEnd = j;
                        break;
                    }
                }
                if (valEnd == -1) valEnd = len;
            }
            
            if (valEnd != -1) {
                String val = content.substring(valStart, valEnd).trim();
                fields.put(key, val);
                i = valEnd;
                while (i < len && (Character.isWhitespace(content.charAt(i)) || content.charAt(i) == ',')) i++;
            } else {
                break;
            }
        }
        return fields;
    }






}
