public class BinaryOpJoinSubQuery extends AbstractSQLSubQuery{

   private AbstractSQLSubQuery leftSubQuery ;
   private AbstractSQLSubQuery rightSubQuery;

    String operator;



    public BinaryOpJoinSubQuery(AbstractSQLSubQuery leftSubQuery, String operator, AbstractSQLSubQuery rightSubQuery) {
        super();
        this.leftSubQuery = leftSubQuery;
        this.operator = operator;
        this.rightSubQuery = rightSubQuery;
    }


    public AbstractSQLSubQuery getLeftSubQuery() {
        return leftSubQuery;
    }

    public AbstractSQLSubQuery getRightSubQuery() {
        return rightSubQuery;
    }

    @Override
    public String toString() {
        return "SELECT l.timestamp, (l.`value` " + operator + " r.`value`) as `value` FROM ( " + leftSubQuery.toString() + " ) l "
                + "JOIN ( " + rightSubQuery.toString() + " ) r ON l.timestamp = r.timestamp";
    }



}
