package a2geek.ghost.model.basic;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public interface Expression {
    public DataType getType();

    public default boolean isType(DataType...types) {
        for (var type : types) {
            if (type == getType()) return true;
        }
        return false;
    }

    public default void mustBe(DataType...types) {
        if (isType(types)) return;

        String message = String.format("Must be type %s but is %s",
                Arrays.stream(types).map(DataType::toString).collect(Collectors.joining(",")),
                getType());
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
