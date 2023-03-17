package a2geek.ghost.model.statement;

import a2geek.ghost.model.Statement;

public class GotoGosubStatement implements Statement {
    private String op;
    private String id;

    public GotoGosubStatement(String op, String id) {
        this.op = op;
        this.id = id;
    }

    public String getOp() {
        return op;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return String.format("%s %s", op.toUpperCase(), id);
    }
}
