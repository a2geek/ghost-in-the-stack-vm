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
                        case UnaryExpression unaryExpression -> {
                            var expr2 = capture(tracker, unaryExpression.getExpr(), i);
                            if (expr == null) expr = expr2;
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
                case DynamicGotoGosubStatement dynamicGotoGosubStatement -> capture(tracker, dynamicGotoGosubStatement.getTarget(), i);
                case EndStatement ignored -> null;
                case GotoGosubStatement ignored -> null;
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
                case PokeStatement pokeStatement -> captureParams(tracker, List.of(pokeStatement.getA(), pokeStatement.getB()), i);
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
                        case UnaryExpression unaryExpression -> unaryExpression.setExpr(replace(unaryExpression.getExpr(), candidate, replacement));
                        case VariableReference variableReference -> {
                            // once the value changes, we stop processing
                            if (ExpressionTracker.has(candidate, variableReference.getSymbol())) return;
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
                }
                case EndStatement ignored -> {}
                case GotoGosubStatement ignored -> {}
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
                case PokeStatement pokeStatement -> {
                    pokeStatement.setA(replace(pokeStatement.getA(), candidate, replacement));
                    pokeStatement.setB(replace(pokeStatement.getB(), candidate, replacement));
                }
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
        if (!hasVolatileFunction(expression) && weight(expression) > 1 && tracker.capture(expression, n)) {
            return expression;
        }
        return switch (expression) {
            case AddressOfFunction ignored -> null;
            case ArrayLengthFunction ignored -> null;
            case BinaryExpression binaryExpression -> captureParams(tracker, List.of(binaryExpression.getL(), binaryExpression.getR()), n);
            case BooleanConstant ignored -> null;
            case FunctionExpression functionExpression -> captureParams(tracker, functionExpression.getParameters(), n);
            case IntegerConstant ignored -> null;
            case PlaceholderExpression ignored -> null;
            case StringConstant ignored -> null;
            case UnaryExpression unaryExpression -> capture(tracker, unaryExpression.getExpr(), n);
            case VariableReference ignored -> null;
            default -> throw new RuntimeException("[compiler bug] unexpected expression: " + expression);
        };
    }

    /**
     * Recursively look for a volatile function. We don't want to optimize functions like
     * RND, ALLOC, etc. This is either marked in the function definition or in the intrinsic
     * definition.
     */
    public boolean hasVolatileFunction(Expression expression) {
        return switch (expression) {
            case AddressOfFunction ignored -> false;
            case ArrayLengthFunction ignored -> false;
            case BinaryExpression binaryExpression -> hasVolatileFunction(binaryExpression.getL()) || hasVolatileFunction(binaryExpression.getR());
            case BooleanConstant ignored -> false;
            case FunctionExpression functionExpression -> {
                if (functionExpression.isVolatile()) {
                    yield true;
                }
                yield functionExpression.getParameters().stream().map(this::hasVolatileFunction).reduce(Boolean::logicalOr).orElse(false);
            }
            case IntegerConstant ignored -> false;
            case PlaceholderExpression ignored -> false;
            case StringConstant ignored -> false;
            case UnaryExpression unaryExpression -> hasVolatileFunction(unaryExpression.getExpr());
            case VariableReference ignored -> false;
            default -> throw new RuntimeException("[compiler bug] unexpected expression: " + expression);
        };
    }

    /**
     * Calculate the "weight" of an expression. Used to determine if this expression should be
     * considered for optimizations. Note that simple constants and variable references are a 1;
     * everything else will be larger. Hypothetically, this could be used to prioritize which
     * expressions are optimized first, should that become useful.
     */
    public int weight(Expression expression) {
        return switch (expression) {
            case AddressOfFunction ignored -> 1;
            case ArrayLengthFunction ignored -> 2;
            case BinaryExpression binaryExpression -> weight(binaryExpression.getL()) + weight(binaryExpression.getR());
            case BooleanConstant ignored -> 1;
            case FunctionExpression functionExpression -> 2 + functionExpression.getParameters().stream().mapToInt(this::weight).sum();
            case IntegerConstant ignored -> 1;
            case PlaceholderExpression ignored -> 1;
            case StringConstant ignored -> 1;
            case UnaryExpression unaryExpression -> 1 + weight(unaryExpression.getExpr());
            case VariableReference ignored -> 1;
            default -> throw new RuntimeException("[compiler bug] unexpected expression: " + expression);
        };
    }
}
