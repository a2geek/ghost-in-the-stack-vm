package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;

import java.util.Objects;
import java.util.Optional;

public class ByteConstant implements Expression {
    private final int value;

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public Optional<Integer> asInteger() {
        return Optional.of(value);
    }

    @Override
    public Optional<Boolean> asBoolean() {
        return Optional.of(value != 0);
    }

    @Override
    public Optional<String> asString() {
        return Optional.of(Integer.toString(value));
    }

    @Override
    public DataType getType() {
        return DataType.BYTE;    // Always
    }

    public int getValue() {
        return value;
    }

    public ByteConstant(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ByteConstant that) {
            return value == that.value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
