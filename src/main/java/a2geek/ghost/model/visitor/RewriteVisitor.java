package a2geek.ghost.model.visitor;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Visitor;
import a2geek.ghost.model.expression.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class RewriteVisitor extends Visitor {
    private static final Map<String, BiFunction<Integer,Integer,Integer>> BINOPS;
    static {
        // 'Map.of(...)' maxes out at 10 items.
        BINOPS = new HashMap<>();
        BINOPS.putAll(Map.of(
                "+",   (a,b) -> a+b,
                "-",   (a,b) -> a-b,
                "*",   (a,b) -> a*b,
                "/",   (a,b) -> a/b,
                "mod", (a,b) -> a%b
        ));
        BINOPS.putAll(Map.of(
                "<",   (a,b) -> (a<b) ? 1 : 0,
                "<=",  (a,b) -> (a<=b) ? 1 : 0,
                ">",   (a,b) -> (a>b) ? 1 : 0,
                ">=",  (a,b) -> (a>=b) ? 1 : 0,
                "=",   (a,b) -> (a==b) ? 1 : 0,
                "<>",  (a,b) -> (a!=b) ? 1 : 0,
                "or",  (a,b) -> ((a!=0) || (b!=0)) ? 1 : 0,
                "and", (a,b) -> ((a!=0) && (b!=0)) ? 1 : 0
        ));
    }

    @Override
    public Expression visit(BinaryExpression expression) {
        dispatch(expression.getL()).ifPresent(expression::setL);
        dispatch(expression.getR()).ifPresent(expression::setR);

        // Handle constant reduction
        if (expression.getL() instanceof IntegerConstant l
                && expression.getR() instanceof IntegerConstant r) {
            if (BINOPS.containsKey(expression.getOp())) {
                BiFunction<Integer,Integer,Integer> f = BINOPS.get(expression.getOp());
                return new IntegerConstant(f.apply(l.getValue(), r.getValue()));
            }
        }

        // Special cases
        switch (expression.getOp()) {
            case "+", "-", "<", "<=", ">", ">=", "<>", "=", "/", "mod", "or", "and" -> {
                return null;
            }
            case "*" -> {
                // Strength reduction
                if (expression.getL() instanceof IntegerConstant l) {
                    if (l.getValue() == 2) {
                        var right = expression.getR();
                        return new BinaryExpression(right, right, "+");
                    }
                }
                if (expression.getR() instanceof IntegerConstant r) {
                    if (r.getValue() == 2) {
                        var left = expression.getL();
                        return new BinaryExpression(left, left, "+");
                    }
                }
                return null;
            }
        }

        throw new RuntimeException("Operation not supported: " + expression);
    }

    @Override
    public Expression visit(UnaryExpression expression) {
        dispatch(expression.getExpr()).ifPresent(expression::setExpr);

        if (expression.getExpr() instanceof IntegerConstant e) {
            return new IntegerConstant(- e.getValue());
        }

        throw new RuntimeException("Negate expression (Unary??) not supported: " + expression);
    }

    @Override
    public Expression visit(FunctionExpression expression) {
        List<Expression> exprs = expression.getParameters();
        for (int i=0; i<exprs.size(); i++) {
            Expression expr = exprs.get(i);
            final int idx = i;
            dispatch(expr).ifPresent(e -> exprs.set(idx,e));
        }

        if ("asc".equalsIgnoreCase(expression.getName()) && exprs.size() == 1 &&
                exprs.get(0) instanceof StringConstant s) {
            return new IntegerConstant(s.getValue().charAt(0)|0x80);
        }

        return null;
    }
}
