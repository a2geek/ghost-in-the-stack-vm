package a2geek.ghost.model.visitor;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Visitor;
import a2geek.ghost.model.expression.BinaryExpression;
import a2geek.ghost.model.expression.IntegerConstant;

import java.util.Arrays;
import java.util.List;

public class StrengthReductionVisitor extends Visitor {
    private static final List<Integer> POWERS2 = Arrays.asList(
                0x0002, 0x0004, 0x0008, 0x0010, 0x0020, 0x0040, 0x0080,
        0x0100, 0x0200, 0x0400, 0x0800, 0x1000, 0x2000, 0x4000, 0x8000
    );
    private static final List<Integer> MASK2 = Arrays.asList(
        0x0001, 0x0003, 0x0007, 0x000f, 0x001f, 0x003f, 0x007f, 0x00ff,
        0x01ff, 0x03ff, 0x07ff, 0x0fff, 0x1fff, 0x3fff, 0x7fff, 0xffff
    );

    @Override
    public Expression visit(BinaryExpression expression) {
        dispatch(expression.getL()).ifPresent(expression::setL);
        dispatch(expression.getR()).ifPresent(expression::setR);

        var lhs = expression.getL();
        var rhs = expression.getR();

        switch (expression.getOp()) {
            case "*" -> {
                // multiplication identity  A*1 or 1*A
                if (matches(lhs,1)) return rhs;
                if (matches(rhs,1)) return lhs;
                // negate  A*-1 or -1*A
                if (matches(lhs,-1)) return rhs.negate();
                if (matches(rhs,-1)) return lhs.negate();
                // reduction to shifts   A*(2^n)
                if (pow2(lhs) > 0) return lshift(rhs,pow2(lhs));
                if (pow2(rhs) > 0) return lshift(lhs,pow2(rhs));
            }
            case "/" -> {
                // division identity  A/1
                if (matches(rhs,1)) return lhs;
                // reduction to shifts    A/(2^n)
                if (pow2(rhs) > 0) return rshift(lhs,pow2(rhs));
            }
            case "+" -> {
                // additive identity   A+0   0+A
                if (matches(lhs,0)) return rhs;
                if (matches(rhs,0)) return lhs;
                // reduction to shift (A<<2 is faster than A+A maybe)
                if (lhs.equals(rhs)) return lshift(lhs,2);
            }
            case "-" -> {
                // subtraction identity   A-0
                if (matches(rhs,0)) return lhs;
                // negate    0-A
                if (matches(lhs,0)) return rhs.negate();
            }
            case "mod" -> {
                // bitwise AND    A mod 2^n ==> A & (2^n)-1
                if (pow2(rhs) > 0) return bitand(lhs,MASK2.get(pow2(rhs)-1));
            }
        }

        return null;
    }

    public boolean matches(Expression expression, int value) {
        return expression.asInteger().map(e -> e == value).orElse(false);
    }
    public BinaryExpression lshift(Expression expression, int bits) {
        return expression.lshift(constant(bits));
    }
    public BinaryExpression rshift(Expression expression, int bits) {
        return expression.rshift(constant(bits));
    }
    public BinaryExpression bitand(Expression expression, int mask) {
        return expression.and(constant(mask));
    }
    public IntegerConstant constant(int value) {
        return new IntegerConstant(value);
    }
    public int pow2(Expression expression) {
        var value = expression.asInteger();
        if (value.isPresent()) {
            var num = value.get();
            if (POWERS2.contains(num)) return POWERS2.indexOf(num)+1;
        }
        return -1;
    }
}
