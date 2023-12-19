package a2geek.ghost.model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public record Symbol(String name, Scope.Type type, DeclarationType declarationType, List<Expression> defaultValues, DataType dataType, int numDimensions) {
    public boolean hasDefaultValue(int size) {
        return defaultValues != null && defaultValues.size() == size;
    }

    public static Builder label(String name) {
        return new Builder(name, Scope.Type.LABEL).dataType(DataType.ADDRESS);
    }
    public static Builder variable(String name, Scope.Type type) {
        return new Builder(name, type);
    }
    public static Builder constant(String name, Expression expr) {
        return new Builder(name, Scope.Type.CONSTANT).defaultValues(expr).dataType(expr.getType());
    }
    public static class Builder {
        private String name;
        private Scope.Type type;
        private DeclarationType declarationType;
        private DataType dataType;
        private int numDimensions = 0;  // not an array!
        private List<Expression> defaultValues;

        Builder(String name, Scope.Type type) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(type);
            this.name = name;
            this.type = type;
        }
        public Builder name(String name) {
            Objects.requireNonNull(name);
            // Included for case correction
            this.name = name;
            return this;
        }
        public Scope.Type type() {
            return type;
        }
        public Builder type(Scope.Type type) {
            Objects.requireNonNull(type);
            this.type = type;
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
        public Builder dimensions(int numDimensions) {
            this.numDimensions = numDimensions;
            return this;
        }
        public boolean equals(Symbol symbol) {
            if (symbol == null) return false;
            return Objects.equals(this.name, symbol.name)
                && Objects.equals(this.type, symbol.type)
                && Objects.equals(this.defaultValues, symbol.defaultValues)
                && Objects.equals(this.dataType, symbol.dataType)
                && Objects.equals(this.numDimensions, symbol.numDimensions);
        }
        public Symbol build() {
            if (defaultValues != null && defaultValues.size() > 1 && numDimensions == 0) {
                throw new RuntimeException("expecting array but no dimensions assigned " + name);
            }
            return new Symbol(name, type, declarationType, defaultValues, dataType, numDimensions);
        }
    }

    public static Predicate<Symbol> in(Scope.Type... types) {
        final var typesList = Arrays.asList(types);
        return symbol -> typesList.contains(symbol.type());
    }
    public static Predicate<Symbol> is(DeclarationType declarationType) {
        return symbol -> declarationType == symbol.declarationType();
    }
}
