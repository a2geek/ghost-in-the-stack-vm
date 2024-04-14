package a2geek.ghost.model.expression;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class BinaryExpression implements Expression {
    private static final List<Descriptor> descriptors = new ArrayList<>();
    static {
        op("+").bothTypesAre(DataType.STRING).stringReduction((a, b)->a+b).result(DataType.STRING)
                .and().leftTypeIs(DataType.ADDRESS, DataType.STRING).coerceLeftTo(DataType.ADDRESS).coerceRightTo(DataType.INTEGER).result(DataType.ADDRESS)
                .and().rightTypeIs(DataType.ADDRESS, DataType.STRING).coerceRightTo(DataType.ADDRESS).coerceLeftTo(DataType.INTEGER).result(DataType.ADDRESS)
                .and().anyType().coerceBothTo(DataType.INTEGER).integerReduction(Integer::sum).result(DataType.INTEGER);
        op("-").leftTypeIs(DataType.ADDRESS).coerceRightTo(DataType.INTEGER).result(DataType.ADDRESS)
                .and().rightTypeIs(DataType.ADDRESS).coerceLeftTo(DataType.INTEGER).result(DataType.ADDRESS)
                .and().anyType().coerceBothTo(DataType.INTEGER).integerReduction((a, b)->a-b).result(DataType.INTEGER);
        op("*").anyType().coerceBothTo(DataType.INTEGER).integerReduction((a,b)->a*b).result(DataType.INTEGER);
        op("/").anyType().coerceBothTo(DataType.INTEGER).integerReduction((a,b)->a/b).result(DataType.INTEGER);
        op("mod").anyType().coerceBothTo(DataType.INTEGER).integerReduction((a,b)->a%b).result(DataType.INTEGER);
        op("^").anyType().coerceBothTo(DataType.INTEGER).integerReduction((a,b)->(int)Math.pow(a,b)).result(DataType.INTEGER);
        op("=").bothTypesAre(DataType.STRING).result(DataType.BOOLEAN)
                .and().anyType().coerceBothTo(DataType.INTEGER).integerReduction((a, b)-> Objects.equals(a, b) ? 1 : 0).result(DataType.BOOLEAN);
        op("<>").bothTypesAre(DataType.STRING).result(DataType.BOOLEAN)
                .and().anyType().coerceBothTo(DataType.INTEGER).integerReduction((a, b)-> !Objects.equals(a, b) ? 1 : 0).result(DataType.BOOLEAN);
        op("<").anyType().coerceBothTo(DataType.INTEGER).integerReduction((a,b)->a<b ? 1 : 0).result(DataType.BOOLEAN);
        op("<=").anyType().coerceBothTo(DataType.INTEGER).integerReduction((a,b)->a<=b ? 1 : 0).result(DataType.BOOLEAN);
        op(">").anyType().coerceBothTo(DataType.INTEGER).integerReduction((a,b)->a>b ? 1 : 0).result(DataType.BOOLEAN);
        op(">=").anyType().coerceBothTo(DataType.INTEGER).integerReduction((a,b)->a>=b ? 1 : 0).result(DataType.BOOLEAN);
        op("or").bothTypesAre(DataType.BOOLEAN).booleanReduction((a,b)->a||b).result(DataType.BOOLEAN)
                .and().anyType().coerceBothTo(DataType.INTEGER).integerReduction((a, b)->a|b).result(DataType.INTEGER);
        op("and").bothTypesAre(DataType.BOOLEAN).booleanReduction((a,b)->a&&b).result(DataType.BOOLEAN)
                .and().anyType().coerceBothTo(DataType.INTEGER).integerReduction((a, b)->a&b).result(DataType.INTEGER);
        op("xor").bothTypesAre(DataType.BOOLEAN).booleanReduction((a,b)->a^b).result(DataType.BOOLEAN)
                .and().anyType().coerceBothTo(DataType.INTEGER).integerReduction((a, b)->a^b).result(DataType.INTEGER);
        op("<<").anyType().coerceBothTo(DataType.INTEGER).integerReduction((a, b)->a<<b).result(DataType.INTEGER);
        op(">>").anyType().coerceBothTo(DataType.INTEGER).integerReduction((a, b)->a>>b).result(DataType.INTEGER);
    }

    private final Descriptor descriptor;
    private Expression l;
    private Expression r;

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
        return descriptor.operation;
    }

    @Override
    public DataType getType() {
        return descriptor.resultType;
    }

    @Override
    public boolean isConstant() {
        return getL().isConstant() && getR().isConstant();
    }

    public BinaryExpression(Expression l, Expression r, String op) {
        op = op.toLowerCase();
        Descriptor descriptor = null;
        for (var d : descriptors) {
            if (Objects.equals(op, d.operation) && d.requirements.test(l,r)) {
                descriptor = d;
                break;
            }
        }
        if (descriptor == null) {
            var msg = String.format("unknown operation: (%s %s %s)", l, op, r);
            throw new RuntimeException(msg);
        }
        this.l = l.checkAndCoerce(descriptor.leftType);
        this.r = r.checkAndCoerce(descriptor.rightType);
        this.descriptor = descriptor;
    }
    public BinaryExpression(String op, Expression l, Expression r) {
        // this seems a better arrangement, at least sometimes?
        this(l, r, op);
    }

    @Override
    public Optional<Boolean> asBoolean() {
        if (isConstant()) {
            if (descriptor.booleanReduction != null) {
                Boolean left = this.getL().asBoolean().orElseThrow(() -> new RuntimeException("expecting a left boolean constant: " + toString()));
                Boolean right = this.getR().asBoolean().orElseThrow(() -> new RuntimeException("expecting a right boolean constant: " + toString()));
                return Optional.of(descriptor.booleanReduction.apply(left, right));
            }
            else if (descriptor.integerReduction != null) {
                return asInteger().map(i -> i != 0);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Integer> asInteger() {
        if (isConstant()) {
            if (descriptor.integerReduction != null) {
                Integer left = this.getL().asInteger().orElseThrow(() -> new RuntimeException("expecting a left integer constant: " + toString()));
                Integer right = this.getR().asInteger().orElseThrow(() -> new RuntimeException("expecting a right integer constant: " + toString()));
                return Optional.of(descriptor.integerReduction.apply(left, right));
            }
            else if (descriptor.booleanReduction != null) {
                return asBoolean().map(b -> b ? 1 : 0);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> asString() {
        if (isConstant()) {
            if (descriptor.stringReduction != null) {
                String left = this.getL().asString().orElseThrow(() -> new RuntimeException("expecting a left string constant: " + toString()));
                String right = this.getR().asString().orElseThrow(() -> new RuntimeException("expecting a right string constant: " + toString()));
                return Optional.of(descriptor.stringReduction.apply(left, right));
            }
            else if (descriptor.booleanReduction != null) {
                return asBoolean().map(b -> b ? "True" : "False");
            }
            else if (descriptor.integerReduction != null) {
                return asInteger().map(i -> Integer.toString(i));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof BinaryExpression that) {
            return Objects.equals(l, that.l) && Objects.equals(r, that.r) && Objects.equals(descriptor, that.descriptor);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(l, r, descriptor);
    }

    @Override
    public String toString() {
        return String.format("(%s %s %s)", l, descriptor.operation, r);
    }

    private record Descriptor(String operation, BiPredicate<Expression,Expression> requirements,
                      BiFunction<Integer,Integer,Integer> integerReduction,
                      BiFunction<Boolean,Boolean,Boolean> booleanReduction,
                      BiFunction<String,String,String> stringReduction,
                      DataType leftType, DataType rightType, DataType resultType) {
    }
    private static Builder op(String operation) {
        return new Builder(operation);
    }
    private static class Builder {
        private final String operation;
        private BiPredicate<Expression,Expression> requirements;
        private BiFunction<Integer,Integer,Integer> integerReduction;
        private BiFunction<Boolean,Boolean,Boolean> booleanReduction;
        private BiFunction<String,String,String> stringReduction;
        private DataType leftType;
        private DataType rightType;

        Builder(String operation) {
            Objects.requireNonNull(operation);
            this.operation = operation;
        }
        Builder bothTypesAre(DataType ...dataTypes) {
            Objects.requireNonNull(dataTypes, operation);
            addRequirement((l, r) -> l.isType(dataTypes) && r.isType(dataTypes));
            if (dataTypes.length == 1) {
                return coerceBothTo(dataTypes[0]);
            }
            return this;
        }
        Builder leftTypeIs(DataType ...dataTypes) {
            Objects.requireNonNull(dataTypes, operation);
            addRequirement((l, r) -> l.isType(dataTypes));
            if (dataTypes.length == 1) {
                return coerceLeftTo(dataTypes[0]);
            }
            return this;
        }
        Builder rightTypeIs(DataType ...dataTypes) {
            Objects.requireNonNull(dataTypes, operation);
            addRequirement((l, r) -> r.isType(dataTypes));
            if (dataTypes.length == 1) {
                return coerceRightTo(dataTypes[0]);
            }
            return this;
        }
        Builder anyType() {
            addRequirement((l,r) -> true);
            return this;
        }

        Builder coerceBothTo(DataType dataType) {
            Objects.requireNonNull(dataType, operation);
            this.leftType = dataType;
            this.rightType = dataType;
            return this;
        }
        Builder coerceLeftTo(DataType dataType) {
            Objects.requireNonNull(dataType, operation);
            this.leftType = dataType;
            return this;
        }
        Builder coerceRightTo(DataType dataType) {
            Objects.requireNonNull(dataType, operation);
            this.rightType = dataType;
            return this;
        }

        Builder stringReduction(BiFunction<String,String,String> stringReduction) {
            Objects.requireNonNull(stringReduction, operation);
            this.stringReduction = stringReduction;
            return this;
        }
        Builder booleanReduction(BiFunction<Boolean,Boolean,Boolean> booleanReduction) {
            Objects.requireNonNull(booleanReduction, operation);
            this.booleanReduction = booleanReduction;
            return this;
        }
        Builder integerReduction(BiFunction<Integer,Integer,Integer> integerReduction) {
            Objects.requireNonNull(integerReduction, operation);
            this.integerReduction = integerReduction;
            return this;
        }

        Continuation result(DataType resultType) {
            Objects.requireNonNull(resultType);

            // validation to help prevent stupid programmer errors
            Objects.requireNonNull(requirements, operation);
            Objects.requireNonNull(leftType, operation);
            Objects.requireNonNull(rightType, operation);

            // add to our list
            var d = new Descriptor(operation, requirements, integerReduction, booleanReduction, stringReduction, leftType, rightType, resultType);
            BinaryExpression.descriptors.add(d);

            return new Continuation(this);
        }

        void addRequirement(BiPredicate<Expression,Expression> requirement) {
            if (requirements != null) {
                requirements = requirements.and(requirement);
            }
            else {
                requirements = requirement;
            }
        }
    }
    static class Continuation {
        private final Builder previous;
        
        private Continuation(Builder builder) {
            this.previous = builder;
        }
        
        Builder and() {
            return new Builder(previous.operation);
        }
    }
}
