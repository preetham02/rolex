public class DummySQLSubquery extends AbstractSQLSubQuery{

    public static DummySQLSubquery INSTANCE = new DummySQLSubquery();

    private DummySQLSubquery() {
        super();
    }

    @Override
    public String toString() {
        return "SELECT DUMMY_SUBQUERY";
    }
}
