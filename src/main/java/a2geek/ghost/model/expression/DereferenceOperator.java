package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;

import java.util.Objects;

public class DereferenceOperator implements Expression {
    private final DataType type;
    private Expression expr;

    public DereferenceOperator(Expression expr, DataType type) {
        this.expr = expr.checkAndCoerce(DataType.ADDRESS);
        this.type = type;
    }

    public Expression getExpr() {
        return expr;
    }
    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    @Override
    public DataType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DereferenceOperator that = (DereferenceOperator) o;
        return type == that.type && Objects.equals(expr, that.expr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, expr);
    }

    @Override
    public String toString() {
        return String.format("*(%s)", expr);
    }
}
