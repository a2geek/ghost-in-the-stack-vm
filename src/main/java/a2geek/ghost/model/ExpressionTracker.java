package a2geek.ghost.model;

import a2geek.ghost.model.expression.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ExpressionTracker {
    private final Set<Expression> exprs = new HashSet<>();

    public boolean isCovered(Expression expression) {
        if (exprs.contains(expression)) {
            return true;
        }
        exprs.add(expression);
        return false;
    }

    public void changed(Symbol symbol) {
        exprs.removeIf(expr -> has(expr, symbol));
    }

    public void reset() {
        exprs.clear();
    }

    public boolean has(Expression expr, Symbol symbol) {
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
