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
    public void changed(Expression expression) {
        exprs.entrySet().removeIf(entry -> has(entry.getKey(), expression));
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
            case FunctionExpression func -> func.getParameters().stream().map(param -> has(param, symbol)).reduce(Boolean::logicalOr).orElse(false);
            case IntegerConstant ignored -> false;
            case PlaceholderExpression ignored -> false;
            case StringConstant ignored -> false;
            case UnaryExpression unary -> has(unary.getExpr(), symbol);
            case VariableReference ref -> Objects.equals(ref.getSymbol(), symbol);
            default -> throw new RuntimeException("unsupported expression type: " + expr);
        };
    }
    public static boolean has(Expression expression, Expression target) {
        if (Objects.equals(expression, target)) {
            return true;
        }
        return switch (expression) {
            case AddressOfFunction ignored -> false;
            case ArrayLengthFunction ignored -> false;
            case BinaryExpression bin -> has(bin.getL(), target) || has(bin.getR(), target);
            case BooleanConstant ignored -> false;
            case FunctionExpression func -> func.getParameters().stream().map(param -> has(param, target)).reduce(Boolean::logicalOr).orElse(false);
            case IntegerConstant ignored -> false;
            case PlaceholderExpression ignored -> false;
            case StringConstant ignored -> false;
            case UnaryExpression unary -> has(unary.getExpr(), target);
            case VariableReference ignored -> false;
            default -> throw new RuntimeException("unsupported expression type: " + expression);
        };
    }
}
