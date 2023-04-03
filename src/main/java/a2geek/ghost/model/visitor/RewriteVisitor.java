package a2geek.ghost.model.visitor;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Visitor;
import a2geek.ghost.model.expression.*;

import java.util.List;
import java.util.Optional;

public class RewriteVisitor extends Visitor {
     @Override
    public Expression visit(BinaryExpression expression) {
        dispatch(expression.getL()).ifPresent(expression::setL);
        dispatch(expression.getR()).ifPresent(expression::setR);

        // Handle constant reduction first
        var opt = constantReduction(expression);
        if (opt.isPresent()) {
            return opt.get();
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

    @Override
    public Expression visit(UnaryExpression expression) {
        dispatch(expression.getExpr()).ifPresent(expression::setExpr);

        return constantReduction(expression).orElse(null);
    }

    @Override
    public Expression visit(FunctionExpression expression) {
        List<Expression> exprs = expression.getParameters();
        for (int i=0; i<exprs.size(); i++) {
            Expression expr = exprs.get(i);
            final int idx = i;
            dispatch(expr).ifPresent(e -> exprs.set(idx,e));
        }

        return constantReduction(expression).orElse(null);
    }

    public Optional<Expression> constantReduction(Expression expression) {
        if (expression.isConstant()) {
            return switch (expression.getType()) {
                case INTEGER -> expression.asInteger().map(IntegerConstant::new);
                case BOOLEAN -> expression.asBoolean().map(BooleanConstant::new);
                case STRING -> expression.asString().map(StringConstant::new);
            };
        }
        return Optional.empty();
    }
}
