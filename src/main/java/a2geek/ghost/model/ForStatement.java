package a2geek.ghost.model;

import java.util.ArrayList;
import java.util.List;

public class ForStatement implements Statement, CodeBlock {
    private String id;
    private Expression start;
    private Expression end;
    private List<Statement> statements = new ArrayList<>();

    public ForStatement(String id, Expression start, Expression end) {
        this.id = id;
        this.start = start;
        this.end = end;
    }

    public void addStatement(Statement statement) {
        this.statements.add(statement);
    }

    @Override
    public List<Statement> getStatements() {
        return statements;
    }

    @Override
    public String toString() {
        return String.format("FOR %s = %s TO %s : %s : NEXT %s", id, start, end, statementsAsString(), id);
    }
}
