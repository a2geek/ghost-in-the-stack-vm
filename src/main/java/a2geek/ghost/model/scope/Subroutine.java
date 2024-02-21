package a2geek.ghost.model.scope;

import a2geek.ghost.model.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static a2geek.ghost.model.Symbol.in;

public class Subroutine extends Scope {
    protected Visibility visibility = Visibility.PUBLIC;
    protected Set<Modifier> modifiers = new HashSet<>();
    private String exitLabel;

    public Subroutine(Scope parent, String name, List<Symbol.Builder> parameters) {
        super(parent, name, DeclarationType.LOCAL);
        parameters.reversed().forEach(this::addLocalSymbol);
    }

    public void set(Visibility visibility) {
        this.visibility = visibility;
    }
    public boolean is(Visibility visibility) {
        return this.visibility == visibility;
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
        return String.format("%s %s SUB %s(%s) : %s : END SUB",
                visibility,
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
