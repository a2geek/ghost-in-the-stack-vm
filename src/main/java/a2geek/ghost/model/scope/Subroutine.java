package a2geek.ghost.model.scope;

import a2geek.ghost.model.DeclarationType;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.SymbolType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static a2geek.ghost.model.Symbol.in;

public class Subroutine extends Scope {
    protected Set<Modifier> modifiers = new HashSet<>();
    private String exitLabel;

    public Subroutine(Scope parent, String name, List<Symbol.Builder> parameters) {
        super(parent, name, DeclarationType.LOCAL);
        parameters.reversed().forEach(this::addLocalSymbol);
    }

    public void add(Modifier modifier) {
        modifiers.add(modifier);
    }
    public boolean is(Modifier modifier) {
        return modifiers.contains(modifier);
    }

    public String getExitLabel() {
        return exitLabel;
    }
    public void setExitLabel(String exitLabel) {
        this.exitLabel = exitLabel;
    }

    @Override
    public String toString() {
        return String.format("%s SUB %s(%s) : %s : END SUB",
                modifiers.toString(),
                getName(),
                findAllLocalScope(in(SymbolType.PARAMETER)).stream()
                        .map(Symbol::name)
                        .collect(Collectors.joining(", ")),
                statementsAsString());
    }

    public enum Modifier {
        EXPORT,
        INLINE,
        VOLATILE
    }
}
