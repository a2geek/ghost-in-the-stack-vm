package a2geek.ghost.model;

import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static a2geek.ghost.model.Symbol.in;

public abstract class Visitor {
    public void dispatch(Scope scope) {
        switch (scope) {
            case Program s -> visit(s);
            case Function s -> visit(s);
            case Subroutine s -> visit(s);
            default -> throw new RuntimeException("scope type not supported: " +
                    scope.getClass().getName());
        }
    }
    public void dispatch(StatementContext context) {
        var statement = context.currentStatement();
        switch (statement) {
            case AssignmentStatement s -> visit(s, context);
            case EndStatement s -> visit(s, context);
            case IfStatement s -> visit(s, context);
            case PokeStatement s -> visit(s, context);
            case CallStatement s -> visit(s, context);
            case GotoGosubStatement s -> visit(s, context);
            case LabelStatement s -> visit(s, context);
            case ReturnStatement s -> visit(s, context);
            case CallSubroutine s -> visit(s, context);
            case PopStatement s -> visit(s, context);
            case DynamicGotoGosubStatement s -> visit(s, context);
            case OnErrorStatement s -> visit(s, context);
            case RaiseErrorStatement s -> visit(s, context);
            default -> throw new RuntimeException("statement type not supported: " +
                    statement.getClass().getName());
        }
    }
    public Optional<Expression> dispatch(Expression expression) {
        return switch (expression) {
            // This occurs when the expression is optional, such as the RETURN statement.
            // Catching it here instead of testing everywhere else as issues are discovered.
            case null -> Optional.empty();
            case BinaryExpression e -> Optional.ofNullable(visit(e));
            case VariableReference e -> Optional.ofNullable(visit(e));
            case IntegerConstant e -> Optional.ofNullable(visit(e));
            case StringConstant e -> Optional.ofNullable(visit(e));
            case BooleanConstant e -> Optional.ofNullable(visit(e));
            case UnaryExpression e -> Optional.ofNullable(visit(e));
            case FunctionExpression e -> Optional.ofNullable(visit(e));
            case ArrayLengthFunction e -> Optional.ofNullable(visit(e));
            case AddressOfFunction e -> Optional.ofNullable(visit(e));
            default -> throw new RuntimeException("expression type not supported: " +
                            expression.getClass().getName());
        };
    }

    public void dispatchAll(StatementBlock block) {
        dispatchToList(block.getInitializationStatements());
        dispatchToList(block.getStatements());
    }
    public void dispatchToList(List<Statement> statements) {
        for (int i=0; i<statements.size(); i++) {
            StatementContext context = new StatementContext(statements, i);
            dispatch(context);
            // If we insert or delete a row, we should reprocess it
            while (context.getIndex() != i && i < statements.size()) {
                context = new StatementContext(statements, i);
                dispatch(context);
            }
        }
    }

    public void visit(Program program) {
        dispatchAll(program);
        program.findAllLocalScope(in(SymbolType.FUNCTION,SymbolType.SUBROUTINE)).forEach(symbol -> {
            dispatch(symbol.scope());
        });
    }
    public void visit(Subroutine subroutine) {
        dispatchAll(subroutine);
    }
    public void visit(Function function) {
        dispatchAll(function);
    }

    public void visit(AssignmentStatement statement, StatementContext context) {
        var expr = dispatch(statement.getExpr());
        expr.ifPresent(statement::setExpr);
    }

    public void visit(EndStatement statement, StatementContext context) {
    }

    public void visit(CallStatement statement, StatementContext context) {
        var expr = dispatch(statement.getExpr());
        expr.ifPresent(statement::setExpr);
    }

    public void visit(IfStatement statement, StatementContext context) {
        var expr = dispatch(statement.getExpression());
        dispatchAll(statement.getTrueStatements());
        if (statement.hasFalseStatements()) {
            dispatchAll(statement.getFalseStatements());
        }
        expr.ifPresent(statement::setExpression);
    }

    public void visit(PokeStatement statement, StatementContext context) {
        var a = dispatch(statement.getA());
        var b = dispatch(statement.getB());
        a.ifPresent(statement::setA);
        b.ifPresent(statement::setB);
    }

    public void visit(GotoGosubStatement statement, StatementContext context) {

    }

    public void visit(DynamicGotoGosubStatement statement, StatementContext context) {
        var target = dispatch(statement.getTarget());
        target.ifPresent(statement::setTarget);
    }

    public void visit(LabelStatement statement, StatementContext context) {

    }

    public void visit(ReturnStatement statement, StatementContext context) {
        var expr = dispatch(statement.getExpr());
        expr.ifPresent(statement::setExpr);
    }

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

    public void visit(PopStatement statement, StatementContext context) {

    }

    public void visit(OnErrorStatement statement, StatementContext context) {

    }

    public void visit(RaiseErrorStatement statement, StatementContext context) {

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

    public Expression visit(AddressOfFunction expression) {
        return null;
    }
}
