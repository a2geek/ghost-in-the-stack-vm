package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Symbol;

import java.util.Objects;
import java.util.Optional;

public class ArrayLengthFunction implements Expression {
    private final Symbol symbol;
    private final int dimensionNumber;
    public ArrayLengthFunction(Symbol symbol, int dimensionNumber) {
        this.symbol = symbol;
        this.dimensionNumber = dimensionNumber;
        if (dimensionNumber < 1 || dimensionNumber > symbol.dimensions().size()) {
            throw new RuntimeException("invalid reference to non-existant dimension: " + toString());
        }
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public int getDimensionNumber() {
        return dimensionNumber;
    }

    @Override
    public Optional<Integer> asInteger() {
        return symbol.dimensions().get(dimensionNumber-1).asInteger();
    }

    @Override
    public DataType getType() {
        return DataType.INTEGER;
    }

    @Override
    public boolean isConstant() {
        return symbol.dimensions().get(dimensionNumber-1).isConstant();
    }

    @Override
    public String toString() {
        return String.format("ubound(%s,%d)", symbol.name(), dimensionNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayLengthFunction that = (ArrayLengthFunction) o;
        // Intentionally skipping model
        return Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        // Intentionally skipping model
        return Objects.hash(symbol);
    }
}
