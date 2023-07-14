package a2geek.ghost.model.basic.statement;

import a2geek.ghost.model.basic.Statement;

public class LabelStatement implements Statement {
    private String id;

    public LabelStatement(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return String.format("%s:", id);
    }
}
