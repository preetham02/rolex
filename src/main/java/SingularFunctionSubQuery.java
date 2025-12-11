public class SingularFunctionSubQuery extends AbstractSQLSubQuery{

    String functionName;
    AbstractSQLSubQuery innerSubquery;


    public SingularFunctionSubQuery(String functionName, AbstractSQLSubQuery innerSubquery) {
        super();
        this.functionName = functionName;
        this.innerSubquery = innerSubquery;
    }

    public AbstractSQLSubQuery getInnerSubquery() {
        return innerSubquery;
    }

    @Override
    public String toString() {
        return "SELECT timestamp, " + functionName + "(`value`) as `value` FROM ( " + innerSubquery.toString() + " ) sub";
    }

}
