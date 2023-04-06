package a2geek.ghost.model;

public record Reference(String name, Scope.Type type, Expression expr, DataType dataType) {
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
        public Reference build() {
            return new Reference(name, type, expr, dataType);
        }
    }
}
