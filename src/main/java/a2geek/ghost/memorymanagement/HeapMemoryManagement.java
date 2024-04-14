package a2geek.ghost.memorymanagement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.MemoryManagement;
import a2geek.ghost.model.ModelBuilder;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.expression.VariableReference;

import java.util.HashSet;
import java.util.Set;

public class HeapMemoryManagement implements MemoryManagement {
    private final ModelBuilder model;
    private final Set<Symbol> allocations = new HashSet<>();

    public HeapMemoryManagement(ModelBuilder model) {
        this.model = model;
    }

    @Override
    public boolean isUsingHeap() {
        return true;
    }

    @Override
    public Expression allocate(Symbol symbol, Expression bytes) {
        allocations.add(symbol);
        return model.callFunction("memory.HeapAlloc", bytes);
    }

    @Override
    public void deallocateAll() {
        allocations.forEach(symbol -> {
            model.callSubroutine("memory.HeapFree", VariableReference.with(symbol));
        });
        allocations.clear();
    }

    @Override
    public void incrementReferenceCount(Expression expr) {
        model.callSubroutine("memory.HeapRefIncr", expr);
    }

    @Override
    public void decrementReferenceCount(Expression expr) {
        model.callSubroutine("memory.HeapRefDecr", expr);
    }

    @Override
    public MemoryManagement create() {
        return new HeapMemoryManagement(model);
    }
}