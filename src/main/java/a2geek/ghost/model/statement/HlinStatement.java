package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

public class HlinStatement implements Statement {
    private Expression a;
    private Expression b;
    private Expression y;

    public HlinStatement(Expression a, Expression b, Expression y) {
        setA(a);
        setB(b);
        setY(y);
    }

    public Expression getA() {
        return a;
    }

    public void setA(Expression a) {
        a.mustBe(Expression.Type.INTEGER);
        this.a = a;
    }

    public Expression getB() {
        return b;
    }

    public void setB(Expression b) {
        b.mustBe(Expression.Type.INTEGER);
        this.b = b;
    }

    public Expression getY() {
        return y;
    }

    public void setY(Expression y) {
        y.mustBe(Expression.Type.INTEGER);
        this.y = y;
    }

    @Override
    public String toString() {
        return String.format("HLIN %s,%s AT %s", a, b, y);
    }
}
