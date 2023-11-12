package a2geek.ghost.command.util;

import picocli.CommandLine.ITypeConverter;

public class IntegerTypeConverter implements ITypeConverter<Integer> {
    @Override
    public Integer convert(String value) {
        try {
            if (value == null) {
                return null;
            } else if (value.startsWith("$")) {
                return Integer.valueOf(value.substring(1), 16);
            } else if (value.startsWith("0x") || value.startsWith("0X")) {
                return Integer.valueOf(value.substring(2), 16);
            } else {
                return Integer.valueOf(value);
            }
        } catch (NumberFormatException ex) {
            throw new NumberFormatException(ex.getMessage());
        }
    }
}
