package a2geek.ghost.model.expression;

import a2geek.ghost.model.Expression;

import java.util.Objects;

public class IdentifierExpression implements Expression {
    private Type type = Type.INTEGER;   // Default for now
    private String id;

    @Override
    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public IdentifierExpression(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof IdentifierExpression that) {
            return Objects.equals(id, that.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
