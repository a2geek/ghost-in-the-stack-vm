package a2geek.ghost.model.basic.visitor;

import a2geek.ghost.model.basic.Expression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static a2geek.ghost.antlr.ExpressionBuilder.*;

public class ConstantReductionVisitorTest {
    private ConstantReductionVisitor visitor = new ConstantReductionVisitor();
    private void assertEquals(Expression expected, Expression testCase) {
        var actual = visitor.dispatch(testCase);
        Assertions.assertEquals(expected, actual.orElseThrow());
    }

    @Test
    public void testBinaryExpression() {
        // Arithmetic
        assertEquals(constant(5), binary("+", constant(2), constant(3)));
        assertEquals(constant(-1), binary("-", constant(2), constant(3)));
        assertEquals(constant(6), binary("*", constant(2), constant(3)));
        assertEquals(constant(2), binary("/", constant(7), constant(3)));
        assertEquals(constant(1), binary("mod", constant(7), constant(3)));
        // Comparison
        assertEquals(constant(false), binary("<", constant(7), constant(3)));
        assertEquals(constant(false), binary("<=", constant(7), constant(3)));
        assertEquals(constant(true), binary(">", constant(7), constant(3)));
        assertEquals(constant(true), binary(">=", constant(7), constant(3)));
        assertEquals(constant(false), binary("=", constant(7), constant(3)));
        assertEquals(constant(true), binary("<>", constant(7), constant(3)));
        // Bit
        assertEquals(constant(7), binary("or", constant(7), constant(3)));
        assertEquals(constant(3), binary("and", constant(7), constant(3)));
        assertEquals(constant(4), binary("xor", constant(7), constant(3)));
        assertEquals(constant(56), binary("<<", constant(7), constant(3)));
        assertEquals(constant(0), binary(">>", constant(7), constant(3)));
        // Logical
        assertEquals(constant(true), binary("or", constant(true), constant(false)));
        assertEquals(constant(false), binary("and", constant(true), constant(false)));
        assertEquals(constant(true), binary("xor", constant(true), constant(false)));
    }

    @Test
    public void testUnaryExpression() {
        assertEquals(constant(-5), unary("-", constant(5)));
    }

    @Test
    public void testFunctionExpression() {
        assertEquals(constant(0xc5), function("asc", constant("E")));
    }
}
