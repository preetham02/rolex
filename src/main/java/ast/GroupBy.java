package ast;

import java.util.List;
import java.util.Map;

public class GroupBy implements GroupInstantVector {
    private final List<String> columns;
    private final String metricName;
    private final Map<String, String> filters;

    public GroupBy(List<String> columns, String metricName, Map<String, String> filters) {
        this.columns = columns;
        this.metricName = metricName;
        this.filters = filters;
    }

    public List<String> getColumns() { return columns; }
    public String getMetricName() { return metricName; }
    public Map<String, String> getFilters() { return filters; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("group_by[");
        sb.append(String.join(", ", columns)).append("](");
        sb.append(metricName);
        if (filters != null && !filters.isEmpty()) {
            sb.append("{");
            filters.forEach((k, v) -> sb.append(k).append("='").append(v).append("',"));
            sb.setLength(sb.length() - 1);
            sb.append("}");
        }
        sb.append(")");
        return sb.toString();
    }
}

