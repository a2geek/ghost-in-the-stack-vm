package a2geek.ghost.model;

import a2geek.ghost.model.expression.BinaryExpression;
import a2geek.ghost.model.expression.DereferenceOperator;
import a2geek.ghost.model.expression.IntegerConstant;
import a2geek.ghost.model.expression.VariableReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static a2geek.ghost.model.CommonExpressions.arrayReference;
import static a2geek.ghost.model.expression.IntegerConstant.TWO;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommonExpressionTest {
    private static final Expression THREE = new IntegerConstant(3);
    private static final Expression FOUR = new IntegerConstant(4);
    private static final Expression FIVE = new IntegerConstant(5);
    private static final Expression TEN = new IntegerConstant(10);

    /**
     * This allows us to cheat a little bit and reduce a more complex binary expression to a number.
     */
    public void patch(Expression expression) {
        if (expression instanceof BinaryExpression bin) {
            bin.getL().asInteger().ifPresent(i -> bin.setL(new IntegerConstant(i)));
            bin.getR().asInteger().ifPresent(i -> bin.setR(new IntegerConstant(i)));
        }
        else if (expression instanceof DereferenceOperator deref) {
            patch(deref.getExpr());
        }
    }

    @Test
    public void testSingleArrayDimension() {
        var array = Symbol.variable("TEST", SymbolType.VARIABLE)
            .dataType(DataType.INTEGER)
            .dimensions(TEN)
            .build();
        // TEST(5) => 1*2 + 5*2 => 2 + 10
        var actual = arrayReference(array, List.of(FIVE));
        patch(actual);
        //  +------+---+---+---+---+---+---+---+---+---+---+----+
        //  | DIM1 | 0 | 1 | 2 | 3 | 4 |>5<| 6 | 7 | 8 | 9 | 10 |
        //  +------+---+---+---+---+---+---+---+---+---+---+----+
        var expected = VariableReference.with(array).plus(TWO).plus(new IntegerConstant(10)).deref();
        assertEquals(expected, actual);
    }

    @Test
    public void testTwoArrayDimensions() {
        var array = Symbol.variable("TEST", SymbolType.VARIABLE)
            .dataType(DataType.INTEGER)
            .dimensions(TWO, THREE)
            .build();
        // TEST(2,3) => 2*2 + 2*4*2 + 3*2 => 4 + 22
        var actual = arrayReference(array, List.of(TWO,THREE));
        patch(actual);
        // +-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+
        // | [2] | [3] | 0,0 | 0,1 | 0,2 | 0,3 | 1,0 | 1,1 | 1,2 | 1,3 | 2,0 | 2,1 | 2,2 | 2,3 |
        // +-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+
        var expected = VariableReference.with(array).plus(FOUR).plus(new IntegerConstant(22)).deref();
        assertEquals(expected, actual);
    }
}
