package ast;

public class BinaryOp extends InstantVector {
    private final InstantVector left;
    private final String operator;
    private final Node right; // Can be InstantVector or Scalar

    public BinaryOp(InstantVector left, String operator, Node right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public InstantVector getLeft() { return left; }
    public String getOperator() { return operator; }
    public Node getRight() { return right; }

    @Override
    public String toString() {
        return "(" + left + " " + operator + " " + right + ")";
    }
}

