package a2geek.ghost.memorymanagement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.MemoryManagement;
import a2geek.ghost.model.ModelBuilder;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.expression.VariableReference;

import java.util.HashMap;
import java.util.Map;

public class StackMemoryManagement implements MemoryManagement {
    private final ModelBuilder model;
    private final Map<Symbol, Expression> allocations = new HashMap<>();

    public StackMemoryManagement(ModelBuilder model) {
        this.model = model;
    }

    @Override
    public boolean isUsingHeap() {
        return false;
    }

    @Override
    public Expression allocate(Symbol symbol, Expression bytes) {
        allocations.put(symbol, bytes);
        if (bytes.asInteger().isEmpty()) {
            var temp = VariableReference.with(model.addGeneratedVariable(bytes.getType()));
            model.assignStmt(temp, bytes);
            allocations.put(symbol, temp);
        }
        return model.callFunction("alloc", allocations.get(symbol));
    }

    @Override
    public void deallocateAll() {
        allocations.forEach((symbol,bytes) -> {
            model.callSubroutine("dealloc", bytes);
        });
        allocations.clear();
    }

    @Override
    public void incrementReferenceCount(Expression expr) {
        // not applicable
    }

    @Override
    public void decrementReferenceCount(Expression expr) {
        // not applicable
    }

    @Override
    public MemoryManagement create() {
        return new StackMemoryManagement(model);
    }
}