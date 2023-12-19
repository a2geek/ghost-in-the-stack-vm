package a2geek.ghost.model.visitor;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.DeclarationType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Scope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static a2geek.ghost.antlr.ExpressionBuilder.*;

public class StrengthReductionVisitorTest {
    private StrengthReductionVisitor visitor = new StrengthReductionVisitor();
    private void assertEquals(Expression expected, Expression testCase) {
        var actual = visitor.dispatch(testCase);
        Assertions.assertEquals(expected, actual.orElseThrow());
    }

    @Test
    public void testMultiplication() {
    var id = identifier("A", DataType.INTEGER, Scope.Type.VARIABLE, DeclarationType.GLOBAL);
        // identity
        assertEquals(id, binary("*", id, constant(1)));
        assertEquals(id, binary("*", constant(1), id));
        // negate
        assertEquals(unary("-", id), binary("*", id, constant(-1)));
        assertEquals(unary("-", id), binary("*", constant(-1), id));
        // reduction
        assertEquals(binary("<<", id, constant(4)), binary("*", id, constant(16)));
        assertEquals(binary("<<", id, constant(4)), binary("*", constant(16), id));
    }

    @Test
    public void testDivision() {
        var id = identifier("A", DataType.INTEGER, Scope.Type.VARIABLE, DeclarationType.GLOBAL);
        // identity
        assertEquals(id, binary("/", id, constant(1)));
        // reduction
        assertEquals(binary(">>", id, constant(4)), binary("/", id, constant(16)));
    }

    @Test
    public void testAddition() {
        var id = identifier("A", DataType.INTEGER, Scope.Type.VARIABLE, DeclarationType.GLOBAL);
        // identity
        assertEquals(id, binary("+", id, constant(0)));
        assertEquals(id, binary("+", constant(0), id));
        // reduction
        assertEquals(binary("<<", id, constant(2)), binary("+", id, id));
    }

    @Test
    public void testSubtraction() {
        var id = identifier("A", DataType.INTEGER, Scope.Type.VARIABLE, DeclarationType.GLOBAL);
        // identity
        assertEquals(id, binary("-", id, constant(0)));
        // negate
        assertEquals(unary("-", id), binary("-", constant(0), id));
    }

    @Test
    public void testModulus() {
        var id = identifier("A", DataType.INTEGER, Scope.Type.VARIABLE, DeclarationType.GLOBAL);
        // identity
        assertEquals(binary("and", id, constant(15)), binary("mod", id, constant(16)));
    }
}
