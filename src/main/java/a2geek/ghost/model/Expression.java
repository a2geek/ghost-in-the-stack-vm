package a2geek.ghost.model;

import a2geek.ghost.model.expression.*;

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
                if (isType(DataType.INTEGER, DataType.ADDRESS, DataType.BOOLEAN, DataType.STRING)) {
                    return this;
                }
                else if (isType(DataType.BYTE)) {
                    return this.toWord();
                }
            }
            case BYTE -> {
                if (isType(DataType.BYTE)) {
                    return this;
                }
                else if (isType(DataType.INTEGER, DataType.ADDRESS, DataType.BOOLEAN)) {
                    return this.toByte();
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

    default BinaryExpression minus1() {
        return new BinaryExpression("-", this, IntegerConstant.ONE);
    }
    default BinaryExpression times(Expression rhs) {
        return new BinaryExpression("*", this, rhs);
    }
    default BinaryExpression plus(Expression rhs) {
        return new BinaryExpression("+", this, rhs);
    }
    default BinaryExpression minus(Expression rhs) {
        return new BinaryExpression("-", this, rhs);
    }
    default BinaryExpression lshift(Expression bits) {
        return new BinaryExpression("<<", this, bits);
    }
    default BinaryExpression rshift(Expression bits) {
        return new BinaryExpression(">>", this, bits);
    }
    default BinaryExpression lt(Expression rhs) {
        return new BinaryExpression("<", this, rhs);
    }
    default BinaryExpression le(Expression rhs) {
        return new BinaryExpression("<=", this, rhs);
    }
    default BinaryExpression eq(Expression rhs) {
        return new BinaryExpression("=", this, rhs);
    }
    default BinaryExpression ge(Expression rhs) {
        return new BinaryExpression(">=", this, rhs);
    }
    default BinaryExpression gt(Expression rhs) {
        return new BinaryExpression(">", this, rhs);
    }
    default BinaryExpression and(Expression rhs) {
        return new BinaryExpression("and", this, rhs);
    }
    default BinaryExpression or(Expression rhs) {
        return new BinaryExpression("or", this, rhs);
    }
    default UnaryExpression negate() {
        return new UnaryExpression("-", this);
    }
    default DereferenceOperator deref() {
        return new DereferenceOperator(this, getType());
    }
    default DereferenceOperator deref(DataType type) {
        return new DereferenceOperator(this, type);
    }
    default Expression toByte() {
        if (getType().sizeof() == 2) {
            if (this.isConstant() && this.asInteger().isPresent()) {
                return new ByteConstant(this.asInteger().get());
            }
            return new UnaryExpression("w2b", this, DataType.BYTE);
        }
        return this;
    }

    default Expression toWord() {
        if (getType().sizeof() == 1) {
            if (this.isConstant() && this.asInteger().isPresent()) {
                return new IntegerConstant(this.asInteger().get());
            }
            return new UnaryExpression("b2w", this, DataType.INTEGER);
        }
        return this;
    }
}
