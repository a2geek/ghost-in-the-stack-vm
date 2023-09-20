package a2geek.ghost.model.statement;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

public class CallStatement implements Statement {
    private Expression expr;

    public CallStatement(Expression expr) {
        this.expr = expr;
        expr.mustBe(DataType.INTEGER);
    }

    public Expression getExpr() {
        return expr;
    }

    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    @Override
    public String toString() {
        return String.format("CALL %s", expr);
    }
}
