package a2geek.ghost.model.expression;

import a2geek.ghost.model.Expression;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class FunctionExpression implements Expression {
    private static final Map<String,Integer> FUNCS = Map.of(
        "peek", 1,
        "scrn", 2
    );

    private String name;
    private Expression[] expr;

    public FunctionExpression(String name, Expression ...expr) {
        if (!FUNCS.containsKey(name)) {
            throw new RuntimeException("Unknown function: " + name);
        };
        if (expr.length != FUNCS.get(name)) {
            throw new RuntimeException("Wrong number of arguments to: " + name);
        }

        this.name = name;
        this.expr = expr;
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
}
