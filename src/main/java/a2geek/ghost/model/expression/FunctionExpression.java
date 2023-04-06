package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.scope.Function;

import java.util.*;
import java.util.stream.Collectors;

public class FunctionExpression implements Expression {
    private static final Map<String,Descriptor> FUNCS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static {
        FUNCS.putAll(Map.of(
            "peek", new Descriptor(DataType.INTEGER, DataType.INTEGER),
            "scrn", new Descriptor(DataType.INTEGER, DataType.INTEGER, DataType.INTEGER),
            "asc", new Descriptor(DataType.INTEGER, DataType.STRING)
        ));
    }
    public static boolean isIntrinsicFunction(String name) {
        return FUNCS.containsKey(name);
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
            DataType returnType,
            DataType... parameterTypes
    ) {

    }
}
