package a2geek.ghost.model.visitor;

import a2geek.ghost.model.Visitor;
import a2geek.ghost.model.VisitorContext;
import a2geek.ghost.model.statement.GotoGosubStatement;
import a2geek.ghost.model.statement.IfStatement;
import a2geek.ghost.model.statement.LabelStatement;

import java.util.Objects;

public class DeadCodeEliminationVisitor extends Visitor {
    @Override
    public void visit(IfStatement statement, VisitorContext context) {
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

    @Override
    public void visit(GotoGosubStatement statement, VisitorContext context) {
        //     GOTO _label   ==>  (delete)
        // _label:           ==>  _label:
        if ("goto".equals(statement.getOp())) {
            if (context.nextStatement() instanceof LabelStatement labelStmt) {
                if (Objects.equals(statement.getLabel(), labelStmt.getLabel())) {
                    context.deleteStatement();
                    return;
                }
            }
        }
        super.visit(statement, context);
    }
}
