package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.expression.VariableReference;

public class AssignmentStatement implements Statement {
    private VariableReference var;
    private Expression expr;

    public AssignmentStatement(VariableReference var, Expression expr) {
        this.var = var;
        this.expr = expr.checkAndCoerce(var.getType());
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
