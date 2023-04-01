package a2geek.ghost.model.visitor;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Visitor;
import a2geek.ghost.model.expression.*;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

public class RewriteVisitor extends Visitor {
    private static final Map<String, BiFunction<Integer,Integer,Integer>> BINOPS;
    static {
        // 'Map.of(...)' maxes out at 10 items.
        BINOPS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        // Arithmetic
        BINOPS.putAll(Map.of(
            "+",   (a,b) -> a+b,
            "-",   (a,b) -> a-b,
            "*",   (a,b) -> a*b,
            "/",   (a,b) -> a/b,
            "mod", (a,b) -> a%b
        ));
        // Comparison
        BINOPS.putAll(Map.of(
            "<",   (a,b) -> (a<b) ? 1 : 0,
            "<=",  (a,b) -> (a<=b) ? 1 : 0,
            ">",   (a,b) -> (a>b) ? 1 : 0,
            ">=",  (a,b) -> (a>=b) ? 1 : 0,
            "=",   (a,b) -> (a==b) ? 1 : 0,
            "<>",  (a,b) -> (a!=b) ? 1 : 0
        ));
        // Logical/Bit
        BINOPS.putAll(Map.of(
            "or",  (a,b) -> a|b,
            "and", (a,b) -> a&b,
            "xor", (a,b) -> a^b
        ));
        // Bit
        BINOPS.putAll(Map.of(
           "<<", (a,b) -> a<<b,
           ">>", (a,b) -> a>>b
        ));
    }

    @Override
    public Expression visit(BinaryExpression expression) {
        dispatch(expression.getL()).ifPresent(expression::setL);
        dispatch(expression.getR()).ifPresent(expression::setR);

        var desc = BinaryExpression.findDescriptor(expression.getOp(), expression.getL().getType(), expression.getR().getType()).orElseGet(() -> {
            String message = String.format("argument types not supported for '%s': %s, %s",
                    expression.getOp(), expression.getL().getType(), expression.getR().getType());
            throw new RuntimeException(message);
        });

        // Handle constant reduction
        boolean lconst = expression.getL() instanceof IntegerConstant || expression.getL() instanceof BooleanConstant;
        boolean rconst = expression.getR() instanceof IntegerConstant || expression.getR() instanceof BooleanConstant;
        if (lconst && rconst && BINOPS.containsKey(expression.getOp())) {
            BiFunction<Integer,Integer,Integer> f = BINOPS.get(expression.getOp());
            int l = toInteger(expression.getL());
            int r = toInteger(expression.getR());
            int value = f.apply(l, r);
            return switch (desc.returnType()) {
                case INTEGER -> new IntegerConstant(value);
                case BOOLEAN -> new BooleanConstant(value != 0);
                default -> {
                    throw new RuntimeException("unexpected datatype in constant reduction: " + desc.returnType());
                }
            };
        }

        // Special cases
        switch (expression.getOp()) {
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

        return null;
    }

    public int toInteger(Expression expr) {
        if (expr instanceof IntegerConstant e) {
            return e.getValue();
        }
        else if (expr instanceof BooleanConstant e) {
            return e.getValue() ? 1 : 0;
        }
        else {
            throw new RuntimeException("unable to convert to integer: " + expr.getClass().getName());
        }
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
