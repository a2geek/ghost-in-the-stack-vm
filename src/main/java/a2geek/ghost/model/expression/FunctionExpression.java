package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.scope.Function;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class FunctionExpression implements Expression {
    private static final String INPUT_LIBRARY = "input";
    private static final String LORES_LIBRARY = "lores";
    private static final String MISC_LIBRARY = "misc";
    private static final String MATH_LIBRARY = "math";
    private static final String RUNTIME_LIBRARY = "runtime";
    private static final String STRING_LIBRARY = "string";
    public static final List<Descriptor> DESCRIPTORS = List.of(
        new Descriptor("peek", null, DataType.INTEGER, DataType.INTEGER),
        new Descriptor("peek", null, DataType.INTEGER, DataType.ADDRESS),
        new Descriptor("peek", null, DataType.INTEGER, DataType.STRING),
        new Descriptor("peekw", null, DataType.INTEGER, DataType.INTEGER),
        new Descriptor("peekw", null, DataType.INTEGER, DataType.ADDRESS),
        new Descriptor("peekw", null, DataType.INTEGER, DataType.STRING),
        new Descriptor("scrn", LORES_LIBRARY, DataType.INTEGER, DataType.INTEGER, DataType.INTEGER),
        new Descriptor("asc", STRING_LIBRARY, DataType.INTEGER, DataType.STRING),
        new Descriptor("rnd", MATH_LIBRARY, DataType.INTEGER, DataType.INTEGER),
        new Descriptor("abs", MATH_LIBRARY, DataType.INTEGER, DataType.INTEGER),
        new Descriptor("sgn", MATH_LIBRARY, DataType.INTEGER, DataType.INTEGER),
        new Descriptor("pdl", MISC_LIBRARY, DataType.INTEGER, DataType.INTEGER),
        new Descriptor("ipow", MATH_LIBRARY, DataType.INTEGER, DataType.INTEGER, DataType.INTEGER),
        new Descriptor("integer", INPUT_LIBRARY, DataType.INTEGER),
        new Descriptor("line_index", RUNTIME_LIBRARY, DataType.INTEGER, DataType.INTEGER, DataType.INTEGER),
        new Descriptor("len", STRING_LIBRARY, DataType.INTEGER, DataType.STRING),
        new Descriptor("strcmp", STRING_LIBRARY, DataType.INTEGER, DataType.STRING, DataType.STRING)
    );
    public static Optional<Descriptor> findDescriptor(String name, DataType... parameterTypes) {
        String message = null;
        for (var d : DESCRIPTORS) {
            if (d.name.equalsIgnoreCase(name)) {
                if (Arrays.equals(d.parameterTypes, parameterTypes)) {
                    return Optional.of(d);
                }
                if (d.parameterTypes.length != parameterTypes.length) {
                    message = String.format("wrong number of arguments to function '%s' expecting %d",
                            d.name(), d.parameterTypes.length);
                }
                else {
                    message = String.format("expecting parameters of %s but found %s instead",
                            DataType.asString(d.parameterTypes()),
                            DataType.asString(parameterTypes));
                }
            }
        }
        if (message != null) {
            throw new RuntimeException(message);
        }
        return Optional.empty();
    }
    public static Optional<Descriptor> findDescriptor(String name, List<Expression> parameters) {
        return findDescriptor(name, parameters.stream().map(Expression::getType).toArray(DataType[]::new));
    }

    public static boolean isIntrinsicFunction(String name) {
        return DESCRIPTORS.stream()
                .filter(d -> d.name().equalsIgnoreCase(name))
                .map(d -> d.library() == null)
                .findAny()
                .orElse(false);
    }
    public static boolean isLibraryFunction(String name) {
        return DESCRIPTORS.stream()
                .filter(d -> d.name().equalsIgnoreCase(name))
                .map(d -> d.library() != null)
                .findAny()
                .orElse(false);
    }

    private String name;
    private Function function;
    private List<Expression> parameters;
    private DataType returnType;

    public FunctionExpression(String name, List<Expression> parameters) {
        this.name = name;
        this.parameters = parameters;
        var descriptor = findDescriptor(name, parameters);  // also validates
        if (descriptor.isEmpty()) {
            throw new RuntimeException("Unknown function: " + name);
        }
        this.returnType = descriptor.get().returnType();
    }
    public FunctionExpression(Function function, List<Expression> expr) {
        this.name = function.getName();
        this.function = function;
        this.parameters = expr;
        this.returnType = function.getDataType();
    }

    @Override
    public DataType getType() {
        return returnType;
    }

    public String getName() {
        return name;
    }

    public Function getFunction() {
        return function;
    }

    public List<Expression> getParameters() {
        return parameters;
    }

    public void setParameters(List<Expression> parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean isConstant() {
        if (matches("asc") && parameters.size() == 1) {
            return parameters.get(0).isConstant();
        }
        else if (matches("sgn", "math_sgn") && parameters.size() == 1) {
            return parameters.get(0).isConstant();
        }
        return false;
    }
    boolean matches(String... values) {
        for (var value : values) {
            if (value.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    @Override
    public Optional<Integer> asInteger() {
        if (matches("asc") && parameters.size() == 1 &&
                parameters.get(0) instanceof StringConstant s) {
            return Optional.of(s.getValue().charAt(0)|0x80);
        }
        else if (matches("sgn", "math_sgn") && parameters.size() == 1 &&
                parameters.get(0) instanceof IntegerConstant i) {
            return Optional.of(Integer.signum(i.getValue()));
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionExpression that = (FunctionExpression) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(function, that.function) &&
                Objects.equals(parameters, that.parameters) &&
                returnType == that.returnType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, function, parameters, returnType);
    }

    @Override
    public String toString() {
        String args = this.parameters.stream().map(Expression::toString).collect(Collectors.joining(", "));
        return String.format("%s(%s)", name, args);
    }

    public record Descriptor(
        String name,
        String library,
        DataType returnType,
        DataType... parameterTypes
    ) {
        public String fullName() {
            if (library() == null) {
                return name();
            }
            return String.format("%s_%s", library(), name());
        }
    }
}
