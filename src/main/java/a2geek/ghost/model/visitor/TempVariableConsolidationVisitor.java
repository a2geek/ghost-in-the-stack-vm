package a2geek.ghost.model.visitor;

import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.visitor.StatementVisitors.ExpressionTracker;

import java.util.*;

import static a2geek.ghost.model.Symbol.in;
import static a2geek.ghost.model.visitor.StatementVisitors.dispatchToExpression;

public class TempVariableConsolidationVisitor implements ProgramVisitor {
    public void visit(Program program) {
        dispatch(program);
        program.findAllLocalScope(in(SymbolType.FUNCTION, SymbolType.SUBROUTINE)).stream()
                .map(Symbol::scope).forEach(this::dispatch);
    }

    public void dispatch(Scope scope) {
        var tracker = new SymbolRangeTracker();
        // Identify ranges of all variables
        dispatchToExpression(scope.getInitializationStatements(), tracker, this::captureActiveRanges);
        dispatchToExpression(scope.getStatements(), tracker, this::captureActiveRanges);
        // Reassign temporary variables
        dispatchToExpression(scope.getInitializationStatements(), tracker, this::reassignTempVariables);
        dispatchToExpression(scope.getStatements(), tracker, this::reassignTempVariables);
        // Whatever temp variables are remaining/unused, we can delete from scope
        tracker.unused().forEach(scope.getLocalSymbols()::remove);
    }

    public Optional<Expression> captureActiveRanges(Expression expression, VisitorContext ctx, SymbolRangeTracker tracker) {
        switch (expression) {
            case AddressOfFunction addrOf -> tracker.merge(addrOf.getSymbol(), ctx);
            case ArrayLengthFunction arrayLen -> tracker.merge(arrayLen.getSymbol(), ctx);
            case BinaryExpression bin -> {
                captureActiveRanges(bin.getL(), ctx, tracker.create(ctx));
                captureActiveRanges(bin.getR(), ctx, tracker.create(ctx));
            }
            case BooleanConstant ignored -> {}
            case FunctionExpression func -> func.getParameters().forEach(param -> captureActiveRanges(param, ctx, tracker));
            case IntegerConstant ignored -> {}
            case PlaceholderExpression ignored -> {}
            case StringConstant ignored -> {}
            case UnaryExpression unary -> captureActiveRanges(unary.getExpr(), ctx, tracker);
            case VariableReference ref -> tracker.merge(ref.getSymbol(), ctx);
            default -> throw new RuntimeException("[compiler bug] unsupported expression type: " + expression);
        }
        return Optional.empty();
    }

    public Optional<Expression> reassignTempVariables(Expression expression, VisitorContext ctx, SymbolRangeTracker tracker) {
        switch (expression) {
            case AddressOfFunction addrOf -> {
                expression = new AddressOfFunction(tracker.replacement(addrOf.getSymbol(), ctx));
            }
            case ArrayLengthFunction arrayLen -> {
                expression = new ArrayLengthFunction(
                    tracker.replacement(arrayLen.getSymbol(), ctx),
                    arrayLen.getDimensionNumber());
            }
            case BinaryExpression bin -> {
                reassignTempVariables(bin.getL(), ctx, tracker.create(ctx)).ifPresent(bin::setL);
                reassignTempVariables(bin.getR(), ctx, tracker.create(ctx)).ifPresent(bin::setR);
            }
            case BooleanConstant ignored -> {}
            case FunctionExpression func -> {
                for (int i=0; i<func.getParameters().size(); i++) {
                    var param = reassignTempVariables(func.getParameters().get(i), ctx, tracker);
                    if (param.isPresent()) {
                        func.getParameters().set(i, param.get());
                    }
                }
            }
            case IntegerConstant ignored -> {}
            case PlaceholderExpression ignored -> {}
            case StringConstant ignored -> {}
            case UnaryExpression unary -> {
                reassignTempVariables(unary.getExpr(), ctx, tracker).ifPresent(unary::setExpr);
            }
            case VariableReference ref -> {
                expression = VariableReference.with(tracker.replacement(ref.getSymbol(), ctx));
            }
            default -> throw new RuntimeException("[compiler bug] unsupported expression type: " + expression);
        }
        return Optional.of(expression);
    }

    public static class SymbolRangeTracker implements ExpressionTracker<SymbolRangeTracker> {
        private SymbolRangeTracker parent;
        private VisitorContext parentContext;
        private final Map<Symbol,Range> ranges = new HashMap<>();
        private final Map<Symbol,Symbol> replacements = new HashMap<>();
        private final Set<Symbol> available = new HashSet<>();

        @Override
        public SymbolRangeTracker create(VisitorContext ctx) {
            // Tracking into sub blocks is problematic; for now just tracking at the scope level
            // FIXME left all the parts to support sub blocks; need to resolve at some point
            // Primary issue tracking is the line number conundrum. It starts fresh within an IF
            // statement since it is simply the index into the List<Statement>.
            return this;
        }

        public void merge(Symbol symbol, VisitorContext ctx) {
            if (parent != null) {
                // Note that this _intentionally_ locks in any parent line number (such as IF statement)
                parent.merge(symbol, parentContext);
            }
            else {
                if (symbol.temporary() && symbol.dataType() == DataType.INTEGER) {
                    ranges.merge(symbol, Range.of(ctx.getIndex()), Range::merge);
                }
            }
        }

        public Set<Symbol> unused() {
            var unused = new HashSet<>(ranges.keySet());
            unused.removeAll(replacements.values());
            unused.removeAll(available);
            return unused;
        }

        public Symbol replacement(Symbol symbol, VisitorContext ctx) {
            if (symbol.temporary() && symbol.dataType() == DataType.INTEGER) {
                // Clean up symbols that are out of scope; need to use iterator to modify source Map
                var iterator = replacements.entrySet().iterator();
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    if (ctx.getIndex() > ranges.get(entry.getKey()).max()) {
                        available.add(entry.getValue());
                        iterator.remove();
                    }
                }
                // Have we seen the symbol already?
                if (replacements.containsKey(symbol)) {
                    return replacements.get(symbol);
                }
                // No. Do we have any unused variables?
                if (!available.isEmpty()) {
                    var replacement = available.stream().findAny().orElseThrow();
                    available.remove(replacement);
                    replacements.put(symbol, replacement);
                    return replacement;
                }
                // We should be able to just pick this symbol as one to preserve
                replacements.put(symbol, symbol);
            }
            return symbol;
        }
    }

    public record Range(int min, int max) {
        public static Range of(int startingValue) {
            return new Range(startingValue, startingValue);
        }
        public static Range merge(Range a, Range b) {
            return new Range(Math.min(a.min, b.min), Math.max(a.max, b.max));
        }
    }
}
