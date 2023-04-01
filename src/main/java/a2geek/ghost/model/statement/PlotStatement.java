package a2geek.ghost.model.statement;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

public class PlotStatement implements Statement {
    private Expression x;
    private Expression y;

    public PlotStatement(Expression x, Expression y) {
        setX(x);
        setY(y);
    }

    public Expression getX() {
        return x;
    }

    public void setX(Expression x) {
        x.mustBe(DataType.INTEGER);
        this.x = x;
    }

    public Expression getY() {
        return y;
    }

    public void setY(Expression y) {
        y.mustBe(DataType.INTEGER);
        this.y = y;
    }

    @Override
    public String toString() {
        return String.format("PLOT %s,%s", x, y);
    }
}
