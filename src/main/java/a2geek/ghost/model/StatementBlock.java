package a2geek.ghost.model;

import java.util.List;
import java.util.stream.Collectors;

public interface StatementBlock {
    public List<Statement> getStatements();
    public void addStatement(Statement statement);

    default boolean isEmpty() {
        return getStatements() != null && getStatements().isEmpty();
    }

    default public String statementsAsString() {
        return getStatements().stream().map(Statement::toString).collect(Collectors.joining(" : "));
    }
}
