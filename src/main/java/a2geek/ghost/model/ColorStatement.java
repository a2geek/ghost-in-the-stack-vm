package a2geek.ghost.model;

public class ColorStatement implements Statement {
    private Expression expr;

    public ColorStatement(Expression expr) {
        this.expr = expr;
    }

    @Override
    public String toString() {
        return String.format("COLOR=%s", expr);
    }
}
