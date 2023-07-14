package a2geek.ghost.model.basic;

import java.util.List;

public class StatementContext {
    private int index;
    private StatementBlock block;

    public StatementContext(StatementBlock block, int index) {
        this.block = block;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public Statement currentStatement() {
        return block.getStatements().get(index);
    }

    public void insertAllBefore(StatementBlock block) {
        insertAllBefore(block.getStatements());
    }
    public void insertAllBefore(List<Statement> statements) {
        block.getStatements().addAll(index, statements);
        index += statements.size();
    }

    public void deleteStatement() {
        if (index == -1) {
            throw new RuntimeException("delete already called on this statement");
        }
        block.getStatements().remove(index);
        index = -1;
    }
}
