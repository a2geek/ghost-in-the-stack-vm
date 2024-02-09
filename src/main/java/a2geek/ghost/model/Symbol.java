package a2geek.ghost.model;

import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Subroutine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public record Symbol(String name, SymbolType symbolType, DeclarationType declarationType,
                     List<Expression> defaultValues, DataType dataType, List<Expression> dimensions,
                     Scope scope, String targetName, boolean temporary) {
    public boolean hasDefaultValue(int size) {
        return defaultValues != null && defaultValues.size() == size;
    }
    public int numDimensions() {
        return dimensions.size();
    }

    public static Builder from(Symbol original) {
        return new Builder(original);
    }
    public static Builder label(String name) {
        return new Builder(name, SymbolType.LABEL).dataType(DataType.ADDRESS);
    }
    public static Builder variable(String name, SymbolType type) {
        return new Builder(name, type);
    }
    public static Builder constant(String name, Expression expr) {
        return new Builder(name, SymbolType.CONSTANT).defaultValues(expr).dataType(expr.getType());
    }
    public static Builder scope(Scope scope) {
        var symbolType = switch (scope) {
            case Function f -> SymbolType.FUNCTION;
            case Subroutine s -> SymbolType.SUBROUTINE;
            default -> SymbolType.MODULE;
        };
        return new Builder(scope.getName(), symbolType).declarationType(DeclarationType.GLOBAL).scope(scope);
    }
    public static class Builder {
        private String name;
        private SymbolType symbolType;
        private DeclarationType declarationType;
        private DataType dataType;
        private List<Expression> dimensions = new ArrayList<>();    // if empty, not an array
        private List<Expression> defaultValues;
        private Scope scope;
        private String targetName;
        private boolean temporary = false;  // explicitly the default

        Builder(String name, SymbolType symbolType) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(symbolType);
            this.name = name;
            this.symbolType = symbolType;
        }
        Builder(Symbol original) {
            this.name = original.name;
            this.symbolType = original.symbolType;
            this.declarationType = original.declarationType;
            this.defaultValues = original.defaultValues;
            this.dataType = original.dataType;
            this.dimensions = original.dimensions;
            this.scope = original.scope;
            this.targetName = original.targetName;
            this.temporary = original.temporary;
        }
        public Builder name(String name) {
            Objects.requireNonNull(name);
            // Included for case correction
            this.name = name;
            return this;
        }
        public SymbolType symbolType() {
            return symbolType;
        }
        public Builder symbolType(SymbolType symbolType) {
            Objects.requireNonNull(symbolType);
            this.symbolType = symbolType;
            return this;
        }
        public Builder declarationType(DeclarationType declarationType) {
            this.declarationType = declarationType;
            return this;
        }
        public DeclarationType declarationType() {
            return declarationType;
        }
        public String name() {
            return name;
        }
        public Builder defaultValues(Expression... exprs) {
            Objects.requireNonNull(exprs);
            this.defaultValues = Arrays.asList(exprs);
            return this;
        }
        public Builder defaultValues(List<Expression> exprs) {
            Objects.requireNonNull(exprs);
            this.defaultValues = exprs;
            return this;
        }
        public Builder dataType(DataType dataType) {
            Objects.requireNonNull(dataType);
            this.dataType = dataType;
            return this;
        }
        public DataType dataType() {
            return dataType;
        }
        public Builder dimensions(Expression... dimensions) {
            Objects.requireNonNull(dimensions);
            this.dimensions = Arrays.asList(dimensions);
            return this;
        }
        public Builder dimensions(List<Expression> dimensions) {
            Objects.requireNonNull(dimensions);
            this.dimensions = dimensions;
            return this;
        }
        public Builder scope(Scope scope) {
            this.scope = scope;
            return this;
        }
        public Builder targetName(String targetName) {
            this.targetName = targetName;
            return this;
        }
        public Builder temporary(boolean temporary) {
            this.temporary = temporary;
            return this;
        }
        public boolean equals(Symbol symbol) {
            if (symbol == null) return false;
            return Objects.equals(this.name, symbol.name)
                && Objects.equals(this.symbolType, symbol.symbolType)
                && Objects.equals(this.targetName, symbol.targetName)
                && Objects.equals(this.declarationType, symbol.declarationType)
                && Objects.equals(this.defaultValues, symbol.defaultValues)
                && Objects.equals(this.dataType, symbol.dataType)
                && Objects.equals(this.dimensions, symbol.dimensions)
                && Objects.equals(this.temporary, symbol.temporary);
        }
        public Symbol build() {
            if (defaultValues != null && defaultValues.size() > 1 && dimensions.isEmpty()) {
                throw new RuntimeException("expecting array but no dimensions assigned " + name);
            }
            return new Symbol(name, symbolType, declarationType, defaultValues, dataType, dimensions,
                    scope, targetName, temporary);
        }
    }

    public static Predicate<Symbol> in(SymbolType... symbolTypes) {
        final var typesList = Arrays.asList(symbolTypes);
        return symbol -> typesList.contains(symbol.symbolType());
    }
    public static Predicate<Symbol> is(DeclarationType declarationType) {
        return symbol -> declarationType == symbol.declarationType();
    }
    public static Predicate<Symbol> named(String name) {
        return new NamedPredicate(name);
    }

    private static class NamedPredicate implements Predicate<Symbol> {
        private String targetName;

        NamedPredicate(String targetName) {
            this.targetName = targetName;
        }

        @Override
        public boolean test(Symbol symbol) {
            boolean found = symbol.name().equals(targetName);
            if (found && symbol.symbolType() == SymbolType.ALIAS) {
                targetName = symbol.targetName();
                return false;
            }
            return found;
        }
    }
}
