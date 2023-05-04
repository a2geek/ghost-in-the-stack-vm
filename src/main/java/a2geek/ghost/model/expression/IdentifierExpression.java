package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.Symbol;

import java.util.Objects;
import java.util.Optional;

public class IdentifierExpression implements Expression {
    private Symbol symbol;

    @Override
    public DataType getType() {
        return symbol.dataType();
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public IdentifierExpression(Symbol symbol) {
        this.symbol = symbol;
    }

    @Override
    public boolean isConstant() {
        return symbol.type() == Scope.Type.CONSTANT;
    }

    @Override
    public Optional<Integer> asInteger() {
        if (symbol.expr() == null) return Optional.empty();
        return symbol.expr().asInteger();
    }

    @Override
    public Optional<Boolean> asBoolean() {
        if (symbol.expr() == null) return Optional.empty();
        return symbol.expr().asBoolean();
    }

    @Override
    public Optional<String> asString() {
        if (symbol.expr() == null) return Optional.empty();
        return symbol.expr().asString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof IdentifierExpression that) {
            return Objects.equals(symbol, that.symbol);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }

    @Override
    public String toString() {
        return symbol.name();
    }
}
