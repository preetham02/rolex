package ast;

public class LabelFilter {
    private final String key;
    private final String operator; // "=" or "!=" (extendable)
    private final String value;    // unquoted

    public LabelFilter(String key, String operator, String value) {
        this.key = key;
        this.operator = operator;
        this.value = value;
    }

    public String getKey() { return key; }
    public String getOperator() { return operator; }
    public String getValue() { return value; }

    @Override
    public String toString() {
        return key + operator + "'" + value + "'";
    }
}


