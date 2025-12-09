import java.util.Map;

public class MetricNode implements PromQLNode {
    private final String metricName;
    private final Map<String, String> labels;
    private final String rangeDuration; // e.g., "1d", "5m"

    public MetricNode(String metricName, Map<String, String> labels, String rangeDuration) {
        this.metricName = metricName;
        this.labels = labels;
        this.rangeDuration = rangeDuration;
    }

    public String getMetricName() {
        return metricName;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public String getRangeDuration() {
        return rangeDuration;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(metricName);
        if (labels != null && !labels.isEmpty()) {
            sb.append("{");
            int i = 0;
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                if (i < labels.size() - 1) {
                    sb.append(", ");
                }
                i++;
            }
            sb.append("}");
        }
        if (rangeDuration != null) {
            sb.append("[").append(rangeDuration).append("]");
        }
        return sb.toString();
    }
}

