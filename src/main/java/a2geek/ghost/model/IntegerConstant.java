package a2geek.ghost.model;

public class IntegerConstant implements Expression {
    private int value;

    public IntegerConstant(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
