package a2geek.ghost.model;

import java.util.List;

public class VisitorContext {
    private int index;
    private final Scope scope;
    private final List<Statement> statements;

    public VisitorContext(Scope scope, List<Statement> statements, int index) {
        this.scope = scope;
        this.statements = statements;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public Scope getScope() {
        return scope;
    }

    public Statement currentStatement() {
        return statements.get(index);
    }
    public Statement nextStatement() {
        if (index+1 < statements.size()) {
            return statements.get(index+1);
        }
        return null;
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
