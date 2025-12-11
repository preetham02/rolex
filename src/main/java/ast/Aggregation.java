package ast;

public class Aggregation extends Scalar {
    private final String functionName;
    private final InstantVector arg;

    public Aggregation(String functionName, InstantVector arg) {
        this.functionName = functionName;
        this.arg = arg;
    }

    public String getFunctionName() { return functionName; }
    public InstantVector getArg() { return arg; }

    @Override
    public String toString() {
        return functionName + "(" + arg + ")";
    }
}

