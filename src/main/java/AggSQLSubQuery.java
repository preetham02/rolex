public class AggSQLSubQuery extends AbstractSQLSubQuery {

    AbstractSQLSubQuery subQuery;
    Long windowSizeMillis;
    String aggName;


    public AggSQLSubQuery(AbstractSQLSubQuery subQuery, Long windowSizeMillis, String aggName) {
        super();
        this.subQuery = subQuery;
        this.windowSizeMillis = windowSizeMillis;
        this.aggName = aggName;
    }

    public AbstractSQLSubQuery getSubQuery() {
        return subQuery;
    }

    @Override
    public String toString() {
        return "SELECT timestamp, "+ aggName  +"(`value`) OVER (ORDER BY timestamp RANGE BETWEEN " + windowSizeMillis + " PRECEDING AND CURRENT ROW) AS `value` FROM (" + subQuery.toString() + ") sub";
    }


}
