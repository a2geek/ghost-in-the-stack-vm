package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.Visitor;

public class AssignmentStatement implements Statement {
    private String id;
    private Expression expr;

    public AssignmentStatement(String id, Expression expr) {
        this.id = id;
        this.expr = expr;
    }

    public String getId() {
        return id;
    }

    public Expression getExpr() {
        return expr;
    }

    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    @Override
    public void accept(Visitor visitor) {
        expr.accept(visitor);
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("%s = %s", id, expr);
    }
}
