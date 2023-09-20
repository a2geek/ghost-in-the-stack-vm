package a2geek.ghost.model.scope;

import a2geek.ghost.model.Scope;

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
            .map(scope -> String.format("%s: %s", getDescriptiveName(scope), scope.statementsAsString()))
            .collect(Collectors.joining("\n"));
    }

    public String getDescriptiveName(Scope scope) {
        if (scope instanceof a2geek.ghost.model.scope.Function func) {
            return String.format("%s (FUNC%s)", func.getName(), func.isInline() ? ", INLINE" : "");
        }
        else if (scope instanceof Subroutine sub) {
            return String.format("%s (SUB%s)", sub.getName(), sub.isInline() ? ", INLINE" : "");
        }
        else if (scope instanceof Program program) {
            return String.format("%s (PROGRAM)", program.getName());
        }
        else {
            return String.format("%s (SCOPE)", scope.getName());
        }
    }
}
