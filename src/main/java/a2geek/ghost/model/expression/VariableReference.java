package a2geek.ghost.model.expression;

import a2geek.ghost.model.*;

import java.util.Objects;
import java.util.Optional;

public class VariableReference implements Expression {
    public static Expression with(Symbol symbol) {
        if (symbol.passingMode() == PassingMode.BYREF) {
            return new DereferenceOperator(new VariableReference(symbol), symbol.dataType());
        }
        else {
            return new VariableReference(symbol);
        }
    }

    private Symbol symbol;

    private VariableReference(Symbol symbol) {
        this.symbol = symbol;
    }

    @Override
    public DataType getType() {
        return symbol.dataType();
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
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
