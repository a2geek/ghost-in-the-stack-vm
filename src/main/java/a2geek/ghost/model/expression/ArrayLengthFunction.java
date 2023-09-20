package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.ModelBuilder;
import a2geek.ghost.model.Symbol;

import java.util.Objects;
import java.util.Optional;

public class ArrayLengthFunction implements Expression {
    private Symbol symbol;
    private ModelBuilder model;
    public ArrayLengthFunction(ModelBuilder model, Symbol symbol) {
        this.symbol = symbol;
        this.model = model;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public Optional<Integer> asInteger() {
        return model.getArrayDim(symbol).asInteger();
    }

    @Override
    public DataType getType() {
        return DataType.INTEGER;
    }

    @Override
    public boolean isConstant() {
        return model.getArrayDim(symbol).isConstant();
    }

    @Override
    public String toString() {
        return String.format("ubound(%s)", symbol.name());
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
