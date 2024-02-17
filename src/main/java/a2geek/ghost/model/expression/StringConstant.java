package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;

import java.util.Objects;
import java.util.Optional;

public class StringConstant implements Expression {
    public static final StringConstant EMPTY = new StringConstant("");
    private final String value;

    public StringConstant(String value) {
        this.value = value;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public Optional<Integer> asInteger() {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Boolean> asBoolean() {
        var asint = asInteger();
        if (asint.isPresent()) {
            return Optional.of(asint.get() != 0);
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Optional.of("true".equalsIgnoreCase(value));
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> asString() {
        return Optional.of(value);
    }

    @Override
    public DataType getType() {
        return DataType.STRING;    // Always
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringConstant that = (StringConstant) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.format("\"%s\"", value);
    }
}
