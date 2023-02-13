package a2geek.ghost.model;

public class BinaryExpression implements Expression {
    private Expression l;
    private Expression r;
    private String op;

    public BinaryExpression(Expression l, Expression r, String op) {
        this.l = l;
        this.r = r;
        this.op = op;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", l, op, r);
    }
}
