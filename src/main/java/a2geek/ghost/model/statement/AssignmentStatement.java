package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.expression.UnaryExpression;
import a2geek.ghost.model.expression.VariableReference;

public class AssignmentStatement implements Statement {
    private final Expression var;
    private Expression value;

    public AssignmentStatement(VariableReference var, Expression value) {
        this.var = var;
        this.value = value.checkAndCoerce(var.getType());
    }
    public AssignmentStatement(UnaryExpression var, Expression value) {
        if (!"*".equals(var.getOp())) {
            throw new RuntimeException("assignment can only be to a variable or a dereference operation");
        }
        this.var = var;
        this.value = value.checkAndCoerce(var.getType());
    }

    public Expression getVar() {
        return var;
    }

    public Expression getValue() {
        return value;
    }

    public void setValue(Expression value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("%s = %s", var, value);
    }
}
