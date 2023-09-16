package a2geek.ghost.model.basic.statement;

import a2geek.ghost.model.basic.Statement;
import a2geek.ghost.model.basic.Symbol;

public class GotoGosubStatement implements Statement {
    private String op;
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
