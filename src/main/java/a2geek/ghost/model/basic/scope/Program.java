package a2geek.ghost.model.basic.scope;

import a2geek.ghost.model.basic.Scope;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Program extends Scope {
    public Program(Function<String,String> caseStrategy) {
        super(caseStrategy, "main");
    }

    @Override
    public String toString() {
        var allScopes = new ArrayList<>(getScopes());
        allScopes.add(this);
        return allScopes.stream()
            .map(scope -> String.format("%s: %s", scope.getName(), scope.statementsAsString()))
            .collect(Collectors.joining("\n"));
    }
}
