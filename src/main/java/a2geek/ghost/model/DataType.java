package a2geek.ghost.model;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum DataType {
    INTEGER(2),
    STRING(2),
    BOOLEAN(2),
    /** ADDRESS is used by the ON GOTO/GOSUB constructs for an array of code addresses. */
    ADDRESS(2);

    private final int sizeof;

    private DataType(int sizeof) {
        this.sizeof = sizeof;
    }

    public int sizeof() {
        return sizeof;
    }

    public static String asString(DataType... dataTypes) {
        return Arrays.stream(dataTypes).map(DataType::toString).collect(Collectors.joining(", "));
    }
}
