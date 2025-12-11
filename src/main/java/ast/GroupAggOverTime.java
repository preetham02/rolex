package ast;

public class GroupAggOverTime implements GroupInstantVector {
    private final String functionName;
    private final GroupRangeVector arg;

    public GroupAggOverTime(String functionName, GroupRangeVector arg) {
        if (functionName.toLowerCase().endsWith("_over_time")) {
            this.functionName = functionName.substring(0, functionName.length() - 10);
        } else {
            this.functionName = functionName;
        }
        this.arg = arg;
    }

    public String getFunctionName() { return functionName; }
    public GroupRangeVector getArg() { return arg; }

    @Override
    public String toString() {
        return functionName + "_over_time(" + arg + ")";
    }
}

