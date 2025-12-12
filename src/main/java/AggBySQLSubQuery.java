import java.util.List;
import java.util.Map;

public class AggBySQLSubQuery extends AbstractGroupSubQuery {

    private final String functionName;
    private final List<String> columns;
    private final String metricName;
    private final Map<String, String> filters;
    private final String duration;

    public AggBySQLSubQuery(String functionName, List<String> columns, String metricName, Map<String, String> filters, String duration) {
        this.functionName = functionName;
        this.columns = columns;
        this.metricName = metricName;
        this.filters = filters;
        this.duration = duration;
    }


    @Override
    public String toString() {
       String select =  String.join(", ", columns.stream().map( c -> "\"" + c + "\" : " + c).toArray(String[]::new));
       return  "SELECT { " +  select + "} ``group` , timestamp, " + functionName + "(value)  OVER ( " +
               " PARTITION BY " + String.join(", ", columns.stream().toArray(String[]::new)) +
               " ORDER BY timestamp " +
                " RANGE BETWEEN interval '" + AverageSQLSubquery.parseDuration(duration) + "' PRECEDING AND CURRENT ROW ) as `value` " +
               "  FROM " + metricName +
              ( filters.isEmpty() ? "" : (" WHERE " + String.join(" AND ", filters.entrySet().stream().map( e -> e.getKey() + " = \"" + e.getValue() + "\"").toArray(String[]::new))));
    }

}
