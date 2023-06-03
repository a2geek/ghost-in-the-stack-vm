package a2geek.ghost.target.ghost;

import a2geek.ghost.model.DataType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record ConstantValue(DataType dataType, int size, String string, List<Integer> integerArray) {
    public static ConstantValue with(String string) {
        Objects.requireNonNull(string);
        return new ConstantValue(DataType.STRING, string.length()+1, string, null);
    }
    public static ConstantValue with(List<Integer> integerArray) {
        Objects.requireNonNull(integerArray);
        return new ConstantValue(DataType.INTEGER, integerArray.size()*2 + 2, null, integerArray);
    }

    @Override
    public String toString() {
        return switch (dataType) {
            case STRING -> String.format("\"%s\"", string);
            case INTEGER -> integerArray.stream().map(Object::toString).collect(Collectors.joining(","));
            default -> {
                throw new RuntimeException("unsupported constant directive constant: " + dataType);
            }
        };
    }
}
