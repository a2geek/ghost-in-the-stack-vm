package a2geek.ghost.model.scope;

import a2geek.ghost.model.CompilerConfiguration;
import a2geek.ghost.model.MemoryManagement;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.SymbolType;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static a2geek.ghost.model.Symbol.in;

public class Program extends Scope {
    public Program(CompilerConfiguration config, MemoryManagement memoryManagementStrategy) {
        super(config, memoryManagementStrategy, "main");
    }

    @Override
    public String toString() {
        var allScopes = new ArrayList<Scope>();
        findAllLocalScope(in(SymbolType.FUNCTION,SymbolType.SUBROUTINE)).forEach(symbol -> {
            allScopes.add(symbol.scope());
        });
        allScopes.add(this);
        return allScopes.stream()
            .map(scope -> String.format("%s: %s", getDescriptiveName(scope), scope.statementsAsString()))
            .collect(Collectors.joining("\n"));
    }

    public String getDescriptiveName(Scope scope) {
        return switch (scope) {
            case a2geek.ghost.model.scope.Function func ->
                    String.format("%s (FUNC)", func.getName());
            case Subroutine sub ->
                    String.format("%s (SUB)", sub.getName());
            case Program program ->
                    String.format("%s (PROGRAM)", program.getName());
            default ->
                    String.format("%s (SCOPE)", scope.getName());
        };
    }
}
