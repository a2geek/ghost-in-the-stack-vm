package a2geek.ghost.model;

public interface Expression {
    public Type getType();

    public default boolean isType(Type ...types) {
        for (var type : types) {
            if (type == getType()) return true;
        }
        return false;
    }

    public default void mustBe(Type ...types) {
        if (isType(types)) return;

        String message = String.format("Must be type %s but is %s", types, getType());
        throw new RuntimeException(message);
    }

    public enum Type {
        INTEGER,
        STRING
    }
}
