package a2geek.ghost.model.statement;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Reference;
import a2geek.ghost.model.Statement;

public class AssignmentStatement implements Statement {
    private Reference ref;
    private Expression expr;

    public AssignmentStatement(Reference ref, Expression expr) {
        this.ref = ref;
        this.expr = expr;
        expr.mustBe(DataType.INTEGER, DataType.BOOLEAN, DataType.STRING);
    }

    public Reference getRef() {
        return ref;
    }

    public Expression getExpr() {
        return expr;
    }

    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    @Override
    public String toString() {
        return String.format("%s = %s", ref.name(), expr);
    }
}
