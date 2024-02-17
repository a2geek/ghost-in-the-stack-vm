package a2geek.ghost.model;

import a2geek.ghost.model.expression.BinaryExpression;
import a2geek.ghost.model.visitor.ExpressionVisitors;

import java.util.HashMap;
import java.util.Map;

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
        exprs.entrySet().removeIf(entry -> ExpressionVisitors.hasSymbol(entry.getKey(), symbol));
    }
    public void changed(Expression expression) {
        exprs.entrySet().removeIf(entry -> ExpressionVisitors.hasSubexpression(entry.getKey(), expression));
    }

    public void reset() {
        exprs.clear();
    }
}
