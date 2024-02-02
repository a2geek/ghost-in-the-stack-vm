package a2geek.ghost.model;

import a2geek.ghost.model.expression.BinaryExpression;
import a2geek.ghost.model.expression.IntegerConstant;

import java.util.Optional;

public interface Expression {
    DataType getType();

    default boolean isType(DataType... types) {
        for (var type : types) {
            if (type == getType()) return true;
        }
        return false;
    }

    default Expression checkAndCoerce(DataType target) {
        switch (target) {
            case ADDRESS -> {
                if (isType(DataType.ADDRESS, DataType.STRING, DataType.INTEGER)) {
                    return this;
                }
            }
            case INTEGER -> {
                if (isType(DataType.INTEGER, DataType.ADDRESS, DataType.BOOLEAN)) {
                    return this;
                }
            }
            case BOOLEAN -> {
                if (isType(DataType.BOOLEAN, DataType.INTEGER)) {
                    return this;
                }
            }
            case STRING -> {
                if (isType(DataType.STRING, DataType.ADDRESS)) {
                    return this;
                }
            }
        }
        String message = String.format("unable to convert expression '%s' to %s", this, target);
        throw new RuntimeException(message);
    }

    default boolean isConstant() {
        return false;
    }

    default Optional<Integer> asInteger() {
        return Optional.empty();
    }
    default Optional<Boolean> asBoolean() {
        return Optional.empty();
    }
    default Optional<String> asString() {
        return Optional.empty();
    }

    default Expression minus1() {
        return new BinaryExpression("-", this, IntegerConstant.ONE);
    }
    default Expression times2() {
        return new BinaryExpression("*", this, IntegerConstant.TWO);
    }
    default Expression plus(Expression rhs) {
        return new BinaryExpression("+", this, rhs);
    }
    default Expression minus(Expression rhs) {
        return new BinaryExpression("-", this, rhs);
    }
    default Expression lt(Expression rhs) {
        return new BinaryExpression("<", this, rhs);
    }
    default Expression le(Expression rhs) {
        return new BinaryExpression("<=", this, rhs);
    }
    default Expression eq(Expression rhs) {
        return new BinaryExpression("=", this, rhs);
    }
    default Expression ge(Expression rhs) {
        return new BinaryExpression(">=", this, rhs);
    }
    default Expression gt(Expression rhs) {
        return new BinaryExpression(">", this, rhs);
    }
    default Expression and(Expression rhs) {
        return new BinaryExpression("and", this, rhs);
    }
}
