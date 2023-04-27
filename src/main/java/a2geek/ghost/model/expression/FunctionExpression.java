package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.scope.Function;

import java.util.*;
import java.util.stream.Collectors;

public class FunctionExpression implements Expression {
    private static final String LORES_LIBRARY = "lores";
    private static final String MISC_LIBRARY = "misc";
    private static final String MATH_LIBRARY = "math";
    private static final Map<String,Descriptor> FUNCS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static {
        Arrays.asList(
            new Descriptor("peek", null, DataType.INTEGER, DataType.INTEGER),
            new Descriptor("scrn", LORES_LIBRARY, DataType.INTEGER, DataType.INTEGER, DataType.INTEGER),
            new Descriptor("asc", null, DataType.INTEGER, DataType.STRING),
            new Descriptor("rnd", MATH_LIBRARY, DataType.INTEGER, DataType.INTEGER),
            new Descriptor("abs", MATH_LIBRARY, DataType.INTEGER, DataType.INTEGER),
            new Descriptor("sgn", MATH_LIBRARY, DataType.INTEGER, DataType.INTEGER),
            new Descriptor("pdl", MISC_LIBRARY, DataType.INTEGER, DataType.INTEGER)
        ).forEach(d -> {
            FUNCS.put(d.name(), d);
        });
    }
    public static boolean isIntrinsicFunction(String name) {
        return getDescriptor(name).map(Descriptor::library).isEmpty();
    }
    public static boolean isLibraryFunction(String name) {
        return getDescriptor(name).map(Descriptor::library).isPresent();
    }
    public static Optional<Descriptor> getDescriptor(String name) {
        return Optional.ofNullable(FUNCS.get(name));
    }

    private String name;
    private Function function;
    private List<Expression> parameters;
    private DataType returnType;

    public FunctionExpression(String name, List<Expression> parameters) {
        this.name = name;
        this.parameters = parameters;
        var descriptor = FUNCS.get(name);
        if (descriptor == null) {
            throw new RuntimeException("Unknown function: " + name);
        }
        if (this.parameters.size() != descriptor.parameterTypes.length) {
            throw new RuntimeException("Wrong number of arguments to: " + name);
        }
        for (int i = 0; i<this.parameters.size(); i++) {
            this.parameters.get(i).mustBe(descriptor.parameterTypes[i]);
        }
        this.returnType = descriptor.returnType;
    }
    public FunctionExpression(Function function, List<Expression> expr) {
        this.name = function.getName();
        this.function = function;
        this.parameters = expr;
        this.returnType = function.getType();
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
        if ("asc".equalsIgnoreCase(name) && parameters.size() == 1) {
            return parameters.get(0).isConstant();
        }
        return false;
    }

    @Override
    public Optional<Integer> asInteger() {
        if ("asc".equalsIgnoreCase(name) && parameters.size() == 1 &&
                parameters.get(0) instanceof StringConstant s) {
            return Optional.of(s.getValue().charAt(0)|0x80);
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
                throw new RuntimeException("fullName does not apply " + this);
            }
            return String.format("%s_%s", library(), name());
        }
    }
}
