package a2geek.ghost.model;

public class IdentifierExpression implements Expression {
    private String id;

    public IdentifierExpression(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
