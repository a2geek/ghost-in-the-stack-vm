package a2geek.ghost.model;

import java.util.Optional;

public interface Expression {
    public DataType getType();

    public default boolean isType(DataType...types) {
        for (var type : types) {
            if (type == getType()) return true;
        }
        return false;
    }

    public default Expression checkAndCoerce(DataType target) {
        switch (target) {
            case ADDRESS -> {
                if (isType(DataType.ADDRESS, DataType.STRING, DataType.INTEGER)) {
                    return this;
                }
            }
            case INTEGER -> {
                if (isType(DataType.INTEGER, DataType.ADDRESS, DataType.BOOLEAN)) {
                    return this;
                }
            }
            case BOOLEAN -> {
                if (isType(DataType.BOOLEAN, DataType.INTEGER)) {
                    return this;
                }
            }
            case STRING -> {
                if (isType(DataType.STRING, DataType.ADDRESS)) {
                    return this;
                }
            }
        }
        String message = String.format("unable to convert expression '%s' to %s", this, target);
        throw new RuntimeException(message);
    }

    public default boolean isConstant() {
        return false;
    }

    public default Optional<Integer> asInteger() {
        return Optional.empty();
    }
    public default Optional<Boolean> asBoolean() {
        return Optional.empty();
    }
    public default Optional<String> asString() {
        return Optional.empty();
    }
}
