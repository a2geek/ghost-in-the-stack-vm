package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;

import java.util.*;

public class BinaryExpression implements Expression {
    private static final List<Descriptor> OPS = List.of(
        // Arithmetic
        new Descriptor("+", DataType.INTEGER, DataType.INTEGER, DataType.BOOLEAN),
        new Descriptor("-", DataType.INTEGER, DataType.INTEGER, DataType.BOOLEAN),
        new Descriptor("*", DataType.INTEGER, DataType.INTEGER, DataType.BOOLEAN),
        new Descriptor("/", DataType.INTEGER, DataType.INTEGER, DataType.BOOLEAN),
        new Descriptor("mod", DataType.INTEGER, DataType.INTEGER, DataType.BOOLEAN),
        // Comparison
        new Descriptor("<", DataType.BOOLEAN, DataType.INTEGER, DataType.BOOLEAN),
        new Descriptor(">", DataType.BOOLEAN, DataType.INTEGER, DataType.BOOLEAN),
        new Descriptor("<=", DataType.BOOLEAN, DataType.INTEGER, DataType.BOOLEAN),
        new Descriptor(">=", DataType.BOOLEAN, DataType.INTEGER, DataType.BOOLEAN),
        new Descriptor("=", DataType.BOOLEAN, DataType.INTEGER, DataType.BOOLEAN),
        new Descriptor("<>", DataType.BOOLEAN, DataType.INTEGER, DataType.BOOLEAN),
        // Logical
        new Descriptor("or", DataType.BOOLEAN, DataType.BOOLEAN),
        new Descriptor("and", DataType.BOOLEAN, DataType.BOOLEAN),
        new Descriptor("xor", DataType.BOOLEAN, DataType.BOOLEAN),
        // Bit
        new Descriptor("or", DataType.INTEGER, DataType.INTEGER),
        new Descriptor("and", DataType.INTEGER, DataType.INTEGER),
        new Descriptor("xor", DataType.INTEGER, DataType.INTEGER),
        new Descriptor("<<", DataType.INTEGER, DataType.INTEGER, DataType.BOOLEAN),
        new Descriptor(">>", DataType.INTEGER, DataType.INTEGER, DataType.BOOLEAN)
    );
    public static Optional<Descriptor> findDescriptor(String operator, DataType left, DataType right) {
        for (var d : OPS) {
            if (d.operator().equalsIgnoreCase(operator) && d.validateArgTypes(left, right)) {
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }

    private DataType type;
    private Expression l;
    private Expression r;
    private String op;

    public Expression getL() {
        return l;
    }

    public void setL(Expression l) {
        this.l = l;
    }

    public Expression getR() {
        return r;
    }

    public void setR(Expression r) {
        this.r = r;
    }

    public String getOp() {
        return op;
    }

    @Override
    public DataType getType() {
        return type;
    }

    public BinaryExpression(Expression l, Expression r, String op) {
        Descriptor d = findDescriptor(op, l.getType(), r.getType()).orElseGet(() -> {
            String message = String.format("argument types not supported for '%s': %s, %s",
                    op, l.getType(), r.getType());
            throw new RuntimeException(message);
        });

        this.l = l;
        this.r = r;
        this.op = op.toLowerCase();
        this.type = d.returnType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof BinaryExpression that) {
            return Objects.equals(l, that.l) && Objects.equals(r, that.r) && Objects.equals(op, that.op);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(l, r, op);
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", l, op, r);
    }

    public record Descriptor(
            String operator,
            DataType returnType,
            DataType ...allowedTypes
    ) {
        public boolean validateArgTypes(DataType left, DataType right) {
            return isAllowed(left) && isAllowed(right);
        }
        boolean isAllowed(DataType type) {
            for (var t : allowedTypes) {
                if (t == type) return true;
            }
            return false;
        }
    }
}
