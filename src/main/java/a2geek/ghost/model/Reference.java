package a2geek.ghost.model;

public record Reference(String name, Scope.Type type, Expression expr) {
    public Reference(String name, Scope.Type type) {
        this(name, type, null);
    }
    public Reference(String name, Expression expr) {
        this(name, Scope.Type.CONSTANT, expr);
    }
}
