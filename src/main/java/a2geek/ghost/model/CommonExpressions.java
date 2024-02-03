package a2geek.ghost.model;

import a2geek.ghost.model.expression.IntegerConstant;
import a2geek.ghost.model.expression.UnaryExpression;
import a2geek.ghost.model.expression.VariableReference;

/**
 * A set of common expression evaluations that can be added into classes.
 */
public class CommonExpressions {
    private CommonExpressions() {
        // prevent construction
    }

    public static Expression arrayReference(Symbol array, Expression index) {
        // TODO need to take datatype into account for sizing
        return new UnaryExpression("*", VariableReference.with(array).plus(index.plus(IntegerConstant.ONE).times2()));
    }
}
