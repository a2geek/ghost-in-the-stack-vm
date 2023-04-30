package a2geek.ghost.model.statement;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

public class PokeStatement implements Statement {
    private String op;
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
        a.mustBe(DataType.INTEGER);
        this.a = a;
    }

    public Expression getB() {
        return b;
    }

    public void setB(Expression b) {
        b.mustBe(DataType.INTEGER);
        this.b = b;
    }

    @Override
    public String toString() {
        return String.format("%s %s,%s", op.toUpperCase(), a, b);
    }
}
