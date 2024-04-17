package a2geek.ghost.model.visitor;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.VisitorContext;
import a2geek.ghost.model.statement.*;

import java.util.*;

public class StatementVisitors {
    private StatementVisitors() {
        // prevent construction
    }

    /**
     * Capture all symbols for the given statement. Note that this optionally unwraps IfStatement if recursive is specified.
     */
    public static Set<Symbol> captureSymbols(Statement statement, boolean recursive) {
        List<Expression> exprs = new ArrayList<>();
        Set<Symbol> symbols = new HashSet<>();
        switch (statement) {
            case AssignmentStatement assignmentStatement -> {
                exprs.add(assignmentStatement.getVar());
                exprs.add(assignmentStatement.getValue());
            }
            case CallStatement callStatement -> exprs.add(callStatement.getExpr());
            case CallSubroutine callSubroutine -> exprs.addAll(callSubroutine.getParameters());
            case DynamicGotoGosubStatement dynamicGotoGosubStatement -> exprs.add(dynamicGotoGosubStatement.getTarget());
            case EndStatement ignore -> {}
            case GotoGosubStatement gotoGosubStatement -> symbols.add(gotoGosubStatement.getLabel());
            case IfStatement ifStatement -> {
                exprs.add(ifStatement.getExpression());
                if (recursive) {
                    List<Statement> stmts = new ArrayList<>();
                    if (ifStatement.hasTrueStatements()) {
                        stmts.addAll(ifStatement.getTrueStatements().getInitializationStatements());
                        stmts.addAll(ifStatement.getTrueStatements().getStatements());
                    }
                    if (ifStatement.hasFalseStatements()) {
                        stmts.addAll(ifStatement.getFalseStatements().getInitializationStatements());
                        stmts.addAll(ifStatement.getFalseStatements().getStatements());
                    }
                    stmts.stream().map(stmt -> StatementVisitors.captureSymbols(stmt, recursive)).forEach(symbols::addAll);
                }
            }
            case LabelStatement labelStatement -> symbols.add(labelStatement.getLabel());
            case OnErrorStatement onErrorStatement -> {
                if (onErrorStatement.getLabel() != null) {
                    symbols.add(onErrorStatement.getLabel());
                }
            }
            case PopStatement ignored -> {}
            case RaiseErrorStatement ignored -> {}
            case ReturnStatement returnStatement -> {
                if (returnStatement.getExpr() != null) {
                    exprs.add(returnStatement.getExpr());
                }
            }
            default -> throw new RuntimeException("[compiler bug] statement type not supported: " + statement);
        }
        exprs.stream().map(ExpressionVisitors::captureSymbols).forEach(symbols::addAll);
        return symbols;
    }

    public static <T extends ExpressionTracker<T>> void dispatchToExpression(List<Statement> statements, T tracker, ExpressionDispatcherFunction<T> dispatcher) {
        for (int i=0; i< statements.size(); i++) {
            var ctx = new VisitorContext(null, statements, i);
            switch (ctx.currentStatement()) {
                case AssignmentStatement assignmentStatement -> {
                    dispatcher.dispatch(assignmentStatement.getVar(), ctx, tracker).ifPresent(assignmentStatement::setVar);
                    dispatcher.dispatch(assignmentStatement.getValue(), ctx, tracker).ifPresent(assignmentStatement::setValue);
                }
                case EndStatement ignored -> {}
                case IfStatement ifStatement -> {
                    dispatcher.dispatch(ifStatement.getExpression(), ctx, tracker).ifPresent(ifStatement::setExpression);
                    if (ifStatement.hasTrueStatements()) {
                        dispatchToExpression(ifStatement.getTrueStatements().getInitializationStatements(), tracker.create(ctx), dispatcher);
                        dispatchToExpression(ifStatement.getTrueStatements().getStatements(), tracker.create(ctx), dispatcher);
                    }
                    if (ifStatement.hasFalseStatements()) {
                        dispatchToExpression(ifStatement.getFalseStatements().getInitializationStatements(), tracker.create(ctx), dispatcher);
                        dispatchToExpression(ifStatement.getFalseStatements().getStatements(), tracker.create(ctx), dispatcher);
                    }
                }
                case CallStatement callStatement -> dispatcher.dispatch(callStatement.getExpr(), ctx, tracker).ifPresent(callStatement::setExpr);
                case GotoGosubStatement ignored -> {}
                case LabelStatement ignored -> {}
                case ReturnStatement returnStatement -> {
                    if (returnStatement.getExpr() != null) {
                        dispatcher.dispatch(returnStatement.getExpr(), ctx, tracker).ifPresent(returnStatement::setExpr);
                    }
                }
                case CallSubroutine callSubroutine -> {
                    for (int j=0; j<callSubroutine.getParameters().size(); j++) {
                        var replacement = dispatcher.dispatch(callSubroutine.getParameters().get(j), ctx, tracker);
                        if (replacement.isPresent()) {
                            callSubroutine.getParameters().set(j, replacement.get());
                        }
                    }
                }
                case PopStatement ignored -> {}
                case DynamicGotoGosubStatement dynamicGotoGosubStatement -> dispatcher.dispatch(dynamicGotoGosubStatement.getTarget(), ctx, tracker);
                case OnErrorStatement ignored -> {}
                case RaiseErrorStatement ignored -> {}
                default -> throw new RuntimeException("statement type not supported: " + ctx.currentStatement().getClass().getName());
            }
        }
    }

    public interface ExpressionDispatcherFunction<T extends ExpressionTracker<T>> {
        Optional<Expression> dispatch(Expression expression, VisitorContext ctx, T tracker);
    }
    public interface ExpressionTracker<T extends ExpressionTracker<T>> {
        T create(VisitorContext ctx);
    }
}
