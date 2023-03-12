package a2geek.ghost.model.expression;

import a2geek.ghost.model.Expression;

public class FunctionExpression implements Expression {
    private String name;
    private Expression expr;

    public FunctionExpression(String name, Expression expr) {
        this.name = name;
        this.expr = expr;
    }

    public String getName() {
        return name;
    }

    public Expression getExpr() {
        return expr;
    }

    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name, expr);
    }
}
