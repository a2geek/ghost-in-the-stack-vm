package a2geek.ghost.model.statement;

import a2geek.ghost.model.Statement;
import a2geek.ghost.model.Symbol;

public class OnErrorStatement implements Statement {
    private Operation op;
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
        String msg = switch (op) {
            case GOTO -> String.format("GOTO %s", label.name());
            default -> op.toString();
        };
        return String.format("ON ERROR %s", msg);
    }

    public enum Operation {
        GOTO, DISABLE, RESUME_NEXT
    }
}
