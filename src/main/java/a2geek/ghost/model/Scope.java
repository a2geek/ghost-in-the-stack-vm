package a2geek.ghost.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class Scope extends StatementBlock {
    private Function<String,String> caseStrategy;
    private String name;
    private Scope parent;
    private int varOffset = 0;
    private List<Reference> variables = new ArrayList<>();

    public Scope(Function<String,String> caseStrategy, String name) {
        this.caseStrategy = caseStrategy;
        this.name = caseStrategy.apply(name);
    }
    public Scope(Scope parent, String name) {
        this.caseStrategy = parent.caseStrategy;
        this.name = caseStrategy.apply(name);
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public List<Reference> getLocalVariables() {
        return variables;
    }
    public Reference addLocalVariable(String name) {
        return findLocalVariable(name).orElseGet(() -> {
            var fixedName = caseStrategy.apply(name);
            var ref = new Reference(fixedName, Type.LOCAL, varOffset);
            variables.add(ref);
            // TODO: this will need to be fixed when more than integers are supported!
            varOffset += 2;
            return ref;
        });
    }
    public int getVarOffset() {
        return varOffset;
    }
    public Optional<Reference> findLocalVariable(String name) {
        var fixedName = caseStrategy.apply(name);
        return variables.stream().filter(r -> fixedName.equals(r.name())).findFirst();
    }
    public Optional<Reference> findVariable(String name) {
        var fixedName = caseStrategy.apply(name);
        var found = findLocalVariable(fixedName);
        if (found.isPresent()) {
            return found;
        }
        if (parent != null) {
            return parent.findVariable(fixedName)
                .map(r -> new Reference(r.name(), Type.GLOBAL, r.offset()));
        }
        return Optional.empty();
    }

    public enum Type {
        GLOBAL,
        LOCAL
    }
}
