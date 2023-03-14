package a2geek.ghost.model.expression;

import a2geek.ghost.model.Expression;

import java.util.Objects;

public class NegateExpression implements Expression {
    private Expression expr;

    public NegateExpression(Expression expr) {
        this.expr = expr;
    }

    public Expression getExpr() {
        return expr;
    }

    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof NegateExpression that) {
            return Objects.equals(expr, that.expr);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(expr);
    }

    @Override
    public String toString() {
        return String.format("- %s", expr);
    }
}
