package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.scope.Function;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class FunctionExpression implements Expression {
    private static final String MATH_LIBRARY = "math";
    private static final String STRINGS_LIBRARY = "strings";
    public static final List<Descriptor> DESCRIPTORS = List.of(
        new Descriptor("peek", null, DataType.INTEGER, DataType.ADDRESS),
        new Descriptor("peekw", null, DataType.INTEGER, DataType.ADDRESS),
        new Descriptor("alloc", null, DataType.ADDRESS, DataType.INTEGER),
        new Descriptor("asc", STRINGS_LIBRARY, DataType.INTEGER, DataType.STRING),
        new Descriptor("sgn", MATH_LIBRARY, DataType.INTEGER, DataType.INTEGER)
    );
    public static Optional<Descriptor> findDescriptor(String name, List<Expression> parameters) {
        String message = null;
        for (var d : DESCRIPTORS) {
            if (d.name.equalsIgnoreCase(name)) {
                if (d.parameterTypes.length != parameters.size()) {
                    message = String.format("wrong number of arguments to function '%s' expecting %d",
                        d.name(), d.parameterTypes.length);
                }
                try {
                    for (int i = 0; i < d.parameterTypes.length; i++) {
                        parameters.get(i).checkAndCoerce(d.parameterTypes[i]);
                    }
                    return Optional.of(d);
                }
                catch (RuntimeException ex) {
                    message = String.format("expecting parameters of %s but found %s instead",
                        DataType.asString(d.parameterTypes()),
                        parameters.stream().map(Expression::getType)
                            .map(DataType::toString)
                            .collect(Collectors.joining(",")));
                }
            }
        }
        if (message != null) {
            throw new RuntimeException(message);
        }
        return Optional.empty();

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
        this.name = function.getFullPathName();
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
        if (matches("asc", "strings.asc") && parameters.size() == 1) {
            return parameters.get(0).isConstant();
        }
        else if (matches("sgn", "math.sgn") && parameters.size() == 1) {
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
        if (matches("asc", "strings.asc") && parameters.size() == 1 &&
                parameters.get(0) instanceof StringConstant s) {
            return Optional.of(s.getValue().charAt(0)|0x80);
        }
        else if (matches("sgn", "math.sgn") && parameters.size() == 1 &&
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
            if (library == null) {
                return name;
            }
            return String.format("%s.%s", library, name);
        }
    }
}
