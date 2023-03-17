package a2geek.ghost.model.expression;

import a2geek.ghost.model.Expression;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class FunctionExpression implements Expression {
    private static final Map<String,Descriptor> FUNCS = Map.of(
        "peek", new Descriptor(Type.INTEGER, Type.INTEGER),
        "scrn", new Descriptor(Type.INTEGER,Type.INTEGER, Type.INTEGER),
        "asc", new Descriptor(Type.INTEGER, Type.STRING)

    );

    private Descriptor descriptor;
    private String name;
    private Expression[] expr;

    public FunctionExpression(String name, Expression ...expr) {
        this.name = name;
        this.expr = expr;
        this.descriptor = FUNCS.get(name);
        if (this.descriptor == null) {
            throw new RuntimeException("Unknown function: " + name);
        };
        if (this.expr.length != this.descriptor.parameterTypes.length) {
            throw new RuntimeException("Wrong number of arguments to: " + name);
        }
        for (int i=0; i<this.expr.length; i++) {
            this.expr[i].mustBe(this.descriptor.parameterTypes[i]);
        }
    }

    @Override
    public Type getType() {
        return descriptor.returnType;
    }

    public String getName() {
        return name;
    }

    public Expression[] getExpr() {
        return expr;
    }

    public void setExpr(Expression[] expr) {
        this.expr = expr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof FunctionExpression that) {
            return Objects.equals(name, that.name) && Objects.equals(expr, that.expr);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, expr);
    }

    @Override
    public String toString() {
        String args = Arrays.asList(expr).stream().map(Expression::toString).collect(Collectors.joining(", "));
        return String.format("%s(%s)", name, args);
    }

    public record Descriptor(
            Type returnType,
            Type... parameterTypes
    ) {

    }
}
