package a2geek.ghost.model.visitor;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Visitor;
import a2geek.ghost.model.expression.BinaryExpression;
import a2geek.ghost.model.expression.IntegerConstant;

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
                return visitor.getNonConstants().stream()
                        .reduce((a,b) -> new BinaryExpression(expression.getOp(), a, b))
                        .orElseThrow()
                        .plus(new IntegerConstant(visitor.getConstantSum()));
            }
        }

        return super.visit(expression);
    }

    private static class BinaryExpressionVisitor extends Visitor {
        private DataType dataType;
        private String operation;
        private int constantSum;
        private int constantCounter;
        private List<Expression> nonConstants = new ArrayList<>();

        public BinaryExpressionVisitor(DataType dataType, String operation) {
            this.dataType = dataType;
            this.operation = operation;
        }

        public int getConstantCounter() {
            return constantCounter;
        }
        public int getConstantSum() {
            return constantSum;
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
                constantSum += expression.asInteger().orElseThrow();
                constantCounter += 1;
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
