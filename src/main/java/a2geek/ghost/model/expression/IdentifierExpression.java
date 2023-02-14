package a2geek.ghost.model.expression;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Visitor;

public class IdentifierExpression implements Expression {
    private String id;

    public String getId() {
        return id;
    }

    public IdentifierExpression(String id) {
        this.id = id;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return id;
    }
}
