package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Reference;

import java.util.Objects;

public class IdentifierExpression implements Expression {
    private DataType type = DataType.INTEGER;   // Default for now
    private Reference ref;

    @Override
    public DataType getType() {
        return type;
    }

    public Reference getRef() {
        return ref;
    }

    public IdentifierExpression(Reference ref) {
        this.ref = ref;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof IdentifierExpression that) {
            return Objects.equals(ref, that.ref);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref);
    }

    @Override
    public String toString() {
        return ref.name();
    }
}
