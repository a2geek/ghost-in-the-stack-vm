package a2geek.ghost.model.statement;

import a2geek.ghost.model.Statement;
import a2geek.ghost.model.Symbol;

public class GotoGosubStatement implements Statement {
    private final String op;
    private Symbol label;

    public GotoGosubStatement(String op, Symbol label) {
        this.op = op;
        this.label = label;
    }

    public String getOp() {
        return op;
    }

    public Symbol getLabel() {
        return label;
    }

    public void setLabel(Symbol label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("%s %s", op.toUpperCase(), label.name());
    }
}
