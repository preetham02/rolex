import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AbstractGroupSubQuery {

    public static List<SelectSQLSubQuery.WhereCondition> toWhereConditions(Map<String,String> filters) {
        List<SelectSQLSubQuery.WhereCondition> whereConditions = new ArrayList<>();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            whereConditions.add(new SelectSQLSubQuery.WhereCondition(entry.getKey(), "=", "\"" + entry.getValue() + "\""));
        }
        return whereConditions;
    }

}
