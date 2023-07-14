package a2geek.ghost.model.basic.expression;

import a2geek.ghost.model.basic.DataType;
import a2geek.ghost.model.basic.Expression;

import java.util.Objects;
import java.util.Optional;

public class ParenthesisExpression implements Expression {
    private Expression expr;

    public ParenthesisExpression(Expression expr) {
        this.expr = expr;
    }

    @Override
    public boolean isConstant() {
        return expr.isConstant();
    }

    @Override
    public Optional<Boolean> asBoolean() {
        return expr.asBoolean();
    }

    @Override
    public Optional<Integer> asInteger() {
        return expr.asInteger();
    }

    @Override
    public Optional<String> asString() {
        return expr.asString();
    }

    @Override
    public DataType getType() {
        return expr.getType();
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
        if (o instanceof ParenthesisExpression that) {
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
        return String.format("( %s )", expr);
    }
}
