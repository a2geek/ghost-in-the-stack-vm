package a2geek.ghost.model;

public record Symbol(String name, Scope.Type type, Expression expr, DataType dataType, int numDimensions) {
    public static Builder builder(String name, Scope.Type type) {
        return new Builder(name, type);
    }
    public static Builder builder(String name, Expression expr) {
        return new Builder(name, Scope.Type.CONSTANT).expression(expr).dataType(expr.getType());
    }
    public static class Builder {
        String name;
        Scope.Type type;
        Expression expr;
        DataType dataType;
        int numDimensions = 0;  // not an array!

        Builder(String name, Scope.Type type) {
            this.name = name;
            this.type = type;
        }
        public Builder expression(Expression expr) {
            this.expr = expr;
            return this;
        }
        public Builder dataType(DataType dataType) {
            this.dataType = dataType;
            return this;
        }
        public Builder dimensions(int numDimensions) {
            this.numDimensions = numDimensions;
            return this;
        }
        public Symbol build() {
            return new Symbol(name, type, expr, dataType, numDimensions);
        }
    }
}
