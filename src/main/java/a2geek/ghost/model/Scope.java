package a2geek.ghost.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

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
    public Symbol addAliasToParent(String aliasName, Symbol target) {
        if (parent != null) {
            return parent.addAliasToParent(aliasName, target);
        }
        return addLocalSymbol(Symbol.variable(aliasName, SymbolType.ALIAS).targetName(target.name()));
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
        return symbolTable.stream().filter(condition).findFirst();
    }
    public List<Symbol> findAllLocalScope(Predicate<Symbol> condition) {
        return symbolTable.stream().filter(condition).toList();
    }
}
