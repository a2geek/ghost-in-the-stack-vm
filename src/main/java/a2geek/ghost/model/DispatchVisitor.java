package a2geek.ghost.model;

import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.*;

import java.util.List;
import java.util.Optional;

import static a2geek.ghost.model.Symbol.in;

/**
 * A visitor pattern with only the dispatch methods implemented.
 * @see Visitor a class with default visit methods
 */
public abstract class DispatchVisitor<E> implements ProgramVisitor {
    public void dispatch(Scope scope) {
        switch (scope) {
            case Program s -> visit(s);
            case Function s -> visit(s);
            case Subroutine s -> visit(s);
            default -> throw new RuntimeException("scope type not supported: " +
                    scope.getClass().getName());
        }
    }
    public void dispatch(VisitorContext context) {
        var statement = context.currentStatement();
        switch (statement) {
            case AssignmentStatement s -> visit(s, context);
            case EndStatement s -> visit(s, context);
            case IfStatement s -> visit(s, context);
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
    public Optional<E> dispatch(Expression expression) {
        return switch (expression) {
            // This occurs when the expression is optional, such as the RETURN statement.
            // Catching it here instead of testing everywhere else as issues are discovered.
            case null -> Optional.empty();
            case AddressOfOperator e -> Optional.ofNullable(visit(e));
            case ArrayLengthFunction e -> Optional.ofNullable(visit(e));
            case BinaryExpression e -> Optional.ofNullable(visit(e));
            case BooleanConstant e -> Optional.ofNullable(visit(e));
            case ByteConstant e -> Optional.ofNullable(visit(e));
            case DereferenceOperator e -> Optional.ofNullable(visit(e));
            case FunctionExpression e -> Optional.ofNullable(visit(e));
            case IfExpression e -> Optional.ofNullable(visit(e));
            case IntegerConstant e -> Optional.ofNullable(visit(e));
            case PlaceholderExpression e -> Optional.ofNullable(visit(e));
            case StringConstant e -> Optional.ofNullable(visit(e));
            case TypeConversionOperator e -> Optional.ofNullable(visit(e));
            case UnaryExpression e -> Optional.ofNullable(visit(e));
            case VariableReference e -> Optional.ofNullable(visit(e));
            default -> throw new RuntimeException("expression type not supported: " +
                            expression.getClass().getName());
        };
    }

    public void dispatchAll(VisitorContext context, StatementBlock block) {
        dispatchAll(context.getScope(), block);
    }
    public void dispatchAll(Scope scope, StatementBlock block) {
        dispatchToList(scope, block.getInitializationStatements());
        dispatchToList(scope, block.getStatements());
    }
    public void dispatchToList(Scope scope, List<Statement> statements) {
        for (int i=0; i<statements.size(); i++) {
            VisitorContext context = new VisitorContext(scope, statements, i);
            dispatch(context);
            // If we insert or delete a row, we should reprocess it
            while (context.getIndex() != i && i < statements.size()) {
                context = new VisitorContext(scope, statements, i);
                dispatch(context);
            }
        }
    }

    @Override
    public void visit(Program program) {
        dispatchAll(program, program);
        program.findAllLocalScope(in(SymbolType.FUNCTION,SymbolType.SUBROUTINE)).forEach(symbol -> {
            dispatch(symbol.scope());
        });
    }
    public void visit(Subroutine subroutine) {
        dispatchAll(subroutine, subroutine);
    }
    public void visit(Function function) {
        dispatchAll(function, function);
    }

    public abstract void visit(AssignmentStatement statement, VisitorContext context);
    public abstract void visit(EndStatement statement, VisitorContext context);
    public abstract void visit(CallStatement statement, VisitorContext context);
    public abstract void visit(IfStatement statement, VisitorContext context);
    public abstract void visit(GotoGosubStatement statement, VisitorContext context);
    public abstract void visit(DynamicGotoGosubStatement statement, VisitorContext context);
    public abstract void visit(LabelStatement statement, VisitorContext context);
    public abstract void visit(ReturnStatement statement, VisitorContext context);
    public abstract void visit(CallSubroutine statement, VisitorContext context);
    public abstract void visit(PopStatement statement, VisitorContext context);
    public abstract void visit(OnErrorStatement statement, VisitorContext context);
    public abstract void visit(RaiseErrorStatement statement, VisitorContext context);

    public abstract E visit(AddressOfOperator expression);
    public abstract E visit(ArrayLengthFunction expression);
    public abstract E visit(BinaryExpression expression);
    public abstract E visit(BooleanConstant expression);
    public abstract E visit(ByteConstant expression);
    public abstract E visit(DereferenceOperator expression);
    public abstract E visit(FunctionExpression expression);
    public abstract E visit(IfExpression expression);
    public abstract E visit(IntegerConstant expression);
    public abstract E visit(PlaceholderExpression expression);
    public abstract E visit(StringConstant expression);
    public abstract E visit(TypeConversionOperator expression);
    public abstract E visit(UnaryExpression expression);
    public abstract E visit(VariableReference expression);
}
