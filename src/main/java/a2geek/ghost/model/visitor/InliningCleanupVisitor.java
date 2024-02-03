package a2geek.ghost.model.visitor;

import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.VariableReference;
import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.AssignmentStatement;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This visitor is to patch up after the inlining of functions.
 * Usage is essentially to run it after inlining. 1 pass should be sufficient.
 * <p/>
 * Looks for code patterns like:
 * <pre>
 *     DIM _TEMP19 AS BOOLEAN
 *     _TEMP19 = (PEEK(49050) <> 0)
 *     IF _TEMP19 THEN
 *         RETURN
 *     END IF
 * </pre>
 * ... to replace and remove the temp variable:
 * <pre>
 *     IF (PEEK(49050) <> 0) THEN
 *         RETURN
 *     END IF
 * </pre>
 * This should lessen stack usage and do a bit of optimization where appropriate.
 * (If a function has a RETURN in the middle of the function, there will be two assignments
 * to the temporary variable and will not be considered for this optimization.)
 */
public class InliningCleanupVisitor extends Visitor {
    @Override
    public void visit(Program program) {
        cleanupTempVariables(program, DeclarationType.GLOBAL);
        super.visit(program);
    }
    @Override
    public void visit(Function function) {
        cleanupTempVariables(function, DeclarationType.LOCAL);
    }
    @Override
    public void visit(Subroutine subroutine) {
        cleanupTempVariables(subroutine, DeclarationType.LOCAL);
    }

    public void cleanupTempVariables(Scope scope, DeclarationType declType) {
        var stats = new StatisticsVisitor();
        stats.dispatch(scope);
        var oneWrite = stats.getSymbolAssignments().entrySet().stream()
                .filter(e -> e.getKey().declarationType() == declType)
                .filter(e -> e.getKey().temporary())
                .filter(e -> e.getValue() == 1)
                .collect(Collectors.toSet());
        var oneRead = stats.getSymbolReads().entrySet().stream()
                .filter(e -> e.getKey().declarationType() == declType)
                .filter(e -> e.getKey().temporary())
                .filter(e -> e.getValue() == 1)
                .collect(Collectors.toSet());
        var candidates = new HashSet<>(oneWrite);
        candidates.retainAll(oneRead);
        if (candidates.isEmpty()) {
            // nothing to do!
            return;
        }
        System.out.printf("In '%s', candidates are: %s\n", scope.getFullPathName(), candidates);
        var symbols = candidates.stream().map(Map.Entry::getKey).toList();
        var rewrite = new RewriteVisitor(symbols);
        rewrite.dispatch(scope);
        // now we can clean up the symbol table
        symbols.forEach(symbol -> scope.getLocalSymbols().remove(symbol));
    }

    private static class RewriteVisitor extends Visitor {
        private Map<Symbol, Expression> rewrites = new HashMap<>();

        private RewriteVisitor(Collection<Symbol> candidates) {
            candidates.forEach(symbol -> rewrites.put(symbol, null));
        }

        @Override
        public void visit(AssignmentStatement statement, VisitorContext context) {
            if (statement.getVar() instanceof VariableReference ref) {
                if (rewrites.containsKey(ref.getSymbol())) {
                    if (rewrites.get(ref.getSymbol()) != null) {
                        var msg = String.format("expecting to set rewrite expression, but already set: %s", statement);
                        throw new RuntimeException(msg);
                    }
                    rewrites.put(ref.getSymbol(), statement.getValue());
                    context.deleteStatement();
                    return;
                }
            }
            super.visit(statement, context);
        }

        @Override
        public Expression visit(VariableReference expression) {
            if (rewrites.containsKey(expression.getSymbol())) {
                var expr = rewrites.get(expression.getSymbol());
                if (expr == null) {
                    var msg = String.format("expecting rewrite expression for symbol, but not set: %s", expression);
                    throw new RuntimeException(msg);
                }
                return expr;
            }
            return null;
        }
    }
}
