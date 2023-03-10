package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

public class VlinStatement implements Statement {
    private Expression a;
    private Expression b;
    private Expression x;

    public VlinStatement(Expression a, Expression b, Expression x) {
        this.a = a;
        this.b = b;
        this.x = x;
    }

    public Expression getA() {
        return a;
    }

    public void setA(Expression a) {
        this.a = a;
    }

    public Expression getB() {
        return b;
    }

    public void setB(Expression b) {
        this.b = b;
    }

    public Expression getX() {
        return x;
    }

    public void setX(Expression x) {
        this.x = x;
    }

    @Override
    public String toString() {
        return String.format("VLIN %s,%s AT %s", a, b, x);
    }
}
