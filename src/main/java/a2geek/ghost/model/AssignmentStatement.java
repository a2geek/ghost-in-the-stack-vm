package a2geek.ghost.model;

public class AssignmentStatement implements Statement {
    private String id;
    private Expression expr;

    public AssignmentStatement(String id, Expression expr) {
        this.id = id;
        this.expr = expr;
    }

    @Override
    public String toString() {
        return String.format("%s = %s", id, expr);
    }
}
