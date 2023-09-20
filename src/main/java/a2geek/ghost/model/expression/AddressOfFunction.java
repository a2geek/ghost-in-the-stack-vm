package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Symbol;

import java.util.Objects;

public class AddressOfFunction implements Expression {
    private Symbol symbol;
    public AddressOfFunction(Symbol symbol) {
        this.symbol = symbol;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public DataType getType() {
        // FIXME
        return DataType.INTEGER;
    }

    @Override
    public String toString() {
        return String.format("addrof(%s)", symbol.name());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressOfFunction that = (AddressOfFunction) o;
        return Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }
}
