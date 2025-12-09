import java.util.List;

public class ProjectSQLSubquery extends AbstractSQLSubQuery{

    private final List<String> projectedColumns;

    private final AbstractSQLSubQuery innerSubquery;

    public  ProjectSQLSubquery(List<String> projectedColumns, AbstractSQLSubQuery innerSubquery) {
            super();
            this.projectedColumns = projectedColumns;
            this.innerSubquery = innerSubquery;
        }

    @Override
    public String toString() {
        return "SELECT " + String.join(", ", projectedColumns) + " FROM (" + innerSubquery.toString() + ")";
    }
}
