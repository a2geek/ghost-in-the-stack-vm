package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.Symbol;

import java.util.*;
import java.util.stream.Collectors;

public class VariableReference implements Expression {
    public static VariableReference with(Symbol symbol, Expression... indexes) {
        if (indexes.length == 0) {
            return new VariableReference(symbol);
        }
        return new VariableReference(symbol, Arrays.asList(indexes));
    }

    private Symbol symbol;
    private List<Expression> indexes = new ArrayList<>();

    public VariableReference(Symbol symbol) {
        this.symbol = symbol;
    }
    public VariableReference(Symbol symbol, List<Expression> indexes) {
        this.symbol = symbol;
        this.indexes.addAll(indexes);
    }

    @Override
    public DataType getType() {
        return symbol.dataType();
    }

    public boolean isArray() {
        return indexes != null && indexes.size() > 0;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public List<Expression> getIndexes() {
        return indexes;
    }
    public void setIndexes(List<Expression> indexes) {
        this.indexes = indexes;
    }

    @Override
    public boolean isConstant() {
        return symbol.type() == Scope.Type.CONSTANT;
    }

    @Override
    public Optional<Integer> asInteger() {
        if (symbol.hasDefaultValue(1)) return symbol.defaultValues().get(0).asInteger();
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> asBoolean() {
        if (symbol.hasDefaultValue(1)) return symbol.defaultValues().get(0).asBoolean();
        return Optional.empty();
    }

    @Override
    public Optional<String> asString() {
        if (symbol.hasDefaultValue(1)) return symbol.defaultValues().get(0).asString();
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariableReference that = (VariableReference) o;
        return Objects.equals(symbol, that.symbol) && Objects.equals(indexes, that.indexes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, indexes);
    }

    @Override
    public String toString() {
        if (isArray()) {
            return String.format("%s(%s)", symbol.name(),
                    indexes.stream().map(Expression::toString).collect(Collectors.joining(",")));
        }
        return symbol.name();
    }
}
