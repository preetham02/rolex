public class ScalarNode implements PromQLNode {
    private final double value;

    public ScalarNode(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

