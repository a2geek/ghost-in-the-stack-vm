package a2geek.ghost.model;

public interface Expression {
    public DataType getType();

    public default boolean isType(DataType...types) {
        for (var type : types) {
            if (type == getType()) return true;
        }
        return false;
    }

    public default void mustBe(DataType...types) {
        if (isType(types)) return;

        String message = String.format("Must be type %s but is %s", types, getType());
        throw new RuntimeException(message);
    }

}
