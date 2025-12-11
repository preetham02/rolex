package ast;

import java.util.Map;

public class GroupMetricSelector implements GroupInstantVector {
    private final String metricName;
    private final Map<String, String> labels;

    public GroupMetricSelector(String metricName, Map<String, String> labels) {
        this.metricName = metricName;
        this.labels = labels;
    }

    public String getMetricName() { return metricName; }
    public Map<String, String> getLabels() { return labels; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(metricName);
        if (labels != null && !labels.isEmpty()) {
            sb.append("{");
            labels.forEach((k, v) -> sb.append(k).append("='").append(v).append("',"));
            sb.setLength(sb.length() - 1); // remove last comma
            sb.append("}");
        }
        return sb.toString();
    }
}

