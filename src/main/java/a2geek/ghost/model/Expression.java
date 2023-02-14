package a2geek.ghost.model;

public interface Expression {
    void accept(Visitor visitor);
}
