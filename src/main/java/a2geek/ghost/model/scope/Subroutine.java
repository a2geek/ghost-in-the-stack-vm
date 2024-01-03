package a2geek.ghost.model.scope;

import a2geek.ghost.model.Scope;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.SymbolType;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static a2geek.ghost.model.Symbol.in;

public class Subroutine extends Scope {
    private boolean inline;
    private boolean export;

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

    public void setExport(boolean export) {
        this.export = export;
    }
    public boolean isExport() {
        return export;
    }

    @Override
    public String toString() {
        return String.format("%s%sSUB %s(%s) : %s : END SUB",
                export ? "EXPORT " : "",
                inline ? "INLINE " : "",
                getName(),
                findAllLocalScope(in(SymbolType.PARAMETER)).stream()
                        .map(Symbol::name)
                        .collect(Collectors.joining(", ")),
                statementsAsString());
    }
}
