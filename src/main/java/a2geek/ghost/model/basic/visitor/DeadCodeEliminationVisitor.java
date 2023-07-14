package a2geek.ghost.model.basic.visitor;

import a2geek.ghost.model.basic.StatementContext;
import a2geek.ghost.model.basic.Visitor;
import a2geek.ghost.model.basic.statement.IfStatement;

public class DeadCodeEliminationVisitor extends Visitor {
    @Override
    public void visit(IfStatement statement, StatementContext context) {
        var expr = statement.getExpression();
        if (expr.isConstant()) {
            expr.asBoolean().ifPresent(b -> {
                if (b) {
                    context.insertAllBefore(statement.getTrueStatements());
                }
                else if (statement.hasFalseStatements()) {
                    context.insertAllBefore(statement.getFalseStatements());
                }
                context.deleteStatement();
            });
        }
        else {
            super.visit(statement, context);
        }
    }
}
