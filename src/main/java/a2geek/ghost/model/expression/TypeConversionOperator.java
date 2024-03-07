package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;

import java.util.Objects;
import java.util.Optional;

public class TypeConversionOperator implements Expression {
    private Expression expr;
    private DataType targetType;

    public TypeConversionOperator(Expression expr, DataType targetType) {
        this.expr = expr;
        this.targetType = targetType;
    }

    public Expression getExpr() {
        return expr;
    }
    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    @Override
    public Optional<Boolean> asBoolean() {
        return expr.asBoolean();
    }

    @Override
    public Optional<Integer> asInteger() {
        return expr.asInteger();
    }

    @Override
    public Optional<String> asString() {
        return expr.asString();
    }

    @Override
    public boolean isConstant() {
        return expr.isConstant();
    }

    @Override
    public DataType getType() {
        return targetType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeConversionOperator that = (TypeConversionOperator) o;
        return Objects.equals(expr, that.expr) && targetType == that.targetType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(expr, targetType);
    }

    @Override
    public String toString() {
        var fn = switch (targetType) {
            case STRING -> "CString";
            case INTEGER -> "CInt";
            case BOOLEAN -> "CBool";
            case BYTE -> "CByte";
            case ADDRESS -> "CAddress";
        };
        return String.format("%s(%s)", fn, expr);
    }
}
