import java.util.List;

public class GroupAggSubQuery extends AbstractGroupSubQuery{

    AbstractGroupSubQuery groupSubQuery;
    String functionName;
    List<String> groups;

    public GroupAggSubQuery(AbstractGroupSubQuery groupSubQuery, String functionName, List<String> groups) {
        super();
        this.groupSubQuery = groupSubQuery;
        this.functionName = functionName;
        this.groups = groups;
    }

    public List<String> getGroups() {
        return groups;
    }

    @Override
    public String toString() {
        return "SELECT `group`, timestamp,  " + functionName + "(`value`) " +
                "OVER( " +
                " PARTITION BY  " + String.join(", ", groups.stream().map(s -> "`group`.`" + s + "`").toArray(String[]::new) ) +
                " ORDER BY timestamp ) as `value` " +
                " FROM ( " + groupSubQuery.toString() + " ) sub";

    }
}
