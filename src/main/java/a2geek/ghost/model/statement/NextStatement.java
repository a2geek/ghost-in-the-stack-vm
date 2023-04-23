package a2geek.ghost.model.statement;

import a2geek.ghost.model.Reference;
import a2geek.ghost.model.Statement;

public class NextStatement implements Statement {
    private Reference ref;

    public NextStatement(Reference ref) {
        this.ref = ref;
    }

    public Reference getRef() {
        return ref;
    }

    @Override
    public String toString() {
        return String.format("NEXT %s", ref.name());
    }
}
