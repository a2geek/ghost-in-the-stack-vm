package a2geek.ghost.model.statement;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

public class VtabStatement implements Statement {
    private Expression expr;

    public VtabStatement(Expression expr) {
        setExpr(expr);
    }

    public Expression getExpr() {
        return expr;
    }

    public void setExpr(Expression expr) {
        expr.mustBe(DataType.INTEGER);
        this.expr = expr;
    }

    @Override
    public String toString() {
        return String.format("VTAB %s", expr);
    }
}
