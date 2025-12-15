package ast;

import java.util.List;

public class GroupBy implements GroupInstantVector {
    private final List<String> columns;
    private final String metricName;
    private final List<LabelFilter> filters;

    public GroupBy(List<String> columns, String metricName, List<LabelFilter> filters) {
        this.columns = columns;
        this.metricName = metricName;
        this.filters = filters;
    }

    public List<String> getColumns() { return columns; }
    public String getMetricName() { return metricName; }
    public List<LabelFilter> getFilters() { return filters; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("group_by[");
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
        sb.append(")");
        return sb.toString();
    }
}

