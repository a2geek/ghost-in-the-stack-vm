package a2geek.ghost.model;

import a2geek.ghost.model.statement.IfStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StatementBlock {
    private List<Statement> statements = new ArrayList<>();

    public List<Statement> getStatements() {
        return statements;
    }
    public boolean isLastStatement(Class<? extends Statement> clazz) {
        Statement stmt = statements.get(statements.size() - 1);
        if (clazz.isAssignableFrom(stmt.getClass())) {
            return true;
        }
        else if (stmt instanceof IfStatement ifStatement && ifStatement.hasFalseStatements()) {
            return ifStatement.getTrueStatements().isLastStatement(clazz)
                && ifStatement.getFalseStatements().isLastStatement(clazz);
        }
        return false;
    }
    public void insertStatement(Statement statement) {
        this.statements.add(0, statement);
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
