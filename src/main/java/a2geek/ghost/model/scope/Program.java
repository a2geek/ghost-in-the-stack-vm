package a2geek.ghost.model.scope;

import a2geek.ghost.model.Scope;

public class Program extends Scope {
    public Program() {
        super("_main");
    }

    @Override
    public String toString() {
        return statementsAsString();
    }
}
