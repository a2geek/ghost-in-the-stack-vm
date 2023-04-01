package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;

public class StringConstant implements Expression {
    private String value;

    public StringConstant(String value) {
        this.value = value;
    }

    @Override
    public DataType getType() {
        return DataType.STRING;    // Always
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("\"%s\"", value);
    }
}
