package a2geek.ghost.model.expression;

import a2geek.ghost.model.Expression;

import java.util.Objects;

public class BinaryExpression implements Expression {
    private Type type;
    private Expression l;
    private Expression r;
    private String op;

    public Expression getL() {
        return l;
    }

    public void setL(Expression l) {
        this.l = l;
    }

    public Expression getR() {
        return r;
    }

    public void setR(Expression r) {
        this.r = r;
    }

    public String getOp() {
        return op;
    }

    @Override
    public Type getType() {
        return type;
    }

    public BinaryExpression(Expression l, Expression r, String op) {
        this.l = l;
        this.r = r;
        this.op = op.toLowerCase();
        this.type = l.getType();
        if (l.getType() != l.getType()) {
            String message = String.format("No type coercion at this time. Expressions must be same type. [%s]",
                    toString());
            throw new RuntimeException(message);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof BinaryExpression that) {
            return Objects.equals(l, that.l) && Objects.equals(r, that.r) && Objects.equals(op, that.op);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(l, r, op);
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", l, op, r);
    }
}
