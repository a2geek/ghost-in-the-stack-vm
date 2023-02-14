package a2geek.ghost.model.expression;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Visitor;

public class IntegerConstant implements Expression {
    private int value;

    public int getValue() {
        return value;
    }

    public IntegerConstant(int value) {
        this.value = value;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
