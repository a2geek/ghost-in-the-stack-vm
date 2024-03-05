package a2geek.ghost.model.visitor;

import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.statement.*;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static a2geek.ghost.model.Symbol.in;

public class SubexpressionEliminationVisitor implements ProgramVisitor {
    public void visit(Program program) {
        scan(program::addTempVariable, new ExpressionTracker(), program.getStatements());
        program.findAllLocalScope(in(SymbolType.SUBROUTINE, SymbolType.FUNCTION)).forEach(symbol -> {
            scan(symbol.scope()::addTempVariable, new ExpressionTracker(), symbol.scope().getStatements());
        });
    }

    public void scan(Function<DataType,Symbol> makeTempVariable, ExpressionTracker tracker, List<Statement> statements) {
        for (int i=0; i<statements.size(); i++) {
            Expression candidate = switch (statements.get(i)) {
                case AssignmentStatement assignmentStatement -> {
                    var expr = capture(tracker, assignmentStatement.getValue(), i);
                    switch (assignmentStatement.getVar()) {
                        case DereferenceOperator dereferenceOperator -> {
                            var expr2 = capture(tracker, dereferenceOperator.getExpr(), i);
                            if (expr == null) expr = expr2;
                            tracker.changed(dereferenceOperator);   // mark the assignment itself as changed
                        }
                        case VariableReference variableReference -> {
                            tracker.changed(variableReference.getSymbol());
                        }
                        default -> throw new RuntimeException("[compiler bug] unexpected LHS of assignment statement: " + assignmentStatement);
                    }
                    yield expr;
                }
                case CallStatement callStatement -> capture(tracker, callStatement.getExpr(), i);
                case CallSubroutine callSubroutine -> captureParams(tracker, callSubroutine.getParameters(), i);
                case DynamicGotoGosubStatement dynamicGotoGosubStatement -> {
                    var expr = capture(tracker, dynamicGotoGosubStatement.getTarget(), i);
                    if ("gosub".equals(dynamicGotoGosubStatement.getOp())) tracker.reset();
                    yield expr;
                }
                case EndStatement ignored -> null;
                case GotoGosubStatement gotoGosubStatement -> {
                    if ("gosub".equals(gotoGosubStatement.getOp())) tracker.reset();
                    yield null;
                }
                case IfStatement ifStatement -> {
                    if (ifStatement.hasTrueStatements()) scan(makeTempVariable, new ExpressionTracker(), ifStatement.getTrueStatements().getStatements());
                    if (ifStatement.hasFalseStatements()) scan(makeTempVariable, new ExpressionTracker(), ifStatement.getFalseStatements().getStatements());
                    yield capture(tracker, ifStatement.getExpression(), i);
                }
                case LabelStatement ignored -> {
                    tracker.reset();
                    yield null;
                }
                case OnErrorStatement ignored -> null;
                case PopStatement ignored -> null;
                case RaiseErrorStatement ignored -> null;
                case ReturnStatement returnStatement -> returnStatement.getExpr() == null ? null : capture(tracker, returnStatement.getExpr(), i);
                default -> throw new RuntimeException("[compiler bug] unexpected statement type: " + statements.get(i));
            };
            // Loop variables can trigger optimization and then get removed due to assignment (like "I = I + 1")
            if (candidate != null && tracker.exists(candidate)) {
                var replacement = makeTempVariable.apply(candidate.getType());
                var n = tracker.remove(candidate);
                replace(replacement, statements.subList(n, statements.size()), candidate);
                statements.add(n, new AssignmentStatement(VariableReference.with(replacement), candidate));
                i = -1;  // rewind and try again
                tracker.reset();
            }
        }
    }

