package a2geek.ghost.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StatementBlock {
    private List<Statement> statements = new ArrayList<>();

    public List<Statement> getStatements() {
        return statements;
    }
    public void addStatement(Statement statement) {
        this.statements.add(statement);
    }
    public boolean isEmpty() {
        return statements.isEmpty();
    }

    public String statementsAsString() {
        return statements.stream().map(Statement::toString).collect(Collectors.joining(" : "));
    }
}
