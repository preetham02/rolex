import java.sql.Struct;
import java.util.List;
import java.util.Map;

public class GroupBySQLSubQuery extends AbstractGroupSubQuery {

    private final List<String> columns;
    private final String metricName;
    private final List<SelectSQLSubQuery.WhereCondition> filters;

    public GroupBySQLSubQuery(List<String> columns, String metricName, Map<String, String> filters) {
        super();
        this.columns = columns;
        this.metricName = metricName;
        this.filters = AbstractGroupSubQuery.toWhereConditions(filters);
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
       return  "SELECT { " + select  +" } `group`  ,  `timestamp`, `value`  FROM " + metricName +
              ( filters.isEmpty() ? "" : (" WHERE " + String.join(" AND ", filters.stream().map( whereCondition -> whereCondition.columnName + whereCondition.operator + whereCondition.value).toArray(String[]::new))));

    }
}
