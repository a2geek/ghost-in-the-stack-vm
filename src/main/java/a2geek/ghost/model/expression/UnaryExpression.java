package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;

import java.util.Objects;

public class UnaryExpression implements Expression {
    private DataType type;
    private Expression expr;
    private String op;

    public UnaryExpression(String op, Expression expr) {
        this.expr = expr;
        this.op = op;
        this.type = DataType.INTEGER;
        if (!expr.isType(type)) {
            throw new RuntimeException("Unary operation must be of type " + type);
        }
    }

    @Override
    public DataType getType() {
        return type;
    }

    public Expression getExpr() {
        return expr;
    }

    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    public String getOp() {
        return op;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnaryExpression that = (UnaryExpression) o;
        return type == that.type && Objects.equals(expr, that.expr) && Objects.equals(op, that.op);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, expr, op);
    }

    @Override
    public String toString() {
        return String.format("- %s", expr);
    }
}
