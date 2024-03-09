package a2geek.ghost.model;

public interface MemoryManagement {
    boolean isUsingMemory();
    Expression allocate(Symbol symbol, Expression bytes);
    void deallocateAll();
    MemoryManagement create();
}
