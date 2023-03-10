package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

public class PokeStatement implements Statement {
    private Expression a;
    private Expression b;

    public PokeStatement(Expression a, Expression b) {
        this.a = a;
        this.b = b;
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

    @Override
    public String toString() {
        return String.format("POKE %s,%s", a, b);
    }
}
