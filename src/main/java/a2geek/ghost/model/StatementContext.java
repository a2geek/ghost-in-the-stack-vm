package a2geek.ghost.model;

import java.util.List;

public class StatementContext {
    private int index;
    private List<Statement> statements;

    public StatementContext(List<Statement> statements, int index) {
        this.statements = statements;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public Statement currentStatement() {
        return statements.get(index);
    }

    public void insertAllBefore(StatementBlock block) {
        insertAllBefore(block.getStatements());
    }
    public void insertAllBefore(List<Statement> initialStatements) {
        statements.addAll(index, initialStatements);
        index += initialStatements.size();
    }

    public void deleteStatement() {
        if (index == -1) {
            throw new RuntimeException("delete already called on this statement");
        }
        statements.remove(index);
        index = -1;
    }
}
