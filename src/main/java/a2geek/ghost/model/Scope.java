package a2geek.ghost.model;

import java.util.*;
import java.util.stream.Collectors;

public class Scope extends StatementBlock {
    private String name;
    private Scope parent;
    private Set<String> variables = new HashSet<>();

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
        return variables.stream().map(n -> new Reference(n, Type.LOCAL)).collect(Collectors.toList());
    }
    public void addLocalVariable(String name) {
        variables.add(name);
    }
    public Optional<Reference> findVariable(String name) {
        if (variables.contains(name)) {
            return Optional.of(new Reference(name, Type.LOCAL));
        }
        if (parent != null) {
            return parent.findVariable(name)
                .map(r -> new Reference(r.name(), Type.GLOBAL));
        }
        return Optional.empty();
    }

    public record Reference(String name, Type type) {
        @Override
        public String toString() {
            return String.format("name: %s, type: %s", name, type);
        }
    }
    public enum Type {
        GLOBAL,
        LOCAL
    }
}
