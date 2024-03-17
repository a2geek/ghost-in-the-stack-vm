package a2geek.ghost.command.util;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.expression.BooleanConstant;
import a2geek.ghost.model.expression.IntegerConstant;
import a2geek.ghost.model.expression.StringConstant;
import picocli.CommandLine.ITypeConverter;

public class ExpressionConverter implements ITypeConverter<Expression> {
    private static IntegerTypeConverter integerConverter = new IntegerTypeConverter();
    @Override
    public Expression convert(String value) throws Exception {
        try {
            Integer ival = integerConverter.convert(value);
            if (ival != null) {
                return new IntegerConstant(ival);
            }
        } catch (NumberFormatException ignored) {
            // ignored
        }
        if ("true".equalsIgnoreCase(value)) {
            return new BooleanConstant(true);
        }
        if ("false".equalsIgnoreCase(value)) {
            return new BooleanConstant(false);
        }
        return new StringConstant(value);
    }
}
