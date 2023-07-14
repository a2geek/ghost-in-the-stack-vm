package a2geek.ghost.model.basic.statement;

import a2geek.ghost.model.basic.Expression;
import a2geek.ghost.model.basic.Statement;

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
