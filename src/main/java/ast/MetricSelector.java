package ast;

import java.util.List;

public class MetricSelector extends InstantVector {
    private final String metricName;
    private final List<LabelFilter> labelFilters;

    public MetricSelector(String metricName, List<LabelFilter> labelFilters) {
        this.metricName = metricName;
        this.labelFilters = labelFilters;
    }

    public String getMetricName() { return metricName; }
    public List<LabelFilter> getLabelFilters() { return labelFilters; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(metricName);
        if (labelFilters != null && !labelFilters.isEmpty()) {
            sb.append("{");
            for (LabelFilter f : labelFilters) {
                sb.append(f.toString()).append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("}");
        }
        return sb.toString();
    }
}

