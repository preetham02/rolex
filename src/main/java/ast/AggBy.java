package ast;

import java.util.List;

public class AggBy implements GroupInstantVector {
    private final String functionName;
    private final List<String> columns;
    private final String metricName;
    private final List<LabelFilter> filters;
    private final String duration;

    public AggBy(String functionName, List<String> columns, String metricName, List<LabelFilter> filters, String duration) {
        this.functionName = functionName;
        this.columns = columns;
        this.metricName = metricName;
        this.filters = filters;
        this.duration = duration;
    }

    public String getFunctionName() { return functionName; }
    public List<String> getColumns() { return columns; }
    public String getMetricName() { return metricName; }
    public List<LabelFilter> getFilters() { return filters; }
    public String getDuration() { return duration; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(functionName).append("_by[");
        sb.append(String.join(", ", columns)).append("](");
        sb.append(metricName);
        if (filters != null && !filters.isEmpty()) {
            sb.append("{");
            for (LabelFilter f : filters) {
                sb.append(f.toString()).append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("}");
        }
        if (duration != null) {
            sb.append("[").append(duration).append("]");
        }
        sb.append(")");
        return sb.toString();
    }
}
