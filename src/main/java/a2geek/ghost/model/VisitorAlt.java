package a2geek.ghost.model;

import a2geek.ghost.model.expression.BinaryExpression;
import a2geek.ghost.model.expression.IdentifierExpression;
import a2geek.ghost.model.expression.IntegerConstant;
import a2geek.ghost.model.statement.*;

import java.util.Optional;

public abstract class VisitorAlt {
    public void visit(Program program) {
        program.getStatements().forEach(this::dispatch);
    }
    public void dispatch(Statement statement) {
        if (statement instanceof AssignmentStatement s) {
            visit(s);
        }
        else if (statement instanceof ColorStatement s) {
            visit(s);
        }
        else if (statement instanceof EndStatement s) {
            visit(s);
        }
        else if (statement instanceof ForStatement s) {
            visit(s);
        }
        else if (statement instanceof GrStatement s) {
            visit(s);
        }
        else if (statement instanceof PlotStatement s) {
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
        else if (expression instanceof IdentifierExpression e) {
            return Optional.ofNullable(visit(e));
        }
        else if (expression instanceof IntegerConstant e) {
            return Optional.ofNullable(visit(e));
        }
        else {
            throw new RuntimeException("expression type not supported: " +
                    expression.getClass().getName());
        }
    }

    public void visit(AssignmentStatement statement) {
        var expr = dispatch(statement.getExpr());
        expr.ifPresent(statement::setExpr);
    }

    public void visit(ColorStatement statement) {
        var expr = dispatch(statement.getExpr());
        expr.ifPresent(statement::setExpr);
    }

    public void visit(EndStatement statement) {
    }

    public void visit(ForStatement statement) {
        var start = dispatch(statement.getStart());
        var end = dispatch(statement.getEnd());
        statement.getStatements().forEach(this::dispatch);
        start.ifPresent(statement::setStart);
        end.ifPresent(statement::setEnd);
    }

    public void visit(GrStatement statement) {

    }

    public void visit(PlotStatement statement) {
        var x = dispatch(statement.getX());
        var y = dispatch(statement.getY());
        x.ifPresent(statement::setX);
        y.ifPresent(statement::setY);
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

    public Expression visit(IdentifierExpression expression) {
        return null;
    }

    public Expression visit(IntegerConstant expression) {
        return null;
    }
}
