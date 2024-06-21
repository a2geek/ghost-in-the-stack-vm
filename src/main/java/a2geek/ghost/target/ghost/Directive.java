package a2geek.ghost.target.ghost;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record Directive(Type type, int size, List<?> array) {
    public static Directive with(String string) {
        Objects.requireNonNull(string);
        return new Directive(Type.STRING_VALUE, string.length()+2, List.of(string));
    }
    public static Directive with(List<Integer> integerArray) {
        Objects.requireNonNull(integerArray);
        return new Directive(Type.INTEGER_ARRAY, integerArray.size()*2 + 2, integerArray);
    }
    public static Directive withLabels(List<String> labelArray) {
        Objects.requireNonNull(labelArray);
        return new Directive(Type.LABEL_ARRAY_LESS_1, labelArray.size()*2 + 2, labelArray);
    }

    byte[] generate(Map<String,Integer> addrs) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        switch (type()) {
            case STRING_VALUE -> {
                String s = array.getFirst().toString();
                bytes.write(s.length());
                for (byte ch : s.getBytes()) {
                    bytes.write(ch|0x80);
                }
                bytes.write(0);
            }
            case INTEGER_ARRAY -> {
                // BASIC arrays ignore the zeroth element, so 1,2,3,4 is length of 3
                List<Integer> integerArray = array.stream().map(Integer.class::cast).toList();
                int size = integerArray.size()-1;
                bytes.write(size & 0xff);
                bytes.write(size >> 8 & 0xff);
                for (int value : integerArray) {
                    bytes.write(value & 0xff);
                    bytes.write(value >> 8 & 0xff);
                }
            }
            case LABEL_ARRAY_LESS_1 -> {
                // BASIC arrays ignore the zeroth element, so 1,2,3,4 is length of 3
                List<String> stringArray = array.stream().map(String.class::cast).toList();
                int size = stringArray.size()-1;
                bytes.write(size & 0xff);
                bytes.write(size >> 8 & 0xff);
                for (String label : stringArray) {
                    Integer value = addrs.get(label);
                    if (value == null) {
                        throw new RuntimeException("label not found: " + label);
                    }
                    value = value - 1;
                    bytes.write(value & 0xff);
                    bytes.write(value >> 8 & 0xff);
                }
            }
            default -> {
                throw new RuntimeException("Unsupported directive: " + this);
            }
        }
        return bytes.toByteArray();
    }

    public String format() {
        var detail = switch (type) {
            case STRING_VALUE -> String.format("\"%s\"", array.getFirst());
            case INTEGER_ARRAY, LABEL_ARRAY_LESS_1 -> array.stream().map(Object::toString).collect(Collectors.joining(","));
        };
        return String.format("%s %s", type, detail);
    }

    public enum Type {
        STRING_VALUE,
        INTEGER_ARRAY,
        LABEL_ARRAY_LESS_1
    }
}
