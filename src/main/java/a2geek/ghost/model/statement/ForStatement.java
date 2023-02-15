package a2geek.ghost.model.statement;

import a2geek.ghost.model.CodeBlock;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.Visitor;

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

    public String getId() {
        return id;
    }

    public Expression getStart() {
        return start;
    }

    public void setStart(Expression start) {
        this.start = start;
    }

    public Expression getEnd() {
        return end;
    }

    public void setEnd(Expression end) {
        this.end = end;
    }

    @Override
    public void accept(Visitor visitor) {
        start.accept(visitor);
        end.accept(visitor);
        statements.forEach(statement -> statement.accept(visitor));
        visitor.visit(this);
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
