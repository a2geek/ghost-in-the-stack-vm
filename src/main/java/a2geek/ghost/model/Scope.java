package a2geek.ghost.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Scope extends StatementBlock {
    private String name;
    private Scope parent;
    private int varOffset = 0;
    private List<Reference> variables = new ArrayList<>();

    public Scope(String name) {
        this.name = name;
    }
    public Scope(Scope parent, String name) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public List<Reference> getLocalVariables() {
        return variables;
    }
    public void addLocalVariable(String name) {
        var found = findLocalVariable(name);
        if (found.isPresent()) {
            return;
        }
        variables.add(new Reference(name, Type.LOCAL, varOffset));
        // TODO: this will need to be fixed when more than integer are supported!
        varOffset += 2;
    }
    public int getVarOffset() {
        return varOffset;
    }
    public Optional<Reference> findLocalVariable(String name) {
        return variables.stream().filter(r -> name.equals(r.name)).findFirst();
    }
    public Optional<Reference> findVariable(String name) {
        var found = findLocalVariable(name);
        if (found.isPresent()) {
            return found;
        }
        if (parent != null) {
            return parent.findVariable(name)
                .map(r -> new Reference(r.name, Type.GLOBAL, r.offset));
        }
        return Optional.empty();
    }

    public record Reference(String name, Type type, int offset) {
    }

    public enum Type {
        GLOBAL,
        LOCAL
    }
}
