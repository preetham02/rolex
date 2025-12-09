import java.util.*;
import java.util.regex.*;

public class PromQLParser {

    public PromQLNode parse(String query) {
        if (query == null) return null;
        String trimmed = query.trim();
        if (trimmed.isEmpty()) return null;

        // Check for number literal
        if (trimmed.matches("^-?\\d+(\\.\\d+)?$")) {
            return new ScalarNode(Double.parseDouble(trimmed));
        }

        // Check for string literal
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return new StringLiteralNode(trimmed.substring(1, trimmed.length() - 1));
        }

        // Check for Function Call: name(...)
        int firstParen = trimmed.indexOf('(');
        if (firstParen > 0) {
            String name = trimmed.substring(0, firstParen);
            if (isValidIdentifier(name)) {
                int matchingParen = findMatchingParen(trimmed, firstParen);
                if (matchingParen == trimmed.length() - 1) {
                    String argsString = trimmed.substring(firstParen + 1, trimmed.length() - 1);
                    List<PromQLNode> args = parseArgs(argsString);
                    return new FunctionNode(name, args);
                }
            }
        }
        
        // Handle (expr) grouping
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
             int matchingParen = findMatchingParen(trimmed, 0);
             if (matchingParen == trimmed.length() - 1) {
                 return parse(trimmed.substring(1, trimmed.length() - 1));
             }
        }

        // Fallback: Metric Selector
        return parseMetric(trimmed);
    }

    private boolean isValidIdentifier(String s) {
        return s.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }

    private List<PromQLNode> parseArgs(String argsString) {
        List<PromQLNode> args = new ArrayList<>();
        if (argsString.trim().isEmpty()) return args;

        int start = 0;
        int parenDepth = 0;
        int braceDepth = 0; 
        boolean inQuote = false;
        char quoteChar = 0;

        for (int i = 0; i < argsString.length(); i++) {
            char c = argsString.charAt(i);

            if (inQuote) {
                if (c == quoteChar && (i == 0 || argsString.charAt(i - 1) != '\\')) {
                    inQuote = false;
                }
                continue;
            }

            if (c == '"' || c == '\'') {
                inQuote = true;
                quoteChar = c;
            } else if (c == '(') {
                parenDepth++;
            } else if (c == ')') {
                parenDepth--;
            } else if (c == '{') {
                braceDepth++;
            } else if (c == '}') {
                braceDepth--;
            } else if (c == ',' && parenDepth == 0 && braceDepth == 0) {
                String arg = argsString.substring(start, i);
                args.add(parse(arg));
                start = i + 1;
            }
        }

        if (start < argsString.length()) {
            args.add(parse(argsString.substring(start)));
        }

        return args;
    }

    private int findMatchingParen(String s, int startParen) {
        int depth = 0;
        boolean inQuote = false;
        char quoteChar = 0;

        for (int i = startParen; i < s.length(); i++) {
            char c = s.charAt(i);

            if (inQuote) {
                if (c == quoteChar && (i == 0 || s.charAt(i - 1) != '\\')) {
                    inQuote = false;
                }
                continue;
            }

            if (c == '"' || c == '\'') {
                inQuote = true;
                quoteChar = c;
                continue;
            }

            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private PromQLNode parseMetric(String selector) {
        Pattern p = Pattern.compile("^([a-zA-Z_:][a-zA-Z0-9_:]*)(\\{.*\\})?(\\[([0-9]+[smhdwy])\\])?$");
        Matcher m = p.matcher(selector);
        if (m.matches()) {
            String name = m.group(1);
            String labelPart = m.group(2);
            String range = m.group(4);

            Map<String, String> labels = new HashMap<>();
            if (labelPart != null && labelPart.length() > 2) {
                String content = labelPart.substring(1, labelPart.length() - 1);
                labels = parseLabels(content);
            }

            return new MetricNode(name, labels, range);
        }
        return null;
    }

    private Map<String, String> parseLabels(String content) {
        Map<String, String> labels = new HashMap<>();
        List<String> pairs = new ArrayList<>();
        int start = 0;
        boolean inQuote = false;
        char quoteChar = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (inQuote) {
                if (c == quoteChar && (i == 0 || content.charAt(i - 1) != '\\')) {
                    inQuote = false;
                }
            } else {
                if (c == '"' || c == '\'') {
                    inQuote = true;
                    quoteChar = c;
                } else if (c == ',') {
                    pairs.add(content.substring(start, i));
                    start = i + 1;
                }
            }
        }
        if (start < content.length()) {
            pairs.add(content.substring(start));
        }

        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key = kv[0].trim();
                String val = kv[1].trim();
                if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                    val = val.substring(1, val.length() - 1);
                }
                labels.put(key, val);
            }
        }
        return labels;
    }
}

