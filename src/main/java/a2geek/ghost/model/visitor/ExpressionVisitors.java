package a2geek.ghost.model.visitor;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.expression.*;

import java.util.Objects;
import java.util.Set;

/**
 * This class contains simple visitors that are implemented in a single recursive method.
 */
public class ExpressionVisitors {
    private ExpressionVisitors() {
        // prevent construction
    }

    /**
     * Determine if the expression uses symbol.
     */
    public static boolean hasSymbol(Expression expr, Symbol symbol) {
        return switch (expr) {
            case AddressOfOperator addrOf -> Objects.equals(addrOf.getSymbol(), symbol);
            case ArrayLengthFunction arrayLen -> Objects.equals(arrayLen.getSymbol(), symbol);
            case BinaryExpression bin -> hasSymbol(bin.getL(), symbol) || hasSymbol(bin.getR(), symbol);
            case BooleanConstant ignored -> false;
            case FunctionExpression func -> func.getParameters().stream().map(param -> hasSymbol(param, symbol)).reduce(Boolean::logicalOr).orElse(false);
            case IntegerConstant ignored -> false;
            case ByteConstant ignored -> false;
            case PlaceholderExpression ignored -> false;
            case StringConstant ignored -> false;
            case UnaryExpression unary -> hasSymbol(unary.getExpr(), symbol);
            case VariableReference ref -> Objects.equals(ref.getSymbol(), symbol);
            default -> throw new RuntimeException("[compiler bug] unsupported expression type: " + expr);
        };
    }

    /**
     * Determine if expression contains target.
     */
    public static boolean hasSubexpression(Expression expression, Expression target) {
        if (Objects.equals(expression, target)) {
            return true;
        }
        return switch (expression) {
            case AddressOfOperator ignored -> false;
            case ArrayLengthFunction ignored -> false;
            case BinaryExpression bin -> hasSubexpression(bin.getL(), target) || hasSubexpression(bin.getR(), target);
            case BooleanConstant ignored -> false;
            case FunctionExpression func -> func.getParameters().stream().map(param -> hasSubexpression(param, target)).reduce(Boolean::logicalOr).orElse(false);
            case IntegerConstant ignored -> false;
            case ByteConstant ignored -> false;
            case PlaceholderExpression ignored -> false;
            case StringConstant ignored -> false;
            case UnaryExpression unary -> hasSubexpression(unary.getExpr(), target);
            case VariableReference ignored -> false;
            default -> throw new RuntimeException("[compiler bug] unsupported expression type: " + expression);
        };
    }

    /**
     * Recursively look for a volatile function. We don't want to optimize functions like
     * RND, ALLOC, etc. This is either marked in the function definition or in the intrinsic
     * definition.
     */
    public static boolean hasVolatileFunction(Expression expression) {
        return switch (expression) {
            case AddressOfOperator ignored -> false;
            case ArrayLengthFunction ignored -> false;
            case BinaryExpression binaryExpression -> hasVolatileFunction(binaryExpression.getL()) || hasVolatileFunction(binaryExpression.getR());
            case BooleanConstant ignored -> false;
            case FunctionExpression functionExpression -> {
                if (functionExpression.isVolatile()) {
                    yield true;
                }
                yield functionExpression.getParameters().stream().map(ExpressionVisitors::hasVolatileFunction).reduce(Boolean::logicalOr).orElse(false);
            }
            case IntegerConstant ignored -> false;
            case ByteConstant ignored -> false;
            case PlaceholderExpression ignored -> false;
            case StringConstant ignored -> false;
            case UnaryExpression unaryExpression -> hasVolatileFunction(unaryExpression.getExpr());
            case VariableReference ignored -> false;
            default -> throw new RuntimeException("[compiler bug] unexpected expression: " + expression);
        };
    }

    /**
     * Calculate the "weight" of an expression. Used to determine if this expression should be
     * considered for optimizations. Note that simple constants and variable references are a 1;
     * everything else will be larger. Hypothetically, this could be used to prioritize which
     * expressions are optimized first, should that become useful.
     */
    private static final Set<String> UNARY_IGNORE = Set.of("w2b", "b2w");   // no-ops and can cause problems
    public static int weight(Expression expression) {
        return switch (expression) {
            case AddressOfOperator ignored -> 1;
            case ArrayLengthFunction ignored -> 2;
            case BinaryExpression binaryExpression -> weight(binaryExpression.getL()) + weight(binaryExpression.getR());
            case BooleanConstant ignored -> 1;
            case FunctionExpression functionExpression -> 2 + functionExpression.getParameters().stream().mapToInt(ExpressionVisitors::weight).sum();
            case IntegerConstant ignored -> 1;
            case ByteConstant ignored -> 1;
            case PlaceholderExpression ignored -> 1;
            case StringConstant ignored -> 1;
            case UnaryExpression unaryExpression -> {
                if (UNARY_IGNORE.contains(unaryExpression.getOp())) {
                    yield weight(unaryExpression.getExpr());
                }
                yield 1 + weight(unaryExpression.getExpr());
            }
            case VariableReference ignored -> 1;
            default -> throw new RuntimeException("[compiler bug] unexpected expression: " + expression);
        };
    }
}
