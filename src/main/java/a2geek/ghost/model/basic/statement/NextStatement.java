package a2geek.ghost.model.basic.statement;

import a2geek.ghost.model.basic.Statement;
import a2geek.ghost.model.basic.Symbol;
import a2geek.ghost.model.basic.scope.ForFrame;

public class NextStatement implements Statement {
    private static int nextNumber = 0;
    private Symbol symbol;
    private ForFrame frame;
    private String exitLabel;

    public NextStatement(Symbol symbol, ForFrame frame) {
        this.symbol = symbol;
        this.frame = frame;
        this.exitLabel = String.format("do_next_%s_exit%d", symbol.name(), nextNumber++);
    }

    public Symbol getSymbol() {
        return symbol;
    }
    public ForFrame getFrame() {
        return frame;
    }
    public String getExitLabel() {
        return exitLabel;
    }

    @Override
    public String toString() {
        return String.format("NEXT %s", symbol.name());
    }
}
