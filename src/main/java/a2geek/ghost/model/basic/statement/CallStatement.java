package a2geek.ghost.model.basic.statement;

import a2geek.ghost.model.basic.DataType;
import a2geek.ghost.model.basic.Expression;
import a2geek.ghost.model.basic.Statement;

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
