public class GroupAggSubQuery extends AbstractGroupSubQuery{

    AbstractGroupSubQuery groupSubQuery;
    String functionName;

    public GroupAggSubQuery(AbstractGroupSubQuery groupSubQuery, String functionName) {
        super();
        this.groupSubQuery = groupSubQuery;
        this.functionName = functionName;
    }

    @Override
    public String toString() {
        return "SELECT `group`, timestamp,  " + functionName + "(`value`) " +
                "OVER( " +
                " PARTITION BY `group` " +
                " ORDER BY timestamp ) as `value` " +
                " FROM ( " + groupSubQuery.toString() + " ) sub";

    }
}
