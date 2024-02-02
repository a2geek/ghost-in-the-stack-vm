package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.SymbolType;

import java.util.Objects;
import java.util.Optional;

public class VariableReference implements Expression {
    public static VariableReference with(Symbol symbol) {
        return new VariableReference(symbol);
    }

    private Symbol symbol;

    public VariableReference(Symbol symbol) {
        this.symbol = symbol;
    }

    @Override
    public DataType getType() {
        return symbol.dataType();
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public boolean isConstant() {
        return symbol.symbolType() == SymbolType.CONSTANT;
    }

    @Override
    public Optional<Integer> asInteger() {
        if (symbol.hasDefaultValue(1)) return symbol.defaultValues().getFirst().asInteger();
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> asBoolean() {
        if (symbol.hasDefaultValue(1)) return symbol.defaultValues().getFirst().asBoolean();
        return Optional.empty();
    }

    @Override
    public Optional<String> asString() {
        if (symbol.hasDefaultValue(1)) return symbol.defaultValues().getFirst().asString();
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariableReference that = (VariableReference) o;
        return Objects.equals(symbol, that.symbol);
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
