package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class BinaryExpression implements Expression {
    public static final Map<String, Descriptor> OPS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static {
        // Arithmetic
        OPS.putAll(Map.of(
            "+", new Descriptor(DataType.INTEGER, false, DataType.INTEGER, DataType.BOOLEAN),
            "-", new Descriptor(DataType.INTEGER, false, DataType.INTEGER, DataType.BOOLEAN),
            "*", new Descriptor(DataType.INTEGER, false, DataType.INTEGER, DataType.BOOLEAN),
            "/", new Descriptor(DataType.INTEGER, false, DataType.INTEGER, DataType.BOOLEAN),
            "mod", new Descriptor(DataType.INTEGER, false, DataType.INTEGER, DataType.BOOLEAN)
        ));
        // Comparison
        OPS.putAll(Map.of(
            "<", new Descriptor(DataType.BOOLEAN, true, DataType.INTEGER, DataType.BOOLEAN),
            ">", new Descriptor(DataType.BOOLEAN, true, DataType.INTEGER, DataType.BOOLEAN),
            "<=", new Descriptor(DataType.BOOLEAN, true, DataType.INTEGER, DataType.BOOLEAN),
            ">=", new Descriptor(DataType.BOOLEAN, true, DataType.INTEGER, DataType.BOOLEAN),
            "=", new Descriptor(DataType.BOOLEAN, true, DataType.INTEGER, DataType.BOOLEAN),
            "<>", new Descriptor(DataType.BOOLEAN, true, DataType.INTEGER, DataType.BOOLEAN)
        ));
        // Logical
        OPS.putAll(Map.of(
            "or", new Descriptor(DataType.BOOLEAN, true, DataType.INTEGER, DataType.BOOLEAN),
            "and", new Descriptor(DataType.BOOLEAN, true, DataType.INTEGER, DataType.BOOLEAN)
        ));
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
        Descriptor d = OPS.get(op);
        if (d == null) {
            throw new RuntimeException("unknown binary operator: " + op);
        }

        if (!d.validateArgTypes(l.getType(), r.getType())) {
            String message = String.format("argument types not supported for '%s': %s, %s",
                    op, l.getType(), r.getType());
            throw new RuntimeException(message);
        }

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
            DataType returnType,
            boolean typesMustMatch,
            DataType ...allowedTypes
    ) {
        public boolean validateArgTypes(DataType left, DataType right) {
            if (typesMustMatch && left != right) return false;
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
