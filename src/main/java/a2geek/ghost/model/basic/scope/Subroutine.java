package a2geek.ghost.model.basic.scope;

import a2geek.ghost.model.basic.Scope;
import a2geek.ghost.model.basic.Symbol;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Subroutine extends Scope {
    private boolean inline;

    public Subroutine(Scope parent, String name, List<Symbol.Builder> parameters) {
        super(parent, name);
        Collections.reverse(parameters);
        parameters.forEach(this::addLocalSymbol);
    }

    public void setInline(boolean inline) {
        this.inline = inline;
    }
    public boolean isInline() {
        return inline;
    }

    @Override
    public String toString() {
        return String.format("SUB %s(%s) : %s : END SUB", getName(),
                findByType(Type.PARAMETER).stream()
                        .map(Symbol::name)
                        .collect(Collectors.joining(", ")),
                statementsAsString());
    }
}
