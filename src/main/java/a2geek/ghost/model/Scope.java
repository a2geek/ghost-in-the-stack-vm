package a2geek.ghost.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Scope extends StatementBlock {
    private Function<String,String> caseStrategy;
    private String name;
    private Scope parent;
    private Type type;
    private List<Reference> variables = new ArrayList<>();
    private List<Scope> scopes = new ArrayList<>();

    public Scope(Function<String,String> caseStrategy, String name) {
        this.caseStrategy = caseStrategy;
        this.name = caseStrategy.apply(name);
        this.type = Type.GLOBAL;
    }
    public Scope(Scope parent, String name) {
        this.caseStrategy = parent.caseStrategy;
        this.name = caseStrategy.apply(name);
        this.parent = parent;
        this.type = Type.LOCAL;
    }

    public String getName() {
        return name;
    }

    public List<Reference> getAllVariables() {
        var allVariables = new ArrayList<Reference>();
        allVariables.addAll(this.variables);
        if (this.parent != null) {
            this.parent.getAllVariables().stream()
                    .filter(ref -> Type.GLOBAL == ref.type())
                    .forEach(allVariables::add);
        }
        return allVariables;
    }
    public List<Reference> getLocalVariables() {
        return variables;
    }
    public Reference addLocalVariable(String name) {
        return addLocalVariable(name, this.type);
    }
    public Reference addLocalVariable(String name, Type type) {
        return findLocalVariable(name).orElseGet(() -> {
            var fixedName = caseStrategy.apply(name);
            var ref = new Reference(fixedName, type);
            variables.add(ref);
            return ref;
        });
    }
    public Reference addLocalConstant(String name, Expression expr) {
        if (findLocalVariable(name).isPresent()) {
            String msg = String.format("name already exists in scope, cannot add twice: %s", name);
            throw new RuntimeException(msg);
        }
        var fixedName = caseStrategy.apply(name);
        var ref = new Reference(fixedName, expr);
        variables.add(ref);
        return ref;
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
            return parent.findVariable(fixedName);
        }
        return Optional.empty();
    }
    public List<Reference> findByType(Type type) {
        return variables.stream()
                .filter(ref -> ref.type() == type)
                .collect(Collectors.toList());
    }

    public void addScope(Scope scope) {
        System.out.printf("Adding scope %s to %s\n", scope.getName(), this.getName());
        this.scopes.add(scope);
    }
    public List<Scope> getScopes() {
        return scopes;
    }
    public Optional<Scope> findScope(String name) {
        return scopes.stream().filter(s -> s.getName().equals(name)).findFirst();
    }

    public enum Type {
        GLOBAL,
        LOCAL,
        PARAMETER,
        RETURN_VALUE,
        INTRINSIC,
        CONSTANT
    }
}
