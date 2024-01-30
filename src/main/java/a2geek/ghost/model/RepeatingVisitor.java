package a2geek.ghost.model;

/**
 * A 'RepeatingVisitor' tracks how many times it has been used.
 * Intended for visitors that work incrementally.
 */
public interface RepeatingVisitor {
    public int getCounter();
}
