package a2geek.ghost.model;

import java.util.Objects;

public record Symbol(String name, Scope.Type type, Expression expr, DataType dataType, int numDimensions) {
    public static Builder variable(String name, Scope.Type type) {
        return new Builder(name, type);
    }
    public static Builder constant(String name, Expression expr) {
        return new Builder(name, Scope.Type.CONSTANT).expression(expr).dataType(expr.getType());
    }
    public static class Builder {
        private String name;
        private Scope.Type type;
        private Expression expr;
        private DataType dataType;
        private int numDimensions = 0;  // not an array!

        Builder(String name, Scope.Type type) {
            this.name = name;
            this.type = type;
        }
        public Builder name(String name) {
            // Included for case correction
            this.name = name;
            return this;
        }
        public Scope.Type type() {
            return type;
        }
        public Builder type(Scope.Type type) {
            this.type = type;
            return this;
        }
        public String name() {
            return name;
        }
        public Builder expression(Expression expr) {
            this.expr = expr;
            return this;
        }
        public Builder dataType(DataType dataType) {
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
            return Objects.equals(this.name, symbol.name())
                && Objects.equals(this.type, symbol.type())
                && Objects.equals(this.expr, symbol.expr())
                && Objects.equals(this.dataType, symbol.dataType())
                && Objects.equals(this.numDimensions, symbol.numDimensions);
        }
        public Symbol build() {
            return new Symbol(name, type, expr, dataType, numDimensions);
        }
    }
}
