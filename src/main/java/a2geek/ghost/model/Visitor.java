package a2geek.ghost.model;

import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class Visitor {
    public void dispatch(Scope scope) {
        if (scope instanceof Program s) {
            visit(s);
        }
        else if (scope instanceof Function s) {
            visit(s);
        }
        else if (scope instanceof Subroutine s) {
            visit(s);
        }
        else {
            throw new RuntimeException("scope type not supported: " +
                    scope.getClass().getName());
        }
    }
    public void dispatch(Statement statement) {
        if (statement instanceof AssignmentStatement s) {
            visit(s);
        }
        else if (statement instanceof EndStatement s) {
            visit(s);
        }
        else if (statement instanceof IfStatement s) {
            visit(s);
        }
        else if (statement instanceof ForNextStatement s) {
            visit(s);
        }
        else if (statement instanceof ForStatement s) {
            visit(s);
        }
        else if (statement instanceof NextStatement s) {
            visit(s);
        }
        else if (statement instanceof PokeStatement s) {
            visit(s);
        }
        else if (statement instanceof CallStatement s) {
            visit(s);
        }
        else if (statement instanceof GotoGosubStatement s) {
            visit(s);
        }
        else if (statement instanceof LabelStatement s) {
            visit(s);
        }
        else if (statement instanceof ReturnStatement s) {
            visit(s);
        }
        else if (statement instanceof CallSubroutine s) {
            visit(s);
        }
        else if (statement instanceof DimStatement s) {
            visit(s);
        }
        else if (statement instanceof PopStatement s) {
            visit(s);
        }
        else {
            throw new RuntimeException("statement type not supported: " +
                    statement.getClass().getName());
        }
    }
    public Optional<Expression> dispatch(Expression expression) {
        if (expression instanceof BinaryExpression e) {
            return Optional.ofNullable(visit(e));
        }
        else if (expression instanceof VariableReference e) {
            return Optional.ofNullable(visit(e));
        }
        else if (expression instanceof IntegerConstant e) {
            return Optional.ofNullable(visit(e));
        }
        else if (expression instanceof StringConstant e) {
            return Optional.ofNullable(visit(e));
        }
        else if (expression instanceof BooleanConstant e) {
            return Optional.ofNullable(visit(e));
        }
        else if (expression instanceof ParenthesisExpression e) {
            return Optional.ofNullable(visit(e));
        }
        else if (expression instanceof UnaryExpression e) {
            return Optional.ofNullable(visit(e));
        }
        else if (expression instanceof FunctionExpression e) {
            return Optional.ofNullable(visit(e));
        }
        else if (expression instanceof ArrayLengthFunction e) {
            return Optional.ofNullable(visit(e));
        }
        else {
            throw new RuntimeException("expression type not supported: " +
                    expression.getClass().getName());
        }
    }

    public void visit(Program program) {
        program.getStatements().forEach(this::dispatch);
        program.getScopes().forEach(this::dispatch);
    }
    public void visit(Subroutine subroutine) {
        subroutine.getStatements().forEach(this::dispatch);
        // We assume there are no additional scopes for BASIC.
    }
    public void visit(Function function) {
        function.getStatements().forEach(this::dispatch);
    }

    public void visit(AssignmentStatement statement) {
        var expr = dispatch(statement.getExpr());
        expr.ifPresent(statement::setExpr);
    }

    public void visit(EndStatement statement) {
    }

    public void visit(CallStatement statement) {
        var expr = dispatch(statement.getExpr());
        expr.ifPresent(statement::setExpr);
    }

    public void visit(IfStatement statement) {
        var expr = dispatch(statement.getExpression());
        statement.getTrueStatements().getStatements().forEach(this::dispatch);
        if (statement.hasFalseStatements()) {
            statement.getFalseStatements().getStatements().forEach(this::dispatch);
        }
        expr.ifPresent(statement::setExpression);
    }

    public void visit(ForNextStatement statement) {
        var start = dispatch(statement.getStart());
        var end = dispatch(statement.getEnd());
        var step = dispatch(statement.getStep());   // Always set by GhostBasicVisitor
        statement.getStatements().forEach(this::dispatch);
        start.ifPresent(statement::setStart);
        end.ifPresent(statement::setEnd);
        step.ifPresent(statement::setStep);
    }

    public void visit(ForStatement statement) {
        var start = dispatch(statement.getStart());
        var end = dispatch(statement.getEnd());
        var step = dispatch(statement.getStep());   // Always set by GhostBasicVisitor
        start.ifPresent(statement::setStart);
        end.ifPresent(statement::setEnd);
        step.ifPresent(statement::setStep);
    }

    public void visit(NextStatement statement) {

    }

    public void visit(PokeStatement statement) {
        var a = dispatch(statement.getA());
        var b = dispatch(statement.getB());
        a.ifPresent(statement::setA);
        b.ifPresent(statement::setB);
    }

    public void visit(GotoGosubStatement statement) {

    }

    public void visit(LabelStatement statement) {

    }

    public void visit(ReturnStatement statement) {

    }

    public void visit(CallSubroutine statement) {
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

    public void visit(DimStatement statement) {
        var expr = dispatch(statement.getExpr());
        expr.ifPresent(statement::setExpr);
    }

    public void visit(PopStatement statement) {

    }

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

    public Expression visit(IntegerConstant expression) {
        return null;
    }

    public Expression visit(StringConstant expression) {
        return null;
    }

    public Expression visit(BooleanConstant expression) {
        return null;
    }

    public Expression visit(ParenthesisExpression expression) {
        var e = dispatch(expression.getExpr());
        if (e.isPresent()) {
            e.ifPresent(expression::setExpr);
            return expression;
        }
        return null;
    }

    public Expression visit(UnaryExpression expression) {
        var e = dispatch(expression.getExpr());
        if (e.isPresent()) {
            e.ifPresent(expression::setExpr);
            return expression;
        }
        return null;
    }

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

    public Expression visit(ArrayLengthFunction expression) {
        return null;
    }
}
