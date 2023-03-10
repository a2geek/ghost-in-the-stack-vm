package a2geek.ghost.model.expression;

import a2geek.ghost.model.Expression;

public class ParenthesisExpression implements Expression {
    private Expression expr;

    public ParenthesisExpression(Expression expr) {
        this.expr = expr;
    }

    public Expression getExpr() {
        return expr;
    }

    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    @Override
    public String toString() {
        return String.format("( %s )", expr);
    }
}
