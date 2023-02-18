package a2geek.ghost.model;

import java.util.ArrayList;
import java.util.List;

public class Program implements StatementBlock {
    List<Statement> statements = new ArrayList<>();

    public List<Statement> getStatements() {
        return statements;
    }
    public void addStatement(Statement statement) {
        this.statements.add(statement);
    }

    @Override
    public String toString() {
        return statementsAsString();
    }
}
