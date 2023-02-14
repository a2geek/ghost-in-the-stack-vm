package a2geek.ghost.model;

public interface Statement {
    void accept(Visitor visitor);
}
