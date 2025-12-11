public class LiteralSubquery extends  AbstractSQLSubQuery {

    String value;

    public LiteralSubquery(String value) {
        super();
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

}
