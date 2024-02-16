package a2geek.ghost.model.visitor;

import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.visitor.StatementVisitors.ExpressionTracker;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
        dispatchToExpression(scope.getInitializationStatements(), tracker, this::captureActiveRanges);
        dispatchToExpression(scope.getStatements(), tracker, this::captureActiveRanges);
        System.out.printf("SCOPE = %s\n", scope.getFullPathName());
        tracker.ranges.forEach((symbol,range) -> {
            System.out.printf("%-15.15s ==> %d..%d\n", symbol.name(), range.min(), range.max());
        });
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

    public static class SymbolRangeTracker implements ExpressionTracker<SymbolRangeTracker> {
        private SymbolRangeTracker parent;
        private VisitorContext parentContext;
        private Map<Symbol,Range> ranges = new HashMap<>();

        @Override
        public SymbolRangeTracker create(VisitorContext ctx) {
            var tracker = new SymbolRangeTracker();
            tracker.parent = this;
            tracker.parentContext = ctx;
            return tracker;
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
