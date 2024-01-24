package a2geek.ghost.model.visitor;

import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static a2geek.ghost.model.Symbol.in;

/**
 * Provide inlining capability for a SUB or FUNCTION.
 * This handled by literally rewriting the method as if it were a macro.
 * Every type of statement must be handled to ensure it gets added into the model.
 * Note that the <code>Optional&lt;Expression&gt;</code> are assumed to be present;
 * partly to double-check that all expressions got rewritten as well.
 */
public class InliningVisitor extends DispatchVisitor {
    private final ModelBuilder model;
    private final Subroutine sub;
    private final Map<Symbol,Expression> replacements = new HashMap<>();
    private final Symbol returnValue;
    private final Symbol returnLabel;

    public InliningVisitor(ModelBuilder model, Subroutine sub, List<Expression> params) {
        this.model = model;
        this.sub = sub;
        // Assign replacement values for incoming symbols
        var parameters = sub.findAllLocalScope(in(SymbolType.PARAMETER));
        if (parameters.size() != params.size()) {
            throw new RuntimeException(String.format("parameter size mismatch for call to '%s'", sub.getFullPathName()));
        }
        // Subroutine parameters are REVERSED (for stack placement), so taking that into account:
        parameters = parameters.reversed();
        for (int i=0; i<parameters.size(); i++) {
            var param = parameters.get(i);
            var value = params.get(i);
            if (param.numDimensions() > 0) {
                throw new RuntimeException("parameter inlining not available for arrays: " + param.name());
            }
            replacements.put(param, value);
        }
        // Setup return value
        DataType dt = sub.findFirstLocalScope(in(SymbolType.RETURN_VALUE)).map(Symbol::dataType).orElse(null);
        returnValue = dt == null ? null : model.addTempVariable(dt);
        // Target for return statements
        returnLabel = model.addLabels(String.format("%sEXIT", sub.getName())).getFirst();
    }

    public Symbol getReturnValue() {
        return returnValue;
    }

    public void inline() {
        visit(sub);
        model.labelStmt(returnLabel);
    }

    public List<Expression> dispatchAll(List<Expression> original) {
        return original.stream().map(this::dispatch).map(Optional::orElseThrow).collect(Collectors.toList());
    }

    @Override
    public void visit(IfStatement statement, StatementContext context) {
        var expr = dispatch(statement.getExpression());
        StatementBlock trueStatements = model.pushStatementBlock(new StatementBlock());
        dispatchAll(statement.getTrueStatements());
        model.popStatementBlock();
        StatementBlock falseStatements = null;
        if (statement.hasFalseStatements()) {
            falseStatements = model.pushStatementBlock(new StatementBlock());
            dispatchAll(statement.getFalseStatements());
            model.popStatementBlock();
        }
        model.ifStmt(expr.orElseThrow(), trueStatements, falseStatements);
    }

    @Override
    public void visit(EndStatement statement, StatementContext context) {
        model.endStmt();
    }

    @Override
    public void visit(PopStatement statement, StatementContext context) {
        model.addStatement(new PopStatement());
    }

    @Override
    public void visit(CallStatement statement, StatementContext context) {
        var expr = dispatch(statement.getExpr());
        model.callAddr(expr.orElseThrow());
    }

    @Override
    public void visit(PokeStatement statement, StatementContext context) {
        var a = dispatch(statement.getA());
        var b = dispatch(statement.getB());
        model.pokeStmt(statement.getOp(), a.orElseThrow(), b.orElseThrow());
    }

    @Override
    public void visit(CallSubroutine statement, StatementContext context) {
        var params = dispatchAll(statement.getParameters());
        model.callSubroutine(statement.getSubroutine().getFullPathName(), params);
    }

    @Override
    public void visit(LabelStatement statement, StatementContext context) {
        model.labelStmt(statement.getLabel());
    }

    @Override
    public void visit(ReturnStatement statement, StatementContext context) {
        if (statement.getExpr() != null) {
            if (returnValue == null) {
                var msg = String.format("return value mismatch: '%s' is not in a function", statement);
                throw new RuntimeException(msg);
            }
            model.assignStmt(VariableReference.with(returnValue), dispatch(statement.getExpr()).orElseThrow());
        }
        model.gotoGosubStmt("goto", returnLabel);
    }

