package a2geek.ghost.model.expression;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Visitor;

public class BinaryExpression implements Expression {
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

    public BinaryExpression(Expression l, Expression r, String op) {
        this.l = l;
        this.r = r;
        this.op = op;
    }

    @Override
    public void accept(Visitor visitor) {
        l.accept(visitor);
        r.accept(visitor);
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", l, op, r);
    }
}
