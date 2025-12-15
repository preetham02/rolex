import java.util.ArrayList;
import java.util.List;
import ast.LabelFilter;

public class AbstractGroupSubQuery {

    public static List<SelectSQLSubQuery.WhereCondition> toWhereConditions(List<LabelFilter> filters) {
        List<SelectSQLSubQuery.WhereCondition> whereConditions = new ArrayList<>();
        if (filters == null) return whereConditions;
        for (LabelFilter f : filters) {
            whereConditions.add(new SelectSQLSubQuery.WhereCondition(f.getKey(), f.getOperator(), "\"" + f.getValue() + "\""));
        }
        return whereConditions;
    }

}
