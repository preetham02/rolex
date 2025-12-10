public class MaxSQLSubQuery extends AbstractSQLSubQuery{
    AbstractSQLSubQuery subQuery;
    String windowTime;


    public MaxSQLSubQuery(AbstractSQLSubQuery subQuery, String windowTime){
        this.subQuery = subQuery;
        this.windowTime = windowTime;
    }

    @Override
    public String toString() {
        return "SELECT timestamp, MAX(`value`) OVER (ORDER BY timestamp RANGE BETWEEN " + AverageSQLSubquery.parseDuration(windowTime) + " PRECEDING AND CURRENT ROW) AS `value` FROM (" + subQuery.toString() + ") sub";
    }

}
