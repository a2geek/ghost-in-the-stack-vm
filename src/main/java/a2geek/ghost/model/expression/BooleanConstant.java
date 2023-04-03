package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;

import java.util.Optional;

public class BooleanConstant implements Expression {
    private boolean value;

    public BooleanConstant(boolean value) {
        this.value = value;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public Optional<Boolean> asBoolean() {
        return Optional.of(value);
    }

    @Override
    public Optional<Integer> asInteger() {
        return Optional.of(value ? 1 : 0);
    }

    @Override
    public Optional<String> asString() {
        return Optional.of(value ? "True" : "False");
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
