package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Symbol;

import java.util.Objects;

public class AddressOfOperator implements Expression {
    private final Symbol symbol;

    public AddressOfOperator(Symbol symbol) {
        this.symbol = symbol;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public DataType getType() {
        return DataType.ADDRESS;
    }

    @Override
    public String toString() {
        return String.format("addrof(%s)", symbol.name());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressOfOperator that = (AddressOfOperator) o;
        return Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }
}
