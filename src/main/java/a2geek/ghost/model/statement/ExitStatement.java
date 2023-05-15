package a2geek.ghost.model.statement;

import a2geek.ghost.model.Statement;

public class ExitStatement implements Statement {
    private String op;

    public ExitStatement(String op) {
        this.op = op;
    }

    public String getOp() {
        return op;
    }

    @Override
    public String toString() {
        return String.format("exit %s", op);
    }
}
