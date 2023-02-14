package a2geek.ghost.model.statement;

import a2geek.ghost.model.Statement;
import a2geek.ghost.model.Visitor;

public class GrStatement implements Statement {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "GR";
    }
}