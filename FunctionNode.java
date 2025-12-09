import java.util.List;

public class FunctionNode implements PromQLNode {
    private final String functionName;
    private final List<PromQLNode> args;

    public FunctionNode(String functionName, List<PromQLNode> args) {
        this.functionName = functionName;
        this.args = args;
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<PromQLNode> getArgs() {
        return args;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(functionName).append("(");
        for (int i = 0; i < args.size(); i++) {
            sb.append(args.get(i).toString());
            if (i < args.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}

