public class SQLQuery extends AbstractSQLSubQuery {

    private final AbstractSQLSubQuery subQuery;

    public SQLQuery(AbstractSQLSubQuery subQuery) {
        super();
        this.subQuery = subQuery;
    }

    @Override
    public String toString() {
        return subQuery.toString() + ";";
    }
}
