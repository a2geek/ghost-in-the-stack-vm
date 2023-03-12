package a2geek.ghost.model.statement;

import a2geek.ghost.model.Statement;

public class GotoStatement implements Statement {
    private String id;

    public GotoStatement(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("GOTO %s", id);
    }
}
