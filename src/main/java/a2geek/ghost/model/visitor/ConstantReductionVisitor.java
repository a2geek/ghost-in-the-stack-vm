package a2geek.ghost.model.visitor;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.SymbolType;
import a2geek.ghost.model.Visitor;
import a2geek.ghost.model.expression.*;

import java.util.List;
import java.util.Optional;

public class ConstantReductionVisitor extends Visitor {
    @Override
    public Expression visit(BinaryExpression expression) {
        dispatch(expression.getL()).ifPresent(expression::setL);
        dispatch(expression.getR()).ifPresent(expression::setR);

        return constantReduction(expression).orElse(null);
    }

    @Override
    public Expression visit(ArrayLengthFunction expression) {
        return constantReduction(expression).orElse(null);
    }

    @Override
    public Expression visit(UnaryExpression expression) {
        dispatch(expression.getExpr()).ifPresent(expression::setExpr);

        return constantReduction(expression).orElse(null);
    }

    @Override
    public Expression visit(DereferenceOperator expression) {
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

    @Override
    public Expression visit(VariableReference expression) {
        var symbol = expression.getSymbol();
        if (symbol.symbolType() == SymbolType.CONSTANT && symbol.numDimensions() == 0 && symbol.defaultValues().size() == 1) {
            return symbol.defaultValues().getFirst();
        }
        return super.visit(expression);
    }

    public Optional<Expression> constantReduction(Expression expression) {
        if (expression.isConstant()) {
            return switch (expression.getType()) {
                // TODO fix when BYTE becomes a real type
                case INTEGER, BYTE -> expression.asInteger().map(IntegerConstant::new);
                case BOOLEAN -> expression.asBoolean().map(BooleanConstant::new);
                case STRING -> expression.asString().map(StringConstant::new);
                case ADDRESS -> Optional.empty();
            };
        }
        return Optional.empty();
    }
}
