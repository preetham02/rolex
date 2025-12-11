import java.util.ArrayList;
import java.util.List;

public class CollectionSQLSubQuery extends AbstractSQLSubQuery {

    private final String CollectionName;

    private List<SelectSQLSubQuery.WhereCondition> rangeFilters = new ArrayList<>();


  public CollectionSQLSubQuery(String collectionName) {
        super();
        this.CollectionName = collectionName;

    }

    public void setRangeFilters(List<SelectSQLSubQuery.WhereCondition> rangeFilters) {
        this.rangeFilters = rangeFilters;
    }

    @Override
    public String toString() {
        return "SELECT a.* FROM " + CollectionName  + " a"+
                (rangeFilters.isEmpty() ? "" :
                " WHERE " + String.join(" AND ", rangeFilters.stream().map(SelectSQLSubQuery.WhereCondition::toString).toArray(String[]::new))
                );


    }
}
