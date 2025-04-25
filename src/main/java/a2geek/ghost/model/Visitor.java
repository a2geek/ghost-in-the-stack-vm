package a2geek.ghost.model;

import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.statement.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A default implementation of the visitor pattern.
 */
public abstract class Visitor extends DispatchVisitor<Expression> {
    @Override
    public void visit(AssignmentStatement statement, VisitorContext context) {
        var expr = dispatch(statement.getValue());
        expr.ifPresent(statement::setValue);
        var var = dispatch(statement.getVar());
        var.ifPresent(statement::setVar);
    }

    @Override
    public void visit(EndStatement statement, VisitorContext context) {
    }

    @Override
    public void visit(CallStatement statement, VisitorContext context) {
        var expr = dispatch(statement.getExpr());
        expr.ifPresent(statement::setExpr);
    }

    @Override
    public void visit(IfStatement statement, VisitorContext context) {
        var expr = dispatch(statement.getExpression());
        dispatchAll(context, statement.getTrueStatements());
        if (statement.hasFalseStatements()) {
            dispatchAll(context, statement.getFalseStatements());
        }
        expr.ifPresent(statement::setExpression);
    }

    @Override
    public void visit(GotoGosubStatement statement, VisitorContext context) {

    }

    @Override
    public void visit(DynamicGotoGosubStatement statement, VisitorContext context) {
        var target = dispatch(statement.getTarget());
        target.ifPresent(statement::setTarget);
    }

    @Override
    public void visit(LabelStatement statement, VisitorContext context) {

    }

    @Override
    public void visit(ReturnStatement statement, VisitorContext context) {
        var expr = dispatch(statement.getExpr());
        expr.ifPresent(statement::setExpr);
    }

    @Override
    public void visit(CallSubroutine statement, VisitorContext context) {
        boolean changed = false;
        List<Expression> exprs = new ArrayList<>();
        for (Expression expr : statement.getParameters()) {
            var newExpr = dispatch(expr);
            if (newExpr.isPresent()) {
                changed = true;
                exprs.add(newExpr.get());
            } else {
                exprs.add(expr);
            }
        }
        if (changed) {
            statement.setParameters(exprs);
        }
    }

    @Override
    public void visit(PopStatement statement, VisitorContext context) {

    }

    @Override
    public void visit(OnErrorStatement statement, VisitorContext context) {

    }

    @Override
    public void visit(RaiseErrorStatement statement, VisitorContext context) {

    }

    @Override
    public Expression visit(BinaryExpression expression) {
        var l = dispatch(expression.getL());
        var r = dispatch(expression.getR());
        if (l.isPresent() || r.isPresent()) {
            l.ifPresent(expression::setL);
            r.ifPresent(expression::setR);
            return expression;
        }
        return null;
    }

    @Override
    public Expression visit(VariableReference expression) {
        // These can't be changed, but it ensures any visitors see all expressions (see StatisticsVisitor)
        if (expression.getSymbol().defaultValues() != null) {
            expression.getSymbol().defaultValues().forEach(this::dispatch);
        }
        return null;
    }

    @Override
    public Expression visit(IntegerConstant expression) {
        return null;
    }

    @Override
    public Expression visit(ByteConstant expression) {
        return null;
    }

    @Override
    public Expression visit(StringConstant expression) {
        return null;
    }

    @Override
    public Expression visit(BooleanConstant expression) {
        return null;
    }

    @Override
    public Expression visit(UnaryExpression expression) {
        var e = dispatch(expression.getExpr());
        if (e.isPresent()) {
            e.ifPresent(expression::setExpr);
            return expression;
        }
        return null;
    }

    @Override
    public Expression visit(FunctionExpression expression) {
        boolean changed = false;
        var exprs = new ArrayList<Expression>();
        for (var expr : expression.getParameters()) {
            var e = dispatch(expr);
            changed |= e.isPresent();
            exprs.add(e.orElse(expr));
        }
        if (changed) {
            expression.setParameters(exprs);
            return expression;
        }
        return null;
    }

    @Override
    public Expression visit(TypeConversionOperator expression) {
        var expr = dispatch(expression.getExpr());
        if (expr.isPresent()) {
            expr.ifPresent(expression::setExpr);
            return expression;
        }
        return null;
    }

    @Override
    public Expression visit(ArrayLengthFunction expression) {
        return null;
    }

    @Override
    public Expression visit(AddressOfOperator expression) {
        return null;
    }

    @Override
    public Expression visit(DereferenceOperator expression) {
        var e = dispatch(expression.getExpr());
        if (e.isPresent()) {
            e.ifPresent(expression::setExpr);
            return expression;
        }
        return null;
    }

    @Override
    public Expression visit(PlaceholderExpression expression) {
        return null;
    }

    @Override
    public Expression visit(IfExpression expression) {
        var c = dispatch(expression.getCondition());
        var t = dispatch(expression.getTrueValue());
        var f = dispatch(expression.getFalseValue());
        if (c.isPresent() || t.isPresent() || f.isPresent()) {
            c.ifPresent(expression::setCondition);
            t.ifPresent(expression::setTrueValue);
            f.ifPresent(expression::setFalseValue);
            return expression;
        }
        return null;
    }
}
