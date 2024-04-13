package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;

public class BinaryExpression implements Expression {
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

    private final DataType dataType;
    private Expression l;
    private Expression r;
    private final String op;

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
        return dataType;
    }

    @Override
    public boolean isConstant() {
        return getL().isConstant() && getR().isConstant();
    }

    public BinaryExpression(Expression l, Expression r, String op) {
        this.op = op.toLowerCase();
        switch (this.op) {
            // Arithmetic
            case "+" -> {
                if (l.isType(DataType.STRING) && r.isType(DataType.STRING)) {
                    this.l = l.checkAndCoerce(DataType.STRING);
                    this.r = r.checkAndCoerce(DataType.STRING);
                    this.dataType = DataType.STRING;
                }
                else if (l.isType(DataType.ADDRESS, DataType.STRING)) {
                    this.l = l.checkAndCoerce(DataType.ADDRESS);
                    this.r = r.checkAndCoerce(DataType.INTEGER);
                    this.dataType = DataType.ADDRESS;
                }
                else if (r.isType(DataType.ADDRESS, DataType.STRING)) {
                    this.l = l.checkAndCoerce(DataType.INTEGER);
                    this.r = r.checkAndCoerce(DataType.ADDRESS);
                    this.dataType = DataType.ADDRESS;
                }
                else {
                    this.l = l.checkAndCoerce(DataType.INTEGER);
                    this.r = r.checkAndCoerce(DataType.INTEGER);
                    this.dataType = DataType.INTEGER;
                }
            }
            case "-" -> {
                if (l.isType(DataType.ADDRESS)) {
                    this.l = l.checkAndCoerce(DataType.ADDRESS);
                    this.r = r.checkAndCoerce(DataType.INTEGER);
                    this.dataType = DataType.ADDRESS;
                }
                else if (r.isType(DataType.ADDRESS)) {
                    this.l = l.checkAndCoerce(DataType.INTEGER);
                    this.r = r.checkAndCoerce(DataType.ADDRESS);
                    this.dataType = DataType.ADDRESS;
                }
                else {
                    this.l = l.checkAndCoerce(DataType.INTEGER);
                    this.r = r.checkAndCoerce(DataType.INTEGER);
                    this.dataType = DataType.INTEGER;
                }
            }
            case "*", "/", "mod", "^" -> {
                this.l = l.checkAndCoerce(DataType.INTEGER);
                this.r = r.checkAndCoerce(DataType.INTEGER);
                this.dataType = DataType.INTEGER;
            }
            // Comparison
            case "=", "<>" -> {
                if (l.isType(DataType.STRING) && r.isType(DataType.STRING)) {
                    this.l = l.checkAndCoerce(DataType.STRING);
                    this.r = r.checkAndCoerce(DataType.STRING);
                    this.dataType = DataType.BOOLEAN;
                }
                else {
                    this.l = l.checkAndCoerce(DataType.INTEGER);
                    this.r = r.checkAndCoerce(DataType.INTEGER);
                    this.dataType = DataType.BOOLEAN;
                }
            }
            case "<", "<=", ">", ">=" -> {
                this.l = l.checkAndCoerce(DataType.INTEGER);
                this.r = r.checkAndCoerce(DataType.INTEGER);
                this.dataType = DataType.BOOLEAN;
            }
            // Logical | Bit
            case "or", "and", "xor" -> {
                if (l.isType(DataType.BOOLEAN) && r.isType(DataType.BOOLEAN)) {
                    this.l = l.checkAndCoerce(DataType.BOOLEAN);
                    this.r = r.checkAndCoerce(DataType.BOOLEAN);
                    this.dataType = DataType.BOOLEAN;
                } else {
                    this.l = l.checkAndCoerce(DataType.INTEGER);
                    this.r = r.checkAndCoerce(DataType.INTEGER);
                    this.dataType = DataType.INTEGER;
                }
            }
            case "<<", ">>" -> {
                this.l = l.checkAndCoerce(DataType.INTEGER);
                this.r = r.checkAndCoerce(DataType.INTEGER);
                this.dataType = DataType.INTEGER;
            }
            default -> {
                throw new RuntimeException("unknown operation: " + this.op);
            }
        }
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
            return switch (dataType) {
                case BOOLEAN -> asBoolean().map(b -> b ? "True" : "False");
                // TODO fix this up when byte becomes a real type
                case INTEGER, BYTE -> asInteger().map(i -> Integer.toString(i));
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
}
