package a2geek.ghost.model.statement;

import java.util.ArrayList;
import java.util.List;

import a2geek.ghost.model.Statement;
import a2geek.ghost.model.StatementBlock;

public class BaseStatementBlock implements StatementBlock {
    List<Statement> statements = new ArrayList<>();

    @Override
    public void addStatement(Statement statement) {
        statements.add(statement);
    }

    @Override
    public List<Statement> getStatements() {
        return statements;
    }
}
