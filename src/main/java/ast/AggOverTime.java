package ast;

public class AggOverTime extends InstantVector {
    private final String functionName;
    private final RangeVector arg;

    public AggOverTime(String functionName, RangeVector arg) {
        if (functionName.toLowerCase().endsWith("_over_time")) {
            this.functionName = functionName.substring(0, functionName.length() - 10);
        } else {
            this.functionName = functionName;
        }
        this.arg = arg;
    }

    public String getFunctionName() { return functionName; }
    public RangeVector getArg() { return arg; }

    @Override
    public String toString() {
        return functionName + "_over_time(" + arg + ")";
    }
}

