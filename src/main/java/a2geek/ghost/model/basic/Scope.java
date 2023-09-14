package a2geek.ghost.model.basic;

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
    private List<Symbol> symbols = new ArrayList<>();
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
        return super.isEmpty() && symbols.isEmpty() && scopes.isEmpty();
    }

    public String getName() {
        return name;
    }
    public Type getType() {
        return type;
    }

    public void setName(String name) {
        // Renaming is needed for libraries (prefix library name to the method name)
        this.name = name;
    }

    public List<Symbol> getLocalSymbols() {
        return symbols;
    }
    public Symbol addLocalSymbol(Symbol.Builder builder) {
        var fixedName = caseStrategy.apply(builder.name());
        builder.name(fixedName);
        return findLocalSymbols(builder.name())
            .map(symbol -> {
                if (builder.equals(symbol)) {
                    return symbol;
                }
                var msg = String.format("name already exists in scope and is a different type, cannot override: %s", builder.name());
                throw new RuntimeException(msg);
            })
            .orElseGet(() -> {
                if (builder.type() == null) {
                    builder.type(type);
                }
                var symbol = builder.build();
                symbols.add(symbol);
                return symbol;
            });
    }

    public Optional<Symbol> findLocalSymbols(String name) {
        var fixedName = caseStrategy.apply(name);
        return symbols.stream().filter(r -> fixedName.equals(r.name())).findFirst();
    }
    public Optional<Symbol> findSymbol(String name) {
        var fixedName = caseStrategy.apply(name);
        var found = findLocalSymbols(fixedName);
        if (found.isPresent()) {
            return found;
        }
        if (parent != null) {
            return parent.findSymbol(fixedName);
        }
        return Optional.empty();
    }
    public List<Symbol> findByType(Type... types) {
        final var typesList = Arrays.asList(types);
        return symbols.stream()
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
        CONSTANT,
        LABEL
    }
}