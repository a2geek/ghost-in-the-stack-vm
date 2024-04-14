package a2geek.ghost.model.visitor;

import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.*;

import java.util.*;
import java.util.stream.Collectors;

import static a2geek.ghost.model.Symbol.in;

public class InliningVisitor extends Visitor implements RepeatingVisitor {
    private int counter = 0;
    private VisitorContext statementContext = null;

    @Override
    public int getCounter() {
        return counter;
    }

    @Override
    public void dispatch(VisitorContext context) {
        // preserving a statement context for the function processing
        statementContext = context;
        super.dispatch(context);
        statementContext = null;
    }

    @Override
    public void visit(Subroutine subroutine) {
        if (subroutine.is(Subroutine.Modifier.INLINE)) {
            // skip inlining of the inlined subroutine
        }
        else {
            super.visit(subroutine);
        }
    }

    @Override
    public void visit(Function function) {
        if (function.is(Subroutine.Modifier.INLINE)) {
            // skip inlining of the inlined function
        }
        else {
            super.visit(function);
        }
    }

    @Override
    public void visit(CallSubroutine statement, VisitorContext context) {
        if (statement.getSubroutine().is(Subroutine.Modifier.INLINE)) {
            counter += 1;
            var visitor = new RewriteVisitor(statement, context);
            visitor.apply(context);
        }
        else {
            super.visit(statement, context);
        }
    }

    @Override
    public Expression visit(FunctionExpression expression) {
        if (expression.getFunction() != null && expression.getFunction().is(Subroutine.Modifier.INLINE)) {
            counter += 1;
            var visitor = new RewriteVisitor(expression, statementContext);
            return visitor.apply(statementContext);
        }
        else {
            return super.visit(expression);
        }
    }

    private static class RewriteVisitor extends DispatchVisitor {
        private final Stack<StatementBlock> statementBlocks = new Stack<>();
        private final Scope sourceScope;
        private final Scope targetScope;
        private final Map<Symbol,Expression> replacements = new HashMap<>();
        private final Symbol returnValue;
        private final Symbol returnLabel;

        private RewriteVisitor(Scope sourceScope, Scope targetScope, List<Expression> methodParameters) {
            this.sourceScope = sourceScope;
            this.targetScope = targetScope;
            // Assign replacement values for incoming symbols
            var parameters = sourceScope.findAllLocalScope(in(SymbolType.PARAMETER));
            if (parameters.size() != methodParameters.size()) {
                throw new RuntimeException(String.format("parameter size mismatch for call to '%s'", sourceScope.getFullPathName()));
            }
            // Subroutine parameters are REVERSED (for stack placement), so taking that into account:
            parameters = parameters.reversed();
            for (int i=0; i<parameters.size(); i++) {
                var param = parameters.get(i);
                var value = methodParameters.get(i);
                if (param.numDimensions() > 0) {
                    throw new RuntimeException("parameter inlining not available for arrays: " + param.name());
                }
                replacements.put(param, value);
            }
            // Setup return value
            returnValue = sourceScope.findFirstLocalScope(in(SymbolType.RETURN_VALUE))
                    .map(Symbol::dataType)
                    .map(targetScope::addTempVariable)
                    .orElse(null);
            // Target for return statements
            returnLabel = targetScope.addLabels(String.format("%sEXIT", sourceScope.getName())).getFirst();
        }
        public RewriteVisitor(CallSubroutine statement, VisitorContext context) {
            this(statement.getSubroutine(), context.getScope(), statement.getParameters());
        }
        public RewriteVisitor(FunctionExpression expression, VisitorContext context) {
            this(expression.getFunction(), context.getScope(), expression.getParameters());
        }

        public StatementBlock pushStatementBlock() {
            return statementBlocks.push(new StatementBlock());
        }
        public StatementBlock popStatementBlock() {
            return statementBlocks.pop();
        }
        public void addStatement(Statement statement) {
            this.statementBlocks.peek().addStatement(statement);
        }

        public Expression apply(VisitorContext context) {
            pushStatementBlock();
            dispatch(sourceScope);
            // always add the exit label; assume we will remove it in a later code pass
            addStatement(new LabelStatement(returnLabel));
            context.insertAllBefore(statementBlocks.peek());
            // for function, we are just replacing the function call with the temp variable
            if (returnValue != null) {
                return VariableReference.with(returnValue);
            }
            // for a sub we are removing the original call since the new code has been inserted
            else {
                context.deleteStatement();
                return null;
            }
        }

        public List<Expression> dispatchAll(List<Expression> original) {
            return original.stream().map(this::dispatch).map(Optional::orElseThrow).collect(Collectors.toList());
        }

