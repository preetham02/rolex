public class CollectionSQLSubQuery extends AbstractSQLSubQuery {

    private final String CollectionName;


  public   CollectionSQLSubQuery(String collectionName) {
        super();
        this.CollectionName = collectionName;

    }


    @Override
    public String toString() {
        return "SELECT a.* FROM " + CollectionName  + " a";
    }
}