    @Override
    public void visit(OnErrorStatement statement, StatementContext context) {
        // TODO do we need to do more here?
        model.addStatement(statement);
    }

    @Override
    public void visit(RaiseErrorStatement statement, StatementContext context) {
        // TODO do we need to do more here?
        model.addStatement(statement);
    }

    @Override
    public void visit(GotoGosubStatement statement, StatementContext context) {
        model.gotoGosubStmt(statement.getOp(), statement.getLabel());
    }

    @Override
    public void visit(AssignmentStatement statement, StatementContext context) {
        // this has to be a VariableReference on the LHS, right?
        var ref = dispatch(statement.getVar()).map(VariableReference.class::cast);
        var expr = dispatch(statement.getExpr());
        model.assignStmt(ref.orElseThrow(), expr.orElseThrow());
    }

    @Override
    public void visit(DynamicGotoGosubStatement statement, StatementContext context) {
        var expr = dispatch(statement.getTarget());
        model.dynamicGotoGosubStmt(statement.getOp(), expr.orElseThrow(), statement.needsAddressAdjustment());
    }

    @Override
    public Expression visit(VariableReference expression) {
        if (expression.isArray()) {
            var symbol = expression.getSymbol();
            List<Expression> indexes = dispatchAll(expression.getIndexes());
            if (replacements.containsKey(symbol)) {
                var replacement = replacements.get(symbol);
                if (replacement instanceof VariableReference ref) {
                    symbol = ref.getSymbol();
                    if (!ref.isArray() && symbol.numDimensions() > 0) {
                        return new VariableReference(symbol, indexes);
                    }
                }
                var msg = String.format("unable to combine '%s' and '%s'", expression, replacement);
                throw new RuntimeException(msg);
            }
            else {
                return new VariableReference(symbol, indexes);
            }
        }
        else {
            return replacements.getOrDefault(expression.getSymbol(), expression);
        }
    }

    @Override
    public Expression visit(BinaryExpression expression) {
        var l = dispatch(expression.getL()).orElseThrow();
        var r = dispatch(expression.getR()).orElseThrow();
        return new BinaryExpression(l, r, expression.getOp());
    }

    @Override
    public Expression visit(IntegerConstant expression) {
        return expression;
    }

    @Override
    public Expression visit(StringConstant expression) {
        return expression;
    }

    @Override
    public Expression visit(BooleanConstant expression) {
        return expression;
    }

    @Override
    public Expression visit(UnaryExpression expression) {
        var expr = dispatch(expression.getExpr());
        return new UnaryExpression(expression.getOp(), expr.orElseThrow());
    }

    @Override
    public Expression visit(FunctionExpression expression) {
        var params = dispatchAll(expression.getParameters());
        if (expression.getFunction() != null) {
            return model.callFunction(expression.getFunction().getFullPathName(), params);
        }
        else {
            return model.callFunction(expression.getName(), params);
        }
    }

    @Override
    public Expression visit(ArrayLengthFunction expression) {
        if (replacements.containsKey(expression.getSymbol())) {
            var replacement = replacements.get(expression.getSymbol());
            if (replacement instanceof VariableReference ref) {
                var symbol = ref.getSymbol();
                if (!ref.isArray() && symbol.numDimensions() > 0) {
                    return new ArrayLengthFunction(model, symbol);
                }
            }
            var msg = String.format("unable to combine '%s' and '%s'", expression, replacement);
            throw new RuntimeException(msg);
        }
        return expression;
    }

    @Override
    public Expression visit(AddressOfFunction expression) {
        if (replacements.containsKey(expression.getSymbol())) {
            var replacement = replacements.get(expression.getSymbol());
            if (replacement instanceof VariableReference ref) {
                var symbol = ref.getSymbol();
                if (!ref.isArray() && symbol.numDimensions() > 0) {
                    return new AddressOfFunction(symbol);
                }
            }
            var msg = String.format("unable to combine '%s' and '%s'", expression, replacement);
            throw new RuntimeException(msg);
        }
        return expression;
    }
}
