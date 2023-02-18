package a2geek.ghost.model.visitor;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Visitor;
import a2geek.ghost.model.expression.IdentifierExpression;
import a2geek.ghost.model.statement.AssignmentStatement;
import a2geek.ghost.model.statement.ForStatement;

import java.util.HashSet;
import java.util.Set;

public class MetadataVisitor extends Visitor {
    private Set<String> variables = new HashSet<>();

    public Set<String> getVariables() {
        return variables;
    }

    @Override
    public void visit(ForStatement statement) {
        variables.add(statement.getId());
        super.visit(statement);
    }

    @Override
    public void visit(AssignmentStatement statement) {
        variables.add(statement.getId());
        super.visit(statement);
    }

    @Override
    public Expression visit(IdentifierExpression expression) {
        variables.add(expression.getId());
        return super.visit(expression);
    }
}
