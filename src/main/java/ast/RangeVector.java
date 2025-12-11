package ast;

public class RangeVector implements Node {
    private final InstantVector vector;
    private final String duration;

    public RangeVector(InstantVector vector, String duration) {
        this.vector = vector;
        this.duration = duration;
    }

    public InstantVector getVector() { return vector; }
    public String getDuration() { return duration; }

    @Override
    public String toString() {
        return vector.toString() + "[" + duration + "]";
    }
}

