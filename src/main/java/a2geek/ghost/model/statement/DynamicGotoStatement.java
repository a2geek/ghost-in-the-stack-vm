package a2geek.ghost.model.statement;

import a2geek.ghost.model.Statement;
import a2geek.ghost.model.Symbol;

public class DynamicGotoStatement implements Statement {
    private Symbol label;

    public DynamicGotoStatement(Symbol label) {
        this.label = label;
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
