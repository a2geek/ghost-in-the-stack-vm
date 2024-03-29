package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

public class ReturnStatement implements Statement {
    private Expression expr;

    public ReturnStatement() {
    }
    public ReturnStatement(Expression expr) {
        this.expr = expr;
    }

    public Expression getExpr() {
        return expr;
    }
    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    @Override
    public String toString() {
        return expr == null ? "RETURN" : String.format("RETURN %s", expr);
    }
}
