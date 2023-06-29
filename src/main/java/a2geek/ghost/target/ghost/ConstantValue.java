package a2geek.ghost.target.ghost;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record ConstantValue(ConstantType constantType, int size, String string, List<Integer> integerArray, List<String> stringArray) {
    public static ConstantValue with(String string) {
        Objects.requireNonNull(string);
        return new ConstantValue(ConstantType.STRING_VALUE, string.length()+1, string, null, null);
    }
    public static ConstantValue with(List<Integer> integerArray) {
        Objects.requireNonNull(integerArray);
        return new ConstantValue(ConstantType.INTEGER_ARRAY, integerArray.size()*2 + 2, null, integerArray, null);
    }
    public static ConstantValue withLabels(List<String> labelArray) {
        Objects.requireNonNull(labelArray);
        return new ConstantValue(ConstantType.LABEL_ARRAY_LESS_1, labelArray.size()*2 + 2, null, null, labelArray);
    }

    @Override
    public String toString() {
        return switch (constantType) {
            case STRING_VALUE -> String.format("\"%s\"", string);
            case INTEGER_ARRAY -> integerArray.stream().map(Object::toString).collect(Collectors.joining(","));
            case LABEL_ARRAY_LESS_1 -> String.join(",", stringArray);
        };
    }
}
