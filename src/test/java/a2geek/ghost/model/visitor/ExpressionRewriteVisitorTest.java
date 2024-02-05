package a2geek.ghost.model.visitor;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.DeclarationType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.SymbolType;
import org.junit.jupiter.api.Test;

import static a2geek.ghost.antlr.ExpressionBuilder.constant;
import static a2geek.ghost.antlr.ExpressionBuilder.identifier;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExpressionRewriteVisitorTest {
    @Test
    public void testOnlyAddition() {
        // ((1 + A) + 1) => (A + 2)
        ExpressionRewriteVisitor visitor = new ExpressionRewriteVisitor();
        Expression variable = identifier("A", DataType.INTEGER, SymbolType.VARIABLE, DeclarationType.GLOBAL);
        Expression actual = visitor.visit(constant(1).plus(variable).plus(constant(1)));
        Expression expected = variable.plus(constant(2));
        assertEquals(expected, actual);
    }
    @Test
    public void testOnlyMultiplication() {
        // ((2 * A) * 2) => (A * 4)
        ExpressionRewriteVisitor visitor = new ExpressionRewriteVisitor();
        Expression variable = identifier("A", DataType.INTEGER, SymbolType.VARIABLE, DeclarationType.GLOBAL);
        Expression actual = visitor.visit(constant(2).times(variable).times(constant(2)));
        Expression expected = variable.times(constant(4));
        assertEquals(expected, actual);
    }

    @Test
    public void testBothAdditionAndMultiplication() {
        // (((1 + A) + 2) * 2) => ((A + 3) * 2)
        ExpressionRewriteVisitor visitor = new ExpressionRewriteVisitor();
        Expression variable = identifier("A", DataType.INTEGER, SymbolType.VARIABLE, DeclarationType.GLOBAL);
        Expression actual = visitor.visit(constant(1).plus(variable).plus(constant(2))).times(constant(2));
        Expression expected = variable.plus(constant(3)).times(constant(2));
        assertEquals(expected, actual);
    }
}
