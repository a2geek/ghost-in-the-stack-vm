package a2geek.ghost.model.expression;

import a2geek.ghost.model.Expression;

public class IntegerConstant implements Expression {
    private int value;

    public int getValue() {
        return value;
    }

    public IntegerConstant(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
