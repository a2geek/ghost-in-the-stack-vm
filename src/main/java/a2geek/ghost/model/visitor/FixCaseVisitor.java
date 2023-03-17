package a2geek.ghost.model.visitor;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Visitor;
import a2geek.ghost.model.expression.IdentifierExpression;
import a2geek.ghost.model.statement.AssignmentStatement;
import a2geek.ghost.model.statement.ForStatement;
import a2geek.ghost.model.statement.GotoGosubStatement;
import a2geek.ghost.model.statement.LabelStatement;

import java.util.HashSet;
import java.util.Set;

/**
 * The ANTLR grammar has been configured to be case-insensitive.
 * However, that does not have any impact on the identifiers specified
 * in the program. This rewrite visitor will "fix" all variables to be
 * uppercase. It is independent with the thought that it will become
 * optional at some point. It must be run first before any variable
 * metadata is gathered.
 */
public class FixCaseVisitor extends Visitor {
    @Override
    public void visit(ForStatement statement) {
        statement.setId(statement.getId().toUpperCase());
        super.visit(statement);
    }

    @Override
    public void visit(GotoGosubStatement statement) {
        statement.setId(statement.getId().toUpperCase());
        super.visit(statement);
    }

    @Override
    public void visit(LabelStatement statement) {
        statement.setId(statement.getId().toUpperCase());
        super.visit(statement);
    }

    @Override
    public void visit(AssignmentStatement statement) {
        statement.setId(statement.getId().toUpperCase());
        super.visit(statement);
    }

    @Override
    public Expression visit(IdentifierExpression expression) {
        expression.setId(expression.getId().toUpperCase());
        return super.visit(expression);
    }
}
