package a2geek.ghost.model.visitor;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.VisitorContext;
import a2geek.ghost.model.statement.*;

import java.util.List;
import java.util.Optional;

public class StatementVisitors {
    private StatementVisitors() {
        // prevent construction
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
                case PokeStatement pokeStatement -> {
                    dispatcher.dispatch(pokeStatement.getA(), ctx, tracker).ifPresent(pokeStatement::setA);
                    dispatcher.dispatch(pokeStatement.getB(), ctx, tracker).ifPresent(pokeStatement::setB);
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
