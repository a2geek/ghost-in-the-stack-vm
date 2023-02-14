package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.Visitor;

public class PlotStatement implements Statement {
    private Expression x;
    private Expression y;

    public PlotStatement(Expression x, Expression y) {
        this.x = x;
        this.y = y;
    }

    public Expression getX() {
        return x;
    }

    public Expression getY() {
        return y;
    }

    @Override
    public void accept(Visitor visitor) {
        x.accept(visitor);
        y.accept(visitor);
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("PLOT %s,%s", x, y);
    }
}
