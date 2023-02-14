package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.Visitor;

public class ColorStatement implements Statement {
    private Expression expr;

    public ColorStatement(Expression expr) {
        this.expr = expr;
    }

    public Expression getExpr() {
        return expr;
    }

    @Override
    public void accept(Visitor visitor) {
        expr.accept(visitor);
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("COLOR=%s", expr);
    }
}
