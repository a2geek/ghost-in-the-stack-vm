package a2geek.ghost.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static a2geek.ghost.model.Symbol.in;

public class Scope extends StatementBlock {
    private Function<String,String> caseStrategy;
    private String name;
    private Scope parent;
    private DeclarationType defaultDeclarationType;
    private List<Symbol> symbols = new ArrayList<>();

    public Scope(Function<String,String> caseStrategy, String name) {
        this.caseStrategy = caseStrategy;
        this.name = caseStrategy.apply(name);
        this.defaultDeclarationType = DeclarationType.GLOBAL;
    }
    public Scope(Scope parent, String name) {
        this.caseStrategy = parent.caseStrategy;
        this.name = caseStrategy.apply(name);
        this.parent = parent;
        this.defaultDeclarationType = DeclarationType.LOCAL;
    }

    @Override
    public boolean isEmpty() {
        // no code and no symbols
        return super.isEmpty() && symbols.isEmpty();
    }

    public String getName() {
        return name;
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
        if (builder.declarationType() == null) {
            builder.declarationType(defaultDeclarationType);
        }
        return findLocalSymbols(builder.name())
            .map(symbol -> {
                if (builder.equals(symbol)) {
                    return symbol;
                }
                var msg = String.format("name already exists in scope and is a different type, cannot override: %s", builder.name());
                throw new RuntimeException(msg);
            })
            .orElseGet(() -> {
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
    public List<Symbol> findByType(SymbolType... types) {
        final var typesList = Arrays.asList(types);
        return symbols.stream()
                .filter(ref -> typesList.contains(ref.symbolType()))
                .collect(Collectors.toList());
    }
    public List<Symbol> findAllLocalScope(Predicate<Symbol> condition) {
        return symbols.stream().filter(condition).toList();
    }

    public void addScope(Scope scope) {
        Consumer<Scope> alreadyExists = s -> {
            throw new RuntimeException(s.getName() + " already exists");
        };
        Runnable addNew = () -> addLocalSymbol(Symbol.scope(scope));
        findLocalScope(scope.getName()).ifPresentOrElse(alreadyExists, addNew);
    }
    public List<Scope> getScopes() {
        return symbols.stream()
                .filter(in(SymbolType.FUNCTION, SymbolType.SUBROUTINE))
                .map(Symbol::scope)
                .toList();
    }
    public Optional<Scope> findLocalScope(String name) {
        return symbols.stream()
                .filter(in(SymbolType.FUNCTION, SymbolType.SUBROUTINE))
                .filter(s -> s.name().equals(name))
                .map(Symbol::scope)
                .findFirst();
    }
    public Optional<Scope> findScope(String name) {
        var scope = findLocalScope(name);
        if (scope.isEmpty() && parent != null) {
            return parent.findLocalScope(name);
        }
        return scope;
    }

}
