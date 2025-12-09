import java.util.List;

public class SelectSQLSubQuery extends AbstractSQLSubQuery{

    List<WhereCondition> selectedColumns;
    AbstractSQLSubQuery innerSubquery;

    public SelectSQLSubQuery(List<WhereCondition> selectedColumns, AbstractSQLSubQuery innerSubquery) {
        super();
        this.selectedColumns = selectedColumns;
        this.innerSubquery = innerSubquery;
    }

    @Override
    public String toString() {
        return "SELECT * FROM (" + innerSubquery.toString() + ")"
                + " WHERE " + String.join(" AND ", selectedColumns.stream().map(WhereCondition::toString).toArray(String[]::new));
    }


    public static class WhereCondition{

        String columnName;
        String operator;
        String value;


        public WhereCondition(String columnName, String operator, String value) {
            this.columnName = columnName;
            this.operator = operator;
            this.value = value;
        }


        @Override
        public String toString() {
            return columnName + " " + operator + " " + value;
        }


    }

}
