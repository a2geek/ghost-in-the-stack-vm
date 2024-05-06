package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;

import java.util.Objects;

public class IfExpression implements Expression {
    private Expression condition;
    private Expression trueValue;
    private Expression falseValue;

    public IfExpression(Expression condition, Expression trueValue, Expression falseValue) {
        Objects.requireNonNull(condition);
        Objects.requireNonNull(trueValue);
        Objects.requireNonNull(falseValue);
        this.condition = condition;
        this.trueValue = trueValue.checkAndCoerce(falseValue.getType());
        this.falseValue = falseValue.checkAndCoerce(trueValue.getType());
        if (this.trueValue.getType() != this.falseValue.getType()) {
            throw new RuntimeException("incompatible datatypes in if expression: " + this);
        }
    }

    public Expression getCondition() {
        return condition;
    }
    public void setCondition(Expression condition) {
        this.condition = condition;
    }

    public Expression getTrueValue() {
        return trueValue;
    }
    public void setTrueValue(Expression trueValue) {
        this.trueValue = trueValue;
    }

    public Expression getFalseValue() {
        return falseValue;
    }
    public void setFalseValue(Expression falseValue) {
        this.falseValue = falseValue;
    }

    @Override
    public DataType getType() {
        return trueValue.getType();
    }

    @Override
    public String toString() {
        return String.format("%s IF %s ELSE %s", trueValue, condition, falseValue);
    }
}
