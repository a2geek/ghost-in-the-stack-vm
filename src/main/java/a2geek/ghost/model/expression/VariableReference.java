package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class VariableReference implements Expression {
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
        if (o instanceof VariableReference that) {
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
        if (isArray()) {
            return String.format("%s(%s)", symbol.name(),
                    indexes.stream().map(Expression::toString).collect(Collectors.joining(",")));
        }
        return symbol.name();
    }
}
