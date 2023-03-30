package a2geek.ghost.model.scope;

import a2geek.ghost.model.Reference;
import a2geek.ghost.model.Scope;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Function extends Subroutine {
    // FIXME need to accept types at some point!
    public Function(Scope parent, String name, List<String> parameters) {
        super(parent, name, parameters);
        addLocalVariable(name, Type.RETURN_VALUE);
    }

    @Override
    public String toString() {
        return String.format("FUNCTION %s(%s) : %s : END FUNCTION", getName(),
                findByType(Type.PARAMETER).stream()
                        .map(Reference::name)
                        .collect(Collectors.joining(", ")),
                statementsAsString());
    }
}
