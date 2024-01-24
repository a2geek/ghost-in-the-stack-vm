package a2geek.ghost.model;

import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.statement.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A default implementation of the visitor pattern.
 */
public abstract class Visitor extends DispatchVisitor {
    @Override
    public void visit(AssignmentStatement statement, StatementContext context) {
        var expr = dispatch(statement.getExpr());
        expr.ifPresent(statement::setExpr);
    }

    @Override
    public void visit(EndStatement statement, StatementContext context) {
    }

    @Override
    public void visit(CallStatement statement, StatementContext context) {
        var expr = dispatch(statement.getExpr());
        expr.ifPresent(statement::setExpr);
    }

    @Override
    public void visit(IfStatement statement, StatementContext context) {
        var expr = dispatch(statement.getExpression());
        dispatchAll(statement.getTrueStatements());
        if (statement.hasFalseStatements()) {
            dispatchAll(statement.getFalseStatements());
        }
        expr.ifPresent(statement::setExpression);
    }

    @Override
    public void visit(PokeStatement statement, StatementContext context) {
        var a = dispatch(statement.getA());
        var b = dispatch(statement.getB());
        a.ifPresent(statement::setA);
        b.ifPresent(statement::setB);
    }

    @Override
    public void visit(GotoGosubStatement statement, StatementContext context) {

    }

    @Override
    public void visit(DynamicGotoGosubStatement statement, StatementContext context) {
        var target = dispatch(statement.getTarget());
        target.ifPresent(statement::setTarget);
    }

    @Override
    public void visit(LabelStatement statement, StatementContext context) {

    }

    @Override
    public void visit(ReturnStatement statement, StatementContext context) {
        var expr = dispatch(statement.getExpr());
        expr.ifPresent(statement::setExpr);
    }

    @Override
    public void visit(CallSubroutine statement, StatementContext context) {
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
    public void visit(PopStatement statement, StatementContext context) {

    }

    @Override
    public void visit(OnErrorStatement statement, StatementContext context) {

    }

    @Override
    public void visit(RaiseErrorStatement statement, StatementContext context) {

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
        boolean changed = false;
        var exprs = new ArrayList<Expression>();
        for (var expr : expression.getIndexes()) {
            var e = dispatch(expr);
            changed |= e.isPresent();
            exprs.add(e.orElse(expr));
        }
        if (changed) {
            expression.setIndexes(exprs);
            return expression;
        }
        return null;
    }

    @Override
    public Expression visit(IntegerConstant expression) {
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
    public Expression visit(ArrayLengthFunction expression) {
        return null;
    }

    @Override
    public Expression visit(AddressOfFunction expression) {
        return null;
    }
}
