package a2geek.ghost.model;

import a2geek.ghost.model.statement.IfStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StatementBlock {
    public static final StatementBlock EMPTY = new StatementBlock();
    public static StatementBlock with(Statement... statements) {
        StatementBlock sb = new StatementBlock();
        for (var stmt : statements) {
            sb.addStatement(stmt);
        }
        return sb;
    }
    private List<Statement> initializationStatements = new ArrayList<>();
    private List<Statement> statements = new ArrayList<>();

    public List<Statement> getInitializationStatements() {
        return initializationStatements;
    }
    public List<Statement> getStatements() {
        return statements;
    }
    public boolean isLastStatement(Class<? extends Statement> clazz) {
        Statement stmt = statements.getLast();
        if (clazz.isAssignableFrom(stmt.getClass())) {
            return true;
        }
        else if (stmt instanceof IfStatement ifStatement && ifStatement.hasFalseStatements()) {
            return ifStatement.getTrueStatements().isLastStatement(clazz)
                && ifStatement.getFalseStatements().isLastStatement(clazz);
        }
        return false;
    }
    public boolean contains(Class<? extends Statement> clazz) {
        for (var stmt : statements) {
            if (clazz.isAssignableFrom(stmt.getClass())) {
                return true;
            }
            else if (stmt instanceof IfStatement ifStatement) {
                if (ifStatement.hasTrueStatements() && ifStatement.getTrueStatements().contains(clazz)) {
                    return true;
                }
                if (ifStatement.hasFalseStatements() && ifStatement.getFalseStatements().contains(clazz)) {
                    return true;
                }
            }
        }
        return false;
    }
    public void addInitializationStatement(Statement statement) {
        this.initializationStatements.add(statement);
    }
    public void addStatement(Statement statement) {
        this.statements.add(statement);
    }
    public boolean isEmpty() {
        return statements.isEmpty();
    }

    public String statementsAsString() {
        return Stream.concat(initializationStatements.stream(), statements.stream())
            .map(Statement::toString).collect(Collectors.joining(" : "));
    }
}
