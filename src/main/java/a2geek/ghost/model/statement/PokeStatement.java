package a2geek.ghost.model.statement;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

public class PokeStatement implements Statement {
    private final String op;
    private Expression a;
    private Expression b;

    public PokeStatement(String op, Expression a, Expression b) {
        this.op = op;
        setA(a);
        setB(b);
    }

    public String getOp() {
        return op;
    }

    public Expression getA() {
        return a;
    }

    public void setA(Expression a) {
        this.a = a.checkAndCoerce(DataType.ADDRESS);
    }

    public Expression getB() {
        return b;
    }

    public void setB(Expression b) {
        this.b = b.checkAndCoerce(DataType.ADDRESS);
    }

    @Override
    public String toString() {
        return String.format("%s %s,%s", op.toUpperCase(), a, b);
    }
}
