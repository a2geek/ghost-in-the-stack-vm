package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;

public class BooleanConstant implements Expression {
    private boolean value;

    public BooleanConstant(boolean value) {
        this.value = value;
    }

    @Override
    public DataType getType() {
        return DataType.BOOLEAN;    // Always
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value ? "True" : "False";
    }
}
