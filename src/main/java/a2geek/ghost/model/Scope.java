package a2geek.ghost.model;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static a2geek.ghost.model.Symbol.in;
import static a2geek.ghost.model.Symbol.named;

public class Scope extends StatementBlock {
    private final Function<String,String> caseStrategy;
    private String name;
    private Scope parent;
    private final DeclarationType defaultDeclarationType;
    private final List<Symbol> symbolTable = new ArrayList<>();
    private final Set<Symbol> exports = new HashSet<>();
    private OnErrorContext onErrorContext;
    private MemoryManagement memoryManagementStrategy;
    /** Tracking a distinct global label number to prevent name collisions. */
    private static int symbolNumber = 1;

    public static void reset() {
        symbolNumber = 1;
    }
    public static int nextSymbolNumber() {
        symbolNumber += 1;
        return symbolNumber;
    }

    public Scope(Function<String,String> caseStrategy, MemoryManagement memoryManagementStrategy, String name) {
        this.caseStrategy = caseStrategy;
        this.memoryManagementStrategy = memoryManagementStrategy;
        this.name = caseStrategy.apply(name);
        this.defaultDeclarationType = DeclarationType.GLOBAL;
    }
    public Scope(Scope parent, String name, DeclarationType defaultDeclarationType) {
        this.caseStrategy = parent.caseStrategy;
        this.memoryManagementStrategy = parent.getMemoryManagementStrategy().create();
        this.name = caseStrategy.apply(name);
        this.parent = parent;
        this.defaultDeclarationType = defaultDeclarationType;
    }

    public void addAllExports(Collection<String> exportNames) {
        for (String exportName : exportNames) {
            String moduleName = caseStrategy.apply(getName());
            String targetName = caseStrategy.apply(String.format("%s.%s", moduleName, exportName));
            exports.add(Symbol.variable(exportName, SymbolType.ALIAS).targetName(targetName).build());
        }
    }

    public boolean sameParent(Scope scope) {
        // they should actually be the same Scope object, right?
        return this.parent == scope.parent;
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

    public String getFullPathName() {
        if (parent == null) {
            return null;  // we don't want MAIN in our name list
        }
        var parentName = parent.getFullPathName();
        if (parentName == null) {
            return name;
        }
        return String.format("%s.%s", parentName, name);
    }

    public OnErrorContext getOnErrorContext() {
        return onErrorContext;
    }
    public void setOnErrorContext(OnErrorContext onErrorContext) {
        this.onErrorContext = onErrorContext;
    }

    public MemoryManagement getMemoryManagementStrategy() {
        return memoryManagementStrategy;
    }
    public void setMemoryManagementStrategy(MemoryManagement memoryManagementStrategy) {
        this.memoryManagementStrategy = memoryManagementStrategy;
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

    /** Generate a temporary variable within this scope. */
    public Symbol addTempVariable(DataType dataType) {
        symbolNumber+= 1;
        var name = String.format("_temp%d", symbolNumber);
        return addLocalSymbol(Symbol.variable(name, SymbolType.VARIABLE).dataType(dataType).temporary(true));
    }

    /** Generate labels for code. The multiple values is to allow grouping of labels (same label number) for complex structures. */
    public List<Symbol> addLabels(String... names) {
        symbolNumber+= 1;
        List<Symbol> symbols = new ArrayList<>();
        for (var name : names) {
            var builder = Symbol.label(String.format("_%s%d", name, symbolNumber));
            symbols.add(addLocalSymbol(builder));
        }
        return symbols;
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
                var module = namespaced.scope();
                var newPrefix = String.format("%s%s.", prefix, namespaced.name());
                return Stream.concat(Stream.concat(module.exports.stream(), Stream.of(namespaced)),
                    module.streamAllLocalScope(newPrefix).filter(in(SymbolType.FUNCTION, SymbolType.SUBROUTINE,
                            SymbolType.VARIABLE, SymbolType.CONSTANT)));
            }
            else {
                return Stream.of(namespaced);
            }
        });
    }
}
