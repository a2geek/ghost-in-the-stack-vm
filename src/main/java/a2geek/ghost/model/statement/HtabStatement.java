package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

public class HtabStatement implements Statement {
    private Expression expr;

    public HtabStatement(Expression expr) {
        setExpr(expr);
    }

    public Expression getExpr() {
        return expr;
    }

    public void setExpr(Expression expr) {
        expr.mustBe(Expression.Type.INTEGER);
        this.expr = expr;
    }

    @Override
    public String toString() {
        return String.format("HTAB %s", expr);
    }
}