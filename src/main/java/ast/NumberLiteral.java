package ast;

public class NumberLiteral extends Scalar {
    private final double value;

    public NumberLiteral(double value) {
        this.value = value;
    }

    public double getValue() { return value; }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

