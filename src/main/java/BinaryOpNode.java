public class BinaryOpNode implements PromQLNode {
    private final PromQLNode left;
    private final PromQLNode right;
    private final String operator; // e.g., "+"

    public BinaryOpNode(PromQLNode left, String operator, PromQLNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public PromQLNode getLeft() {
        return left;
    }

    public PromQLNode getRight() {
        return right;
    }

    public String getOperator() {
        return operator;
    }

    @Override
    public String toString() {
        return "(" + left.toString() + " " + operator + " " + right.toString() + ")";
    }
}

