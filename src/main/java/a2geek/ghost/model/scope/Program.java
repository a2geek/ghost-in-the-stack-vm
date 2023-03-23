package a2geek.ghost.model.scope;

import a2geek.ghost.model.Scope;

import java.util.function.Function;

public class Program extends Scope {
    public Program(Function<String,String> caseStrategy) {
        super(caseStrategy, "_main");
    }

    @Override
    public String toString() {
        return statementsAsString();
    }
}
