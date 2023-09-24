package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

public class DynamicGotoGosubStatement implements Statement {
    private String op;
    private Expression target;

    public DynamicGotoGosubStatement(String op, Expression target) {
        this.op = op;
        this.target = target;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public Expression getTarget() {
        return target;
    }

    public void setTarget(Expression target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return String.format("GOTO *%s", target);
    }
}
