import java.util.List;
import ast.LabelFilter;

public class AggBySQLSubQuery extends AbstractGroupSubQuery {

    private final String functionName;
    private final List<String> columns;
    private final String metricName;
    private final List<SelectSQLSubQuery.WhereCondition> filters;
    private final String duration;

    public AggBySQLSubQuery(String functionName, List<String> columns, String metricName, List<LabelFilter> filters, String duration) {
        this.functionName = functionName;
        this.columns = columns;
        this.metricName = metricName;
        this.filters = AbstractGroupSubQuery.toWhereConditions(filters);
        this.duration = duration;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void pushDownFilter(List<SelectSQLSubQuery.WhereCondition> newFilters) {
        this.filters.addAll(newFilters);
    }

    @Override
    public String toString() {
       String select =  String.join(", ", columns.stream().map( c -> "\"" + c + "\" : " + c).toArray(String[]::new));
       return  "SELECT { " +  select + "} `group` , timestamp, " + functionName + "(`value`)  OVER ( " +
               " PARTITION BY " + String.join(", ", columns.stream().toArray(String[]::new)) +
               " ORDER BY timestamp " +
                " RANGE BETWEEN  " + AverageSQLSubquery.parseDuration(duration) + " PRECEDING AND CURRENT ROW ) as `value` " +
               "  FROM " + metricName +
               ( filters.isEmpty() ? "" : (" WHERE " + String.join(" AND ", filters.stream().map( whereCondition -> whereCondition.columnName + whereCondition.operator + whereCondition.value).toArray(String[]::new))));
    }

}
