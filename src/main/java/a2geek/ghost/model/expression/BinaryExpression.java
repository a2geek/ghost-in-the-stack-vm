package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;

import java.util.*;
import java.util.function.BiFunction;

public class BinaryExpression implements Expression {
    private static final List<Descriptor> DESCRIPTORS = List.of(
        // Arithmetic
        new Descriptor("+", DataType.INTEGER, DataType.INTEGER, DataType.BOOLEAN),
        new Descriptor("+", DataType.ADDRESS, DataType.STRING, DataType.INTEGER),       // STR + n  => ADDR
        new Descriptor("+", DataType.ADDRESS, DataType.ADDRESS, DataType.INTEGER),      // ADDR + n => ADDR
        new Descriptor("-", DataType.INTEGER, DataType.INTEGER, DataType.BOOLEAN),
        new Descriptor("-", DataType.ADDRESS, DataType.ADDRESS, DataType.INTEGER),      // ADDR - n => ADDR
        new Descriptor("*", DataType.INTEGER, DataType.INTEGER, DataType.BOOLEAN),
        new Descriptor("/", DataType.INTEGER, DataType.INTEGER, DataType.BOOLEAN),
        new Descriptor("mod", DataType.INTEGER, DataType.INTEGER, DataType.BOOLEAN),
        new Descriptor("^", DataType.INTEGER, DataType.INTEGER, DataType.BOOLEAN),
        // Comparison
        new Descriptor("<", DataType.BOOLEAN, DataType.INTEGER, DataType.BOOLEAN, DataType.ADDRESS),
        new Descriptor(">", DataType.BOOLEAN, DataType.INTEGER, DataType.BOOLEAN, DataType.ADDRESS),
        new Descriptor("<=", DataType.BOOLEAN, DataType.INTEGER, DataType.BOOLEAN, DataType.ADDRESS),
        new Descriptor(">=", DataType.BOOLEAN, DataType.INTEGER, DataType.BOOLEAN, DataType.ADDRESS),
        new Descriptor("=", DataType.BOOLEAN, DataType.INTEGER, DataType.BOOLEAN, DataType.ADDRESS),
        new Descriptor("<>", DataType.BOOLEAN, DataType.INTEGER, DataType.BOOLEAN, DataType.ADDRESS),
        // Logical
        new Descriptor("or", DataType.BOOLEAN, DataType.BOOLEAN),
        new Descriptor("and", DataType.BOOLEAN, DataType.BOOLEAN),
        new Descriptor("xor", DataType.BOOLEAN, DataType.BOOLEAN),
        // Bit
        new Descriptor("or", DataType.INTEGER, DataType.INTEGER),
        new Descriptor("and", DataType.INTEGER, DataType.INTEGER),
        new Descriptor("xor", DataType.INTEGER, DataType.INTEGER),
        new Descriptor("<<", DataType.INTEGER, DataType.INTEGER, DataType.BOOLEAN),
        // FIXME: STRING needs to be replaced here
        new Descriptor(">>", DataType.INTEGER, DataType.INTEGER, DataType.BOOLEAN, DataType.STRING, DataType.ADDRESS)
    );
    public static Optional<Descriptor> findDescriptor(String operator, DataType left, DataType right) {
        for (var d : DESCRIPTORS) {
            if (d.operator().equalsIgnoreCase(operator) && d.validateArgTypes(left, right)) {
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }

    private static final Map<String, BiFunction<Integer,Integer,Integer>> INTEGER_OPS;
    static {
        // 'Map.of(...)' maxes out at 10 items.
        INTEGER_OPS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        // Arithmetic
        INTEGER_OPS.putAll(Map.of(
            "+",   (a,b) -> a+b,
            "-",   (a,b) -> a-b,
            "*",   (a,b) -> a*b,
            "/",   (a,b) -> a/b,
            "mod", (a,b) -> a%b,
            "^", (a,b) -> (int)Math.pow(a,b)
        ));
        // Comparison
        INTEGER_OPS.putAll(Map.of(
            "<",   (a,b) -> (a<b) ? 1 : 0,
            "<=",  (a,b) -> (a<=b) ? 1 : 0,
            ">",   (a,b) -> (a>b) ? 1 : 0,
            ">=",  (a,b) -> (a>=b) ? 1 : 0,
            "=",   (a,b) -> (a==b) ? 1 : 0,
            "<>",  (a,b) -> (a!=b) ? 1 : 0
        ));
        // Bit
        INTEGER_OPS.putAll(Map.of(
            "or",  (a,b) -> a|b,
            "and", (a,b) -> a&b,
            "xor", (a,b) -> a^b,
            "<<", (a,b) -> a<<b,
            ">>", (a,b) -> a>>b
        ));
    }

    private static final Map<String, BiFunction<Boolean,Boolean,Boolean>> LOGICAL_OPS;
    static {
        // 'Map.of(...)' maxes out at 10 items.
        LOGICAL_OPS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        // Logical
        LOGICAL_OPS.putAll(Map.of(
            "or",  (a,b) -> a || b,
            "and", (a,b) -> a && b,
            "xor", (a,b) -> a ^ b
        ));
    }

    private Descriptor descriptor;
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
        return descriptor.returnType();
    }

    public Descriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public boolean isConstant() {
        return getL().isConstant() && getR().isConstant();
    }

    public BinaryExpression(Expression l, Expression r, String op) {
        Descriptor d = findDescriptor(op, l.getType(), r.getType()).orElseThrow(() ->
            new RuntimeException(String.format("argument types not supported for '%s': %s, %s",
                    op, l.getType(), r.getType()))
        );

        this.l = l;
        this.r = r;
        this.op = op.toLowerCase();
        this.descriptor = d;
    }
    public BinaryExpression(String op, Expression l, Expression r) {
        // this seems a better arrangement, at least sometimes?
        this(l, r, op);
    }

    @Override
    public Optional<Boolean> asBoolean() {
        if (isConstant()) {
            if (INTEGER_OPS.containsKey(op)) {
                return asInteger().map(i -> i != 0);
            }
            if (LOGICAL_OPS.containsKey(op)) {
                Boolean left = this.getL().asBoolean().orElseThrow(() -> new RuntimeException("expecting a left boolean constant: " + toString()));
                Boolean right = this.getR().asBoolean().orElseThrow(() -> new RuntimeException("expecting a right boolean constant: " + toString()));
                return Optional.of(LOGICAL_OPS.get(op).apply(left, right));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Integer> asInteger() {
        if (isConstant() && INTEGER_OPS.containsKey(op)) {
            Integer left = this.getL().asInteger().orElseThrow(() -> new RuntimeException("expecting a left integer constant: " + toString()));
            Integer right = this.getR().asInteger().orElseThrow(() -> new RuntimeException("expecting a right integer constant: " + toString()));
            return Optional.of(INTEGER_OPS.get(op).apply(left, right));
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> asString() {
        if (isConstant()) {
            return switch (descriptor.returnType()) {
                case BOOLEAN -> asBoolean().map(b -> b ? "True" : "False");
                case INTEGER -> asInteger().map(i -> Integer.toString(i));
                case STRING -> throw new RuntimeException("unable to evaluate string expressions at this time");
                case ADDRESS -> throw new RuntimeException("unable to evaluate address expressions at this time");
            };
        }
        return Optional.empty();
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
        return String.format("(%s %s %s)", l, op, r);
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
