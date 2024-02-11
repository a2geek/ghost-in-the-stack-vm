package a2geek.ghost.model;

import a2geek.ghost.model.expression.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ExpressionTracker {
    private static final Map<String,String> REVERSE_OPS = Map.of(
            "=", "<>",
            "<>", "=",
            "<", ">=",
            ">=", "<",
            "<=", ">",
            ">", "<=");

    private final Map<Expression,Integer> exprs = new HashMap<>();

    public ExpressionTracker with(Expression constraint) {
        var tracker = new ExpressionTracker();
        tracker.exprs.putAll(this.exprs);

        // only setting up the ones we need!
        if (constraint instanceof BinaryExpression bin && REVERSE_OPS.containsKey(bin.getOp())) {
            var newbin = new BinaryExpression(bin.getL(), bin.getR(), REVERSE_OPS.get(bin.getOp()));
            tracker.exprs.put(newbin, 0);
        }

        return tracker;
    }

    public boolean isCovered(Expression expression) {
        return exprs.put(expression, 0) != null;
    }

    public boolean capture(Expression expression, int n) {
        return exprs.putIfAbsent(expression, n) != null;
    }
    public int remove(Expression expression) {
        return exprs.remove(expression);
    }
    public boolean exists(Expression expression) {
        return exprs.containsKey(expression);
    }

    public void changed(Symbol symbol) {
        exprs.entrySet().removeIf(entry -> has(entry.getKey(), symbol));
    }

    public void reset() {
        exprs.clear();
    }

    public static boolean has(Expression expr, Symbol symbol) {
        return switch (expr) {
            case AddressOfFunction addrOf -> Objects.equals(addrOf.getSymbol(), symbol);
            case ArrayLengthFunction arrayLen -> Objects.equals(arrayLen.getSymbol(), symbol);
            case BinaryExpression bin -> has(bin.getL(), symbol) || has(bin.getR(), symbol);
            case BooleanConstant ignored -> false;
            case FunctionExpression func -> {
                for (var param : func.getParameters()) {
                    if (has(param, symbol)) yield true;
                }
                yield false;
            }
            case IntegerConstant ignored -> false;
            case PlaceholderExpression ignored -> false;
            case StringConstant ignored -> false;
            case UnaryExpression unary -> has(unary.getExpr(), symbol);
            case VariableReference ref -> Objects.equals(ref.getSymbol(), symbol);
            default -> throw new RuntimeException("unsupported expression type: " + expr);
        };
    }
}
