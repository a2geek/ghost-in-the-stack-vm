package a2geek.ghost.model.basic.expression;

import a2geek.ghost.model.basic.DataType;
import a2geek.ghost.model.basic.Expression;

import java.util.Objects;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BooleanConstant that = (BooleanConstant) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value ? "True" : "False";
    }
}
