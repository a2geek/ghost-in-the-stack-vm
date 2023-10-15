package a2geek.ghost.model;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum DataType {
    INTEGER,
    STRING,
    BOOLEAN,
    /** ADDRESS is used by the ON GOTO/GOSUB constructs for an array of code addresses. */
    ADDRESS;

    public static String asString(DataType... dataTypes) {
        return Arrays.stream(dataTypes).map(DataType::toString).collect(Collectors.joining(", "));
    }
}
