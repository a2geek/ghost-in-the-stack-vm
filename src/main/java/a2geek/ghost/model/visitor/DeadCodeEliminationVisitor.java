package a2geek.ghost.model.visitor;

import a2geek.ghost.model.*;
import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.GotoGosubStatement;
import a2geek.ghost.model.statement.IfStatement;
import a2geek.ghost.model.statement.LabelStatement;

import java.util.Objects;

import static a2geek.ghost.model.Symbol.in;

public class DeadCodeEliminationVisitor extends Visitor implements RepeatingVisitor {
    private int counter;
    private StatisticsVisitor statistics;

    @Override
    public int getCounter() {
        return counter;
    }

    public void gatherStatistics(Scope scope) {
        statistics = new StatisticsVisitor();
        statistics.dispatch(scope);
    }

    public void clearStatistics() {
        statistics = null;
    }

    @Override
    public void visit(Program program) {
        gatherStatistics(program);
        dispatchAll(program, program);
        clearStatistics();
        program.findAllLocalScope(in(SymbolType.FUNCTION,SymbolType.SUBROUTINE)).forEach(symbol -> {
            dispatch(symbol.scope());
        });
    }

    @Override
    public void visit(Subroutine subroutine) {
        gatherStatistics(subroutine);
        dispatchAll(subroutine, subroutine);
        clearStatistics();
    }

    @Override
    public void visit(Function function) {
        gatherStatistics(function);
        dispatchAll(function, function);
        clearStatistics();
    }

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
                counter+= 1;
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
                    counter+= 1;
                    return;
                }
            }
        }
        super.visit(statement, context);
    }

    @Override
    public void visit(LabelStatement statement, VisitorContext context) {
        if (!statistics.getUsedLabels().contains(statement.getLabel())) {
            context.deleteStatement();
            counter+= 1;
        }
        else {
            super.visit(statement, context);
        }
    }
}