        public void visit(IfStatement statement, VisitorContext context) {
            var expr = dispatch(statement.getExpression()).orElseThrow();
            StatementBlock trueStatements = pushStatementBlock();
            dispatchAll(context, statement.getTrueStatements());
            popStatementBlock();
            StatementBlock falseStatements = null;
            if (statement.hasFalseStatements()) {
                falseStatements = pushStatementBlock();
                dispatchAll(context, statement.getFalseStatements());
                popStatementBlock();
            }
            addStatement(new IfStatement(expr, trueStatements, falseStatements));
        }

        @Override
        public void visit(EndStatement statement, VisitorContext context) {
            addStatement(new EndStatement());
        }

        @Override
        public void visit(PopStatement statement, VisitorContext context) {
            addStatement(new PopStatement());
        }

        @Override
        public void visit(CallStatement statement, VisitorContext context) {
            var expr = dispatch(statement.getExpr()).orElseThrow();
            addStatement(new CallStatement(expr));
        }

        @Override
        public void visit(CallSubroutine statement, VisitorContext context) {
            var params = dispatchAll(statement.getParameters());
            addStatement(new CallSubroutine(statement.getSubroutine(), params));
        }

        @Override
        public void visit(LabelStatement statement, VisitorContext context) {
            addStatement(new LabelStatement(statement.getLabel()));
        }

        @Override
        public void visit(ReturnStatement statement, VisitorContext context) {
            if (statement.getExpr() != null) {
                if (returnValue == null) {
                    var msg = String.format("return value mismatch: '%s' is not in a function", statement);
                    throw new RuntimeException(msg);
                }
                var expr = dispatch(statement.getExpr()).orElseThrow();
                addStatement(AssignmentStatement.create(VariableReference.with(returnValue), expr));
            }
            addStatement(new GotoGosubStatement("goto", returnLabel));
        }

        @Override
        public void visit(OnErrorStatement statement, VisitorContext context) {
            addStatement(new OnErrorStatement(statement.getLabel()));
        }

        @Override
        public void visit(RaiseErrorStatement statement, VisitorContext context) {
            addStatement(new RaiseErrorStatement());
        }

        @Override
        public void visit(GotoGosubStatement statement, VisitorContext context) {
            addStatement(new GotoGosubStatement(statement.getOp(), statement.getLabel()));
        }

        @Override
        public void visit(AssignmentStatement statement, VisitorContext context) {
            var lhs = dispatch(statement.getVar()).orElseThrow();
            var expr = dispatch(statement.getValue()).orElseThrow();
            addStatement(AssignmentStatement.create(lhs, expr));
        }

        @Override
        public void visit(DynamicGotoGosubStatement statement, VisitorContext context) {
            var expr = dispatch(statement.getTarget()).orElseThrow();
            addStatement(new DynamicGotoGosubStatement(statement.getOp(), expr, statement.needsAddressAdjustment()));
        }

        @Override
        public Expression visit(VariableReference expression) {
            return replacements.getOrDefault(expression.getSymbol(), expression);
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
        public Expression visit(ByteConstant expression) {
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
        public Expression visit(TypeConversionOperator expression) {
            var expr = dispatch(expression.getExpr());
            return new TypeConversionOperator(expr.orElseThrow(), expression.getType());
        }

        @Override
        public Expression visit(DereferenceOperator expression) {
            var expr = dispatch(expression.getExpr());

            return new DereferenceOperator(expr.orElseThrow(), expression.getType());
        }

        @Override
        public Expression visit(FunctionExpression expression) {
            var params = dispatchAll(expression.getParameters());
            if (expression.getFunction() != null) {
                return new FunctionExpression(expression.getFunction(), params);
            }
            else {
                return new FunctionExpression(expression.getName(), params);
            }
        }

        @Override
        public Expression visit(ArrayLengthFunction expression) {
            if (replacements.containsKey(expression.getSymbol())) {
                var replacement = replacements.get(expression.getSymbol());
                if (replacement instanceof VariableReference ref) {
                    var symbol = ref.getSymbol();
                    if (symbol.numDimensions() > 0) {
                        return new ArrayLengthFunction(symbol, expression.getDimensionNumber());
                    }
                }
                var msg = String.format("unable to combine '%s' and '%s'", expression, replacement);
                throw new RuntimeException(msg);
            }
            return expression;
        }

        @Override
        public Expression visit(AddressOfOperator expression) {
            if (replacements.containsKey(expression.getSymbol())) {
                var replacement = replacements.get(expression.getSymbol());
                if (replacement instanceof VariableReference ref) {
                    var symbol = ref.getSymbol();
                    if (symbol.numDimensions() > 0) {
                        return new AddressOfOperator(symbol);
                    }
                }
                var msg = String.format("unable to combine '%s' and '%s'", expression, replacement);
                throw new RuntimeException(msg);
            }
            return expression;
        }

        @Override
        public Expression visit(PlaceholderExpression expression) {
            return null;
        }
    }
}
