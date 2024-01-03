package a2geek.ghost.model;

import a2geek.ghost.model.scope.Subroutine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static a2geek.ghost.model.Symbol.in;
import static a2geek.ghost.model.Symbol.named;

public class Scope extends StatementBlock {
    private Function<String,String> caseStrategy;
    private String name;
    private Scope parent;
    private DeclarationType defaultDeclarationType;
    private List<Symbol> symbolTable = new ArrayList<>();

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
        return super.isEmpty() && symbolTable.isEmpty();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        // Renaming is needed for libraries (prefix library name to the method name)
        this.name = name;
    }

    public List<Symbol> getLocalSymbols() {
        return symbolTable;
    }
    public Symbol addLocalSymbol(Symbol.Builder builder) {
        var fixedName = caseStrategy.apply(builder.name());
        builder.name(fixedName);
        if (builder.declarationType() == null) {
            builder.declarationType(defaultDeclarationType);
        }
        return findFirstLocalScope(named(builder.name()))
            .map(symbol -> {
                if (builder.equals(symbol)) {
                    return symbol;
                }
                var msg = String.format("name already exists in scope and is a different type, cannot override: %s", builder.name());
                throw new RuntimeException(msg);
            })
            .orElseGet(() -> {
                var symbol = builder.build();
                symbolTable.add(symbol);
                return symbol;
            });
    }

    public Optional<Symbol> findFirst(Predicate<Symbol> condition) {
        return findFirstLocalScope(condition).or(() -> {
            if (parent != null) {
                return parent.findFirst(condition);
            }
            return Optional.empty();
        });
    }
    public Optional<Symbol> findFirstLocalScope(Predicate<Symbol> condition) {
        return streamAllLocalScope().filter(condition).findFirst();
    }
    public List<Symbol> findAllLocalScope(Predicate<Symbol> condition) {
        return streamAllLocalScope().filter(condition).toList();
    }

    public Stream<Symbol> streamAll() {
        if (parent != null) {
            return Stream.concat(streamAllLocalScope(), parent.streamAll());
        }
        return streamAllLocalScope();
    }
    public Stream<Symbol> streamAllLocalScope() {
        return this.streamAllLocalScope("");
    }

    /**
     * Synthetically generates exported function aliases as well as "public" members of nested
     * scopes. ("Public" are things like CONST, SUB, and FUNCTION declarations from a module.)
     * Note that nested scopes get renamed on the fly, so the MIN function in the MATH module
     * becomes "MATH.MIN" _except_ when it's the local scope.
     */
    Stream<Symbol> streamAllLocalScope(String prefix) {
        return symbolTable.stream().flatMap(original -> {
            Symbol namespaced = Symbol.from(original).name(prefix + original.name()).build();
            if (namespaced.symbolType() == SymbolType.MODULE) {
                var newPrefix = String.format("%s%s.", prefix, namespaced.name());
                return Stream.concat(
                        Stream.of(namespaced),
                        namespaced.scope().streamAllLocalScope(newPrefix)
                            .filter(in(SymbolType.FUNCTION, SymbolType.SUBROUTINE))
                            .flatMap(symbol -> {
                                if (symbol.scope() instanceof Subroutine sub) {
                                    if (sub.isExport()) {
                                        String origName = sub.getName();
                                        String targetName = String.format("%s.%s", namespaced.name(), origName);
                                        var alias = Symbol.variable(origName, SymbolType.ALIAS).targetName(targetName).build();
                                        return Stream.of(alias, symbol);
                                    }
                                }
                                return Stream.of(symbol);
                            }));
            }
            else {
                return Stream.of(namespaced);
            }
        });
    }
}