    public void replace(Symbol replacement, List<Statement> statements, Expression candidate) {
        for (var statement : statements) {
            switch (statement) {
                case AssignmentStatement assignmentStatement -> {
                    assignmentStatement.setValue(replace(assignmentStatement.getValue(), candidate, replacement));
                    switch (assignmentStatement.getVar()) {
                        case DereferenceOperator dereferenceOperator -> {
                            dereferenceOperator.setExpr(replace(dereferenceOperator.getExpr(), candidate, replacement));
                            // this is an assignment to an array reference, time to exit
                            if (ExpressionVisitors.hasSubexpression(candidate, dereferenceOperator)) return;
                        }
                        case VariableReference variableReference -> {
                            // once the value changes, we stop processing
                            if (ExpressionVisitors.hasSymbol(candidate, variableReference.getSymbol())) return;
                        }
                        default -> throw new RuntimeException("[compiler bug] unexpected LHS of assignment statement: " + assignmentStatement);
                    }
                }
                case CallStatement callStatement -> {
                    callStatement.setExpr(replace(callStatement.getExpr(), candidate, replacement));
                }
                case CallSubroutine callSubroutine -> {
                    callSubroutine.getParameters().replaceAll(expression -> replace(expression, candidate, replacement));
                }
                case DynamicGotoGosubStatement dynamicGotoGosubStatement -> {
                    dynamicGotoGosubStatement.setTarget(replace(dynamicGotoGosubStatement.getTarget(), candidate, replacement));
                    if ("gosub".equals(dynamicGotoGosubStatement.getOp())) return;
                }
                case EndStatement ignored -> {}
                case GotoGosubStatement gotoGosubStatement -> {
                    if ("gosub".equals(gotoGosubStatement.getOp())) return;
                }
                case IfStatement ifStatement -> {
                    ifStatement.setExpression(replace(ifStatement.getExpression(), candidate, replacement));
                    if (ifStatement.hasTrueStatements()) {
                        replace(replacement, ifStatement.getTrueStatements().getStatements(), candidate);
                    }
                    if (ifStatement.hasFalseStatements()) {
                        replace(replacement, ifStatement.getFalseStatements().getStatements(), candidate);
                    }
                }
                case LabelStatement ignored -> {
                    return;
                }
                case OnErrorStatement ignored -> {}
                case PopStatement ignored -> {}
                case RaiseErrorStatement ignored -> {}
                case ReturnStatement returnStatement -> {
                    if (returnStatement.getExpr() != null) {
                        returnStatement.setExpr(replace(returnStatement.getExpr(), candidate, replacement));
                    }
                }
                default -> throw new RuntimeException("[compiler bug] unexpected statement type: " + statement);
            }
        }
    }

    /**
     * Replace expressions that match our candidate. Note that the list of what is processed
     * recursively is limited to expression types that have sub expressions.
     */
    public Expression replace(Expression original, Expression candidate, Symbol replacement) {
        if (Objects.equals(original, candidate)) {
            return VariableReference.with(replacement);
        }
        if (original instanceof BinaryExpression binaryExpression) {
            binaryExpression.setL(replace(binaryExpression.getL(), candidate, replacement));
            binaryExpression.setR(replace(binaryExpression.getR(), candidate, replacement));
        }
        else if (original instanceof UnaryExpression unaryExpression) {
            unaryExpression.setExpr(replace(unaryExpression.getExpr(), candidate, replacement));
        }
        else if (original instanceof FunctionExpression functionExpression) {
            functionExpression.getParameters().replaceAll(expression -> replace(expression, candidate, replacement));
        }
        else if (original instanceof DereferenceOperator dereferenceOperator) {
            dereferenceOperator.setExpr(replace(dereferenceOperator.getExpr(), candidate, replacement));
        }
        return original;
    }

    /**
     * Capture and return any non-null duplicate expressions that were found.
     */
    public Expression captureParams(ExpressionTracker tracker, List<Expression> params, int n) {
        return params.stream()
            .map(param -> capture(tracker, param, n))
            .filter(Objects::nonNull)
            .findAny()
            .orElse(null);
    }

    /**
     * Recursively search for an expression we've already found. If no duplicate found, return null.
     */
    public Expression capture(ExpressionTracker tracker, Expression expression, int n) {
        if (!ExpressionVisitors.hasVolatileFunction(expression) && ExpressionVisitors.weight(expression) > 1 && tracker.capture(expression, n)) {
            return expression;
        }
        return switch (expression) {
            case AddressOfOperator ignored -> null;
            case ArrayLengthFunction ignored -> null;
            case BinaryExpression binaryExpression -> captureParams(tracker, List.of(binaryExpression.getL(), binaryExpression.getR()), n);
            case BooleanConstant ignored -> null;
            case ByteConstant ignored -> null;
            case DereferenceOperator dereferenceOperator -> capture(tracker, dereferenceOperator.getExpr(), n);
            case FunctionExpression functionExpression -> captureParams(tracker, functionExpression.getParameters(), n);
            case IntegerConstant ignored -> null;
            case PlaceholderExpression ignored -> null;
            case StringConstant ignored -> null;
            case UnaryExpression unaryExpression -> capture(tracker, unaryExpression.getExpr(), n);
            case VariableReference ignored -> null;
            default -> throw new RuntimeException("[compiler bug] unexpected expression: " + expression);
        };
    }
}
