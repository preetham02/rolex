package ast;

import java.util.List;

public class AggBy implements GroupInstantVector {
    private final String functionName; // sum, count, avg
    private final List<String> columns;
    private final GroupInstantVector vector;

    public AggBy(String functionName, List<String> columns, GroupInstantVector vector) {
        this.functionName = functionName;
        this.columns = columns;
        this.vector = vector;
    }

    public String getFunctionName() { return functionName; }
    public List<String> getColumns() { return columns; }
    public GroupInstantVector getVector() { return vector; }

    @Override
    public String toString() {
        return functionName + "_by(" + String.join(", ", columns) + ")(" + vector + ")";
    }
}

