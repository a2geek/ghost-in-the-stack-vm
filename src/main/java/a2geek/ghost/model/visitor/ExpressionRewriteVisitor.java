package a2geek.ghost.model.visitor;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Visitor;
import a2geek.ghost.model.expression.BinaryExpression;
import a2geek.ghost.model.expression.IntegerConstant;
import a2geek.ghost.model.expression.UnaryExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ExpressionRewriteVisitor extends Visitor {
    @Override
    public Expression visit(BinaryExpression expression) {
        Set<String> commutativeAndAssociativeOperators = Set.of("+","*");
        if (commutativeAndAssociativeOperators.contains(expression.getOp())) {
            var visitor = new BinaryExpressionVisitor(expression.getType(), expression.getOp());
            visitor.visit(expression);
            // only rewrite if we actually gathered more than 1 constant
            if (visitor.getConstantCounter() > 1 && !visitor.getNonConstants().isEmpty()) {
                var nonConstants = visitor.getNonConstants().stream()
                        .reduce((a,b) -> new BinaryExpression(expression.getOp(), a, b))
                        .orElseThrow();
                return switch(expression.getOp()) {
                    case "+" -> nonConstants.plus(new IntegerConstant(visitor.getConstantTotal()));
                    case "*" -> nonConstants.times(new IntegerConstant(visitor.getConstantTotal()));
                    default -> throw new RuntimeException("unexpected op: " + expression.getOp());
                };
            }
        }
        // check additive operators combined with unary minus - note we have to check numerical constants as well
        if ("+".equals(expression.getOp())) {
            if (expression.getL() instanceof UnaryExpression ul && ul.getOp().equals("-")) {
                // -a + b => b - a
                var a = ul.getExpr();
                var b = expression.getR();
                return new BinaryExpression(b, a, "-");
            }
            else if (expression.getR() instanceof UnaryExpression ur && ur.getOp().equals("-")) {
                // a + -b => a - b
                var a = expression.getL();
                var b = ur.getExpr();
                return new BinaryExpression(a, b, "-");
            }
            else if (expression.getL() instanceof IntegerConstant il && il.getValue() < 0) {
                // -# + b => b - #
                var a = new IntegerConstant(-il.getValue());
                var b = expression.getR();
                return new BinaryExpression(b, a, "-");
            }
            else if (expression.getR() instanceof IntegerConstant ir && ir.getValue() < 0) {
                // a + -# => a - #
                var a = expression.getL();
                var b = new IntegerConstant(-ir.getValue());
                return new BinaryExpression(a, b, "-");
            }

        }
        else if ("-".equals(expression.getOp())) {
            if (expression.getR() instanceof IntegerConstant ir && ir.getValue() < 0) {
                // a - -# => a + #
                var a = expression.getL();
                var b = new IntegerConstant(-ir.getValue());
                return new BinaryExpression(a, b, "+");
            }
        }

        return super.visit(expression);
    }

    private static class BinaryExpressionVisitor extends Visitor {
        private DataType dataType;
        private String operation;
        private int constantTotal;
        private int constantCounter;
        private List<Expression> nonConstants = new ArrayList<>();

        public BinaryExpressionVisitor(DataType dataType, String operation) {
            this.dataType = dataType;
            this.operation = operation;
            // need to choose the correct identity value for operation
            this.constantTotal = switch (operation) {
                case "+" -> 0;
                case "*" -> 1;
                default -> throw new RuntimeException("unexpected operator: " + operation);
            };
        }

        public int getConstantCounter() {
            return constantCounter;
        }
        public int getConstantTotal() {
            return constantTotal;
        }
        public List<Expression> getNonConstants() {
            return nonConstants;
        }

        @Override
        public Expression visit(BinaryExpression expression) {
            if (operation.equals(expression.getOp()) && dataType == expression.getType()) {
                handle(expression.getL());
                handle(expression.getR());
            }
            return null;
        }

        public void handle(Expression expression) {
            if (expression.isConstant()) {
                constantCounter += 1;
                switch (operation) {
                    case "+" -> constantTotal += expression.asInteger().orElseThrow();
                    case "*" -> constantTotal *= expression.asInteger().orElseThrow();
                    default -> throw new RuntimeException("unexpected operator: " + operation);
                }
            }
            else {
                if (expression instanceof BinaryExpression bin) {
                    visit(bin);
                }
                else {
                    nonConstants.add(expression);
                }
            }
        }
    }
}
