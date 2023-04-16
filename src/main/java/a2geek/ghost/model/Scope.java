package a2geek.ghost.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
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

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && variables.isEmpty() && scopes.isEmpty();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        // Renaming is needed for libraries (prefix library name to the method name)
        this.name = name;
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
    public Reference addLocalVariable(String name, DataType dataType) {
        return addLocalVariable(name, this.type, dataType);
    }
    public Reference addLocalVariable(String name, Type type, DataType dataType) {
        return findLocalVariable(name).orElseGet(() -> {
            var fixedName = caseStrategy.apply(name);
            var ref = Reference.builder(fixedName, type).dataType(dataType).build();
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
        var ref = Reference.builder(fixedName, expr).build();
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
    public List<Reference> findByType(Type... types) {
        final var typesList = Arrays.asList(types);
        return variables.stream()
                .filter(ref -> typesList.contains(ref.type()))
                .collect(Collectors.toList());
    }

    public void addScope(Scope scope) {
        Consumer<Scope> alreadyExists = s -> {
            throw new RuntimeException(s.getName() + " already exists");
        };
        Runnable addNew = () -> this.scopes.add(scope);
        findScope(scope.getName()).ifPresentOrElse(alreadyExists, addNew);
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
