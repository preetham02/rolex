public class ScalarSubQuery extends AbstractSQLSubQuery{

    String aggName;
    private AbstractSQLSubQuery subQuery;

    public ScalarSubQuery(String aggName, AbstractSQLSubQuery subQuery) {
        super();
        this.aggName = aggName;
        this.subQuery = subQuery;
    }

    public AbstractSQLSubQuery getSubQuery() {
        return subQuery;
    }

    @Override
    public String toString() {
        return "SELECT VALUE" + aggName + "(`value`) FROM ( " + subQuery.toString() + " )[0]";
    }




}
