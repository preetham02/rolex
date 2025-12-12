import java.sql.Struct;
import java.util.List;
import java.util.Map;

public class GroupBySQLSubQuery extends AbstractGroupSubQuery {

    private final List<String> columns;
    private final String metricName;
    private final Map<String, String> filters;

    public GroupBySQLSubQuery(List<String> columns, String metricName, Map<String, String> filters) {
        super();
        this.columns = columns;
        this.metricName = metricName;
        this.filters = filters;
    }


    @Override
    public String toString() {
       String select =  String.join(", ", columns.stream().map( c -> "\"" + c + "\" : " + c).toArray(String[]::new));
       return  "SELECT { " + select  +" } `group`  ,  `timestamp`, `value`  FROM " + metricName +
              ( filters.isEmpty() ? "" : (" WHERE " + String.join(" AND ", filters.entrySet().stream().map( e -> e.getKey() + " = \"" + e.getValue() + "\"").toArray(String[]::new))));

    }
}
