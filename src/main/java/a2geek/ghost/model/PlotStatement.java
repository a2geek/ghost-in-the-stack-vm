package a2geek.ghost.model;

public class PlotStatement implements Statement {
    private Expression x;
    private Expression y;

    public PlotStatement(Expression x, Expression y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return String.format("PLOT %s,%s", x, y);
    }
}
