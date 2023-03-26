package a2geek.ghost.model.scope;

import a2geek.ghost.model.Reference;
import a2geek.ghost.model.Scope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Subroutine extends Scope {
    public Subroutine(Scope parent, String name, List<String> parameters) {
        super(parent, name);
        parameters.sort(Collections.reverseOrder());
        for (String param : parameters) {
            addLocalVariable(param, Type.PARAMETER);
        }
    }

    @Override
    public String toString() {
        return String.format("SUB %s(%s) : %s : END SUB", getName(),
                findByType(Type.PARAMETER).stream()
                        .map(Reference::name)
                        .collect(Collectors.joining(", ")),
                statementsAsString());
    }
}
