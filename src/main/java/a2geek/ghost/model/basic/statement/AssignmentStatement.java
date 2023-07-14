package a2geek.ghost.model.basic.statement;

import a2geek.ghost.model.basic.DataType;
import a2geek.ghost.model.basic.Expression;
import a2geek.ghost.model.basic.Statement;
import a2geek.ghost.model.basic.expression.VariableReference;

public class AssignmentStatement implements Statement {
    private VariableReference var;
    private Expression expr;

    public AssignmentStatement(VariableReference var, Expression expr) {
        this.var = var;
        this.expr = expr;
        // TODO string is really being evaluated as a pointer to a string.
        expr.mustBe(DataType.INTEGER, DataType.BOOLEAN, DataType.STRING);
    }

    public VariableReference getVar() {
        return var;
    }

    public Expression getExpr() {
        return expr;
    }

    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    @Override
    public String toString() {
        return String.format("%s = %s", var, expr);
    }
}
