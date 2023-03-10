package a2geek.ghost.model.visitor;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Visitor;
import a2geek.ghost.model.expression.BinaryExpression;
import a2geek.ghost.model.expression.IntegerConstant;
import a2geek.ghost.model.expression.NegateExpression;

import java.util.Map;
import java.util.function.BiFunction;

public class RewriteVisitor extends Visitor {
    private static final Map<String, BiFunction<Integer,Integer,Integer>> FUNCS = Map.of(
            "+",   (a,b) -> a+b,
            "-",   (a,b) -> a-b,
            "*",   (a,b) -> a*b,
            "/",   (a,b) -> a/b,
            "mod", (a,b) -> a%b,
            "<",   (a,b) -> (a<b) ? 1 : 0,
            ">",   (a,b) -> (a>b) ? 1 : 0,
            "=",   (a,b) -> (a==b) ? 1 : 0
        );

    @Override
    public Expression visit(BinaryExpression expression) {
        dispatch(expression.getL()).ifPresent(expression::setL);
        dispatch(expression.getR()).ifPresent(expression::setR);

        // Handle constant reduction
        if (expression.getL() instanceof IntegerConstant l
                && expression.getR() instanceof IntegerConstant r) {
            if (FUNCS.containsKey(expression.getOp())) {
                BiFunction<Integer,Integer,Integer> f = FUNCS.get(expression.getOp());
                return new IntegerConstant(f.apply(l.getValue(), r.getValue()));
            }
        }

        // Special cases
        switch (expression.getOp()) {
            case "+", "-", "<", ">", "=" -> {
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
    public Expression visit(NegateExpression expression) {
        dispatch(expression.getExpr()).ifPresent(expression::setExpr);

        if (expression.getExpr() instanceof IntegerConstant e) {
            return new IntegerConstant(- e.getValue());
        }

        throw new RuntimeException("Negate expression (Unary??) not supported: " + expression);
    }
}
