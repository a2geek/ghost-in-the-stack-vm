package a2geek.ghost.model.statement;

import a2geek.ghost.model.Statement;
import a2geek.ghost.model.Symbol;

public class DynamicGotoGosubStatement implements Statement {
    private String op;
    private Symbol label;

    public DynamicGotoGosubStatement(String op, Symbol label) {
        this.op = op;
        this.label = label;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public Symbol getLabel() {
        return label;
    }

    public void setLabel(Symbol label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("GOTO *%s", label.name());
    }
}
