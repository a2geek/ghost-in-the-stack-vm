package a2geek.ghost.model.statement;

import a2geek.ghost.model.Statement;
import a2geek.ghost.model.Symbol;

public class OnErrorStatement implements Statement {
    private final Operation op;
    private Symbol label;

    public OnErrorStatement(Operation op) {
        this.op = op;
    }
    public OnErrorStatement(Symbol label) {
        this.op = Operation.GOTO;
        this.label = label;
    }

    public Operation getOp() {
        return op;
    }
    public Symbol getLabel() {
        return label;
    }

    @Override
    public String toString() {
        String msg = op.toString();
        if (op == Operation.GOTO) {
            msg = String.format("GOTO %s", label.name());
        }
        return String.format("ON ERROR %s", msg);
    }

    public enum Operation {
        GOTO, DISABLE, RESUME_NEXT
    }
}
