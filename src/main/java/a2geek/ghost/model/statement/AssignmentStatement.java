package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.expression.DereferenceOperator;
import a2geek.ghost.model.expression.VariableReference;

public class AssignmentStatement implements Statement {
    private Expression var;
    private Expression value;

    public AssignmentStatement(VariableReference var, Expression value) {
        this.var = var;
        this.value = value.checkAndCoerce(var.getType());
    }
    public AssignmentStatement(DereferenceOperator deref, Expression value) {
        this.var = deref;
        this.value = value.checkAndCoerce(deref.getType());
    }

    public Expression getVar() {
        return var;
    }
    public Expression getValue() {
        return value;
    }

    public void setVar(Expression var) {
        this.var = var;
    }
    public void setValue(Expression value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("%s = %s", var, value);
    }
}
