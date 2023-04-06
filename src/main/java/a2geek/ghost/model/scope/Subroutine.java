package a2geek.ghost.model.scope;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Reference;
import a2geek.ghost.model.Scope;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Subroutine extends Scope {
    public Subroutine(Scope parent, String name, List<Pair<String, DataType>> parameters) {
        super(parent, name);
        parameters.sort(Collections.reverseOrder());
        for (var param : parameters) {
            addLocalVariable(param.getValue0(), Type.PARAMETER, param.getValue1());
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
