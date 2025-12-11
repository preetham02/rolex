package ast;

public class GroupRangeVector implements Node {
    private final GroupInstantVector vector;
    private final String duration;

    public GroupRangeVector(GroupInstantVector vector, String duration) {
        this.vector = vector;
        this.duration = duration;
    }

    public GroupInstantVector getVector() { return vector; }
    public String getDuration() { return duration; }

    @Override
    public String toString() {
        return vector + "[" + duration + "]";
    }
}

