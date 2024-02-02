package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

public class DynamicGotoGosubStatement implements Statement {
    private String op;
    private Expression target;
    private final boolean needsAddressAdjustment;

    public DynamicGotoGosubStatement(String op, Expression target, boolean needsAddressAdjustment) {
        this.op = op;
        this.target = target;
        this.needsAddressAdjustment = needsAddressAdjustment;
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

    public boolean needsAddressAdjustment() {
        return needsAddressAdjustment;
    }

    @Override
    public String toString() {
        return String.format("GOTO *%s", target);
    }
}
