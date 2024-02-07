package a2geek.ghost.model.visitor;

import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.VariableReference;
import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.*;

import java.util.List;
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
    public void dispatchToList(Scope scope, List<Statement> statements) {
        ExpressionTracker tracker = new ExpressionTracker();
        for (int i=0; i<statements.size(); i++) {
            switch (statements.get(i)) {
                case IfStatement ifStmt -> {
                    if (ifStmt.getSource() == SourceType.BOUNDS_CHECK && tracker.isCovered(ifStmt.getExpression())) {
                        statements.remove(i);
                        i -= 1;  // since we removed the statement, we want to look again
                        counter += 1;
                    }
                }
                case AssignmentStatement assgnStmt -> {
                    if (assgnStmt.getVar() instanceof VariableReference ref) {
                        tracker.changed(ref.getSymbol());
                    }
                }
                case LabelStatement ignored -> tracker.reset();
                case GotoGosubStatement ignored -> tracker.reset();
                case DynamicGotoGosubStatement ignored -> tracker.reset();
                case EndStatement ignored -> tracker.reset();
                case RaiseErrorStatement ignored -> tracker.reset();
                case ReturnStatement ignored -> tracker.reset();
                default -> { /* do nothing */ }
            }
        }
        super.dispatchToList(scope, statements);
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
