package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;

import java.util.Objects;
import java.util.Optional;

/**
 * This is not really an expression, sometimes we need an expression to hold a place.
 * For instance, with a SUB or FUNCTION, the parameter may be an array. We don't have
 * an actual Expression for the dimension(s), so a placeholder is used. It is not a
 * constant and all the "as" functions return empty. But it has a DataType, allowing
 * normal computations. And because it's a class, we can detect it if the need arises.
 */
public class PlaceholderExpression implements Expression {
    public static PlaceholderExpression of(DataType dataType) {
        return new PlaceholderExpression(dataType);
    }

    private final DataType dataType;
    private Expression expression;

    private PlaceholderExpression(DataType dataType) {
        this.dataType = dataType;
    }

    public void setExpression(Expression expression) {
        Objects.requireNonNull(expression);
        this.expression = expression;
    }

    @Override
    public DataType getType() {
        return expression == null ? dataType : expression.getType();
    }

    @Override
    public boolean isConstant() {
        return expression != null && expression.isConstant();
    }

    @Override
    public Optional<Boolean> asBoolean() {
        return expression == null ? Optional.empty() : expression.asBoolean();
    }

    @Override
    public Optional<Integer> asInteger() {
        return expression == null ? Optional.empty() : expression.asInteger();
    }

    @Override
    public Optional<String> asString() {
        return expression == null ? Optional.empty() : expression.asString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlaceholderExpression that = (PlaceholderExpression) o;
        return dataType == that.dataType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataType);
    }

    @Override
    public String toString() {
        return expression == null ? "-PH-" : expression.toString();
    }
}
